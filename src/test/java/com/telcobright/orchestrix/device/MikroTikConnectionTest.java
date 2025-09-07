package com.telcobright.orchestrix.device;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CompletableFuture;

@Slf4j
public class MikroTikConnectionTest {
    
    @Test
    public void testConnectToSMSMK01() {
        MikroTikRouter router = new MikroTikRouter("smsmk01");
        
        try {
            log.info("Attempting to connect to smsmk01...");
            
            CompletableFuture<Boolean> connectionFuture = router.connect(
                "114.130.145.70", 
                22, 
                "admin", 
                "Takay1#$ane%%"
            );
            
            Boolean connected = connectionFuture.get();
            
            if (connected) {
                log.info("✅ Successfully connected to smsmk01!");
                
                // Test system identity
                CompletableFuture<String> identityFuture = router.getSystemInfo();
                String identity = identityFuture.get();
                log.info("System Identity: {}", identity);
                
                // Test interface list
                CompletableFuture<String> interfacesFuture = router.getInterfaces();
                String interfaces = interfacesFuture.get();
                log.info("Interfaces: {}", interfaces);
                
                // Test IP addresses
                CompletableFuture<String> ipAddressesFuture = router.getIpAddresses();
                String ipAddresses = ipAddressesFuture.get();
                log.info("IP Addresses: {}", ipAddresses);
                
                // Disconnect
                router.disconnect();
                log.info("✅ Disconnected from smsmk01");
                
            } else {
                log.error("❌ Failed to connect to smsmk01");
            }
            
        } catch (Exception e) {
            log.error("❌ Error during connection test", e);
        }
    }
}