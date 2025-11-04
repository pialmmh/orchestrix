#!/bin/bash
# Upload and import Kafka & Zookeeper base images to remote servers

set -e

SERVERS=(
    "123.200.0.50:8210"
    "123.200.0.117:8210"
    "123.200.0.51:8210"
)

USER="tbsms"
PASSWORD="TB@l38800"

KAFKA_IMAGE="/home/mustafa/telcobright-projects/orchestrix/images/containers/lxc/kafka/kafka-v.1/generated/artifact/kafka-v1-20251018_204403.tar.gz"
ZOOKEEPER_IMAGE="/home/mustafa/telcobright-projects/orchestrix/images/containers/lxc/zookeeper/zookeeper-v.1/generated/artifact/zookeeper-v1-20251018_210529.tar.gz"

echo "═══════════════════════════════════════════════════════"
echo "  Uploading Base Images to Remote Servers"
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

    # Create temp directory on remote
    echo "Creating temp directory..."
    sshpass -p "$PASSWORD" ssh -o StrictHostKeyChecking=no -p $PORT $USER@$HOST "mkdir -p ~/lxc-images"

    # Upload Zookeeper image
    echo "Uploading Zookeeper image (215MB)..."
    sshpass -p "$PASSWORD" scp -o StrictHostKeyChecking=no -P $PORT \
        "$ZOOKEEPER_IMAGE" \
        $USER@$HOST:~/lxc-images/zookeeper.tar.gz

    # Upload Kafka image
    echo "Uploading Kafka image (475MB)..."
    sshpass -p "$PASSWORD" scp -o StrictHostKeyChecking=no -P $PORT \
        "$KAFKA_IMAGE" \
        $USER@$HOST:~/lxc-images/kafka.tar.gz

    # Import images
    echo "Importing images into LXC..."
    sshpass -p "$PASSWORD" ssh -o StrictHostKeyChecking=no -p $PORT $USER@$HOST << 'ENDSSH'
cd ~/lxc-images

echo "Importing zookeeper-base..."
echo 'TB@l38800' | sudo -S lxc image import zookeeper.tar.gz --alias zookeeper-base 2>&1 | tail -5

echo "Importing kafka-base..."
echo 'TB@l38800' | sudo -S lxc image import kafka.tar.gz --alias kafka-base 2>&1 | tail -5

# Verify
echo ""
echo "Verifying images..."
echo 'TB@l38800' | sudo -S lxc image list | grep -E 'kafka-base|zookeeper-base'

# Cleanup
rm -f zookeeper.tar.gz kafka.tar.gz
ENDSSH

    echo ""
    echo "✓ Server $SERVER_NUM complete!"
    echo ""
done

echo "═══════════════════════════════════════════════════════"
echo "  All Images Uploaded and Imported Successfully!"
echo "═══════════════════════════════════════════════════════"
