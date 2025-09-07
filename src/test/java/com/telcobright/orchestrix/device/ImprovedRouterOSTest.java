package com.telcobright.orchestrix.device;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Slf4j
public class ImprovedRouterOSTest {
    
    @Test
    public void testImprovedRouterOSOutputCollection() {
        MikroTikRouter router = new MikroTikRouter("smsmk01");
        
        try {
            log.info("Testing IMPROVED RouterOS output collection...");
            
            CompletableFuture<Boolean> connectionFuture = router.connect(
                "114.130.145.70", 22, "admin", "Takay1#$ane%%"
            );
            
            Boolean connected = connectionFuture.get(60, TimeUnit.SECONDS);
            
            if (connected) {
                log.info("✅ Connected! Testing improved output collection...");
                
                // Test simple command with improved debug logging
                testImprovedCommand(router, "/system identity print", "System Identity");
                
                // Test another command
                testImprovedCommand(router, "/ip route print", "IP Routes");
                
                router.disconnect();
                log.info("✅ Improved RouterOS output collection test completed");
                
            } else {
                log.error("❌ Failed to connect");
            }
            
        } catch (Exception e) {
            log.error("❌ Error during improved output collection test", e);
        }
    }
    
    private void testImprovedCommand(MikroTikRouter router, String command, String description) {
        try {
            log.info("🔍 Testing IMPROVED {}: {}", description, command);
            
            CompletableFuture<String> future = router.executeCustomCommand(command);
            String response = future.get(30, TimeUnit.SECONDS);
            
            if (response != null) {
                if (response.trim().isEmpty()) {
                    log.warn("⚠️  {} - Response is EMPTY (length: {})", description, response.length());
                    log.warn("⚠️  Raw response: '{}'", response);
                } else if (response.contains("Command timeout")) {
                    log.warn("⚠️  {} - Timeout occurred", description);
                } else {
                    String[] lines = response.split("\n");
                    log.info("📋 {} Response ({} lines, {} chars):", description, lines.length, response.length());
                    
                    // Show actual content with line numbers
                    for (int i = 0; i < Math.min(5, lines.length); i++) {
                        log.info("   [{}] '{}'", i+1, lines[i]);
                    }
                    
                    if (lines.length > 5) {
                        log.info("   ... ({} more lines)", lines.length - 5);
                    }
                    
                    log.info("✅ {} - SUCCESS", description);
                }
            } else {
                log.warn("⚠️  {} - NULL response", description);
            }
            
        } catch (Exception e) {
            log.error("❌ {} - Error: {}", description, e.getMessage());
        }
    }
}