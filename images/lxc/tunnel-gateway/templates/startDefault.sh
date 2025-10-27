#!/bin/bash
# Quick start script - launches with default configuration

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
CONTAINER_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
CONTAINER_BASE=$(basename "$CONTAINER_DIR")

# Find latest version
LATEST_VERSION=$(ls -d "$CONTAINER_DIR"/${CONTAINER_BASE}-v.* 2>/dev/null | \
                 grep -o 'v\.[0-9]*' | \
                 cut -d. -f2 | \
                 sort -n | \
                 tail -1)

if [ -z "$LATEST_VERSION" ]; then
    echo "Error: No built versions found"
    echo "Please build the image first:"
    echo "  cd build && ./build.sh"
    exit 1
fi

VERSION_DIR="${CONTAINER_DIR}/${CONTAINER_BASE}-v.${LATEST_VERSION}"
GENERATED_DIR="${VERSION_DIR}/generated"

if [ ! -d "$GENERATED_DIR" ]; then
    echo "Error: Generated directory not found: $GENERATED_DIR"
    exit 1
fi

echo "Starting tunnel-gateway v.${LATEST_VERSION} with default configuration..."
echo ""

cd "$GENERATED_DIR"
exec ./launchTunnelGateway.sh sample.conf
