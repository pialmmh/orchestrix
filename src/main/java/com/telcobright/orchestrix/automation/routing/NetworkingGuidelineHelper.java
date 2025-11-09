package com.telcobright.orchestrix.automation.routing;

/**
 * Utility class for calculating network parameters according to TelcoBright
 * Container Networking Guideline.
 *
 * Reference: /home/mustafa/telcobright-projects/orchestrix/images/networking_guideline_claude.md
 *
 * Standard patterns:
 * - Overlay IP:        10.9.9.N (where N is host number)
 * - Container Subnet:  10.10.(200-N).0/24
 * - BGP AS Number:     65(200-N)
 * - VPN Client IPs:    10.9.9.254 down to 10.9.9.245 (developer range)
 * - VPN Client Range:  10.9.9.245/28 (covers 10.9.9.240-255)
 *
 * Example for 3-node cluster:
 * - Node 1: 10.9.9.1, AS 65199, 10.10.199.0/24
 * - Node 2: 10.9.9.2, AS 65198, 10.10.198.0/24
 * - Node 3: 10.9.9.3, AS 65197, 10.10.197.0/24
 *
 * @author TelcoBright Orchestrix Team
 * @since 2025-11-10
 */
public class NetworkingGuidelineHelper {

    // Constants per guideline
    private static final String OVERLAY_NETWORK_PREFIX = "10.9.9.";
    private static final String CONTAINER_NETWORK_PREFIX = "10.10.";
    private static final int BASE_AS_NUMBER = 65200;
    private static final int CONTAINER_SUBNET_BASE = 200;
    private static final String VPN_CLIENT_RANGE = "10.9.9.245/28";
    private static final int VPN_CLIENT_START_IP = 254;
    private static final int VPN_CLIENT_END_IP = 245;
    private static final int OVERLAY_LISTEN_PORT = 51820;

    /**
     * Calculates the overlay IP for a given host number.
     * Pattern: 10.9.9.N
     *
     * @param hostNumber The host number (1, 2, 3, ...)
     * @return The overlay IP address (e.g., "10.9.9.1")
     * @throws IllegalArgumentException if hostNumber is invalid
     */
    public static String calculateOverlayIp(int hostNumber) {
        validateHostNumber(hostNumber);
        return OVERLAY_NETWORK_PREFIX + hostNumber;
    }

    /**
     * Calculates the overlay IP with CIDR notation for interface configuration.
     * Pattern: 10.9.9.N/24
     *
     * @param hostNumber The host number (1, 2, 3, ...)
     * @return The overlay IP with CIDR (e.g., "10.9.9.1/24")
     */
    public static String calculateOverlayIpWithCidr(int hostNumber) {
        return calculateOverlayIp(hostNumber) + "/24";
    }

    /**
     * Calculates the container subnet for a given host number.
     * Pattern: 10.10.(200-N).0/24
     *
     * @param hostNumber The host number (1, 2, 3, ...)
     * @return The container subnet (e.g., "10.10.199.0/24")
     * @throws IllegalArgumentException if hostNumber is invalid
     */
    public static String calculateContainerSubnet(int hostNumber) {
        validateHostNumber(hostNumber);
        int subnetNumber = CONTAINER_SUBNET_BASE - hostNumber;
        return CONTAINER_NETWORK_PREFIX + subnetNumber + ".0/24";
    }

    /**
     * Calculates the BGP AS number for a given host number.
     * Pattern: 65(200-N)
     *
     * @param hostNumber The host number (1, 2, 3, ...)
     * @return The BGP AS number (e.g., 65199 for host 1)
     * @throws IllegalArgumentException if hostNumber is invalid
     */
    public static int calculateBgpAsNumber(int hostNumber) {
        validateHostNumber(hostNumber);
        return BASE_AS_NUMBER - hostNumber;
    }

    /**
     * Calculates a VPN client IP address based on client number.
     * Pattern: 10.9.9.(254 down to 245)
     *
     * @param clientNumber The client number (1-10)
     * @return The VPN client IP (e.g., "10.9.9.254" for client 1)
     * @throws IllegalArgumentException if clientNumber is invalid
     */
    public static String calculateVpnClientIp(int clientNumber) {
        if (clientNumber < 1 || clientNumber > 10) {
            throw new IllegalArgumentException("Client number must be between 1 and 10, got: " + clientNumber);
        }
        int ipLastOctet = VPN_CLIENT_START_IP - (clientNumber - 1);
        return OVERLAY_NETWORK_PREFIX + ipLastOctet;
    }

