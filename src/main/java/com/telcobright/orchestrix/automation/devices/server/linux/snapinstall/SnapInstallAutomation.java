package com.telcobright.orchestrix.automation.devices.server.linux.snapinstall;

import com.telcobright.orchestrix.automation.devices.server.linux.base.LinuxAutomation;
import com.telcobright.orchestrix.automation.core.device.SshDevice;
import java.util.List;

/**
 * Unit automation for Snap package management
 */
public interface SnapInstallAutomation extends LinuxAutomation {

    /**
     * Check if snapd is installed
     */
    boolean isSnapInstalled(SshDevice device) throws Exception;

    /**
     * Install snapd package manager
     */
    boolean installSnapd(SshDevice device) throws Exception;

    /**
     * Install a snap package
     */
    boolean installSnapPackage(SshDevice device, String packageName) throws Exception;

    /**
     * Install a snap package with specific channel
     */
    boolean installSnapPackage(SshDevice device, String packageName, String channel) throws Exception;

    /**
     * Remove a snap package
     */
    boolean removeSnapPackage(SshDevice device, String packageName) throws Exception;

    /**
     * List installed snap packages
     */
    List<String> listSnapPackages(SshDevice device) throws Exception;

    /**
     * Check if a snap package is installed
     */
    boolean isSnapPackageInstalled(SshDevice device, String packageName) throws Exception;

    /**
     * Configure snap bin in PATH
     */
    boolean configureSnapPath(SshDevice device) throws Exception;

    /**
     * Refresh/update a snap package
     */
    boolean refreshSnapPackage(SshDevice device, String packageName) throws Exception;

    /**
     * Get snap package info
     */
    String getSnapPackageInfo(SshDevice device, String packageName) throws Exception;
}