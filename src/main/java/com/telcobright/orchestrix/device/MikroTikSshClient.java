package com.telcobright.orchestrix.device;

import com.jcraft.jsch.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MikroTikSshClient {

    private static final Logger log = LoggerFactory.getLogger(MikroTikSshClient.class);
    
    private final ExecutorService executorService = Executors.newCachedThreadPool();
    private Session session;
    private String hostname;
    private int port;
    private String username;
    private String password;
    
    public CompletableFuture<Boolean> connect(String hostname, int port, String username, String password) {
        this.hostname = hostname;
        this.port = port;
        this.username = username;
        this.password = password;
        
        return CompletableFuture.supplyAsync(() -> {
            try {
                log.info("Connecting to MikroTik at {}:{}", hostname, port);
                
                JSch jsch = new JSch();
                session = jsch.getSession(username, hostname, port);
                session.setPassword(password);
                
                session.setConfig("StrictHostKeyChecking", "no");
                session.setConfig("UserKnownHostsFile", "/dev/null");
                session.setConfig("CheckHostIP", "no");
                session.setConfig("LogLevel", "ERROR");
                
                session.connect(30000);
                log.info("Successfully connected to MikroTik");
                return true;
                
            } catch (Exception e) {
                log.error("Failed to connect to MikroTik: {}", e.getMessage());
                return false;
            }
        }, executorService);
    }
    
    public CompletableFuture<String> executeCommand(String command) {
        return CompletableFuture.supplyAsync(() -> {
            if (!isConnected()) {
                throw new RuntimeException("Not connected to MikroTik");
            }
            
            try {
                log.debug("Executing MikroTik command: {}", command);
                
                // Use exec channel for single command execution
                ChannelExec channelExec = (ChannelExec) session.openChannel("exec");
                
                // Set the command
                channelExec.setCommand(command);
                
                // Set up streams
                ByteArrayOutputStream responseStream = new ByteArrayOutputStream();
                ByteArrayOutputStream errorStream = new ByteArrayOutputStream();
                
                channelExec.setOutputStream(responseStream);
                channelExec.setErrStream(errorStream);
                
                // Connect and execute
                channelExec.connect();
                
                // Wait for command completion with timeout
                int timeout = 15000; // 15 seconds
                int waited = 0;
                int step = 100;
                
                while (!channelExec.isClosed() && waited < timeout) {
                    Thread.sleep(step);
                    waited += step;
                }
                
                // Get the results
                String response = responseStream.toString("UTF-8");
                String error = errorStream.toString("UTF-8");
                
                channelExec.disconnect();
                
                if (!error.isEmpty()) {
                    log.warn("Command stderr: {}", error);
                }
                
                // Clean the response
                String cleanResponse = cleanMikroTikOutput(response);
                
                log.debug("Command response length: {} characters", cleanResponse.length());
                
                return cleanResponse;
                
            } catch (Exception e) {
                log.error("Error executing command '{}': {}", command, e.getMessage());
                throw new RuntimeException("Command execution failed: " + e.getMessage());
            }
        }, executorService);
    }
    
    private String cleanMikroTikOutput(String output) {
        if (output == null || output.trim().isEmpty()) {
            return output;
        }
        
        StringBuilder cleaned = new StringBuilder();
        String[] lines = output.split("\n");
        boolean foundData = false;
        
        for (String line : lines) {
            String trimmed = line.trim();
            
            // Skip empty lines at the beginning
            if (!foundData && trimmed.isEmpty()) {
                continue;
            }
            
            // Skip MikroTik banner and system messages
            if (trimmed.contains("MikroTik RouterOS") || 
                trimmed.contains("http://www.mikrotik.com") ||
                trimmed.contains("MMM") || trimmed.contains("KKK") ||
                trimmed.contains("TTT") || trimmed.contains("III") ||
                trimmed.contains("(c) 1999-")) {
                continue;
            }
            
            // Skip command prompts and system info
            if (trimmed.startsWith("[") || trimmed.contains("gives the list") ||
                trimmed.contains("Gives help") || trimmed.contains("Completes the command")) {
                continue;
            }
            
            // Keep actual data
            if (!trimmed.isEmpty()) {
                cleaned.append(line).append("\n");
                foundData = true;
            }
        }
        
        return cleaned.toString().trim();
    }
    
    public boolean isConnected() {
        return session != null && session.isConnected();
    }
    
    public void disconnect() {
        if (session != null && session.isConnected()) {
            session.disconnect();
            log.info("Disconnected from MikroTik");
        }
    }
}