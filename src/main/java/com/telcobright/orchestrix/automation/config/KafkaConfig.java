package com.telcobright.orchestrix.automation.config;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Kafka Cluster Configuration
 *
 * Extends CommonConfig with Kafka-specific parameters for KRaft mode deployment
 *
 * Loads configuration from deployments/{tenant}/kafka/cluster-config.conf
 *
 * Supports KRaft mode (no Zookeeper) with:
 * - Multiple broker nodes with dedicated IPs
 * - Controller quorum for cluster coordination
 * - BGP-routed container subnets
 * - Host networking with secondary bridge IPs
 *
 * Usage:
 * <pre>
 * KafkaConfig config = new KafkaConfig("deployments/netlab/kafka");
 * // Access common params
 * String sshUser = config.getSshUser();
 * String sshPassword = config.getSshPassword();
 * // Access Kafka-specific params
 * String kafkaImage = config.getKafkaImage();
 * List&lt;KafkaNodeConfig&gt; nodes = config.getNodes();
 * </pre>
 *
 * @author TelcoBright Orchestrix Team
 * @since 2025-11-16
 */
public class KafkaConfig extends CommonConfig {

    /**
     * Load Kafka configuration from deployment directory
     *
     * @param deploymentDir Kafka deployment directory (e.g., "deployments/netlab/kafka")
     */
    public KafkaConfig(String deploymentDir) throws IOException {
        super(); // Don't call super(tenantDir) - we'll load manually

        // Load Kafka cluster configuration
        parseConfig(deploymentDir + "/cluster-config.conf");

        // Validate required parameters
        validate();
    }

    /**
     * Validate required Kafka parameters
     *
     * Note: CONTROLLER_QUORUM_VOTERS is NOT required in config file as it is
     * dynamically generated from NODE*_KAFKA_ID and NODE*_KAFKA_IP values.
     */
    private void validate() throws IllegalArgumentException {
        requireProperty("CLUSTER_NAME", "Cluster name is required");
        requireProperty("NODE_COUNT", "Node count is required");
        requireProperty("KAFKA_IMAGE", "Kafka image is required");

        // Validate node configurations (including KAFKA_ID and KAFKA_IP used to build quorum voters)
        int nodeCount = getNodeCount();
        for (int i = 1; i <= nodeCount; i++) {
            requireProperty("NODE" + i + "_MGMT_IP", "Node " + i + " management IP is required");
            requireProperty("NODE" + i + "_SSH_USER", "Node " + i + " SSH user is required");
            requireProperty("NODE" + i + "_SSH_PASS", "Node " + i + " SSH password is required");
            requireProperty("NODE" + i + "_KAFKA_IP", "Node " + i + " Kafka IP is required");
            requireProperty("NODE" + i + "_KAFKA_ID", "Node " + i + " Kafka ID is required");
            requireProperty("NODE" + i + "_BRIDGE_NAME", "Node " + i + " bridge name is required");
        }
    }

    private void requireProperty(String key, String message) {
        if (!hasProperty(key)) {
            throw new IllegalArgumentException(message + " (missing: " + key + ")");
        }
    }

    // ============================================================
    // Cluster Configuration
    // ============================================================

    public String getClusterName() {
        return getProperty("CLUSTER_NAME");
    }

    public String getClusterId() {
        return getProperty("CLUSTER_ID");
    }

    public int getNodeCount() {
        return Integer.parseInt(getProperty("NODE_COUNT", "3"));
    }

    public int getSshPort() {
        return Integer.parseInt(getProperty("SSH_PORT", "22"));
    }

    // ============================================================
    // Kafka Configuration
    // ============================================================

    public String getKafkaImage() {
        return getProperty("KAFKA_IMAGE");
    }

    public String getKafkaDataDir() {
        return getProperty("KAFKA_DATA_DIR", "/var/lib/kafka/data");
    }

    public String getKafkaWorkDirPrefix() {
        return getProperty("KAFKA_WORK_DIR_PREFIX", "kafka-node");
    }

    public int getOffsetsReplicationFactor() {
        return Integer.parseInt(getProperty("KAFKA_OFFSETS_REPLICATION_FACTOR", "3"));
    }

    public int getTransactionReplicationFactor() {
        return Integer.parseInt(getProperty("KAFKA_TRANSACTION_REPLICATION_FACTOR", "3"));
    }

    public int getTransactionMinIsr() {
        return Integer.parseInt(getProperty("KAFKA_TRANSACTION_MIN_ISR", "2"));
    }

