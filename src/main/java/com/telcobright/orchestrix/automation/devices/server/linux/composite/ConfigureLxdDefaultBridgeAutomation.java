package com.telcobright.orchestrix.automation.devices.server.linux.composite;

import com.telcobright.orchestrix.automation.devices.server.linux.base.AbstractLinuxAutomation;
import com.telcobright.orchestrix.automation.devices.server.linux.base.LinuxDistribution;
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
import com.telcobright.orchestrix.automation.devices.server.linux.lxd.LxdBridgeConfig;
import com.telcobright.orchestrix.automation.devices.server.linux.common.SystemDetector;
import com.telcobright.orchestrix.device.SshDevice;

import java.util.Map;
import java.util.HashMap;

/**
 * Composite automation that orchestrates multiple unit automations
 * to set up a complete LXD environment with bridge networking
 */
public class ConfigureLxdDefaultBridgeAutomation extends AbstractLinuxAutomation {

    private LxdInstallerAutomation lxdInstaller;
    private SnapInstallAutomation snapInstaller;
    private IpForwardAutomation ipForwardAutomation;
    private LxdBridgeConfigureAutomation bridgeConfigureAutomation;
    private MasqueradeAutomation masqueradeAutomation;
    private LxdBridgeConfig bridgeConfig;

    public ConfigureLxdDefaultBridgeAutomation(boolean useSudo) {
        this(LinuxDistribution.UNKNOWN, useSudo, null);
    }

    public ConfigureLxdDefaultBridgeAutomation(LinuxDistribution distribution, boolean useSudo) {
        this(distribution, useSudo, null);
    }

    public ConfigureLxdDefaultBridgeAutomation(LinuxDistribution distribution, boolean useSudo, LxdBridgeConfig config) {
        super(distribution, useSudo);

        // Initialize unit automations using factories
        this.lxdInstaller = LxdInstallerAutomationFactory.createLxdInstallerAutomation(distribution, useSudo);
        this.snapInstaller = SnapInstallAutomationFactory.createSnapInstallAutomation(distribution, useSudo);
        this.ipForwardAutomation = IpForwardAutomationFactory.createIpForwardAutomation(distribution, useSudo);
        this.bridgeConfigureAutomation = LxdBridgeConfigureAutomationFactory.createLxdBridgeConfigureAutomation(distribution, useSudo);
        this.masqueradeAutomation = MasqueradeAutomationFactory.createMasqueradeAutomation(distribution, useSudo);

        // Use provided config or create default
        this.bridgeConfig = config != null ? config : new LxdBridgeConfig.Builder()
            .bridgeName("lxdbr0")
            .ipv4Network("10.0.3.1/24")
            .ipv4Nat(true)
            .ipv4DhcpRange("10.0.3.2,10.0.3.254")
            .storagePool("default")
            .storageBackend("dir")
            .build();
    }

    @Override
    public boolean execute(SshDevice device) throws Exception {
        System.out.println("Starting LXD Default Bridge Configuration...");

        Map<String, Boolean> results = new HashMap<>();

        // Step 1: Detect distribution if not provided
        if (getSupportedDistribution() == LinuxDistribution.UNKNOWN) {
            try {
                LinuxDistribution detected = SystemDetector.detectDistribution(device);
                System.out.println("Detected distribution: " + detected);

                // Recreate automations with detected distribution
                this.lxdInstaller = LxdInstallerAutomationFactory.createLxdInstallerAutomation(detected, isUsingSudo());
                this.snapInstaller = SnapInstallAutomationFactory.createSnapInstallAutomation(detected, isUsingSudo());
                this.ipForwardAutomation = IpForwardAutomationFactory.createIpForwardAutomation(detected, isUsingSudo());
                this.bridgeConfigureAutomation = LxdBridgeConfigureAutomationFactory.createLxdBridgeConfigureAutomation(detected, isUsingSudo());
                this.masqueradeAutomation = MasqueradeAutomationFactory.createMasqueradeAutomation(detected, isUsingSudo());
            } catch (Exception e) {
                System.err.println("Failed to detect distribution, using default implementations: " + e.getMessage());
            }
        }

        // Step 2: Install LXD if needed
        System.out.println("\nStep 1/6: Checking and installing LXD...");
        if (!lxdInstaller.isLxdInstalled(device)) {
            boolean installed = lxdInstaller.installLxd(device);
            results.put("lxd_install", installed);
            if (!installed) {
                System.err.println("Failed to install LXD");
                return false;
            }
            System.out.println("LXD installed successfully");
        } else {
            System.out.println("LXD already installed");
            results.put("lxd_install", true);
        }

        // Step 3: Configure snap path if snap is used
        System.out.println("\nStep 2/6: Configuring snap path if needed...");
        if (snapInstaller.isSnapInstalled(device)) {
            boolean pathConfigured = snapInstaller.configureSnapPath(device);
            results.put("snap_path", pathConfigured);
            if (pathConfigured) {
                System.out.println("Snap path configured");
            }
        } else {
            System.out.println("Snap not used, skipping path configuration");
            results.put("snap_path", true);
        }

        // Step 4: Enable IP forwarding
        System.out.println("\nStep 3/6: Enabling IP forwarding...");
        boolean ipForwardEnabled = ipForwardAutomation.execute(device);
        results.put("ip_forward", ipForwardEnabled);
        if (!ipForwardEnabled) {
            System.err.println("Failed to enable IP forwarding");
            return false;
        }
        System.out.println("IP forwarding enabled");

        // Step 5: Initialize or configure LXD bridge
        System.out.println("\nStep 4/6: Configuring LXD bridge...");
        boolean bridgeConfigured;
        if (!bridgeConfigureAutomation.isLxdInitialized(device)) {
            System.out.println("Initializing LXD with preseed configuration...");
            bridgeConfigured = bridgeConfigureAutomation.initializeLxd(device, bridgeConfig);
        } else {
            System.out.println("LXD already initialized, updating bridge configuration...");
            bridgeConfigured = bridgeConfigureAutomation.configureBridge(device, bridgeConfig);
        }
        results.put("bridge_config", bridgeConfigured);
        if (!bridgeConfigured) {
            System.err.println("Failed to configure LXD bridge");
            return false;
        }
        System.out.println("LXD bridge configured successfully");

        // Step 6: Configure masquerading for NAT
        System.out.println("\nStep 5/6: Configuring NAT masquerading...");
        boolean masqueradeEnabled = masqueradeAutomation.enableMasqueradeFromSource(
            device,
            bridgeConfig.getIpv4Network(),
            getDefaultInterface(device)
        );
        results.put("masquerade", masqueradeEnabled);
        if (!masqueradeEnabled) {
            System.err.println("Warning: Failed to configure masquerading, containers may not have internet access");
        } else {
            System.out.println("NAT masquerading configured");
        }

        // Step 7: Verify everything is working
        System.out.println("\nStep 6/6: Verifying configuration...");
        boolean verified = verify(device);
        results.put("verification", verified);

        if (verified) {
            System.out.println("\n✅ LXD Default Bridge Configuration completed successfully!");
            System.out.println("Bridge: " + bridgeConfig.getBridgeName());
            System.out.println("Network: " + bridgeConfig.getIpv4Network());
            System.out.println("DHCP Range: " + bridgeConfig.getIpv4DhcpRange());
        } else {
            System.err.println("\n⚠️ Configuration completed with warnings - please verify manually");
        }

        // Return true if core components succeeded
        return results.get("lxd_install") && results.get("bridge_config");
    }

