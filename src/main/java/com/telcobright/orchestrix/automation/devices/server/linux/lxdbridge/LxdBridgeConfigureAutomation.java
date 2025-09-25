package com.telcobright.orchestrix.automation.devices.server.linux.lxdbridge;

import com.telcobright.orchestrix.automation.devices.server.linux.base.LinuxAutomation;
import com.telcobright.orchestrix.automation.devices.server.linux.lxd.LxdBridgeConfig;
import com.telcobright.orchestrix.device.SshDevice;

/**
 * Unit automation for LXD bridge configuration
 */
public interface LxdBridgeConfigureAutomation extends LinuxAutomation {

    /**
     * Check if LXD is initialized
     */
    boolean isLxdInitialized(SshDevice device) throws Exception;

    /**
     * Initialize LXD with preseed configuration
     */
    boolean initializeLxd(SshDevice device, LxdBridgeConfig config) throws Exception;

    /**
     * Configure LXD bridge network
     */
    boolean configureBridge(SshDevice device, LxdBridgeConfig config) throws Exception;

    /**
     * Check if bridge exists
     */
    boolean bridgeExists(SshDevice device, String bridgeName) throws Exception;

    /**
     * Create LXD bridge
     */
    boolean createBridge(SshDevice device, String bridgeName, String network) throws Exception;

    /**
     * Delete LXD bridge
     */
    boolean deleteBridge(SshDevice device, String bridgeName) throws Exception;

    /**
     * Update bridge configuration
     */
    boolean updateBridge(SshDevice device, String bridgeName, String key, String value) throws Exception;

    /**
     * Get bridge configuration
     */
    String getBridgeConfig(SshDevice device, String bridgeName) throws Exception;

    /**
     * Configure default profile to use bridge
     */
    boolean configureDefaultProfile(SshDevice device, String bridgeName) throws Exception;

    /**
     * Verify bridge is operational
     */
    boolean verifyBridgeOperation(SshDevice device, String bridgeName) throws Exception;
}