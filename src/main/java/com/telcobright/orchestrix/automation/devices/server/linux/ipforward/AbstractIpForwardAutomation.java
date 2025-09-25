package com.telcobright.orchestrix.automation.devices.server.linux.ipforward;

import com.telcobright.orchestrix.automation.devices.server.linux.base.AbstractLinuxAutomation;
import com.telcobright.orchestrix.automation.devices.server.linux.base.LinuxDistribution;
import com.telcobright.orchestrix.device.SshDevice;
import java.util.Map;
import java.util.HashMap;

public abstract class AbstractIpForwardAutomation extends AbstractLinuxAutomation implements IpForwardAutomation {

    protected final boolean enableIpv4;
    protected final boolean enableIpv6;
    protected final boolean makePersistent;

    protected AbstractIpForwardAutomation(LinuxDistribution distribution, boolean useSudo,
                         boolean enableIpv4, boolean enableIpv6, boolean makePersistent) {
        super(distribution, useSudo);
        this.enableIpv4 = enableIpv4;
        this.enableIpv6 = enableIpv6;
        this.makePersistent = makePersistent;
    }

    @Override
    public boolean execute(SshDevice device) throws Exception {
        boolean success = true;

        if (enableIpv4) {
            success = enableIpv4(device) && success;
        }

        if (enableIpv6) {
            success = enableIpv6(device) && success;
        }

        if (makePersistent && success) {
            success = makePersistent(device) && success;
        }

        return success;
    }

    @Override
    public String getName() {
        return "IP Forwarding";
    }

    @Override
    public String getDescription() {
        return "Configure IP forwarding for packet routing";
    }

    @Override
    protected boolean isPackageInstalled(SshDevice device, String packageName) throws Exception {
        // Default implementation - override in specific classes
        return false;
    }

    @Override
    protected String getPackageManagerCommand() {
        // Default implementation - override in specific classes
        return "apt";
    }
}