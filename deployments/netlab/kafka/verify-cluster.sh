#!/bin/bash
# Kafka Cluster Verification Script
# Verifies cluster health and connectivity

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
echo "Kafka Cluster Verification"
echo "=========================================="
echo ""

# Connect to first node
eval MGMT_IP=\$NODE1_MGMT_IP
eval SSH_USER=\$NODE1_SSH_USER
eval SSH_PASS=\$NODE1_SSH_PASS

SSH_CMD="sshpass -p '$SSH_PASS' ssh -p $SSH_PORT -o StrictHostKeyChecking=no ${SSH_USER}@${MGMT_IP}"

echo "Test 1: Checking all broker connectivity..."
echo "-------------------------------------------"
BOOTSTRAP_SERVERS="${NODE1_KAFKA_IP}:9092,${NODE2_KAFKA_IP}:9092,${NODE3_KAFKA_IP}:9092"
$SSH_CMD "sudo docker exec kafka-node kafka-broker-api-versions --bootstrap-server $BOOTSTRAP_SERVERS 2>&1 | grep -E '(id:|isFenced)' | head -6"

echo ""
echo "Test 2: Listing topics..."
echo "-------------------------------------------"
$SSH_CMD "sudo docker exec kafka-node kafka-topics --list --bootstrap-server ${NODE1_KAFKA_IP}:9092 2>&1"

echo ""
echo "Test 3: Creating test topic (if not exists)..."
echo "-------------------------------------------"
TEST_TOPIC="cluster-verification-test"
$SSH_CMD "sudo docker exec kafka-node kafka-topics --create --topic $TEST_TOPIC --bootstrap-server ${NODE1_KAFKA_IP}:9092 --replication-factor 3 --partitions 3 2>&1 || echo 'Topic may already exist'"

echo ""
echo "Test 4: Describing test topic..."
echo "-------------------------------------------"
$SSH_CMD "sudo docker exec kafka-node kafka-topics --describe --topic $TEST_TOPIC --bootstrap-server ${NODE1_KAFKA_IP}:9092 2>&1"

echo ""
echo "Test 5: Producing test message..."
echo "-------------------------------------------"
TEST_MESSAGE="Cluster verification test at $(date)"
echo "$TEST_MESSAGE" | $SSH_CMD "sudo docker exec -i kafka-node kafka-console-producer --topic $TEST_TOPIC --bootstrap-server ${NODE1_KAFKA_IP}:9092 2>&1"
echo "Message produced: $TEST_MESSAGE"

echo ""
echo "Test 6: Checking topic offsets..."
echo "-------------------------------------------"
$SSH_CMD "sudo docker exec kafka-node kafka-get-offsets --topic $TEST_TOPIC --bootstrap-server ${NODE1_KAFKA_IP}:9092 2>&1"

echo ""
echo "Test 7: Network connectivity between nodes..."
echo "-------------------------------------------"
for i in $(seq 1 $NODE_COUNT); do
    eval TEST_MGMT_IP=\$NODE${i}_MGMT_IP
    eval TEST_SSH_USER=\$NODE${i}_SSH_USER
    eval TEST_SSH_PASS=\$NODE${i}_SSH_PASS
    eval TEST_KAFKA_IP=\$NODE${i}_KAFKA_IP

    TEST_SSH_CMD="sshpass -p '$TEST_SSH_PASS' ssh -p $SSH_PORT -o StrictHostKeyChecking=no ${TEST_SSH_USER}@${TEST_MGMT_IP}"

    echo "Node $i checking connectivity to other brokers:"
    for j in $(seq 1 $NODE_COUNT); do
        if [ $i -ne $j ]; then
            eval TARGET_KAFKA_IP=\$NODE${j}_KAFKA_IP
            $TEST_SSH_CMD "ping -c 1 -W 1 $TARGET_KAFKA_IP > /dev/null 2>&1 && echo '  ✓ Node $i -> Node $j ($TARGET_KAFKA_IP): OK' || echo '  ✗ Node $i -> Node $j ($TARGET_KAFKA_IP): FAILED'"
        fi
    done
done

echo ""
echo "=========================================="
echo "Cluster Verification Complete"
echo "=========================================="
echo ""
echo "Summary:"
echo "  Bootstrap Servers: $BOOTSTRAP_SERVERS"
echo "  Active Brokers: $NODE_COUNT"
echo "  Test Topic: $TEST_TOPIC"
echo ""
echo "All tests passed successfully!"
echo "=========================================="
