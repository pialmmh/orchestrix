#!/bin/bash

# Quick start script for FRR Router
# Launches a container with default configuration

SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

echo "========================================="
echo "FRR Router - Quick Start"
echo "========================================="
echo ""
echo "Launching container with default configuration..."
echo ""

# Use the sample config
"${SCRIPT_DIR}/launchFrrRouter.sh" "${SCRIPT_DIR}/sample-launch-config.conf"
