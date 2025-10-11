#!/bin/bash
#
# Build script for Grafana-Prometheus-Loki Container
# Follows Orchestrix Container Scaffolding Standard v2.0
#
# This script builds a Full-Stack Debian container with:
# - Grafana 10.2.3
# - Prometheus 2.48.0
# - Loki 2.9.4
# - Promtail 2.9.4
# - BTRFS storage with quota management
# - Automatic log rotation and storage monitoring
#

set -e

# ============================================
# ABSOLUTE PATHS (Critical for LXC snap)
# ============================================
BASE_DIR="/home/mustafa/telcobright-projects/orchestrix/images/lxc/grafana-prometheus-loki"
SCRIPT_DIR="$BASE_DIR/build"
TEMPLATES_DIR="$BASE_DIR/templates"
SCRIPTS_DIR="$BASE_DIR/scripts"
ORCHESTRIX_HOME="/home/mustafa/telcobright-projects/orchestrix"

# Load configuration
CONFIG_FILE="${1:-${SCRIPT_DIR}/build.conf}"

if [ ! -f "$CONFIG_FILE" ]; then
    echo "ERROR: Configuration file not found: $CONFIG_FILE"
    echo "Usage: $0 [config-file]"
    exit 1
fi

echo "Loading configuration from: $CONFIG_FILE"
source "$CONFIG_FILE"

# Validate required parameters
if [ -z "$STORAGE_LOCATION_ID" ]; then
    echo "ERROR: STORAGE_LOCATION_ID is required in build.conf"
    echo "Please edit $CONFIG_FILE and set STORAGE_LOCATION_ID"
    exit 1
fi

if [ -z "$STORAGE_QUOTA_SIZE" ]; then
    echo "ERROR: STORAGE_QUOTA_SIZE is required in build.conf"
    echo "Please edit $CONFIG_FILE and set STORAGE_QUOTA_SIZE"
    exit 1
fi

# Build metadata with absolute paths
BUILD_DATE=$(date +%Y%m%d_%H%M%S)
TIMESTAMP=$(date +%s)
# Container name without dots (LXC requirement)
CONTAINER_NAME="${CONTAINER_NAME_PREFIX}-v${CONTAINER_VERSION}"
# Directory name can have dots for organization
VERSION_DIR="${BASE_DIR}/${CONTAINER_NAME_PREFIX}-v.${CONTAINER_VERSION}"
GENERATED_DIR="${VERSION_DIR}/generated"
ARTIFACT_DIR="${GENERATED_DIR}/artifact"
EXPORT_FILE="${ARTIFACT_DIR}/${CONTAINER_NAME_PREFIX}-v${CONTAINER_VERSION}-${TIMESTAMP}.tar.gz"

# Create directory structure
mkdir -p "$ARTIFACT_DIR"

echo "========================================="
echo "Building Grafana-Prometheus-Loki Container"
echo "========================================="
echo "Version: ${CONTAINER_VERSION}"
echo "Container: ${CONTAINER_NAME}"
echo "Grafana: ${GRAFANA_VERSION}"
echo "Prometheus: ${PROMETHEUS_VERSION}"
echo "Loki: ${LOKI_VERSION}"
echo "Storage: ${STORAGE_LOCATION_ID} (${STORAGE_QUOTA_SIZE})"
echo "Output: ${ARTIFACT_DIR}"
echo "========================================="
echo ""

# ============================================
# PREREQUISITE CHECKS (Mandatory)
# ============================================
echo "Checking prerequisites..."
echo "=========================================="

# Check BTRFS
if ! command -v btrfs &> /dev/null; then
    echo "ERROR: BTRFS tools not installed"
    echo "Install with: sudo apt-get install btrfs-progs"
    exit 1
fi
echo "✓ BTRFS tools: $(which btrfs)"
btrfs --version | head -1

# Check BTRFS kernel module
if ! lsmod | grep -q btrfs; then
    echo "WARNING: BTRFS kernel module not loaded"
    echo "Loading module..."
    sudo modprobe btrfs || echo "  Could not load BTRFS module"
else
    echo "✓ BTRFS module: LOADED"
fi

# Check LXC/LXD
if ! command -v lxc &> /dev/null; then
    echo "ERROR: LXC not installed"
    echo "Install with: snap install lxd"
    exit 1
fi
echo "✓ LXC: $(which lxc)"
lxc version | head -1

