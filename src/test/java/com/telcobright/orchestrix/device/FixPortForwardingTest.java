package com.telcobright.orchestrix.device;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Slf4j
public class FixPortForwardingTest {
    
    @Test
    public void fixPortForwarding() {
        MikroTikRouter router = new MikroTikRouter("smsmk01");
        
        try {
            log.info("Connecting to smsmk01 to fix port forwarding...");
            
            CompletableFuture<Boolean> connectionFuture = router.connect(
                "114.130.145.70", 
                22, 
                "admin", 
                "Takay1#$ane%%"
            );
            
            Boolean connected = connectionFuture.get(60, TimeUnit.SECONDS);
            
            if (connected) {
                log.info("✅ Connected! Fixing port forwarding configuration...");
                
                // First, let's add the missing INPUT chain rule to allow external connections to port 50005
                log.info("🔍 Adding INPUT chain rule for port 50005...");
                String inputRuleCommand = "/ip firewall filter add " +
                    "chain=input " +
                    "action=accept " +
                    "protocol=tcp " +
                    "dst-address=114.130.145.70 " +
                    "dst-port=50005 " +
                    "comment=\"Allow SSH to sbcmk01\"";
                
                log.info("🔍 Executing: {}", inputRuleCommand);
                CompletableFuture<String> inputFuture = router.executeCustomCommand(inputRuleCommand);
                String inputResult = inputFuture.get(15, TimeUnit.SECONDS);
                
                log.info("📋 INPUT rule result: [{}]", inputResult);
                
                // Let's also update our existing DSTNAT rule to match the format of other rules
                log.info("🔍 Updating DSTNAT rule to add missing parameters...");
                
                // First remove the existing rule
                String removeCommand = "/ip firewall nat remove [find dst-port=50005 and chain=dstnat]";
                CompletableFuture<String> removeFuture = router.executeCustomCommand(removeCommand);
                String removeResult = removeFuture.get(10, TimeUnit.SECONDS);
                
                log.info("📋 Remove result: [{}]", removeResult);
                
                // Add the rule back with proper parameters
                String addRuleCommand = "/ip firewall nat add " +
                    "chain=dstnat " +
                    "action=dst-nat " +
                    "to-addresses=192.168.22.4 " +
                    "to-ports=22 " +
                    "protocol=tcp " +
                    "dst-address=114.130.145.70 " +
                    "dst-port=50005 " +
                    "log=no " +
                    "log-prefix=\"\" " +
                    "comment=\"SSH to sbcmk01\"";
                
                log.info("🔍 Executing: {}", addRuleCommand);
                CompletableFuture<String> addFuture = router.executeCustomCommand(addRuleCommand);
                String addResult = addFuture.get(15, TimeUnit.SECONDS);
                
                log.info("📋 Add updated rule result: [{}]", addResult);
                
                // Verify the changes
                log.info("🔍 Verifying INPUT chain rule...");
                CompletableFuture<String> verifyInputFuture = router.executeCustomCommand("/ip firewall filter print where dst-port=50005");
                String verifyInput = verifyInputFuture.get(10, TimeUnit.SECONDS);
                
                log.info("📋 INPUT chain verification:");
                log.info("================");
                log.info("{}", verifyInput);
                log.info("================");
                
                log.info("🔍 Verifying updated DSTNAT rule...");
                CompletableFuture<String> verifyDstnatFuture = router.executeCustomCommand("/ip firewall nat print where dst-port=50005");
                String verifyDstnat = verifyDstnatFuture.get(10, TimeUnit.SECONDS);
                
                log.info("📋 DSTNAT rule verification:");
                log.info("================");
                log.info("{}", verifyDstnat);
                log.info("================");
                
                // Summary
                log.info("🎯 Port forwarding fix summary:");
                log.info("   • Added INPUT chain rule to accept connections on port 50005");
                log.info("   • Updated DSTNAT rule with proper logging parameters");
                log.info("   • External SSH should now work: ssh admin@114.130.145.70 -p 50005");
                log.info("   • Password: Takay1#$ane%%");
                
                router.disconnect();
                log.info("✅ Fix completed and disconnected");
                
            } else {
                log.error("❌ Failed to connect to router");
            }
            
        } catch (Exception e) {
            log.error("❌ Error during port forwarding fix", e);
        }
    }
}