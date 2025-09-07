package com.telcobright.orchestrix.device;

import com.jcraft.jsch.*;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import java.io.*;

@Slf4j
public class WorkingRouterOSTest {
    
    @Test
    public void testWorkingRouterOSCommands() {
        try {
            log.info("Testing working RouterOS command execution...");
            
            JSch jsch = new JSch();
            Session session = jsch.getSession("admin", "114.130.145.70", 22);
            session.setPassword("Takay1#$ane%%");
            session.setConfig("StrictHostKeyChecking", "no");
            session.connect();
            
            ChannelShell shell = (ChannelShell) session.openChannel("shell");
            shell.connect();
            
            PrintWriter writer = new PrintWriter(shell.getOutputStream(), true);
            BufferedReader reader = new BufferedReader(new InputStreamReader(shell.getInputStream()));
            
            // Wait for initial setup and clear buffer
            Thread.sleep(8000);
            while (reader.ready()) {
                reader.readLine(); // Clear initial output
            }
            
            log.info("✅ RouterOS ready, testing commands...");
            
            // Test exact command that works manually
            testRouterOSCommand(writer, reader, "/system identity print", "System Identity");
            testRouterOSCommand(writer, reader, "/ip route print", "IP Routes");
            
            shell.disconnect();
            session.disconnect();
            log.info("✅ Working RouterOS test complete");
            
        } catch (Exception e) {
            log.error("❌ Working RouterOS test failed", e);
        }
    }
    
    private void testRouterOSCommand(PrintWriter writer, BufferedReader reader, String command, String description) {
        try {
            log.info("🔍 Executing: {}", command);
            
            // Send command
            writer.println(command);
            writer.flush();
            
            // Give RouterOS time to process
            Thread.sleep(2000);
            
            // Read response
            StringBuilder response = new StringBuilder();
            String line;
            int lines = 0;
            boolean foundPrompt = false;
            
            while (reader.ready() && lines < 100) {
                line = reader.readLine();
                if (line != null) {
                    lines++;
                    
                    // Check for prompt to know we're done
                    if (line.contains("[admin@TB_Mikrotik_01_Borak] >")) {
                        foundPrompt = true;
                        break;
                    }
                    
                    // Skip command echo
                    if (!line.trim().equals(command)) {
                        response.append(line).append("\n");
                    }
                }
            }
            
            String result = response.toString().trim();
            
            if (!result.isEmpty()) {
                String[] resultLines = result.split("\n");
                log.info("📋 {} ({} lines):", description, resultLines.length);
                
                // Show first few lines
                for (int i = 0; i < Math.min(3, resultLines.length); i++) {
                    if (!resultLines[i].trim().isEmpty()) {
                        log.info("   {}", resultLines[i].trim());
                    }
                }
                
                if (resultLines.length > 3) {
                    log.info("   ... ({} more lines)", resultLines.length - 3);
                }
                
                log.info("✅ {} - Success (prompt found: {})", description, foundPrompt);
            } else {
                log.warn("⚠️  {} - Empty response", description);
            }
            
        } catch (Exception e) {
            log.error("❌ {} failed: {}", description, e.getMessage());
        }
    }
}