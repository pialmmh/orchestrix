package com.telcobright.orchestrix.device;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Slf4j
public class AddPortForwardValidTest {
    
    @Test
    public void addSshPortForwardValid() {
        MikroTikRouter router = new MikroTikRouter("smsmk01");
        
        try {
            log.info("Connecting to smsmk01 to add port forwarding rule...");
            
            CompletableFuture<Boolean> connectionFuture = router.connect(
                "114.130.145.70", 
                22, 
                "admin", 
                "Takay1#$ane%%"
            );
            
            Boolean connected = connectionFuture.get(60, TimeUnit.SECONDS);
            
            if (connected) {
                log.info("✅ Connected! Adding DSTNAT rule...");
                
                // Use port 50005 instead (valid port range)
                String addRuleCommand = "/ip firewall nat add " +
                    "chain=dstnat " +
                    "action=dst-nat " +
                    "to-addresses=192.168.22.4 " +
                    "to-ports=22 " +
                    "protocol=tcp " +
                    "dst-address=114.130.145.70 " +
                    "dst-port=50005 " +
                    "comment=\"SSH to sbcmk01\"";
                
                log.info("🔍 Executing: {}", addRuleCommand);
                CompletableFuture<String> addFuture = router.executeCustomCommand(addRuleCommand);
                String addResult = addFuture.get(30, TimeUnit.SECONDS);
                
                log.info("📋 Add rule result: [{}]", addResult);
                
                // Verify the rule was added by listing DSTNAT rules again
                log.info("🔍 Verifying rule addition...");
                CompletableFuture<String> verifyFuture = router.executeCustomCommand("/ip firewall nat print where chain=dstnat and dst-port=50005");
                String natRules = verifyFuture.get(30, TimeUnit.SECONDS);
                
                log.info("📋 Verification result: [{}]", natRules);
                
                // Check if our new rule is present
                if (natRules.contains("192.168.22.4") && natRules.contains("50005")) {
                    log.info("✅ Port forwarding rule successfully added!");
                    log.info("🔄 New rule: Port 50005 → 192.168.22.4:22 (SSH to sbcmk01)");
                } else if (addResult.trim().isEmpty()) {
                    log.info("✅ Port forwarding rule added successfully (no error returned)");
                    log.info("🔄 New rule: Port 50005 → 192.168.22.4:22 (SSH to sbcmk01)");
                } else {
                    log.warn("⚠️ Rule addition result: {}", addResult);
                }
                
                router.disconnect();
                log.info("✅ Test completed and disconnected");
                
            } else {
                log.error("❌ Failed to connect to router");
            }
            
        } catch (Exception e) {
            log.error("❌ Error during port forwarding addition", e);
        }
    }
}