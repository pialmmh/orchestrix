package com.telcobright.orchestrix.automation.devices.server.linux.base;

import com.telcobright.orchestrix.device.SshDevice;
import java.util.Map;

/**
 * Base interface for all Linux automation tasks
 */
public interface LinuxAutomation {

    /**
     * Execute the automation task
     * @param device The SSH device to execute on
     * @return true if successful, false otherwise
     * @throws Exception on execution errors
     */
    boolean execute(SshDevice device) throws Exception;

    /**
     * Verify the automation was successful
     * @param device The SSH device to verify on
     * @return true if verification passes, false otherwise
     * @throws Exception on verification errors
     */
    boolean verify(SshDevice device) throws Exception;

    /**
     * Get the current status of this automation
     * @param device The SSH device to check status on
     * @return Map of status key-value pairs
     */
    Map<String, String> getStatus(SshDevice device);

    /**
     * Get the specific distribution this automation supports
     * @return The supported Linux distribution
     */
    LinuxDistribution getSupportedDistribution();

    /**
     * Check if this automation is compatible with a given distribution
     * @param distribution The distribution to check
     * @return true if compatible, false otherwise
     */
    boolean isCompatible(LinuxDistribution distribution);

    /**
     * Get the name of this automation
     * @return The automation name
     */
    String getName();

    /**
     * Get a description of what this automation does
     * @return The automation description
     */
    String getDescription();
}