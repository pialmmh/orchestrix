package com.telcobright.orchestrix.device;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CompletableFuture;

@Slf4j
public class MikroTikSimpleTest {
    
    @Test
    public void testSimpleMikroTikCommands() {
        MikroTikSshClient client = new MikroTikSshClient();
        
        try {
            log.info("Testing simple MikroTik SSH client...");
            
            // Connect
            CompletableFuture<Boolean> connectionFuture = client.connect(
                "114.130.145.70", 22, "admin", "Takay1#$ane%%"
            );
            
            Boolean connected = connectionFuture.get();
            
            if (connected) {
                log.info("✅ Connected! Testing RouterOS commands...");
                
                // Test various commands
                testCommand(client, "/system identity print", "System Identity");
                testCommand(client, "/system resource print", "System Resources");
                testCommand(client, "/ip route print count-only", "Route Count");
                testCommand(client, "/interface print count-only", "Interface Count");
                testCommand(client, "/ip address print", "IP Addresses");
                
                client.disconnect();
                log.info("✅ All tests completed");
                
            } else {
                log.error("❌ Failed to connect");
            }
            
        } catch (Exception e) {
            log.error("❌ Error during test", e);
        }
    }
    
    private void testCommand(MikroTikSshClient client, String command, String description) {
        try {
            log.info("🔍 Testing {}: {}", description, command);
            
            CompletableFuture<String> future = client.executeCommand(command);
            String response = future.get();
            
            if (response != null && !response.trim().isEmpty()) {
                // Show first few lines
                String[] lines = response.split("\n");
                int linesToShow = Math.min(lines.length, 3);
                
                log.info("📋 {} Response ({} lines):", description, lines.length);
                for (int i = 0; i < linesToShow; i++) {
                    log.info("   {}", lines[i].trim());
                }
                if (lines.length > linesToShow) {
                    log.info("   ... ({} more lines)", lines.length - linesToShow);
                }
                
                log.info("✅ {} - Success", description);
            } else {
                log.warn("⚠️  {} - Empty response", description);
            }
            
        } catch (Exception e) {
            log.error("❌ {} - Error: {}", description, e.getMessage());
        }
        
        // Wait between commands
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}