package com.telcobright.orchestrix.automation.devices.server.linux.lxdbridge.impl;

import com.telcobright.orchestrix.automation.devices.server.linux.base.AbstractLinuxAutomation;
import com.telcobright.orchestrix.automation.devices.server.linux.base.LinuxDistribution;
import com.telcobright.orchestrix.automation.devices.server.linux.lxdbridge.LxdBridgeConfigureAutomation;
import com.telcobright.orchestrix.automation.devices.server.linux.lxd.LxdBridgeConfig;
import com.telcobright.orchestrix.device.SshDevice;
import java.util.Map;
import java.util.HashMap;

public class RHEL8LxdBridgeConfigureAutomation extends AbstractLinuxAutomation implements LxdBridgeConfigureAutomation {

    public RHEL8LxdBridgeConfigureAutomation(boolean useSudo) {
        super(LinuxDistribution.RHEL_8, useSudo);
    }

    @Override
    public boolean execute(SshDevice device) throws Exception {
        LxdBridgeConfig config = new LxdBridgeConfig.Builder().build();
        return initializeLxd(device, config);
    }

    @Override
    public boolean verify(SshDevice device) throws Exception {
        return isLxdInitialized(device);
    }

    @Override
    public boolean isLxdInitialized(SshDevice device) throws Exception {
        String result = executeCommand(device, "lxc profile show default 2>/dev/null");
        return result != null && !result.isEmpty() && !result.contains("error");
    }

    @Override
    public boolean initializeLxd(SshDevice device, LxdBridgeConfig config) throws Exception {
        if (isLxdInitialized(device)) {
            return true;
        }

        String preseedYaml = config.toPreseedYaml();
        String tempFile = "/tmp/lxd-preseed.yaml";
        String escapedYaml = preseedYaml.replace("'", "'\\''");
        executeCommand(device, String.format("echo '%s' > %s", escapedYaml, tempFile));

        String result = executeCommand(device, "cat " + tempFile + " | lxd init --preseed");
        executeCommand(device, "rm -f " + tempFile);

        return result != null && !result.toLowerCase().contains("error");
    }

    @Override
    public boolean configureBridge(SshDevice device, LxdBridgeConfig config) throws Exception {
        if (bridgeExists(device, config.getBridgeName())) {
            return updateBridge(device, config.getBridgeName(), "ipv4.address", config.getIpv4Network());
        } else {
            return createBridge(device, config.getBridgeName(), config.getIpv4Network());
        }
    }

    @Override
    public boolean bridgeExists(SshDevice device, String bridgeName) throws Exception {
        String result = executeCommand(device, "lxc network show " + bridgeName + " 2>/dev/null");
        return result != null && !result.isEmpty() && !result.contains("error");
    }

    @Override
    public boolean createBridge(SshDevice device, String bridgeName, String network) throws Exception {
        String cmd = String.format("lxc network create %s ipv4.address=%s ipv4.nat=true",
                                  bridgeName, network);
        String result = executeCommand(device, cmd);
        return result != null && !result.toLowerCase().contains("error");
    }

    @Override
    public boolean deleteBridge(SshDevice device, String bridgeName) throws Exception {
        String result = executeCommand(device, "lxc network delete " + bridgeName);
        return result != null && !result.toLowerCase().contains("error");
    }

    @Override
    public boolean updateBridge(SshDevice device, String bridgeName, String key, String value) throws Exception {
        String cmd = String.format("lxc network set %s %s %s", bridgeName, key, value);
        String result = executeCommand(device, cmd);
        return result != null && !result.toLowerCase().contains("error");
    }

    @Override
    public String getBridgeConfig(SshDevice device, String bridgeName) throws Exception {
        return executeCommand(device, "lxc network show " + bridgeName);
    }

    @Override
    public boolean configureDefaultProfile(SshDevice device, String bridgeName) throws Exception {
        String cmd = String.format("lxc profile device set default eth0 network=%s", bridgeName);
        String result = executeCommand(device, cmd);
        return result != null && !result.toLowerCase().contains("error");
    }

    @Override
    public boolean verifyBridgeOperation(SshDevice device, String bridgeName) throws Exception {
        String result = executeCommand(device, "ip link show " + bridgeName + " 2>/dev/null");
        if (result == null || result.isEmpty()) {
            return false;
        }

        result = executeCommand(device, "lxc network list --format csv");
        return result != null && result.contains(bridgeName);
    }

    @Override
    protected boolean isPackageInstalled(SshDevice device, String packageName) throws Exception {
        String result = executeCommand(device, "which " + packageName);
        return result != null && !result.isEmpty();
    }

    @Override
    protected String getPackageManagerCommand() {
        return "yum";
    }

    @Override
    public String getName() {
        return "RHEL8 LxdBridgeConfigureAutomation";
    }

    @Override
    public String getDescription() {
        return "LXD bridge configuration for RHEL8";
    }
}