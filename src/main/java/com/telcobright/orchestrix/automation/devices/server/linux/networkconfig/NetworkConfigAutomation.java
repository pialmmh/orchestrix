package com.telcobright.orchestrix.automation.devices.server.linux.networkconfig;

import com.telcobright.orchestrix.automation.devices.server.linux.base.LinuxAutomation;
import com.telcobright.orchestrix.device.SshDevice;
import java.util.Map;

public interface NetworkConfigAutomation extends LinuxAutomation {
    boolean configureStaticIp(SshDevice device, String interfaceName, String ipAddress,
                             String netmask, String gateway) throws Exception;
    boolean configureDhcp(SshDevice device, String interfaceName) throws Exception;
    boolean addDns(SshDevice device, String dnsServer) throws Exception;
    boolean setHostname(SshDevice device, String hostname) throws Exception;
    Map<String, String> getNetworkInfo(SshDevice device) throws Exception;
    boolean restartNetworking(SshDevice device) throws Exception;
}