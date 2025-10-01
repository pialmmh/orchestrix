package com.telcobright.orchestrix.device;

import com.jcraft.jsch.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.concurrent.CompletableFuture;

public class SimpleRouterOSClient {

    private static final Logger log = LoggerFactory.getLogger(SimpleRouterOSClient.class);
    
    private Session session;
    private ChannelShell shell;
    private PrintWriter writer;
    private BufferedReader reader;
    
    public CompletableFuture<Boolean> connect(String hostname, int port, String username, String password) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                log.info("Connecting to RouterOS at {}:{}", hostname, port);
                
                JSch jsch = new JSch();
                session = jsch.getSession(username, hostname, port);
                session.setPassword(password);
                
                session.setConfig("StrictHostKeyChecking", "no");
                session.setConfig("UserKnownHostsFile", "/dev/null");
                
                session.connect();
                
                shell = (ChannelShell) session.openChannel("shell");
                shell.connect();
                
                writer = new PrintWriter(shell.getOutputStream(), true);
                reader = new BufferedReader(new InputStreamReader(shell.getInputStream()));
                
                // Give RouterOS time to show banner
                Thread.sleep(3000);
                
                // Clear any initial output
                while (reader.ready()) {
                    reader.readLine();
                }
                
                log.info("Connected to RouterOS");
                return true;
                
            } catch (Exception e) {
                log.error("Connection failed: {}", e.getMessage());
                return false;
            }
        });
    }
    
    public CompletableFuture<String> executeCommand(String command) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                log.debug("Sending: {}", command);
                
                // Send command
                writer.println(command);
                
                // Wait and collect response
                Thread.sleep(2000);
                
                StringBuilder response = new StringBuilder();
                int lines = 0;
                
                while (reader.ready() && lines < 100) { // Limit to prevent hanging
                    String line = reader.readLine();
                    if (line != null) {
                        // Skip command echo and prompts
                        if (!line.equals(command) && 
                            !line.contains("[admin@") &&
                            !line.trim().isEmpty()) {
                            response.append(line).append("\n");
                        }
                        lines++;
                    }
                }
                
                String result = response.toString().trim();
                log.debug("Got {} lines, {} chars", lines, result.length());
                
                return result;
                
            } catch (Exception e) {
                log.error("Command failed: {}", e.getMessage());
                return "Error: " + e.getMessage();
            }
        });
    }
    
    public boolean isConnected() {
        return session != null && session.isConnected();
    }
    
    public void disconnect() {
        try {
            if (shell != null) shell.disconnect();
            if (session != null) session.disconnect();
        } catch (Exception e) {
            log.error("Disconnect error: {}", e.getMessage());
        }
    }
}