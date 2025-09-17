package com.telcobright.orchestrix.device;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * Reusable MikroTik IPsec tunnel configuration manager
 */
@Slf4j
public class MikroTikIpsecManager {

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
}