package com.telcobright.orchestrix.automation.cluster.kafkadocker;

import com.telcobright.orchestrix.automation.core.device.impl.RemoteSshDevice;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Network Configuration Automation
 *
 * Atomic automation for managing network interface configuration:
 * - Add/remove secondary IPs to bridge interfaces
 * - Verify bridge existence
 * - Check IP configuration
 *
 * Reusable across all tenants and deployment types.
 *
 * Usage:
 * <pre>
 * NetworkConfigAutomation netConfig = new NetworkConfigAutomation(sshDevice);
 * netConfig.addSecondaryIp("lxdbr0", "10.10.199.20/24");
 * boolean exists = netConfig.verifyIpExists("lxdbr0", "10.10.199.20");
 * </pre>
 *
 * @author TelcoBright Orchestrix Team
 * @since 2025-11-16
 */
public class NetworkConfigAutomation {

    private static final Logger log = LoggerFactory.getLogger(NetworkConfigAutomation.class);
    private final RemoteSshDevice ssh;

    public NetworkConfigAutomation(RemoteSshDevice ssh) {
        this.ssh = ssh;
    }

    /**
     * Verify bridge interface exists
     *
     * @param bridgeName Bridge interface name (e.g., "lxdbr0")
     * @return true if bridge exists
     */
    public boolean verifyBridgeExists(String bridgeName) throws Exception {
        log.debug("Verifying bridge: {}", bridgeName);

        String output = ssh.executeCommand("ip addr show " + bridgeName + " 2>&1");

        if (output.contains("does not exist")) {
            log.error("Bridge {} does not exist", bridgeName);
            return false;
        }

        if (!output.contains("inet")) {
            log.error("Bridge {} exists but has no IP configuration", bridgeName);
            return false;
        }

        log.debug("Bridge {} verified successfully", bridgeName);
        return true;
    }

    /**
     * Add secondary IP address to bridge interface (PERSISTENT)
     *
     * Idempotent - does not fail if IP already exists.
     * Creates a systemd service to persist the IP across reboots.
     *
     * @param bridgeName Bridge interface name (e.g., "lxdbr0")
     * @param ipWithCidr IP address with CIDR notation (e.g., "10.10.199.20/24")
     */
    public void addSecondaryIp(String bridgeName, String ipWithCidr) throws Exception {
        log.info("Adding secondary IP {} to bridge {} (persistent)", ipWithCidr, bridgeName);

        // First verify bridge exists
        if (!verifyBridgeExists(bridgeName)) {
            throw new IllegalStateException("Bridge " + bridgeName + " does not exist");
        }

        // Add IP immediately (will fail silently if already exists)
        String addCommand = String.format("sudo ip addr add %s dev %s 2>&1 || echo 'IP already exists (OK)'",
                ipWithCidr, bridgeName);
        String output = ssh.executeCommand(addCommand);

        if (output.contains("IP already exists") || output.contains("File exists")) {
            log.info("Secondary IP {} already configured on {}", ipWithCidr, bridgeName);
        } else {
            log.info("Secondary IP {} added to {}", ipWithCidr, bridgeName);
        }

        // Make persistent using systemd service
        makeSecondaryIpPersistent(bridgeName, ipWithCidr);
    }

    /**
     * Create systemd service to persist secondary IP across reboots
     */
    private void makeSecondaryIpPersistent(String bridgeName, String ipWithCidr) throws Exception {
        // Extract IP without CIDR for service name (e.g., 10.10.199.20)
        String ipOnly = ipWithCidr.split("/")[0];
        String serviceName = String.format("%s-%s", bridgeName, ipOnly.replace(".", "-"));

        log.info("Creating persistent systemd service: {}", serviceName);

        // Create systemd service file content
        String serviceContent = String.format(
            "[Unit]\\n" +
            "Description=Add secondary IP %s to %s\\n" +
            "After=network-online.target lxd.service snap.lxd.daemon.service\\n" +
            "Wants=network-online.target\\n" +
            "\\n" +
            "[Service]\\n" +
            "Type=oneshot\\n" +
            "ExecStart=/sbin/ip addr add %s dev %s\\n" +
            "ExecStop=/sbin/ip addr del %s dev %s\\n" +
            "RemainAfterExit=yes\\n" +
            "\\n" +
            "[Install]\\n" +
            "WantedBy=multi-user.target\\n",
            ipWithCidr, bridgeName,
            ipWithCidr, bridgeName,
            ipWithCidr, bridgeName
        );

        // Write service file
        String serviceFile = String.format("/etc/systemd/system/%s-ip.service", serviceName);
        String writeCommand = String.format(
            "echo -e '%s' | sudo tee %s > /dev/null",
            serviceContent, serviceFile
        );
        ssh.executeCommand(writeCommand);

        // Reload systemd and enable service
        ssh.executeCommand("sudo systemctl daemon-reload");
        ssh.executeCommand(String.format("sudo systemctl enable %s-ip.service 2>/dev/null || true", serviceName));

        log.info("Persistent service created: {}-ip.service", serviceName);
    }

