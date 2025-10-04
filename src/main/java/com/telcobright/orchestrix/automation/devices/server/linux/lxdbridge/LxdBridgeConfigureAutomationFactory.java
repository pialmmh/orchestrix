package com.telcobright.orchestrix.automation.devices.server.linux.lxdbridge;

import com.telcobright.orchestrix.automation.devices.server.linux.base.LinuxDistribution;
import com.telcobright.orchestrix.automation.devices.server.linux.lxdbridge.impl.*;
import com.telcobright.orchestrix.automation.core.device.SshDevice;
import com.telcobright.orchestrix.automation.devices.server.linux.common.SystemDetector;

public class LxdBridgeConfigureAutomationFactory {

    public static LxdBridgeConfigureAutomation createLxdBridgeConfigureAutomation(LinuxDistribution distribution, boolean useSudo) {
        switch (distribution.getFamily()) {
            case DEBIAN:
                if (distribution == LinuxDistribution.DEBIAN_12) {
                    return new Debian12LxdBridgeConfigureAutomation(useSudo);
                } else if (distribution == LinuxDistribution.UBUNTU_2204) {
                    return new Ubuntu2204LxdBridgeConfigureAutomation(useSudo);
                }
                break;
            case RHEL:
                if (distribution == LinuxDistribution.CENTOS_7) {
                    return new CentOS7LxdBridgeConfigureAutomation(useSudo);
                } else if (distribution == LinuxDistribution.RHEL_8) {
                    return new RHEL8LxdBridgeConfigureAutomation(useSudo);
                }
                break;
        }

        return new DefaultLxdBridgeConfigureAutomation(useSudo);
    }

    public static LxdBridgeConfigureAutomation createLxdBridgeConfigureAutomation(SshDevice device, boolean useSudo) {
        try {
            LinuxDistribution distribution = SystemDetector.detectDistribution(device);
            return createLxdBridgeConfigureAutomation(distribution, useSudo);
        } catch (Exception e) {
            return new DefaultLxdBridgeConfigureAutomation(useSudo);
        }
    }
}
