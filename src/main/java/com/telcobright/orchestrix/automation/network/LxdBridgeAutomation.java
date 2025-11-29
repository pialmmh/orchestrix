package com.telcobright.orchestrix.automation.network;

import com.telcobright.orchestrix.automation.core.device.impl.RemoteSshDevice;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * LXD Bridge Network Automation
 *
 * Reusable automation for managing lxdbr0 bridge configuration:
 * - Create/ensure lxdbr0 exists with proper configuration
 * - Add gateway IP to lxdbr0 (persistent with systemd)
 * - Configure for container networking (NAT disabled for BGP routing)
 *
 * Per networking guideline:
 * - Bridge gateway is .1 of container subnet (e.g., 10.10.199.1)
 * - Bridge should have NAT disabled for BGP-based routing
 * - IPs must persist across reboots using systemd services
 *
 * Usage:
 * <pre>
 * LxdBridgeAutomation bridge = new LxdBridgeAutomation(sshDevice);
 * bridge.configureBridge("10.10.199.0/24");  // Creates lxdbr0 with 10.10.199.1 gateway
 * bridge.addSecondaryIp("10.10.199.20/24");  // Adds Kafka/app IP
 * </pre>
 *
 * @author TelcoBright Orchestrix Team
 * @since 2025-11-29
 */
public class LxdBridgeAutomation {

    private static final Logger log = LoggerFactory.getLogger(LxdBridgeAutomation.class);
    private static final String BRIDGE_NAME = "lxdbr0";

    private final RemoteSshDevice ssh;

    public LxdBridgeAutomation(RemoteSshDevice ssh) {
        this.ssh = ssh;
    }

    /**
     * Configure lxdbr0 bridge with the specified container subnet.
     * Creates the bridge if it doesn't exist, configures gateway IP.
     *
     * @param containerSubnet Container subnet in CIDR notation (e.g., "10.10.199.0/24")
     * @return Gateway IP that was configured (e.g., "10.10.199.1")
     */
    public String configureBridge(String containerSubnet) throws Exception {
        log.info("Configuring {} for subnet: {}", BRIDGE_NAME, containerSubnet);

        // Calculate gateway IP (.1 of subnet)
        String gatewayIp = calculateGatewayIp(containerSubnet);
        String gatewayWithCidr = gatewayIp + "/24";

        // Step 1: Initialize LXD if needed
        log.info("  Initializing LXD...");
        ssh.executeCommand("sudo lxd init --auto 2>/dev/null || echo 'LXD already initialized'");

        // Step 2: Create or configure lxdbr0 via LXD
        log.info("  Creating/configuring lxdbr0 via LXD...");
        ssh.executeCommand("sudo lxc network create " + BRIDGE_NAME + " 2>/dev/null || echo 'Network exists'");

        // Configure LXD network settings
        ssh.executeCommand("sudo lxc network set " + BRIDGE_NAME + " ipv4.address=" + gatewayWithCidr + " 2>/dev/null || true");
        ssh.executeCommand("sudo lxc network set " + BRIDGE_NAME + " ipv4.nat=false 2>/dev/null || true");  // Disable NAT for BGP

        // Step 3: Ensure bridge is UP
        log.info("  Ensuring bridge is UP...");
        ssh.executeCommand("sudo ip link set " + BRIDGE_NAME + " up 2>/dev/null || true");

        // Step 4: Add gateway IP directly to interface (may already exist from LXD)
        log.info("  Adding gateway IP: {}", gatewayWithCidr);
        String addResult = ssh.executeCommand("sudo ip addr add " + gatewayWithCidr + " dev " + BRIDGE_NAME + " 2>&1 || true");

        if (addResult.contains("File exists")) {
            log.info("  Gateway IP already configured");
        }

        // Step 5: Create systemd service for persistence
        createGatewayPersistenceService(gatewayWithCidr);

        // Step 6: Verify configuration
        String bridgeStatus = ssh.executeCommand("ip addr show " + BRIDGE_NAME + " | grep 'inet ' | head -1");
        log.info("  Bridge status: {}", bridgeStatus.trim());

        log.info("  {} configured successfully with gateway {}", BRIDGE_NAME, gatewayIp);
        return gatewayIp;
    }

