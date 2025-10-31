package com.telcobright.orchestrix.automation.cluster.kafkazookeeper;

import com.telcobright.orchestrix.automation.core.device.SshDevice;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.*;

/**
 * Main orchestration class for deploying Kafka + Zookeeper cluster
 * 
 * Deployment workflow:
 * 1. Parse master config file (link3/cluster.conf)
 * 2. Connect to all servers via SSH
 * 3. Cleanup existing containers (if configured)
 * 4. Generate and upload container configs
 * 5. Deploy Zookeeper ensemble (3 nodes)
 * 6. Deploy Kafka cluster (3 brokers)
 * 7. Apply BTRFS quotas to Kafka containers
 * 8. Verify cluster formation
 */
public class KafkaZookeeperClusterDeployment {

    private static final Logger log = LoggerFactory.getLogger(KafkaZookeeperClusterDeployment.class);

    private final String configFilePath;
    private final ClusterConfigParser configParser;
    private final Map<String, SshDevice> sshConnections;
    private final String orchestrixPath;

    public KafkaZookeeperClusterDeployment(String configFilePath) throws IOException {
        this.configFilePath = configFilePath;
        this.configParser = new ClusterConfigParser(configFilePath);
        this.sshConnections = new HashMap<>();
        this.orchestrixPath = configParser.getProperty("ORCHESTRIX_PATH", "/home/user/orchestrix");
    }

    /**
     * Main deployment entry point
     */
    public ClusterDeploymentResult deploy() throws Exception {
        log.info("==========================================");
        log.info("  Kafka + Zookeeper Cluster Deployment");
        log.info("==========================================");
        log.info("Config: {}", configFilePath);
        log.info("");

        ClusterDeploymentResult result = new ClusterDeploymentResult();

        try {
            // Step 1: Connect to all servers
            connectToServers();

            // Step 2: Cleanup existing containers (if enabled)
            if (Boolean.parseBoolean(configParser.getProperty("CLEANUP_EXISTING", "true"))) {
                cleanupExistingContainers();
            }

            // Step 3: Deploy Zookeeper ensemble
            List<ContainerConfig> zkConfigs = configParser.parseZookeeperConfigs();
            deployZookeeperEnsemble(zkConfigs);
            result.setZookeeperNodes(zkConfigs);

            // Step 4: Deploy Kafka cluster
            List<ContainerConfig> kafkaConfigs = configParser.parseKafkaConfigs();
            deployKafkaCluster(kafkaConfigs);
            result.setKafkaBrokers(kafkaConfigs);

            // Step 5: Verify cluster
            if (Boolean.parseBoolean(configParser.getProperty("VERIFY_CLUSTER", "true"))) {
                verifyCluster(zkConfigs, kafkaConfigs);
            }

            result.setSuccess(true);
            result.setMessage("Cluster deployed successfully");

            log.info("");
            log.info("==========================================");
            log.info("  Deployment Complete!");
            log.info("==========================================");
            log.info("Zookeeper Ensemble: {}", getZookeeperConnectString(zkConfigs));
            log.info("Kafka Bootstrap: {}", getKafkaBootstrapString(kafkaConfigs));
            log.info("");

            return result;

        } catch (Exception e) {
            log.error("Deployment failed", e);
            result.setSuccess(false);
            result.setMessage("Deployment failed: " + e.getMessage());
            throw e;
        } finally {
            // Close all SSH connections
            disconnectAll();
        }
    }

