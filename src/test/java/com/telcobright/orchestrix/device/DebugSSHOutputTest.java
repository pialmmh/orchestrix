package com.telcobright.orchestrix.device;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import java.io.FileWriter;
import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Slf4j
public class DebugSSHOutputTest {
    
    @Test
    public void debugSSHOutput() {
        MikroTikRouter router = new MikroTikRouter("smsmk01");
        
        try (FileWriter debugFile = new FileWriter("/tmp/ssh_debug_output.txt")) {
            debugFile.write("=== SSH Debug Output Test ===\n");
            debugFile.flush();
            
            log.info("Connecting to smsmk01 for SSH debug...");
            debugFile.write("Connecting to smsmk01...\n");
            debugFile.flush();
            
            CompletableFuture<Boolean> connectionFuture = router.connect(
                "114.130.145.70", 
                22, 
                "admin", 
                "Takay1#$ane%%"
            );
            
            Boolean connected = connectionFuture.get(60, TimeUnit.SECONDS);
            
            if (connected) {
                log.info("✅ Connected! Now capturing SSH output...");
                debugFile.write("✅ Connected successfully!\n");
                debugFile.write("Now sending command: /ip route print\n");
                debugFile.write("=== RAW SSH OUTPUT BELOW ===\n");
                debugFile.flush();
                
                // Create a custom debug version to capture raw output
                CompletableFuture<String> debugFuture = CompletableFuture.supplyAsync(() -> {
                    try {
                        log.info("🔍 Sending debug command...");
                        
                        // Access the RouterOS components directly for debugging
                        java.lang.reflect.Field writerField = router.getClass().getDeclaredField("routerOSWriter");
                        java.lang.reflect.Field readerField = router.getClass().getDeclaredField("routerOSReader");
                        writerField.setAccessible(true);
                        readerField.setAccessible(true);
                        
                        java.io.PrintWriter writer = (java.io.PrintWriter) writerField.get(router);
                        java.io.BufferedReader reader = (java.io.BufferedReader) readerField.get(router);
                        
                        debugFile.write("About to send command...\n");
                        debugFile.flush();
                        
                        // Send the command
                        writer.println("/ip route print");
                        writer.flush();
                        
                        debugFile.write("Command sent, waiting for response...\n");
                        debugFile.flush();
                        
                        // Read everything for 10 seconds and capture it
                        StringBuilder allOutput = new StringBuilder();
                        long startTime = System.currentTimeMillis();
                        long timeout = 10000; // 10 seconds
                        int lineCount = 0;
                        
                        while (System.currentTimeMillis() - startTime < timeout) {
                            try {
                                if (reader.ready()) {
                                    String line = reader.readLine();
                                    if (line != null) {
                                        lineCount++;
                                        String logLine = String.format("LINE[%d]: '%s'\n", lineCount, line);
                                        allOutput.append(logLine);
                                        debugFile.write(logLine);
                                        debugFile.flush();
                                        log.info("DEBUG LINE[{}]: '{}'", lineCount, line);
                                    }
                                } else {
                                    Thread.sleep(100);
                                }
                            } catch (Exception e) {
                                String errorMsg = "ERROR reading: " + e.getMessage() + "\n";
                                debugFile.write(errorMsg);
                                debugFile.flush();
                                log.error("Error reading line: {}", e.getMessage());
                                break;
                            }
                        }
                        
                        debugFile.write("=== END OF SSH OUTPUT ===\n");
                        debugFile.write("Total lines captured: " + lineCount + "\n");
                        debugFile.flush();
                        
                        log.info("Captured {} lines total", lineCount);
                        return allOutput.toString();
                        
                    } catch (Exception e) {
                        try {
                            debugFile.write("EXCEPTION: " + e.getMessage() + "\n");
                            debugFile.flush();
                        } catch (IOException ioEx) {
                            log.error("Failed to write exception to file", ioEx);
                        }
                        log.error("Debug error: {}", e.getMessage(), e);
                        return "ERROR: " + e.getMessage();
                    }
                }, java.util.concurrent.Executors.newSingleThreadExecutor());
                
                String result = debugFuture.get(15, TimeUnit.SECONDS);
                
                debugFile.write("=== FINAL RESULT ===\n");
                debugFile.write(result);
                debugFile.write("\n=== TEST COMPLETE ===\n");
                debugFile.flush();
                
                log.info("📋 Debug complete. Check /tmp/ssh_debug_output.txt for raw output");
                
                router.disconnect();
                log.info("✅ Disconnected");
                
            } else {
                debugFile.write("❌ Failed to connect to router\n");
                debugFile.flush();
                log.error("❌ Failed to connect to router");
            }
            
        } catch (Exception e) {
            log.error("❌ Debug test error", e);
            try (FileWriter errorFile = new FileWriter("/tmp/ssh_debug_error.txt")) {
                errorFile.write("Debug test failed with error: " + e.getMessage() + "\n");
                e.printStackTrace(new java.io.PrintWriter(errorFile));
            } catch (IOException ioEx) {
                log.error("Failed to write error file", ioEx);
            }
        }
    }
}