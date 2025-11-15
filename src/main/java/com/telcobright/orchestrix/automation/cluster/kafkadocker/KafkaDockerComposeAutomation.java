package com.telcobright.orchestrix.automation.cluster.kafkadocker;

import com.telcobright.orchestrix.automation.core.device.impl.RemoteSshDevice;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

/**
 * Kafka + Zookeeper Docker Compose Cluster Automation
 *
 * Reusable automation for deploying Kafka clusters using Docker Compose
 * across all tenants (Link3, Netlab, BDCOM, BTCL, etc.)
 *
 * Architecture:
 * - Deploys 3-node Zookeeper ensemble
 * - Deploys 3-broker Kafka cluster
 * - Uses docker-compose for container orchestration
 * - Follows container networking guidelines with BGP routing
 *
 * Usage:
 * <pre>
 * KafkaDockerComposeAutomation automation = new KafkaDockerComposeAutomation("deployments/netlab/kafka");
 * automation.deploy();
 * </pre>
 *
 * @author TelcoBright Orchestrix Team
 * @since 2025-11-16
 */
public class KafkaDockerComposeAutomation {

    private static final Logger log = LoggerFactory.getLogger(KafkaDockerComposeAutomation.class);

    private final String deploymentDir;
    private final Map<String, RemoteSshDevice> connections = new HashMap<>();
    private final List<NodeConfig> nodes = new ArrayList<>();

    public KafkaDockerComposeAutomation(String deploymentDir) {
        this.deploymentDir = deploymentDir;
    }

    /**
     * Deploy Kafka cluster across all nodes
     */
    public DeploymentResult deploy() throws Exception {
        log.info("╔════════════════════════════════════════════════════════════════╗");
        log.info("║  Kafka Docker Compose Cluster - Automated Deployment         ║");
        log.info("╚════════════════════════════════════════════════════════════════╝");
        log.info("");

        DeploymentResult result = new DeploymentResult();

        try {
            // Step 1: Load configuration
            loadConfiguration();

            // Step 2: Connect to all nodes
            connectToNodes();

            // Step 3: Verify prerequisites
            verifyPrerequisites();

            // Step 4: Upload docker-compose files
            uploadDockerComposeFiles();

            // Step 5: Deploy Zookeeper ensemble
            deployZookeeper();

            // Step 6: Deploy Kafka cluster
            deployKafka();

            // Step 7: Verify cluster health
            verifyCluster();

            result.setSuccess(true);
            result.setMessage("Cluster deployed successfully");

            printSummary();

        } catch (Exception e) {
            log.error("❌ Deployment failed", e);
            result.setSuccess(false);
            result.setMessage(e.getMessage());
            throw e;
        } finally {
            disconnectAll();
        }

        return result;
    }

    /**
     * Load configuration from deployment directory
     */
    private void loadConfiguration() throws IOException {
        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        log.info("📋 Loading Configuration");
        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");

        Properties props = new Properties();
        props.load(Files.newInputStream(Paths.get(deploymentDir + "/cluster.conf")));

        // Load SSH password from secrets
        String sshPassword = Files.readString(
            Paths.get(deploymentDir + "/../.secrets/ssh-password.txt")
        ).trim();

        // Parse node configurations
        for (int i = 1; i <= 3; i++) {
            String serverKey = "SERVER_" + i;
            String serverValue = props.getProperty(serverKey);
            if (serverValue == null) continue;

            String[] parts = serverValue.split(":");
            NodeConfig node = new NodeConfig();
            node.setNodeId(i);
            node.setHost(parts[0]);
            node.setSshPort(Integer.parseInt(parts[1]));
            node.setSshUser(parts[2]);
            node.setSshPassword(sshPassword);

            // Load node-specific IPs
            node.setMgmtIp(props.getProperty("SERVER_" + i + "_MGMT_IP"));
            node.setZookeeperIp(props.getProperty("ZK" + i + "_CONTAINER_IP"));
            node.setKafkaIp(props.getProperty("KAFKA" + i + "_CONTAINER_IP"));

            nodes.add(node);
            log.info("  ✓ Node {}: {} (mgmt: {}, zk: {}, kafka: {})",
                    i, node.getHost(), node.getMgmtIp(), node.getZookeeperIp(), node.getKafkaIp());
        }

        log.info("✓ Configuration loaded ({} nodes)", nodes.size());
        log.info("");
    }

    /**
     * Connect to all nodes via SSH
     */
    private void connectToNodes() throws Exception {
        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        log.info("🔌 Connecting to Nodes");
        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");

        for (NodeConfig node : nodes) {
            log.info("  Connecting to {}:{}...", node.getHost(), node.getSshPort());

            RemoteSshDevice ssh = new RemoteSshDevice(
                    node.getHost(),
                    node.getSshPort(),
                    node.getSshUser()
            );
            ssh.connect(node.getSshPassword());

            connections.put(node.getHost(), ssh);
            log.info("  ✓ Connected to {}", node.getHost());
        }

        log.info("✓ All nodes connected");
        log.info("");
    }

