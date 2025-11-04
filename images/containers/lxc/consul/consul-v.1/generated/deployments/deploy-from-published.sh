#!/bin/bash
set -e

PUBLISH_ID="PUB_CONSUL_V1_20251009_170016"

# Query artifact from database
DB_RESULT=$(mysql -h 127.0.0.1 -P 3306 -u root -p123456 orchestrix -N -B << SQL
SELECT p.artifact_name, p.version, p.checksum, 
       l.publish_url, n.rclone_remote, n.base_path
FROM artifact_publications p
JOIN artifact_publish_locations l ON p.publish_id = l.publish_id
JOIN publish_nodes n ON l.node_id = n.node_id
WHERE p.publish_id = '$PUBLISH_ID'
  AND l.publish_status = 'success'
  AND l.available = TRUE
LIMIT 1;
SQL
)

if [ -z "$DB_RESULT" ]; then
    echo "ERROR: Published artifact not found: $PUBLISH_ID"
    exit 1
fi

ARTIFACT_NAME=$(echo "$DB_RESULT" | awk '{print $1}')
VERSION=$(echo "$DB_RESULT" | awk '{print $2}')
RCLONE_REMOTE=$(echo "$DB_RESULT" | awk '{print $5}')
BASE_PATH=$(echo "$DB_RESULT" | awk '{print $6}')

echo "Downloading $ARTIFACT_NAME from Google Drive..."
REMOTE_PATH="$RCLONE_REMOTE:$BASE_PATH/consul/consul-v.1/generated/artifact"

# Download to /tmp
cd /tmp
rm -f consul-*.tar.gz
rclone copy "$REMOTE_PATH/" . --include "consul-*.tar.gz"

ARTIFACT=$(ls -t consul-*.tar.gz 2>/dev/null | head -1)

if [ -z "$ARTIFACT" ]; then
    echo "ERROR: Failed to download artifact"
    exit 1
fi

echo "✓ Downloaded: $ARTIFACT"
echo ""

# Import image
echo "Importing image..."
lxc image delete consul-published 2>/dev/null || true
lxc image import "/tmp/$ARTIFACT" --alias consul-published

echo "✓ Image imported"
echo ""

# Deploy 3-node cluster
echo "Deploying 3-node cluster..."

HTTP_PORTS=(8500 8510 8520)
SERF_PORTS=(8301 8311 8321)
RPC_PORTS=(8300 8310 8320)

for i in 1 2 3; do
    NODE_NAME="consul-node-$i"
    IDX=$((i-1))

    echo "  Creating $NODE_NAME..."
    timeout 30 lxc launch consul-published "$NODE_NAME" || {
        echo "ERROR: Failed to create $NODE_NAME"
        continue
    }

    sleep 2

    NODE_IP=$(lxc exec "$NODE_NAME" -- ip -4 addr show eth0 2>/dev/null | grep inet | awk '{print $2}' | cut -d/ -f1)

    if [ $i -eq 1 ]; then
        NODE1_IP="$NODE_IP"
        RETRY_JOIN="[\"${NODE1_IP}:8301\"]"
    fi

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
    echo "  ✓ $NODE_NAME deployed (IP: $NODE_IP)"
done

echo ""
echo "Waiting for cluster..."
sleep 5

echo ""
lxc exec consul-node-1 -- consul members
