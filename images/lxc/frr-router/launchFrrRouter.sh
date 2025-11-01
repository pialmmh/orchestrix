#!/bin/bash

set -e

# Launch script for FRR Router container
# Usage: ./launchFrrRouter.sh <config-file>
#
# This container uses HOST NETWORKING MODE
# - Shares host's network namespace
# - FRR runs on host's external IP
# - No LXC bridge networking needed

CONFIG_FILE="$1"

if [ -z "$CONFIG_FILE" ]; then
    echo "ERROR: Configuration file not provided"
    echo "Usage: $0 <config-file>"
    echo ""
    echo "Example: $0 /path/to/config.conf"
    exit 1
fi

if [ ! -f "$CONFIG_FILE" ]; then
    echo "ERROR: Configuration file not found: $CONFIG_FILE"
    exit 1
fi

echo "========================================="
echo "FRR Router Container Launcher"
echo "========================================="
echo "Config: $CONFIG_FILE"
echo ""

# Load configuration
source "$CONFIG_FILE"

# Validate required parameters
if [ -z "$CONTAINER_NAME" ]; then
    echo "ERROR: CONTAINER_NAME not set in config file"
    exit 1
fi

if [ -z "$BASE_IMAGE" ]; then
    echo "ERROR: BASE_IMAGE not set in config file"
    exit 1
fi

if [ -z "$BGP_ASN" ]; then
    echo "ERROR: BGP_ASN not set in config file"
    exit 1
fi

# Function to cleanup on failure
cleanup_on_failure() {
    echo "ERROR: Launch failed. Cleaning up..."
    lxc delete --force "$CONTAINER_NAME" 2>/dev/null || true
    exit 1
}

trap cleanup_on_failure ERR

# Check if container already exists
if lxc info "$CONTAINER_NAME" &>/dev/null; then
    echo "ERROR: Container '$CONTAINER_NAME' already exists"
    echo "Please delete it first or choose a different name"
    exit 1
fi

# Check if base image exists
if ! lxc image info "$BASE_IMAGE" &>/dev/null; then
    echo "ERROR: Base image '$BASE_IMAGE' not found"
    echo "Please build the image first using buildFrrRouter.sh"
    exit 1
fi

# Auto-detect host's external IP for ROUTER_ID if not set
if [ -z "$ROUTER_ID" ]; then
    echo "Detecting host external IP..."
    ROUTER_ID=$(ip route get 8.8.8.8 | awk '{print $7; exit}')
    if [ -z "$ROUTER_ID" ]; then
        echo "ERROR: Could not auto-detect host IP. Please set ROUTER_ID in config"
        exit 1
    fi
    echo "Auto-detected ROUTER_ID: $ROUTER_ID"
fi

# Auto-generate hostname if not set
if [ -z "$ROUTER_HOSTNAME" ]; then
    ROUTER_HOSTNAME="$CONTAINER_NAME"
    echo "Auto-generated ROUTER_HOSTNAME: $ROUTER_HOSTNAME"
fi

# Apply defaults
MEMORY_LIMIT="${MEMORY_LIMIT:-100MB}"
CPU_LIMIT="${CPU_LIMIT:-1}"
ENABLE_BGP="${ENABLE_BGP:-true}"
ENABLE_OSPF="${ENABLE_OSPF:-false}"

echo ""
echo "Configuration Summary:"
echo "  Container: $CONTAINER_NAME"
echo "  Base Image: $BASE_IMAGE"
echo "  Router ID: $ROUTER_ID"
echo "  Hostname: $ROUTER_HOSTNAME"
echo "  Memory: $MEMORY_LIMIT"
echo "  CPU: $CPU_LIMIT"
echo "  BGP: $ENABLE_BGP (ASN: ${BGP_ASN:-N/A})"
echo "  OSPF: $ENABLE_OSPF"
echo ""

