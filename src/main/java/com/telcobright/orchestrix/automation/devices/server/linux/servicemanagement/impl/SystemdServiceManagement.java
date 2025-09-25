package com.telcobright.orchestrix.automation.devices.server.linux.servicemanagement.impl;

import com.telcobright.orchestrix.automation.devices.server.linux.servicemanagement.ServiceManagementAutomation;
import com.telcobright.orchestrix.automation.devices.server.linux.base.AbstractLinuxAutomation;
import com.telcobright.orchestrix.automation.devices.server.linux.base.LinuxDistribution;
import com.telcobright.orchestrix.automation.devices.server.linux.base.AbstractLinuxAutomation.DefaultImplementation;
import com.telcobright.orchestrix.device.SshDevice;
import java.util.Map;

public class SystemdServiceManagement extends AbstractLinuxAutomation implements ServiceManagementAutomation, AbstractLinuxAutomation.DefaultImplementation {

    public SystemdServiceManagement(boolean useSudo) {
        super(LinuxDistribution.UNKNOWN, useSudo);
    }

    @Override
    public boolean execute(SshDevice device) throws Exception {
        // Service management is typically invoked for specific services
        return true;
    }

    @Override
    public boolean verify(SshDevice device) throws Exception {
        return true;
    }

    @Override
    public boolean startService(SshDevice device, String serviceName) throws Exception {
        String result = executeCommand(device, "systemctl start " + serviceName);
        return result != null && !result.toLowerCase().contains("failed");
    }

    @Override
    public boolean stopService(SshDevice device, String serviceName) throws Exception {
        String result = executeCommand(device, "systemctl stop " + serviceName);
        return result != null && !result.toLowerCase().contains("failed");
    }

    @Override
    public boolean restartService(SshDevice device, String serviceName) throws Exception {
        String result = executeCommand(device, "systemctl restart " + serviceName);
        return result != null && !result.toLowerCase().contains("failed");
    }

    @Override
    public boolean enableService(SshDevice device, String serviceName) throws Exception {
        String result = executeCommand(device, "systemctl enable " + serviceName);
        return result != null && !result.toLowerCase().contains("failed");
    }

    @Override
    public boolean disableService(SshDevice device, String serviceName) throws Exception {
        String result = executeCommand(device, "systemctl disable " + serviceName);
        return result != null && !result.toLowerCase().contains("failed");
    }

    @Override
    public boolean isServiceRunning(SshDevice device, String serviceName) throws Exception {
        String result = executeCommand(device, "systemctl is-active " + serviceName);
        return result != null && result.toLowerCase().contains("active");
    }

    @Override
    public boolean isServiceEnabled(SshDevice device, String serviceName) throws Exception {
        String result = executeCommand(device, "systemctl is-enabled " + serviceName);
        return result != null && result.toLowerCase().contains("enabled");
    }

    @Override
    protected boolean isPackageInstalled(SshDevice device, String packageName) throws Exception {
        return false;
    }

    @Override
    protected String getPackageManagerCommand() {
        return "apt";
    }

    @Override
    public String getName() {
        return "Service Management";
    }

    @Override
    public String getDescription() {
        return "Manage system services using systemd";
    }
}