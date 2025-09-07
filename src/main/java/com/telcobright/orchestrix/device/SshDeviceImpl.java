package com.telcobright.orchestrix.device;

import com.jcraft.jsch.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
@Component
public class SshDeviceImpl implements SshDevice {
    
    protected final ExecutorService executorService = Executors.newFixedThreadPool(10);
    
    protected Session session;
    protected ChannelShell shell;
    protected PrintWriter writer;
    protected BufferedReader reader;
    
    @Override
    public CompletableFuture<Boolean> connectSsh(String hostname, int port, String username, String password) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                log.info("Connecting to SSH device: {}:{}", hostname, port);
                
                JSch jsch = new JSch();
                session = jsch.getSession(username, hostname, port);
                session.setPassword(password);
                
                session.setConfig("StrictHostKeyChecking", "no");
                session.setConfig("UserKnownHostsFile", "/dev/null");
                session.setConfig("CheckHostIP", "no");
                session.setConfig("LogLevel", "ERROR");
                
                session.connect(30000);
                
                shell = (ChannelShell) session.openChannel("shell");
                shell.connect();
                
                writer = new PrintWriter(shell.getOutputStream(), true);
                reader = new BufferedReader(new InputStreamReader(shell.getInputStream()));
                
                log.info("Successfully connected to SSH device: {}:{}", hostname, port);
                return true;
                
            } catch (Exception e) {
                log.error("Failed to connect to SSH device {}:{}", hostname, port, e);
                return false;
            }
        }, executorService);
    }
    
    @Override
    public CompletableFuture<String> sendSshCommand(String command) {
        return CompletableFuture.supplyAsync(() -> {
            if (!isSshConnected()) {
                throw new RuntimeException("SSH not connected");
            }
            
            try {
                log.debug("Sending SSH command: {}", command);
                writer.println(command);
                writer.flush();
                return command;
            } catch (Exception e) {
                log.error("Failed to send SSH command: {}", command, e);
                throw new RuntimeException("Failed to send SSH command", e);
            }
        }, executorService);
    }
    
    @Override
    public CompletableFuture<String> receiveSshResponse() {
        return CompletableFuture.supplyAsync(() -> {
            if (!isSshConnected()) {
                throw new RuntimeException("SSH not connected");
            }
            
            try {
                StringBuilder response = new StringBuilder();
                String line;
                boolean foundCommandOutput = false;
                int emptyLineCount = 0;
                int timeoutCount = 0;
                
                // Wait a bit for command to execute
                Thread.sleep(1000);
                
                while ((line = reader.readLine()) != null && timeoutCount < 50) {
                    response.append(line).append("\n");
                    
                    // Skip initial empty lines and prompts
                    if (line.trim().isEmpty()) {
                        emptyLineCount++;
                        if (emptyLineCount > 3 && foundCommandOutput) {
                            break; // Too many empty lines after output, likely done
                        }
                        continue;
                    }
                    
                    emptyLineCount = 0;
                    
                    // Look for MikroTik prompt patterns
                    if (line.contains("[admin@") && line.contains("] >")) {
                        if (foundCommandOutput) {
                            break; // Found prompt after output, we're done
                        } else {
                            continue; // Initial prompt, keep reading
                        }
                    }
                    
                    // If we see actual content (not just prompts), mark as found
                    if (!line.contains("[admin@") && !line.trim().isEmpty() && 
                        !line.contains("MMM") && !line.contains("KKK") && 
                        !line.contains("MikroTik RouterOS")) {
                        foundCommandOutput = true;
                    }
                    
                    // Fallback timeout
                    if (!reader.ready()) {
                        timeoutCount++;
                        Thread.sleep(100);
                    }
                }
                
                String result = response.toString();
                log.debug("Received SSH response length: {}", result.length());
                
                // Clean up the response by removing excessive whitespace and prompts
                String cleanResult = cleanMikroTikResponse(result);
                
                return cleanResult;
                
            } catch (Exception e) {
                log.error("Failed to receive SSH response", e);
                throw new RuntimeException("Failed to receive SSH response", e);
            }
        }, executorService);
    }
    
    private String cleanMikroTikResponse(String response) {
        if (response == null || response.trim().isEmpty()) {
            return response;
        }
        
        // Split into lines for processing
        String[] lines = response.split("\n");
        StringBuilder cleaned = new StringBuilder();
        boolean inBanner = false;
        boolean foundData = false;
        
        for (String line : lines) {
            String trimmed = line.trim();
            
            // Skip MikroTik banner lines
            if (trimmed.contains("MMM") || trimmed.contains("KKK") || 
                trimmed.contains("MikroTik RouterOS") || trimmed.contains("http://www.mikrotik.com")) {
                inBanner = true;
                continue;
            }
            
            // Skip empty lines in banner
            if (inBanner && trimmed.isEmpty()) {
                continue;
            }
            
            // End of banner
            if (inBanner && !trimmed.isEmpty() && !trimmed.contains("MMM") && !trimmed.contains("KKK")) {
                inBanner = false;
            }
            
            // Skip prompt lines
            if (trimmed.contains("[admin@") && trimmed.contains("] >")) {
                continue;
            }
            
            // Skip command echo
            if (trimmed.startsWith("/") && !foundData) {
                continue;
            }
            
            // Keep actual data lines
            if (!trimmed.isEmpty() && !inBanner) {
                cleaned.append(line).append("\n");
                foundData = true;
            }
        }
        
        return cleaned.toString().trim();
    }
    
    @Override
    public boolean isSshConnected() {
        return session != null && 
               session.isConnected() && 
               shell != null && 
               shell.isConnected();
    }
    
    @Override
    public void disconnectSsh() throws IOException {
        try {
            log.info("Disconnecting SSH connection");
            
            if (writer != null) {
                writer.close();
            }
            if (reader != null) {
                reader.close();
            }
            if (shell != null && shell.isConnected()) {
                shell.disconnect();
            }
            if (session != null && session.isConnected()) {
                session.disconnect();
            }
            
            log.info("Successfully disconnected SSH");
            
        } catch (Exception e) {
            log.error("Error disconnecting SSH", e);
            throw new IOException("Failed to disconnect SSH", e);
        }
    }
}