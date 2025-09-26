#!/bin/bash
# Quick start script for Grafana-Loki container with default configuration

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

# Create default config if it doesn't exist
DEFAULT_CONFIG="$SCRIPT_DIR/default.conf"

if [ ! -f "$DEFAULT_CONFIG" ]; then
    echo "Creating default configuration..."
    cat > "$DEFAULT_CONFIG" << EOF
# Default configuration for Grafana-Loki quick start
container.name=grafana-loki-default
container.version=1.0.0
version.tag=latest
image.path=/tmp/grafana-loki-v1.0.0.tar.gz

# Storage configuration
storage.provider=btrfs
storage.location.id=btrfs_ssd_main
storage.container.root=grafana-loki-default
storage.quota.size=20G
storage.compression=true

# Service versions
grafana.version=10.2.3
loki.version=2.9.4
promtail.version=2.9.4

# Service ports
grafana.port=3000
loki.port=3100
promtail.port=9080

# Resources
memory_limit=2GB
cpu_limit=2

# Debian version
debian.version=bookworm
EOF
    echo "Default configuration created."
fi

# Check if image exists
IMAGE_PATH="/tmp/grafana-loki-v1.0.0.tar.gz"
if [ ! -f "$IMAGE_PATH" ]; then
    echo "Image not found. Building container first..."
    "$SCRIPT_DIR/buildGrafanaLoki.sh" "$DEFAULT_CONFIG"
else
    echo "Using existing image at $IMAGE_PATH"
fi

# Launch the container
echo "Launching container with default configuration..."
"$SCRIPT_DIR/launchGrafanaLoki.sh" "$DEFAULT_CONFIG"

echo ""
echo "========================================="
echo "Grafana-Loki Quick Start Complete!"
echo "========================================="
echo ""
echo "Access Grafana at: http://localhost:3000"
echo "Default login: admin/admin"
echo ""
echo "Loki is pre-configured as a data source in Grafana."
echo "========================================="