package com.telcobright.orchestrix.automation.devices.server.linux.firewall.nat.masquerade;

import com.telcobright.orchestrix.automation.devices.server.linux.firewall.nat.NatAutomation;
import com.telcobright.orchestrix.device.SshDevice;
import java.util.List;

public interface MasqueradeAutomation extends NatAutomation {

    // Simple common use cases
    boolean enableMasqueradeForInterface(SshDevice device, String outInterface) throws Exception;
    boolean enableMasqueradeFromSource(SshDevice device, String sourceIp, String outInterface) throws Exception;
    boolean enableMasqueradeFromSourceInterface(SshDevice device, String sourceInterface, String outInterface) throws Exception;

    // Advanced rule-based operations
    boolean addMasqueradeRule(SshDevice device, MasqueradeRule rule) throws Exception;
    boolean removeMasqueradeRule(SshDevice device, MasqueradeRule rule) throws Exception;

    // Management operations
    List<MasqueradeRule> listMasqueradeRules(SshDevice device) throws Exception;
    boolean clearAllMasqueradeRules(SshDevice device) throws Exception;
    boolean verifyMasqueradeRule(SshDevice device, MasqueradeRule rule) throws Exception;

    // Check if masquerading is active
    boolean isMasqueradeEnabled(SshDevice device) throws Exception;
}