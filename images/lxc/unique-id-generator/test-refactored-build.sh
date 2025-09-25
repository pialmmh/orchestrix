#!/bin/bash

# Test the refactored UniqueIdGeneratorBuilder

echo "Testing Refactored Unique ID Generator Builder"
echo "=============================================="
echo ""

# Set up classpath
CP="src/main/java"
CP="$CP:images/lxc/unique-id-generator/src/main/java"

# Compile all required files
echo "Step 1: Compiling Java files..."
echo ""

# First compile the core classes
# Find all required Java files
CORE_FILES=""
CORE_FILES="$CORE_FILES src/main/java/com/telcobright/orchestrix/device/TerminalDevice.java"
CORE_FILES="$CORE_FILES src/main/java/com/telcobright/orchestrix/automation/model/AutomationConfig.java"
CORE_FILES="$CORE_FILES src/main/java/com/telcobright/orchestrix/automation/core/LocalCommandExecutor.java"
CORE_FILES="$CORE_FILES src/main/java/com/telcobright/orchestrix/automation/core/BaseAutomation.java"
CORE_FILES="$CORE_FILES src/main/java/com/telcobright/orchestrix/device/LocalDevice.java"
CORE_FILES="$CORE_FILES src/main/java/com/telcobright/orchestrix/device/LxcDevice.java"

javac -cp "$CP" $CORE_FILES 2>&1

if [ $? -ne 0 ]; then
    echo "✗ Failed to compile core classes"
    exit 1
fi

echo "✓ Core classes compiled"

# Compile unique-id-generator classes
javac -cp "$CP" \
    images/lxc/unique-id-generator/src/main/java/com/telcobright/orchestrix/images/lxc/uniqueidgenerator/entities/*.java \
    images/lxc/unique-id-generator/src/main/java/com/telcobright/orchestrix/images/lxc/uniqueidgenerator/scripts/*.java \
    images/lxc/unique-id-generator/src/main/java/com/telcobright/orchestrix/images/lxc/uniqueidgenerator/*.java \
    2>&1

if [ $? -ne 0 ]; then
    echo "✗ Failed to compile unique-id-generator classes"
    exit 1
fi

echo "✓ Unique-id-generator classes compiled"
echo ""

# Create a test configuration file
echo "Step 2: Creating test configuration..."
cat > /tmp/test-unique-id-build.conf <<EOF
# Test configuration for unique-id-generator build
BUILD_CONTAINER_NAME=unique-id-test-build
IMAGE_NAME=unique-id-generator-refactored-test
BASE_IMAGE=images:debian/12
BUILD_IP=10.100.100.199/24
GATEWAY=10.100.100.1
SERVER_JS_PATH=$(pwd)/images/lxc/unique-id-generator/scripts/server.js
PACKAGE_JSON_PATH=$(pwd)/images/lxc/unique-id-generator/scripts/package.json
EOF

echo "✓ Test configuration created"
echo ""

# Run the builder in test mode
echo "Step 3: Running builder test..."
echo ""
echo "Configuration:"
cat /tmp/test-unique-id-build.conf
echo ""
echo "Press Ctrl+C to cancel, or Enter to continue..."
read -r

# Run the builder
java -cp "$CP" \
    com.telcobright.orchestrix.images.lxc.uniqueidgenerator.UniqueIdGeneratorBuilder \
    /tmp/test-unique-id-build.conf

if [ $? -eq 0 ]; then
    echo ""
    echo "✓ Test completed successfully"
    echo ""
    echo "Image created: unique-id-generator-refactored-test"
    echo ""
    echo "To launch a container from this image:"
    echo "  lxc launch unique-id-generator-refactored-test my-unique-id-instance"
else
    echo ""
    echo "✗ Test failed"
    exit 1
fi