#!/bin/bash
#
# Simple YAML-based LXC Deployment Script
# Direct LXC execution without Java dependency
#

set -e

CONFIG_FILE="$1"

if [ -z "$CONFIG_FILE" ]; then
    echo "Usage: $0 <yaml-config>"
    exit 1
fi

if [ ! -f "$CONFIG_FILE" ]; then
    echo "Error: Config file not found: $CONFIG_FILE"
    exit 1
fi

echo "================================="
echo "Simple LXC Deployment"
echo "================================="
echo "Config: $CONFIG_FILE"
echo ""

# Extract deployment type
DEPLOY_TYPE=$(grep "type:" "$CONFIG_FILE" | awk '{print $2}' | tr -d '"')
ARTIFACT_PATH=$(grep "path:" "$CONFIG_FILE" | head -1 | awk '{print $2}' | tr -d '"')
ARTIFACT_NAME=$(grep "name:" "$CONFIG_FILE" | head -1 | awk '{print $2}' | tr -d '"')

echo "Deployment type: $DEPLOY_TYPE"
echo "Artifact: $ARTIFACT_NAME"
echo ""

# Function to deploy Consul cluster
deploy_consul_cluster() {
    echo "Deploying Consul 3-node cluster..."

    # Import image
    IMAGE_ALIAS="consul-v1"
    if ! lxc image list | grep -q "$IMAGE_ALIAS"; then
        echo "Importing Consul image..."
        lxc image import "$ARTIFACT_PATH" --alias "$IMAGE_ALIAS"
    fi

    # Deploy 3 nodes
    for i in 1 2 3; do
        NODE_NAME="consul-node-$i"

        # Clean existing
        lxc stop "$NODE_NAME" --force 2>/dev/null || true
        lxc delete "$NODE_NAME" --force 2>/dev/null || true

        # Create container
        echo "Creating $NODE_NAME..."
        lxc init "$IMAGE_ALIAS" "$NODE_NAME"
        lxc config set "$NODE_NAME" limits.memory 256MB
        lxc config set "$NODE_NAME" limits.cpu 1

        # Start container
        lxc start "$NODE_NAME"
        sleep 3

        # Get IP
        NODE_IP=$(lxc list "$NODE_NAME" -c 4 --format csv | cut -d' ' -f1)
        echo "$NODE_NAME IP: $NODE_IP"

        # Configure Consul based on node number
        case $i in
            1)
                HTTP_PORT=8500
                SERF_PORT=8301
                SERVER_PORT=8300
                RETRY_JOIN="[]"
                ;;
            2)
                HTTP_PORT=8510
                SERF_PORT=8311
                SERVER_PORT=8310
                # Get node 1 IP
                NODE1_IP=$(lxc list consul-node-1 -c 4 --format csv | cut -d' ' -f1)
                RETRY_JOIN="[\"${NODE1_IP}:8301\"]"
                ;;
            3)
                HTTP_PORT=8520
                SERF_PORT=8321
                SERVER_PORT=8320
                # Get node 1 and 2 IPs
                NODE1_IP=$(lxc list consul-node-1 -c 4 --format csv | cut -d' ' -f1)
                NODE2_IP=$(lxc list consul-node-2 -c 4 --format csv | cut -d' ' -f1)
                RETRY_JOIN="[\"${NODE1_IP}:8301\",\"${NODE2_IP}:8311\"]"
                ;;
        esac

        # Create Consul config
        CONSUL_CONFIG=$(cat <<EOF
{
    "node_name": "consul-node-$i",
    "datacenter": "dc1",
    "data_dir": "/consul/data",
    "server": true,
    "bootstrap_expect": 3,
    "bind_addr": "0.0.0.0",
    "client_addr": "0.0.0.0",
    "retry_join": $RETRY_JOIN,
    "ui_config": {"enabled": true},
    "ports": {
        "http": $HTTP_PORT,
        "dns": 8600,
        "server": $SERVER_PORT,
        "serf_lan": $SERF_PORT,
        "serf_wan": $((8302 + ($i - 1) * 10))
    },
    "log_level": "INFO"
}
EOF
)

        # Write config to container
        echo "$CONSUL_CONFIG" | lxc exec "$NODE_NAME" -- tee /consul/config/consul.json > /dev/null

        # Start Consul service
        lxc exec "$NODE_NAME" -- rc-service consul start

        echo "$NODE_NAME deployed successfully"
        echo ""
    done

    # Wait for cluster formation
    echo "Waiting for cluster formation..."
    sleep 10

    # Check cluster status
    echo "Cluster members:"
    lxc exec consul-node-1 -- consul members
}

# Function to deploy single Go-ID instance
deploy_goid_single() {
    echo "Deploying Go-ID service..."

    # Import image
    IMAGE_ALIAS="go-id-v1"
    if ! lxc image list | grep -q "$IMAGE_ALIAS"; then
        echo "Importing Go-ID image..."
        lxc image import "$ARTIFACT_PATH" --alias "$IMAGE_ALIAS"
    fi

    NODE_NAME="go-id-main"

    # Clean existing
    lxc stop "$NODE_NAME" --force 2>/dev/null || true
    lxc delete "$NODE_NAME" --force 2>/dev/null || true

    # Create and start
    lxc launch "$IMAGE_ALIAS" "$NODE_NAME"

    sleep 5

    # Test
    NODE_IP=$(lxc list "$NODE_NAME" -c 4 --format csv | cut -d' ' -f1)
    echo "Go-ID running at: http://$NODE_IP:8080"

    curl -s "http://$NODE_IP:8080/health" && echo ""
}

# Main deployment logic
case "$DEPLOY_TYPE" in
    "multi-instance")
        if [[ "$ARTIFACT_NAME" == *"consul"* ]]; then
            deploy_consul_cluster
        fi
        ;;
    "single")
        if [[ "$ARTIFACT_NAME" == *"go-id"* ]]; then
            deploy_goid_single
        fi
        ;;
    *)
        echo "Unsupported deployment type: $DEPLOY_TYPE"
        exit 1
        ;;
esac

echo ""
echo "================================="
echo "Deployment Complete!"
echo "================================="