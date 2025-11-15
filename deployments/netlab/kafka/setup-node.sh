#!/bin/bash
# Kafka Node Setup Script
# Sets up Docker Compose and networking for a single Kafka node

set -e

# Check arguments
if [ "$#" -ne 1 ]; then
    echo "Usage: $0 <node_number>"
    echo "Example: $0 1"
    exit 1
fi

NODE_NUM=$1

# Load configuration
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
CONFIG_FILE="${SCRIPT_DIR}/cluster-config.conf"

if [ ! -f "$CONFIG_FILE" ]; then
    echo "Error: Configuration file not found: $CONFIG_FILE"
    exit 1
fi

source "$CONFIG_FILE"

# Get node-specific variables
eval MGMT_IP=\$NODE${NODE_NUM}_MGMT_IP
eval SSH_USER=\$NODE${NODE_NUM}_SSH_USER
eval SSH_PASS=\$NODE${NODE_NUM}_SSH_PASS
eval KAFKA_IP=\$NODE${NODE_NUM}_KAFKA_IP
eval KAFKA_ID=\$NODE${NODE_NUM}_KAFKA_ID
eval BRIDGE_NAME=\$NODE${NODE_NUM}_BRIDGE_NAME
eval SUBNET=\$NODE${NODE_NUM}_SUBNET

# Validate variables
if [ -z "$MGMT_IP" ]; then
    echo "Error: Node $NODE_NUM not configured in cluster-config.conf"
    exit 1
fi

echo "=========================================="
echo "Setting up Kafka Node ${NODE_NUM}"
echo "=========================================="
echo "Management IP: $MGMT_IP"
echo "Kafka IP: $KAFKA_IP"
echo "Kafka ID: $KAFKA_ID"
echo "Bridge: $BRIDGE_NAME"
echo "=========================================="

# SSH command wrapper
SSH_CMD="sshpass -p '$SSH_PASS' ssh -p $SSH_PORT -o StrictHostKeyChecking=no ${SSH_USER}@${MGMT_IP}"

echo ""
echo "Step 1: Checking bridge interface..."
$SSH_CMD "ip addr show $BRIDGE_NAME | grep -q 'inet' && echo 'Bridge $BRIDGE_NAME exists' || (echo 'Error: Bridge $BRIDGE_NAME not found'; exit 1)"

echo ""
echo "Step 2: Adding Kafka IP as secondary address to bridge..."
$SSH_CMD "sudo ip addr add ${KAFKA_IP}/24 dev ${BRIDGE_NAME} 2>/dev/null || echo 'IP already exists (OK)'"

echo ""
echo "Step 3: Verifying IP configuration..."
$SSH_CMD "ip addr show $BRIDGE_NAME | grep 'inet '"

echo ""
echo "Step 4: Creating Kafka data directory..."
$SSH_CMD "sudo mkdir -p $KAFKA_DATA_DIR"

echo ""
echo "Step 5: Creating Kafka working directory..."
KAFKA_WORK_DIR="${KAFKA_WORK_DIR_PREFIX}-${NODE_NUM}"
$SSH_CMD "mkdir -p ~/${KAFKA_WORK_DIR}"

echo ""
echo "Step 6: Creating docker-compose.yml..."
DOCKER_COMPOSE_CONTENT="version: '3.8'

services:
  kafka:
    image: ${KAFKA_IMAGE}
    container_name: kafka-node
    restart: always
    network_mode: host
    environment:
      KAFKA_NODE_ID: ${KAFKA_ID}
      KAFKA_PROCESS_ROLES: \"broker,controller\"
      KAFKA_LISTENERS: \"PLAINTEXT://${KAFKA_IP}:9092,CONTROLLER://${KAFKA_IP}:9093\"
      KAFKA_ADVERTISED_LISTENERS: \"PLAINTEXT://${KAFKA_IP}:9092\"
      KAFKA_CONTROLLER_LISTENER_NAMES: \"CONTROLLER\"
      KAFKA_CONTROLLER_QUORUM_VOTERS: \"${CONTROLLER_QUORUM_VOTERS}\"
      KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR: ${KAFKA_OFFSETS_REPLICATION_FACTOR}
      KAFKA_TRANSACTION_STATE_LOG_REPLICATION_FACTOR: ${KAFKA_TRANSACTION_REPLICATION_FACTOR}
      KAFKA_TRANSACTION_STATE_LOG_MIN_ISR: ${KAFKA_TRANSACTION_MIN_ISR}
      KAFKA_AUTO_CREATE_TOPICS_ENABLE: \"${KAFKA_AUTO_CREATE_TOPICS}\"
    volumes:
      - ${KAFKA_DATA_DIR}:/var/lib/kafka/data"

$SSH_CMD "cat > ~/${KAFKA_WORK_DIR}/docker-compose.yml << 'EOF'
$DOCKER_COMPOSE_CONTENT
EOF"

echo ""
echo "Step 7: Verifying docker-compose.yml..."
$SSH_CMD "cat ~/${KAFKA_WORK_DIR}/docker-compose.yml"

echo ""
echo "Step 8: Starting Kafka container..."
$SSH_CMD "cd ~/${KAFKA_WORK_DIR} && sudo docker compose up -d"

echo ""
echo "Step 9: Waiting for Kafka to start (10 seconds)..."
sleep 10

echo ""
echo "Step 10: Verifying Kafka is listening..."
$SSH_CMD "nc -zv ${KAFKA_IP} 9092 2>&1"

echo ""
echo "=========================================="
echo "Node ${NODE_NUM} setup completed successfully!"
echo "Kafka broker accessible at: ${KAFKA_IP}:9092"
echo "=========================================="
