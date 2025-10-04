package com.telcobright.orchestrix.automation.devices.server.linux.snapinstall.impl;

import com.telcobright.orchestrix.automation.devices.server.linux.base.AbstractLinuxAutomation;
import com.telcobright.orchestrix.automation.devices.server.linux.base.LinuxDistribution;
import com.telcobright.orchestrix.automation.devices.server.linux.snapinstall.SnapInstallAutomation;
import java.util.List;
import java.util.ArrayList;
import com.telcobright.orchestrix.automation.core.device.SshDevice;
import java.util.Map;
import java.util.HashMap;

public class RHEL8SnapInstallAutomation extends AbstractLinuxAutomation implements SnapInstallAutomation {

    public RHEL8SnapInstallAutomation(boolean useSudo) {
        super(LinuxDistribution.RHEL_8, useSudo);
    }

    @Override
    public boolean execute(SshDevice device) throws Exception {
        return installSnapd(device);
    }

    @Override
    public boolean verify(SshDevice device) throws Exception {
        return isSnapInstalled(device);
    }

    @Override
    public boolean isSnapInstalled(SshDevice device) throws Exception {
        String result = executeCommand(device, "snap --version");
        return result != null && !result.isEmpty();
    }

    @Override
    public boolean installSnapd(SshDevice device) throws Exception {
        if (isSnapInstalled(device)) {
            return true;
        }

        executeCommand(device, "yum install -y epel-release");
        String result = executeCommand(device, "yum install -y snapd");
        if (result != null) {
            executeCommand(device, "systemctl enable --now snapd.socket");
            executeCommand(device, "ln -s /var/lib/snapd/snap /snap 2>/dev/null");
        }
        return result != null;
    }

    @Override
    public boolean installSnapPackage(SshDevice device, String packageName) throws Exception {
        if (!isSnapInstalled(device)) {
            installSnapd(device);
        }
        String result = executeCommand(device, "snap install " + packageName);
        return result != null && !result.toLowerCase().contains("error");
    }

    @Override
    public boolean installSnapPackage(SshDevice device, String packageName, String channel) throws Exception {
        if (!isSnapInstalled(device)) {
            installSnapd(device);
        }
        String result = executeCommand(device, "snap install " + packageName + " --" + channel);
        return result != null && !result.toLowerCase().contains("error");
    }

    @Override
    public boolean removeSnapPackage(SshDevice device, String packageName) throws Exception {
        String result = executeCommand(device, "snap remove " + packageName);
        return result != null && !result.toLowerCase().contains("error");
    }

    @Override
    public List<String> listSnapPackages(SshDevice device) throws Exception {
        List<String> packages = new ArrayList<>();
        String result = executeCommand(device, "snap list");
        if (result != null) {
            String[] lines = result.split("\n");
            for (int i = 1; i < lines.length; i++) {
                String[] parts = lines[i].split("\\s+");
                if (parts.length > 0) {
                    packages.add(parts[0]);
                }
            }
        }
        return packages;
    }

    @Override
    public boolean isSnapPackageInstalled(SshDevice device, String packageName) throws Exception {
        String result = executeCommand(device, "snap list " + packageName + " 2>/dev/null");
        return result != null && result.contains(packageName);
    }

    @Override
    public boolean configureSnapPath(SshDevice device) throws Exception {
        String snapPath = "/snap/bin";
        String result = executeCommand(device, "test -d " + snapPath + " && echo exists");
        if (result != null && result.contains("exists")) {
            executeCommand(device, "echo 'export PATH=$PATH:" + snapPath + "' >> ~/.bashrc");
            executeCommand(device, "echo 'export PATH=$PATH:" + snapPath + "' >> ~/.profile");
            return true;
        }
        return false;
    }

    @Override
    public boolean refreshSnapPackage(SshDevice device, String packageName) throws Exception {
        String result = executeCommand(device, "snap refresh " + packageName);
        return result != null && !result.toLowerCase().contains("error");
    }

    @Override
    public String getSnapPackageInfo(SshDevice device, String packageName) throws Exception {
        return executeCommand(device, "snap info " + packageName);
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
        return "RHEL8 SnapInstallAutomation";
    }

    @Override
    public String getDescription() {
        return "Snap package management for RHEL8";
    }
}