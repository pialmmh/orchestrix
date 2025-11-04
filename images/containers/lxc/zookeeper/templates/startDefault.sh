#!/bin/bash

# Quick start script for Zookeeper
# Launches a single-node Zookeeper container

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

echo "Starting Zookeeper (Single Node)..."
"${SCRIPT_DIR}/../launch.sh" "${SCRIPT_DIR}/sample.conf"
