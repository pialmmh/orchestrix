#!/bin/bash

# launchDevEnv.sh - Launch development environment container with arbitrary config file
# This script starts an already-built dev-env container with any config file

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
CYAN='\033[0;36m'
NC='\033[0m' # No Color

# Default container name
DEFAULT_CONTAINER_NAME="dev-env"

# Function to print colored messages
print_message() {
    local color=$1
    local message=$2
    echo -e "${color}${message}${NC}"
}

# Show usage
show_usage() {
    print_message "$CYAN" "Development Environment Container Launcher"
    print_message "$YELLOW" "Usage: $0 <config-file> [options]"
    print_message "$YELLOW" ""
    print_message "$YELLOW" "Arguments:"
    print_message "$YELLOW" "  config-file         Path to configuration file"
    print_message "$YELLOW" ""
    print_message "$YELLOW" "Options:"
    print_message "$YELLOW" "  --name <name>       Container name (default: dev-env)"
    print_message "$YELLOW" "  --help, -h          Show this help message"
    print_message "$YELLOW" ""
    print_message "$YELLOW" "Config file format:"
    print_message "$YELLOW" "  CONTAINER_NAME=dev-env"
    print_message "$YELLOW" "  BIND_MOUNTS=(/host/path1:/container/path1 /host/path2:/container/path2)"
    print_message "$YELLOW" "  TUNNEL_CONFIG=/path/to/tunnels.conf"
    print_message "$YELLOW" "  AUTO_START_TUNNELS=true"
    print_message "$YELLOW" "  # Tunnel definitions (if TUNNEL_CONFIG not specified):"
    print_message "$YELLOW" "  TUNNELS=("
    print_message "$YELLOW" "    'Service|RemoteServer|RemotePort|LocalPort|SSHUser|SSHPassword|SSHPort|Description'"
    print_message "$YELLOW" "  )"
    print_message "$YELLOW" ""
    print_message "$YELLOW" "Examples:"
    print_message "$YELLOW" "  $0 /tmp/dev-env.conf"
    print_message "$YELLOW" "  $0 /opt/configs/production.conf --name prod-dev"
    exit 0
}

# Check if running as root
check_root() {
    if [[ $EUID -ne 0 ]]; then
        print_message "$RED" "This script must be run as root (use sudo)"
        exit 1
    fi
}

# Parse command line arguments
CONFIG_FILE=""
CONTAINER_NAME="$DEFAULT_CONTAINER_NAME"

while [[ $# -gt 0 ]]; do
    case $1 in
        --name)
            CONTAINER_NAME="$2"
            shift 2
            ;;
        --help|-h)
            show_usage
            ;;
        *)
            if [ -z "$CONFIG_FILE" ]; then
                CONFIG_FILE="$1"
            else
                print_message "$RED" "Unknown option: $1"
                show_usage
            fi
            shift
            ;;
    esac
done

# Validate config file
if [ -z "$CONFIG_FILE" ]; then
    print_message "$RED" "Error: No configuration file specified"
    show_usage
fi

if [ ! -f "$CONFIG_FILE" ]; then
    print_message "$RED" "Error: Configuration file not found: $CONFIG_FILE"
    exit 1
fi

# Main execution
print_message "$GREEN" "=== Development Environment Container Launcher ==="
check_root

# Load configuration
print_message "$YELLOW" "Loading configuration from: $CONFIG_FILE"
source "$CONFIG_FILE"

# Override container name if specified in config
if [ -n "$CONTAINER_NAME_FROM_CONFIG" ]; then
    CONTAINER_NAME="$CONTAINER_NAME_FROM_CONFIG"
fi

# Check if container already exists
if lxc list --format=json | jq -r '.[].name' | grep -q "^${CONTAINER_NAME}$"; then
    STATUS=$(lxc list ${CONTAINER_NAME} --format=json | jq -r '.[0].status')
    if [ "$STATUS" == "Running" ]; then
        print_message "$YELLOW" "Container '${CONTAINER_NAME}' is already running"
        print_message "$YELLOW" "Stopping existing container..."
        lxc stop ${CONTAINER_NAME}
    fi
    print_message "$YELLOW" "Removing existing container '${CONTAINER_NAME}'..."
    lxc delete ${CONTAINER_NAME} --force
fi

# Check if base image exists
BASE_IMAGE="dev-env-base"
if ! lxc image list --format=json | jq -r '.[].aliases[].name' | grep -q "^${BASE_IMAGE}$"; then
    print_message "$RED" "Base image '${BASE_IMAGE}' not found!"
    print_message "$YELLOW" "Please run buildDevEnv.sh first to create the base image"
    exit 1
fi

# Launch container from base image
print_message "$YELLOW" "Launching container '${CONTAINER_NAME}' from base image..."
lxc launch ${BASE_IMAGE} ${CONTAINER_NAME}

# Wait for container to be ready
print_message "$YELLOW" "Waiting for container to be ready..."
sleep 5

