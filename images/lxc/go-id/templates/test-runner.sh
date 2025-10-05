#!/bin/bash
# LXC Container Test Runner
# Deploys container to remote server for testing

set -e

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
CONFIG_FILE="${SCRIPT_DIR}/test-runner.conf"

echo -e "${GREEN}=== LXC Container Test Runner ===${NC}\n"

# Load configuration
if [ ! -f "$CONFIG_FILE" ]; then
    echo -e "${RED}Error: Configuration file not found: $CONFIG_FILE${NC}"
    exit 1
fi

source "$CONFIG_FILE"

# Display deployment information
echo "Remote Server: ${SSH_USER}@${SERVER_IP}:${SERVER_PORT}"
echo "Container:     ${CONTAINER_NAME}"
echo "Image:         ${IMAGE_NAME}"
echo "Artifact:      ${ARTIFACT_NAME}"
if [ -n "$CONTAINER_IP" ]; then
    echo "IP Address:    ${CONTAINER_IP}"
fi
if [ -n "$PORT_MAPPING" ]; then
    echo "Port Mapping:  ${PORT_MAPPING}"
fi
echo ""

# Confirm deployment
read -p "Proceed with deployment? (y/N): " -n 1 -r
echo
echo ""

if [[ ! $REPLY =~ ^[Yy]$ ]]; then
    echo -e "${YELLOW}Deployment cancelled${NC}"
    exit 0
fi

# Check prerequisites
if [ ! -f "$ARTIFACT_PATH" ]; then
    echo -e "${RED}Error: Artifact not found: $ARTIFACT_PATH${NC}"
    exit 1
fi

# Check sshpass if using password auth
if [ -n "$SSH_PASSWORD" ] && [ -z "$SSH_KEY_PATH" ]; then
    if ! command -v sshpass &> /dev/null; then
        echo -e "${RED}Error: sshpass is required for password authentication${NC}"
        echo "Install with: sudo apt-get install sshpass"
        exit 1
    fi
fi

# Navigate to Orchestrix project root
PROJECT_ROOT="/home/mustafa/telcobright-projects/orchestrix"

if [ ! -d "$PROJECT_ROOT" ]; then
    echo -e "${RED}Error: Orchestrix project not found at $PROJECT_ROOT${NC}"
    exit 1
fi

cd "$PROJECT_ROOT"

# Run Java deployment automation via Maven
echo -e "${GREEN}Starting deployment automation...${NC}\n"

mvn exec:java \
    -Dexec.mainClass="com.telcobright.orchestrix.automation.deploy.DeploymentRunner" \
    -Dexec.args="$CONFIG_FILE"

DEPLOY_EXIT=$?

echo ""

if [ $DEPLOY_EXIT -eq 0 ]; then
    echo -e "${GREEN}=== Deployment Successful ===${NC}"
    echo ""
    echo "Container deployed to: ${SSH_USER}@${SERVER_IP}"
    echo "Container name: ${CONTAINER_NAME}"
    echo ""
    echo -e "${YELLOW}To access container:${NC}"
    echo "  ssh ${SSH_USER}@${SERVER_IP} -t 'lxc exec ${CONTAINER_NAME} -- bash'"
    echo ""
    echo -e "${YELLOW}To check logs:${NC}"
    echo "  ssh ${SSH_USER}@${SERVER_IP} -t 'lxc exec ${CONTAINER_NAME} -- journalctl -u go-id -f'"
    echo ""
    echo -e "${YELLOW}To stop container:${NC}"
    echo "  ssh ${SSH_USER}@${SERVER_IP} -t 'lxc stop ${CONTAINER_NAME}'"
    exit 0
else
    echo -e "${RED}=== Deployment Failed ===${NC}"
    echo "Check logs above for error details"
    exit 1
fi
