package com.telcobright.orchestrix.automation.example;

import com.telcobright.orchestrix.automation.core.device.impl.RemoteSshDevice;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Reset MySQL root password on slave using skip-grant-tables method
 */
public class ResetSlavePassword {
    private static final Logger log = LoggerFactory.getLogger(ResetSlavePassword.class);

    private static final String SLAVE_MGMT_IP = "172.27.27.136";
    private static final String SSH_USER = "csas";
    private static final String SSH_PASSWORD = "KsCSAS!@9";
    private static final String MYSQL_ROOT_PASSWORD = "KsMySQLRoot!2025";

    public static void main(String[] args) {
        log.info("╔════════════════════════════════════════════════════════════════╗");
        log.info("║       Resetting Percona Slave MySQL Root Password             ║");
        log.info("╚════════════════════════════════════════════════════════════════╝");

        RemoteSshDevice device = new RemoteSshDevice(SLAVE_MGMT_IP, 22, SSH_USER);
        try {
            device.connect(SSH_PASSWORD);
            log.info("Connected to slave node");

            // Method 1: Try without password (fresh Percona might have no root password initially)
            log.info("\n═══ Trying without password ═══");
            String testNoPass = device.executeCommand(
                "sudo docker exec percona-slave1 mysql -uroot -e 'SELECT 1' 2>&1");
            log.info("No password test: {}", testNoPass);

            if (testNoPass.contains("1")) {
                log.info("MySQL works without password! Setting password...");
                String setPass = device.executeCommand(String.format(
                    "sudo docker exec percona-slave1 mysql -uroot -e \""
                    + "ALTER USER 'root'@'localhost' IDENTIFIED BY '%s'; "
                    + "CREATE USER IF NOT EXISTS 'root'@'%%' IDENTIFIED BY '%s'; "
                    + "GRANT ALL PRIVILEGES ON *.* TO 'root'@'%%' WITH GRANT OPTION; "
                    + "FLUSH PRIVILEGES;\" 2>&1", MYSQL_ROOT_PASSWORD, MYSQL_ROOT_PASSWORD));
                log.info("Set password result: {}", setPass);
            } else {
                // Method 2: Check what users exist
                log.info("\n═══ Checking MySQL users table ═══");

                // Stop container, start with skip-grant-tables
                log.info("Stopping container to start with skip-grant-tables...");
                device.executeCommand("sudo docker stop percona-slave1 2>/dev/null || true");
                device.executeCommand("sudo docker rm percona-slave1 2>/dev/null || true");

                // Start a temporary container with skip-grant-tables
                log.info("Starting MySQL with skip-grant-tables...");
                String runCmd = "cd ~/percona-slave1 && "
                    + "sudo docker run -d --name percona-slave1-temp "
                    + "--network host "
                    + "-v /var/lib/mysql:/var/lib/mysql "
                    + "percona:5.7 "
                    + "--skip-grant-tables --skip-networking";
                String runResult = device.executeCommand(runCmd);
                log.info("Temp container: {}", runResult);

                Thread.sleep(10000);

                // Reset password
                log.info("Resetting root password...");
                String resetCmd = String.format(
                    "sudo docker exec percona-slave1-temp mysql -uroot mysql -e \""
                    + "UPDATE user SET authentication_string=PASSWORD('%s') WHERE User='root'; "
                    + "FLUSH PRIVILEGES;\" 2>&1", MYSQL_ROOT_PASSWORD);
                String resetResult = device.executeCommand(resetCmd);
                log.info("Reset result: {}", resetResult);

                // Also create root@'%'
                String createCmd = String.format(
                    "sudo docker exec percona-slave1-temp mysql -uroot mysql -e \""
                    + "INSERT IGNORE INTO user (Host,User,authentication_string,ssl_cipher,x509_issuer,x509_subject,Select_priv,Insert_priv,Update_priv,Delete_priv,Create_priv,Drop_priv,Reload_priv,Shutdown_priv,Process_priv,File_priv,Grant_priv,References_priv,Index_priv,Alter_priv,Show_db_priv,Super_priv,Create_tmp_table_priv,Lock_tables_priv,Execute_priv,Repl_slave_priv,Repl_client_priv,Create_view_priv,Show_view_priv,Create_routine_priv,Alter_routine_priv,Create_user_priv,Event_priv,Trigger_priv,Create_tablespace_priv) "
                    + "SELECT '%%',User,authentication_string,ssl_cipher,x509_issuer,x509_subject,'Y','Y','Y','Y','Y','Y','Y','Y','Y','Y','Y','Y','Y','Y','Y','Y','Y','Y','Y','Y','Y','Y','Y','Y','Y','Y','Y','Y','Y' FROM user WHERE User='root' AND Host='localhost'; "
                    + "FLUSH PRIVILEGES;\" 2>&1");
                String createResult = device.executeCommand(createCmd);
                log.info("Create root@'%%' result: {}", createResult);

                // Stop temp container
                log.info("Stopping temp container...");
                device.executeCommand("sudo docker stop percona-slave1-temp");
                device.executeCommand("sudo docker rm percona-slave1-temp");

                // Start normal container
                log.info("Starting normal container...");
                device.executeCommand("cd ~/percona-slave1 && sudo docker compose up -d");

                Thread.sleep(15000);
            }

            // Final test
            log.info("\n═══ Final test ═══");
            String finalTest = device.executeCommand(String.format(
                "sudo docker exec percona-slave1 mysql -uroot -p'%s' -e 'SELECT 1 AS test' 2>&1", MYSQL_ROOT_PASSWORD));
            log.info("Final test (socket): {}", finalTest);

            String finalTestTcp = device.executeCommand(String.format(
                "sudo docker exec percona-slave1 mysql -h10.10.198.10 -uroot -p'%s' -e 'SELECT 1 AS test' 2>&1", MYSQL_ROOT_PASSWORD));
            log.info("Final test (TCP): {}", finalTestTcp);

            device.disconnect();

        } catch (Exception e) {
            log.error("Error: {}", e.getMessage(), e);
        }
    }
}
