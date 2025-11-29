package com.telcobright.orchestrix.automation.cluster.kafkadocker;

import com.telcobright.orchestrix.automation.config.KafkaConfig;
import com.telcobright.orchestrix.automation.config.KafkaConfig.KafkaNodeConfig;
import com.telcobright.orchestrix.automation.core.device.impl.RemoteSshDevice;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Kafka Cluster Deployment Orchestrator
 *
 * Main orchestrator that coordinates all atomic automations to deploy
 * a complete Kafka cluster in KRaft mode.
 *
 * Uses atomic automations:
 * - NetworkConfigAutomation: Bridge IP configuration
 * - DockerComposeGenerator: YAML generation
 * - DockerDeploymentAutomation: Container lifecycle
 * - KafkaClusterVerifier: Health checks
 *
 * Reusable across all tenants (netlab, link3, bdcom, btcl, etc.)
 *
 * Usage:
 * <pre>
 * KafkaConfig config = new KafkaConfig("deployments/netlab/kafka");
 * KafkaClusterDeployment deployment = new KafkaClusterDeployment(config);
 * deployment.deploy();
 * </pre>
 *
 * @author TelcoBright Orchestrix Team
 * @since 2025-11-16
 */
public class KafkaClusterDeployment {

    private static final Logger log = LoggerFactory.getLogger(KafkaClusterDeployment.class);

    private final KafkaConfig config;
    private final Map<String, RemoteSshDevice> connections = new HashMap<>();
    private final Map<String, NetworkConfigAutomation> networkAutomations = new HashMap<>();
    private final Map<String, DockerDeploymentAutomation> dockerAutomations = new HashMap<>();

    public KafkaClusterDeployment(KafkaConfig config) {
        this.config = config;
    }

    /**
     * Deploy complete Kafka cluster
     *
     * @return Deployment result
     */
    public DeploymentResult deploy() {
        log.info("╔════════════════════════════════════════════════════════════════╗");
        log.info("║        Kafka Cluster Deployment - KRaft Mode                 ║");
        log.info("╚════════════════════════════════════════════════════════════════╝");
        log.info("");
        log.info("Cluster: {}", config.getClusterName());
        log.info("Nodes: {}", config.getNodeCount());
        log.info("Image: {}", config.getKafkaImage());
        log.info("Bootstrap: {}", config.getBootstrapServers());
        log.info("");

        DeploymentResult result = new DeploymentResult();
        result.setClusterName(config.getClusterName());
        result.setBootstrapServers(config.getBootstrapServers());

        try {
            // Step 1: Connect to all nodes
            connectToAllNodes();

            // Step 2: Verify prerequisites
            verifyPrerequisites();

            // Step 3: Configure network (add secondary IPs)
            configureNetwork();

            // Step 4: Deploy to each node
            deployToAllNodes();

            // Step 5: Wait for cluster formation
            waitForClusterFormation();

            // Step 6: Verify cluster health
            boolean healthy = verifyClusterHealth();

            if (healthy) {
                result.setSuccess(true);
                result.setMessage("Cluster deployed successfully");
                printSuccessSummary();
            } else {
                result.setSuccess(false);
                result.setMessage("Cluster deployed but health checks failed");
                log.warn("⚠ Cluster deployed but some health checks failed");
            }

        } catch (Exception e) {
            log.error("❌ Deployment failed", e);
            result.setSuccess(false);
            result.setMessage("Deployment failed: " + e.getMessage());
            result.setException(e);

        } finally {
            disconnectAll();
        }

        return result;
    }

    /**
     * Connect to all cluster nodes
     */
    private void connectToAllNodes() throws Exception {
        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        log.info("🔌 Connecting to Nodes");
        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");

        List<KafkaNodeConfig> nodes = config.getNodes();

        for (KafkaNodeConfig node : nodes) {
            log.info("Connecting to Node {} ({}:{})...",
                    node.getNodeId(), node.getMgmtIp(), node.getSshPort());

            RemoteSshDevice ssh = new RemoteSshDevice(
                    node.getMgmtIp(),
                    node.getSshPort(),
                    node.getSshUser()
            );
            ssh.connect(node.getSshPassword());

            // Use Kafka IP as key (unique per node) - mgmtIp may be shared when nodes use NAT
            String nodeKey = node.getKafkaIp();
            connections.put(nodeKey, ssh);
            networkAutomations.put(nodeKey, new NetworkConfigAutomation(ssh));
            dockerAutomations.put(nodeKey, new DockerDeploymentAutomation(ssh));

            log.info("  ✓ Connected to Node {}", node.getNodeId());
        }

        log.info("✓ All nodes connected ({} nodes)", nodes.size());
        log.info("");
    }

