package com.telcobright.orchestrix.device;

import com.jcraft.jsch.*;
import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.util.concurrent.CompletableFuture;

@Slf4j
public class RouterOSSshClient {
    
    private Session session;
    private ChannelShell shell;
    private PrintWriter writer;
    private BufferedReader reader;
    private String hostname;
    
    public CompletableFuture<Boolean> connect(String hostname, int port, String username, String password) {
        this.hostname = hostname;
        
        return CompletableFuture.supplyAsync(() -> {
            try {
                log.info("Connecting to RouterOS at {}:{}", hostname, port);
                
                JSch jsch = new JSch();
                session = jsch.getSession(username, hostname, port);
                session.setPassword(password);
                
                session.setConfig("StrictHostKeyChecking", "no");
                session.setConfig("UserKnownHostsFile", "/dev/null");
                session.setConfig("CheckHostIP", "no");
                session.setConfig("LogLevel", "ERROR");
                
                session.connect(30000);
                
                // Open shell channel
                shell = (ChannelShell) session.openChannel("shell");
                shell.setPtyType("vt100");
                shell.connect();
                
                writer = new PrintWriter(shell.getOutputStream(), true);
                reader = new BufferedReader(new InputStreamReader(shell.getInputStream()));
                
                // Wait for initial RouterOS banner and prompt
                waitForPrompt();
                
                log.info("Successfully connected to RouterOS");
                return true;
                
            } catch (Exception e) {
                log.error("Failed to connect to RouterOS: {}", e.getMessage());
                return false;
            }
        });
    }
    
    public CompletableFuture<String> executeCommand(String command) {
        return CompletableFuture.supplyAsync(() -> {
            if (!isConnected()) {
                throw new RuntimeException("Not connected to RouterOS");
            }
            
            try {
                log.debug("Executing RouterOS command: {}", command);
                
                // Send command
                writer.println(command);
                writer.flush();
                
                // Read response until we get back to prompt
                StringBuilder response = new StringBuilder();
                String line;
                boolean foundOutput = false;
                int promptCount = 0;
                
                while ((line = reader.readLine()) != null) {
                    
                    // Skip the command echo
                    if (line.trim().equals(command)) {
                        continue;
                    }
                    
                    // Check for RouterOS prompt
                    if (line.contains("[admin@") && line.contains("] >")) {
                        promptCount++;
                        if (promptCount >= 2 && foundOutput) {
                            // We've seen output and now back to prompt
                            break;
                        }
                        continue;
                    }
                    
                    // Skip empty lines at start
                    if (!foundOutput && line.trim().isEmpty()) {
                        continue;
                    }
                    
                    // Skip RouterOS banner/help if it appears
                    if (line.contains("MikroTik RouterOS") || 
                        line.contains("MMM") || line.contains("KKK") ||
                        line.contains("http://www.mikrotik.com")) {
                        continue;
                    }
                    
                    // Skip help text indicators
                    if (line.contains("Gives the list of available commands") ||
                        line.contains("Gives help on the command") ||
                        line.contains("Completes the command/word")) {
                        continue;
                    }
                    
                    // This looks like actual command output
                    if (!line.trim().isEmpty()) {
                        response.append(line).append("\n");
                        foundOutput = true;
                    }
                }
                
                String result = response.toString().trim();
                log.debug("Command response length: {} characters", result.length());
                
                return result;
                
            } catch (Exception e) {
                log.error("Error executing command '{}': {}", command, e.getMessage());
                throw new RuntimeException("Command execution failed: " + e.getMessage());
            }
        });
    }
    
    private void waitForPrompt() throws IOException, InterruptedException {
        // Read initial output and wait for prompt
        StringBuilder initial = new StringBuilder();
        String line;
        int timeout = 0;
        
        while (timeout < 100) { // 10 second timeout
            if (reader.ready()) {
                line = reader.readLine();
                if (line != null) {
                    initial.append(line).append("\n");
                    if (line.contains("[admin@") && line.contains("] >")) {
                        log.debug("Found RouterOS prompt, ready for commands");
                        break;
                    }
                }
            } else {
                Thread.sleep(100);
                timeout++;
            }
        }
        
        if (timeout >= 100) {
            throw new RuntimeException("Timeout waiting for RouterOS prompt");
        }
    }
    
    public boolean isConnected() {
        return session != null && session.isConnected() && 
               shell != null && shell.isConnected();
    }
    
    public void disconnect() {
        try {
            if (writer != null) writer.close();
            if (reader != null) reader.close();
            if (shell != null && shell.isConnected()) shell.disconnect();
            if (session != null && session.isConnected()) session.disconnect();
            log.info("Disconnected from RouterOS");
        } catch (Exception e) {
            log.error("Error during disconnect: {}", e.getMessage());
        }
    }
}