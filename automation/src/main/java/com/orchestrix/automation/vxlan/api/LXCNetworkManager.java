package com.orchestrix.automation.vxlan.api;

import java.util.List;
import java.util.Map;

/**
 * Interface for managing LXC container network configuration with VXLAN
 */
public interface LXCNetworkManager {
    
    /**
     * Configure network for an LXC container
     */
    void configureContainer(String containerName, ContainerNetworkConfig config) throws NetworkException;
    
    /**
     * Assign IP address to container
     */
    void assignIP(String containerName, String subnet, String ipAddress) throws NetworkException;
    
    /**
     * Get container's current network configuration
     */
    ContainerNetworkConfig getContainerConfig(String containerName) throws NetworkException;
    
    /**
     * List all containers in a subnet
     */
    List<ContainerInfo> listContainersInSubnet(String subnet) throws NetworkException;
    
    /**
     * Move container to different subnet
     */
    void moveToSubnet(String containerName, String targetSubnet, String newIP) throws NetworkException;
    
    /**
     * Apply network policy to container
     */
    void applyNetworkPolicy(String containerName, NetworkPolicy policy) throws NetworkException;
    
    /**
     * Get next available IP in subnet
     */
    String getNextAvailableIP(String subnet) throws NetworkException;
    
    /**
     * Reserve IP address for future use
     */
    void reserveIP(String subnet, String ipAddress, String purpose) throws NetworkException;
    
    /**
     * Release reserved IP
     */
    void releaseIP(String subnet, String ipAddress) throws NetworkException;
    
    /**
     * Container Network Configuration
     */
    class ContainerNetworkConfig {
        private String containerName;
        private String subnet;
        private String ipAddress;
        private String macAddress;
        private String gateway;
        private String netmask;
        private String vxlanInterface;
        private String bridgeInterface;
        private List<String> dnsServers;
        private Map<String, String> routes;
        private boolean natEnabled;
        private int mtu = 1450;
        
        // Getters and setters
        public String getContainerName() { return containerName; }
        public void setContainerName(String containerName) { this.containerName = containerName; }
        
        public String getSubnet() { return subnet; }
        public void setSubnet(String subnet) { this.subnet = subnet; }
        
        public String getIpAddress() { return ipAddress; }
        public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }
        
        public String getMacAddress() { return macAddress; }
        public void setMacAddress(String macAddress) { this.macAddress = macAddress; }
        
        public String getGateway() { return gateway; }
        public void setGateway(String gateway) { this.gateway = gateway; }
        
        public String getNetmask() { return netmask; }
        public void setNetmask(String netmask) { this.netmask = netmask; }
        
        public String getVxlanInterface() { return vxlanInterface; }
        public void setVxlanInterface(String vxlanInterface) { this.vxlanInterface = vxlanInterface; }
        
        public String getBridgeInterface() { return bridgeInterface; }
        public void setBridgeInterface(String bridgeInterface) { this.bridgeInterface = bridgeInterface; }
        
        public List<String> getDnsServers() { return dnsServers; }
        public void setDnsServers(List<String> dnsServers) { this.dnsServers = dnsServers; }
        
        public Map<String, String> getRoutes() { return routes; }
        public void setRoutes(Map<String, String> routes) { this.routes = routes; }
        
        public boolean isNatEnabled() { return natEnabled; }
        public void setNatEnabled(boolean natEnabled) { this.natEnabled = natEnabled; }
        
        public int getMtu() { return mtu; }
        public void setMtu(int mtu) { this.mtu = mtu; }
    }
    
    /**
     * Container Information
     */
    class ContainerInfo {
        private String name;
        private String state;
        private String ipAddress;
        private String subnet;
        private String macAddress;
        private long rxBytes;
        private long txBytes;
        private long uptime;
        
        public ContainerInfo(String name, String state, String ipAddress, String subnet) {
            this.name = name;
            this.state = state;
            this.ipAddress = ipAddress;
            this.subnet = subnet;
        }
        
        // Getters and setters
        public String getName() { return name; }
        public String getState() { return state; }
        public String getIpAddress() { return ipAddress; }
        public String getSubnet() { return subnet; }
        public String getMacAddress() { return macAddress; }
        public void setMacAddress(String macAddress) { this.macAddress = macAddress; }
        public long getRxBytes() { return rxBytes; }
        public void setRxBytes(long rxBytes) { this.rxBytes = rxBytes; }
        public long getTxBytes() { return txBytes; }
        public void setTxBytes(long txBytes) { this.txBytes = txBytes; }
        public long getUptime() { return uptime; }
        public void setUptime(long uptime) { this.uptime = uptime; }
    }
    
    /**
     * Network Policy
     */
    class NetworkPolicy {
        private String name;
        private PolicyType type;
        private List<String> allowedSubnets;
        private List<String> deniedSubnets;
        private List<String> allowedPorts;
        private List<String> deniedPorts;
        private boolean allowInternet;
        private int bandwidthLimit; // in Mbps, 0 = unlimited
        
        public enum PolicyType {
            ALLOW_ALL,
            DENY_ALL,
            RESTRICTED,
            CUSTOM
        }
        
        // Getters and setters
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        
        public PolicyType getType() { return type; }
        public void setType(PolicyType type) { this.type = type; }
        
        public List<String> getAllowedSubnets() { return allowedSubnets; }
        public void setAllowedSubnets(List<String> allowedSubnets) { this.allowedSubnets = allowedSubnets; }
        
        public List<String> getDeniedSubnets() { return deniedSubnets; }
        public void setDeniedSubnets(List<String> deniedSubnets) { this.deniedSubnets = deniedSubnets; }
        
        public List<String> getAllowedPorts() { return allowedPorts; }
        public void setAllowedPorts(List<String> allowedPorts) { this.allowedPorts = allowedPorts; }
        
        public List<String> getDeniedPorts() { return deniedPorts; }
        public void setDeniedPorts(List<String> deniedPorts) { this.deniedPorts = deniedPorts; }
        
        public boolean isAllowInternet() { return allowInternet; }
        public void setAllowInternet(boolean allowInternet) { this.allowInternet = allowInternet; }
        
        public int getBandwidthLimit() { return bandwidthLimit; }
        public void setBandwidthLimit(int bandwidthLimit) { this.bandwidthLimit = bandwidthLimit; }
    }
    
    /**
     * Network Exception
     */
    class NetworkException extends Exception {
        public NetworkException(String message) { super(message); }
        public NetworkException(String message, Throwable cause) { super(message, cause); }
    }
}