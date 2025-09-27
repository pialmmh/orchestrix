#!/bin/bash
# Build script for Grafana-Loki container
# Follows Orchestrix container scaffolding standards

set -e

# Check if we should use Java automation
if [ "$USE_JAVA_AUTOMATION" = "true" ] || [ "$1" = "--java" ]; then
    echo "Using Java automation for build..."
    exec /home/mustafa/telcobright-projects/orchestrix/scripts/build-grafana-loki-java.sh "$@"
    exit $?
fi

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
CONTAINER_DIR="$(dirname "$SCRIPT_DIR")"
SCRIPTS_DIR="${CONTAINER_DIR}/scripts"

# Load configuration
CONFIG_FILE="${1:-${SCRIPT_DIR}/build.conf}"
if [ ! -f "$CONFIG_FILE" ]; then
    echo "Error: Configuration file not found: $CONFIG_FILE"
    echo "Usage: $0 [config-file]"
    exit 1
fi

echo "Loading configuration from: $CONFIG_FILE"
source "$CONFIG_FILE"

# Validate required parameters
if [ -z "$STORAGE_LOCATION_ID" ]; then
    echo "Error: STORAGE_LOCATION_ID is required in configuration"
    echo "Please edit build.conf and set STORAGE_LOCATION_ID"
    exit 1
fi

if [ -z "$STORAGE_QUOTA_SIZE" ]; then
    echo "Error: STORAGE_QUOTA_SIZE is required in configuration"
    echo "Please edit build.conf and set STORAGE_QUOTA_SIZE"
    exit 1
fi

# Build metadata
BUILD_DATE=$(date +%Y%m%d_%H%M%S)
CONTAINER_NAME="${CONTAINER_NAME_PREFIX}-v.${CONTAINER_VERSION}"
EXPORT_FILE="${EXPORT_PATH}/${CONTAINER_NAME}-${BUILD_DATE}.tar.gz"
GENERATED_DIR="${CONTAINER_DIR}/${CONTAINER_NAME}/generated"

echo "========================================="
echo "Building Grafana-Loki Container"
echo "========================================="
echo "Version: ${CONTAINER_VERSION}"
echo "Container: ${CONTAINER_NAME}"
echo "Grafana: ${GRAFANA_VERSION}"
echo "Loki: ${LOKI_VERSION}"
echo "Storage: ${STORAGE_LOCATION_ID} (${STORAGE_QUOTA_SIZE})"
echo "========================================="

# Helper functions
source "${SCRIPTS_DIR}/helper-functions.sh" 2>/dev/null || true

# Function to get storage path
get_storage_path() {
    local location_id=$1
    if [ -f "/etc/orchestrix/storage-locations.conf" ]; then
        grep "^${location_id}.path=" /etc/orchestrix/storage-locations.conf | cut -d'=' -f2
    else
        echo "/home/telcobright/btrfs"
    fi
}

# Function to parse size to bytes
parse_size() {
    local size=$1
    local num=$(echo $size | sed 's/[^0-9.]//g')
    local unit=$(echo $size | sed 's/[0-9.]//g' | tr '[:lower:]' '[:upper:]')

    case $unit in
        G|GB) echo $(( ${num%.*} * 1024 * 1024 * 1024 )) ;;
        M|MB) echo $(( ${num%.*} * 1024 * 1024 )) ;;
        T|TB) echo $(( ${num%.*} * 1024 * 1024 * 1024 * 1024 )) ;;
        *) echo ${num%.*} ;;
    esac
}

# Clean existing container if requested
if [ "$CLEAN_BUILD" = "true" ] && lxc info $CONTAINER_NAME &>/dev/null; then
    echo "Removing existing container..."
    lxc stop $CONTAINER_NAME --force 2>/dev/null || true
    lxc delete $CONTAINER_NAME --force
fi

# Setup BTRFS storage
echo "Setting up BTRFS storage..."
STORAGE_PATH=$(get_storage_path "$STORAGE_LOCATION_ID")
VOLUME_PATH="${STORAGE_PATH}/containers/${STORAGE_CONTAINER_ROOT}-v${CONTAINER_VERSION}"

# Ensure BTRFS tools are installed
if ! command -v btrfs &> /dev/null; then
    echo "Installing BTRFS tools..."
    sudo apt-get update && sudo apt-get install -y btrfs-progs
fi

