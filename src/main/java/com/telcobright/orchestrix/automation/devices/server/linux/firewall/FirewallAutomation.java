package com.telcobright.orchestrix.automation.devices.server.linux.firewall;

import com.telcobright.orchestrix.automation.devices.server.linux.base.LinuxAutomation;
import com.telcobright.orchestrix.automation.core.device.SshDevice;
import java.util.List;

public interface FirewallAutomation extends LinuxAutomation {
    boolean enableFirewall(SshDevice device) throws Exception;
    boolean disableFirewall(SshDevice device) throws Exception;
    boolean addRule(SshDevice device, String rule) throws Exception;
    boolean removeRule(SshDevice device, String rule) throws Exception;
    boolean addPort(SshDevice device, int port, String protocol) throws Exception;
    boolean removePort(SshDevice device, int port, String protocol) throws Exception;
    List<String> listRules(SshDevice device) throws Exception;
    boolean isEnabled(SshDevice device) throws Exception;
}