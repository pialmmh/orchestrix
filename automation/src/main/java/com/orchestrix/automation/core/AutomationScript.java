package com.orchestrix.automation.core;

import com.orchestrix.network.entity.SshDevice;

/**
 * Base interface for all automation scripts
 */
public interface AutomationScript {

    /**
     * Execute the automation script
     * @return true if successful, false otherwise
     */
    boolean execute();

    /**
     * Validate prerequisites before execution
     * @return true if validation passes
     */
    boolean validate();

    /**
     * Get the name of this automation script
     * @return script name
     */
    String getName();

    /**
     * Get description of what this script does
     * @return script description
     */
    String getDescription();

    /**
     * Rollback changes if possible
     * @return true if rollback successful
     */
    default boolean rollback() {
        return false;
    }

    /**
     * Check if the script supports rollback
     * @return true if rollback is supported
     */
    default boolean supportsRollback() {
        return false;
    }
}