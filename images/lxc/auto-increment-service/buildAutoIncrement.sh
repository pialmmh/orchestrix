#!/bin/bash

set -e

SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
PROJECT_DIR="$SCRIPT_DIR"
CONFIG_FILE="${SCRIPT_DIR}/buildAutoIncrementConfig.cnf"
VERSION="1.1.0"

echo "========================================="
echo "Auto-Increment Service LXC Builder"
echo "Version: ${VERSION}"
echo "========================================="

if [ ! -f "$CONFIG_FILE" ]; then
    echo "ERROR: Configuration file not found: $CONFIG_FILE"
    exit 1
fi

source "$CONFIG_FILE"

# Validate network configuration
validate_network_config() {
    echo ""
    echo "Validating network configuration..."

    # Check if IP and Gateway are set
    if [ -z "$CONTAINER_IP" ] || [ "$CONTAINER_IP" = "" ]; then
        echo "ERROR: CONTAINER_IP is not set in $CONFIG_FILE"
        echo "Please set a valid IP with /24 notation (e.g., 10.10.199.50/24)"
        exit 1
    fi

    if [ -z "$GATEWAY_IP" ] || [ "$GATEWAY_IP" = "" ]; then
        echo "ERROR: GATEWAY_IP is not set in $CONFIG_FILE"
        echo "Please set the gateway IP with /24 notation (e.g., 10.10.199.1/24)"
        exit 1
    fi

    # Check if /24 is included
    if [[ ! "$CONTAINER_IP" =~ /24$ ]]; then
        echo "ERROR: CONTAINER_IP must include /24 subnet mask"
        echo "Current value: $CONTAINER_IP"
        echo "Expected format: 10.10.199.50/24"
        exit 1
    fi

    if [[ ! "$GATEWAY_IP" =~ /24$ ]]; then
        echo "ERROR: GATEWAY_IP must include /24 subnet mask"
        echo "Current value: $GATEWAY_IP"
        echo "Expected format: 10.10.199.1/24"
        exit 1
    fi

    # Extract IP without /24 for validation
    CONTAINER_IP_ONLY="${CONTAINER_IP%/24}"
    GATEWAY_IP_ONLY="${GATEWAY_IP%/24}"

    # Extract first 3 octets for /24 subnet comparison
    CONTAINER_SUBNET=$(echo "$CONTAINER_IP_ONLY" | cut -d. -f1-3)
    GATEWAY_SUBNET=$(echo "$GATEWAY_IP_ONLY" | cut -d. -f1-3)

    # Check if both are in 10.10.199 subnet
    if [ "$CONTAINER_SUBNET" != "10.10.199" ]; then
        echo "ERROR: CONTAINER_IP ($CONTAINER_IP) is not in 10.10.199.0/24 subnet"
        exit 1
    fi

    if [ "$GATEWAY_SUBNET" != "10.10.199" ]; then
        echo "ERROR: GATEWAY_IP ($GATEWAY_IP) is not in 10.10.199.0/24 subnet"
        exit 1
    fi

    # Check if IPs are in same subnet
    if [ "$CONTAINER_SUBNET" != "$GATEWAY_SUBNET" ]; then
        echo "ERROR: CONTAINER_IP and GATEWAY_IP are not in the same subnet"
        echo "Container subnet: ${CONTAINER_SUBNET}.0/24"
        echo "Gateway subnet: ${GATEWAY_SUBNET}.0/24"
        exit 1
    fi

    echo "✓ Network configuration validated"
    echo "  Container IP: $CONTAINER_IP"
    echo "  Gateway IP: $GATEWAY_IP"
    echo "  Bridge: $BRIDGE_NAME"
}

check_lxc() {
    if ! command -v lxc &> /dev/null; then
        echo "ERROR: LXC/LXD is not installed"
        exit 1
    fi
    echo "✓ LXC/LXD is installed"
}