    /**
     * Verify prerequisites on all nodes
     */
    private void verifyPrerequisites() throws Exception {
        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        log.info("✅ Verifying Prerequisites");
        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");

        for (NodeConfig node : nodes) {
            RemoteSshDevice ssh = connections.get(node.getHost());

            // Check Docker
            String dockerVersion = ssh.executeCommand("docker --version");
            log.info("  [{}] Docker: {}", node.getHost(), dockerVersion.trim());

            // Check docker-compose
            String composeVersion = ssh.executeCommand("docker-compose --version || docker compose version");
            log.info("  [{}] Docker Compose: {}", node.getHost(), composeVersion.trim());

            // Check network connectivity (BGP routes if applicable)
            String routes = ssh.executeCommand("ip route | grep '10.10.' || echo 'No container routes'");
            log.info("  [{}] Container routes: {}", node.getHost(), routes.trim());
        }

        log.info("✓ Prerequisites verified");
        log.info("");
    }

    /**
     * Upload docker-compose files to all nodes
     */
    private void uploadDockerComposeFiles() throws Exception {
        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        log.info("📤 Uploading Docker Compose Files");
        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");

        for (NodeConfig node : nodes) {
            RemoteSshDevice ssh = connections.get(node.getHost());

            // Create deployment directory
            ssh.executeCommand("mkdir -p ~/kafka-cluster");

            // Upload docker-compose file for this node
            String composeFile = generateDockerComposeForNode(node);
            ssh.executeCommand("cat > ~/kafka-cluster/docker-compose.yml << 'EOF'\n" + composeFile + "\nEOF");

            log.info("  ✓ Uploaded docker-compose.yml to {}", node.getHost());
        }

        log.info("✓ Docker Compose files uploaded");
        log.info("");
    }

    /**
     * Generate docker-compose.yml for a specific node
     */
    private String generateDockerComposeForNode(NodeConfig node) {
        // This will be populated with actual docker-compose content
        // based on Link3 reference files
        return String.format(
                "version: '3.8'\n" +
                "services:\n" +
                "  zookeeper:\n" +
                "    image: confluentinc/cp-zookeeper:latest\n" +
                "    container_name: zookeeper-node-%d\n" +
                "    environment:\n" +
                "      ZOOKEEPER_SERVER_ID: %d\n" +
                "      ZOOKEEPER_CLIENT_PORT: 2181\n" +
                "      ZOOKEEPER_TICK_TIME: 2000\n" +
                "      ZOOKEEPER_SERVERS: 'server.1=%s:2888:3888;server.2=%s:2888:3888;server.3=%s:2888:3888'\n" +
                "    network_mode: host\n" +
                "\n" +
                "  kafka:\n" +
                "    image: confluentinc/cp-kafka:latest\n" +
                "    container_name: kafka-broker-%d\n" +
                "    depends_on:\n" +
                "      - zookeeper\n" +
                "    environment:\n" +
                "      KAFKA_BROKER_ID: %d\n" +
                "      KAFKA_ZOOKEEPER_CONNECT: '%s:2181,%s:2181,%s:2181/kafka'\n" +
                "      KAFKA_ADVERTISED_LISTENERS: 'PLAINTEXT://%s:9092'\n" +
                "      KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR: 3\n" +
                "    network_mode: host\n",
                node.getNodeId(),
                node.getNodeId(),
                nodes.get(0).getZookeeperIp(), nodes.get(1).getZookeeperIp(), nodes.get(2).getZookeeperIp(),
                node.getNodeId(),
                node.getNodeId(),
                nodes.get(0).getZookeeperIp(), nodes.get(1).getZookeeperIp(), nodes.get(2).getZookeeperIp(),
                node.getKafkaIp()
        );
    }

    /**
     * Deploy Zookeeper ensemble
     */
    private void deployZookeeper() throws Exception {
        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        log.info("🐘 Deploying Zookeeper Ensemble");
        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");

        for (NodeConfig node : nodes) {
            RemoteSshDevice ssh = connections.get(node.getHost());
            ssh.executeCommand("cd ~/kafka-cluster && docker-compose up -d zookeeper");
            log.info("  ✓ Zookeeper started on {}", node.getHost());
        }

        log.info("  Waiting for ensemble formation...");
        Thread.sleep(10000);

        log.info("✓ Zookeeper ensemble deployed");
        log.info("");
    }

    /**
     * Deploy Kafka cluster
     */
    private void deployKafka() throws Exception {
        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        log.info("📨 Deploying Kafka Cluster");
        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");

        for (NodeConfig node : nodes) {
            RemoteSshDevice ssh = connections.get(node.getHost());
            ssh.executeCommand("cd ~/kafka-cluster && docker-compose up -d kafka");
            log.info("  ✓ Kafka broker started on {}", node.getHost());
        }

        log.info("  Waiting for brokers to register...");
        Thread.sleep(15000);

        log.info("✓ Kafka cluster deployed");
        log.info("");
    }

