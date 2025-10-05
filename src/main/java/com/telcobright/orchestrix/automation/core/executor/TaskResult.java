package com.telcobright.orchestrix.automation.core.executor;

/**
 * Result of a task execution
 */
public class TaskResult<T> {
    private final String targetName;
    private final boolean success;
    private final T result;
    private final Exception error;
    private final long executionTimeMs;

    public TaskResult(String targetName, boolean success, T result, Exception error, long executionTimeMs) {
        this.targetName = targetName;
        this.success = success;
        this.result = result;
        this.error = error;
        this.executionTimeMs = executionTimeMs;
    }

    public static <T> TaskResult<T> success(String targetName, T result, long executionTimeMs) {
        return new TaskResult<>(targetName, true, result, null, executionTimeMs);
    }

    public static <T> TaskResult<T> failure(String targetName, Exception error, long executionTimeMs) {
        return new TaskResult<>(targetName, false, null, error, executionTimeMs);
    }

    // Getters
    public String getTargetName() {
        return targetName;
    }

    public boolean isSuccess() {
        return success;
    }

    public T getResult() {
        return result;
    }

    public Exception getError() {
        return error;
    }

    public long getExecutionTimeMs() {
        return executionTimeMs;
    }

    @Override
    public String toString() {
        if (success) {
            return String.format("TaskResult{target='%s', success=true, time=%dms}",
                targetName, executionTimeMs);
        } else {
            return String.format("TaskResult{target='%s', success=false, error=%s, time=%dms}",
                targetName, error.getMessage(), executionTimeMs);
        }
    }
}
