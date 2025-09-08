package com.telcobright.orchestrix.device;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Slf4j
public class FirewallAnalysisTest {
    
    @Test
    public void analyzeFirewallRules() {
        MikroTikRouter router = new MikroTikRouter("smsmk01");
        
        try {
            log.info("Connecting to smsmk01 to analyze firewall rules...");
            
            CompletableFuture<Boolean> connectionFuture = router.connect(
                "114.130.145.70", 
                22, 
                "admin", 
                "Takay1#$ane%%"
            );
            
            Boolean connected = connectionFuture.get(60, TimeUnit.SECONDS);
            
            if (connected) {
                log.info("✅ Connected! Analyzing firewall configuration...");
                
                // Check input chain firewall rules (these might block incoming connections)
                log.info("🔍 Checking INPUT chain firewall rules...");
                CompletableFuture<String> inputFuture = router.executeCustomCommand("/ip firewall filter print where chain=input");
                String inputRules = inputFuture.get(30, TimeUnit.SECONDS);
                
                log.info("📋 INPUT Chain Rules:");
                log.info("================");
                log.info("{}", inputRules);
                log.info("================");
                
                // Check forward chain firewall rules
                log.info("🔍 Checking FORWARD chain firewall rules...");
                CompletableFuture<String> forwardFuture = router.executeCustomCommand("/ip firewall filter print where chain=forward");
                String forwardRules = forwardFuture.get(30, TimeUnit.SECONDS);
                
                log.info("📋 FORWARD Chain Rules:");
                log.info("================");
                log.info("{}", forwardRules);
                log.info("================");
                
                // Check specifically for our port 50005
                log.info("🔍 Checking for port 50005 specific rules...");
                CompletableFuture<String> portFuture = router.executeCustomCommand("/ip firewall filter print where dst-port=50005");
                String portRules = portFuture.get(15, TimeUnit.SECONDS);
                
                log.info("📋 Port 50005 Specific Rules:");
                log.info("================");
                log.info("{}", portRules);
                log.info("================");
                
                // Verify our DSTNAT rule is still there and correct
                log.info("🔍 Re-checking our DSTNAT rule...");
                CompletableFuture<String> dstnatFuture = router.executeCustomCommand("/ip firewall nat print where dst-port=50005");
                String dstnatRule = dstnatFuture.get(15, TimeUnit.SECONDS);
                
                log.info("📋 Our DSTNAT Rule (Port 50005):");
                log.info("================");
                log.info("{}", dstnatRule);
                log.info("================");
                
                // Check interfaces that might affect external access
                log.info("🔍 Checking interface configuration...");
                CompletableFuture<String> interfaceFuture = router.executeCustomCommand("/interface print");
                String interfaces = interfaceFuture.get(15, TimeUnit.SECONDS);
                
                log.info("📋 Interface List (first 20 lines):");
                log.info("================");
                String[] interfaceLines = interfaces.split("\n");
                for (int i = 0; i < Math.min(interfaceLines.length, 20); i++) {
                    log.info("{}", interfaceLines[i]);
                }
                log.info("================");
                
                // Analysis
                log.info("🔍 ANALYSIS:");
                
                if (inputRules != null && inputRules.contains("drop") && inputRules.contains("50005")) {
                    log.warn("❌ Found DROP rule for port 50005 in INPUT chain - this blocks external access");
                } else if (inputRules != null && inputRules.contains("drop") && !inputRules.contains("accept")) {
                    log.warn("⚠️ INPUT chain has DROP rules but no clear ACCEPT rules - may be blocking traffic");
                } else {
                    log.info("ℹ️ No obvious blocking rules found in INPUT chain for port 50005");
                }
                
                if (forwardRules != null && forwardRules.contains("drop") && forwardRules.contains("50005")) {
                    log.warn("❌ Found DROP rule for port 50005 in FORWARD chain - this blocks forwarding");
                } else {
                    log.info("ℹ️ No obvious blocking rules found in FORWARD chain for port 50005");
                }
                
                if (dstnatRule != null && dstnatRule.contains("192.168.22.4") && dstnatRule.contains("50005")) {
                    log.info("✅ DSTNAT rule is correctly configured");
                } else {
                    log.warn("❌ DSTNAT rule may be missing or incorrectly configured");
                }
                
                router.disconnect();
                log.info("✅ Analysis completed and disconnected");
                
            } else {
                log.error("❌ Failed to connect to router");
            }
            
        } catch (Exception e) {
            log.error("❌ Error during firewall analysis", e);
        }
    }
}