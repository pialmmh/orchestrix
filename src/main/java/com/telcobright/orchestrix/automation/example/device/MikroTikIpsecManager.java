package com.telcobright.orchestrix.automation.example.device;

import lombok.Data;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * Reusable MikroTik IPsec tunnel configuration manager
 */
public class MikroTikIpsecManager {

    private static final Logger log = LoggerFactory.getLogger(MikroTikIpsecManager.class);

    private final MikroTikRouter router;

    /**
     * IPsec tunnel configuration parameters
     */
    @Data
    public static class IpsecTunnelConfig {
        private String tunnelName;
        private String localWanIp;
        private String remoteWanIp;
        private String preSharedKey;
        private String localSubnet;
        private String remoteSubnet;

        // Phase 1 parameters (defaults)
        private String authMethod = "pre-shared-key";
        private String exchangeMode = "main";
        private String encAlgorithm = "aes-256";
        private String hashAlgorithm = "sha256";
        private String dhGroup = "modp2048";  // DH Group 14
        private int phase1Lifetime = 86400;  // 24 hours in seconds

        // Phase 2 parameters (defaults)
        private String protocol = "esp";
        private String phase2EncAlgorithm = "aes-256";
        private String phase2AuthAlgorithm = "sha256";
        private String pfs = "no";
        private int phase2Lifetime = 3600;  // 1 hour in seconds

        // Constructor with minimum required parameters
        public IpsecTunnelConfig(String tunnelName, String localWanIp, String remoteWanIp,
                                 String preSharedKey, String localSubnet, String remoteSubnet) {
            this.tunnelName = tunnelName;
            this.localWanIp = localWanIp;
            this.remoteWanIp = remoteWanIp;
            this.preSharedKey = preSharedKey;
            this.localSubnet = localSubnet;
            this.remoteSubnet = remoteSubnet;
        }

        // Constructor with only WAN IPs and PSK (minimal config)
        public IpsecTunnelConfig(String tunnelName, String localWanIp, String remoteWanIp, String preSharedKey) {
            this(tunnelName, localWanIp, remoteWanIp, preSharedKey, null, null);
        }
    }

    public MikroTikIpsecManager(MikroTikRouter router) {
        this.router = router;
    }

    /**
     * Create IPsec tunnel with minimal configuration
     */
    public CompletableFuture<Boolean> createIpsecTunnel(IpsecTunnelConfig config) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                log.info("Creating IPsec tunnel '{}' from {} to {}",
                        config.getTunnelName(), config.getLocalWanIp(), config.getRemoteWanIp());

                List<String> commands = new ArrayList<>();

                // 1. Create IPsec peer (Phase 1)
                commands.add(String.format(
                    "/ip ipsec peer add name=%s address=%s exchange-mode=%s",
                    config.getTunnelName(), config.getRemoteWanIp(), config.getExchangeMode()
                ));

                // 2. Create IPsec identity with PSK
                commands.add(String.format(
                    "/ip ipsec identity add peer=%s auth-method=%s secret=\"%s\" generate-policy=no",
                    config.getTunnelName(), config.getAuthMethod(), config.getPreSharedKey()
                ));

                // 3. Create IPsec proposal (Phase 2)
                // Note: MikroTik uses "aes-256-cbc" not "aes-256" for proposal
                String proposalName = config.getTunnelName() + "-proposal";
                commands.add(String.format(
                    "/ip ipsec proposal add name=%s auth-algorithms=%s enc-algorithms=aes-256-cbc lifetime=%ds pfs-group=%s",
                    proposalName, config.getPhase2AuthAlgorithm(),
                    config.getPhase2Lifetime(), config.getPfs().equals("yes") ? "modp2048" : "none"
                ));

                // 4. Create IPsec profile (Phase 1)
                String profileName = config.getTunnelName() + "-profile";
                commands.add(String.format(
                    "/ip ipsec profile add name=%s hash-algorithm=%s enc-algorithm=%s dh-group=%s lifetime=%ds",
                    profileName, config.getHashAlgorithm(), config.getEncAlgorithm(),
                    config.getDhGroup(), config.getPhase1Lifetime()
                ));

