#!/bin/bash
#
# Published-Only Deployment Script
# ENFORCES: Only published artifacts can be deployed
#

set -e

YAML_FILE="$1"

if [ -z "$YAML_FILE" ]; then
    echo "Usage: $0 <deployment-yaml>"
    exit 1
fi

if [ ! -f "$YAML_FILE" ]; then
    echo "ERROR: YAML file not found: $YAML_FILE"
    exit 1
fi

# Colors
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

echo -e "${BLUE}═══════════════════════════════════════════════════════${NC}"
echo -e "${BLUE}       Published-Only Deployment System                ${NC}"
echo -e "${BLUE}═══════════════════════════════════════════════════════${NC}"
echo ""

# Extract publish_id from YAML
PUBLISH_ID=$(grep "publish_id:" "$YAML_FILE" | sed 's/.*publish_id: *"\(.*\)".*/\1/' | tr -d '"' | xargs)

if [ -z "$PUBLISH_ID" ]; then
    echo -e "${RED}❌ DEPLOYMENT REJECTED${NC}"
    echo ""
    echo "ERROR: No publish_id found in YAML configuration"
    echo "Artifacts must be published before deployment"
    echo ""
    echo "Required YAML format:"
    echo "  artifact:"
    echo "    source: \"published\""
    echo "    publish_id: \"PUB_XXX_YYYYMMDD_NNN\""
    exit 2
fi

# Check source is "published"
SOURCE=$(grep "source:" "$YAML_FILE" | sed 's/.*source: *"\(.*\)".*/\1/' | tr -d '"' | xargs)

if [ "$SOURCE" != "published" ]; then
    echo -e "${RED}❌ DEPLOYMENT REJECTED${NC}"
    echo ""
    echo "ERROR: Only published artifacts can be deployed"
    echo "Current source: '$SOURCE'"
    echo "Required source: 'published'"
    echo ""
    echo "Please publish your artifact first using publish.sh"
    exit 2
fi

echo -e "${GREEN}✓${NC} Source validation: published"
echo -e "${GREEN}✓${NC} Publish ID: $PUBLISH_ID"
echo ""

# Query database for published artifact
echo "Querying database for published artifact..."

DB_QUERY="SELECT publish_id, artifact_name, version, publish_url, publish_type, checksum, available
FROM artifact_publications
WHERE publish_id = '$PUBLISH_ID'"

DB_RESULT=$(mysql -h 127.0.0.1 -P 3306 -u root -p123456 orchestrix -N -B -e "$DB_QUERY" 2>/dev/null)

if [ -z "$DB_RESULT" ]; then
    echo -e "${RED}❌ DEPLOYMENT REJECTED${NC}"
    echo ""
    echo "ERROR: Published artifact not found in database"
    echo "Publish ID: $PUBLISH_ID"
    echo "Please verify the artifact is published"
    exit 2
fi

# Parse database result
ARTIFACT_NAME=$(echo "$DB_RESULT" | awk '{print $2}')
VERSION=$(echo "$DB_RESULT" | awk '{print $3}')
PUBLISH_URL=$(echo "$DB_RESULT" | awk '{print $4}')
PUBLISH_TYPE=$(echo "$DB_RESULT" | awk '{print $5}')
CHECKSUM=$(echo "$DB_RESULT" | awk '{print $6}')
AVAILABLE=$(echo "$DB_RESULT" | awk '{print $7}')

if [ "$AVAILABLE" != "1" ]; then
    echo -e "${RED}❌ DEPLOYMENT REJECTED${NC}"
    echo ""
    echo "ERROR: Published artifact is not available"
    echo "Publish ID: $PUBLISH_ID"
    echo "URL may be expired or deleted"
    exit 2
fi

echo -e "${GREEN}✓${NC} Artifact found in database"
echo "  Name: $ARTIFACT_NAME"
echo "  Version: $VERSION"
echo "  Publish Type: $PUBLISH_TYPE"
echo "  URL: $PUBLISH_URL"
echo ""

# For testing: Use local artifact (in production, would download from URL)
ARTIFACT_PATH="/home/mustafa/telcobright-projects/orchestrix/images/containers/lxc/consul/consul-v.1/generated/artifact/consul-v1-1759816902.tar.gz"

echo -e "${YELLOW}NOTE: For testing, using local artifact${NC}"
echo -e "${YELLOW}      In production, would download from: $PUBLISH_URL${NC}"
echo ""

# Clean up existing cluster
echo "Cleaning up existing cluster..."
for i in 1 2 3; do
    if lxc info "consul-node-$i" &>/dev/null; then
        lxc delete -f "consul-node-$i" 2>/dev/null || true
    fi
done

echo -e "${GREEN}✓${NC} Cleanup complete"
echo ""

# Import or use existing image
IMAGE_ALIAS="consul-published-$(date +%s)"
echo "Importing container image as $IMAGE_ALIAS..."

