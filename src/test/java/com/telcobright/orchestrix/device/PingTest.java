package com.telcobright.orchestrix.device;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Slf4j
public class PingTest {
    
    @Test
    public void pingTarget() {
        MikroTikRouter router = new MikroTikRouter("smsmk01");
        
        try {
            log.info("Connecting to smsmk01 to test ping...");
            
            CompletableFuture<Boolean> connectionFuture = router.connect(
                "114.130.145.70", 
                22, 
                "admin", 
                "Takay1#$ane%%"
            );
            
            Boolean connected = connectionFuture.get(60, TimeUnit.SECONDS);
            
            if (connected) {
                log.info("✅ Connected! Testing ping to 192.168.22.4...");
                
                // Send ping command (RouterOS ping syntax)
                String pingCommand = "/ping 192.168.22.4 count=4";
                
                log.info("🔍 Executing: {}", pingCommand);
                CompletableFuture<String> pingFuture = router.executeCustomCommand(pingCommand);
                String pingResult = pingFuture.get(30, TimeUnit.SECONDS);
                
                log.info("📋 Ping result:");
                log.info("================");
                log.info("{}", pingResult);
                log.info("================");
                
                // Analyze ping results
                if (pingResult != null) {
                    if (pingResult.contains("timeout") || pingResult.contains("host unreachable")) {
                        log.warn("❌ 192.168.22.4 is NOT reachable (timeout/unreachable)");
                    } else if (pingResult.contains("seq=")) {
                        log.info("✅ 192.168.22.4 is REACHABLE (ping responses received)");
                        
                        // Extract packet loss info if available
                        String[] lines = pingResult.split("\n");
                        for (String line : lines) {
                            if (line.contains("packet loss") || line.contains("sent") || line.contains("received")) {
                                log.info("📊 Packet statistics: {}", line.trim());
                            }
                        }
                    } else if (pingResult.trim().isEmpty()) {
                        log.warn("⚠️ Empty ping result - command may have timed out");
                    } else {
                        log.info("ℹ️ Ping result (unclear status): {}", pingResult.trim());
                    }
                } else {
                    log.warn("⚠️ No ping result returned");
                }
                
                router.disconnect();
                log.info("✅ Test completed and disconnected");
                
            } else {
                log.error("❌ Failed to connect to router");
            }
            
        } catch (Exception e) {
            log.error("❌ Error during ping test", e);
        }
    }
}