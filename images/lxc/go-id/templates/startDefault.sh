#!/bin/bash

# Go-ID Container - Quick Start Script
# Imports the container image and launches it with default configuration

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
CONFIG_FILE="${SCRIPT_DIR}/sample.conf"

# Color output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

echo -e "${GREEN}=== Go-ID Container Quick Start ===${NC}\n"

# Check if config exists
if [ ! -f "$CONFIG_FILE" ]; then
    echo -e "${RED}Error: Configuration file not found: $CONFIG_FILE${NC}"
    echo "Please ensure sample.conf exists in the same directory as this script"
    exit 1
fi

# Load configuration
echo "Loading configuration from: $CONFIG_FILE"
source "$CONFIG_FILE"

# Find the container image in current directory
IMAGE_FILE=$(ls -t ${SCRIPT_DIR}/go-id-*.tar.gz 2>/dev/null | head -1)

if [ -z "$IMAGE_FILE" ]; then
    echo -e "${RED}Error: No container image found (go-id-*.tar.gz)${NC}"
    echo "Please ensure the container image is in the same directory as this script"
    exit 1
fi

echo -e "${GREEN}Found image:${NC} $IMAGE_FILE\n"

# Check if image already imported
if sudo lxc image list --format csv | grep -q "^${IMAGE_NAME},"; then
    echo -e "${YELLOW}Image '${IMAGE_NAME}' already exists${NC}"
    read -p "Do you want to delete and re-import? (y/N): " -n 1 -r
    echo
    if [[ $REPLY =~ ^[Yy]$ ]]; then
        echo "Deleting existing image..."
        sudo lxc image delete "${IMAGE_NAME}"
    else
        echo "Using existing image"
    fi
fi

# Import image if not exists
if ! sudo lxc image list --format csv | grep -q "^${IMAGE_NAME},"; then
    echo "Importing container image..."
    sudo lxc image import "$IMAGE_FILE" --alias "${IMAGE_NAME}"
    echo -e "${GREEN}✓ Image imported as '${IMAGE_NAME}'${NC}\n"
fi

# Check if container exists
if sudo lxc list --format csv | grep -q "^${CONTAINER_NAME},"; then
    echo -e "${YELLOW}Container '${CONTAINER_NAME}' already exists${NC}"
    read -p "Do you want to delete and recreate? (y/N): " -n 1 -r
    echo
    if [[ $REPLY =~ ^[Yy]$ ]]; then
        echo "Stopping and deleting existing container..."
        sudo lxc stop "${CONTAINER_NAME}" --force 2>/dev/null || true
        sudo lxc delete "${CONTAINER_NAME}" --force
    else
        echo "Using existing container"
        sudo lxc start "${CONTAINER_NAME}" 2>/dev/null || true
        echo -e "${GREEN}✓ Container started${NC}"
        sudo lxc list "${CONTAINER_NAME}"
        exit 0
    fi
fi

# Create container
echo "Creating container: ${CONTAINER_NAME}"
sudo lxc init "${IMAGE_NAME}" "${CONTAINER_NAME}"

# Configure network
echo "Configuring network..."
sudo lxc config device add "${CONTAINER_NAME}" eth0 nic \
    nictype=bridged \
    parent="${HOST_INTERFACE}" \
    ipv4.address="${CONTAINER_IP}"

# Configure port mapping
if [ -n "$HTTP_PORT_MAPPING" ]; then
    IFS=':' read -r HOST_PORT CONTAINER_PORT <<< "$HTTP_PORT_MAPPING"
    echo "Configuring port mapping: ${HOST_PORT} -> ${CONTAINER_PORT}"
    sudo lxc config device add "${CONTAINER_NAME}" http-port proxy \
        listen=tcp:0.0.0.0:${HOST_PORT} \
        connect=tcp:127.0.0.1:${CONTAINER_PORT}
fi

# Configure storage quota
if [ -n "$STORAGE_QUOTA" ]; then
    echo "Setting storage quota: ${STORAGE_QUOTA}"
    sudo lxc config device override "${CONTAINER_NAME}" root size="${STORAGE_QUOTA}"
fi

# Add bind mounts if configured
for i in {1..10}; do
    BIND_VAR="BIND_MOUNT_${i}"
    BIND_VALUE="${!BIND_VAR}"
    if [ -n "$BIND_VALUE" ]; then
        IFS=':' read -r HOST_PATH CONTAINER_PATH <<< "$BIND_VALUE"
        echo "Adding bind mount: ${HOST_PATH} -> ${CONTAINER_PATH}"
        sudo lxc config device add "${CONTAINER_NAME}" "bind-mount-${i}" disk \
            source="${HOST_PATH}" \
            path="${CONTAINER_PATH}"
    fi
done

# Set environment variables
for i in {1..10}; do
    ENV_VAR="ENV_VAR_${i}"
    ENV_VALUE="${!ENV_VAR}"
    if [ -n "$ENV_VALUE" ]; then
        IFS='=' read -r KEY VALUE <<< "$ENV_VALUE"
        echo "Setting environment variable: ${KEY}"
        sudo lxc config set "${CONTAINER_NAME}" "environment.${KEY}" "${VALUE}"
    fi
done

# Set Go-ID specific environment
sudo lxc config set "${CONTAINER_NAME}" environment.GO_ID_PORT "${GO_ID_PORT}"
sudo lxc config set "${CONTAINER_NAME}" environment.GO_ID_SHARD_ID "${GO_ID_SHARD_ID}"
sudo lxc config set "${CONTAINER_NAME}" environment.GO_ID_DATACENTER_ID "${GO_ID_DATACENTER_ID}"

# Start container
echo -e "\nStarting container..."
sudo lxc start "${CONTAINER_NAME}"

# Wait for network
echo "Waiting for container network..."
sleep 3

# Show container info
echo -e "\n${GREEN}=== Container Started Successfully ===${NC}\n"
sudo lxc list "${CONTAINER_NAME}"

echo -e "\n${GREEN}Go-ID Service:${NC}"
echo "  API: http://localhost:${HTTP_PORT_MAPPING%%:*}"
echo "  Endpoints:"
echo "    - GET  /next-id/{type}           - Get next ID"
echo "    - GET  /next-batch/{type}/{size} - Get batch of IDs"
echo "    - GET  /status                   - Service status"
echo ""
echo -e "${YELLOW}To access container:${NC} sudo lxc exec ${CONTAINER_NAME} -- bash"
echo -e "${YELLOW}To stop container:${NC} sudo lxc stop ${CONTAINER_NAME}"
echo -e "${YELLOW}To delete container:${NC} sudo lxc delete ${CONTAINER_NAME} --force"
