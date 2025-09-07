package com.telcobright.orchestrix.device;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Slf4j
public class BasicCommandTest {
    
    @Test
    public void testBasicCommands() {
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
                log.info("✅ Connected! Testing basic commands...");
                
                Thread.sleep(5000);
                
                // Test system identity first (this usually works)
                testCommand(router, "/system identity print", "System Identity");
                
                Thread.sleep(3000);
                
                // Test interface list
                testCommand(router, "/interface print brief", "Interface List");
                
                Thread.sleep(3000);
                
                // Test IP addresses
                testCommand(router, "/ip address print brief", "IP Addresses");
                
                Thread.sleep(3000);
                
                // Test routes with brief flag
                testCommand(router, "/ip route print brief", "Routes Brief");
                
                router.disconnect();
                log.info("✅ Test completed and disconnected");
                
            } else {
                log.error("❌ Failed to connect");
            }
            
        } catch (Exception e) {
            log.error("❌ Error during test", e);
        }
    }
    
    private void testCommand(MikroTikRouter router, String command, String description) {
        try {
            log.info("🔍 Testing {}: {}", description, command);
            CompletableFuture<String> future = router.executeCustomCommand(command);
            String result = future.get(20, TimeUnit.SECONDS);
            log.info("📋 {} result: [{}]", description, result);
        } catch (Exception e) {
            log.error("❌ Failed to execute {}: {}", description, e.getMessage());
        }
    }
}