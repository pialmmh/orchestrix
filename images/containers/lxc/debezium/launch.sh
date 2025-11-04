#!/bin/bash
#
# Debezium Container Launch Script
# Follows Orchestrix Container Scaffolding Standard v2.0
#

set -e

CONFIG_FILE="$1"

if [ -z "$CONFIG_FILE" ]; then
    echo "Usage: $0 <config-file>"
    echo "Example: $0 templates/sample.conf"
    exit 1
fi

if [ ! -f "$CONFIG_FILE" ]; then
    echo "Error: Config file not found: $CONFIG_FILE"
    exit 1
fi

# Source configuration
source "$CONFIG_FILE"

# Validate required parameters
if [ -z "$CONTAINER_NAME" ]; then
    echo "Error: CONTAINER_NAME not set in config"
    exit 1
fi

if [ -z "$KAFKA_BOOTSTRAP_SERVERS" ]; then
    echo "Error: KAFKA_BOOTSTRAP_SERVERS not set in config"
    exit 1
fi

if [ -z "$DB_TYPE" ]; then
    echo "Error: DB_TYPE not set in config"
    exit 1
fi

echo "=========================================="
echo "  Debezium Container Launch"
echo "=========================================="
echo "Container: $CONTAINER_NAME"
echo "Database: $DB_TYPE"
echo "Kafka: $KAFKA_BOOTSTRAP_SERVERS"
echo ""

# Check if container already exists
if lxc list -c n --format csv | grep -q "^${CONTAINER_NAME}$"; then
    echo "Container $CONTAINER_NAME already exists"
    echo "Use: lxc delete $CONTAINER_NAME --force"
    exit 1
fi

# Check if debezium-base image exists
if ! lxc image list | grep -q "debezium-base"; then
    echo "Error: debezium-base image not found"
    echo "Please build the image first using: build/build.sh"
    exit 1
fi

echo "Step 1: Creating container from debezium-base..."
lxc init debezium-base "$CONTAINER_NAME"

echo "Step 2: Configuring container resources..."
lxc config set "$CONTAINER_NAME" limits.memory="${MEMORY_LIMIT:-2GB}"
lxc config set "$CONTAINER_NAME" limits.cpu="${CPU_LIMIT:-2}"

echo "Step 3: Creating runtime configuration..."

# Create runtime.conf
RUNTIME_CONF="# Debezium Runtime Configuration
# Generated at: $(date)

# Kafka Configuration
KAFKA_BOOTSTRAP_SERVERS=\"$KAFKA_BOOTSTRAP_SERVERS\"
GROUP_ID=\"${GROUP_ID:-debezium-cluster}\"
OFFSET_STORAGE_TOPIC=\"${OFFSET_STORAGE_TOPIC:-debezium-offsets}\"
CONFIG_STORAGE_TOPIC=\"${CONFIG_STORAGE_TOPIC:-debezium-configs}\"
STATUS_STORAGE_TOPIC=\"${STATUS_STORAGE_TOPIC:-debezium-status}\"
CONNECT_REST_PORT=\"${CONNECT_REST_PORT:-8083}\"
CONNECT_HEAP_OPTS=\"${CONNECT_HEAP_OPTS:--Xms1G -Xmx1G}\"
"

# Create connector.conf
CONNECTOR_CONF="# Debezium Connector Configuration
# Generated at: $(date)

# Connector Configuration
CONNECTOR_NAME=\"${CONNECTOR_NAME:-${DB_TYPE}-connector}\"
DB_TYPE=\"$DB_TYPE\"

# Database Connection
DB_HOST=\"$DB_HOST\"
DB_PORT=\"$DB_PORT\"
DB_USER=\"$DB_USER\"
DB_PASSWORD=\"$DB_PASSWORD\"
DB_NAME=\"${DB_NAME:-}\"

