package com.telcobright.orchestrix.device;

import com.telcobright.orchestrix.automation.core.LocalCommandExecutor;
import com.telcobright.orchestrix.automation.model.AutomationConfig;
import com.telcobright.orchestrix.automation.model.CommandResult;

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * LocalDevice implementation for executing commands on the local machine.
 * Implements TerminalDevice interface for unified device management.
 */
public class LocalDevice implements TerminalDevice {

    private final ExecutorService executorService = Executors.newCachedThreadPool();
    private final LocalCommandExecutor executor;
    private final String deviceId;
    private DeviceStatus status = DeviceStatus.CONNECTED; // Local is always "connected"
    private String lastCommand;
    private String lastOutput;

    public LocalDevice() {
        this.deviceId = "local-" + UUID.randomUUID().toString();
        AutomationConfig config = new AutomationConfig();
        config.setStreamOutput(true);
        config.setCommandTimeoutMs(120000); // 2 minute default timeout
        this.executor = new LocalCommandExecutor(config);
    }

    public LocalDevice(AutomationConfig config) {
        this.deviceId = "local-" + UUID.randomUUID().toString();
        this.executor = new LocalCommandExecutor(config);
    }

    @Override
    public CompletableFuture<Boolean> connect(String hostname, int port, String username, String password) {
        // Local device is always connected
        return CompletableFuture.completedFuture(true);
    }

    @Override
    public CompletableFuture<String> send(String command) {
        return CompletableFuture.supplyAsync(() -> {
            this.lastCommand = command;
            return command;
        }, executorService);
    }

    @Override
    public CompletableFuture<String> receive() {
        return CompletableFuture.supplyAsync(() -> {
            if (lastCommand == null) {
                throw new RuntimeException("No command to execute");
            }

            CommandResult result = executor.execute(lastCommand);

            if (result.getExitCode() == 0) {
                lastOutput = result.getStdout();
            } else {
                lastOutput = result.getStderr();
                if (lastOutput.isEmpty()) {
                    lastOutput = "Command failed with exit code: " + result.getExitCode();
                }
            }

            lastCommand = null;
            return lastOutput;
        }, executorService);
    }

    @Override
    public CompletableFuture<String> sendAndReceive(String command) {
        return CompletableFuture.supplyAsync(() -> {
            CommandResult result = executor.execute(command);

            if (result.getExitCode() == 0) {
                return result.getStdout();
            } else {
                String error = result.getStderr();
                if (error.isEmpty()) {
                    error = "Command failed with exit code: " + result.getExitCode();
                }
                throw new RuntimeException(error);
            }
        }, executorService);
    }

    @Override
    public boolean isConnected() {
        // Local device is always connected
        return true;
    }

    @Override
    public void disconnect() throws IOException {
        // Nothing to disconnect for local device
        executorService.shutdown();
    }

    @Override
    public String getDeviceId() {
        return deviceId;
    }

    @Override
    public String getHostname() {
        return "localhost";
    }

    @Override
    public DeviceType getDeviceType() {
        return DeviceType.SSH; // Could add LOCAL type to enum
    }

    @Override
    public DeviceStatus getStatus() {
        return status;
    }

    /**
     * Execute a script file on the local machine
     */
    public CompletableFuture<String> executeScript(String script) {
        return CompletableFuture.supplyAsync(() -> {
            CommandResult result = executor.executeScript(script);

            if (result.getExitCode() == 0) {
                return result.getStdout();
            } else {
                throw new RuntimeException("Script failed: " + result.getStderr());
            }
        }, executorService);
    }

    /**
     * Get the underlying LocalCommandExecutor for advanced usage
     */
    public LocalCommandExecutor getExecutor() {
        return executor;
    }
}