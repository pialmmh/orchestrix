#!/bin/bash
# Build script for Unique ID Generator LXC Container
# Version: 1.0.0
# Uses Java automation for orchestrated build process

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
CONTAINER_DIR="$(dirname "$SCRIPT_DIR")"
CONFIG_FILE="${1:-$SCRIPT_DIR/build.conf}"

# Load configuration
source "$CONFIG_FILE"

echo "=========================================="
echo "Unique ID Generator Container Builder"
echo "Version: $VERSION"
echo "=========================================="
echo ""

# Function to print section headers
print_section() {
    echo ""
    echo "▶ $1"
    echo "─────────────────────────────────────"
}

# Function to print success messages
print_success() {
    echo "✓ $1"
}

# Function to print error messages
print_error() {
    echo "✗ $1" >&2
}

# Check prerequisites
print_section "Checking Prerequisites"

if ! command -v lxc &> /dev/null; then
    print_error "LXC is not installed"
    exit 1
fi
print_success "LXC found"

if ! command -v java &> /dev/null; then
    print_error "Java is not installed"
    exit 1
fi
print_success "Java found: $(java -version 2>&1 | head -n1)"

if ! command -v javac &> /dev/null; then
    print_error "Java compiler (javac) is not installed"
    exit 1
fi
print_success "Java compiler found: $(javac -version 2>&1)"

# Check source files
print_section "Verifying Source Files"

if [ ! -f "$CONTAINER_DIR/$SERVER_JS_PATH" ]; then
    print_error "server.js not found at: $CONTAINER_DIR/$SERVER_JS_PATH"
    exit 1
fi
print_success "Found server.js"

if [ ! -f "$CONTAINER_DIR/$PACKAGE_JSON_PATH" ]; then
    print_error "package.json not found at: $CONTAINER_DIR/$PACKAGE_JSON_PATH"
    exit 1
fi
print_success "Found package.json"

# Create output directory
print_section "Setting Up Build Environment"

OUTPUT_PATH="$CONTAINER_DIR/unique-id-generator-v.$VERSION"
mkdir -p "$OUTPUT_PATH/generated"
print_success "Created output directory: $OUTPUT_PATH"

# Copy build configuration to output
cp "$CONFIG_FILE" "$OUTPUT_PATH/buildConfig.cnf"
print_success "Copied build configuration"

# Compile Java automation
print_section "Compiling Java Automation"

SRC_DIR="$CONTAINER_DIR/src/main/java"
BUILD_DIR="$CONTAINER_DIR/target/classes"

if [ ! -d "$SRC_DIR" ]; then
    print_error "Java source directory not found: $SRC_DIR"
    exit 1
fi

# Create build directory
mkdir -p "$BUILD_DIR"

# Find all Java files
JAVA_FILES=$(find "$SRC_DIR" -name "*.java" -type f)

if [ -z "$JAVA_FILES" ]; then
    print_error "No Java source files found in $SRC_DIR"
    exit 1
fi

# Count Java files
JAVA_FILE_COUNT=$(echo "$JAVA_FILES" | wc -l)
print_success "Found $JAVA_FILE_COUNT Java source files"

# Compile all Java files
echo "Compiling Java sources..."
javac -d "$BUILD_DIR" -cp "$BUILD_DIR" $JAVA_FILES

if [ $? -ne 0 ]; then
    print_error "Java compilation failed"
    exit 1
fi
print_success "Java automation compiled successfully"

# Run the Java builder
print_section "Building Container Image"

echo "Launching Java builder..."
echo ""

# Create temporary config for Java builder
TEMP_CONFIG=$(mktemp)
cat > "$TEMP_CONFIG" <<EOF
# Auto-generated configuration for Java builder
BASE_IMAGE=$BASE_IMAGE
BUILD_CONTAINER_NAME=$BUILD_CONTAINER_NAME
IMAGE_NAME=$IMAGE_NAME
BRIDGE=$BRIDGE
IP_ADDRESS=$BUILD_IP
GATEWAY=$GATEWAY
DNS_PRIMARY=$DNS_PRIMARY
DNS_SECONDARY=$DNS_SECONDARY
NODE_VERSION=$NODE_VERSION
SERVICE_PORT=$SERVICE_PORT
SERVICE_USER=$SERVICE_USER
SERVICE_GROUP=$SERVICE_GROUP
DATA_DIRECTORY=$DATA_DIR
LOG_FILE=$LOG_FILE
SERVER_JS_PATH=$CONTAINER_DIR/$SERVER_JS_PATH
PACKAGE_JSON_PATH=$CONTAINER_DIR/$PACKAGE_JSON_PATH
EOF

