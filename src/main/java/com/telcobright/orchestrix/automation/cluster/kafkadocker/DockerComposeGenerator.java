package com.telcobright.orchestrix.automation.cluster.kafkadocker;

import com.telcobright.orchestrix.automation.config.KafkaConfig;
import com.telcobright.orchestrix.automation.config.KafkaConfig.KafkaNodeConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Docker Compose Generator for Kafka KRaft Mode
 *
 * Atomic automation for generating docker-compose.yml files for Kafka
 * in KRaft mode (no Zookeeper).
 *
 * Generates configuration for:
 * - Host networking mode (avoids bridge conflicts)
 * - KRaft controller quorum
 * - Proper listener configuration
 * - Data volume mounting
 *
 * Reusable across all tenants and deployment types.
 *
 * Usage:
 * <pre>
 * DockerComposeGenerator generator = new DockerComposeGenerator(config);
 * String yaml = generator.generateForNode(nodeConfig);
 * </pre>
 *
 * @author TelcoBright Orchestrix Team
 * @since 2025-11-16
 */
public class DockerComposeGenerator {

    private static final Logger log = LoggerFactory.getLogger(DockerComposeGenerator.class);
    private final KafkaConfig config;

    public DockerComposeGenerator(KafkaConfig config) {
        this.config = config;
    }

    /**
     * Generate docker-compose.yml for a specific Kafka node
     *
     * @param node Node configuration
     * @return Docker Compose YAML content
     */
    public String generateForNode(KafkaNodeConfig node) {
        log.debug("Generating docker-compose.yml for node {}", node.getNodeId());

        StringBuilder yaml = new StringBuilder();

        // Header
        yaml.append("version: '3.8'\n");
        yaml.append("\n");

        // Kafka service
        yaml.append("services:\n");
        yaml.append("  kafka:\n");
        yaml.append("    image: ").append(config.getKafkaImage()).append("\n");
        yaml.append("    container_name: kafka-node\n");
        yaml.append("    restart: always\n");
        yaml.append("    network_mode: host\n");
        yaml.append("    environment:\n");

        // KRaft mode configuration
        yaml.append("      KAFKA_NODE_ID: ").append(node.getKafkaId()).append("\n");
        yaml.append("      KAFKA_PROCESS_ROLES: \"broker,controller\"\n");

        // Listeners
        yaml.append("      KAFKA_LISTENERS: \"PLAINTEXT://")
                .append(node.getKafkaIp()).append(":9092,CONTROLLER://")
                .append(node.getKafkaIp()).append(":9093\"\n");

        yaml.append("      KAFKA_ADVERTISED_LISTENERS: \"PLAINTEXT://")
                .append(node.getKafkaIp()).append(":9092\"\n");

        yaml.append("      KAFKA_CONTROLLER_LISTENER_NAMES: \"CONTROLLER\"\n");

        // Controller quorum voters
        yaml.append("      KAFKA_CONTROLLER_QUORUM_VOTERS: \"")
                .append(config.getControllerQuorumVoters()).append("\"\n");

        // Replication configuration
        yaml.append("      KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR: ")
                .append(config.getOffsetsReplicationFactor()).append("\n");

        yaml.append("      KAFKA_TRANSACTION_STATE_LOG_REPLICATION_FACTOR: ")
                .append(config.getTransactionReplicationFactor()).append("\n");

        yaml.append("      KAFKA_TRANSACTION_STATE_LOG_MIN_ISR: ")
                .append(config.getTransactionMinIsr()).append("\n");

        yaml.append("      KAFKA_AUTO_CREATE_TOPICS_ENABLE: \"")
                .append(config.isAutoCreateTopics()).append("\"\n");

        // Volume mapping
        yaml.append("    volumes:\n");
        yaml.append("      - ").append(config.getKafkaDataDir())
                .append(":/var/lib/kafka/data\n");

        String yamlContent = yaml.toString();
        log.debug("Generated docker-compose.yml ({} bytes)", yamlContent.length());

        return yamlContent;
    }

    /**
     * Validate docker-compose.yml syntax
     *
     * @param yaml Docker Compose YAML content
     * @return true if valid
     */
    public boolean validate(String yaml) {
        if (yaml == null || yaml.isEmpty()) {
            log.error("Docker Compose YAML is empty");
            return false;
        }

        // Basic validation
        if (!yaml.contains("version:")) {
            log.error("Missing 'version:' in docker-compose.yml");
            return false;
        }

        if (!yaml.contains("services:")) {
            log.error("Missing 'services:' in docker-compose.yml");
            return false;
        }

        if (!yaml.contains("KAFKA_NODE_ID:")) {
            log.error("Missing KAFKA_NODE_ID in docker-compose.yml");
            return false;
        }

        if (!yaml.contains("KAFKA_CONTROLLER_QUORUM_VOTERS:")) {
            log.error("Missing KAFKA_CONTROLLER_QUORUM_VOTERS in docker-compose.yml");
            return false;
        }

        log.debug("Docker Compose YAML validation passed");
        return true;
    }

    /**
     * Generate docker-compose.yml with cluster info comment
     *
     * @param node Node configuration
     * @return Docker Compose YAML with metadata comments
     */
    public String generateForNodeWithMetadata(KafkaNodeConfig node) {
        StringBuilder yaml = new StringBuilder();

        // Add metadata comments
        yaml.append("# ============================================================\n");
        yaml.append("# Kafka Cluster - KRaft Mode (No Zookeeper)\n");
        yaml.append("# ============================================================\n");
        yaml.append("# Cluster: ").append(config.getClusterName()).append("\n");
        yaml.append("# Node ID: ").append(node.getNodeId()).append("\n");
        yaml.append("# Kafka ID: ").append(node.getKafkaId()).append("\n");
        yaml.append("# Kafka IP: ").append(node.getKafkaIp()).append(":9092\n");
        yaml.append("# Controller: ").append(node.getKafkaIp()).append(":9093\n");
        yaml.append("# ============================================================\n");
        yaml.append("# Bootstrap Servers: ").append(config.getBootstrapServers()).append("\n");
        yaml.append("# ============================================================\n");
        yaml.append("\n");

        // Append actual docker-compose content
        yaml.append(generateForNode(node));

        return yaml.toString();
    }
}
