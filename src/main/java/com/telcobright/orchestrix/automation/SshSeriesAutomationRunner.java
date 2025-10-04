package com.telcobright.orchestrix.automation;

import com.telcobright.orchestrix.automation.core.device.SshDevice;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.logging.Logger;

/**
 * Runner that executes multiple SSH automation tasks sequentially
 * using a single SSH connection for improved performance
 */
public class SshSeriesAutomationRunner {

    private static final Logger logger = Logger.getLogger(SshSeriesAutomationRunner.class.getName());

    private SshDevice sshDevice;
    private List<SshRunner> runners = new ArrayList<>();
    private Map<String, Map<String, String>> runnerConfigs = new HashMap<>();
    private Map<String, Boolean> executionResults = new HashMap<>();
    private boolean stopOnFailure = true;

    /**
     * Default constructor
     */
    public SshSeriesAutomationRunner() {
    }

    /**
     * Constructor with SSH device
     */
    public SshSeriesAutomationRunner(SshDevice device) {
        this.sshDevice = device;
    }

    /**
     * Set the SSH device to use
     */
    public SshSeriesAutomationRunner setSshDevice(SshDevice device) {
        this.sshDevice = device;
        return this;
    }

    /**
     * Add a runner to the execution list
     */
    public SshSeriesAutomationRunner addRunner(SshRunner runner) {
        this.runners.add(runner);
        return this;
    }

    /**
     * Add a runner with specific configuration
     */
    public SshSeriesAutomationRunner addRunner(SshRunner runner, Map<String, String> config) {
        this.runners.add(runner);
        if (config != null) {
            this.runnerConfigs.put(runner.getName(), config);
        }
        return this;
    }

    /**
     * Set runners list
     */
    public SshSeriesAutomationRunner setRunners(List<SshRunner> runners) {
        this.runners = new ArrayList<>(runners);
        return this;
    }

    /**
     * Set whether to stop execution on first failure
     */
    public SshSeriesAutomationRunner setStopOnFailure(boolean stop) {
        this.stopOnFailure = stop;
        return this;
    }

    /**
     * Set configuration for a specific runner
     */
    public SshSeriesAutomationRunner setRunnerConfig(String runnerName, Map<String, String> config) {
        this.runnerConfigs.put(runnerName, config);
        return this;
    }

    /**
     * Execute all runners sequentially
     * @return true if all runners executed successfully, false if any failed
     */
    public boolean execute() throws Exception {
        if (sshDevice == null) {
            throw new IllegalStateException("SSH device not set");
        }

        if (!sshDevice.isConnected()) {
            throw new IllegalStateException("SSH device not connected");
        }

        if (runners.isEmpty()) {
            logger.warning("No runners configured");
            return true;
        }

        logger.info("Starting series automation with " + runners.size() + " runners");
        boolean allSuccess = true;

        for (int i = 0; i < runners.size(); i++) {
            SshRunner runner = runners.get(i);
            String runnerName = runner.getName();

            logger.info("========================================");
            logger.info("Runner " + (i + 1) + "/" + runners.size() + ": " + runnerName);
            logger.info("Description: " + runner.getDescription());
            logger.info("========================================");

            try {
                // Get configuration for this runner
                Map<String, String> config = runnerConfigs.getOrDefault(runnerName, new HashMap<>());

                // Check if should skip
                if (runner.shouldSkip(sshDevice, config)) {
                    logger.info("Skipping runner: " + runnerName + " (conditions not met)");
                    executionResults.put(runnerName, true); // Mark as success if skipped
                    continue;
                }

                // Execute the runner
                logger.info("Executing: " + runnerName);
                boolean success = runner.execute(sshDevice, config);

                executionResults.put(runnerName, success);

                if (success) {
                    logger.info("✓ " + runnerName + " completed successfully");

                    // Optionally verify
                    if (runner.verify(sshDevice, config)) {
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
     * Creates and manages the SSH connection internally
     */
    public boolean executeWithConnection(String host, int port, String username,
                                        String password) throws Exception {
        logger.info("Establishing SSH connection to " + host + ":" + port);

        if (sshDevice == null) {
            sshDevice = new SshDevice();
        }

        try {
            // Connect to SSH
            boolean connected = sshDevice.connect(host, port, username, password).get();
            if (!connected) {
                logger.severe("Failed to establish SSH connection");
                return false;
            }

            logger.info("SSH connection established successfully");

            // Execute all runners
            return execute();

        } finally {
            // Always disconnect when done
            if (sshDevice != null && sshDevice.isConnected()) {
                logger.info("Closing SSH connection");
                sshDevice.disconnect();
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