package com.orchestrix.automation.ssh.api;

import java.util.Map;

/**
 * SSH Client interface defining contract for SSH operations
 */
public interface SSHClient extends AutoCloseable {
    
    /**
     * Connect to remote host
     */
    void connect(String host, int port, String username, String keyPath) throws SSHException;
    
    /**
     * Connect using password authentication
     */
    void connectWithPassword(String host, int port, String username, String password) throws SSHException;
    
    /**
     * Execute a command on remote host
     */
    CommandResult executeCommand(String command) throws SSHException;
    
    /**
     * Execute command with timeout
     */
    CommandResult executeCommand(String command, long timeoutMs) throws SSHException;
    
    /**
     * Execute command with environment variables
     */
    CommandResult executeCommand(String command, Map<String, String> env) throws SSHException;
    
    /**
     * Upload file to remote host
     */
    void uploadFile(String localPath, String remotePath) throws SSHException;
    
    /**
     * Download file from remote host
     */
    void downloadFile(String remotePath, String localPath) throws SSHException;
    
    /**
     * Create SSH tunnel
     */
    int createTunnel(int localPort, String remoteHost, int remotePort) throws SSHException;
    
    /**
     * Check if connected
     */
    boolean isConnected();
    
    /**
     * Disconnect from remote host
     */
    void disconnect();
    
    /**
     * Get session information
     */
    SessionInfo getSessionInfo();
    
    /**
     * Execute script on remote host
     */
    CommandResult executeScript(String script) throws SSHException;
    
    /**
     * Result of command execution
     */
    class CommandResult {
        private final int exitCode;
        private final String stdout;
        private final String stderr;
        private final long executionTime;
        
        public CommandResult(int exitCode, String stdout, String stderr, long executionTime) {
            this.exitCode = exitCode;
            this.stdout = stdout;
            this.stderr = stderr;
            this.executionTime = executionTime;
        }
        
        public int getExitCode() { return exitCode; }
        public String getStdout() { return stdout; }
        public String getStderr() { return stderr; }
        public long getExecutionTime() { return executionTime; }
        public boolean isSuccess() { return exitCode == 0; }
    }
    
    /**
     * SSH session information
     */
    class SessionInfo {
        private final String host;
        private final int port;
        private final String username;
        private final long connectedSince;
        
        public SessionInfo(String host, int port, String username, long connectedSince) {
            this.host = host;
            this.port = port;
            this.username = username;
            this.connectedSince = connectedSince;
        }
        
        public String getHost() { return host; }
        public int getPort() { return port; }
        public String getUsername() { return username; }
        public long getConnectedSince() { return connectedSince; }
    }
    
    /**
     * SSH Exception
     */
    class SSHException extends Exception {
        public SSHException(String message) { super(message); }
        public SSHException(String message, Throwable cause) { super(message, cause); }
    }
}