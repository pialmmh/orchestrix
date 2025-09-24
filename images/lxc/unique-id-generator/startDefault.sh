#!/bin/bash
# Quick start script for Unique ID Generator with default configuration

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

echo "================================================"
echo "Unique ID Generator - Quick Start"
echo "================================================"

# Get the latest version
LATEST_VERSION=$(ls -d "$SCRIPT_DIR"/unique-id-generator-v.* 2>/dev/null | sort -V | tail -n1 | xargs basename)
VERSION="${LATEST_VERSION#unique-id-generator-v.}"

# If no version found, use default
if [ -z "$VERSION" ]; then
    VERSION="1.0.0"
fi

IMAGE_NAME="unique-id-generator-base-v.$VERSION"

# Check if the base image exists
if ! lxc image list --format json | grep -q "\"$IMAGE_NAME\""; then
    echo ""
    echo "Base image not found: $IMAGE_NAME"
    echo "Building it first..."
    echo ""

    # Run the build script with default config
    "$SCRIPT_DIR/buildUniqueIdGenerator.sh"

    if [ $? -ne 0 ]; then
        echo "Build failed. Please check the logs."
        exit 1
    fi
else
    echo "Base image found: $IMAGE_NAME"
fi

echo ""
echo "Launching container with default configuration..."
echo ""

# Find the sample config for this version
SAMPLE_CONFIG="$SCRIPT_DIR/unique-id-generator-v.$VERSION/generated/sample.conf"

if [ ! -f "$SAMPLE_CONFIG" ]; then
    echo "Warning: Sample config not found for version $VERSION"
    echo "Creating a default configuration..."

    # Create a minimal default config
    TEMP_CONFIG=$(mktemp)
    cat > "$TEMP_CONFIG" <<EOF
CONTAINER_NAME="uid-generator-default"
BASE_IMAGE="$IMAGE_NAME"
CONTAINER_IP="10.10.199.51/24"
SHARD_ID=1
TOTAL_SHARDS=1
EOF

    SAMPLE_CONFIG="$TEMP_CONFIG"
fi

# Launch with the config
"$SCRIPT_DIR/launchUniqueIdGenerator.sh" "$SAMPLE_CONFIG"

echo ""
echo "================================================"
echo "Quick start complete!"
echo "================================================"