    /**
     * Connect to all servers via SSH
     */
    private void connectToServers() throws Exception {
        log.info("Connecting to servers...");

        List<ContainerConfig> allConfigs = new ArrayList<>();
        allConfigs.addAll(configParser.parseZookeeperConfigs());
        allConfigs.addAll(configParser.parseKafkaConfigs());

        Set<String> uniqueServers = new HashSet<>();
        for (ContainerConfig config : allConfigs) {
            uniqueServers.add(config.getServer().getHost());
        }

        for (ContainerConfig config : allConfigs) {
            ServerConfig server = config.getServer();
            String serverKey = server.getHost();

            if (!sshConnections.containsKey(serverKey)) {
                log.info("  Connecting to {}:{}...", server.getHost(), server.getSshPort());

                SshDevice device = new SshDevice(serverKey);
                boolean connected = device.connect(
                        server.getHost(),
                        server.getSshPort(),
                        server.getSshUser(),
                        server.getSshPassword()
                ).get();

                if (!connected) {
                    throw new RuntimeException("Failed to connect to " + serverKey);
                }

                sshConnections.put(serverKey, device);
                log.info("  ✓ Connected to {}", serverKey);
            }
        }

        log.info("✓ All servers connected");
        log.info("");
    }

    /**
     * Cleanup existing Kafka and Zookeeper containers
     */
    private void cleanupExistingContainers() throws Exception {
        log.info("Cleaning up existing containers...");

        List<ContainerConfig> allConfigs = new ArrayList<>();
        allConfigs.addAll(configParser.parseZookeeperConfigs());
        allConfigs.addAll(configParser.parseKafkaConfigs());

        for (ContainerConfig config : allConfigs) {
            SshDevice device = getSshDevice(config);
            String containerName = config.getContainerName();

            try {
                // Stop and delete container
                device.executeSudo("lxc stop " + containerName + " --force", config.getServer().getSshPassword()).get();
                device.executeSudo("lxc delete " + containerName + " --force", config.getServer().getSshPassword()).get();
                log.info("  ✓ Cleaned up {}", containerName);
            } catch (Exception e) {
                // Container might not exist, ignore
                log.debug("  Container {} not found or already deleted", containerName);
            }
        }

        log.info("✓ Cleanup complete");
        log.info("");
    }

    /**
     * Deploy Zookeeper ensemble (3 nodes)
     */
    private void deployZookeeperEnsemble(List<ContainerConfig> configs) throws Exception {
        log.info("Deploying Zookeeper Ensemble ({} nodes)...", configs.size());

        for (ContainerConfig config : configs) {
            deployContainer(config);
        }

        // Wait for ensemble to form
        log.info("  Waiting for ensemble formation...");
        Thread.sleep(8000);

        log.info("✓ Zookeeper ensemble deployed");
        log.info("");
    }

    /**
     * Deploy Kafka cluster (3 brokers)
     */
    private void deployKafkaCluster(List<ContainerConfig> configs) throws Exception {
        log.info("Deploying Kafka Cluster ({} brokers)...", configs.size());

        for (ContainerConfig config : configs) {
            deployContainer(config);

            // Apply BTRFS quota for Kafka containers
            String quota = config.getProperty("BTRFS_QUOTA");
            if (quota != null && !quota.isEmpty()) {
                applyBtrfsQuota(config, quota);
            }
        }

        // Wait for brokers to register
        log.info("  Waiting for brokers to register...");
        Thread.sleep(10000);

        log.info("✓ Kafka cluster deployed");
        log.info("");
    }

    /**
     * Deploy a single container (Zookeeper or Kafka)
     */
    private void deployContainer(ContainerConfig config) throws Exception {
        SshDevice device = getSshDevice(config);
        String sudoPassword = config.getServer().getSshPassword();

        log.info("  Deploying {}...", config.getContainerName());

        // Launch container from base image
        String baseImage = config.getProperty("BASE_IMAGE");
        String containerName = config.getContainerName();

        device.executeSudo(String.format("lxc launch %s %s", baseImage, containerName), sudoPassword).get();
        log.debug("Container {} launched from {}", containerName, baseImage);

        // Wait for container to start
        Thread.sleep(3000);

        // Configure port forwarding (LXC proxy devices)
        String hostIp = config.getStaticIp();  // Now contains host IP
        configurePortForwarding(device, config, sudoPassword, hostIp);

        // Configure the service based on type
        if (config.getType() == ContainerConfig.ContainerType.ZOOKEEPER) {
            configureZookeeper(device, config, sudoPassword);
        } else {
            configureKafka(device, config, sudoPassword);
        }

        log.info("  ✓ {} deployed with host IP {}", containerName, hostIp);
    }

