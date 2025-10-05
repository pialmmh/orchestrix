package com.telcobright.orchestrix.automation.core.device;

import com.telcobright.orchestrix.automation.api.device.TerminalDevice;
import com.telcobright.orchestrix.automation.api.device.TerminalDevice.DeviceType;
import com.telcobright.orchestrix.automation.api.device.TerminalDevice.DeviceStatus;

import com.jcraft.jsch.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * SSH implementation of TerminalDevice for remote command execution.
 * Provides unified interface for SSH connections alongside LocalDevice.
 */
public class SshDevice implements TerminalDevice {

    private static final Logger log = LoggerFactory.getLogger(SshDevice.class);

    private final ExecutorService executorService = Executors.newCachedThreadPool();

    private Session session;
    private ChannelExec execChannel;
    private ChannelShell shellChannel;
    private PrintWriter shellWriter;
    private BufferedReader shellReader;

    private final String deviceId;
    private String hostname;
    private int port;
    private String username;
    private DeviceStatus status = DeviceStatus.DISCONNECTED;

    private boolean useShellMode = false; // Toggle between exec and shell mode

    public SshDevice() {
        this.deviceId = "ssh-" + UUID.randomUUID().toString();
    }

    public SshDevice(String deviceId) {
        this.deviceId = deviceId != null ? deviceId : "ssh-" + UUID.randomUUID().toString();
    }

    /**
     * Enable shell mode for interactive sessions (default is exec mode for single commands)
     */
    public void setShellMode(boolean useShell) {
        this.useShellMode = useShell;
    }

    @Override
    public CompletableFuture<Boolean> connect(String hostname, int port, String username, String password) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                this.hostname = hostname;
                this.port = port;
                this.username = username;
                this.status = DeviceStatus.CONNECTING;

                log.info("Connecting to SSH device: {}:{}", hostname, port);

                JSch jsch = new JSch();

                // Try SSH key authentication if password is empty
                if (password == null || password.isEmpty()) {
                    String homeDir = System.getProperty("user.home");
                    String[] keyPaths = {
                        homeDir + "/.ssh/id_rsa",
                        homeDir + "/.ssh/id_ed25519",
                        homeDir + "/.ssh/id_dsa"
                    };

                    for (String keyPath : keyPaths) {
                        File keyFile = new File(keyPath);
                        if (keyFile.exists()) {
                            try {
                                jsch.addIdentity(keyFile.getAbsolutePath());
                                log.debug("Using SSH key: {}", keyPath);
                                break;
                            } catch (JSchException e) {
                                log.debug("Failed to add key {}: {}", keyPath, e.getMessage());
                            }
                        }
                    }
                }

                session = jsch.getSession(username, hostname, port);

                if (password != null && !password.isEmpty()) {
                    session.setPassword(password);
                }

                // SSH configuration
                session.setConfig("StrictHostKeyChecking", "no");
                session.setConfig("UserKnownHostsFile", "/dev/null");
                session.setConfig("CheckHostIP", "no");
                session.setConfig("LogLevel", "ERROR");

                session.connect(30000); // 30 second timeout

