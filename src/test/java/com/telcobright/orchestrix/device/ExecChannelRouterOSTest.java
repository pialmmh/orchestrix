package com.telcobright.orchestrix.device;

import com.jcraft.jsch.*;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import java.io.*;

@Slf4j
public class ExecChannelRouterOSTest {
    
    @Test
    public void testExecChannelApproach() {
        try {
            log.info("Testing RouterOS with EXEC channel (non-interactive)...");
            
            JSch jsch = new JSch();
            Session session = jsch.getSession("admin", "114.130.145.70", 22);
            session.setPassword("Takay1#$ane%%");
            session.setConfig("StrictHostKeyChecking", "no");
            session.connect();
            
            // Test single command execution using exec channel
            testExecCommand(session, "/system identity print", "System Identity");
            testExecCommand(session, "/ip route print", "IP Routes");
            
            session.disconnect();
            log.info("✅ Exec channel test complete");
            
        } catch (Exception e) {
            log.error("❌ Exec channel test failed", e);
        }
    }
    
    private void testExecCommand(Session session, String command, String description) {
        try {
            log.info("🔍 Executing {} via EXEC channel: {}", description, command);
            
            // Use exec channel for single command execution
            ChannelExec execChannel = (ChannelExec) session.openChannel("exec");
            execChannel.setCommand(command);
            
            // Set up streams
            InputStream inputStream = execChannel.getInputStream();
            InputStream errorStream = execChannel.getErrStream();
            
            execChannel.connect();
            
            // Read response
            StringBuilder response = new StringBuilder();
            StringBuilder errors = new StringBuilder();
            
            byte[] buffer = new byte[1024];
            while (true) {
                // Read stdout
                while (inputStream.available() > 0) {
                    int bytesRead = inputStream.read(buffer, 0, 1024);
                    if (bytesRead < 0) break;
                    response.append(new String(buffer, 0, bytesRead));
                }
                
                // Read stderr
                while (errorStream.available() > 0) {
                    int bytesRead = errorStream.read(buffer, 0, 1024);
                    if (bytesRead < 0) break;
                    errors.append(new String(buffer, 0, bytesRead));
                }
                
                if (execChannel.isClosed()) {
                    if (inputStream.available() == 0 && errorStream.available() == 0) {
                        break;
                    }
                }
                
                Thread.sleep(100);
            }
            
            execChannel.disconnect();
            
            String result = response.toString().trim();
            String errorResult = errors.toString().trim();
            
            if (!result.isEmpty()) {
                String[] lines = result.split("\n");
                log.info("📋 {} SUCCESS ({} lines, {} chars):", description, lines.length, result.length());
                
                // Show first few lines
                for (int i = 0; i < Math.min(3, lines.length); i++) {
                    log.info("   EXEC: {}", lines[i].trim());
                }
                
                if (lines.length > 3) {
                    log.info("   ... ({} more lines)", lines.length - 3);
                }
                
            } else {
                log.warn("⚠️  {} - Empty response", description);
            }
            
            if (!errorResult.isEmpty()) {
                log.warn("⚠️  {} - Errors: {}", description, errorResult);
            }
            
            log.info("Exit status: {}", execChannel.getExitStatus());
            
        } catch (Exception e) {
            log.error("❌ {} failed: {}", description, e.getMessage());
        }
    }
}