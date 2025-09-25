package com.telcobright.orchestrix.device;

import java.io.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

/**
 * LocalShellDevice implementation for executing commands on the local system
 * Provides a terminal interface for local bash/cmd/powershell operations
 */
public class LocalShellDevice implements TerminalDevice {

    private static final Logger logger = Logger.getLogger(LocalShellDevice.class.getName());

    private boolean connected = false;
    private DeviceStatus status = DeviceStatus.DISCONNECTED;
    private String shell = null;
    private String hostname = "localhost";
    private String username = System.getProperty("user.name");
    private String deviceId = "local-shell-" + System.currentTimeMillis();
    private long commandTimeout = 120000; // 2 minutes default

    /**
     * Default constructor
     */
    public LocalShellDevice() {
        detectShell();
    }

    /**
     * Detect the appropriate shell for the current OS
     */
    private void detectShell() {
        String os = System.getProperty("os.name").toLowerCase();
        if (os.contains("win")) {
            // Windows - use PowerShell if available, otherwise cmd
            if (new File("C:\\Windows\\System32\\WindowsPowerShell\\v1.0\\powershell.exe").exists()) {
                shell = "powershell.exe";
            } else {
                shell = "cmd.exe";
            }
        } else {
            // Unix/Linux/Mac - use bash if available, otherwise sh
            if (new File("/bin/bash").exists()) {
                shell = "/bin/bash";
            } else if (new File("/usr/bin/bash").exists()) {
                shell = "/usr/bin/bash";
            } else {
                shell = "/bin/sh";
            }
        }
        logger.info("Detected shell: " + shell);
    }

    @Override
    public CompletableFuture<Boolean> connect(String hostname, int port, String username, String password) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                this.hostname = hostname != null ? hostname : "localhost";
                this.username = username != null ? username : System.getProperty("user.name");

                status = DeviceStatus.CONNECTING;
                logger.info("Initializing local shell connection for user: " + this.username);

                // For local shell, we don't actually connect to anything
                // Just verify we can execute commands
                ProcessBuilder pb = new ProcessBuilder(shell, "--version");
                Process process = pb.start();
                boolean completed = process.waitFor(5, TimeUnit.SECONDS);

                if (completed && process.exitValue() == 0) {
                    connected = true;
                    status = DeviceStatus.CONNECTED;
                    logger.info("Local shell initialized successfully: " + shell);
                    return true;
                } else {
                    status = DeviceStatus.ERROR;
                    logger.severe("Failed to initialize local shell");
                    return false;
                }
            } catch (Exception e) {
                status = DeviceStatus.ERROR;
                logger.severe("Error initializing local shell: " + e.getMessage());
                return false;
            }
        });
    }

    @Override
    public CompletableFuture<String> send(String command) {
        return sendAndReceive(command);
    }

    @Override
    public CompletableFuture<String> receive() {
        // For local shell, receive doesn't make sense without a command
        return CompletableFuture.completedFuture("");
    }

    @Override
    public CompletableFuture<String> sendAndReceive(String command) {
        return CompletableFuture.supplyAsync(() -> {
            if (!connected) {
                logger.warning("Not connected to local shell");
                return "Error: Not connected";
            }

            try {
                logger.fine("Executing local command: " + command);

                ProcessBuilder pb;
                String os = System.getProperty("os.name").toLowerCase();

                if (os.contains("win")) {
                    if (shell.contains("powershell")) {
                        pb = new ProcessBuilder("powershell.exe", "-Command", command);
                    } else {
                        pb = new ProcessBuilder("cmd.exe", "/c", command);
                    }
                } else {
                    pb = new ProcessBuilder(shell, "-c", command);
                }

                pb.redirectErrorStream(true);
                Process process = pb.start();

                // Read output
                StringBuilder output = new StringBuilder();
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(process.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        output.append(line).append("\n");
                    }
                }

                // Wait for completion with timeout
                boolean completed = process.waitFor(commandTimeout, TimeUnit.MILLISECONDS);

                if (!completed) {
                    process.destroyForcibly();
                    logger.warning("Command timed out: " + command);
                    return "Error: Command timed out after " + commandTimeout + "ms";
                }

                String result = output.toString();
                logger.fine("Command output: " + result);

                return result;

            } catch (Exception e) {
                logger.severe("Error executing command: " + e.getMessage());
                return "Error: " + e.getMessage();
            }
        });
    }

    @Override
    public boolean isConnected() {
        return connected;
    }

    @Override
    public void disconnect() {
        if (connected) {
            logger.info("Closing local shell connection");
            connected = false;
            status = DeviceStatus.DISCONNECTED;
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
        return DeviceType.LOCAL_SHELL;
    }

    @Override
    public DeviceStatus getStatus() {
        return status;
    }

    /**
     * Set the shell to use (bash, sh, cmd, powershell)
     */
    public void setShell(String shell) {
        this.shell = shell;
        logger.info("Shell set to: " + shell);
    }

    /**
     * Get the current shell
     */
    public String getShell() {
        return shell;
    }

    /**
     * Set command timeout in milliseconds
     */
    public void setCommandTimeout(long timeout) {
        this.commandTimeout = timeout;
    }

    /**
     * Get command timeout in milliseconds
     */
    public long getCommandTimeout() {
        return commandTimeout;
    }

    /**
     * Execute a command with a specific timeout
     */
    public CompletableFuture<String> executeWithTimeout(String command, long timeoutMs) {
        long originalTimeout = commandTimeout;
        commandTimeout = timeoutMs;
        CompletableFuture<String> result = sendAndReceive(command);
        commandTimeout = originalTimeout;
        return result;
    }

    /**
     * Check if a command exists in the system PATH
     */
    public CompletableFuture<Boolean> commandExists(String command) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String checkCmd;
                String os = System.getProperty("os.name").toLowerCase();

                if (os.contains("win")) {
                    checkCmd = "where " + command + " 2>nul";
                } else {
                    checkCmd = "which " + command + " 2>/dev/null";
                }

                String result = sendAndReceive(checkCmd).get();
                return result != null && !result.isEmpty() && !result.contains("Error:");

            } catch (Exception e) {
                return false;
            }
        });
    }

    /**
     * Get environment variable value
     */
    public CompletableFuture<String> getEnvironmentVariable(String varName) {
        String os = System.getProperty("os.name").toLowerCase();
        String command;

        if (os.contains("win")) {
            command = "echo %" + varName + "%";
        } else {
            command = "echo $" + varName;
        }

        return sendAndReceive(command);
    }

    @Override
    public String toString() {
        return "LocalShellDevice{" +
                "shell='" + shell + '\'' +
                ", hostname='" + hostname + '\'' +
                ", username='" + username + '\'' +
                ", connected=" + connected +
                ", status=" + status +
                '}';
    }
}