    /**
     * Add a secondary IP address to lxdbr0 (for containers/apps).
     * Creates systemd service for persistence across reboots.
     *
     * @param ipWithCidr IP address with CIDR (e.g., "10.10.199.20/24")
     */
    public void addSecondaryIp(String ipWithCidr) throws Exception {
        String ipOnly = ipWithCidr.split("/")[0];
        log.info("Adding secondary IP {} to {}", ipWithCidr, BRIDGE_NAME);

        // Check if bridge exists
        String bridgeCheck = ssh.executeCommand("ip link show " + BRIDGE_NAME + " 2>&1");
        if (bridgeCheck.contains("does not exist")) {
            throw new IllegalStateException(BRIDGE_NAME + " does not exist. Call configureBridge() first.");
        }

        // Add IP immediately
        String addResult = ssh.executeCommand("sudo ip addr add " + ipWithCidr + " dev " + BRIDGE_NAME + " 2>&1 || true");
        if (addResult.contains("File exists")) {
            log.info("  IP {} already configured", ipOnly);
        } else {
            log.info("  IP {} added", ipOnly);
        }

        // Create persistence service
        createSecondaryIpPersistenceService(ipWithCidr);

        // Verify
        boolean exists = verifyIpExists(ipOnly);
        if (!exists) {
            throw new IllegalStateException("Failed to add IP " + ipOnly + " to " + BRIDGE_NAME);
        }

        log.info("  Secondary IP {} configured and persistent", ipOnly);
    }

    /**
     * Verify if an IP address exists on lxdbr0.
     *
     * @param ipAddress IP address without CIDR
     * @return true if IP exists on bridge
     */
    public boolean verifyIpExists(String ipAddress) throws Exception {
        String output = ssh.executeCommand("ip addr show " + BRIDGE_NAME + " | grep 'inet '");
        return output.contains(ipAddress);
    }

    /**
     * Verify bridge exists and is properly configured.
     *
     * @return true if bridge is ready for use
     */
    public boolean verifyBridgeReady() throws Exception {
        String output = ssh.executeCommand("ip addr show " + BRIDGE_NAME + " 2>&1");

        if (output.contains("does not exist")) {
            log.warn("{} does not exist", BRIDGE_NAME);
            return false;
        }

        if (!output.contains("inet ")) {
            log.warn("{} exists but has no IP configured", BRIDGE_NAME);
            return false;
        }

        return true;
    }

    /**
     * Get all IPs configured on lxdbr0.
     *
     * @return List of configured IPs
     */
    public String getConfiguredIps() throws Exception {
        return ssh.executeCommand("ip addr show " + BRIDGE_NAME + " | grep 'inet ' | awk '{print $2}'");
    }

    /**
     * Remove an IP address from lxdbr0 and its persistence service.
     *
     * @param ipWithCidr IP address with CIDR
     */
    public void removeIp(String ipWithCidr) throws Exception {
        String ipOnly = ipWithCidr.split("/")[0];
        String serviceName = BRIDGE_NAME + "-" + ipOnly.replace(".", "-") + "-ip.service";

        log.info("Removing IP {} from {}", ipOnly, BRIDGE_NAME);

        // Remove from interface
        ssh.executeCommand("sudo ip addr del " + ipWithCidr + " dev " + BRIDGE_NAME + " 2>/dev/null || true");

        // Stop and disable systemd service
        ssh.executeCommand("sudo systemctl stop " + serviceName + " 2>/dev/null || true");
        ssh.executeCommand("sudo systemctl disable " + serviceName + " 2>/dev/null || true");
        ssh.executeCommand("sudo rm -f /etc/systemd/system/" + serviceName);
        ssh.executeCommand("sudo systemctl daemon-reload");

        log.info("  IP {} removed", ipOnly);
    }

