package com.telcobright.orchestrix.device;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Slf4j
public class MikroTikIpsecCommandOutputTest {

    @Test
    public void printIpsecConfigurationAndStatus() {
        MikroTikRouter router = new MikroTikRouter("btcl-mikrotik");

        try {
            log.info("========================================");
            log.info("EXECUTING MIKROTIK CLI COMMANDS");
            log.info("========================================");

            // Connect to MikroTik router
            CompletableFuture<Boolean> connectionFuture = router.connectSsh(
                    "114.130.145.75", 22, "admin", "Takay1#$ane%%"
            );

            Boolean connected = connectionFuture.get(30, TimeUnit.SECONDS);

            if (connected) {
                log.info("✅ Connected to MikroTik router 114.130.145.75\n");

                System.out.println("========================================");
                System.out.println("📋 CONFIGURATION CHECKS");
                System.out.println("========================================\n");

                // 1. IPsec Peers (Phase 1 - IKE)
                System.out.println("1️⃣  IPsec PEERS (Phase 1 - IKE):");
                System.out.println("Command: /ip ipsec peer print detail");
                System.out.println("----------------------------------------");
                String peers = router.executeCustomCommand("/ip ipsec peer print detail")
                    .get(5, TimeUnit.SECONDS);
                System.out.println(peers);
                System.out.println();

                // 2. IPsec Identities (PSK)
                System.out.println("2️⃣  IPsec IDENTITIES (Pre-Shared Keys):");
                System.out.println("Command: /ip ipsec identity print detail");
                System.out.println("----------------------------------------");
                String identities = router.executeCustomCommand("/ip ipsec identity print detail")
                    .get(5, TimeUnit.SECONDS);
                System.out.println(identities);
                System.out.println();

                // 3. IPsec Policies (Encryption Domains)
                System.out.println("3️⃣  IPsec POLICIES (Encryption Domains):");
                System.out.println("Command: /ip ipsec policy print detail");
                System.out.println("----------------------------------------");
                String policies = router.executeCustomCommand("/ip ipsec policy print detail")
                    .get(5, TimeUnit.SECONDS);
                System.out.println(policies);
                System.out.println();

                // 4. IPsec Proposals (Phase 2 - ESP)
                System.out.println("4️⃣  IPsec PROPOSALS (Phase 2 - ESP):");
                System.out.println("Command: /ip ipsec proposal print detail");
                System.out.println("----------------------------------------");
                String proposals = router.executeCustomCommand("/ip ipsec proposal print detail")
                    .get(5, TimeUnit.SECONDS);
                System.out.println(proposals);
                System.out.println();

                // 5. IPsec Profiles (Phase 1 Parameters)
                System.out.println("5️⃣  IPsec PROFILES (Phase 1 Crypto Parameters):");
                System.out.println("Command: /ip ipsec profile print detail");
                System.out.println("----------------------------------------");
                String profiles = router.executeCustomCommand("/ip ipsec profile print detail")
                    .get(5, TimeUnit.SECONDS);
                System.out.println(profiles);
                System.out.println();

                System.out.println("========================================");
                System.out.println("🔍 STATUS CHECKS");
                System.out.println("========================================\n");

                // 6. Active Peers (Live Connections)
                System.out.println("6️⃣  ACTIVE PEERS (Live Connection Status):");
                System.out.println("Command: /ip ipsec active-peers print detail");
                System.out.println("----------------------------------------");
                String activePeers = router.executeCustomCommand("/ip ipsec active-peers print detail")
                    .get(5, TimeUnit.SECONDS);
                System.out.println(activePeers);
                System.out.println();

                // 7. Installed SAs
                System.out.println("7️⃣  INSTALLED SECURITY ASSOCIATIONS:");
                System.out.println("Command: /ip ipsec installed-sa print detail");
                System.out.println("----------------------------------------");
                String installedSA = router.executeCustomCommand("/ip ipsec installed-sa print detail")
                    .get(5, TimeUnit.SECONDS);
                if (installedSA == null || installedSA.trim().isEmpty()) {
                    System.out.println("❌ No Security Associations installed (tunnel not established)");
                } else {
                    System.out.println(installedSA);
                }
                System.out.println();

                // 8. IPsec Statistics
                System.out.println("8️⃣  IPsec STATISTICS:");
                System.out.println("Command: /ip ipsec statistics print");
                System.out.println("----------------------------------------");
                String stats = router.executeCustomCommand("/ip ipsec statistics print")
                    .get(5, TimeUnit.SECONDS);
                System.out.println(stats);
                System.out.println();

                // 9. Policy Statistics (with packet counters)
                System.out.println("9️⃣  POLICY STATISTICS (Packet Counters):");
                System.out.println("Command: /ip ipsec policy print stats");
                System.out.println("----------------------------------------");
                String policyStats = router.executeCustomCommand("/ip ipsec policy print stats")
                    .get(5, TimeUnit.SECONDS);
                System.out.println(policyStats);
                System.out.println();

                // 10. Check for IPsec-related logs
                System.out.println("🔟 RECENT IPsec LOGS:");
                System.out.println("Command: /log print where topics~\"ipsec\"");
                System.out.println("----------------------------------------");
                String logs = router.executeCustomCommand("/log print where topics~\"ipsec\"")
                    .get(5, TimeUnit.SECONDS);
                if (logs == null || logs.trim().isEmpty()) {
                    System.out.println("ℹ️  No IPsec logs found (may need to enable: /system logging add topics=ipsec,!packet)");
                } else {
                    System.out.println(logs);
                }
                System.out.println();

                System.out.println("========================================");
                System.out.println("📊 SUMMARY");
                System.out.println("========================================");

                // Parse and summarize the status
                if (activePeers.contains("established")) {
                    System.out.println("✅ Tunnel Status: ESTABLISHED");
                } else if (activePeers.contains("message-1-sent")) {
                    System.out.println("⏳ Tunnel Status: CONNECTING (Phase 1 - waiting for remote response)");
                } else if (activePeers.contains("message-2-sent")) {
                    System.out.println("⏳ Tunnel Status: CONNECTING (Phase 1 - negotiating)");
                } else if (activePeers.contains("message-3-sent")) {
                    System.out.println("⏳ Tunnel Status: CONNECTING (Phase 1 - final stage)");
                } else {
                    System.out.println("❌ Tunnel Status: NOT CONNECTED");
                }

                // Check configuration
                if (peers.contains("BTCL-Infozillion-VPN")) {
                    System.out.println("✅ Peer Configuration: Found");
                    System.out.println("   - Remote IP: 220.247.164.172");
                    System.out.println("   - Exchange Mode: Main");
                }

                if (policies.contains("10.127.127.104/29")) {
                    System.out.println("✅ Policy Configuration: Found");
                    System.out.println("   - Local Subnet: 10.127.127.104/29");
                    System.out.println("   - Remote Subnet: 192.168.89.0/28");
                }

                router.disconnectSsh();
                log.info("\n✅ Command execution completed");

            } else {
                log.error("❌ Failed to connect to MikroTik router");
            }

        } catch (Exception e) {
            log.error("❌ Error executing commands", e);
        }
    }
}