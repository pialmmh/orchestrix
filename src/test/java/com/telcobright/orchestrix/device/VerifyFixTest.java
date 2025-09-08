package com.telcobright.orchestrix.device;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Slf4j
public class VerifyFixTest {
    
    @Test
    public void verifyPortForwardingFix() {
        MikroTikRouter router = new MikroTikRouter("smsmk01");
        
        try {
            log.info("Connecting to smsmk01 to verify port forwarding fix...");
            
            CompletableFuture<Boolean> connectionFuture = router.connect(
                "114.130.145.70", 
                22, 
                "admin", 
                "Takay1#$ane%%"
            );
            
            Boolean connected = connectionFuture.get(60, TimeUnit.SECONDS);
            
            if (connected) {
                log.info("✅ Connected! Verifying port forwarding configuration...");
                
                // Check INPUT chain rules for port 50005
                log.info("🔍 Checking INPUT chain for port 50005...");
                CompletableFuture<String> inputFuture = router.executeCustomCommand("/ip firewall filter print where chain=input and dst-port=50005");
                String inputResult = inputFuture.get(15, TimeUnit.SECONDS);
                
                log.info("📋 INPUT chain rules for port 50005:");
                log.info("================");
                log.info("{}", inputResult);
                log.info("================");
                
                // Check DSTNAT rules for port 50005
                log.info("🔍 Checking DSTNAT chain for port 50005...");
                CompletableFuture<String> dstnatFuture = router.executeCustomCommand("/ip firewall nat print where chain=dstnat and dst-port=50005");
                String dstnatResult = dstnatFuture.get(15, TimeUnit.SECONDS);
                
                log.info("📋 DSTNAT chain rules for port 50005:");
                log.info("================");
                log.info("{}", dstnatResult);
                log.info("================");
                
                // Also check all INPUT chain rules to see the context
                log.info("🔍 Checking ALL INPUT chain rules...");
                CompletableFuture<String> allInputFuture = router.executeCustomCommand("/ip firewall filter print where chain=input");
                String allInputResult = allInputFuture.get(15, TimeUnit.SECONDS);
                
                log.info("📋 ALL INPUT chain rules:");
                log.info("================");
                log.info("{}", allInputResult);
                log.info("================");
                
                // Analysis
                boolean hasInputRule = inputResult != null && inputResult.contains("50005") && inputResult.contains("accept");
                boolean hasDstnatRule = dstnatResult != null && dstnatResult.contains("192.168.22.4") && dstnatResult.contains("50005");
                
                log.info("🔍 VERIFICATION RESULTS:");
                
                if (hasInputRule) {
                    log.info("✅ INPUT chain rule for port 50005: FOUND and configured to ACCEPT");
                } else {
                    log.warn("❌ INPUT chain rule for port 50005: NOT FOUND or not configured properly");
                }
                
                if (hasDstnatRule) {
                    log.info("✅ DSTNAT rule for port 50005: FOUND and forwarding to 192.168.22.4:22");
                } else {
                    log.warn("❌ DSTNAT rule for port 50005: NOT FOUND or not configured properly");
                }
                
                if (hasInputRule && hasDstnatRule) {
                    log.info("🎉 PORT FORWARDING SHOULD NOW WORK!");
                    log.info("📞 Test external SSH connection: ssh admin@114.130.145.70 -p 50005");
                    log.info("🔐 Password: Takay1#$ane%%");
                } else {
                    log.warn("⚠️ Port forwarding may still have issues - some rules missing");
                }
                
                router.disconnect();
                log.info("✅ Verification completed and disconnected");
                
            } else {
                log.error("❌ Failed to connect to router");
            }
            
        } catch (Exception e) {
            log.error("❌ Error during verification", e);
        }
    }
}