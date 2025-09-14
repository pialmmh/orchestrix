package com.orchestrix.automation.networking;

import com.orchestrix.automation.core.SshAutomation;
import com.orchestrix.network.entity.SshDevice;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Creates and configures LXC bridge network in bridge mode (no NAT)
 * Perfect for VoIP applications like FusionPBX
 */
public class LxcNetworkCreator extends SshAutomation {

    private static final Logger logger = LoggerFactory.getLogger(LxcNetworkCreator.class);

    private final String lxcInterfaceName;
    private final String hostInterfaceName;
    private final int subnetMaskPrefixLen;
    private final String hostInterfaceIp;
    private final String lxcInternalIp;

    public LxcNetworkCreator(SshDevice sshDevice,
                            String lxcInterfaceName,
                            String hostInterfaceName,
                            int subnetMaskPrefixLen,
                            String hostInterfaceIp,
                            String lxcInternalIp) {
        super(sshDevice);
        this.lxcInterfaceName = lxcInterfaceName;
        this.hostInterfaceName = hostInterfaceName;
        this.subnetMaskPrefixLen = subnetMaskPrefixLen;
        this.hostInterfaceIp = hostInterfaceIp;
        this.lxcInternalIp = lxcInternalIp;
    }

    @Override
    public String getName() {
        return "LXC Network Creator";
    }

    @Override
    public String getDescription() {
        return String.format("Creates LXC bridge network %s in bridge mode (no NAT) with subnet %s/%d",
                lxcInterfaceName, lxcInternalIp, subnetMaskPrefixLen);
    }

    @Override
    public boolean validate() {
        if (!super.validate()) {
            return false;
        }

        // Validate parameters
        if (lxcInterfaceName == null || lxcInterfaceName.isEmpty()) {
            logger.error("LXC interface name is required");
            return false;
        }

        if (lxcInternalIp == null || !isValidIp(lxcInternalIp)) {
            logger.error("Invalid LXC internal IP: {}", lxcInternalIp);
            return false;
        }

        if (subnetMaskPrefixLen < 8 || subnetMaskPrefixLen > 30) {
            logger.error("Invalid subnet mask prefix length: {}", subnetMaskPrefixLen);
            return false;
        }

        // Check if LXD is installed
        if (!commandExists("lxc")) {
            logger.error("LXD is not installed on the target system");
            return false;
        }

        // Check if host interface exists (if specified)
        if (hostInterfaceName != null && !hostInterfaceName.isEmpty()) {
            if (!interfaceExists(hostInterfaceName)) {
                logger.error("Host interface {} does not exist", hostInterfaceName);
                return false;
            }
        }

        return true;
    }

    @Override
    public boolean execute() {
        logger.info("Starting LXC network creation for {}", lxcInterfaceName);

        try {
            // Step 1: Delete existing LXC bridge if it exists
            deleteExistingInterface();

            // Step 2: Create new LXD network in bridge mode (no NAT)
            createLxdNetwork();

            // Step 3: Configure IP forwarding
            configureIpForwarding();

            // Step 4: Configure bridge with host interface (if specified)
            if (hostInterfaceName != null && !hostInterfaceName.isEmpty()) {
                configureBridgeWithHost();
            }

            // Step 5: Verify configuration
            if (!verifyConfiguration()) {
                logger.error("Network configuration verification failed");
                return false;
            }

            logger.info("LXC network {} created successfully", lxcInterfaceName);
            return true;

        } catch (Exception e) {
            logger.error("Failed to create LXC network", e);
            return false;
        }
    }

    /**
     * Delete existing interface with the same name
     */
    private void deleteExistingInterface() {
        logger.info("Checking for existing interface: {}", lxcInterfaceName);

        // Check if it's an LXD network
        String lxdNetworks = executeCommand("lxc network list --format csv", false);
        if (lxdNetworks != null && lxdNetworks.contains(lxcInterfaceName)) {
            logger.info("Deleting existing LXD network: {}", lxcInterfaceName);

            // First, detach from any containers
            String containers = executeCommand("lxc list --format csv", false);
            if (containers != null && !containers.isEmpty()) {
                for (String line : containers.split("\n")) {
                    if (line.trim().isEmpty()) continue;
                    String containerName = line.split(",")[0];
                    executeCommand(String.format("lxc config device remove %s eth0 2>/dev/null || true", containerName), false);
                }
            }

            // Delete the network
            executeCommand("lxc network delete " + lxcInterfaceName + " 2>/dev/null || true", false);
        }

        // Check if it's a Linux bridge
        if (interfaceExists(lxcInterfaceName)) {
            logger.info("Deleting existing Linux bridge: {}", lxcInterfaceName);
            executeSudo("ip link set " + lxcInterfaceName + " down 2>/dev/null || true");
            executeSudo("brctl delbr " + lxcInterfaceName + " 2>/dev/null || true");
        }
    }

