#!/bin/bash
set -e

# ============================================
# Quarkus Runtime Container Launch Script
# ============================================
# Launches a Quarkus container instance from exported image
# Following Orchestrix single-config-file convention

# Usage: ./launch.sh [config-file]
# Example: ./launch.sh /path/to/my-app-prod.conf
# Example: ./launch.sh  (uses templates/launch.conf)

# ABSOLUTE PATHS
BASE_DIR="/home/mustafa/telcobright-projects/orchestrix-lxc-work/images/lxc/quarkus-runtime"
TEMPLATES_DIR="$BASE_DIR/templates"

# Load configuration
if [ -n "$1" ]; then
    CONFIG_FILE="$1"
else
    CONFIG_FILE="$TEMPLATES_DIR/launch.conf"
fi

if [ ! -f "$CONFIG_FILE" ]; then
    echo "ERROR: Configuration file not found: $CONFIG_FILE"
    echo ""
    echo "Usage: $0 [config-file]"
    echo "Example: $0 /path/to/my-app-prod.conf"
    echo "Example: $0  (uses templates/launch.conf)"
    exit 1
fi

echo "Loading configuration from: $CONFIG_FILE"
source "$CONFIG_FILE"

# Validate required configuration
if [ -z "$INSTANCE_NAME" ]; then
    echo "ERROR: INSTANCE_NAME not set in config file"
    exit 1
fi

if [ -z "$CONTAINER_IMAGE" ]; then
    echo "ERROR: CONTAINER_IMAGE not set in config file"
    exit 1
fi

if [[ "$CONTAINER_IMAGE" == *"TIMESTAMP"* ]]; then
    echo "ERROR: CONTAINER_IMAGE contains placeholder 'TIMESTAMP'"
    echo "Please replace with actual image filename from build artifacts"
    echo "Example: infinite-scheduler-v1-1730400000.tar.gz"
    exit 1
fi

echo "=========================================="
echo "Launching Quarkus Container Instance"
echo "=========================================="
echo "Instance: $INSTANCE_NAME"
echo "Image: $CONTAINER_IMAGE"
echo "App Port: $HOST_PORT_APP"
echo "Loki: http://${LOKI_HOST}:${LOKI_PORT}"
echo "=========================================="
echo ""

# ============================================
# CHECK IF INSTANCE ALREADY EXISTS
# ============================================
if lxc list | grep -q "^| $INSTANCE_NAME "; then
    echo "ERROR: Instance $INSTANCE_NAME already exists"
    echo "Options:"
    echo "  1. Stop and delete: lxc stop $INSTANCE_NAME && lxc delete $INSTANCE_NAME"
    echo "  2. Choose a different INSTANCE_NAME in config file"
    exit 1
fi

# ============================================
# FIND IMAGE FILE
# ============================================
# Search for image in version directories
IMAGE_PATH=""
for VERSION_DIR in "$BASE_DIR"/*/generated/artifact/; do
    if [ -f "$VERSION_DIR/$CONTAINER_IMAGE" ]; then
        IMAGE_PATH="$VERSION_DIR/$CONTAINER_IMAGE"
        break
    fi
done

if [ -z "$IMAGE_PATH" ]; then
    echo "ERROR: Container image not found: $CONTAINER_IMAGE"
    echo ""
    echo "Searched in: $BASE_DIR/*/generated/artifact/"
    echo ""
    echo "Available images:"
    find "$BASE_DIR" -name "*.tar.gz" 2>/dev/null | head -10
    exit 1
fi

echo "Found image: $IMAGE_PATH"

# ============================================
# IMPORT IMAGE
# ============================================
echo ""
echo "Importing image to LXD..."

IMAGE_ALIAS="${INSTANCE_NAME}-image"
lxc image import "$IMAGE_PATH" --alias "$IMAGE_ALIAS" 2>/dev/null || {
    echo "Image already imported or import failed, continuing..."
}

echo "✓ Image imported with alias: $IMAGE_ALIAS"

# ============================================
# LAUNCH CONTAINER
# ============================================
echo ""
echo "Launching container..."

LAUNCH_CMD="lxc launch $IMAGE_ALIAS $INSTANCE_NAME"

# Add resource limits
LAUNCH_CMD="$LAUNCH_CMD -c limits.memory=$MEMORY_LIMIT"
LAUNCH_CMD="$LAUNCH_CMD -c limits.cpu=$CPU_LIMIT"

# Execute launch
eval "$LAUNCH_CMD"

echo "✓ Container launched"

# Wait for container to be ready
echo "Waiting for container to start..."
sleep 5

# ============================================
# CONFIGURE STATIC IP (if specified)
# ============================================
if [ -n "$STATIC_IP" ]; then
    echo ""
    echo "Configuring static IP: $STATIC_IP"

    lxc exec "$INSTANCE_NAME" -- bash -c "cat > /etc/netplan/50-static.yaml << EOF
network:
  version: 2
  ethernets:
    eth0:
      addresses:
        - $STATIC_IP/24
      gateway4: 10.10.199.1
      nameservers:
        addresses: [8.8.8.8, 8.8.4.4]
EOF"

    lxc exec "$INSTANCE_NAME" -- netplan apply
    echo "✓ Static IP configured"
fi

# Get container IP
CONTAINER_IP=$(lxc list "$INSTANCE_NAME" -c 4 --format csv | cut -d' ' -f1)
echo "✓ Container IP: $CONTAINER_IP"

