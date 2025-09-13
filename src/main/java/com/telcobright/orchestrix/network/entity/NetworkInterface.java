package com.telcobright.orchestrix.network.entity;

import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * Common network interface entity that can be used across different container technologies
 * (LXC, Docker, Kubernetes)
 */
@Entity
@Table(name = "network_interfaces")
@Inheritance(strategy = InheritanceType.JOINED)
public class NetworkInterface {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "interface_name", nullable = false)
    private String interfaceName; // e.g., eth0, eth1, docker0, lxcbr0
    
    @Column(name = "interface_type")
    @Enumerated(EnumType.STRING)
    private InterfaceType type; // BRIDGE, VETH, PHYSICAL, VIRTUAL, MACVLAN, VXLAN
    
    @Column(name = "ip_address")
    private String ipAddress; // e.g., 10.10.199.100
    
    @Column(name = "netmask")
    private Integer netmask; // e.g., 24 for /24
    
    @Column(name = "gateway")
    private String gateway; // e.g., 10.10.199.1
    
    @Column(name = "mac_address")
    private String macAddress;
    
    @Column(name = "mtu")
    private Integer mtu; // Maximum Transmission Unit
    
    @Column(name = "state")
    @Enumerated(EnumType.STRING)
    private InterfaceState state; // UP, DOWN, UNKNOWN
    
    @Column(name = "host_machine")
    private String hostMachine; // Which host this interface belongs to
    
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
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public String getInterfaceName() {
        return interfaceName;
    }
    
    public void setInterfaceName(String interfaceName) {
        this.interfaceName = interfaceName;
    }
    
    public InterfaceType getType() {
        return type;
    }
    
    public void setType(InterfaceType type) {
        this.type = type;
    }
    
    public String getIpAddress() {
        return ipAddress;
    }
    
    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }
    
    public Integer getNetmask() {
        return netmask;
    }
    
    public void setNetmask(Integer netmask) {
        this.netmask = netmask;
    }
    
    public String getGateway() {
        return gateway;
    }
    
    public void setGateway(String gateway) {
        this.gateway = gateway;
    }
    
    public String getMacAddress() {
        return macAddress;
    }
    
    public void setMacAddress(String macAddress) {
        this.macAddress = macAddress;
    }
    
    public Integer getMtu() {
        return mtu;
    }
    
    public void setMtu(Integer mtu) {
        this.mtu = mtu;
    }
    
    public InterfaceState getState() {
        return state;
    }
    
    public void setState(InterfaceState state) {
        this.state = state;
    }
    
    public String getHostMachine() {
        return hostMachine;
    }
    
    public void setHostMachine(String hostMachine) {
        this.hostMachine = hostMachine;
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
    
    // Enum for Interface Types
    public enum InterfaceType {
        BRIDGE,
        VETH,
        PHYSICAL,
        VIRTUAL,
        MACVLAN,
        VXLAN,
        TAP,
        TUN
    }
    
    // Enum for Interface States
    public enum InterfaceState {
        UP,
        DOWN,
        UNKNOWN
    }
}