package com.telcobright.orchestrix.automation.example;

import com.telcobright.orchestrix.automation.config.PerconaConfig;
import com.telcobright.orchestrix.automation.core.device.impl.RemoteSshDevice;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Set up MySQL GTID-based replication between master and slave
 *
 * Prerequisites:
 * - Master running on 10.10.199.10
 * - Slave running on 10.10.198.10
 * - Both have GTID mode enabled
 * - SSH credentials configured in cluster-config.conf
 */
public class SetupPerconaReplication {
    private static final Logger log = LoggerFactory.getLogger(SetupPerconaReplication.class);

    private static PerconaConfig config;
    private static String MASTER_MGMT_IP;
    private static String SLAVE_MGMT_IP;
    private static String MASTER_MYSQL_IP;
    private static String SLAVE_MYSQL_IP;
    private static String MYSQL_ROOT_PASSWORD;
    private static String MASTER_SSH_USER;
    private static String MASTER_SSH_PASSWORD;
    private static String SLAVE_SSH_USER;
    private static String SLAVE_SSH_PASSWORD;

    public static void main(String[] args) {
        log.info("╔════════════════════════════════════════════════════════════════╗");
        log.info("║       Setting up Percona MySQL GTID-based Replication         ║");
        log.info("╚════════════════════════════════════════════════════════════════╝");

        try {
            // Load configuration from cluster-config.conf
            config = new PerconaConfig("deployments/ks_network/percona");

            MASTER_MGMT_IP = config.getMaster().getMgmtIp();
            MASTER_MYSQL_IP = config.getMaster().getMysqlIp();
            MASTER_SSH_USER = config.getMaster().getSshUser();
            MASTER_SSH_PASSWORD = config.getMaster().getSshPassword();

            SLAVE_MGMT_IP = config.getSlaves().get(0).getMgmtIp();
            SLAVE_MYSQL_IP = config.getSlaves().get(0).getMysqlIp();
            SLAVE_SSH_USER = config.getSlaves().get(0).getSshUser();
            SLAVE_SSH_PASSWORD = config.getSlaves().get(0).getSshPassword();

            MYSQL_ROOT_PASSWORD = config.getRootPassword();

            log.info("Loaded configuration from cluster-config.conf");
            log.info("Master: {} ({})", MASTER_MGMT_IP, MASTER_MYSQL_IP);
            log.info("Slave: {} ({})", SLAVE_MGMT_IP, SLAVE_MYSQL_IP);
            // Step 1: Fix slave root user permissions (allow from any host)
            log.info("\n═══ Step 1: Fix slave MySQL root permissions ═══");
            fixSlaveRootPermissions();

            // Step 2: Create replication user on master
            log.info("\n═══ Step 2: Create replication user on master ═══");
            createReplicationUser();

            // Step 3: Configure slave to replicate from master
            log.info("\n═══ Step 3: Configure slave replication ═══");
            configureSlaveReplication();

            // Step 4: Verify replication
            log.info("\n═══ Step 4: Verify replication status ═══");
            verifyReplication();

            log.info("\n╔════════════════════════════════════════════════════════════════╗");
            log.info("║       Replication setup complete!                              ║");
            log.info("╚════════════════════════════════════════════════════════════════╝");

        } catch (Exception e) {
            log.error("Replication setup failed: {}", e.getMessage(), e);
        }
    }

