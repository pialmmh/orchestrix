package com.telcobright.orchestrix.automation.devices.server.linux.kernelparams;

import com.telcobright.orchestrix.automation.devices.server.linux.base.LinuxAutomation;
import com.telcobright.orchestrix.automation.core.device.SshDevice;
import java.util.Map;

public interface KernelParamsAutomation extends LinuxAutomation {
    boolean setSysctl(SshDevice device, String key, String value) throws Exception;
    String getSysctl(SshDevice device, String key) throws Exception;
    boolean loadModule(SshDevice device, String moduleName) throws Exception;
    boolean unloadModule(SshDevice device, String moduleName) throws Exception;
    boolean blacklistModule(SshDevice device, String moduleName) throws Exception;
    boolean makeSysctlPersistent(SshDevice device, String key, String value) throws Exception;
    Map<String, String> getAllSysctl(SshDevice device) throws Exception;
    boolean isModuleLoaded(SshDevice device, String moduleName) throws Exception;
}