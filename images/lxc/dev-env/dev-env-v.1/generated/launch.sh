#!/bin/bash
set -e
CONFIG_FILE="${1:-$(dirname "$0")/sample.conf}"
source "$CONFIG_FILE"

# Fetch Jenkins params from instance config
if [ -n "$JENKINS_INSTANCE" ]; then
    JENKINS_CONFIG="/home/mustafa/telcobright-projects/orchestrix/jenkins/instances/$JENKINS_INSTANCE/config.yml"
    if [ -f "$JENKINS_CONFIG" ]; then
        JENKINS_URL=$(grep "jenkins_url:" "$JENKINS_CONFIG" | awk '{print $2}')
        
        if [ -n "$JENKINS_AGENT_NAME" ]; then
            # Simple extraction of secret
            AGENT_SECRET=$(grep -A1 "  ${JENKINS_AGENT_NAME}:" "$JENKINS_CONFIG" | grep "secret:" | awk '{print $2}')
        fi
    fi
fi

# Launch from versioned image
# Get absolute path to determine version
SCRIPT_DIR=$(cd "$(dirname "$0")" && pwd)
VERSION_DIR=$(cd "$SCRIPT_DIR/.." && pwd)
VERSION=$(basename "$VERSION_DIR" | cut -d. -f2)
BASE_DIR=$(cd "$VERSION_DIR/.." && pwd)
BASE_NAME=$(basename "$BASE_DIR")
IMAGE_NAME="${BASE_NAME}:${VERSION}"

# Check if container already exists
if lxc list --format=csv -c n | grep -q "^${CONTAINER_NAME}$"; then
    echo "⚠️  Container ${CONTAINER_NAME} already exists"
    read -p "Delete existing container? (y/N): " DELETE_EXISTING
    if [[ "$DELETE_EXISTING" =~ ^[Yy]$ ]]; then
        echo "Deleting existing container..."
        lxc delete ${CONTAINER_NAME} --force
    else
        echo "Aborted."
        exit 1
    fi
fi

echo "Launching container from image: ${IMAGE_NAME}"

# Use local: prefix to specify local image (not remote)
lxc launch local:${IMAGE_NAME} ${CONTAINER_NAME}
sleep 3

# Apply bind mounts
for mount in "${BIND_MOUNTS[@]}"; do
    lxc config device add ${CONTAINER_NAME} $(basename ${mount%%:*}) disk source=${mount%%:*} path=${mount#*:}
done

# Push runtime config
cat <<EOF | lxc file push - ${CONTAINER_NAME}/etc/container.conf
export JENKINS_URL="$JENKINS_URL"
export JENKINS_AGENT_NAME="$JENKINS_AGENT_NAME"
export AGENT_SECRET="$AGENT_SECRET"
EOF

# Start agent if configured
if [ -n "$AGENT_SECRET" ]; then
    lxc exec ${CONTAINER_NAME} -- /usr/local/bin/start-jenkins-agent.sh
fi

# Display container status
echo ""
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "✅ Container ${CONTAINER_NAME} is running"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""
echo "Container Info:"
lxc list ${CONTAINER_NAME} --format=table --columns=ns4tS
echo ""
echo "Container Details:"
lxc info ${CONTAINER_NAME} | head -15
echo ""
echo "Quick Commands:"
echo "  Access:  lxc exec ${CONTAINER_NAME} -- bash"
echo "  Stop:    lxc stop ${CONTAINER_NAME}"
echo "  Delete:  lxc delete ${CONTAINER_NAME} --force"
echo "  Status:  lxc list ${CONTAINER_NAME}"