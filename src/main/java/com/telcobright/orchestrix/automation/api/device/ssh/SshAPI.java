package com.telcobright.orchestrix.automation.api.device.ssh;

import com.telcobright.orchestrix.automation.api.model.AutomationOperationResult;

/**
 * API for SSH-based remote automation operations.
 *
 * <p>This interface provides methods for executing commands on remote Linux servers
 * via SSH connection. Supports both password and key-based authentication.
 *
 * <p>Common use cases:
 * <ul>
 *   <li>Remote server configuration and setup</li>
 *   <li>Package installation and updates</li>
 *   <li>File system operations (BTRFS, LVM, etc.)</li>
 *   <li>Service management and monitoring</li>
 *   <li>Log collection and analysis</li>
 * </ul>
 *
 * <p>Connection parameters:
 * <ul>
 *   <li>hostname - Remote server IP or hostname</li>
 *   <li>port - SSH port (default: 22)</li>
 *   <li>username - SSH username</li>
 *   <li>password - SSH password (optional if using key authentication)</li>
 * </ul>
 */
public interface SshAPI {

    /**
     * Connect to a remote server via SSH.
     *
     * <p>Attempts key-based authentication first (from ~/.ssh/), then falls back
     * to password authentication if provided.
     *
     * @param hostname Remote server hostname or IP address
     * @param port SSH port (typically 22)
     * @param username SSH username
     * @param password SSH password (can be null for key-based auth)
     * @return Result indicating connection success/failure
     * @throws Exception if connection fails
     *
     * @example
     * <pre>
     * SshAPI ssh = new SshAPIImpl();
     * AutomationOperationResult result = ssh.connect(
     *     "192.168.1.100",
     *     22,
     *     "admin",
     *     "password123"
     * );
     * if (result.isSuccess()) {
     *     System.out.println("Connected successfully");
     * }
     * </pre>
     */
    AutomationOperationResult connect(String hostname, int port, String username, String password) throws Exception;

    /**
     * Execute a single command on the remote server.
     *
     * <p>For commands requiring sudo, use {@link #executeSudo(String)} instead.
     *
     * @param command Shell command to execute
     * @return Result containing command output
     * @throws Exception if command execution fails
     *
     * @example
     * <pre>
     * AutomationOperationResult result = ssh.executeCommand("ls -la /var/log");
     * System.out.println("Output: " + result.getMessage());
     * </pre>
     */
    AutomationOperationResult executeCommand(String command) throws Exception;

    /**
     * Execute a command with sudo privileges.
     *
     * <p>For passwordless sudo (user in sudoers NOPASSWD), password can be null.
     *
     * @param command Command to execute with sudo
     * @return Result containing command output
     * @throws Exception if command execution fails
     *
     * @example
     * <pre>
     * // User mustafa is in sudo group with NOPASSWD
     * AutomationOperationResult result = ssh.executeSudo("apt-get update");
     * </pre>
     */
    AutomationOperationResult executeSudo(String command) throws Exception;

    /**
     * Copy a file to the remote server.
     *
     * @param localPath Path to local file
     * @param remotePath Destination path on remote server
     * @return Result indicating success/failure of file transfer
     * @throws Exception if file transfer fails
     *
     * @example
     * <pre>
     * AutomationOperationResult result = ssh.uploadFile(
     *     "/tmp/config.yaml",
     *     "/etc/myapp/config.yaml"
     * );
     * </pre>
     */
    AutomationOperationResult uploadFile(String localPath, String remotePath) throws Exception;

    /**
     * Copy a file from the remote server.
     *
     * @param remotePath Path to file on remote server
     * @param localPath Destination path on local machine
     * @return Result indicating success/failure of file transfer
     * @throws Exception if file transfer fails
     *
     * @example
     * <pre>
     * AutomationOperationResult result = ssh.downloadFile(
     *     "/var/log/app.log",
     *     "/tmp/downloaded-app.log"
     * );
     * </pre>
     */
    AutomationOperationResult downloadFile(String remotePath, String localPath) throws Exception;

    /**
     * Check if currently connected to remote server.
     *
     * @return true if connected, false otherwise
     */
    boolean isConnected();

    /**
     * Disconnect from the remote server.
     *
     * <p>Releases all SSH channels and session resources.
     *
     * @return Result indicating disconnection status
     * @throws Exception if disconnection fails
     */
    AutomationOperationResult disconnect() throws Exception;
}
