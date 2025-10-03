package com.telcobright.orchestrix.automation.api.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Result of an automation operation
 * Represents the outcome of complex automation tasks that may involve multiple commands
 */
public class AutomationOperationResult {
    private final String operationName;
    private final boolean success;
    private final String message;
    private final String errorMessage;
    private final LocalDateTime timestamp;
    private final long durationMs;
    private final List<CommandResult> commandResults;

    private AutomationOperationResult(Builder builder) {
        this.operationName = builder.operationName;
        this.success = builder.success;
        this.message = builder.message;
        this.errorMessage = builder.errorMessage;
        this.timestamp = LocalDateTime.now();
        this.durationMs = builder.durationMs;
        this.commandResults = builder.commandResults;
    }

    // Getters
    public String getOperationName() { return operationName; }
    public boolean isSuccess() { return success; }
    public String getMessage() { return message; }
    public String getErrorMessage() { return errorMessage; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public long getDurationMs() { return durationMs; }
    public List<CommandResult> getCommandResults() { return new ArrayList<>(commandResults); }

    // Helper methods
    public boolean hasErrors() {
        return !success || (errorMessage != null && !errorMessage.isEmpty());
    }

    public int getCommandCount() {
        return commandResults.size();
    }

    public int getFailedCommandCount() {
        return (int) commandResults.stream().filter(r -> !r.isSuccess()).count();
    }

    @Override
    public String toString() {
        return String.format("AutomationOperationResult[operation='%s', success=%s, duration=%dms, commands=%d]",
            operationName, success, durationMs, commandResults.size());
    }

    // Static factory methods for common cases
    public static AutomationOperationResult success(String operationName, String message) {
        return new Builder(operationName)
            .success(true)
            .message(message)
            .build();
    }

    public static AutomationOperationResult failure(String operationName, String errorMessage) {
        return new Builder(operationName)
            .success(false)
            .errorMessage(errorMessage)
            .build();
    }

    // Builder pattern
    public static class Builder {
        private final String operationName;
        private boolean success = true;
        private String message = "";
        private String errorMessage = "";
        private long durationMs = 0;
        private List<CommandResult> commandResults = new ArrayList<>();

        public Builder(String operationName) {
            this.operationName = operationName;
        }

        public Builder success(boolean success) {
            this.success = success;
            return this;
        }

        public Builder message(String message) {
            this.message = message;
            return this;
        }

        public Builder errorMessage(String errorMessage) {
            this.errorMessage = errorMessage;
            return this;
        }

        public Builder durationMs(long durationMs) {
            this.durationMs = durationMs;
            return this;
        }

        public Builder addCommandResult(CommandResult result) {
            this.commandResults.add(result);
            return this;
        }

        public Builder commandResults(List<CommandResult> results) {
            this.commandResults = new ArrayList<>(results);
            return this;
        }

        public AutomationOperationResult build() {
            return new AutomationOperationResult(this);
        }
    }
}
