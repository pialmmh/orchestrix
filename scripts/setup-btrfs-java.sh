#!/bin/bash
# Setup BTRFS using Java automation
# This script compiles and runs the Java automation code

set -e

ORCHESTRIX_HOME="/home/mustafa/telcobright-projects/orchestrix"
cd "$ORCHESTRIX_HOME"

echo "========================================="
echo "BTRFS Setup using Java Automation"
echo "========================================="

# Step 1: Compile the project
echo "Compiling Java automation code..."
mvn compile -DskipTests

# Step 2: Run the BTRFS setup using Java
echo "Running BTRFS setup automation..."
sudo mvn exec:java -Dexec.mainClass="com.telcobright.orchestrix.automation.storage.runners.LocalBtrfsSetupRunner" \
    -Dexec.classpathScope=compile

echo ""
echo "Setup complete! You can now build containers using Java automation."