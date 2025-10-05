package com.telcobright.orchestrix.automation.infrastructure.consul;

import com.telcobright.orchestrix.automation.core.device.CommandExecutor;

/**
 * Factory for creating Consul cluster automation instances
 */
public class ConsulClusterAutomationFactory {

    /**
     * Create Consul cluster automation
     *
     * @param device Command executor (SshDevice or LocalSshDevice)
     * @return ConsulClusterAutomation instance
     */
    public static ConsulClusterAutomation create(CommandExecutor device) {
        return new ConsulClusterAutomation(device);
    }
}
