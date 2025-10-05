#!/bin/bash
# Build script for Go ID Generator Container
# Follows Orchestrix container scaffolding standards
# Uses Java automation for all operations

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
CONTAINER_DIR="$(dirname "$SCRIPT_DIR")"
ORCHESTRIX_HOME="/home/mustafa/telcobright-projects/orchestrix"

# Load configuration
CONFIG_FILE="${1:-${SCRIPT_DIR}/build.conf}"

if [ ! -f "$CONFIG_FILE" ]; then
    echo "Error: Configuration file not found: $CONFIG_FILE"
    exit 1
fi

echo "Loading configuration from: $CONFIG_FILE"
source "$CONFIG_FILE"

echo "========================================="
echo "Building Go ID Generator Container"
echo "========================================="
echo "Version: $CONTAINER_VERSION"
echo "Go: $GO_VERSION"
echo "Port: $SERVICE_PORT"
echo "Storage: $STORAGE_LOCATION_ID ($STORAGE_QUOTA_SIZE)"
echo "========================================="

# Build container using Java automation
cd "$ORCHESTRIX_HOME"

# Set Java 21 (required for compilation)
export JAVA_HOME=/usr/lib/jvm/java-1.21.0-openjdk-amd64

# Compile if needed
if [ ! -d "target/classes" ]; then
    echo "Compiling Java automation..."
    mvn compile -DskipTests
fi

# Run Java automation for container build (SSH-based, no ProcessBuilder)
echo "Running Java automation for Go ID container build..."
echo "NOTE: Running as current user (not sudo) to use SSH authentication"
JAVA_HOME=$JAVA_HOME mvn exec:java \
    -Dexec.mainClass="com.telcobright.orchestrix.automation.api.container.lxc.app.goid.example.GoIdBuildRunner" \
    -Dexec.args="$CONFIG_FILE" \
    -Dexec.classpathScope=compile

echo ""
echo "========================================="
echo "Build Complete!"
echo "========================================="