    /**
     * Create systemd service to persist gateway IP across reboots.
     */
    private void createGatewayPersistenceService(String gatewayWithCidr) throws Exception {
        String ipOnly = gatewayWithCidr.split("/")[0];
        String serviceName = BRIDGE_NAME + "-gateway";
        String serviceFile = "/etc/systemd/system/" + serviceName + "-ip.service";

        log.info("  Creating persistence service: {}-ip.service", serviceName);

        String serviceContent = String.format(
            "[Unit]\n" +
            "Description=Configure %s gateway IP %s\n" +
            "After=network-online.target lxd.service snap.lxd.daemon.service\n" +
            "Wants=network-online.target\n" +
            "\n" +
            "[Service]\n" +
            "Type=oneshot\n" +
            "ExecStartPre=/bin/sh -c 'for i in $(seq 1 60); do ip link show %s > /dev/null 2>&1 && break || sleep 1; done'\n" +
            "ExecStart=/bin/sh -c 'ip addr add %s dev %s 2>&1 || true'\n" +
            "ExecStop=/sbin/ip addr del %s dev %s 2>/dev/null || true\n" +
            "RemainAfterExit=yes\n" +
            "\n" +
            "[Install]\n" +
            "WantedBy=multi-user.target\n",
            BRIDGE_NAME, gatewayWithCidr,
            BRIDGE_NAME,
            gatewayWithCidr, BRIDGE_NAME,
            gatewayWithCidr, BRIDGE_NAME
        );

        // Write service file
        ssh.executeCommand("cat << 'EOF' | sudo tee " + serviceFile + " > /dev/null\n" + serviceContent + "EOF");

        // Enable service
        ssh.executeCommand("sudo systemctl daemon-reload");
        ssh.executeCommand("sudo systemctl enable " + serviceName + "-ip.service 2>/dev/null || true");
    }

    /**
     * Create systemd service to persist secondary IP across reboots.
     */
    private void createSecondaryIpPersistenceService(String ipWithCidr) throws Exception {
        String ipOnly = ipWithCidr.split("/")[0];
        String serviceName = BRIDGE_NAME + "-" + ipOnly.replace(".", "-");
        String serviceFile = "/etc/systemd/system/" + serviceName + "-ip.service";

        log.info("  Creating persistence service: {}-ip.service", serviceName);

        String serviceContent = String.format(
            "[Unit]\n" +
            "Description=Add secondary IP %s to %s\n" +
            "After=network-online.target lxd.service snap.lxd.daemon.service %s-gateway-ip.service\n" +
            "Wants=network-online.target\n" +
            "\n" +
            "[Service]\n" +
            "Type=oneshot\n" +
            "ExecStartPre=/bin/sh -c 'for i in $(seq 1 60); do ip link show %s > /dev/null 2>&1 && break || sleep 1; done'\n" +
            "ExecStart=/bin/sh -c 'ip addr add %s dev %s 2>&1 || true'\n" +
            "ExecStop=/sbin/ip addr del %s dev %s 2>/dev/null || true\n" +
            "RemainAfterExit=yes\n" +
            "\n" +
            "[Install]\n" +
            "WantedBy=multi-user.target\n",
            ipWithCidr, BRIDGE_NAME,
            BRIDGE_NAME,
            BRIDGE_NAME,
            ipWithCidr, BRIDGE_NAME,
            ipWithCidr, BRIDGE_NAME
        );

        // Write service file
        ssh.executeCommand("cat << 'EOF' | sudo tee " + serviceFile + " > /dev/null\n" + serviceContent + "EOF");

        // Enable service
        ssh.executeCommand("sudo systemctl daemon-reload");
        ssh.executeCommand("sudo systemctl enable " + serviceName + "-ip.service 2>/dev/null || true");
    }

    /**
     * Calculate gateway IP from container subnet.
     * Gateway is always .1 of the subnet.
     *
     * @param containerSubnet Subnet in CIDR notation (e.g., "10.10.199.0/24")
     * @return Gateway IP (e.g., "10.10.199.1")
     */
    private String calculateGatewayIp(String containerSubnet) {
        // Remove CIDR suffix
        String subnetBase = containerSubnet.split("/")[0];
        // Replace .0 at the end with .1
        return subnetBase.substring(0, subnetBase.lastIndexOf('.')) + ".1";
    }
}