    /**
     * Verify cluster health
     */
    private void verifyCluster() throws Exception {
        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        log.info("🔍 Verifying Cluster Health");
        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");

        for (NodeConfig node : nodes) {
            RemoteSshDevice ssh = connections.get(node.getHost());

            // Check Zookeeper
            String zkStatus = ssh.executeCommand("docker exec zookeeper-node-" + node.getNodeId() +
                    " zkServer.sh status 2>&1 | grep -i mode || echo 'status check failed'");
            log.info("  [{}] Zookeeper: {}", node.getHost(), zkStatus.trim());

            // Check Kafka
            String kafkaStatus = ssh.executeCommand("docker ps | grep kafka-broker-" + node.getNodeId());
            log.info("  [{}] Kafka: {}", node.getHost(),
                    kafkaStatus.contains("Up") ? "Running" : "Not running");
        }

        log.info("✓ Cluster verification complete");
        log.info("");
    }

    /**
     * Print deployment summary
     */
    private void printSummary() {
        log.info("╔════════════════════════════════════════════════════════════════╗");
        log.info("║              Deployment Complete! 🚀                          ║");
        log.info("╚════════════════════════════════════════════════════════════════╝");
        log.info("");

        log.info("📊 Cluster Summary:");
        log.info("  Zookeeper Ensemble:");
        for (NodeConfig node : nodes) {
            log.info("    • Node {}: {}:2181", node.getNodeId(), node.getZookeeperIp());
        }
        log.info("");

        log.info("  Kafka Cluster:");
        for (NodeConfig node : nodes) {
            log.info("    • Broker {}: {}:9092", node.getNodeId(), node.getKafkaIp());
        }
        log.info("");

        // Generate connection strings
        StringBuilder zkConnect = new StringBuilder();
        StringBuilder kafkaBootstrap = new StringBuilder();
        for (int i = 0; i < nodes.size(); i++) {
            if (i > 0) {
                zkConnect.append(",");
                kafkaBootstrap.append(",");
            }
            zkConnect.append(nodes.get(i).getZookeeperIp()).append(":2181");
            kafkaBootstrap.append(nodes.get(i).getKafkaIp()).append(":9092");
        }

        log.info("📝 Connection Strings:");
        log.info("  Zookeeper: {}/kafka", zkConnect);
        log.info("  Kafka: {}", kafkaBootstrap);
        log.info("");
    }

    /**
     * Disconnect from all nodes
     */
    private void disconnectAll() {
        for (Map.Entry<String, RemoteSshDevice> entry : connections.entrySet()) {
            try {
                entry.getValue().disconnect();
            } catch (Exception e) {
                log.warn("Error disconnecting from {}", entry.getKey());
            }
        }
        connections.clear();
    }

    /**
     * Node configuration
     */
    public static class NodeConfig {
        private int nodeId;
        private String host;
        private int sshPort;
        private String sshUser;
        private String sshPassword;
        private String mgmtIp;
        private String zookeeperIp;
        private String kafkaIp;

        // Getters and setters
        public int getNodeId() { return nodeId; }
        public void setNodeId(int nodeId) { this.nodeId = nodeId; }

        public String getHost() { return host; }
        public void setHost(String host) { this.host = host; }

        public int getSshPort() { return sshPort; }
        public void setSshPort(int sshPort) { this.sshPort = sshPort; }

        public String getSshUser() { return sshUser; }
        public void setSshUser(String sshUser) { this.sshUser = sshUser; }

        public String getSshPassword() { return sshPassword; }
        public void setSshPassword(String sshPassword) { this.sshPassword = sshPassword; }

        public String getMgmtIp() { return mgmtIp; }
        public void setMgmtIp(String mgmtIp) { this.mgmtIp = mgmtIp; }

        public String getZookeeperIp() { return zookeeperIp; }
        public void setZookeeperIp(String zookeeperIp) { this.zookeeperIp = zookeeperIp; }

        public String getKafkaIp() { return kafkaIp; }
        public void setKafkaIp(String kafkaIp) { this.kafkaIp = kafkaIp; }
    }

    /**
     * Deployment result
     */
    public static class DeploymentResult {
        private boolean success;
        private String message;

        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }

        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
    }

    /**
     * Main method for command-line usage
     */
    public static void main(String[] args) {
        if (args.length < 1) {
            System.out.println("Usage: KafkaDockerComposeAutomation <deployment-dir>");
            System.out.println("");
            System.out.println("Example:");
            System.out.println("  KafkaDockerComposeAutomation deployments/netlab/kafka");
            System.out.println("  KafkaDockerComposeAutomation deployments/link3/kafka");
            System.exit(1);
        }

        String deploymentDir = args[0];

        try {
            KafkaDockerComposeAutomation automation = new KafkaDockerComposeAutomation(deploymentDir);
            DeploymentResult result = automation.deploy();

            if (result.isSuccess()) {
                System.out.println("\n✓ Deployment successful!");
                System.exit(0);
            } else {
                System.err.println("\n✗ Deployment failed: " + result.getMessage());
                System.exit(1);
            }

        } catch (Exception e) {
            System.err.println("\n✗ Deployment failed: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}
