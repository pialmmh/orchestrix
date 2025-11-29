package com.telcobright.orchestrix.automation.overlay;

import com.telcobright.orchestrix.automation.config.OverlayConfig;
import com.telcobright.orchestrix.automation.config.OverlayConfig.NodeConfig;
import com.telcobright.orchestrix.automation.core.device.impl.RemoteSshDevice;
import com.telcobright.orchestrix.automation.network.LxdBridgeAutomation;
import com.telcobright.orchestrix.automation.routing.NetworkingGuidelineHelper;
import com.telcobright.orchestrix.automation.routing.NetworkingGuidelineHelper.NodeNetworkConfig;
import com.telcobright.orchestrix.automation.routing.wireguard.WireGuardConfigGenerator;
import com.telcobright.orchestrix.automation.routing.wireguard.WireGuardConfigGenerator.KeyPair;
import com.telcobright.orchestrix.automation.routing.wireguard.WireGuardConfigGenerator.MeshPeer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Config-Driven Overlay Network Deployment
 *
 * One-click deployment for WireGuard overlay + FRR BGP routing.
 * Reads all configuration from deployment config files.
 *
 * Usage:
 * <pre>
 * # Deploy to any tenant:
 * mvn exec:java -Dexec.mainClass="...OverlayNetworkDeployment" \
 *     -Dexec.args="deployments/ks_network"
 *
 * # Default (netlab):
 * mvn exec:java -Dexec.mainClass="...OverlayNetworkDeployment"
 * </pre>
 *
 * @author TelcoBright Orchestrix Team
 * @since 2025-11-27
 */
public class OverlayNetworkDeployment {

    private static final Logger log = LoggerFactory.getLogger(OverlayNetworkDeployment.class);

    private final OverlayConfig config;
    private final Map<Integer, RemoteSshDevice> connections = new HashMap<>();
    private final Map<Integer, KeyPair> nodeKeys = new HashMap<>();
    private final Map<Integer, NodeNetworkConfig> networkConfigs = new HashMap<>();

    public OverlayNetworkDeployment(OverlayConfig config) {
        this.config = config;
    }

    /**
     * Deploy complete overlay network stack
     */
    public DeploymentResult deploy() {
        log.info("╔════════════════════════════════════════════════════════════════╗");
        log.info("║    Overlay Network Deployment - WireGuard + FRR BGP           ║");
        log.info("╚════════════════════════════════════════════════════════════════╝");
        log.info("");
        log.info("Tenant: {}", config.getTenantName());
        log.info("Nodes: {}", config.getNodeCount());
        log.info("Overlay Network: {}", config.getOverlayNetwork());
        log.info("");

        DeploymentResult result = new DeploymentResult();
        result.setTenantName(config.getTenantName());

        try {
            // Step 1: Connect to all nodes
            connectToAllNodes();

            // Step 2: Install prerequisites
            installPrerequisites();

            // Step 3: Configure LXD bridges
            configureLxdBridges();

            // Step 4: Generate and deploy WireGuard
            deployWireGuard();

            // Step 5: Configure FRR BGP
            configureFrrBgp();

            // Step 6: Verify deployment
            boolean verified = verifyDeployment();

            if (verified) {
                result.setSuccess(true);
                result.setMessage("Overlay network deployed successfully");
                printSuccessSummary();
            } else {
                result.setSuccess(false);
                result.setMessage("Deployment completed but verification failed");
            }

        } catch (Exception e) {
            log.error("Deployment failed", e);
            result.setSuccess(false);
            result.setMessage("Deployment failed: " + e.getMessage());
            result.setException(e);

        } finally {
            disconnectAll();
        }

        return result;
    }

    private void connectToAllNodes() throws Exception {
        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        log.info("🔌 Connecting to Nodes");
        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");

        for (NodeConfig node : config.getNodes()) {
            log.info("Connecting to Node {} ({})...", node.getNodeId(), node.getMgmtIp());

            RemoteSshDevice ssh = new RemoteSshDevice(
                node.getMgmtIp(),
                node.getSshPort(),
                node.getSshUser()
            );
            ssh.connect(node.getSshPassword());
            connections.put(node.getNodeId(), ssh);

            // Create network config from config file values (not calculated)
            NodeNetworkConfig netConfig = new NodeNetworkConfig(
                node.getNodeId(),
                node.getHostname(),
                node.getMgmtIp(),
                node.getSshPort(),
                node.getOverlayIp(),
                node.getOverlayIp() + "/24",
                node.getContainerSubnet(),
                Integer.parseInt(node.getBgpAs()),
                config.getOverlayPort()
            );
            networkConfigs.put(node.getNodeId(), netConfig);

            log.info("  ✓ Connected to Node {} ({})", node.getNodeId(), node.getHostname());
        }

        log.info("✓ All nodes connected");
        log.info("");
    }

