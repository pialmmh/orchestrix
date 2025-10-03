#!/bin/bash
# Build script for Quarkus Runner Base Container
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

# Source configuration
source "$CONFIG_FILE"

# Determine build type
if [ "${BUILD_TYPE:-base}" = "base" ]; then
    echo "========================================="
    echo "Building Quarkus Runner BASE Container"
    echo "========================================="
    echo "This builds the infrastructure container"
    echo "App containers will extend this base"
    echo "========================================="
    echo "Version: $CONTAINER_VERSION"
    echo "JVM: $JVM_DISTRIBUTION $JVM_VERSION"
    echo "Maven: $MAVEN_VERSION"
    echo "Quarkus: $QUARKUS_VERSION"
    echo "Promtail: $PROMTAIL_VERSION"
    echo "Storage: $STORAGE_LOCATION_ID ($STORAGE_QUOTA_SIZE)"
    echo "========================================="

    # Build base container using Java automation
    cd "$ORCHESTRIX_HOME"

    # Compile if needed
    if [ ! -d "target/classes" ]; then
        echo "Compiling Java automation..."
        mvn compile -DskipTests
    fi

    # Run Java automation for base container build
    echo "Running Java automation for base container build..."
    sudo mvn exec:java \
        -Dexec.mainClass="com.telcobright.orchestrix.automation.api.container.lxc.app.quarkus.example.QuarkusBaseContainerBuilder" \
        -Dexec.args="$CONFIG_FILE" \
        -Dexec.classpathScope=compile

else
    echo "========================================="
    echo "Building Quarkus APP Container"
    echo "========================================="
    echo "Extending base: $BASE_CONTAINER"
    echo "App: $APP_NAME v$APP_VERSION"
    echo "JAR: $APP_JAR_PATH"
    echo "========================================="

    # Validate JAR exists
    if [ ! -f "$APP_JAR_PATH" ]; then
        echo "Error: Application JAR not found: $APP_JAR_PATH"
        echo "Build your Quarkus app first: mvn clean package"
        exit 1
    fi

    # Build app container using Java automation
    cd "$ORCHESTRIX_HOME"

    # Compile if needed
    if [ ! -d "target/classes" ]; then
        echo "Compiling Java automation..."
        mvn compile -DskipTests
    fi

    # Run Java automation for app container build
    echo "Running Java automation for app container build..."
    sudo mvn exec:java \
        -Dexec.mainClass="com.telcobright.orchestrix.automation.api.container.lxc.app.quarkus.example.QuarkusAppContainerBuilder" \
        -Dexec.args="$CONFIG_FILE" \
        -Dexec.classpathScope=compile
fi

echo ""
echo "========================================="
echo "Build Complete!"
echo "========================================="