    private static void fixSlaveRootPermissions() throws Exception {
        RemoteSshDevice device = new RemoteSshDevice(SLAVE_MGMT_IP, 22, SLAVE_SSH_USER);
        try {
            device.connect(SLAVE_SSH_PASSWORD);
            log.info("Connected to slave node");

            // Connect via socket (localhost) - use localhost/socket explicitly
            // First try with password via socket
            // Create root@'%' and network-specific root users
            String grantCmd = String.format(
                "sudo docker exec percona-slave1 mysql -S /var/lib/mysql/mysql.sock -uroot -p'%s' -e \""
                + "CREATE USER IF NOT EXISTS 'root'@'%%' IDENTIFIED BY '%s'; "
                + "GRANT ALL PRIVILEGES ON *.* TO 'root'@'%%' WITH GRANT OPTION; "
                // Add root access from container and overlay networks
                + "CREATE USER IF NOT EXISTS 'root'@'10.10.%%' IDENTIFIED BY '%s'; "
                + "GRANT ALL PRIVILEGES ON *.* TO 'root'@'10.10.%%' WITH GRANT OPTION; "
                + "CREATE USER IF NOT EXISTS 'root'@'10.9.%%' IDENTIFIED BY '%s'; "
                + "GRANT ALL PRIVILEGES ON *.* TO 'root'@'10.9.%%' WITH GRANT OPTION; "
                + "FLUSH PRIVILEGES;\" 2>&1",
                MYSQL_ROOT_PASSWORD, MYSQL_ROOT_PASSWORD, MYSQL_ROOT_PASSWORD, MYSQL_ROOT_PASSWORD);

            String result = device.executeCommand(grantCmd);
            log.info("Grant result: {}", result);

            // If that fails, try with -hlocalhost
            if (result.contains("Access denied")) {
                log.info("Trying with -hlocalhost...");
                grantCmd = String.format(
                    "sudo docker exec percona-slave1 mysql -hlocalhost -uroot -p'%s' -e \""
                    + "CREATE USER IF NOT EXISTS 'root'@'%%' IDENTIFIED BY '%s'; "
                    + "GRANT ALL PRIVILEGES ON *.* TO 'root'@'%%' WITH GRANT OPTION; "
                    + "FLUSH PRIVILEGES;\" 2>&1",
                    MYSQL_ROOT_PASSWORD, MYSQL_ROOT_PASSWORD);
                result = device.executeCommand(grantCmd);
                log.info("Grant result (localhost): {}", result);
            }

            // Verify we can now connect via TCP
            String testCmd = String.format(
                "sudo docker exec percona-slave1 mysql -h%s -uroot -p'%s' -e 'SELECT 1 AS test' 2>&1",
                SLAVE_MYSQL_IP, MYSQL_ROOT_PASSWORD);
            String testResult = device.executeCommand(testCmd);

            if (testResult.contains("test") && testResult.contains("1")) {
                log.info("Slave MySQL TCP connection verified!");
            } else {
                log.warn("Slave MySQL test result: {}", testResult);
            }

            device.disconnect();
        } catch (Exception e) {
            device.disconnect();
            throw e;
        }
    }

    private static void createReplicationUser() throws Exception {
        RemoteSshDevice device = new RemoteSshDevice(MASTER_MGMT_IP, 22, MASTER_SSH_USER);
        try {
            device.connect(MASTER_SSH_PASSWORD);
            log.info("Connected to master node");

            // Create replication user and network-based root access
            String createUserCmd = String.format(
                "sudo docker exec percona-master mysql -h%s -uroot -p'%s' -e \""
                + "CREATE USER IF NOT EXISTS 'repl'@'%%' IDENTIFIED BY 'ReplPass!2025'; "
                + "GRANT REPLICATION SLAVE, REPLICATION CLIENT ON *.* TO 'repl'@'%%'; "
                // Add root access from container and overlay networks
                + "CREATE USER IF NOT EXISTS 'root'@'10.10.%%' IDENTIFIED BY '%s'; "
                + "GRANT ALL PRIVILEGES ON *.* TO 'root'@'10.10.%%' WITH GRANT OPTION; "
                + "CREATE USER IF NOT EXISTS 'root'@'10.9.%%' IDENTIFIED BY '%s'; "
                + "GRANT ALL PRIVILEGES ON *.* TO 'root'@'10.9.%%' WITH GRANT OPTION; "
                + "FLUSH PRIVILEGES;\" 2>&1",
                MASTER_MYSQL_IP, MYSQL_ROOT_PASSWORD, MYSQL_ROOT_PASSWORD, MYSQL_ROOT_PASSWORD);

            String result = device.executeCommand(createUserCmd);
            log.info("Create replication user result: {}", result);

            // Verify master status
            String statusCmd = String.format(
                "sudo docker exec percona-master mysql -h%s -uroot -p'%s' -e 'SHOW MASTER STATUS\\G' 2>&1",
                MASTER_MYSQL_IP, MYSQL_ROOT_PASSWORD);
            String statusResult = device.executeCommand(statusCmd);
            log.info("Master status:\n{}", statusResult);

            device.disconnect();
        } catch (Exception e) {
            device.disconnect();
            throw e;
        }
    }

