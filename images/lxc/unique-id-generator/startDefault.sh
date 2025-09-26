#!/bin/bash
# Quick start script with default configuration

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

# Create default config if it doesn't exist
DEFAULT_CONFIG="$SCRIPT_DIR/default.conf"

if [ ! -f "$DEFAULT_CONFIG" ]; then
    echo "Creating default configuration..."
    cat > "$DEFAULT_CONFIG" << EOF
# Default configuration for quick start
container.name=uid-generator-default
image.path=/tmp/unique-id-generator-base.tar.gz
storage.provider=btrfs
storage.location.id=btrfs_ssd_main
storage.container.root=unique-id-generator
storage.quota.size=10G
port=7001
memory_limit=512MB
cpu_limit=1
debian_version=bookworm
EOF
fi

# Check if image exists
if [ ! -f "/tmp/unique-id-generator-base.tar.gz" ]; then
    echo "Image not found. Building container first..."
    "$SCRIPT_DIR/buildUniqueIdGenerator.sh" "$DEFAULT_CONFIG"
else
    echo "Using existing image at /tmp/unique-id-generator-base.tar.gz"
fi

# Launch the container
echo "Launching container with default configuration..."
"$SCRIPT_DIR/launchUniqueIdGenerator.sh" "$DEFAULT_CONFIG"