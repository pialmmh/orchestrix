package com.telcobright.orchestrix.automation.infrastructure.consul.entity;

import java.util.ArrayList;
import java.util.List;

/**
 * Minimal Consul cluster configuration
 * No DNS, just IP-based service discovery
 */
public class ConsulClusterConfig {
    private String datacenter = "dc1";
    private String consulVersion = "1.17.0";
    private List<ConsulNode> nodes = new ArrayList<>();
    private boolean enableUI = true;
    private boolean enableDNS = false;  // Minimal setup - no DNS
    private int dnsPort = 8600;
    private String logLevel = "INFO";

    public ConsulClusterConfig() {
    }

    public void addNode(ConsulNode node) {
        this.nodes.add(node);
    }

    public void addNode(String name, String ipAddress, ConsulNodeRole role) {
        this.nodes.add(new ConsulNode(name, ipAddress, role));
    }

    /**
     * Get list of server nodes
     */
    public List<ConsulNode> getServerNodes() {
        List<ConsulNode> servers = new ArrayList<>();
        for (ConsulNode node : nodes) {
            if (node.getRole() == ConsulNodeRole.SERVER) {
                servers.add(node);
            }
        }
        return servers;
    }

    /**
     * Get list of client nodes
     */
    public List<ConsulNode> getClientNodes() {
        List<ConsulNode> clients = new ArrayList<>();
        for (ConsulNode node : nodes) {
            if (node.getRole() == ConsulNodeRole.CLIENT) {
                clients.add(node);
            }
        }
        return clients;
    }

    /**
     * Get server count for bootstrap_expect
     */
    public int getServerCount() {
        return getServerNodes().size();
    }

    /**
     * Get retry_join list (all server IPs)
     */
    public List<String> getRetryJoinList() {
        List<String> retryJoin = new ArrayList<>();
        for (ConsulNode node : getServerNodes()) {
            retryJoin.add(node.getIpAddress());
        }
        return retryJoin;
    }

    // Getters and Setters
    public String getDatacenter() {
        return datacenter;
    }

    public void setDatacenter(String datacenter) {
        this.datacenter = datacenter;
    }

    public String getConsulVersion() {
        return consulVersion;
    }

    public void setConsulVersion(String consulVersion) {
        this.consulVersion = consulVersion;
    }

    public List<ConsulNode> getNodes() {
        return nodes;
    }

    public void setNodes(List<ConsulNode> nodes) {
        this.nodes = nodes;
    }

    public boolean isEnableUI() {
        return enableUI;
    }

    public void setEnableUI(boolean enableUI) {
        this.enableUI = enableUI;
    }

    public boolean isEnableDNS() {
        return enableDNS;
    }

    public void setEnableDNS(boolean enableDNS) {
        this.enableDNS = enableDNS;
    }

    public int getDnsPort() {
        return dnsPort;
    }

    public void setDnsPort(int dnsPort) {
        this.dnsPort = dnsPort;
    }

    public String getLogLevel() {
        return logLevel;
    }

    public void setLogLevel(String logLevel) {
        this.logLevel = logLevel;
    }

    @Override
    public String toString() {
        return "ConsulClusterConfig{" +
                "datacenter='" + datacenter + '\'' +
                ", consulVersion='" + consulVersion + '\'' +
                ", servers=" + getServerCount() +
                ", clients=" + getClientNodes().size() +
                ", enableUI=" + enableUI +
                ", enableDNS=" + enableDNS +
                '}';
    }
}
