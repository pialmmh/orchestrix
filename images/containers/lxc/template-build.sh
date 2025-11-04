#!/bin/bash
# Template build script for new containers
# Copy this to your container-name/build/ directory and customize

set -e

# Get paths
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
CONTAINER_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
ORCHESTRIX_ROOT="$(cd "$CONTAINER_DIR/../../.." && pwd)"
BUILDER_JAR="$ORCHESTRIX_ROOT/builder/target/container-builder-1.0.0.jar"

# Check if builder JAR exists, build if not
if [ ! -f "$BUILDER_JAR" ]; then
    echo "Building Java builder library..."
    cd "$ORCHESTRIX_ROOT/builder"
    ./build.sh
    cd - > /dev/null
fi

# Find existing versions and determine default
CONTAINER_BASE=$(basename "$CONTAINER_DIR")
EXISTING_VERSIONS=($(ls -d "$CONTAINER_DIR"/${CONTAINER_BASE}-v.* 2>/dev/null | grep -o 'v\.[0-9]*' | cut -d. -f2 | sort -n))

if [ ${#EXISTING_VERSIONS[@]} -eq 0 ]; then
    DEFAULT_VERSION=1
else
    LATEST=${EXISTING_VERSIONS[-1]}
    DEFAULT_VERSION=$LATEST
fi

# Handle --overwrite flag
FORCE_FLAG=""
if [ "$1" == "--overwrite" ]; then
    FORCE_FLAG="--force"
    VERSION=$DEFAULT_VERSION
else
    # Prompt for version
    read -p "Version to build [$DEFAULT_VERSION]: " VERSION
    VERSION=${VERSION:-$DEFAULT_VERSION}
fi

# Validate integer
if ! [[ "$VERSION" =~ ^[0-9]+$ ]]; then
    echo "Error: Version must be integer"
    exit 1
fi

# Check for artifact
ARTIFACT_FILE="$CONTAINER_DIR/${CONTAINER_BASE}-v.${VERSION}/generated/${CONTAINER_BASE}-v.${VERSION}.tar.gz"
if [ -f "$ARTIFACT_FILE" ] && [ "$1" != "--overwrite" ]; then
    read -p "Artifact ${CONTAINER_BASE}-v.${VERSION}.tar.gz already exists. Overwrite? (y/N): " OVERWRITE
    if [[ ! "$OVERWRITE" =~ ^[Yy]$ ]]; then
        echo "Aborted."
        exit 0
    fi
fi

# Update version in build.yaml
BUILD_YAML="$CONTAINER_DIR/build.yaml"
if [ -f "$BUILD_YAML" ]; then
    # Update version in YAML
    sed -i "s/^version:.*/version: $VERSION/" "$BUILD_YAML"
fi

# Run Java builder
echo "Building ${CONTAINER_BASE}:${VERSION} (fully unattended)..."
cd "$CONTAINER_DIR"
java -jar "$BUILDER_JAR" ${CONTAINER_BASE} build.yaml $FORCE_FLAG

echo ""
echo "✅ Built ${CONTAINER_BASE}:${VERSION}"
echo "   Generated files in: ${CONTAINER_BASE}-v.${VERSION}/generated/"
echo ""
echo "To launch: cd ${CONTAINER_BASE}-v.${VERSION}/generated && sudo ./launch.sh"