                if (useShellMode) {
                    // Open shell channel for interactive mode
                    shellChannel = (ChannelShell) session.openChannel("shell");
                    shellChannel.connect();

                    shellWriter = new PrintWriter(new OutputStreamWriter(shellChannel.getOutputStream()), true);
                    shellReader = new BufferedReader(new InputStreamReader(shellChannel.getInputStream()));
                }

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
                if (useShellMode && shellWriter != null) {
                    log.debug("Sending shell command to {}: {}", hostname, command);
                    shellWriter.println(command);
                    shellWriter.flush();
                } else {
                    // Store command for exec mode execution in receive()
                    log.debug("Preparing exec command for {}: {}", hostname, command);
                }
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
                if (useShellMode && shellReader != null) {
                    // Read from shell channel
                    StringBuilder response = new StringBuilder();
                    String line;

                    while (shellReader.ready() || response.length() == 0) {
                        line = shellReader.readLine();
                        if (line == null) break;
                        response.append(line).append("\n");

                        // Stop at prompt
                        if (line.contains(">") || line.contains("#") || line.contains("$")) {
                            break;
                        }
                    }

                    String result = response.toString();
                    log.debug("Received shell response from {}: {} bytes", hostname, result.length());
                    return result;

                } else {
                    // Exec mode - no separate receive needed
                    return "";
                }

            } catch (Exception e) {
                log.error("Failed to receive response from device {}", hostname, e);
                throw new RuntimeException("Failed to receive response", e);
            }
        }, executorService);
    }

    @Override
    public CompletableFuture<String> sendAndReceive(String command) {
        return CompletableFuture.supplyAsync(() -> {
            if (!isConnected()) {
                throw new RuntimeException("Device not connected");
            }

            try {
                if (useShellMode) {
                    // Use shell channel
                    return send(command)
                        .thenCompose(sent -> receive())
                        .get();
                } else {
                    // Use exec channel for single command
                    execChannel = (ChannelExec) session.openChannel("exec");
                    execChannel.setCommand(command);

                    // Get output streams
                    ByteArrayOutputStream stdout = new ByteArrayOutputStream();
                    ByteArrayOutputStream stderr = new ByteArrayOutputStream();

                    execChannel.setOutputStream(stdout);
                    execChannel.setErrStream(stderr);

                    execChannel.connect();

                    // Wait for command to complete
                    while (!execChannel.isClosed()) {
                        Thread.sleep(100);
                    }

                    String output = stdout.toString();
                    String error = stderr.toString();

                    int exitStatus = execChannel.getExitStatus();

                    execChannel.disconnect();
                    execChannel = null;

                    if (exitStatus != 0 && !error.isEmpty()) {
                        log.warn("Command '{}' on {} returned exit code {} with error: {}",
                                command, hostname, exitStatus, error);
                        return error;
                    }

                    log.debug("Command '{}' executed on {}, response length: {}",
                            command, hostname, output.length());

                    return output;
                }

            } catch (Exception e) {
                log.error("Failed to execute command '{}' on device {}", command, hostname, e);
                throw new RuntimeException("Failed to execute command", e);
            }
        }, executorService);
    }

    @Override
    public boolean isConnected() {
        return session != null && session.isConnected() && status == DeviceStatus.CONNECTED;
    }

    @Override
    public void disconnect() throws IOException {
        try {
            log.info("Disconnecting from SSH device: {}", hostname);

            if (shellWriter != null) {
                shellWriter.close();
            }
            if (shellReader != null) {
                shellReader.close();
            }
            if (shellChannel != null && shellChannel.isConnected()) {
                shellChannel.disconnect();
            }
            if (execChannel != null && execChannel.isConnected()) {
                execChannel.disconnect();
            }
            if (session != null && session.isConnected()) {
                session.disconnect();
            }

            this.status = DeviceStatus.DISCONNECTED;
            executorService.shutdown();

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
        return hostname != null ? hostname : "not-connected";
    }

    @Override
    public DeviceType getDeviceType() {
        return DeviceType.SSH;
    }

    @Override
    public DeviceStatus getStatus() {
        return status;
    }

    /**
     * Execute command with sudo
     */
    public CompletableFuture<String> executeSudo(String command, String sudoPassword) {
        String sudoCommand = String.format("echo '%s' | sudo -S %s", sudoPassword, command);
        return sendAndReceive(sudoCommand);
    }

    /**
     * Copy file to remote host using SCP
     */
    public CompletableFuture<Boolean> scpTo(String localFile, String remoteFile) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                ChannelSftp sftpChannel = (ChannelSftp) session.openChannel("sftp");
                sftpChannel.connect();

                sftpChannel.put(localFile, remoteFile);

                sftpChannel.disconnect();
                log.info("Successfully copied {} to {}:{}", localFile, hostname, remoteFile);
                return true;

            } catch (Exception e) {
                log.error("Failed to copy file to {}: {}", hostname, e.getMessage());
                return false;
            }
        }, executorService);
    }

    /**
     * Copy file from remote host using SCP
     */
    public CompletableFuture<Boolean> scpFrom(String remoteFile, String localFile) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                ChannelSftp sftpChannel = (ChannelSftp) session.openChannel("sftp");
                sftpChannel.connect();

                sftpChannel.get(remoteFile, localFile);

                sftpChannel.disconnect();
                log.info("Successfully copied {}:{} to {}", hostname, remoteFile, localFile);
                return true;

            } catch (Exception e) {
                log.error("Failed to copy file from {}: {}", hostname, e.getMessage());
                return false;
            }
        }, executorService);
    }

    /**
     * Synchronous wrapper for sendAndReceive - executes command and returns result
     * Blocks until command completes
     */
    public String executeCommand(String command) throws Exception {
        try {
            return sendAndReceive(command).get();
        } catch (Exception e) {
            log.error("Failed to execute command '{}' on {}: {}", command, hostname, e.getMessage());
            throw e;
        }
    }

    /**
     * Synchronous wrapper for sendAndReceive with PTY flag (ignored - for compatibility)
     * Blocks until command completes
     */
    public String executeCommand(String command, boolean usePty) throws Exception {
        // PTY flag is ignored in this implementation - just execute normally
        return executeCommand(command);
    }
}