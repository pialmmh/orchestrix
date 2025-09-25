package com.telcobright.orchestrix.automation.runners;

import com.telcobright.orchestrix.automation.TerminalRunner;
import com.telcobright.orchestrix.automation.devices.server.platform.SnapInstallerWithAddToPathAutomation;
import com.telcobright.orchestrix.device.TerminalDevice;
import com.telcobright.orchestrix.device.SshDevice;
import java.util.Map;
import java.util.HashMap;
import java.util.logging.Logger;

/**
 * TerminalRunner implementation for Snap package manager installation
 */
public class SnapInstallerTerminalRunner implements TerminalRunner {

    private static final Logger logger = Logger.getLogger(SnapInstallerTerminalRunner.class.getName());

    private Map<String, String> lastStatus = new HashMap<>();

    @Override
    public String getName() {
        return "SnapInstaller";
    }

    @Override
    public String getDescription() {
        return "Install Snap package manager and add to PATH on Linux systems";
    }

    @Override
    public boolean execute(TerminalDevice terminalDevice, Map<String, String> config) throws Exception {
        logger.info("Installing Snap package manager...");

        // Extract configuration
        boolean useSudo = Boolean.parseBoolean(config.getOrDefault("snap.use.sudo", "true"));
        boolean addToPath = Boolean.parseBoolean(config.getOrDefault("snap.add.to.path", "true"));
        boolean updateSystemPath = Boolean.parseBoolean(config.getOrDefault("snap.update.system.path", "true"));

        // Build automation config
        // For now, this automation only supports SSH devices
        if (!(terminalDevice instanceof SshDevice)) {
            logger.severe("SnapInstallerWithAddToPathAutomation currently only supports SSH devices");
            return false;
        }
        SshDevice sshDevice = (SshDevice) terminalDevice;
        SnapInstallerWithAddToPathAutomation automation =
            new SnapInstallerWithAddToPathAutomation.Config()
                .useSudo(useSudo)
                .addToPath(addToPath)
                .updateSystemPath(updateSystemPath)
                .build(sshDevice);

        // Check current status before installation
        Map<String, String> beforeStatus = automation.getStatus();
        lastStatus.putAll(beforeStatus);

        // Perform installation
        boolean success = automation.install();

        // Get status after installation
        Map<String, String> afterStatus = automation.getStatus();
        lastStatus.putAll(afterStatus);
        lastStatus.put("installation.success", String.valueOf(success));

        return success;
    }

    @Override
    public boolean verify(TerminalDevice terminalDevice, Map<String, String> config) throws Exception {
        logger.info("Verifying Snap installation...");

        // Build automation config
        // For now, this automation only supports SSH devices
        if (!(terminalDevice instanceof SshDevice)) {
            logger.severe("SnapInstallerWithAddToPathAutomation currently only supports SSH devices");
            return false;
        }
        SshDevice sshDevice = (SshDevice) terminalDevice;
        SnapInstallerWithAddToPathAutomation automation =
            new SnapInstallerWithAddToPathAutomation.Config()
                .build(sshDevice);

        // Verify installation
        boolean verified = automation.verify();

        // Get detailed status
        Map<String, String> status = automation.getStatus();
        lastStatus.putAll(status);
        lastStatus.put("verification.success", String.valueOf(verified));

        // Additional verification checks
        String snapVersion = terminalDevice.sendAndReceive("snap version 2>/dev/null || /snap/bin/snap version 2>/dev/null").get();
        if (snapVersion != null && !snapVersion.isEmpty()) {
            lastStatus.put("snap.version", snapVersion.split("\\n")[0].trim());
        }

        return verified;
    }

    @Override
    public boolean shouldSkip(TerminalDevice terminalDevice, Map<String, String> config) throws Exception {
        // Check if forced execution
        boolean force = Boolean.parseBoolean(config.getOrDefault("force.execution", "false"));
        if (force) {
            logger.info("Force execution enabled, not skipping");
            return false;
        }

        // Check current installation status
        String snapCheck = terminalDevice.sendAndReceive("which snap 2>/dev/null || test -f /snap/bin/snap && echo found").get();
        boolean snapInstalled = (snapCheck != null && !snapCheck.isEmpty());

        // Check if snap is in PATH
        String pathCheck = terminalDevice.sendAndReceive("which snap 2>/dev/null").get();
        boolean snapInPath = (pathCheck != null && !pathCheck.isEmpty());

        if (snapInstalled && snapInPath) {
            logger.info("Snap is already installed and in PATH");
            lastStatus.put("skip.reason", "Already installed and in PATH");
            lastStatus.put("snap.installed", "true");
            lastStatus.put("snap.in.path", "true");
            return true;
        }

        return false;
    }

    @Override
    public Map<String, String> getStatus() {
        return new HashMap<>(lastStatus);
    }
}