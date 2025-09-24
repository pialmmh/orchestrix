package com.telcobright.orchestrix.model;

/**
 * Network configuration - most reusable, goes at orchestrix level
 * Used by both automation and images
 */
public class NetworkConfig {
    private String ipAddress;
    private String subnet;
    private String gateway;
    private String bridge;
    private String[] dnsServers;
    private Integer vlanId;

    // Constructors
    public NetworkConfig() {}

    public NetworkConfig(String ipAddress, String subnet, String gateway) {
        this.ipAddress = ipAddress;
        this.subnet = subnet;
        this.gateway = gateway;
        this.dnsServers = new String[]{"8.8.8.8", "8.8.4.4"};
    }

    // Validation methods
    public boolean isValid() {
        return ipAddress != null && !ipAddress.isEmpty() &&
               subnet != null && !subnet.isEmpty() &&
               gateway != null && !gateway.isEmpty();
    }

    public boolean isPrivateNetwork() {
        return ipAddress.startsWith("10.") ||
               ipAddress.startsWith("192.168.") ||
               ipAddress.startsWith("172.");
    }

    // Getters and Setters
    public String getIpAddress() { return ipAddress; }
    public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }

    public String getSubnet() { return subnet; }
    public void setSubnet(String subnet) { this.subnet = subnet; }

    public String getGateway() { return gateway; }
    public void setGateway(String gateway) { this.gateway = gateway; }

    public String getBridge() { return bridge; }
    public void setBridge(String bridge) { this.bridge = bridge; }

    public String[] getDnsServers() { return dnsServers; }
    public void setDnsServers(String[] dnsServers) { this.dnsServers = dnsServers; }

    public Integer getVlanId() { return vlanId; }
    public void setVlanId(Integer vlanId) { this.vlanId = vlanId; }

    @Override
    public String toString() {
        return String.format("NetworkConfig[ip=%s, subnet=%s, gateway=%s, bridge=%s]",
            ipAddress, subnet, gateway, bridge);
    }
}