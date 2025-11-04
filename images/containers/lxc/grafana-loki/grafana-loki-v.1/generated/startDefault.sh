#!/bin/bash
#
# Quick Start Script for Grafana-Loki Container (Version-Specific)
# Generated from template
#

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
CONTAINER_DIR="$(dirname "$(dirname "$SCRIPT_DIR")")"

echo "========================================="
echo "Grafana-Loki Quick Start"
echo "========================================="

# Check if build is needed
ARTIFACT_DIR="$SCRIPT_DIR/artifact"
if [ ! -d "$ARTIFACT_DIR" ] || [ -z "$(ls -A $ARTIFACT_DIR/*.tar.gz 2>/dev/null)" ]; then
    echo "No container image found. Building..."
    "$CONTAINER_DIR/build/build.sh"
fi

# Use the most recent artifact
LATEST_IMAGE=$(ls -t $ARTIFACT_DIR/*.tar.gz 2>/dev/null | head -1)

if [ -z "$LATEST_IMAGE" ]; then
    echo "ERROR: No container image found after build"
    exit 1
fi

echo "Using image: $LATEST_IMAGE"

# Create default launch config
DEFAULT_CONFIG="/tmp/grafana-loki-quickstart.conf"
cat > "$DEFAULT_CONFIG" << 'EOF'
#!/bin/bash
# Quick start configuration

# Container settings
CONTAINER_NAME="grafana-loki-quickstart"
IMAGE_FILE="__IMAGE_FILE__"
CONTAINER_VERSION="1"

# Storage (adjust as needed)
STORAGE_LOCATION_ID="btrfs_local_main"
STORAGE_QUOTA_SIZE="20G"

# Ports (using 7000 range per guidelines)
GRAFANA_PORT="7300"
LOKI_PORT="7310"
PROMTAIL_PORT="7320"

# Resources
MEMORY_LIMIT="2GB"
CPU_LIMIT="2"

# Services
START_SERVICES="true"
EOF

# Replace placeholder with actual image path
sed -i "s|__IMAGE_FILE__|$LATEST_IMAGE|" "$DEFAULT_CONFIG"

# Launch the container
echo "Launching container..."
# TODO: Create launch script or import and start container here
lxc image import "$LATEST_IMAGE" --alias grafana-loki-quickstart-image 2>/dev/null || true
lxc launch grafana-loki-quickstart-image grafana-loki-quickstart || echo "Container may already exist"

# Wait for container
sleep 5

# Get container IP
CONTAINER_IP=$(lxc list grafana-loki-quickstart -c4 --format csv | cut -d' ' -f1)

echo ""
echo "========================================="
echo "✓ Grafana-Loki Quick Start Complete!"
echo "========================================="
echo "Container: grafana-loki-quickstart"
echo "IP Address: $CONTAINER_IP"
echo ""
echo "Access Grafana:"
echo "  http://$CONTAINER_IP:3000"
echo "  http://localhost:7300 (if port forwarding configured)"
echo ""
echo "Default Login: admin/admin"
echo ""
echo "Loki API:"
echo "  http://$CONTAINER_IP:3100"
echo "  http://localhost:7310 (if port forwarding configured)"
echo ""
echo "View container:"
echo "  lxc list grafana-loki-quickstart"
echo ""
echo "Enter container:"
echo "  lxc exec grafana-loki-quickstart -- bash"
echo "========================================="