# Check LXD service
if ! systemctl is-active --quiet snap.lxd.daemon; then
    echo "ERROR: LXD service not active"
    echo "Start with: sudo systemctl start snap.lxd.daemon"
    exit 1
fi
echo "✓ LXD service: ACTIVE"

# Check network bridge
if ! lxc network list | grep -q lxdbr0; then
    echo "ERROR: Network bridge 'lxdbr0' not found"
    echo "Initialize LXD with: lxd init"
    exit 1
fi
echo "✓ Network bridge: lxdbr0"

# Check storage availability
STORAGE_PATH=$(grep "^${STORAGE_LOCATION_ID}.path=" /etc/orchestrix/storage-locations.conf 2>/dev/null | cut -d'=' -f2 || echo "/home/telcobright/btrfs")
if [ ! -d "$STORAGE_PATH" ]; then
    echo "WARNING: Storage path not found: $STORAGE_PATH"
    echo "  Using default LXC storage"
    USE_BTRFS_STORAGE="false"
else
    echo "✓ Storage path: $STORAGE_PATH"
    df -h "$STORAGE_PATH" | tail -1

    # Check if path is actually on a BTRFS filesystem
    if df -T "$STORAGE_PATH" | tail -1 | grep -q btrfs; then
        echo "✓ BTRFS filesystem detected"
        USE_BTRFS_STORAGE="true"
    else
        echo "⚠ WARNING: Path exists but is NOT on a BTRFS filesystem"
        echo "  Detected filesystem: $(df -T "$STORAGE_PATH" | tail -1 | awk '{print $2}')"
        echo "  Using default LXC storage instead"
        echo ""
        echo "  To use BTRFS storage:"
        echo "    1. Create a BTRFS filesystem"
        echo "    2. Mount it at $STORAGE_PATH"
        echo "    3. Update /etc/orchestrix/storage-locations.conf"
        echo ""
        USE_BTRFS_STORAGE="false"
    fi
fi

echo "=========================================="
echo "✓ All prerequisite checks PASSED"
echo "=========================================="
echo ""

# ============================================
# BTRFS STORAGE SETUP (if available)
# ============================================
if [ "$USE_BTRFS_STORAGE" = "true" ]; then
    echo "Setting up BTRFS storage..."

    # Use container name without dots for BTRFS subvolume
    VOLUME_PATH="${STORAGE_PATH}/containers/${CONTAINER_NAME}"

    # Create BTRFS subvolume if it doesn't exist
    if [ ! -d "$VOLUME_PATH" ]; then
        echo "Creating BTRFS subvolume: $VOLUME_PATH"
        sudo btrfs subvolume create "$VOLUME_PATH"
    else
        echo "BTRFS subvolume exists: $VOLUME_PATH"
    fi

    # Set quota
    echo "Setting quota: $STORAGE_QUOTA_SIZE"
    # Enable quota on filesystem first
    sudo btrfs quota enable "$STORAGE_PATH" 2>/dev/null || true

    # Get subvolume ID
    SUBVOL_ID=$(sudo btrfs subvolume show "$VOLUME_PATH" | grep "Subvolume ID" | awk '{print $3}')

    # Parse size to bytes
    QUOTA_BYTES=$(echo "$STORAGE_QUOTA_SIZE" | sed 's/G/*1024*1024*1024/' | sed 's/M/*1024*1024/' | bc)

    # Set quota limit
    sudo btrfs qgroup limit "${QUOTA_BYTES}" "0/${SUBVOL_ID}" "$STORAGE_PATH" 2>/dev/null || \
        echo "  Note: Quota may already be set"

    # Enable compression if configured
    if [ "$STORAGE_COMPRESSION" = "true" ]; then
        echo "Enabling compression..."
        sudo btrfs property set "$VOLUME_PATH" compression zstd
    fi

    echo "✓ BTRFS storage configured"
    echo ""
fi

# ============================================
# CLEAN EXISTING CONTAINER
# ============================================
if [ "$CLEAN_BUILD" = "true" ]; then
    if lxc info "$CONTAINER_NAME" &>/dev/null; then
        echo "Removing existing container: $CONTAINER_NAME"
        lxc stop "$CONTAINER_NAME" --force 2>/dev/null || true
        lxc delete "$CONTAINER_NAME" --force
        echo "✓ Old container removed"
        echo ""
    fi
fi

