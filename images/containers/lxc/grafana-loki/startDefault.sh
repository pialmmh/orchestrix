#!/bin/bash
#
# Quick Start Script for Grafana-Loki Container
# This script builds (if needed) and launches the container with default settings
#

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
BUILD_SCRIPT="$SCRIPT_DIR/build/build.sh"
BUILD_CONF="$SCRIPT_DIR/build/build.conf"

echo "========================================="
echo "Grafana-Loki Container - Quick Start"
echo "========================================="
echo ""

# Check if build directory exists
if [ ! -d "$SCRIPT_DIR/build" ]; then
    echo "ERROR: build/ directory not found"
    exit 1
fi

# Load version from config
if [ -f "$BUILD_CONF" ]; then
    source "$BUILD_CONF"
    VERSION_DIR="$SCRIPT_DIR/grafana-loki-v.${CONTAINER_VERSION}"
    GENERATED_DIR="$VERSION_DIR/generated"
    ARTIFACT_DIR="$GENERATED_DIR/artifact"
else
    echo "ERROR: build.conf not found"
    exit 1
fi

# Check if container image already exists
if [ -d "$ARTIFACT_DIR" ] && [ -n "$(ls -A $ARTIFACT_DIR/*.tar.gz 2>/dev/null)" ]; then
    echo "✓ Container image found in: $ARTIFACT_DIR"
    echo ""
    read -p "Rebuild container? (y/N): " -n 1 -r
    echo
    if [[ ! $REPLY =~ ^[Yy]$ ]]; then
        echo "Using existing image..."
        SKIP_BUILD=true
    fi
fi

# Build if needed
if [ "$SKIP_BUILD" != "true" ]; then
    echo "Building container..."
    echo "This may take 10-15 minutes..."
    echo ""
    "$BUILD_SCRIPT" "$BUILD_CONF"
    echo ""
fi

# Launch using version-specific startDefault.sh
if [ -f "$GENERATED_DIR/startDefault.sh" ]; then
    echo "Launching container from generated script..."
    "$GENERATED_DIR/startDefault.sh"
else
    echo "ERROR: Generated launch script not found"
    echo "Expected: $GENERATED_DIR/startDefault.sh"
    echo ""
    echo "Try rebuilding:"
    echo "  $BUILD_SCRIPT"
    exit 1
fi