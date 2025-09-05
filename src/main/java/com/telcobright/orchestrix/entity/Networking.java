package com.telcobright.orchestrix.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "networking")
public class Networking {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "network_type")
    private NetworkType networkType; // VLAN, VXLAN, BRIDGE, OVERLAY

    @Column(name = "network_cidr")
    private String networkCidr;

    @Column(name = "vlan_id")
    private Integer vlanId;

    private String gateway;

    @Column(name = "dns_servers")
    private String dnsServers;

    @Column(name = "dhcp_enabled")
    private Boolean dhcpEnabled = false;

    private String status; // ACTIVE, INACTIVE, MAINTENANCE

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cloud_id")
    @JsonBackReference
    private Cloud cloud;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "datacenter_id")
    private Datacenter datacenter;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // Constructors
    public Networking() {}

    public Networking(String name, String description, NetworkType networkType, String networkCidr,
                     Integer vlanId, String gateway, String dnsServers, Boolean dhcpEnabled, String status) {
        this.name = name;
        this.description = description;
        this.networkType = networkType;
        this.networkCidr = networkCidr;
        this.vlanId = vlanId;
        this.gateway = gateway;
        this.dnsServers = dnsServers;
        this.dhcpEnabled = dhcpEnabled;
        this.status = status;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public NetworkType getNetworkType() {
        return networkType;
    }

    public void setNetworkType(NetworkType networkType) {
        this.networkType = networkType;
    }

    public String getNetworkCidr() {
        return networkCidr;
    }

    public void setNetworkCidr(String networkCidr) {
        this.networkCidr = networkCidr;
    }

    public Integer getVlanId() {
        return vlanId;
    }

    public void setVlanId(Integer vlanId) {
        this.vlanId = vlanId;
    }

    public String getGateway() {
        return gateway;
    }

    public void setGateway(String gateway) {
        this.gateway = gateway;
    }

    public String getDnsServers() {
        return dnsServers;
    }

    public void setDnsServers(String dnsServers) {
        this.dnsServers = dnsServers;
    }

    public Boolean getDhcpEnabled() {
        return dhcpEnabled;
    }

    public void setDhcpEnabled(Boolean dhcpEnabled) {
        this.dhcpEnabled = dhcpEnabled;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Cloud getCloud() {
        return cloud;
    }

    public void setCloud(Cloud cloud) {
        this.cloud = cloud;
    }

    public Datacenter getDatacenter() {
        return datacenter;
    }

    public void setDatacenter(Datacenter datacenter) {
        this.datacenter = datacenter;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public enum NetworkType {
        VLAN,
        VXLAN,
        BRIDGE,
        OVERLAY
    }
}