#!/bin/bash

# Launch script for Unique ID Generator v1.0.0
# Usage: ./launch.sh <config-file>

set -e

# Check arguments
if [ $# -ne 1 ]; then
    echo "Usage: $0 <config-file>"
    echo "Example: $0 sample.conf"
    exit 1
fi

CONFIG_FILE="$1"

# Check if config file exists
if [ ! -f "$CONFIG_FILE" ]; then
    echo "Error: Config file not found: $CONFIG_FILE"
    exit 1
fi

# Load configuration
source "$CONFIG_FILE"

# Validate required variables
if [ -z "$CONTAINER_NAME" ]; then
    echo "Error: CONTAINER_NAME not set in config"
    exit 1
fi

BASE_IMAGE="${BASE_IMAGE:-unique-id-generator-base}"

echo "========================================="
echo "Unique ID Generator v1.0.0 Launcher"
echo "========================================="
echo "Container: $CONTAINER_NAME"
echo "Image: $BASE_IMAGE"
echo "Shard: ${SHARD_ID:-1} of ${TOTAL_SHARDS:-1}"
echo ""

# Check if base image exists
if ! lxc image list --format json | jq -e ".[] | select(.aliases[].name == \"$BASE_IMAGE\")" > /dev/null; then
    echo "ERROR: Base image '$BASE_IMAGE' not found"
    echo "Please build the image first with build-minimal.sh"
    exit 1
fi

# Check if container already exists
if lxc list | grep -q "^| $CONTAINER_NAME "; then
    echo "Container $CONTAINER_NAME already exists. Delete it first or use a different name."
    exit 1
fi

# Launch container
echo "Launching container..."
lxc launch "$BASE_IMAGE" "$CONTAINER_NAME"

# Wait for container to be ready
echo "Waiting for container to initialize..."
sleep 5

# Configure network if IP specified
if [ ! -z "$CONTAINER_IP" ]; then
    echo "Configuring network: $CONTAINER_IP"
    lxc config device remove "$CONTAINER_NAME" eth0 2>/dev/null || true
    lxc config device add "$CONTAINER_NAME" eth0 nic \
        nictype=bridged \
        parent="${BRIDGE_NAME:-lxdbr0}" \
        ipv4.address="$CONTAINER_IP"
    sleep 2
fi

# Create environment configuration
echo "Configuring service..."
lxc exec "$CONTAINER_NAME" -- bash -c "cat > /opt/unique-id-generator/.env << EOF
PORT=${SERVICE_PORT:-8080}
SHARD_ID=${SHARD_ID:-1}
TOTAL_SHARDS=${TOTAL_SHARDS:-1}
DATA_DIR=${DATA_DIR:-/var/lib/unique-id-generator}
LOG_LEVEL=${LOG_LEVEL:-info}
EOF"

# Mount host directories if specified
if [ ! -z "$HOST_DATA_DIR" ]; then
    echo "Mounting data directory: $HOST_DATA_DIR"
    mkdir -p "$HOST_DATA_DIR"
    lxc config device add "$CONTAINER_NAME" data disk source="$HOST_DATA_DIR" path=/var/lib/unique-id-generator
    lxc exec "$CONTAINER_NAME" -- chown nobody:nogroup /var/lib/unique-id-generator
fi

if [ ! -z "$HOST_LOG_DIR" ]; then
    echo "Mounting log directory: $HOST_LOG_DIR"
    mkdir -p "$HOST_LOG_DIR"
    lxc config device add "$CONTAINER_NAME" logs disk source="$HOST_LOG_DIR" path=/var/log/unique-id-generator
    lxc exec "$CONTAINER_NAME" -- chown nobody:nogroup /var/log/unique-id-generator
fi

# Restart service
echo "Starting service..."
lxc exec "$CONTAINER_NAME" -- systemctl restart unique-id-generator

# Wait for service to start
sleep 3

# Get actual container IP
ACTUAL_IP=$(lxc list "$CONTAINER_NAME" -f json | jq -r '.[0].state.network.eth0.addresses[0].address')

# Test service
echo ""
echo "Testing service..."
if curl -s "http://${ACTUAL_IP}:${SERVICE_PORT:-8080}/health" > /dev/null 2>&1; then
    echo "✓ Service is healthy"
else
    echo "⚠ Service not responding"
    echo "Check logs: lxc exec $CONTAINER_NAME -- journalctl -u unique-id-generator -n 20"
fi

echo ""
echo "========================================="
echo "✓ Container Launch Complete!"
echo "========================================="
echo "Container: $CONTAINER_NAME"
echo "IP Address: $ACTUAL_IP"
echo "Service URL: http://${ACTUAL_IP}:${SERVICE_PORT:-8080}"
echo "Shard: ${SHARD_ID:-1} of ${TOTAL_SHARDS:-1}"
echo ""
echo "API Examples:"
echo "  # Generate Snowflake ID (recommended)"
echo "  curl 'http://${ACTUAL_IP}:${SERVICE_PORT:-8080}/api/next-id/user?dataType=snowflake'"
echo ""
echo "  # Batch generation"
echo "  curl 'http://${ACTUAL_IP}:${SERVICE_PORT:-8080}/api/next-batch/event?dataType=snowflake&batchSize=10'"
echo ""
echo "CLI Usage:"
echo "  lxc exec $CONTAINER_NAME -- uid-cli list"
echo "  lxc exec $CONTAINER_NAME -- uid-cli init entity snowflake"
echo ""