    private void installPrerequisites() throws Exception {
        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        log.info("📦 Installing Prerequisites");
        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");

        for (NodeConfig node : config.getNodes()) {
            RemoteSshDevice ssh = connections.get(node.getNodeId());
            log.info("Installing packages on Node {}...", node.getNodeId());

            ssh.executeCommand("sudo apt-get update -qq");
            ssh.executeCommand("sudo DEBIAN_FRONTEND=noninteractive apt-get install -y " +
                "wireguard wireguard-tools frr lxd bridge-utils iproute2 iptables");

            log.info("  ✓ Packages installed on Node {}", node.getNodeId());
        }

        log.info("✓ Prerequisites installed");
        log.info("");
    }

    private void configureLxdBridges() throws Exception {
        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        log.info("🌉 Configuring LXD Bridges (with persistent IPs)");
        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");

        for (NodeConfig node : config.getNodes()) {
            RemoteSshDevice ssh = connections.get(node.getNodeId());
            NodeNetworkConfig netConfig = networkConfigs.get(node.getNodeId());

            log.info("Configuring lxdbr0 on Node {}...", node.getNodeId());
            log.info("  Subnet: {}", netConfig.containerSubnet());

            // Use LxdBridgeAutomation for persistent IP configuration
            LxdBridgeAutomation bridgeAutomation = new LxdBridgeAutomation(ssh);

            // Configure bridge with gateway IP (creates systemd persistence service)
            String gatewayIp = bridgeAutomation.configureBridge(netConfig.containerSubnet());
            log.info("  ✓ lxdbr0 configured with persistent gateway: {}", gatewayIp);

            // Verify bridge is ready
            if (bridgeAutomation.verifyBridgeReady()) {
                log.info("  ✓ Bridge verification passed");
            } else {
                log.warn("  ⚠ Bridge verification issue - may need attention");
            }
        }

        log.info("✓ LXD bridges configured with persistent IPs");
        log.info("");
    }

    private void deployWireGuard() throws Exception {
        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        log.info("🔐 Deploying WireGuard Overlay");
        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");

        // Generate keys for all nodes
        for (NodeConfig node : config.getNodes()) {
            log.info("Generating WireGuard keys for Node {}...", node.getNodeId());
            KeyPair keys = WireGuardConfigGenerator.generateKeyPair();
            nodeKeys.put(node.getNodeId(), keys);
            log.info("  Public Key: {}", keys.publicKey());
        }
        log.info("");

        // Deploy WireGuard config to each node
        for (NodeConfig node : config.getNodes()) {
            RemoteSshDevice ssh = connections.get(node.getNodeId());
            NodeNetworkConfig thisNodeConfig = networkConfigs.get(node.getNodeId());
            KeyPair thisNodeKeys = nodeKeys.get(node.getNodeId());

            log.info("Deploying WireGuard to Node {}...", node.getNodeId());

            // Build peer list
            List<MeshPeer> peers = new ArrayList<>();
            for (NodeConfig peerNode : config.getNodes()) {
                if (peerNode.getNodeId() != node.getNodeId()) {
                    NodeNetworkConfig peerConfig = networkConfigs.get(peerNode.getNodeId());
                    KeyPair peerKeys = nodeKeys.get(peerNode.getNodeId());

                    // Use WireGuard endpoint IP for peer connectivity (may differ from mgmt IP in NAT scenarios)
                    String wgEndpoint = peerNode.getWgEndpoint();
                    log.debug("  Node {} -> Node {}: WG endpoint {}",
                        node.getNodeId(), peerNode.getNodeId(), wgEndpoint);

                    peers.add(new MeshPeer(
                        peerNode.getHostname(),
                        peerNode.getMgmtIp(),
                        wgEndpoint,
                        peerConfig.overlayIp(),
                        peerKeys.publicKey(),
                        peerConfig.containerSubnet(),
                        false
                    ));
                }
            }

            // Generate config
            String wgConfig = WireGuardConfigGenerator.generateOverlayConfig(
                thisNodeConfig,
                thisNodeKeys.privateKey(),
                peers,
                List.of(),
                false
            );

            // Deploy config
            String escapedConfig = wgConfig.replace("'", "'\\''");
            ssh.executeCommand("echo '" + escapedConfig + "' | sudo tee /etc/wireguard/wg-overlay.conf > /dev/null");

            // Start WireGuard
            ssh.executeCommand("sudo wg-quick down wg-overlay 2>/dev/null || true");
            ssh.executeCommand("sudo wg-quick up wg-overlay");

            log.info("  ✓ WireGuard started - Overlay IP: {}", thisNodeConfig.overlayIp());
        }

        // Wait for mesh to establish
        log.info("Waiting for WireGuard mesh to establish...");
        Thread.sleep(5000);

        log.info("✓ WireGuard overlay deployed");
        log.info("");
    }

