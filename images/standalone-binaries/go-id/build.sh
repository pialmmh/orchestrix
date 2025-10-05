#!/bin/bash

# Build Go-ID Standalone Binary
# Creates statically-linked Go binary for Alpine containers

set -e

# Colors
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

echo "========================================="
echo "  Go-ID Binary Builder"
echo "========================================="
echo ""

# Check if version provided
VERSION=${1:-1}

echo "Building Go-ID binary version: ${VERSION}"
echo ""

# Check if version directory exists
VERSION_DIR="$(dirname "$0")/go-id-binary-v.${VERSION}"
if [ ! -d "$VERSION_DIR" ]; then
    echo -e "${YELLOW}⚠ Version directory not found: ${VERSION_DIR}${NC}"
    echo "Creating directory..."
    mkdir -p "$VERSION_DIR"
fi

# Navigate to project root (assuming orchestrix structure)
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_ROOT="$(cd "${SCRIPT_DIR}/../../.." && pwd)"

echo "Project root: ${PROJECT_ROOT}"
echo ""

# Run Java automation
cd "${PROJECT_ROOT}"

echo "Running binary build automation..."
echo ""

mvn exec:java \
    -Dexec.mainClass="com.telcobright.orchestrix.automation.binary.goid.GoIdBinaryBuildRunner" \
    -Dexec.args="${VERSION}"

EXIT_CODE=$?

echo ""
if [ $EXIT_CODE -eq 0 ]; then
    echo -e "${GREEN}=========================================${NC}"
    echo -e "${GREEN}✓ Build Complete!${NC}"
    echo -e "${GREEN}=========================================${NC}"
    echo ""
    echo "Binary location:"
    echo "  ${VERSION_DIR}/go-id"
    echo ""
    echo "Next steps:"
    echo "  1. Test binary: ${VERSION_DIR}/go-id &"
    echo "  2. Create Alpine container with this binary"
    echo "  3. Deploy to production"
    echo ""
else
    echo -e "${RED}=========================================${NC}"
    echo -e "${RED}✗ Build Failed${NC}"
    echo -e "${RED}=========================================${NC}"
    echo ""
    echo "Check the error messages above"
    echo ""
    exit 1
fi
