package com.telcobright.orchestrix.automation.devices.server.linux.lxdinstall;

import com.telcobright.orchestrix.automation.devices.server.linux.base.LinuxAutomation;
import com.telcobright.orchestrix.automation.core.device.SshDevice;

/**
 * Unit automation for LXD installation
 */
public interface LxdInstallerAutomation extends LinuxAutomation {

    /**
     * Check if LXD is already installed
     */
    boolean isLxdInstalled(SshDevice device) throws Exception;

    /**
     * Install LXD using the appropriate method for the distribution
     */
    boolean installLxd(SshDevice device) throws Exception;

    /**
     * Add user to LXD group
     */
    boolean addUserToLxdGroup(SshDevice device, String username) throws Exception;

    /**
     * Get LXD version
     */
    String getLxdVersion(SshDevice device) throws Exception;

    /**
     * Check if LXD service is running
     */
    boolean isLxdServiceRunning(SshDevice device) throws Exception;

    /**
     * Start LXD service
     */
    boolean startLxdService(SshDevice device) throws Exception;
}