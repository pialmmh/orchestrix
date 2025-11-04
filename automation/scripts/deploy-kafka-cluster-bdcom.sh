#!/bin/bash
# Deploy Kafka & Zookeeper cluster to BDCOM servers using Java automation

set -e

cd "$(dirname "$0")/../.."

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

echo "═══════════════════════════════════════════════════════"
echo "  Kafka & Zookeeper 3-Server Cluster Deployment"
echo "  Target: BDCOM Servers"
echo "  Using Java Automation Framework"
echo "═══════════════════════════════════════════════════════"
echo ""
echo "Note: Requires VPN connection to be active"
echo ""

# Check if VPN is connected
if ! ip addr show tun0 &>/dev/null; then
    echo "✗ Error: VPN connection not active!"
    echo "Please connect to VPN first:"
    echo "  nmcli connection up Mostofa"
    echo "  OR"
    echo "  ./automation/scripts/vpn-bdcom.sh start"
    exit 1
fi

echo "✓ VPN connection detected"
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

    # Step 1: Test connectivity
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

    # Step 2: Clean up Docker Kafka containers if they exist
    echo "Cleaning up any existing Kafka/Zookeeper containers..."
    sshpass -p "$PASSWORD" ssh -o StrictHostKeyChecking=no -p $PORT $USER@$HOST << 'ENDSSH' 2>&1 || true
echo "Checking for existing containers..."
docker ps -a | grep -E "kafka|zookeeper" | awk '{print $1}' | xargs -r docker stop 2>/dev/null || true
docker ps -a | grep -E "kafka|zookeeper" | awk '{print $1}' | xargs -r docker rm 2>/dev/null || true
docker volume ls | grep -E "kafka|zookeeper" | awk '{print $2}' | xargs -r docker volume rm 2>/dev/null || true
rm -rf ~/kafka-data ~/kafka-logs ~/zookeeper-data 2>/dev/null || true
echo "✓ Cleanup complete"
ENDSSH
    echo ""

    # Step 3: Deploy using Java automation
    echo "Deploying Kafka & Zookeeper via Java automation..."
    echo ""

    if java -cp ".:automation/api:automation/api/deployment:automation/api/infrastructure:lib/*" \
        automation.api.deployment.KafkaZookeeperDeployment \
        "$HOST" "$USER" "$PORT" "$PASSWORD" "true"; then

        echo ""
        echo "✓ Server $SERVER_NUM deployment successful!"
        echo ""
        ((SUCCESS_COUNT++))
    else
        echo ""
        echo "✗ Server $SERVER_NUM deployment failed!"
        echo ""
        ((FAIL_COUNT++))
    fi

    # Small delay between servers
    if [ $i -lt $((${#SERVERS[@]} - 1)) ]; then
        echo "Waiting 5 seconds before next server..."
        echo ""
        sleep 5
    fi
done

# Final summary
echo "═══════════════════════════════════════════════════════"
echo "  Deployment Complete"
echo "═══════════════════════════════════════════════════════"
echo ""
echo "Successful: $SUCCESS_COUNT/${#SERVERS[@]}"
echo "Failed:     $FAIL_COUNT/${#SERVERS[@]}"
echo ""

if [ $SUCCESS_COUNT -eq ${#SERVERS[@]} ]; then
    echo "✓ All servers deployed successfully!"
    echo ""
    echo "Cluster Bootstrap Servers:"
    echo "  10.255.246.173:9092,10.255.246.174:9092,10.255.246.175:9092"
    echo ""
    exit 0
else
    echo "✗ Some deployments failed. Check logs above."
    exit 1
fi
