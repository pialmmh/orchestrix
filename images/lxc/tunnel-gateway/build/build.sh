#!/bin/bash
set -e

# Load configuration
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
source "${SCRIPT_DIR}/build.conf"

CONTAINER_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
CONTAINER_BASE=$(basename "$CONTAINER_DIR")

# Check for existing versions
EXISTING_VERSIONS=($(ls -d "$CONTAINER_DIR"/${CONTAINER_BASE}-v.* 2>/dev/null | grep -o 'v\.[0-9]*' | cut -d. -f2 | sort -n))

if [ ${#EXISTING_VERSIONS[@]} -eq 0 ]; then
    DEFAULT_VERSION=1
else
    LATEST=${EXISTING_VERSIONS[-1]}
    DEFAULT_VERSION=$LATEST
fi

# Handle --overwrite flag
FORCE_FLAG=""
if [ "$1" == "--overwrite" ]; then
    FORCE_FLAG="--force"
    BUILD_VERSION=$DEFAULT_VERSION
else
    read -p "Version to build [$DEFAULT_VERSION]: " BUILD_VERSION
    BUILD_VERSION=${BUILD_VERSION:-$DEFAULT_VERSION}
fi

# Validate version is integer
if ! [[ "$BUILD_VERSION" =~ ^[0-9]+$ ]]; then
    echo "Error: Version must be an integer"
    exit 1
fi

VERSION_DIR="${CONTAINER_DIR}/${CONTAINER_BASE}-v.${BUILD_VERSION}"
GENERATED_DIR="${VERSION_DIR}/generated"
ARTIFACT_DIR="${GENERATED_DIR}/artifact"

# Check if version exists
if [ -d "$VERSION_DIR" ] && [ "$1" != "--overwrite" ]; then
    echo "Error: Version $BUILD_VERSION already exists"
    echo "Use --overwrite to rebuild"
    exit 1
fi

# Create directories
mkdir -p "$GENERATED_DIR" "$ARTIFACT_DIR"

echo "=========================================="
echo "Building ${CONTAINER_BASE}:${BUILD_VERSION}"
echo "=========================================="
echo "Base Image: $BASE_IMAGE"
echo "Version: $BUILD_VERSION"
echo ""

# Generate timestamp (hyphens only - underscores not allowed in LXC names)
TIMESTAMP=$(date +%Y%m%d-%H%M%S)
IMAGE_NAME="${CONTAINER_BASE}-base"
BUILD_CONTAINER="${BUILD_CONTAINER_PREFIX}-${BUILD_VERSION}-${TIMESTAMP}"

echo "Creating build container: $BUILD_CONTAINER"
lxc launch "$BASE_IMAGE" "$BUILD_CONTAINER"
sleep 3

echo ""
echo "Installing packages..."
lxc exec "$BUILD_CONTAINER" -- apk update
lxc exec "$BUILD_CONTAINER" -- apk add --no-cache \
    openssh-client \
    autossh \
    sshpass \
    bash \
    curl

echo ""
echo "Creating directory structure..."
lxc exec "$BUILD_CONTAINER" -- mkdir -p /etc/tunnel-gateway /usr/local/bin /var/log/tunnel-gateway

echo ""
echo "Creating startup script..."
lxc exec "$BUILD_CONTAINER" -- bash -c 'cat > /usr/local/bin/start-tunnels.sh << '\''EOF'\''
#!/bin/bash
set -e

# Load tunnel configuration
if [ ! -f /etc/tunnel-gateway/tunnels.conf ]; then
    echo "No tunnel configuration found at /etc/tunnel-gateway/tunnels.conf"
    exit 0
fi

source /etc/tunnel-gateway/tunnels.conf

echo "Starting SSH tunnels..."
echo "======================"

# Start each tunnel
for tunnel_def in "${TUNNELS[@]}"; do
    IFS=':' read -r name local_port ssh_host ssh_user auth_type auth_value remote_host remote_port <<< "$tunnel_def"

    echo "Starting tunnel: $name"
    echo "  Local: 0.0.0.0:$local_port -> SSH: $ssh_user@$ssh_host -> Remote: $remote_host:$remote_port"

    if [ "$auth_type" = "PASSWORD" ]; then
        # Password authentication - uses ssh -f
        sshpass -p "$auth_value" ssh -f -N \
            -o StrictHostKeyChecking=no \
            -o UserKnownHostsFile=/dev/null \
            -o ServerAliveInterval=30 \
            -o ServerAliveCountMax=3 \
            -o LogLevel=ERROR \
            -L 0.0.0.0:${local_port}:${remote_host}:${remote_port} \
            ${ssh_user}@${ssh_host}
    elif [ "$auth_type" = "KEY" ]; then
        # Key authentication - uses autossh for auto-reconnection
        autossh -M 0 -f -N \
            -o StrictHostKeyChecking=no \
            -o UserKnownHostsFile=/dev/null \
            -o ServerAliveInterval=30 \
            -o ServerAliveCountMax=3 \
            -o LogLevel=ERROR \
            -i "$auth_value" \
            -L 0.0.0.0:${local_port}:${remote_host}:${remote_port} \
            ${ssh_user}@${ssh_host}
    else
        echo "  Error: Unknown auth type: $auth_type"
        continue
    fi

    echo "  Started successfully"
done

echo ""
echo "All tunnels started"
echo "Tunnel processes:"
ps aux | grep -E "(autossh|ssh.*ServerAlive)" | grep -v grep
EOF
'

lxc exec "$BUILD_CONTAINER" -- chmod +x /usr/local/bin/start-tunnels.sh

echo ""
echo "Creating tunnel management scripts..."
lxc exec "$BUILD_CONTAINER" -- bash -c 'cat > /usr/local/bin/list-tunnels.sh << '\''EOF'\''
#!/bin/bash
echo "Active SSH Tunnels:"
echo "==================="
ps aux | grep autossh | grep -v grep | awk '\''{print $2, $11, $12, $13, $14, $15}'\''
echo ""
echo "Listening Ports:"
netstat -tlnp 2>/dev/null | grep autossh || echo "No tunnels active"
EOF
'

lxc exec "$BUILD_CONTAINER" -- bash -c 'cat > /usr/local/bin/stop-tunnels.sh << '\''EOF'\''
#!/bin/bash
echo "Stopping all SSH tunnels..."
pkill -9 autossh
echo "All tunnels stopped"
EOF
'

lxc exec "$BUILD_CONTAINER" -- chmod +x /usr/local/bin/list-tunnels.sh /usr/local/bin/stop-tunnels.sh

echo ""
echo "Creating rc.local for auto-start..."
lxc exec "$BUILD_CONTAINER" -- bash -c 'cat > /etc/local.d/tunnels.start << '\''EOF'\''
#!/bin/sh
/usr/local/bin/start-tunnels.sh > /var/log/tunnel-gateway/startup.log 2>&1
EOF
'
lxc exec "$BUILD_CONTAINER" -- chmod +x /etc/local.d/tunnels.start
lxc exec "$BUILD_CONTAINER" -- rc-update add local default

echo ""
echo "Cleaning up..."
lxc exec "$BUILD_CONTAINER" -- rm -rf /var/cache/apk/*

echo ""
echo "Stopping build container..."
lxc stop "$BUILD_CONTAINER"

echo ""
echo "Publishing image as: $IMAGE_NAME"
lxc publish "$BUILD_CONTAINER" --alias "$IMAGE_NAME" --force

echo ""
echo "Exporting image..."
IMAGE_FILE="${ARTIFACT_DIR}/${CONTAINER_BASE}-v${BUILD_VERSION}-${TIMESTAMP}.tar.gz"
lxc image export "$IMAGE_NAME" "$IMAGE_FILE"

echo ""
echo "Cleaning up build container..."
lxc delete "$BUILD_CONTAINER"

echo ""
echo "Creating version tag..."
lxc image alias create "${CONTAINER_BASE}:${BUILD_VERSION}" "$IMAGE_NAME" 2>/dev/null || \
    lxc image alias delete "${CONTAINER_BASE}:${BUILD_VERSION}" && \
    lxc image alias create "${CONTAINER_BASE}:${BUILD_VERSION}" "$IMAGE_NAME"

echo ""
echo "=========================================="
echo "✅ Build Complete!"
echo "=========================================="
echo "Image: ${CONTAINER_BASE}:${BUILD_VERSION}"
echo "Alias: $IMAGE_NAME"
echo "Artifact: $IMAGE_FILE"
echo "Size: $(du -h "$IMAGE_FILE" | cut -f1)"
echo ""
echo "Generated files in: $GENERATED_DIR"
