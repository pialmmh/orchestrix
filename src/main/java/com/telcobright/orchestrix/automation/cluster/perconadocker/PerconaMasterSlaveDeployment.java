package com.telcobright.orchestrix.automation.cluster.perconadocker;

import com.telcobright.orchestrix.automation.cluster.kafkadocker.DockerDeploymentAutomation;
import com.telcobright.orchestrix.automation.cluster.kafkadocker.NetworkConfigAutomation;
import com.telcobright.orchestrix.automation.config.PerconaConfig;
import com.telcobright.orchestrix.automation.config.PerconaConfig.PerconaMasterConfig;
import com.telcobright.orchestrix.automation.config.PerconaConfig.PerconaSlaveConfig;
import com.telcobright.orchestrix.automation.core.device.impl.RemoteSshDevice;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * Percona MySQL Master-Slave Deployment Orchestrator
 *
 * Coordinates deployment of a Percona MySQL 5.7 master-slave cluster.
 * Follows the same pattern as KafkaClusterDeployment for consistency.
 *
 * Deployment steps:
 * 1. Connect to all nodes via SSH
 * 2. Verify prerequisites (Docker, bridge)
 * 3. Configure network (add secondary IPs to bridge)
 * 4. Deploy master node
 * 5. Wait for master to initialize
 * 6. Setup replication user on master
 * 7. Deploy slave nodes
 * 8. Configure replication on slaves
 * 9. Verify replication health
 *
 * Usage:
 * <pre>
 * PerconaConfig config = new PerconaConfig("deployments/ks_network/percona");
 * PerconaMasterSlaveDeployment deployment = new PerconaMasterSlaveDeployment(config);
 * DeploymentResult result = deployment.deploy();
 * </pre>
 *
 * @author TelcoBright Orchestrix Team
 * @since 2025-11-27
 */
public class PerconaMasterSlaveDeployment {

    private static final Logger log = LoggerFactory.getLogger(PerconaMasterSlaveDeployment.class);

    private final PerconaConfig config;
    private final Map<String, RemoteSshDevice> connections = new HashMap<>();
    private final Map<String, DockerDeploymentAutomation> dockerAutomations = new HashMap<>();
    private final Map<String, NetworkConfigAutomation> networkAutomations = new HashMap<>();

    private static final String MASTER_CONTAINER = "percona-master";
    private static final String SLAVE_CONTAINER_PREFIX = "percona-slave";

    public PerconaMasterSlaveDeployment(PerconaConfig config) {
        this.config = config;
    }

    /**
     * Deploy complete Percona MySQL master-slave cluster
     */
    public DeploymentResult deploy() {
        log.info("╔════════════════════════════════════════════════════════════════╗");
        log.info("║      Percona MySQL 5.7 Master-Slave Deployment                ║");
        log.info("╚════════════════════════════════════════════════════════════════╝");
        log.info("");
        log.info("Cluster: {}", config.getClusterName());
        log.info("Image: {}", config.getMysqlImage());
        log.info("Master: {} (server-id={})", config.getMaster().getMysqlIp(), config.getMaster().getServerId());
        log.info("Slaves: {}", config.getSlaveCount());
        for (PerconaSlaveConfig slave : config.getSlaves()) {
            log.info("  - Slave {}: {} (server-id={})",
                    slave.getSlaveIndex(), slave.getMysqlIp(), slave.getServerId());
        }
        log.info("InnoDB Buffer Pool: {}", config.getInnodbBufferPoolSize());
        log.info("GTID Mode: {}", config.isGtidMode() ? "ON" : "OFF");
        log.info("");

        DeploymentResult result = new DeploymentResult();
        result.setClusterName(config.getClusterName());
        result.setMasterIp(config.getMaster().getMysqlIp());

        try {
            // Step 1: Connect to all nodes
            connectToAllNodes();

            // Step 2: Verify prerequisites
            verifyPrerequisites();

            // Step 3: Configure network
            configureNetwork();

            // Step 4: Deploy master
            deployMaster();

            // Step 5: Wait for master to initialize
            waitForMasterInit();

            // Step 6: Setup replication user on master
            setupMasterReplication();

            // Step 7: Deploy slaves
            deploySlaves();

            // Step 8: Setup replication on slaves
            setupSlaveReplication();

            // Step 9: Verify replication
            boolean healthy = verifyReplicationHealth();

            if (healthy) {
                result.setSuccess(true);
                result.setMessage("Master-slave cluster deployed successfully");
                printSuccessSummary();
            } else {
                result.setSuccess(false);
                result.setMessage("Cluster deployed but replication health check failed");
                log.warn("Cluster deployed but some health checks failed");
            }

        } catch (Exception e) {
            log.error("Deployment failed", e);
            result.setSuccess(false);
            result.setMessage("Deployment failed: " + e.getMessage());
            result.setException(e);
        } finally {
            disconnectAll();
        }

        return result;
    }

