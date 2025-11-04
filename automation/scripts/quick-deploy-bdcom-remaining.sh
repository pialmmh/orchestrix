#!/bin/bash
# Quick deployment for servers 2 and 3 only

set -e

cd "$(dirname "$0")/../.."

echo "═══════════════════════════════════════════════════════"
echo "  Deploying Kafka to BDCOM Servers 2 & 3"
echo "═══════════════════════════════════════════════════════"
echo ""

# Server 2
echo "Deploying to Server 2 (10.255.246.174)..."
java -cp ".:automation/api:automation/api/deployment:automation/api/infrastructure:lib/*" \
    automation.api.deployment.KafkaZookeeperDeployment \
    "10.255.246.174" "bdcom" "15605" "hqv3gJh63buuwXcu" "false" 2>&1 | tail -50

echo ""
echo "═══════════════════════════════════════════════════════"
echo ""

# Server 3
echo "Deploying to Server 3 (10.255.246.175)..."
java -cp ".:automation/api:automation/api/deployment:automation/api/infrastructure:lib/*" \
    automation.api.deployment.KafkaZookeeperDeployment \
    "10.255.246.175" "bdcom" "15605" "ReBJxyd3kGDFW5Cm" "false" 2>&1 | tail -50

echo ""
echo "═══════════════════════════════════════════════════════"
echo "  Deployment Complete!"
echo "═══════════════════════════════════════════════════════"
