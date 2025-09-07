package com.telcobright.orchestrix.device;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Slf4j
public class SingleCommandTest {
    
    @Test
    public void testSingleCommand() {
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
                log.info("✅ Connected! Waiting for session to fully initialize...");
                
                // Wait much longer for the session to be ready
                Thread.sleep(10000);
                
                // Try a simple command first - maybe system identity without print
                log.info("🔍 Sending: /system identity");
                CompletableFuture<String> identityFuture = router.executeCustomCommand("/system identity");
                String identity = identityFuture.get(45, TimeUnit.SECONDS);
                log.info("📋 Identity: [{}]", identity);
                
                Thread.sleep(5000);
                
                // Try the route command 
                log.info("🔍 Sending: /ip route");
                CompletableFuture<String> routeFuture = router.executeCustomCommand("/ip route");
                String routes = routeFuture.get(45, TimeUnit.SECONDS);
                log.info("📋 Routes: [{}]", routes);
                
                router.disconnect();
                log.info("✅ Test completed");
                
            } else {
                log.error("❌ Failed to connect");
            }
            
        } catch (Exception e) {
            log.error("❌ Error during test", e);
            e.printStackTrace();
        }
    }
}