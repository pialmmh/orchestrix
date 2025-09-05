package com.orchestrix.automation.vxlan.core;

import com.orchestrix.automation.vxlan.api.VXLANService;
import com.orchestrix.automation.ssh.api.SSHClient;
import com.orchestrix.automation.ssh.core.BaseSSHClient;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Base implementation of VXLAN Service
 */
public abstract class BaseVXLANService implements VXLANService {
    
    protected Map<String, SSHClient> sshClients = new ConcurrentHashMap<>();
    protected ExecutorService executor = Executors.newCachedThreadPool();
    protected Properties config;
    
    public BaseVXLANService() {
        loadConfiguration();
    }
    
    /**
     * Load configuration from pulumi.conf
     */
    protected void loadConfiguration() {
        config = new Properties();
        String configPath = System.getProperty("vxlan.config.path", 
            "/home/mustafa/telcobright-projects/orchestrix/network/vxlan-lxc.conf");
        
        try {
            List<String> lines = Files.readAllLines(Paths.get(configPath));
            for (String line : lines) {
                line = line.trim();
                if (!line.startsWith("#") && line.contains("=")) {
                    String[] parts = line.split("=", 2);
                    config.setProperty(parts[0].trim(), parts[1].trim());
                }
            }
        } catch (IOException e) {
            System.err.println("Warning: Could not load config from " + configPath);
        }
    }
    
    /**
     * Get or create SSH connection to host
     */
    protected SSHClient getSSHClient(String host) throws VXLANException {
        return sshClients.computeIfAbsent(host, h -> {
            try {
                SSHClient client = createSSHClient();
                String sshUser = config.getProperty("SSH_USER", "root");
                String sshKey = config.getProperty("SSH_KEY_PATH", "~/.ssh/id_rsa");
                int sshPort = Integer.parseInt(config.getProperty("SSH_PORT", "22"));
                
                client.connect(h, sshPort, sshUser, expandPath(sshKey));
                return client;
            } catch (Exception e) {
                throw new RuntimeException("Failed to connect to " + h, e);
            }
        });
    }
    
    /**
     * Create SSH client - override to customize
     */
    protected abstract SSHClient createSSHClient();
    
    /**
     * Expand tilde in path
     */
    protected String expandPath(String path) {
        if (path.startsWith("~")) {
            return System.getProperty("user.home") + path.substring(1);
        }
        return path;
    }
    
    @Override
    public void initializeHost(String host, VXLANConfig config) throws VXLANException {
        SSHClient ssh = getSSHClient(host);
        
        try {
            // Enable IP forwarding
            ssh.executeCommand("sysctl -w net.ipv4.ip_forward=1");
            ssh.executeCommand("sysctl -w net.ipv6.conf.all.forwarding=1");
            
            // Load necessary kernel modules
            ssh.executeCommand("modprobe vxlan");
            ssh.executeCommand("modprobe bridge");
            
            // Install required packages if not present
            SSHClient.CommandResult result = ssh.executeCommand("which ip");
            if (!result.isSuccess()) {
                ssh.executeCommand("apt-get update && apt-get install -y iproute2");
            }
            
            result = ssh.executeCommand("which bridge");
            if (!result.isSuccess()) {
                ssh.executeCommand("apt-get install -y bridge-utils");
            }
            
            // Save sysctl settings
            ssh.executeCommand("echo 'net.ipv4.ip_forward=1' >> /etc/sysctl.conf");
            ssh.executeCommand("echo 'net.ipv6.conf.all.forwarding=1' >> /etc/sysctl.conf");
            
            afterInitializeHost(host, config);
            
        } catch (SSHClient.SSHException e) {
            throw new VXLANException("Failed to initialize host " + host, e);
        }
    }
    
    /**
     * Hook for subclasses after host initialization
     */
    protected void afterInitializeHost(String host, VXLANConfig config) throws VXLANException {
        // Override in subclasses
    }
    
