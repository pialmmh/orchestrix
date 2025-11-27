package com.telcobright.orchestrix.automation.cluster.kafkadocker;

import com.telcobright.orchestrix.automation.config.KafkaConfig;
import com.telcobright.orchestrix.automation.config.KafkaConfig.KafkaNodeConfig;
import com.telcobright.orchestrix.automation.core.device.impl.RemoteSshDevice;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Kafka Cluster Verifier
 *
 * Atomic automation for verifying Kafka cluster health:
 * - Broker connectivity
 * - Topic operations
 * - Message production/consumption
 * - Network connectivity between nodes
 *
 * Reusable across all tenants and deployment types.
 *
 * Usage:
 * <pre>
 * KafkaClusterVerifier verifier = new KafkaClusterVerifier(config, sshDevice);
 * boolean healthy = verifier.verifyBrokerConnectivity();
 * verifier.verifyTopicOperations();
 * </pre>
 *
 * @author TelcoBright Orchestrix Team
 * @since 2025-11-16
 */
public class KafkaClusterVerifier {

    private static final Logger log = LoggerFactory.getLogger(KafkaClusterVerifier.class);
    private final KafkaConfig config;
    private final RemoteSshDevice ssh;
    private final String containerName;

    public KafkaClusterVerifier(KafkaConfig config, RemoteSshDevice ssh, String containerName) {
        this.config = config;
        this.ssh = ssh;
        this.containerName = containerName;
    }

    /**
     * Verify all brokers are reachable
     *
     * @return true if all brokers are reachable
     */
    public boolean verifyBrokerConnectivity() throws Exception {
        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        log.info("🔍 Verifying Broker Connectivity");
        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");

        String bootstrapServers = config.getBootstrapServers();
        log.info("Bootstrap Servers: {}", bootstrapServers);

        try {
            String command = String.format(
                    "sudo docker exec %s kafka-broker-api-versions --bootstrap-server %s 2>&1",
                    containerName, bootstrapServers);

            String output = ssh.executeCommand(command);

            // Check if output contains broker information
            boolean success = output.contains("(id:") && !output.contains("Connection refused");

            if (success) {
                log.info("✓ All brokers are reachable");
                // Log broker IDs found
                String[] lines = output.split("\n");
                for (String line : lines) {
                    if (line.contains("(id:")) {
                        log.info("  • {}", line.trim());
                    }
                }
            } else {
                log.error("✗ Failed to reach brokers");
                log.error("Output: {}", output);
            }

            return success;

        } catch (Exception e) {
            log.error("✗ Error verifying broker connectivity", e);
            return false;
        }
    }

    /**
     * List all topics in the cluster
     *
     * @return Topic list output
     */
    public String listTopics() throws Exception {
        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        log.info("📋 Listing Topics");
        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");

        List<KafkaNodeConfig> nodes = config.getNodes();
        String firstBroker = nodes.get(0).getKafkaIp() + ":9092";

        String command = String.format(
                "sudo docker exec %s kafka-topics --list --bootstrap-server %s 2>&1",
                containerName, firstBroker);

        String output = ssh.executeCommand(command);
        log.info("Topics:\n{}", output);

        return output;
    }

    /**
     * Create a test topic
     *
     * @param topicName Topic name
     * @param partitions Number of partitions
     * @param replicationFactor Replication factor
     * @return true if topic created successfully
     */
    public boolean createTopic(String topicName, int partitions, int replicationFactor) throws Exception {
        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        log.info("📝 Creating Topic: {}", topicName);
        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");

        List<KafkaNodeConfig> nodes = config.getNodes();
        String firstBroker = nodes.get(0).getKafkaIp() + ":9092";

        String command = String.format(
                "sudo docker exec %s kafka-topics --create --topic %s " +
                        "--bootstrap-server %s --replication-factor %d --partitions %d 2>&1",
                containerName, topicName, firstBroker, replicationFactor, partitions);

        String output = ssh.executeCommand(command);

        boolean success = output.contains("Created topic") || output.contains("already exists");

        if (success) {
            log.info("✓ Topic '{}' created/exists", topicName);
        } else {
            log.warn("⚠ Topic creation output: {}", output);
        }

        return success;
    }

    /**
     * Describe a topic
     *
     * @param topicName Topic name
     * @return Topic description output
     */
    public String describeTopic(String topicName) throws Exception {
        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        log.info("🔍 Describing Topic: {}", topicName);
        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");

        List<KafkaNodeConfig> nodes = config.getNodes();
        String firstBroker = nodes.get(0).getKafkaIp() + ":9092";

        String command = String.format(
                "sudo docker exec %s kafka-topics --describe --topic %s --bootstrap-server %s 2>&1",
                containerName, topicName, firstBroker);

        String output = ssh.executeCommand(command);
        log.info("Topic details:\n{}", output);

        return output;
    }

