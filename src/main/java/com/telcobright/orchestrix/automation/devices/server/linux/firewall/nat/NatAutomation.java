package com.telcobright.orchestrix.automation.devices.server.linux.firewall.nat;

import com.telcobright.orchestrix.automation.devices.server.linux.base.LinuxAutomation;
import com.telcobright.orchestrix.automation.core.device.SshDevice;

public interface NatAutomation extends LinuxAutomation {
    boolean enableIpForwarding(SshDevice device) throws Exception;
    boolean disableIpForwarding(SshDevice device) throws Exception;
    boolean isIpForwardingEnabled(SshDevice device) throws Exception;
    boolean saveRules(SshDevice device) throws Exception;
    boolean restoreRules(SshDevice device) throws Exception;
}