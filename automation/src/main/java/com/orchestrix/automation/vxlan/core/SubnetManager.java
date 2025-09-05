package com.orchestrix.automation.vxlan.core;

import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Manages multiple VXLAN subnets and IP address allocation
 */
public class SubnetManager {
    
    private final Map<String, Subnet> subnets = new ConcurrentHashMap<>();
    private final Map<String, Set<String>> ipAllocations = new ConcurrentHashMap<>();
    private final Properties config;
    private String defaultSubnet = "development";
    
    public SubnetManager() {
        this.config = loadConfiguration();
        initializeSubnets();
    }
    
    public SubnetManager(String configPath) {
        this.config = loadConfiguration(configPath);
        initializeSubnets();
    }
    
    /**
     * Load configuration
     */
    private Properties loadConfiguration() {
        return loadConfiguration("/home/mustafa/telcobright-projects/orchestrix/network/vxlan-lxc.conf");
    }
    
    private Properties loadConfiguration(String configPath) {
        Properties props = new Properties();
        try {
            List<String> lines = Files.readAllLines(Paths.get(configPath));
            for (String line : lines) {
                line = line.trim();
                if (!line.startsWith("#") && line.contains("=")) {
                    String[] parts = line.split("=", 2);
                    String key = parts[0].trim();
                    String value = parts[1].trim();
                    
                    // Remove quotes if present
                    if (value.startsWith("\"") && value.endsWith("\"")) {
                        value = value.substring(1, value.length() - 1);
                    }
                    
                    // Handle array values
                    if (value.startsWith("(") && value.endsWith(")")) {
                        value = value.substring(1, value.length() - 1);
                    }
                    
                    props.setProperty(key, value);
                }
            }
            
            // Set default subnet
            if (props.containsKey("DEFAULT_SUBNET")) {
                this.defaultSubnet = props.getProperty("DEFAULT_SUBNET");
            }
            
        } catch (IOException e) {
            System.err.println("Warning: Could not load config from " + configPath);
        }
        return props;
    }
    
    /**
     * Initialize subnets from configuration
     */
    private void initializeSubnets() {
        String subnetsConfig = config.getProperty("SUBNETS", "");
        if (!subnetsConfig.isEmpty()) {
            String[] subnetEntries = subnetsConfig.split("\\s+");
            for (String entry : subnetEntries) {
                entry = entry.trim().replace("\"", "");
                if (entry.isEmpty()) continue;
                
                String[] parts = entry.split(":");
                if (parts.length >= 5) {
                    String name = parts[0];
                    int vxlanId = Integer.parseInt(parts[1]);
                    String cidr = parts[2];
                    String gateway = parts[3];
                    String multicast = parts[4];
                    String description = parts.length > 5 ? parts[5] : "";
                    
                    Subnet subnet = new Subnet(name, vxlanId, cidr, gateway, multicast);
                    subnet.description = description;
                    subnets.put(name, subnet);
                    ipAllocations.put(name, ConcurrentHashMap.newKeySet());
                }
            }
        }
        
        // Load existing IP assignments
        loadIPAssignments();
    }
    
    /**
     * Load IP assignments from configuration
     */
    private void loadIPAssignments() {
        String containerIPs = config.getProperty("CONTAINER_IPS", "");
        if (!containerIPs.isEmpty()) {
            String[] entries = containerIPs.split("\\s+");
            for (String entry : entries) {
                entry = entry.trim().replace("\"", "");
                if (entry.isEmpty()) continue;
                
                String[] parts = entry.split(":");
                if (parts.length >= 3) {
                    String subnet = parts[1];
                    String ip = parts[2];
                    
                    if (!ip.isEmpty() && ipAllocations.containsKey(subnet)) {
                        ipAllocations.get(subnet).add(ip);
                    }
                }
            }
        }
    }
    
    /**
     * Get subnet by name
     */
    public Subnet getSubnet(String name) {
        return subnets.get(name);
    }
    
