package com.telcobright.orchestrix.automation.devices.server.linux.lxd.impl;

import com.telcobright.orchestrix.automation.devices.server.linux.base.AbstractLinuxAutomation;
import com.telcobright.orchestrix.automation.devices.server.linux.base.LinuxDistribution;
import com.telcobright.orchestrix.automation.devices.server.linux.lxd.LxdBridgeAutomation;
import com.telcobright.orchestrix.automation.devices.server.linux.lxd.LxdBridgeConfig;
import com.telcobright.orchestrix.automation.core.device.SshDevice;

import java.io.File;
import java.io.FileWriter;
import java.util.Map;

/**
 * Default implementation of LXD bridge automation
 * Handles the complete process of setting up LXD with bridge networking
 */
public class DefaultLxdBridgeAutomation extends AbstractLinuxAutomation implements LxdBridgeAutomation {

    public DefaultLxdBridgeAutomation(boolean useSudo) {
        super(LinuxDistribution.UNKNOWN, useSudo);
    }

    @Override
    public boolean execute(SshDevice device) throws Exception {
        // Default execution performs complete setup with default config
        LxdBridgeConfig config = new LxdBridgeConfig.Builder().build();
        return performCompleteSetup(device, config);
    }

    @Override
    public boolean verify(SshDevice device) throws Exception {
        return isLxdInstalled(device) && isLxdInitialized(device);
    }

    @Override
    public boolean isLxdInstalled(SshDevice device) throws Exception {
        // Check if lxd command exists
        String result = executeCommand(device, "which lxd");
        if (result != null && !result.isEmpty()) {
            return true;
        }

        // Check snap installation
        result = executeCommand(device, "snap list lxd 2>/dev/null");
        return result != null && result.contains("lxd");
    }

    @Override
    public boolean installLxd(SshDevice device) throws Exception {
        if (isLxdInstalled(device)) {
            System.out.println("LXD is already installed");
            return true;
        }

        // Try snap first (preferred method)
        String result = executeCommand(device, "snap --version");
        if (result != null && !result.isEmpty()) {
            // Install via snap
            result = executeCommand(device, "snap install lxd");
            if (result != null) {
                // Add user to lxd group
                executeCommand(device, "usermod -aG lxd $USER");
                return true;
            }
        }

        // Fallback to package manager
        String distroCheck = executeCommand(device, "cat /etc/os-release");
        if (distroCheck != null) {
            if (distroCheck.toLowerCase().contains("ubuntu") || distroCheck.toLowerCase().contains("debian")) {
                executeCommand(device, "apt-get update");
                result = executeCommand(device, "apt-get install -y lxd lxd-client");
            } else if (distroCheck.toLowerCase().contains("centos") || distroCheck.toLowerCase().contains("rhel")) {
                executeCommand(device, "yum install -y epel-release");
                result = executeCommand(device, "yum install -y lxd");
            }
        }

        return isLxdInstalled(device);
    }

    @Override
    public boolean configureLxdPath(SshDevice device) throws Exception {
        // Check if lxd is in PATH
        String result = executeCommand(device, "which lxd");
        if (result != null && !result.isEmpty()) {
            return true;
        }

        // Add snap bin to PATH if needed
        String snapPath = "/snap/bin";
        result = executeCommand(device, "test -d " + snapPath + " && echo exists");
        if (result != null && result.contains("exists")) {
            // Add to bashrc
            executeCommand(device, "echo 'export PATH=$PATH:" + snapPath + "' >> ~/.bashrc");
            // Add to profile
            executeCommand(device, "echo 'export PATH=$PATH:" + snapPath + "' >> ~/.profile");
            // Source for current session
            executeCommand(device, "export PATH=$PATH:" + snapPath);
            return true;
        }

        return false;
    }

