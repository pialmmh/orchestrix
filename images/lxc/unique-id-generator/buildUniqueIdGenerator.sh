#!/bin/bash
# Build wrapper script for Unique ID Generator Container
# Delegates to versioned build system in build/ directory

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
BUILD_SCRIPT="$SCRIPT_DIR/build/build.sh"
CONFIG_FILE="${1:-$SCRIPT_DIR/build/build.conf}"

# Check if build script exists
if [ ! -f "$BUILD_SCRIPT" ]; then
    echo "Error: Build script not found: $BUILD_SCRIPT"
    exit 1
fi

# Check if config exists
if [ ! -f "$CONFIG_FILE" ]; then
    echo "Error: Configuration file not found: $CONFIG_FILE"
    echo "Usage: $0 [config-file]"
    exit 1
fi

# Execute the build script
exec "$BUILD_SCRIPT" "$CONFIG_FILE"