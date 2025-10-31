#!/bin/bash

# Quick start script for Zookeeper
# Uses the latest version's generated template

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

# Find latest version
LATEST_VERSION=$(ls -d "${SCRIPT_DIR}"/zookeeper-v.* 2>/dev/null | sort -V | tail -n1)

if [ -z "$LATEST_VERSION" ]; then
    echo "Error: No generated versions found. Please build first:"
    echo "  cd build && ./build.sh"
    exit 1
fi

GENERATED_START="${LATEST_VERSION}/generated/startDefault.sh"

if [ ! -f "$GENERATED_START" ]; then
    echo "Error: startDefault.sh not found in $(basename "$LATEST_VERSION")/generated/"
    echo "Please rebuild: cd build && ./build.sh"
    exit 1
fi

echo "Using $(basename "$LATEST_VERSION")/generated/startDefault.sh"
exec "$GENERATED_START"
