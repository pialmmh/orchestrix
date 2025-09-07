package com.telcobright.orchestrix.device;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Slf4j
public class SimpleRouteTest {
    
    @Test
    public void getSimpleRoutes() {
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
                log.info("✅ Connected! Getting routing table...");
                
                // Wait for session to stabilize
                Thread.sleep(5000);
                
                // Try the built-in getRoutes method first
                log.info("🔍 Using built-in getRoutes() method...");
                CompletableFuture<String> routesFuture = router.getRoutes();
                String routes = routesFuture.get(30, TimeUnit.SECONDS);
                log.info("📋 Built-in Routes: [{}]", routes);
                
                // Small delay between commands
                Thread.sleep(2000);
                
                // Try a simple /ip route print
                log.info("🔍 Using simple /ip route print...");
                CompletableFuture<String> simpleRouteFuture = router.executeCustomCommand("/ip route print");
                String simpleRoute = simpleRouteFuture.get(30, TimeUnit.SECONDS);
                log.info("📋 Simple route: [{}]", simpleRoute);
                
                router.disconnect();
                log.info("✅ Test completed and disconnected");
                
            } else {
                log.error("❌ Failed to connect");
            }
            
        } catch (Exception e) {
            log.error("❌ Error during test", e);
        }
    }
}