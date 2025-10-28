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

echo "Starting SSH tunnels..."
echo "======================"

# Function to start a tunnel
start_tunnel() {
    local name="${tunnel[name]}"
    local ssh_addr="${tunnel[sshAddress]}"
    local ssh_user="${tunnel[sshUsername]}"
    local ssh_pass="${tunnel[sshPassword]}"
    local ssh_key="${tunnel[sshKeyFile]}"
    local ssh_port="${tunnel[sshPort]:-22}"
    local local_port="${tunnel[localPort]}"
    local remote_host="${tunnel[remoteHost]:-localhost}"
    local remote_port="${tunnel[remotePort]}"

    # Validate required fields
    if [ -z "$ssh_addr" ] || [ -z "$ssh_user" ] || [ -z "$local_port" ] || [ -z "$remote_port" ]; then
        echo "  Error: Missing required fields for tunnel '\''$name'\''"
        return 1
    fi

    echo ""
    echo "Starting tunnel: $name"
    echo "  SSH: $ssh_user@$ssh_addr:$ssh_port"
    echo "  Forward: 0.0.0.0:$local_port -> $remote_host:$remote_port"

    # Build SSH command
    local ssh_opts="-f -N -p $ssh_port"
    ssh_opts="$ssh_opts -o StrictHostKeyChecking=no"
    ssh_opts="$ssh_opts -o UserKnownHostsFile=/dev/null"
    ssh_opts="$ssh_opts -o ServerAliveInterval=30"
    ssh_opts="$ssh_opts -o ServerAliveCountMax=3"
    ssh_opts="$ssh_opts -o LogLevel=ERROR"
    ssh_opts="$ssh_opts -L 0.0.0.0:${local_port}:${remote_host}:${remote_port}"

    # Start tunnel with appropriate auth
    if [ -n "$ssh_key" ]; then
        # Key authentication
        ssh $ssh_opts -i "$ssh_key" "${ssh_user}@${ssh_addr}"
    elif [ -n "$ssh_pass" ]; then
        # Password authentication
        sshpass -p "$ssh_pass" ssh $ssh_opts "${ssh_user}@${ssh_addr}"
    else
        echo "  Error: No authentication method specified (need sshPassword or sshKeyFile)"
        return 1
    fi

    echo "  ✓ Started"
}

# Parse INI file and start tunnels
current_section=""
declare -A tunnel

while IFS= read -r line || [ -n "$line" ]; do
    # Skip comments and empty lines
    [[ "$line" =~ ^[[:space:]]*# ]] && continue
    [[ -z "${line// }" ]] && continue

    # Check for section header [name]
    if [[ "$line" =~ ^\[(.*)\]$ ]]; then
        # If we have a previous tunnel, start it
        if [ -n "$current_section" ]; then
            start_tunnel
        fi

        # Start new section
        current_section="${BASH_REMATCH[1]}"
        unset tunnel
        declare -A tunnel
        tunnel[name]="$current_section"
        continue
    fi

    # Parse key = value
    if [[ "$line" =~ ^[[:space:]]*([^=]+)[[:space:]]*=[[:space:]]*(.*)$ ]]; then
        key="${BASH_REMATCH[1]// }"
        value="${BASH_REMATCH[2]}"
        # Remove trailing comments
        value="${value%% #*}"
        # Trim whitespace
        value="${value## }"
        value="${value%% }"
        tunnel[$key]="$value"
    fi
done < /etc/tunnel-gateway/tunnels.conf

# Start the last tunnel
if [ -n "$current_section" ]; then
    start_tunnel
fi

echo ""
echo "All tunnels started"
echo "Active SSH processes:"
ps aux | grep -E "ssh.*ServerAlive" | grep -v grep | grep -v "start-tunnels"
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
