#!/bin/bash
# Quick start script for Go ID Generator Container
# Uses default configuration for development

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

echo "========================================="
echo "Go ID Generator - Quick Start"
echo "========================================="
echo "Using default configuration..."
echo ""

# Run build with default config
"${SCRIPT_DIR}/build/build.sh"

echo ""
echo "========================================="
echo "Quick Start Complete!"
echo "========================================="
echo ""
echo "Container built successfully."
echo ""
echo "To launch the container:"
echo "  cd go-id-v.1/generated"
echo "  ./launch.sh"
echo ""
echo "API will be available at:"
echo "  http://<container-ip>:7001/generate"
echo "  http://<container-ip>:7001/health"
echo "========================================="
