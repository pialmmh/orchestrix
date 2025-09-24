package com.telcobright.orchestrix.images.lxc.uniqueidgenerator.scripts;

import com.telcobright.orchestrix.images.lxc.uniqueidgenerator.entities.UniqueIdConfig;

/**
 * Network setup scripts for Unique ID Generator container
 * This task is retryable independently
 */
public class NetworkSetup {

    private static final String VALIDATE_NETWORK_SCRIPT = """
        #!/bin/bash
        # Validate network configuration

        CONTAINER_IP="%s"
        GATEWAY_IP="%s"
        BRIDGE="%s"

        # Check if bridge exists
        if ! lxc network show "$BRIDGE" &>/dev/null; then
            echo "ERROR: Bridge $BRIDGE does not exist"
            exit 1
        fi

        # Validate IP is in required subnet
        if [[ ! "$CONTAINER_IP" =~ ^10\\.10\\.199\\. ]]; then
            echo "ERROR: Container IP must be in 10.10.199.0/24 subnet"
            exit 1
        fi

        # Validate /24 notation
        if [[ ! "$CONTAINER_IP" =~ /24$ ]]; then
            echo "ERROR: Container IP must include /24 notation"
            exit 1
        fi

        echo "✓ Network configuration validated"
        """;

    private static final String CONFIGURE_CONTAINER_NETWORK_SCRIPT = """
        #!/bin/bash
        # Configure container network

        CONTAINER="%s"
        BRIDGE="%s"
        IP_ADDRESS="%s"

        # Remove default network device if exists
        lxc config device remove "$CONTAINER" eth0 2>/dev/null || true

        # Add bridge network device with static IP
        lxc config device add "$CONTAINER" eth0 nic \\
            nictype=bridged \\
            parent="$BRIDGE" \\
            ipv4.address="$IP_ADDRESS"

        echo "✓ Network device configured"
        """;

    private static final String CONFIGURE_INTERNAL_NETWORK_SCRIPT = """
        #!/bin/bash
        # Configure network inside container

        # Set DNS servers
        echo 'nameserver %s' > /etc/resolv.conf
        echo 'nameserver %s' >> /etc/resolv.conf

        # Add default route
        ip route add default via %s 2>/dev/null || true

        # Show network configuration
        ip addr show eth0
        ip route show

        echo "✓ Internal network configured"
        """;

    private static final String TEST_INTERNET_CONNECTIVITY_SCRIPT = """
        #!/bin/bash
        # Test internet connectivity

        if ping -c 1 google.com &>/dev/null; then
            echo "✓ Internet connectivity confirmed"
            exit 0
        else
            echo "✗ No internet connectivity"
            exit 1
        fi
        """;

    private static final String FIX_HOST_NETWORKING_SCRIPT = """
        #!/bin/bash
        # Fix host networking for containers

        echo "Applying network fixes..."

        # Enable IP forwarding
        sysctl -w net.ipv4.ip_forward=1

        # Setup NAT for container subnet
        iptables -t nat -A POSTROUTING -s 10.10.199.0/24 -j MASQUERADE 2>/dev/null || true

        # Show current settings
        sysctl net.ipv4.ip_forward
        iptables -t nat -L POSTROUTING -n -v | grep 10.10.199.0

        echo "✓ Network fixes applied"
        """;

    private final UniqueIdConfig config;

    public NetworkSetup(UniqueIdConfig config) {
        this.config = config;
    }

    /**
     * Validate network configuration
     */
    public String getValidateNetworkScript() {
        return String.format(VALIDATE_NETWORK_SCRIPT,
            config.getIpAddress(),
            config.getGateway(),
            config.getBridge());
    }

    /**
     * Configure container network device
     */
    public String getConfigureNetworkScript() {
        return String.format(CONFIGURE_CONTAINER_NETWORK_SCRIPT,
            config.getBuildContainerName(),
            config.getBridge(),
            config.getIpWithoutMask());
    }

    /**
     * Configure network inside container
     */
    public String getInternalNetworkScript() {
        String[] dns = config.getDnsServers();
        return String.format(CONFIGURE_INTERNAL_NETWORK_SCRIPT,
            dns[0],
            dns[1],
            config.getGatewayWithoutMask());
    }

    /**
     * Test internet connectivity
     */
    public String getTestConnectivityScript() {
        return TEST_INTERNET_CONNECTIVITY_SCRIPT;
    }

    /**
     * Fix host networking if needed
     */
    public String getFixNetworkScript() {
        return FIX_HOST_NETWORKING_SCRIPT;
    }

    /**
     * Get all network setup scripts in order
     */
    public String[] getAllScripts() {
        return new String[] {
            getValidateNetworkScript(),
            getConfigureNetworkScript(),
            getInternalNetworkScript(),
            getTestConnectivityScript()
        };
    }
}