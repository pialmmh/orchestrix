#!/bin/bash
# Build the Java-based container builder library

set -e

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
cd "$SCRIPT_DIR"

# Check if Maven is installed
if ! command -v mvn &> /dev/null; then
    echo "Maven not found. Installing..."
    sudo apt-get update
    sudo apt-get install -y maven
fi

# Build the project
echo "Building container-builder library..."
mvn clean package -q

echo "✅ Builder library compiled successfully"
echo "    JAR location: $SCRIPT_DIR/target/container-builder-1.0.0.jar"