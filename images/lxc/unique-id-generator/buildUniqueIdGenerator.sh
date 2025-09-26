#!/bin/bash
# Build script for unique-id-generator LXC container with BTRFS support

set -e

# Configuration
CONTAINER_NAME="unique-id-generator-base"
IMAGE_NAME="unique-id-generator-base"
EXPORT_PATH="/tmp/${IMAGE_NAME}.tar.gz"
CONFIG_FILE="${1:-./buildUniqueIdGeneratorConfig.cnf}"

# Load configuration
if [ -f "$CONFIG_FILE" ]; then
    echo "Loading configuration from $CONFIG_FILE"
    source "$CONFIG_FILE"
else
    echo "Configuration file not found: $CONFIG_FILE"
    echo "Using default values"
fi

# Storage configuration (mandatory)
STORAGE_PROVIDER="${storage_provider:-btrfs}"
STORAGE_LOCATION_ID="${storage_location_id}"
STORAGE_ROOT="${storage_container_root:-unique-id-generator}"
STORAGE_QUOTA="${storage_quota_size:-10G}"

# Container configuration
DEBIAN_VERSION="${debian_version:-bookworm}"
PORT="${port:-7001}"
MEMORY_LIMIT="${memory_limit:-512MB}"
CPU_LIMIT="${cpu_limit:-1}"

# Validate storage configuration
if [ -z "$STORAGE_LOCATION_ID" ]; then
    echo "Error: storage.location.id is required in configuration"
    echo "Please specify a storage location ID (e.g., btrfs_ssd_main)"
    exit 1
fi

echo "========================================="
echo "Building Unique ID Generator Container"
echo "========================================="
echo "Storage Provider: $STORAGE_PROVIDER"
echo "Storage Location: $STORAGE_LOCATION_ID"
echo "Storage Root: $STORAGE_ROOT"
echo "Storage Quota: $STORAGE_QUOTA"
echo "Container Port: $PORT"
echo "========================================="

# Function to get storage path
get_storage_path() {
    local location_id=$1
    # Read from system storage configuration
    if [ -f "/etc/orchestrix/storage-locations.conf" ]; then
        grep "^${location_id}.path=" /etc/orchestrix/storage-locations.conf | cut -d'=' -f2
    else
        echo "/home/telcobright/btrfs"  # Default fallback
    fi
}

# Function to setup BTRFS storage
setup_btrfs_storage() {
    local storage_path=$(get_storage_path "$STORAGE_LOCATION_ID")
    local volume_path="${storage_path}/containers/${STORAGE_ROOT}"

    echo "Setting up BTRFS storage at: $volume_path"

    # Check if BTRFS tools are installed
    if ! command -v btrfs &> /dev/null; then
        echo "Installing BTRFS tools..."
        sudo apt-get update
        sudo apt-get install -y btrfs-progs btrfs-tools
    fi

    # Create parent directory
    sudo mkdir -p "${storage_path}/containers"

    # Check if subvolume exists
    if ! sudo btrfs subvolume show "$volume_path" &> /dev/null; then
        echo "Creating BTRFS subvolume..."
        sudo btrfs subvolume create "$volume_path"

        # Set quota if specified
        if [ -n "$STORAGE_QUOTA" ]; then
            echo "Setting quota to $STORAGE_QUOTA..."
            # Enable quota on the filesystem
            local mount_point=$(df "$storage_path" | tail -1 | awk '{print $6}')
            sudo btrfs quota enable "$mount_point" 2>/dev/null || true

            # Get subvolume ID and set quota
            local subvol_id=$(sudo btrfs subvolume show "$volume_path" | grep 'Subvolume ID:' | awk '{print $3}')
            local quota_bytes=$(parse_size "$STORAGE_QUOTA")
            sudo btrfs qgroup limit $quota_bytes 0/$subvol_id "$mount_point"
        fi
    else
        echo "BTRFS subvolume already exists"
    fi

    echo "$volume_path"
}

# Function to parse human-readable size to bytes
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

# Check if container exists
if lxc info $CONTAINER_NAME &>/dev/null; then
    echo "Container $CONTAINER_NAME already exists. Stopping and removing..."
    lxc stop $CONTAINER_NAME --force 2>/dev/null || true
    lxc delete $CONTAINER_NAME --force
fi

# Setup BTRFS storage
VOLUME_PATH=$(setup_btrfs_storage)
echo "Storage volume created at: $VOLUME_PATH"

# Create container with BTRFS backing
echo "Creating container $CONTAINER_NAME..."
lxc init images:debian/$DEBIAN_VERSION $CONTAINER_NAME

# Configure container storage binding
echo "Configuring container storage..."
lxc config device add $CONTAINER_NAME root disk source=$VOLUME_PATH path=/

# Configure container resources
echo "Configuring container resources..."
lxc config set $CONTAINER_NAME limits.memory $MEMORY_LIMIT
lxc config set $CONTAINER_NAME limits.cpu $CPU_LIMIT

# Configure container network
echo "Configuring container network..."
lxc config device add $CONTAINER_NAME eth0 nic name=eth0 nictype=bridged parent=lxdbr0

# Start container
echo "Starting container..."
lxc start $CONTAINER_NAME