# Create storage volume
sudo mkdir -p "${STORAGE_PATH}/containers"
if ! sudo btrfs subvolume show "$VOLUME_PATH" &> /dev/null; then
    echo "Creating BTRFS subvolume..."
    sudo btrfs subvolume create "$VOLUME_PATH"

    # Set quota
    echo "Setting quota to ${STORAGE_QUOTA_SIZE}..."
    local mount_point=$(df "$STORAGE_PATH" | tail -1 | awk '{print $6}')
    sudo btrfs quota enable "$mount_point" 2>/dev/null || true

    local subvol_id=$(sudo btrfs subvolume show "$VOLUME_PATH" | grep 'Subvolume ID:' | awk '{print $3}')
    local quota_bytes=$(parse_size "$STORAGE_QUOTA_SIZE")
    sudo btrfs qgroup limit $quota_bytes 0/$subvol_id "$mount_point"

    # Enable compression
    if [ "$STORAGE_COMPRESSION" = "true" ]; then
        sudo btrfs property set "$VOLUME_PATH" compression lzo
    fi
fi

# Create container
echo "Creating container ${CONTAINER_NAME}..."
lxc init ${BASE_IMAGE} ${CONTAINER_NAME}

# Configure storage
echo "Configuring storage..."
lxc config device add ${CONTAINER_NAME} root disk source=${VOLUME_PATH} path=/

# Configure resources
echo "Configuring resources..."
lxc config set ${CONTAINER_NAME} limits.memory ${MEMORY_LIMIT}
lxc config set ${CONTAINER_NAME} limits.cpu ${CPU_LIMIT}

# Configure network
echo "Configuring network..."
lxc config device add ${CONTAINER_NAME} eth0 nic name=eth0 nictype=bridged parent=${NETWORK_BRIDGE}

# Start container
echo "Starting container..."
lxc start ${CONTAINER_NAME}

# Wait for container
echo "Waiting for container to be ready..."
sleep 5
while ! lxc exec ${CONTAINER_NAME} -- ping -c 1 google.com &>/dev/null; do
    echo "Waiting for network..."
    sleep 2
done

# Update system
echo "Updating system packages..."
lxc exec ${CONTAINER_NAME} -- apt-get update
lxc exec ${CONTAINER_NAME} -- apt-get upgrade -y

# Install core packages
echo "Installing core packages..."
lxc exec ${CONTAINER_NAME} -- apt-get install -y ${PACKAGES_CORE}

# Install system packages
echo "Installing system packages..."
lxc exec ${CONTAINER_NAME} -- apt-get install -y ${PACKAGES_SYSTEM}

# Configure SSH if enabled
if [ "$ENABLE_SSH" = "true" ]; then
    echo "Configuring SSH..."
    lxc exec ${CONTAINER_NAME} -- bash -c "echo 'PermitRootLogin ${SSH_PERMIT_ROOT}' >> /etc/ssh/sshd_config"
    lxc exec ${CONTAINER_NAME} -- bash -c "echo 'PasswordAuthentication ${SSH_PASSWORD_AUTH}' >> /etc/ssh/sshd_config"

    if [ "$SSH_AUTO_ACCEPT" = "true" ]; then
        lxc exec ${CONTAINER_NAME} -- bash -c "echo 'StrictHostKeyChecking no' >> /etc/ssh/ssh_config"
        lxc exec ${CONTAINER_NAME} -- bash -c "echo 'UserKnownHostsFile /dev/null' >> /etc/ssh/ssh_config"
    fi

    lxc exec ${CONTAINER_NAME} -- systemctl restart sshd
fi

# Install Grafana
echo "Installing Grafana ${GRAFANA_VERSION}..."
lxc exec ${CONTAINER_NAME} -- wget -q -O /usr/share/keyrings/grafana.key ${GRAFANA_GPG_KEY}
lxc exec ${CONTAINER_NAME} -- bash -c "echo 'deb [signed-by=/usr/share/keyrings/grafana.key] ${GRAFANA_APT_REPO} stable main' > /etc/apt/sources.list.d/grafana.list"
lxc exec ${CONTAINER_NAME} -- apt-get update
lxc exec ${CONTAINER_NAME} -- apt-get install -y grafana

# Install Loki
echo "Installing Loki ${LOKI_VERSION}..."
lxc exec ${CONTAINER_NAME} -- mkdir -p /opt/loki ${LOKI_CONFIG_DIR} ${LOKI_DATA_DIR}
lxc exec ${CONTAINER_NAME} -- wget -q -O /opt/loki/loki.gz "${LOKI_DOWNLOAD_URL}"
lxc exec ${CONTAINER_NAME} -- bash -c "cd /opt/loki && gunzip -c loki.gz > loki && chmod +x loki"

# Install Promtail
echo "Installing Promtail ${PROMTAIL_VERSION}..."
lxc exec ${CONTAINER_NAME} -- wget -q -O /opt/loki/promtail.gz "${PROMTAIL_DOWNLOAD_URL}"
lxc exec ${CONTAINER_NAME} -- bash -c "cd /opt/loki && gunzip -c promtail.gz > promtail && chmod +x promtail"