    /**
     * Verify IP address exists on bridge interface
     *
     * @param bridgeName Bridge interface name
     * @param ipAddress IP address (without CIDR)
     * @return true if IP exists on bridge
     */
    public boolean verifyIpExists(String bridgeName, String ipAddress) throws Exception {
        log.debug("Verifying IP {} exists on bridge {}", ipAddress, bridgeName);

        String output = ssh.executeCommand("ip addr show " + bridgeName + " | grep 'inet '");

        boolean exists = output.contains(ipAddress);
        if (exists) {
            log.debug("IP {} verified on bridge {}", ipAddress, bridgeName);
        } else {
            log.warn("IP {} NOT found on bridge {}", ipAddress, bridgeName);
        }

        return exists;
    }

    /**
     * Get all IP addresses configured on bridge
     *
     * @param bridgeName Bridge interface name
     * @return IP configuration output
     */
    public String getIpConfiguration(String bridgeName) throws Exception {
        log.debug("Getting IP configuration for bridge: {}", bridgeName);
        return ssh.executeCommand("ip addr show " + bridgeName + " | grep 'inet '");
    }

    /**
     * Remove secondary IP address from bridge
     *
     * @param bridgeName Bridge interface name
     * @param ipWithCidr IP address with CIDR notation
     */
    public void removeSecondaryIp(String bridgeName, String ipWithCidr) throws Exception {
        log.info("Removing secondary IP {} from bridge {}", ipWithCidr, bridgeName);

        String command = String.format("sudo ip addr del %s dev %s 2>&1 || echo 'IP not found (OK)'",
                ipWithCidr, bridgeName);

        String output = ssh.executeCommand(command);

        if (output.contains("IP not found") || output.contains("Cannot assign")) {
            log.info("Secondary IP {} was not configured on {}", ipWithCidr, bridgeName);
        } else {
            log.info("Secondary IP {} removed from {}", ipWithCidr, bridgeName);
        }
    }

    /**
     * Verify network connectivity to IP address
     *
     * @param targetIp Target IP to ping
     * @param timeoutSeconds Timeout in seconds
     * @return true if reachable
     */
    public boolean verifyConnectivity(String targetIp, int timeoutSeconds) throws Exception {
        log.debug("Testing connectivity to: {}", targetIp);

        String command = String.format("ping -c 1 -W %d %s > /dev/null 2>&1 && echo 'OK' || echo 'FAILED'",
                timeoutSeconds, targetIp);

        String output = ssh.executeCommand(command);
        boolean reachable = output.trim().equals("OK");

        if (reachable) {
            log.debug("Connectivity to {} verified", targetIp);
        } else {
            log.warn("Connectivity to {} FAILED", targetIp);
        }

        return reachable;
    }

    /**
     * Check if port is listening on IP address
     *
     * @param ip IP address
     * @param port Port number
     * @return true if port is listening
     */
    public boolean verifyPortListening(String ip, int port) throws Exception {
        log.debug("Checking if {}:{} is listening", ip, port);

        String command = String.format("nc -zv %s %d 2>&1", ip, port);
        String output = ssh.executeCommand(command);

        boolean listening = output.contains("succeeded") || output.contains("open");

        if (listening) {
            log.debug("Port {}:{} is listening", ip, port);
        } else {
            log.warn("Port {}:{} is NOT listening", ip, port);
        }

        return listening;
    }
}
