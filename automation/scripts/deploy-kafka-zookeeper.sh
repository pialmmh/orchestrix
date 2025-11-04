#!/bin/bash
#
# Kafka & Zookeeper Deployment Script
#
# This script deploys Kafka and Zookeeper containers to a remote server via SSH.
# It uses the Java automation framework to manage the deployment.
#
# Usage: ./deploy-kafka-zookeeper.sh <host> <user> <port> <password> [useSudo]
#
# Example:
#   ./deploy-kafka-zookeeper.sh 123.200.0.50 tbsms 8210 "TB@l38800" true
#
# Or use environment variables:
#   export DEPLOY_HOST=123.200.0.50
#   export DEPLOY_USER=tbsms
#   export DEPLOY_PORT=8210
#   export DEPLOY_PASSWORD="TB@l38800"
#   export DEPLOY_USE_SUDO=true
#   ./deploy-kafka-zookeeper.sh

set -e

# Script directory
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Parse arguments or use environment variables
HOST="${1:-${DEPLOY_HOST}}"
USER="${2:-${DEPLOY_USER}}"
PORT="${3:-${DEPLOY_PORT}}"
PASSWORD="${4:-${DEPLOY_PASSWORD}}"
USE_SUDO="${5:-${DEPLOY_USE_SUDO:-true}}"

# Validate required parameters
if [ -z "$HOST" ] || [ -z "$USER" ] || [ -z "$PORT" ] || [ -z "$PASSWORD" ]; then
    echo -e "${RED}Error: Missing required parameters${NC}"
    echo ""
    echo "Usage: $0 <host> <user> <port> <password> [useSudo]"
    echo ""
    echo "Example:"
    echo "  $0 123.200.0.50 tbsms 8210 \"TB@l38800\" true"
    echo ""
    echo "Or use environment variables:"
    echo "  export DEPLOY_HOST=123.200.0.50"
    echo "  export DEPLOY_USER=tbsms"
    echo "  export DEPLOY_PORT=8210"
    echo "  export DEPLOY_PASSWORD=\"TB@l38800\""
    echo "  export DEPLOY_USE_SUDO=true"
    echo "  $0"
    echo ""
    exit 1
fi

# Print deployment info
echo -e "${GREEN}═══════════════════════════════════════════════════════${NC}"
echo -e "${GREEN}  Kafka & Zookeeper Deployment${NC}"
echo -e "${GREEN}═══════════════════════════════════════════════════════${NC}"
echo ""
echo "Target Server: $HOST:$PORT"
echo "User: $USER"
echo "Use Sudo: $USE_SUDO"
echo ""

# Change to project root
cd "$PROJECT_ROOT"

# Compile Java classes if needed
if [ ! -d "automation/api/deployment" ]; then
    echo -e "${RED}Error: automation/api/deployment directory not found${NC}"
    exit 1
fi

echo -e "${YELLOW}Compiling Java classes...${NC}"
javac -cp ".:lib/*" \
    automation/api/infrastructure/LxcPrerequisitesManager.java \
    automation/api/deployment/KafkaZookeeperDeployment.java 2>&1 | grep -v "Note:" || true

echo -e "${GREEN}✓ Compilation complete${NC}"
echo ""

# Run deployment
echo -e "${YELLOW}Starting deployment...${NC}"
echo ""

java -cp ".:automation/api:automation/api/deployment:automation/api/infrastructure:lib/*" \
    automation.api.deployment.KafkaZookeeperDeployment \
    "$HOST" "$USER" "$PORT" "$PASSWORD" "$USE_SUDO"

EXIT_CODE=$?

echo ""
if [ $EXIT_CODE -eq 0 ]; then
    echo -e "${GREEN}═══════════════════════════════════════════════════════${NC}"
    echo -e "${GREEN}  Deployment Completed Successfully!${NC}"
    echo -e "${GREEN}═══════════════════════════════════════════════════════${NC}"
else
    echo -e "${RED}═══════════════════════════════════════════════════════${NC}"
    echo -e "${RED}  Deployment Failed${NC}"
    echo -e "${RED}═══════════════════════════════════════════════════════${NC}"
fi
echo ""

exit $EXIT_CODE
