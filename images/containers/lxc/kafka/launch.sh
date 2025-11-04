#!/bin/bash

# Kafka Broker Launch Script
# Launches a Kafka broker container from kafka-base image

set -e

if [ $# -lt 1 ]; then
    echo "Usage: $0 <config-file>"
    echo
    echo "Example:"
    echo "  $0 templates/sample.conf"
    echo "  $0 templates/broker1.conf"
    exit 1
fi

CONFIG_FILE="$1"

if [ ! -f "$CONFIG_FILE" ]; then
    echo "Error: Configuration file not found: $CONFIG_FILE"
    exit 1
fi

# Source configuration
source "$CONFIG_FILE"

# Validate required variables
if [ -z "$CONTAINER_NAME" ]; then
    echo "Error: CONTAINER_NAME not set in config"
    exit 1
fi

echo "=========================================="
echo "  Launching Kafka Broker"
echo "=========================================="
echo "Container: $CONTAINER_NAME"
echo "Broker ID: ${BROKER_ID:-1}"
echo

# Determine base image name
BASE_IMAGE=${BASE_IMAGE:-"kafka-base"}

# Launch container from base image
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
echo "Configuring Kafka broker..."
sudo lxc exec "$CONTAINER_NAME" -- bash -c "cat > /etc/kafka/runtime.conf << 'EOFCONF'
BROKER_ID=\"${BROKER_ID:-1}\"
ZOOKEEPER_CONNECT=\"${ZOOKEEPER_CONNECT:-localhost:2181/kafka}\"
LISTENERS=\"${LISTENERS:-PLAINTEXT://0.0.0.0:9092}\"
ADVERTISED_LISTENERS=\"${ADVERTISED_LISTENERS:-PLAINTEXT://localhost:9092}\"
LOG_DIRS=\"${LOG_DIRS:-/data/kafka-logs}\"
KAFKA_HEAP_OPTS=\"${KAFKA_HEAP_OPTS:--Xms1G -Xmx1G}\"
EOFCONF
"

# Start Kafka service if requested
if [ "$AUTO_START" = "true" ]; then
    echo "Starting Kafka service..."
    sudo lxc exec "$CONTAINER_NAME" -- systemctl start kafka

    echo
    echo "Waiting for Kafka to start..."
    sleep 5

    # Check status
    if lxc exec "$CONTAINER_NAME" -- systemctl is-active --quiet kafka; then
        echo "Kafka broker is running"
    else
        echo "Warning: Kafka service failed to start"
        echo "Check logs: lxc exec $CONTAINER_NAME -- journalctl -u kafka -n 50"
    fi
fi

echo
echo "=========================================="
echo "  Container launched successfully!"
echo "=========================================="
echo "Container: $CONTAINER_NAME"
echo "Broker ID: ${BROKER_ID:-1}"
echo
echo "Useful commands:"
echo "  lxc exec $CONTAINER_NAME -- systemctl status kafka"
echo "  lxc exec $CONTAINER_NAME -- journalctl -u kafka -f"
echo "  lxc exec $CONTAINER_NAME -- /opt/kafka/bin/kafka-topics.sh --list --bootstrap-server localhost:9092"
echo
