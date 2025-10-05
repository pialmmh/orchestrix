package com.telcobright.orchestrix.automation.infrastructure.consul;

import com.telcobright.orchestrix.automation.core.device.SshDevice;

/**
 * Factory for creating Consul cluster automation instances
 */
public class ConsulClusterAutomationFactory {

    /**
     * Create Consul cluster automation
     *
     * @param device SSH device to execute commands
     * @return ConsulClusterAutomation instance
     */
    public static ConsulClusterAutomation create(SshDevice device) {
        return new ConsulClusterAutomation(device);
    }
}
