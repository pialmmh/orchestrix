package com.telcobright.orchestrix.automation.cluster.perconadocker;

import com.telcobright.orchestrix.automation.cluster.kafkadocker.DockerDeploymentAutomation;
import com.telcobright.orchestrix.automation.config.PerconaConfig;
import com.telcobright.orchestrix.automation.config.PerconaConfig.PerconaSlaveConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * MySQL Replication Setup Automation
 *
 * Configures GTID-based master-slave replication for Percona MySQL 5.7.
 *
 * Steps:
 * 1. Create replication user on master
 * 2. Configure each slave to replicate from master
 * 3. Start replication on slaves
 * 4. Verify replication status
 *
 * Usage:
 * <pre>
 * ReplicationSetupAutomation repl = new ReplicationSetupAutomation(config);
 * repl.setupMaster(masterDockerAutomation);
 * repl.setupSlave(slaveDockerAutomation, slaveConfig);
 * </pre>
 *
 * @author TelcoBright Orchestrix Team
 * @since 2025-11-27
 */
public class ReplicationSetupAutomation {

    private static final Logger log = LoggerFactory.getLogger(ReplicationSetupAutomation.class);
    private final PerconaConfig config;

    public ReplicationSetupAutomation(PerconaConfig config) {
        this.config = config;
    }

    /**
     * Setup master node - create replication user
     *
     * @param docker Docker automation connected to master host
     * @param containerName Container name (e.g., "percona-master")
     */
    public void setupMaster(DockerDeploymentAutomation docker, String containerName) throws Exception {
        log.info("Setting up master replication user...");

        String replUser = config.getReplUser();
        String replPassword = config.getReplPassword();
        String rootPassword = config.getRootPassword();

        // Create replication user with access from any host in the overlay network
        // Use % for any host (10.10.% would be more restrictive)
        String createUserSql = String.format(
                "CREATE USER IF NOT EXISTS '%s'@'%%' IDENTIFIED BY '%s'",
                replUser, replPassword
        );

        String grantSql = String.format(
                "GRANT REPLICATION SLAVE, REPLICATION CLIENT ON *.* TO '%s'@'%%'",
                replUser
        );

        // Execute SQL on master
        executeMysql(docker, containerName, rootPassword, createUserSql);
        executeMysql(docker, containerName, rootPassword, grantSql);
        executeMysql(docker, containerName, rootPassword, "FLUSH PRIVILEGES");

        log.info("  Created replication user: {}", replUser);

        // Show master status
        String masterStatus = executeMysql(docker, containerName, rootPassword, "SHOW MASTER STATUS\\G");
        log.debug("Master status:\n{}", masterStatus);

        log.info("  Master setup complete");
    }

    /**
     * Setup slave node - configure replication to master
     *
     * @param docker Docker automation connected to slave host
     * @param containerName Container name (e.g., "percona-slave1")
     * @param slave Slave configuration
     */
    public void setupSlave(DockerDeploymentAutomation docker, String containerName,
                           PerconaSlaveConfig slave) throws Exception {
        log.info("Setting up slave {} replication...", slave.getSlaveIndex());

        String rootPassword = config.getRootPassword();
        String masterIp = config.getMaster().getMysqlIp();
        String replUser = config.getReplUser();
        String replPassword = config.getReplPassword();

        // Stop any existing replication
        try {
            executeMysql(docker, containerName, rootPassword, "STOP SLAVE");
        } catch (Exception e) {
            // Ignore - slave might not be started
            log.debug("Stop slave returned: {}", e.getMessage());
        }

        // Reset slave state
        try {
            executeMysql(docker, containerName, rootPassword, "RESET SLAVE ALL");
        } catch (Exception e) {
            log.debug("Reset slave returned: {}", e.getMessage());
        }

        // Configure replication using GTID auto-positioning
        String changeMasterSql = String.format(
                "CHANGE MASTER TO " +
                "MASTER_HOST='%s', " +
                "MASTER_PORT=3306, " +
                "MASTER_USER='%s', " +
                "MASTER_PASSWORD='%s', " +
                "MASTER_AUTO_POSITION=1",
                masterIp, replUser, replPassword
        );

        executeMysql(docker, containerName, rootPassword, changeMasterSql);
        log.info("  Configured slave to replicate from {}", masterIp);

        // Start replication
        executeMysql(docker, containerName, rootPassword, "START SLAVE");
        log.info("  Started replication");

        // Wait for replication to start
        Thread.sleep(2000);

        // Verify replication status
        String slaveStatus = executeMysql(docker, containerName, rootPassword, "SHOW SLAVE STATUS\\G");
        log.debug("Slave status:\n{}", slaveStatus);

        // Check for errors
        if (slaveStatus.contains("Slave_IO_Running: Yes") && slaveStatus.contains("Slave_SQL_Running: Yes")) {
            log.info("  Replication running successfully");
        } else if (slaveStatus.contains("Slave_IO_Running: Connecting")) {
            log.warn("  Replication IO thread connecting - may still be establishing connection");
        } else {
            log.warn("  Replication may have issues - check slave status");
        }
    }

