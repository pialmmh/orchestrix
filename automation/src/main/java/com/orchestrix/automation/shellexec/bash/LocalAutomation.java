package com.orchestrix.automation.shellexec.bash;

import com.orchestrix.automation.core.AutomationScript;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Base class for local bash automation scripts
 * Similar to SshAutomation but executes locally
 */
public abstract class LocalAutomation implements AutomationScript {

    protected static final Logger logger = LoggerFactory.getLogger(LocalAutomation.class);

    protected final BashExecutor executor;
    protected final List<String> executedCommands;
    protected boolean dryRun;

    public LocalAutomation() {
        this.executor = new BashExecutor();
        this.executedCommands = new ArrayList<>();
        this.dryRun = false;
    }

    /**
     * Execute a command locally
     */
    protected String executeCommand(String command) {
        return executeCommand(command, true);
    }

    /**
     * Execute a command locally with option to check exit code
     */
    protected String executeCommand(String command, boolean checkExitCode) {
        logger.info("Executing locally: {}", command);

        if (dryRun) {
            logger.info("[DRY RUN] Would execute: {}", command);
            return "";
        }

        BashExecutor.CommandResult result = executor.execute(command);
        executedCommands.add(command);

        if (!result.isSuccess() && checkExitCode) {
            throw new RuntimeException(String.format(
                "Command failed with exit code %d: %s\nError: %s",
                result.getExitCode(), command, result.getStderr()));
        }

        return result.getStdout();
    }

    /**
     * Execute a command with sudo
     */
    protected String executeSudo(String command) {
        return executeCommand("sudo " + command);
    }

    /**
     * Execute command and return full result
     */
    protected BashExecutor.CommandResult executeWithResult(String command) {
        logger.info("Executing locally: {}", command);

        if (dryRun) {
            logger.info("[DRY RUN] Would execute: {}", command);
            return new BashExecutor.CommandResult(0, "", "", 0);
        }

        BashExecutor.CommandResult result = executor.execute(command);
        executedCommands.add(command);
        return result;
    }

    /**
     * Execute command with retry on failure
     */
    protected String executeWithRetry(String command, int maxRetries) {
        BashExecutor.CommandResult result = executor.executeWithRetry(command, maxRetries, 2000);

        if (!result.isSuccess()) {
            throw new RuntimeException(String.format(
                "Command failed after %d retries: %s", maxRetries, command));
        }

        return result.getStdout();
    }

    /**
     * Check if a command exists locally
     */
    protected boolean commandExists(String command) {
        return executor.commandExists(command);
    }

    /**
     * Check if a package is installed (Debian/Ubuntu)
     */
    protected boolean isPackageInstalled(String packageName) {
        BashExecutor.CommandResult result = executeWithResult(
            "dpkg -l | grep -w " + packageName);
        return result.isSuccess() && result.getStdout().contains(packageName);
    }

    /**
     * Install a package if not already installed
     */
    protected void ensurePackageInstalled(String packageName) {
        if (!isPackageInstalled(packageName)) {
            logger.info("Installing package: {}", packageName);
            executeSudo("apt-get update");
            executeSudo("DEBIAN_FRONTEND=noninteractive apt-get install -y " + packageName);
        } else {
            logger.info("Package {} is already installed", packageName);
        }
    }

    /**
     * Check if a file exists
     */
    protected boolean fileExists(String path) {
        BashExecutor.CommandResult result = executeWithResult("test -f " + path);
        return result.isSuccess();
    }

    /**
     * Check if a directory exists
     */
    protected boolean directoryExists(String path) {
        BashExecutor.CommandResult result = executeWithResult("test -d " + path);
        return result.isSuccess();
    }

    /**
     * Create directory if it doesn't exist
     */
    protected void ensureDirectoryExists(String path) {
        if (!directoryExists(path)) {
            logger.info("Creating directory: {}", path);
            executeCommand("mkdir -p " + path);
        }
    }

    /**
     * Copy file with backup
     */
    protected void copyFileWithBackup(String source, String destination) {
        if (fileExists(destination)) {
            String backup = destination + ".bak." + System.currentTimeMillis();
            logger.info("Backing up {} to {}", destination, backup);
            executeCommand("cp " + destination + " " + backup);
        }
        executeCommand("cp " + source + " " + destination);
    }

    /**
     * Enable dry run mode
     */
    public void enableDryRun() {
        this.dryRun = true;
        executor.setDryRun(true);
    }

    /**
     * Disable dry run mode
     */
    public void disableDryRun() {
        this.dryRun = false;
        executor.setDryRun(false);
    }

    /**
     * Enable output streaming
     */
    public void enableOutputStreaming() {
        executor.setStreamOutput(true);
    }

    /**
     * Set working directory
     */
    public void setWorkingDirectory(String path) {
        executor.setWorkingDirectory(path);
    }

    @Override
    public boolean validate() {
        try {
            // Test basic command execution
            String result = executeCommand("echo 'Local execution test'", false);
            if (!result.contains("Local execution test")) {
                logger.error("Failed to execute local commands");
                return false;
            }

            // Check if we have sudo access (if needed)
            if (requiresSudo() && !executor.hasSudoPrivileges()) {
                logger.error("Sudo access required but not available");
                return false;
            }

            return true;
        } catch (Exception e) {
            logger.error("Validation failed", e);
            return false;
        }
    }

    /**
     * Override to indicate if this automation requires sudo
     */
    protected boolean requiresSudo() {
        return false;
    }

    /**
     * Get list of executed commands
     */
    public List<String> getExecutedCommands() {
        return new ArrayList<>(executedCommands);
    }
}