    public boolean isAutoCreateTopics() {
        return Boolean.parseBoolean(getProperty("KAFKA_AUTO_CREATE_TOPICS", "true"));
    }

    /**
     * Get controller quorum voters string dynamically built from node configs.
     * Format: "1@10.10.199.20:9093,2@10.10.198.20:9093,3@10.10.197.20:9093"
     */
    public String getControllerQuorumVoters() {
        StringBuilder voters = new StringBuilder();
        int nodeCount = getNodeCount();
        for (int i = 1; i <= nodeCount; i++) {
            if (i > 1) voters.append(",");
            voters.append(getProperty("NODE" + i + "_KAFKA_ID"))
                  .append("@")
                  .append(getProperty("NODE" + i + "_KAFKA_IP"))
                  .append(":9093");
        }
        return voters.toString();
    }

    // ============================================================
    // Node-Specific Configuration
    // ============================================================

    /**
     * Get node configuration by index (1-based)
     */
    public KafkaNodeConfig getNode(int nodeNumber) {
        KafkaNodeConfig node = new KafkaNodeConfig();
        node.setNodeId(nodeNumber);
        node.setMgmtIp(getProperty("NODE" + nodeNumber + "_MGMT_IP"));
        node.setSshUser(getProperty("NODE" + nodeNumber + "_SSH_USER"));
        node.setSshPassword(getProperty("NODE" + nodeNumber + "_SSH_PASS"));
        node.setSshPort(Integer.parseInt(getProperty("NODE" + nodeNumber + "_SSH_PORT",
                String.valueOf(getSshPort()))));
        node.setSubnet(getProperty("NODE" + nodeNumber + "_SUBNET"));
        node.setBridgeGw(getProperty("NODE" + nodeNumber + "_BRIDGE_GW"));
        node.setKafkaIp(getProperty("NODE" + nodeNumber + "_KAFKA_IP"));
        node.setKafkaId(Integer.parseInt(getProperty("NODE" + nodeNumber + "_KAFKA_ID")));
        node.setBridgeName(getProperty("NODE" + nodeNumber + "_BRIDGE_NAME"));
        return node;
    }

    /**
     * Get all node configurations
     */
    public List<KafkaNodeConfig> getNodes() {
        List<KafkaNodeConfig> nodes = new ArrayList<>();
        int nodeCount = getNodeCount();
        for (int i = 1; i <= nodeCount; i++) {
            nodes.add(getNode(i));
        }
        return nodes;
    }

    /**
     * Get bootstrap servers connection string
     */
    public String getBootstrapServers() {
        List<KafkaNodeConfig> nodes = getNodes();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < nodes.size(); i++) {
            if (i > 0) sb.append(",");
            sb.append(nodes.get(i).getKafkaIp()).append(":9092");
        }
        return sb.toString();
    }

    // ============================================================
    // Kafka Node Configuration
    // ============================================================

    /**
     * Configuration for a single Kafka node
     */
    public static class KafkaNodeConfig {
        private int nodeId;
        private String mgmtIp;
        private String sshUser;
        private String sshPassword;
        private int sshPort;
        private String subnet;
        private String bridgeGw;
        private String kafkaIp;
        private int kafkaId;
        private String bridgeName;

        // Getters and setters
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

        public String getSubnet() { return subnet; }
        public void setSubnet(String subnet) { this.subnet = subnet; }

        public String getBridgeGw() { return bridgeGw; }
        public void setBridgeGw(String bridgeGw) { this.bridgeGw = bridgeGw; }

        public String getKafkaIp() { return kafkaIp; }
        public void setKafkaIp(String kafkaIp) { this.kafkaIp = kafkaIp; }

        public int getKafkaId() { return kafkaId; }
        public void setKafkaId(int kafkaId) { this.kafkaId = kafkaId; }

        public String getBridgeName() { return bridgeName; }
        public void setBridgeName(String bridgeName) { this.bridgeName = bridgeName; }

        @Override
        public String toString() {
            return "KafkaNodeConfig{" +
                    "nodeId=" + nodeId +
                    ", mgmtIp='" + mgmtIp + '\'' +
                    ", kafkaIp='" + kafkaIp + '\'' +
                    ", kafkaId=" + kafkaId +
                    ", bridge='" + bridgeName + '\'' +
                    '}';
        }
    }

    @Override
    public String toString() {
        return "KafkaConfig{" +
                "cluster=" + getClusterName() +
                ", nodes=" + getNodeCount() +
                ", image=" + getKafkaImage() +
                ", bootstrap=" + getBootstrapServers() +
                '}';
    }
}
