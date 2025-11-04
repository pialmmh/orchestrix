#!/bin/bash
# Upload and import Kafka & Zookeeper base images to BDCOM servers

set -e

SERVERS=(
    "10.255.246.173:15605"
    "10.255.246.174:15605"
    "10.255.246.175:15605"
)

USER="bdcom"
PASSWORDS=(
    "M6nthDNrxcYfPQLu"
    "hqv3gJh63buuwXcu"
    "ReBJxyd3kGDFW5Cm"
)

KAFKA_IMAGE="/home/mustafa/telcobright-projects/orchestrix/images/containers/lxc/kafka/kafka-v.1/generated/artifact/kafka-v1-20251018_204403.tar.gz"
ZOOKEEPER_IMAGE="/home/mustafa/telcobright-projects/orchestrix/images/containers/lxc/zookeeper/zookeeper-v.1/generated/artifact/zookeeper-v1-20251018_210529.tar.gz"

echo "═══════════════════════════════════════════════════════"
echo "  Uploading Base Images to BDCOM Servers"
echo "═══════════════════════════════════════════════════════"
echo ""
echo "Note: Requires VPN connection to be active"
echo ""

# Check if VPN is connected
if ! ip addr show tun0 &>/dev/null; then
    echo "✗ Error: VPN connection not active!"
    echo "Please connect to VPN first:"
    echo "  nmcli connection up Mostofa"
    exit 1
fi

echo "✓ VPN connection detected"
echo ""

# Check if images exist
if [ ! -f "$KAFKA_IMAGE" ]; then
    echo "✗ Error: Kafka image not found: $KAFKA_IMAGE"
    exit 1
fi

if [ ! -f "$ZOOKEEPER_IMAGE" ]; then
    echo "✗ Error: Zookeeper image not found: $ZOOKEEPER_IMAGE"
    exit 1
fi

echo "✓ Local images found"
echo "  Zookeeper: $(du -h "$ZOOKEEPER_IMAGE" | cut -f1)"
echo "  Kafka: $(du -h "$KAFKA_IMAGE" | cut -f1)"
echo ""

SUCCESS_COUNT=0
FAIL_COUNT=0

for i in "${!SERVERS[@]}"; do
    SERVER_NUM=$((i + 1))
    HOST=$(echo "${SERVERS[$i]}" | cut -d: -f1)
    PORT=$(echo "${SERVERS[$i]}" | cut -d: -f2)
    PASSWORD="${PASSWORDS[$i]}"

    echo "───────────────────────────────────────────────────────"
    echo "  Server $SERVER_NUM: $HOST:$PORT"
    echo "───────────────────────────────────────────────────────"
    echo ""

    # Test connectivity
    echo "Testing connectivity..."
    if ! timeout 5 bash -c "echo >/dev/tcp/$HOST/$PORT" 2>/dev/null; then
        echo "✗ Cannot reach server $HOST:$PORT"
        echo "  Skipping this server..."
        echo ""
        ((FAIL_COUNT++))
        continue
    fi
    echo "✓ Server is reachable"
    echo ""

    # Create temp directory on remote
    echo "Creating temp directory..."
    sshpass -p "$PASSWORD" ssh -o StrictHostKeyChecking=no -p $PORT $USER@$HOST "mkdir -p ~/lxc-images" 2>&1 | tail -1
    echo "✓ Directory ready"
    echo ""

    # Upload Zookeeper image
    echo "Uploading Zookeeper image (this may take a few minutes)..."
    if sshpass -p "$PASSWORD" scp -o StrictHostKeyChecking=no -P $PORT \
        "$ZOOKEEPER_IMAGE" \
        $USER@$HOST:~/lxc-images/zookeeper.tar.gz 2>&1 | tail -1; then
        echo "✓ Zookeeper image uploaded"
    else
        echo "✗ Failed to upload Zookeeper image"
        ((FAIL_COUNT++))
        continue
    fi
    echo ""

    # Upload Kafka image
    echo "Uploading Kafka image (this may take several minutes)..."
    if sshpass -p "$PASSWORD" scp -o StrictHostKeyChecking=no -P $PORT \
        "$KAFKA_IMAGE" \
        $USER@$HOST:~/lxc-images/kafka.tar.gz 2>&1 | tail -1; then
        echo "✓ Kafka image uploaded"
    else
        echo "✗ Failed to upload Kafka image"
        ((FAIL_COUNT++))
        continue
    fi
    echo ""

    # Import images
    echo "Importing images into LXC..."
    sshpass -p "$PASSWORD" ssh -o StrictHostKeyChecking=no -p $PORT $USER@$HOST bash << ENDSSH
cd ~/lxc-images

echo "Importing zookeeper-base..."
echo '$PASSWORD' | sudo -S lxc image import zookeeper.tar.gz --alias zookeeper-base 2>&1 | tail -5

echo ""
echo "Importing kafka-base..."
echo '$PASSWORD' | sudo -S lxc image import kafka.tar.gz --alias kafka-base 2>&1 | tail -5

# Verify
echo ""
echo "Verifying images..."
echo '$PASSWORD' | sudo -S lxc image list | grep -E 'kafka-base|zookeeper-base'

# Cleanup
rm -f zookeeper.tar.gz kafka.tar.gz
echo ""
echo "✓ Cleanup complete"
ENDSSH

    if [ $? -eq 0 ]; then
        echo ""
        echo "✓ Server $SERVER_NUM complete!"
        ((SUCCESS_COUNT++))
    else
        echo ""
        echo "✗ Server $SERVER_NUM failed during import"
        ((FAIL_COUNT++))
    fi
    echo ""
done

echo "═══════════════════════════════════════════════════════"
echo "  Upload Complete"
echo "═══════════════════════════════════════════════════════"
echo ""
echo "Successful: $SUCCESS_COUNT/${#SERVERS[@]}"
echo "Failed:     $FAIL_COUNT/${#SERVERS[@]}"
echo ""

if [ $SUCCESS_COUNT -eq ${#SERVERS[@]} ]; then
    echo "✓ All images uploaded and imported successfully!"
    echo ""
    echo "You can now run the deployment:"
    echo "  ./automation/scripts/deploy-kafka-cluster-bdcom.sh"
    echo ""
    exit 0
else
    echo "✗ Some uploads failed. Check logs above."
    exit 1
fi
