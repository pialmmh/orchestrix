package com.telcobright.orchestrix.automation.example;

import com.telcobright.orchestrix.automation.core.device.impl.RemoteSshDevice;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Quick status check for BTCL Kafka cluster
 */
public class CheckKafkaClusterStatus {

    private static final Logger log = LoggerFactory.getLogger(CheckKafkaClusterStatus.class);

    // BTCL Node Configuration
    private static final String[][] NODES = {
        {"114.130.145.75", "50005", "telcobright", "Takay1#$ane%%", "10.10.199.20", "Node 1 (btcl-kafka01)"},
        {"114.130.145.75", "50006", "telcobright", "Takay1#$ane%%", "10.10.198.20", "Node 2 (btcl-kafka02)"},
        {"114.130.145.70", "50010", "telcobright", "Takay1#$ane%%", "10.10.197.20", "Node 3 (btcl-kafka03)"}
    };

    public static void main(String[] args) {
        log.info("═══════════════════════════════════════════════════════════════");
        log.info("           BTCL Kafka Cluster Status Check");
        log.info("═══════════════════════════════════════════════════════════════");
        log.info("");

        for (String[] node : NODES) {
            checkNode(node[0], Integer.parseInt(node[1]), node[2], node[3], node[4], node[5]);
        }
    }

    private static void checkNode(String ip, int port, String user, String pass, String kafkaIp, String name) {
        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        log.info("Checking {}", name);
        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");

        try {
            RemoteSshDevice ssh = new RemoteSshDevice(ip, port, user);
            ssh.connect(pass);

            // Check Docker status
            log.info("Docker containers:");
            String dockerPs = ssh.executeCommand("docker ps --format '{{.Names}} {{.Status}}'");
            log.info("  {}", dockerPs.trim());

            // Check Kafka logs (last few lines)
            log.info("Kafka container logs (last 10 lines):");
            String logs = ssh.executeCommand("docker logs kafka-node --tail 10 2>&1 | head -20");
            for (String line : logs.split("\n")) {
                log.info("  {}", line);
            }

            // Check if Kafka is listening
            log.info("Port check:");
            String ports = ssh.executeCommand("ss -tlnp | grep :9092 | head -3");
            if (ports.trim().isEmpty()) {
                log.warn("  Port 9092 NOT listening!");
            } else {
                log.info("  {}", ports.trim());
            }

            // Check lxdbr0 IPs
            log.info("Bridge IPs:");
            String ips = ssh.executeCommand("ip addr show lxdbr0 | grep inet");
            log.info("  {}", ips.trim());

            // Check WireGuard
            log.info("WireGuard status:");
            String wg = ssh.executeCommand("sudo wg show wg-overlay 2>&1 | head -5");
            log.info("  {}", wg.trim());

            // Check BGP
            log.info("BGP status:");
            String bgp = ssh.executeCommand("sudo vtysh -c 'show bgp summary' 2>&1 | grep -E '(Neighbor|10\\.9\\.)' | head -5");
            log.info("  {}", bgp.trim());

            ssh.disconnect();
            log.info("✓ {} check complete", name);

        } catch (Exception e) {
            log.error("✗ {} check failed: {}", name, e.getMessage());
        }

        log.info("");
    }
}
