#!/bin/bash
# Auto-generated launch script for FusionPBX container

# Accept config file path as argument
CONFIG_FILE="${1:-$(dirname "$0")/sample.conf}"

# Check if config file exists
if [ ! -f "$CONFIG_FILE" ]; then
    echo "Error: Configuration file not found: $CONFIG_FILE"
    echo "Usage: $0 [config-file]"
    exit 1
fi

# Make config path absolute
CONFIG_FILE="$(realpath "$CONFIG_FILE")"
echo "Loading configuration from: $CONFIG_FILE"

# Source configuration
source "$CONFIG_FILE"

# Set defaults
CONTAINER_NAME="${CONTAINER_NAME:-fusionpbx-01}"
NETWORK="${NETWORK:-lxdbr0}"
IMAGE_NAME="fusion-pbx-v.1"

# Check if container already exists
if lxc list | grep -q "$CONTAINER_NAME"; then
    echo "Container $CONTAINER_NAME already exists"
    read -p "Do you want to delete and recreate it? (y/n) " -n 1 -r
    echo
    if [[ $REPLY =~ ^[Yy]$ ]]; then
        lxc delete "$CONTAINER_NAME" --force
    else
        exit 1
    fi
fi

# Launch container
echo "Launching container: $CONTAINER_NAME"
lxc launch "$IMAGE_NAME" "$CONTAINER_NAME"

# Configure network
if [ ! -z "$STATIC_IP" ]; then
    echo "Setting static IP: $STATIC_IP"
    lxc config device override "$CONTAINER_NAME" eth0
    lxc config device set "$CONTAINER_NAME" eth0 ipv4.address="$STATIC_IP"
fi

if [ ! -z "$NETWORK" ]; then
    echo "Attaching to network: $NETWORK"
    lxc config device set "$CONTAINER_NAME" eth0 network="$NETWORK"
fi

# Mount configuration directory
CONFIG_DIR="$(dirname "$CONFIG_FILE")"
echo "Mounting config directory: $CONFIG_DIR -> /mnt/config"
lxc config device add "$CONTAINER_NAME" config disk source="$CONFIG_DIR" path=/mnt/config

# Copy runtime config
lxc exec "$CONTAINER_NAME" -- cp "/mnt/config/$(basename "$CONFIG_FILE")" /mnt/config/runtime.conf

# Add bind mounts
if [ ${#BIND_MOUNTS[@]} -gt 0 ]; then
    echo "Adding bind mounts..."
    for i in "${!BIND_MOUNTS[@]}"; do
        MOUNT="${BIND_MOUNTS[$i]}"
        IFS=':' read -r HOST_PATH CONTAINER_PATH <<< "$MOUNT"

        # Expand tilde and make absolute
        HOST_PATH="${HOST_PATH/#\~/$HOME}"
        HOST_PATH="$(realpath -m "$HOST_PATH")"

        # Create host directory if it doesn't exist
        mkdir -p "$HOST_PATH"

        DEVICE_NAME="mount$i"
        echo "  Mounting $HOST_PATH -> $CONTAINER_PATH"
        lxc config device add "$CONTAINER_NAME" "$DEVICE_NAME" disk source="$HOST_PATH" path="$CONTAINER_PATH"
    done
fi

# Wait for container to start
echo "Waiting for container to start..."
sleep 5
while ! lxc exec "$CONTAINER_NAME" -- systemctl is-system-running &>/dev/null; do
    sleep 2
done

# Get container IP
if [ -z "$STATIC_IP" ]; then
    CONTAINER_IP=$(lxc list "$CONTAINER_NAME" -c4 --format csv | cut -d' ' -f1)
else
    CONTAINER_IP="$STATIC_IP"
fi

echo ""
echo "==========================================="
echo "FusionPBX Container Started Successfully!"
echo "==========================================="
echo "Container: $CONTAINER_NAME"
echo "IP Address: $CONTAINER_IP"
echo ""
echo "Access FusionPBX:"
echo "  Web Interface: http://$CONTAINER_IP"
echo "  Initial Setup: http://$CONTAINER_IP/install.php"
echo ""
echo "SSH Access:"
echo "  ssh fusionpbx@$CONTAINER_IP"
echo "  Password: fusion123"
echo ""
echo "To stop: lxc stop $CONTAINER_NAME"
echo "To delete: lxc delete $CONTAINER_NAME --force"
echo "==========================================="
