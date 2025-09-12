#!/bin/bash

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

echo "==================== FusionPBX Quick Start ===================="
echo ""
echo "This script will:"
echo "1. Build the FusionPBX base image (if not exists)"
echo "2. Launch a FusionPBX container with default settings"
echo ""

BASE_IMAGE_NAME="fusion-pbx-base"
EXISTING_IMAGE=$(lxc image list --format=json | jq -r ".[] | select(.aliases[].name==\"$BASE_IMAGE_NAME\") | .aliases[].name" | head -n1)

if [ -z "$EXISTING_IMAGE" ]; then
    echo "Base image not found. Building FusionPBX base image..."
    echo "--------------------------------------------------------------"
    "${SCRIPT_DIR}/buildFusionPBX.sh" "${SCRIPT_DIR}/sample-config.conf"
    echo "--------------------------------------------------------------"
    echo ""
else
    echo "Base image '$BASE_IMAGE_NAME' already exists."
    read -p "Do you want to rebuild it? (y/n): " -n 1 -r
    echo
    if [[ $REPLY =~ ^[Yy]$ ]]; then
        echo "Rebuilding base image..."
        echo "--------------------------------------------------------------"
        "${SCRIPT_DIR}/buildFusionPBX.sh" "${SCRIPT_DIR}/sample-config.conf"
        echo "--------------------------------------------------------------"
        echo ""
    fi
fi

echo "Launching FusionPBX container with default configuration..."
echo "--------------------------------------------------------------"
"${SCRIPT_DIR}/launchFusionPBX.sh" "${SCRIPT_DIR}/sample-config.conf"
echo "--------------------------------------------------------------"

echo ""
echo "==================== Quick Start Complete ===================="
echo ""
echo "Your FusionPBX container is now running!"
echo ""
echo "To customize the configuration:"
echo "1. Copy sample-config.conf to your desired location"
echo "2. Edit the configuration as needed"
echo "3. Run: ${SCRIPT_DIR}/launchFusionPBX.sh /path/to/your/config.conf"
echo ""
echo "To stop the container:"
echo "  lxc stop fusion-pbx-dev"
echo ""
echo "To restart the container:"
echo "  lxc start fusion-pbx-dev"
echo ""
echo "To delete the container:"
echo "  lxc delete fusion-pbx-dev --force"