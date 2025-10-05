package com.telcobright.orchestrix.automation.core.device.impl;

import com.jcraft.jsch.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.logging.Logger;

/**
 * Remote SSH Device implementation
 * Connects to remote servers via SSH for deployment automation
 */
public class RemoteSshDevice {
    private static final Logger logger = Logger.getLogger(RemoteSshDevice.class.getName());
    private static final int MAX_WAIT_ITERATIONS = 1200; // 2 minutes max

    private final String host;
    private final int port;
    private final String username;

    private JSch jsch;
    private Session session;
    private boolean connected = false;

    public RemoteSshDevice(String host, int port, String username) {
        this.host = host;
        this.port = port;
        this.username = username;
        this.jsch = new JSch();
    }

    public void connect(String password) throws Exception {
        logger.info("Connecting to remote server: " + username + "@" + host + ":" + port);

        session = jsch.getSession(username, host, port);
        session.setPassword(password);

        // Disable strict host key checking for automation
        java.util.Properties config = new java.util.Properties();
        config.put("StrictHostKeyChecking", "no");
        session.setConfig(config);

        session.connect();
        connected = true;

        logger.info("✓ Connected to remote server: " + host);
    }

    public void connectWithKey(String privateKeyPath) throws Exception {
        logger.info("Connecting to remote server with key: " + username + "@" + host + ":" + port);

        jsch.addIdentity(privateKeyPath);
        session = jsch.getSession(username, host, port);

        // Disable strict host key checking for automation
        java.util.Properties config = new java.util.Properties();
        config.put("StrictHostKeyChecking", "no");
        session.setConfig(config);

        session.connect();
        connected = true;

        logger.info("✓ Connected to remote server: " + host);
    }

    public void disconnect() throws Exception {
        if (session != null && session.isConnected()) {
            session.disconnect();
            connected = false;
            logger.info("Disconnected from: " + host);
        }
    }

    public boolean isConnected() {
        return connected && session != null && session.isConnected();
    }

    public String executeCommand(String command) throws Exception {
        return executeCommand(command, shouldUsePty(command));
    }

    public String executeCommand(String command, boolean usePty) throws Exception {
        if (!isConnected()) {
            throw new IllegalStateException("Not connected to remote server");
        }

        ChannelExec channel = (ChannelExec) session.openChannel("exec");
        channel.setCommand(command);

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        ByteArrayOutputStream errorStream = new ByteArrayOutputStream();

        channel.setOutputStream(outputStream);
        channel.setErrStream(errorStream);

        if (usePty) {
            channel.setPty(true);
            channel.setInputStream(new ByteArrayInputStream(new byte[0]));
        }

        channel.connect();

        // Wait for command completion (check exit status, not channel close when using PTY)
        int waited = 0;
        while (channel.getExitStatus() == -1 && waited < MAX_WAIT_ITERATIONS) {
            Thread.sleep(100);
            waited++;
        }

        String output = outputStream.toString();
        String error = errorStream.toString();
        int exitStatus = channel.getExitStatus();

        channel.disconnect();

        // Combine output and error
        String result = output;
        if (!error.isEmpty()) {
            result += error;
        }

        if (exitStatus != 0 && exitStatus != -1) {
            logger.warning("Command exited with status " + exitStatus + ": " + command);
        }

        return result;
    }

    private boolean shouldUsePty(String command) {
        // Commands that require PTY
        return command.contains("lxc ") ||
                command.contains("apt-get") ||
                command.contains("apt ") ||
                command.contains("docker ") ||
                command.contains("systemctl") ||
                command.contains("dpkg");
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public String getUsername() {
        return username;
    }
}
