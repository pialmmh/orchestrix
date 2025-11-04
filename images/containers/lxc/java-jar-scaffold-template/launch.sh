#!/bin/bash
#
# Launch Script for Java JAR Container
# Usage: ./launch.sh <config-file-path>
#
# This script launches containers from the pre-built Java JAR image
# following the same pattern as Consul container launcher
#

set -e

# Accept config from anywhere (critical for our standard)
CONFIG_FILE="${1:-$(dirname "$0")/launch.conf}"

if [ ! -f "$CONFIG_FILE" ]; then
    echo "ERROR: Configuration file not found: $CONFIG_FILE"
    echo "Usage: $0 <config-file-path>"
    exit 1
fi

echo "Loading configuration from: $CONFIG_FILE"
source "$CONFIG_FILE"

# Container name with instance suffix
INSTANCE_NAME="${CONTAINER_NAME}-${INSTANCE_ID:-1}"

echo "========================================="
echo "Launching Java JAR Container"
echo "========================================="
echo "Instance: $INSTANCE_NAME"
echo "Image: $IMAGE_ALIAS"
echo "Port: $SERVICE_PORT"
echo "========================================="

# Check if container already exists
if lxc info "$INSTANCE_NAME" >/dev/null 2>&1; then
    echo "Container $INSTANCE_NAME already exists"
    read -p "Delete and recreate? (y/n): " -n 1 -r
    echo
    if [[ $REPLY =~ ^[Yy]$ ]]; then
        lxc stop "$INSTANCE_NAME" --force 2>/dev/null || true
        lxc delete "$INSTANCE_NAME" --force
    else
        echo "Aborted"
        exit 1
    fi
fi

# Launch container from image
echo "Launching container from image..."
lxc launch "$IMAGE_ALIAS" "$INSTANCE_NAME"

# Wait for network
echo "Waiting for container network..."
sleep 5

# Apply resource limits if specified
if [ -n "$MEMORY_LIMIT" ]; then
    echo "Setting memory limit: $MEMORY_LIMIT"
    lxc config set "$INSTANCE_NAME" limits.memory="$MEMORY_LIMIT"
fi

if [ -n "$CPU_LIMIT" ]; then
    echo "Setting CPU limit: $CPU_LIMIT"
    lxc config set "$INSTANCE_NAME" limits.cpu="$CPU_LIMIT"
fi

# Mount application config if specified
if [ -n "$APP_CONFIG_PATH" ] && [ -f "$APP_CONFIG_PATH" ]; then
    echo "Mounting application config..."
    lxc file push "$APP_CONFIG_PATH" "$INSTANCE_NAME/app/config/application.properties"
fi

# Mount additional volumes if specified
if [ -n "$MOUNT_BINDS" ]; then
    echo "Setting up bind mounts..."
    IFS=';' read -ra MOUNTS <<< "$MOUNT_BINDS"
    for mount in "${MOUNTS[@]}"; do
        IFS=':' read -r host_path container_path <<< "$mount"
        if [ -n "$host_path" ] && [ -n "$container_path" ]; then
            echo "  Mounting $host_path -> $container_path"
            lxc config device add "$INSTANCE_NAME" "$(basename $host_path)" disk \
                source="$host_path" \
                path="$container_path"
        fi
    done
fi

# Configure Promtail if needed
if [ "$PROMTAIL_ENABLED" = "true" ] && [ -n "$LOKI_ENDPOINT" ]; then
    echo "Configuring Promtail endpoint..."
    lxc exec "$INSTANCE_NAME" -- sed -i \
        "s|url:.*|url: $LOKI_ENDPOINT/loki/api/v1/push|" \
        /etc/promtail/config.yaml
fi

# Set environment variables
echo "Setting environment variables..."
lxc exec "$INSTANCE_NAME" -- bash -c "cat > /etc/environment << EOF
SERVICE_PORT=$SERVICE_PORT
METRICS_PORT=$METRICS_PORT
JVM_OPTS=\"$JVM_OPTS\"
APP_HOME=/app
LOG_PATH=/var/log/app
DEBUG_ENABLED=$DEBUG_ENABLED
DEBUG_PORT=$DEBUG_PORT
$EXTRA_ENV_VARS
EOF"

# Start the Java application service
echo "Starting Java application..."
lxc exec "$INSTANCE_NAME" -- rc-service "$APP_NAME" start

# Start Promtail if enabled
if [ "$PROMTAIL_ENABLED" = "true" ]; then
    echo "Starting Promtail..."
    lxc exec "$INSTANCE_NAME" -- rc-service promtail start
fi

# Get container IP
CONTAINER_IP=$(lxc list "$INSTANCE_NAME" -c 4 --format csv | cut -d' ' -f1)

echo ""
echo "========================================="
echo "✓ Container launched successfully!"
echo "========================================="
echo "Container: $INSTANCE_NAME"
echo "IP Address: $CONTAINER_IP"
echo "Service Port: $SERVICE_PORT"
echo "Metrics Port: $METRICS_PORT"
if [ "$DEBUG_ENABLED" = "true" ]; then
    echo "Debug Port: $DEBUG_PORT"
fi
echo ""
echo "Check status:"
echo "  lxc exec $INSTANCE_NAME -- rc-service $APP_NAME status"
echo ""
echo "View logs:"
echo "  lxc exec $INSTANCE_NAME -- tail -f /var/log/app/${APP_NAME}.log"
echo ""
echo "Access service:"
echo "  curl http://$CONTAINER_IP:$SERVICE_PORT/health"
echo "========================================="