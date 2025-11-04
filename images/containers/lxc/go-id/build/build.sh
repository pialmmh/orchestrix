#!/bin/bash
#
# Build script for Go-ID Alpine container
# Copies existing standalone binary into minimal Alpine container
#

set -e

# Load configuration
CONFIG_FILE="${1:-$(dirname "$0")/build.conf}"
if [ ! -f "$CONFIG_FILE" ]; then
    echo "Configuration file not found: $CONFIG_FILE"
    exit 1
fi

echo "Loading configuration from: $CONFIG_FILE"
source "$CONFIG_FILE"

echo "========================================="
echo "Building Alpine Container: ${CONTAINER_NAME}"
echo "========================================="
echo "Version: ${VERSION}"
echo "Binary: ${BINARY_SOURCE}"
echo "========================================="

# Check if binary exists
if [ ! -f "${BINARY_SOURCE}" ]; then
    echo "ERROR: Binary not found: ${BINARY_SOURCE}"
    echo ""
    echo "Please build the binary first:"
    echo "  cd /home/mustafa/telcobright-projects/orchestrix/images/standalone-binaries/go-id"
    echo "  mvn exec:java -Dexec.mainClass="com.telcobright.orchestrix.automation.binary.goid.GoIdBinaryBuildRunner" -Dexec.args="${BINARY_VERSION}""
    exit 1
fi

# Clean existing container
echo "Checking for existing container..."
if lxc info ${BUILD_CONTAINER} >/dev/null 2>&1; then
    echo "Removing existing build container..."
    lxc stop ${BUILD_CONTAINER} --force >/dev/null 2>&1 || true
    lxc delete ${BUILD_CONTAINER} --force
fi

# Create Alpine container
echo "Creating Alpine container..."
lxc launch images:alpine/3.20 ${BUILD_CONTAINER}

# Wait for container to be ready
echo "Waiting for container to be ready..."
sleep 5

# Copy binary
echo "Copying binary to container..."
lxc file push ${BINARY_SOURCE} ${BUILD_CONTAINER}/usr/local/bin/${BINARY_NAME}
lxc exec ${BUILD_CONTAINER} -- chmod +x /usr/local/bin/${BINARY_NAME}

# Create startup script
echo "Creating startup script..."
cat << 'EOF' | lxc exec ${BUILD_CONTAINER} -- tee /etc/init.d/${SERVICE_NAME}
#!/sbin/openrc-run

name="${SERVICE_NAME}"
description="${SERVICE_DESC}"
command="/usr/local/bin/${BINARY_NAME}"
command_args=""
pidfile="/var/run/${SERVICE_NAME}.pid"
command_background=true

depend() {
    need net
}

start_pre() {
    # Environment setup
    export SERVICE_PORT=${SERVICE_PORT}
    export SHARD_ID=${SHARD_ID}
    export TOTAL_SHARDS=${TOTAL_SHARDS}
}
EOF

lxc exec ${BUILD_CONTAINER} -- chmod +x /etc/init.d/${SERVICE_NAME}
lxc exec ${BUILD_CONTAINER} -- rc-update add ${SERVICE_NAME} default

# Stop and publish as image
echo "Stopping container..."
lxc stop ${BUILD_CONTAINER}

# Publish container as image
TIMESTAMP=$(date +%s)
BASE_DIR="/home/mustafa/telcobright-projects/orchestrix/images/lxc/go-id"
IMAGE_FILE="${BASE_DIR}/go-id-v.${VERSION}/generated/artifact/${CONTAINER_NAME}-v${VERSION}-${TIMESTAMP}.tar.gz"
IMAGE_ALIAS="go-id-v${VERSION}-${TIMESTAMP}"

echo "Publishing as image: ${IMAGE_ALIAS}"
lxc publish ${BUILD_CONTAINER} --alias "${IMAGE_ALIAS}"

# Export the image
echo "Exporting image to: ${IMAGE_FILE}"
lxc image export "${IMAGE_ALIAS}" "${IMAGE_FILE%.tar.gz}"

# The export creates a .tar.gz automatically, rename if needed
if [ -f "${IMAGE_FILE%.tar.gz}.tar.gz" ]; then
    mv "${IMAGE_FILE%.tar.gz}.tar.gz" "${IMAGE_FILE}"
fi

# Generate MD5
md5sum "${IMAGE_FILE}" > "${IMAGE_FILE}.md5"

# Delete the temporary image alias
lxc image delete "${IMAGE_ALIAS}"

# Copy templates to version-specific generated directory
cp ${BASE_DIR}/templates/sample.conf ${BASE_DIR}/go-id-v.${VERSION}/generated/
cp ${BASE_DIR}/templates/startDefault.sh ${BASE_DIR}/go-id-v.${VERSION}/generated/
chmod +x ${BASE_DIR}/go-id-v.${VERSION}/generated/startDefault.sh

# Clean up
lxc delete ${BUILD_CONTAINER}

echo "========================================="
echo "Build Complete!"
echo "========================================="
echo "Image: ${IMAGE_FILE}"
echo "Size: $(du -h ${IMAGE_FILE} | cut -f1)"
echo ""
echo "Test with:"
echo "  cd ../generated"
echo "  ./startDefault.sh"
echo "========================================="
