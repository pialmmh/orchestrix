package com.telcobright.orchestrix.automation.runners;

import com.telcobright.orchestrix.automation.TerminalRunner;
import com.telcobright.orchestrix.automation.devices.server.platform.LinuxIpForwardEnablerAutomation;
import com.telcobright.orchestrix.automation.devices.server.platform.LinuxIpForwardEnablerAutomation.LinuxDistribution;
import com.telcobright.orchestrix.automation.api.device.TerminalDevice;
import com.telcobright.orchestrix.automation.core.device.SshDevice;
import java.util.Map;
import java.util.HashMap;
import java.util.logging.Logger;

/**
 * TerminalRunner implementation for Linux IP forwarding configuration
 * Supports multiple Linux distributions
 */
public class LinuxIpForwardEnablerTerminalRunner implements TerminalRunner {

    private static final Logger logger = Logger.getLogger(LinuxIpForwardEnablerTerminalRunner.class.getName());

    private Map<String, String> lastStatus = new HashMap<>();

    @Override
    public String getName() {
        return "LinuxIpForwardEnabler";
    }

    @Override
    public String getDescription() {
        return "Enable IP forwarding on Linux systems for packet routing (supports multiple distributions)";
    }

    @Override
    public boolean execute(TerminalDevice terminalDevice, Map<String, String> config) throws Exception {
        logger.info("Configuring IP forwarding on Linux system...");

        // Extract configuration
        boolean useSudo = Boolean.parseBoolean(config.getOrDefault("ipforward.use.sudo", "true"));
        boolean enableIpv4 = Boolean.parseBoolean(config.getOrDefault("ipforward.enable.ipv4", "true"));
        boolean enableIpv6 = Boolean.parseBoolean(config.getOrDefault("ipforward.enable.ipv6", "false"));
        boolean makePersistent = Boolean.parseBoolean(config.getOrDefault("ipforward.make.persistent", "true"));

        // Get distribution if specified
        String distroStr = config.getOrDefault("ipforward.distribution", "AUTO_DETECT");
        LinuxDistribution distribution = LinuxDistribution.AUTO_DETECT;
        try {
            distribution = LinuxDistribution.valueOf(distroStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            logger.warning("Unknown distribution: " + distroStr + ", using auto-detection");
        }

        // Build automation config
        // For now, this automation only supports SSH devices
        if (!(terminalDevice instanceof SshDevice)) {
            logger.severe("LinuxIpForwardEnablerAutomation currently only supports SSH devices");
            return false;
        }
        SshDevice sshDevice = (SshDevice) terminalDevice;
        LinuxIpForwardEnablerAutomation automation =
            new LinuxIpForwardEnablerAutomation.Config()
                .useSudo(useSudo)
                .enableIpv4(enableIpv4)
                .enableIpv6(enableIpv6)
                .makePersistent(makePersistent)
                .distribution(distribution)
                .build(sshDevice);

        // Get status before configuration
        Map<String, String> beforeStatus = automation.getStatus();
        lastStatus.putAll(beforeStatus);
        lastStatus.put("before.ipv4.forwarding", beforeStatus.get("ipv4.forwarding"));
        lastStatus.put("before.ipv6.forwarding", beforeStatus.get("ipv6.forwarding"));

        // Enable IP forwarding
        boolean success = automation.enable();

        // Get status after configuration
        Map<String, String> afterStatus = automation.getStatus();
        lastStatus.putAll(afterStatus);
        lastStatus.put("configuration.success", String.valueOf(success));

        return success;
    }

    @Override
    public boolean verify(TerminalDevice terminalDevice, Map<String, String> config) throws Exception {
        logger.info("Verifying IP forwarding configuration...");

        // Extract configuration
        boolean useSudo = Boolean.parseBoolean(config.getOrDefault("ipforward.use.sudo", "true"));
        boolean enableIpv4 = Boolean.parseBoolean(config.getOrDefault("ipforward.enable.ipv4", "true"));
        boolean enableIpv6 = Boolean.parseBoolean(config.getOrDefault("ipforward.enable.ipv6", "false"));
        boolean makePersistent = Boolean.parseBoolean(config.getOrDefault("ipforward.make.persistent", "true"));

        // Get distribution if specified
        String distroStr = config.getOrDefault("ipforward.distribution", "AUTO_DETECT");
        LinuxDistribution distribution = LinuxDistribution.AUTO_DETECT;
        try {
            distribution = LinuxDistribution.valueOf(distroStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            logger.warning("Unknown distribution: " + distroStr + ", using auto-detection");
        }

        // Build automation config
        // For now, this automation only supports SSH devices
        if (!(terminalDevice instanceof SshDevice)) {
            logger.severe("LinuxIpForwardEnablerAutomation currently only supports SSH devices");
            return false;
        }
        SshDevice sshDevice = (SshDevice) terminalDevice;
        LinuxIpForwardEnablerAutomation automation =
            new LinuxIpForwardEnablerAutomation.Config()
                .useSudo(useSudo)
                .enableIpv4(enableIpv4)
                .enableIpv6(enableIpv6)
                .makePersistent(makePersistent)
                .distribution(distribution)
                .build(sshDevice);

        // Verify configuration
        boolean verified = automation.verify();

        // Get detailed status
        Map<String, String> status = automation.getStatus();
        lastStatus.putAll(status);
        lastStatus.put("verification.success", String.valueOf(verified));

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

        // Check what should be enabled
        boolean enableIpv4 = Boolean.parseBoolean(config.getOrDefault("ipforward.enable.ipv4", "true"));
        boolean enableIpv6 = Boolean.parseBoolean(config.getOrDefault("ipforward.enable.ipv6", "false"));
        boolean useSudo = Boolean.parseBoolean(config.getOrDefault("ipforward.use.sudo", "true"));
        String sudoCmd = useSudo ? "sudo " : "";

        // Check current status
        boolean ipv4AlreadyEnabled = false;
        boolean ipv6AlreadyEnabled = false;

        if (enableIpv4) {
            String ipv4Status = terminalDevice.sendAndReceive(sudoCmd + "sysctl net.ipv4.ip_forward 2>/dev/null").get();
            ipv4AlreadyEnabled = (ipv4Status != null && ipv4Status.contains("= 1"));
        }

        if (enableIpv6) {
            String ipv6Status = terminalDevice.sendAndReceive(sudoCmd + "sysctl net.ipv6.conf.all.forwarding 2>/dev/null").get();
            ipv6AlreadyEnabled = (ipv6Status != null && ipv6Status.contains("= 1"));
        }

        // Skip if all requested forwarding is already enabled
        boolean skipIpv4 = enableIpv4 && ipv4AlreadyEnabled;
        boolean skipIpv6 = enableIpv6 && ipv6AlreadyEnabled;

        if ((!enableIpv4 || skipIpv4) && (!enableIpv6 || skipIpv6)) {
            logger.info("All requested IP forwarding is already enabled");
            lastStatus.put("skip.reason", "Already configured");
            lastStatus.put("ipv4.enabled", String.valueOf(ipv4AlreadyEnabled));
            lastStatus.put("ipv6.enabled", String.valueOf(ipv6AlreadyEnabled));
            return true;
        }

        return false;
    }

    @Override
    public Map<String, String> getStatus() {
        return new HashMap<>(lastStatus);
    }
}