    @Override
    public boolean enableIpForwarding(SshDevice device) throws Exception {
        // Enable IP forwarding temporarily
        String result = executeCommand(device, "echo 1 > /proc/sys/net/ipv4/ip_forward");

        // Make it permanent
        executeCommand(device, "sysctl -w net.ipv4.ip_forward=1");

        // Persist across reboots
        String sysctlConf = executeCommand(device, "grep 'net.ipv4.ip_forward' /etc/sysctl.conf");
        if (sysctlConf == null || !sysctlConf.contains("net.ipv4.ip_forward")) {
            executeCommand(device, "echo 'net.ipv4.ip_forward=1' >> /etc/sysctl.conf");
        } else {
            executeCommand(device, "sed -i 's/.*net.ipv4.ip_forward.*/net.ipv4.ip_forward=1/' /etc/sysctl.conf");
        }

        executeCommand(device, "sysctl -p");

        return true;
    }

    @Override
    public boolean configureLxdBridge(SshDevice device, LxdBridgeConfig config) throws Exception {
        // Check if bridge already exists
        String bridges = executeCommand(device, "lxc network list --format csv");
        if (bridges != null && bridges.contains(config.getBridgeName())) {
            // Update existing bridge
            executeCommand(device, String.format("lxc network set %s ipv4.address %s",
                config.getBridgeName(), config.getIpv4Network()));
            executeCommand(device, String.format("lxc network set %s ipv4.nat %s",
                config.getBridgeName(), config.isIpv4Nat()));
            if (config.getIpv4DhcpRange() != null) {
                executeCommand(device, String.format("lxc network set %s ipv4.dhcp.ranges %s",
                    config.getBridgeName(), config.getIpv4DhcpRange()));
            }
        } else {
            // Create new bridge
            String createCmd = String.format(
                "lxc network create %s ipv4.address=%s ipv4.nat=%s",
                config.getBridgeName(),
                config.getIpv4Network(),
                config.isIpv4Nat()
            );
            executeCommand(device, createCmd);
        }

        return verifyBridge(device, config.getBridgeName());
    }

    @Override
    public boolean initializeLxd(SshDevice device, LxdBridgeConfig config) throws Exception {
        if (isLxdInitialized(device)) {
            System.out.println("LXD is already initialized");
            return true;
        }

        // Generate preseed configuration
        String preseedYaml = config.toPreseedYaml();

        // Create temporary file with preseed
        String tempFile = "/tmp/lxd-preseed.yaml";
        String escapedYaml = preseedYaml.replace("'", "'\\''");
        executeCommand(device, String.format("echo '%s' > %s", escapedYaml, tempFile));

        // Initialize LXD with preseed
        String result = executeCommand(device, "cat " + tempFile + " | lxd init --preseed");

        // Clean up temp file
        executeCommand(device, "rm -f " + tempFile);

        return result != null && !result.toLowerCase().contains("error");
    }

    @Override
    public boolean isLxdInitialized(SshDevice device) throws Exception {
        // Check if LXD is initialized by checking for default profile
        String result = executeCommand(device, "lxc profile show default 2>/dev/null");
        return result != null && !result.isEmpty() && !result.contains("error");
    }

    @Override
    public boolean verifyBridge(SshDevice device, String bridgeName) throws Exception {
        // Check if bridge exists in system
        String result = executeCommand(device, "ip link show " + bridgeName + " 2>/dev/null");
        if (result == null || result.isEmpty()) {
            return false;
        }

        // Check if bridge is in LXD
        result = executeCommand(device, "lxc network show " + bridgeName + " 2>/dev/null");
        return result != null && !result.isEmpty() && !result.contains("error");
    }

