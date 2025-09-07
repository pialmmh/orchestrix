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
                
                while ((line = reader.readLine()) != null) {
                    response.append(line).append("\n");
                    
                    if (line.contains(">") || line.contains("#") || line.contains("$")) {
                        break;
                    }
                }
                
                String result = response.toString();
                log.debug("Received SSH response length: {}", result.length());
                
                return result;
                
            } catch (Exception e) {
                log.error("Failed to receive SSH response", e);
                throw new RuntimeException("Failed to receive SSH response", e);
            }
        }, executorService);
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