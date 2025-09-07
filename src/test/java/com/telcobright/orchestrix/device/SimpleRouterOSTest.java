package com.telcobright.orchestrix.device;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

@Slf4j
public class SimpleRouterOSTest {
    
    @Test
    public void testSimpleRouterOS() {
        SimpleRouterOSClient client = new SimpleRouterOSClient();
        
        try {
            boolean connected = client.connect("114.130.145.70", 22, "admin", "Takay1#$ane%%").get();
            
            if (connected) {
                log.info("✅ Connected! Testing simple commands...");
                
                String routes = client.executeCommand("/ip route print").get();
                log.info("📋 Routes ({} chars):\n{}", routes.length(), routes.substring(0, Math.min(routes.length(), 500)));
                
                String identity = client.executeCommand("/system identity print").get();
                log.info("📋 Identity: {}", identity);
                
                client.disconnect();
                log.info("✅ Test complete");
            }
            
        } catch (Exception e) {
            log.error("❌ Test failed", e);
        }
    }
}