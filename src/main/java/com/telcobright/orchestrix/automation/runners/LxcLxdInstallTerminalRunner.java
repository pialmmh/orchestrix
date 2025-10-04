package com.telcobright.orchestrix.automation.runners;

import com.telcobright.orchestrix.automation.TerminalRunner;
import com.telcobright.orchestrix.automation.devices.server.platform.LxcLxdInstallDebian12Automation;
import com.telcobright.orchestrix.automation.api.device.TerminalDevice;
import com.telcobright.orchestrix.automation.core.device.SshDevice;
import java.util.Map;
import java.util.HashMap;
import java.util.logging.Logger;

/**
 * TerminalRunner implementation for LXC/LXD installation on Debian systems
 */
public class LxcLxdInstallTerminalRunner implements TerminalRunner {

    private static final Logger logger = Logger.getLogger(LxcLxdInstallTerminalRunner.class.getName());

    private Map<String, String> lastStatus = new HashMap<>();

    @Override
    public String getName() {
        return "LxcLxdInstall";
    }

    @Override
    public String getDescription() {
        return "Install and configure LXC/LXD on Debian 12 systems";
    }

    @Override
    public boolean execute(TerminalDevice terminalDevice, Map<String, String> config) throws Exception {
        logger.info("Installing LXC/LXD on Debian system...");

        // Extract configuration
        boolean useSudo = Boolean.parseBoolean(config.getOrDefault("install.use.sudo", "true"));
        boolean installLxc = Boolean.parseBoolean(config.getOrDefault("install.lxc", "true"));
        boolean installLxd = Boolean.parseBoolean(config.getOrDefault("install.lxd", "true"));
        boolean useSnap = Boolean.parseBoolean(config.getOrDefault("install.use.snap", "false"));

        // Build automation config
        // For now, this automation only supports SSH devices
        if (!(terminalDevice instanceof SshDevice)) {
            logger.severe("LxcLxdInstallDebian12Automation currently only supports SSH devices");
            return false;
        }
        SshDevice sshDevice = (SshDevice) terminalDevice;
        LxcLxdInstallDebian12Automation automation =
            new LxcLxdInstallDebian12Automation.Config()
                .useSudo(useSudo)
                .installLxc(installLxc)
                .installLxd(installLxd)
                .installSnapd(useSnap)
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
        logger.info("Verifying LXC/LXD installation...");

        // Extract configuration
        boolean useSudo = Boolean.parseBoolean(config.getOrDefault("install.use.sudo", "true"));
        boolean checkLxc = Boolean.parseBoolean(config.getOrDefault("install.lxc", "true"));
        boolean checkLxd = Boolean.parseBoolean(config.getOrDefault("install.lxd", "true"));
        boolean useSnap = Boolean.parseBoolean(config.getOrDefault("install.use.snap", "false"));

        // Build automation config
        // For now, this automation only supports SSH devices
        if (!(terminalDevice instanceof SshDevice)) {
            logger.severe("LxcLxdInstallDebian12Automation currently only supports SSH devices");
            return false;
        }
        SshDevice sshDevice = (SshDevice) terminalDevice;
        LxcLxdInstallDebian12Automation automation =
            new LxcLxdInstallDebian12Automation.Config()
                .useSudo(useSudo)
                .installLxc(checkLxc)
                .installLxd(checkLxd)
                .installSnapd(useSnap)
                .build(sshDevice);

        // Verify installation
        boolean verified = automation.verify();

        // Get detailed status
        Map<String, String> status = automation.getStatus();
        lastStatus.putAll(status);
        lastStatus.put("verification.success", String.valueOf(verified));

        // Additional verification checks
        if (checkLxc) {
            String lxcVersion = terminalDevice.sendAndReceive("lxc-ls --version 2>/dev/null").get();
            if (lxcVersion != null && !lxcVersion.isEmpty()) {
                lastStatus.put("lxc.version", lxcVersion.trim());
            }
        }

        if (checkLxd) {
            String lxdCmd = useSnap ? "/snap/bin/lxc" : "lxc";
            String lxdVersion = terminalDevice.sendAndReceive(lxdCmd + " version 2>/dev/null").get();
            if (lxdVersion != null && !lxdVersion.isEmpty()) {
                lastStatus.put("lxd.version", lxdVersion.trim());
            }
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

        // Check what should be installed
        boolean installLxc = Boolean.parseBoolean(config.getOrDefault("install.lxc", "true"));
        boolean installLxd = Boolean.parseBoolean(config.getOrDefault("install.lxd", "true"));

        // Check current installation status
        boolean lxcInstalled = false;
        boolean lxdInstalled = false;

        if (installLxc) {
            String lxcCheck = terminalDevice.sendAndReceive("which lxc-ls 2>/dev/null").get();
            lxcInstalled = (lxcCheck != null && !lxcCheck.isEmpty());
        }

        if (installLxd) {
            // Check both snap and apt installations
            String lxdCheck = terminalDevice.sendAndReceive("which lxc 2>/dev/null || which lxd 2>/dev/null").get();
            lxdInstalled = (lxdCheck != null && !lxdCheck.isEmpty());

            // Also check snap installation
            if (!lxdInstalled) {
                String snapCheck = terminalDevice.sendAndReceive("test -f /snap/bin/lxc && echo found").get();
                lxdInstalled = "found".equals(snapCheck);
            }
        }

        // Skip if everything requested is already installed
        boolean skipLxc = installLxc && lxcInstalled;
        boolean skipLxd = installLxd && lxdInstalled;

        if ((!installLxc || skipLxc) && (!installLxd || skipLxd)) {
            logger.info("All requested components are already installed");
            lastStatus.put("skip.reason", "Already installed");
            lastStatus.put("lxc.installed", String.valueOf(lxcInstalled));
            lastStatus.put("lxd.installed", String.valueOf(lxdInstalled));
            return true;
        }

        return false;
    }

    @Override
    public Map<String, String> getStatus() {
        return new HashMap<>(lastStatus);
    }
}