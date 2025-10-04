package com.telcobright.orchestrix.automation.devices.server.linux.firewall.nat.masquerade.impl;

import com.telcobright.orchestrix.automation.devices.server.linux.base.AbstractLinuxAutomation;
import com.telcobright.orchestrix.automation.devices.server.linux.base.LinuxDistribution;
import com.telcobright.orchestrix.automation.devices.server.linux.firewall.nat.masquerade.MasqueradeAutomation;
import com.telcobright.orchestrix.automation.devices.server.linux.firewall.nat.masquerade.MasqueradeRule;
import com.telcobright.orchestrix.automation.core.device.SshDevice;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class IptablesMasqueradeAutomation extends AbstractLinuxAutomation implements MasqueradeAutomation {

    public IptablesMasqueradeAutomation(boolean useSudo) {
        super(LinuxDistribution.UNKNOWN, useSudo);
    }

    @Override
    public boolean execute(SshDevice device) throws Exception {
        return enableIpForwarding(device);
    }

    @Override
    public boolean verify(SshDevice device) throws Exception {
        return isIpForwardingEnabled(device) && isMasqueradeEnabled(device);
    }

    @Override
    public boolean enableIpForwarding(SshDevice device) throws Exception {
        String result = executeCommand(device, "echo 1 > /proc/sys/net/ipv4/ip_forward");
        if (result != null) {
            executeCommand(device, "sysctl -w net.ipv4.ip_forward=1");
            return true;
        }
        return false;
    }

    @Override
    public boolean disableIpForwarding(SshDevice device) throws Exception {
        String result = executeCommand(device, "echo 0 > /proc/sys/net/ipv4/ip_forward");
        if (result != null) {
            executeCommand(device, "sysctl -w net.ipv4.ip_forward=0");
            return true;
        }
        return false;
    }

    @Override
    public boolean isIpForwardingEnabled(SshDevice device) throws Exception {
        String result = executeCommand(device, "cat /proc/sys/net/ipv4/ip_forward");
        return result != null && result.trim().equals("1");
    }

    @Override
    public boolean saveRules(SshDevice device) throws Exception {
        String result = executeCommand(device, "iptables-save > /etc/iptables/rules.v4");
        if (result == null) {
            // Try alternative location
            result = executeCommand(device, "iptables-save > /etc/sysconfig/iptables");
        }
        return result != null;
    }

    @Override
    public boolean restoreRules(SshDevice device) throws Exception {
        String result = executeCommand(device, "iptables-restore < /etc/iptables/rules.v4");
        if (result == null) {
            // Try alternative location
            result = executeCommand(device, "iptables-restore < /etc/sysconfig/iptables");
        }
        return result != null;
    }

    @Override
    public boolean enableMasqueradeForInterface(SshDevice device, String outInterface) throws Exception {
        String cmd = String.format("iptables -t nat -A POSTROUTING -o %s -j MASQUERADE", outInterface);
        String result = executeCommand(device, cmd);
        return result != null && !result.toLowerCase().contains("error");
    }

    @Override
    public boolean enableMasqueradeFromSource(SshDevice device, String sourceIp, String outInterface) throws Exception {
        String cmd = String.format("iptables -t nat -A POSTROUTING -s %s -o %s -j MASQUERADE", sourceIp, outInterface);
        String result = executeCommand(device, cmd);
        return result != null && !result.toLowerCase().contains("error");
    }

    @Override
    public boolean enableMasqueradeFromSourceInterface(SshDevice device, String sourceInterface, String outInterface) throws Exception {
        String cmd = String.format("iptables -t nat -A POSTROUTING -i %s -o %s -j MASQUERADE", sourceInterface, outInterface);
        String result = executeCommand(device, cmd);
        return result != null && !result.toLowerCase().contains("error");
    }

    @Override
    public boolean addMasqueradeRule(SshDevice device, MasqueradeRule rule) throws Exception {
        String cmd = rule.toIptablesRule();
        String result = executeCommand(device, cmd);
        boolean success = result != null && !result.toLowerCase().contains("error");

        if (success && rule.isPersistent()) {
            saveRules(device);
        }

        return success;
    }

    @Override
    public boolean removeMasqueradeRule(SshDevice device, MasqueradeRule rule) throws Exception {
        // Convert ADD to DELETE
        String cmd = rule.toIptablesRule().replace("-A POSTROUTING", "-D POSTROUTING");
        String result = executeCommand(device, cmd);
        boolean success = result != null && !result.toLowerCase().contains("error");

        if (success && rule.isPersistent()) {
            saveRules(device);
        }

        return success;
    }

    @Override
    public List<MasqueradeRule> listMasqueradeRules(SshDevice device) throws Exception {
        List<MasqueradeRule> rules = new ArrayList<>();
        String result = executeCommand(device, "iptables -t nat -L POSTROUTING -v -n --line-numbers");

        if (result != null) {
            String[] lines = result.split("\n");
            for (String line : lines) {
                if (line.contains("MASQUERADE")) {
                    // Parse the rule line to extract components
                    MasqueradeRule.Builder builder = new MasqueradeRule.Builder();

                    // Extract source IP
                    Pattern srcPattern = Pattern.compile("\\s+(\\d+\\.\\d+\\.\\d+\\.\\d+/\\d+)\\s+");
                    Matcher srcMatcher = srcPattern.matcher(line);
                    if (srcMatcher.find()) {
                        builder.sourceIp(srcMatcher.group(1));
                    }

                    // Note: Full parsing would require more complex regex patterns
                    // This is a simplified version for basic rules

                    rules.add(builder.build());
                }
            }
        }

        return rules;
    }

    @Override
    public boolean clearAllMasqueradeRules(SshDevice device) throws Exception {
        // Clear all MASQUERADE rules from POSTROUTING chain
        String listCmd = "iptables -t nat -L POSTROUTING --line-numbers -n | grep MASQUERADE | awk '{print $1}' | sort -r";
        String lineNumbers = executeCommand(device, listCmd);

        if (lineNumbers != null && !lineNumbers.trim().isEmpty()) {
            String[] lines = lineNumbers.split("\n");
            for (String lineNum : lines) {
                if (!lineNum.trim().isEmpty()) {
                    executeCommand(device, "iptables -t nat -D POSTROUTING " + lineNum.trim());
                }
            }
        }

        return true;
    }

    @Override
    public boolean verifyMasqueradeRule(SshDevice device, MasqueradeRule rule) throws Exception {
        String result = executeCommand(device, "iptables -t nat -L POSTROUTING -v -n");
        if (result != null) {
            // Check if the rule components exist in the output
            boolean hasRule = true;

            if (rule.getSourceIp() != null) {
                hasRule = hasRule && result.contains(rule.getSourceIp());
            }
            if (rule.getOutInterface() != null) {
                hasRule = hasRule && result.contains(rule.getOutInterface());
            }

            return hasRule && result.contains("MASQUERADE");
        }
        return false;
    }

    @Override
    public boolean isMasqueradeEnabled(SshDevice device) throws Exception {
        String result = executeCommand(device, "iptables -t nat -L POSTROUTING -v -n");
        return result != null && result.contains("MASQUERADE");
    }

    @Override
    protected boolean isPackageInstalled(SshDevice device, String packageName) throws Exception {
        String result = executeCommand(device, "which iptables");
        return result != null && !result.isEmpty();
    }

    @Override
    protected String getPackageManagerCommand() {
        return "apt";
    }

    @Override
    public String getName() {
        return "Iptables Masquerade Automation";
    }

    @Override
    public String getDescription() {
        return "Configure NAT masquerading using iptables";
    }
}