# Setup bind mounts
if [ -n "${BIND_MOUNTS}" ]; then
    print_message "$YELLOW" "Setting up bind mounts..."
    
    # Stop container to add devices
    lxc stop ${CONTAINER_NAME}
    
    MOUNT_INDEX=0
    for MOUNT in "${BIND_MOUNTS[@]}"; do
        IFS=':' read -r HOST_PATH CONTAINER_PATH <<< "$MOUNT"
        
        # Validate host path
        if [ ! -e "$HOST_PATH" ]; then
            print_message "$YELLOW" "Creating host path: $HOST_PATH"
            mkdir -p "$HOST_PATH"
        fi
        
        DEVICE_NAME="mount${MOUNT_INDEX}"
        print_message "$YELLOW" "  Mounting $HOST_PATH -> $CONTAINER_PATH"
        
        lxc config device add ${CONTAINER_NAME} ${DEVICE_NAME} disk \
            source="${HOST_PATH}" \
            path="${CONTAINER_PATH}"
        
        ((MOUNT_INDEX++))
    done
    
    # Start container again
    lxc start ${CONTAINER_NAME}
    sleep 5
fi

# Setup tunnel configuration (optional)
if [ -n "$TUNNEL_CONFIG" ] && [ -f "$TUNNEL_CONFIG" ]; then
    print_message "$YELLOW" "Setting up SSH tunnels from external config: $TUNNEL_CONFIG"
    
    # Copy tunnel config to container
    lxc file push "$TUNNEL_CONFIG" ${CONTAINER_NAME}/etc/ssh-tunnels/tunnels.conf
    
elif [ -n "${TUNNELS}" ]; then
    print_message "$YELLOW" "Setting up SSH tunnels from inline definitions..."
    
    # Create tunnels.conf in container
    TEMP_TUNNEL_CONF="/tmp/tunnels-${CONTAINER_NAME}.conf"
    cat > "$TEMP_TUNNEL_CONF" << 'EOF'
# SSH Tunnels Configuration
# Generated from launch config
EOF
    
    for TUNNEL in "${TUNNELS[@]}"; do
        echo "$TUNNEL" >> "$TEMP_TUNNEL_CONF"
    done
    
    lxc file push "$TEMP_TUNNEL_CONF" ${CONTAINER_NAME}/etc/ssh-tunnels/tunnels.conf
    rm -f "$TEMP_TUNNEL_CONF"
else
    print_message "$YELLOW" "No SSH tunnels configured (tunnels are optional)"
    # Create empty tunnel config to avoid errors
    lxc exec ${CONTAINER_NAME} -- bash -c 'echo "# No tunnels configured" > /etc/ssh-tunnels/tunnels.conf'
fi

# Pass any additional parameters to container
if [ -n "$CONTAINER_ENV_VARS" ]; then
    print_message "$YELLOW" "Setting container environment variables..."
    for ENV_VAR in "${CONTAINER_ENV_VARS[@]}"; do
        KEY="${ENV_VAR%%=*}"
        VALUE="${ENV_VAR#*=}"
        lxc exec ${CONTAINER_NAME} -- bash -c "echo 'export $KEY=\"$VALUE\"' >> /etc/environment"
    done
fi

# Start services if configured
if [ "$AUTO_START_TUNNELS" == "true" ] && ([ -n "$TUNNEL_CONFIG" ] || [ -n "${TUNNELS}" ]); then
    print_message "$YELLOW" "Starting SSH tunnels..."
    lxc exec ${CONTAINER_NAME} -- systemctl start ssh-tunnels
    lxc exec ${CONTAINER_NAME} -- systemctl enable ssh-tunnels
    lxc exec ${CONTAINER_NAME} -- systemctl enable tunnel-health.timer
    
    sleep 3
    
    # Show status
    print_message "$GREEN" "Tunnel Status:"
    lxc exec ${CONTAINER_NAME} -- /usr/local/bin/tunnel-manager.sh status
elif [ "$AUTO_START_TUNNELS" == "true" ]; then
    print_message "$YELLOW" "AUTO_START_TUNNELS is true but no tunnels are configured"
fi

# Get container info
CONTAINER_IP=$(lxc list ${CONTAINER_NAME} --format=json | jq -r '.[0].state.network.eth0.addresses[] | select(.family=="inet").address' 2>/dev/null || echo "Not available")

# Show summary
print_message "$GREEN" "=== Container Launch Complete ==="
print_message "$YELLOW" "Container Name: ${CONTAINER_NAME}"
print_message "$YELLOW" "Container IP: ${CONTAINER_IP}"
print_message "$YELLOW" "Config File: ${CONFIG_FILE}"

if [ -n "${BIND_MOUNTS}" ]; then
    print_message "$YELLOW" ""
    print_message "$YELLOW" "Bind Mounts:"
    for MOUNT in "${BIND_MOUNTS[@]}"; do
        print_message "$YELLOW" "  $MOUNT"
    done
fi

print_message "$YELLOW" ""
print_message "$YELLOW" "Useful commands:"
print_message "$YELLOW" "  Check status:  lxc exec ${CONTAINER_NAME} -- /usr/local/bin/tunnel-manager.sh status"
print_message "$YELLOW" "  View logs:     lxc exec ${CONTAINER_NAME} -- tail -f /var/log/ssh-tunnels/*.log"
print_message "$YELLOW" "  Shell access:  lxc exec ${CONTAINER_NAME} -- bash"
print_message "$YELLOW" "  Stop:          lxc stop ${CONTAINER_NAME}"

print_message "$GREEN" "=== Done ==="