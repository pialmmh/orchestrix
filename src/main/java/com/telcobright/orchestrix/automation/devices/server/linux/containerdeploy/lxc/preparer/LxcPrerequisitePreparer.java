package com.telcobright.orchestrix.automation.devices.server.linux.containerdeploy.lxc.preparer;

import com.telcobright.orchestrix.automation.devices.server.linux.lxdinstall.LxdInstallerAutomation;
import com.telcobright.orchestrix.automation.devices.server.linux.lxdinstall.LxdInstallerAutomationFactory;
import com.telcobright.orchestrix.automation.devices.server.linux.snapinstall.SnapInstallAutomation;
import com.telcobright.orchestrix.automation.devices.server.linux.snapinstall.SnapInstallAutomationFactory;
import com.telcobright.orchestrix.automation.devices.server.linux.ipforward.IpForwardAutomation;
import com.telcobright.orchestrix.automation.devices.server.linux.ipforward.IpForwardAutomationFactory;
import com.telcobright.orchestrix.automation.devices.server.linux.lxdbridge.LxdBridgeConfigureAutomation;
import com.telcobright.orchestrix.automation.devices.server.linux.lxdbridge.LxdBridgeConfigureAutomationFactory;
import com.telcobright.orchestrix.automation.devices.server.linux.firewall.nat.masquerade.MasqueradeAutomation;
import com.telcobright.orchestrix.automation.devices.server.linux.firewall.nat.masquerade.MasqueradeAutomationFactory;
import com.telcobright.orchestrix.automation.devices.server.linux.firewall.nat.masquerade.MasqueradeRule;
import com.telcobright.orchestrix.automation.devices.server.linux.lxd.LxdBridgeConfig;
import com.telcobright.orchestrix.automation.devices.server.linux.base.LinuxDistribution;
import com.telcobright.orchestrix.automation.devices.server.linux.common.SystemDetector;
import com.telcobright.orchestrix.device.SshDevice;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * Prerequisite preparer for LXC/LXD deployments
 * Checks and prepares the environment for container deployment
 */
public class LxcPrerequisitePreparer {

    private static final Logger logger = Logger.getLogger(LxcPrerequisitePreparer.class.getName());

    private final LxdInstallerAutomation lxdInstaller;
    private final SnapInstallAutomation snapInstaller;
    private final IpForwardAutomation ipForwardAutomation;
    private final LxdBridgeConfigureAutomation bridgeAutomation;
    private final MasqueradeAutomation masqueradeAutomation;

    public static class PrerequisiteConfig {
        private boolean requireLxd = true;
        private boolean requireSnapPath = true;
        private boolean requireIpForward = true;
        private boolean requireBridge = true;
        private boolean requireMasquerade = true;
        private String bridgeNetwork = "10.0.3.0/24";
        private String bridgeName = "lxdbr0";
        private boolean forceReinstall = false;

        // Getters and setters
        public boolean isRequireLxd() { return requireLxd; }
        public void setRequireLxd(boolean requireLxd) { this.requireLxd = requireLxd; }

        public boolean isRequireSnapPath() { return requireSnapPath; }
        public void setRequireSnapPath(boolean requireSnapPath) { this.requireSnapPath = requireSnapPath; }

        public boolean isRequireIpForward() { return requireIpForward; }
        public void setRequireIpForward(boolean requireIpForward) { this.requireIpForward = requireIpForward; }

        public boolean isRequireBridge() { return requireBridge; }
        public void setRequireBridge(boolean requireBridge) { this.requireBridge = requireBridge; }

        public boolean isRequireMasquerade() { return requireMasquerade; }
        public void setRequireMasquerade(boolean requireMasquerade) { this.requireMasquerade = requireMasquerade; }

        public String getBridgeNetwork() { return bridgeNetwork; }
        public void setBridgeNetwork(String bridgeNetwork) { this.bridgeNetwork = bridgeNetwork; }

        public String getBridgeName() { return bridgeName; }
        public void setBridgeName(String bridgeName) { this.bridgeName = bridgeName; }

        public boolean isForceReinstall() { return forceReinstall; }
        public void setForceReinstall(boolean forceReinstall) { this.forceReinstall = forceReinstall; }

        public static class Builder {
            private PrerequisiteConfig config = new PrerequisiteConfig();

            public Builder requireLxd(boolean require) {
                config.requireLxd = require;
                return this;
            }

