#!/bin/bash
# Quick start script for FusionPBX container

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

# Find the latest version
LATEST_VERSION=$(ls -d ${SCRIPT_DIR}/fusion-pbx-v.* 2>/dev/null | sort -V | tail -1)

if [ -z "$LATEST_VERSION" ]; then
    echo "Error: No built version found. Run build/build.sh first."
    exit 1
fi

echo "Starting FusionPBX using version: $(basename $LATEST_VERSION)"

# Launch with sample config
"${LATEST_VERSION}/generated/launch.sh" "${LATEST_VERSION}/generated/sample.conf"
