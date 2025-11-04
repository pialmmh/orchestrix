#!/bin/bash
#
# Quick Start Script for Consul 3-Node Cluster (Local Testing)
# Launches 3 Consul nodes on the same machine
#

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

echo "========================================="
echo "Starting Consul 3-Node Cluster"
echo "========================================="
echo ""

# Find the latest container image
IMAGE_FILE=$(ls -t ${SCRIPT_DIR}/artifact/*.tar.gz 2>/dev/null | head -1)

if [ -z "$IMAGE_FILE" ]; then
    echo "ERROR: No container image found in ${SCRIPT_DIR}/artifact/"
    echo "Please build the container first:"
    echo "  cd ../../build && ./build.sh"
    exit 1
fi

echo "Using image: $(basename $IMAGE_FILE)"
echo ""

# Update config files with image path
sed -i "s|IMAGE_PATH=.*|IMAGE_PATH=\"${IMAGE_FILE}\"|" ${SCRIPT_DIR}/node1.conf
sed -i "s|IMAGE_PATH=.*|IMAGE_PATH=\"${IMAGE_FILE}\"|" ${SCRIPT_DIR}/node2.conf
sed -i "s|IMAGE_PATH=.*|IMAGE_PATH=\"${IMAGE_FILE}\"|" ${SCRIPT_DIR}/node3.conf

# Launch Node 1
echo "→ Launching Node 1..."
../../launchConsul.sh ${SCRIPT_DIR}/node1.conf consul-node-1

# Get Node 1 IP
sleep 2
NODE1_IP=$(lxc list consul-node-1 -c 4 --format csv | cut -d' ' -f1)
echo "  Node 1 IP: $NODE1_IP"

# Update node2 and node3 configs with node1 IP:PORT
sed -i "s|RETRY_JOIN=.*|RETRY_JOIN='[\"${NODE1_IP}:8301\"]'|" ${SCRIPT_DIR}/node2.conf
sed -i "s|RETRY_JOIN=.*|RETRY_JOIN='[\"${NODE1_IP}:8301\"]'|" ${SCRIPT_DIR}/node3.conf

# Launch Node 2
echo ""
echo "→ Launching Node 2..."
../../launchConsul.sh ${SCRIPT_DIR}/node2.conf consul-node-2

# Get Node 2 IP
sleep 2
NODE2_IP=$(lxc list consul-node-2 -c 4 --format csv | cut -d' ' -f1)
echo "  Node 2 IP: $NODE2_IP"

# Launch Node 3
echo ""
echo "→ Launching Node 3..."
../../launchConsul.sh ${SCRIPT_DIR}/node3.conf consul-node-3

# Get Node 3 IP
sleep 2
NODE3_IP=$(lxc list consul-node-3 -c 4 --format csv | cut -d' ' -f1)
echo "  Node 3 IP: $NODE3_IP"

# Wait for cluster to form
echo ""
echo "Waiting for cluster to form..."
sleep 5

echo ""
echo "========================================="
echo "Consul Cluster Started!"
echo "========================================="
echo ""
echo "Node 1: $NODE1_IP:8500 (standard ports)"
echo "Node 2: $NODE2_IP:8510 (port +10)"
echo "Node 3: $NODE3_IP:8520 (port +20)"
echo ""
echo "Consul UI:"
echo "  http://$NODE1_IP:8500/ui"
echo "  http://$NODE2_IP:8510/ui"
echo "  http://$NODE3_IP:8520/ui"
echo ""
echo "Check cluster members:"
echo "  lxc exec consul-node-1 -- consul members"
echo ""
echo "Check cluster status:"
echo "  lxc exec consul-node-1 -- consul operator raft list-peers"
echo ""
echo "View logs:"
echo "  lxc exec consul-node-1 -- tail -f /var/log/consul.log"
echo ""
echo "Stop cluster:"
echo "  lxc stop consul-node-1 consul-node-2 consul-node-3"
echo ""
echo "Delete cluster:"
echo "  lxc delete consul-node-1 consul-node-2 consul-node-3 --force"
echo "========================================="
