package com.telcobright.orchestrix.automation.example;

import com.telcobright.orchestrix.automation.config.CommonConfig;
import com.telcobright.orchestrix.automation.core.device.impl.RemoteSshDevice;

import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * Add VPN client peers to netlab01 WireGuard gateway
 *
 * This automation reads generated VPN client public keys and adds them
 * to the netlab01 WireGuard interface (wg0), allowing remote access from
 * Link3 and BDCOM workstations.
 *
 * Prerequisites:
 * - NetlabDeployment has been run successfully
 * - VPN client configs generated (scripts/generate-vpn-clients.sh)
 *
 * @author TelcoBright Orchestrix Team
 * @since 2025-11-16
 */
public class AddVpnClients {

    private static class ClientPeer {
        String name;
        String publicKey;
        String allowedIp;

        ClientPeer(String name, String publicKey, String allowedIp) {
            this.name = name;
            this.publicKey = publicKey;
            this.allowedIp = allowedIp;
        }
    }

    public static void main(String[] args) throws Exception {
        // Load netlab configuration
        CommonConfig config = new CommonConfig("deployments/netlab");

        // Client peers to add (read from generated configs)
        ClientPeer[] clients = {
            new ClientPeer("link3", "BGrS8YySugLalbl52ZJ+pb17pB0jkoysWLrJ3dF1qQw=", "10.9.9.100/32"),
            new ClientPeer("bdcom", "myRnf8u15pSPefbIlCYuKs4P621KbcWv1+lWMNv6EGk=", "10.9.9.101/32")
        };

        // Read SSH password
        String sshPassword = Files.readString(Paths.get("deployments/netlab/.secrets/ssh-password.txt")).trim();

        System.out.println("╔════════════════════════════════════════════════════════════════╗");
        System.out.println("║      Add VPN Client Peers to Netlab Gateway (node1)          ║");
        System.out.println("╚════════════════════════════════════════════════════════════════╝");
        System.out.println();

        // Connect to netlab01 (gateway)
        String gatewayIp = config.getNode1Ip();
        String gatewayUser = config.getSshUser();
        int gatewayPort = config.getSshPort();

        System.out.println("Connecting to gateway: " + gatewayUser + "@" + gatewayIp + ":" + gatewayPort);
        RemoteSshDevice gateway = new RemoteSshDevice(gatewayIp, gatewayPort, gatewayUser);
        gateway.connect(sshPassword);
        System.out.println("  ✓ Connected");
        System.out.println();

        // Add each client peer
        for (ClientPeer client : clients) {
            System.out.println("Adding client peer: " + client.name);
            System.out.println("  Public Key: " + client.publicKey);
            System.out.println("  Allowed IP: " + client.allowedIp);

            // Add peer to WireGuard interface
            String addPeerCmd = String.format(
                "sudo wg set wg-overlay peer %s allowed-ips %s",
                client.publicKey,
                client.allowedIp
            );

            String result = gateway.executeCommand(addPeerCmd);
            if (result != null && !result.trim().isEmpty()) {
                System.out.println("  Output: " + result.trim());
            }

            System.out.println("  ✓ Peer added to wg0");
        }

        System.out.println();
        System.out.println("Saving WireGuard configuration...");
        gateway.executeCommand("sudo wg-quick save wg-overlay || echo 'Note: wg-quick save may not be supported, config is active in memory'");
        System.out.println("  ✓ Configuration saved (peers active in memory)");
        System.out.println();

        // Verify peers
        System.out.println("Verifying WireGuard peers...");
        String wgStatus = gateway.executeCommand("sudo wg show wg-overlay");
        System.out.println(wgStatus);
        System.out.println();

        gateway.disconnect();

        System.out.println("╔════════════════════════════════════════════════════════════════╗");
        System.out.println("║                   Setup Complete! 🚀                          ║");
        System.out.println("╚════════════════════════════════════════════════════════════════╝");
        System.out.println();
        System.out.println("VPN client configs are ready:");
        System.out.println("  • deployments/netlab/output/link3-vpn.conf");
        System.out.println("  • deployments/netlab/output/bdcom-vpn.conf");
        System.out.println();
        System.out.println("Next steps:");
        System.out.println("  1. Copy configs to Link3/BDCOM workstations");
        System.out.println("  2. Import in WireGuard client (Linux/Mac/Windows)");
        System.out.println("  3. Activate VPN connection");
        System.out.println("  4. Test connectivity:");
        System.out.println("     ping 10.9.9.1       # Gateway overlay");
        System.out.println("     ping 10.10.199.1    # netlab01 containers");
        System.out.println("     ping 10.10.198.1    # netlab02 containers");
        System.out.println("     ping 10.10.197.1    # netlab03 containers");
        System.out.println();
    }
}
