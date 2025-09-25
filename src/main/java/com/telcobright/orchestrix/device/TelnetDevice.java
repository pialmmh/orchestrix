package com.telcobright.orchestrix.device;

import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.net.Socket;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Telnet implementation of TerminalDevice for legacy device connections.
 * Provides unified interface for Telnet connections alongside SSH and Local devices.
 */
@Slf4j
public class TelnetDevice implements TerminalDevice {

    private final ExecutorService executorService = Executors.newCachedThreadPool();

    private Socket socket;
    private PrintWriter writer;
    private BufferedReader reader;

    private final String deviceId;
    private String hostname;
    private int port;
    private DeviceStatus status = DeviceStatus.DISCONNECTED;

    public TelnetDevice() {
        this.deviceId = "telnet-" + UUID.randomUUID().toString();
    }

    public TelnetDevice(String deviceId) {
        this.deviceId = deviceId != null ? deviceId : "telnet-" + UUID.randomUUID().toString();
    }

    @Override
    public CompletableFuture<Boolean> connect(String hostname, int port, String username, String password) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                this.hostname = hostname;
                this.port = port;
                this.status = DeviceStatus.CONNECTING;

                log.info("Connecting to Telnet device: {}:{}", hostname, port);

                socket = new Socket(hostname, port);
                socket.setSoTimeout(30000); // 30 second timeout

                writer = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()), true);
                reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));

                // Handle login if credentials provided
                if (username != null && !username.isEmpty()) {
                    // Wait for login prompt and send credentials
                    Thread.sleep(1000); // Give device time to send prompt

                    // Read initial prompt
                    while (reader.ready()) {
                        String line = reader.readLine();
                        log.debug("Initial: {}", line);
                        if (line.toLowerCase().contains("username") || line.toLowerCase().contains("login")) {
                            writer.println(username);
                            break;
                        }
                    }

                    Thread.sleep(500);

                    // Send password if prompted
                    while (reader.ready()) {
                        String line = reader.readLine();
                        log.debug("After username: {}", line);
                        if (line.toLowerCase().contains("password")) {
                            writer.println(password);
                            break;
                        }
                    }
                }

                this.status = DeviceStatus.CONNECTED;
                log.info("Successfully connected to Telnet device: {}:{}", hostname, port);

                return true;

            } catch (Exception e) {
                log.error("Failed to connect to Telnet device {}:{}", hostname, port, e);
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
                log.debug("Sending Telnet command to {}: {}", hostname, command);
                writer.println(command);
                writer.flush();
                return command;

            } catch (Exception e) {
                log.error("Failed to send command to Telnet device {}: {}", hostname, command, e);
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

                // Read until we hit a prompt or timeout
                long startTime = System.currentTimeMillis();
                while (System.currentTimeMillis() - startTime < 5000) { // 5 second timeout
                    if (reader.ready()) {
                        line = reader.readLine();
                        if (line != null) {
                            response.append(line).append("\n");

                            // Check for common prompts
                            if (line.contains(">") || line.contains("#") ||
                                line.contains("$") || line.contains(":")) {
                                break;
                            }
                        }
                    } else {
                        Thread.sleep(100);
                    }
                }

                String result = response.toString();
                log.debug("Received Telnet response from {}: {} bytes", hostname, result.length());
                return result;

            } catch (Exception e) {
                log.error("Failed to receive response from Telnet device {}", hostname, e);
                throw new RuntimeException("Failed to receive response", e);
            }
        }, executorService);
    }

    @Override
    public CompletableFuture<String> sendAndReceive(String command) {
        return send(command)
            .thenCompose(sent -> receive())
            .thenApply(response -> {
                log.debug("Telnet command '{}' executed on {}, response length: {}",
                         command, hostname, response.length());
                return response;
            });
    }

    @Override
    public boolean isConnected() {
        return socket != null && socket.isConnected() && !socket.isClosed() &&
               status == DeviceStatus.CONNECTED;
    }

    @Override
    public void disconnect() throws IOException {
        try {
            log.info("Disconnecting from Telnet device: {}", hostname);

            if (writer != null) {
                writer.close();
            }
            if (reader != null) {
                reader.close();
            }
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }

            this.status = DeviceStatus.DISCONNECTED;
            executorService.shutdown();

            log.info("Successfully disconnected from Telnet device: {}", hostname);

        } catch (Exception e) {
            log.error("Error disconnecting from Telnet device {}", hostname, e);
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
        return DeviceType.TELNET;
    }

    @Override
    public DeviceStatus getStatus() {
        return status;
    }
}