package com.telcobright.orchestrix.device;

import com.jcraft.jsch.*;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import java.io.*;

@Slf4j
public class RouterOSDebugTest {
    
    @Test
    public void debugRouterOSSession() {
        try {
            log.info("Debugging RouterOS session behavior...");
            
            JSch jsch = new JSch();
            Session session = jsch.getSession("admin", "114.130.145.70", 22);
            session.setPassword("Takay1#$ane%%");
            session.setConfig("StrictHostKeyChecking", "no");
            session.connect();
            
            ChannelShell shell = (ChannelShell) session.openChannel("shell");
            shell.connect();
            
            PrintWriter writer = new PrintWriter(shell.getOutputStream(), true);
            BufferedReader reader = new BufferedReader(new InputStreamReader(shell.getInputStream()));
            
            log.info("Session established, collecting initial output...");
            
            // Read initial banner
            Thread.sleep(5000); // Give RouterOS time
            
            log.info("Reading initial RouterOS output:");
            while (reader.ready()) {
                String line = reader.readLine();
                log.info("INITIAL: '{}'", line);
            }
            
            log.info("Sending test command: /system identity print");
            writer.println("/system identity print");
            
            // Read response
            Thread.sleep(3000);
            
            log.info("Reading command response:");
            int lines = 0;
            while (reader.ready() && lines < 20) {
                String line = reader.readLine();
                log.info("RESPONSE[{}]: '{}'", lines, line);
                lines++;
                
                if (line != null && line.contains("[admin@") && line.contains("] >")) {
                    log.info("*** FOUND PROMPT ON LINE {} ***", lines);
                    break;
                }
            }
            
            shell.disconnect();
            session.disconnect();
            log.info("Debug session complete");
            
        } catch (Exception e) {
            log.error("Debug failed", e);
        }
    }
}