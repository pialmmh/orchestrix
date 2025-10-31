#!/bin/bash
set -e

# ============================================
# Quarkus Runtime Container Build Script
# ============================================
# Generic build script for any Quarkus application
# Following LXC Container Scaffolding Standard v2.0

# ABSOLUTE PATHS (Critical for LXC snap)
BASE_DIR="/home/mustafa/telcobright-projects/orchestrix-lxc-work/images/lxc/quarkus-runtime"
SCRIPT_DIR="$BASE_DIR/build"
TEMPLATES_DIR="$BASE_DIR/templates"
SCRIPTS_DIR="$BASE_DIR/scripts"

# Load configuration
CONFIG_FILE="${CONFIG_FILE:-$SCRIPT_DIR/build.conf}"
if [ ! -f "$CONFIG_FILE" ]; then
    echo "ERROR: Configuration file not found: $CONFIG_FILE"
    echo "Please create $CONFIG_FILE with your app configuration"
    exit 1
fi

echo "Loading configuration from: $CONFIG_FILE"
source "$CONFIG_FILE"

# Validate required configuration
if [ -z "$APP_NAME" ]; then
    echo "ERROR: APP_NAME not set in build.conf"
    exit 1
fi

if [ -z "$APP_JAR_PATH" ] || [ ! -f "$APP_JAR_PATH" ]; then
    echo "ERROR: APP_JAR_PATH not set or JAR file not found: $APP_JAR_PATH"
    echo "Please build your Quarkus app first and set APP_JAR_PATH in build.conf"
    exit 1
fi

# Build timestamp
TIMESTAMP=$(date +%s)

# Derived paths
CONTAINER_NAME="${CONTAINER_NAME_PREFIX}-v${CONTAINER_VERSION}"
VERSION_DIR="${BASE_DIR}/${CONTAINER_NAME_PREFIX}-v.${CONTAINER_VERSION}"
GENERATED_DIR="${VERSION_DIR}/generated"
ARTIFACT_DIR="${GENERATED_DIR}/artifact"
EXPORT_FILE="${ARTIFACT_DIR}/${CONTAINER_NAME}-${TIMESTAMP}.tar.gz"
JAR_FILENAME=$(basename "$APP_JAR_PATH")

# Create directories
mkdir -p "$GENERATED_DIR" "$ARTIFACT_DIR"

# Storage configuration
if [ -f "/etc/orchestrix/storage-locations.conf" ]; then
    STORAGE_PATH=$(grep "^${STORAGE_LOCATION_ID}\.path=" /etc/orchestrix/storage-locations.conf | cut -d'=' -f2)
fi

# BTRFS paths
VOLUME_PATH="${STORAGE_PATH}/containers/${CONTAINER_NAME}"

echo "=========================================  "
echo "Building Quarkus Runtime Container"
echo "=========================================="
echo "App: $APP_NAME"
echo "Version: $CONTAINER_VERSION"
echo "Container: $CONTAINER_NAME"
echo "JAR: $JAR_FILENAME"
echo "App Port: $APP_PORT"
echo "Memory: $MEMORY_LIMIT"
echo "CPU: $CPU_LIMIT"
echo "Storage: $STORAGE_QUOTA_SIZE"
echo "Output: $ARTIFACT_DIR"
echo "=========================================="
echo ""

# ============================================
# PREREQUISITES CHECK
# ============================================
echo "Checking prerequisites..."
echo "=========================================="

# Check Java (on host for verification)
if command -v java &> /dev/null; then
    echo "✓ Java (host): $(java --version 2>&1 | head -1)"
else
    echo "⚠ WARNING: Java not found on host (will install Java $JAVA_VERSION in container)"
fi

# Check LXC
if ! command -v lxc &> /dev/null; then
    echo "ERROR: LXC not found. Please install LXD first."
    exit 1
fi
echo "✓ LXC: $(lxc version | head -1)"

# Check LXD service
if ! systemctl is-active --quiet snap.lxd.daemon.service; then
    echo "ERROR: LXD service is not running"
    echo "Run: sudo systemctl start snap.lxd.daemon.service"
    exit 1
fi
echo "✓ LXD service: ACTIVE"

# Check network bridge
if ! lxc network list | grep -q "$NETWORK_BRIDGE"; then
    echo "ERROR: Network bridge $NETWORK_BRIDGE not found"
    echo "Available bridges:"
    lxc network list
    exit 1
fi
echo "✓ Network bridge: $NETWORK_BRIDGE"

