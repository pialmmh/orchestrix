#!/bin/bash

# Direct JAR Deployment Script for infinite-scheduler
# Target: BTCL Production Server (114.130.145.71:50004)
# Deployment: btcl-prod

set -e

# ============================================================================
# CONFIGURATION
# ============================================================================

DEPLOYMENT_NAME="btcl-prod"
SERVER_HOST="114.130.145.71"
SERVER_PORT="50004"
SERVER_USER="Administrator"
SERVER_PASSWORD="Takay1#\$ane%%"

APP_NAME="infinite-scheduler"
APP_VERSION="1.0.0"
JAR_FILE="infinite-scheduler-1.0.0.jar"

# Source JAR location
SOURCE_JAR="/home/mustafa/telcobright-projects/routesphere/infinite-scheduler/target/${JAR_FILE}"

# Deployment directory
DEPLOYMENT_DIR="/home/mustafa/telcobright-projects/orchestrix/images/lxc/infinite-scheduler/infinite-scheduler-v1.0.0/deployments/btcl-prod"

# Server paths
INSTALL_PATH="/opt/${APP_NAME}"
APP_DIR="${INSTALL_PATH}/app"
CONFIG_DIR="${INSTALL_PATH}/config"
LOGS_DIR="${INSTALL_PATH}/logs"
BIN_DIR="${INSTALL_PATH}/bin"

# JVM settings
JVM_MIN_HEAP="512m"
JVM_MAX_HEAP="1024m"
JVM_GC="G1GC"
JVM_MAX_GC_PAUSE="200"
DEBUG_PORT="5005"
APP_PORT="7070"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# ============================================================================
# FUNCTIONS
# ============================================================================

print_header() {
    echo -e "${BLUE}═══════════════════════════════════════════════════════════${NC}"
    echo -e "${BLUE}  $1${NC}"
    echo -e "${BLUE}═══════════════════════════════════════════════════════════${NC}"
}

print_info() {
    echo -e "${BLUE}ℹ${NC} $1"
}

print_success() {
    echo -e "${GREEN}✅${NC} $1"
}

print_error() {
    echo -e "${RED}❌${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}⚠${NC} $1"
}

# ============================================================================
# VALIDATIONS
# ============================================================================

print_header "Validating deployment setup"

# Check if source JAR exists
if [ ! -f "$SOURCE_JAR" ]; then
    print_error "Source JAR not found: $SOURCE_JAR"
    exit 1
fi
print_success "Source JAR found: $SOURCE_JAR"

# Check if configuration files exist
if [ ! -f "${DEPLOYMENT_DIR}/quartz.properties" ]; then
    print_error "quartz.properties not found in ${DEPLOYMENT_DIR}"
    exit 1
fi
print_success "Configuration files found"

# Check if sshpass is installed for password authentication
if ! command -v sshpass &> /dev/null; then
    print_warning "sshpass not installed. Installing..."
    sudo apt-get update && sudo apt-get install -y sshpass
fi

# ============================================================================
# SERVER CONNECTION TEST
# ============================================================================

print_header "Testing server connection"

if sshpass -p "${SERVER_PASSWORD}" ssh -o StrictHostKeyChecking=no -o ConnectTimeout=10 -p ${SERVER_PORT} ${SERVER_USER}@${SERVER_HOST} "echo 'Connection successful'" &>/dev/null; then
    print_success "Connected to ${SERVER_HOST}:${SERVER_PORT}"
else
    print_error "Failed to connect to ${SERVER_HOST}:${SERVER_PORT}"
    print_info "Please check server credentials and network connectivity"
    exit 1
fi

# ============================================================================
# CHECK JAVA INSTALLATION
# ============================================================================

print_header "Checking Java installation on server"

JAVA_VERSION_OUTPUT=$(sshpass -p "${SERVER_PASSWORD}" ssh -p ${SERVER_PORT} ${SERVER_USER}@${SERVER_HOST} "java -version 2>&1" || echo "not_found")

if echo "$JAVA_VERSION_OUTPUT" | grep -q "not_found\|command not found"; then
    print_error "Java is not installed on the server"
    print_info "Please install Java 21 on the server first"
    exit 1
else
    print_success "Java is installed on server"
    echo "$JAVA_VERSION_OUTPUT" | head -n 1
fi

# ============================================================================
# CREATE DIRECTORY STRUCTURE
# ============================================================================

print_header "Creating directory structure on server"

sshpass -p "${SERVER_PASSWORD}" ssh -p ${SERVER_PORT} ${SERVER_USER}@${SERVER_HOST} << 'EOF'
mkdir -p /opt/infinite-scheduler/{app,config,logs,bin}
chown -R root:root /opt/infinite-scheduler
chmod -R 755 /opt/infinite-scheduler
EOF

print_success "Directory structure created"

# ============================================================================
# COPY JAR FILE
# ============================================================================

print_header "Copying JAR file to server"

sshpass -p "${SERVER_PASSWORD}" scp -P ${SERVER_PORT} "${SOURCE_JAR}" "${SERVER_USER}@${SERVER_HOST}:${APP_DIR}/"

print_success "JAR file copied to ${APP_DIR}/${JAR_FILE}"

# ============================================================================
# COPY CONFIGURATION FILES
# ============================================================================

print_header "Copying configuration files"

sshpass -p "${SERVER_PASSWORD}" scp -P ${SERVER_PORT} "${DEPLOYMENT_DIR}/quartz.properties" "${SERVER_USER}@${SERVER_HOST}:${CONFIG_DIR}/"
sshpass -p "${SERVER_PASSWORD}" scp -P ${SERVER_PORT} "${DEPLOYMENT_DIR}/kafka.properties" "${SERVER_USER}@${SERVER_HOST}:${CONFIG_DIR}/"

