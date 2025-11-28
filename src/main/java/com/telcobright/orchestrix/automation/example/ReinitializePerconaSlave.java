package com.telcobright.orchestrix.automation.example;

import com.telcobright.orchestrix.automation.core.device.impl.RemoteSshDevice;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Reinitialize Percona slave container with clean data directory
 */
public class ReinitializePerconaSlave {
    private static final Logger log = LoggerFactory.getLogger(ReinitializePerconaSlave.class);

    private static final String SLAVE_MGMT_IP = "172.27.27.136";
    private static final String SSH_USER = "csas";
    private static final String SSH_PASSWORD = "KsCSAS!@9";

    public static void main(String[] args) {
        log.info("╔════════════════════════════════════════════════════════════════╗");
        log.info("║       Reinitializing Percona Slave Container                  ║");
        log.info("╚════════════════════════════════════════════════════════════════╝");

        RemoteSshDevice device = new RemoteSshDevice(SLAVE_MGMT_IP, 22, SSH_USER);
        try {
            device.connect(SSH_PASSWORD);
            log.info("Connected to slave node");

            // Step 1: Stop and remove the container
            log.info("\n═══ Step 1: Stop and remove container ═══");
            device.executeCommand("sudo docker stop percona-slave1 2>/dev/null || true");
            device.executeCommand("sudo docker rm percona-slave1 2>/dev/null || true");
            log.info("Container stopped and removed");

            // Step 2: Clear data directory
            log.info("\n═══ Step 2: Clear and prepare data directory ═══");
            device.executeCommand("sudo rm -rf /var/lib/mysql/*");
            device.executeCommand("sudo chown -R 999:999 /var/lib/mysql");
            device.executeCommand("sudo chmod 755 /var/lib/mysql");
            log.info("Data directory cleared and permissions set");

            // Step 3: Start fresh container using docker-compose
            log.info("\n═══ Step 3: Start fresh container ═══");
            String startResult = device.executeCommand("cd ~/percona-slave1 && sudo docker compose up -d 2>&1");
            log.info("Start result: {}", startResult);

            // Step 4: Wait for MySQL to initialize
            log.info("\n═══ Step 4: Waiting for MySQL initialization (30 seconds) ═══");
            Thread.sleep(30000);

            // Step 5: Check container status
            log.info("\n═══ Step 5: Check container status ═══");
            String psResult = device.executeCommand("sudo docker ps -a | grep percona");
            log.info("Container status: {}", psResult);

            // Step 6: Check logs for initialization completion
            log.info("\n═══ Step 6: Check initialization logs ═══");
            String logs = device.executeCommand("sudo docker logs percona-slave1 2>&1 | tail -20");
            log.info("Recent logs:\n{}", logs);

            // Step 7: Test MySQL connection via socket (localhost)
            log.info("\n═══ Step 7: Test MySQL connection ═══");
            String testResult = device.executeCommand(
                "sudo docker exec percona-slave1 mysql -uroot -p'KsMySQLRoot!2025' -e 'SELECT 1 AS test' 2>&1");
            log.info("MySQL test (via socket): {}", testResult);

            if (testResult.contains("test") && testResult.contains("1")) {
                log.info("✓ MySQL is working! Now run SetupPerconaReplication to configure replication.");
            } else {
                // Try without password (sometimes default is empty)
                String testNoPass = device.executeCommand(
                    "sudo docker exec percona-slave1 mysql -uroot -e 'SELECT 1 AS test' 2>&1");
                log.info("MySQL test (no password): {}", testNoPass);
            }

            device.disconnect();

        } catch (Exception e) {
            log.error("Error: {}", e.getMessage(), e);
        }
    }
}
