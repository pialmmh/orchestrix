package com.telcobright.orchestrix.automation.devices.networking.mikrotik;

import com.telcobright.orchestrix.device.MikroTikRouter;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * MikroTik Destination NAT IP-to-IP automation class.
 * Manages destination NAT rules that translate incoming packets from one IP to another.
 */
@RequiredArgsConstructor
public class DstNatIpToIp {

    private static final Logger log = LoggerFactory.getLogger(DstNatIpToIp.class);

    private final MikroTikRouter router;

    @Data
    public static class DstNatRule {
        private final String originalDstIp;
        private final String translatedDstIp;
        private String comment;
        private String inInterface;
        private String protocol;
        private Integer dstPort;
        private boolean disabled = false;

        public DstNatRule(String originalDstIp, String translatedDstIp) {
            this.originalDstIp = originalDstIp;
            this.translatedDstIp = translatedDstIp;
        }

        public DstNatRule(String originalDstIp, String translatedDstIp, String comment) {
            this.originalDstIp = originalDstIp;
            this.translatedDstIp = translatedDstIp;
            this.comment = comment;
        }
    }

    /**
     * Add a destination NAT rule
     */
    public CompletableFuture<Boolean> addDstNatRule(DstNatRule rule) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                log.info("Adding DST-NAT rule: {} -> {}", rule.getOriginalDstIp(), rule.getTranslatedDstIp());

                StringBuilder command = new StringBuilder("/ip firewall nat add");
                command.append(" chain=dstnat");
                command.append(" dst-address=").append(rule.getOriginalDstIp());
                command.append(" action=dst-nat");
                command.append(" to-addresses=").append(rule.getTranslatedDstIp());

                // Add optional parameters
                if (rule.getInInterface() != null) {
                    command.append(" in-interface=").append(rule.getInInterface());
                }
                if (rule.getProtocol() != null) {
                    command.append(" protocol=").append(rule.getProtocol());
                }
                if (rule.getDstPort() != null) {
                    command.append(" dst-port=").append(rule.getDstPort());
                    command.append(" to-ports=").append(rule.getDstPort());
                }
                if (rule.getComment() != null) {
                    command.append(" comment=\"").append(rule.getComment()).append("\"");
                }
                if (rule.isDisabled()) {
                    command.append(" disabled=yes");
                }

                log.debug("Executing command: {}", command);
                String result = router.executeCustomCommand(command.toString()).get();

                if (result != null && result.toLowerCase().contains("failure")) {
                    log.error("Failed to add DST-NAT rule: {}", result);
                    return false;
                }

