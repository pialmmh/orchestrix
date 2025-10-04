package com.telcobright.orchestrix.automation.devices.server.linux.servicemanagement.impl;

import com.telcobright.orchestrix.automation.devices.server.linux.servicemanagement.ServiceManagementAutomation;
import com.telcobright.orchestrix.automation.devices.server.linux.base.AbstractLinuxAutomation;
import com.telcobright.orchestrix.automation.devices.server.linux.base.LinuxDistribution;
import com.telcobright.orchestrix.automation.core.device.SshDevice;
import java.util.Map;

public class SysVInitServiceManagement extends AbstractLinuxAutomation implements ServiceManagementAutomation  {

    public SysVInitServiceManagement(boolean useSudo) {
        super(LinuxDistribution.CENTOS_7, useSudo);
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
        String result = executeCommand(device, "service " + serviceName);
        return result != null && !result.toLowerCase().contains("failed");
    }

    @Override
    public boolean stopService(SshDevice device, String serviceName) throws Exception {
        String result = executeCommand(device, "service " + serviceName);
        return result != null && !result.toLowerCase().contains("failed");
    }

    @Override
    public boolean restartService(SshDevice device, String serviceName) throws Exception {
        String result = executeCommand(device, "service " + serviceName);
        return result != null && !result.toLowerCase().contains("failed");
    }

    @Override
    public boolean enableService(SshDevice device, String serviceName) throws Exception {
        String result = executeCommand(device, "chkconfig " + serviceName);
        return result != null && !result.toLowerCase().contains("failed");
    }

    @Override
    public boolean disableService(SshDevice device, String serviceName) throws Exception {
        String result = executeCommand(device, "chkconfig " + serviceName);
        return result != null && !result.toLowerCase().contains("failed");
    }

    @Override
    public boolean isServiceRunning(SshDevice device, String serviceName) throws Exception {
        String result = executeCommand(device, "service " + serviceName);
        return result != null && result.toLowerCase().contains("running");
    }

    @Override
    public boolean isServiceEnabled(SshDevice device, String serviceName) throws Exception {
        String result = executeCommand(device, "chkconfig --list " + serviceName);
        return result != null && result.toLowerCase().contains("enabled");
    }

    @Override
    protected boolean isPackageInstalled(SshDevice device, String packageName) throws Exception {
        return false;
    }

    @Override
    protected String getPackageManagerCommand() {
        return "yum";
    }

    @Override
    public String getName() {
        return "Service Management";
    }

    @Override
    public String getDescription() {
        return "Manage system services using SysV Init";
    }
}