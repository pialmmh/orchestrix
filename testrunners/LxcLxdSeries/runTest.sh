#!/bin/bash

# LXC/LXD Series Automation Test Runner
# =====================================
# Demonstrates running multiple automation tasks with a single SSH connection

# Get the directory of this script
SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
PROJECT_ROOT="$( cd "$SCRIPT_DIR/../.." && pwd )"

# Default configuration file
CONFIG_FILE="${1:-$SCRIPT_DIR/config.properties}"

# Check if configuration file exists
if [ ! -f "$CONFIG_FILE" ]; then
    echo "Error: Configuration file not found: $CONFIG_FILE"
    echo "Usage: $0 [config.properties]"
    echo ""
    echo "Available configurations:"
    ls -1 $SCRIPT_DIR/*.properties 2>/dev/null
    exit 1
fi

echo "========================================="
echo "LXC/LXD Series Automation Test"
echo "========================================="
echo "Configuration: $CONFIG_FILE"
echo "Project Root: $PROJECT_ROOT"
echo ""

# Change to project root directory
cd "$PROJECT_ROOT"

# Check if project is compiled
if [ ! -d "target/classes" ]; then
    echo "Project not compiled. Running Maven compile..."
    mvn compile
    if [ $? -ne 0 ]; then
        echo "Error: Maven compilation failed"
        exit 1
    fi
fi

# Compile test runner if needed
TEST_CLASS="target/test-classes/testrunners/LxcLxdSeries/TestLxcLxdSeriesAutomation.class"
if [ ! -f "$TEST_CLASS" ]; then
    echo "Compiling test runner..."
    mkdir -p target/test-classes
    javac -cp "target/classes:lib/*" \
          -d target/test-classes \
          testrunners/LxcLxdSeries/TestLxcLxdSeriesAutomation.java

    if [ $? -ne 0 ]; then
        echo "Error: Failed to compile test runner"
        exit 1
    fi
fi

# Run the test
echo "Running series automation with configuration: $(basename $CONFIG_FILE)"
echo "-----------------------------------------"

java -cp "target/classes:target/test-classes:lib/*" \
     testrunners.LxcLxdSeries.TestLxcLxdSeriesAutomation \
     "$CONFIG_FILE"

TEST_RESULT=$?

echo ""
echo "========================================="
if [ $TEST_RESULT -eq 0 ]; then
    echo "Series automation completed successfully"
else
    echo "Series automation failed with exit code: $TEST_RESULT"
fi
echo "========================================="

exit $TEST_RESULT