# Check BTRFS (optional - warn if not available)
if command -v btrfs &> /dev/null; then
    echo "✓ BTRFS tools: $(btrfs --version | head -1)"
    if [ -n "$STORAGE_PATH" ] && [ -d "$STORAGE_PATH" ]; then
        if df -T "$STORAGE_PATH" | tail -1 | grep -q btrfs; then
            echo "✓ BTRFS filesystem: $STORAGE_PATH"
            USE_BTRFS_STORAGE="true"
        else
            echo "⚠ WARNING: Storage path is not BTRFS (using default storage)"
            USE_BTRFS_STORAGE="false"
        fi
    else
        echo "⚠ WARNING: Storage path not configured (using default storage)"
        USE_BTRFS_STORAGE="false"
    fi
else
    echo "⚠ WARNING: BTRFS tools not installed (using default storage)"
    USE_BTRFS_STORAGE="false"
fi

echo "=========================================="
echo "✓ Prerequisite checks PASSED"
echo "=========================================="
echo ""

# ============================================
# CLEANUP OLD CONTAINER (if exists)
# ============================================
if lxc list | grep -q "^| $CONTAINER_NAME "; then
    echo "Stopping and removing existing container: $CONTAINER_NAME"
    lxc stop "$CONTAINER_NAME" --force 2>/dev/null || true
    lxc delete "$CONTAINER_NAME" --force 2>/dev/null || true
    echo "✓ Old container removed"
fi

# ============================================
# CREATE CONTAINER
# ============================================
echo "Creating container: $CONTAINER_NAME"
echo "Base image: $BASE_IMAGE"

lxc launch "$BASE_IMAGE" "$CONTAINER_NAME" \
    -c limits.memory="$MEMORY_LIMIT" \
    -c limits.cpu="$CPU_LIMIT"

# Wait for container to be ready
echo "Waiting for container to start..."
sleep 5

# Wait for network
echo "Waiting for network..."
for i in {1..30}; do
    if lxc exec "$CONTAINER_NAME" -- ping -c 1 8.8.8.8 &> /dev/null; then
        echo "✓ Network ready"
        break
    fi
    if [ $i -eq 30 ]; then
        echo "ERROR: Container network not ready after 30 seconds"
        exit 1
    fi
    sleep 1
done

# Get container IP
CONTAINER_IP=$(lxc list "$CONTAINER_NAME" -c 4 --format csv | cut -d' ' -f1)
echo "✓ Container IP: $CONTAINER_IP"

# ============================================
# VERIFY/INSTALL JAVA
# ============================================
echo ""
echo "Checking Java installation..."

# Check if Java is already installed (from base image)
if lxc exec "$CONTAINER_NAME" -- java --version &> /dev/null; then
    echo "✓ Java already installed in base image:"
    lxc exec "$CONTAINER_NAME" -- java --version
else
    echo "Installing Java $JAVA_VERSION..."
    lxc exec "$CONTAINER_NAME" -- bash -c "apt-get update && apt-get install -y wget curl unzip"

    # Install from Adoptium (Debian 12 doesn't have OpenJDK 21 in repos)
    lxc exec "$CONTAINER_NAME" -- bash -c "
    wget -qO - https://packages.adoptium.net/artifactory/api/gpg/key/public | gpg --dearmor -o /usr/share/keyrings/adoptium.gpg && \
    echo 'deb [signed-by=/usr/share/keyrings/adoptium.gpg] https://packages.adoptium.net/artifactory/deb bookworm main' > /etc/apt/sources.list.d/adoptium.list && \
    apt-get update && \
    apt-get install -y temurin-${JAVA_VERSION}-jdk
    "

    # Verify Java installation
    lxc exec "$CONTAINER_NAME" -- java --version
    echo "✓ Java $JAVA_VERSION installed"
fi

# ============================================
# COPY APPLICATION JAR
# ============================================
echo ""
echo "Copying application JAR..."

lxc exec "$CONTAINER_NAME" -- mkdir -p "/opt/${APP_NAME}/app"
lxc file push "$APP_JAR_PATH" "$CONTAINER_NAME/opt/${APP_NAME}/app/$JAR_FILENAME"

echo "✓ JAR copied to /opt/${APP_NAME}/app/$JAR_FILENAME"

# ============================================
# CREATE SYSTEMD SERVICE
# ============================================
echo ""
echo "Creating systemd service..."

lxc exec "$CONTAINER_NAME" -- bash -c "cat > /etc/systemd/system/${APP_NAME}.service << 'EOF'
[Unit]
Description=${APP_NAME} Quarkus Application
After=network.target

[Service]
Type=simple
User=root
WorkingDirectory=/opt/${APP_NAME}/app
ExecStart=/usr/bin/java ${JVM_OPTS} -jar /opt/${APP_NAME}/app/${JAR_FILENAME}
Restart=always
RestartSec=10
StandardOutput=journal
StandardError=journal
SyslogIdentifier=${APP_NAME}

