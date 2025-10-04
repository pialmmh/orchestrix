package com.telcobright.orchestrix.automation.runners;

import com.telcobright.orchestrix.automation.TerminalRunner;
import com.telcobright.orchestrix.automation.devices.server.platform.LxdBridgeNetworkingAutomation;
import com.telcobright.orchestrix.automation.api.device.TerminalDevice;
import com.telcobright.orchestrix.automation.core.device.SshDevice;
import java.util.Map;
import java.util.HashMap;
import java.util.logging.Logger;

/**
 * TerminalRunner implementation for LXD Bridge Networking configuration
 */
public class LxdBridgeNetworkingTerminalRunner implements TerminalRunner {

    private static final Logger logger = Logger.getLogger(LxdBridgeNetworkingTerminalRunner.class.getName());

    private Map<String, String> lastStatus = new HashMap<>();

    @Override
    public String getName() {
        return "LxdBridgeNetworking";
    }

    @Override
    public String getDescription() {
        return "Configure LXD bridge networking (lxdbr0) with NAT/masquerade and DHCP";
    }

    @Override
    public boolean execute(TerminalDevice terminalDevice, Map<String, String> config) throws Exception {
        logger.info("Configuring LXD bridge networking...");

        // Extract configuration
        String bridgeName = config.getOrDefault("bridge.name", "lxdbr0");
        String hostGatewayIp = config.getOrDefault("bridge.gateway.ip", "10.0.8.1");
        String networkCidr = config.getOrDefault("bridge.network.cidr", "10.0.8.0/24");
        boolean enableNat = Boolean.parseBoolean(config.getOrDefault("bridge.enable.nat", "true"));
        boolean enableDns = Boolean.parseBoolean(config.getOrDefault("bridge.enable.dns", "true"));
        String dhcpRangeStart = config.getOrDefault("bridge.dhcp.start", "10.0.8.2");
        String dhcpRangeEnd = config.getOrDefault("bridge.dhcp.end", "10.0.8.254");
        boolean deleteBridge = Boolean.parseBoolean(config.getOrDefault("delete.existing", "false"));
        boolean useSudo = Boolean.parseBoolean(config.getOrDefault("bridge.use.sudo", "true"));

        // Build automation config
        LxdBridgeNetworkingAutomation.Config automationConfig =
            new LxdBridgeNetworkingAutomation.Config()
                .bridgeName(bridgeName)
                .hostGatewayIp(hostGatewayIp)
                .networkCidr(networkCidr)
                .enableNat(enableNat)
                .enableDns(enableDns)
                .dhcpRange(dhcpRangeStart, dhcpRangeEnd)
                .useSudo(useSudo);

        // Create and execute automation
        // For now, this automation only supports SSH devices
        if (!(terminalDevice instanceof SshDevice)) {
            logger.severe("LxdBridgeNetworkingAutomation currently only supports SSH devices");
            return false;
        }
        SshDevice sshDevice = (SshDevice) terminalDevice;
        LxdBridgeNetworkingAutomation automation = automationConfig.build(sshDevice);

        // Delete existing bridge if requested
        if (deleteBridge) {
            logger.info("Deleting existing bridge if present...");
            boolean deleted = automation.remove();
            lastStatus.put("bridge.deleted", String.valueOf(deleted));
        }

        // Configure the bridge
        boolean success = automation.configure();

        // Store status
        lastStatus.put("configuration.success", String.valueOf(success));
        lastStatus.put("bridge.name", bridgeName);
        lastStatus.put("bridge.gateway.ip", hostGatewayIp);
        lastStatus.put("bridge.network.cidr", networkCidr);
        lastStatus.put("bridge.enable.nat", String.valueOf(enableNat));
        lastStatus.put("bridge.enable.dns", String.valueOf(enableDns));

        return success;
    }

    @Override
    public boolean verify(TerminalDevice terminalDevice, Map<String, String> config) throws Exception {
        logger.info("Verifying LXD bridge configuration...");

        String bridgeName = config.getOrDefault("bridge.name", "lxdbr0");

        // Build automation config
        // For now, this automation only supports SSH devices
        if (!(terminalDevice instanceof SshDevice)) {
            logger.severe("LxdBridgeNetworkingAutomation currently only supports SSH devices");
            return false;
        }
        SshDevice sshDevice = (SshDevice) terminalDevice;
        LxdBridgeNetworkingAutomation automation =
            new LxdBridgeNetworkingAutomation.Config()
                .bridgeName(bridgeName)
                .build(sshDevice);

        // Verify the bridge
        boolean verified = automation.verify();

        // Get status
        Map<String, String> status = automation.getCurrentConfiguration();
        lastStatus.putAll(status);
        lastStatus.put("verification.success", String.valueOf(verified));

        return verified;
    }

    @Override
    public boolean shouldSkip(TerminalDevice terminalDevice, Map<String, String> config) throws Exception {
        // Check if LXD is installed - check both regular and snap installations
        String result = terminalDevice.sendAndReceive("which lxc 2>/dev/null || which lxd 2>/dev/null || test -f /snap/bin/lxc && echo /snap/bin/lxc").get();

        if (result == null || result.trim().isEmpty()) {
            logger.warning("LXD/LXC not installed, skipping bridge configuration");
            lastStatus.put("skip.reason", "LXD/LXC not installed");
            return true;
        }

        // Check if forced execution
        boolean force = Boolean.parseBoolean(config.getOrDefault("force.execution", "false"));
        if (force) {
            return false;
        }

        // Check if bridge already exists with desired config
        String bridgeName = config.getOrDefault("bridge.name", "lxdbr0");
        String checkBridge = terminalDevice.sendAndReceive("ip addr show " + bridgeName + " 2>/dev/null | grep inet").get();

        if (checkBridge != null && !checkBridge.isEmpty()) {
            String desiredIp = config.getOrDefault("bridge.gateway.ip", "10.0.8.1");
            if (checkBridge.contains(desiredIp)) {
                logger.info("Bridge " + bridgeName + " already configured with desired IP");
                lastStatus.put("skip.reason", "Already configured");
                return true;
            }
        }

        return false;
    }

    @Override
    public Map<String, String> getStatus() {
        return new HashMap<>(lastStatus);
    }
}