# ============================================
# CREATE CONTAINER
# ============================================
echo "Creating container from base image: $BASE_IMAGE"
lxc init "$BASE_IMAGE" "$CONTAINER_NAME"
echo "✓ Container created"
echo ""

# ============================================
# CONFIGURE RESOURCES
# ============================================
echo "Configuring resources..."
lxc config set "$CONTAINER_NAME" limits.memory "$MEMORY_LIMIT"
lxc config set "$CONTAINER_NAME" limits.cpu "$CPU_LIMIT"
echo "✓ Memory: $MEMORY_LIMIT, CPU: $CPU_LIMIT"
echo ""

# ============================================
# CONFIGURE NETWORK
# ============================================
echo "Configuring network..."
lxc config device add "$CONTAINER_NAME" eth0 nic name=eth0 nictype=bridged parent="$NETWORK_BRIDGE" 2>/dev/null || \
    echo "  Network device may already exist"
echo "✓ Network configured (bridge: $NETWORK_BRIDGE)"
echo ""

# ============================================
# START CONTAINER
# ============================================
echo "Starting container..."
lxc start "$CONTAINER_NAME"
echo "Waiting for container to initialize..."
sleep 10

# Wait for network connectivity
echo "Waiting for network..."
for i in {1..30}; do
    if lxc exec "$CONTAINER_NAME" -- ping -c 1 8.8.8.8 &>/dev/null; then
        echo "✓ Network ready"
        break
    fi
    sleep 2
done
echo ""

# ============================================
# INSTALL PACKAGES
# ============================================
echo "Installing system packages..."
lxc exec "$CONTAINER_NAME" -- apt-get update
lxc exec "$CONTAINER_NAME" -- apt-get upgrade -y
lxc exec "$CONTAINER_NAME" -- apt-get install -y $PACKAGES_CORE
lxc exec "$CONTAINER_NAME" -- apt-get install -y $PACKAGES_SYSTEM
echo "✓ System packages installed"
echo ""

# ============================================
# INSTALL GRAFANA
# ============================================
echo "Installing Grafana ${GRAFANA_VERSION}..."
lxc exec "$CONTAINER_NAME" -- bash -c "wget -q -O /usr/share/keyrings/grafana.key ${GRAFANA_GPG_KEY}"
lxc exec "$CONTAINER_NAME" -- bash -c "echo 'deb [signed-by=/usr/share/keyrings/grafana.key] ${GRAFANA_APT_REPO} stable main' > /etc/apt/sources.list.d/grafana.list"
lxc exec "$CONTAINER_NAME" -- apt-get update
lxc exec "$CONTAINER_NAME" -- apt-get install -y grafana

# Configure Grafana datasources
echo "Configuring Grafana datasources..."
lxc exec "$CONTAINER_NAME" -- mkdir -p /etc/grafana/provisioning/datasources
lxc exec "$CONTAINER_NAME" -- mkdir -p /etc/grafana/provisioning/dashboards

# Create datasources provisioning file
lxc exec "$CONTAINER_NAME" -- bash -c "cat > /etc/grafana/provisioning/datasources/datasources.yaml << 'DSEOF'
apiVersion: 1

datasources:
  - name: Prometheus
    type: prometheus
    access: proxy
    url: http://localhost:${PROMETHEUS_PORT}
    isDefault: true
    editable: true
    jsonData:
      timeInterval: 15s

  - name: Loki
    type: loki
    access: proxy
    url: http://localhost:${LOKI_PORT}
    editable: true
    jsonData:
      maxLines: 1000
DSEOF"

# Create dashboard provisioning file
lxc exec "$CONTAINER_NAME" -- bash -c "cat > /etc/grafana/provisioning/dashboards/dashboards.yaml << 'DBEOF'
apiVersion: 1

providers:
  - name: 'default'
    orgId: 1
    folder: ''
    type: file
    disableDeletion: false
    updateIntervalSeconds: 10
    allowUiUpdates: true
    options:
      path: /etc/grafana/provisioning/dashboards
DBEOF"

# Copy OmniQueue dashboard
lxc file push "${SCRIPTS_DIR}/omniqueue-dashboard.json" "$CONTAINER_NAME/etc/grafana/provisioning/dashboards/"

echo "✓ Grafana installed and configured"
echo ""