print_success "Configuration files copied (quartz.properties, kafka.properties)"

# ============================================================================
# CREATE STARTUP SCRIPT
# ============================================================================

print_header "Creating startup script"

sshpass -p "${SERVER_PASSWORD}" ssh -p ${SERVER_PORT} ${SERVER_USER}@${SERVER_HOST} << EOF
cat > ${BIN_DIR}/start.sh << 'STARTSCRIPT'
#!/bin/bash

# Infinite Scheduler Startup Script
# Generated by deploy-direct.sh

JAVA_OPTS="-Xms${JVM_MIN_HEAP} \\
  -Xmx${JVM_MAX_HEAP} \\
  -XX:+Use${JVM_GC} \\
  -XX:MaxGCPauseMillis=${JVM_MAX_GC_PAUSE} \\
  -XX:+HeapDumpOnOutOfMemoryError \\
  -XX:HeapDumpPath=${LOGS_DIR}/heap_dump.hprof \\
  -Dquartz.properties=${CONFIG_DIR}/quartz.properties \\
  -Dkafka.properties=${CONFIG_DIR}/kafka.properties \\
  -Dlogback.configurationFile=${CONFIG_DIR}/logback.xml \\
  -Dapp.port=${APP_PORT}"

# Debug mode (commented out by default)
# JAVA_OPTS="\${JAVA_OPTS} -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:${DEBUG_PORT}"

exec java \${JAVA_OPTS} -jar ${APP_DIR}/${JAR_FILE}
STARTSCRIPT

chmod +x ${BIN_DIR}/start.sh
EOF

print_success "Startup script created"

# ============================================================================
# CREATE SYSTEMD SERVICE
# ============================================================================

print_header "Creating systemd service"

sshpass -p "${SERVER_PASSWORD}" ssh -p ${SERVER_PORT} ${SERVER_USER}@${SERVER_HOST} << 'EOF'
cat > /etc/systemd/system/infinite-scheduler.service << 'SERVICEEOF'
[Unit]
Description=Infinite Scheduler - High-Performance Job Scheduler
After=network.target mysql.service
Wants=mysql.service

[Service]
Type=simple
User=root
Group=root
WorkingDirectory=/opt/infinite-scheduler
ExecStart=/opt/infinite-scheduler/bin/start.sh
Restart=always
RestartSec=10
StandardOutput=append:/opt/infinite-scheduler/logs/stdout.log
StandardError=append:/opt/infinite-scheduler/logs/stderr.log

# Resource limits
LimitNOFILE=65536
LimitNPROC=4096

# Security settings
NoNewPrivileges=true
PrivateTmp=true

[Install]
WantedBy=multi-user.target
SERVICEEOF

# Reload systemd
systemctl daemon-reload
EOF

print_success "Systemd service created"

# ============================================================================
# ENABLE AND START SERVICE
# ============================================================================

print_header "Enabling and starting service"

sshpass -p "${SERVER_PASSWORD}" ssh -p ${SERVER_PORT} ${SERVER_USER}@${SERVER_HOST} << 'EOF'
# Enable service
systemctl enable infinite-scheduler.service

# Start service
systemctl start infinite-scheduler.service
EOF

print_success "Service enabled and started"

# ============================================================================
# CHECK SERVICE STATUS
# ============================================================================

print_header "Checking service status"

sleep 3

SERVICE_STATUS=$(sshpass -p "${SERVER_PASSWORD}" ssh -p ${SERVER_PORT} ${SERVER_USER}@${SERVER_HOST} "systemctl is-active infinite-scheduler.service" || echo "failed")

if [ "$SERVICE_STATUS" = "active" ]; then
    print_success "Service is running successfully"
else
    print_error "Service failed to start"
    print_info "Checking logs..."
    sshpass -p "${SERVER_PASSWORD}" ssh -p ${SERVER_PORT} ${SERVER_USER}@${SERVER_HOST} "journalctl -u infinite-scheduler.service -n 50 --no-pager"
    exit 1
fi

# ============================================================================
# DEPLOYMENT SUMMARY
# ============================================================================

print_header "Deployment Summary"

echo ""
echo "Deployment:      ${DEPLOYMENT_NAME}"
echo "Server:          ${SERVER_HOST}:${SERVER_PORT}"
echo "Application:     ${APP_NAME} v${APP_VERSION}"
echo "Install Path:    ${INSTALL_PATH}"
echo "Service:         infinite-scheduler.service"
echo "Status:          Running"
echo ""
echo "Application Port: ${APP_PORT}"
echo "Debug Port:       ${DEBUG_PORT} (disabled by default)"
echo ""
echo "Logs:"
echo "  - Application:  ${LOGS_DIR}/infinite-scheduler.log"
echo "  - Stdout:       ${LOGS_DIR}/stdout.log"
echo "  - Stderr:       ${LOGS_DIR}/stderr.log"
echo "  - Systemd:      journalctl -u infinite-scheduler.service"
echo ""

print_header "Deployment completed successfully!"

echo ""
print_info "Useful commands:"
echo "  - Check status:  ssh -p ${SERVER_PORT} ${SERVER_USER}@${SERVER_HOST} 'systemctl status infinite-scheduler'"
echo "  - View logs:     ssh -p ${SERVER_PORT} ${SERVER_USER}@${SERVER_HOST} 'tail -f ${LOGS_DIR}/infinite-scheduler.log'"
echo "  - Restart:       ssh -p ${SERVER_PORT} ${SERVER_USER}@${SERVER_HOST} 'systemctl restart infinite-scheduler'"
echo "  - Stop:          ssh -p ${SERVER_PORT} ${SERVER_USER}@${SERVER_HOST} 'systemctl stop infinite-scheduler'"
echo ""
