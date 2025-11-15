#!/bin/bash
# Kafka Node 1 Setup Script (Local)
# Run this directly on the Kafka server

set -e

# Node 1 Configuration (local)
KAFKA_IP="10.10.199.20"
KAFKA_ID=1
BRIDGE_NAME="lxdbr0"
KAFKA_DATA_DIR="$HOME/kafka-data"
KAFKA_WORK_DIR="kafka-node-1"
KAFKA_IMAGE="confluentinc/cp-kafka:8.1.0"
CONTROLLER_QUORUM_VOTERS="1@10.10.199.20:9093,2@10.10.198.20:9093,3@10.10.197.20:9093"
KAFKA_OFFSETS_REPLICATION_FACTOR=3
KAFKA_TRANSACTION_REPLICATION_FACTOR=3
KAFKA_TRANSACTION_MIN_ISR=2
KAFKA_AUTO_CREATE_TOPICS="true"

echo "=========================================="
echo "Setting up Kafka Node 1 (Local)"
echo "=========================================="
echo "Kafka IP: $KAFKA_IP"
echo "Kafka ID: $KAFKA_ID"
echo "Bridge: $BRIDGE_NAME"
echo "=========================================="

echo ""
echo "Step 1: Checking bridge interface..."
ip addr show "$BRIDGE_NAME" | grep -q 'inet' && echo "Bridge $BRIDGE_NAME exists" || { echo "Error: Bridge $BRIDGE_NAME not found"; exit 1; }

echo ""
echo "Step 2: Adding Kafka IP as secondary address to bridge..."
sudo ip addr add "${KAFKA_IP}/24" dev "${BRIDGE_NAME}" 2>/dev/null || echo "IP already exists (OK)"

echo ""
echo "Step 3: Verifying IP configuration..."
ip addr show "$BRIDGE_NAME" | grep 'inet '

echo ""
echo "Step 4: Creating Kafka data directory..."
sudo mkdir -p "$KAFKA_DATA_DIR"

echo ""
echo "Step 5: Creating Kafka working directory..."
mkdir -p ~/"$KAFKA_WORK_DIR"

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

cat > ~/"$KAFKA_WORK_DIR"/docker-compose.yml <<EOF
$DOCKER_COMPOSE_CONTENT
EOF

echo ""
echo "Step 7: Verifying docker-compose.yml..."
cat ~/"$KAFKA_WORK_DIR"/docker-compose.yml

echo ""
echo "Step 8: Starting Kafka container..."
cd ~/"$KAFKA_WORK_DIR" && sudo docker compose up -d

echo ""
echo "Step 9: Waiting for Kafka to start (10 seconds)..."
sleep 10

echo ""
echo "Step 10: Verifying Kafka is listening..."
nc -zv "$KAFKA_IP" 9092 2>&1

echo ""
echo "=========================================="
echo "Node 1 setup completed successfully!"
echo "Kafka broker accessible at: ${KAFKA_IP}:9092"
echo "=========================================="