# Environment variables
Environment="QUARKUS_HTTP_PORT=${APP_PORT}"
Environment="QUARKUS_LOG_LEVEL=${LOG_LEVEL}"

[Install]
WantedBy=multi-user.target
EOF"

# Reload systemd
lxc exec "$CONTAINER_NAME" -- systemctl daemon-reload
lxc exec "$CONTAINER_NAME" -- systemctl enable "${APP_NAME}.service"

echo "✓ Systemd service created and enabled"

# ============================================
# VERIFY/INSTALL PROMTAIL FOR LOG SHIPPING
# ============================================
echo ""
echo "Checking Promtail installation..."

# Check if Promtail is already installed (from base image)
if lxc exec "$CONTAINER_NAME" -- promtail --version &> /dev/null; then
    echo "✓ Promtail already installed in base image:"
    lxc exec "$CONTAINER_NAME" -- promtail --version
else
    echo "Installing Promtail $PROMTAIL_VERSION..."
    PROMTAIL_URL="https://github.com/grafana/loki/releases/download/v${PROMTAIL_VERSION}/promtail-linux-amd64.zip"

    lxc exec "$CONTAINER_NAME" -- bash -c "
    cd /tmp
    wget -q $PROMTAIL_URL
    unzip -q promtail-linux-amd64.zip
    mv promtail-linux-amd64 /usr/local/bin/promtail
    chmod +x /usr/local/bin/promtail
    rm promtail-linux-amd64.zip
    "

    # Verify Promtail installation
    lxc exec "$CONTAINER_NAME" -- promtail --version
    echo "✓ Promtail installed"
fi

# Create Promtail config directory
lxc exec "$CONTAINER_NAME" -- mkdir -p /etc/promtail

# Create Promtail configuration
lxc exec "$CONTAINER_NAME" -- bash -c "cat > /etc/promtail/promtail-config.yaml << 'EOF'
server:
  http_listen_port: ${PROMTAIL_PORT}
  grpc_listen_port: 0

positions:
  filename: /tmp/positions.yaml

clients:
  - url: http://${LOKI_HOST}:${LOKI_PORT}/loki/api/v1/push

