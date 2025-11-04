#!/bin/bash
#
# Launch script for Go-ID Alpine container
# Usage: ./launchGoId.sh [config-file]
#

CONFIG_FILE="${1:-sample-config.conf}"

if [ ! -f "$CONFIG_FILE" ]; then
    echo "Configuration file not found: $CONFIG_FILE"
    echo "Usage: $0 [config-file]"
    exit 1
fi

# Load configuration
source "$CONFIG_FILE"

# Import image if provided
if [ -n "${IMAGE_FILE}" ] && [ -f "${IMAGE_FILE}" ]; then
    echo "Importing image: ${IMAGE_FILE}"
    lxc image import "${IMAGE_FILE}" --alias "${IMAGE_ALIAS}"
fi

# Launch container
echo "Launching container: ${CONTAINER_NAME}"
lxc launch "${IMAGE_ALIAS}" "${CONTAINER_NAME}"

# Set environment variables
lxc exec "${CONTAINER_NAME}" -- sh -c "echo 'export SERVICE_PORT=${SERVICE_PORT}' >> /etc/profile"
lxc exec "${CONTAINER_NAME}" -- sh -c "echo 'export SHARD_ID=${SHARD_ID}' >> /etc/profile"
lxc exec "${CONTAINER_NAME}" -- sh -c "echo 'export TOTAL_SHARDS=${TOTAL_SHARDS}' >> /etc/profile"

# Start service
echo "Starting Go-ID service..."
lxc exec "${CONTAINER_NAME}" -- rc-service Go-ID start

echo "Container ${CONTAINER_NAME} is running"
echo "Test with: curl http://$(lxc list ${CONTAINER_NAME} -c4 --format csv | cut -d' ' -f1):${SERVICE_PORT}/health"
