#!/bin/bash
set -e

# Load configuration
CONFIG_FILE="${1:-$(dirname "$0")/sample.conf}"
if [ ! -f "$CONFIG_FILE" ]; then
    echo "Error: Config file not found: $CONFIG_FILE"
    echo "Usage: $0 [config-file]"
    exit 1
fi
source "$CONFIG_FILE"

# Pre-launch setup
# Fetch Jenkins params from instance config
if [ -n "$JENKINS_INSTANCE" ]; then
    JENKINS_CONFIG="/home/mustafa/telcobright-projects/orchestrix/jenkins/instances/$JENKINS_INSTANCE/config.yml"
    if [ -f "$JENKINS_CONFIG" ]; then
        JENKINS_URL=$(grep "jenkins_url:" "$JENKINS_CONFIG" | awk '{print $2}')

        if [ -n "$JENKINS_AGENT_NAME" ]; then
            AGENT_SECRET=$(awk "/^agents:/,/^[^ ]/ {
                if (\$0 ~ /^  $JENKINS_AGENT_NAME:/) {found=1}
                else if (found && \$0 ~ /secret:/) {gsub(/^[ \t]+secret:[ \t]*/, \"\"); print; exit}
            }" "$JENKINS_CONFIG")
        fi
    fi
fi


# Resolve image name from directory structure
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

echo "Launching container: ${CONTAINER_NAME}"
echo "From image: ${IMAGE_NAME}"

# Launch container (use local: prefix to avoid remote interpretation)
lxc launch local:${IMAGE_NAME} ${CONTAINER_NAME}

# Wait for container to be ready
echo "Waiting for container initialization..."
for i in {1..30}; do
    if lxc exec ${CONTAINER_NAME} -- systemctl --version &>/dev/null 2>&1; then
        echo "  Container ready"
        break
    fi
    sleep 1
done

# Apply bind mounts if configured
if [ -n "${BIND_MOUNTS+x}" ]; then
    for mount in "${BIND_MOUNTS[@]}"; do
        if [ -n "$mount" ]; then
            HOST_PATH="${mount%%:*}"
            CONTAINER_PATH="${mount#*:}"
            DEVICE_NAME=$(basename "$HOST_PATH" | tr '/' '_')
            echo "  Mounting: $HOST_PATH -> $CONTAINER_PATH"
            lxc config device add ${CONTAINER_NAME} ${DEVICE_NAME} disk source="${HOST_PATH}" path="${CONTAINER_PATH}"
        fi
    done
fi

# Push runtime configuration
# Push Jenkins configuration to container
cat <<EOF | lxc file push - ${CONTAINER_NAME}/etc/container.conf
export JENKINS_URL="$JENKINS_URL"
export JENKINS_AGENT_NAME="$JENKINS_AGENT_NAME"
export AGENT_SECRET="$AGENT_SECRET"
EOF


# Post-launch setup
# Start Jenkins agent if configured
if [ -n "$AGENT_SECRET" ]; then
    echo "Starting Jenkins agent..."
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
