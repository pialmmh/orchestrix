package com.telcobright.orchestrix.automation.devices.server.linux.lxd;

import com.telcobright.orchestrix.automation.devices.server.linux.base.LinuxAutomation;
import com.telcobright.orchestrix.device.SshDevice;

/**
 * Interface for LXD bridge setup automation
 * Handles the complete LXD installation and bridge configuration process
 */
public interface LxdBridgeAutomation extends LinuxAutomation {

    /**
     * Check if LXD is installed
     */
    boolean isLxdInstalled(SshDevice device) throws Exception;

    /**
     * Install LXD via snap or package manager
     */
    boolean installLxd(SshDevice device) throws Exception;

    /**
     * Add LXD to PATH if installed via snap
     */
    boolean configureLxdPath(SshDevice device) throws Exception;

    /**
     * Enable IP forwarding for container networking
     */
    boolean enableIpForwarding(SshDevice device) throws Exception;

    /**
     * Configure LXD bridge with specified settings
     */
    boolean configureLxdBridge(SshDevice device, LxdBridgeConfig config) throws Exception;

    /**
     * Initialize LXD with preseed configuration
     */
    boolean initializeLxd(SshDevice device, LxdBridgeConfig config) throws Exception;

    /**
     * Check if LXD is already initialized
     */
    boolean isLxdInitialized(SshDevice device) throws Exception;

    /**
     * Verify bridge is working correctly
     */
    boolean verifyBridge(SshDevice device, String bridgeName) throws Exception;

    /**
     * Configure firewall rules for LXD bridge
     */
    boolean configureFirewallRules(SshDevice device, LxdBridgeConfig config) throws Exception;

    /**
     * Get current LXD network configuration
     */
    String getLxdNetworkConfig(SshDevice device) throws Exception;

    /**
     * Restart LXD service
     */
    boolean restartLxd(SshDevice device) throws Exception;

    /**
     * Complete setup: install, configure path, enable IP forwarding, and configure bridge
     */
    boolean performCompleteSetup(SshDevice device, LxdBridgeConfig config) throws Exception;
}