    /**
     * Connect to all nodes
     */
    private void connectToAllNodes() throws Exception {
        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        log.info("Connecting to Nodes");
        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");

        // Connect to master
        PerconaMasterConfig master = config.getMaster();
        log.info("Connecting to master ({}:{})...", master.getMgmtIp(), master.getSshPort());
        RemoteSshDevice masterSsh = new RemoteSshDevice(master.getMgmtIp(), master.getSshPort(), master.getSshUser());
        masterSsh.connect(master.getSshPassword());
        connections.put(master.getMgmtIp(), masterSsh);
        dockerAutomations.put(master.getMgmtIp(), new DockerDeploymentAutomation(masterSsh));
        networkAutomations.put(master.getMgmtIp(), new NetworkConfigAutomation(masterSsh));
        log.info("  Connected to master");

        // Connect to slaves
        for (PerconaSlaveConfig slave : config.getSlaves()) {
            log.info("Connecting to slave {} ({}:{})...",
                    slave.getSlaveIndex(), slave.getMgmtIp(), slave.getSshPort());
            RemoteSshDevice slaveSsh = new RemoteSshDevice(slave.getMgmtIp(), slave.getSshPort(), slave.getSshUser());
            slaveSsh.connect(slave.getSshPassword());
            connections.put(slave.getMgmtIp(), slaveSsh);
            dockerAutomations.put(slave.getMgmtIp(), new DockerDeploymentAutomation(slaveSsh));
            networkAutomations.put(slave.getMgmtIp(), new NetworkConfigAutomation(slaveSsh));
            log.info("  Connected to slave {}", slave.getSlaveIndex());
        }

        log.info("All nodes connected ({} total)", connections.size());
        log.info("");
    }

    /**
     * Verify prerequisites
     */
    private void verifyPrerequisites() throws Exception {
        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        log.info("Verifying Prerequisites");
        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");

        // Verify master
        PerconaMasterConfig master = config.getMaster();
        DockerDeploymentAutomation masterDocker = dockerAutomations.get(master.getMgmtIp());
        NetworkConfigAutomation masterNetwork = networkAutomations.get(master.getMgmtIp());

        String dockerVersion = masterDocker.verifyDocker();
        log.info("  [Master] Docker: {}", dockerVersion.trim());

        if (!masterNetwork.verifyBridgeExists(master.getBridgeName())) {
            throw new IllegalStateException("Bridge " + master.getBridgeName() + " not found on master");
        }
        log.info("  [Master] Bridge {} exists", master.getBridgeName());

        // Verify slaves
        for (PerconaSlaveConfig slave : config.getSlaves()) {
            DockerDeploymentAutomation slaveDocker = dockerAutomations.get(slave.getMgmtIp());
            NetworkConfigAutomation slaveNetwork = networkAutomations.get(slave.getMgmtIp());

            dockerVersion = slaveDocker.verifyDocker();
            log.info("  [Slave {}] Docker: {}", slave.getSlaveIndex(), dockerVersion.trim());

            if (!slaveNetwork.verifyBridgeExists(slave.getBridgeName())) {
                throw new IllegalStateException("Bridge " + slave.getBridgeName() +
                        " not found on slave " + slave.getSlaveIndex());
            }
            log.info("  [Slave {}] Bridge {} exists", slave.getSlaveIndex(), slave.getBridgeName());
        }

        log.info("Prerequisites verified");
        log.info("");
    }

