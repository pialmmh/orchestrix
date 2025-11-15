package com.telcobright.orchestrix.automation.example;

import com.telcobright.orchestrix.automation.routing.NetworkingGuidelineHelper;
import com.telcobright.orchestrix.automation.routing.NetworkingGuidelineHelper.NodeNetworkConfig;
import com.telcobright.orchestrix.automation.routing.wireguard.WireGuardConfigGenerator;
import com.telcobright.orchestrix.automation.routing.wireguard.WireGuardConfigGenerator.KeyPair;
import com.telcobright.orchestrix.automation.routing.wireguard.WireGuardConfigGenerator.MeshPeer;
import com.telcobright.orchestrix.automation.routing.wireguard.WireGuardConfigGenerator.VpnClient;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * Complete example showing how to deploy a BGP cluster with WireGuard overlay
 * following TelcoBright Container Networking Guideline.
 *
 * This example demonstrates:
 * 1. Generating WireGuard keys for all nodes
 * 2. Creating WireGuard overlay configurations (mesh + VPN gateway)
 * 3. Generating VPN client configurations
 * 4. Creating FRR BGP configurations
 *
 * Based on successful BDCOM deployment (2025-11-10).
 *
 * @author TelcoBright Orchestrix Team
 * @since 2025-11-10
 */
public class FullClusterDeploymentExample {

    /**
     * Cluster node definition
     */
    static class ClusterNode {
        String name;
        String hostname;
        String managementIp;
        int sshPort;
        int hostNumber;
        boolean isVpnGateway;

        // Generated during deployment
        KeyPair keys;
        NodeNetworkConfig networkConfig;

        ClusterNode(String name, String hostname, String managementIp,
                   int sshPort, int hostNumber, boolean isVpnGateway) {
            this.name = name;
            this.hostname = hostname;
            this.managementIp = managementIp;
            this.sshPort = sshPort;
            this.hostNumber = hostNumber;
            this.isVpnGateway = isVpnGateway;
        }
    }

