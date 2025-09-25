package com.telcobright.orchestrix.automation.devices.server.platform;

import com.telcobright.orchestrix.device.SshDevice;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Reusable automation class for configuring LXD bridge networking
 * Sets up LXD bridge with custom IP range and NAT/masquerade rules
 */
public class LxdBridgeNetworkingAutomation {

    private static final Logger logger = Logger.getLogger(LxdBridgeNetworkingAutomation.class.getName());

    private final SshDevice sshDevice;
    private final String bridgeName;
    private final String hostGatewayIp;
    private final String networkCidr;
    private final String dhcpRangeStart;
    private final String dhcpRangeEnd;
    private final boolean enableNat;
    private final boolean enableDns;
    private final boolean useSudo;
    private String lxcCommand = null;
    private String sudoPrefix = "";

    /**
     * Configuration builder for LXD bridge networking
     */
    public static class Config {
        private String bridgeName = "lxdbr0";
        private String hostGatewayIp = "10.10.199.1";
        private String networkCidr = "10.10.199.0/24";
        private String dhcpRangeStart = "10.10.199.50";
        private String dhcpRangeEnd = "10.10.199.254";
        private boolean enableNat = true;
        private boolean enableDns = true;
        private boolean useSudo = true;

        public Config bridgeName(String name) {
            this.bridgeName = name;
            return this;
        }

        public Config hostGatewayIp(String ip) {
            this.hostGatewayIp = ip;
            return this;
        }

        public Config networkCidr(String cidr) {
            this.networkCidr = cidr;
            return this;
        }

        public Config dhcpRange(String start, String end) {
            this.dhcpRangeStart = start;
            this.dhcpRangeEnd = end;
            return this;
        }

        public Config enableNat(boolean enable) {
            this.enableNat = enable;
            return this;
        }

        public Config enableDns(boolean enable) {
            this.enableDns = enable;
            return this;
        }

        public Config useSudo(boolean use) {
            this.useSudo = use;
            return this;
        }

        public LxdBridgeNetworkingAutomation build(SshDevice device) {
            return new LxdBridgeNetworkingAutomation(device, this);
        }
    }

    private LxdBridgeNetworkingAutomation(SshDevice device, Config config) {
        this.sshDevice = device;
        this.bridgeName = config.bridgeName;
        this.hostGatewayIp = config.hostGatewayIp;
        this.networkCidr = config.networkCidr;
        this.dhcpRangeStart = config.dhcpRangeStart;
        this.dhcpRangeEnd = config.dhcpRangeEnd;
        this.enableNat = config.enableNat;
        this.enableDns = config.enableDns;
        this.useSudo = config.useSudo;
        this.sudoPrefix = useSudo ? "sudo " : "";
    }

    /**
     * Get the correct lxc command path
     */
    private String getLxcCommand() throws Exception {
        if (lxcCommand != null) {
            return lxcCommand;
        }

        // Check for lxc in PATH
        String result = sshDevice.sendAndReceive("which lxc 2>/dev/null").get();
        if (result != null && !result.isEmpty()) {
            lxcCommand = sudoPrefix + "lxc";
            return lxcCommand;
        }

        // Check for snap installation
        result = sshDevice.sendAndReceive("test -f /snap/bin/lxc && echo found").get();
        if ("found".equals(result.trim())) {
            lxcCommand = sudoPrefix + "/snap/bin/lxc";
            return lxcCommand;
        }

        // Default to lxc and hope it works
        lxcCommand = sudoPrefix + "lxc";
        return lxcCommand;
    }

