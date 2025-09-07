package com.telcobright.orchestrix.device;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Slf4j
public class MikroTikInteractiveTest {
    
    @Test
    public void testInteractiveCommands() {
        MikroTikRouter router = new MikroTikRouter("smsmk01");
        
        try {
            log.info("Connecting to smsmk01...");
            
            CompletableFuture<Boolean> connectionFuture = router.connect(
                "114.130.145.70", 
                22, 
                "admin", 
                "Takay1#$ane%%"
            );
            
            Boolean connected = connectionFuture.get(30, TimeUnit.SECONDS);
            
            if (connected) {
                log.info("✅ Connected! Executing commands...");
                
                // Wait a bit for the session to stabilize
                Thread.sleep(2000);
                
                // Send raw commands and check responses
                testCommand(router, "/system identity print");
                testCommand(router, "/interface print brief");
                testCommand(router, "/ip address print");
                testCommand(router, "/ip route print where default=yes");
                
                // Use the built-in getRoutes method
                log.info("🔍 Getting routes using built-in method...");
                CompletableFuture<String> routesFuture = router.getRoutes();
                String routes = routesFuture.get(10, TimeUnit.SECONDS);
                log.info("📋 Routes: {}", routes);
                
                testCommand(router, "/system resource print");
                
                router.disconnect();
                log.info("✅ Test completed and disconnected");
                
            } else {
                log.error("❌ Failed to connect");
            }
            
        } catch (Exception e) {
            log.error("❌ Error during test", e);
        }
    }
    
    private void testCommand(MikroTikRouter router, String command) {
        try {
            log.info("🔍 Executing: {}", command);
            CompletableFuture<String> future = router.executeCustomCommand(command);
            String response = future.get(10, TimeUnit.SECONDS);
            
            // Clean up the response for better logging
            String cleanResponse = response.replaceAll("\\[admin@TB_Mikrotik_01_Borak\\].*?>", "").trim();
            
            if (!cleanResponse.isEmpty()) {
                log.info("📋 Response for '{}': {}", command, cleanResponse);
            } else {
                log.warn("⚠️  Empty response for: {}", command);
            }
            
        } catch (Exception e) {
            log.error("❌ Error executing '{}': {}", command, e.getMessage());
        }
    }
}