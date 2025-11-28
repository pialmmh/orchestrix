package com.telcobright.orchestrix.automation.example;

import com.telcobright.orchestrix.automation.core.device.impl.RemoteSshDevice;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Fix /var/lib/mysql permissions for Percona MySQL container
 * Percona 5.7 runs as uid 999 (mysql user)
 */
public class FixPerconaPermissions {
    private static final Logger log = LoggerFactory.getLogger(FixPerconaPermissions.class);

    // Fix only slave - master is working
    private static final String[][] NODES = {
        {"172.27.27.136", "Node 2 (Slave)"}
    };

    public static void main(String[] args) {
        String user = "csas";
        String password = "KsCSAS!@9";

        log.info("╔════════════════════════════════════════════════════════════════╗");
        log.info("║       Fix Percona MySQL Data Directory Permissions            ║");
        log.info("╚════════════════════════════════════════════════════════════════╝");

        for (String[] node : NODES) {
            String host = node[0];
            String name = node[1];

            log.info("\n━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            log.info("Fixing permissions on {} ({})", name, host);
            log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");

            RemoteSshDevice device = new RemoteSshDevice(host, 22, user);
            try {
                device.connect(password);

                // Stop any running Percona container (don't remove - just stop)
                log.info("  Stopping existing containers...");
                device.executeCommand("sudo docker stop percona-master 2>/dev/null || true");
                device.executeCommand("sudo docker stop percona-slave1 2>/dev/null || true");

                // Check current permissions
                String currentPerms = device.executeCommand("ls -la /var/lib/mysql 2>&1 | head -3").trim();
                log.info("  Current permissions:\n{}", currentPerms);

                // Create directory if it doesn't exist, clear and set correct ownership
                // Percona 5.7 uses uid 999 for mysql user
                log.info("  Creating/clearing and fixing /var/lib/mysql...");
                device.executeCommand("sudo mkdir -p /var/lib/mysql");
                device.executeCommand("sudo rm -rf /var/lib/mysql/*");
                device.executeCommand("sudo chown -R 999:999 /var/lib/mysql");
                device.executeCommand("sudo chmod 755 /var/lib/mysql");

                // Verify
                String newPerms = device.executeCommand("ls -la /var/lib/ | grep mysql").trim();
                log.info("  New permissions: {}", newPerms);

                // Start the container if it exists
                log.info("  Starting containers...");
                String startMaster = device.executeCommand("sudo docker start percona-master 2>&1 || true");
                log.info("  Start master: {}", startMaster.trim());
                String startSlave = device.executeCommand("sudo docker start percona-slave1 2>&1 || true");
                log.info("  Start slave: {}", startSlave.trim());

                device.disconnect();
                log.info("  {} done", name);

            } catch (Exception e) {
                log.error("Error on {}: {}", name, e.getMessage(), e);
            }
        }

        log.info("\n━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        log.info("Permissions fixed! Now run KsNetworkPerconaDeployment again.");
    }
}