                // 5. Update peer with profile
                commands.add(String.format(
                    "/ip ipsec peer set [find name=%s] profile=%s",
                    config.getTunnelName(), profileName
                ));

                // 6. Create IPsec policy if subnets are provided
                if (config.getLocalSubnet() != null && config.getRemoteSubnet() != null) {
                    commands.add(String.format(
                        "/ip ipsec policy add peer=%s src-address=%s dst-address=%s tunnel=yes sa-src-address=%s sa-dst-address=%s proposal=%s",
                        config.getTunnelName(), config.getLocalSubnet(), config.getRemoteSubnet(),
                        config.getLocalWanIp(), config.getRemoteWanIp(), proposalName
                    ));
                }

                // Execute all commands
                for (String command : commands) {
                    log.debug("Executing: {}", command);
                    CompletableFuture<String> future = router.executeCustomCommand(command);
                    String result = future.get(10, TimeUnit.SECONDS);

                    // Check for errors in the response
                    if (result != null && (result.contains("failure") || result.contains("error"))) {
                        log.error("Command failed: {} - Response: {}", command, result);
                        return false;
                    }
                    log.debug("Command result: {}", result);
                }

                log.info("IPsec tunnel '{}' created successfully", config.getTunnelName());
                return true;

            } catch (Exception e) {
                log.error("Failed to create IPsec tunnel: {}", e.getMessage(), e);
                return false;
            }
        });
    }

    /**
     * Create IPsec tunnel with minimal parameters (only WAN IPs and PSK)
     */
    public CompletableFuture<Boolean> createMinimalIpsecTunnel(String tunnelName,
                                                                String localWanIp,
                                                                String remoteWanIp,
                                                                String preSharedKey) {
        IpsecTunnelConfig config = new IpsecTunnelConfig(tunnelName, localWanIp, remoteWanIp, preSharedKey);
        return createIpsecTunnel(config);
    }

    /**
     * Check IPsec tunnel status
     */
    public CompletableFuture<String> checkTunnelStatus(String tunnelName) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                log.info("Checking status of IPsec tunnel '{}'", tunnelName);

                // Check peer status
                CompletableFuture<String> peerFuture = router.executeCustomCommand(
                    String.format("/ip ipsec peer print where name=%s", tunnelName)
                );
                String peerStatus = peerFuture.get(10, TimeUnit.SECONDS);

                // Check active peers
                CompletableFuture<String> activeFuture = router.executeCustomCommand(
                    "/ip ipsec active-peers print"
                );
                String activeStatus = activeFuture.get(10, TimeUnit.SECONDS);

                // Check installed SAs
                CompletableFuture<String> saFuture = router.executeCustomCommand(
                    "/ip ipsec installed-sa print"
                );
                String saStatus = saFuture.get(10, TimeUnit.SECONDS);

                StringBuilder status = new StringBuilder();
                status.append("=== IPsec Tunnel Status: ").append(tunnelName).append(" ===\n");
                status.append("\n--- Peer Configuration ---\n").append(peerStatus);
                status.append("\n--- Active Peers ---\n").append(activeStatus);
                status.append("\n--- Installed SAs ---\n").append(saStatus);

                return status.toString();

            } catch (Exception e) {
                log.error("Failed to check tunnel status: {}", e.getMessage());
                return "Error checking tunnel status: " + e.getMessage();
            }
        });
    }

    /**
     * Delete IPsec tunnel
     */
    public CompletableFuture<Boolean> deleteTunnel(String tunnelName) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                log.info("Deleting IPsec tunnel '{}'", tunnelName);

                List<String> commands = new ArrayList<>();

                // Delete policy
                commands.add(String.format("/ip ipsec policy remove [find peer=%s]", tunnelName));

                // Delete identity
                commands.add(String.format("/ip ipsec identity remove [find peer=%s]", tunnelName));

                // Delete peer
                commands.add(String.format("/ip ipsec peer remove [find name=%s]", tunnelName));

                // Delete custom proposal and profile
                commands.add(String.format("/ip ipsec proposal remove [find name=%s-proposal]", tunnelName));
                commands.add(String.format("/ip ipsec profile remove [find name=%s-profile]", tunnelName));

                // Execute all commands (ignore errors as some might not exist)
                for (String command : commands) {
                    try {
                        CompletableFuture<String> future = router.executeCustomCommand(command);
                        future.get(5, TimeUnit.SECONDS);
                    } catch (Exception e) {
                        log.debug("Command might have failed (resource might not exist): {}", command);
                    }
                }

                log.info("IPsec tunnel '{}' deleted", tunnelName);
                return true;

            } catch (Exception e) {
                log.error("Failed to delete tunnel: {}", e.getMessage());
                return false;
            }
        });
    }

    /**
     * List all IPsec tunnels
     */
    public CompletableFuture<String> listTunnels() {
        return router.executeCustomCommand("/ip ipsec peer print");
    }

    /**
     * Get comprehensive IPsec configuration status
     * Useful for debugging and monitoring
     */
    public CompletableFuture<String> getFullIpsecStatus() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                StringBuilder status = new StringBuilder();
                status.append("========================================\n");
                status.append("     FULL IPsec CONFIGURATION STATUS    \n");
                status.append("========================================\n\n");

                // 1. IPsec Peers
                String peers = router.executeCustomCommand("/ip ipsec peer print detail")
                    .get(5, TimeUnit.SECONDS);
                status.append("📋 IPsec PEERS (Phase 1 - IKE):\n");
                status.append("----------------------------------------\n");
                status.append(peers).append("\n\n");

                // 2. IPsec Identities
                String identities = router.executeCustomCommand("/ip ipsec identity print detail")
                    .get(5, TimeUnit.SECONDS);
                status.append("🔑 IPsec IDENTITIES:\n");
                status.append("----------------------------------------\n");
                status.append(identities).append("\n\n");

                // 3. IPsec Policies
                String policies = router.executeCustomCommand("/ip ipsec policy print detail")
                    .get(5, TimeUnit.SECONDS);
                status.append("📜 IPsec POLICIES (Encryption Domains):\n");
                status.append("----------------------------------------\n");
                status.append(policies).append("\n\n");

                // 4. IPsec Proposals
                String proposals = router.executeCustomCommand("/ip ipsec proposal print")
                    .get(5, TimeUnit.SECONDS);
                status.append("🔧 IPsec PROPOSALS (Phase 2 - ESP):\n");
                status.append("----------------------------------------\n");
                status.append(proposals).append("\n\n");

                // 5. IPsec Profiles
                String profiles = router.executeCustomCommand("/ip ipsec profile print")
                    .get(5, TimeUnit.SECONDS);
                status.append("⚙️  IPsec PROFILES (Phase 1 Parameters):\n");
                status.append("----------------------------------------\n");
                status.append(profiles).append("\n\n");

                // 6. Active Peers (Connection Status)
                String activePeers = router.executeCustomCommand("/ip ipsec active-peers print detail")
                    .get(5, TimeUnit.SECONDS);
                status.append("✅ ACTIVE PEERS (Live Connections):\n");
                status.append("----------------------------------------\n");
                status.append(activePeers).append("\n\n");

                // 7. Installed SAs
                String installedSA = router.executeCustomCommand("/ip ipsec installed-sa print detail")
                    .get(5, TimeUnit.SECONDS);
                status.append("🔐 INSTALLED SAs (Security Associations):\n");
                status.append("----------------------------------------\n");
                status.append(installedSA).append("\n\n");

                // 8. IPsec Statistics
                String stats = router.executeCustomCommand("/ip ipsec statistics print")
                    .get(5, TimeUnit.SECONDS);
                status.append("📊 IPsec STATISTICS:\n");
                status.append("----------------------------------------\n");
                status.append(stats).append("\n");

                return status.toString();

            } catch (Exception e) {
                log.error("Failed to get full IPsec status: {}", e.getMessage());
                return "Error getting full IPsec status: " + e.getMessage();
            }
        });
    }

    /**
     * Get quick IPsec connection status
     * Shows only the most important runtime information
     */
    public CompletableFuture<String> getQuickStatus() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                StringBuilder status = new StringBuilder();
                status.append("========================================\n");
                status.append("       QUICK IPsec STATUS CHECK        \n");
                status.append("========================================\n\n");

                // Active connections
                String activePeers = router.executeCustomCommand("/ip ipsec active-peers print")
                    .get(5, TimeUnit.SECONDS);
                status.append("🟢 ACTIVE CONNECTIONS:\n");
                status.append(activePeers).append("\n\n");

                // Installed SAs (brief)
                String sas = router.executeCustomCommand("/ip ipsec installed-sa print")
                    .get(5, TimeUnit.SECONDS);
                status.append("🔐 SECURITY ASSOCIATIONS:\n");
                status.append(sas).append("\n\n");

                // Check for any errors in remote-peers
                String remotePeers = router.executeCustomCommand("/ip ipsec remote-peers print")
                    .get(5, TimeUnit.SECONDS);
                status.append("🌐 REMOTE PEERS STATUS:\n");
                status.append(remotePeers).append("\n");

                return status.toString();

            } catch (Exception e) {
                log.error("Failed to get quick status: {}", e.getMessage());
                return "Error getting quick status: " + e.getMessage();
            }
        });
    }

    /**
     * Get CLI commands for manual verification
     * Returns a list of useful MikroTik CLI commands
     */
    public static String getUsefulCliCommands() {
        StringBuilder commands = new StringBuilder();
        commands.append("========================================\n");
        commands.append("  USEFUL MIKROTIK CLI COMMANDS FOR IPsec\n");
        commands.append("========================================\n\n");

        commands.append("📋 CONFIGURATION CHECKS:\n");
        commands.append("----------------------------------------\n");
        commands.append("# View all IPsec peers (Phase 1):\n");
        commands.append("/ip ipsec peer print detail\n\n");

        commands.append("# View IPsec identities (PSK):\n");
        commands.append("/ip ipsec identity print detail\n\n");

        commands.append("# View IPsec policies (encryption domains):\n");
        commands.append("/ip ipsec policy print detail\n\n");

        commands.append("# View IPsec proposals (Phase 2):\n");
        commands.append("/ip ipsec proposal print detail\n\n");

        commands.append("# View IPsec profiles (Phase 1 crypto):\n");
        commands.append("/ip ipsec profile print detail\n\n");

        commands.append("\n🔍 STATUS CHECKS:\n");
        commands.append("----------------------------------------\n");
        commands.append("# Check active IPsec connections:\n");
        commands.append("/ip ipsec active-peers print detail\n\n");

        commands.append("# Check installed Security Associations:\n");
        commands.append("/ip ipsec installed-sa print detail\n\n");

        commands.append("# Check remote peers status:\n");
        commands.append("/ip ipsec remote-peers print\n\n");

        commands.append("# View IPsec statistics:\n");
        commands.append("/ip ipsec statistics print\n\n");

        commands.append("\n🛠️ TROUBLESHOOTING:\n");
        commands.append("----------------------------------------\n");
        commands.append("# Enable IPsec debug logging:\n");
        commands.append("/system logging add topics=ipsec,!packet\n\n");

        commands.append("# View IPsec logs:\n");
        commands.append("/log print where topics~\"ipsec\"\n\n");

        commands.append("# Flush all SAs (force reconnect):\n");
        commands.append("/ip ipsec installed-sa flush\n\n");

        commands.append("# Disable/Enable a peer:\n");
        commands.append("/ip ipsec peer disable [find name=\"TUNNEL-NAME\"]\n");
        commands.append("/ip ipsec peer enable [find name=\"TUNNEL-NAME\"]\n\n");

        commands.append("\n🗑️ CLEANUP COMMANDS:\n");
        commands.append("----------------------------------------\n");
        commands.append("# Remove specific tunnel completely:\n");
        commands.append("/ip ipsec policy remove [find peer=\"TUNNEL-NAME\"]\n");
        commands.append("/ip ipsec identity remove [find peer=\"TUNNEL-NAME\"]\n");
        commands.append("/ip ipsec peer remove [find name=\"TUNNEL-NAME\"]\n");
        commands.append("/ip ipsec proposal remove [find name=\"TUNNEL-NAME-proposal\"]\n");
        commands.append("/ip ipsec profile remove [find name=\"TUNNEL-NAME-profile\"]\n\n");

        commands.append("\n📊 MONITORING:\n");
        commands.append("----------------------------------------\n");
        commands.append("# Monitor IPsec in real-time:\n");
        commands.append("/ip ipsec active-peers print interval=1\n\n");

        commands.append("# Check IPsec packet counters:\n");
        commands.append("/ip ipsec policy print stats\n\n");

        return commands.toString();
    }

    /**
     * Troubleshoot IPsec connection issues
     * Provides diagnostic information
     */
    public CompletableFuture<String> troubleshootConnection(String tunnelName) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                StringBuilder report = new StringBuilder();
                report.append("========================================\n");
                report.append("   IPsec TROUBLESHOOTING REPORT\n");
                report.append("   Tunnel: ").append(tunnelName).append("\n");
                report.append("========================================\n\n");

                // 1. Check if peer exists
                String peer = router.executeCustomCommand(
                    String.format("/ip ipsec peer print detail where name=%s", tunnelName))
                    .get(5, TimeUnit.SECONDS);

                if (peer == null || peer.trim().isEmpty()) {
                    report.append("❌ ERROR: Peer '").append(tunnelName).append("' not found!\n");
                    return report.toString();
                }

                report.append("✅ Peer Configuration:\n");
                report.append(peer).append("\n\n");

                // 2. Check identity
                String identity = router.executeCustomCommand(
                    String.format("/ip ipsec identity print detail where peer=%s", tunnelName))
                    .get(5, TimeUnit.SECONDS);
                report.append("🔑 Identity Configuration:\n");
                report.append(identity.isEmpty() ? "⚠️ No identity configured!\n" : identity).append("\n\n");

                // 3. Check policy
                String policy = router.executeCustomCommand(
                    String.format("/ip ipsec policy print detail where peer=%s", tunnelName))
                    .get(5, TimeUnit.SECONDS);
                report.append("📜 Policy Configuration:\n");
                report.append(policy.isEmpty() ? "⚠️ No policy configured!\n" : policy).append("\n\n");

                // 4. Check if peer is active
                String activePeer = router.executeCustomCommand(
                    "/ip ipsec active-peers print detail")
                    .get(5, TimeUnit.SECONDS);
                report.append("🔌 Connection Status:\n");
                if (activePeer.contains(tunnelName) || activePeer.contains("220.247.164.172")) {
                    report.append("✅ Peer is attempting to connect\n");
                    report.append(activePeer).append("\n\n");
                } else {
                    report.append("⚠️ Peer is not actively connecting\n\n");
                }

                // 5. Check recent logs
                String logs = router.executeCustomCommand(
                    "/log print where topics~\"ipsec\" last=20")
                    .get(5, TimeUnit.SECONDS);
                report.append("📝 Recent IPsec Logs:\n");
                report.append(logs).append("\n\n");

                // 6. Recommendations
                report.append("💡 RECOMMENDATIONS:\n");
                report.append("----------------------------------------\n");

                if (!activePeer.contains("established")) {
                    report.append("• Tunnel is not established. Possible causes:\n");
                    report.append("  - Remote side not configured\n");
                    report.append("  - PSK mismatch\n");
                    report.append("  - Firewall blocking UDP 500/4500\n");
                    report.append("  - NAT-T issues\n");
                    report.append("  - Phase 1/2 parameter mismatch\n\n");

                    report.append("• Try these commands:\n");
                    report.append("  /ip ipsec installed-sa flush\n");
                    report.append("  /ip ipsec peer disable [find name=\"").append(tunnelName).append("\"]\n");
                    report.append("  /ip ipsec peer enable [find name=\"").append(tunnelName).append("\"]\n");
                }

                return report.toString();

            } catch (Exception e) {
                log.error("Failed to troubleshoot connection: {}", e.getMessage());
                return "Error during troubleshooting: " + e.getMessage();
            }
        });
    }
}