package com.telcobright.orchestrix.automation.core.executor;

/**
 * Functional interface for tasks that can be executed
 */
@FunctionalInterface
public interface Task<T> {
    /**
     * Execute the task
     *
     * @return Result of the task
     * @throws Exception if task fails
     */
    T execute() throws Exception;
}