    /**
     * Configure LXD bridge networking
     * @return true if configuration successful
     */
    public boolean configure() {
        try {
            logger.info("Starting LXD bridge configuration for: " + bridgeName);

            // Check if LXD is installed
            if (!isLxdInstalled()) {
                logger.severe("LXD is not installed on the target system");
                return false;
            }

            // Get the correct lxc command path
            getLxcCommand();
            logger.info("Using lxc command: " + lxcCommand);

            // Check if bridge already exists
            if (bridgeExists()) {
                logger.info("Bridge " + bridgeName + " already exists, updating configuration");
                return updateBridgeConfiguration();
            } else {
                logger.info("Creating new bridge: " + bridgeName);
                return createBridgeConfiguration();
            }

        } catch (Exception e) {
            logger.severe("Failed to configure LXD bridge: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Check if LXD is installed
     */
    private boolean isLxdInstalled() throws Exception {
        // Try multiple ways to check if LXD is installed
        // 1. Check for lxc command in PATH
        String result = sshDevice.sendAndReceive("which lxc 2>/dev/null").get();
        if (result != null && !result.isEmpty()) {
            return true;
        }

        // 2. Check for lxd command in PATH
        result = sshDevice.sendAndReceive("which lxd 2>/dev/null").get();
        if (result != null && !result.isEmpty()) {
            return true;
        }

        // 3. Check for snap installation of LXD
        result = sshDevice.sendAndReceive("test -f /snap/bin/lxc && echo found").get();
        if ("found".equals(result.trim())) {
            logger.info("LXD found at /snap/bin/lxc");
            return true;
        }

        // 4. Try to list networks (if this works, LXD is installed)
        result = sshDevice.sendAndReceive("lxc network list 2>/dev/null || /snap/bin/lxc network list 2>/dev/null").get();
        if (result != null && !result.contains("command not found") && !result.isEmpty()) {
            return true;
        }

        return false;
    }

    /**
     * Check if bridge already exists
     */
    private boolean bridgeExists() throws Exception {
        String result = sshDevice.sendAndReceive(getLxcCommand() + " network list --format json").get();
        return result != null && result.contains("\"" + bridgeName + "\"");
    }

    /**
     * Create new bridge configuration
     */
    private boolean createBridgeConfiguration() throws Exception {
        logger.info("Creating bridge " + bridgeName + " with IP " + hostGatewayIp);

        // Create bridge with configuration
        StringBuilder createCmd = new StringBuilder();
        createCmd.append(getLxcCommand()).append(" network create ").append(bridgeName);
        createCmd.append(" ipv4.address=").append(hostGatewayIp).append("/24");
        createCmd.append(" ipv4.nat=").append(enableNat ? "true" : "false");

        if (enableDns) {
            createCmd.append(" dns.mode=managed");
        }

        String result = sshDevice.sendAndReceive(createCmd.toString()).get();

        if (result.contains("error") || result.contains("Error")) {
            logger.severe("Failed to create bridge: " + result);
            return false;
        }

        // Configure DHCP range
        configureDhcpRange();

        // Configure NAT/masquerade if enabled
        if (enableNat) {
            configureNatMasquerade();
        }

        // Set as default network for new containers
        setDefaultNetwork();

        logger.info("Bridge " + bridgeName + " created successfully");
        return true;
    }

    /**
     * Update existing bridge configuration
     */
    private boolean updateBridgeConfiguration() throws Exception {
        logger.info("Updating bridge " + bridgeName + " configuration");

        // Update IP address
        String setIpCmd = String.format("%s network set %s ipv4.address %s/24",
            getLxcCommand(), bridgeName, hostGatewayIp);
        sshDevice.sendAndReceive(setIpCmd).get();

        // Update NAT setting
        String setNatCmd = String.format("%s network set %s ipv4.nat %s",
            getLxcCommand(), bridgeName, enableNat ? "true" : "false");
        sshDevice.sendAndReceive(setNatCmd).get();

        // Configure DHCP range
        configureDhcpRange();

        // Configure NAT/masquerade if enabled
        if (enableNat) {
            configureNatMasquerade();
        }

        logger.info("Bridge " + bridgeName + " updated successfully");
        return true;
    }

    /**
     * Configure DHCP range for the bridge
     */
    private void configureDhcpRange() throws Exception {
        logger.info("Configuring DHCP range: " + dhcpRangeStart + " - " + dhcpRangeEnd);

        String dhcpCmd = String.format("%s network set %s ipv4.dhcp.ranges %s-%s",
            getLxcCommand(), bridgeName, dhcpRangeStart, dhcpRangeEnd);
        sshDevice.sendAndReceive(dhcpCmd).get();
    }

    /**
     * Configure NAT/masquerade rules using iptables
     */
    private void configureNatMasquerade() throws Exception {
        logger.info("Configuring NAT/masquerade for " + networkCidr);

        // Get primary network interface
        String primaryInterface = getPrimaryNetworkInterface();

        if (primaryInterface == null || primaryInterface.isEmpty()) {
            logger.warning("Could not determine primary network interface, using eth0");
            primaryInterface = "eth0";
        }

        // Enable IP forwarding
        sshDevice.sendAndReceive(sudoPrefix + "sysctl -w net.ipv4.ip_forward=1").get();
        sshDevice.sendAndReceive("echo 'net.ipv4.ip_forward=1' | " + sudoPrefix + "tee -a /etc/sysctl.conf").get();

        // Add iptables rules for NAT
        String iptablesCmd = String.format(
            "%siptables -t nat -A POSTROUTING -s %s ! -d %s -j MASQUERADE",
            sudoPrefix, networkCidr, networkCidr
        );

        // Check if rule already exists
        String checkCmd = String.format(
            "%siptables -t nat -C POSTROUTING -s %s ! -d %s -j MASQUERADE 2>/dev/null",
            sudoPrefix, networkCidr, networkCidr
        );

        String checkResult = sshDevice.sendAndReceive(checkCmd).get();
        if (checkResult == null || !checkResult.contains("exists")) {
            sshDevice.sendAndReceive(iptablesCmd).get();
            logger.info("NAT masquerade rule added");
        } else {
            logger.info("NAT masquerade rule already exists");
        }

        // Allow forwarding from bridge
        String forwardCmd = String.format(
            "%siptables -A FORWARD -i %s -j ACCEPT", sudoPrefix, bridgeName
        );

        String checkForwardCmd = String.format(
            "%siptables -C FORWARD -i %s -j ACCEPT 2>/dev/null", sudoPrefix, bridgeName
        );

        checkResult = sshDevice.sendAndReceive(checkForwardCmd).get();
        if (checkResult == null || !checkResult.contains("exists")) {
            sshDevice.sendAndReceive(forwardCmd).get();
        }

        // Save iptables rules (Ubuntu/Debian)
        saveIptablesRules();
    }

    /**
     * Get primary network interface
     */
    private String getPrimaryNetworkInterface() throws Exception {
        // Try to get default route interface
        String result = sshDevice.sendAndReceive(
            "ip route | grep default | awk '{print $5}' | head -1"
        ).get();

        if (result != null && !result.trim().isEmpty()) {
            return result.trim();
        }

        // Fallback: get first non-loopback interface
        result = sshDevice.sendAndReceive(
            "ip link show | grep -E '^[0-9]+:' | grep -v lo | head -1 | cut -d: -f2 | tr -d ' '"
        ).get();

        return result != null ? result.trim() : null;
    }

    /**
     * Save iptables rules persistently
     */
    private void saveIptablesRules() throws Exception {
        // Check if iptables-persistent is installed
        String checkCmd = "dpkg -l | grep iptables-persistent";
        String result = sshDevice.sendAndReceive(checkCmd).get();

        if (result == null || result.isEmpty()) {
            logger.info("Installing iptables-persistent");
            // Use debconf to pre-answer the questions
            sshDevice.sendAndReceive("echo iptables-persistent iptables-persistent/autosave_v4 boolean true | " + sudoPrefix + "debconf-set-selections").get();
            sshDevice.sendAndReceive("echo iptables-persistent iptables-persistent/autosave_v6 boolean true | " + sudoPrefix + "debconf-set-selections").get();
            sshDevice.sendAndReceive("DEBIAN_FRONTEND=noninteractive " + sudoPrefix + "apt-get install -y iptables-persistent").get();
        }

        // Save current rules
        sshDevice.sendAndReceive(sudoPrefix + "sh -c 'iptables-save > /etc/iptables/rules.v4'").get();
        sshDevice.sendAndReceive(sudoPrefix + "sh -c 'ip6tables-save > /etc/iptables/rules.v6'").get();
        logger.info("Iptables rules saved");
    }

    /**
     * Set this bridge as default for new containers
     */
    private void setDefaultNetwork() throws Exception {
        String setDefaultCmd = String.format(
            "%s profile device set default eth0 network=%s", getLxcCommand(), bridgeName
        );
        sshDevice.sendAndReceive(setDefaultCmd).get();
        logger.info("Set " + bridgeName + " as default network for new containers");
    }

    /**
     * Verify the configuration
     */
    public boolean verify() {
        try {
            logger.info("Verifying LXD bridge configuration");

            // Check bridge exists
            String listResult = sshDevice.sendAndReceive(getLxcCommand() + " network show " + bridgeName).get();
            if (listResult == null || listResult.contains("error")) {
                logger.severe("Bridge " + bridgeName + " not found");
                return false;
            }

            // Check IP configuration
            if (!listResult.contains(hostGatewayIp)) {
                logger.warning("Bridge IP does not match expected: " + hostGatewayIp);
            }

            // Check NAT rules
            if (enableNat) {
                String natCheck = sshDevice.sendAndReceive(
                    sudoPrefix + "iptables -t nat -L POSTROUTING -n | grep " + networkCidr
                ).get();
                if (natCheck == null || natCheck.isEmpty()) {
                    logger.warning("NAT rules not found for " + networkCidr);
                    return false;
                }
            }

            // Check IP forwarding
            String ipForward = sshDevice.sendAndReceive("sysctl net.ipv4.ip_forward").get();
            if (!ipForward.contains("= 1")) {
                logger.warning("IP forwarding is not enabled");
                return false;
            }

            logger.info("LXD bridge configuration verified successfully");
            return true;

        } catch (Exception e) {
            logger.severe("Failed to verify configuration: " + e.getMessage());
            return false;
        }
    }

    /**
     * Get current bridge configuration as a map
     */
    public Map<String, String> getCurrentConfiguration() {
        Map<String, String> config = new HashMap<>();

        try {
            String result = sshDevice.sendAndReceive(getLxcCommand() + " network show " + bridgeName).get();

            // Parse YAML output
            String[] lines = result.split("\n");
            for (String line : lines) {
                if (line.contains("ipv4.address:")) {
                    config.put("ipv4.address", line.split(":")[1].trim());
                } else if (line.contains("ipv4.nat:")) {
                    config.put("ipv4.nat", line.split(":")[1].trim());
                } else if (line.contains("ipv4.dhcp.ranges:")) {
                    config.put("ipv4.dhcp.ranges", line.split(":")[1].trim());
                }
            }

        } catch (Exception e) {
            logger.severe("Failed to get current configuration: " + e.getMessage());
        }

        return config;
    }

    /**
     * Remove bridge configuration (cleanup)
     */
    public boolean remove() {
        try {
            logger.info("Removing bridge " + bridgeName);

            // Remove bridge
            String removeCmd = getLxcCommand() + " network delete " + bridgeName;
            String result = sshDevice.sendAndReceive(removeCmd).get();

            if (result.contains("error")) {
                logger.severe("Failed to remove bridge: " + result);
                return false;
            }

            // Remove iptables rules
            if (enableNat) {
                String removeNatCmd = String.format(
                    "iptables -t nat -D POSTROUTING -s %s ! -d %s -j MASQUERADE",
                    networkCidr, networkCidr
                );
                sshDevice.sendAndReceive(removeNatCmd).get();

                String removeForwardCmd = String.format(
                    "iptables -D FORWARD -i %s -j ACCEPT", bridgeName
                );
                sshDevice.sendAndReceive(removeForwardCmd).get();

                saveIptablesRules();
            }

            logger.info("Bridge " + bridgeName + " removed successfully");
            return true;

        } catch (Exception e) {
            logger.severe("Failed to remove bridge: " + e.getMessage());
            return false;
        }
    }
}