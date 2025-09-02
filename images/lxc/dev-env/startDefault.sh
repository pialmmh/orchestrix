#!/bin/bash

# startDefault.sh - Quick start script for development environment container
# This script launches the container with the sample configuration

set -e

# Colors for output
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color

# Script directory
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"

# Function to print colored messages
print_message() {
    local color=$1
    local message=$2
    echo -e "${color}${message}${NC}"
}

# Check if running as root
if [[ $EUID -ne 0 ]]; then
    print_message "$RED" "This script must be run as root (use sudo)"
    exit 1
fi

print_message "$GREEN" "=== Starting Default Development Environment ==="

# Check if base image exists
if ! lxc image list --format=json | jq -r '.[].aliases[].name' | grep -q "^dev-env-base$"; then
    print_message "$RED" "Base image 'dev-env-base' not found!"
    print_message "$YELLOW" "Building base image first..."
    
    # Build the base image
    "${SCRIPT_DIR}/buildDevEnv.sh"
    
    if [ $? -ne 0 ]; then
        print_message "$RED" "Failed to build base image"
        exit 1
    fi
fi

# Check if default container already exists and is running
if lxc list --format=json | jq -r '.[].name' | grep -q "^dev-env$"; then
    STATUS=$(lxc list dev-env --format=json | jq -r '.[0].status')
    if [ "$STATUS" == "Running" ]; then
        print_message "$YELLOW" "Container 'dev-env' is already running"
        
        # Show container info
        CONTAINER_IP=$(lxc list dev-env --format=json | jq -r '.[0].state.network.eth0.addresses[] | select(.family=="inet").address' 2>/dev/null || echo "Not available")
        print_message "$GREEN" ""
        print_message "$GREEN" "Container Information:"
        print_message "$YELLOW" "  Name: dev-env"
        print_message "$YELLOW" "  IP Address: ${CONTAINER_IP}"
        print_message "$YELLOW" ""
        print_message "$YELLOW" "Access with: lxc exec dev-env -- bash"
        exit 0
    fi
fi

# Launch container with sample config
print_message "$YELLOW" "Launching container with sample configuration..."
"${SCRIPT_DIR}/launchDevEnv.sh" "${SCRIPT_DIR}/sample-config.conf"

# Show summary
print_message "$GREEN" ""
print_message "$GREEN" "=== Default Dev Environment Started ==="
print_message "$YELLOW" ""
print_message "$YELLOW" "The container is running with:"
print_message "$YELLOW" "  - No SSH tunnels configured"
print_message "$YELLOW" "  - Ready for bind mounts"
print_message "$YELLOW" "  - SSH client auto-accepts all certificates"
print_message "$YELLOW" ""
print_message "$YELLOW" "To customize:"
print_message "$YELLOW" "  1. Copy sample-config.conf to my-config.conf"
print_message "$YELLOW" "  2. Edit my-config.conf with your settings"
print_message "$YELLOW" "  3. Run: sudo ./launchDevEnv.sh my-config.conf"
print_message "$GREEN" ""