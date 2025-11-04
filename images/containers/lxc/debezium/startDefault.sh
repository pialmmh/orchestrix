#\!/bin/bash
set -e
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
if \! lxc image list | grep -q "debezium-base"; then
    echo "Building debezium-base image..."
    cd "$SCRIPT_DIR" && ./build/buildDebezium.sh
fi
"$SCRIPT_DIR/launchDebezium.sh" "$SCRIPT_DIR/templates/sample.conf"

