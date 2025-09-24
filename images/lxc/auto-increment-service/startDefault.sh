#!/bin/bash

set -e

SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

echo "========================================="
echo "Sequence Service Quick Start"
echo "========================================="
echo ""

# Check if base image exists
if ! lxc image list --format json | grep -q '"sequence-service-base"'; then
    echo "ERROR: Base image not found. Please run buildAutoIncrement.sh first"
    echo ""
    echo "To build the base image:"
    echo "  ./buildAutoIncrement.sh"
    echo ""
    exit 1
fi

# Check for existing default container
DEFAULT_CONTAINER="sequence-default"
if lxc info "$DEFAULT_CONTAINER" &>/dev/null 2>&1; then
    echo "Default container '$DEFAULT_CONTAINER' already exists."
    read -p "Delete and recreate? (y/N): " -n 1 -r
    echo
    if [[ $REPLY =~ ^[Yy]$ ]]; then
        lxc delete --force "$DEFAULT_CONTAINER"
    else
        echo "Container already running. Access it with:"
        echo "  lxc exec $DEFAULT_CONTAINER -- bash"
        exit 0
    fi
fi

# Find the latest version directory
VERSION_DIR=$(ls -d "$SCRIPT_DIR"/sequence-service-v.* 2>/dev/null | sort -V | tail -1)

if [ -z "$VERSION_DIR" ] || [ ! -f "$VERSION_DIR/generated/sample.conf" ]; then
    echo "ERROR: No generated configuration found."
    echo "Please run buildAutoIncrement.sh first to generate the configuration."
    exit 1
fi

# Create a default config based on sample
DEFAULT_CONFIG="/tmp/sequence-default.conf"
cp "$VERSION_DIR/generated/sample.conf" "$DEFAULT_CONFIG"

# Modify config for default container
sed -i 's/CONTAINER_NAME="sequence-1"/CONTAINER_NAME="sequence-default"/' "$DEFAULT_CONFIG"

echo "Launching default container with minimal configuration..."
echo ""

# Launch with default config
bash "$SCRIPT_DIR/launchSequenceService.sh" "$DEFAULT_CONFIG"

# Clean up temp config
rm -f "$DEFAULT_CONFIG"

echo ""
echo "========================================="
echo "Quick Start Complete!"
echo "========================================="
echo ""
echo "Default container '$DEFAULT_CONTAINER' is running."
echo ""
echo "To test the service:"
echo "  # Get container IP"
echo "  lxc list $DEFAULT_CONTAINER"
echo ""
echo "  # Test API examples (replace <IP> with container IP)"
echo "  curl 'http://<IP>:7001/api/next-id/test?dataType=int'"
echo "  curl 'http://<IP>:7001/api/next-id/session?dataType=uuid16'"
echo "  curl 'http://<IP>:7001/api/next-batch/orders?dataType=long&batchSize=100'"
echo ""
echo "To access the container:"
echo "  lxc exec $DEFAULT_CONTAINER -- bash"
echo ""
echo "To use CLI reset command:"
echo "  lxc exec $DEFAULT_CONTAINER -- sequence-reset -h  # Show help"
echo "  lxc exec $DEFAULT_CONTAINER -- sequence-reset -e users -v 1000  # Set counter"
echo "  lxc exec $DEFAULT_CONTAINER -- sequence-reset -e tokens -t uuid22  # Change type"
echo ""
echo "To stop the container:"
echo "  lxc stop $DEFAULT_CONTAINER"
echo ""