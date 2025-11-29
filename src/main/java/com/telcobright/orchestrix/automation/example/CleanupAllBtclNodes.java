package com.telcobright.orchestrix.automation.example;

import com.telcobright.orchestrix.automation.core.device.impl.RemoteSshDevice;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Complete cleanup of all BTCL nodes - WireGuard, Kafka, systemd services
 */
public class CleanupAllBtclNodes {
    private static final Logger log = LoggerFactory.getLogger(CleanupAllBtclNodes.class);

    private static final Object[][] NODES = {
        {"114.130.145.75", 50005, "Node 1 (sms-Kafka01@sbc01)"},
        {"114.130.145.75", 50006, "Node 2 (sms-Kafka02@sbc04)"},
        {"114.130.145.70", 50010, "Node 3 (sms-kafka03@sms01)"}
    };

    public static void main(String[] args) {
        String user = "telcobright";
        String password = "Takay1#$ane%%";

        log.info("╔════════════════════════════════════════════════════════════════╗");
        log.info("║       Complete Cleanup of All BTCL Nodes                     ║");
        log.info("╚════════════════════════════════════════════════════════════════╝");

        for (Object[] node : NODES) {
            String host = (String) node[0];
            int port = (int) node[1];
            String name = (String) node[2];

            log.info("\n━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            log.info("Cleaning {}", name);
            log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");

            RemoteSshDevice device = new RemoteSshDevice(host, port, user);
            try {
                device.connect(password);

                // 1. Stop and remove Kafka containers
                log.info("  Stopping Kafka containers...");
                device.executeCommand("sudo docker ps -a | grep kafka | awk '{print $1}' | xargs -r sudo docker stop 2>/dev/null || true");
                device.executeCommand("sudo docker ps -a | grep kafka | awk '{print $1}' | xargs -r sudo docker rm -f 2>/dev/null || true");

                // 2. Remove Kafka work directories
                log.info("  Removing Kafka directories...");
                device.executeCommand("rm -rf ~/kafka-node-* 2>/dev/null || true");
                device.executeCommand("sudo rm -rf /var/lib/kafka 2>/dev/null || true");

                // 3. Stop and disable WireGuard
                log.info("  Stopping WireGuard...");
                device.executeCommand("sudo wg-quick down wg-overlay 2>/dev/null || true");
                device.executeCommand("sudo wg-quick down wg0 2>/dev/null || true");
                device.executeCommand("sudo systemctl stop wg-quick@wg-overlay 2>/dev/null || true");
                device.executeCommand("sudo systemctl disable wg-quick@wg-overlay 2>/dev/null || true");

                // 4. Remove WireGuard config
                log.info("  Removing WireGuard config...");
                device.executeCommand("sudo rm -f /etc/wireguard/wg-overlay.conf 2>/dev/null || true");
                device.executeCommand("sudo rm -f /etc/wireguard/wg0.conf 2>/dev/null || true");

                // 5. Stop and remove all systemd IP services
                log.info("  Removing systemd IP services...");
                device.executeCommand("sudo systemctl stop lxdbr0-*-ip.service 2>/dev/null || true");
                device.executeCommand("sudo systemctl stop docker0-*-ip.service 2>/dev/null || true");
                device.executeCommand("sudo systemctl disable lxdbr0-*-ip.service 2>/dev/null || true");
                device.executeCommand("sudo systemctl disable docker0-*-ip.service 2>/dev/null || true");
                device.executeCommand("sudo rm -f /etc/systemd/system/lxdbr0-*-ip.service 2>/dev/null || true");
                device.executeCommand("sudo rm -f /etc/systemd/system/docker0-*-ip.service 2>/dev/null || true");
                device.executeCommand("sudo systemctl daemon-reload");

                // 6. Remove secondary IPs from bridges
                log.info("  Removing secondary IPs...");
                device.executeCommand("sudo ip addr flush dev lxdbr0 2>/dev/null || true");
                device.executeCommand("sudo ip addr flush dev docker0 2>/dev/null || true");

                // 7. Reset FRR BGP config
                log.info("  Resetting FRR...");
                device.executeCommand("sudo systemctl stop frr 2>/dev/null || true");
                device.executeCommand("sudo rm -f /etc/frr/frr.conf 2>/dev/null || true");

                // 8. Remove lxdbr0 (let it be recreated fresh)
                log.info("  Removing lxdbr0...");
                device.executeCommand("sudo lxc network delete lxdbr0 2>/dev/null || true");

                log.info("  ✓ {} cleaned up", name);

                device.disconnect();

            } catch (Exception e) {
                log.error("  ✗ Error cleaning {}: {}", name, e.getMessage(), e);
            }
        }

        log.info("\n━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        log.info("Cleanup complete! All nodes are clean.");
    }
}
