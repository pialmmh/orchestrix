package com.telcobright.orchestrix.device;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Slf4j
public class MikroTikImprovedTest {
    
    @Test
    public void testImprovedRouterOSCommands() {
        MikroTikRouter router = new MikroTikRouter("smsmk01");
        
        try {
            log.info("Testing improved MikroTik RouterOS command parsing...");
            
            CompletableFuture<Boolean> connectionFuture = router.connect(
                "114.130.145.70", 
                22, 
                "admin", 
                "Takay1#$ane%%"
            );
            
            Boolean connected = connectionFuture.get(30, TimeUnit.SECONDS);
            
            if (connected) {
                log.info("✅ Connected! Testing improved command parsing...");
                
                // Wait for session to stabilize
                Thread.sleep(3000);
                
                // Test various RouterOS commands
                testImprovedCommand(router, "/ip route print", "Routing Table");
                testImprovedCommand(router, "/interface print brief", "Interfaces Brief");
                testImprovedCommand(router, "/ip address print", "IP Addresses");
                testImprovedCommand(router, "/system resource print", "System Resources");
                testImprovedCommand(router, "/system identity print", "System Identity");
                
                router.disconnect();
                log.info("✅ Test completed successfully");
                
            } else {
                log.error("❌ Failed to connect");
            }
            
        } catch (Exception e) {
            log.error("❌ Error during improved parsing test", e);
        }
    }
    
    private void testImprovedCommand(MikroTikRouter router, String command, String description) {
        try {
            log.info("🔍 Testing {}: {}", description, command);
            CompletableFuture<String> future = router.executeCustomCommand(command);
            String response = future.get(15, TimeUnit.SECONDS);
            
            if (response != null && !response.trim().isEmpty()) {
                // Log first few lines of actual data
                String[] lines = response.split("\n");
                int dataLines = 0;
                log.info("📋 {} Response:", description);
                
                for (String line : lines) {
                    if (!line.trim().isEmpty() && dataLines < 5) {
                        log.info("   {}", line.trim());
                        dataLines++;
                    }
                }
                
                if (lines.length > 5) {
                    log.info("   ... ({} total lines)", lines.length);
                }
                
                log.info("✅ {} - Success ({} characters)", description, response.length());
            } else {
                log.warn("⚠️  {} - Empty response", description);
            }
            
        } catch (Exception e) {
            log.error("❌ {} - Error: {}", description, e.getMessage());
        }
        
        // Wait between commands
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}