    private void configureFrrBgp() throws Exception {
        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        log.info("🛜 Configuring FRR BGP Routing");
        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");

        for (NodeConfig node : config.getNodes()) {
            RemoteSshDevice ssh = connections.get(node.getNodeId());
            NodeNetworkConfig thisNodeConfig = networkConfigs.get(node.getNodeId());

            log.info("Configuring BGP on Node {}...", node.getNodeId());
            log.info("  AS: {}", thisNodeConfig.bgpAsNumber());
            log.info("  Router ID: {}", thisNodeConfig.getRouterId());

            // Generate FRR config
            String frrConfig = generateFrrConfig(node, thisNodeConfig);

            // Deploy config
            String escapedConfig = frrConfig.replace("'", "'\\''");
            ssh.executeCommand("echo '" + escapedConfig + "' | sudo tee /etc/frr/frr.conf > /dev/null");

            // Enable BGP daemon
            ssh.executeCommand("sudo sed -i 's/bgpd=no/bgpd=yes/' /etc/frr/daemons");

            // Restart FRR
            ssh.executeCommand("sudo systemctl restart frr");
            Thread.sleep(2000);

            String frrStatus = ssh.executeCommand("sudo systemctl is-active frr");
            if (frrStatus.trim().equals("active")) {
                log.info("  ✓ FRR BGP running on Node {}", node.getNodeId());
            } else {
                log.warn("  ⚠ FRR may not be running properly on Node {}", node.getNodeId());
            }
        }

        // Wait for BGP to establish
        log.info("Waiting for BGP sessions to establish...");
        Thread.sleep(10000);

        log.info("✓ FRR BGP configured");
        log.info("");
    }

    private String generateFrrConfig(NodeConfig node, NodeNetworkConfig netConfig) {
        StringBuilder config = new StringBuilder();
        config.append("!\n");
        config.append("! FRR BGP Configuration - ").append(node.getHostname()).append("\n");
        config.append("! Generated by Overlay Network Deployment\n");
        config.append("!\n");
        config.append("frr version 8.1\n");
        config.append("frr defaults traditional\n");
        config.append("hostname ").append(node.getHostname()).append("\n");
        config.append("log syslog informational\n");
        config.append("no ipv6 forwarding\n");
        config.append("service integrated-vtysh-config\n");
        config.append("!\n");
        config.append("router bgp ").append(netConfig.bgpAsNumber()).append("\n");
        config.append(" bgp router-id ").append(netConfig.getRouterId()).append("\n");
        config.append(" no bgp ebgp-requires-policy\n");
        config.append(" no bgp network import-check\n");
        config.append(" !\n");

        // Add neighbors
        for (NodeConfig peerNode : this.config.getNodes()) {
            if (peerNode.getNodeId() != node.getNodeId()) {
                NodeNetworkConfig peerConfig = networkConfigs.get(peerNode.getNodeId());
                config.append(" neighbor ").append(peerConfig.overlayIp())
                    .append(" remote-as ").append(peerConfig.bgpAsNumber()).append("\n");
                config.append(" neighbor ").append(peerConfig.overlayIp())
                    .append(" description ").append(peerNode.getHostname()).append("-via-WireGuard\n");
                config.append(" neighbor ").append(peerConfig.overlayIp())
                    .append(" ebgp-multihop 2\n");
                config.append(" neighbor ").append(peerConfig.overlayIp())
                    .append(" timers 10 30\n");
                config.append(" !\n");
            }
        }

        // Address family
        config.append(" address-family ipv4 unicast\n");
        config.append("  network ").append(netConfig.containerSubnet()).append("\n");
        for (NodeConfig peerNode : this.config.getNodes()) {
            if (peerNode.getNodeId() != node.getNodeId()) {
                NodeNetworkConfig peerConfig = networkConfigs.get(peerNode.getNodeId());
                config.append("  neighbor ").append(peerConfig.overlayIp()).append(" activate\n");
            }
        }
        config.append(" exit-address-family\n");
        config.append("exit\n");
        config.append("!\n");
        config.append("line vty\n");
        config.append("!\n");
        config.append("end\n");

        return config.toString();
    }

