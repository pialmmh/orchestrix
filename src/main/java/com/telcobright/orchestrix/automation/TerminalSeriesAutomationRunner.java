package com.telcobright.orchestrix.automation;

import com.telcobright.orchestrix.automation.api.device.TerminalDevice;
import com.telcobright.orchestrix.automation.core.device.SshDevice;
import com.telcobright.orchestrix.automation.core.device.TelnetDevice;
import com.telcobright.orchestrix.automation.core.device.LocalShellDevice;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.logging.Logger;

/**
 * Runner that executes multiple terminal automation tasks sequentially
 * using a single terminal connection for improved performance.
 * Supports SSH, Telnet, and Local Shell connections.
 */
public class TerminalSeriesAutomationRunner {

    private static final Logger logger = Logger.getLogger(TerminalSeriesAutomationRunner.class.getName());

    private TerminalDevice terminalDevice;
    private List<TerminalRunner> runners = new ArrayList<>();
    private Map<String, Map<String, String>> runnerConfigs = new HashMap<>();
    private Map<String, Boolean> executionResults = new HashMap<>();
    private boolean stopOnFailure = true;
    private boolean autoCreateDevice = true;

    /**
     * Default constructor
     */
    public TerminalSeriesAutomationRunner() {
    }

    /**
     * Constructor with terminal device
     */
    public TerminalSeriesAutomationRunner(TerminalDevice device) {
        this.terminalDevice = device;
    }

    /**
     * Set the terminal device to use
     */
    public TerminalSeriesAutomationRunner setTerminalDevice(TerminalDevice device) {
        this.terminalDevice = device;
        return this;
    }

    /**
     * Get the current terminal device
     */
    public TerminalDevice getTerminalDevice() {
        return terminalDevice;
    }

    /**
     * Create a terminal device based on type
     */
    public TerminalSeriesAutomationRunner createDevice(TerminalDevice.DeviceType type) {
        switch (type) {
            case SSH:
                this.terminalDevice = new SshDevice();
                break;
            case TELNET:
                this.terminalDevice = new TelnetDevice();
                break;
            case LOCAL_SHELL:
                this.terminalDevice = new LocalShellDevice();
                break;
            default:
                throw new IllegalArgumentException("Unsupported device type: " + type);
        }
        logger.info("Created " + type + " device");
        return this;
    }

    /**
     * Add a runner to the execution list
     */
    public TerminalSeriesAutomationRunner addRunner(TerminalRunner runner) {
        this.runners.add(runner);
        return this;
    }

    /**
     * Add a runner with specific configuration
     */
    public TerminalSeriesAutomationRunner addRunner(TerminalRunner runner, Map<String, String> config) {
        this.runners.add(runner);
        if (config != null) {
            this.runnerConfigs.put(runner.getName(), config);
        }
        return this;
    }

    /**
     * Set runners list
     */
    public TerminalSeriesAutomationRunner setRunners(List<TerminalRunner> runners) {
        this.runners = new ArrayList<>(runners);
        return this;
    }

    /**
     * Set whether to stop execution on first failure
     */
    public TerminalSeriesAutomationRunner setStopOnFailure(boolean stop) {
        this.stopOnFailure = stop;
        return this;
    }

    /**
     * Set configuration for a specific runner
     */
    public TerminalSeriesAutomationRunner setRunnerConfig(String runnerName, Map<String, String> config) {
        this.runnerConfigs.put(runnerName, config);
        return this;
    }

