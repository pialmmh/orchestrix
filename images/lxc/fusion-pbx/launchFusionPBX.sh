#!/bin/bash
# Launch script for FusionPBX LXC container
# Accepts configuration file from any path

set -e

# Check for config file argument
CONFIG_FILE="$1"
if [ -z "$CONFIG_FILE" ]; then
    echo "Error: Configuration file path required"
    echo "Usage: $0 /path/to/config.conf"
    echo ""
    echo "Example: $0 /tmp/my-fusion-config.conf"
    echo "Example: $0 ~/configs/fusion-pbx.conf"
    echo "Example: $0 ./sample-config.conf"
    exit 1
fi

# Check if config file exists
if [ ! -f "$CONFIG_FILE" ]; then
    echo "Error: Configuration file not found: $CONFIG_FILE"
    exit 1
fi

# Get absolute path of config file
CONFIG_FILE="$(realpath "$CONFIG_FILE")"
echo "Using configuration: $CONFIG_FILE"

# Source the configuration
source "$CONFIG_FILE"

# Validate required variables
if [ -z "$CONTAINER_NAME" ]; then
    echo "Error: CONTAINER_NAME not defined in configuration"
    exit 1
fi

# Set defaults for optional variables
IMAGE_NAME="${IMAGE_NAME:-fusion-pbx-base}"
NETWORK="${NETWORK:-lxdbr0}"

echo "==========================================="
echo "Launching FusionPBX Container"
echo "==========================================="
echo "Container name: $CONTAINER_NAME"
echo "Base image: $IMAGE_NAME"
echo "Network: $NETWORK"
if [ ! -z "$STATIC_IP" ]; then
    echo "Static IP: $STATIC_IP"
fi

# Check if container already exists
if lxc list | grep -q "$CONTAINER_NAME"; then
    echo "Error: Container $CONTAINER_NAME already exists"
    echo "To delete: lxc delete $CONTAINER_NAME --force"
    exit 1
fi

# Check if image exists
if ! lxc image list | grep -q "$IMAGE_NAME"; then
    echo "Error: Image $IMAGE_NAME not found"
    echo "Please build the image first: ./buildFusionPBX.sh"
    exit 1
fi

# Launch container
echo "Creating container from image..."
lxc launch "$IMAGE_NAME" "$CONTAINER_NAME" --network="$NETWORK"

# Wait for container to be ready
echo "Waiting for container to start..."
sleep 3
while ! lxc exec "$CONTAINER_NAME" -- systemctl is-system-running &>/dev/null; do
    sleep 2
done

# Configure static IP if specified
if [ ! -z "$STATIC_IP" ]; then
    echo "Setting static IP: $STATIC_IP"
    lxc config device set "$CONTAINER_NAME" eth0 ipv4.address="$STATIC_IP"
    
    # Restart container to apply IP
    echo "Restarting container to apply network settings..."
    lxc restart "$CONTAINER_NAME"
    sleep 5
fi

# Add configuration file mount
echo "Mounting configuration file..."
CONFIG_DIR="/mnt/config"
lxc config device add "$CONTAINER_NAME" config disk \
    source="$(dirname "$CONFIG_FILE")" \
    path="$CONFIG_DIR"

# Add bind mounts if specified
if [ ! -z "$BIND_MOUNTS" ]; then
    echo "Adding bind mounts..."
    MOUNT_INDEX=0
    for MOUNT in "${BIND_MOUNTS[@]}"; do
        IFS=':' read -r HOST_PATH CONTAINER_PATH <<< "$MOUNT"
        
        # Expand ~ to home directory
        HOST_PATH="${HOST_PATH/#\~/$HOME}"
        
        # Check if host path exists
        if [ ! -e "$HOST_PATH" ]; then
            echo "Warning: Host path does not exist: $HOST_PATH"
            echo "Creating directory..."
            mkdir -p "$HOST_PATH"
        fi
        
        # Get absolute path
        HOST_PATH="$(realpath "$HOST_PATH")"
        
        # Add mount
        MOUNT_NAME="mount${MOUNT_INDEX}"
        echo "  Mounting $HOST_PATH -> $CONTAINER_PATH"
        lxc config device add "$CONTAINER_NAME" "$MOUNT_NAME" disk \
            source="$HOST_PATH" \
            path="$CONTAINER_PATH"
        
        MOUNT_INDEX=$((MOUNT_INDEX + 1))
    done
fi

# Apply configuration inside container
echo "Applying configuration..."
CONFIG_FILENAME="$(basename "$CONFIG_FILE")"
lxc exec "$CONTAINER_NAME" -- bash -c "ln -sf $CONFIG_DIR/$CONFIG_FILENAME /mnt/config/fusion-pbx.conf"
lxc exec "$CONTAINER_NAME" -- /usr/local/bin/apply-config.sh

# Get container IP
echo "Getting container network information..."
sleep 2
CONTAINER_IP=$(lxc list "$CONTAINER_NAME" --format=json | jq -r '.[0].state.network.eth0.addresses[] | select(.family=="inet").address')

echo ""
echo "==========================================="
echo "FusionPBX Container Launched Successfully!"
echo "==========================================="
echo "Container: $CONTAINER_NAME"
echo "IP Address: ${CONTAINER_IP:-Waiting for IP...}"
echo ""
echo "Access FusionPBX:"
echo "  Web Interface: http://${CONTAINER_IP:-<container-ip>}"
echo "  Initial Setup: http://${CONTAINER_IP:-<container-ip>}/install.php"
echo ""
echo "SSH Access:"
echo "  ssh fusionpbx@${CONTAINER_IP:-<container-ip>}"
echo "  Password: fusion123"
echo ""
echo "FreeSWITCH SIP Ports:"
echo "  Internal Profile: 5060 (UDP/TCP)"
echo "  External Profile: 5080 (UDP/TCP)"
echo "  RTP Ports: 16384-32768"
echo "  Note: No NAT between container and host (ideal for VoIP)"
echo ""
echo "PostgreSQL:"
echo "  Port: 5432"
echo "  User: postgres"
echo ""
echo "Services Status:"
lxc exec "$CONTAINER_NAME" -- systemctl is-active postgresql && echo "  PostgreSQL: active" || echo "  PostgreSQL: inactive"
lxc exec "$CONTAINER_NAME" -- systemctl is-active freeswitch && echo "  FreeSWITCH: active" || echo "  FreeSWITCH: inactive"
lxc exec "$CONTAINER_NAME" -- systemctl is-active nginx && echo "  Nginx: active" || echo "  Nginx: inactive"
lxc exec "$CONTAINER_NAME" -- systemctl is-active php8.2-fpm && echo "  PHP-FPM: active" || echo "  PHP-FPM: inactive"
echo ""
echo "Management commands:"
echo "  lxc exec $CONTAINER_NAME -- bash"
echo "  lxc stop $CONTAINER_NAME"
echo "  lxc start $CONTAINER_NAME"
echo "  lxc delete $CONTAINER_NAME --force"
echo ""

# If MONITOR flag is set, tail the logs
if [ "$MONITOR_STARTUP" = "true" ]; then
    echo "Monitoring startup (Ctrl+C to exit)..."
    lxc exec "$CONTAINER_NAME" -- journalctl -f
fi