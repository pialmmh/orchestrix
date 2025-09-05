package com.orchestrix.automation.vxlan.core;

import com.orchestrix.automation.ssh.api.SSHClient;
import com.orchestrix.automation.ssh.core.BaseSSHClient;
import com.orchestrix.automation.vxlan.api.LXCNetworkManager;
import com.orchestrix.automation.vxlan.api.VXLANService;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

/**
 * Main orchestrator for VXLAN network management on PC with LXC containers
 */
public class VXLANOrchestrator {
    
    private static final Logger logger = Logger.getLogger(VXLANOrchestrator.class.getName());
    
    private final SSHClient sshClient;
    private final SubnetManager subnetManager;
    private final LXCNetworkManagerImpl lxcManager;
    private final Properties config;
    private final ExecutorService executor;
    private final Map<String, Boolean> initializedSubnets;
    
    public VXLANOrchestrator() {
        this(new OrchestriXSSHClient(), 
             new SubnetManager(),
             loadConfig());
    }
    
    public VXLANOrchestrator(SSHClient sshClient, SubnetManager subnetManager, Properties config) {
        this.sshClient = sshClient;
        this.subnetManager = subnetManager;
        this.config = config;
        this.lxcManager = new LXCNetworkManagerImpl(sshClient, subnetManager);
        this.executor = Executors.newCachedThreadPool();
        this.initializedSubnets = new ConcurrentHashMap<>();
    }
    
    /**
     * Initialize the orchestrator and connect to host
     */
    public void initialize() throws OrchestratorException {
        try {
            String host = config.getProperty("HOST_IP", "127.0.0.1");
            String user = config.getProperty("HOST_SSH_USER", "mustafa");
            String keyPath = config.getProperty("HOST_SSH_KEY", "~/.ssh/id_rsa");
            int port = Integer.parseInt(config.getProperty("HOST_SSH_PORT", "22"));
            
            // Expand tilde in path
            if (keyPath.startsWith("~")) {
                keyPath = System.getProperty("user.home") + keyPath.substring(1);
            }
            
            logger.info("Connecting to host " + host + " as " + user);
            sshClient.connect(host, port, user, keyPath);
            
            // Initialize host for VXLAN
            initializeHost();
            
        } catch (Exception e) {
            throw new OrchestratorException("Failed to initialize orchestrator", e);
        }
    }
    
    /**
     * Initialize host for VXLAN operations
     */
    private void initializeHost() throws OrchestratorException {
        try {
            logger.info("Initializing host for VXLAN");
            
            // Enable IP forwarding
            executeCommand("sysctl -w net.ipv4.ip_forward=1");
            executeCommand("sysctl -w net.ipv6.conf.all.forwarding=1");
            
            // Load kernel modules
            executeCommand("modprobe vxlan");
            executeCommand("modprobe bridge");
            
            // Make changes persistent
            executeCommand("echo 'net.ipv4.ip_forward=1' >> /etc/sysctl.conf");
            executeCommand("echo 'net.ipv6.conf.all.forwarding=1' >> /etc/sysctl.conf");
            
        } catch (Exception e) {
            throw new OrchestratorException("Failed to initialize host", e);
        }
    }
    
    /**
     * Setup all configured subnets
     */
    public void setupAllSubnets() throws OrchestratorException {
        logger.info("Setting up all configured subnets");
        
        for (SubnetManager.Subnet subnet : subnetManager.getAllSubnets()) {
            try {
                setupSubnet(subnet.getName());
            } catch (Exception e) {
                logger.warning("Failed to setup subnet " + subnet.getName() + ": " + e.getMessage());
            }
        }
    }
    
