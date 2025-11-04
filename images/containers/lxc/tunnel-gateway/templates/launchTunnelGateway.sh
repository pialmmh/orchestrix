#!/bin/bash
set -e

# Check for config file argument
if [ -z "$1" ]; then
    echo "Usage: $0 <config-file>"
    echo ""
    echo "Example: $0 sample.conf"
    exit 1
fi

CONFIG_FILE="$1"

if [ ! -f "$CONFIG_FILE" ]; then
    echo "Error: Config file '$CONFIG_FILE' not found"
    exit 1
fi

echo ""
echo "Loading configuration from: $CONFIG_FILE"
echo ""

# Extract container configuration from config file
CONTAINER_NAME=$(grep "^CONTAINER_NAME" "$CONFIG_FILE" | cut -d'=' -f2 | tr -d ' "')
CONTAINER_IP=$(grep "^CONTAINER_IP" "$CONFIG_FILE" | cut -d'=' -f2 | tr -d ' "#' | cut -d'#' -f1)

if [ -z "$CONTAINER_NAME" ] || [ -z "$CONTAINER_IP" ]; then
    echo "Error: Could not find CONTAINER_NAME and CONTAINER_IP in config file"
    exit 1
fi

IMAGE_NAME="tunnel-gateway-base"

echo "=========================================="
echo "Launching Tunnel Gateway"
echo "=========================================="
echo "Container: $CONTAINER_NAME"
echo "IP Address: $CONTAINER_IP"
echo "Image: $IMAGE_NAME"
echo ""

# Count tunnel sections
TUNNEL_COUNT=$(grep -c "^\[.*\]" "$CONFIG_FILE" || echo "0")
echo "Tunnels configured: $TUNNEL_COUNT"
echo ""

# Check if container already exists
if lxc list --format=csv -c n | grep -q "^${CONTAINER_NAME}$"; then
    echo "Warning: Container $CONTAINER_NAME already exists"
    echo "Delete it? (y/n)"
    read -r DELETE_EXISTING
    if [[ "$DELETE_EXISTING" =~ ^[Yy]$ ]]; then
        echo "Deleting existing container..."
        lxc delete "$CONTAINER_NAME" --force
    else
        echo "Aborted"
        exit 1
    fi
fi

# Launch container
echo "Creating container..."
lxc launch "local:${IMAGE_NAME}" "$CONTAINER_NAME"

# Wait for container to start
sleep 3

# Configure static IP
echo "Configuring static IP: $CONTAINER_IP"
lxc config device override "$CONTAINER_NAME" eth0 ipv4.address="$CONTAINER_IP"

# Restart to apply IP
echo "Restarting container to apply network configuration..."
lxc restart "$CONTAINER_NAME"
sleep 3

# Push configuration file
echo "Pushing tunnel configuration..."
lxc file push "$CONFIG_FILE" "$CONTAINER_NAME/etc/tunnel-gateway/tunnels.conf"

# Start tunnels if AUTO_START is true
AUTO_START=$(grep "^AUTO_START" "$CONFIG_FILE" | cut -d'=' -f2 | tr -d ' "' || echo "false")
if [ "$AUTO_START" = "true" ]; then
    echo ""
    echo "Starting SSH tunnels..."
    lxc exec "$CONTAINER_NAME" -- /usr/local/bin/start-tunnels.sh
fi

echo ""
echo "=========================================="
echo "✓ Tunnel Gateway Ready!"
echo "=========================================="
echo ""

# Show container info
lxc list "$CONTAINER_NAME" --format=table --columns=ns4

echo ""
echo "Management commands:"
echo "  List tunnels:  lxc exec $CONTAINER_NAME -- /usr/local/bin/list-tunnels.sh"
echo "  Stop tunnels:  lxc exec $CONTAINER_NAME -- /usr/local/bin/stop-tunnels.sh"
echo "  Start tunnels: lxc exec $CONTAINER_NAME -- /usr/local/bin/start-tunnels.sh"
echo "  Container shell: lxc exec $CONTAINER_NAME -- bash"
echo ""