    /**
     * Verify prerequisites on all nodes
     */
    private void verifyPrerequisites() throws Exception {
        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        log.info("✅ Verifying Prerequisites");
        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");

        List<KafkaNodeConfig> nodes = config.getNodes();

        for (KafkaNodeConfig node : nodes) {
            String nodeKey = node.getKafkaIp();
            DockerDeploymentAutomation docker = dockerAutomations.get(nodeKey);
            NetworkConfigAutomation network = networkAutomations.get(nodeKey);

            // Verify Docker
            String dockerVersion = docker.verifyDocker();
            log.info("  [Node {}] Docker: {}", node.getNodeId(), dockerVersion.trim());

            // Verify Docker Compose
            String composeVersion = docker.verifyDockerCompose();
            log.info("  [Node {}] Docker Compose: {}", node.getNodeId(), composeVersion.trim());

            // Verify bridge exists
            if (!network.verifyBridgeExists(node.getBridgeName())) {
                throw new IllegalStateException("Bridge " + node.getBridgeName() +
                        " not found on Node " + node.getNodeId());
            }
            log.info("  [Node {}] Bridge: {} verified", node.getNodeId(), node.getBridgeName());
        }

        log.info("✓ Prerequisites verified");
        log.info("");
    }

    /**
     * Configure network on all nodes
     */
    private void configureNetwork() throws Exception {
        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        log.info("🌐 Configuring Network");
        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");

        List<KafkaNodeConfig> nodes = config.getNodes();

        for (KafkaNodeConfig node : nodes) {
            String nodeKey = node.getKafkaIp();
            NetworkConfigAutomation network = networkAutomations.get(nodeKey);

            // Add secondary IP to bridge
            network.addSecondaryIp(node.getBridgeName(), node.getKafkaIp() + "/24");

            // Verify IP was added
            boolean exists = network.verifyIpExists(node.getBridgeName(), node.getKafkaIp());
            if (!exists) {
                throw new IllegalStateException("Failed to add IP " + node.getKafkaIp() +
                        " to bridge " + node.getBridgeName());
            }

            log.info("  ✓ Node {}: {} configured on {}",
                    node.getNodeId(), node.getKafkaIp(), node.getBridgeName());

            // Display IP configuration
            String ipConfig = network.getIpConfiguration(node.getBridgeName());
            log.debug("  IP Config:\n{}", ipConfig);
        }

        log.info("✓ Network configured");
        log.info("");
    }

    /**
     * Deploy to all nodes
     */
    private void deployToAllNodes() throws Exception {
        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        log.info("🚀 Deploying Kafka to All Nodes");
        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");

        List<KafkaNodeConfig> nodes = config.getNodes();
        DockerComposeGenerator generator = new DockerComposeGenerator(config);

        for (KafkaNodeConfig node : nodes) {
            log.info("Deploying to Node {} ({})...", node.getNodeId(), node.getKafkaIp());

            String nodeKey = node.getKafkaIp();
            DockerDeploymentAutomation docker = dockerAutomations.get(nodeKey);

            // Create directories
            String workDir = String.format("~/%s-%d",
                    config.getKafkaWorkDirPrefix(), node.getNodeId());

            docker.createWorkingDirectory(workDir);
            docker.createDataDirectory(config.getKafkaDataDir());

            // Generate and upload docker-compose.yml
            String dockerComposeYaml = generator.generateForNodeWithMetadata(node);

            if (!generator.validate(dockerComposeYaml)) {
                throw new IllegalStateException("Invalid docker-compose.yml generated for Node " +
                        node.getNodeId());
            }

            docker.uploadDockerCompose(workDir, dockerComposeYaml);
            log.debug("  docker-compose.yml uploaded to {}", workDir);

            // Start containers
            docker.startContainers(workDir);
            log.info("  ✓ Kafka container started on Node {}", node.getNodeId());
        }

        log.info("✓ Deployed to all nodes");
        log.info("");
    }

