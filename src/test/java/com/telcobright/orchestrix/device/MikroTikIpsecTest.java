package com.telcobright.orchestrix.device;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Slf4j
public class MikroTikIpsecTest {

    @Test
    public void testCreateBtclIpsecTunnel() {
        MikroTikRouter router = new MikroTikRouter("btcl-mikrotik");

        try {
            log.info("========================================");
            log.info("Creating IPsec tunnel for BTCL");
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

                // From Excel:
                // Local: 114.130.145.75, Remote: 220.247.164.172
                // PSK: alWnJ3$tV@nSK
                // Local Subnet: 10.127.127.104/29, Remote Subnet: 192.168.89.0/28

                // First, try with minimal configuration (only WAN IPs and PSK)
                log.info("\n--- Creating IPsec tunnel with minimal config ---");
                CompletableFuture<Boolean> tunnelFuture = ipsecManager.createMinimalIpsecTunnel(
                    "BTCL-Infozillion-VPN",
                    "114.130.145.75",      // Local WAN IP
                    "220.247.164.172",     // Remote WAN IP
                    "alWnJ3$tV@nSK"        // Pre-shared key
                );

                Boolean tunnelCreated = tunnelFuture.get(30, TimeUnit.SECONDS);

                if (tunnelCreated) {
                    log.info("✅ IPsec tunnel created successfully with minimal config");

                    // Check tunnel status
                    CompletableFuture<String> statusFuture = ipsecManager.checkTunnelStatus("BTCL-Infozillion-VPN");
                    String status = statusFuture.get(10, TimeUnit.SECONDS);
                    log.info("\n📊 Tunnel Status:\n{}", status);

                } else {
                    log.error("❌ Failed to create IPsec tunnel with minimal config");

                    // Try with full configuration including subnets
                    log.info("\n--- Trying with full configuration including subnets ---");

                    MikroTikIpsecManager.IpsecTunnelConfig fullConfig =
                            new MikroTikIpsecManager.IpsecTunnelConfig(
                                "BTCL-Infozillion-VPN-Full",
                                "114.130.145.75",      // Local WAN IP
                                "220.247.164.172",     // Remote WAN IP
                                "alWnJ3$tV@nSK",       // Pre-shared key
                                "10.127.127.104/29",   // Local subnet
                                "192.168.89.0/28"      // Remote subnet
                            );

                    CompletableFuture<Boolean> fullTunnelFuture = ipsecManager.createIpsecTunnel(fullConfig);
                    Boolean fullTunnelCreated = fullTunnelFuture.get(30, TimeUnit.SECONDS);

                    if (fullTunnelCreated) {
                        log.info("✅ IPsec tunnel created successfully with full config");

                        // Check tunnel status
                        CompletableFuture<String> statusFuture = ipsecManager.checkTunnelStatus("BTCL-Infozillion-VPN-Full");
                        String status = statusFuture.get(10, TimeUnit.SECONDS);
                        log.info("\n📊 Tunnel Status:\n{}", status);

                    } else {
                        log.error("❌ Failed to create IPsec tunnel with full config");
                    }
                }

                // List all tunnels
                log.info("\n--- Listing all IPsec peers ---");
                CompletableFuture<String> listFuture = ipsecManager.listTunnels();
                String tunnelList = listFuture.get(10, TimeUnit.SECONDS);
                log.info("IPsec Peers:\n{}", tunnelList);

                router.disconnectSsh();
                log.info("✅ Test completed");

            } else {
                log.error("❌ Failed to connect to MikroTik router");
            }

        } catch (Exception e) {
            log.error("❌ Error during IPsec configuration test", e);
        }
    }

    @Test
    public void testDeleteBtclIpsecTunnel() {
        MikroTikRouter router = new MikroTikRouter("btcl-mikrotik");

        try {
            log.info("========================================");
            log.info("Deleting IPsec tunnel for BTCL");
            log.info("========================================");

            // Connect to MikroTik router
            CompletableFuture<Boolean> connectionFuture = router.connectSsh(
                    "114.130.145.75", 22, "admin", "Takay1#$ane%%"
            );

            Boolean connected = connectionFuture.get(30, TimeUnit.SECONDS);

            if (connected) {
                log.info("✅ Connected to MikroTik router");

                MikroTikIpsecManager ipsecManager = new MikroTikIpsecManager(router);

                // Delete both possible tunnel names
                log.info("Deleting IPsec tunnels...");
                ipsecManager.deleteTunnel("BTCL-Infozillion-VPN").get(10, TimeUnit.SECONDS);
                ipsecManager.deleteTunnel("BTCL-Infozillion-VPN-Full").get(10, TimeUnit.SECONDS);

                log.info("✅ Cleanup completed");

                // List remaining tunnels
                CompletableFuture<String> listFuture = ipsecManager.listTunnels();
                String tunnelList = listFuture.get(10, TimeUnit.SECONDS);
                log.info("Remaining IPsec Peers:\n{}", tunnelList);

                router.disconnectSsh();

            } else {
                log.error("❌ Failed to connect to MikroTik router");
            }

        } catch (Exception e) {
            log.error("❌ Error during cleanup", e);
        }
    }
}