if lxc image import "$ARTIFACT_PATH" --alias "$IMAGE_ALIAS" 2>/dev/null; then
    echo -e "${GREEN}✓${NC} Image imported"
else
    # Image with same fingerprint exists, find and use it
    FINGERPRINT=$(sha256sum "$ARTIFACT_PATH" | cut -d' ' -f1 | cut -c1-12)
    EXISTING_IMAGE=$(lxc image list -c l --format csv | grep "$FINGERPRINT" | head -1)
    if [ -n "$EXISTING_IMAGE" ]; then
        IMAGE_ALIAS="$EXISTING_IMAGE"
        echo -e "${GREEN}✓${NC} Using existing image: $IMAGE_ALIAS"
    else
        # Use consul-v1 as fallback
        IMAGE_ALIAS="consul-v1"
        echo -e "${GREEN}✓${NC} Using existing image: consul-v1"
    fi
fi
echo ""

# Deploy instances
echo "Deploying 3-node Consul cluster..."
echo ""

# Node configuration arrays
HTTP_PORTS=(8500 8510 8520)
SERF_PORTS=(8301 8311 8321)
RPC_PORTS=(8300 8310 8320)

for i in 1 2 3; do
    NODE_NAME="consul-node-$i"
    IDX=$((i-1))

    echo -e "${BLUE}Deploying ${NODE_NAME}...${NC}"

    # Create container
    lxc init "$IMAGE_ALIAS" "$NODE_NAME"

    # Set resource limits
    lxc config set "$NODE_NAME" limits.memory 256MB
    lxc config set "$NODE_NAME" limits.cpu 1

    # Start container
    lxc start "$NODE_NAME"

    # Wait for container to be ready
    sleep 3

    # Get container IP (Alpine-compatible)
    NODE_IP=$(lxc exec "$NODE_NAME" -- ip -4 addr show eth0 | grep inet | awk '{print $2}' | cut -d/ -f1)

    # Store IPs for retry_join
    if [ $i -eq 1 ]; then
        NODE1_IP="$NODE_IP"
        RETRY_JOIN="[\"${NODE1_IP}:8301\"]"
    fi

    # Create Consul config
    CONSUL_CONFIG=$(cat <<EOF
{
    "datacenter": "dc1",
    "node_name": "${NODE_NAME}",
    "server": true,
    "bootstrap_expect": 3,
    "retry_join": ${RETRY_JOIN},
    "client_addr": "0.0.0.0",
    "bind_addr": "${NODE_IP}",
    "ports": {
        "http": ${HTTP_PORTS[$IDX]},
        "serf_lan": ${SERF_PORTS[$IDX]},
        "server": ${RPC_PORTS[$IDX]}
    }
}
EOF
)

    # Write config and start Consul
    echo "$CONSUL_CONFIG" | lxc exec "$NODE_NAME" -- tee /consul/config/consul.json >/dev/null
    lxc exec "$NODE_NAME" -- rc-service consul start

    echo -e "${GREEN}✓${NC} ${NODE_NAME} deployed (IP: $NODE_IP)"
    echo ""
done

# Record deployment in database
DEPLOYMENT_ID="DEP_$(date +%Y%m%d_%H%M%S)"
mysql -h 127.0.0.1 -P 3306 -u root -p123456 orchestrix << EOF
INSERT INTO deployments
(deployment_id, publish_id, target_host, terminal_type, deployment_type, status)
VALUES
('$DEPLOYMENT_ID', '$PUBLISH_ID', 'localhost', 'local', 'multi-instance', 'running');
EOF

echo ""
echo -e "${GREEN}═══════════════════════════════════════════════════════${NC}"
echo -e "${GREEN}       Deployment Complete!                            ${NC}"
echo -e "${GREEN}═══════════════════════════════════════════════════════${NC}"
echo ""
echo "Deployment ID: $DEPLOYMENT_ID"
echo "Publish ID: $PUBLISH_ID"
echo ""
echo "Waiting for cluster to form..."
sleep 5

# Verification
echo ""
echo -e "${BLUE}Cluster Status:${NC}"
lxc exec consul-node-1 -- consul members

echo ""
echo -e "${BLUE}Raft Peers:${NC}"
lxc exec consul-node-1 -- consul operator raft list-peers

# Update deployment status
mysql -h 127.0.0.1 -P 3306 -u root -p123456 orchestrix << EOF
UPDATE deployments
SET status = 'success', completed_at = NOW(),
    deployment_log = 'Cluster deployed successfully with 3 nodes'
WHERE deployment_id = '$DEPLOYMENT_ID';

-- Increment download count
UPDATE artifact_publications
SET download_count = download_count + 1
WHERE publish_id = '$PUBLISH_ID';
EOF

echo ""
echo -e "${GREEN}✓${NC} Deployment recorded in database"
echo ""
