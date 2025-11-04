#!/bin/bash
# ============================================
# Quarkus Hello World Container Quick Start
# ============================================
# This script provides a quick way to launch the container with default settings

set -e

# Get the directory where this script is located
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

# Find the latest artifact
LATEST_ARTIFACT=$(ls -t "$SCRIPT_DIR/artifact/quarkus-hello-v1-"*.tar.gz 2>/dev/null | head -1)

if [ -z "$LATEST_ARTIFACT" ]; then
    echo "ERROR: No container artifact found in $SCRIPT_DIR/artifact/"
    exit 1
fi

echo "========================================="
echo "Quarkus Hello World - Quick Start"
echo "========================================="
echo "Artifact: $(basename $LATEST_ARTIFACT)"
echo ""

# Default configuration
CONTAINER_NAME="quarkus-hello-demo"
NETWORK_BRIDGE="lxdbr0"

# Import container
echo "Importing container..."
if lxc list | grep -q "$CONTAINER_NAME"; then
    echo "Container already exists. Stopping and removing..."
    lxc stop "$CONTAINER_NAME" --force 2>/dev/null || true
    lxc delete "$CONTAINER_NAME" --force
fi

lxc import "$LATEST_ARTIFACT" "$CONTAINER_NAME"
echo "✓ Container imported"
echo ""

# Attach to network
echo "Configuring network..."
lxc network attach "$NETWORK_BRIDGE" "$CONTAINER_NAME" eth0 2>/dev/null || true
echo "✓ Network attached"
echo ""

# Start container
echo "Starting container..."
lxc start "$CONTAINER_NAME"
sleep 5

# Wait for network
echo "Waiting for network..."
sleep 10

# Get container IP
CONTAINER_IP=$(lxc list "$CONTAINER_NAME" -c 4 --format csv | cut -d' ' -f1)
echo "✓ Container started"
echo ""

# Configure Loki connection (default to localhost - update as needed)
echo "Configuring Promtail to send logs to Loki..."
echo "NOTE: Update /etc/promtail/promtail-config.yaml with your Loki server address"
echo ""

# Start services
echo "Starting services..."
lxc exec "$CONTAINER_NAME" -- systemctl restart quarkus-hello
sleep 3
lxc exec "$CONTAINER_NAME" -- systemctl restart promtail
sleep 2
echo "✓ Services started"
echo ""

echo "========================================="
echo "✓ Container Ready!"
echo "========================================="
echo "Container Name: $CONTAINER_NAME"
echo "Container IP: $CONTAINER_IP"
echo ""
echo "Services:"
echo "  - Quarkus Hello World: http://$CONTAINER_IP:8080/hello"
echo "  - Health Check: http://$CONTAINER_IP:8080/health"
echo "  - Promtail Metrics: http://$CONTAINER_IP:9080/metrics"
echo ""
echo "Test the API:"
echo "  curl http://$CONTAINER_IP:8080/hello"
echo "  curl http://$CONTAINER_IP:8080/hello?name=YourName"
echo "  curl http://$CONTAINER_IP:8080/hello/status"
echo ""
echo "View logs:"
echo "  lxc exec $CONTAINER_NAME -- tail -f /var/log/quarkus-hello/application.log"
echo ""
echo "Service status:"
echo "  lxc exec $CONTAINER_NAME -- systemctl status quarkus-hello"
echo "  lxc exec $CONTAINER_NAME -- systemctl status promtail"
echo ""
echo "IMPORTANT: Update Loki configuration:"
echo "  lxc exec $CONTAINER_NAME -- vi /etc/promtail/promtail-config.yaml"
echo "  (Set LOKI_HOST and LOKI_PORT to your Grafana-Loki container)"
echo ""
