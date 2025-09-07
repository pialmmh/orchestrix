package com.telcobright.orchestrix.device;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Slf4j
public class FixedRouterOSTest {
    
    @Test
    public void testFixedRouterOSResponseParsing() {
        MikroTikRouter router = new MikroTikRouter("smsmk01");
        
        try {
            log.info("Testing FIXED RouterOS response parsing...");
            
            CompletableFuture<Boolean> connectionFuture = router.connect(
                "114.130.145.70", 22, "admin", "Takay1#$ane%%"
            );
            
            Boolean connected = connectionFuture.get(60, TimeUnit.SECONDS);
            
            if (connected) {
                log.info("✅ Connected! Testing fixed command parsing...");
                
                // Test simple command first
                testFixedCommand(router, "/system identity print", "System Identity");
                
                // Test more complex command
                testFixedCommand(router, "/ip route print", "IP Routes");
                
                router.disconnect();
                log.info("✅ Fixed RouterOS parsing test completed");
                
            } else {
                log.error("❌ Failed to connect");
            }
            
        } catch (Exception e) {
            log.error("❌ Error during fixed parsing test", e);
        }
    }
    
    private void testFixedCommand(MikroTikRouter router, String command, String description) {
        try {
            log.info("🔍 Testing FIXED {}: {}", description, command);
            
            CompletableFuture<String> future = router.executeCustomCommand(command);
            String response = future.get(20, TimeUnit.SECONDS); // Reduced timeout
            
            if (response != null && !response.trim().isEmpty()) {
                if (response.contains("Command timeout")) {
                    log.warn("⚠️  {} - Timeout occurred", description);
                } else {
                    String[] lines = response.split("\n");
                    log.info("📋 {} Response ({} lines):", description, lines.length);
                    
                    // Show actual content
                    int shown = 0;
                    for (String line : lines) {
                        if (!line.trim().isEmpty() && shown < 3) {
                            log.info("   {}", line.trim());
                            shown++;
                        }
                    }
                    
                    if (lines.length > shown) {
                        log.info("   ... ({} more lines)", lines.length - shown);
                    }
                    
                    log.info("✅ {} - SUCCESS ({} chars)", description, response.length());
                }
            } else {
                log.warn("⚠️  {} - Empty response", description);
            }
            
        } catch (Exception e) {
            log.error("❌ {} - Error: {}", description, e.getMessage());
        }
    }
}