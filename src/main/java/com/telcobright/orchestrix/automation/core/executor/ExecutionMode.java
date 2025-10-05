package com.telcobright.orchestrix.automation.core.executor;

/**
 * Execution mode for multi-target operations
 */
public enum ExecutionMode {
    /**
     * Execute tasks one after another
     */
    SEQUENTIAL,

    /**
     * Execute all tasks concurrently
     */
    PARALLEL
}
