package com.telcobright.orchestrix.device;

import com.jcraft.jsch.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.*;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
@Component
public class SshDevice implements TerminalDevice {
    
    private final ExecutorService executorService = Executors.newFixedThreadPool(10);
    
    private Session session;
    private ChannelShell shell;
    private PrintWriter writer;
    private BufferedReader reader;
    
    private String deviceId;
    private String hostname;
    private int port;
    private DeviceStatus status = DeviceStatus.DISCONNECTED;
    
    public SshDevice() {
        this.deviceId = UUID.randomUUID().toString();
    }
    
    public SshDevice(String deviceId) {
        this.deviceId = deviceId != null ? deviceId : UUID.randomUUID().toString();
    }
    
    @Override
    public CompletableFuture<Boolean> connect(String hostname, int port, String username, String password) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                this.hostname = hostname;
                this.port = port;
                this.status = DeviceStatus.CONNECTING;
                
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
                
                this.status = DeviceStatus.CONNECTED;
                log.info("Successfully connected to SSH device: {}:{}", hostname, port);
                
                return true;
                
            } catch (Exception e) {
                log.error("Failed to connect to SSH device {}:{}", hostname, port, e);
                this.status = DeviceStatus.ERROR;
                return false;
            }
        }, executorService);
    }
    
    @Override
    public CompletableFuture<String> send(String command) {
        return CompletableFuture.supplyAsync(() -> {
            if (!isConnected()) {
                throw new RuntimeException("Device not connected");
            }
            
            try {
                log.debug("Sending command to {}: {}", hostname, command);
                writer.println(command);
                writer.flush();
                return command;
            } catch (Exception e) {
                log.error("Failed to send command to device {}: {}", hostname, command, e);
                throw new RuntimeException("Failed to send command", e);
            }
        }, executorService);
    }
    
    @Override
    public CompletableFuture<String> receive() {
        return CompletableFuture.supplyAsync(() -> {
            if (!isConnected()) {
                throw new RuntimeException("Device not connected");
            }
            
            try {
                StringBuilder response = new StringBuilder();
                String line;
                
                while ((line = reader.readLine()) != null) {
                    response.append(line).append("\n");
                    
                    if (line.contains(">") || line.contains("#") || line.contains("$")) {
                        break;
                    }
                }
                
                String result = response.toString();
                log.debug("Received response from {}: {}", hostname, result.substring(0, Math.min(result.length(), 100)));
                
                return result;
                
            } catch (Exception e) {
                log.error("Failed to receive response from device {}", hostname, e);
                throw new RuntimeException("Failed to receive response", e);
            }
        }, executorService);
    }
    
    @Override
    public CompletableFuture<String> sendAndReceive(String command) {
        return send(command)
            .thenCompose(sent -> receive())
            .thenApply(response -> {
                log.debug("Command '{}' executed on {}, response length: {}", command, hostname, response.length());
                return response;
            });
    }
    
    @Override
    public boolean isConnected() {
        return session != null && 
               session.isConnected() && 
               shell != null && 
               shell.isConnected() && 
               status == DeviceStatus.CONNECTED;
    }
    
    @Override
    public void disconnect() throws IOException {
        try {
            log.info("Disconnecting from SSH device: {}", hostname);
            
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
            
            this.status = DeviceStatus.DISCONNECTED;
            log.info("Successfully disconnected from SSH device: {}", hostname);
            
        } catch (Exception e) {
            log.error("Error disconnecting from SSH device {}", hostname, e);
            this.status = DeviceStatus.ERROR;
            throw new IOException("Failed to disconnect", e);
        }
    }
    
    @Override
    public String getDeviceId() {
        return deviceId;
    }
    
    @Override
    public String getHostname() {
        return hostname;
    }
    
    @Override
    public DeviceType getDeviceType() {
        return DeviceType.SSH;
    }
    
    @Override
    public DeviceStatus getStatus() {
        return status;
    }
}