package com.telcobright.orchestrix.device;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Slf4j
public class DeepFirewallAnalysisTest {
    
    @Test
    public void analyzeAllFirewallRules() {
        MikroTikRouter router = new MikroTikRouter("smsmk01");
        
        try {
            log.info("Connecting to smsmk01 for deep firewall analysis...");
            
            CompletableFuture<Boolean> connectionFuture = router.connect(
                "114.130.145.70", 
                22, 
                "admin", 
                "Takay1#$ane%%"
            );
            
            Boolean connected = connectionFuture.get(60, TimeUnit.SECONDS);
            
            if (connected) {
                log.info("✅ Connected! Performing deep firewall analysis...");
                
                // Check ALL filter rules, not just specific chains
                log.info("🔍 Checking ALL firewall filter rules...");
                CompletableFuture<String> allFilterFuture = router.executeCustomCommand("/ip firewall filter print");
                String allFilterRules = allFilterFuture.get(30, TimeUnit.SECONDS);
                
                log.info("📋 ALL Firewall Filter Rules:");
                log.info("================");
                log.info("{}", allFilterRules);
                log.info("================");
                
                // Check which interface is the external interface (ether3 seems to be WAN based on routing)
                log.info("🔍 Checking interface IP addresses...");
                CompletableFuture<String> ipAddressFuture = router.executeCustomCommand("/ip address print");
                String ipAddresses = ipAddressFuture.get(15, TimeUnit.SECONDS);
                
                log.info("📋 Interface IP Addresses:");
                log.info("================");
                log.info("{}", ipAddresses);
                log.info("================");
                
                // Check if there are any src-nat rules that might interfere
                log.info("🔍 Checking SRCNAT rules...");
                CompletableFuture<String> srcnatFuture = router.executeCustomCommand("/ip firewall nat print where chain=srcnat");
                String srcnatRules = srcnatFuture.get(15, TimeUnit.SECONDS);
                
                log.info("📋 SRCNAT Rules:");
                log.info("================");
                log.info("{}", srcnatRules);
                log.info("================");
                
                // Check connection tracking
                log.info("🔍 Checking connection tracking settings...");
                CompletableFuture<String> conntrackFuture = router.executeCustomCommand("/ip firewall connection tracking print");
                String conntrackSettings = conntrackFuture.get(10, TimeUnit.SECONDS);
                
                log.info("📋 Connection Tracking:");
                log.info("================");
                log.info("{}", conntrackSettings);
                log.info("================");
                
                // Test what happens when we try to connect FROM the router to our test location
                log.info("🔍 Testing if the router can connect back to our location...");
                CompletableFuture<String> pingBackFuture = router.executeCustomCommand("/ping 114.130.145.7 count=2");
                String pingBackResult = pingBackFuture.get(15, TimeUnit.SECONDS);
                
                log.info("📋 Ping back to external host:");
                log.info("================");
                log.info("{}", pingBackResult);
                log.info("================");
                
                // Analysis
                log.info("🔍 DEEP ANALYSIS:");
                
                if (allFilterRules != null) {
                    if (allFilterRules.contains("drop") && allFilterRules.contains("input")) {
                        log.warn("❌ Found DROP rules in INPUT chain - these may be blocking external connections");
                        
                        // Count and analyze drop rules
                        String[] lines = allFilterRules.split("\n");
                        int dropRules = 0;
                        int acceptRules = 0;
                        
                        for (String line : lines) {
                            if (line.contains("action=drop") && line.contains("chain=input")) {
                                dropRules++;
                                log.warn("   🚫 DROP rule found: {}", line.trim());
                            }
                            if (line.contains("action=accept") && line.contains("chain=input")) {
                                acceptRules++;
                                if (line.contains("50005")) {
                                    log.info("   ✅ Our ACCEPT rule found: {}", line.trim());
                                }
                            }
                        }
                        
                        log.info("📊 INPUT chain summary: {} DROP rules, {} ACCEPT rules", dropRules, acceptRules);
                        
                        if (dropRules > 0 && acceptRules == 0) {
                            log.error("❌ CRITICAL: INPUT chain has DROP rules but no ACCEPT rules - blocking all external traffic");
                        } else if (dropRules > acceptRules) {
                            log.warn("⚠️ WARNING: More DROP rules than ACCEPT rules - restrictive firewall");
                        }
                    }
                }
                
                router.disconnect();
                log.info("✅ Deep analysis completed and disconnected");
                
            } else {
                log.error("❌ Failed to connect to router");
            }
            
        } catch (Exception e) {
            log.error("❌ Error during deep analysis", e);
        }
    }
}