    /**
     * Wait for cluster formation
     */
    private void waitForClusterFormation() throws Exception {
        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        log.info("⏳ Waiting for Cluster Formation");
        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");

        List<KafkaNodeConfig> nodes = config.getNodes();

        // Wait for all containers to be running
        for (KafkaNodeConfig node : nodes) {
            String nodeKey = node.getKafkaIp();
            DockerDeploymentAutomation docker = dockerAutomations.get(nodeKey);

            boolean running = docker.waitForContainer("kafka-node", 30);
            if (running) {
                log.info("  ✓ Node {} container is running", node.getNodeId());
            } else {
                log.warn("  ⚠ Node {} container did not start within timeout", node.getNodeId());
            }
        }

        // Wait for KRaft quorum to form
        log.info("Waiting for KRaft controller quorum to form (10 seconds)...");
        Thread.sleep(10000);

        // Wait for brokers to become ready
        log.info("Waiting for brokers to become ready (5 seconds)...");
        Thread.sleep(5000);

        log.info("✓ Cluster formation complete");
        log.info("");
    }

    /**
     * Verify cluster health
     */
    private boolean verifyClusterHealth() throws Exception {
        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        log.info("🔍 Verifying Cluster Health");
        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");

        // Use first node for verification
        List<KafkaNodeConfig> nodes = config.getNodes();
        KafkaNodeConfig firstNode = nodes.get(0);
        RemoteSshDevice ssh = connections.get(firstNode.getMgmtIp());

        KafkaClusterVerifier verifier = new KafkaClusterVerifier(config, ssh, "kafka-node");

        return verifier.runFullVerification();
    }

    /**
     * Print success summary
     */
    private void printSuccessSummary() {
        log.info("");
        log.info("╔════════════════════════════════════════════════════════════════╗");
        log.info("║              Deployment Complete! 🚀                          ║");
        log.info("╚════════════════════════════════════════════════════════════════╝");
        log.info("");
        log.info("📊 Cluster Summary:");
        log.info("  Cluster Name: {}", config.getClusterName());
        log.info("  Node Count: {}", config.getNodeCount());
        log.info("  Image: {}", config.getKafkaImage());
        log.info("");

        log.info("  Kafka Brokers:");
        for (KafkaNodeConfig node : config.getNodes()) {
            log.info("    • Broker {}: {}:9092 (controller: {}:9093)",
                    node.getKafkaId(), node.getKafkaIp(), node.getKafkaIp());
        }
        log.info("");

        log.info("📝 Connection String:");
        log.info("  Bootstrap Servers: {}", config.getBootstrapServers());
        log.info("");

        log.info("🎯 Next Steps:");
        log.info("  • Create topics using kafka-topics command");
        log.info("  • Produce/consume messages");
        log.info("  • Configure client applications with bootstrap servers");
        log.info("");
    }

    /**
     * Disconnect from all nodes
     */
    private void disconnectAll() {
        for (Map.Entry<String, RemoteSshDevice> entry : connections.entrySet()) {
            try {
                entry.getValue().disconnect();
                log.debug("Disconnected from {}", entry.getKey());
            } catch (Exception e) {
                log.warn("Error disconnecting from {}: {}", entry.getKey(), e.getMessage());
            }
        }
        connections.clear();
        networkAutomations.clear();
        dockerAutomations.clear();
    }

    /**
     * Deployment result
     */
    public static class DeploymentResult {
        private boolean success;
        private String message;
        private String clusterName;
        private String bootstrapServers;
        private Exception exception;

        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }

        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }

        public String getClusterName() { return clusterName; }
        public void setClusterName(String clusterName) { this.clusterName = clusterName; }

        public String getBootstrapServers() { return bootstrapServers; }
        public void setBootstrapServers(String bootstrapServers) { this.bootstrapServers = bootstrapServers; }

        public Exception getException() { return exception; }
        public void setException(Exception exception) { this.exception = exception; }

        @Override
        public String toString() {
            return "DeploymentResult{" +
                    "success=" + success +
                    ", cluster=" + clusterName +
                    ", bootstrap=" + bootstrapServers +
                    ", message='" + message + '\'' +
                    '}';
        }
    }
}