    @Override
    public boolean verify(SshDevice device) throws Exception {
        boolean allGood = true;

        // Verify LXD is installed and running
        if (!lxdInstaller.isLxdInstalled(device)) {
            System.err.println("❌ LXD is not installed");
            allGood = false;
        } else if (!lxdInstaller.isLxdServiceRunning(device)) {
            System.err.println("⚠️ LXD service is not running");
            allGood = false;
        } else {
            System.out.println("✅ LXD is installed and running");
        }

        // Verify IP forwarding
        if (!ipForwardAutomation.verify(device)) {
            System.err.println("❌ IP forwarding is not enabled");
            allGood = false;
        } else {
            System.out.println("✅ IP forwarding is enabled");
        }

        // Verify bridge exists and is operational
        if (!bridgeConfigureAutomation.bridgeExists(device, bridgeConfig.getBridgeName())) {
            System.err.println("❌ Bridge " + bridgeConfig.getBridgeName() + " does not exist");
            allGood = false;
        } else if (!bridgeConfigureAutomation.verifyBridgeOperation(device, bridgeConfig.getBridgeName())) {
            System.err.println("⚠️ Bridge " + bridgeConfig.getBridgeName() + " exists but may not be fully operational");
            allGood = false;
        } else {
            System.out.println("✅ Bridge " + bridgeConfig.getBridgeName() + " is operational");
        }

        // Verify masquerading
        if (!masqueradeAutomation.isMasqueradeEnabled(device)) {
            System.err.println("⚠️ NAT masquerading is not configured (containers may lack internet access)");
        } else {
            System.out.println("✅ NAT masquerading is configured");
        }

        return allGood;
    }

    private String getDefaultInterface(SshDevice device) throws Exception {
        // Get the default network interface (usually the one with default route)
        String result = executeCommand(device, "ip route | grep default | awk '{print $5}' | head -1");
        return (result != null && !result.trim().isEmpty()) ? result.trim() : "eth0";
    }

    @Override
    protected boolean isPackageInstalled(SshDevice device, String packageName) throws Exception {
        return lxdInstaller.isLxdInstalled(device);
    }

    @Override
    protected String getPackageManagerCommand() {
        return "apt";
    }

    @Override
    public String getName() {
        return "Configure LXD Default Bridge";
    }

    @Override
    public String getDescription() {
        return "Complete LXD setup with bridge networking using unit automations";
    }

    @Override
    public Map<String, String> getStatus(SshDevice device) {
        Map<String, String> status = new HashMap<>();
        try {
            status.put("lxd_installed", String.valueOf(lxdInstaller.isLxdInstalled(device)));
            status.put("lxd_running", String.valueOf(lxdInstaller.isLxdServiceRunning(device)));
            status.put("lxd_initialized", String.valueOf(bridgeConfigureAutomation.isLxdInitialized(device)));
            status.put("bridge_exists", String.valueOf(bridgeConfigureAutomation.bridgeExists(device, bridgeConfig.getBridgeName())));
            status.put("ip_forward_enabled", String.valueOf(ipForwardAutomation.verify(device)));
            status.put("masquerade_enabled", String.valueOf(masqueradeAutomation.isMasqueradeEnabled(device)));
        } catch (Exception e) {
            status.put("error", e.getMessage());
        }
        return status;
    }

    // Setters for custom configurations
    public void setBridgeConfig(LxdBridgeConfig config) {
        this.bridgeConfig = config;
    }

    public LxdBridgeConfig getBridgeConfig() {
        return this.bridgeConfig;
    }
}