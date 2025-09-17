package com.telcobright.orchestrix.device;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Slf4j
public class MikroTikIpsecFullTest {

    @Test
    public void testCreateFullBtclIpsecTunnelWithSubnets() {
        MikroTikRouter router = new MikroTikRouter("btcl-mikrotik");

        try {
            log.info("========================================");
            log.info("Creating FULL IPsec tunnel for BTCL with encryption domains");
            log.info("========================================");

            // Connect to MikroTik router
            CompletableFuture<Boolean> connectionFuture = router.connectSsh(
                    "114.130.145.75", 22, "admin", "Takay1#$ane%%"
            );

            Boolean connected = connectionFuture.get(30, TimeUnit.SECONDS);

            if (connected) {
                log.info("✅ Connected to MikroTik router 114.130.145.75");

                // Create IPsec manager
                MikroTikIpsecManager ipsecManager = new MikroTikIpsecManager(router);

                // First, cleanup any existing tunnels
                log.info("\n--- Cleaning up existing tunnels ---");
                try {
                    ipsecManager.deleteTunnel("BTCL-Infozillion-VPN").get(5, TimeUnit.SECONDS);
                    ipsecManager.deleteTunnel("BTCL-Infozillion-VPN-Full").get(5, TimeUnit.SECONDS);
                } catch (Exception e) {
                    log.debug("Cleanup of old tunnels (might not exist): {}", e.getMessage());
                }

                // Create full IPsec configuration with encryption domains
                log.info("\n--- Creating FULL IPsec tunnel with encryption domains ---");
                log.info("Configuration:");
                log.info("  Local WAN IP: 114.130.145.75");
                log.info("  Remote WAN IP: 220.247.164.172");
                log.info("  PSK: alWnJ3$tV@nSK");
                log.info("  Local Subnet: 10.127.127.104/29");
                log.info("  Remote Subnet: 192.168.89.0/28");

                MikroTikIpsecManager.IpsecTunnelConfig fullConfig =
                        new MikroTikIpsecManager.IpsecTunnelConfig(
                            "BTCL-Infozillion-VPN",
                            "114.130.145.75",      // Local WAN IP
                            "220.247.164.172",     // Remote WAN IP
                            "alWnJ3$tV@nSK",       // Pre-shared key
                            "10.127.127.104/29",   // Local subnet
                            "192.168.89.0/28"      // Remote subnet
                        );

                CompletableFuture<Boolean> tunnelFuture = ipsecManager.createIpsecTunnel(fullConfig);
                Boolean tunnelCreated = tunnelFuture.get(30, TimeUnit.SECONDS);

                if (tunnelCreated) {
                    log.info("✅ IPsec tunnel created successfully with full configuration");

                    // Wait a bit for tunnel to establish
                    Thread.sleep(3000);

                    // Check tunnel status
                    log.info("\n--- Checking Tunnel Status ---");
                    CompletableFuture<String> statusFuture = ipsecManager.checkTunnelStatus("BTCL-Infozillion-VPN");
                    String status = statusFuture.get(10, TimeUnit.SECONDS);
                    log.info("\n📊 Tunnel Status:\n{}", status);

                    // Check policies
                    log.info("\n--- Checking IPsec Policies ---");
                    CompletableFuture<String> policyFuture = router.executeCustomCommand("/ip ipsec policy print");
                    String policies = policyFuture.get(10, TimeUnit.SECONDS);
                    log.info("IPsec Policies:\n{}", policies);

                    // Check proposals
                    log.info("\n--- Checking IPsec Proposals ---");
                    CompletableFuture<String> proposalFuture = router.executeCustomCommand("/ip ipsec proposal print");
                    String proposals = proposalFuture.get(10, TimeUnit.SECONDS);
                    log.info("IPsec Proposals:\n{}", proposals);

                } else {
                    log.error("❌ Failed to create IPsec tunnel with full config");
                }

                // List all tunnels
                log.info("\n--- Final IPsec Configuration ---");
                CompletableFuture<String> listFuture = ipsecManager.listTunnels();
                String tunnelList = listFuture.get(10, TimeUnit.SECONDS);
                log.info("All IPsec Peers:\n{}", tunnelList);

                router.disconnectSsh();
                log.info("\n✅ Test completed successfully");

            } else {
                log.error("❌ Failed to connect to MikroTik router");
            }

        } catch (Exception e) {
            log.error("❌ Error during IPsec configuration test", e);
        }
    }
}