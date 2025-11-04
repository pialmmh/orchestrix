#!/bin/bash
# Copy deployment files to BDCOM servers

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

ORCHESTRIX_DIR="/home/mustafa/telcobright-projects/orchestrix"

echo "═══════════════════════════════════════════════════════"
echo "  Copying Deployment Files to BDCOM Servers"
echo "═══════════════════════════════════════════════════════"
echo ""

# Check VPN
if ! ip addr show tun0 &>/dev/null; then
    echo "✗ Error: VPN connection not active!"
    exit 1
fi
echo "✓ VPN connected"
echo ""

for i in "${!SERVERS[@]}"; do
    SERVER_NUM=$((i + 1))
    HOST=$(echo "${SERVERS[$i]}" | cut -d: -f1)
    PORT=$(echo "${SERVERS[$i]}" | cut -d: -f2)
    PASSWORD="${PASSWORDS[$i]}"

    echo "───────────────────────────────────────────────────────"
    echo "  Server $SERVER_NUM: $HOST:$PORT"
    echo "───────────────────────────────────────────────────────"
    echo ""

    # Create directory structure
    echo "Creating directory structure..."
    sshpass -p "$PASSWORD" ssh -o StrictHostKeyChecking=no -p $PORT $USER@$HOST \
        "mkdir -p ~/orchestrix/images/containers/lxc/zookeeper ~/orchestrix/images/containers/lxc/kafka" 2>&1 | tail -1

    # Copy Zookeeper deployment files
    echo "Copying Zookeeper deployment files..."
    sshpass -p "$PASSWORD" scp -r -o StrictHostKeyChecking=no -P $PORT \
        $ORCHESTRIX_DIR/images/containers/lxc/zookeeper/launch.sh \
        $ORCHESTRIX_DIR/images/containers/lxc/zookeeper/templates \
        $USER@$HOST:~/orchestrix/images/containers/lxc/zookeeper/ 2>&1 | tail -1

    # Copy Kafka deployment files
    echo "Copying Kafka deployment files..."
    sshpass -p "$PASSWORD" scp -r -o StrictHostKeyChecking=no -P $PORT \
        $ORCHESTRIX_DIR/images/containers/lxc/kafka/launch.sh \
        $ORCHESTRIX_DIR/images/containers/lxc/kafka/templates \
        $USER@$HOST:~/orchestrix/images/containers/lxc/kafka/ 2>&1 | tail -1

    # Make launch scripts executable
    echo "Setting permissions..."
    sshpass -p "$PASSWORD" ssh -o StrictHostKeyChecking=no -p $PORT $USER@$HOST \
        "chmod +x ~/orchestrix/images/containers/lxc/zookeeper/launch.sh ~/orchestrix/images/containers/lxc/kafka/launch.sh" 2>&1 | tail -1

    echo "✓ Server $SERVER_NUM complete!"
    echo ""
done

echo "═══════════════════════════════════════════════════════"
echo "  All Deployment Files Copied Successfully!"
echo "═══════════════════════════════════════════════════════"