# ============================================
# INSTALL LOKI
# ============================================
echo "Installing Loki ${LOKI_VERSION}..."
lxc exec "$CONTAINER_NAME" -- mkdir -p "$LOKI_DATA_DIR" "$LOKI_CONFIG_DIR" "$LOKI_CHUNKS_DIR" "$LOKI_RULES_DIR"
lxc exec "$CONTAINER_NAME" -- bash -c "wget -q -O /tmp/loki.zip ${LOKI_DOWNLOAD_URL}"
lxc exec "$CONTAINER_NAME" -- bash -c "cd /tmp && unzip -q loki.zip && mv loki-linux-amd64 /usr/local/bin/loki && chmod +x /usr/local/bin/loki && rm loki.zip"

# Create Loki config with OmniQueue distributed tracing support
echo "Deploying enhanced Loki config (OmniQueue-ready)..."
# Use envsubst to replace variables in config template
cat "${SCRIPTS_DIR}/loki-config.yaml" | \
  sed "s|\${LOKI_DATA_DIR}|${LOKI_DATA_DIR}|g" | \
  sed "s|\${LOKI_CHUNKS_DIR}|${LOKI_CHUNKS_DIR}|g" | \
  sed "s|\${LOKI_INGESTION_RATE_MB}|${LOKI_INGESTION_RATE_MB}|g" | \
  sed "s|\${LOKI_INGESTION_BURST_MB}|${LOKI_INGESTION_BURST_MB}|g" | \
  sed "s|\${LOKI_RETENTION_PERIOD}|${LOKI_RETENTION_PERIOD}|g" | \
  sed "s|\${LOKI_RETENTION_DELETE_DELAY}|${LOKI_RETENTION_DELETE_DELAY}|g" > /tmp/loki-config-processed.yaml

lxc file push /tmp/loki-config-processed.yaml "$CONTAINER_NAME${LOKI_CONFIG_DIR}/loki-config.yaml"
rm /tmp/loki-config-processed.yaml
echo "✓ Loki config deployed with distributed tracing support"

# Create Loki systemd service
lxc exec "$CONTAINER_NAME" -- bash -c "cat > /etc/systemd/system/loki.service << 'SVCEOF'
[Unit]
Description=Loki Log Aggregation System
After=network.target

[Service]
Type=simple
User=root
ExecStart=/usr/local/bin/loki -config.file=${LOKI_CONFIG_DIR}/loki-config.yaml
Restart=always
RestartSec=10s

[Install]
WantedBy=multi-user.target
SVCEOF"

lxc exec "$CONTAINER_NAME" -- systemctl daemon-reload
echo "✓ Loki installed"
echo ""

# ============================================
# INSTALL PROMETHEUS
# ============================================
echo "Installing Prometheus ${PROMETHEUS_VERSION}..."
lxc exec "$CONTAINER_NAME" -- mkdir -p "$PROMETHEUS_DATA_DIR" "$PROMETHEUS_CONFIG_DIR"
lxc exec "$CONTAINER_NAME" -- bash -c "wget -q -O /tmp/prometheus.tar.gz ${PROMETHEUS_DOWNLOAD_URL}"
lxc exec "$CONTAINER_NAME" -- bash -c "cd /tmp && tar xzf prometheus.tar.gz && cd prometheus-${PROMETHEUS_VERSION}.linux-amd64 && mv prometheus promtool /usr/local/bin/ && mv consoles console_libraries /etc/prometheus/ && rm -rf /tmp/prometheus*"

# Create Prometheus config
echo "Deploying Prometheus config..."
cat "${SCRIPTS_DIR}/prometheus.yml" | \
  sed "s|\${PROMETHEUS_SCRAPE_INTERVAL}|${PROMETHEUS_SCRAPE_INTERVAL}|g" | \
  sed "s|\${PROMETHEUS_EVALUATION_INTERVAL}|${PROMETHEUS_EVALUATION_INTERVAL}|g" | \
  sed "s|\${PROMETHEUS_PORT}|${PROMETHEUS_PORT}|g" | \
  sed "s|\${GRAFANA_PORT}|${GRAFANA_PORT}|g" | \
  sed "s|\${LOKI_PORT}|${LOKI_PORT}|g" > /tmp/prometheus-config-processed.yml

lxc file push /tmp/prometheus-config-processed.yml "$CONTAINER_NAME${PROMETHEUS_CONFIG_DIR}/prometheus.yml"
rm /tmp/prometheus-config-processed.yml
echo "✓ Prometheus config deployed"

