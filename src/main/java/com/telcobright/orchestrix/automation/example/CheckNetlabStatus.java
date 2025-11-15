package com.telcobright.orchestrix.automation.example;

import com.telcobright.orchestrix.automation.config.CommonConfig;
import com.telcobright.orchestrix.automation.core.device.impl.RemoteSshDevice;

import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * Check current status of netlab deployment
 *
 * Verifies:
 * - SSH connectivity
 * - WireGuard interface status
 * - FRR BGP status
 * - Network connectivity
 *
 * @author TelcoBright Orchestrix Team
 * @since 2025-11-16
 */
public class CheckNetlabStatus {

    public static void main(String[] args) throws Exception {
        CommonConfig config = new CommonConfig("deployments/netlab");
        String sshPassword = Files.readString(Paths.get("deployments/netlab/.secrets/ssh-password.txt")).trim();

        System.out.println("╔════════════════════════════════════════════════════════════════╗");
        System.out.println("║            Netlab Deployment Status Check                    ║");
        System.out.println("╚════════════════════════════════════════════════════════════════╝");
        System.out.println();

        // Check node1
        String node1Ip = config.getNode1Ip();
        System.out.println("Checking netlab01 (" + node1Ip + ")...");
        RemoteSshDevice node1 = new RemoteSshDevice(node1Ip, config.getSshPort(), config.getSshUser());
        node1.connect(sshPassword);
        System.out.println("  ✓ SSH connected");

        // Check hostname
        String hostname = node1.executeCommand("hostname");
        System.out.println("  Hostname: " + hostname.trim());

        // Check WireGuard interfaces
        System.out.println("\n  WireGuard Interfaces:");
        String wgInterfaces = node1.executeCommand("ip link show | grep -i wg || echo '  No WireGuard interfaces found'");
        System.out.println(wgInterfaces);

        // Check all network interfaces
        System.out.println("\n  All Network Interfaces:");
        String allInterfaces = node1.executeCommand("ip -br addr");
        System.out.println(allInterfaces);

        // Check if WireGuard is installed
        System.out.println("\n  WireGuard Installation:");
        String wgVersion = node1.executeCommand("which wg && wg --version || echo 'WireGuard not found'");
        System.out.println(wgInterfaces);

        // Check WireGuard systemd services
        System.out.println("\n  WireGuard Services:");
        String wgServices = node1.executeCommand("systemctl list-units --type=service --all | grep wg || echo 'No WireGuard services found'");
        System.out.println(wgServices);

        // Check FRR status
        System.out.println("\n  FRR Status:");
        String frrStatus = node1.executeCommand("systemctl status frr --no-pager | head -5 || echo 'FRR not found'");
        System.out.println(frrStatus);

        // Check BGP summary
        System.out.println("\n  BGP Summary:");
        String bgpSummary = node1.executeCommand("sudo vtysh -c 'show ip bgp summary' 2>/dev/null | tail -5 || echo 'BGP not available'");
        System.out.println(bgpSummary);

        node1.disconnect();

        System.out.println();
        System.out.println("╔════════════════════════════════════════════════════════════════╗");
        System.out.println("║                    Status Check Complete                     ║");
        System.out.println("╚════════════════════════════════════════════════════════════════╝");
    }
}