            public Builder requireSnapPath(boolean require) {
                config.requireSnapPath = require;
                return this;
            }

            public Builder requireIpForward(boolean require) {
                config.requireIpForward = require;
                return this;
            }

            public Builder requireBridge(boolean require) {
                config.requireBridge = require;
                return this;
            }

            public Builder requireMasquerade(boolean require) {
                config.requireMasquerade = require;
                return this;
            }

            public Builder bridgeNetwork(String network) {
                config.bridgeNetwork = network;
                return this;
            }

            public Builder bridgeName(String name) {
                config.bridgeName = name;
                return this;
            }

            public Builder forceReinstall(boolean force) {
                config.forceReinstall = force;
                return this;
            }

            public PrerequisiteConfig build() {
                return config;
            }
        }
    }

    public static class PrerequisiteReport {
        private boolean success = false;
        private List<String> messages = new ArrayList<>();
        private List<String> warnings = new ArrayList<>();
        private List<String> errors = new ArrayList<>();

        public void log(String message) {
            logger.info(message);
            messages.add(message);
        }

        public void warn(String message) {
            logger.warning(message);
            warnings.add(message);
        }

        public void fail(String message) {
            logger.severe(message);
            errors.add(message);
            success = false;
        }

        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }

        public List<String> getMessages() { return messages; }
        public List<String> getWarnings() { return warnings; }
        public List<String> getErrors() { return errors; }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("Prerequisite Report:\n");
            sb.append("Status: ").append(success ? "SUCCESS" : "FAILED").append("\n");

            if (!messages.isEmpty()) {
                sb.append("\nMessages:\n");
                messages.forEach(m -> sb.append("  ").append(m).append("\n"));
            }

            if (!warnings.isEmpty()) {
                sb.append("\nWarnings:\n");
                warnings.forEach(w -> sb.append("  ⚠ ").append(w).append("\n"));
            }

            if (!errors.isEmpty()) {
                sb.append("\nErrors:\n");
                errors.forEach(e -> sb.append("  ✗ ").append(e).append("\n"));
            }