    /**
     * Setup a specific subnet
     */
    public void setupSubnet(String subnetName) throws OrchestratorException {
        if (initializedSubnets.getOrDefault(subnetName, false)) {
            logger.info("Subnet " + subnetName + " already initialized");
            return;
        }
        
        SubnetManager.Subnet subnet = subnetManager.getSubnet(subnetName);
        if (subnet == null) {
            throw new OrchestratorException("Subnet not found: " + subnetName);
        }
        
        try {
            logger.info("Setting up subnet: " + subnetName);
            
            // Create VXLAN interface
            String vxlanIface = subnet.getVxlanInterface();
            String physIface = config.getProperty("NAT_INTERFACE", "eth0");
            int port = Integer.parseInt(config.getProperty("VXLAN_PORT", "4789"));
            
            // Check if interface already exists
            if (!interfaceExists(vxlanIface)) {
                String cmd = String.format(
                    "ip link add %s type vxlan id %d group %s dev %s dstport %d",
                    vxlanIface, subnet.getVxlanId(), subnet.getMulticastGroup(), physIface, port
                );
                executeCommand(cmd);
                
                // Set MTU
                int mtu = Integer.parseInt(config.getProperty("VXLAN_MTU", "1450"));
                executeCommand("ip link set " + vxlanIface + " mtu " + mtu);
                
                // Bring interface up
                executeCommand("ip link set " + vxlanIface + " up");
            }
            
            // Create bridge
            String bridgeName = subnet.getBridgeName();
            if (!interfaceExists(bridgeName)) {
                executeCommand("ip link add name " + bridgeName + " type bridge");
                
                // Configure bridge
                String stp = config.getProperty("BRIDGE_STP", "on");
                executeCommand("ip link set " + bridgeName + " type bridge stp_state " + 
                              (stp.equals("on") ? "1" : "0"));
                
                // Attach VXLAN to bridge
                executeCommand("ip link set " + vxlanIface + " master " + bridgeName);
                
                // Bring bridge up
                executeCommand("ip link set " + bridgeName + " up");
            }
            
            // Configure gateway IP on bridge
            String gatewayIP = subnet.getGateway();
            int prefix = subnet.getPrefixLength();
            
            // Check if IP already assigned
            if (!ipAssigned(bridgeName, gatewayIP)) {
                executeCommand(String.format("ip addr add %s/%d dev %s", 
                    gatewayIP, prefix, bridgeName));
            }
            
            // Setup NAT if enabled
            if (isNATEnabled(subnetName)) {
                setupNAT(subnet);
            }
            
            // Apply routing rules
            applyRoutingRules(subnet);
            
            initializedSubnets.put(subnetName, true);
            logger.info("Subnet " + subnetName + " setup completed");
            
        } catch (Exception e) {
            throw new OrchestratorException("Failed to setup subnet " + subnetName, e);
        }
    }
    
    /**
     * Configure LXC container network
     */
    public void configureContainer(String containerName) throws OrchestratorException {
        try {
            // Find container configuration
            ContainerConfig containerConfig = findContainerConfig(containerName);
            if (containerConfig == null) {
                // Use default subnet
                SubnetManager.Subnet defaultSubnet = subnetManager.getDefaultSubnet();
                String ip = subnetManager.allocateIP(defaultSubnet.getName());
                containerConfig = new ContainerConfig(containerName, defaultSubnet.getName(), ip);
            }
            
            // Ensure subnet is setup
            setupSubnet(containerConfig.subnet);
            
            // Configure container
            lxcManager.configureContainer(containerName, containerConfig);
            
            logger.info("Container " + containerName + " configured with IP " + 
                       containerConfig.ipAddress + " in subnet " + containerConfig.subnet);
            
        } catch (Exception e) {
            throw new OrchestratorException("Failed to configure container " + containerName, e);
        }
    }
    
    /**
     * Assign IP to container
     */
    public void assignIPToContainer(String containerName, String subnet, String ipAddress) 
            throws OrchestratorException {
        try {
            // Ensure subnet is setup
            setupSubnet(subnet);
            
            // Allocate IP
            subnetManager.allocateSpecificIP(subnet, ipAddress);
            
            // Configure container
            lxcManager.assignIP(containerName, subnet, ipAddress);
            
            logger.info("Assigned IP " + ipAddress + " to container " + containerName);
            
        } catch (Exception e) {
            throw new OrchestratorException("Failed to assign IP to container " + containerName, e);
        }
    }
    
    /**
     * Move container to different subnet
     */
    public void moveContainerToSubnet(String containerName, String targetSubnet, String newIP) 
            throws OrchestratorException {
        try {
            // Get current configuration
            LXCNetworkManager.ContainerNetworkConfig currentConfig = 
                lxcManager.getContainerConfig(containerName);
            
            if (currentConfig != null) {
                // Release old IP
                subnetManager.releaseIP(currentConfig.getSubnet(), currentConfig.getIpAddress());
            }
            
            // Ensure target subnet is setup
            setupSubnet(targetSubnet);
            
            // Assign new IP
            if (newIP == null) {
                newIP = subnetManager.allocateIP(targetSubnet);
            } else {
                subnetManager.allocateSpecificIP(targetSubnet, newIP);
            }
            
            // Move container
            lxcManager.moveToSubnet(containerName, targetSubnet, newIP);
            
            logger.info("Moved container " + containerName + " to subnet " + targetSubnet + 
                       " with IP " + newIP);
            
        } catch (Exception e) {
            throw new OrchestratorException("Failed to move container " + containerName, e);
        }
    }
    
