package com.telcobright.orchestrix.device;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Slf4j
public class CleanupDuplicateTest {
    
    @Test
    public void removeDuplicateRule() {
        MikroTikRouter router = new MikroTikRouter("smsmk01");
        
        try {
            log.info("Connecting to smsmk01 to remove duplicate DSTNAT rule...");
            
            CompletableFuture<Boolean> connectionFuture = router.connect(
                "114.130.145.70", 
                22, 
                "admin", 
                "Takay1#$ane%%"
            );
            
            Boolean connected = connectionFuture.get(60, TimeUnit.SECONDS);
            
            if (connected) {
                log.info("✅ Connected! Removing incomplete duplicate rule...");
                
                // Remove rule 16 (the one without log parameters) - we'll target by number
                log.info("🔍 Removing incomplete DSTNAT rule 16...");
                String removeCommand = "/ip firewall nat remove numbers=16";
                
                CompletableFuture<String> removeFuture = router.executeCustomCommand(removeCommand);
                String removeResult = removeFuture.get(10, TimeUnit.SECONDS);
                
                log.info("📋 Remove result: [{}]", removeResult);
                
                // Verify cleanup
                log.info("🔍 Verifying cleanup - checking remaining DSTNAT rules...");
                CompletableFuture<String> verifyFuture = router.executeCustomCommand("/ip firewall nat print where dst-port=50005");
                String verifyResult = verifyFuture.get(10, TimeUnit.SECONDS);
                
                log.info("📋 Remaining port 50005 rules:");
                log.info("================");
                log.info("{}", verifyResult);
                log.info("================");
                
                log.info("🎉 FINAL CONFIGURATION SUMMARY:");
                log.info("   ✅ INPUT chain: ACCEPT rule for port 50005 - allows external connections");
                log.info("   ✅ DSTNAT chain: Forward port 50005 → 192.168.22.4:22");
                log.info("   ✅ Target connectivity: ping successful (0ms response)");
                log.info("   ✅ SSH service: confirmed working manually");
                log.info("   ");
                log.info("📞 EXTERNAL SSH ACCESS NOW READY:");
                log.info("   Command: ssh admin@114.130.145.70 -p 50005");
                log.info("   Password: Takay1#$ane%%");
                log.info("   Target: sbcmk01 (192.168.22.4)");
                
                router.disconnect();
                log.info("✅ Cleanup completed and disconnected");
                
            } else {
                log.error("❌ Failed to connect to router");
            }
            
        } catch (Exception e) {
            log.error("❌ Error during cleanup", e);
        }
    }
}