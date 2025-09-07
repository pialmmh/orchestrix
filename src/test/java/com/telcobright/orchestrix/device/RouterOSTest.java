package com.telcobright.orchestrix.device;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CompletableFuture;

@Slf4j
public class RouterOSTest {
    
    @Test
    public void testRouterOSCommands() {
        RouterOSSshClient client = new RouterOSSshClient();
        
        try {
            log.info("Testing RouterOS-specific SSH client...");
            
            CompletableFuture<Boolean> connectionFuture = client.connect(
                "114.130.145.70", 22, "admin", "Takay1#$ane%%"
            );
            
            Boolean connected = connectionFuture.get();
            
            if (connected) {
                log.info("✅ Connected to RouterOS! Testing commands...");
                
                // Test the exact command that works manually
                testRouterOSCommand(client, "/ip route print", "IP Routes");
                testRouterOSCommand(client, "/system identity print", "System Identity");
                testRouterOSCommand(client, "/interface print brief", "Interfaces Brief");
                testRouterOSCommand(client, "/ip address print", "IP Addresses");
                testRouterOSCommand(client, "/system resource print", "System Resources");
                
                client.disconnect();
                log.info("✅ RouterOS tests completed");
                
            } else {
                log.error("❌ Failed to connect to RouterOS");
            }
            
        } catch (Exception e) {
            log.error("❌ Error during RouterOS test", e);
        }
    }
    
    private void testRouterOSCommand(RouterOSSshClient client, String command, String description) {
        try {
            log.info("🔍 Testing {}: {}", description, command);
            
            CompletableFuture<String> future = client.executeCommand(command);
            String response = future.get();
            
            if (response != null && !response.trim().isEmpty()) {
                // Show response details
                String[] lines = response.split("\n");
                log.info("📋 {} Response ({} lines):", description, lines.length);
                
                // Show first few lines of actual data
                int shown = 0;
                for (String line : lines) {
                    if (!line.trim().isEmpty() && shown < 5) {
                        log.info("   {}", line.trim());
                        shown++;
                    }
                }
                
                if (lines.length > shown) {
                    log.info("   ... ({} more lines)", lines.length - shown);
                }
                
                log.info("✅ {} - Success ({} chars)", description, response.length());
            } else {
                log.warn("⚠️  {} - Empty response", description);
            }
            
        } catch (Exception e) {
            log.error("❌ {} - Error: {}", description, e.getMessage());
        }
        
        // Small delay between commands
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}