    /**
     * Setup NAT for subnet
     */
    private void setupNAT(SubnetManager.Subnet subnet) throws Exception {
        String natInterface = config.getProperty("NAT_INTERFACE", "eth0");
        String cidr = subnet.getCidr();
        
        // Add iptables rules for NAT
        String natRule = String.format(
            "iptables -t nat -A POSTROUTING -s %s ! -d %s -o %s -j MASQUERADE",
            cidr, cidr, natInterface
        );
        
        // Check if rule exists
        SSHClient.CommandResult result = sshClient.executeCommand(
            "iptables -t nat -L POSTROUTING -n | grep '" + cidr + "'"
        );
        
        if (!result.isSuccess() || result.getStdout().isEmpty()) {
            executeCommand(natRule);
            
            // Save iptables rules
            executeCommand("iptables-save > /etc/iptables/rules.v4");
        }
    }
    
    /**
     * Apply routing rules between subnets
     */
    private void applyRoutingRules(SubnetManager.Subnet subnet) throws Exception {
        List<SubnetManager.RoutingRule> rules = subnetManager.getRoutingRules();
        
        for (SubnetManager.RoutingRule rule : rules) {
            if (rule.getSourceSubnet().equals(subnet.getName()) || 
                rule.getSourceSubnet().equals("*")) {
                
                String action = rule.isAllowed() ? "ACCEPT" : "DROP";
                SubnetManager.Subnet destSubnet = null;
                
                if (!rule.getDestSubnet().equals("*")) {
                    destSubnet = subnetManager.getSubnet(rule.getDestSubnet());
                }
                
                if (destSubnet != null) {
                    String iptablesRule = String.format(
                        "iptables -A FORWARD -s %s -d %s -j %s",
                        subnet.getCidr(), destSubnet.getCidr(), action
                    );
                    
                    // Check if rule exists
                    SSHClient.CommandResult result = sshClient.executeCommand(
                        "iptables -L FORWARD -n | grep '" + subnet.getCidr() + ".*" + 
                        destSubnet.getCidr() + "'"
                    );
                    
                    if (!result.isSuccess() || result.getStdout().isEmpty()) {
                        executeCommand(iptablesRule);
                    }
                }
            }
        }
    }
    
    /**
     * Check if interface exists
     */
    private boolean interfaceExists(String ifaceName) throws Exception {
        SSHClient.CommandResult result = sshClient.executeCommand("ip link show " + ifaceName);
        return result.isSuccess();
    }
    
    /**
     * Check if IP is assigned to interface
     */
    private boolean ipAssigned(String ifaceName, String ip) throws Exception {
        SSHClient.CommandResult result = sshClient.executeCommand("ip addr show " + ifaceName);
        return result.isSuccess() && result.getStdout().contains(ip);
    }
    
    /**
     * Check if NAT is enabled for subnet
     */
    private boolean isNATEnabled(String subnetName) {
        String natSubnets = config.getProperty("NAT_SUBNETS", "");
        return natSubnets.contains(subnetName);
    }
    
    /**
     * Find container configuration
     */
    private ContainerConfig findContainerConfig(String containerName) {
        String containerIPs = config.getProperty("CONTAINER_IPS", "");
        if (!containerIPs.isEmpty()) {
            String[] entries = containerIPs.split("\\s+");
            for (String entry : entries) {
                entry = entry.trim().replace("\"", "");
                if (entry.isEmpty()) continue;
                
                String[] parts = entry.split(":");
                if (parts.length >= 3 && parts[0].equals(containerName)) {
                    return new ContainerConfig(parts[0], parts[1], parts[2]);
                }
            }
        }
        return null;
    }
    
    /**
     * Execute command via SSH
     */
    private SSHClient.CommandResult executeCommand(String command) throws Exception {
        SSHClient.CommandResult result = sshClient.executeCommand(command);
        if (!result.isSuccess() && !result.getStderr().contains("File exists")) {
            throw new Exception("Command failed: " + command + " - " + result.getStderr());
        }
        return result;
    }
    