# Wait for container to be ready
echo "Waiting for container to be ready..."
sleep 5

# Wait for network
while ! lxc exec $CONTAINER_NAME -- ping -c 1 google.com &>/dev/null; do
    echo "Waiting for network connectivity..."
    sleep 2
done

# Install basic packages
echo "Installing basic packages..."
lxc exec $CONTAINER_NAME -- apt-get update
lxc exec $CONTAINER_NAME -- apt-get install -y \
    curl \
    wget \
    vim \
    net-tools \
    iputils-ping \
    openssh-server \
    sudo \
    python3 \
    python3-pip

# Setup SSH for development
echo "Configuring SSH..."
lxc exec $CONTAINER_NAME -- bash -c "echo 'PermitRootLogin yes' >> /etc/ssh/sshd_config"
lxc exec $CONTAINER_NAME -- bash -c "echo 'PasswordAuthentication yes' >> /etc/ssh/sshd_config"
lxc exec $CONTAINER_NAME -- bash -c "echo 'StrictHostKeyChecking no' >> /etc/ssh/ssh_config"
lxc exec $CONTAINER_NAME -- bash -c "echo 'UserKnownHostsFile /dev/null' >> /etc/ssh/ssh_config"
lxc exec $CONTAINER_NAME -- systemctl restart sshd

# Create unique-id-generator service directory
echo "Setting up unique-id-generator service..."
lxc exec $CONTAINER_NAME -- mkdir -p /opt/unique-id-generator
lxc exec $CONTAINER_NAME -- mkdir -p /var/log/unique-id-generator
lxc exec $CONTAINER_NAME -- mkdir -p /etc/unique-id-generator

# Create a simple unique ID generator script (placeholder)
cat << 'EOF' | lxc exec $CONTAINER_NAME -- tee /opt/unique-id-generator/generator.py
#!/usr/bin/env python3
import uuid
import time
import json
from http.server import HTTPServer, BaseHTTPRequestHandler

class UniqueIDHandler(BaseHTTPRequestHandler):
    def do_GET(self):
        if self.path == '/generate':
            unique_id = str(uuid.uuid4())
            response = {
                'id': unique_id,
                'timestamp': int(time.time()),
                'type': 'uuid4'
            }
            self.send_response(200)
            self.send_header('Content-type', 'application/json')
            self.end_headers()
            self.wfile.write(json.dumps(response).encode())
        elif self.path == '/health':
            self.send_response(200)
            self.send_header('Content-type', 'text/plain')
            self.end_headers()
            self.wfile.write(b'OK')
        else:
            self.send_response(404)
            self.end_headers()

if __name__ == '__main__':
    port = ${PORT}
    server = HTTPServer(('0.0.0.0', port), UniqueIDHandler)
    print(f'Unique ID Generator running on port {port}')
    server.serve_forever()
EOF

lxc exec $CONTAINER_NAME -- chmod +x /opt/unique-id-generator/generator.py

# Create systemd service
cat << EOF | lxc exec $CONTAINER_NAME -- tee /etc/systemd/system/unique-id-generator.service
[Unit]
Description=Unique ID Generator Service
After=network.target

[Service]
Type=simple
User=root
WorkingDirectory=/opt/unique-id-generator
ExecStart=/usr/bin/python3 /opt/unique-id-generator/generator.py
Restart=always
RestartSec=10

[Install]
WantedBy=multi-user.target
EOF

# Enable and start the service
lxc exec $CONTAINER_NAME -- systemctl daemon-reload
lxc exec $CONTAINER_NAME -- systemctl enable unique-id-generator
lxc exec $CONTAINER_NAME -- systemctl start unique-id-generator

# Configure port forwarding
echo "Configuring port forwarding..."
lxc config device add $CONTAINER_NAME unique-id-port proxy \
    listen=tcp:0.0.0.0:$PORT \
    connect=tcp:127.0.0.1:$PORT

# Stop container for export
echo "Stopping container for export..."
lxc stop $CONTAINER_NAME

# Export container
echo "Exporting container to $EXPORT_PATH..."
lxc export $CONTAINER_NAME $EXPORT_PATH

# Create snapshot for backup
if [ "$STORAGE_PROVIDER" = "btrfs" ]; then
    echo "Creating BTRFS snapshot..."
    SNAPSHOT_NAME="${CONTAINER_NAME}_$(date +%Y%m%d_%H%M%S)"
    SNAPSHOT_PATH="${storage_path}/snapshots/${STORAGE_ROOT}/${SNAPSHOT_NAME}"
    sudo mkdir -p "$(dirname "$SNAPSHOT_PATH")"
    sudo btrfs subvolume snapshot -r "$VOLUME_PATH" "$SNAPSHOT_PATH"
    echo "Snapshot created: $SNAPSHOT_PATH"
fi

echo "========================================="
echo "Build completed successfully!"
echo "========================================="
echo "Export file: $EXPORT_PATH"
echo "Container name: $CONTAINER_NAME"
echo "Storage volume: $VOLUME_PATH"
echo "Port: $PORT"
echo ""
echo "To import on another system:"
echo "  lxc import $EXPORT_PATH"
echo ""
echo "To start the container:"
echo "  lxc start $CONTAINER_NAME"
echo "========================================="