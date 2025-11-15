#!/bin/bash
# Kafka Cluster Management Script
# Manages the lifecycle of Kafka cluster nodes

set -e

# Check arguments
if [ "$#" -lt 1 ]; then
    echo "Usage: $0 <action> [node_number]"
    echo ""
    echo "Actions:"
    echo "  start    - Start all nodes or specific node"
    echo "  stop     - Stop all nodes or specific node"
    echo "  restart  - Restart all nodes or specific node"
    echo "  status   - Show status of all nodes"
    echo "  logs     - Show logs of specific node"
    echo ""
    echo "Examples:"
    echo "  $0 start           # Start all nodes"
    echo "  $0 start 1         # Start node 1 only"
    echo "  $0 stop            # Stop all nodes"
    echo "  $0 restart 2       # Restart node 2 only"
    echo "  $0 status          # Show status of all nodes"
    echo "  $0 logs 1          # Show logs of node 1"
    exit 1
fi

ACTION=$1
NODE_NUM=${2:-"all"}

# Get script directory
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
CONFIG_FILE="${SCRIPT_DIR}/cluster-config.conf"

# Load configuration
if [ ! -f "$CONFIG_FILE" ]; then
    echo "Error: Configuration file not found: $CONFIG_FILE"
    exit 1
fi

source "$CONFIG_FILE"

# Function to execute action on a node
execute_on_node() {
    local node_num=$1
    local action=$2

    eval MGMT_IP=\$NODE${node_num}_MGMT_IP
    eval SSH_USER=\$NODE${node_num}_SSH_USER
    eval SSH_PASS=\$NODE${node_num}_SSH_PASS
    eval KAFKA_IP=\$NODE${node_num}_KAFKA_IP

    if [ -z "$MGMT_IP" ]; then
        echo "Error: Node $node_num not configured"
        return 1
    fi

    SSH_CMD="sshpass -p '$SSH_PASS' ssh -p $SSH_PORT -o StrictHostKeyChecking=no ${SSH_USER}@${MGMT_IP}"
    WORK_DIR="${KAFKA_WORK_DIR_PREFIX}-${node_num}"

    echo "Node ${node_num} (${KAFKA_IP}): ${action}..."

    case "$action" in
        start)
            $SSH_CMD "cd ~/${WORK_DIR} && sudo docker compose up -d"
            ;;
        stop)
            $SSH_CMD "cd ~/${WORK_DIR} && sudo docker compose down"
            ;;
        restart)
            $SSH_CMD "cd ~/${WORK_DIR} && sudo docker compose restart"
            ;;
        status)
            $SSH_CMD "sudo docker ps -a --filter name=kafka-node --format 'table {{.Names}}\t{{.Status}}\t{{.Ports}}'"
            ;;
        logs)
            $SSH_CMD "sudo docker logs kafka-node --tail 50"
            ;;
    esac
}

# Main execution
echo "=========================================="
echo "Kafka Cluster Management"
echo "=========================================="
echo "Action: $ACTION"
echo "Target: $NODE_NUM"
echo "=========================================="
echo ""

if [ "$NODE_NUM" = "all" ]; then
    # Execute on all nodes
    for i in $(seq 1 $NODE_COUNT); do
        execute_on_node $i $ACTION
        echo ""
    done
else
    # Execute on specific node
    if [ "$NODE_NUM" -lt 1 ] || [ "$NODE_NUM" -gt "$NODE_COUNT" ]; then
        echo "Error: Invalid node number. Must be between 1 and $NODE_COUNT"
        exit 1
    fi
    execute_on_node $NODE_NUM $ACTION
fi

echo "=========================================="
echo "Action '$ACTION' completed"
echo "=========================================="
