package com.telcobright.orchestrix.device;

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
        // Default SSH key
        this.privateKeyPath = System.getProperty("user.home") + "/.ssh/id_rsa";
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

            // Configure session
            Properties config = new Properties();
            config.put("StrictHostKeyChecking", "no");
            config.put("PreferredAuthentications", "publickey,password");
            session.setConfig(config);

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

            // Use SSH session
            return executeViaSsh(command);
        });
    }

    /**
     * Execute command via SSH session
     */
    private String executeViaSsh(String command) {
        if (session == null || !session.isConnected()) {
            logger.warning("Not connected to " + host);
            return null;
        }

        try {
            ChannelExec channel = (ChannelExec) session.openChannel("exec");
            channel.setCommand(command);

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            ByteArrayOutputStream errorStream = new ByteArrayOutputStream();

            channel.setOutputStream(outputStream);
            channel.setErrStream(errorStream);

            channel.connect();

            // Wait for command to complete
            while (!channel.isClosed()) {
                Thread.sleep(100);
            }

            channel.disconnect();

            String output = outputStream.toString();
            String error = errorStream.toString();

            if (!error.isEmpty()) {
                logger.warning("Command error: " + error);
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
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                }
            }

            process.waitFor();

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
     * Auto-handles connection if needed
     */
    public String executeCommand(String command) throws Exception {
        // For localhost, directly execute locally
        if (host.equals("localhost") || host.equals("127.0.0.1")) {
            return executeLocally(command);
        }

        // For remote hosts, ensure SSH connection
        if (session == null || !session.isConnected()) {
            if (!connect()) {
                throw new Exception("Failed to connect to " + host);
            }
        }

        return executeViaSsh(command);
    }
}