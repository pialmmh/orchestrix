package com.telcobright.orchestrix.automation.example;

import com.telcobright.orchestrix.automation.cluster.kafkadocker.KafkaClusterDeployment;
import com.telcobright.orchestrix.automation.config.KafkaConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Kafka Cluster Deployment - Config-Driven One-Click Runner
 *
 * Deploys Kafka cluster (KRaft mode) to any tenant environment.
 * Reads all configuration from deployment config files.
 *
 * Prerequisites:
 * - Overlay network must be running (WireGuard + FRR + BGP)
 * - Docker and Docker Compose installed on all nodes
 * - VPN connection (if deploying remotely)
 *
 * Usage:
 * <pre>
 * # Deploy to any tenant (specify deployment directory):
 * mvn compile exec:java \
 *   -Dexec.mainClass="com.telcobright.orchestrix.automation.example.NetlabKafkaDeployment" \
 *   -Dexec.args="deployments/ks_network/kafka"
 *
 * # Default (netlab):
 * mvn compile exec:java \
 *   -Dexec.mainClass="com.telcobright.orchestrix.automation.example.NetlabKafkaDeployment"
 * </pre>
 *
 * @author TelcoBright Orchestrix Team
 * @since 2025-11-16
 */
public class NetlabKafkaDeployment {

    private static final Logger log = LoggerFactory.getLogger(NetlabKafkaDeployment.class);

    private static final String DEFAULT_DEPLOYMENT_DIR = "deployments/netlab/kafka";

    public static void main(String[] args) {
        // Accept deployment directory as argument, default to netlab
        String deploymentDir = args.length > 0 ? args[0] : DEFAULT_DEPLOYMENT_DIR;

        log.info("╔════════════════════════════════════════════════════════════════╗");
        log.info("║           Kafka Cluster - One-Click Deployment                ║");
        log.info("╚════════════════════════════════════════════════════════════════╝");
        log.info("");

        try {
            // Step 1: Load configuration
            log.info("📋 Loading configuration from: {}", deploymentDir);
            KafkaConfig config = new KafkaConfig(deploymentDir);
            log.info("✓ Configuration loaded");
            log.info("  Cluster: {}", config.getClusterName());
            log.info("  Nodes: {}", config.getNodeCount());
            log.info("  Bootstrap: {}", config.getBootstrapServers());
            log.info("");

            // Step 2: Create deployment automation
            KafkaClusterDeployment deployment = new KafkaClusterDeployment(config);

            // Step 3: Execute deployment
            KafkaClusterDeployment.DeploymentResult result = deployment.deploy();

            // Step 4: Check result
            if (result.isSuccess()) {
                log.info("");
                log.info("╔════════════════════════════════════════════════════════════════╗");
                log.info("║                  ✓ Deployment Successful!                    ║");
                log.info("╚════════════════════════════════════════════════════════════════╝");
                log.info("");
                log.info("🎯 Kafka Cluster Ready:");
                log.info("  Cluster Name: {}", result.getClusterName());
                log.info("  Bootstrap Servers: {}", result.getBootstrapServers());
                log.info("");
                log.info("📝 Usage Examples:");
                log.info("  # Create topic:");
                log.info("  kafka-topics.sh --create --topic my-topic \\");
                log.info("    --bootstrap-server {} \\", result.getBootstrapServers());
                log.info("    --partitions 3 --replication-factor 3");
                log.info("");
                log.info("  # Produce messages:");
                log.info("  kafka-console-producer.sh --topic my-topic \\");
                log.info("    --bootstrap-server {}", result.getBootstrapServers());
                log.info("");
                log.info("  # Consume messages:");
                log.info("  kafka-console-consumer.sh --topic my-topic \\");
                log.info("    --bootstrap-server {} \\", result.getBootstrapServers());
                log.info("    --from-beginning");
                log.info("");

                System.exit(0);

            } else {
                log.error("");
                log.error("╔════════════════════════════════════════════════════════════════╗");
                log.error("║                  ✗ Deployment Failed!                        ║");
                log.error("╚════════════════════════════════════════════════════════════════╝");
                log.error("");
                log.error("Error: {}", result.getMessage());

                if (result.getException() != null) {
                    log.error("Exception details:", result.getException());
                }

                System.exit(1);
            }

        } catch (Exception e) {
            log.error("");
            log.error("╔════════════════════════════════════════════════════════════════╗");
            log.error("║              ✗ Deployment Failed with Exception!             ║");
            log.error("╚════════════════════════════════════════════════════════════════╝");
            log.error("");
            log.error("Error: {}", e.getMessage(), e);

            System.exit(1);
        }
    }
}