# Create container with host networking mode
echo "Creating container with host networking mode..."
lxc init "$BASE_IMAGE" "$CONTAINER_NAME"

# Configure host networking mode
echo "Configuring host networking..."
lxc config set "$CONTAINER_NAME" security.privileged true
lxc config set "$CONTAINER_NAME" security.nesting true

# Remove default eth0 device - we use host networking
lxc config device remove "$CONTAINER_NAME" eth0 2>/dev/null || true

# Configure resource limits
echo "Setting resource limits..."
lxc config set "$CONTAINER_NAME" limits.memory "$MEMORY_LIMIT"
lxc config set "$CONTAINER_NAME" limits.cpu "$CPU_LIMIT"

# Start container
echo "Starting container..."
lxc start "$CONTAINER_NAME"

# Wait for container to be ready
echo "Waiting for container to be ready..."
sleep 3

# Push configuration to container (with auto-detected values)
echo "Pushing configuration to container..."
lxc exec "$CONTAINER_NAME" -- bash -c "cat > /etc/frr-launch-config.conf <<'EOF'
CONTAINER_NAME=\"$CONTAINER_NAME\"
BASE_IMAGE=\"$BASE_IMAGE\"
ROUTER_ID=\"$ROUTER_ID\"
ROUTER_HOSTNAME=\"$ROUTER_HOSTNAME\"
ENABLE_BGP=\"$ENABLE_BGP\"
BGP_ASN=\"$BGP_ASN\"
BGP_NEIGHBORS=\"$BGP_NEIGHBORS\"
BGP_NETWORKS=\"$BGP_NETWORKS\"
ENABLE_OSPF=\"$ENABLE_OSPF\"
OSPF_AREA=\"${OSPF_AREA:-0.0.0.0}\"
OSPF_NETWORKS=\"${OSPF_NETWORKS:-}\"
OSPF_INTERFACES=\"${OSPF_INTERFACES:-}\"
OSPF_PASSIVE_INTERFACES=\"${OSPF_PASSIVE_INTERFACES:-}\"
EOF"

# Run FRR configuration script
echo "Generating FRR configuration..."
lxc exec "$CONTAINER_NAME" -- /usr/local/bin/configure-frr-from-env.sh

# Verify FRR is running
echo "Verifying FRR services..."
sleep 2
lxc exec "$CONTAINER_NAME" -- systemctl status frr --no-pager || true

# Print summary
echo ""
echo "========================================="
echo "Container launched successfully!"
echo "========================================="
echo "Container Name: $CONTAINER_NAME"
echo "Networking: HOST MODE (uses host's network)"
echo "Router ID: $ROUTER_ID (host's external IP)"
echo ""
echo "Router Configuration:"
if [ "${ENABLE_BGP}" = "true" ]; then
    echo "  BGP: Enabled (ASN: ${BGP_ASN})"
    echo "  BGP Neighbors: ${BGP_NEIGHBORS:-None}"
    echo "  Announcing: ${BGP_NETWORKS:-None}"
fi
if [ "${ENABLE_OSPF}" = "true" ]; then
    echo "  OSPF: Enabled (Area: ${OSPF_AREA:-0.0.0.0})"
    echo "  Networks: ${OSPF_NETWORKS:-None}"
fi
echo ""
echo "Management:"
echo "  Shell access: lxc exec $CONTAINER_NAME -- bash"
echo "  FRR console: lxc exec $CONTAINER_NAME -- vtysh"
echo "  View routes: lxc exec $CONTAINER_NAME -- vtysh -c 'show ip route'"
echo "  View BGP: lxc exec $CONTAINER_NAME -- vtysh -c 'show ip bgp summary'"
echo "  View OSPF: lxc exec $CONTAINER_NAME -- vtysh -c 'show ip ospf neighbor'"
echo ""
echo "Stop container: lxc stop $CONTAINER_NAME"
echo "Delete container: lxc delete $CONTAINER_NAME"
echo "========================================="
