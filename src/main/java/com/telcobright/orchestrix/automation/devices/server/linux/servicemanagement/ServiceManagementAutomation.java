package com.telcobright.orchestrix.automation.devices.server.linux.servicemanagement;

import com.telcobright.orchestrix.automation.devices.server.linux.base.LinuxAutomation;
import com.telcobright.orchestrix.device.SshDevice;

public interface ServiceManagementAutomation extends LinuxAutomation {
    boolean startService(SshDevice device, String serviceName) throws Exception;
    boolean stopService(SshDevice device, String serviceName) throws Exception;
    boolean restartService(SshDevice device, String serviceName) throws Exception;
    boolean enableService(SshDevice device, String serviceName) throws Exception;
    boolean disableService(SshDevice device, String serviceName) throws Exception;
    boolean isServiceRunning(SshDevice device, String serviceName) throws Exception;
    boolean isServiceEnabled(SshDevice device, String serviceName) throws Exception;
}