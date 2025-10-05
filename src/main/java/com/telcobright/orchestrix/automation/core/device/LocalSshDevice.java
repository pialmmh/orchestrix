package com.telcobright.orchestrix.automation.core.device;

import com.jcraft.jsch.*;
import java.io.*;
import java.util.concurrent.CompletableFuture;
import java.util.Properties;
import java.util.logging.Logger;

/**
 * SSH Device implementation for localhost connections
 * Optimized for connecting to the local machine
 * Provides simple command execution without requiring explicit connection
 */
public class LocalSshDevice {

    private static final Logger logger = Logger.getLogger(LocalSshDevice.class.getName());

    private String host = "localhost";
    private int port = 22;
    private String username;
    private String password;
    private String privateKeyPath;
    private Session session;
    private JSch jsch;

    public LocalSshDevice() {
        this.jsch = new JSch();
        // Default to current user
        this.username = System.getProperty("user.name");
        // Default SSH key - use JSch-compatible key if available
        String jschKey = System.getProperty("user.home") + "/.ssh/id_rsa_jsch";
        File jschKeyFile = new File(jschKey);
        if (jschKeyFile.exists()) {
            this.privateKeyPath = jschKey;
        } else {
            this.privateKeyPath = System.getProperty("user.home") + "/.ssh/id_rsa";
        }
    }

    public boolean connect() {
        try {
            logger.info("Connecting to " + host + ":" + port + " as " + username);

            // Try to add private key if it exists
            if (privateKeyPath != null && !privateKeyPath.isEmpty()) {
                File keyFile = new File(privateKeyPath);
                if (keyFile.exists()) {
                    try {
                        jsch.addIdentity(privateKeyPath);
                        logger.info("Added SSH key: " + privateKeyPath);
                    } catch (JSchException e) {
                        logger.warning("Failed to add SSH key: " + e.getMessage());
                    }
                }
            }

            // Create session
            session = jsch.getSession(username, host, port);

            // Configure session - disable all host key checking
            Properties config = new Properties();
            config.put("StrictHostKeyChecking", "no");
            config.put("PreferredAuthentications", "publickey,password");
            config.put("CheckHostIP", "no");
            config.put("UserKnownHostsFile", "/dev/null");
            // Use modern RSA signature algorithms (ssh-rsa is deprecated)
            config.put("server_host_key", "ssh-rsa,rsa-sha2-256,rsa-sha2-512");
            config.put("PubkeyAcceptedAlgorithms", "rsa-sha2-256,rsa-sha2-512,ssh-rsa");
            session.setConfig(config);

            // Also set global config
            JSch.setConfig("StrictHostKeyChecking", "no");
            JSch.setConfig("server_host_key", "ssh-rsa,rsa-sha2-256,rsa-sha2-512");
            JSch.setConfig("PubkeyAcceptedAlgorithms", "rsa-sha2-256,rsa-sha2-512,ssh-rsa");

            // Set password if provided
            if (password != null && !password.isEmpty()) {
                session.setPassword(password);
            }

            // Set timeout
            session.setTimeout(10000);

            // Connect
            session.connect();

            logger.info("Successfully connected to " + host);
            return true;

        } catch (JSchException e) {
            logger.severe("Failed to connect: " + e.getMessage());

            // Try alternative connection for localhost
            if (host.equals("localhost") || host.equals("127.0.0.1")) {
                logger.info("Attempting passwordless sudo connection...");
                return tryLocalConnection();
            }

            return false;
        }
    }

    /**
     * Try alternative connection method for localhost
     */
    private boolean tryLocalConnection() {
        try {
            // Test if we can run commands locally without SSH
            Process process = Runtime.getRuntime().exec("whoami");
            process.waitFor();

            if (process.exitValue() == 0) {
                logger.info("Local command execution available");
                // Mark as connected even without SSH session
                return true;
            }
        } catch (Exception e) {
            logger.warning("Local execution failed: " + e.getMessage());
        }
        return false;
    }

    public boolean disconnect() {
        if (session != null && session.isConnected()) {
            session.disconnect();
            logger.info("Disconnected from " + host);
        }
        return true;
    }

    public CompletableFuture<String> sendAndReceive(String command) {
        return CompletableFuture.supplyAsync(() -> {
            // If localhost and no SSH session, try local execution
            if ((host.equals("localhost") || host.equals("127.0.0.1"))
                && (session == null || !session.isConnected())) {
                return executeLocally(command);
            }

            // Use SSH session with conditional PTY
            return executeViaSsh(command, shouldUsePty(command));
        });
    }

