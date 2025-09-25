package com.telcobright.orchestrix.automation.devices.server.linux.ipforward.impl;

import com.telcobright.orchestrix.automation.devices.server.linux.ipforward.AbstractIpForwardAutomation;
import com.telcobright.orchestrix.automation.devices.server.linux.base.LinuxDistribution;
import com.telcobright.orchestrix.device.SshDevice;
import java.util.Map;
import java.util.HashMap;

public class Debian12IpForwardAutomation extends AbstractIpForwardAutomation {

    public Debian12IpForwardAutomation(boolean useSudo, boolean enableIpv4, boolean enableIpv6, boolean makePersistent) {
        super(LinuxDistribution.DEBIAN_12, useSudo, enableIpv4, enableIpv6, makePersistent);
    }

    @Override
    public boolean enableIpv4(SshDevice device) throws Exception {
        logger.info("Enabling IPv4 forwarding on " + getSupportedDistribution());
        String result = executeCommand(device, "sysctl -w net.ipv4.ip_forward=1");
        return result != null && result.contains("net.ipv4.ip_forward = 1");
    }

    @Override
    public boolean enableIpv6(SshDevice device) throws Exception {
        logger.info("Enabling IPv6 forwarding on " + getSupportedDistribution());
        String result = executeCommand(device, "sysctl -w net.ipv6.conf.all.forwarding=1");
        return result != null && result.contains("net.ipv6.conf.all.forwarding = 1");
    }

    @Override
    public boolean disableIpv4(SshDevice device) throws Exception {
        String result = executeCommand(device, "sysctl -w net.ipv4.ip_forward=0");
        return result != null && result.contains("net.ipv4.ip_forward = 0");
    }

    @Override
    public boolean disableIpv6(SshDevice device) throws Exception {
        String result = executeCommand(device, "sysctl -w net.ipv6.conf.all.forwarding=0");
        return result != null && result.contains("net.ipv6.conf.all.forwarding = 0");
    }

    @Override
    public boolean makePersistent(SshDevice device) throws Exception {
        // Implementation specific to Debian12
        String config = "net.ipv4.ip_forward=1\n";
        if (enableIpv6) {
            config += "net.ipv6.conf.all.forwarding=1\n";
        }

        executeCommand(device, "echo '" + config + "' | tee /etc/sysctl.d/99-ip-forward.conf");
        executeCommand(device, "sysctl --system");
        return true;
    }

    @Override
    public boolean isIpv4Enabled(SshDevice device) throws Exception {
        String result = executeCommand(device, "sysctl net.ipv4.ip_forward");
        return result != null && result.contains("= 1");
    }

    @Override
    public boolean isIpv6Enabled(SshDevice device) throws Exception {
        String result = executeCommand(device, "sysctl net.ipv6.conf.all.forwarding");
        return result != null && result.contains("= 1");
    }

    @Override
    public boolean verify(SshDevice device) throws Exception {
        boolean verified = true;

        if (enableIpv4) {
            verified = isIpv4Enabled(device) && verified;
        }

        if (enableIpv6) {
            verified = isIpv6Enabled(device) && verified;
        }

        return verified;
    }
}