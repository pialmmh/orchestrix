package com.telcobright.orchestrix.device;

import com.jcraft.jsch.*;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import java.io.*;

@Slf4j
public class DirectRouterOSTest {
    
    @Test
    public void testDirectRouterOSApproach() {
        try {
            log.info("Testing DIRECT RouterOS approach exactly like working test...");
            
            JSch jsch = new JSch();
            Session session = jsch.getSession("admin", "114.130.145.70", 22);
            session.setPassword("Takay1#$ane%%");
            session.setConfig("StrictHostKeyChecking", "no");
            session.connect();
            
            ChannelShell shell = (ChannelShell) session.openChannel("shell");
            shell.connect();
            
            PrintWriter writer = new PrintWriter(shell.getOutputStream(), true);
            BufferedReader reader = new BufferedReader(new InputStreamReader(shell.getInputStream()));
            
            // Wait for initial setup and clear buffer (exact copy from working test)
            Thread.sleep(8000);
            while (reader.ready()) {
                reader.readLine(); // Clear initial output
            }
            
            log.info("✅ RouterOS ready, testing direct command approach...");
            
            // Test with exact same logic as working test
            testDirectCommand(writer, reader, "/system identity print", "System Identity");
            testDirectCommand(writer, reader, "/ip route print", "IP Routes");
            
            shell.disconnect();
            session.disconnect();
            log.info("✅ Direct RouterOS test complete");
            
        } catch (Exception e) {
            log.error("❌ Direct RouterOS test failed", e);
        }
    }
    
    // Exact copy of working test method
    private void testDirectCommand(PrintWriter writer, BufferedReader reader, String command, String description) {
        try {
            log.info("🔍 Executing direct: {}", command);
            
            // Send command (exact same as working test)
            writer.println(command);
            writer.flush();
            
            // Give RouterOS time to process (exact same)
            Thread.sleep(2000);
            
            // Read response (exact same logic as working test)
            StringBuilder response = new StringBuilder();
            String line;
            int lines = 0;
            boolean foundPrompt = false;
            
            while (reader.ready() && lines < 100) {
                line = reader.readLine();
                if (line != null) {
                    lines++;
                    
                    // Check for prompt to know we're done (exact same pattern)
                    if (line.contains("[admin@TB_Mikrotik_01_Borak] >")) {
                        foundPrompt = true;
                        log.info("FOUND PROMPT: '{}'", line);
                        break;
                    }
                    
                    // Skip command echo (exact same)
                    if (!line.trim().equals(command)) {
                        response.append(line).append("\n");
                    }
                    
                    log.info("DIRECT LINE[{}]: '{}'", lines, line);
                }
            }
            
            String result = response.toString().trim();
            
            if (!result.isEmpty()) {
                String[] resultLines = result.split("\n");
                log.info("📋 {} ({} lines):", description, resultLines.length);
                
                // Show first few lines
                for (int i = 0; i < Math.min(3, resultLines.length); i++) {
                    if (!resultLines[i].trim().isEmpty()) {
                        log.info("   RESULT: {}", resultLines[i].trim());
                    }
                }
                
                log.info("✅ {} - SUCCESS (prompt found: {}, {} chars)", description, foundPrompt, result.length());
            } else {
                log.warn("⚠️  {} - EMPTY RESULT (prompt found: {})", description, foundPrompt);
            }
            
        } catch (Exception e) {
            log.error("❌ {} failed: {}", description, e.getMessage());
        }
    }
}