            return sb.toString();
        }
    }

    public LxcPrerequisitePreparer(LinuxDistribution distribution, boolean useSudo) {
        this.lxdInstaller = LxdInstallerAutomationFactory.createLxdInstallerAutomation(distribution, useSudo);
        this.snapInstaller = SnapInstallAutomationFactory.createSnapInstallAutomation(distribution, useSudo);
        this.ipForwardAutomation = IpForwardAutomationFactory.createIpForwardAutomation(distribution, useSudo);
        this.bridgeAutomation = LxdBridgeConfigureAutomationFactory.createLxdBridgeConfigureAutomation(distribution, useSudo);
        this.masqueradeAutomation = MasqueradeAutomationFactory.createMasqueradeAutomation(distribution, useSudo);
    }

    public LxcPrerequisitePreparer(SshDevice device, boolean useSudo) {
        LinuxDistribution distribution = SystemDetector.detectDistribution(device);
        this.lxdInstaller = LxdInstallerAutomationFactory.createLxdInstallerAutomation(distribution, useSudo);
        this.snapInstaller = SnapInstallAutomationFactory.createSnapInstallAutomation(distribution, useSudo);
        this.ipForwardAutomation = IpForwardAutomationFactory.createIpForwardAutomation(distribution, useSudo);
        this.bridgeAutomation = LxdBridgeConfigureAutomationFactory.createLxdBridgeConfigureAutomation(distribution, useSudo);
        this.masqueradeAutomation = MasqueradeAutomationFactory.createMasqueradeAutomation(distribution, useSudo);
    }

    /**
     * Check and prepare all prerequisites for LXC deployment
     */
    public PrerequisiteReport checkAndPrepare(SshDevice device, PrerequisiteConfig config) {
        PrerequisiteReport report = new PrerequisiteReport();

        try {
            // Step 1: Check and install LXD
            if (config.isRequireLxd()) {
                report.log("Checking LXD installation...");
                if (!lxdInstaller.isLxdInstalled(device) || config.isForceReinstall()) {
                    report.log("LXD not installed or force reinstall requested, installing...");
                    if (!lxdInstaller.installLxd(device)) {
                        report.fail("Failed to install LXD");
                        return report;
                    }
                    report.log("✓ LXD installed successfully");
                } else {
                    report.log("✓ LXD already installed");
                }

                // Verify LXD service is running
                if (!lxdInstaller.isLxdServiceRunning(device)) {
                    report.log("Starting LXD service...");
                    if (!lxdInstaller.startLxdService(device)) {
                        report.warn("Failed to start LXD service");
                    } else {
                        report.log("✓ LXD service started");
                    }
                }
            }

            // Step 2: Configure snap path if needed
            if (config.isRequireSnapPath()) {
                try {
                    if (snapInstaller.isSnapInstalled(device)) {
                        report.log("Configuring snap path...");
                        if (!snapInstaller.configureSnapPath(device)) {
                            report.warn("Failed to configure snap path");
                        } else {
                            report.log("✓ Snap path configured");
                        }
                    }
                } catch (Exception e) {
                    report.warn("Snap configuration skipped: " + e.getMessage());
                }
            }

            // Step 3: Enable IP forwarding
            if (config.isRequireIpForward()) {
                report.log("Checking IP forwarding...");
                if (!ipForwardAutomation.verify(device)) {
                    report.log("IP forwarding disabled, enabling...");
                    if (!ipForwardAutomation.execute(device)) {
                        report.fail("Failed to enable IP forwarding");
                        return report;
                    }
                    report.log("✓ IP forwarding enabled");
                } else {
                    report.log("✓ IP forwarding already enabled");
                }
            }

            // Step 4: Configure LXD bridge
            if (config.isRequireBridge()) {
                report.log("Checking LXD bridge configuration...");

                LxdBridgeConfig bridgeConfig = new LxdBridgeConfig.Builder()
                    .bridgeName(config.getBridgeName())
                    .ipv4Network(config.getBridgeNetwork())
                    .ipv4Nat(true)
                    .build();

                if (!bridgeAutomation.isLxdInitialized(device)) {
                    report.log("LXD not initialized, initializing with bridge...");
                    if (!bridgeAutomation.initializeLxd(device, bridgeConfig)) {
                        report.fail("Failed to initialize LXD");
                        return report;
                    }
                    report.log("✓ LXD initialized with bridge");
                } else if (!bridgeAutomation.bridgeExists(device, config.getBridgeName())) {
                    report.log("Bridge not found, creating...");
                    if (!bridgeAutomation.createBridge(device, config.getBridgeName(),
                            config.getBridgeNetwork())) {
                        report.fail("Failed to create bridge");
                        return report;
                    }
                    report.log("✓ Bridge created");
                } else {
                    report.log("✓ Bridge already exists");
                }

                // Verify bridge operation
                if (!bridgeAutomation.verifyBridgeOperation(device, config.getBridgeName())) {
                    report.warn("Bridge verification failed, may not be fully operational");
                }
            }

            // Step 5: Setup masquerading for NAT
            if (config.isRequireMasquerade()) {
                report.log("Checking NAT masquerading...");
                if (!masqueradeAutomation.isMasqueradeEnabled(device)) {
                    report.log("Masquerading not enabled, configuring...");

                    MasqueradeRule rule = new MasqueradeRule.Builder()
                        .sourceIp(config.getBridgeNetwork())
                        .outInterface(getDefaultInterface(device))
                        .persistent(true)
                        .comment("LXC NAT masquerading")
                        .build();

                    if (!masqueradeAutomation.addMasqueradeRule(device, rule)) {
                        report.warn("Failed to configure masquerading - containers may lack internet access");
                    } else {
                        report.log("✓ Masquerading configured");
                    }
                } else {
                    report.log("✓ Masquerading already enabled");
                }
            }

            report.setSuccess(true);
            report.log("\n✅ All prerequisites prepared successfully");

        } catch (Exception e) {
            report.fail("Unexpected error during prerequisite preparation: " + e.getMessage());
            e.printStackTrace();
        }

        return report;
    }

    /**
     * Get the default network interface for masquerading
     */
    private String getDefaultInterface(SshDevice device) {
        try {
            String result = device.sendAndReceive(
                "ip route | grep default | awk '{print $5}' | head -1"
            ).get();
            return (result != null && !result.trim().isEmpty()) ? result.trim() : "eth0";
        } catch (Exception e) {
            return "eth0";
        }
    }

    /**
     * Quick check if basic prerequisites are met
     */
    public boolean quickCheck(SshDevice device) {
        try {
            return lxdInstaller.isLxdInstalled(device) &&
                   ipForwardAutomation.verify(device);
        } catch (Exception e) {
            return false;
        }
    }
}