#!/bin/bash

# Build Unique ID Generator Container using refactored Java builder
# This script uses the new LxcDevice architecture

set -e

echo "=========================================="
echo "Unique ID Generator Container Builder"
echo "(Refactored with TerminalDevice)"
echo "=========================================="
echo ""

# Change to orchestrix directory
cd /home/mustafa/telcobright-projects/orchestrix

# Configuration file path
CONFIG_FILE="${1:-images/lxc/unique-id-generator/build/build.conf}"

if [ ! -f "$CONFIG_FILE" ]; then
    echo "✗ Configuration file not found: $CONFIG_FILE"
    echo ""
    echo "Usage: $0 [config-file-path]"
    echo "Default: images/lxc/unique-id-generator/build/build.conf"
    exit 1
fi

echo "Using configuration: $CONFIG_FILE"
echo ""

# Set up classpath - include all source directories
export CLASSPATH="src/main/java:images/lxc/unique-id-generator/src/main/java"

# Compile all required Java files
echo "Step 1: Compiling Java classes..."
echo ""

# Create a temporary directory for compiled classes
TEMP_CLASSES="/tmp/unique-id-build-$$"
mkdir -p "$TEMP_CLASSES"

# Compile with explicit output directory to avoid classpath issues
javac -d "$TEMP_CLASSES" -cp "$CLASSPATH" \
    src/main/java/com/telcobright/orchestrix/device/TerminalDevice.java \
    src/main/java/com/telcobright/orchestrix/automation/model/AutomationConfig.java \
    src/main/java/com/telcobright/orchestrix/automation/core/*.java \
    src/main/java/com/telcobright/orchestrix/device/LocalDevice.java \
    src/main/java/com/telcobright/orchestrix/device/LxcDevice.java \
    images/lxc/unique-id-generator/src/main/java/com/telcobright/orchestrix/images/lxc/uniqueidgenerator/entities/*.java \
    images/lxc/unique-id-generator/src/main/java/com/telcobright/orchestrix/images/lxc/uniqueidgenerator/scripts/*.java \
    images/lxc/unique-id-generator/src/main/java/com/telcobright/orchestrix/images/lxc/uniqueidgenerator/*.java \
    2>/dev/null

if [ $? -eq 0 ]; then
    echo "✓ All classes compiled successfully"
else
    echo "✗ Compilation failed"
    echo ""
    echo "Trying verbose compilation for debugging..."
    javac -d "$TEMP_CLASSES" -cp "$CLASSPATH" \
        src/main/java/com/telcobright/orchestrix/device/TerminalDevice.java \
        src/main/java/com/telcobright/orchestrix/automation/model/AutomationConfig.java \
        src/main/java/com/telcobright/orchestrix/automation/core/*.java \
        src/main/java/com/telcobright/orchestrix/device/LocalDevice.java \
        src/main/java/com/telcobright/orchestrix/device/LxcDevice.java \
        images/lxc/unique-id-generator/src/main/java/com/telcobright/orchestrix/images/lxc/uniqueidgenerator/entities/*.java \
        images/lxc/unique-id-generator/src/main/java/com/telcobright/orchestrix/images/lxc/uniqueidgenerator/scripts/*.java \
        images/lxc/unique-id-generator/src/main/java/com/telcobright/orchestrix/images/lxc/uniqueidgenerator/*.java
    rm -rf "$TEMP_CLASSES"
    exit 1
fi

echo ""
echo "Step 2: Running UniqueIdGeneratorBuilder..."
echo "============================================"
echo ""

# Add signal handler for cleanup
cleanup() {
    echo ""
    echo "⚠ Build interrupted - cleaning up..."

    # Try to extract container name from config
    if [ -f "$CONFIG_FILE" ]; then
        CONTAINER_NAME=$(grep "^BUILD_CONTAINER_NAME=" "$CONFIG_FILE" | cut -d= -f2)
        if [ -n "$CONTAINER_NAME" ]; then
            echo "Checking for container: $CONTAINER_NAME"
            if lxc list | grep -q "$CONTAINER_NAME"; then
                echo "Deleting container: $CONTAINER_NAME"
                lxc delete --force "$CONTAINER_NAME" 2>/dev/null || true
            fi
        fi
    fi

    # Clean up temp directory
    rm -rf "$TEMP_CLASSES"

    echo "✓ Cleanup complete"
    exit 130
}

trap cleanup INT TERM

# Run the builder with compiled classes
java -cp "$TEMP_CLASSES:$CLASSPATH" \
    com.telcobright.orchestrix.images.lxc.uniqueidgenerator.UniqueIdGeneratorBuilder \
    "$CONFIG_FILE"

BUILD_RESULT=$?

# Clean up temp directory
rm -rf "$TEMP_CLASSES"

if [ $BUILD_RESULT -eq 0 ]; then
    echo ""
    echo "=========================================="
    echo "✓ Build completed successfully!"
    echo "=========================================="
    echo ""
    echo "Image created: unique-id-generator-base"
    echo ""
    echo "To launch a container:"
    echo "  lxc launch unique-id-generator-base <container-name>"
    echo ""
    echo "Or use the launch script:"
    echo "  ./images/lxc/unique-id-generator/launch/launchUniqueIdGenerator.sh <config-file>"
else
    echo ""
    echo "✗ Build failed with exit code: $BUILD_RESULT"
    exit $BUILD_RESULT
fi