    /**
     * Configure network - add secondary IPs
     */
    private void configureNetwork() throws Exception {
        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        log.info("Configuring Network");
        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");

        // Configure master IP
        PerconaMasterConfig master = config.getMaster();
        NetworkConfigAutomation masterNetwork = networkAutomations.get(master.getMgmtIp());
        masterNetwork.addSecondaryIp(master.getBridgeName(), master.getMysqlIp() + "/24");
        log.info("  [Master] {} configured on {}", master.getMysqlIp(), master.getBridgeName());

        // Configure slave IPs
        for (PerconaSlaveConfig slave : config.getSlaves()) {
            NetworkConfigAutomation slaveNetwork = networkAutomations.get(slave.getMgmtIp());
            slaveNetwork.addSecondaryIp(slave.getBridgeName(), slave.getMysqlIp() + "/24");
            log.info("  [Slave {}] {} configured on {}",
                    slave.getSlaveIndex(), slave.getMysqlIp(), slave.getBridgeName());
        }

        log.info("Network configured");
        log.info("");
    }

    /**
     * Deploy master node
     */
    private void deployMaster() throws Exception {
        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        log.info("Deploying Master");
        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");

        PerconaMasterConfig master = config.getMaster();
        DockerDeploymentAutomation docker = dockerAutomations.get(master.getMgmtIp());
        PerconaComposeGenerator generator = new PerconaComposeGenerator(config);

        // Create directories
        String workDir = "~/percona-master";
        docker.createWorkingDirectory(workDir);
        docker.createDataDirectory(config.getDataDir());

        // Generate and upload docker-compose.yml
        String yaml = generator.generateForMasterWithMetadata();
        docker.uploadDockerCompose(workDir, yaml);
        log.info("  docker-compose.yml uploaded");

        // Start container
        docker.startContainers(workDir);
        log.info("  Container started");

        log.info("Master deployed");
        log.info("");
    }

    /**
     * Wait for master to initialize
     */
    private void waitForMasterInit() throws Exception {
        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        log.info("Waiting for Master Initialization");
        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");

        PerconaMasterConfig master = config.getMaster();
        DockerDeploymentAutomation docker = dockerAutomations.get(master.getMgmtIp());

        // Wait for container to be running
        boolean running = docker.waitForContainer(MASTER_CONTAINER, 60);
        if (!running) {
            throw new IllegalStateException("Master container did not start");
        }
        log.info("  Container is running");

        // Wait for MySQL to be ready (check for mysqld process ready)
        log.info("  Waiting for MySQL to initialize (30 seconds)...");
        Thread.sleep(30000);

        // Try to connect
        int retries = 10;
        for (int i = 0; i < retries; i++) {
            try {
                String result = docker.execInContainer(MASTER_CONTAINER,
                        "mysql -u root -p'" + config.getRootPassword() + "' -e 'SELECT 1'");
                if (result.contains("1")) {
                    log.info("  MySQL is ready");
                    return;
                }
            } catch (Exception e) {
                log.debug("  MySQL not ready yet: {}", e.getMessage());
            }
            Thread.sleep(5000);
        }

        throw new IllegalStateException("MySQL did not become ready on master");
    }

    /**
     * Setup replication user on master
     */
    private void setupMasterReplication() throws Exception {
        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        log.info("Setting up Replication on Master");
        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");

        PerconaMasterConfig master = config.getMaster();
        DockerDeploymentAutomation docker = dockerAutomations.get(master.getMgmtIp());
        ReplicationSetupAutomation replication = new ReplicationSetupAutomation(config);

        replication.setupMaster(docker, MASTER_CONTAINER);
        log.info("Replication user created on master");
        log.info("");
    }

    /**
     * Deploy slave nodes
     */
    private void deploySlaves() throws Exception {
        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        log.info("Deploying Slaves");
        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");

        PerconaComposeGenerator generator = new PerconaComposeGenerator(config);

        for (PerconaSlaveConfig slave : config.getSlaves()) {
            log.info("Deploying slave {}...", slave.getSlaveIndex());

            DockerDeploymentAutomation docker = dockerAutomations.get(slave.getMgmtIp());

            // Create directories
            String workDir = "~/percona-slave" + slave.getSlaveIndex();
            docker.createWorkingDirectory(workDir);
            docker.createDataDirectory(config.getDataDir());

            // Generate and upload docker-compose.yml
            String yaml = generator.generateForSlaveWithMetadata(slave);
            docker.uploadDockerCompose(workDir, yaml);
            log.info("  docker-compose.yml uploaded");

            // Start container
            docker.startContainers(workDir);
            log.info("  Container started");

            // Wait for container
            String containerName = SLAVE_CONTAINER_PREFIX + slave.getSlaveIndex();
            boolean running = docker.waitForContainer(containerName, 60);
            if (!running) {
                throw new IllegalStateException("Slave " + slave.getSlaveIndex() + " container did not start");
            }
        }

        // Wait for slaves to initialize
        log.info("Waiting for slaves to initialize (30 seconds)...");
        Thread.sleep(30000);

        log.info("Slaves deployed");
        log.info("");
    }

