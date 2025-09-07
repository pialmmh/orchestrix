package com.telcobright.orchestrix.device;

import com.jcraft.jsch.*;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import java.io.*;

@Slf4j
public class RouterOSOutputDebugTest {
    
    @Test
    public void debugRouterOSCommandOutput() {
        try {
            log.info("Debugging RouterOS command output in detail...");
            
            JSch jsch = new JSch();
            Session session = jsch.getSession("admin", "114.130.145.70", 22);
            session.setPassword("Takay1#$ane%%");
            session.setConfig("StrictHostKeyChecking", "no");
            session.connect();
            
            ChannelShell shell = (ChannelShell) session.openChannel("shell");
            shell.connect();
            
            PrintWriter writer = new PrintWriter(shell.getOutputStream(), true);
            BufferedReader reader = new BufferedReader(new InputStreamReader(shell.getInputStream()));
            
            // Wait for RouterOS to be ready
            Thread.sleep(8000);
            while (reader.ready()) {
                reader.readLine(); // Clear initial output
            }
            
            log.info("Sending command: /system identity print");
            writer.println("/system identity print");
            
            Thread.sleep(1500);
            
            log.info("=== RAW RouterOS OUTPUT DEBUG ===");
            String line;
            int lineNum = 0;
            
            while (reader.ready() && lineNum < 20) {
                line = reader.readLine();
                if (line != null) {
                    lineNum++;
                    log.info("LINE[{}]: '{}' (length: {}, isEmpty: {})", 
                            lineNum, line, line.length(), line.trim().isEmpty());
                    
                    if (line.contains("[admin@") && line.contains("] >")) {
                        log.info("*** PROMPT FOUND ON LINE {} ***", lineNum);
                        break;
                    }
                }
            }
            log.info("=== END DEBUG OUTPUT ===");
            
            shell.disconnect();
            session.disconnect();
            
        } catch (Exception e) {
            log.error("Debug failed", e);
        }
    }
}