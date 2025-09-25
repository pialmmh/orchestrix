package com.telcobright.orchestrix.automation.devices.server.linux.firewall.nat.masquerade;

import com.telcobright.orchestrix.automation.devices.server.linux.base.LinuxDistribution;
import com.telcobright.orchestrix.automation.devices.server.linux.firewall.nat.masquerade.impl.DefaultMasqueradeAutomation;
import com.telcobright.orchestrix.automation.devices.server.linux.firewall.nat.masquerade.impl.IptablesMasqueradeAutomation;
import com.telcobright.orchestrix.device.SshDevice;

public class MasqueradeAutomationFactory {

    public static MasqueradeAutomation createMasqueradeAutomation(LinuxDistribution distribution, boolean useSudo) {
        // Currently all distributions use iptables for masquerading
        // Future: Add nftables support for newer distributions

        switch (distribution.getFamily()) {
            case DEBIAN:
                if (distribution == LinuxDistribution.DEBIAN_12 ||
                    distribution == LinuxDistribution.DEBIAN_11 ||
                    distribution == LinuxDistribution.UBUNTU_2204 ||
                    distribution == LinuxDistribution.UBUNTU_2004) {
                    return new IptablesMasqueradeAutomation(useSudo);
                }
                break;
            case RHEL:
                if (distribution == LinuxDistribution.CENTOS_7 ||
                    distribution == LinuxDistribution.RHEL_8 ||
                    distribution == LinuxDistribution.RHEL_9 ||
                    distribution == LinuxDistribution.ROCKY_8 ||
                    distribution == LinuxDistribution.ROCKY_9) {
                    return new IptablesMasqueradeAutomation(useSudo);
                }
                break;
            case ARCH:
                return new IptablesMasqueradeAutomation(useSudo);
            case ALPINE:
                return new IptablesMasqueradeAutomation(useSudo);
        }

        // Default implementation for unlisted distributions
        return new DefaultMasqueradeAutomation(useSudo);
    }

    public static MasqueradeAutomation createMasqueradeAutomation(SshDevice device, boolean useSudo) {
        LinuxDistribution distribution = detectDistribution(device);
        return createMasqueradeAutomation(distribution, useSudo);
    }

    private static LinuxDistribution detectDistribution(SshDevice device) {
        // This would use SystemDetector to identify the distribution
        // For now, return UNKNOWN to use default implementation
        return LinuxDistribution.UNKNOWN;
    }
}