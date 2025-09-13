#!/bin/bash
# Quick start script for FusionPBX container
# Uses the sample configuration file

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

echo "==========================================="
echo "FusionPBX Container - Quick Start"
echo "==========================================="
echo ""

# Check if base image exists
if ! lxc image list | grep -q "fusion-pbx-base"; then
    echo "Base image not found. Building it now..."
    echo "This will take several minutes on first run..."
    echo ""
    "${SCRIPT_DIR}/buildFusionPBX.sh"
    echo ""
fi

# Launch with sample config
echo "Launching FusionPBX container with sample configuration..."
echo ""
"${SCRIPT_DIR}/launchFusionPBX.sh" "${SCRIPT_DIR}/sample-config.conf"

echo ""
echo "==========================================="
echo "Quick Start Complete!"
echo "==========================================="
echo ""
echo "To use a custom configuration:"
echo "  1. Copy sample-config.conf to any location"
echo "  2. Edit the configuration as needed"
echo "  3. Run: ./launchFusionPBX.sh /path/to/your/config.conf"
echo ""