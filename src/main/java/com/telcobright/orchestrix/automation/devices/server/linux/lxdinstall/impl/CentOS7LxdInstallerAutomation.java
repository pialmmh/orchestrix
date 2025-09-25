package com.telcobright.orchestrix.automation.devices.server.linux.lxdinstall.impl;

import com.telcobright.orchestrix.automation.devices.server.linux.base.AbstractLinuxAutomation;
import com.telcobright.orchestrix.automation.devices.server.linux.base.LinuxDistribution;
import com.telcobright.orchestrix.automation.devices.server.linux.lxdinstall.LxdInstallerAutomation;
import com.telcobright.orchestrix.device.SshDevice;
import java.util.Map;
import java.util.HashMap;

public class CentOS7LxdInstallerAutomation extends AbstractLinuxAutomation implements LxdInstallerAutomation {

    public CentOS7LxdInstallerAutomation(boolean useSudo) {
        super(LinuxDistribution.CENTOS_7, useSudo);
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

        // Install EPEL repository first
        executeCommand(device, "yum install -y epel-release");

        // Try snap
        String result = executeCommand(device, "yum install -y snapd");
        if (result != null) {
            executeCommand(device, "systemctl enable --now snapd.socket");
            executeCommand(device, "ln -s /var/lib/snapd/snap /snap 2>/dev/null");
            result = executeCommand(device, "snap install lxd");
            if (result != null) {
                return addUserToLxdGroup(device, "$USER");
            }
        }

        // Fallback to package manager
        result = executeCommand(device, "yum install -y lxd");
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
        return "yum";
    }

    @Override
    public String getName() {
        return "CentOS7 LxdInstallerAutomation";
    }

    @Override
    public String getDescription() {
        return "LXD installation for CentOS7";
    }
}
