package com.telcobright.orchestrix.automation.config;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Overlay Network Configuration
 *
 * Extends CommonConfig with overlay-specific parameters for WireGuard + FRR deployment.
 *
 * Loads configuration from deployments/{tenant}/overlay-config.conf
 * Falls back to common.conf for shared parameters
 *
 * Usage:
 * <pre>
 * OverlayConfig config = new OverlayConfig("deployments/ks_network");
 * List&lt;NodeConfig&gt; nodes = config.getNodes();
 * </pre>
 *
 * @author TelcoBright Orchestrix Team
 * @since 2025-11-27
 */
public class OverlayConfig extends CommonConfig {

    /**
     * Load overlay configuration from deployment directory
     */
    public OverlayConfig(String deploymentDir) throws IOException {
        super();

        // Try overlay-config.conf first, fall back to common.conf
        String overlayConfigPath = deploymentDir + "/overlay-config.conf";
        String commonConfigPath = deploymentDir + "/common.conf";

        try {
            parseConfig(overlayConfigPath);
        } catch (IOException e) {
            // Fall back to common.conf
            parseConfig(commonConfigPath);
        }

        validate();
    }

    private void validate() {
        requireProperty("TENANT_NAME", "Tenant name is required");
        requireProperty("NODE_COUNT", "Node count is required");

        int nodeCount = getNodeCount();
        for (int i = 1; i <= nodeCount; i++) {
            requireProperty("NODE" + i + "_MGMT_IP", "Node " + i + " management IP is required");
            requireProperty("NODE" + i + "_SSH_USER", "Node " + i + " SSH user is required");
            requireProperty("NODE" + i + "_SSH_PASS", "Node " + i + " SSH password is required");
        }
    }

    private void requireProperty(String key, String message) {
        if (!hasProperty(key)) {
            throw new IllegalArgumentException(message + " (missing: " + key + ")");
        }
    }

    // ============================================================
    // Node Configuration
    // ============================================================

    public int getNodeCount() {
        return Integer.parseInt(getProperty("NODE_COUNT", "3"));
    }

    /**
     * Get node configuration by index (1-based)
     */
    public NodeConfig getNode(int nodeNumber) {
        NodeConfig node = new NodeConfig();
        node.setNodeId(nodeNumber);
        node.setMgmtIp(getProperty("NODE" + nodeNumber + "_MGMT_IP"));
        node.setSshUser(getProperty("NODE" + nodeNumber + "_SSH_USER"));
        node.setSshPassword(getProperty("NODE" + nodeNumber + "_SSH_PASS"));
        node.setSshPort(Integer.parseInt(getProperty("NODE" + nodeNumber + "_SSH_PORT",
            getProperty("SSH_PORT", "22"))));
        node.setHostname(getProperty("NODE" + nodeNumber + "_HOSTNAME", "node" + nodeNumber));
        node.setContainerSubnet(getProperty("NODE" + nodeNumber + "_CONTAINER_SUBNET"));
        node.setOverlayIp(getProperty("NODE" + nodeNumber + "_OVERLAY_IP"));
        node.setBgpAs(getProperty("NODE" + nodeNumber + "_BGP_AS"));
        return node;
    }

    /**
     * Get all node configurations
     */
    public List<NodeConfig> getNodes() {
        List<NodeConfig> nodes = new ArrayList<>();
        int nodeCount = getNodeCount();
        for (int i = 1; i <= nodeCount; i++) {
            nodes.add(getNode(i));
        }
        return nodes;
    }

    // ============================================================
    // Overlay Configuration
    // ============================================================

    @Override
    public String getOverlayNetwork() {
        return getProperty("OVERLAY_NETWORK", "10.9.9.0/24");
    }

    @Override
    public int getOverlayPort() {
        return Integer.parseInt(getProperty("OVERLAY_PORT", "51820"));
    }

    public String getVpnClientRange() {
        return getProperty("VPN_CLIENT_RANGE", "10.9.9.100/32");
    }

    public boolean isVpnGatewayEnabled() {
        return Boolean.parseBoolean(getProperty("VPN_GATEWAY_ENABLED", "false"));
    }

    public int getVpnGatewayNode() {
        return Integer.parseInt(getProperty("VPN_GATEWAY_NODE", "1"));
    }

    // ============================================================
    // Node Configuration Class
    // ============================================================

    public static class NodeConfig {
        private int nodeId;
        private String mgmtIp;
        private String sshUser;
        private String sshPassword;
        private int sshPort;
        private String hostname;
        private String containerSubnet;
        private String overlayIp;
        private String bgpAs;

        // Getters and Setters
        public int getNodeId() { return nodeId; }
        public void setNodeId(int nodeId) { this.nodeId = nodeId; }

        public String getMgmtIp() { return mgmtIp; }
        public void setMgmtIp(String mgmtIp) { this.mgmtIp = mgmtIp; }

        public String getSshUser() { return sshUser; }
        public void setSshUser(String sshUser) { this.sshUser = sshUser; }

        public String getSshPassword() { return sshPassword; }
        public void setSshPassword(String sshPassword) { this.sshPassword = sshPassword; }

        public int getSshPort() { return sshPort; }
        public void setSshPort(int sshPort) { this.sshPort = sshPort; }

        public String getHostname() { return hostname; }
        public void setHostname(String hostname) { this.hostname = hostname; }

        public String getContainerSubnet() { return containerSubnet; }
        public void setContainerSubnet(String containerSubnet) { this.containerSubnet = containerSubnet; }

        public String getOverlayIp() { return overlayIp; }
        public void setOverlayIp(String overlayIp) { this.overlayIp = overlayIp; }

        public String getBgpAs() { return bgpAs; }
        public void setBgpAs(String bgpAs) { this.bgpAs = bgpAs; }

        @Override
        public String toString() {
            return "NodeConfig{" +
                "nodeId=" + nodeId +
                ", mgmtIp='" + mgmtIp + '\'' +
                ", hostname='" + hostname + '\'' +
                '}';
        }
    }

    @Override
    public String toString() {
        return "OverlayConfig{" +
            "tenant=" + getTenantName() +
            ", nodes=" + getNodeCount() +
            ", overlay=" + getOverlayNetwork() +
            '}';
    }
}
