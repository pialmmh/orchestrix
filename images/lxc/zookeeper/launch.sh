#!/bin/bash

# Zookeeper Container Launch Script

set -e

if [ $# -ne 1 ]; then
    echo "Usage: $0 <config-file>"
    echo "Example: $0 templates/zk1.conf"
    exit 1
fi

CONFIG_FILE="$1"

if [ ! -f "$CONFIG_FILE" ]; then
    echo "Error: Configuration file not found: $CONFIG_FILE"
    exit 1
fi

# Source configuration
source "$CONFIG_FILE"

# Validate required parameters
if [ -z "$CONTAINER_NAME" ]; then
    echo "Error: CONTAINER_NAME not set in config"
    exit 1
fi

if [ -z "$SERVER_ID" ]; then
    echo "Error: SERVER_ID not set in config"
    exit 1
fi

# Set defaults
BASE_IMAGE=${BASE_IMAGE:-"zookeeper-base"}
CLIENT_PORT=${CLIENT_PORT:-2181}
PEER_PORT=${PEER_PORT:-2888}
LEADER_PORT=${LEADER_PORT:-3888}
DATA_DIR=${DATA_DIR:-"/data/zookeeper"}
ZOO_HEAP_OPTS=${ZOO_HEAP_OPTS:-"-Xms512M -Xmx512M"}
AUTO_START=${AUTO_START:-"true"}

echo "=========================================="
echo "  Launching Zookeeper Container"
echo "=========================================="
echo "Container: $CONTAINER_NAME"
echo "Server ID: $SERVER_ID"
echo "Client Port: $CLIENT_PORT"
echo

# Check if container already exists
if sudo lxc info "$CONTAINER_NAME" &>/dev/null; then
    echo "Error: Container '$CONTAINER_NAME' already exists"
    echo "To recreate, first run: lxc delete $CONTAINER_NAME --force"
    exit 1
fi

# Launch container
echo "Creating container from $BASE_IMAGE..."
sudo lxc launch "$BASE_IMAGE" "$CONTAINER_NAME"
echo "Waiting for container to start..."
sleep 3

# Configure static IP if specified
if [ -n "$STATIC_IP" ]; then
    echo "Configuring static IP: $STATIC_IP"
    GATEWAY=${GATEWAY:-"10.10.199.1"}
    DNS_SERVERS=${DNS_SERVERS:-"8.8.8.8,8.8.4.4"}

    # Convert comma-separated DNS to space-separated
    DNS_LIST=$(echo "$DNS_SERVERS" | tr ',' ' ')

    # Configure static IP inside container
    sudo lxc exec "$CONTAINER_NAME" -- bash -c "
        # Flush existing IP
        ip addr flush dev eth0

        # Add static IP
        ip addr add $STATIC_IP/24 dev eth0

        # Bring interface up
        ip link set eth0 up

        # Add default route
        ip route add default via $GATEWAY

        # Configure DNS
        echo '# Static DNS configuration' > /etc/resolv.conf
        for dns in $DNS_LIST; do
            echo \"nameserver \$dns\" >> /etc/resolv.conf
        done

        echo 'Static IP configured successfully'
    "

    echo "Static IP configured: $STATIC_IP"
else
    echo "Using DHCP (no STATIC_IP specified)"
fi

# Create runtime configuration
echo "Configuring Zookeeper..."
sudo lxc exec "$CONTAINER_NAME" -- bash -c "cat > /etc/zookeeper/runtime.conf << 'EOF'
# Zookeeper Runtime Configuration
SERVER_ID=$SERVER_ID
CLIENT_PORT=$CLIENT_PORT
PEER_PORT=$PEER_PORT
LEADER_PORT=$LEADER_PORT
DATA_DIR=$DATA_DIR
ZOO_HEAP_OPTS=\"$ZOO_HEAP_OPTS\"

# Cluster servers configuration
ZOO_SERVERS=\"$ZOO_SERVERS\"
EOF
"

# Enable and start service if AUTO_START is true
if [ "$AUTO_START" = "true" ]; then
    echo "Enabling and starting Zookeeper service..."
    sudo lxc exec "$CONTAINER_NAME" -- systemctl enable zookeeper
    sudo lxc exec "$CONTAINER_NAME" -- systemctl start zookeeper

    echo "Waiting for Zookeeper to start..."
    sleep 5

    # Check service status
    if sudo lxc exec "$CONTAINER_NAME" -- systemctl is-active zookeeper &>/dev/null; then
        echo
        echo "=========================================="
        echo "  Zookeeper Started Successfully!"
        echo "=========================================="
        echo "Container: $CONTAINER_NAME"
        echo "Server ID: $SERVER_ID"
        echo "Client Port: $CLIENT_PORT"
        echo
        echo "Check status:"
        echo "  lxc exec $CONTAINER_NAME -- systemctl status zookeeper"
        echo
        echo "View logs:"
        echo "  lxc exec $CONTAINER_NAME -- journalctl -u zookeeper -f"
        echo
        echo "Connect to container:"
        echo "  lxc exec $CONTAINER_NAME -- bash"
        echo
        echo "Check Zookeeper status:"
        echo "  lxc exec $CONTAINER_NAME -- /opt/zookeeper/bin/zkServer.sh status"
        echo
    else
        echo
        echo "Warning: Zookeeper service failed to start"
        echo "Check logs with: lxc exec $CONTAINER_NAME -- journalctl -u zookeeper -n 50"
    fi
else
    echo
    echo "=========================================="
    echo "  Container Created (Service Not Started)"
    echo "=========================================="
    echo "Container: $CONTAINER_NAME"
    echo
    echo "To start Zookeeper:"
    echo "  lxc exec $CONTAINER_NAME -- systemctl start zookeeper"
    echo
fi
