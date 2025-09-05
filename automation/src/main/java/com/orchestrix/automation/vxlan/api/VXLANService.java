package com.orchestrix.automation.vxlan.api;

import java.util.List;
import java.util.Map;

/**
 * VXLAN Service interface for managing VXLAN networks
 */
public interface VXLANService {
    
    /**
     * Initialize VXLAN service on a host
     */
    void initializeHost(String host, VXLANConfig config) throws VXLANException;
    
    /**
     * Create a VXLAN interface
     */
    void createVXLAN(String host, String interfaceName, int vxlanId, String multicastGroup) throws VXLANException;
    
    /**
     * Delete a VXLAN interface
     */
    void deleteVXLAN(String host, String interfaceName) throws VXLANException;
    
    /**
     * Create a bridge and attach VXLAN
     */
    void createBridge(String host, String bridgeName, String vxlanInterface) throws VXLANException;
    
    /**
     * Configure IP address on interface
     */
    void configureIP(String host, String interfaceName, String ipAddress, int prefixLength) throws VXLANException;
    
    /**
     * Add static ARP entry for VTEP
     */
    void addVTEP(String host, String remoteIP, String remoteMac) throws VXLANException;
    
    /**
     * Remove VTEP entry
     */
    void removeVTEP(String host, String remoteIP) throws VXLANException;
    
    /**
     * Get VXLAN status
     */
    VXLANStatus getStatus(String host, String interfaceName) throws VXLANException;
    
    /**
     * Get all VXLAN interfaces on host
     */
    List<VXLANInterface> listVXLANs(String host) throws VXLANException;
    
    /**
     * Setup VXLAN mesh network
     */
    void setupMesh(List<String> hosts, VXLANConfig config) throws VXLANException;
    
    /**
     * Teardown VXLAN mesh network
     */
    void teardownMesh(List<String> hosts, String vxlanName) throws VXLANException;
    
    /**
     * Health check for VXLAN
     */
    HealthStatus healthCheck(String host, String interfaceName) throws VXLANException;
    
    /**
     * Backup VXLAN configuration
     */
    String backupConfiguration(String host) throws VXLANException;
    
    /**
     * Restore VXLAN configuration
     */
    void restoreConfiguration(String host, String backupData) throws VXLANException;
    
    /**
     * VXLAN Configuration
     */
    class VXLANConfig {
        private int vxlanId = 100;
        private String interfaceName = "vxlan0";
        private String multicastGroup = "239.1.1.1";
        private String physicalInterface = "eth0";
        private String subnet = "10.200.0.0/16";
        private String bridgeName = "br-vxlan";
        private int mtu = 1450;
        private int port = 4789;
        private boolean enableEncryption = false;
        private Map<String, String> customOptions;
        
        // Getters and setters
        public int getVxlanId() { return vxlanId; }
        public void setVxlanId(int vxlanId) { this.vxlanId = vxlanId; }
        
        public String getInterfaceName() { return interfaceName; }
        public void setInterfaceName(String interfaceName) { this.interfaceName = interfaceName; }
        
        public String getMulticastGroup() { return multicastGroup; }
        public void setMulticastGroup(String multicastGroup) { this.multicastGroup = multicastGroup; }
        
        public String getPhysicalInterface() { return physicalInterface; }
        public void setPhysicalInterface(String physicalInterface) { this.physicalInterface = physicalInterface; }
        
        public String getSubnet() { return subnet; }
        public void setSubnet(String subnet) { this.subnet = subnet; }
        
        public String getBridgeName() { return bridgeName; }
        public void setBridgeName(String bridgeName) { this.bridgeName = bridgeName; }
        
        public int getMtu() { return mtu; }
        public void setMtu(int mtu) { this.mtu = mtu; }
        
        public int getPort() { return port; }
        public void setPort(int port) { this.port = port; }
        
        public boolean isEnableEncryption() { return enableEncryption; }
        public void setEnableEncryption(boolean enableEncryption) { this.enableEncryption = enableEncryption; }
        
        public Map<String, String> getCustomOptions() { return customOptions; }
        public void setCustomOptions(Map<String, String> customOptions) { this.customOptions = customOptions; }
    }
    
    /**
     * VXLAN Interface information
     */
    class VXLANInterface {
        private String name;
        private int vxlanId;
        private String state;
        private String ipAddress;
        private String macAddress;
        private String multicastGroup;
        private long rxPackets;
        private long txPackets;
        
        // Constructor
        public VXLANInterface(String name, int vxlanId, String state) {
            this.name = name;
            this.vxlanId = vxlanId;
            this.state = state;
        }
        
        // Getters and setters
        public String getName() { return name; }
        public int getVxlanId() { return vxlanId; }
        public String getState() { return state; }
        public String getIpAddress() { return ipAddress; }
        public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }
        public String getMacAddress() { return macAddress; }
        public void setMacAddress(String macAddress) { this.macAddress = macAddress; }
        public String getMulticastGroup() { return multicastGroup; }
        public void setMulticastGroup(String multicastGroup) { this.multicastGroup = multicastGroup; }
        public long getRxPackets() { return rxPackets; }
        public void setRxPackets(long rxPackets) { this.rxPackets = rxPackets; }
        public long getTxPackets() { return txPackets; }
        public void setTxPackets(long txPackets) { this.txPackets = txPackets; }
    }
    
    /**
     * VXLAN Status
     */
    class VXLANStatus {
        private boolean active;
        private String state;
        private long uptime;
        private Map<String, String> details;
        
        public VXLANStatus(boolean active, String state) {
            this.active = active;
            this.state = state;
        }
        
        public boolean isActive() { return active; }
        public String getState() { return state; }
        public long getUptime() { return uptime; }
        public void setUptime(long uptime) { this.uptime = uptime; }
        public Map<String, String> getDetails() { return details; }
        public void setDetails(Map<String, String> details) { this.details = details; }
    }
    
    /**
     * Health Status
     */
    class HealthStatus {
        private boolean healthy;
        private String status;
        private List<String> issues;
        private Map<String, Object> metrics;
        
        public HealthStatus(boolean healthy, String status) {
            this.healthy = healthy;
            this.status = status;
        }
        
        public boolean isHealthy() { return healthy; }
        public String getStatus() { return status; }
        public List<String> getIssues() { return issues; }
        public void setIssues(List<String> issues) { this.issues = issues; }
        public Map<String, Object> getMetrics() { return metrics; }
        public void setMetrics(Map<String, Object> metrics) { this.metrics = metrics; }
    }
    
    /**
     * VXLAN Exception
     */
    class VXLANException extends Exception {
        public VXLANException(String message) { super(message); }
        public VXLANException(String message, Throwable cause) { super(message, cause); }
    }
}