    @Override
    public boolean configureFirewallRules(SshDevice device, LxdBridgeConfig config) throws Exception {
        // Allow forwarding from bridge
        String bridgeName = config.getBridgeName();

        // Check if iptables exists
        String iptablesCheck = executeCommand(device, "which iptables");
        if (iptablesCheck != null && !iptablesCheck.isEmpty()) {
            // Allow forwarding
            executeCommand(device, String.format(
                "iptables -A FORWARD -i %s -j ACCEPT", bridgeName));
            executeCommand(device, String.format(
                "iptables -A FORWARD -o %s -j ACCEPT", bridgeName));

            // Setup NAT if enabled
            if (config.isIpv4Nat()) {
                executeCommand(device, String.format(
                    "iptables -t nat -A POSTROUTING -s %s ! -d %s -j MASQUERADE",
                    config.getIpv4Network(), config.getIpv4Network()));
            }

            // Save rules (try different methods)
            String saveResult = executeCommand(device, "iptables-save > /etc/iptables/rules.v4 2>/dev/null");
            if (saveResult == null) {
                executeCommand(device, "iptables-save > /etc/sysconfig/iptables 2>/dev/null");
            }
        }

        // Check for firewalld
        String firewalldCheck = executeCommand(device, "systemctl is-active firewalld 2>/dev/null");
        if (firewalldCheck != null && firewalldCheck.trim().equals("active")) {
            executeCommand(device, String.format(
                "firewall-cmd --zone=trusted --add-interface=%s --permanent", bridgeName));
            executeCommand(device, "firewall-cmd --reload");
        }

        return true;
    }

    @Override
    public String getLxdNetworkConfig(SshDevice device) throws Exception {
        return executeCommand(device, "lxc network list");
    }

    @Override
    public boolean restartLxd(SshDevice device) throws Exception {
        // Try snap first
        String result = executeCommand(device, "snap restart lxd 2>/dev/null");
        if (result != null) {
            return true;
        }

        // Try systemctl
        result = executeCommand(device, "systemctl restart lxd 2>/dev/null");
        if (result != null) {
            return true;
        }

        // Try service command
        result = executeCommand(device, "service lxd restart 2>/dev/null");
        return result != null;
    }

    @Override
    public boolean performCompleteSetup(SshDevice device, LxdBridgeConfig config) throws Exception {
        boolean success = true;

        // Step 1: Install LXD if not present
        System.out.println("Step 1: Checking and installing LXD...");
        success = success && installLxd(device);
        if (!success) {
            System.err.println("Failed to install LXD");
            return false;
        }

        // Step 2: Configure PATH
        System.out.println("Step 2: Configuring LXD path...");
        success = success && configureLxdPath(device);
        if (!success) {
            System.err.println("Failed to configure LXD path");
            return false;
        }

        // Step 3: Enable IP forwarding
        System.out.println("Step 3: Enabling IP forwarding...");
        success = success && enableIpForwarding(device);
        if (!success) {
            System.err.println("Failed to enable IP forwarding");
            return false;
        }

        // Step 4: Initialize LXD if needed
        System.out.println("Step 4: Initializing LXD...");
        if (!isLxdInitialized(device)) {
            success = success && initializeLxd(device, config);
        } else {
            // Just configure the bridge if already initialized
            success = success && configureLxdBridge(device, config);
        }
        if (!success) {
            System.err.println("Failed to initialize/configure LXD");
            return false;
        }

        // Step 5: Configure firewall rules
        System.out.println("Step 5: Configuring firewall rules...");
        success = success && configureFirewallRules(device, config);
        if (!success) {
            System.err.println("Failed to configure firewall rules");
            return false;
        }

        // Step 6: Verify setup
        System.out.println("Step 6: Verifying bridge configuration...");
        success = success && verifyBridge(device, config.getBridgeName());

        if (success) {
            System.out.println("LXD bridge setup completed successfully!");
            System.out.println("Network configuration:");
            System.out.println(getLxdNetworkConfig(device));
        }

        return success;
    }

    @Override
    protected boolean isPackageInstalled(SshDevice device, String packageName) throws Exception {
        String result = executeCommand(device, "which " + packageName);
        return result != null && !result.isEmpty();
    }

    @Override
    protected String getPackageManagerCommand() {
        return "apt";
    }

    @Override
    public String getName() {
        return "LXD Bridge Automation";
    }

    @Override
    public String getDescription() {
        return "Complete LXD installation and bridge configuration automation";
    }
}