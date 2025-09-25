package com.telcobright.orchestrix.automation.devices.server.linux.ipforward;

import com.telcobright.orchestrix.automation.devices.server.linux.base.LinuxAutomation;
import com.telcobright.orchestrix.device.SshDevice;

/**
 * Interface for IP forwarding automation
 */
public interface IpForwardAutomation extends LinuxAutomation {

    /**
     * Enable IPv4 forwarding
     */
    boolean enableIpv4(SshDevice device) throws Exception;

    /**
     * Enable IPv6 forwarding
     */
    boolean enableIpv6(SshDevice device) throws Exception;

    /**
     * Disable IPv4 forwarding
     */
    boolean disableIpv4(SshDevice device) throws Exception;

    /**
     * Disable IPv6 forwarding
     */
    boolean disableIpv6(SshDevice device) throws Exception;

    /**
     * Make IP forwarding configuration persistent
     */
    boolean makePersistent(SshDevice device) throws Exception;

    /**
     * Check if IPv4 forwarding is enabled
     */
    boolean isIpv4Enabled(SshDevice device) throws Exception;

    /**
     * Check if IPv6 forwarding is enabled
     */
    boolean isIpv6Enabled(SshDevice device) throws Exception;
}