    /**
     * Get status of all subnets
     */
    public Map<String, SubnetStatus> getAllSubnetStatus() throws OrchestratorException {
        Map<String, SubnetStatus> statuses = new HashMap<>();
        
        for (SubnetManager.Subnet subnet : subnetManager.getAllSubnets()) {
            try {
                SubnetStatus status = getSubnetStatus(subnet.getName());
                statuses.put(subnet.getName(), status);
            } catch (Exception e) {
                logger.warning("Failed to get status for subnet " + subnet.getName());
            }
        }
        
        return statuses;
    }
    
    /**
     * Get subnet status
     */
    public SubnetStatus getSubnetStatus(String subnetName) throws OrchestratorException {
        SubnetManager.Subnet subnet = subnetManager.getSubnet(subnetName);
        if (subnet == null) {
            throw new OrchestratorException("Subnet not found: " + subnetName);
        }
        
        try {
            SubnetStatus status = new SubnetStatus(subnetName);
            
            // Check if interfaces exist
            status.vxlanExists = interfaceExists(subnet.getVxlanInterface());
            status.bridgeExists = interfaceExists(subnet.getBridgeName());
            
            // Get container count
            List<LXCNetworkManager.ContainerInfo> containers = 
                lxcManager.listContainersInSubnet(subnetName);
            status.containerCount = containers.size();
            
            // Get allocated IPs
            status.allocatedIPs = subnetManager.getAllocatedIPs(subnetName).size();
            
            // Check if gateway is configured
            if (status.bridgeExists) {
                status.gatewayConfigured = ipAssigned(subnet.getBridgeName(), subnet.getGateway());
            }
            
            return status;
            
        } catch (Exception e) {
            throw new OrchestratorException("Failed to get subnet status", e);
        }
    }
    
    /**
     * Cleanup and shutdown
     */
    public void shutdown() {
        try {
            if (sshClient != null) {
                sshClient.close();
            }
            executor.shutdown();
        } catch (Exception e) {
            logger.warning("Error during shutdown: " + e.getMessage());
        }
    }
    
    /**
     * Load configuration
     */
    private static Properties loadConfig() {
        Properties props = new Properties();
        String configPath = "/home/mustafa/telcobright-projects/orchestrix/network/vxlan-lxc.conf";
        
        try {
            List<String> lines = Files.readAllLines(Paths.get(configPath));
            for (String line : lines) {
                line = line.trim();
                if (!line.startsWith("#") && line.contains("=")) {
                    String[] parts = line.split("=", 2);
                    props.setProperty(parts[0].trim(), parts[1].trim().replace("\"", ""));
                }
            }
        } catch (IOException e) {
            logger.warning("Could not load config from " + configPath);
        }
        
        return props;
    }
    
    /**
     * Container configuration
     */
    private static class ContainerConfig extends LXCNetworkManager.ContainerNetworkConfig {
        public ContainerConfig(String name, String subnet, String ip) {
            setContainerName(name);
            setSubnet(subnet);
            setIpAddress(ip);
        }
    }
    
    /**
     * Subnet status
     */
    public static class SubnetStatus {
        public String name;
        public boolean vxlanExists;
        public boolean bridgeExists;
        public boolean gatewayConfigured;
        public int containerCount;
        public int allocatedIPs;
        
        public SubnetStatus(String name) {
            this.name = name;
        }
    }
    
    /**
     * SSH Client implementation
     */
    private static class OrchestriXSSHClient extends BaseSSHClient {
        @Override
        protected void afterConnect() throws SSHException {
            System.out.println("Connected to host for VXLAN orchestration");
        }
    }
    
    /**
     * LXC Network Manager implementation
     */
    private static class LXCNetworkManagerImpl implements LXCNetworkManager {
        private final SSHClient sshClient;
        private final SubnetManager subnetManager;
        
        public LXCNetworkManagerImpl(SSHClient sshClient, SubnetManager subnetManager) {
            this.sshClient = sshClient;
            this.subnetManager = subnetManager;
        }
        
