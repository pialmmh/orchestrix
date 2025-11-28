package com.telcobright.orchestrix.automation.example;

import com.telcobright.orchestrix.automation.core.device.impl.RemoteSshDevice;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Fix slave MySQL by temporarily disabling super-read-only for initial setup
 */
public class FixSlaveReadOnly {
    private static final Logger log = LoggerFactory.getLogger(FixSlaveReadOnly.class);

    private static final String SLAVE_MGMT_IP = "172.27.27.136";
    private static final String SSH_USER = "csas";
    private static final String SSH_PASSWORD = "KsCSAS!@9";
    private static final String MYSQL_ROOT_PASSWORD = "KsMySQLRoot!2025";

    public static void main(String[] args) {
        log.info("╔════════════════════════════════════════════════════════════════╗");
        log.info("║       Fix Slave: Disable super-read-only for setup            ║");
        log.info("╚════════════════════════════════════════════════════════════════╝");

        RemoteSshDevice device = new RemoteSshDevice(SLAVE_MGMT_IP, 22, SSH_USER);
        try {
            device.connect(SSH_PASSWORD);
            log.info("Connected to slave node");

            // Step 1: Stop the current container
            log.info("\n═══ Step 1: Stop container ═══");
            device.executeCommand("sudo docker stop percona-slave1 2>/dev/null || true");
            device.executeCommand("sudo docker rm percona-slave1 2>/dev/null || true");
            log.info("Container stopped and removed");

            // Step 2: Modify docker-compose.yml to remove read-only flags
            log.info("\n═══ Step 2: Update docker-compose.yml ═══");
            device.executeCommand("cd ~/percona-slave1 && "
                + "sed -i 's/- --read-only=ON/# - --read-only=ON/' docker-compose.yml && "
                + "sed -i 's/- --super-read-only=ON/# - --super-read-only=ON/' docker-compose.yml");

            String checkCompose = device.executeCommand("grep -E 'read-only' ~/percona-slave1/docker-compose.yml");
            log.info("Updated compose (read-only lines):\n{}", checkCompose);

            // Step 3: Start container without read-only
            log.info("\n═══ Step 3: Start container ═══");
            String startResult = device.executeCommand("cd ~/percona-slave1 && sudo docker compose up -d 2>&1");
            log.info("Start: {}", startResult.substring(Math.max(0, startResult.length() - 200)));

            // Wait for MySQL to start
            log.info("Waiting 15 seconds for MySQL to start...");
            Thread.sleep(15000);

            // Step 4: Set password and create root@'%'
            log.info("\n═══ Step 4: Configure root user ═══");

            // Check connection first
            String check = device.executeCommand("sudo docker exec percona-slave1 mysql -uroot -e 'SELECT 1' 2>&1");
            log.info("Connection check: {}", check);

            if (check.contains("1")) {
                // Set password
                String setPass = device.executeCommand(String.format(
                    "sudo docker exec percona-slave1 mysql -uroot -e \""
                    + "ALTER USER 'root'@'localhost' IDENTIFIED BY '%s'; "
                    + "CREATE USER IF NOT EXISTS 'root'@'%%' IDENTIFIED BY '%s'; "
                    + "GRANT ALL PRIVILEGES ON *.* TO 'root'@'%%' WITH GRANT OPTION; "
                    + "FLUSH PRIVILEGES;\" 2>&1", MYSQL_ROOT_PASSWORD, MYSQL_ROOT_PASSWORD));
                log.info("Set password result: {}", setPass);

                if (!setPass.contains("ERROR")) {
                    log.info("Password set successfully!");
                }
            } else {
                log.error("Cannot connect to MySQL");
            }

            // Step 5: Test connection
            log.info("\n═══ Step 5: Test connections ═══");

            String testSocket = device.executeCommand(String.format(
                "sudo docker exec percona-slave1 mysql -uroot -p'%s' -e 'SELECT \"socket_ok\" AS status' 2>&1", MYSQL_ROOT_PASSWORD));
            log.info("Socket test: {}", testSocket);

            String testTcp = device.executeCommand(String.format(
                "sudo docker exec percona-slave1 mysql -h10.10.198.10 -uroot -p'%s' -e 'SELECT \"tcp_ok\" AS status' 2>&1", MYSQL_ROOT_PASSWORD));
            log.info("TCP test: {}", testTcp);

            if (testTcp.contains("tcp_ok")) {
                log.info("\n✓ MySQL is ready! Run SetupPerconaReplication to configure replication.");
                log.info("Note: After replication is configured, you can re-enable read-only mode.");
            }

            device.disconnect();

        } catch (Exception e) {
            log.error("Error: {}", e.getMessage(), e);
        }
    }
}