    /**
     * Get all subnets
     */
    public Collection<Subnet> getAllSubnets() {
        return subnets.values();
    }
    
    /**
     * Get default subnet
     */
    public Subnet getDefaultSubnet() {
        return subnets.get(defaultSubnet);
    }
    
    /**
     * Allocate next available IP in subnet
     */
    public String allocateIP(String subnetName) throws SubnetException {
        Subnet subnet = subnets.get(subnetName);
        if (subnet == null) {
            throw new SubnetException("Subnet not found: " + subnetName);
        }
        
        Set<String> allocated = ipAllocations.get(subnetName);
        
        // Parse DHCP pool range if configured
        String poolRange = getPoolRange(subnetName);
        int startIP = 100, endIP = 200;
        
        if (poolRange != null) {
            String[] parts = poolRange.split(":");
            if (parts.length == 2) {
                startIP = getLastOctet(parts[0]);
                endIP = getLastOctet(parts[1]);
            }
        }
        
        // Find next available IP
        String baseIP = subnet.getBaseIP();
        for (int i = startIP; i <= endIP; i++) {
            String candidateIP = buildIP(baseIP, i);
            if (!allocated.contains(candidateIP) && !candidateIP.equals(subnet.gateway)) {
                allocated.add(candidateIP);
                return candidateIP;
            }
        }
        
        throw new SubnetException("No available IPs in subnet " + subnetName);
    }
    
    /**
     * Allocate specific IP
     */
    public void allocateSpecificIP(String subnetName, String ipAddress) throws SubnetException {
        Subnet subnet = subnets.get(subnetName);
        if (subnet == null) {
            throw new SubnetException("Subnet not found: " + subnetName);
        }
        
        if (!isIPInSubnet(ipAddress, subnet.cidr)) {
            throw new SubnetException("IP " + ipAddress + " not in subnet " + subnet.cidr);
        }
        
        Set<String> allocated = ipAllocations.get(subnetName);
        if (allocated.contains(ipAddress)) {
            throw new SubnetException("IP " + ipAddress + " already allocated");
        }
        
        allocated.add(ipAddress);
    }
    
    /**
     * Release IP address
     */
    public void releaseIP(String subnetName, String ipAddress) {
        Set<String> allocated = ipAllocations.get(subnetName);
        if (allocated != null) {
            allocated.remove(ipAddress);
        }
    }
    
    /**
     * Check if IP is available
     */
    public boolean isIPAvailable(String subnetName, String ipAddress) {
        Set<String> allocated = ipAllocations.get(subnetName);
        return allocated != null && !allocated.contains(ipAddress);
    }
    
    /**
     * Get all allocated IPs in subnet
     */
    public Set<String> getAllocatedIPs(String subnetName) {
        return new HashSet<>(ipAllocations.getOrDefault(subnetName, Collections.emptySet()));
    }
    
