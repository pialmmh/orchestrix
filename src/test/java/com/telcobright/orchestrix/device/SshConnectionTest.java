package com.telcobright.orchestrix.device;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Slf4j
public class SshConnectionTest {
    
    @Test
    public void testSshToTarget() {
        MikroTikRouter router = new MikroTikRouter("smsmk01");
        
        try {
            log.info("Connecting to smsmk01 to test SSH to target...");
            
            CompletableFuture<Boolean> connectionFuture = router.connect(
                "114.130.145.70", 
                22, 
                "admin", 
                "Takay1#$ane%%"
            );
            
            Boolean connected = connectionFuture.get(60, TimeUnit.SECONDS);
            
            if (connected) {
                log.info("✅ Connected! Testing SSH connection to 192.168.22.4...");
                
                // Test SSH connection from router to target
                // First check if SSH service is listening on port 22
                String sshTestCommand = "/tool netwatch add host=192.168.22.4 port=22 timeout=5s";
                
                log.info("🔍 Setting up netwatch test for SSH port...");
                CompletableFuture<String> netwatchFuture = router.executeCustomCommand(sshTestCommand);
                String netwatchResult = netwatchFuture.get(10, TimeUnit.SECONDS);
                
                log.info("📋 Netwatch setup result: [{}]", netwatchResult);
                
                // Wait a moment for netwatch to test
                Thread.sleep(3000);
                
                // Check netwatch results
                log.info("🔍 Checking netwatch status...");
                CompletableFuture<String> statusFuture = router.executeCustomCommand("/tool netwatch print");
                String statusResult = statusFuture.get(10, TimeUnit.SECONDS);
                
                log.info("📋 Netwatch status:");
                log.info("================");
                log.info("{}", statusResult);
                log.info("================");
                
                // Clean up the netwatch entry
                log.info("🧹 Cleaning up netwatch entry...");
                CompletableFuture<String> cleanupFuture = router.executeCustomCommand("/tool netwatch remove [find host=192.168.22.4]");
                String cleanupResult = cleanupFuture.get(10, TimeUnit.SECONDS);
                
                // Analyze the results
                if (statusResult != null) {
                    if (statusResult.contains("up") || statusResult.contains("reachable")) {
                        log.info("✅ SSH service appears to be running on 192.168.22.4:22");
                    } else if (statusResult.contains("down") || statusResult.contains("timeout")) {
                        log.warn("❌ SSH service may not be running on 192.168.22.4:22");
                    } else {
                        log.info("ℹ️ SSH connectivity test result: {}", statusResult.trim());
                    }
                }
                
                // Try a simple TCP connection test
                log.info("🔍 Testing TCP connection to port 22...");
                String tcpTestCommand = "/tool tcp-test connect-to=192.168.22.4 port=22 count=1";
                CompletableFuture<String> tcpFuture = router.executeCustomCommand(tcpTestCommand);
                String tcpResult = tcpFuture.get(15, TimeUnit.SECONDS);
                
                log.info("📋 TCP connection test result:");
                log.info("================");
                log.info("{}", tcpResult);
                log.info("================");
                
                if (tcpResult != null) {
                    if (tcpResult.contains("connected") || tcpResult.contains("success")) {
                        log.info("✅ TCP connection to 192.168.22.4:22 SUCCESSFUL");
                        log.info("🎯 SSH service is likely running and accessible");
                    } else if (tcpResult.contains("timeout") || tcpResult.contains("refused")) {
                        log.warn("❌ TCP connection to 192.168.22.4:22 FAILED");
                        log.warn("🚫 SSH service may not be running or blocked");
                    } else {
                        log.info("ℹ️ TCP test result: {}", tcpResult.trim());
                    }
                }
                
                router.disconnect();
                log.info("✅ Test completed and disconnected");
                
            } else {
                log.error("❌ Failed to connect to router");
            }
            
        } catch (Exception e) {
            log.error("❌ Error during SSH connection test", e);
        }
    }
}