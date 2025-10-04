package com.telcobright.orchestrix.automation;

import com.telcobright.orchestrix.automation.api.device.TerminalDevice;
import java.util.Map;

/**
 * Interface for terminal-based automation tasks that can be run sequentially
 * over a single terminal connection by TerminalSeriesAutomationRunner.
 * Works with any TerminalDevice implementation (SSH, Telnet, LocalShell)
 */
public interface TerminalRunner {

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
     * Get supported terminal types for this runner
     * @return Array of supported terminal types, empty array means all types supported
     */
    default TerminalDevice.DeviceType[] getSupportedTerminalTypes() {
        return new TerminalDevice.DeviceType[0]; // Empty = supports all
    }

    /**
     * Check if this runner supports the given terminal type
     * @param type Terminal device type
     * @return true if supported, false otherwise
     */
    default boolean supportsTerminalType(TerminalDevice.DeviceType type) {
        TerminalDevice.DeviceType[] supported = getSupportedTerminalTypes();
        if (supported.length == 0) {
            return true; // Supports all if not specified
        }
        for (TerminalDevice.DeviceType supportedType : supported) {
            if (supportedType == type) {
                return true;
            }
        }
        return false;
    }

    /**
     * Execute the automation task using the provided terminal connection
     * @param terminalDevice Already connected terminal device
     * @param config Configuration parameters for this runner
     * @return true if execution successful, false otherwise
     */
    boolean execute(TerminalDevice terminalDevice, Map<String, String> config) throws Exception;

    /**
     * Verify the results of the automation
     * @param terminalDevice Already connected terminal device
     * @param config Configuration parameters for this runner
     * @return true if verification successful, false otherwise
     */
    default boolean verify(TerminalDevice terminalDevice, Map<String, String> config) throws Exception {
        return true; // Default implementation - no verification
    }

    /**
     * Check if this runner should be skipped based on conditions
     * @param terminalDevice Already connected terminal device
     * @param config Configuration parameters for this runner
     * @return true if should skip, false if should run
     */
    default boolean shouldSkip(TerminalDevice terminalDevice, Map<String, String> config) throws Exception {
        // Check if terminal type is supported
        if (!supportsTerminalType(terminalDevice.getDeviceType())) {
            return true;
        }
        return false; // Default implementation - don't skip if type is supported
    }

    /**
     * Get status/result after execution
     * @return Status map with key-value pairs
     */
    default Map<String, String> getStatus() {
        return Map.of();
    }

    /**
     * Called before execution starts
     * Can be used for initialization or validation
     * @param terminalDevice Terminal device that will be used
     * @param config Configuration parameters
     * @return true if ready to execute, false to skip
     */
    default boolean beforeExecute(TerminalDevice terminalDevice, Map<String, String> config) throws Exception {
        return true;
    }

    /**
     * Called after execution completes (success or failure)
     * Can be used for cleanup
     * @param terminalDevice Terminal device that was used
     * @param config Configuration parameters
     * @param success Whether execution was successful
     */
    default void afterExecute(TerminalDevice terminalDevice, Map<String, String> config, boolean success) throws Exception {
        // Default implementation - no cleanup needed
    }
}