    /**
     * Check if IP is in subnet
     */
    private boolean isIPInSubnet(String ip, String cidr) {
        try {
            String[] parts = cidr.split("/");
            InetAddress targetAddr = InetAddress.getByName(ip);
            InetAddress subnetAddr = InetAddress.getByName(parts[0]);
            int prefixLength = Integer.parseInt(parts[1]);
            
            byte[] targetBytes = targetAddr.getAddress();
            byte[] subnetBytes = subnetAddr.getAddress();
            
            int bytesToCheck = prefixLength / 8;
            int bitsToCheck = prefixLength % 8;
            
            for (int i = 0; i < bytesToCheck; i++) {
                if (targetBytes[i] != subnetBytes[i]) {
                    return false;
                }
            }
            
            if (bitsToCheck > 0 && bytesToCheck < targetBytes.length) {
                int mask = (0xFF << (8 - bitsToCheck)) & 0xFF;
                return (targetBytes[bytesToCheck] & mask) == (subnetBytes[bytesToCheck] & mask);
            }
            
            return true;
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Get pool range for subnet
     */
    private String getPoolRange(String subnetName) {
        String pools = config.getProperty("DHCP_POOLS", "");
        Pattern pattern = Pattern.compile(subnetName + ":([^:]+):([^\\s]+)");
        Matcher matcher = pattern.matcher(pools);
        if (matcher.find()) {
            return matcher.group(1) + ":" + matcher.group(2);
        }
        return null;
    }
    
    /**
     * Get last octet from IP
     */
    private int getLastOctet(String ip) {
        String[] parts = ip.split("\\.");
        return Integer.parseInt(parts[3]);
    }
    
    /**
     * Build IP with new last octet
     */
    private String buildIP(String baseIP, int lastOctet) {
        String[] parts = baseIP.split("\\.");
        return String.format("%s.%s.%s.%d", parts[0], parts[1], parts[2], lastOctet);
    }
    
    /**
     * Create routing rules between subnets
     */
    public List<RoutingRule> getRoutingRules() {
        List<RoutingRule> rules = new ArrayList<>();
        String routingConfig = config.getProperty("ROUTING_RULES", "");
        
        if (!routingConfig.isEmpty()) {
            String[] entries = routingConfig.split("\\s+");
            for (String entry : entries) {
                entry = entry.trim().replace("\"", "");
                if (entry.isEmpty()) continue;
                
                String[] parts = entry.split(":");
                if (parts.length == 3) {
                    rules.add(new RoutingRule(parts[0], parts[1], parts[2].equals("ALLOW")));
                }
            }
        }
        
        return rules;
    }
    
    /**
     * Subnet class
     */
    public static class Subnet {
        private final String name;
        private final int vxlanId;
        private final String cidr;
        private final String gateway;
        private final String multicastGroup;
        private String description;
        private String bridgeName;
        private String vxlanInterface;
        
        public Subnet(String name, int vxlanId, String cidr, String gateway, String multicastGroup) {
            this.name = name;
            this.vxlanId = vxlanId;
            this.cidr = cidr;
            this.gateway = gateway;
            this.multicastGroup = multicastGroup;
            this.bridgeName = "br-vx" + vxlanId;
            this.vxlanInterface = "vxlan" + vxlanId;
        }
        
        public String getName() { return name; }
        public int getVxlanId() { return vxlanId; }
        public String getCidr() { return cidr; }
        public String getGateway() { return gateway; }
        public String getMulticastGroup() { return multicastGroup; }
        public String getDescription() { return description; }
        public String getBridgeName() { return bridgeName; }
        public String getVxlanInterface() { return vxlanInterface; }
        
        public String getBaseIP() {
            return cidr.split("/")[0];
        }
        
        public int getPrefixLength() {
            return Integer.parseInt(cidr.split("/")[1]);
        }
        
        public String getNetmask() {
            int prefix = getPrefixLength();
            int mask = 0xFFFFFFFF << (32 - prefix);
            return String.format("%d.%d.%d.%d",
                (mask >> 24) & 0xFF,
                (mask >> 16) & 0xFF,
                (mask >> 8) & 0xFF,
                mask & 0xFF
            );
        }
    }
    
    /**
     * Routing Rule
     */
    public static class RoutingRule {
        private final String sourceSubnet;
        private final String destSubnet;
        private final boolean allowed;
        
        public RoutingRule(String sourceSubnet, String destSubnet, boolean allowed) {
            this.sourceSubnet = sourceSubnet;
            this.destSubnet = destSubnet;
            this.allowed = allowed;
        }
        
        public String getSourceSubnet() { return sourceSubnet; }
        public String getDestSubnet() { return destSubnet; }
        public boolean isAllowed() { return allowed; }
    }
    
    /**
     * Subnet Exception
     */
    public static class SubnetException extends Exception {
        public SubnetException(String message) { super(message); }
        public SubnetException(String message, Throwable cause) { super(message, cause); }
    }
}