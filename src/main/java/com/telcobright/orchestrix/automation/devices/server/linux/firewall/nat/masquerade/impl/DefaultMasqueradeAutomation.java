package com.telcobright.orchestrix.automation.devices.server.linux.firewall.nat.masquerade.impl;

import com.telcobright.orchestrix.automation.devices.server.linux.base.AbstractLinuxAutomation;
import com.telcobright.orchestrix.automation.devices.server.linux.base.LinuxDistribution;

public class DefaultMasqueradeAutomation extends IptablesMasqueradeAutomation implements AbstractLinuxAutomation.DefaultImplementation {

    public DefaultMasqueradeAutomation(boolean useSudo) {
        super(useSudo);
    }

    @Override
    public String getName() {
        return "Default Masquerade Automation";
    }

    @Override
    public String getDescription() {
        return "Default NAT masquerading implementation using iptables for unlisted Linux distributions";
    }

    @Override
    public boolean isCompatible(LinuxDistribution distribution) {
        return true;
    }
}