cleanup_existing() {
    if lxc info "$BUILD_CONTAINER" &>/dev/null 2>&1; then
        echo "Cleaning up existing build container..."
        lxc delete --force "$BUILD_CONTAINER"
    fi

    if lxc info "$CONTAINER_NAME" &>/dev/null 2>&1; then
        echo "WARNING: Base container '$CONTAINER_NAME' already exists"
        read -p "Delete and rebuild? (y/N): " -n 1 -r
        echo
        if [[ $REPLY =~ ^[Yy]$ ]]; then
            lxc delete --force "$CONTAINER_NAME"
        else
            echo "Build cancelled"
            exit 1
        fi
    fi

    # Check for running containers using the base image
    echo "Checking for running containers using $CONTAINER_NAME image..."
    RUNNING_CONTAINERS=$(lxc list --format json | python3 -c "
import json, sys
data = json.load(sys.stdin)
for c in data:
    if c.get('config', {}).get('volatile.base_image') == '$CONTAINER_NAME' and c['status'] == 'Running':
        print(c['name'])
" 2>/dev/null || true)

    # Alternative method if the above doesn't work
    if [ -z "$RUNNING_CONTAINERS" ]; then
        RUNNING_CONTAINERS=$(lxc list --format csv -c n,l | grep "$CONTAINER_NAME" | cut -d, -f1 2>/dev/null || true)
    fi

    if [ -n "$RUNNING_CONTAINERS" ]; then
        echo ""
        echo "WARNING: The following containers are running using the $CONTAINER_NAME image:"
        echo "$RUNNING_CONTAINERS"
        echo ""
        read -p "Stop and delete these containers to proceed with rebuild? (y/N): " -n 1 -r
        echo
        if [[ $REPLY =~ ^[Yy]$ ]]; then
            for container in $RUNNING_CONTAINERS; do
                echo "Stopping and deleting $container..."
                lxc delete --force "$container" 2>/dev/null || true
            done
        else
            echo "Build cancelled - please stop running containers first"
            exit 1
        fi
    fi
}

create_build_container() {
    echo ""
    echo "Creating build container with bridge networking..."

    # Launch container
    lxc launch "$BASE_IMAGE" "$BUILD_CONTAINER"

    # Configure bridge networking with static IP
    echo "Configuring network (IP: $CONTAINER_IP)..."

    # Remove default network device if exists
    lxc config device remove "$BUILD_CONTAINER" eth0 2>/dev/null || true

    # Add bridge network device with static IP (use IP without /24 for LXC config)
    lxc config device add "$BUILD_CONTAINER" eth0 nic \
        nictype=bridged \
        parent="$BRIDGE_NAME" \
        ipv4.address="${CONTAINER_IP%/24}"

    echo "Waiting for container to be ready..."
    sleep 5

    # Configure network inside container
    echo "Configuring container network settings..."

    # Set DNS servers
    lxc exec "$BUILD_CONTAINER" -- bash -c "echo 'nameserver ${DNS_SERVERS%% *}' > /etc/resolv.conf"
    lxc exec "$BUILD_CONTAINER" -- bash -c "echo 'nameserver ${DNS_SERVERS##* }' >> /etc/resolv.conf"

    # Add default route (use IP without /24)
    lxc exec "$BUILD_CONTAINER" -- bash -c "ip route add default via ${GATEWAY_IP%/24} 2>/dev/null || true"

    # Test and wait for internet connectivity
    echo ""
    echo "Testing internet connectivity..."

    while true; do
        if lxc exec "$BUILD_CONTAINER" -- ping -c 1 google.com &>/dev/null; then
            echo "✓ Internet connectivity confirmed"
            break
        else
            echo ""
            echo "⚠ Container cannot reach the internet!"
            echo ""
            echo "Please check:"
            echo "1. Bridge '$BRIDGE_NAME' exists and is configured"
            echo "2. IP forwarding is enabled: sysctl net.ipv4.ip_forward"
            echo "3. NAT/Masquerading is configured for the bridge"
            echo ""
            echo "Common fixes:"
            echo "  sudo sysctl -w net.ipv4.ip_forward=1"
            echo "  sudo iptables -t nat -A POSTROUTING -s 10.10.199.0/24 -j MASQUERADE"
            echo ""
            read -p "Press Enter to retry after fixing the network issue..."

            # Retry DNS and routing configuration
            lxc exec "$BUILD_CONTAINER" -- bash -c "echo 'nameserver 8.8.8.8' > /etc/resolv.conf"
            lxc exec "$BUILD_CONTAINER" -- bash -c "echo 'nameserver 8.8.4.4' >> /etc/resolv.conf"
            lxc exec "$BUILD_CONTAINER" -- bash -c "ip route add default via ${GATEWAY_IP%/24} 2>/dev/null || true"
        fi
    done

    echo "✓ Build container ready with network access"
}

install_dependencies() {
    echo ""
    echo "Installing dependencies..."

    lxc exec "$BUILD_CONTAINER" -- bash -c "
        set -e

        # Update system
        apt-get update

        # Install minimal dependencies
        DEBIAN_FRONTEND=noninteractive apt-get install -y \
            curl \
            ca-certificates \
            gnupg \
            systemd \
            nano \
            less \
            python3

        # Install Node.js
        mkdir -p /etc/apt/keyrings
        curl -fsSL https://deb.nodesource.com/gpgkey/nodesource-repo.gpg.key | gpg --dearmor -o /etc/apt/keyrings/nodesource.gpg
        echo \"deb [signed-by=/etc/apt/keyrings/nodesource.gpg] https://deb.nodesource.com/node_${NODE_VERSION}.x nodistro main\" > /etc/apt/sources.list.d/nodesource.list

        apt-get update
        DEBIAN_FRONTEND=noninteractive apt-get install -y nodejs

        # Verify installation
        node --version
        npm --version
    "

    echo "✓ Dependencies installed"
}

setup_service() {
    echo ""
    echo "Setting up auto-increment service..."

    # Create service directory
    lxc exec "$BUILD_CONTAINER" -- mkdir -p /opt/sequence-service

    # Copy service files
    lxc file push "$PROJECT_DIR/scripts/server.js" "$BUILD_CONTAINER/opt/sequence-service/server.js"
    lxc file push "$PROJECT_DIR/scripts/package.json" "$BUILD_CONTAINER/opt/sequence-service/package.json"
    lxc file push "$PROJECT_DIR/scripts/reset-data.sh" "$BUILD_CONTAINER/usr/local/bin/sequence-reset"
    lxc file push "$PROJECT_DIR/scripts/sequence-service.service" "$BUILD_CONTAINER/etc/systemd/system/sequence-service.service"

    # Install Node.js dependencies and setup service
    lxc exec "$BUILD_CONTAINER" -- bash -c "
        set -e

        # Install npm dependencies
        cd /opt/sequence-service
        npm install --production

        # Create data directory
        mkdir -p /var/lib/sequence-service
        chown nobody:nogroup /var/lib/sequence-service

        # Set permissions
        chmod 755 /usr/local/bin/sequence-reset
        chmod 644 /etc/systemd/system/sequence-service.service
        chown -R nobody:nogroup /opt/sequence-service

        # Enable service
        systemctl daemon-reload
        systemctl enable sequence-service.service

        # Create log file
        touch /var/log/sequence-service.log
        chown nobody:nogroup /var/log/sequence-service.log
    "

    echo "✓ Service configured"
}

optimize_container() {
    if [ "$OPTIMIZE_SIZE" = "true" ]; then
        echo ""
        echo "Optimizing container size..."

        lxc exec "$BUILD_CONTAINER" -- bash -c "
            set -e

            # Clean package cache
            apt-get clean
            apt-get autoremove -y
            rm -rf /var/lib/apt/lists/*

            # Remove unnecessary files
            rm -rf /tmp/* /var/tmp/*
            rm -rf /usr/share/doc/*
            rm -rf /usr/share/man/*

            # Clear logs
            find /var/log -type f -exec truncate -s 0 {} \;
        "

        echo "✓ Container optimized"
    fi
}

create_base_image() {
    echo ""
    echo "Creating base image..."

    # Stop the container
    lxc stop "$BUILD_CONTAINER"

    # Delete existing alias if it exists
    lxc image alias delete "$CONTAINER_NAME" 2>/dev/null || true

    # Publish as an image
    lxc publish "$BUILD_CONTAINER" --alias "$CONTAINER_NAME" --public

    echo "✓ Base image created: $CONTAINER_NAME"
}

cleanup() {
    if [ "$CLEANUP_BUILD_CONTAINER" = "true" ]; then
        echo ""
        echo "Cleaning up build container..."
        lxc delete "$BUILD_CONTAINER"
        echo "✓ Cleanup complete"
    fi
}

create_generated_files() {
    echo ""
    echo "Creating generated files..."

    # Create versioned directory
    VERSION_DIR="${PROJECT_DIR}/auto-increment-service-v.${VERSION}"
    GENERATED_DIR="${VERSION_DIR}/generated"
    mkdir -p "$GENERATED_DIR"

    # Copy build config
    cp "$CONFIG_FILE" "${VERSION_DIR}/buildConfig.cnf"

    # Create sample configuration
    cat > "${GENERATED_DIR}/sample.conf" << 'EOF'
#!/bin/bash

# Auto-Increment Service Container Configuration

# Container settings
CONTAINER_NAME="auto-increment-1"
BASE_IMAGE="auto-increment-service-base"

# MANDATORY NETWORK CONFIGURATION - MUST INCLUDE /24
# All containers must use 10.10.199.0/24 subnet
CONTAINER_IP="10.10.199.51/24"     # REQUIRED: Must include /24 notation
GATEWAY_IP="10.10.199.1/24"        # REQUIRED: Must include /24 notation
BRIDGE_NAME="lxdbr0"                # Bridge interface
DNS_SERVERS="8.8.8.8 8.8.4.4"      # DNS servers

# Service configuration
SERVICE_PORT="7001"

# Resource limits (optional)
MEMORY_LIMIT=""  # e.g., "512MB"
CPU_LIMIT=""     # e.g., "1"

# Bind mounts (optional - for external state persistence)
# Format: "host_path:container_path"
BIND_MOUNTS=()

# Example with external state directory:
# BIND_MOUNTS=(
#     "~/auto-increment-data:/var/lib/auto-increment"
# )
EOF

    echo "✓ Generated files created in ${VERSION_DIR}"
}

print_summary() {
    echo ""
    echo "========================================="
    echo "Build Complete!"
    echo "========================================="
    echo ""
    echo "Base image created: $CONTAINER_NAME"
    echo "Version: $VERSION"
    echo "Network: 10.10.199.0/24"
    echo "Build IP used: $CONTAINER_IP"
    echo ""
    echo "The container stores state in: /var/lib/auto-increment/state.json"
    echo "Data persists within the container across restarts."
    echo ""
    echo "To create a backup with state:"
    echo "  lxc export <container-name> backup.tar.gz"
    echo ""
    echo "To restore on another machine:"
    echo "  lxc import backup.tar.gz"
    echo ""
    echo "API Endpoints:"
    echo "  GET  /api/next-id/<entity>?dataType=<int|long>"
    echo "  GET  /api/status/<entity>"
    echo "  GET  /api/list"
    echo "  DELETE /api/reset"
    echo ""
    echo "CLI command to reset data:"
    echo "  lxc exec <container> -- reset-auto-increment"
    echo ""
    echo "To launch a container:"
    echo "  cd $SCRIPT_DIR"
    echo "  ./launchAutoIncrement.sh auto-increment-service-v.${VERSION}/generated/sample.conf"
    echo ""
    echo "Or use quick start:"
    echo "  ./startDefault.sh"
    echo ""
}

# Main execution
main() {
    validate_network_config
    check_lxc
    cleanup_existing
    create_build_container
    install_dependencies
    setup_service
    optimize_container
    create_base_image
    cleanup
    create_generated_files
    print_summary
}

main "$@"