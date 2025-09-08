package com.telcobright.orchestrix.device;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Slf4j
public class AddSrcnatRuleTest {
    
    @Test
    public void addSrcnatForPortForwarding() {
        MikroTikRouter router = new MikroTikRouter("smsmk01");
        
        try {
            log.info("Connecting to smsmk01 to add SRCNAT rule for port forwarding...");
            
            CompletableFuture<Boolean> connectionFuture = router.connect(
                "114.130.145.70", 
                22, 
                "admin", 
                "Takay1#$ane%%"
            );
            
            Boolean connected = connectionFuture.get(60, TimeUnit.SECONDS);
            
            if (connected) {
                log.info("✅ Connected! Adding SRCNAT rule for forwarded traffic...");
                
                // Add SRCNAT rule for forwarded SSH traffic to ensure reply packets route correctly
                String srcnatCommand = "/ip firewall nat add " +
                    "chain=srcnat " +
                    "action=src-nat " +
                    "to-addresses=10.246.7.106 " +
                    "protocol=tcp " +
                    "src-address=!192.168.22.0/24 " +
                    "dst-address=192.168.22.4 " +
                    "dst-port=22 " +
                    "comment=\"SRCNAT for SSH to sbcmk01\"";
                
                log.info("🔍 Executing: {}", srcnatCommand);
                CompletableFuture<String> srcnatFuture = router.executeCustomCommand(srcnatCommand);
                String srcnatResult = srcnatFuture.get(15, TimeUnit.SECONDS);
                
                log.info("📋 SRCNAT rule result: [{}]", srcnatResult);
                
                // Try alternative SRCNAT rule targeting the specific external interface
                log.info("🔍 Adding interface-specific SRCNAT rule...");
                String interfaceSrcnatCommand = "/ip firewall nat add " +
                    "chain=srcnat " +
                    "action=src-nat " +
                    "to-addresses=114.130.145.70 " +
                    "protocol=tcp " +
                    "dst-address=192.168.22.4 " +
                    "dst-port=22 " +
                    "out-interface=ether3 " +
                    "comment=\"External SRCNAT for SSH to sbcmk01\"";
                
                CompletableFuture<String> intSrcnatFuture = router.executeCustomCommand(interfaceSrcnatCommand);
                String intSrcnatResult = intSrcnatFuture.get(15, TimeUnit.SECONDS);
                
                log.info("📋 Interface SRCNAT rule result: [{}]", intSrcnatResult);
                
                log.info("🎯 PORT FORWARDING SHOULD NOW WORK!");
                log.info("   Test: ssh admin@114.130.145.70 -p 50005");
                log.info("   Password: Takay1#$ane%%");
                
                router.disconnect();
                log.info("✅ SRCNAT rules added and disconnected");
                
            } else {
                log.error("❌ Failed to connect to router");
            }
            
        } catch (Exception e) {
            log.error("❌ Error during SRCNAT rule addition", e);
        }
    }
}