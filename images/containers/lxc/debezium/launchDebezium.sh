#!/bin/bash

# Debezium CDC Container Launch Script

set -e

if [ $# -lt 1 ]; then
    echo "Usage: $0 <config-file>"
    echo "Example: $0 templates/sample.conf"
    exit 1
fi

CONFIG_FILE="$1"
[ ! -f "$CONFIG_FILE" ] && echo "Error: Config not found: $CONFIG_FILE" && exit 1

source "$CONFIG_FILE"
[ -z "$CONTAINER_NAME" ] && echo "Error: CONTAINER_NAME not set" && exit 1

echo "=========================================="
echo "  Launching Debezium CDC Container"
echo "=========================================="
echo "Container: $CONTAINER_NAME"
echo "Bootstrap: $BOOTSTRAP_SERVERS"
echo

echo "Creating container..."
lxc launch debezium-base "$CONTAINER_NAME"
sleep 3

echo "Configuring..."
lxc exec "$CONTAINER_NAME" -- bash -c "cat > /etc/debezium/runtime.conf << 'EOF'
BOOTSTRAP_SERVERS=\"${BOOTSTRAP_SERVERS}\"
CONNECT_GROUP_ID=\"${CONNECT_GROUP_ID:-debezium-cluster}\"
CONFIG_STORAGE_TOPIC=\"${CONFIG_STORAGE_TOPIC:-debezium-configs}\"
OFFSET_STORAGE_TOPIC=\"${OFFSET_STORAGE_TOPIC:-debezium-offsets}\"
STATUS_STORAGE_TOPIC=\"${STATUS_STORAGE_TOPIC:-debezium-status}\"
CONNECT_REST_PORT=\"${CONNECT_REST_PORT:-8083}\"
CONNECT_HEAP_OPTS=\"${CONNECT_HEAP_OPTS:--Xms512M -Xmx512M}\"
EOF
"

if [ -n "$MYSQL_SERVERS_JSON" ]; then
    echo "Configuring MySQL servers..."
    lxc exec "$CONTAINER_NAME" -- bash -c "cat > /etc/debezium/mysql-servers.json << 'EOF'
$MYSQL_SERVERS_JSON
EOF
"
fi

if [ "$AUTO_START" = "true" ]; then
    echo "Starting Debezium..."
    lxc exec "$CONTAINER_NAME" -- systemctl start debezium
    sleep 5
    lxc exec "$CONTAINER_NAME" -- systemctl is-active --quiet debezium && echo "Debezium is running" || echo "Warning: Failed to start"
fi

echo
echo "=========================================="
echo "  Container launched!"
echo "=========================================="
echo "Commands:"
echo "  lxc exec $CONTAINER_NAME -- systemctl status debezium"
echo "  lxc exec $CONTAINER_NAME -- curl localhost:${CONNECT_REST_PORT:-8083}/connectors"
echo