    /**
     * Configure port forwarding using LXC proxy devices
     */
    private void configurePortForwarding(SshDevice device, ContainerConfig config, String sudoPassword, String hostIp) throws Exception {
        String containerName = config.getContainerName();

        if (config.getType() == ContainerConfig.ContainerType.ZOOKEEPER) {
            // Zookeeper needs 3 ports: 2181 (client), 2888 (peer), 3888 (leader election)
            String clientPort = config.getProperty("CLIENT_PORT", "2181");
            String peerPort = config.getProperty("PEER_PORT", "2888");
            String leaderPort = config.getProperty("LEADER_PORT", "3888");

            // Add proxy devices for each port
            device.executeSudo(String.format(
                "lxc config device add %s proxy-client proxy listen=tcp:%s:%s connect=tcp:127.0.0.1:%s bind=host",
                containerName, hostIp, clientPort, clientPort), sudoPassword).get();

            device.executeSudo(String.format(
                "lxc config device add %s proxy-peer proxy listen=tcp:%s:%s connect=tcp:127.0.0.1:%s bind=host",
                containerName, hostIp, peerPort, peerPort), sudoPassword).get();

            device.executeSudo(String.format(
                "lxc config device add %s proxy-leader proxy listen=tcp:%s:%s connect=tcp:127.0.0.1:%s bind=host",
                containerName, hostIp, leaderPort, leaderPort), sudoPassword).get();

            log.debug("Port forwarding configured for Zookeeper {} - ports {},{},{}", containerName, clientPort, peerPort, leaderPort);
        } else {
            // Kafka needs port 9092
            String kafkaPort = config.getProperty("PORT", "9092");

            device.executeSudo(String.format(
                "lxc config device add %s proxy-kafka proxy listen=tcp:%s:%s connect=tcp:127.0.0.1:%s bind=host",
                containerName, hostIp, kafkaPort, kafkaPort), sudoPassword).get();

            log.debug("Port forwarding configured for Kafka {} - port {}", containerName, kafkaPort);
        }
    }

    /**
     * Configure Zookeeper inside the container
     */
    private void configureZookeeper(SshDevice device, ContainerConfig config, String sudoPassword) throws Exception {
        String containerName = config.getContainerName();

        // Convert semicolon-separated ZOO_SERVERS to newline-separated format
        String zooServers = config.getProperty("ZOO_SERVERS", "");
        String formattedZooServers = zooServers.replace(";", "\n");

        // Create runtime configuration
        String runtimeConfig = String.format(
            "SERVER_ID=%s\n" +
            "CLIENT_PORT=%s\n" +
            "PEER_PORT=%s\n" +
            "LEADER_PORT=%s\n" +
            "DATA_DIR=%s\n" +
            "ZOO_HEAP_OPTS=\"%s\"\n" +
            "ZOO_SERVERS=\"%s\"",
            config.getProperty("SERVER_ID"),
            config.getProperty("CLIENT_PORT", "2181"),
            config.getProperty("PEER_PORT", "2888"),
            config.getProperty("LEADER_PORT", "3888"),
            config.getProperty("DATA_DIR", "/data/zookeeper"),
            config.getProperty("HEAP_OPTS", "-Xms512M -Xmx512M"),
            formattedZooServers
        );

        String configCmd = String.format(
            "sudo lxc exec %s -- bash -c 'cat > /etc/zookeeper/runtime.conf << \"EOF\"\n%s\nEOF\n'",
            containerName, runtimeConfig
        );

        device.executeSudo(configCmd, sudoPassword).get();

        // Enable and start service
        device.executeSudo(String.format("lxc exec %s -- systemctl enable zookeeper", containerName), sudoPassword).get();
        device.executeSudo(String.format("lxc exec %s -- systemctl start zookeeper", containerName), sudoPassword).get();

        log.debug("Zookeeper service started in {}", containerName);
    }

