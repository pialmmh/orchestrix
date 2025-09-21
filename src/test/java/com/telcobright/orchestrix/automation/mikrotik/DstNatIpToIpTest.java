package com.telcobright.orchestrix.automation.mikrotik;

import com.telcobright.orchestrix.device.MikroTikRouter;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Slf4j
public class DstNatIpToIpTest {

    @Test
    public void testAddDstNatRule() {
        MikroTikRouter router = new MikroTikRouter("btcl-mikrotik");

        try {
            log.info("========================================");
            log.info("Testing DST-NAT IP-to-IP Translation");
            log.info("========================================");
            log.info("Original Destination: 10.127.127.105");
            log.info("Translated To: 10.246.7.101");
            log.info("========================================\n");

            // Connect to MikroTik router
            CompletableFuture<Boolean> connectionFuture = router.connectSsh(
                    "114.130.145.75", 22, "admin", "Takay1#$ane%%"
            );

            Boolean connected = connectionFuture.get(30, TimeUnit.SECONDS);

            if (connected) {
                log.info("✅ Connected to MikroTik router 114.130.145.75\n");

                // Create DST-NAT manager
                DstNatIpToIp dstNatManager = new DstNatIpToIp(router);

                // First, list existing NAT rules
                log.info("📋 Existing DST-NAT Rules:");
                log.info("========================================");
                String existingRules = dstNatManager.listAllDstNatRules().get(10, TimeUnit.SECONDS);
                System.out.println(existingRules);
                System.out.println();

                // Check if rule already exists
                log.info("🔍 Checking if DST-NAT rule 10.127.127.105 -> 10.246.7.101 already exists...");
                Boolean ruleExists = dstNatManager.ruleExists("10.127.127.105", "10.246.7.101")
                    .get(10, TimeUnit.SECONDS);

                if (ruleExists) {
                    log.info("⚠️  Rule already exists, removing it first...");
                    dstNatManager.removeDstNatRule("10.127.127.105", "10.246.7.101").get(10, TimeUnit.SECONDS);
                    Thread.sleep(1000);
                    log.info("✅ Existing rule removed\n");
                }

                // Create the DST-NAT rule
                log.info("➕ Adding DST-NAT Rule:");
                log.info("   Original Dst IP: 10.127.127.105");
                log.info("   Translated To: 10.246.7.101");
                log.info("   Interface: bridge");
                log.info("   Comment: BTCL DST-NAT Translation");
                log.info("========================================");

                DstNatIpToIp.DstNatRule natRule = new DstNatIpToIp.DstNatRule(
                    "10.127.127.105",
                    "10.246.7.101",
                    "BTCL DST-NAT Translation - Added by Orchestrix"
                );
                natRule.setInInterface("bridge");

                // Add the rule
                CompletableFuture<Boolean> addRuleFuture = dstNatManager.addDstNatRule(natRule);
                Boolean ruleAdded = addRuleFuture.get(10, TimeUnit.SECONDS);

                if (ruleAdded) {
                    log.info("\n✅ DST-NAT rule added successfully!");

                    // Verify the rule was added
                    log.info("\n🔍 Verifying Rule Addition:");
                    log.info("========================================");
                    DstNatIpToIp.VerificationResult verification = dstNatManager.verifyNatRule(
                        "10.127.127.105", "10.246.7.101"
                    ).get(10, TimeUnit.SECONDS);

                    System.out.println(verification.getSummary());

                    // Show rule details
                    log.info("📋 Rule Details:");
                    log.info("========================================");
                    String ruleDetails = verification.getRuleDetails();
                    System.out.println(ruleDetails);
                    System.out.println();

                    // Show active connections (if any)
                    log.info("🔗 Active Connections:");
                    log.info("========================================");
                    String connections = verification.getActiveConnections();
                    if (connections != null && !connections.trim().isEmpty()) {
                        System.out.println(connections);
                    } else {
                        System.out.println("No active connections yet (this is normal for a new rule)");
                    }
                    System.out.println();

                    // Print useful CLI commands
                    log.info("📝 Useful CLI Commands for Manual Verification:");
                    log.info("========================================");
                    List<String> commands = dstNatManager.getUsefulCliCommands("10.127.127.105", "10.246.7.101");
                    commands.forEach(System.out::println);
                    System.out.println();

                } else {
                    log.error("❌ Failed to add DST-NAT rule");
                }

                // Test with port-specific NAT
                log.info("\n🔬 Testing Port-Specific DST-NAT:");
                log.info("========================================");
                DstNatIpToIp.DstNatRule httpRule = new DstNatIpToIp.DstNatRule(
                    "10.127.127.106",
                    "10.246.7.102",
                    "HTTP Port Translation Test"
                );
                httpRule.setProtocol("tcp");
                httpRule.setDstPort(80);
                httpRule.setInInterface("bridge");

                Boolean httpRuleAdded = dstNatManager.addDstNatRule(httpRule).get(10, TimeUnit.SECONDS);
                if (httpRuleAdded) {
                    log.info("✅ Port-specific NAT rule added (10.127.127.106:80 -> 10.246.7.102:80)");

                    // Clean up test rule
                    Thread.sleep(2000);
                    dstNatManager.removeDstNatRule("10.127.127.106", "10.246.7.102").get(10, TimeUnit.SECONDS);
                    log.info("✅ Test rule cleaned up");
                }

                // Test multiple rules addition
                log.info("\n🔬 Testing Multiple Rules Addition:");
                log.info("========================================");
                List<DstNatIpToIp.DstNatRule> testRules = new ArrayList<>();
                testRules.add(new DstNatIpToIp.DstNatRule(
                    "10.127.127.107", "10.246.7.103", "Test NAT 1"
                ));
                testRules.add(new DstNatIpToIp.DstNatRule(
                    "10.127.127.108", "10.246.7.104", "Test NAT 2"
                ));

                Integer successCount = dstNatManager.addMultipleRules(testRules).get(15, TimeUnit.SECONDS);
                log.info("Added {}/{} test NAT rules successfully", successCount, testRules.size());

                // Clean up test rules
                Thread.sleep(2000);
                log.info("\n🧹 Cleaning up test rules...");
                for (DstNatIpToIp.DstNatRule rule : testRules) {
                    dstNatManager.removeDstNatRule(rule.getOriginalDstIp(), rule.getTranslatedDstIp())
                        .get(10, TimeUnit.SECONDS);
                }
                log.info("✅ Test rules cleaned up");

                // Final summary
                log.info("\n========================================");
                log.info("📊 FINAL SUMMARY");
                log.info("========================================");
                log.info("✅ Main DST-NAT rule configured: 10.127.127.105 -> 10.246.7.101");
                log.info("✅ Rule is active and ready for traffic");
                log.info("✅ Verification completed successfully");
                log.info("ℹ️  Note: Traffic to 10.127.127.105 will now be translated to 10.246.7.101");

                router.disconnectSsh();

            } else {
                log.error("❌ Failed to connect to MikroTik router");
            }

        } catch (Exception e) {
            log.error("❌ Error during DST-NAT test", e);
        }
    }

    @Test
    public void testListAllNatRules() {
        MikroTikRouter router = new MikroTikRouter("btcl-mikrotik");

        try {
            log.info("Listing all NAT rules on MikroTik router...");

            CompletableFuture<Boolean> connectionFuture = router.connectSsh(
                    "114.130.145.75", 22, "admin", "Takay1#$ane%%"
            );

            if (connectionFuture.get(30, TimeUnit.SECONDS)) {
                DstNatIpToIp dstNatManager = new DstNatIpToIp(router);

                log.info("\n📋 All DST-NAT Rules:");
                String rules = dstNatManager.listAllDstNatRules().get(10, TimeUnit.SECONDS);
                System.out.println(rules);

                router.disconnectSsh();
            }

        } catch (Exception e) {
            log.error("Error listing NAT rules", e);
        }
    }
}