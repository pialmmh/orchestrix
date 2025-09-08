package com.telcobright.orchestrix.device;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Slf4j
public class DirectSshTest {
    
    @Test
    public void testDirectSshConnection() {
        MikroTikRouter router = new MikroTikRouter("smsmk01");
        
        try {
            log.info("Connecting to smsmk01 to test direct SSH connection...");
            
            CompletableFuture<Boolean> connectionFuture = router.connect(
                "114.130.145.70", 
                22, 
                "admin", 
                "Takay1#$ane%%"
            );
            
            Boolean connected = connectionFuture.get(60, TimeUnit.SECONDS);
            
            if (connected) {
                log.info("✅ Connected! Testing SSH connection to 192.168.22.4...");
                
                // Test if we can initiate SSH connection (this will likely fail due to no credentials, but will tell us if SSH service is running)
                log.info("🔍 Testing SSH service availability on 192.168.22.4:22...");
                
                // Try to connect with SSH (expect authentication failure but connection success)
                String sshTestCommand = "ssh -o ConnectTimeout=5 -o BatchMode=yes admin@192.168.22.4 exit";
                
                log.info("🔍 Executing: {}", sshTestCommand);
                CompletableFuture<String> sshFuture = router.executeCustomCommand(sshTestCommand);
                String sshResult = sshFuture.get(15, TimeUnit.SECONDS);
                
                log.info("📋 SSH test result:");
                log.info("================");
                log.info("{}", sshResult);
                log.info("================");
                
                // Alternative approach: use RouterOS system script to test connection
                log.info("🔍 Testing with system script approach...");
                String scriptCommand = ":put [/tool fetch url=\"192.168.22.4:22\" mode=tcp as-value]";
                
                CompletableFuture<String> scriptFuture = router.executeCustomCommand(scriptCommand);
                String scriptResult = scriptFuture.get(15, TimeUnit.SECONDS);
                
                log.info("📋 Script test result:");
                log.info("================");
                log.info("{}", scriptResult);
                log.info("================");
                
                // Try simple telnet-like connection test
                log.info("🔍 Testing basic port connectivity...");
                String portTestCommand = "/system resource print";
                
                CompletableFuture<String> resourceFuture = router.executeCustomCommand(portTestCommand);
                String resourceResult = resourceFuture.get(10, TimeUnit.SECONDS);
                
                // Now try a simple connection test with fetch
                log.info("🔍 Using fetch to test TCP port 22...");
                String fetchCommand = "/tool fetch url=\"tcp://192.168.22.4:22\" mode=tcp";
                
                CompletableFuture<String> fetchFuture = router.executeCustomCommand(fetchCommand);
                String fetchResult = fetchFuture.get(20, TimeUnit.SECONDS);
                
                log.info("📋 Fetch result:");
                log.info("================");
                log.info("{}", fetchResult);
                log.info("================");
                
                // Analyze results
                if (fetchResult != null && (fetchResult.contains("connecting") || fetchResult.contains("connected") || !fetchResult.contains("timeout"))) {
                    log.info("✅ TCP connection to port 22 appears successful");
                    log.info("🎯 SSH service is likely running on 192.168.22.4");
                } else if (fetchResult != null && (fetchResult.contains("timeout") || fetchResult.contains("refused"))) {
                    log.warn("❌ TCP connection to port 22 failed");
                    log.warn("🚫 SSH service may not be running on 192.168.22.4");
                } else {
                    log.info("ℹ️ Connection test completed with mixed results");
                }
                
                router.disconnect();
                log.info("✅ Test completed and disconnected");
                
            } else {
                log.error("❌ Failed to connect to router");
            }
            
        } catch (Exception e) {
            log.error("❌ Error during direct SSH test", e);
        }
    }
}