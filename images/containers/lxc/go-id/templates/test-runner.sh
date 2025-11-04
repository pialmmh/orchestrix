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
# Check if multi-target deployment
TARGET_COUNT=0
for i in {1..100}; do
    TARGET_NAME_VAR="TARGET_${i}_NAME"
    if [ -n "${!TARGET_NAME_VAR}" ]; then
        TARGET_COUNT=$((TARGET_COUNT + 1))
    else
        break
    fi
done

if [ $TARGET_COUNT -gt 0 ]; then
    echo "Deployment Mode: Multi-Target (${TARGET_COUNT} servers)"
    echo "Execution Mode:  ${EXECUTION_MODE:-SEQUENTIAL}"
    echo "Artifact:        ${ARTIFACT_NAME}"
    echo ""
    echo "Targets:"
    for i in $(seq 1 $TARGET_COUNT); do
        TARGET_NAME_VAR="TARGET_${i}_NAME"
        TARGET_IP_VAR="TARGET_${i}_SERVER_IP"
        TARGET_CONTAINER_VAR="TARGET_${i}_CONTAINER_NAME"
        echo "  [$i] ${!TARGET_NAME_VAR} - ${!TARGET_IP_VAR} (${!TARGET_CONTAINER_VAR})"
    done
else
    echo "Deployment Mode: Single Target"
    echo "Remote Server:   ${SSH_USER}@${SERVER_IP}:${SERVER_PORT}"
    echo "Container:       ${CONTAINER_NAME}"
    echo "Image:           ${IMAGE_NAME}"
    echo "Artifact:        ${ARTIFACT_NAME}"
    if [ -n "$CONTAINER_IP" ]; then
        echo "IP Address:      ${CONTAINER_IP}"
    fi
    if [ -n "$PORT_MAPPING" ]; then
        echo "Port Mapping:    ${PORT_MAPPING}"
    fi
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

# Use MultiDeploymentRunner (supports both single and multi-target)
mvn exec:java \
    -Dexec.mainClass="com.telcobright.orchestrix.automation.deploy.MultiDeploymentRunner" \
    -Dexec.args="$CONFIG_FILE"

DEPLOY_EXIT=$?

echo ""

if [ $DEPLOY_EXIT -eq 0 ]; then
    echo -e "${GREEN}=== Deployment Successful ===${NC}"
    echo ""

    if [ $TARGET_COUNT -gt 0 ]; then
        # Multi-target deployment
        echo "Containers deployed to ${TARGET_COUNT} servers"
        echo ""
        echo -e "${YELLOW}Access containers:${NC}"
        for i in $(seq 1 $TARGET_COUNT); do
            TARGET_IP_VAR="TARGET_${i}_SERVER_IP"
            TARGET_USER_VAR="TARGET_${i}_SSH_USER"
            TARGET_CONTAINER_VAR="TARGET_${i}_CONTAINER_NAME"
            echo "  ssh ${!TARGET_USER_VAR}@${!TARGET_IP_VAR} -t 'lxc exec ${!TARGET_CONTAINER_VAR} -- bash'"
        done
    else
        # Single target deployment
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
    fi
    exit 0
else
    echo -e "${RED}=== Deployment Failed ===${NC}"
    echo "Check logs above for error details"
    exit 1
fi