    /**
     * Verify replication status on a slave
     *
     * @param docker Docker automation connected to slave host
     * @param containerName Container name
     * @return ReplicationStatus object
     */
    public ReplicationStatus verifyReplication(DockerDeploymentAutomation docker,
                                                String containerName) throws Exception {
        String rootPassword = config.getRootPassword();
        String status = executeMysql(docker, containerName, rootPassword, "SHOW SLAVE STATUS\\G");

        ReplicationStatus result = new ReplicationStatus();
        result.rawOutput = status;

        // Parse key fields
        result.ioRunning = status.contains("Slave_IO_Running: Yes");
        result.sqlRunning = status.contains("Slave_SQL_Running: Yes");

        // Extract seconds behind master
        if (status.contains("Seconds_Behind_Master:")) {
            String[] lines = status.split("\n");
            for (String line : lines) {
                if (line.contains("Seconds_Behind_Master:")) {
                    String value = line.split(":")[1].trim();
                    if (!"NULL".equals(value)) {
                        try {
                            result.secondsBehindMaster = Integer.parseInt(value);
                        } catch (NumberFormatException e) {
                            result.secondsBehindMaster = -1;
                        }
                    }
                    break;
                }
            }
        }

        // Extract error messages
        for (String line : status.split("\n")) {
            if (line.contains("Last_Error:") && !line.trim().endsWith(":")) {
                result.lastError = line.split(":", 2)[1].trim();
            }
            if (line.contains("Last_IO_Error:") && !line.trim().endsWith(":")) {
                result.lastIoError = line.split(":", 2)[1].trim();
            }
        }

        result.healthy = result.ioRunning && result.sqlRunning && result.secondsBehindMaster >= 0;

        return result;
    }

    /**
     * Get master binary log position
     */
    public MasterStatus getMasterStatus(DockerDeploymentAutomation docker,
                                         String containerName) throws Exception {
        String rootPassword = config.getRootPassword();
        String status = executeMysql(docker, containerName, rootPassword, "SHOW MASTER STATUS\\G");

        MasterStatus result = new MasterStatus();
        result.rawOutput = status;

        // Parse file and position
        for (String line : status.split("\n")) {
            if (line.contains("File:")) {
                result.file = line.split(":")[1].trim();
            }
            if (line.contains("Position:")) {
                try {
                    result.position = Long.parseLong(line.split(":")[1].trim());
                } catch (NumberFormatException e) {
                    result.position = 0;
                }
            }
            if (line.contains("Executed_Gtid_Set:")) {
                result.gtidSet = line.split(":")[1].trim();
            }
        }

        return result;
    }

    /**
     * Execute MySQL command inside container
     */
    private String executeMysql(DockerDeploymentAutomation docker, String containerName,
                                String password, String sql) throws Exception {
        // Escape quotes in SQL
        String escapedSql = sql.replace("'", "'\\''");

        String command = String.format(
                "mysql -u root -p'%s' -e \"%s\"",
                password, escapedSql
        );

        return docker.execInContainer(containerName, command);
    }

    /**
     * Replication status information
     */
    public static class ReplicationStatus {
        public boolean ioRunning;
        public boolean sqlRunning;
        public int secondsBehindMaster = -1;
        public String lastError = "";
        public String lastIoError = "";
        public boolean healthy;
        public String rawOutput;

        @Override
        public String toString() {
            return String.format("ReplicationStatus{io=%s, sql=%s, lag=%ds, healthy=%s}",
                    ioRunning, sqlRunning, secondsBehindMaster, healthy);
        }
    }

    /**
     * Master status information
     */
    public static class MasterStatus {
        public String file;
        public long position;
        public String gtidSet;
        public String rawOutput;

        @Override
        public String toString() {
            return String.format("MasterStatus{file=%s, pos=%d, gtid=%s}",
                    file, position, gtidSet);
        }
    }
}
