#!/bin/bash
#
# Launch Script for Consul Container
# Usage: ./launchConsul.sh <config-file> <container-name>
#

set -e

if [ -z "$1" ] || [ -z "$2" ]; then
    echo "Usage: $0 <config-file> <container-name>"
    echo ""
    echo "Example:"
    echo "  $0 ./consul-v.1/generated/node1.conf consul-node-1"
    echo ""
    echo "For 3-node cluster:"
    echo "  $0 ./consul-v.1/generated/node1.conf consul-node-1"
    echo "  $0 ./consul-v.1/generated/node2.conf consul-node-2"
    echo "  $0 ./consul-v.1/generated/node3.conf consul-node-3"
    exit 1
fi

CONFIG_FILE="$1"
CONTAINER_NAME="$2"

if [ ! -f "$CONFIG_FILE" ]; then
    echo "ERROR: Config file not found: $CONFIG_FILE"
    exit 1
fi

echo "Loading configuration from: $CONFIG_FILE"
source "$CONFIG_FILE"

# Import container image if needed
IMAGE_ALIAS="consul-v1"
if ! lxc image list | grep -q "$IMAGE_ALIAS"; then
    echo "Importing Consul image..."
    lxc image import "$IMAGE_PATH" --alias "$IMAGE_ALIAS"
fi

# Clean existing container
if lxc info "$CONTAINER_NAME" >/dev/null 2>&1; then
    echo "Removing existing container: $CONTAINER_NAME"
    lxc stop "$CONTAINER_NAME" --force || true
    lxc delete "$CONTAINER_NAME"
fi

# Create container
echo "Creating container: $CONTAINER_NAME"
lxc init "$IMAGE_ALIAS" "$CONTAINER_NAME"

# Set resource limits
lxc config set "$CONTAINER_NAME" limits.memory "$MEMORY_LIMIT"
lxc config set "$CONTAINER_NAME" limits.cpu "$CPU_LIMIT"

# Set environment variables
lxc config set "$CONTAINER_NAME" environment.NODE_ID "$NODE_ID"
lxc config set "$CONTAINER_NAME" environment.DATACENTER "$DATACENTER"
lxc config set "$CONTAINER_NAME" environment.SERVER_MODE "$SERVER_MODE"
lxc config set "$CONTAINER_NAME" environment.BOOTSTRAP_EXPECT "$BOOTSTRAP_EXPECT"
lxc config set "$CONTAINER_NAME" environment.BIND_ADDR "$BIND_ADDR"
lxc config set "$CONTAINER_NAME" environment.RETRY_JOIN "$RETRY_JOIN"
lxc config set "$CONTAINER_NAME" environment.HTTP_PORT "$HTTP_PORT"
lxc config set "$CONTAINER_NAME" environment.DNS_PORT "$DNS_PORT"
lxc config set "$CONTAINER_NAME" environment.SERVER_RPC_PORT "$SERVER_RPC_PORT"
lxc config set "$CONTAINER_NAME" environment.SERF_LAN_PORT "$SERF_LAN_PORT"
lxc config set "$CONTAINER_NAME" environment.SERF_WAN_PORT "$SERF_WAN_PORT"
lxc config set "$CONTAINER_NAME" environment.LOG_LEVEL "$LOG_LEVEL"

# Start container
echo "Starting container..."
lxc start "$CONTAINER_NAME"

# Wait for network
sleep 3

# Get container IP
CONTAINER_IP=$(lxc list "$CONTAINER_NAME" -c 4 --format csv | cut -d' ' -f1)

# Create Consul configuration file
echo "Creating Consul configuration..."
cat <<CONFIG | lxc exec "$CONTAINER_NAME" -- tee /consul/config/consul.json >/dev/null
{
    "node_name": "$NODE_ID",
    "datacenter": "$DATACENTER",
    "data_dir": "/consul/data",
    "server": $SERVER_MODE,
    "bootstrap_expect": $BOOTSTRAP_EXPECT,
    "bind_addr": "$BIND_ADDR",
    "client_addr": "0.0.0.0",
    "retry_join": $RETRY_JOIN,
    "retry_join_wan": [],
    "ui_config": {
        "enabled": true
    },
    "ports": {
        "http": $HTTP_PORT,
        "dns": $DNS_PORT,
        "server": $SERVER_RPC_PORT,
        "serf_lan": $SERF_LAN_PORT,
        "serf_wan": $SERF_WAN_PORT
    },
    "log_level": "$LOG_LEVEL",
    "enable_syslog": false,
    "leave_on_terminate": true
}
CONFIG

# Start consul service with new config
lxc exec "$CONTAINER_NAME" -- rc-service consul start

echo ""
echo "========================================="
echo "Consul Node Started"
echo "========================================="
echo "Container: $CONTAINER_NAME"
echo "Node ID: $NODE_ID"
echo "IP Address: $CONTAINER_IP"
echo "Datacenter: $DATACENTER"
echo "Server Mode: $SERVER_MODE"
echo ""
echo "Consul UI: http://$CONTAINER_IP:$HTTP_PORT/ui"
echo ""
echo "Check status:"
echo "  lxc exec $CONTAINER_NAME -- consul members"
echo ""
echo "View logs:"
echo "  lxc exec $CONTAINER_NAME -- tail -f /var/log/consul.log"
echo "========================================="
