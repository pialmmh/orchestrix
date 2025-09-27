#!/bin/bash
# Build Grafana-Loki container using Java automation
# This replaces the shell-based build script with Java automation

set -e

ORCHESTRIX_HOME="/home/mustafa/telcobright-projects/orchestrix"
cd "$ORCHESTRIX_HOME"

CONFIG_FILE="${1:-images/lxc/grafana-loki/build/build.conf}"

echo "========================================="
echo "Building Grafana-Loki using Java Automation"
echo "========================================="
echo "Config: $CONFIG_FILE"

# Step 1: Compile the project if needed
if [ ! -d "target/classes" ] || [ "$2" == "--compile" ]; then
    echo "Compiling Java automation code..."
    mvn compile -DskipTests
fi

# Step 2: Run the container build using Java
echo "Running container build automation..."
sudo mvn exec:java -Dexec.mainClass="com.telcobright.orchestrix.automation.storage.runners.GrafanaLokiBuildRunner" \
    -Dexec.args="$CONFIG_FILE" \
    -Dexec.classpathScope=compile

echo ""
echo "Build complete!"