# Kafka Topic Configuration
KAFKA_TOPIC_PREFIX=\"${KAFKA_TOPIC_PREFIX:-dbserver1}\"
KAFKA_BOOTSTRAP_SERVERS=\"$KAFKA_BOOTSTRAP_SERVERS\"

# MySQL-specific
DB_SERVER_ID=\"${DB_SERVER_ID:-1}\"
DB_INCLUDE_LIST=\"${DB_INCLUDE_LIST:-}\"
TABLE_INCLUDE_LIST=\"${TABLE_INCLUDE_LIST:-}\"

# PostgreSQL-specific
# (Add PostgreSQL-specific config if needed)

# MongoDB-specific
COLLECTION_INCLUDE_LIST=\"${COLLECTION_INCLUDE_LIST:-}\"

# Snapshot Configuration
SNAPSHOT_MODE=\"${SNAPSHOT_MODE:-initial}\"
SCHEMA_HISTORY_TOPIC=\"${SCHEMA_HISTORY_TOPIC:-dbhistory.${CONNECTOR_NAME}}\"

# Auto-register connector on startup
AUTO_REGISTER=\"${AUTO_REGISTER:-true}\"
"

echo "Step 4: Pushing configuration to container..."
lxc start "$CONTAINER_NAME"
sleep 3

echo "$RUNTIME_CONF" | lxc exec "$CONTAINER_NAME" -- bash -c "mkdir -p /etc/debezium && cat > /etc/debezium/runtime.conf"
echo "$CONNECTOR_CONF" | lxc exec "$CONTAINER_NAME" -- bash -c "cat > /etc/debezium/connector.conf"

echo "Step 5: Starting Debezium service..."
lxc exec "$CONTAINER_NAME" -- systemctl start debezium.service

echo "Step 6: Waiting for Kafka Connect to be ready..."
for i in {1..30}; do
    if lxc exec "$CONTAINER_NAME" -- curl -s http://localhost:${CONNECT_REST_PORT:-8083}/ > /dev/null 2>&1; then
        echo "Kafka Connect is ready!"
        break
    fi
    echo "Waiting... ($i/30)"
    sleep 2
done

if [ "${AUTO_REGISTER:-true}" = "true" ]; then
    echo "Step 7: Registering connector..."
    sleep 5
    lxc exec "$CONTAINER_NAME" -- /opt/debezium/register-connector.sh

    echo ""
    echo "Step 8: Checking connector status..."
    sleep 2
    lxc exec "$CONTAINER_NAME" -- curl -s http://localhost:${CONNECT_REST_PORT:-8083}/connectors/${CONNECTOR_NAME:-${DB_TYPE}-connector}/status | jq . || true
fi

echo ""
echo "=========================================="
echo "  DEPLOYMENT COMPLETE"
echo "=========================================="
echo "Container: $CONTAINER_NAME"
echo "Status: RUNNING"
echo ""
CONTAINER_IP=$(lxc list "$CONTAINER_NAME" -c 4 --format csv | cut -d' ' -f1)
echo "Container IP: $CONTAINER_IP"
echo "REST API: http://$CONTAINER_IP:${CONNECT_REST_PORT:-8083}"
echo ""
echo "Useful commands:"
echo "  # Check connector status"
echo "  lxc exec $CONTAINER_NAME -- curl http://localhost:${CONNECT_REST_PORT:-8083}/connectors/${CONNECTOR_NAME:-${DB_TYPE}-connector}/status"
echo ""
echo "  # List all connectors"
echo "  lxc exec $CONTAINER_NAME -- curl http://localhost:${CONNECT_REST_PORT:-8083}/connectors"
echo ""
echo "  # View logs"
echo "  lxc exec $CONTAINER_NAME -- journalctl -u debezium.service -f"
echo ""
echo "  # Delete connector"
echo "  lxc exec $CONTAINER_NAME -- curl -X DELETE http://localhost:${CONNECT_REST_PORT:-8083}/connectors/${CONNECTOR_NAME:-${DB_TYPE}-connector}"
echo ""