    @Override
    public void createVXLAN(String host, String interfaceName, int vxlanId, String multicastGroup) throws VXLANException {
        SSHClient ssh = getSSHClient(host);
        
        try {
            // Check if interface already exists
            SSHClient.CommandResult result = ssh.executeCommand("ip link show " + interfaceName);
            if (result.isSuccess()) {
                throw new VXLANException("Interface " + interfaceName + " already exists on " + host);
            }
            
            // Get physical interface
            String physInterface = config.getProperty("MULTICAST_INTERFACE", "eth0");
            int port = Integer.parseInt(config.getProperty("VXLAN_PORT", "4789"));
            
            // Create VXLAN interface
            String cmd = String.format(
                "ip link add %s type vxlan id %d group %s dev %s dstport %d",
                interfaceName, vxlanId, multicastGroup, physInterface, port
            );
            
            result = ssh.executeCommand(cmd);
            if (!result.isSuccess()) {
                throw new VXLANException("Failed to create VXLAN: " + result.getStderr());
            }
            
            // Set MTU
            int mtu = Integer.parseInt(config.getProperty("VXLAN_MTU", "1450"));
            ssh.executeCommand("ip link set " + interfaceName + " mtu " + mtu);
            
            // Bring interface up
            ssh.executeCommand("ip link set " + interfaceName + " up");
            
            afterCreateVXLAN(host, interfaceName, vxlanId);
            
        } catch (SSHClient.SSHException e) {
            throw new VXLANException("Failed to create VXLAN on " + host, e);
        }
    }
    
    /**
     * Hook for subclasses after VXLAN creation
     */
    protected void afterCreateVXLAN(String host, String interfaceName, int vxlanId) throws VXLANException {
        // Override in subclasses
    }
    
    @Override
    public void deleteVXLAN(String host, String interfaceName) throws VXLANException {
        SSHClient ssh = getSSHClient(host);
        
        try {
            ssh.executeCommand("ip link set " + interfaceName + " down");
            ssh.executeCommand("ip link delete " + interfaceName);
        } catch (SSHClient.SSHException e) {
            throw new VXLANException("Failed to delete VXLAN on " + host, e);
        }
    }
    
    @Override
    public void createBridge(String host, String bridgeName, String vxlanInterface) throws VXLANException {
        SSHClient ssh = getSSHClient(host);
        
        try {
            // Create bridge
            ssh.executeCommand("ip link add name " + bridgeName + " type bridge");
            
            // Attach VXLAN to bridge
            ssh.executeCommand("ip link set " + vxlanInterface + " master " + bridgeName);
            
            // Enable STP
            ssh.executeCommand("ip link set " + bridgeName + " type bridge stp_state 1");
            
            // Bring bridge up
            ssh.executeCommand("ip link set " + bridgeName + " up");
            
        } catch (SSHClient.SSHException e) {
            throw new VXLANException("Failed to create bridge on " + host, e);
        }
    }
    
    @Override
    public void configureIP(String host, String interfaceName, String ipAddress, int prefixLength) throws VXLANException {
        SSHClient ssh = getSSHClient(host);
        
        try {
            String cmd = String.format("ip addr add %s/%d dev %s", ipAddress, prefixLength, interfaceName);
            SSHClient.CommandResult result = ssh.executeCommand(cmd);
            
            if (!result.isSuccess() && !result.getStderr().contains("File exists")) {
                throw new VXLANException("Failed to configure IP: " + result.getStderr());
            }
        } catch (SSHClient.SSHException e) {
            throw new VXLANException("Failed to configure IP on " + host, e);
        }
    }
    
    @Override
    public void addVTEP(String host, String remoteIP, String remoteMac) throws VXLANException {
        SSHClient ssh = getSSHClient(host);
        
        try {
            String vxlanName = config.getProperty("VXLAN_NAME", "vxlan0");
            String cmd = String.format("bridge fdb add %s dev %s dst %s", remoteMac, vxlanName, remoteIP);
            ssh.executeCommand(cmd);
        } catch (SSHClient.SSHException e) {
            throw new VXLANException("Failed to add VTEP on " + host, e);
        }
    }
    
    @Override
    public void removeVTEP(String host, String remoteIP) throws VXLANException {
        SSHClient ssh = getSSHClient(host);
        
        try {
            String vxlanName = config.getProperty("VXLAN_NAME", "vxlan0");
            ssh.executeCommand("bridge fdb del to 00:00:00:00:00:00 dst " + remoteIP + " dev " + vxlanName);
        } catch (SSHClient.SSHException e) {
            // Ignore errors as entry might not exist
        }
    }
    
