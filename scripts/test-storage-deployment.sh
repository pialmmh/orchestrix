#!/bin/bash
# Test script for BTRFS storage container deployment

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"

echo "========================================="
echo "Storage Deployment Automation Test"
echo "========================================="

# Function to display usage
usage() {
    echo "Usage: $0 [options] <config-file>"
    echo ""
    echo "Options:"
    echo "  -i, --interactive    Run interactive test mode"
    echo "  -c, --compile        Compile the project first"
    echo "  -h, --help           Show this help message"
    echo ""
    echo "Examples:"
    echo "  $0 -i                                    # Interactive test mode"
    echo "  $0 deployment.properties                 # Run with config file"
    echo "  $0 -c deployment.properties              # Compile and run"
    exit 0
}

# Parse arguments
COMPILE=false
INTERACTIVE=false
CONFIG_FILE=""

while [[ $# -gt 0 ]]; do
    case $1 in
        -i|--interactive)
            INTERACTIVE=true
            shift
            ;;
        -c|--compile)
            COMPILE=true
            shift
            ;;
        -h|--help)
            usage
            ;;
        *)
            CONFIG_FILE="$1"
            shift
            ;;
    esac
done

# Change to project root
cd "$PROJECT_ROOT"

# Compile if requested
if [ "$COMPILE" = true ]; then
    echo "Compiling project..."
    if [ -f "pom.xml" ]; then
        mvn clean compile
    elif [ -f "build.gradle" ]; then
        gradle clean compileJava
    else
        echo "Warning: No build file found, attempting direct javac compilation"
        mkdir -p target/classes
        find src/main/java -name "*.java" | xargs javac -d target/classes -cp "lib/*"
    fi
    echo "Compilation complete"
fi

# Set classpath
if [ -d "target/classes" ]; then
    CLASSPATH="target/classes"
elif [ -d "build/classes/java/main" ]; then
    CLASSPATH="build/classes/java/main"
else
    echo "Error: Compiled classes not found. Please compile the project first."
    exit 1
fi

# Add resources to classpath
if [ -d "src/main/resources" ]; then
    CLASSPATH="${CLASSPATH}:src/main/resources"
fi

# Add lib directory if exists
if [ -d "lib" ]; then
    CLASSPATH="${CLASSPATH}:lib/*"
fi

# Export classpath
export CLASSPATH

# Run the appropriate test
if [ "$INTERACTIVE" = true ]; then
    echo "Starting interactive test mode..."
    java com.telcobright.orchestrix.automation.runner.storage.StorageDeploymentTest
elif [ -n "$CONFIG_FILE" ]; then
    if [ ! -f "$CONFIG_FILE" ]; then
        echo "Error: Configuration file not found: $CONFIG_FILE"
        exit 1
    fi

    echo "Running deployment with configuration: $CONFIG_FILE"
    java com.telcobright.orchestrix.automation.runner.storage.BtrfsContainerDeploymentRunner "$CONFIG_FILE"
else
    usage
fi