#!/bin/bash
# Kafka Cluster Deployment Script
# Deploys all Kafka nodes in the cluster

set -e

# Get script directory
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
CONFIG_FILE="${SCRIPT_DIR}/cluster-config.conf"

# Load configuration
if [ ! -f "$CONFIG_FILE" ]; then
    echo "Error: Configuration file not found: $CONFIG_FILE"
    exit 1
fi

source "$CONFIG_FILE"

echo "=========================================="
echo "Kafka Cluster Deployment"
echo "=========================================="
echo "Cluster Name: $CLUSTER_NAME"
echo "Node Count: $NODE_COUNT"
echo "Controller Quorum: $CONTROLLER_QUORUM_VOTERS"
echo "=========================================="
echo ""

# Confirm before proceeding
read -p "Do you want to proceed with the deployment? (yes/no): " CONFIRM
if [ "$CONFIRM" != "yes" ]; then
    echo "Deployment cancelled."
    exit 0
fi

echo ""
echo "Starting deployment..."
echo ""

# Deploy each node
for i in $(seq 1 $NODE_COUNT); do
    echo ""
    echo "=========================================="
    echo "Deploying Node $i..."
    echo "=========================================="

    bash "${SCRIPT_DIR}/setup-node.sh" $i

    if [ $? -eq 0 ]; then
        echo "✓ Node $i deployed successfully"
    else
        echo "✗ Failed to deploy Node $i"
        exit 1
    fi

    echo ""
done

echo ""
echo "=========================================="
echo "All nodes deployed. Waiting for cluster formation..."
echo "=========================================="
sleep 15

# Verify cluster health
echo ""
echo "Verifying cluster health..."
echo ""

# Connect to first node to check cluster
eval MGMT_IP=\$NODE1_MGMT_IP
eval SSH_USER=\$NODE1_SSH_USER
eval SSH_PASS=\$NODE1_SSH_PASS
eval KAFKA_IP=\$NODE1_KAFKA_IP

SSH_CMD="sshpass -p '$SSH_PASS' ssh -p $SSH_PORT -o StrictHostKeyChecking=no ${SSH_USER}@${MGMT_IP}"

echo "Checking broker list..."
$SSH_CMD "sudo docker exec kafka-node kafka-broker-api-versions --bootstrap-server ${NODE1_KAFKA_IP}:9092,${NODE2_KAFKA_IP}:9092,${NODE3_KAFKA_IP}:9092 2>&1 | grep 'id:'"

echo ""
echo "=========================================="
echo "Kafka Cluster Deployment Complete!"
echo "=========================================="
echo ""
echo "Broker Endpoints:"
echo "  Node 1: ${NODE1_KAFKA_IP}:9092"
echo "  Node 2: ${NODE2_KAFKA_IP}:9092"
echo "  Node 3: ${NODE3_KAFKA_IP}:9092"
echo ""
echo "Bootstrap Servers: ${NODE1_KAFKA_IP}:9092,${NODE2_KAFKA_IP}:9092,${NODE3_KAFKA_IP}:9092"
echo "=========================================="
