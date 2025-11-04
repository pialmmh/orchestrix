#!/bin/bash
#
# Wrapper script for Java JAR Alpine scaffolding automation
# Uses JavaJarAlpineScaffold class to create container structure
#

set -e

# Base paths
ORCHESTRIX_DIR="/home/mustafa/telcobright-projects/orchestrix"
AUTOMATION_DIR="$ORCHESTRIX_DIR/automation"
SRC_DIR="$ORCHESTRIX_DIR/src/main/java"

# Check for required parameters
if [ $# -lt 3 ]; then
    echo "Usage: $0 <app-name> <version> <jar-path> [java-version] [include-promtail]"
    echo ""
    echo "Examples:"
    echo "  $0 my-service 1 /path/to/my-service.jar"
    echo "  $0 my-service 1 /path/to/my-service.jar 21 true"
    echo ""
    echo "Java versions: 11, 17, 21 (default), 22"
    exit 1
fi

APP_NAME="$1"
VERSION="$2"
JAR_PATH="$3"
JAVA_VERSION="${4:-21}"
INCLUDE_PROMTAIL="${5:-true}"

# Verify JAR exists
if [ ! -f "$JAR_PATH" ]; then
    echo "ERROR: JAR file not found: $JAR_PATH"
    exit 1
fi

# Create expected JAR location
JAR_DIR="$ORCHESTRIX_DIR/images/standalone-binaries/$APP_NAME/${APP_NAME}-jar-v.$VERSION"
mkdir -p "$JAR_DIR"

# Copy JAR to expected location
echo "Copying JAR to standard location..."
cp "$JAR_PATH" "$JAR_DIR/${APP_NAME}.jar"

# Compile Java class if needed
echo "Compiling Java scaffolding class..."
cd "$ORCHESTRIX_DIR"
javac -cp "$SRC_DIR" \
    "$SRC_DIR/com/telcobright/orchestrix/automation/scaffold/JavaJarAlpineScaffold.java" \
    "$SRC_DIR/com/telcobright/orchestrix/automation/scaffold/AlpineContainerScaffold.java" \
    "$SRC_DIR/com/telcobright/orchestrix/automation/scaffold/entity/AlpineScaffoldConfig.java" \
    "$SRC_DIR/com/telcobright/orchestrix/automation/core/device/CommandExecutor.java" \
    "$SRC_DIR/com/telcobright/orchestrix/automation/core/device/LocalDevice.java"

# Create config file for scaffolding
CONFIG_FILE="/tmp/scaffold-config-$$.properties"
cat > "$CONFIG_FILE" << EOF
# Scaffold Configuration
SERVICE_NAME=$APP_NAME
CONTAINER_VERSION=$VERSION
CONTAINER_PATH=$ORCHESTRIX_DIR/images/containers/lxc/$APP_NAME
BINARY_PATH=$JAR_DIR/${APP_NAME}.jar
JAVA_VERSION=$JAVA_VERSION
JVM_OPTS=-Xms256m -Xmx512m
MAIN_CLASS=
INCLUDE_PROMTAIL=$INCLUDE_PROMTAIL
PROMTAIL_VERSION=2.9.4
LOKI_ENDPOINT=http://grafana-loki:3100
EOF

# Run Java scaffolding
echo "Running Java JAR scaffolding..."
java -cp "$ORCHESTRIX_DIR/src/main/java" \
    com.telcobright.orchestrix.automation.scaffold.JavaJarAlpineScaffold \
    "$CONFIG_FILE"

# Clean up temp config
rm -f "$CONFIG_FILE"

echo ""
echo "========================================="
echo "✓ JAR Scaffolding Complete!"
echo "========================================="
echo "Container directory: $ORCHESTRIX_DIR/images/containers/lxc/$APP_NAME"
echo "JAR location: $JAR_DIR/${APP_NAME}.jar"
echo ""
echo "To build and run:"
echo "  cd $ORCHESTRIX_DIR/images/containers/lxc/$APP_NAME"
echo "  ./startDefault.sh"
echo "========================================="