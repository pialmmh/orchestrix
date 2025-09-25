package com.telcobright.orchestrix.automation.devices.server.linux.base;

import com.telcobright.orchestrix.device.SshDevice;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Abstract base class for Linux automations providing common functionality
 */
public abstract class AbstractLinuxAutomation implements LinuxAutomation {

    protected final Logger logger = Logger.getLogger(getClass().getName());
    protected final boolean useSudo;
    protected final LinuxDistribution supportedDistribution;

    protected AbstractLinuxAutomation(LinuxDistribution distribution, boolean useSudo) {
        this.supportedDistribution = distribution;
        this.useSudo = useSudo;
    }

    @Override
    public LinuxDistribution getSupportedDistribution() {
        return supportedDistribution;
    }

    @Override
    public boolean isCompatible(LinuxDistribution distribution) {
        // Check exact match first
        if (supportedDistribution.getName().equals(distribution.getName()) &&
            supportedDistribution.getVersion().equals(distribution.getVersion())) {
            return true;
        }

        // Check family compatibility for default implementations
        if (this instanceof DefaultImplementation) {
            return supportedDistribution.getFamily() == distribution.getFamily();
        }

        return false;
    }

    /**
     * Execute a command with optional sudo
     */
    protected String executeCommand(SshDevice device, String command) throws Exception {
        String fullCommand = useSudo ? "sudo " + command : command;
        return device.sendAndReceive(fullCommand).get();
    }

    /**
     * Check if a command exists
     */
    protected boolean commandExists(SshDevice device, String command) {
        try {
            String result = executeCommand(device, "which " + command + " 2>/dev/null");
            return result != null && !result.isEmpty();
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Check if a package is installed
     */
    protected abstract boolean isPackageInstalled(SshDevice device, String packageName) throws Exception;

    /**
     * Get the package manager command
     */
    protected abstract String getPackageManagerCommand();

    @Override
    public Map<String, String> getStatus(SshDevice device) {
        Map<String, String> status = new HashMap<>();
        status.put("automation", getName());
        status.put("distribution", supportedDistribution.toString());
        return status;
    }

    /**
     * Marker interface for default implementations
     */
    public interface DefaultImplementation {
    }
}