# Create Prometheus systemd service
lxc exec "$CONTAINER_NAME" -- bash -c "cat > /etc/systemd/system/prometheus.service << 'SVCEOF'
[Unit]
Description=Prometheus Monitoring System
After=network.target

[Service]
Type=simple
User=root
ExecStart=/usr/local/bin/prometheus \
  --config.file=${PROMETHEUS_CONFIG_DIR}/prometheus.yml \
  --storage.tsdb.path=${PROMETHEUS_DATA_DIR} \
  --storage.tsdb.retention.time=${PROMETHEUS_RETENTION_TIME} \
  --storage.tsdb.retention.size=${PROMETHEUS_RETENTION_SIZE} \
  --web.listen-address=0.0.0.0:${PROMETHEUS_PORT} \
  --web.enable-lifecycle
Restart=always
RestartSec=10s

[Install]
WantedBy=multi-user.target
SVCEOF"

lxc exec "$CONTAINER_NAME" -- systemctl daemon-reload
echo "✓ Prometheus installed"
echo ""

# ============================================
# INSTALL PROMTAIL
# ============================================
echo "Installing Promtail ${PROMTAIL_VERSION}..."
lxc exec "$CONTAINER_NAME" -- bash -c "wget -q -O /tmp/promtail.zip ${PROMTAIL_DOWNLOAD_URL}"
lxc exec "$CONTAINER_NAME" -- bash -c "cd /tmp && unzip -q promtail.zip && mv promtail-linux-amd64 /usr/local/bin/promtail && chmod +x /usr/local/bin/promtail && rm promtail.zip"

# Create Promtail config
lxc exec "$CONTAINER_NAME" -- bash -c "cat > ${PROMTAIL_CONFIG_DIR}/promtail-config.yaml << 'PROMEOF'
server:
  http_listen_port: ${PROMTAIL_PORT}
  grpc_listen_port: 0

positions:
  filename: ${PROMTAIL_POSITIONS_FILE}

clients:
  - url: http://localhost:3100/loki/api/v1/push