        @Override
        public void configureContainer(String containerName, ContainerNetworkConfig config) 
                throws NetworkException {
            try {
                SubnetManager.Subnet subnet = subnetManager.getSubnet(config.getSubnet());
                if (subnet == null) {
                    throw new NetworkException("Subnet not found: " + config.getSubnet());
                }
                
                // Add network device to container
                String deviceName = "eth1";
                String cmd = String.format(
                    "lxc config device add %s %s nic name=%s nictype=bridged parent=%s",
                    containerName, deviceName, deviceName, subnet.getBridgeName()
                );
                
                SSHClient.CommandResult result = sshClient.executeCommand(cmd);
                if (!result.isSuccess() && !result.getStderr().contains("already exists")) {
                    throw new NetworkException("Failed to add network device: " + result.getStderr());
                }
                
                // Configure IP inside container
                String ipConfig = String.format(
                    "lxc exec %s -- ip addr add %s/%d dev %s",
                    containerName, config.getIpAddress(), subnet.getPrefixLength(), deviceName
                );
                sshClient.executeCommand(ipConfig);
                
                // Bring interface up
                sshClient.executeCommand(String.format(
                    "lxc exec %s -- ip link set %s up",
                    containerName, deviceName
                ));
                
                // Set default route
                sshClient.executeCommand(String.format(
                    "lxc exec %s -- ip route add default via %s",
                    containerName, subnet.getGateway()
                ));
                
            } catch (Exception e) {
                throw new NetworkException("Failed to configure container " + containerName, e);
            }
        }
        
        @Override
        public void assignIP(String containerName, String subnet, String ipAddress) 
                throws NetworkException {
            ContainerNetworkConfig config = new ContainerNetworkConfig();
            config.setContainerName(containerName);
            config.setSubnet(subnet);
            config.setIpAddress(ipAddress);
            configureContainer(containerName, config);
        }
        
        @Override
        public ContainerNetworkConfig getContainerConfig(String containerName) 
                throws NetworkException {
            try {
                SSHClient.CommandResult result = sshClient.executeCommand(
                    "lxc config device show " + containerName
                );
                
                if (result.isSuccess()) {
                    // Parse configuration
                    ContainerNetworkConfig config = new ContainerNetworkConfig();
                    config.setContainerName(containerName);
                    // Parse output to get network configuration
                    return config;
                }
                
            } catch (Exception e) {
                throw new NetworkException("Failed to get container config", e);
            }
            return null;
        }
        
        @Override
        public List<ContainerInfo> listContainersInSubnet(String subnet) throws NetworkException {
            List<ContainerInfo> containers = new ArrayList<>();
            
            try {
                // List all containers
                SSHClient.CommandResult result = sshClient.executeCommand("lxc list --format csv");
                
                if (result.isSuccess()) {
                    String[] lines = result.getStdout().split("\n");
                    for (String line : lines) {
                        if (!line.isEmpty()) {
                            String[] parts = line.split(",");
                            if (parts.length > 0) {
                                String name = parts[0];
                                // Check if container is in subnet
                                // This would need actual IP checking logic
                                containers.add(new ContainerInfo(name, "RUNNING", "", subnet));
                            }
                        }
                    }
                }
                
            } catch (Exception e) {
                throw new NetworkException("Failed to list containers", e);
            }
            
            return containers;
        }
        
        @Override
        public void moveToSubnet(String containerName, String targetSubnet, String newIP) 
                throws NetworkException {
            // Remove old network configuration
            try {
                sshClient.executeCommand("lxc config device remove " + containerName + " eth1");
            } catch (Exception e) {
                // Ignore if device doesn't exist
            }
            
            // Configure with new subnet
            ContainerNetworkConfig config = new ContainerNetworkConfig();
            config.setContainerName(containerName);
            config.setSubnet(targetSubnet);
            config.setIpAddress(newIP);
            configureContainer(containerName, config);
        }
        
        @Override
        public void applyNetworkPolicy(String containerName, NetworkPolicy policy) 
                throws NetworkException {
            // Implement network policy application
            throw new NetworkException("Network policy not yet implemented");
        }
        
        @Override
        public String getNextAvailableIP(String subnet) throws NetworkException {
            try {
                return subnetManager.allocateIP(subnet);
            } catch (Exception e) {
                throw new NetworkException("Failed to get available IP", e);
            }
        }
        
        @Override
        public void reserveIP(String subnet, String ipAddress, String purpose) 
                throws NetworkException {
            try {
                subnetManager.allocateSpecificIP(subnet, ipAddress);
            } catch (Exception e) {
                throw new NetworkException("Failed to reserve IP", e);
            }
        }
        
        @Override
        public void releaseIP(String subnet, String ipAddress) throws NetworkException {
            subnetManager.releaseIP(subnet, ipAddress);
        }
    }
    
    /**
     * Orchestrator Exception
     */
    public static class OrchestratorException extends Exception {
        public OrchestratorException(String message) { super(message); }
        public OrchestratorException(String message, Throwable cause) { super(message, cause); }
    }
}