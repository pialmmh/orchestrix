#!/bin/bash

# =============================================================================
# LXC Container Network Validation and Configuration Template
# =============================================================================
#
# This is a TEMPLATE script containing reusable network validation functions
# for LXC container build scripts.
#
# USAGE: Copy the functions below into your build script. DO NOT source this
#        file - build scripts must be self-sufficient.
#
# REQUIREMENTS IN CONFIG FILE:
#   CONTAINER_IP="10.10.199.XX/24"    # Must include /24
#   GATEWAY_IP="10.10.199.1/24"       # Must include /24
#   BRIDGE_NAME="lxdbr0"               # Bridge interface
#   DNS_SERVERS="8.8.8.8 8.8.4.4"      # DNS servers
#
# =============================================================================

# -----------------------------------------------------------------------------
# FUNCTION: validate_network_config
# Validates that network configuration meets requirements:
# - IPs include /24 notation
# - IPs are in 10.10.199.0/24 subnet
# - IPs are in same subnet
# -----------------------------------------------------------------------------
validate_network_config() {
    echo ""
    echo "Validating network configuration..."

    # Check if IP and Gateway are set
    if [ -z "$CONTAINER_IP" ] || [ "$CONTAINER_IP" = "" ]; then
        echo "ERROR: CONTAINER_IP is not set in config file"
        echo "Please set a valid IP with /24 notation (e.g., 10.10.199.50/24)"
        exit 1
    fi

    if [ -z "$GATEWAY_IP" ] || [ "$GATEWAY_IP" = "" ]; then
        echo "ERROR: GATEWAY_IP is not set in config file"
        echo "Please set the gateway IP with /24 notation (e.g., 10.10.199.1/24)"
        exit 1
    fi

    # Check if /24 is included
    if [[ ! "$CONTAINER_IP" =~ /24$ ]]; then
        echo "ERROR: CONTAINER_IP must include /24 subnet mask"
        echo "Current value: $CONTAINER_IP"
        echo "Expected format: 10.10.199.50/24"
        exit 1
    fi

    if [[ ! "$GATEWAY_IP" =~ /24$ ]]; then
        echo "ERROR: GATEWAY_IP must include /24 subnet mask"
        echo "Current value: $GATEWAY_IP"
        echo "Expected format: 10.10.199.1/24"
        exit 1
    fi

    # Extract IP without /24 for validation
    CONTAINER_IP_ONLY="${CONTAINER_IP%/24}"
    GATEWAY_IP_ONLY="${GATEWAY_IP%/24}"

    # Extract first 3 octets for /24 subnet comparison
    CONTAINER_SUBNET=$(echo "$CONTAINER_IP_ONLY" | cut -d. -f1-3)
    GATEWAY_SUBNET=$(echo "$GATEWAY_IP_ONLY" | cut -d. -f1-3)

    # Check if both are in 10.10.199 subnet
    if [ "$CONTAINER_SUBNET" != "10.10.199" ]; then
        echo "ERROR: CONTAINER_IP ($CONTAINER_IP) is not in 10.10.199.0/24 subnet"
        exit 1
    fi

    if [ "$GATEWAY_SUBNET" != "10.10.199" ]; then
        echo "ERROR: GATEWAY_IP ($GATEWAY_IP) is not in 10.10.199.0/24 subnet"
        exit 1
    fi

    # Check if IPs are in same subnet
    if [ "$CONTAINER_SUBNET" != "$GATEWAY_SUBNET" ]; then
        echo "ERROR: CONTAINER_IP and GATEWAY_IP are not in the same subnet"
        echo "Container subnet: ${CONTAINER_SUBNET}.0/24"
        echo "Gateway subnet: ${GATEWAY_SUBNET}.0/24"
        exit 1
    fi

    echo "✓ Network configuration validated"
    echo "  Container IP: $CONTAINER_IP"
    echo "  Gateway IP: $GATEWAY_IP"
    echo "  Bridge: $BRIDGE_NAME"
}

