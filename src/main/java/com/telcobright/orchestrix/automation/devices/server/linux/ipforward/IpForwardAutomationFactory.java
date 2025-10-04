package com.telcobright.orchestrix.automation.devices.server.linux.ipforward;

import com.telcobright.orchestrix.automation.devices.server.linux.base.LinuxDistribution;
import com.telcobright.orchestrix.automation.devices.server.linux.common.SystemDetector;
import com.telcobright.orchestrix.automation.devices.server.linux.ipforward.impl.*;
import com.telcobright.orchestrix.automation.core.device.SshDevice;

public class IpForwardAutomationFactory {

    public static IpForwardAutomation createIpForwardAutomation(LinuxDistribution distribution, boolean useSudo) {
        // Default configuration for simple use case
        return create(null, distribution, useSudo, true, false, true);
    }

    public static IpForwardAutomation createIpForwardAutomation(SshDevice device, boolean useSudo) {
        LinuxDistribution distribution = SystemDetector.detectDistribution(device);
        return createIpForwardAutomation(distribution, useSudo);
    }

    public static IpForwardAutomation create(SshDevice device, LinuxDistribution distribution,
                                     boolean useSudo, boolean enableIpv4,
                                     boolean enableIpv6, boolean makePersistent) {

        LinuxDistribution targetDist = distribution;
        if (targetDist == null || targetDist == LinuxDistribution.UNKNOWN) {
            targetDist = SystemDetector.detectDistribution(device);
        }

        // Return appropriate implementation based on distribution
        String distId = targetDist.getIdentifier();

        if (distId.startsWith("debian-12")) {
            return new Debian12IpForwardAutomation(useSudo, enableIpv4, enableIpv6, makePersistent);
        } else if (distId.startsWith("ubuntu-22")) {
            return new Ubuntu2204IpForwardAutomation(useSudo, enableIpv4, enableIpv6, makePersistent);
        } else if (distId.startsWith("centos-7")) {
            return new CentOS7IpForwardAutomation(useSudo, enableIpv4, enableIpv6, makePersistent);
        } else if (distId.startsWith("rhel-8")) {
            return new RHEL8IpForwardAutomation(useSudo, enableIpv4, enableIpv6, makePersistent);
        } else {
            // Default implementation
            return new DefaultIpForwardAutomation(useSudo, enableIpv4, enableIpv6, makePersistent);
        }
    }
}