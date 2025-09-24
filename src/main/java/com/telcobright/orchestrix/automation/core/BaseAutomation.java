package com.telcobright.orchestrix.automation.core;

import com.telcobright.orchestrix.automation.model.AutomationConfig;
import com.telcobright.orchestrix.automation.model.CommandResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Base class for all automation tasks
 * Refactored to use the new model structure
 */
public abstract class BaseAutomation {

    protected final Logger logger = LoggerFactory.getLogger(getClass());
    protected final AutomationConfig config;
    protected final List<CommandResult> executionHistory = new ArrayList<>();
    protected CommandExecutor executor;

    public BaseAutomation(AutomationConfig config) {
        this.config = config;
        this.executor = createExecutor();
    }

    /**
     * Create appropriate executor based on execution mode
     */
    private CommandExecutor createExecutor() {
        switch (config.getExecutionMode()) {
            case "local":
                return new LocalCommandExecutor(config);
            case "ssh":
                return new SshCommandExecutor(config);
            case "lxc-exec":
                return new LxcCommandExecutor(config);
            case "docker-exec":
                return new DockerCommandExecutor(config);
            default:
                throw new IllegalArgumentException("Unknown execution mode: " + config.getExecutionMode());
        }
    }

    /**
     * Execute the automation task
     */
    public abstract boolean execute();

    /**
     * Validate prerequisites
     */
    public abstract boolean validate();

    /**
     * Get task name
     */
    public abstract String getName();

    /**
     * Get task description
     */
    public abstract String getDescription();

    /**
     * Execute command with default settings
     */
    protected CommandResult executeCommand(String command) {
        return executeCommand(command, true);
    }

    /**
     * Execute command with exit code check option
     */
    protected CommandResult executeCommand(String command, boolean checkExitCode) {
        logger.info("[{}] Executing: {}", config.getExecutionMode(), command);

        if (config.isDryRun()) {
            logger.info("[DRY RUN] Would execute: {}", command);
            return new CommandResult(command, 0, "", "", 0, config.getTargetHost());
        }

        CommandResult result = executor.execute(command);
        executionHistory.add(result);

        if (!result.isSuccess() && checkExitCode) {
            throw new AutomationException(String.format(
                "Command failed with exit code %d: %s\nError: %s",
                result.getExitCode(), command, result.getStderr()));
        }

        return result;
    }

    /**
     * Execute command with retry
     */
    protected CommandResult executeWithRetry(String command) {
        int attempts = 0;
        CommandResult result = null;

        while (attempts < config.getMaxRetries()) {
            attempts++;
            logger.info("Attempt {} of {}: {}", attempts, config.getMaxRetries(), command);

            result = executor.execute(command);
            executionHistory.add(result);

            if (result.isSuccess()) {
                return result;
            }

            if (attempts < config.getMaxRetries()) {
                logger.warn("Command failed, retrying in {}ms", config.getRetryDelayMs());
                try {
                    Thread.sleep(config.getRetryDelayMs());
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }

        throw new AutomationException(String.format(
            "Command failed after %d attempts: %s", config.getMaxRetries(), command));
    }

    /**
     * Execute script
     */
    protected CommandResult executeScript(String script) {
        return executor.executeScript(script);
    }

    /**
     * Check if command exists
     */
    protected boolean commandExists(String command) {
        CommandResult result = executeCommand("which " + command, false);
        return result.isSuccess() && result.hasOutput();
    }

    /**
     * Get execution history
     */
    public List<CommandResult> getExecutionHistory() {
        return new ArrayList<>(executionHistory);
    }

    /**
     * Command executor interface
     */
    protected interface CommandExecutor {
        CommandResult execute(String command);
        CommandResult executeScript(String script);
    }

    /**
     * Automation exception
     */
    public static class AutomationException extends RuntimeException {
        public AutomationException(String message) {
            super(message);
        }

        public AutomationException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}