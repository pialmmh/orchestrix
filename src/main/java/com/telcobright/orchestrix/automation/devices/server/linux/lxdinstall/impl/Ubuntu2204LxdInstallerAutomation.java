package com.telcobright.orchestrix.automation.devices.server.linux.lxdinstall.impl;

import com.telcobright.orchestrix.automation.devices.server.linux.base.AbstractLinuxAutomation;
import com.telcobright.orchestrix.automation.devices.server.linux.base.LinuxDistribution;
import com.telcobright.orchestrix.automation.devices.server.linux.lxdinstall.LxdInstallerAutomation;
import com.telcobright.orchestrix.automation.core.device.SshDevice;
import java.util.Map;
import java.util.HashMap;

public class Ubuntu2204LxdInstallerAutomation extends AbstractLinuxAutomation implements LxdInstallerAutomation {

    public Ubuntu2204LxdInstallerAutomation(boolean useSudo) {
        super(LinuxDistribution.UBUNTU_2204, useSudo);
    }

    @Override
    public boolean execute(SshDevice device) throws Exception {
        return installLxd(device);
    }

    @Override
    public boolean verify(SshDevice device) throws Exception {
        return isLxdInstalled(device);
    }

    @Override
    public boolean isLxdInstalled(SshDevice device) throws Exception {
        String result = executeCommand(device, "which lxd");
        if (result != null && !result.isEmpty()) {
            return true;
        }
        result = executeCommand(device, "snap list lxd 2>/dev/null");
        return result != null && result.contains("lxd");
    }

    @Override
    public boolean installLxd(SshDevice device) throws Exception {
        if (isLxdInstalled(device)) {
            return true;
        }

        // Try snap first
        String result = executeCommand(device, "snap --version");
        if (result != null) {
            result = executeCommand(device, "snap install lxd");
            if (result != null) {
                return addUserToLxdGroup(device, "$USER");
            }
        }

        // Fallback to apt
        executeCommand(device, "apt-get update");
        result = executeCommand(device, "apt-get install -y lxd lxd-client");
        return result != null && addUserToLxdGroup(device, "$USER");
    }

    @Override
    public boolean addUserToLxdGroup(SshDevice device, String username) throws Exception {
        String result = executeCommand(device, "usermod -aG lxd " + username);
        return result != null;
    }

    @Override
    public String getLxdVersion(SshDevice device) throws Exception {
        return executeCommand(device, "lxd --version");
    }

    @Override
    public boolean isLxdServiceRunning(SshDevice device) throws Exception {
        String result = executeCommand(device, "systemctl is-active snap.lxd.daemon 2>/dev/null");
        if (result != null && result.trim().equals("active")) {
            return true;
        }
        result = executeCommand(device, "systemctl is-active lxd 2>/dev/null");
        return result != null && result.trim().equals("active");
    }

    @Override
    public boolean startLxdService(SshDevice device) throws Exception {
        String result = executeCommand(device, "snap start lxd 2>/dev/null");
        if (result == null) {
            result = executeCommand(device, "systemctl start lxd 2>/dev/null");
        }
        return result != null;
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
        return "Ubuntu2204 LxdInstallerAutomation";
    }

    @Override
    public String getDescription() {
        return "LXD installation for Ubuntu2204";
    }
}
