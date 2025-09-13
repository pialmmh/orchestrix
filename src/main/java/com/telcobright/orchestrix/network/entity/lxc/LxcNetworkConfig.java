package com.telcobright.orchestrix.network.entity.lxc;

import javax.persistence.*;

/**
 * LXC Container Network Configuration
 * Represents the static IP configuration for an LXC container
 */
@Entity
@Table(name = "lxc_network_configs")
public class LxcNetworkConfig {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @OneToOne
    @JoinColumn(name = "container_id", nullable = false)
    private LxcContainer container;
    
    @Column(name = "bridge_name", nullable = false)
    private String bridgeName; // e.g., "lxcbr0"
    
    @Column(name = "ip_address", nullable = false)
    private String ipAddress; // e.g., "10.10.199.100"
    
    @Column(name = "netmask")
    private Integer netmask = 24; // Default /24
    
    @Column(name = "gateway", nullable = false)
    private String gateway; // e.g., "10.10.199.1"
    
    @Column(name = "dns_servers")
    private String dnsServers; // Comma-separated, e.g., "8.8.8.8,8.8.4.4"
    
    @Column(name = "interface_name")
    private String interfaceName = "eth0"; // Interface inside container
    
    @Column(name = "mac_address")
    private String macAddress; // Optional fixed MAC
    
    @Column(name = "mtu")
    private Integer mtu; // Optional MTU size
    
    @Column(name = "vlan_id")
    private Integer vlanId; // Optional VLAN tagging
    
    @Column(name = "ipv6_address")
    private String ipv6Address; // Optional IPv6
    
    @Column(name = "ipv6_gateway")
    private String ipv6Gateway; // Optional IPv6 gateway
    
    // Network configuration method
    @Column(name = "config_method")
    @Enumerated(EnumType.STRING)
    private ConfigMethod configMethod = ConfigMethod.STATIC;
    
    // Helper method to get CIDR notation
    public String getCidr() {
        return ipAddress + "/" + netmask;
    }
    
    // Helper method to generate netplan config
    public String generateNetplanConfig() {
        StringBuilder config = new StringBuilder();
        config.append("network:\n");
        config.append("  version: 2\n");
        config.append("  ethernets:\n");
        config.append("    ").append(interfaceName).append(":\n");
        config.append("      addresses: [").append(getCidr()).append("]\n");
        config.append("      gateway4: ").append(gateway).append("\n");
        if (dnsServers != null && !dnsServers.isEmpty()) {
            config.append("      nameservers:\n");
            config.append("        addresses: [");
            config.append(dnsServers.replace(",", ", "));
            config.append("]\n");
        }
        if (mtu != null) {
            config.append("      mtu: ").append(mtu).append("\n");
        }
        return config.toString();
    }
    
    // Helper method to generate ip commands
    public String generateIpCommands() {
        StringBuilder cmds = new StringBuilder();
        cmds.append("ip addr flush dev ").append(interfaceName).append("\n");
        cmds.append("ip addr add ").append(getCidr()).append(" dev ").append(interfaceName).append("\n");
        cmds.append("ip link set ").append(interfaceName).append(" up\n");
        cmds.append("ip route add default via ").append(gateway).append("\n");
        if (dnsServers != null && !dnsServers.isEmpty()) {
            String[] servers = dnsServers.split(",");
            for (String server : servers) {
                cmds.append("echo 'nameserver ").append(server.trim()).append("' >> /etc/resolv.conf\n");
            }
        }
        return cmds.toString();
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public LxcContainer getContainer() {
        return container;
    }
    
    public void setContainer(LxcContainer container) {
        this.container = container;
    }
    
    public String getBridgeName() {
        return bridgeName;
    }
    
    public void setBridgeName(String bridgeName) {
        this.bridgeName = bridgeName;
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
    
    public String getDnsServers() {
        return dnsServers;
    }
    
    public void setDnsServers(String dnsServers) {
        this.dnsServers = dnsServers;
    }
    
    public String getInterfaceName() {
        return interfaceName;
    }
    
    public void setInterfaceName(String interfaceName) {
        this.interfaceName = interfaceName;
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
    
    public Integer getVlanId() {
        return vlanId;
    }
    
    public void setVlanId(Integer vlanId) {
        this.vlanId = vlanId;
    }
    
    public String getIpv6Address() {
        return ipv6Address;
    }
    
    public void setIpv6Address(String ipv6Address) {
        this.ipv6Address = ipv6Address;
    }
    
    public String getIpv6Gateway() {
        return ipv6Gateway;
    }
    
    public void setIpv6Gateway(String ipv6Gateway) {
        this.ipv6Gateway = ipv6Gateway;
    }
    
    public ConfigMethod getConfigMethod() {
        return configMethod;
    }
    
    public void setConfigMethod(ConfigMethod configMethod) {
        this.configMethod = configMethod;
    }
    
    public enum ConfigMethod {
        STATIC,
        DHCP,
        MANUAL
    }
}