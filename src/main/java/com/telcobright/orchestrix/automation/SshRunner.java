package com.telcobright.orchestrix.automation;

import com.telcobright.orchestrix.device.SshDevice;
import java.util.Map;

/**
 * Interface for SSH-based automation tasks that can be run sequentially
 * over a single SSH connection by SshSeriesAutomationRunner
 */
public interface SshRunner {

    /**
     * Get the name of this runner for identification
     * @return Runner name
     */
    String getName();

    /**
     * Get description of what this runner does
     * @return Runner description
     */
    String getDescription();

    /**
     * Execute the automation task using the provided SSH connection
     * @param sshDevice Already connected SSH device
     * @param config Configuration parameters for this runner
     * @return true if execution successful, false otherwise
     */
    boolean execute(SshDevice sshDevice, Map<String, String> config) throws Exception;

    /**
     * Verify the results of the automation
     * @param sshDevice Already connected SSH device
     * @param config Configuration parameters for this runner
     * @return true if verification successful, false otherwise
     */
    default boolean verify(SshDevice sshDevice, Map<String, String> config) throws Exception {
        return true; // Default implementation - no verification
    }

    /**
     * Check if this runner should be skipped based on conditions
     * @param sshDevice Already connected SSH device
     * @param config Configuration parameters for this runner
     * @return true if should skip, false if should run
     */
    default boolean shouldSkip(SshDevice sshDevice, Map<String, String> config) throws Exception {
        return false; // Default implementation - don't skip
    }

    /**
     * Get status/result after execution
     * @return Status map with key-value pairs
     */
    default Map<String, String> getStatus() {
        return Map.of();
    }
}