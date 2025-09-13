package com.telcobright.orchestrix.network.entity;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Bridge entity for Linux bridge configuration
 * Common for LXC, Docker, and KVM networking
 */
@Entity
@Table(name = "bridges")
public class Bridge extends NetworkInterface {
    
    @Column(name = "stp_enabled")
    private Boolean stpEnabled = false; // Spanning Tree Protocol
    
    @Column(name = "forward_delay")
    private Integer forwardDelay = 0; // in seconds
    
    @Column(name = "bridge_priority")
    private Integer priority = 32768; // Default bridge priority
    
    @Column(name = "ageing_time")
    private Integer ageingTime = 300; // MAC address ageing time in seconds
    
    @Column(name = "ipv4_nat_enabled")
    private Boolean ipv4NatEnabled = false;
    
    @Column(name = "ipv6_nat_enabled")
    private Boolean ipv6NatEnabled = false;
    
    @Column(name = "dhcp_enabled")
    private Boolean dhcpEnabled = false;
    
    @Column(name = "dhcp_range_start")
    private String dhcpRangeStart;
    
    @Column(name = "dhcp_range_end")
    private String dhcpRangeEnd;
    
    @Column(name = "dns_servers")
    private String dnsServers; // Comma-separated DNS servers
    
    @Column(name = "ip_forward_enabled")
    private Boolean ipForwardEnabled = true;
    
    @Column(name = "masquerade_enabled")
    private Boolean masqueradeEnabled = false;
    
    @Column(name = "external_interface")
    private String externalInterface; // Interface for NAT (e.g., wlo1, eth0)
    
    @OneToMany(mappedBy = "bridge", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<BridgeAttachment> attachments = new ArrayList<>();
    
    // Constructor
    public Bridge() {
        this.setType(InterfaceType.BRIDGE);
    }
    
    // Helper method to get CIDR notation
    public String getCidr() {
        if (getIpAddress() != null && getNetmask() != null) {
            return getIpAddress() + "/" + getNetmask();
        }
        return null;
    }
    
    // Helper method to get network address
    public String getNetworkAddress() {
        if (getIpAddress() != null && getNetmask() != null) {
            String[] parts = getIpAddress().split("\\.");
            if (parts.length == 4) {
                // Simple calculation for /24 network
                if (getNetmask() == 24) {
                    return parts[0] + "." + parts[1] + "." + parts[2] + ".0/24";
                }
            }
        }
        return null;
    }
    
    // Getters and Setters
    public Boolean getStpEnabled() {
        return stpEnabled;
    }
    
    public void setStpEnabled(Boolean stpEnabled) {
        this.stpEnabled = stpEnabled;
    }
    
    public Integer getForwardDelay() {
        return forwardDelay;
    }
    
    public void setForwardDelay(Integer forwardDelay) {
        this.forwardDelay = forwardDelay;
    }
    
    public Integer getPriority() {
        return priority;
    }
    
    public void setPriority(Integer priority) {
        this.priority = priority;
    }
    
    public Integer getAgeingTime() {
        return ageingTime;
    }
    
    public void setAgeingTime(Integer ageingTime) {
        this.ageingTime = ageingTime;
    }
    
    public Boolean getIpv4NatEnabled() {
        return ipv4NatEnabled;
    }
    
    public void setIpv4NatEnabled(Boolean ipv4NatEnabled) {
        this.ipv4NatEnabled = ipv4NatEnabled;
    }
    
    public Boolean getIpv6NatEnabled() {
        return ipv6NatEnabled;
    }
    
    public void setIpv6NatEnabled(Boolean ipv6NatEnabled) {
        this.ipv6NatEnabled = ipv6NatEnabled;
    }
    
    public Boolean getDhcpEnabled() {
        return dhcpEnabled;
    }
    
    public void setDhcpEnabled(Boolean dhcpEnabled) {
        this.dhcpEnabled = dhcpEnabled;
    }
    
    public String getDhcpRangeStart() {
        return dhcpRangeStart;
    }
    
    public void setDhcpRangeStart(String dhcpRangeStart) {
        this.dhcpRangeStart = dhcpRangeStart;
    }
    
    public String getDhcpRangeEnd() {
        return dhcpRangeEnd;
    }
    
    public void setDhcpRangeEnd(String dhcpRangeEnd) {
        this.dhcpRangeEnd = dhcpRangeEnd;
    }
    
    public String getDnsServers() {
        return dnsServers;
    }
    
    public void setDnsServers(String dnsServers) {
        this.dnsServers = dnsServers;
    }
    
    public Boolean getIpForwardEnabled() {
        return ipForwardEnabled;
    }
    
    public void setIpForwardEnabled(Boolean ipForwardEnabled) {
        this.ipForwardEnabled = ipForwardEnabled;
    }
    
    public Boolean getMasqueradeEnabled() {
        return masqueradeEnabled;
    }
    
    public void setMasqueradeEnabled(Boolean masqueradeEnabled) {
        this.masqueradeEnabled = masqueradeEnabled;
    }
    
    public String getExternalInterface() {
        return externalInterface;
    }
    
    public void setExternalInterface(String externalInterface) {
        this.externalInterface = externalInterface;
    }
    
    public List<BridgeAttachment> getAttachments() {
        return attachments;
    }
    
    public void setAttachments(List<BridgeAttachment> attachments) {
        this.attachments = attachments;
    }
}