scrape_configs:
  - job_name: system
    static_configs:
      - targets:
          - localhost
        labels:
          job: varlogs
          __path__: /var/log/*log
PROMEOF"

# Create Promtail systemd service
lxc exec "$CONTAINER_NAME" -- bash -c "cat > /etc/systemd/system/promtail.service << 'PSVCEOF'
[Unit]
Description=Promtail Log Collector
After=network.target loki.service

[Service]
Type=simple
User=root
ExecStart=/usr/local/bin/promtail -config.file=${PROMTAIL_CONFIG_DIR}/promtail-config.yaml
Restart=always
RestartSec=10s

[Install]
WantedBy=multi-user.target
PSVCEOF"

lxc exec "$CONTAINER_NAME" -- systemctl daemon-reload
echo "✓ Promtail installed"
echo ""

# ============================================
# CONFIGURE SERVICES
# ============================================
echo "Configuring services..."

# Enable services
lxc exec "$CONTAINER_NAME" -- systemctl enable grafana-server
lxc exec "$CONTAINER_NAME" -- systemctl enable prometheus
lxc exec "$CONTAINER_NAME" -- systemctl enable loki
lxc exec "$CONTAINER_NAME" -- systemctl enable promtail

# Start services if requested
if [ "$START_SERVICES" = "true" ]; then
    echo "Starting services..."
    lxc exec "$CONTAINER_NAME" -- systemctl start grafana-server
    sleep 5
    lxc exec "$CONTAINER_NAME" -- systemctl start prometheus
    sleep 3
    lxc exec "$CONTAINER_NAME" -- systemctl start loki
    sleep 3
    lxc exec "$CONTAINER_NAME" -- systemctl start promtail

    # Import OmniQueue dashboard via Grafana API
    echo "Importing OmniQueue dashboard..."
    lxc exec "$CONTAINER_NAME" -- curl -s -X POST -H "Content-Type: application/json" \
      -d @/etc/grafana/provisioning/dashboards/omniqueue-dashboard.json \
      http://admin:admin@localhost:3000/api/dashboards/db > /dev/null
    echo "✓ OmniQueue dashboard imported"

    echo "✓ Services started"
fi
echo ""

# ============================================
# INSTALL STORAGE MONITORING
# ============================================
if [ "$STORAGE_MONITOR_ENABLED" = "true" ] && [ -f "$SCRIPTS_DIR/storage-monitor.sh" ]; then
    echo "Installing storage monitor..."
    lxc file push "$SCRIPTS_DIR/storage-monitor.sh" "$CONTAINER_NAME/usr/local/bin/storage-monitor.sh"
    lxc exec "$CONTAINER_NAME" -- chmod +x /usr/local/bin/storage-monitor.sh

    # Create systemd service for storage monitor
    lxc exec "$CONTAINER_NAME" -- bash -c "cat > /etc/systemd/system/storage-monitor.service << 'MONEOF'
[Unit]
Description=Storage Usage Monitor
After=network.target

[Service]
Type=simple
ExecStart=/usr/local/bin/storage-monitor.sh
Restart=always
RestartSec=${STORAGE_CHECK_INTERVAL}

[Install]
WantedBy=multi-user.target
MONEOF"

    lxc exec "$CONTAINER_NAME" -- systemctl daemon-reload
    lxc exec "$CONTAINER_NAME" -- systemctl enable storage-monitor
    echo "✓ Storage monitor installed"
    echo ""
fi

# ============================================
# EXPORT CONTAINER
# ============================================
if [ "$EXPORT_CONTAINER" = "true" ]; then
    echo "Exporting container..."

    # Stop container for export
    lxc stop "$CONTAINER_NAME"

    # Export to artifact directory with absolute path
    # Note: lxc export does NOT add .tar.gz automatically, so use full filename
    lxc export "$CONTAINER_NAME" "$EXPORT_FILE"

    # Create MD5 checksum
    cd "$ARTIFACT_DIR"
    md5sum "$(basename $EXPORT_FILE)" > "$(basename $EXPORT_FILE).md5"
    cd - > /dev/null

    IMAGE_SIZE=$(du -h "$EXPORT_FILE" | cut -f1)

    echo "✓ Container exported"
    echo "  File: $EXPORT_FILE"
    echo "  Size: $IMAGE_SIZE"
    echo ""
fi

# ============================================
# COPY TEMPLATES TO GENERATED
# ============================================
echo "Copying templates to generated directory..."
cp "$TEMPLATES_DIR/sample.conf" "$GENERATED_DIR/"
cp "$TEMPLATES_DIR/startDefault.sh" "$GENERATED_DIR/"
chmod +x "$GENERATED_DIR/startDefault.sh"
echo "✓ Templates copied"
echo ""

# ============================================
# CREATE SNAPSHOT (if BTRFS enabled)
# ============================================
if [ "$CREATE_SNAPSHOT" = "true" ] && [ "$USE_BTRFS_STORAGE" = "true" ]; then
    echo "Creating BTRFS snapshot..."
    SNAPSHOT_DIR="${STORAGE_PATH}/snapshots/${CONTAINER_NAME}"
    SNAPSHOT_NAME="${SNAPSHOT_DIR}/build-${TIMESTAMP}"

    sudo mkdir -p "$SNAPSHOT_DIR"
    sudo btrfs subvolume snapshot -r "$VOLUME_PATH" "$SNAPSHOT_NAME" 2>/dev/null || \
        echo "  Note: Snapshot may already exist or BTRFS not available"

    echo "✓ Snapshot created: $SNAPSHOT_NAME"
    echo ""
fi

# ============================================
# BUILD COMPLETE
# ============================================
echo ""
echo "========================================="
echo "✓ Build Complete!"
echo "========================================="
echo "Container: $CONTAINER_NAME"
echo "Version: $CONTAINER_VERSION"
echo "Image: $EXPORT_FILE"
echo "Size: ${IMAGE_SIZE:-N/A}"
echo "Generated: $GENERATED_DIR"
if [ "$USE_BTRFS_STORAGE" = "true" ]; then
    echo "Storage: $VOLUME_PATH (quota: $STORAGE_QUOTA_SIZE)"
fi
echo ""
echo "Quick start:"
echo "  cd $GENERATED_DIR"
echo "  ./startDefault.sh"
echo ""
echo "Or launch manually:"
echo "  lxc image import $EXPORT_FILE --alias grafana-prometheus-loki-v${CONTAINER_VERSION}"
echo "  lxc launch grafana-prometheus-loki-v${CONTAINER_VERSION} my-grafana-prometheus-loki"
echo "========================================="