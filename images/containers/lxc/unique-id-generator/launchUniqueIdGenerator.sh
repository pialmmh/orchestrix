#!/bin/bash
# Launch script for unique-id-generator container from exported image

set -e

# Default values
CONFIG_FILE="${1:-./launchUniqueIdGeneratorConfig.cnf}"
CONTAINER_NAME=""
IMAGE_PATH=""

# Function to display usage
usage() {
    echo "Usage: $0 <config-file>"
    echo ""
    echo "Config file should contain:"
    echo "  container.name=<name>"
    echo "  image.path=<path-to-tar.gz>"
    echo "  storage.location.id=<storage-location>"
    echo "  storage.container.root=<container-root>"
    echo "  storage.quota.size=<size>"
    echo "  port=<port-number>"
    exit 1
}

# Check if config file is provided
if [ -z "$1" ]; then
    echo "Error: Configuration file path required"
    usage
fi

# Check if config file exists
if [ ! -f "$CONFIG_FILE" ]; then
    echo "Error: Configuration file not found: $CONFIG_FILE"
    usage
fi

# Load configuration
source "$CONFIG_FILE"

# Validate required parameters
CONTAINER_NAME="${container_name}"
IMAGE_PATH="${image_path}"
STORAGE_LOCATION_ID="${storage_location_id}"
STORAGE_ROOT="${storage_container_root:-unique-id-generator}"
STORAGE_QUOTA="${storage_quota_size:-10G}"
PORT="${port:-7001}"

if [ -z "$CONTAINER_NAME" ]; then
    echo "Error: container.name is required"
    usage
fi

if [ -z "$IMAGE_PATH" ]; then
    echo "Error: image.path is required"
    usage
fi

if [ ! -f "$IMAGE_PATH" ]; then
    echo "Error: Image file not found: $IMAGE_PATH"
    exit 1
fi

if [ -z "$STORAGE_LOCATION_ID" ]; then
    echo "Error: storage.location.id is required"
    usage
fi

echo "========================================="
echo "Launching Unique ID Generator Container"
echo "========================================="
echo "Container Name: $CONTAINER_NAME"
echo "Image Path: $IMAGE_PATH"
echo "Storage Location: $STORAGE_LOCATION_ID"
echo "Storage Root: $STORAGE_ROOT"
echo "Port: $PORT"
echo "========================================="

# Function to get storage path
get_storage_path() {
    local location_id=$1
    if [ -f "/etc/orchestrix/storage-locations.conf" ]; then
        grep "^${location_id}.path=" /etc/orchestrix/storage-locations.conf | cut -d'=' -f2
    else
        echo "/home/telcobright/btrfs"
    fi
}

# Function to parse size
parse_size() {
    local size=$1
    local num=$(echo $size | sed 's/[^0-9.]//g')
    local unit=$(echo $size | sed 's/[0-9.]//g' | tr '[:lower:]' '[:upper:]')

    case $unit in
        G|GB) echo $(( ${num%.*} * 1024 * 1024 * 1024 )) ;;
        M|MB) echo $(( ${num%.*} * 1024 * 1024 )) ;;
        K|KB) echo $(( ${num%.*} * 1024 )) ;;
        T|TB) echo $(( ${num%.*} * 1024 * 1024 * 1024 * 1024 )) ;;
        *) echo ${num%.*} ;;
    esac
}

# Check if container already exists
if lxc info $CONTAINER_NAME &>/dev/null; then
    echo "Container $CONTAINER_NAME already exists."
    read -p "Do you want to remove it and continue? (y/N): " -n 1 -r
    echo
    if [[ $REPLY =~ ^[Yy]$ ]]; then
        echo "Stopping and removing existing container..."
        lxc stop $CONTAINER_NAME --force 2>/dev/null || true
        lxc delete $CONTAINER_NAME --force
    else
        echo "Aborted."
        exit 1
    fi
fi

# Setup BTRFS storage
STORAGE_PATH=$(get_storage_path "$STORAGE_LOCATION_ID")
VOLUME_PATH="${STORAGE_PATH}/containers/${STORAGE_ROOT}-${CONTAINER_NAME}"