    public static void main(String[] args) {
        System.out.println("╔════════════════════════════════════════════════════════════════╗");
        System.out.println("║   Complete BGP Cluster Deployment with WireGuard Overlay      ║");
        System.out.println("║              Following Networking Guideline                    ║");
        System.out.println("╚════════════════════════════════════════════════════════════════╝");
        System.out.println();

        try {
            // Define cluster - Example BDCOM-like configuration
            ClusterNode[] nodes = {
                new ClusterNode("node1", "bdcom1", "10.255.246.173", 22, 1, false),
                new ClusterNode("node2", "bdcom2", "10.255.246.174", 22, 2, false),
                new ClusterNode("node3", "bdcom3", "10.255.246.175", 22, 3, true) // VPN gateway
            };

            // Number of VPN clients to create
            int numVpnClients = 3;

            System.out.println("Cluster Configuration:");
            for (ClusterNode node : nodes) {
                System.out.println("  " + node.name + " - " + node.managementIp +
                    (node.isVpnGateway ? " (VPN Gateway)" : " (Mesh Only)"));
            }
            System.out.println("  VPN Clients: " + numVpnClients);
            System.out.println();

            // Create output directory
            Path outputDir = Paths.get("generated-configs");
            Files.createDirectories(outputDir);

            // ============================================================
            // Step 1: Generate keys for all nodes
            // ============================================================
            System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            System.out.println("📍 Step 1: Generating WireGuard keys");
            System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");

            for (ClusterNode node : nodes) {
                System.out.println("Generating keys for " + node.name + "...");
                node.keys = WireGuardConfigGenerator.generateKeyPair();
                node.networkConfig = NetworkingGuidelineHelper.createNodeConfig(
                    node.hostNumber, node.hostname, node.managementIp, node.sshPort
                );
                System.out.println("  Public Key: " + node.keys.publicKey());
                System.out.println("  " + node.networkConfig);
            }
            System.out.println("✓ Key generation complete\n");

            // ============================================================
            // Step 2: Generate WireGuard overlay configs for each node
            // ============================================================
            System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            System.out.println("📍 Step 2: Generating WireGuard overlay configurations");
            System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");

            // Generate VPN client keys (for VPN gateway config)
            List<VpnClient> vpnClients = new ArrayList<>();
            for (int i = 1; i <= numVpnClients; i++) {
                KeyPair clientKeys = WireGuardConfigGenerator.generateKeyPair();
                String clientIp = NetworkingGuidelineHelper.calculateVpnClientIp(i);
                vpnClients.add(new VpnClient("client" + i, clientIp, clientKeys.publicKey()));

                // Save client config
                ClusterNode vpnGateway = findVpnGateway(nodes);
                String clientConfig = WireGuardConfigGenerator.generateVpnClientConfig(
                    i, clientKeys.privateKey(), vpnGateway.networkConfig, vpnGateway.keys.publicKey()
                );

                Path clientConfigFile = outputDir.resolve("client" + i + ".conf");
                Files.writeString(clientConfigFile, clientConfig);
                System.out.println("✓ Generated VPN client config: " + clientConfigFile);
            }
            System.out.println();

            // Generate node overlay configs
            for (ClusterNode node : nodes) {
                System.out.println("Generating overlay config for " + node.name + "...");

                // Build peer list (all nodes except this one)
                List<MeshPeer> peers = new ArrayList<>();
                for (ClusterNode peer : nodes) {
                    if (!peer.name.equals(node.name)) {
                        peers.add(new MeshPeer(
                            peer.hostname,
                            peer.managementIp,
                            peer.networkConfig.overlayIp(),
                            peer.keys.publicKey(),
                            peer.networkConfig.containerSubnet(),
                            peer.isVpnGateway
                        ));
                    }
                }

                // Generate config (VPN clients only on gateway node)
                List<VpnClient> nodeVpnClients = node.isVpnGateway ? vpnClients : List.of();
                String overlayConfig = WireGuardConfigGenerator.generateOverlayConfig(
                    node.networkConfig,
                    node.keys.privateKey(),
                    peers,
                    nodeVpnClients,
                    node.isVpnGateway
                );

                // Save to file
                Path configFile = outputDir.resolve(node.name + "-wg-overlay.conf");
                Files.writeString(configFile, overlayConfig);
                System.out.println("  ✓ Saved to: " + configFile);
            }
            System.out.println("✓ WireGuard overlay configurations complete\n");

            // ============================================================
            // Step 3: Generate FRR BGP configurations
            // ============================================================
            System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            System.out.println("📍 Step 3: Generating FRR BGP configurations");
            System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");

            for (ClusterNode node : nodes) {
                System.out.println("Generating FRR config for " + node.name + "...");

                StringBuilder frrConfig = new StringBuilder();
                frrConfig.append("!\n");
                frrConfig.append("! FRR BGP Configuration - ").append(node.hostname).append("\n");
                frrConfig.append("! Generated by Orchestrix automation\n");
                frrConfig.append("! Overlay IP: ").append(node.networkConfig.overlayIp()).append("\n");
                frrConfig.append("! BGP AS: ").append(node.networkConfig.bgpAsNumber()).append("\n");
                frrConfig.append("!\n");
                frrConfig.append("frr version 8.1\n");
                frrConfig.append("frr defaults traditional\n");
                frrConfig.append("hostname ").append(node.hostname).append("\n");
                frrConfig.append("log syslog informational\n");
                frrConfig.append("no ipv6 forwarding\n");
                frrConfig.append("service integrated-vtysh-config\n");
                frrConfig.append("!\n");
                frrConfig.append("router bgp ").append(node.networkConfig.bgpAsNumber()).append("\n");
                frrConfig.append(" bgp router-id ").append(node.networkConfig.getRouterId()).append("\n");
                frrConfig.append(" no bgp ebgp-requires-policy\n");
                frrConfig.append(" no bgp network import-check\n");
                frrConfig.append(" !\n");

                // Add neighbors
                for (ClusterNode peer : nodes) {
                    if (!peer.name.equals(node.name)) {
                        frrConfig.append(" neighbor ").append(peer.networkConfig.overlayIp())
                            .append(" remote-as ").append(peer.networkConfig.bgpAsNumber()).append("\n");
                        frrConfig.append(" neighbor ").append(peer.networkConfig.overlayIp())
                            .append(" description ").append(peer.hostname).append("-via-WireGuard\n");
                        frrConfig.append(" neighbor ").append(peer.networkConfig.overlayIp())
                            .append(" ebgp-multihop 2\n");  // CRITICAL!
                        frrConfig.append(" neighbor ").append(peer.networkConfig.overlayIp())
                            .append(" timers 10 30\n");
                        frrConfig.append(" !\n");
                    }
                }

                // Add address family
                frrConfig.append(" address-family ipv4 unicast\n");
                frrConfig.append("  network ").append(node.networkConfig.containerSubnet()).append("\n");
                for (ClusterNode peer : nodes) {
                    if (!peer.name.equals(node.name)) {
                        frrConfig.append("  neighbor ").append(peer.networkConfig.overlayIp())
                            .append(" activate\n");
                    }
                }
                frrConfig.append(" exit-address-family\n");
                frrConfig.append("exit\n");
                frrConfig.append("!\n");
                frrConfig.append("line vty\n");
                frrConfig.append("!\n");
                frrConfig.append("end\n");

                // Save to file
                Path frrConfigFile = outputDir.resolve(node.name + "-frr.conf");
                Files.writeString(frrConfigFile, frrConfig.toString());
                System.out.println("  ✓ Saved to: " + frrConfigFile);
            }
            System.out.println("✓ FRR BGP configurations complete\n");

            // ============================================================
            // Summary
            // ============================================================
            System.out.println("╔════════════════════════════════════════════════════════════════╗");
            System.out.println("║           Configuration Generation Complete                   ║");
            System.out.println("╚════════════════════════════════════════════════════════════════╝");
            System.out.println();
            System.out.println("Generated files in: " + outputDir.toAbsolutePath());
            System.out.println();
            System.out.println("WireGuard Overlay Configs:");
            for (ClusterNode node : nodes) {
                System.out.println("  " + node.name + "-wg-overlay.conf");
            }
            System.out.println();
            System.out.println("FRR BGP Configs:");
            for (ClusterNode node : nodes) {
                System.out.println("  " + node.name + "-frr.conf");
            }
            System.out.println();
            System.out.println("VPN Client Configs:");
            for (int i = 1; i <= numVpnClients; i++) {
                System.out.println("  client" + i + ".conf");
            }
            System.out.println();
            System.out.println("Network Parameters (Per Guideline):");
            for (ClusterNode node : nodes) {
                System.out.println("  " + node.name + ":");
                System.out.println("    Overlay IP:    " + node.networkConfig.overlayIp());
                System.out.println("    BGP AS:        " + node.networkConfig.bgpAsNumber());
                System.out.println("    Container Net: " + node.networkConfig.containerSubnet());
            }
            System.out.println();
            System.out.println("Next Steps:");
            System.out.println("1. Copy WireGuard configs to nodes: /etc/wireguard/wg-overlay.conf");
            System.out.println("2. Start WireGuard: wg-quick up wg-overlay");
            System.out.println("3. Copy FRR configs to nodes: /etc/frr/frr.conf");
            System.out.println("4. Restart FRR: systemctl restart frr");
            System.out.println("5. Verify BGP: sudo vtysh -c 'show ip bgp summary'");
            System.out.println();

        } catch (Exception e) {
            System.err.println("❌ Error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static ClusterNode findVpnGateway(ClusterNode[] nodes) {
        for (ClusterNode node : nodes) {
            if (node.isVpnGateway) {
                return node;
            }
        }
        throw new IllegalStateException("No VPN gateway node found in cluster");
    }
}
