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
            AGENT_SECRET=$(awk "/^agents:/,/^[^ ]/ {
                if (\$0 ~ /^  $JENKINS_AGENT_NAME:/) {found=1} 
                else if (found && \$0 ~ /secret:/) {gsub(/^[ \t]+secret:[ \t]*/, \"\"); print; exit}
            }" "$JENKINS_CONFIG")
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

echo "Launching container from image: ${IMAGE_NAME}"

lxc launch ${IMAGE_NAME} ${CONTAINER_NAME}
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

echo "Container ${CONTAINER_NAME} launched from ${IMAGE_NAME}"