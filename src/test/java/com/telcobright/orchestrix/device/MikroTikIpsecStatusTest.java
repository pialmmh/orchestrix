package com.telcobright.orchestrix.device;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Slf4j
public class MikroTikIpsecStatusTest {

    @Test
    public void testPrintCliCommands() {
        log.info("Printing useful MikroTik CLI commands for IPsec...");
        String commands = MikroTikIpsecManager.getUsefulCliCommands();
        System.out.println(commands);
    }

    @Test
    public void testGetFullIpsecStatus() {
        MikroTikRouter router = new MikroTikRouter("btcl-mikrotik");

        try {
            log.info("========================================");
            log.info("Getting FULL IPsec status from MikroTik");
            log.info("========================================");

            // Connect to MikroTik router
            CompletableFuture<Boolean> connectionFuture = router.connectSsh(
                    "114.130.145.75", 22, "admin", "Takay1#$ane%%"
            );

            Boolean connected = connectionFuture.get(30, TimeUnit.SECONDS);

            if (connected) {
                log.info("✅ Connected to MikroTik router");

                MikroTikIpsecManager ipsecManager = new MikroTikIpsecManager(router);

                // Get full status
                log.info("\n📊 Getting FULL IPsec Status...\n");
                CompletableFuture<String> fullStatusFuture = ipsecManager.getFullIpsecStatus();
                String fullStatus = fullStatusFuture.get(30, TimeUnit.SECONDS);
                System.out.println(fullStatus);

                router.disconnectSsh();
                log.info("✅ Status check completed");

            } else {
                log.error("❌ Failed to connect to MikroTik router");
            }

        } catch (Exception e) {
            log.error("❌ Error during status check", e);
        }
    }

    @Test
    public void testGetQuickStatus() {
        MikroTikRouter router = new MikroTikRouter("btcl-mikrotik");

        try {
            log.info("========================================");
            log.info("Getting QUICK IPsec status check");
            log.info("========================================");

            // Connect to MikroTik router
            CompletableFuture<Boolean> connectionFuture = router.connectSsh(
                    "114.130.145.75", 22, "admin", "Takay1#$ane%%"
            );

            Boolean connected = connectionFuture.get(30, TimeUnit.SECONDS);

            if (connected) {
                log.info("✅ Connected to MikroTik router");

                MikroTikIpsecManager ipsecManager = new MikroTikIpsecManager(router);

                // Get quick status
                log.info("\n🚀 Getting Quick Status...\n");
                CompletableFuture<String> quickStatusFuture = ipsecManager.getQuickStatus();
                String quickStatus = quickStatusFuture.get(10, TimeUnit.SECONDS);
                System.out.println(quickStatus);

                router.disconnectSsh();
                log.info("✅ Quick status check completed");

            } else {
                log.error("❌ Failed to connect to MikroTik router");
            }

        } catch (Exception e) {
            log.error("❌ Error during quick status check", e);
        }
    }

    @Test
    public void testTroubleshootConnection() {
        MikroTikRouter router = new MikroTikRouter("btcl-mikrotik");

        try {
            log.info("========================================");
            log.info("Troubleshooting IPsec connection");
            log.info("========================================");

            // Connect to MikroTik router
            CompletableFuture<Boolean> connectionFuture = router.connectSsh(
                    "114.130.145.75", 22, "admin", "Takay1#$ane%%"
            );

            Boolean connected = connectionFuture.get(30, TimeUnit.SECONDS);

            if (connected) {
                log.info("✅ Connected to MikroTik router");

                MikroTikIpsecManager ipsecManager = new MikroTikIpsecManager(router);

                // Troubleshoot the tunnel
                log.info("\n🔍 Troubleshooting BTCL-Infozillion-VPN tunnel...\n");
                CompletableFuture<String> troubleshootFuture =
                    ipsecManager.troubleshootConnection("BTCL-Infozillion-VPN");
                String troubleshootReport = troubleshootFuture.get(20, TimeUnit.SECONDS);
                System.out.println(troubleshootReport);

                router.disconnectSsh();
                log.info("✅ Troubleshooting completed");

            } else {
                log.error("❌ Failed to connect to MikroTik router");
            }

        } catch (Exception e) {
            log.error("❌ Error during troubleshooting", e);
        }
    }
}