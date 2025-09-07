package com.telcobright.orchestrix.device;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Slf4j
public class GetDefaultGatewayTest {
    
    @Test
    public void getDefaultGateway() {
        MikroTikRouter router = new MikroTikRouter("smsmk01");
        
        try {
            log.info("Connecting to smsmk01 to get default gateway...");
            
            CompletableFuture<Boolean> connectionFuture = router.connect(
                "114.130.145.70", 
                22, 
                "admin", 
                "Takay1#$ane%%"
            );
            
            Boolean connected = connectionFuture.get(30, TimeUnit.SECONDS);
            
            if (connected) {
                log.info("✅ Connected! Getting default gateway...");
                
                // Wait for session to stabilize
                Thread.sleep(3000);
                
                // Try different commands to get routing information
                log.info("🔍 Trying '/ip route print'...");
                CompletableFuture<String> routesFuture = router.executeCustomCommand("/ip route print");
                String routes = routesFuture.get(15, TimeUnit.SECONDS);
                log.info("📋 Routes output: [{}]", routes);
                
                // Try with specific default route query
                log.info("🔍 Trying '/ip route print where dst-address=0.0.0.0/0'...");
                CompletableFuture<String> defaultRouteFuture = router.executeCustomCommand("/ip route print where dst-address=0.0.0.0/0");
                String defaultRoute = defaultRouteFuture.get(15, TimeUnit.SECONDS);
                log.info("📋 Default route output: [{}]", defaultRoute);
                
                // Try another syntax
                log.info("🔍 Trying '/ip route print where gateway != \"\"'...");
                CompletableFuture<String> gatewayFuture = router.executeCustomCommand("/ip route print where gateway != \"\"");
                String gateway = gatewayFuture.get(15, TimeUnit.SECONDS);
                log.info("📋 Gateway output: [{}]", gateway);
                
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