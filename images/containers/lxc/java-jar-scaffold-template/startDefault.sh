#!/bin/bash
#
# Quick Start Script - Build and Launch with Default Settings
# This provides one-click setup for development
#

set -e

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"

echo "========================================="
echo "Java JAR Container - Quick Start"
echo "========================================="

# Step 1: Check for JAR file
echo "Checking for JAR file..."
source "$SCRIPT_DIR/build/build.conf"

if [ ! -f "$JAR_SOURCE" ]; then
    echo ""
    echo "ERROR: JAR file not found at: $JAR_SOURCE"
    echo ""
    echo "Please build your JAR first and place it at:"
    echo "  $JAR_SOURCE"
    echo ""
    echo "Example:"
    echo "  mvn clean package"
    echo "  mkdir -p $(dirname $JAR_SOURCE)"
    echo "  cp target/your-app.jar $JAR_SOURCE"
    echo ""
    exit 1
fi

echo "✓ JAR found: $JAR_SOURCE"

# Step 2: Build the image
echo ""
echo "Building container image..."
"$SCRIPT_DIR/build/build.sh" "$SCRIPT_DIR/build/build.conf"

# Step 3: Launch container
echo ""
echo "Launching container..."
"$SCRIPT_DIR/launch.sh" "$SCRIPT_DIR/launch.conf"

echo ""
echo "========================================="
echo "✓ Quick start complete!"
echo "========================================="