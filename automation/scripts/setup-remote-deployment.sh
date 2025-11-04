#!/bin/bash
# Copy deployment files to remote servers

set -e

SERVERS=(
    "123.200.0.50:8210"
    "123.200.0.117:8210"
    "123.200.0.51:8210"
)

USER="tbsms"
PASSWORD="TB@l38800"

ORCHESTRIX_DIR="/home/mustafa/telcobright-projects/orchestrix"

echo "═══════════════════════════════════════════════════════"
echo "  Copying Deployment Files to Remote Servers"
echo "═══════════════════════════════════════════════════════"
echo ""

for i in "${!SERVERS[@]}"; do
    SERVER_NUM=$((i + 1))
    HOST=$(echo "${SERVERS[$i]}" | cut -d: -f1)
    PORT=$(echo "${SERVERS[$i]}" | cut -d: -f2)

    echo "───────────────────────────────────────────────────────"
    echo "  Server $SERVER_NUM: $HOST:$PORT"
    echo "───────────────────────────────────────────────────────"
    echo ""

    # Create directory structure
    echo "Creating directory structure..."
    sshpass -p "$PASSWORD" ssh -o StrictHostKeyChecking=no -p $PORT $USER@$HOST "mkdir -p ~/orchestrix/images/containers/lxc/zookeeper ~/orchestrix/images/containers/lxc/kafka"

    # Copy Zookeeper deployment files
    echo "Copying Zookeeper deployment files..."
    sshpass -p "$PASSWORD" scp -r -o StrictHostKeyChecking=no -P $PORT \
        $ORCHESTRIX_DIR/images/containers/lxc/zookeeper/launch.sh \
        $ORCHESTRIX_DIR/images/containers/lxc/zookeeper/templates \
        $USER@$HOST:~/orchestrix/images/containers/lxc/zookeeper/

    # Copy Kafka deployment files
    echo "Copying Kafka deployment files..."
    sshpass -p "$PASSWORD" scp -r -o StrictHostKeyChecking=no -P $PORT \
        $ORCHESTRIX_DIR/images/containers/lxc/kafka/launch.sh \
        $ORCHESTRIX_DIR/images/containers/lxc/kafka/templates \
        $USER@$HOST:~/orchestrix/images/containers/lxc/kafka/

    # Make launch scripts executable
    echo "Setting permissions..."
    sshpass -p "$PASSWORD" ssh -o StrictHostKeyChecking=no -p $PORT $USER@$HOST "chmod +x ~/orchestrix/images/containers/lxc/zookeeper/launch.sh ~/orchestrix/images/containers/lxc/kafka/launch.sh"

    echo "✓ Server $SERVER_NUM complete!"
    echo ""
done

echo "═══════════════════════════════════════════════════════"
echo "  All Deployment Files Copied Successfully!"
echo "═══════════════════════════════════════════════════════"