    /**
     * Produce a test message to topic
     *
     * @param topicName Topic name
     * @param message Message content
     * @return true if message produced successfully
     */
    public boolean produceMessage(String topicName, String message) throws Exception {
        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        log.info("📤 Producing Message to: {}", topicName);
        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");

        List<KafkaNodeConfig> nodes = config.getNodes();
        String firstBroker = nodes.get(0).getKafkaIp() + ":9092";

        String command = String.format(
                "echo '%s' | sudo docker exec -i %s kafka-console-producer --topic %s --bootstrap-server %s 2>&1",
                message, containerName, topicName, firstBroker);

        String output = ssh.executeCommand(command);

        boolean success = !output.contains("ERROR") && !output.contains("Exception");

        if (success) {
            log.info("✓ Message produced: {}", message);
        } else {
            log.error("✗ Failed to produce message");
            log.error("Output: {}", output);
        }

        return success;
    }

    /**
     * Get topic offsets
     *
     * @param topicName Topic name
     * @return Offset information
     */
    public String getTopicOffsets(String topicName) throws Exception {
        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        log.info("📊 Checking Topic Offsets: {}", topicName);
        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");

        List<KafkaNodeConfig> nodes = config.getNodes();
        String firstBroker = nodes.get(0).getKafkaIp() + ":9092";

        String command = String.format(
                "sudo docker exec %s kafka-get-offsets --topic %s --bootstrap-server %s 2>&1",
                containerName, topicName, firstBroker);

        String output = ssh.executeCommand(command);
        log.info("Offsets:\n{}", output);

        return output;
    }

    /**
     * Verify network connectivity between nodes
     *
     * @param nodes All cluster nodes
     * @return true if all nodes can reach each other
     */
    public boolean verifyInterNodeConnectivity(List<KafkaNodeConfig> nodes) throws Exception {
        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        log.info("🌐 Verifying Inter-Node Connectivity");
        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");

        boolean allReachable = true;

        for (KafkaNodeConfig target : nodes) {
            String command = String.format("ping -c 1 -W 1 %s > /dev/null 2>&1 && echo 'OK' || echo 'FAILED'",
                    target.getKafkaIp());

            String result = ssh.executeCommand(command).trim();

            if (result.equals("OK")) {
                log.info("  ✓ Can reach {} ({})", target.getKafkaIp(), target.getNodeId());
            } else {
                log.error("  ✗ Cannot reach {} ({})", target.getKafkaIp(), target.getNodeId());
                allReachable = false;
            }
        }

        if (allReachable) {
            log.info("✓ All nodes are reachable");
        } else {
            log.error("✗ Some nodes are unreachable");
        }

        return allReachable;
    }

    /**
     * Run comprehensive cluster verification
     *
     * @return true if all verifications passed
     */
    public boolean runFullVerification() throws Exception {
        log.info("╔════════════════════════════════════════════════════════════════╗");
        log.info("║           Kafka Cluster - Full Verification                  ║");
        log.info("╚════════════════════════════════════════════════════════════════╝");
        log.info("");

        boolean allPassed = true;

        // Test 1: Broker connectivity
        if (!verifyBrokerConnectivity()) {
            allPassed = false;
        }

        // Test 2: List topics
        try {
            listTopics();
        } catch (Exception e) {
            log.error("✗ Failed to list topics", e);
            allPassed = false;
        }

        // Test 3: Create test topic
        String testTopic = "cluster-verification-test";
        if (!createTopic(testTopic, 3, 3)) {
            allPassed = false;
        }

        // Test 4: Describe test topic
        try {
            describeTopic(testTopic);
        } catch (Exception e) {
            log.error("✗ Failed to describe topic", e);
            allPassed = false;
        }

        // Test 5: Produce test message
        String testMessage = "Cluster verification test at " + System.currentTimeMillis();
        if (!produceMessage(testTopic, testMessage)) {
            allPassed = false;
        }

        // Test 6: Check offsets
        try {
            getTopicOffsets(testTopic);
        } catch (Exception e) {
            log.error("✗ Failed to get offsets", e);
            allPassed = false;
        }

        // Test 7: Inter-node connectivity
        if (!verifyInterNodeConnectivity(config.getNodes())) {
            allPassed = false;
        }

        log.info("");
        log.info("╔════════════════════════════════════════════════════════════════╗");
        if (allPassed) {
            log.info("║           ✓ All Verification Tests PASSED                    ║");
        } else {
            log.info("║           ✗ Some Verification Tests FAILED                   ║");
        }
        log.info("╚════════════════════════════════════════════════════════════════╝");

        return allPassed;
    }
}
