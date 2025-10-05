package com.telcobright.orchestrix.automation.infrastructure.consul.entity;

/**
 * Consul node configuration
 */
public class ConsulNode {
    private String name;
    private String ipAddress;
    private ConsulNodeRole role;
    private int port = 8500;

    public ConsulNode() {
    }

    public ConsulNode(String name, String ipAddress, ConsulNodeRole role) {
        this.name = name;
        this.ipAddress = ipAddress;
        this.role = role;
    }

    // Getters and Setters
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public ConsulNodeRole getRole() {
        return role;
    }

    public void setRole(ConsulNodeRole role) {
        this.role = role;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    @Override
    public String toString() {
        return "ConsulNode{" +
                "name='" + name + '\'' +
                ", ipAddress='" + ipAddress + '\'' +
                ", role=" + role +
                ", port=" + port +
                '}';
    }
}
