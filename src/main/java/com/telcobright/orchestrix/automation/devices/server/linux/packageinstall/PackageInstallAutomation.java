package com.telcobright.orchestrix.automation.devices.server.linux.packageinstall;

import com.telcobright.orchestrix.automation.devices.server.linux.base.LinuxAutomation;
import com.telcobright.orchestrix.automation.core.device.SshDevice;
import java.util.List;

public interface PackageInstallAutomation extends LinuxAutomation {
    boolean installPackage(SshDevice device, String packageName) throws Exception;
    boolean installPackages(SshDevice device, List<String> packageNames) throws Exception;
    boolean removePackage(SshDevice device, String packageName) throws Exception;
    boolean updatePackage(SshDevice device, String packageName) throws Exception;
    boolean updateSystem(SshDevice device) throws Exception;
    boolean isPackageInstalled(SshDevice device, String packageName) throws Exception;
    String getPackageVersion(SshDevice device, String packageName) throws Exception;
}