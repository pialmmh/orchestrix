#!/bin/bash
#
# Simple Deployment Script for Orchestrix
# Usage: ./deploy.sh <yaml-config>
#

set -e

CONFIG_FILE="$1"

if [ -z "$CONFIG_FILE" ]; then
    echo "Usage: $0 <yaml-config>"
    echo "Examples:"
    echo "  $0 consul-cluster.yaml"
    echo "  $0 go-id.yaml"
    exit 1
fi

if [ ! -f "$CONFIG_FILE" ]; then
    echo "Error: Config file not found: $CONFIG_FILE"
    exit 1
fi

echo "========================================"
echo "Orchestrix Deployment Manager"
echo "========================================"
echo "Config: $CONFIG_FILE"
echo "Time: $(date)"
echo ""

# Compile and run the deployment manager
cd /home/mustafa/telcobright-projects/orchestrix

# Compile if needed
if [ ! -f automation/api/deployment/SimpleDeploymentManager.class ]; then
    echo "Compiling deployment manager..."
    javac -cp "lib/*:." automation/api/deployment/SimpleDeploymentManager.java
fi

# Run deployment
echo "Starting deployment..."
java -cp "lib/*:automation/api/deployment:." automation.api.deployment.SimpleDeploymentManager "$CONFIG_FILE"

EXIT_CODE=$?

if [ $EXIT_CODE -eq 0 ]; then
    echo ""
    echo "✓ Deployment successful!"
else
    echo ""
    echo "✗ Deployment failed!"
fi

exit $EXIT_CODE