# Run the builder with timeout
timeout "$BUILD_TIMEOUT" java -cp "$BUILD_DIR" \
    "$JAVA_PACKAGE.$BUILDER_CLASS" \
    "$TEMP_CONFIG"

EXIT_CODE=$?

# Clean up temp config
rm -f "$TEMP_CONFIG"

if [ $EXIT_CODE -eq 0 ]; then
    print_section "Build Completed Successfully"

    # Create version info file
    cat > "$OUTPUT_PATH/generated/version.info" <<EOF
CONTAINER_NAME=$CONTAINER_NAME
VERSION=$VERSION
BUILD_DATE=$(date -u +"%Y-%m-%d %H:%M:%S UTC")
IMAGE_NAME=$IMAGE_NAME
BASE_IMAGE=$BASE_IMAGE
NODE_VERSION=$NODE_VERSION
EOF

    print_success "Created version info"

    # Create README for this version
    cat > "$OUTPUT_PATH/README-v.$VERSION.md" <<EOF
# Unique ID Generator Container v.$VERSION

## Build Information
- **Build Date**: $(date -u +"%Y-%m-%d %H:%M:%S UTC")
- **Base Image**: $BASE_IMAGE
- **Image Name**: $IMAGE_NAME
- **Node.js Version**: $NODE_VERSION.x

## Quick Start

### Launch with Default Configuration
\`\`\`bash
cd $(dirname "$SCRIPT_DIR")
./startDefault.sh
\`\`\`

### Launch with Custom Configuration
\`\`\`bash
./launchUniqueIdGenerator.sh /path/to/config.conf
\`\`\`

## Features
- Distributed ID generation with sharding support
- Interleaved numeric ID distribution
- UUID generation with shard awareness
- RESTful API endpoints
- Systemd service management
- Health monitoring

## Sharding Configuration

Sharding is configured at runtime, not during build. Example configurations:

### Single Instance (No Sharding)
\`\`\`bash
SHARD_ID=1
TOTAL_SHARDS=1
\`\`\`

### Three-Shard Cluster
\`\`\`bash
# Shard 1: Generates 1, 4, 7, 10, ...
SHARD_ID=1
TOTAL_SHARDS=3

# Shard 2: Generates 2, 5, 8, 11, ...
SHARD_ID=2
TOTAL_SHARDS=3

# Shard 3: Generates 3, 6, 9, 12, ...
SHARD_ID=3
TOTAL_SHARDS=3
\`\`\`

## API Endpoints

- \`GET /health\` - Health check
- \`GET /shard-info\` - Current shard configuration
- \`GET /api/next-id/:entity?dataType=<type>\` - Generate next ID
  - Types: int, bigint, uuid, uuid16

## Files Generated

- \`buildConfig.cnf\` - Build configuration used
- \`generated/sample.conf\` - Sample launch configuration
- \`generated/version.info\` - Version information
- \`README-v.$VERSION.md\` - This file
EOF

    print_success "Created README"

    # Package the build artifacts
    print_section "Packaging Build Artifacts"

    cd "$CONTAINER_DIR"
    tar -czf "unique-id-generator-v.$VERSION.tar.gz" \
        "unique-id-generator-v.$VERSION/"

    if [ $? -eq 0 ]; then
        print_success "Created archive: unique-id-generator-v.$VERSION.tar.gz"
    fi

    echo ""
    echo "=========================================="
    echo "✓ BUILD SUCCESSFUL"
    echo "=========================================="
    echo ""
    echo "Image Name: $IMAGE_NAME"
    echo "Version: $VERSION"
    echo "Output: $OUTPUT_PATH"
    echo ""
    echo "Next Steps:"
    echo "  1. Review generated configuration:"
    echo "     cat $OUTPUT_PATH/generated/sample.conf"
    echo ""
    echo "  2. Launch a container:"
    echo "     ./launchUniqueIdGenerator.sh $OUTPUT_PATH/generated/sample.conf"
    echo ""
    echo "  3. Or use quick start:"
    echo "     ./startDefault.sh"
    echo ""
else
    print_section "Build Failed"
    print_error "Build failed with exit code: $EXIT_CODE"

    if [ "$CLEANUP_ON_FAILURE" == "true" ]; then
        echo "Cleaning up..."

        # Try to delete the build container if it exists
        if lxc info "$BUILD_CONTAINER_NAME" &>/dev/null 2>&1; then
            lxc delete --force "$BUILD_CONTAINER_NAME"
            print_success "Cleaned up build container"
        fi
    fi

    exit $EXIT_CODE
fi