# Configure log rotation
if [ "$LOG_ROTATION_ENABLED" = "true" ]; then
    echo "Configuring log rotation..."
    lxc file push ${SCRIPTS_DIR}/configure-log-rotation.sh ${CONTAINER_NAME}/tmp/
    lxc exec ${CONTAINER_NAME} -- chmod +x /tmp/configure-log-rotation.sh
    lxc exec ${CONTAINER_NAME} -- /tmp/configure-log-rotation.sh "$LOKI_RETENTION_PERIOD" "$LOKI_RETENTION_DELETE_DELAY"
fi

# Configure storage monitor
if [ "$STORAGE_MONITOR_ENABLED" = "true" ]; then
    echo "Installing storage monitor..."
    lxc file push ${SCRIPTS_DIR}/storage-monitor.sh ${CONTAINER_NAME}/usr/local/bin/
    lxc exec ${CONTAINER_NAME} -- chmod +x /usr/local/bin/storage-monitor.sh

    # Create systemd service
    cat << EOF | lxc exec ${CONTAINER_NAME} -- tee /etc/systemd/system/storage-monitor.service
[Unit]
Description=Storage Monitor for Automatic Log Rotation
After=loki.service

[Service]
Type=simple
ExecStart=/usr/local/bin/storage-monitor.sh monitor
Restart=always
Environment="STORAGE_THRESHOLD=${STORAGE_ROTATION_THRESHOLD}"
Environment="CHECK_INTERVAL=${STORAGE_CHECK_INTERVAL}"
Environment="FORCE_CLEANUP_THRESHOLD=${STORAGE_FORCE_CLEANUP_THRESHOLD}"

[Install]
WantedBy=multi-user.target
EOF

    lxc exec ${CONTAINER_NAME} -- systemctl enable storage-monitor
fi

# Create Loki systemd service
cat << 'EOF' | lxc exec ${CONTAINER_NAME} -- tee /etc/systemd/system/loki.service
[Unit]
Description=Loki Log Aggregation System
After=network.target

[Service]
Type=simple
User=root
ExecStart=/opt/loki/loki -config.file=/etc/loki/config.yaml
Restart=always

[Install]
WantedBy=multi-user.target
EOF

# Create Promtail systemd service
cat << 'EOF' | lxc exec ${CONTAINER_NAME} -- tee /etc/systemd/system/promtail.service
[Unit]
Description=Promtail Log Collector
After=network.target

[Service]
Type=simple
User=root
ExecStart=/opt/loki/promtail -config.file=/etc/loki/promtail.yaml
Restart=always

[Install]
WantedBy=multi-user.target
EOF

# Configure Grafana datasource
echo "Configuring Grafana datasource..."
cat << 'EOF' | lxc exec ${CONTAINER_NAME} -- tee /etc/grafana/provisioning/datasources/loki.yaml
apiVersion: 1

datasources:
  - name: Loki
    type: loki
    access: proxy
    url: http://localhost:3100
    isDefault: true
    editable: true
EOF

# Enable and start services
if [ "$START_SERVICES" = "true" ]; then
    echo "Starting services..."
    lxc exec ${CONTAINER_NAME} -- systemctl daemon-reload
    lxc exec ${CONTAINER_NAME} -- systemctl enable grafana-server loki promtail
    lxc exec ${CONTAINER_NAME} -- systemctl start grafana-server loki promtail

    if [ "$STORAGE_MONITOR_ENABLED" = "true" ]; then
        lxc exec ${CONTAINER_NAME} -- systemctl start storage-monitor
    fi
fi

# Configure port forwarding
echo "Configuring port forwarding..."
lxc config device add ${CONTAINER_NAME} grafana-port proxy \
    listen=tcp:0.0.0.0:${GRAFANA_PORT} connect=tcp:127.0.0.1:3000
lxc config device add ${CONTAINER_NAME} loki-port proxy \
    listen=tcp:0.0.0.0:${LOKI_PORT} connect=tcp:127.0.0.1:3100
lxc config device add ${CONTAINER_NAME} promtail-port proxy \
    listen=tcp:0.0.0.0:${PROMTAIL_PORT} connect=tcp:127.0.0.1:9080

# Create version info
echo "Creating version info..."
cat << EOF | lxc exec ${CONTAINER_NAME} -- tee /etc/grafana-loki-version
Container Version: ${CONTAINER_VERSION}
Build Date: ${BUILD_DATE}
Grafana: ${GRAFANA_VERSION}
Loki: ${LOKI_VERSION}
Promtail: ${PROMTAIL_VERSION}
Storage: ${STORAGE_LOCATION_ID} (${STORAGE_QUOTA_SIZE})
Rotation Threshold: ${STORAGE_ROTATION_THRESHOLD}%
EOF

