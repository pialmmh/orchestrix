#!/bin/bash
# Build Kafka & Zookeeper base images on remote servers

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
echo "  Building Base Images on Remote Servers"
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

    # Create remote directory structure
    echo "Creating directory structure..."
    sshpass -p "$PASSWORD" ssh -o StrictHostKeyChecking=no -p $PORT $USER@$HOST "mkdir -p ~/orchestrix/images/containers/lxc/zookeeper/build ~/orchestrix/images/containers/lxc/kafka/build"

    # Copy Zookeeper build files
    echo "Copying Zookeeper build files..."
    sshpass -p "$PASSWORD" scp -o StrictHostKeyChecking=no -P $PORT \
        $ORCHESTRIX_DIR/images/containers/lxc/zookeeper/build/build.sh \
        $ORCHESTRIX_DIR/images/containers/lxc/zookeeper/build/build.conf \
        $USER@$HOST:~/orchestrix/images/containers/lxc/zookeeper/build/

    # Copy Kafka build files
    echo "Copying Kafka build files..."
    sshpass -p "$PASSWORD" scp -o StrictHostKeyChecking=no -P $PORT \
        $ORCHESTRIX_DIR/images/containers/lxc/kafka/build/build.sh \
        $ORCHESTRIX_DIR/images/containers/lxc/kafka/build/build.conf \
        $USER@$HOST:~/orchestrix/images/containers/lxc/kafka/build/

    # Build Zookeeper base image
    echo "Building Zookeeper base image..."
    sshpass -p "$PASSWORD" ssh -o StrictHostKeyChecking=no -p $PORT $USER@$HOST << 'ENDSSH'
cd ~/orchestrix/images/containers/lxc/zookeeper/build
chmod +x build.sh
echo 'TB@l38800' | sudo -S ./build.sh 2>&1 | tail -20
ENDSSH

    # Build Kafka base image
    echo "Building Kafka base image..."
    sshpass -p "$PASSWORD" ssh -o StrictHostKeyChecking=no -p $PORT $USER@$HOST << 'ENDSSH'
cd ~/orchestrix/images/containers/lxc/kafka/build
chmod +x build.sh
echo 'TB@l38800' | sudo -S ./build.sh 2>&1 | tail -20
ENDSSH

    # Verify images exist
    echo "Verifying images..."
    sshpass -p "$PASSWORD" ssh -o StrictHostKeyChecking=no -p $PORT $USER@$HOST "echo 'TB@l38800' | sudo -S lxc image list | grep -E 'zookeeper-base|kafka-base'"

    echo ""
    echo "✓ Server $SERVER_NUM complete!"
    echo ""
done

echo "═══════════════════════════════════════════════════════"
echo "  All Base Images Built Successfully!"
echo "═══════════════════════════════════════════════════════"
