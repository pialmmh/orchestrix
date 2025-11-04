package com.telcobright.orchestrix.automation.consul;

/**
 * Configuration for Consul cluster deployment
 */
public class ConsulClusterConfig {

    private int nodeCount = 3;              // Number of Consul nodes
    private String containerPrefix = "consul-node-"; // Container name prefix
    private String datacenter = "dc1";      // Consul datacenter name
    private String consulVersion = "1.17.0"; // Consul version to install
    private int basePort = 8300;            // Base port for Consul
    private int httpPort = 8500;            // HTTP API port

    public ConsulClusterConfig() {
    }

    public ConsulClusterConfig(int nodeCount) {
        this.nodeCount = nodeCount;
    }

    // Getters and setters
    public int getNodeCount() {
        return nodeCount;
    }

    public void setNodeCount(int nodeCount) {
        this.nodeCount = nodeCount;
    }

    public String getContainerPrefix() {
        return containerPrefix;
    }

    public void setContainerPrefix(String containerPrefix) {
        this.containerPrefix = containerPrefix;
    }

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

    public int getBasePort() {
        return basePort;
    }

    public void setBasePort(int basePort) {
        this.basePort = basePort;
    }

    public int getHttpPort() {
        return httpPort;
    }

    public void setHttpPort(int httpPort) {
        this.httpPort = httpPort;
    }
}