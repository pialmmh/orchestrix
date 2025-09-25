#!/bin/bash
# Launch script for Unique ID Generator LXC container
# Launches a container from the base image with runtime shard configuration

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

# Default to the latest version's sample config if no config specified
LATEST_VERSION=$(ls -d "$SCRIPT_DIR"/unique-id-generator-v.* 2>/dev/null | sort -V | tail -n1)
if [ -n "$LATEST_VERSION" ]; then
    DEFAULT_CONFIG="$LATEST_VERSION/generated/sample.conf"
else
    DEFAULT_CONFIG="$SCRIPT_DIR/launch.conf"
fi

CONFIG_FILE="${1:-$DEFAULT_CONFIG}"

# Function to parse config file
parse_config() {
    if [ ! -f "$CONFIG_FILE" ]; then
        echo "Error: Configuration file not found: $CONFIG_FILE"
        echo "Usage: $0 [config-file]"
        echo ""
        echo "Available sample configs:"
        find "$SCRIPT_DIR" -name "sample.conf" -type f 2>/dev/null
        exit 1
    fi

    # Source the config file
    source "$CONFIG_FILE"

    # Set defaults if not specified
    CONTAINER_NAME="${CONTAINER_NAME:-uid-generator-1}"
    BASE_IMAGE="${BASE_IMAGE:-unique-id-generator-base-v.1.0.0}"
    BRIDGE_NAME="${BRIDGE_NAME:-lxdbr0}"
    CONTAINER_IP="${CONTAINER_IP:-10.10.199.51/24}"
    GATEWAY_IP="${GATEWAY_IP:-10.10.199.1}"
    DNS_SERVERS="${DNS_SERVERS:-8.8.8.8 8.8.4.4}"
    SERVICE_PORT="${SERVICE_PORT:-7001}"
    SERVICE_USER="${SERVICE_USER:-node}"
    SERVICE_GROUP="${SERVICE_GROUP:-node}"

    # Shard configuration (runtime)
    SHARD_ID="${SHARD_ID:-1}"
    TOTAL_SHARDS="${TOTAL_SHARDS:-1}"
}

# Parse configuration
parse_config

echo "========================================="
echo "Unique ID Generator Container Launcher"
echo "Shard: ${SHARD_ID} of ${TOTAL_SHARDS}"
echo "========================================="

# Check if base image exists
if ! lxc image list --format json | grep -q "\"$BASE_IMAGE\""; then
    echo "ERROR: Base image '$BASE_IMAGE' not found"
    echo "Please run buildUniqueIdGenerator.sh first"
    exit 1
fi

# Check if container already exists
if lxc info "$CONTAINER_NAME" &>/dev/null 2>&1; then
    echo "WARNING: Container '$CONTAINER_NAME' already exists"
    read -p "Delete and recreate? (y/N): " -n 1 -r
    echo
    if [[ $REPLY =~ ^[Yy]$ ]]; then
        lxc delete --force "$CONTAINER_NAME"
    else
        echo "Launch cancelled"
        exit 1
    fi
fi

# Launch container
echo "Launching container: $CONTAINER_NAME"
lxc launch "$BASE_IMAGE" "$CONTAINER_NAME"

echo "Waiting for container to be ready..."
sleep 3

# Configure network
echo "Configuring network..."
lxc config device remove "$CONTAINER_NAME" eth0 2>/dev/null || true
lxc config device add "$CONTAINER_NAME" eth0 nic \
    nictype=bridged \
    parent="$BRIDGE_NAME" \
    ipv4.address="${CONTAINER_IP%/24}"

# Set environment variables for sharding
echo "Setting shard configuration..."
lxc config set "$CONTAINER_NAME" environment.SHARD_ID="$SHARD_ID"
lxc config set "$CONTAINER_NAME" environment.TOTAL_SHARDS="$TOTAL_SHARDS"

# Configure DNS and routing
lxc exec "$CONTAINER_NAME" -- bash -c "echo 'nameserver 8.8.8.8' > /etc/resolv.conf"
lxc exec "$CONTAINER_NAME" -- bash -c "echo 'nameserver 8.8.4.4' >> /etc/resolv.conf"
lxc exec "$CONTAINER_NAME" -- bash -c "ip route add default via $GATEWAY_IP 2>/dev/null || true"

# Update systemd service with shard configuration
echo "Updating service configuration..."
lxc exec "$CONTAINER_NAME" -- bash -c "
    sed -i 's/Environment=\"SHARD_ID=.*\"/Environment=\"SHARD_ID=$SHARD_ID\"/' /etc/systemd/system/unique-id-generator.service
    sed -i 's/Environment=\"TOTAL_SHARDS=.*\"/Environment=\"TOTAL_SHARDS=$TOTAL_SHARDS\"/' /etc/systemd/system/unique-id-generator.service
    systemctl daemon-reload
    systemctl restart unique-id-generator
"

# Wait for service to start
sleep 3

# Test service
echo "Testing service..."
CONTAINER_IP_ONLY="${CONTAINER_IP%/24}"
if curl -s "http://${CONTAINER_IP_ONLY}:${SERVICE_PORT}/health" > /dev/null 2>&1; then
    echo "✓ Service is responding"

    echo ""
    echo "Shard Info:"
    curl -s "http://${CONTAINER_IP_ONLY}:${SERVICE_PORT}/shard-info" | python3 -m json.tool 2>/dev/null || \
        curl -s "http://${CONTAINER_IP_ONLY}:${SERVICE_PORT}/shard-info"
else
    echo "⚠ Service not responding yet"
fi

echo ""
echo "========================================="
echo "Container Launch Complete!"
echo "========================================="
echo ""
echo "Container: $CONTAINER_NAME"
echo "Shard: $SHARD_ID of $TOTAL_SHARDS"
echo "IP Address: $CONTAINER_IP_ONLY"
echo "Service URL: http://${CONTAINER_IP_ONLY}:${SERVICE_PORT}"
echo ""
echo "Test commands:"
echo "  # Get shard info"
echo "  curl http://${CONTAINER_IP_ONLY}:${SERVICE_PORT}/shard-info"
echo ""
echo "  # Generate sequential ID (interleaved by shard)"
echo "  curl 'http://${CONTAINER_IP_ONLY}:${SERVICE_PORT}/api/next-id/users?dataType=int'"
echo ""
echo "  # Generate UUID"
echo "  curl 'http://${CONTAINER_IP_ONLY}:${SERVICE_PORT}/api/next-id/sessions?dataType=uuid16'"
echo ""
echo "Container management:"
echo "  View logs:  lxc exec $CONTAINER_NAME -- tail -f /var/log/unique-id-generator.log"
echo "  Shell:      lxc exec $CONTAINER_NAME -- bash"
echo "  Stop:       lxc stop $CONTAINER_NAME"
echo "  Delete:     lxc delete --force $CONTAINER_NAME"
echo ""