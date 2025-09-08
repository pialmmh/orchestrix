package com.telcobright.orchestrix.device;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Slf4j
public class NatRulesTest {
    
    @Test
    public void getDstNatRules() {
        MikroTikRouter router = new MikroTikRouter("smsmk01");
        
        try {
            log.info("Connecting to smsmk01 for DSTNAT rules...");
            
            CompletableFuture<Boolean> connectionFuture = router.connect(
                "114.130.145.70", 
                22, 
                "admin", 
                "Takay1#$ane%%"
            );
            
            Boolean connected = connectionFuture.get(60, TimeUnit.SECONDS);
            
            if (connected) {
                log.info("✅ Connected! Getting DSTNAT rules...");
                
                // Get DSTNAT firewall rules
                log.info("🔍 Executing: /ip firewall nat print where chain=dstnat");
                CompletableFuture<String> natFuture = router.executeCustomCommand("/ip firewall nat print where chain=dstnat");
                String natRules = natFuture.get(90, TimeUnit.SECONDS);
                
                log.info("📋 DSTNAT Rules:");
                log.info("================");
                log.info("{}", natRules);
                log.info("================");
                
                // Parse and summarize the port forwarding rules
                if (natRules != null && !natRules.trim().isEmpty()) {
                    String[] lines = natRules.split("\n");
                    int ruleCount = 0;
                    
                    for (String line : lines) {
                        if (line.trim().matches("^\\d+.*")) { // Lines starting with rule number
                            ruleCount++;
                            log.info("🔄 Port Forward Rule {}: {}", ruleCount, line.trim());
                        }
                    }
                    
                    if (ruleCount > 0) {
                        log.info("📊 Total DSTNAT port forwarding rules: {}", ruleCount);
                    } else {
                        log.info("ℹ️ No DSTNAT port forwarding rules found or rules are in different format");
                    }
                } else {
                    log.info("ℹ️ No DSTNAT rules configured");
                }
                
                router.disconnect();
                log.info("✅ Test completed and disconnected");
                
            } else {
                log.error("❌ Failed to connect to router");
            }
            
        } catch (Exception e) {
            log.error("❌ Error during NAT rules test", e);
        }
    }
}