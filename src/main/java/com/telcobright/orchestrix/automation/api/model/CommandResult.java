package com.telcobright.orchestrix.automation.api.model;

import java.time.LocalDateTime;

/**
 * Result of command execution - used by all automation tasks
 */
public class CommandResult {
    private final String command;
    private final int exitCode;
    private final String stdout;
    private final String stderr;
    private final long executionTimeMs;
    private final LocalDateTime executedAt;
    private final String executedOn; // host or container name

    public CommandResult(String command, int exitCode, String stdout, String stderr,
                        long executionTimeMs, String executedOn) {
        this.command = command;
        this.exitCode = exitCode;
        this.stdout = stdout;
        this.stderr = stderr;
        this.executionTimeMs = executionTimeMs;
        this.executedAt = LocalDateTime.now();
        this.executedOn = executedOn;
    }

    // Helper methods
    public boolean isSuccess() {
        return exitCode == 0;
    }

    public boolean hasOutput() {
        return stdout != null && !stdout.trim().isEmpty();
    }

    public boolean hasError() {
        return stderr != null && !stderr.trim().isEmpty();
    }

    public String getCombinedOutput() {
        StringBuilder output = new StringBuilder();
        if (hasOutput()) {
            output.append(stdout);
        }
        if (hasError()) {
            if (output.length() > 0) {
                output.append("\n");
            }
            output.append(stderr);
        }
        return output.toString();
    }

    // Getters
    public String getCommand() { return command; }
    public int getExitCode() { return exitCode; }
    public String getStdout() { return stdout; }
    public String getStderr() { return stderr; }
    public long getExecutionTimeMs() { return executionTimeMs; }
    public LocalDateTime getExecutedAt() { return executedAt; }
    public String getExecutedOn() { return executedOn; }

    @Override
    public String toString() {
        return String.format("CommandResult[cmd='%s', exitCode=%d, time=%dms, host=%s]",
            command.length() > 50 ? command.substring(0, 50) + "..." : command,
            exitCode, executionTimeMs, executedOn);
    }
}