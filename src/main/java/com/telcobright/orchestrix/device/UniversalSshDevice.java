package com.telcobright.orchestrix.device;

import com.jcraft.jsch.*;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
public abstract class UniversalSshDevice extends NetworkingDevice implements SshDevice {
    
    protected final ExecutorService executorService = Executors.newCachedThreadPool();
    protected Session sshSession;
    protected String hostname;
    protected int port;
    protected String username;
    protected String password;
    
    public UniversalSshDevice() {
        super();
    }
    
    public UniversalSshDevice(String deviceId) {
        super(deviceId);
    }
    
    @Override
    public CompletableFuture<Boolean> connectSsh(String hostname, int port, String username, String password) throws IOException {
        this.hostname = hostname;
        this.port = port;
        this.username = username;
        this.password = password;
        this.status = DeviceStatus.CONNECTING;
        
        log.info("Connecting to SSH device at {}:{}", hostname, port);
        
        return CompletableFuture.supplyAsync(() -> {
            try {
                JSch jsch = new JSch();
                
                // Add SSH key authentication if password is empty
                if (password == null || password.isEmpty()) {
                    String homeDir = System.getProperty("user.home");
                    // Try multiple possible SSH key locations
                    String[] keyPaths = {
                        homeDir + "/.ssh/id_rsa_jsch",  // JSch-compatible key
                        homeDir + "/.ssh/id_rsa",        // Standard RSA key
                        homeDir + "/.ssh/id_dsa"         // DSA key
                    };
                    
                    boolean keyAdded = false;
                    for (String keyPath : keyPaths) {
                        java.io.File sshKey = new java.io.File(keyPath);
                        if (sshKey.exists()) {
                            try {
                                jsch.addIdentity(sshKey.getAbsolutePath());
                                log.debug("Using SSH key authentication from: {}", sshKey.getAbsolutePath());
                                keyAdded = true;
                                break;
                            } catch (Exception e) {
                                log.debug("Failed to load key from {}: {}", keyPath, e.getMessage());
                            }
                        }
                    }
                    
                    if (!keyAdded) {
                        log.warn("No valid SSH key found for authentication");
                    }
                }
                
                sshSession = jsch.getSession(username, hostname, port);
                
                // Only set password if provided
                if (password != null && !password.isEmpty()) {
                    sshSession.setPassword(password);
                }
                
                // Universal SSH configuration
                sshSession.setConfig("StrictHostKeyChecking", "no");
                sshSession.setConfig("UserKnownHostsFile", "/dev/null");
                sshSession.setConfig("CheckHostIP", "no");
                sshSession.setConfig("LogLevel", "ERROR");
                
                // Explicitly set authentication methods when using key
                if (password == null || password.isEmpty()) {
                    sshSession.setConfig("PreferredAuthentications", "publickey");
                }
                
                sshSession.connect(30000);
                
                this.status = DeviceStatus.CONNECTED;
                log.info("Successfully connected to SSH device: {}", deviceId);
                return true;
                
            } catch (Exception e) {
                log.error("Failed to connect to SSH device {}: {}", deviceId, e.getMessage());
                this.status = DeviceStatus.ERROR;
                return false;
            }
        }, executorService);
    }
    
    @Override
    public CompletableFuture<String> sendSshCommand(String command) throws IOException {
        return executeSshCommandViaExecChannel(command);
    }
    
    @Override
    public CompletableFuture<String> receiveSshResponse() throws IOException {
        // Not used with exec channel approach - response is received directly in sendSshCommand
        return CompletableFuture.completedFuture("");
    }
    
    /**
     * Universal SSH command execution using exec channel
     * Works with any SSH server (Linux, RouterOS, switches, etc.)
     */
    protected CompletableFuture<String> executeSshCommandViaExecChannel(String command) {
        return CompletableFuture.supplyAsync(() -> {
            if (!isSshConnected()) {
                throw new RuntimeException("SSH not connected");
            }
            
            try {
                log.debug("Executing SSH command via EXEC channel: {}", command);
                
                // Use exec channel for direct command execution (universal approach)
                ChannelExec execChannel = (ChannelExec) sshSession.openChannel("exec");
                execChannel.setCommand(command);
                
                // Set up streams for reading response
                InputStream inputStream = execChannel.getInputStream();
                InputStream errorStream = execChannel.getErrStream();
                
                execChannel.connect();
                
                // Read command output from exec channel
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
                    
                    // Check if command execution is complete
                    if (execChannel.isClosed()) {
                        if (inputStream.available() == 0 && errorStream.available() == 0) {
                            break;
                        }
                    }
                    
                    Thread.sleep(100);
                }
                
                int exitStatus = execChannel.getExitStatus();
                execChannel.disconnect();
                
                String result = response.toString().trim();
                String errorResult = errors.toString().trim();
                
                if (!errorResult.isEmpty()) {
                    log.warn("SSH command '{}' had errors: {}", command, errorResult);
                }
                
                log.debug("SSH command '{}' completed via EXEC: {} chars, exit status: {}", 
                         command, result.length(), exitStatus);
                
                return result;
                
            } catch (Exception e) {
                log.error("Error executing SSH command '{}': {}", command, e.getMessage());
                throw new RuntimeException("SSH command failed: " + e.getMessage());
            }
        }, executorService);
    }
    
    @Override
    public boolean isSshConnected() {
        return sshSession != null && sshSession.isConnected() && status == DeviceStatus.CONNECTED;
    }
    
    @Override
    public void disconnectSsh() throws IOException {
        log.info("Disconnecting from SSH device {}", deviceId);
        
        try {
            if (sshSession != null && sshSession.isConnected()) {
                sshSession.disconnect();
            }
        } catch (Exception e) {
            log.error("Error disconnecting SSH session: {}", e.getMessage());
        }
        
        this.status = DeviceStatus.DISCONNECTED;
    }
    
    // Bridge methods for NetworkingDevice compatibility
    @Override
    public CompletableFuture<Boolean> connect(String hostname, int port, String username, String password) throws IOException {
        return connectSsh(hostname, port, username, password);
    }
    
    @Override
    public void disconnect() throws IOException {
        disconnectSsh();
    }
    
    @Override
    public CompletableFuture<String> sendAndReceive(String command) throws IOException {
        return sendSshCommand(command);
    }
    
    @Override
    public boolean isConnected() {
        return isSshConnected();
    }
}