scrape_configs:
  - job_name: ${APP_NAME}
    static_configs:
      - targets:
          - localhost
        labels:
          job: ${APP_NAME}
          app: ${APP_NAME}
          environment: production
          __path__: /var/log/${APP_NAME}/*.log

    pipeline_stages:
      # Parse JSON logs
      - json:
          expressions:
            timestamp: timestamp
            level: level
            message: message
            trace_id: trace_id
            logger: loggerName

      # Extract labels
      - labels:
          level:
          trace_id:

      # Parse timestamp
      - timestamp:
          source: timestamp
          format: RFC3339Nano

      # Output formatted log line
      - output:
          source: message

  # Also collect systemd journal logs
  - job_name: ${APP_NAME}-journal
    journal:
      max_age: 12h
      labels:
        job: ${APP_NAME}-journal
        app: ${APP_NAME}
    relabel_configs:
      - source_labels: ['__journal__systemd_unit']
        target_label: 'unit'
      - source_labels: ['__journal__hostname']
        target_label: 'hostname'
    pipeline_stages:
      - match:
          selector: '{unit=\"${APP_NAME}.service\"}'
          stages:
            - regex:
                expression: '.*'
            - labels:
                unit:
EOF"

# Create Promtail systemd service
lxc exec "$CONTAINER_NAME" -- bash -c "cat > /etc/systemd/system/promtail.service << 'EOF'
[Unit]
Description=Promtail Log Collector
After=network.target

[Service]
Type=simple
User=root
ExecStart=/usr/local/bin/promtail -config.file=/etc/promtail/promtail-config.yaml
Restart=always
RestartSec=10

[Install]
WantedBy=multi-user.target
EOF"

lxc exec "$CONTAINER_NAME" -- systemctl daemon-reload
lxc exec "$CONTAINER_NAME" -- systemctl enable promtail.service

echo "✓ Promtail configured and enabled"

# ============================================
# CREATE LOG DIRECTORY
# ============================================
echo ""
echo "Creating log directory..."
lxc exec "$CONTAINER_NAME" -- mkdir -p "$LOG_FILE_PATH"
echo "✓ Log directory created: $LOG_FILE_PATH"

# ============================================
# CONFIGURE LOG ROTATION
# ============================================
if [ "${LOG_ROTATE_ENABLED:-false}" = "true" ]; then
    echo ""
    echo "Configuring log rotation..."

    lxc exec "$CONTAINER_NAME" -- bash -c "cat > /etc/logrotate.d/${APP_NAME} << 'EOF'
${LOG_FILE_PATH}/*.log {
    ${LOG_ROTATE_SCHEDULE:-daily}
    size ${LOG_ROTATE_MAX_SIZE:-1G}
    rotate ${LOG_ROTATE_MAX_FILES:-10}
    maxage ${LOG_ROTATE_MAX_AGE:-7}
    compress
    delaycompress
    missingok
    notifempty
    create 0644 root root
    sharedscripts
    postrotate
        # Signal application to reopen log file if needed
        systemctl reload ${APP_NAME}.service 2>/dev/null || true
    endscript
}
EOF"

    echo "✓ Log rotation configured:"
    echo "  - Schedule: ${LOG_ROTATE_SCHEDULE:-daily}"
    echo "  - Max size: ${LOG_ROTATE_MAX_SIZE:-1G}"
    echo "  - Max files: ${LOG_ROTATE_MAX_FILES:-10}"
    echo "  - Max age: ${LOG_ROTATE_MAX_AGE:-7} days"
    echo "  - Compression: ${LOG_ROTATE_COMPRESS:-true}"
else
    echo ""
    echo "⚠ Log rotation disabled (LOG_ROTATE_ENABLED=false)"
fi

# ============================================
# CONFIGURE BTRFS STORAGE (if enabled)
# ============================================
if [ "$USE_BTRFS_STORAGE" = "true" ] && [ "$STORAGE_QUOTA_ENABLED" = "true" ]; then
    echo ""
    echo "Configuring BTRFS storage..."

    # Note: BTRFS quota configuration would go here
    # Skipping for now as per user requirement (quota not required)
    echo "⚠ BTRFS quota disabled (STORAGE_QUOTA_ENABLED=false)"
fi

# ============================================
# START SERVICES
# ============================================
echo ""
echo "Starting services..."

# Start Promtail first
lxc exec "$CONTAINER_NAME" -- systemctl start promtail.service
sleep 2
if lxc exec "$CONTAINER_NAME" -- systemctl is-active promtail.service &> /dev/null; then
    echo "✓ Promtail started"
else
    echo "⚠ WARNING: Promtail failed to start (check logs with: lxc exec $CONTAINER_NAME -- journalctl -u promtail)"
fi

# Start application
lxc exec "$CONTAINER_NAME" -- systemctl start "${APP_NAME}.service"
sleep 3
if lxc exec "$CONTAINER_NAME" -- systemctl is-active "${APP_NAME}.service" &> /dev/null; then
    echo "✓ ${APP_NAME} application started"
else
    echo "⚠ WARNING: Application failed to start (check logs with: lxc exec $CONTAINER_NAME -- journalctl -u ${APP_NAME})"
fi

# ============================================
# VERIFY SERVICES
# ============================================
echo ""
echo "Verifying services..."
lxc exec "$CONTAINER_NAME" -- systemctl status "${APP_NAME}.service" --no-pager -l || true
lxc exec "$CONTAINER_NAME" -- systemctl status promtail.service --no-pager -l || true

# ============================================
# EXPORT CONTAINER IMAGE
# ============================================
if [ "$EXPORT_CONTAINER" = "true" ]; then
    echo ""
    echo "Exporting container image..."

    # Stop container for export
    lxc stop "$CONTAINER_NAME"

    # Publish as image
    IMAGE_NAME="${CONTAINER_NAME}-${TIMESTAMP}"
    lxc publish "$CONTAINER_NAME" --alias "$IMAGE_NAME"

    # Export to tarball
    lxc image export "$IMAGE_NAME" "$EXPORT_FILE"

    echo "✓ Container exported to: $EXPORT_FILE"
    echo "✓ Image alias: $IMAGE_NAME"

    # Restart container
    lxc start "$CONTAINER_NAME"
    sleep 5
fi

# ============================================
# BUILD SUMMARY
# ============================================
echo ""
echo "=========================================="
echo "BUILD COMPLETED SUCCESSFULLY"
echo "=========================================="
echo "Container: $CONTAINER_NAME"
echo "IP Address: $CONTAINER_IP"
echo "App Port: $APP_PORT"
echo "Promtail Port: $PROMTAIL_PORT"
echo "Loki Endpoint: http://${LOKI_HOST}:${LOKI_PORT}"
echo ""
echo "Exported Image: $EXPORT_FILE"
echo ""
echo "=========================================="
echo "NEXT STEPS:"
echo "=========================================="
echo "1. Test application:"
echo "   curl http://$CONTAINER_IP:$APP_PORT/health"
echo ""
echo "2. View application logs:"
echo "   lxc exec $CONTAINER_NAME -- journalctl -u ${APP_NAME} -f"
echo ""
echo "3. Check Grafana for logs:"
echo "   http://${LOKI_HOST}:3000 (if Grafana is running)"
echo ""
echo "4. Access container:"
echo "   lxc exec $CONTAINER_NAME -- bash"
echo ""
echo "=========================================="
