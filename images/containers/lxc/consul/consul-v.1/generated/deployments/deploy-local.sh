#!/bin/bash
#
# Local Consul Deployment for Testing
# Deploys 3-node consul cluster on local machine
#

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
GENERATED_DIR="$(dirname "$SCRIPT_DIR")"
ARTIFACT_DIR="$GENERATED_DIR/artifact"

# Find latest artifact
ARTIFACT=$(ls -t "$ARTIFACT_DIR"/consul-v1-*.tar.gz 2>/dev/null | head -1)

if [ -z "$ARTIFACT" ]; then
    echo "ERROR: No consul artifact found in $ARTIFACT_DIR"
    exit 1
fi

echo "═══════════════════════════════════════════════════════"
echo "  Local Consul Cluster Deployment"
echo "═══════════════════════════════════════════════════════"
echo ""
echo "Artifact: $(basename "$ARTIFACT")"
echo ""

# Use existing published image
IMAGE_ALIAS="consul-published"
echo "Using existing image: $IMAGE_ALIAS"
echo ""

# Deploy 3-node cluster
HTTP_PORTS=(8500 8510 8520)
SERF_PORTS=(8301 8311 8321)
RPC_PORTS=(8300 8310 8320)

echo "Deploying 3-node cluster..."
echo ""

for i in 1 2 3; do
    NODE_NAME="consul-node-$i"
    IDX=$((i-1))

    echo "Creating $NODE_NAME..."
    lxc launch "$IMAGE_ALIAS" "$NODE_NAME"

    # Set resource limits
    lxc config set "$NODE_NAME" limits.memory 256MB
    lxc config set "$NODE_NAME" limits.cpu 1

    sleep 3

    # Get IP
    NODE_IP=$(lxc exec "$NODE_NAME" -- ip -4 addr show eth0 | grep inet | awk '{print $2}' | cut -d/ -f1)

    if [ $i -eq 1 ]; then
        NODE1_IP="$NODE_IP"
        RETRY_JOIN="[\"${NODE1_IP}:8301\"]"
    fi

    # Create consul config
    CONSUL_CONFIG="{
        \"datacenter\": \"dc1\",
        \"node_name\": \"${NODE_NAME}\",
        \"server\": true,
        \"bootstrap_expect\": 3,
        \"retry_join\": ${RETRY_JOIN},
        \"client_addr\": \"0.0.0.0\",
        \"bind_addr\": \"${NODE_IP}\",
        \"ports\": {
            \"http\": ${HTTP_PORTS[$IDX]},
            \"serf_lan\": ${SERF_PORTS[$IDX]},
            \"server\": ${RPC_PORTS[$IDX]}
        }
    }"

    echo "$CONSUL_CONFIG" | lxc exec "$NODE_NAME" -- tee /consul/config/consul.json >/dev/null
    lxc exec "$NODE_NAME" -- rc-service consul start

    echo "✓ $NODE_NAME deployed (IP: $NODE_IP, HTTP: ${HTTP_PORTS[$IDX]})"
    echo ""
done

echo "Waiting for cluster to form..."
sleep 5

echo ""
echo "═══════════════════════════════════════════════════════"
echo "  Cluster Status"
echo "═══════════════════════════════════════════════════════"
echo ""

lxc exec consul-node-1 -- consul members

echo ""
echo "Raft Peers:"
lxc exec consul-node-1 -- consul operator raft list-peers

echo ""
echo "═══════════════════════════════════════════════════════"
echo "  Consul UI Access"
echo "═══════════════════════════════════════════════════════"
echo ""
echo "Node 1: http://${NODE1_IP}:8500"
echo ""
echo "✓ Consul cluster ready for testing!"
echo ""
