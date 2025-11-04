#!/bin/bash

# Deploy Kafka & Zookeeper cluster to 3 servers using Java automation
# This is a wrapper around the Java KafkaZookeeperDeployment tool

set -e

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

# Step 1: Clean up Docker Kafka on first server
echo "Step 1: Cleaning up Docker Kafka on ${SERVERS[0]}"
echo "───────────────────────────────────────────────────────"

HOST=$(echo "${SERVERS[0]}" | cut -d: -f1)
PORT=$(echo "${SERVERS[0]}" | cut -d: -f2)

sshpass -p "$PASSWORD" ssh -o StrictHostKeyChecking=no -p $PORT $USER@$HOST << 'ENDSSH'
echo "Stopping Docker Kafka containers..."
docker ps -a | grep kafka | awk '{print $1}' | xargs -r docker stop 2>/dev/null || true
docker ps -a | grep kafka | awk '{print $1}' | xargs -r docker rm 2>/dev/null || true
docker volume ls | grep kafka | awk '{print $2}' | xargs -r docker volume rm 2>/dev/null || true
rm -rf ~/kafka-data ~/kafka-logs 2>/dev/null || true
echo "✓ Docker Kafka cleanup complete"
ENDSSH

echo ""

# Step 2: Deploy to each server using Java automation
# Note: The Java tool needs to be modified to accept command-line arguments
# For now, this is a placeholder that shows the intended approach

echo "Note: Full Java automation deployment requires modifying"
echo "KafkaZookeeperDeployment.java to accept command-line arguments."
echo ""
echo "Current status: Java automation verified working on localhost."
echo ""
echo "To complete deployment to remote servers:"
echo "  1. Modify KafkaZookeeperDeployment.java main() to accept:"
echo "     - host, user, port, password as args"
echo "  2. Recompile with proper classpath"
echo "  3. Call java deployment for each server in a loop"
echo ""
echo "Alternative: Use the bash deployment scripts which are ready to use."