    /**
     * Execute command via SSH session
     * @param command The command to execute
     * @param usePty Whether to allocate pseudo-terminal (needed for lxc, apt-get, etc.)
     */
    private String executeViaSsh(String command, boolean usePty) {
        if (session == null || !session.isConnected()) {
            logger.warning("Not connected to " + host);
            return null;
        }

        try {
            ChannelExec channel = (ChannelExec) session.openChannel("exec");

            // Request PTY only if needed
            // PTY enables progress bars and terminal features but keeps channel open longer
            if (usePty) {
                channel.setPty(true);
                // Provide empty stdin when using PTY
                channel.setInputStream(new java.io.ByteArrayInputStream(new byte[0]));
            }

            channel.setCommand(command);

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            ByteArrayOutputStream errorStream = new ByteArrayOutputStream();

            channel.setOutputStream(outputStream);
            channel.setErrStream(errorStream);

            channel.connect();

            // Wait for command to complete by checking exit status (not channel close)
            // With PTY, channel may stay open even after command completes
            int maxWait = 3000; // 5 minutes max (300 seconds)
            int waited = 0;
            while (channel.getExitStatus() == -1 && waited < maxWait) {
                Thread.sleep(100);
                waited++;
            }

            // Check if command timed out
            if (channel.getExitStatus() == -1) {
                logger.warning("Command did not complete within timeout");
                channel.disconnect();
                throw new Exception("Command execution timeout");
            }

            int exitStatus = channel.getExitStatus();

            channel.disconnect();

            String output = outputStream.toString();
            String error = errorStream.toString();

            if (!error.isEmpty() && exitStatus != 0) {
                logger.warning("Command error (exit " + exitStatus + "): " + error);
            }

            return output;

        } catch (Exception e) {
            logger.severe("Failed to execute command: " + e.getMessage());
            return null;
        }
    }

    /**
     * Execute command locally (for localhost connections)
     */
    private String executeLocally(String command) {
        try {
            ProcessBuilder pb = new ProcessBuilder("bash", "-c", command);
            pb.redirectErrorStream(true);

            Process process = pb.start();

            StringBuilder output = new StringBuilder();

            // Read output in a separate thread to avoid blocking
            Thread outputReader = new Thread(() -> {
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(process.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        output.append(line).append("\n");
                        // Log progress for long-running commands
                        if (line.contains("%") || line.contains("Retrieving") || line.contains("Unpacking")) {
                            logger.info(line);
                        }
                    }
                } catch (IOException e) {
                    logger.warning("Error reading command output: " + e.getMessage());
                }
            });
            outputReader.start();

            // Wait for process with timeout (30 minutes for image downloads)
            boolean finished = process.waitFor(30, java.util.concurrent.TimeUnit.MINUTES);

            if (!finished) {
                process.destroyForcibly();
                throw new Exception("Command timed out after 30 minutes: " + command);
            }

            // Wait for output reader to finish
            outputReader.join(5000);

            return output.toString();

        } catch (Exception e) {
            logger.severe("Failed to execute local command: " + e.getMessage());
            return null;
        }
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getPrivateKeyPath() {
        return privateKeyPath;
    }

    public void setPrivateKeyPath(String privateKeyPath) {
        this.privateKeyPath = privateKeyPath;
    }

    public boolean isConnected() {
        return session != null && session.isConnected();
    }

    /**
     * Execute a command - main method used by automation classes
     * REQUIRES SSH connection (no ProcessBuilder fallback for automation)
     * Automatically determines if PTY is needed based on command
     */
    public String executeCommand(String command) throws Exception {
        return executeCommand(command, shouldUsePty(command));
    }

    /**
     * Execute a command with explicit PTY control
     * @param command The command to execute
     * @param usePty Whether to allocate a pseudo-terminal
     */
    public String executeCommand(String command, boolean usePty) throws Exception {
        // Ensure SSH connection exists
        if (session == null || !session.isConnected()) {
            throw new Exception("SSH connection required. Please connect() first. No ProcessBuilder fallback allowed.");
        }

        return executeViaSsh(command, usePty);
    }

    /**
     * Determine if command requires PTY (pseudo-terminal)
     * PTY is needed for commands that:
     * - Show progress bars (lxc, apt-get, docker)
     * - Expect interactive terminal (systemctl, some installers)
     * - Use terminal control codes for output
     */
    private boolean shouldUsePty(String command) {
        return command.contains("lxc ") ||
               command.contains("apt-get") ||
               command.contains("apt ") ||
               command.contains("docker ") ||
               command.contains("systemctl") ||
               command.contains("dpkg");
    }
}