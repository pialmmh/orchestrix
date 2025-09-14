package com.orchestrix.automation.core;

import com.orchestrix.network.entity.SshDevice;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Base class for SSH-based automation scripts
 */
public abstract class SshAutomation implements AutomationScript {

    protected static final Logger logger = LoggerFactory.getLogger(SshAutomation.class);

    protected final SshDevice sshDevice;
    protected final List<String> executedCommands;
    protected boolean dryRun;

    public SshAutomation(SshDevice sshDevice) {
        this.sshDevice = sshDevice;
        this.executedCommands = new ArrayList<>();
        this.dryRun = false;
    }

    /**
     * Execute a command on the SSH device
     * @param command the command to execute
     * @return command output
     */
    protected String executeCommand(String command) {
        return executeCommand(command, true);
    }

    /**
     * Execute a command on the SSH device with option to check exit code
     * @param command the command to execute
     * @param checkExitCode whether to check if command succeeded
     * @return command output
     */
    protected String executeCommand(String command, boolean checkExitCode) {
        logger.info("Executing: {}", command);

        if (dryRun) {
            logger.info("[DRY RUN] Would execute: {}", command);
            return "";
        }

        try {
            String result = sshDevice.execute(command);
            executedCommands.add(command);

            if (result != null && result.contains("error") && checkExitCode) {
                logger.warn("Command may have failed. Output: {}", result);
            }

            return result != null ? result : "";
        } catch (Exception e) {
            logger.error("Failed to execute command: {}", command, e);
            if (checkExitCode) {
                throw new RuntimeException("Command execution failed: " + command, e);
            }
            return "";
        }
    }

    /**
     * Execute a command with sudo
     * @param command the command to execute with sudo
     * @return command output
     */
    protected String executeSudo(String command) {
        return executeCommand("sudo " + command);
    }

    /**
     * Check if a command exists on the remote system
     * @param command the command to check
     * @return true if command exists
     */
    protected boolean commandExists(String command) {
        String result = executeCommand("which " + command, false);
        return result != null && !result.isEmpty() && !result.contains("not found");
    }

    /**
     * Check if a package is installed
     * @param packageName the package to check
     * @return true if installed
     */
    protected boolean isPackageInstalled(String packageName) {
        String result = executeCommand("dpkg -l | grep -w " + packageName, false);
        return result != null && result.contains(packageName);
    }

    /**
     * Install a package if not already installed
     * @param packageName the package to install
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
     * Check if a network interface exists
     * @param interfaceName the interface to check
     * @return true if interface exists
     */
    protected boolean interfaceExists(String interfaceName) {
        String result = executeCommand("ip link show " + interfaceName + " 2>/dev/null", false);
        return result != null && !result.isEmpty() && !result.contains("does not exist");
    }

    /**
     * Get IP address of an interface
     * @param interfaceName the interface name
     * @return IP address or null if not found
     */
    protected String getInterfaceIp(String interfaceName) {
        String result = executeCommand("ip -4 addr show " + interfaceName + " | grep -oP '(?<=inet\\s)\\d+(\\.\\d+){3}'", false);
        return result != null && !result.isEmpty() ? result.trim() : null;
    }

    /**
     * Enable dry run mode (no actual commands executed)
     */
    public void enableDryRun() {
        this.dryRun = true;
    }

    /**
     * Disable dry run mode
     */
    public void disableDryRun() {
        this.dryRun = false;
    }

    @Override
    public boolean validate() {
        if (sshDevice == null) {
            logger.error("SSH device is not configured");
            return false;
        }

        try {
            // Test SSH connection
            String result = executeCommand("echo 'SSH connection test'", false);
            if (result == null || !result.contains("SSH connection test")) {
                logger.error("Failed to establish SSH connection");
                return false;
            }

            // Check if we have sudo access
            result = executeSudo("echo 'Sudo test'");
            if (result == null || !result.contains("Sudo test")) {
                logger.error("Sudo access not available");
                return false;
            }

            return true;
        } catch (Exception e) {
            logger.error("Validation failed", e);
            return false;
        }
    }

    /**
     * Get list of executed commands
     * @return list of commands that were executed
     */
    public List<String> getExecutedCommands() {
        return new ArrayList<>(executedCommands);
    }
}