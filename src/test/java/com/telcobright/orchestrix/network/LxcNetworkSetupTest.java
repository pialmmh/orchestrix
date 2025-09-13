package com.telcobright.orchestrix.network;

import com.jcraft.jsch.*;
import com.telcobright.orchestrix.network.entity.Bridge;
import com.telcobright.orchestrix.network.entity.lxc.LxcContainer;
import com.telcobright.orchestrix.network.entity.lxc.LxcNetworkConfig;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * Test class to set up LXC networking via SSH
 * This will execute commands on your local machine through SSH
 */
public class LxcNetworkSetupTest {
    
    private static final String HOST = "localhost";
    private static final int PORT = 22;
    private static final String USERNAME = "mustafa"; // Change as needed
    private static final String PASSWORD = ""; // Set password or use key
    private static final String KEY_PATH = System.getProperty("user.home") + "/.ssh/id_rsa";
    
    private Session session;
    
    /**
     * Initialize SSH connection
     */
    public void connect() throws JSchException {
        JSch jsch = new JSch();
        
        // Use SSH key if available, otherwise use password
        if (!KEY_PATH.isEmpty() && new java.io.File(KEY_PATH).exists()) {
            jsch.addIdentity(KEY_PATH);
        }
        
        session = jsch.getSession(USERNAME, HOST, PORT);
        
        if (!PASSWORD.isEmpty()) {
            session.setPassword(PASSWORD);
        }
        
        // SSH config
        Properties config = new Properties();
        config.put("StrictHostKeyChecking", "no");
        session.setConfig(config);
        
        session.connect();
        System.out.println("Connected to " + HOST);
    }
    
    /**
     * Execute command via SSH
     */
    public String executeCommand(String command) throws Exception {
        ChannelExec channelExec = (ChannelExec) session.openChannel("exec");
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        ByteArrayOutputStream errorStream = new ByteArrayOutputStream();
        
        channelExec.setCommand(command);
        channelExec.setOutputStream(outputStream);
        channelExec.setErrStream(errorStream);
        
        channelExec.connect();
        
        // Wait for command to complete
        while (!channelExec.isClosed()) {
            Thread.sleep(100);
        }
        
        int exitStatus = channelExec.getExitStatus();
        channelExec.disconnect();
        
        String output = outputStream.toString();
        String error = errorStream.toString();
        
        if (exitStatus != 0) {
            System.err.println("Command failed: " + command);
            System.err.println("Error: " + error);
        }
        
        return output;
    }
    
    /**
     * Execute command with sudo
     */
    public String executeSudoCommand(String command) throws Exception {
        // Note: This assumes passwordless sudo or you'll need to handle password prompt
        return executeCommand("sudo " + command);
    }
    
    /**
     * Set up bridge based on Bridge entity
     */
    public void setupBridge(Bridge bridge) throws Exception {
        System.out.println("Setting up bridge: " + bridge.getInterfaceName());
        
        List<String> commands = new ArrayList<>();
        
        // Check if bridge exists
        String checkBridge = executeCommand("ip link show " + bridge.getInterfaceName() + " 2>/dev/null | wc -l");
        boolean bridgeExists = !checkBridge.trim().equals("0");
        
        if (bridgeExists) {
            System.out.println("Bridge " + bridge.getInterfaceName() + " already exists, deleting...");
            commands.add("ip link delete " + bridge.getInterfaceName());
        }
        
        // Create bridge
        commands.add("ip link add " + bridge.getInterfaceName() + " type bridge");
        
        // Configure bridge settings
        if (!bridge.getStpEnabled()) {
            commands.add("echo 0 > /sys/class/net/" + bridge.getInterfaceName() + "/bridge/stp_state");
        }
        
        if (bridge.getForwardDelay() != null) {
            commands.add("echo " + bridge.getForwardDelay() + 
                " > /sys/class/net/" + bridge.getInterfaceName() + "/bridge/forward_delay");
        }
        
        // Bring up bridge
        commands.add("ip link set " + bridge.getInterfaceName() + " up");
        
        // Add IP address to bridge (host's gateway IP)
        if (bridge.getIpAddress() != null && bridge.getNetmask() != null) {
            commands.add("ip addr add " + bridge.getCidr() + " dev " + bridge.getInterfaceName());
        }
        
        // Enable IP forwarding
        if (bridge.getIpForwardEnabled()) {
            commands.add("sysctl -w net.ipv4.ip_forward=1");
        }
        
        // Get default interface for NAT
        String defaultIf = executeCommand("ip route | grep default | awk '{print $5}' | head -n1").trim();
        
        // Set up NAT/Masquerade if enabled
        if (bridge.getMasqueradeEnabled() && bridge.getNetworkAddress() != null) {
            // Remove existing rule if any
            commands.add("iptables -t nat -D POSTROUTING -s " + bridge.getNetworkAddress() + 
                " -j MASQUERADE 2>/dev/null || true");
            
            // Add NAT rule
            commands.add("iptables -t nat -A POSTROUTING -s " + bridge.getNetworkAddress() + 
                " -o " + defaultIf + " -j MASQUERADE");
            
            // Allow forwarding
            commands.add("iptables -D FORWARD -i " + bridge.getInterfaceName() + " -j ACCEPT 2>/dev/null || true");
            commands.add("iptables -D FORWARD -o " + bridge.getInterfaceName() + " -j ACCEPT 2>/dev/null || true");
            commands.add("iptables -A FORWARD -i " + bridge.getInterfaceName() + " -j ACCEPT");
            commands.add("iptables -A FORWARD -o " + bridge.getInterfaceName() + " -j ACCEPT");
        }
        
        // Execute all commands
        for (String cmd : commands) {
            System.out.println("Executing: " + cmd);
            executeSudoCommand(cmd);
        }
        
        // Configure LXD to use this bridge
        String lxdCheck = executeCommand("lxc network list | grep " + bridge.getInterfaceName() + " | wc -l");
        if (!lxdCheck.trim().equals("0")) {
            System.out.println("LXD network exists, updating...");
            executeCommand("lxc network delete " + bridge.getInterfaceName() + " 2>/dev/null || true");
        }
        
        executeCommand("lxc network create " + bridge.getInterfaceName() + 
            " ipv4.address=none ipv6.address=none ipv4.nat=false ipv6.nat=false " +
            " ipv4.dhcp=false ipv6.dhcp=false");
        
        System.out.println("Bridge setup complete!");
    }
    
