#!/bin/bash

# Default launcher for Unique ID Generator v1.0.0
# Uses sample.conf in the same directory

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
CONFIG_FILE="$SCRIPT_DIR/sample.conf"

if [ ! -f "$CONFIG_FILE" ]; then
    echo "Error: sample.conf not found in $SCRIPT_DIR"
    exit 1
fi

echo "Launching with default configuration from sample.conf"
echo ""

# Launch using the main launch script with sample config
"$SCRIPT_DIR/launch.sh" "$CONFIG_FILE"