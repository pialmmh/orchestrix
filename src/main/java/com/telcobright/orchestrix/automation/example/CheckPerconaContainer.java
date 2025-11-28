package com.telcobright.orchestrix.automation.example;

import com.telcobright.orchestrix.automation.core.device.impl.RemoteSshDevice;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Check Percona container status and logs
 */
public class CheckPerconaContainer {
    private static final Logger log = LoggerFactory.getLogger(CheckPerconaContainer.class);

    public static void main(String[] args) {
        String user = "csas";
        String password = "KsCSAS!@9";

        // Check both nodes
        String[][] nodes = {
            {"172.27.27.132", "percona-master", "percona-master"},
            {"172.27.27.136", "percona-slave1", "percona-slave1"}
        };

        for (String[] node : nodes) {
            checkNode(node[0], node[1], node[2], user, password);
        }
    }

    private static void checkNode(String host, String containerName, String deployDir, String user, String password) {

        log.info("\n\n═══════════════════════════════════════════════════════════");
        log.info("Checking Percona container on {} ({})", host, containerName);

        RemoteSshDevice device = new RemoteSshDevice(host, 22, user);
        try {
            device.connect(password);
            log.info("Connected to {}", host);

            // Check docker ps -a
            log.info("\n=== Docker containers ===");
            String containers = device.executeCommand("sudo docker ps -a 2>&1");
            log.info("{}", containers);

            // Check docker-compose.yml
            log.info("\n=== docker-compose.yml ===");
            String compose = device.executeCommand("cat ~/" + deployDir + "/docker-compose.yml 2>&1");
            log.info("{}", compose);

            // Check docker logs
            log.info("\n=== Docker logs ===");
            String logs = device.executeCommand("sudo docker logs " + containerName + " 2>&1 | tail -100");
            log.info("{}", logs);

            // Check IP on lxdbr0
            log.info("\n=== lxdbr0 IP ===");
            String ip = device.executeCommand("ip addr show lxdbr0 2>&1");
            log.info("{}", ip);

            // Check /var/lib/mysql permissions
            log.info("\n=== /var/lib/mysql permissions ===");
            String perms = device.executeCommand("ls -la /var/lib/mysql 2>&1");
            log.info("{}", perms);

            // Check processes in container
            log.info("\n=== Container processes ===");
            String ps = device.executeCommand("sudo docker exec " + containerName + " ps aux 2>&1 || true");
            log.info("{}", ps);

            // Test MySQL connection via TCP (master binds to 10.10.199.10, slave to 10.10.198.10)
            log.info("\n=== Testing MySQL connection ===");
            String mysqlIp = host.equals("172.27.27.132") ? "10.10.199.10" : "10.10.198.10";
            String mysqlTest = device.executeCommand("sudo docker exec " + containerName + " mysql -h" + mysqlIp + " -uroot -pKsMySQLRoot!2025 -e 'SELECT 1 AS test' 2>&1 || true");
            log.info("{}", mysqlTest);

            device.disconnect();
        } catch (Exception e) {
            log.error("Error: {}", e.getMessage(), e);
        }
    }
}
