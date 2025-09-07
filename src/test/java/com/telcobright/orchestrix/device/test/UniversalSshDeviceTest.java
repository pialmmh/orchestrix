package com.telcobright.orchestrix.device.test;

import com.telcobright.orchestrix.device.UniversalSshDevice;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Slf4j
public class UniversalSshDeviceTest {
    
    // Generic SSH device for testing universal SSH implementation
    static class TestSshDevice extends UniversalSshDevice {
        public TestSshDevice(String deviceId) {
            super(deviceId);
            this.vendor = "Generic SSH Device";
        }
        
        @Override
        public CompletableFuture<String> getSystemInfo() {
            try {
                return sendSshCommand("uname -a");
            } catch (Exception e) {
                return CompletableFuture.failedFuture(e);
            }
        }
        
        // Implementation of remaining abstract methods from NetworkingDevice
        @Override
        public CompletableFuture<String> send(String command) throws IOException {
            return sendSshCommand(command);
        }
        
        @Override
        public CompletableFuture<String> receive() throws IOException {
            return receiveSshResponse();
        }
        
        @Override
        public CompletableFuture<String> getInterfaces() {
            try {
                return sendSshCommand("ip addr show");
            } catch (Exception e) {
                return CompletableFuture.failedFuture(e);
            }
        }
        
        @Override
        public CompletableFuture<String> getRoutes() {
            try {
                return sendSshCommand("ip route show");
            } catch (Exception e) {
                return CompletableFuture.failedFuture(e);
            }
        }
        
        @Override
        public CompletableFuture<String> backup(String backupName) {
            try {
                String command = String.format("tar -czf %s.tar.gz ~", backupName);
                return sendSshCommand(command);
            } catch (Exception e) {
                return CompletableFuture.failedFuture(e);
            }
        }
        
        @Override
        public CompletableFuture<String> reboot() {
            try {
                return sendSshCommand("sudo reboot");
            } catch (Exception e) {
                return CompletableFuture.failedFuture(e);
            }
        }
    }
    
    @Test
    public void testUniversalSshImplementation() {
        TestSshDevice device = new TestSshDevice("localhost-test");
        
        try {
            log.info("Testing UNIVERSAL SSH implementation with localhost...");
            
            // Connect to localhost using universal SSH implementation
            CompletableFuture<Boolean> connectionFuture = device.connectSsh(
                "127.0.0.1", 22, "mustafa", "a"
            );
            
            Boolean connected = connectionFuture.get(30, TimeUnit.SECONDS);
            
            if (connected) {
                log.info("✅ Connected! Testing universal SSH commands...");
                
                // Test directory listing
                testUniversalSshCommand(device, "ls ~", "Home Directory List");
                
                // Test system info
                testUniversalSshCommand(device, "uname -a", "System Information");
                
                // Test whoami
                testUniversalSshCommand(device, "whoami", "Current User");
                
                // Test pwd
                testUniversalSshCommand(device, "pwd", "Current Directory");
                
                device.disconnectSsh();
                log.info("✅ Universal SSH implementation test completed");
                
            } else {
                log.error("❌ Failed to connect to localhost");
            }
            
        } catch (Exception e) {
            log.error("❌ Error during universal SSH test", e);
        }
    }
    
    private void testUniversalSshCommand(TestSshDevice device, String command, String description) {
        try {
            log.info("🔍 Testing {}: {}", description, command);
            
            CompletableFuture<String> future = device.sendSshCommand(command);
            String response = future.get(10, TimeUnit.SECONDS);
            
            if (response != null && !response.trim().isEmpty()) {
                String[] lines = response.split("\n");
                log.info("📋 {} SUCCESS ({} lines, {} chars):", description, lines.length, response.length());
                
                // Show first few lines
                for (int i = 0; i < Math.min(3, lines.length); i++) {
                    if (!lines[i].trim().isEmpty()) {
                        log.info("   SSH: {}", lines[i].trim());
                    }
                }
                
                if (lines.length > 3) {
                    log.info("   ... ({} more lines)", lines.length - 3);
                }
                
            } else {
                log.warn("⚠️  {} - Empty response", description);
            }
            
        } catch (Exception e) {
            log.error("❌ {} - Error: {}", description, e.getMessage());
        }
    }
}