# ============================================
# CONFIGURE BIND MOUNTS
# ============================================
if [ -n "$MOUNT_BINDS" ]; then
    echo ""
    echo "Configuring bind mounts..."

    IFS=',' read -ra MOUNTS <<< "$MOUNT_BINDS"
    for MOUNT in "${MOUNTS[@]}"; do
        HOST_PATH=$(echo "$MOUNT" | cut -d':' -f1)
        CONTAINER_PATH=$(echo "$MOUNT" | cut -d':' -f2)

        if [ ! -d "$HOST_PATH" ]; then
            echo "⚠ WARNING: Host path does not exist: $HOST_PATH (creating...)"
            mkdir -p "$HOST_PATH"
        fi

        lxc config device add "$INSTANCE_NAME" "mount-$(basename $HOST_PATH)" disk \
            source="$HOST_PATH" \
            path="$CONTAINER_PATH"

        echo "✓ Mounted: $HOST_PATH → $CONTAINER_PATH"
    done
fi

# ============================================
# UPDATE LOKI ENDPOINT
# ============================================
echo ""
echo "Updating Loki endpoint..."

lxc exec "$INSTANCE_NAME" -- sed -i \
    "s|url: http://.*:.*\/loki|url: http://${LOKI_HOST}:${LOKI_PORT}/loki|g" \
    /etc/promtail/promtail-config.yaml

lxc exec "$INSTANCE_NAME" -- systemctl restart promtail.service

echo "✓ Promtail configured to ship logs to http://${LOKI_HOST}:${LOKI_PORT}"

# ============================================
# SET ENVIRONMENT VARIABLES
# ============================================
if [ -n "$ENV_VARS" ]; then
    echo ""
    echo "Setting environment variables..."

    IFS=',' read -ra VARS <<< "$ENV_VARS"
    for VAR in "${VARS[@]}"; do
        VAR_NAME=$(echo "$VAR" | cut -d'=' -f1)
        VAR_VALUE=$(echo "$VAR" | cut -d'=' -f2-)

        lxc exec "$INSTANCE_NAME" -- bash -c "
            mkdir -p /etc/systemd/system/*.service.d
            echo '[Service]' >> /etc/systemd/system/*.service/override.conf
            echo 'Environment=\"$VAR_NAME=$VAR_VALUE\"' >> /etc/systemd/system/*.service/override.conf
        "

        echo "✓ Set: $VAR_NAME=$VAR_VALUE"
    done

    lxc exec "$INSTANCE_NAME" -- systemctl daemon-reload
fi

# ============================================
# CONFIGURE MYSQL (if enabled)
# ============================================
if [ "$MYSQL_ENABLED" = "true" ]; then
    echo ""
    echo "Configuring MySQL connection..."

    lxc exec "$INSTANCE_NAME" -- bash -c "cat >> /etc/environment << EOF
MYSQL_HOST=$MYSQL_HOST
MYSQL_PORT=$MYSQL_PORT
MYSQL_DATABASE=$MYSQL_DATABASE
MYSQL_USER=$MYSQL_USER
MYSQL_PASSWORD=$MYSQL_PASSWORD
EOF"

    echo "✓ MySQL configuration added"
fi

# ============================================
# RESTART SERVICES
# ============================================
echo ""
echo "Restarting services..."

# Get app name from image name (strip version and timestamp)
APP_NAME=$(echo "$CONTAINER_IMAGE" | sed 's/-v[0-9]*-[0-9]*.tar.gz//')

lxc exec "$INSTANCE_NAME" -- systemctl restart "${APP_NAME}.service" || {
    echo "⚠ WARNING: Could not restart ${APP_NAME}.service"
    echo "Service might have a different name, check with:"
    echo "  lxc exec $INSTANCE_NAME -- systemctl list-units --type=service"
}

sleep 3

# ============================================
# VERIFY SERVICES
# ============================================
echo ""
echo "Verifying services..."

lxc exec "$INSTANCE_NAME" -- systemctl is-active promtail.service && echo "✓ Promtail: ACTIVE" || echo "⚠ Promtail: INACTIVE"
lxc exec "$INSTANCE_NAME" -- systemctl is-active "${APP_NAME}.service" && echo "✓ ${APP_NAME}: ACTIVE" || echo "⚠ ${APP_NAME}: INACTIVE"

# ============================================
# LAUNCH SUMMARY
# ============================================
echo ""
echo "=========================================="
echo "LAUNCH COMPLETED SUCCESSFULLY"
echo "=========================================="
echo "Instance: $INSTANCE_NAME"
echo "IP Address: $CONTAINER_IP"
echo "App Port: http://$CONTAINER_IP:$CONTAINER_PORT_APP"
echo "Promtail Port: http://$CONTAINER_IP:$CONTAINER_PORT_PROMTAIL"
echo "Logs: http://${LOKI_HOST}:3000 (Grafana)"
echo ""
echo "=========================================="
echo "NEXT STEPS:"
echo "=========================================="
echo "1. Test application health:"
echo "   curl http://$CONTAINER_IP:$CONTAINER_PORT_APP/health"
echo ""
echo "2. View application logs:"
echo "   lxc exec $INSTANCE_NAME -- journalctl -u ${APP_NAME} -f"
echo ""
echo "3. Check logs in Grafana:"
echo "   http://${LOKI_HOST}:3000"
echo "   Query: {job=\"${APP_NAME}\"}"
echo ""
echo "4. Access container shell:"
echo "   lxc exec $INSTANCE_NAME -- bash"
echo ""
echo "5. Stop instance:"
echo "   lxc stop $INSTANCE_NAME"
echo ""
echo "=========================================="