    @Override
    public VXLANStatus getStatus(String host, String interfaceName) throws VXLANException {
        SSHClient ssh = getSSHClient(host);
        
        try {
            SSHClient.CommandResult result = ssh.executeCommand("ip link show " + interfaceName);
            
            if (!result.isSuccess()) {
                return new VXLANStatus(false, "NOT_FOUND");
            }
            
            String output = result.getStdout();
            boolean isUp = output.contains("state UP");
            
            VXLANStatus status = new VXLANStatus(isUp, isUp ? "UP" : "DOWN");
            
            // Get additional details
            Map<String, String> details = new HashMap<>();
            
            // Get IP addresses
            result = ssh.executeCommand("ip addr show " + interfaceName);
            if (result.isSuccess()) {
                Pattern ipPattern = Pattern.compile("inet (\\S+)");
                Matcher matcher = ipPattern.matcher(result.getStdout());
                if (matcher.find()) {
                    details.put("ipAddress", matcher.group(1));
                }
            }
            
            // Get statistics
            result = ssh.executeCommand("ip -s link show " + interfaceName);
            if (result.isSuccess()) {
                Pattern rxPattern = Pattern.compile("RX:\\s+bytes\\s+packets.*\\n\\s+(\\d+)\\s+(\\d+)");
                Pattern txPattern = Pattern.compile("TX:\\s+bytes\\s+packets.*\\n\\s+(\\d+)\\s+(\\d+)");
                
                Matcher rxMatcher = rxPattern.matcher(result.getStdout());
                Matcher txMatcher = txPattern.matcher(result.getStdout());
                
                if (rxMatcher.find()) {
                    details.put("rxBytes", rxMatcher.group(1));
                    details.put("rxPackets", rxMatcher.group(2));
                }
                if (txMatcher.find()) {
                    details.put("txBytes", txMatcher.group(1));
                    details.put("txPackets", txMatcher.group(2));
                }
            }
            
            status.setDetails(details);
            return status;
            
        } catch (SSHClient.SSHException e) {
            throw new VXLANException("Failed to get status on " + host, e);
        }
    }
    
    @Override
    public List<VXLANInterface> listVXLANs(String host) throws VXLANException {
        SSHClient ssh = getSSHClient(host);
        List<VXLANInterface> interfaces = new ArrayList<>();
        
        try {
            SSHClient.CommandResult result = ssh.executeCommand("ip -d link show type vxlan");
            
            if (result.isSuccess()) {
                String[] lines = result.getStdout().split("\n");
                VXLANInterface current = null;
                
                for (String line : lines) {
                    // Parse interface line
                    Pattern ifPattern = Pattern.compile("^\\d+: (\\S+):");
                    Matcher matcher = ifPattern.matcher(line);
                    if (matcher.find()) {
                        String name = matcher.group(1).replace("@NONE", "");
                        current = new VXLANInterface(name, 0, "UNKNOWN");
                        interfaces.add(current);
                    }
                    
                    // Parse VXLAN ID
                    if (current != null && line.contains("vxlan id")) {
                        Pattern idPattern = Pattern.compile("vxlan id (\\d+)");
                        matcher = idPattern.matcher(line);
                        if (matcher.find()) {
                            current.vxlanId = Integer.parseInt(matcher.group(1));
                        }
                    }
                    
                    // Parse state
                    if (current != null && line.contains("state")) {
                        Pattern statePattern = Pattern.compile("state (\\S+)");
                        matcher = statePattern.matcher(line);
                        if (matcher.find()) {
                            current.state = matcher.group(1);
                        }
                    }
                }
            }
            
        } catch (SSHClient.SSHException e) {
            throw new VXLANException("Failed to list VXLANs on " + host, e);
        }
        
        return interfaces;
    }
    