# -----------------------------------------------------------------------------
# FUNCTION: configure_container_network
# Configures bridge networking for the container with static IP
# Requires: $BUILD_CONTAINER variable to be set
# -----------------------------------------------------------------------------
configure_container_network() {
    local container_name="$1"

    if [ -z "$container_name" ]; then
        echo "ERROR: Container name not provided to configure_container_network"
        exit 1
    fi

    echo "Configuring network (IP: $CONTAINER_IP)..."

    # Remove default network device if exists
    lxc config device remove "$container_name" eth0 2>/dev/null || true

    # Add bridge network device with static IP (use IP without /24 for LXC config)
    lxc config device add "$container_name" eth0 nic \
        nictype=bridged \
        parent="$BRIDGE_NAME" \
        ipv4.address="${CONTAINER_IP%/24}"

    echo "Waiting for container to be ready..."
    sleep 5

    # Configure network inside container
    echo "Configuring container network settings..."

    # Set DNS servers
    lxc exec "$container_name" -- bash -c "echo 'nameserver ${DNS_SERVERS%% *}' > /etc/resolv.conf"
    lxc exec "$container_name" -- bash -c "echo 'nameserver ${DNS_SERVERS##* }' >> /etc/resolv.conf"

    # Add default route (use IP without /24)
    lxc exec "$container_name" -- bash -c "ip route add default via ${GATEWAY_IP%/24} 2>/dev/null || true"
}

# -----------------------------------------------------------------------------
# FUNCTION: test_and_wait_for_internet
# Tests internet connectivity and provides guidance to fix network issues
# Loops until internet connectivity is established
# Requires: $BUILD_CONTAINER variable to be set
# -----------------------------------------------------------------------------
test_and_wait_for_internet() {
    local container_name="$1"

    if [ -z "$container_name" ]; then
        echo "ERROR: Container name not provided to test_and_wait_for_internet"
        exit 1
    fi

    echo ""
    echo "Testing internet connectivity..."

    while true; do
        if lxc exec "$container_name" -- ping -c 1 google.com &>/dev/null; then
            echo "✓ Internet connectivity confirmed"
            break
        else
            echo ""
            echo "⚠ Container cannot reach the internet!"
            echo ""
            echo "Please check:"
            echo "1. Bridge '$BRIDGE_NAME' exists and is configured"
            echo "2. IP forwarding is enabled: sysctl net.ipv4.ip_forward"
            echo "3. NAT/Masquerading is configured for the bridge"
            echo ""
            echo "Common fixes:"
            echo "  sudo sysctl -w net.ipv4.ip_forward=1"
            echo "  sudo iptables -t nat -A POSTROUTING -s 10.10.199.0/24 -j MASQUERADE"
            echo ""
            echo "To check current settings:"
            echo "  sudo sysctl net.ipv4.ip_forward"
            echo "  sudo iptables -t nat -L POSTROUTING -n -v"
            echo ""
            echo "To make IP forwarding permanent:"
            echo "  echo 'net.ipv4.ip_forward=1' | sudo tee -a /etc/sysctl.conf"
            echo ""
            echo "To check bridge configuration:"
            echo "  lxc network show $BRIDGE_NAME"
            echo ""
            read -p "Press Enter to retry after fixing the network issue..."

            # Retry DNS and routing configuration
            lxc exec "$container_name" -- bash -c "echo 'nameserver 8.8.8.8' > /etc/resolv.conf"
            lxc exec "$container_name" -- bash -c "echo 'nameserver 8.8.4.4' >> /etc/resolv.conf"
            lxc exec "$container_name" -- bash -c "ip route add default via ${GATEWAY_IP%/24} 2>/dev/null || true"
        fi
    done

    echo "✓ Container ready with network access"
}

# =============================================================================
# EXAMPLE USAGE IN BUILD SCRIPT:
# =============================================================================
#
# #!/bin/bash
# set -e
#
# # Load configuration
# source "$CONFIG_FILE"
#
# # [Copy the three functions above into your script here]
#
# # Main execution
# main() {
#     validate_network_config              # Validate network settings
#     check_lxc
#     cleanup_existing
#     create_build_container
#     configure_container_network "$BUILD_CONTAINER"    # Configure network
#     test_and_wait_for_internet "$BUILD_CONTAINER"     # Test connectivity
#     install_dependencies
#     setup_service
#     optimize_container
#     create_base_image
#     cleanup
#     print_summary
# }
#
# main "$@"
#
# =============================================================================