    private static void configureSlaveReplication() throws Exception {
        RemoteSshDevice device = new RemoteSshDevice(SLAVE_MGMT_IP, 22, SLAVE_SSH_USER);
        try {
            device.connect(SLAVE_SSH_PASSWORD);
            log.info("Connected to slave node");

            // Stop any existing replication
            String stopCmd = String.format(
                "sudo docker exec percona-slave1 mysql -h%s -uroot -p'%s' -e 'STOP SLAVE;' 2>&1",
                SLAVE_MYSQL_IP, MYSQL_ROOT_PASSWORD);
            device.executeCommand(stopCmd);

            // Reset slave
            String resetCmd = String.format(
                "sudo docker exec percona-slave1 mysql -h%s -uroot -p'%s' -e 'RESET SLAVE ALL;' 2>&1",
                SLAVE_MYSQL_IP, MYSQL_ROOT_PASSWORD);
            device.executeCommand(resetCmd);

            // Configure GTID-based replication
            String changeCmd = String.format(
                "sudo docker exec percona-slave1 mysql -h%s -uroot -p'%s' -e \""
                + "CHANGE MASTER TO "
                + "MASTER_HOST='%s', "
                + "MASTER_PORT=3306, "
                + "MASTER_USER='repl', "
                + "MASTER_PASSWORD='ReplPass!2025', "
                + "MASTER_AUTO_POSITION=1;\" 2>&1",
                SLAVE_MYSQL_IP, MYSQL_ROOT_PASSWORD, MASTER_MYSQL_IP);

            String changeResult = device.executeCommand(changeCmd);
            log.info("Change master result: {}", changeResult);

            // Start slave
            String startCmd = String.format(
                "sudo docker exec percona-slave1 mysql -h%s -uroot -p'%s' -e 'START SLAVE;' 2>&1",
                SLAVE_MYSQL_IP, MYSQL_ROOT_PASSWORD);
            String startResult = device.executeCommand(startCmd);
            log.info("Start slave result: {}", startResult);

            device.disconnect();
        } catch (Exception e) {
            device.disconnect();
            throw e;
        }
    }

    private static void verifyReplication() throws Exception {
        // Wait for replication to start
        Thread.sleep(2000);

        RemoteSshDevice device = new RemoteSshDevice(SLAVE_MGMT_IP, 22, SLAVE_SSH_USER);
        try {
            device.connect(SLAVE_SSH_PASSWORD);
            log.info("Checking slave replication status...");

            String statusCmd = String.format(
                "sudo docker exec percona-slave1 mysql -h%s -uroot -p'%s' -e 'SHOW SLAVE STATUS\\G' 2>&1",
                SLAVE_MYSQL_IP, MYSQL_ROOT_PASSWORD);
            String statusResult = device.executeCommand(statusCmd);

            // Parse important status fields
            if (statusResult.contains("Slave_IO_Running: Yes") &&
                statusResult.contains("Slave_SQL_Running: Yes")) {
                log.info("✓ Replication is RUNNING!");
                log.info("  Slave_IO_Running: Yes");
                log.info("  Slave_SQL_Running: Yes");
            } else {
                log.warn("Replication may not be running properly.");
                log.info("Full slave status:\n{}", statusResult);
            }

            // Also check GTID executed
            String gtidCmd = String.format(
                "sudo docker exec percona-slave1 mysql -h%s -uroot -p'%s' -e 'SELECT @@GLOBAL.gtid_executed;' 2>&1",
                SLAVE_MYSQL_IP, MYSQL_ROOT_PASSWORD);
            String gtidResult = device.executeCommand(gtidCmd);
            log.info("Executed GTID:\n{}", gtidResult);

            device.disconnect();
        } catch (Exception e) {
            device.disconnect();
            throw e;
        }
    }
}
