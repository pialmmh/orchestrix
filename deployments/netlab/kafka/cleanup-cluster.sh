#!/bin/bash
# Kafka Cluster Cleanup Script
# Removes Kafka containers, data, and network configuration

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
echo "Kafka Cluster Cleanup"
echo "=========================================="
echo "WARNING: This will:"
echo "  1. Stop and remove all Kafka containers"
echo "  2. Remove secondary IPs from bridges"
echo "  3. Optionally delete Kafka data directories"
echo ""
echo "Cluster Name: $CLUSTER_NAME"
echo "Node Count: $NODE_COUNT"
echo "=========================================="
echo ""

# Confirm before proceeding
read -p "Are you sure you want to cleanup the cluster? (yes/no): " CONFIRM
if [ "$CONFIRM" != "yes" ]; then
    echo "Cleanup cancelled."
    exit 0
fi

echo ""
read -p "Do you want to delete Kafka data directories? (yes/no): " DELETE_DATA
echo ""

# Cleanup each node
for i in $(seq 1 $NODE_COUNT); do
    eval MGMT_IP=\$NODE${i}_MGMT_IP
    eval SSH_USER=\$NODE${i}_SSH_USER
    eval SSH_PASS=\$NODE${i}_SSH_PASS
    eval KAFKA_IP=\$NODE${i}_KAFKA_IP
    eval BRIDGE_NAME=\$NODE${i}_BRIDGE_NAME

    if [ -z "$MGMT_IP" ]; then
        continue
    fi

    SSH_CMD="sshpass -p '$SSH_PASS' ssh -p $SSH_PORT -o StrictHostKeyChecking=no ${SSH_USER}@${MGMT_IP}"
    WORK_DIR="${KAFKA_WORK_DIR_PREFIX}-${i}"

    echo "=========================================="
    echo "Cleaning up Node $i (${KAFKA_IP})"
    echo "=========================================="

    echo "Step 1: Stopping and removing Kafka container..."
    $SSH_CMD "cd ~/${WORK_DIR} 2>/dev/null && sudo docker compose down || echo 'Container not running'"

    echo "Step 2: Removing secondary IP from bridge..."
    $SSH_CMD "sudo ip addr del ${KAFKA_IP}/24 dev ${BRIDGE_NAME} 2>/dev/null || echo 'IP already removed'"

    if [ "$DELETE_DATA" = "yes" ]; then
        echo "Step 3: Removing Kafka data directory..."
        $SSH_CMD "sudo rm -rf ${KAFKA_DATA_DIR}"
        echo "Step 4: Removing working directory..."
        $SSH_CMD "rm -rf ~/${WORK_DIR}"
    else
        echo "Step 3: Keeping Kafka data directory (not deleted)"
    fi

    echo "✓ Node $i cleaned up"
    echo ""
done

echo "=========================================="
echo "Cleanup Complete"
echo "=========================================="
if [ "$DELETE_DATA" = "yes" ]; then
    echo "All containers, network IPs, and data removed."
else
    echo "All containers and network IPs removed."
    echo "Data directories preserved for reuse."
fi
echo "=========================================="