    @Override
    public void setupMesh(List<String> hosts, VXLANConfig config) throws VXLANException {
        // Initialize all hosts
        for (String host : hosts) {
            initializeHost(host, config);
        }
        
        // Create VXLAN on all hosts
        for (String host : hosts) {
            createVXLAN(host, config.getInterfaceName(), config.getVxlanId(), config.getMulticastGroup());
            
            if (config.getBridgeName() != null) {
                createBridge(host, config.getBridgeName(), config.getInterfaceName());
            }
        }
        
        // Configure IPs
        String subnet = config.getSubnet();
        String[] parts = subnet.split("/");
        String baseIP = parts[0];
        int prefix = Integer.parseInt(parts[1]);
        
        String[] ipParts = baseIP.split("\\.");
        int lastOctet = Integer.parseInt(ipParts[3]);
        
        for (int i = 0; i < hosts.size(); i++) {
            String hostIP = String.format("%s.%s.%s.%d", 
                ipParts[0], ipParts[1], ipParts[2], lastOctet + i + 1);
            
            String targetInterface = config.getBridgeName() != null ? 
                config.getBridgeName() : config.getInterfaceName();
                
            configureIP(hosts.get(i), targetInterface, hostIP, prefix);
        }
    }
    
    @Override
    public void teardownMesh(List<String> hosts, String vxlanName) throws VXLANException {
        for (String host : hosts) {
            try {
                deleteVXLAN(host, vxlanName);
            } catch (VXLANException e) {
                // Continue with other hosts
            }
        }
    }
    
    @Override
    public HealthStatus healthCheck(String host, String interfaceName) throws VXLANException {
        VXLANStatus status = getStatus(host, interfaceName);
        
        HealthStatus health = new HealthStatus(status.isActive(), 
            status.isActive() ? "HEALTHY" : "UNHEALTHY");
        
        List<String> issues = new ArrayList<>();
        
        if (!status.isActive()) {
            issues.add("Interface is down");
        }
        
        // Check multicast
        SSHClient ssh = getSSHClient(host);
        try {
            SSHClient.CommandResult result = ssh.executeCommand("ip maddr show " + interfaceName);
            if (!result.isSuccess() || !result.getStdout().contains("239.")) {
                issues.add("Multicast not configured");
            }
        } catch (SSHClient.SSHException e) {
            issues.add("Could not check multicast: " + e.getMessage());
        }
        
        health.setIssues(issues);
        
        return health;
    }
    
    @Override
    public String backupConfiguration(String host) throws VXLANException {
        SSHClient ssh = getSSHClient(host);
        StringBuilder backup = new StringBuilder();
        
        try {
            // Backup network configuration
            backup.append("# VXLAN Configuration Backup\n");
            backup.append("# Host: ").append(host).append("\n");
            backup.append("# Date: ").append(new Date()).append("\n\n");
            
            // Get all VXLAN interfaces
            SSHClient.CommandResult result = ssh.executeCommand("ip -d link show type vxlan");
            backup.append("# VXLAN Interfaces:\n");
            backup.append(result.getStdout()).append("\n\n");
            
            // Get bridge configuration
            result = ssh.executeCommand("brctl show 2>/dev/null || bridge link show");
            backup.append("# Bridges:\n");
            backup.append(result.getStdout()).append("\n\n");
            
            // Get IP configuration
            result = ssh.executeCommand("ip addr show");
            backup.append("# IP Addresses:\n");
            backup.append(result.getStdout()).append("\n\n");
            
            // Get routing
            result = ssh.executeCommand("ip route show");
            backup.append("# Routes:\n");
            backup.append(result.getStdout()).append("\n\n");
            
        } catch (SSHClient.SSHException e) {
            throw new VXLANException("Failed to backup configuration on " + host, e);
        }
        
        return backup.toString();
    }
    
    @Override
    public void restoreConfiguration(String host, String backupData) throws VXLANException {
        // Parse backup and restore
        // This is a placeholder - implement based on backup format
        throw new VXLANException("Restore not yet implemented");
    }
    
    /**
     * Cleanup resources
     */
    public void cleanup() {
        for (SSHClient client : sshClients.values()) {
            try {
                client.close();
            } catch (Exception e) {
                // Ignore
            }
        }
        sshClients.clear();
        executor.shutdown();
    }
}