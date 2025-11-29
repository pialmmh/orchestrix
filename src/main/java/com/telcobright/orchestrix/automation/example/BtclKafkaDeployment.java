package com.telcobright.orchestrix.automation.example;

import com.telcobright.orchestrix.automation.cluster.kafkadocker.KafkaClusterDeployment;
import com.telcobright.orchestrix.automation.config.KafkaConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * BTCL Kafka Cluster Deployment - One-Click Runner
 *
 * Deploys Kafka cluster (KRaft mode) to BTCL production environment.
 * Reads all configuration from deployments/btcl_network/kafka/cluster-config.conf
 *
 * Prerequisites:
 * - Overlay network must be running (WireGuard + FRR + BGP) ✓ COMPLETED
 * - Docker and Docker Compose installed on all nodes
 * - Passwordless sudo enabled on all nodes ✓ VERIFIED
 * - Nodes accessible via SSH
 *
 * Network Architecture:
 * - Node 1: 114.130.145.75:50005 (local: 192.168.24.211) -> Kafka broker 1 @ 10.10.101.20
 * - Node 2: 114.130.145.75:50006 (local: 192.168.24.214) -> Kafka broker 2 @ 10.10.102.20
 * - Node 3: 114.130.145.70:50010 (local: 114.130.145.70) -> Kafka broker 3 @ 10.10.103.20
 *
 * Usage:
 * <pre>
 * mvn compile exec:java \
 *   -Dexec.mainClass="com.telcobright.orchestrix.automation.example.BtclKafkaDeployment"
 * </pre>
 *
 * @author TelcoBright Orchestrix Team
 * @since 2025-11-29
 */
public class BtclKafkaDeployment {

    private static final Logger log = LoggerFactory.getLogger(BtclKafkaDeployment.class);

    private static final String DEPLOYMENT_DIR = "deployments/btcl_network/kafka";

    public static void main(String[] args) {
        log.info("╔════════════════════════════════════════════════════════════════╗");
        log.info("║        BTCL Kafka Cluster - One-Click Deployment             ║");
        log.info("╚════════════════════════════════════════════════════════════════╝");
        log.info("");
        log.info("🏢 Environment: Bangladesh Telecommunications Company Limited");
        log.info("📍 Deployment: Production (BTCL Network)");
        log.info("");

        try {
            // Step 1: Load configuration
            log.info("📋 Loading configuration from: {}", DEPLOYMENT_DIR);
            KafkaConfig config = new KafkaConfig(DEPLOYMENT_DIR);
            log.info("✓ Configuration loaded");
            log.info("  Cluster: {}", config.getClusterName());
            log.info("  Nodes: {}", config.getNodeCount());
            log.info("  Bootstrap: {}", config.getBootstrapServers());
            log.info("  Offsets Replication Factor: {}", config.getOffsetsReplicationFactor());
            log.info("  Transaction Min ISR: {}", config.getTransactionMinIsr());
            log.info("");

            // Step 2: Create deployment automation
            KafkaClusterDeployment deployment = new KafkaClusterDeployment(config);

            // Step 3: Execute deployment
            log.info("🚀 Starting deployment...");
            log.info("");
            KafkaClusterDeployment.DeploymentResult result = deployment.deploy();

            // Step 4: Check result
            if (result.isSuccess()) {
                log.info("");
                log.info("╔════════════════════════════════════════════════════════════════╗");
                log.info("║           ✓ BTCL Kafka Cluster Deployed Successfully!        ║");
                log.info("╚════════════════════════════════════════════════════════════════╝");
                log.info("");
                log.info("🎯 Kafka Cluster Ready:");
                log.info("  Cluster Name: {}", result.getClusterName());
                log.info("  Bootstrap Servers: {}", result.getBootstrapServers());
                log.info("  Mode: KRaft (No Zookeeper)");
                log.info("");
                log.info("📡 Broker Details:");
                log.info("  Broker 1: 10.10.101.20:9092 (btcl-kafka01)");
                log.info("  Broker 2: 10.10.102.20:9092 (btcl-kafka02)");
                log.info("  Broker 3: 10.10.103.20:9092 (btcl-kafka03)");
                log.info("");
                log.info("📝 Usage Examples:");
                log.info("  # Create topic:");
                log.info("  kafka-topics.sh --create --topic btcl-events \\");
                log.info("    --bootstrap-server {} \\", result.getBootstrapServers());
                log.info("    --partitions 3 --replication-factor 3");
                log.info("");
                log.info("  # List topics:");
                log.info("  kafka-topics.sh --list --bootstrap-server {}", result.getBootstrapServers());
                log.info("");
                log.info("  # Describe cluster:");
                log.info("  kafka-cluster.sh cluster-id --bootstrap-server {}", result.getBootstrapServers());
                log.info("");
                log.info("💡 Next Steps:");
                log.info("  1. Create test topic and verify replication");
                log.info("  2. Configure monitoring and alerting");
                log.info("  3. Set up VPN access for developers");
                log.info("");

                System.exit(0);

            } else {
                log.error("");
                log.error("╔════════════════════════════════════════════════════════════════╗");
                log.error("║              ✗ BTCL Kafka Deployment Failed!                 ║");
                log.error("╚════════════════════════════════════════════════════════════════╝");
                log.error("");
                log.error("Error: {}", result.getMessage());

                if (result.getException() != null) {
                    log.error("Exception details:", result.getException());
                }

                log.error("");
                log.error("📋 Troubleshooting:");
                log.error("  1. Check overlay network: sudo vtysh -c 'show bgp summary'");
                log.error("  2. Verify Docker: docker --version && docker ps");
                log.error("  3. Check logs: docker logs <container-name>");
                log.error("  4. Review deployment config: cat {}/cluster-config.conf", DEPLOYMENT_DIR);
                log.error("");

                System.exit(1);
            }

        } catch (Exception e) {
            log.error("");
            log.error("╔════════════════════════════════════════════════════════════════╗");
            log.error("║          ✗ Deployment Failed with Exception!                 ║");
            log.error("╚════════════════════════════════════════════════════════════════╝");
            log.error("");
            log.error("Error: {}", e.getMessage(), e);

            System.exit(1);
        }
    }
}