# Stop container for export
echo "Stopping container for export..."
lxc stop ${CONTAINER_NAME}

# Export container
if [ "$EXPORT_CONTAINER" = "true" ]; then
    echo "Exporting container to ${EXPORT_FILE}..."
    lxc export ${CONTAINER_NAME} ${EXPORT_FILE}
fi

# Create snapshot
if [ "$CREATE_SNAPSHOT" = "true" ]; then
    echo "Creating BTRFS snapshot..."
    SNAPSHOT_PATH="${STORAGE_PATH}/snapshots/${STORAGE_CONTAINER_ROOT}/v${CONTAINER_VERSION}_${BUILD_DATE}"
    sudo mkdir -p "$(dirname "$SNAPSHOT_PATH")"
    sudo btrfs subvolume snapshot -r "$VOLUME_PATH" "$SNAPSHOT_PATH"
fi

# Generate launch script and sample config
echo "Generating launch scripts..."
mkdir -p "${GENERATED_DIR}"

# Generate launch.sh
cat << 'LAUNCH_EOF' > "${GENERATED_DIR}/launch.sh"
#!/bin/bash
# Auto-generated launch script for Grafana-Loki container

CONFIG_FILE="${1:-./sample.conf}"

if [ ! -f "$CONFIG_FILE" ]; then
    echo "Error: Config file not found: $CONFIG_FILE"
    echo "Usage: $0 [config-file]"
    exit 1
fi

source "$CONFIG_FILE"

# Validate required parameters
if [ -z "$CONTAINER_NAME" ] || [ -z "$IMAGE_PATH" ] || [ -z "$STORAGE_LOCATION_ID" ]; then
    echo "Error: Missing required parameters in config"
    exit 1
fi

# Import and configure container
echo "Importing container..."
lxc import "$IMAGE_PATH" "$CONTAINER_NAME"

echo "Starting container..."
lxc start "$CONTAINER_NAME"

echo "Container ${CONTAINER_NAME} started successfully"
echo "Grafana: http://localhost:${GRAFANA_PORT:-3000}"
echo "Loki: http://localhost:${LOKI_PORT:-3100}"
LAUNCH_EOF

chmod +x "${GENERATED_DIR}/launch.sh"

# Generate sample.conf
cat << EOF > "${GENERATED_DIR}/sample.conf"
# Auto-generated sample configuration
# Generated on: ${BUILD_DATE}

CONTAINER_NAME="${CONTAINER_NAME_PREFIX}-prod"
IMAGE_PATH="${EXPORT_FILE}"
CONTAINER_VERSION="${CONTAINER_VERSION}"

# Storage
STORAGE_LOCATION_ID="${STORAGE_LOCATION_ID}"
STORAGE_QUOTA_SIZE="${STORAGE_QUOTA_SIZE}"

# Ports
GRAFANA_PORT="${GRAFANA_PORT}"
LOKI_PORT="${LOKI_PORT}"
PROMTAIL_PORT="${PROMTAIL_PORT}"

# Resources
MEMORY_LIMIT="${MEMORY_LIMIT}"
CPU_LIMIT="${CPU_LIMIT}"
EOF

# Generate README
cat << EOF > "${GENERATED_DIR}/README-v.${CONTAINER_VERSION}.md"
# Grafana-Loki Container v.${CONTAINER_VERSION}

Built on: ${BUILD_DATE}

## Components
- Grafana: ${GRAFANA_VERSION}
- Loki: ${LOKI_VERSION}
- Promtail: ${PROMTAIL_VERSION}

## Storage
- Location: ${STORAGE_LOCATION_ID}
- Quota: ${STORAGE_QUOTA_SIZE}
- Auto-rotation: ${STORAGE_ROTATION_THRESHOLD}%

## Access
- Grafana: http://localhost:${GRAFANA_PORT} (admin/admin)
- Loki API: http://localhost:${LOKI_PORT}
- Promtail: http://localhost:${PROMTAIL_PORT}

## Files
- Export: ${EXPORT_FILE}
- Volume: ${VOLUME_PATH}
- Snapshot: ${SNAPSHOT_PATH}
EOF

# Final summary
echo "========================================="
echo "Build Completed Successfully!"
echo "========================================="
echo "Container: ${CONTAINER_NAME}"
echo "Export: ${EXPORT_FILE}"
echo "Generated files: ${GENERATED_DIR}"
echo ""
echo "To launch: ${GENERATED_DIR}/launch.sh"
echo "To start: lxc start ${CONTAINER_NAME}"
echo "========================================="