    /**
     * Launch LXC container with static IP configuration
     */
    public void launchContainer(LxcContainer container) throws Exception {
        System.out.println("Launching container: " + container.getContainerName());
        
        LxcNetworkConfig netConfig = container.getNetworkConfig();
        if (netConfig == null) {
            throw new IllegalArgumentException("Container must have network configuration");
        }
        
        // Check if container exists
        String checkContainer = executeCommand("lxc list " + container.getContainerName() + " --format=json | jq length");
        if (!checkContainer.trim().equals("0")) {
            System.out.println("Container exists, stopping and deleting...");
            executeCommand("lxc stop " + container.getContainerName() + " --force 2>/dev/null || true");
            executeCommand("lxc delete " + container.getContainerName() + " --force");
        }
        
        // Launch container
        String launchCmd = "lxc launch " + container.getBaseImage() + " " + 
            container.getContainerName() + " --network=" + netConfig.getBridgeName();
        System.out.println("Executing: " + launchCmd);
        executeCommand(launchCmd);
        
        // Wait for container to start
        Thread.sleep(3000);
        
        // Configure static IP
        System.out.println("Configuring static IP: " + netConfig.getIpAddress());
        
        // Create netplan config
        String netplanConfig = netConfig.generateNetplanConfig().replace("\n", "\\n").replace("\"", "\\\"");
        executeCommand("lxc exec " + container.getContainerName() + 
            " -- bash -c \"echo '" + netplanConfig + "' > /etc/netplan/10-lxc.yaml\"");
        
        // Apply netplan
        String applyResult = executeCommand("lxc exec " + container.getContainerName() + 
            " -- netplan apply 2>&1 || echo 'netplan-failed'");
        
        if (applyResult.contains("netplan-failed")) {
            System.out.println("Netplan not available, using ip commands...");
            
            // Use ip commands as fallback
            String ipCommands = netConfig.generateIpCommands();
            for (String cmd : ipCommands.split("\n")) {
                if (!cmd.trim().isEmpty()) {
                    executeCommand("lxc exec " + container.getContainerName() + " -- bash -c \"" + cmd + "\"");
                }
            }
        }
        
        // Configure mounts if any
        if (container.getMounts() != null) {
            container.getMounts().forEach(mount -> {
                try {
                    // Create directory if needed
                    if (mount.getCreateIfMissing()) {
                        executeSudoCommand("mkdir -p " + mount.getHostPath());
                    }
                    
                    // Add mount
                    executeCommand("lxc config device add " + container.getContainerName() + 
                        " " + mount.getMountName() + " disk source=" + mount.getHostPath() + 
                        " path=" + mount.getContainerPath());
                    
                    System.out.println("Mounted: " + mount.getHostPath() + " -> " + mount.getContainerPath());
                } catch (Exception e) {
                    System.err.println("Failed to add mount: " + e.getMessage());
                }
            });
        }
        
        System.out.println("Container launched successfully!");
        
        // Show container info
        String containerInfo = executeCommand("lxc list " + container.getContainerName());
        System.out.println(containerInfo);
    }
    
    /**
     * Disconnect SSH session
     */
    public void disconnect() {
        if (session != null && session.isConnected()) {
            session.disconnect();
            System.out.println("Disconnected from " + HOST);
        }
    }
    
    /**
     * Main test method - DO NOT RUN WITHOUT EXPLICIT REQUEST
     */
    public static void main(String[] args) {
        System.out.println("==================================================");
        System.out.println("LXC Network Setup Test");
        System.out.println("This will configure networking on your system!");
        System.out.println("DO NOT RUN without explicit permission!");
        System.out.println("==================================================");
        
        // Uncomment only when ready to test
        /*
        LxcNetworkSetupTest test = new LxcNetworkSetupTest();
        
        try {
            // Connect to SSH
            test.connect();
            
            // Create bridge configuration
            Bridge bridge = new Bridge();
            bridge.setInterfaceName("lxcbr0");
            bridge.setIpAddress("10.10.199.1");
            bridge.setNetmask(24);
            bridge.setStpEnabled(false);
            bridge.setIpForwardEnabled(true);
            bridge.setMasqueradeEnabled(true);
            
            // Set up bridge
            test.setupBridge(bridge);
            
            // Create container configuration
            LxcContainer container = new LxcContainer();
            container.setContainerName("test-static-ip");
            container.setBaseImage("images:debian/12");
            
            LxcNetworkConfig netConfig = new LxcNetworkConfig();
            netConfig.setBridgeName("lxcbr0");
            netConfig.setIpAddress("10.10.199.100");
            netConfig.setNetmask(24);
            netConfig.setGateway("10.10.199.1");
            netConfig.setDnsServers("8.8.8.8,8.8.4.4");
            container.setNetworkConfig(netConfig);
            
            // Launch container
            test.launchContainer(container);
            
            // Test connectivity
            String pingTest = test.executeCommand("ping -c 1 10.10.199.100");
            System.out.println("Ping test: " + (pingTest.contains("1 received") ? "SUCCESS" : "FAILED"));
            
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            test.disconnect();
        }
        */
    }
}