    /**
     * Configure Kafka inside the container
     */
    private void configureKafka(SshDevice device, ContainerConfig config, String sudoPassword) throws Exception {
        String containerName = config.getContainerName();

        // Create runtime configuration
        String runtimeConfig = String.format(
            "BROKER_ID=%s\n" +
            "ZOOKEEPER_CONNECT=%s\n" +
            "LISTENERS=%s\n" +
            "ADVERTISED_LISTENERS=%s\n" +
            "LOG_DIRS=%s\n" +
            "KAFKA_HEAP_OPTS=\"%s\"",
            config.getProperty("BROKER_ID"),
            config.getProperty("ZOOKEEPER_CONNECT"),
            config.getProperty("LISTENERS", "PLAINTEXT://0.0.0.0:9092"),
            config.getProperty("ADVERTISED_LISTENERS"),
            config.getProperty("LOG_DIRS", "/data/kafka-logs"),
            config.getProperty("HEAP_OPTS", "-Xms1G -Xmx1G")
        );

        String configCmd = String.format(
            "sudo lxc exec %s -- bash -c 'cat > /etc/kafka/runtime.conf << \"EOF\"\n%s\nEOF\n'",
            containerName, runtimeConfig
        );

        device.executeSudo(configCmd, sudoPassword).get();

        // Enable and start service
        device.executeSudo(String.format("lxc exec %s -- systemctl enable kafka", containerName), sudoPassword).get();
        device.executeSudo(String.format("lxc exec %s -- systemctl start kafka", containerName), sudoPassword).get();

        log.debug("Kafka service started in {}", containerName);
    }

    /**
     * Apply BTRFS quota to a Kafka container
     */
    private void applyBtrfsQuota(ContainerConfig config, String quota) throws Exception {
        log.info("  Applying BTRFS quota {} to {}...", quota, config.getContainerName());

        SshDevice device = getSshDevice(config);
        String containerName = config.getContainerName();
        String sudoPassword = config.getServer().getSshPassword();

        // Get container's storage path
        String storagePath = device.executeCommand(
                "lxc config get " + containerName + " volatile.root.host_path"
        ).trim();

        if (storagePath.isEmpty()) {
            log.warn("  Could not determine storage path for {}, skipping quota", containerName);
            return;
        }

        // Apply quota
        String quotaCommand = String.format(
                "btrfs qgroup limit %s %s",
                quota,
                storagePath
        );

        device.executeSudo(quotaCommand, sudoPassword).get();
        log.info("  ✓ Quota applied to {}", containerName);
    }

    /**
     * Verify cluster formation
     */
    private void verifyCluster(List<ContainerConfig> zkConfigs, List<ContainerConfig> kafkaConfigs) throws Exception {
        log.info("Verifying cluster...");

        // Verify Zookeeper ensemble
        for (ContainerConfig config : zkConfigs) {
            SshDevice device = getSshDevice(config);
            try {
                String status = device.executeCommand(
                        "lxc exec " + config.getContainerName() +
                                " -- /opt/zookeeper/bin/zkServer.sh status"
                );

                if (status.contains("Mode: leader") || status.contains("Mode: follower")) {
                    log.info("  ✓ {} is healthy ({})", config.getContainerName(),
                            status.contains("leader") ? "leader" : "follower");
                } else {
                    log.warn("  ⚠ {} status unclear", config.getContainerName());
                }
            } catch (Exception e) {
                log.warn("  ⚠ Could not verify {}: {}", config.getContainerName(), e.getMessage());
            }
        }

        // Verify Kafka brokers
        for (ContainerConfig config : kafkaConfigs) {
            SshDevice device = getSshDevice(config);
            try {
                String status = device.executeCommand(
                        "lxc exec " + config.getContainerName() + " -- systemctl is-active kafka"
                ).trim();

                if ("active".equals(status)) {
                    log.info("  ✓ {} is running", config.getContainerName());
                } else {
                    log.warn("  ⚠ {} service status: {}", config.getContainerName(), status);
                }
            } catch (Exception e) {
                log.warn("  ⚠ Could not verify {}: {}", config.getContainerName(), e.getMessage());
            }
        }

        log.info("✓ Verification complete");
        log.info("");
    }

