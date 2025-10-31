#!/bin/bash

# Quick start script for Kafka broker
# Uses sample configuration

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

echo "=========================================="
echo "  Kafka Broker - Quick Start"
echo "=========================================="
echo

# Check if base image exists
if ! lxc image list | grep -q "kafka-base"; then
    echo "Base image 'kafka-base' not found."
    echo "Building base image first..."
    echo
    cd "$SCRIPT_DIR"
    ./build/buildKafka.sh
    echo
fi

# Launch with sample configuration
echo "Launching Kafka broker with sample configuration..."
"$SCRIPT_DIR/launchKafka.sh" "$SCRIPT_DIR/templates/sample.conf"

echo
echo "=========================================="
echo "  Quick Start Complete!"
echo "=========================================="
