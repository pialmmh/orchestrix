package com.telcobright.orchestrix.automation.devices.server.linux.lxdinstall;

import com.telcobright.orchestrix.automation.devices.server.linux.base.LinuxDistribution;
import com.telcobright.orchestrix.automation.devices.server.linux.lxdinstall.impl.*;
import com.telcobright.orchestrix.automation.core.device.SshDevice;
import com.telcobright.orchestrix.automation.devices.server.linux.common.SystemDetector;

public class LxdInstallerAutomationFactory {

    public static LxdInstallerAutomation createLxdInstallerAutomation(LinuxDistribution distribution, boolean useSudo) {
        switch (distribution.getFamily()) {
            case DEBIAN:
                if (distribution == LinuxDistribution.DEBIAN_12) {
                    return new Debian12LxdInstallerAutomation(useSudo);
                } else if (distribution == LinuxDistribution.UBUNTU_2204) {
                    return new Ubuntu2204LxdInstallerAutomation(useSudo);
                }
                break;
            case RHEL:
                if (distribution == LinuxDistribution.CENTOS_7) {
                    return new CentOS7LxdInstallerAutomation(useSudo);
                } else if (distribution == LinuxDistribution.RHEL_8) {
                    return new RHEL8LxdInstallerAutomation(useSudo);
                }
                break;
        }

        return new DefaultLxdInstallerAutomation(useSudo);
    }

    public static LxdInstallerAutomation createLxdInstallerAutomation(SshDevice device, boolean useSudo) {
        try {
            LinuxDistribution distribution = SystemDetector.detectDistribution(device);
            return createLxdInstallerAutomation(distribution, useSudo);
        } catch (Exception e) {
            return new DefaultLxdInstallerAutomation(useSudo);
        }
    }
}
