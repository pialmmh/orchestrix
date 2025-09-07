package com.telcobright.orchestrix.device;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Slf4j
public class MikroTikRouterOSTest {
    
    @Test
    public void testRouterOSPromptBasedCommands() {
        MikroTikRouter router = new MikroTikRouter("smsmk01");
        
        try {
            log.info("Testing RouterOS prompt-based command execution...");
            
            CompletableFuture<Boolean> connectionFuture = router.connect(
                "114.130.145.70", 22, "admin", "Takay1#$ane%%"
            );
            
            Boolean connected = connectionFuture.get(60, TimeUnit.SECONDS);
            
            if (connected) {
                log.info("✅ Connected to RouterOS! Testing prompt-based commands...");
                
                // Test the exact commands that work manually
                testRouterOSCommand(router, "/ip route print", "IP Routes");
                testRouterOSCommand(router, "/system identity print", "System Identity");
                testRouterOSCommand(router, "/interface print brief", "Interfaces Brief");
                testRouterOSCommand(router, "/ip address print", "IP Addresses");
                testRouterOSCommand(router, "/system resource print", "System Resources");
                
                router.disconnect();
                log.info("✅ RouterOS prompt-based tests completed successfully");
                
            } else {
                log.error("❌ Failed to connect to RouterOS");
            }
            
        } catch (Exception e) {
            log.error("❌ Error during RouterOS prompt test", e);
        }
    }
    
    private void testRouterOSCommand(MikroTikRouter router, String command, String description) {
        try {
            log.info("🔍 Testing {}: {}", description, command);
            
            CompletableFuture<String> future = router.executeCustomCommand(command);
            String response = future.get(30, TimeUnit.SECONDS);
            
            if (response != null && !response.trim().isEmpty()) {
                String[] lines = response.split("\n");
                log.info("📋 {} Response ({} lines):", description, lines.length);
                
                // Show first few lines of actual data
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
                
                log.info("✅ {} - Success ({} characters)", description, response.length());
            } else {
                log.warn("⚠️  {} - Empty response", description);
            }
            
        } catch (Exception e) {
            log.error("❌ {} - Error: {}", description, e.getMessage());
        }
        
        // Wait between commands to be nice to RouterOS
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}