echo "Setting up BTRFS storage at: $VOLUME_PATH"

# Ensure BTRFS tools are available
if ! command -v btrfs &> /dev/null; then
    echo "Error: BTRFS tools not installed"
    echo "Please install with: sudo apt-get install -y btrfs-progs"
    exit 1
fi

# Create parent directory
sudo mkdir -p "${STORAGE_PATH}/containers"

# Create BTRFS subvolume if it doesn't exist
if ! sudo btrfs subvolume show "$VOLUME_PATH" &> /dev/null; then
    echo "Creating BTRFS subvolume..."
    sudo btrfs subvolume create "$VOLUME_PATH"

    # Set quota
    if [ -n "$STORAGE_QUOTA" ]; then
        echo "Setting quota to $STORAGE_QUOTA..."
        local mount_point=$(df "$STORAGE_PATH" | tail -1 | awk '{print $6}')
        sudo btrfs quota enable "$mount_point" 2>/dev/null || true

        local subvol_id=$(sudo btrfs subvolume show "$VOLUME_PATH" | grep 'Subvolume ID:' | awk '{print $3}')
        local quota_bytes=$(parse_size "$STORAGE_QUOTA")
        sudo btrfs qgroup limit $quota_bytes 0/$subvol_id "$mount_point"
    fi
fi

# Import the container
echo "Importing container from $IMAGE_PATH..."
lxc import $IMAGE_PATH $CONTAINER_NAME

# Configure storage binding
echo "Configuring storage binding..."
lxc config device remove $CONTAINER_NAME root 2>/dev/null || true
lxc config device add $CONTAINER_NAME root disk source=$VOLUME_PATH path=/

# Configure port forwarding if specified
if [ -n "$PORT" ]; then
    echo "Configuring port forwarding on port $PORT..."
    lxc config device remove $CONTAINER_NAME unique-id-port 2>/dev/null || true
    lxc config device add $CONTAINER_NAME unique-id-port proxy \
        listen=tcp:0.0.0.0:$PORT \
        connect=tcp:127.0.0.1:7001
fi

# Apply any additional bind mounts from config
if [ -n "$bind_mounts" ]; then
    echo "Configuring bind mounts..."
    IFS=',' read -ra MOUNTS <<< "$bind_mounts"
    for mount in "${MOUNTS[@]}"; do
        IFS=':' read -ra MOUNT_PARTS <<< "$mount"
        host_path="${MOUNT_PARTS[0]}"
        container_path="${MOUNT_PARTS[1]}"
        mount_name=$(echo "$container_path" | sed 's/[^a-zA-Z0-9]/-/g')

        echo "  Mounting $host_path to $container_path"
        lxc config device add $CONTAINER_NAME $mount_name disk \
            source=$host_path \
            path=$container_path
    done
fi

# Apply environment variables from config
if [ -n "$environment_vars" ]; then
    echo "Setting environment variables..."
    IFS=',' read -ra VARS <<< "$environment_vars"
    for var in "${VARS[@]}"; do
        echo "  Setting $var"
        lxc config set $CONTAINER_NAME environment.$var
    done
fi

# Start the container
echo "Starting container..."
lxc start $CONTAINER_NAME

# Wait for container to be ready
echo "Waiting for container to be ready..."
sleep 5

# Get container IP
CONTAINER_IP=$(lxc list $CONTAINER_NAME -c 4 --format csv | cut -d' ' -f1)

echo "========================================="
echo "Container launched successfully!"
echo "========================================="
echo "Container Name: $CONTAINER_NAME"
echo "Container IP: $CONTAINER_IP"
echo "Service Port: $PORT"
echo "Storage Volume: $VOLUME_PATH"
echo ""
echo "Access the service at:"
echo "  http://localhost:$PORT/generate"
echo "  http://localhost:$PORT/health"
echo ""
echo "To access the container:"
echo "  lxc exec $CONTAINER_NAME -- /bin/bash"
echo ""
echo "To stop the container:"
echo "  lxc stop $CONTAINER_NAME"
echo "========================================="