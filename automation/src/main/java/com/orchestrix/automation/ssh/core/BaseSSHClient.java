package com.orchestrix.automation.ssh.core;

import com.orchestrix.automation.ssh.api.SSHClient;
import com.jcraft.jsch.*;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.*;

/**
 * Base implementation of SSH client using JSch library
 */
public abstract class BaseSSHClient implements SSHClient {
    
    protected JSch jsch;
    protected Session session;
    protected String host;
    protected int port;
    protected String username;
    protected long connectedSince;
    
    private static final int DEFAULT_TIMEOUT = 30000;
    private static final ExecutorService executor = Executors.newCachedThreadPool();
    
    public BaseSSHClient() {
        this.jsch = new JSch();
        configureJSch();
    }
    
    /**
     * Configure JSch settings - override in subclasses for customization
     */
    protected void configureJSch() {
        JSch.setConfig("StrictHostKeyChecking", "no");
        JSch.setConfig("PreferredAuthentications", "publickey,password");
    }
    
    @Override
    public void connect(String host, int port, String username, String keyPath) throws SSHException {
        try {
            this.host = host;
            this.port = port;
            this.username = username;
            
            jsch.addIdentity(keyPath);
            session = jsch.getSession(username, host, port);
            
            Properties config = new Properties();
            config.put("StrictHostKeyChecking", "no");
            session.setConfig(config);
            session.setTimeout(DEFAULT_TIMEOUT);
            
            beforeConnect();
            session.connect();
            afterConnect();
            
            connectedSince = System.currentTimeMillis();
            
        } catch (JSchException e) {
            throw new SSHException("Failed to connect to " + host + ":" + port, e);
        }
    }
    
    @Override
    public void connectWithPassword(String host, int port, String username, String password) throws SSHException {
        try {
            this.host = host;
            this.port = port;
            this.username = username;
            
            session = jsch.getSession(username, host, port);
            session.setPassword(password);
            
            Properties config = new Properties();
            config.put("StrictHostKeyChecking", "no");
            session.setConfig(config);
            session.setTimeout(DEFAULT_TIMEOUT);
            
            beforeConnect();
            session.connect();
            afterConnect();
            
            connectedSince = System.currentTimeMillis();
            
        } catch (JSchException e) {
            throw new SSHException("Failed to connect to " + host + ":" + port, e);
        }
    }
    
    /**
     * Hook for subclasses - called before connection
     */
    protected void beforeConnect() throws SSHException {
        // Override in subclasses
    }
    
    /**
     * Hook for subclasses - called after successful connection
     */
    protected void afterConnect() throws SSHException {
        // Override in subclasses
    }
    
    @Override
    public CommandResult executeCommand(String command) throws SSHException {
        return executeCommand(command, DEFAULT_TIMEOUT);
    }
    
    @Override
    public CommandResult executeCommand(String command, long timeoutMs) throws SSHException {
        return executeCommand(command, null, timeoutMs);
    }
    
    @Override
    public CommandResult executeCommand(String command, Map<String, String> env) throws SSHException {
        return executeCommand(command, env, DEFAULT_TIMEOUT);
    }
    
    protected CommandResult executeCommand(String command, Map<String, String> env, long timeoutMs) throws SSHException {
        if (!isConnected()) {
            throw new SSHException("Not connected to SSH server");
        }
        
        ChannelExec channel = null;
        try {
            long startTime = System.currentTimeMillis();
            
            channel = (ChannelExec) session.openChannel("exec");
            channel.setCommand(command);
            
            if (env != null && !env.isEmpty()) {
                for (Map.Entry<String, String> entry : env.entrySet()) {
                    channel.setEnv(entry.getKey(), entry.getValue());
                }
            }
            
            ByteArrayOutputStream stdout = new ByteArrayOutputStream();
            ByteArrayOutputStream stderr = new ByteArrayOutputStream();
            
            channel.setOutputStream(stdout);
            channel.setErrStream(stderr);
            
            channel.connect();
            
            // Wait for command to complete or timeout
            Future<Integer> future = executor.submit(() -> {
                while (channel.isConnected()) {
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
                return channel.getExitStatus();
            });
            
            Integer exitCode;
            try {
                exitCode = future.get(timeoutMs, TimeUnit.MILLISECONDS);
            } catch (TimeoutException e) {
                future.cancel(true);
                throw new SSHException("Command timed out after " + timeoutMs + "ms");
            }
            
            long executionTime = System.currentTimeMillis() - startTime;
            
            return new CommandResult(
                exitCode != null ? exitCode : -1,
                stdout.toString(),
                stderr.toString(),
                executionTime
            );
            
        } catch (JSchException | InterruptedException | ExecutionException e) {
            throw new SSHException("Failed to execute command: " + command, e);
        } finally {
            if (channel != null) {
                channel.disconnect();
            }
        }
    }
    
    @Override
    public void uploadFile(String localPath, String remotePath) throws SSHException {
        if (!isConnected()) {
            throw new SSHException("Not connected to SSH server");
        }
        
        ChannelSftp sftpChannel = null;
        try {
            sftpChannel = (ChannelSftp) session.openChannel("sftp");
            sftpChannel.connect();
            sftpChannel.put(localPath, remotePath, ChannelSftp.OVERWRITE);
        } catch (JSchException | SftpException e) {
            throw new SSHException("Failed to upload file: " + localPath, e);
        } finally {
            if (sftpChannel != null) {
                sftpChannel.disconnect();
            }
        }
    }
    
    @Override
    public void downloadFile(String remotePath, String localPath) throws SSHException {
        if (!isConnected()) {
            throw new SSHException("Not connected to SSH server");
        }
        
        ChannelSftp sftpChannel = null;
        try {
            sftpChannel = (ChannelSftp) session.openChannel("sftp");
            sftpChannel.connect();
            sftpChannel.get(remotePath, localPath);
        } catch (JSchException | SftpException e) {
            throw new SSHException("Failed to download file: " + remotePath, e);
        } finally {
            if (sftpChannel != null) {
                sftpChannel.disconnect();
            }
        }
    }
    
    @Override
    public int createTunnel(int localPort, String remoteHost, int remotePort) throws SSHException {
        if (!isConnected()) {
            throw new SSHException("Not connected to SSH server");
        }
        
        try {
            return session.setPortForwardingL(localPort, remoteHost, remotePort);
        } catch (JSchException e) {
            throw new SSHException("Failed to create tunnel", e);
        }
    }
    
    @Override
    public CommandResult executeScript(String script) throws SSHException {
        // Create temporary script file
        String tempScript = "/tmp/orchestrix_script_" + System.currentTimeMillis() + ".sh";
        
        try {
            // Upload script
            File tempFile = File.createTempFile("script", ".sh");
            Files.write(tempFile.toPath(), script.getBytes());
            uploadFile(tempFile.getAbsolutePath(), tempScript);
            tempFile.delete();
            
            // Make executable and run
            executeCommand("chmod +x " + tempScript);
            CommandResult result = executeCommand(tempScript);
            
            // Cleanup
            executeCommand("rm -f " + tempScript);
            
            return result;
            
        } catch (IOException e) {
            throw new SSHException("Failed to execute script", e);
        }
    }
    
    @Override
    public boolean isConnected() {
        return session != null && session.isConnected();
    }
    
    @Override
    public void disconnect() {
        if (session != null) {
            session.disconnect();
            session = null;
        }
    }
    
    @Override
    public SessionInfo getSessionInfo() {
        if (!isConnected()) {
            return null;
        }
        return new SessionInfo(host, port, username, connectedSince);
    }
    
    @Override
    public void close() {
        disconnect();
    }
}