    private boolean verifyDeployment() throws Exception {
        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        log.info("🔍 Verifying Deployment");
        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");

        boolean allGood = true;
        int expectedPeers = config.getNodeCount() - 1;

        for (NodeConfig node : config.getNodes()) {
            RemoteSshDevice ssh = connections.get(node.getNodeId());
            log.info("");
            log.info("Node {} ({}):", node.getNodeId(), node.getHostname());

            // Check WireGuard peers
            String wgShow = ssh.executeCommand("sudo wg show wg-overlay | grep -c '^peer:' 2>/dev/null || echo 0");
            int peerCount = parseCount(wgShow);
            log.info("  WireGuard peers: {}/{}", peerCount, expectedPeers);
            if (peerCount < expectedPeers) allGood = false;

            // Check BGP neighbors (count lines with overlay IPs that end with numbers = established)
            String bgpNeighbors = ssh.executeCommand(
                "sudo vtysh -c 'show ip bgp summary' 2>/dev/null | grep '10\\.9\\.' | grep -cE '[0-9]+\\s+[0-9]+\\s*$' 2>/dev/null || echo 0"
            );
            int bgpCount = parseCount(bgpNeighbors);
            log.info("  BGP neighbors established: {}/{}", bgpCount, expectedPeers);
            if (bgpCount < expectedPeers) allGood = false;

            // Check routes
            String bgpRoutes = ssh.executeCommand(
                "sudo vtysh -c 'show ip bgp' 2>/dev/null | grep -c '10\\.10\\.' || echo 0"
            );
            log.info("  BGP routes: {}", bgpRoutes.trim());
        }

        return allGood;
    }

    /**
     * Safely parse count from command output, handling multiline strings and errors.
     */
    private int parseCount(String output) {
        if (output == null || output.trim().isEmpty()) {
            return 0;
        }

        // Take only the first line and trim whitespace
        String firstLine = output.split("\n")[0].trim();

        try {
            return Integer.parseInt(firstLine);
        } catch (NumberFormatException e) {
            log.warn("Failed to parse count from output: '{}', returning 0", firstLine);
            return 0;
        }
    }

    private void printSuccessSummary() {
        log.info("");
        log.info("╔════════════════════════════════════════════════════════════════╗");
        log.info("║              Deployment Complete! 🚀                          ║");
        log.info("╚════════════════════════════════════════════════════════════════╝");
        log.info("");
        log.info("Network Configuration:");
        for (NodeConfig node : config.getNodes()) {
            NodeNetworkConfig netConfig = networkConfigs.get(node.getNodeId());
            log.info("  Node {} ({}):", node.getNodeId(), node.getHostname());
            log.info("    Management:   {}", node.getMgmtIp());
            log.info("    Overlay:      {}", netConfig.overlayIp());
            log.info("    Containers:   {}", netConfig.containerSubnet());
            log.info("    BGP AS:       {}", netConfig.bgpAsNumber());
        }
        log.info("");
    }

    private void disconnectAll() {
        for (Map.Entry<Integer, RemoteSshDevice> entry : connections.entrySet()) {
            try {
                entry.getValue().disconnect();
            } catch (Exception e) {
                log.warn("Error disconnecting from node {}", entry.getKey());
            }
        }
        connections.clear();
    }

    // ========================================
    // Main entry point
    // ========================================

    public static void main(String[] args) {
        String deploymentDir = args.length > 0 ? args[0] : "deployments/netlab";

        log.info("╔════════════════════════════════════════════════════════════════╗");
        log.info("║         Overlay Network - One-Click Deployment                ║");
        log.info("╚════════════════════════════════════════════════════════════════╝");
        log.info("");

        try {
            log.info("📋 Loading configuration from: {}", deploymentDir);
            OverlayConfig config = new OverlayConfig(deploymentDir);
            log.info("✓ Configuration loaded");
            log.info("  Tenant: {}", config.getTenantName());
            log.info("  Nodes: {}", config.getNodeCount());
            log.info("");

            OverlayNetworkDeployment deployment = new OverlayNetworkDeployment(config);
            DeploymentResult result = deployment.deploy();

            if (result.isSuccess()) {
                System.exit(0);
            } else {
                log.error("Deployment failed: {}", result.getMessage());
                System.exit(1);
            }

        } catch (Exception e) {
            log.error("Failed to load configuration: {}", e.getMessage(), e);
            System.exit(1);
        }
    }

    // ========================================
    // Result class
    // ========================================

    public static class DeploymentResult {
        private boolean success;
        private String message;
        private String tenantName;
        private Exception exception;

        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }

        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }

        public String getTenantName() { return tenantName; }
        public void setTenantName(String tenantName) { this.tenantName = tenantName; }

        public Exception getException() { return exception; }
        public void setException(Exception exception) { this.exception = exception; }
    }
}