    /**
     * Create new LXD network in bridge mode
     */
    private void createLxdNetwork() {
        logger.info("Creating LXD network {} with IP {}/{}", lxcInterfaceName, lxcInternalIp, subnetMaskPrefixLen);

        // Calculate network address from IP and prefix
        String networkAddress = calculateNetworkAddress(lxcInternalIp, subnetMaskPrefixLen);

        // Create network with bridge mode (no NAT)
        String createCommand = String.format(
            "lxc network create %s " +
            "ipv4.address=%s/%d " +
            "ipv4.nat=false " +
            "ipv6.address=none " +
            "dns.mode=none",
            lxcInterfaceName,
            lxcInternalIp,
            subnetMaskPrefixLen
        );

        String result = executeCommand(createCommand);
        logger.info("Network creation result: {}", result);

        // Set additional network properties for VoIP optimization
        executeCommand(String.format("lxc network set %s ipv4.firewall false", lxcInterfaceName));
        executeCommand(String.format("lxc network set %s ipv4.routing true", lxcInterfaceName));
    }

    /**
     * Configure IP forwarding on the host
     */
    private void configureIpForwarding() {
        logger.info("Configuring IP forwarding");

        // Enable IP forwarding
        executeSudo("sysctl -w net.ipv4.ip_forward=1");

        // Make it persistent
        String sysctlConf = executeCommand("grep -q '^net.ipv4.ip_forward' /etc/sysctl.conf && echo 'exists' || echo 'missing'", false);
        if ("missing".equals(sysctlConf.trim())) {
            executeSudo("echo 'net.ipv4.ip_forward=1' >> /etc/sysctl.conf");
        } else {
            executeSudo("sed -i 's/^#*net.ipv4.ip_forward.*/net.ipv4.ip_forward=1/' /etc/sysctl.conf");
        }
    }

    /**
     * Configure bridge with host interface
     */
    private void configureBridgeWithHost() {
        if (hostInterfaceIp != null && !hostInterfaceIp.isEmpty()) {
            logger.info("Configuring routing between {} and host", lxcInterfaceName);

            // Add route for LXC network via bridge
            String networkAddress = calculateNetworkAddress(lxcInternalIp, subnetMaskPrefixLen);
            executeSudo(String.format("ip route add %s/%d dev %s 2>/dev/null || true",
                                     networkAddress, subnetMaskPrefixLen, lxcInterfaceName));

            // If host interface IP is provided, ensure connectivity
            if (isValidIp(hostInterfaceIp)) {
                // Add specific route if needed
                logger.info("Host interface {} has IP {}", hostInterfaceName, hostInterfaceIp);
            }
        }
    }

    /**
     * Verify the network configuration
     */
    private boolean verifyConfiguration() {
        logger.info("Verifying network configuration");

        // Check if network exists
        String networks = executeCommand("lxc network list --format csv");
        if (!networks.contains(lxcInterfaceName)) {
            logger.error("Network {} not found in LXD networks", lxcInterfaceName);
            return false;
        }

        // Check if interface is up
        if (!interfaceExists(lxcInterfaceName)) {
            logger.error("Bridge interface {} does not exist", lxcInterfaceName);
            return false;
        }

        // Check IP configuration
        String actualIp = getInterfaceIp(lxcInterfaceName);
        if (actualIp == null || !actualIp.equals(lxcInternalIp)) {
            logger.error("IP mismatch. Expected: {}, Actual: {}", lxcInternalIp, actualIp);
            return false;
        }

        // Check NAT is disabled
        String natStatus = executeCommand(String.format("lxc network get %s ipv4.nat", lxcInterfaceName));
        if (!"false".equals(natStatus.trim())) {
            logger.warn("NAT is not disabled. Status: {}", natStatus);
        }

        logger.info("Network verification passed");
        return true;
    }

    /**
     * Calculate network address from IP and prefix length
     */
    private String calculateNetworkAddress(String ip, int prefixLen) {
        String[] parts = ip.split("\\.");
        long ipNum = 0;
        for (int i = 0; i < 4; i++) {
            ipNum = (ipNum << 8) + Integer.parseInt(parts[i]);
        }

        long mask = (0xFFFFFFFFL << (32 - prefixLen)) & 0xFFFFFFFFL;
        long network = ipNum & mask;

        return String.format("%d.%d.%d.%d",
                (network >> 24) & 0xFF,
                (network >> 16) & 0xFF,
                (network >> 8) & 0xFF,
                network & 0xFF);
    }

    /**
     * Validate IP address format
     */
    private boolean isValidIp(String ip) {
        if (ip == null || ip.isEmpty()) {
            return false;
        }
        String[] parts = ip.split("\\.");
        if (parts.length != 4) {
            return false;
        }
        try {
            for (String part : parts) {
                int num = Integer.parseInt(part);
                if (num < 0 || num > 255) {
                    return false;
                }
            }
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    @Override
    public boolean supportsRollback() {
        return true;
    }

    @Override
    public boolean rollback() {
        logger.info("Rolling back LXC network configuration");
        try {
            deleteExistingInterface();
            return true;
        } catch (Exception e) {
            logger.error("Failed to rollback", e);
            return false;
        }
    }
}