    /**
     * Get SSH device for a container's server
     */
    private SshDevice getSshDevice(ContainerConfig config) {
        String serverKey = config.getServer().getHost();
        SshDevice device = sshConnections.get(serverKey);
        if (device == null) {
            throw new RuntimeException("No SSH connection for server: " + serverKey);
        }
        return device;
    }

    /**
     * Generate Zookeeper connection string
     */
    private String getZookeeperConnectString(List<ContainerConfig> zkConfigs) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < zkConfigs.size(); i++) {
            if (i > 0) sb.append(",");
            ContainerConfig config = zkConfigs.get(i);
            sb.append(config.getStaticIp()).append(":").append(config.getProperty("CLIENT_PORT"));
        }
        return sb.toString();
    }

    /**
     * Generate Kafka bootstrap servers string
     */
    private String getKafkaBootstrapString(List<ContainerConfig> kafkaConfigs) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < kafkaConfigs.size(); i++) {
            if (i > 0) sb.append(",");
            ContainerConfig config = kafkaConfigs.get(i);
            sb.append(config.getStaticIp()).append(":9092");
        }
        return sb.toString();
    }

    /**
     * Disconnect all SSH connections
     */
    private void disconnectAll() {
        for (Map.Entry<String, SshDevice> entry : sshConnections.entrySet()) {
            try {
                entry.getValue().disconnect();
                log.debug("Disconnected from {}", entry.getKey());
            } catch (Exception e) {
                log.warn("Error disconnecting from {}: {}", entry.getKey(), e.getMessage());
            }
        }
        sshConnections.clear();
    }

    /**
     * Main method for command-line usage
     */
    public static void main(String[] args) {
        if (args.length < 1) {
            System.out.println("Usage: KafkaZookeeperClusterDeployment <config-file>");
            System.out.println("");
            System.out.println("Example:");
            System.out.println("  KafkaZookeeperClusterDeployment /path/to/link3/cluster.conf");
            System.exit(1);
        }

        String configFile = args[0];

        try {
            KafkaZookeeperClusterDeployment deployment = new KafkaZookeeperClusterDeployment(configFile);
            ClusterDeploymentResult result = deployment.deploy();

            if (result.isSuccess()) {
                System.out.println("\n✓ Deployment successful!");
                System.out.println("\nConnection strings:");
                System.out.println("  Zookeeper: " + deployment.getZookeeperConnectString(result.getZookeeperNodes()));
                System.out.println("  Kafka: " + deployment.getKafkaBootstrapString(result.getKafkaBrokers()));
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

    /**
     * Result object for deployment
     */
    public static class ClusterDeploymentResult {
        private boolean success;
        private String message;
        private List<ContainerConfig> zookeeperNodes;
        private List<ContainerConfig> kafkaBrokers;

        public boolean isSuccess() {
            return success;
        }

        public void setSuccess(boolean success) {
            this.success = success;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }

        public List<ContainerConfig> getZookeeperNodes() {
            return zookeeperNodes;
        }

        public void setZookeeperNodes(List<ContainerConfig> zookeeperNodes) {
            this.zookeeperNodes = zookeeperNodes;
        }

        public List<ContainerConfig> getKafkaBrokers() {
            return kafkaBrokers;
        }

        public void setKafkaBrokers(List<ContainerConfig> kafkaBrokers) {
            this.kafkaBrokers = kafkaBrokers;
        }
    }
}