    /**
     * Execute all runners sequentially
     * @return true if all runners executed successfully, false if any failed
     */
    public boolean execute() throws Exception {
        if (terminalDevice == null) {
            throw new IllegalStateException("Terminal device not set");
        }

        if (!terminalDevice.isConnected()) {
            throw new IllegalStateException("Terminal device not connected");
        }

        if (runners.isEmpty()) {
            logger.warning("No runners configured");
            return true;
        }

        TerminalDevice.DeviceType deviceType = terminalDevice.getDeviceType();
        logger.info("Starting series automation with " + runners.size() + " runners on " + deviceType + " device");
        boolean allSuccess = true;

        for (int i = 0; i < runners.size(); i++) {
            TerminalRunner runner = runners.get(i);
            String runnerName = runner.getName();

            // Check if runner supports this terminal type
            if (!runner.supportsTerminalType(deviceType)) {
                logger.warning("Runner " + runnerName + " does not support " + deviceType + ", skipping");
                executionResults.put(runnerName, true); // Don't fail for unsupported type
                continue;
            }

            logger.info("========================================");
            logger.info("Runner " + (i + 1) + "/" + runners.size() + ": " + runnerName);
            logger.info("Description: " + runner.getDescription());
            logger.info("Terminal Type: " + deviceType);
            logger.info("========================================");

            try {
                // Get configuration for this runner
                Map<String, String> config = runnerConfigs.getOrDefault(runnerName, new HashMap<>());

                // Check if should skip
                if (runner.shouldSkip(terminalDevice, config)) {
                    logger.info("Skipping runner: " + runnerName + " (conditions not met)");
                    executionResults.put(runnerName, true); // Mark as success if skipped
                    continue;
                }

                // Call beforeExecute hook
                if (!runner.beforeExecute(terminalDevice, config)) {
                    logger.warning("Runner " + runnerName + " beforeExecute returned false, skipping");
                    executionResults.put(runnerName, false);
                    if (stopOnFailure) {
                        allSuccess = false;
                        break;
                    }
                    continue;
                }

                // Execute the runner
                logger.info("Executing: " + runnerName);
                boolean success = runner.execute(terminalDevice, config);

                executionResults.put(runnerName, success);

                if (success) {
                    logger.info("✓ " + runnerName + " completed successfully");

                    // Optionally verify
                    if (runner.verify(terminalDevice, config)) {
                        logger.info("✓ " + runnerName + " verification passed");
                    } else {
                        logger.warning("✗ " + runnerName + " verification failed");
                        success = false;
                        executionResults.put(runnerName, false);
                    }
                } else {
                    logger.severe("✗ " + runnerName + " execution failed");
                    allSuccess = false;

                    if (stopOnFailure) {
                        logger.severe("Stopping execution due to failure (stopOnFailure=true)");
                        break;
                    }
                }

                // Call afterExecute hook
                runner.afterExecute(terminalDevice, config, success);

                // Log status if available
                Map<String, String> status = runner.getStatus();
                if (!status.isEmpty()) {
                    logger.info("Status for " + runnerName + ":");
                    status.forEach((key, value) ->
                        logger.info("  " + key + ": " + value));
                }

            } catch (Exception e) {
                logger.severe("Exception in runner " + runnerName + ": " + e.getMessage());
                e.printStackTrace();
                executionResults.put(runnerName, false);
                allSuccess = false;

                if (stopOnFailure) {
                    logger.severe("Stopping execution due to exception");
                    break;
                }
            }

            logger.info("");
        }

        // Print summary
        printExecutionSummary();

        return allSuccess;
    }

    /**
     * Execute runners with connection parameters
     * Creates and manages the terminal connection internally
     */
    public boolean executeWithConnection(TerminalDevice.DeviceType deviceType, String host, int port,
                                        String username, String password) throws Exception {
        logger.info("Establishing " + deviceType + " connection to " + host + ":" + port);

        // Create device if not already set
        if (terminalDevice == null) {
            createDevice(deviceType);
        }

        try {
            // Connect to terminal
            boolean connected = terminalDevice.connect(host, port, username, password).get();
            if (!connected) {
                logger.severe("Failed to establish " + deviceType + " connection");
                return false;
            }

            logger.info(deviceType + " connection established successfully");

            // Execute all runners
            return execute();

        } finally {
            // Always disconnect when done
            if (terminalDevice != null && terminalDevice.isConnected()) {
                logger.info("Closing " + deviceType + " connection");
                terminalDevice.disconnect();
            }
        }
    }

    /**
     * Execute runners with SSH connection (backward compatibility)
     */
    public boolean executeWithConnection(String host, int port, String username,
                                        String password) throws Exception {
        return executeWithConnection(TerminalDevice.DeviceType.SSH, host, port, username, password);
    }

    /**
     * Execute runners with local shell
     */
    public boolean executeWithLocalShell() throws Exception {
        logger.info("Using local shell for execution");

        if (terminalDevice == null) {
            terminalDevice = new LocalShellDevice();
        }

        try {
            // Connect to local shell (no actual network connection)
            boolean connected = terminalDevice.connect("localhost", 0, System.getProperty("user.name"), "").get();
            if (!connected) {
                logger.severe("Failed to initialize local shell");
                return false;
            }

            logger.info("Local shell initialized successfully");

            // Execute all runners
            return execute();

        } finally {
            // Clean up local shell resources
            if (terminalDevice != null && terminalDevice.isConnected()) {
                logger.info("Cleaning up local shell");
                terminalDevice.disconnect();
            }
        }
    }

    /**
     * Print execution summary
     */
    private void printExecutionSummary() {
        logger.info("========================================");
        logger.info("EXECUTION SUMMARY");
        logger.info("========================================");

        int successful = 0;
        int failed = 0;
        int total = executionResults.size();

        for (Map.Entry<String, Boolean> entry : executionResults.entrySet()) {
            String status = entry.getValue() ? "✓ SUCCESS" : "✗ FAILED";
            logger.info(status + " - " + entry.getKey());

            if (entry.getValue()) {
                successful++;
            } else {
                failed++;
            }
        }

        logger.info("----------------------------------------");
        logger.info("Total: " + total + " | Success: " + successful + " | Failed: " + failed);
        if (terminalDevice != null) {
            logger.info("Terminal Type: " + terminalDevice.getDeviceType());
        }
        logger.info("========================================");
    }

    /**
     * Get execution results
     */
    public Map<String, Boolean> getExecutionResults() {
        return new HashMap<>(executionResults);
    }

    /**
     * Clear all runners and configurations
     */
    public void clear() {
        runners.clear();
        runnerConfigs.clear();
        executionResults.clear();
    }

    /**
     * Get the number of configured runners
     */
    public int getRunnerCount() {
        return runners.size();
    }
}