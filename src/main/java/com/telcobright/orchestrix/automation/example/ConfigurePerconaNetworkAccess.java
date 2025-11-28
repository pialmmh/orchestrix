package com.telcobright.orchestrix.automation.example;

import com.telcobright.orchestrix.automation.config.PerconaConfig;
import com.telcobright.orchestrix.automation.core.device.impl.RemoteSshDevice;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Configure MySQL root access from container and overlay network subnets
 * - 10.10.0.0/16: Container subnet (lxdbr0 IPs)
 * - 10.9.0.0/16: WireGuard overlay network
 *
 * Reads configuration from deployments/ks_network/percona/cluster-config.conf
 */
public class ConfigurePerconaNetworkAccess {
    private static final Logger log = LoggerFactory.getLogger(ConfigurePerconaNetworkAccess.class);

    private static PerconaConfig config;
    private static String MASTER_MGMT_IP;
    private static String MASTER_MYSQL_IP;
    private static String MASTER_SSH_USER;
    private static String MASTER_SSH_PASSWORD;
    private static String SLAVE_MGMT_IP;
    private static String SLAVE_MYSQL_IP;
    private static String SLAVE_SSH_USER;
    private static String SLAVE_SSH_PASSWORD;
    private static String MYSQL_ROOT_PASSWORD;

    public static void main(String[] args) {
        log.info("╔════════════════════════════════════════════════════════════════╗");
        log.info("║    Configure MySQL Root Access from Network Subnets           ║");
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

            // Configure master
            configureNode("Master", MASTER_MGMT_IP, MASTER_MYSQL_IP, MASTER_SSH_USER, MASTER_SSH_PASSWORD, "percona-master");

            // Configure slave
            configureNode("Slave", SLAVE_MGMT_IP, SLAVE_MYSQL_IP, SLAVE_SSH_USER, SLAVE_SSH_PASSWORD, "percona-slave1");
        } catch (Exception e) {
            log.error("Error loading configuration: {}", e.getMessage(), e);
            return;
        }

        log.info("\n╔════════════════════════════════════════════════════════════════╗");
        log.info("║    Network Access Configuration Complete!                      ║");
        log.info("╚════════════════════════════════════════════════════════════════╝");
        log.info("");
        log.info("Root access now available from:");
        log.info("  - 10.10.%.% (container subnet)");
        log.info("  - 10.9.%.%  (WireGuard overlay)");
    }

    private static void configureNode(String role, String mgmtIp, String mysqlIp, String sshUser, String sshPassword, String containerName) {
        log.info("\n═══ Configuring {} ({}) ═══", role, mgmtIp);

        RemoteSshDevice device = new RemoteSshDevice(mgmtIp, 22, sshUser);
        try {
            device.connect(sshPassword);
            log.info("Connected to {}", role);

            // SQL to create root users for both subnets
            String sql = String.format(
                "CREATE USER IF NOT EXISTS 'root'@'10.10.%%' IDENTIFIED BY '%s'; " +
                "GRANT ALL PRIVILEGES ON *.* TO 'root'@'10.10.%%' WITH GRANT OPTION; " +
                "CREATE USER IF NOT EXISTS 'root'@'10.9.%%' IDENTIFIED BY '%s'; " +
                "GRANT ALL PRIVILEGES ON *.* TO 'root'@'10.9.%%' WITH GRANT OPTION; " +
                "FLUSH PRIVILEGES;",
                MYSQL_ROOT_PASSWORD, MYSQL_ROOT_PASSWORD);

            String cmd = String.format(
                "sudo docker exec %s mysql -h%s -uroot -p'%s' -e \"%s\" 2>&1",
                containerName, mysqlIp, MYSQL_ROOT_PASSWORD, sql);

            String result = device.executeCommand(cmd);

            if (result.contains("ERROR")) {
                log.warn("{} result: {}", role, result);
            } else {
                log.info("{}: Root users created for 10.10.%% and 10.9.%%", role);
            }

            // Verify users
            String verifyCmd = String.format(
                "sudo docker exec %s mysql -h%s -uroot -p'%s' -e " +
                "\"SELECT User, Host FROM mysql.user WHERE User='root' ORDER BY Host\" 2>&1",
                containerName, mysqlIp, MYSQL_ROOT_PASSWORD);

            String users = device.executeCommand(verifyCmd);
            log.info("{} root users:\n{}", role, users);

            device.disconnect();

        } catch (Exception e) {
            log.error("Error configuring {}: {}", role, e.getMessage(), e);
        }
    }
}
