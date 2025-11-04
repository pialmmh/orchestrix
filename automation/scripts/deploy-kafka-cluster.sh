#!/bin/bash
# Deploy Kafka & Zookeeper cluster to 3 servers using Java automation

set -e

cd "$(dirname "$0")/../.."

SERVERS=(
    "123.200.0.50:8210"
    "123.200.0.117:8210"
    "123.200.0.51:8210"
)

USER="tbsms"
PASSWORD="TB@l38800"

echo "═══════════════════════════════════════════════════════"
echo "  Kafka & Zookeeper 3-Server Cluster Deployment"
echo "  Using Java Automation Framework"
echo "═══════════════════════════════════════════════════════"
echo ""

SUCCESS_COUNT=0
FAIL_COUNT=0

for i in "${!SERVERS[@]}"; do
    SERVER_NUM=$((i + 1))
    HOST=$(echo "${SERVERS[$i]}" | cut -d: -f1)
    PORT=$(echo "${SERVERS[$i]}" | cut -d: -f2)

    echo "───────────────────────────────────────────────────────"
    echo "  Server $SERVER_NUM: $HOST:$PORT"
    echo "───────────────────────────────────────────────────────"
    echo ""

    # Step 1: Clean up Docker Kafka on first server only
    if [ $i -eq 0 ]; then
        echo "Cleaning up Docker Kafka containers..."
        sshpass -p "$PASSWORD" ssh -o StrictHostKeyChecking=no -p $PORT $USER@$HOST << 'ENDSSH' 2>&1 || true
echo "Stopping Docker Kafka containers..."
docker ps -a | grep kafka | awk '{print $1}' | xargs -r docker stop 2>/dev/null || true
docker ps -a | grep kafka | awk '{print $1}' | xargs -r docker rm 2>/dev/null || true
docker volume ls | grep kafka | awk '{print $2}' | xargs -r docker volume rm 2>/dev/null || true
rm -rf ~/kafka-data ~/kafka-logs 2>/dev/null || true
echo "✓ Docker Kafka cleanup complete"
ENDSSH
        echo ""
    fi

    # Step 2: Deploy using Java automation
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
        echo "Waiting 3 seconds before next server..."
        echo ""
        sleep 3
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
    exit 0
else
    echo "✗ Some deployments failed. Check logs above."
    exit 1
fi
