#!/bin/bash

set -e

SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

if [ $# -eq 0 ]; then
    echo "Usage: $0 <config-file>"
    echo "Example: $0 ./auto-increment-service-v.1.0.0/generated/sample.conf"
    exit 1
fi

CONFIG_FILE="$1"

if [ ! -f "$CONFIG_FILE" ]; then
    echo "ERROR: Configuration file not found: $CONFIG_FILE"
    exit 1
fi

echo "========================================="
echo "Auto-Increment Service Container Launcher"
echo "========================================="

source "$CONFIG_FILE"

# Set defaults
CONTAINER_NAME="${CONTAINER_NAME:-auto-increment-1}"
BASE_IMAGE="${BASE_IMAGE:-auto-increment-service-base}"
USE_BRIDGE="${USE_BRIDGE:-true}"
BRIDGE_NAME="${BRIDGE_NAME:-lxdbr0}"
SERVICE_PORT="${SERVICE_PORT:-7001}"

check_prerequisites() {
    if ! command -v lxc &> /dev/null; then
        echo "ERROR: LXC/LXD is not installed"
        exit 1
    fi

    if ! lxc image list --format json | grep -q "\"$BASE_IMAGE\""; then
        echo "ERROR: Base image '$BASE_IMAGE' not found"
        echo "Please run the build script first"
        exit 1
    fi

    if lxc info "$CONTAINER_NAME" &>/dev/null 2>&1; then
        echo "WARNING: Container '$CONTAINER_NAME' already exists"
        read -p "Delete and recreate? (y/N): " -n 1 -r
        echo
        if [[ $REPLY =~ ^[Yy]$ ]]; then
            lxc delete --force "$CONTAINER_NAME"
        else
            echo "Launch cancelled"
            exit 1
        fi
    fi
}

launch_container() {
    echo ""
    echo "Launching container: $CONTAINER_NAME"

    lxc launch "$BASE_IMAGE" "$CONTAINER_NAME"

    echo "Waiting for container to be ready..."
    sleep 3

    # Wait for systemd
    lxc exec "$CONTAINER_NAME" -- bash -c "
        until systemctl is-system-running &>/dev/null; do
            sleep 1
        done
    " 2>/dev/null || true

    echo "✓ Container launched"
}

configure_network() {
    echo ""
    echo "Configuring network..."

    # Check for mandatory network configuration
    if [ -z "$CONTAINER_IP" ] || [ -z "$GATEWAY_IP" ]; then
        echo "ERROR: CONTAINER_IP and GATEWAY_IP must be set in configuration"
        exit 1
    fi

    # Validate /24 notation
    if [[ ! "$CONTAINER_IP" =~ /24$ ]]; then
        echo "ERROR: CONTAINER_IP must include /24 subnet mask"
        echo "Current value: $CONTAINER_IP"
        echo "Expected format: 10.10.199.51/24"
        exit 1
    fi

    if [[ ! "$GATEWAY_IP" =~ /24$ ]]; then
        echo "ERROR: GATEWAY_IP must include /24 subnet mask"
        echo "Current value: $GATEWAY_IP"
        echo "Expected format: 10.10.199.1/24"
        exit 1
    fi

    # Validate subnet
    CONTAINER_SUBNET=$(echo "${CONTAINER_IP%/24}" | cut -d. -f1-3)
    GATEWAY_SUBNET=$(echo "${GATEWAY_IP%/24}" | cut -d. -f1-3)

    if [ "$CONTAINER_SUBNET" != "10.10.199" ]; then
        echo "ERROR: CONTAINER_IP must be in 10.10.199.0/24 subnet"
        exit 1
    fi

    if [ "$GATEWAY_SUBNET" != "10.10.199" ]; then
        echo "ERROR: GATEWAY_IP must be in 10.10.199.0/24 subnet"
        exit 1
    fi

    echo "Setting static IP: $CONTAINER_IP"

    # Remove default network device if exists
    lxc config device remove "$CONTAINER_NAME" eth0 2>/dev/null || true

    # Add bridge network device with static IP (strip /24 for LXC)
    lxc config device add "$CONTAINER_NAME" eth0 nic \
        nictype=bridged \
        parent="$BRIDGE_NAME" \
        ipv4.address="${CONTAINER_IP%/24}"

    # Configure DNS and routing inside container
    echo "Configuring DNS and routing..."
    lxc exec "$CONTAINER_NAME" -- bash -c "echo 'nameserver ${DNS_SERVERS%% *}' > /etc/resolv.conf"
    lxc exec "$CONTAINER_NAME" -- bash -c "echo 'nameserver ${DNS_SERVERS##* }' >> /etc/resolv.conf"
    lxc exec "$CONTAINER_NAME" -- bash -c "ip route add default via ${GATEWAY_IP%/24} 2>/dev/null || true"

    echo "✓ Network configured"
}

configure_resources() {
    if [ -n "$MEMORY_LIMIT" ]; then
        echo "Setting memory limit: $MEMORY_LIMIT"
        lxc config set "$CONTAINER_NAME" limits.memory "$MEMORY_LIMIT"
    fi

    if [ -n "$CPU_LIMIT" ]; then
        echo "Setting CPU limit: $CPU_LIMIT"
        lxc config set "$CONTAINER_NAME" limits.cpu "$CPU_LIMIT"
    fi
}

setup_bind_mounts() {
    if [ ${#BIND_MOUNTS[@]} -gt 0 ]; then
        echo ""
        echo "Setting up bind mounts..."

        for mount in "${BIND_MOUNTS[@]}"; do
            IFS=':' read -r host_path container_path <<< "$mount"

            # Expand tilde in host path
            host_path="${host_path/#\~/$HOME}"

            # Create host directory if it doesn't exist
            mkdir -p "$host_path"

            # Extract mount name from path
            mount_name=$(basename "$container_path" | tr '/' '-')

            echo "Mounting $host_path -> $container_path"

            lxc config device add "$CONTAINER_NAME" "$mount_name" disk \
                source="$host_path" \
                path="$container_path"
        done

        echo "✓ Bind mounts configured"
    fi
}

start_service() {
    echo ""
    echo "Starting auto-increment service..."

    lxc exec "$CONTAINER_NAME" -- bash -c "
        systemctl start auto-increment.service
        sleep 2
        systemctl status auto-increment.service --no-pager
    "

    echo "✓ Service started"
}

get_container_ip() {
    local ip=""
    local attempts=0
    local max_attempts=30

    while [ -z "$ip" ] && [ $attempts -lt $max_attempts ]; do
        ip=$(lxc list "$CONTAINER_NAME" -c 4 --format csv | grep -oE '[0-9]+\.[0-9]+\.[0-9]+\.[0-9]+' | head -1)
        if [ -z "$ip" ]; then
            sleep 1
            ((attempts++))
        fi
    done

    echo "$ip"
}

test_service() {
    echo ""
    echo "Testing service..."

    local container_ip=$(get_container_ip)

    if [ -n "$container_ip" ]; then
        echo "Container IP: $container_ip"

        # Test health endpoint
        if curl -s "http://${container_ip}:${SERVICE_PORT}/health" > /dev/null 2>&1; then
            echo "✓ Service is responding"

            # Show health status
            echo ""
            echo "Health check:"
            curl -s "http://${container_ip}:${SERVICE_PORT}/health" | python3 -m json.tool 2>/dev/null || \
                curl -s "http://${container_ip}:${SERVICE_PORT}/health"
        else
            echo "⚠ Service not responding yet (might still be starting)"
        fi
    else
        echo "⚠ Could not determine container IP"
    fi
}

print_summary() {
    local container_ip=$(get_container_ip)

    echo ""
    echo "========================================="
    echo "Container Launch Complete!"
    echo "========================================="
    echo ""
    echo "Container: $CONTAINER_NAME"
    echo "Status: Running"

    if [ -n "$container_ip" ]; then
        echo "IP Address: $container_ip"
        echo "Service URL: http://${container_ip}:${SERVICE_PORT}"
        echo ""
        echo "API Endpoints:"
        echo "  GET  http://${container_ip}:${SERVICE_PORT}/api/next-id/<entity>?dataType=<int|long>"
        echo "  GET  http://${container_ip}:${SERVICE_PORT}/api/status/<entity>"
        echo "  GET  http://${container_ip}:${SERVICE_PORT}/api/list"
        echo "  DELETE http://${container_ip}:${SERVICE_PORT}/api/reset"
    fi

    echo ""
    echo "Container commands:"
    echo "  View logs:    lxc exec $CONTAINER_NAME -- tail -f /var/log/auto-increment.log"
    echo "  Reset data:   lxc exec $CONTAINER_NAME -- reset-auto-increment"
    echo "  Shell access: lxc exec $CONTAINER_NAME -- bash"
    echo "  Stop:         lxc stop $CONTAINER_NAME"
    echo "  Delete:       lxc delete --force $CONTAINER_NAME"
    echo ""
    echo "Backup with state:"
    echo "  lxc export $CONTAINER_NAME ${CONTAINER_NAME}-backup.tar.gz"
    echo ""
}

# Main execution
main() {
    check_prerequisites
    launch_container
    configure_network
    configure_resources
    setup_bind_mounts
    start_service
    test_service
    print_summary
}

main "$@"