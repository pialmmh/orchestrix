package com.telcobright.orchestrix.automation.devices.server.linux.snapinstall;

import com.telcobright.orchestrix.automation.devices.server.linux.base.LinuxDistribution;
import com.telcobright.orchestrix.automation.devices.server.linux.snapinstall.impl.*;
import com.telcobright.orchestrix.automation.core.device.SshDevice;
import com.telcobright.orchestrix.automation.devices.server.linux.common.SystemDetector;

public class SnapInstallAutomationFactory {

    public static SnapInstallAutomation createSnapInstallAutomation(LinuxDistribution distribution, boolean useSudo) {
        switch (distribution.getFamily()) {
            case DEBIAN:
                if (distribution == LinuxDistribution.DEBIAN_12) {
                    return new Debian12SnapInstallAutomation(useSudo);
                } else if (distribution == LinuxDistribution.UBUNTU_2204) {
                    return new Ubuntu2204SnapInstallAutomation(useSudo);
                }
                break;
            case RHEL:
                if (distribution == LinuxDistribution.CENTOS_7) {
                    return new CentOS7SnapInstallAutomation(useSudo);
                } else if (distribution == LinuxDistribution.RHEL_8) {
                    return new RHEL8SnapInstallAutomation(useSudo);
                }
                break;
        }

        return new DefaultSnapInstallAutomation(useSudo);
    }

    public static SnapInstallAutomation createSnapInstallAutomation(SshDevice device, boolean useSudo) {
        try {
            LinuxDistribution distribution = SystemDetector.detectDistribution(device);
            return createSnapInstallAutomation(distribution, useSudo);
        } catch (Exception e) {
            return new DefaultSnapInstallAutomation(useSudo);
        }
    }
}
