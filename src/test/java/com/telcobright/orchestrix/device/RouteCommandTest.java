package com.telcobright.orchestrix.device;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Slf4j
public class RouteCommandTest {
    
    @Test
    public void testRouteCommand() {
        MikroTikRouter router = new MikroTikRouter("smsmk01");
        
        try {
            log.info("Connecting to smsmk01 for route test...");
            
            CompletableFuture<Boolean> connectionFuture = router.connect(
                "114.130.145.70", 
                22, 
                "admin", 
                "Takay1#$ane%%"
            );
            
            Boolean connected = connectionFuture.get(60, TimeUnit.SECONDS);
            
            if (connected) {
                log.info("✅ Connected! Executing route command...");
                
                // Try the exact command that worked manually
                log.info("🔍 Executing: /ip route print");
                CompletableFuture<String> routeFuture = router.executeCustomCommand("/ip route print");
                String routes = routeFuture.get(90, TimeUnit.SECONDS);
                
                log.info("📋 Route output:");
                log.info("================");
                log.info("{}", routes);
                log.info("================");
                
                // Extract default gateway if found
                if (routes.contains("0.0.0.0/0")) {
                    log.info("🎯 Found default route in output!");
                    String[] lines = routes.split("\n");
                    for (String line : lines) {
                        if (line.contains("0.0.0.0/0")) {
                            log.info("🔴 DEFAULT GATEWAY LINE: {}", line.trim());
                            // Try to extract the gateway IP
                            String[] parts = line.trim().split("\\s+");
                            for (int i = 0; i < parts.length; i++) {
                                if (parts[i].equals("0.0.0.0/0") && i + 2 < parts.length) {
                                    String gateway = parts[i + 2];
                                    log.info("🌐 EXTRACTED DEFAULT GATEWAY: {}", gateway);
                                    break;
                                }
                            }
                        }
                    }
                } else {
                    log.warn("⚠️  No default route found in output");
                }
                
                router.disconnect();
                log.info("✅ Test completed and disconnected");
                
            } else {
                log.error("❌ Failed to connect to router");
            }
            
        } catch (Exception e) {
            log.error("❌ Error during route test", e);
            e.printStackTrace();
        }
    }
}