    /**
     * Setup replication on slaves
     */
    private void setupSlaveReplication() throws Exception {
        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        log.info("Configuring Replication on Slaves");
        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");

        ReplicationSetupAutomation replication = new ReplicationSetupAutomation(config);

        for (PerconaSlaveConfig slave : config.getSlaves()) {
            DockerDeploymentAutomation docker = dockerAutomations.get(slave.getMgmtIp());
            String containerName = SLAVE_CONTAINER_PREFIX + slave.getSlaveIndex();

            replication.setupSlave(docker, containerName, slave);
            log.info("  Slave {} replication configured", slave.getSlaveIndex());
        }

        log.info("Replication configured");
        log.info("");
    }

    /**
     * Verify replication health
     */
    private boolean verifyReplicationHealth() throws Exception {
        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        log.info("Verifying Replication Health");
        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");

        boolean allHealthy = true;
        ReplicationSetupAutomation replication = new ReplicationSetupAutomation(config);

        for (PerconaSlaveConfig slave : config.getSlaves()) {
            DockerDeploymentAutomation docker = dockerAutomations.get(slave.getMgmtIp());
            String containerName = SLAVE_CONTAINER_PREFIX + slave.getSlaveIndex();

            ReplicationSetupAutomation.ReplicationStatus status =
                    replication.verifyReplication(docker, containerName);

            if (status.healthy) {
                log.info("  Slave {}: HEALTHY (IO: {}, SQL: {}, Lag: {}s)",
                        slave.getSlaveIndex(), status.ioRunning, status.sqlRunning, status.secondsBehindMaster);
            } else {
                log.warn("  Slave {}: UNHEALTHY (IO: {}, SQL: {}, Error: {})",
                        slave.getSlaveIndex(), status.ioRunning, status.sqlRunning, status.lastError);
                allHealthy = false;
            }
        }

        return allHealthy;
    }

    /**
     * Print success summary
     */
    private void printSuccessSummary() {
        log.info("");
        log.info("╔════════════════════════════════════════════════════════════════╗");
        log.info("║            Deployment Complete!                                ║");
        log.info("╚════════════════════════════════════════════════════════════════╝");
        log.info("");
        log.info("Cluster Summary:");
        log.info("  Cluster: {}", config.getClusterName());
        log.info("  Master: {}:3306 (server-id={})",
                config.getMaster().getMysqlIp(), config.getMaster().getServerId());
        for (PerconaSlaveConfig slave : config.getSlaves()) {
            log.info("  Slave {}: {}:3306 (server-id={})",
                    slave.getSlaveIndex(), slave.getMysqlIp(), slave.getServerId());
        }
        log.info("");
        log.info("Connection String (master):");
        log.info("  jdbc:mysql://{}:3306/<database>?useSSL=false",
                config.getMaster().getMysqlIp());
        log.info("");
        log.info("Root Password: {}", config.getRootPassword());
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
                log.warn("Error disconnecting from {}: {}", entry.getKey(), e.getMessage());
            }
        }
        connections.clear();
        dockerAutomations.clear();
        networkAutomations.clear();
    }

    /**
     * Deployment result
     */
    public static class DeploymentResult {
        private boolean success;
        private String message;
        private String clusterName;
        private String masterIp;
        private Exception exception;

        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }

        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }

        public String getClusterName() { return clusterName; }
        public void setClusterName(String clusterName) { this.clusterName = clusterName; }

        public String getMasterIp() { return masterIp; }
        public void setMasterIp(String masterIp) { this.masterIp = masterIp; }

        public Exception getException() { return exception; }
        public void setException(Exception exception) { this.exception = exception; }

        @Override
        public String toString() {
            return "DeploymentResult{" +
                    "success=" + success +
                    ", cluster=" + clusterName +
                    ", master=" + masterIp +
                    ", message='" + message + '\'' +
                    '}';
        }
    }
}