    /**
     * Gets the VPN client IP range in CIDR notation.
     * This covers IPs from 10.9.9.240 to 10.9.9.255
     *
     * @return VPN client range "10.9.9.245/28"
     */
    public static String getVpnClientRange() {
        return VPN_CLIENT_RANGE;
    }

    /**
     * Gets the standard WireGuard overlay listen port.
     *
     * @return Port number 51820
     */
    public static int getOverlayListenPort() {
        return OVERLAY_LISTEN_PORT;
    }

    /**
     * Validates that a host number is within acceptable range.
     * Host numbers should be between 1 and 199 to avoid subnet conflicts.
     *
     * @param hostNumber The host number to validate
     * @throws IllegalArgumentException if host number is invalid
     */
    private static void validateHostNumber(int hostNumber) {
        if (hostNumber < 1) {
            throw new IllegalArgumentException("Host number must be >= 1, got: " + hostNumber);
        }
        if (hostNumber > 199) {
            throw new IllegalArgumentException(
                "Host number must be <= 199 to avoid subnet conflicts (10.10.1.0/24 min), got: " + hostNumber
            );
        }
    }

    /**
     * Creates a node configuration record with all calculated values.
     *
     * @param hostNumber The host number
     * @param hostname Hostname for the node
     * @param managementIp Management network IP address
     * @param sshPort SSH port for management connection
     * @return NodeNetworkConfig with all calculated network parameters
     */
    public static NodeNetworkConfig createNodeConfig(int hostNumber, String hostname,
                                                      String managementIp, int sshPort) {
        return new NodeNetworkConfig(
            hostNumber,
            hostname,
            managementIp,
            sshPort,
            calculateOverlayIp(hostNumber),
            calculateOverlayIpWithCidr(hostNumber),
            calculateContainerSubnet(hostNumber),
            calculateBgpAsNumber(hostNumber),
            OVERLAY_LISTEN_PORT
        );
    }

    /**
     * Record class representing a node's network configuration per guideline.
     */
    public static record NodeNetworkConfig(
        int hostNumber,
        String hostname,
        String managementIp,
        int sshPort,
        String overlayIp,           // e.g., "10.9.9.1"
        String overlayIpWithCidr,   // e.g., "10.9.9.1/24"
        String containerSubnet,     // e.g., "10.10.199.0/24"
        int bgpAsNumber,            // e.g., 65199
        int overlayPort             // 51820
    ) {
        /**
         * Returns the overlay IP (same as overlayIp, for compatibility).
         * This is used as the BGP router ID.
         */
        public String getRouterId() {
            return overlayIp;
        }

        /**
         * Formats node information as string for logging/debugging.
         */
        @Override
        public String toString() {
            return String.format(
                "NodeNetworkConfig[host=%d, hostname=%s, mgmt=%s:%d, overlay=%s, subnet=%s, as=%d]",
                hostNumber, hostname, managementIp, sshPort, overlayIp, containerSubnet, bgpAsNumber
            );
        }
    }

    // Example usage and validation
    public static void main(String[] args) {
        System.out.println("=== Networking Guideline Helper - Examples ===\n");

        // Example 1: 3-node cluster (like BDCOM)
        System.out.println("Example 1: 3-Node Cluster");
        for (int i = 1; i <= 3; i++) {
            NodeNetworkConfig node = createNodeConfig(i, "node" + i, "10.255.246.17" + (2+i), 22);
            System.out.println(node);
        }

        System.out.println("\nExample 2: VPN Client IPs");
        for (int i = 1; i <= 10; i++) {
            System.out.println("Client " + i + ": " + calculateVpnClientIp(i));
        }

        System.out.println("\nExample 3: VPN Client Range");
        System.out.println("VPN Client Range for AllowedIPs: " + getVpnClientRange());

        System.out.println("\nExample 4: Manual Calculations");
        System.out.println("Host 1 Overlay: " + calculateOverlayIp(1));
        System.out.println("Host 1 Subnet:  " + calculateContainerSubnet(1));
        System.out.println("Host 1 BGP AS:  " + calculateBgpAsNumber(1));
    }
}
