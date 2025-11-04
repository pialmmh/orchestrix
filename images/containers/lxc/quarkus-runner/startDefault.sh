#!/bin/bash
# Quick start script for Quarkus Runner Base Container
# Uses default configuration for local testing

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

echo "========================================="
echo "Quarkus Runner Base Container"
echo "Quick Start with Default Configuration"
echo "========================================="

# Run build script with default config
"${SCRIPT_DIR}/build/build.sh" "${SCRIPT_DIR}/build/build.conf"

echo ""
echo "========================================="
echo "Base Container Built Successfully!"
echo "========================================="
echo ""
echo "The base container is now available as an LXC image."
echo "You can use it to create app containers."
echo ""
echo "Next steps:"
echo "  1. Build your Quarkus application JAR"
echo "  2. Create app config: cp sample-config.conf configs/myapp-build.conf"
echo "  3. Edit configs/myapp-build.conf"
echo "  4. Build app container: ./build/build.sh configs/myapp-build.conf"
echo ""
echo "Or use the helper script:"
echo "  sudo ../../scripts/orchestrix-quarkus.sh build-app myapp 1.0.0 /path/to/app.jar"
echo "========================================="