                log.info("✅ DST-NAT rule added successfully: {} -> {}",
                         rule.getOriginalDstIp(), rule.getTranslatedDstIp());
                return true;

            } catch (Exception e) {
                log.error("Error adding DST-NAT rule", e);
                return false;
            }
        });
    }

    /**
     * Remove a destination NAT rule
     */
    public CompletableFuture<Boolean> removeDstNatRule(String originalDstIp, String translatedDstIp) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                log.info("Removing DST-NAT rule: {} -> {}", originalDstIp, translatedDstIp);

                String command = String.format(
                    "/ip firewall nat remove [find chain=dstnat dst-address=%s to-addresses=%s]",
                    originalDstIp, translatedDstIp
                );

                String result = router.executeCustomCommand(command).get();

                if (result != null && result.toLowerCase().contains("no such item")) {
                    log.warn("DST-NAT rule not found: {} -> {}", originalDstIp, translatedDstIp);
                    return false;
                }

                log.info("✅ DST-NAT rule removed: {} -> {}", originalDstIp, translatedDstIp);
                return true;

            } catch (Exception e) {
                log.error("Error removing DST-NAT rule", e);
                return false;
            }
        });
    }

    /**
     * Check if a destination NAT rule exists
     */
    public CompletableFuture<Boolean> ruleExists(String originalDstIp, String translatedDstIp) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String command = String.format(
                    "/ip firewall nat print where chain=dstnat dst-address=%s to-addresses=%s",
                    originalDstIp, translatedDstIp
                );

                String result = router.executeCustomCommand(command).get();
                boolean exists = result != null && !result.trim().isEmpty() && !result.contains("no such item");

                if (exists) {
                    log.info("DST-NAT rule exists: {} -> {}", originalDstIp, translatedDstIp);
                } else {
                    log.info("DST-NAT rule does not exist: {} -> {}", originalDstIp, translatedDstIp);
                }

                return exists;

            } catch (Exception e) {
                log.error("Error checking DST-NAT rule existence", e);
                return false;
            }
        });
    }

    /**
     * List all destination NAT rules
     */
    public CompletableFuture<String> listAllDstNatRules() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                log.info("Listing all DST-NAT rules...");
                String result = router.executeCustomCommand("/ip firewall nat print detail where chain=dstnat").get();
                return result != null ? result : "No DST-NAT rules found";

            } catch (Exception e) {
                log.error("Error listing DST-NAT rules", e);
                return "Error: " + e.getMessage();
            }
        });
    }

    /**
     * Get specific DST-NAT rule details
     */
    public CompletableFuture<String> getRuleDetails(String originalDstIp, String translatedDstIp) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String command = String.format(
                    "/ip firewall nat print detail where chain=dstnat dst-address=%s to-addresses=%s",
                    originalDstIp, translatedDstIp
                );

                String result = router.executeCustomCommand(command).get();
                return result != null && !result.trim().isEmpty() ? result : "Rule not found";

            } catch (Exception e) {
                log.error("Error getting rule details", e);
                return "Error: " + e.getMessage();
            }
        });
    }

    /**
     * Enable a disabled DST-NAT rule
     */
    public CompletableFuture<Boolean> enableRule(String originalDstIp, String translatedDstIp) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                log.info("Enabling DST-NAT rule: {} -> {}", originalDstIp, translatedDstIp);

                String command = String.format(
                    "/ip firewall nat enable [find chain=dstnat dst-address=%s to-addresses=%s]",
                    originalDstIp, translatedDstIp
                );

                router.executeCustomCommand(command).get();
                log.info("✅ DST-NAT rule enabled: {} -> {}", originalDstIp, translatedDstIp);
                return true;

            } catch (Exception e) {
                log.error("Error enabling rule", e);
                return false;
            }
        });
    }

    /**
     * Disable a DST-NAT rule
     */
    public CompletableFuture<Boolean> disableRule(String originalDstIp, String translatedDstIp) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                log.info("Disabling DST-NAT rule: {} -> {}", originalDstIp, translatedDstIp);

                String command = String.format(
                    "/ip firewall nat disable [find chain=dstnat dst-address=%s to-addresses=%s]",
                    originalDstIp, translatedDstIp
                );

                router.executeCustomCommand(command).get();
                log.info("✅ DST-NAT rule disabled: {} -> {}", originalDstIp, translatedDstIp);
                return true;

            } catch (Exception e) {
                log.error("Error disabling rule", e);
                return false;
            }
        });
    }

    /**
     * Verify DST-NAT rule is working by checking active connections
     */
    public CompletableFuture<VerificationResult> verifyNatRule(String originalDstIp, String translatedDstIp) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                log.info("Verifying DST-NAT rule: {} -> {}", originalDstIp, translatedDstIp);

                VerificationResult result = new VerificationResult();
                result.setOriginalDstIp(originalDstIp);
                result.setTranslatedDstIp(translatedDstIp);

                // Step 1: Check if rule exists
                boolean exists = ruleExists(originalDstIp, translatedDstIp).get();
                result.setRuleExists(exists);

                if (!exists) {
                    result.setVerificationStatus("Rule does not exist");
                    return result;
                }

                // Step 2: Get rule details
                String ruleDetails = getRuleDetails(originalDstIp, translatedDstIp).get();
                result.setRuleDetails(ruleDetails);

                // Check if rule is active (not disabled)
                boolean isActive = !ruleDetails.contains("X");
                result.setRuleActive(isActive);

                // Step 3: Check NAT connections
                String connectionCommand = String.format(
                    "/ip firewall connection print where dst-address~\"%s\"",
                    originalDstIp
                );
                String connections = router.executeCustomCommand(connectionCommand).get();
                result.setActiveConnections(connections);

                // Step 4: Get NAT statistics
                String statsCommand = "/ip firewall nat print stats where chain=dstnat";
                String stats = router.executeCustomCommand(statsCommand).get();
                result.setNatStatistics(stats);

                // Analyze verification
                if (isActive) {
                    if (connections != null && !connections.trim().isEmpty()) {
                        result.setVerificationStatus("✅ Rule active and connections found");
                        result.setWorking(true);
                    } else {
                        result.setVerificationStatus("⚠️ Rule active but no active connections");
                        result.setWorking(true); // Rule is configured correctly
                    }
                } else {
                    result.setVerificationStatus("❌ Rule is disabled");
                    result.setWorking(false);
                }

                return result;

            } catch (Exception e) {
                log.error("Error verifying NAT rule", e);
                VerificationResult errorResult = new VerificationResult();
                errorResult.setOriginalDstIp(originalDstIp);
                errorResult.setTranslatedDstIp(translatedDstIp);
                errorResult.setVerificationStatus("Error: " + e.getMessage());
                errorResult.setWorking(false);
                return errorResult;
            }
        });
    }

    @Data
    public static class VerificationResult {
        private String originalDstIp;
        private String translatedDstIp;
        private boolean ruleExists;
        private boolean ruleActive;
        private boolean working;
        private String verificationStatus;
        private String ruleDetails;
        private String activeConnections;
        private String natStatistics;

        public String getSummary() {
            StringBuilder sb = new StringBuilder();
            sb.append("\n========== DST-NAT Verification Result ==========\n");
            sb.append("Original IP: ").append(originalDstIp).append("\n");
            sb.append("Translated IP: ").append(translatedDstIp).append("\n");
            sb.append("Rule Exists: ").append(ruleExists).append("\n");
            sb.append("Rule Active: ").append(ruleActive).append("\n");
            sb.append("Working: ").append(working).append("\n");
            sb.append("Status: ").append(verificationStatus).append("\n");
            sb.append("================================================\n");
            return sb.toString();
        }
    }

    /**
     * Get useful CLI commands for manual troubleshooting
     */
    public List<String> getUsefulCliCommands(String originalDstIp, String translatedDstIp) {
        List<String> commands = new ArrayList<>();

        commands.add("# View specific NAT rule:");
        commands.add("/ip firewall nat print where chain=dstnat dst-address=" + originalDstIp);

        commands.add("\n# View all DST-NAT rules:");
        commands.add("/ip firewall nat print where chain=dstnat");

        commands.add("\n# View active connections:");
        commands.add("/ip firewall connection print where dst-address~\"" + originalDstIp + "\"");

        commands.add("\n# View NAT statistics:");
        commands.add("/ip firewall nat print stats");

        commands.add("\n# Test from router (if possible):");
        commands.add("/ping " + originalDstIp);

        commands.add("\n# Monitor real-time connections:");
        commands.add("/ip firewall connection print interval=1 where dst-address~\"" + originalDstIp + "\"");

        return commands;
    }

    /**
     * Add multiple DST-NAT rules
     */
    public CompletableFuture<Integer> addMultipleRules(List<DstNatRule> rules) {
        return CompletableFuture.supplyAsync(() -> {
            int successCount = 0;
            log.info("Adding {} DST-NAT rules...", rules.size());

            for (DstNatRule rule : rules) {
                try {
                    boolean success = addDstNatRule(rule).get();
                    if (success) {
                        successCount++;
                    }
                } catch (Exception e) {
                    log.error("Failed to add rule: {} -> {}",
                             rule.getOriginalDstIp(), rule.getTranslatedDstIp(), e);
                }
            }

            log.info("✅ Successfully added {}/{} DST-NAT rules", successCount, rules.size());
            return successCount;
        });
    }

    /**
     * Clear all DST-NAT rules (use with caution!)
     */
    public CompletableFuture<Boolean> clearAllDstNatRules() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                log.warn("⚠️ Clearing ALL DST-NAT rules...");
                String command = "/ip firewall nat remove [find chain=dstnat]";
                router.executeCustomCommand(command).get();
                log.info("✅ All DST-NAT rules cleared");
                return true;

            } catch (Exception e) {
                log.error("Error clearing DST-NAT rules", e);
                return false;
            }
        });
    }
}