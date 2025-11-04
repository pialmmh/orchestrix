#!/bin/bash

# Direct JAR Deployment Script for infinite-scheduler
# Target: BDCOM SMS Application VM (10.255.246.173)
# Deployment: bdcom-sms

set -e

# ============================================================================
# CONFIGURATION
# ============================================================================

DEPLOYMENT_NAME="bdcom-sms"
SERVER_HOST="10.255.246.173"
SERVER_PORT="15605"
SERVER_USER="bdcom"
SERVER_PASSWORD="M6nthDNrxcYfPQLu"

APP_NAME="infinite-scheduler"
APP_VERSION="1.0.0"
JAR_FILE="infinite-scheduler-1.0.0.jar"

# Source JAR location
SOURCE_JAR="/home/mustafa/telcobright-projects/routesphere/infinite-scheduler/target/${JAR_FILE}"

# Deployment directory
DEPLOYMENT_DIR="/home/mustafa/telcobright-projects/orchestrix/images/lxc/infinite-scheduler/infinite-scheduler-v1.0.0/deployments/bdcom-sms"

# Server paths
INSTALL_PATH="/opt/${APP_NAME}"
APP_DIR="${INSTALL_PATH}/app"
CONFIG_DIR="${INSTALL_PATH}/config"
LOGS_DIR="${INSTALL_PATH}/logs"
BIN_DIR="${INSTALL_PATH}/bin"

# JVM settings (higher memory for BDCOM VM - 16GB RAM)
JVM_MIN_HEAP="512m"
JVM_MAX_HEAP="2048m"
JVM_GC="G1GC"
JVM_MAX_GC_PAUSE="200"
DEBUG_PORT="5005"
APP_PORT="7070"

# Database settings (Percona in LXC container)
DB_HOST="10.10.199.165"
DB_PORT="3306"
DB_NAME="btcl_sms"
DB_USER="root"
DB_PASSWORD="Takay1takaane"

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
if [ ! -f "${DEPLOYMENT_DIR}/kafka.properties" ]; then
    print_error "kafka.properties not found in ${DEPLOYMENT_DIR}"
    exit 1
fi
print_success "Configuration files found"

# Check if sshpass is installed
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
# CHECK DATABASE CONNECTION
# ============================================================================

print_header "Checking database connection"

DB_TEST=$(sshpass -p "${SERVER_PASSWORD}" ssh -p ${SERVER_PORT} ${SERVER_USER}@${SERVER_HOST} \
    "mysql -h ${DB_HOST} -P ${DB_PORT} -u ${DB_USER} -p${DB_PASSWORD} -e 'SELECT 1' 2>&1" || echo "failed")

if echo "$DB_TEST" | grep -q "failed\|ERROR"; then
    print_error "Failed to connect to MySQL on server"
    print_info "Please ensure MySQL is running and credentials are correct"
    exit 1
fi
print_success "MySQL connection successful"

# ============================================================================
# CHECK IF DATABASE EXISTS
# ============================================================================

print_header "Checking if btcl_sms database exists"

DB_EXISTS=$(sshpass -p "${SERVER_PASSWORD}" ssh -p ${SERVER_PORT} ${SERVER_USER}@${SERVER_HOST} \
    "mysql -h ${DB_HOST} -P ${DB_PORT} -u ${DB_USER} -p${DB_PASSWORD} -e 'SHOW DATABASES LIKE \"${DB_NAME}\"' | grep ${DB_NAME}" || echo "")

if [ -z "$DB_EXISTS" ]; then
    print_warning "Database '${DB_NAME}' does not exist"
    print_info "Creating database..."
    sshpass -p "${SERVER_PASSWORD}" ssh -p ${SERVER_PORT} ${SERVER_USER}@${SERVER_HOST} \
        "mysql -h ${DB_HOST} -P ${DB_PORT} -u ${DB_USER} -p${DB_PASSWORD} -e 'CREATE DATABASE ${DB_NAME} CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci'"
    print_success "Database '${DB_NAME}' created"
else
    print_success "Database '${DB_NAME}' exists"
fi

# ============================================================================
# CHECK QUARTZ TABLES
# ============================================================================

print_header "Checking Quartz tables"

QUARTZ_TABLES=$(sshpass -p "${SERVER_PASSWORD}" ssh -p ${SERVER_PORT} ${SERVER_USER}@${SERVER_HOST} \
    "mysql -h ${DB_HOST} -P ${DB_PORT} -u ${DB_USER} -p${DB_PASSWORD} ${DB_NAME} -e 'SHOW TABLES LIKE \"qrtz_%\"' | grep qrtz" || echo "")

if [ -z "$QUARTZ_TABLES" ]; then
    print_warning "Quartz tables not found. Creating schema..."

    # Copy schema file to server
    sshpass -p "${SERVER_PASSWORD}" ssh -p ${SERVER_PORT} ${SERVER_USER}@${SERVER_HOST} "mkdir -p /tmp/scheduler-setup"
    sshpass -p "${SERVER_PASSWORD}" scp -P ${SERVER_PORT} "${DEPLOYMENT_DIR}/../btcl-prod/quartz-mysql-schema.sql" \
        "${SERVER_USER}@${SERVER_HOST}:/tmp/scheduler-setup/"

    # Apply schema
    sshpass -p "${SERVER_PASSWORD}" ssh -p ${SERVER_PORT} ${SERVER_USER}@${SERVER_HOST} \
        "mysql -h ${DB_HOST} -P ${DB_PORT} -u ${DB_USER} -p${DB_PASSWORD} ${DB_NAME} < /tmp/scheduler-setup/quartz-mysql-schema.sql"

    # Cleanup
    sshpass -p "${SERVER_PASSWORD}" ssh -p ${SERVER_PORT} ${SERVER_USER}@${SERVER_HOST} "rm -rf /tmp/scheduler-setup"

    print_success "Quartz schema created"
else
    print_success "Quartz tables already exist"
fi

# ============================================================================
# CHECK KAFKA
# ============================================================================

print_header "Checking Kafka availability"

KAFKA_CHECK=$(sshpass -p "${SERVER_PASSWORD}" ssh -p ${SERVER_PORT} ${SERVER_USER}@${SERVER_HOST} \
    "netstat -tulpn 2>/dev/null | grep :9092 || ss -tulpn | grep :9092" || echo "not_running")

if echo "$KAFKA_CHECK" | grep -q "not_running"; then
    print_warning "Kafka might not be running on port 9092"
    print_info "Please ensure Kafka is running before starting the scheduler"
else
    print_success "Kafka appears to be running on port 9092"
fi

# ============================================================================
# CREATE DIRECTORY STRUCTURE
# ============================================================================

print_header "Creating directory structure on server"

sshpass -p "${SERVER_PASSWORD}" ssh -p ${SERVER_PORT} ${SERVER_USER}@${SERVER_HOST} << 'EOF'
sudo mkdir -p /opt/infinite-scheduler/{app,config,logs,bin}
sudo chown -R $USER:$USER /opt/infinite-scheduler
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
  -Ddb.host=10.10.199.165 \\
  -Ddb.port=3306 \\
  -Ddb.name=btcl_sms \\
  -Ddb.user=root \\
  -Ddb.password=Takay1takaane \\
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
sudo bash -c 'cat > /etc/systemd/system/infinite-scheduler.service << "SERVICEEOF"
[Unit]
Description=Infinite Scheduler - High-Performance Job Scheduler
After=network.target mysql.service
Wants=mysql.service

[Service]
Type=simple
User=bdcom
Group=bdcom
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
'

# Reload systemd
sudo systemctl daemon-reload
EOF

print_success "Systemd service created"

# ============================================================================
# ENABLE AND START SERVICE
# ============================================================================

print_header "Enabling and starting service"

sshpass -p "${SERVER_PASSWORD}" ssh -p ${SERVER_PORT} ${SERVER_USER}@${SERVER_HOST} << 'EOF'
# Enable service
sudo systemctl enable infinite-scheduler.service

# Start service
sudo systemctl start infinite-scheduler.service
EOF

print_success "Service enabled and started"

# ============================================================================
# CHECK SERVICE STATUS
# ============================================================================

print_header "Checking service status"

sleep 3

SERVICE_STATUS=$(sshpass -p "${SERVER_PASSWORD}" ssh -p ${SERVER_PORT} ${SERVER_USER}@${SERVER_HOST} "sudo systemctl is-active infinite-scheduler.service" || echo "failed")

if [ "$SERVICE_STATUS" = "active" ]; then
    print_success "Service is running successfully"
else
    print_error "Service failed to start"
    print_info "Checking logs..."
    sshpass -p "${SERVER_PASSWORD}" ssh -p ${SERVER_PORT} ${SERVER_USER}@${SERVER_HOST} "sudo journalctl -u infinite-scheduler.service -n 50 --no-pager"
    exit 1
fi

# ============================================================================
# DEPLOYMENT SUMMARY
# ============================================================================

print_header "Deployment Summary"

echo ""
echo "Deployment:      ${DEPLOYMENT_NAME}"
echo "Server:          ${SERVER_HOST}"
echo "User:            ${SERVER_USER}"
echo "Application:     ${APP_NAME} v${APP_VERSION}"
echo "Install Path:    ${INSTALL_PATH}"
echo "Service:         infinite-scheduler.service"
echo "Status:          Running"
echo ""
echo "Application Port: ${APP_PORT}"
echo "Debug Port:       ${DEBUG_PORT} (disabled by default)"
echo ""
echo "Database:        ${DB_NAME}@${DB_HOST}:${DB_PORT}"
echo "Kafka:           localhost:9092"
echo ""
echo "JVM Settings:"
echo "  Min Heap:      ${JVM_MIN_HEAP}"
echo "  Max Heap:      ${JVM_MAX_HEAP}"
echo "  GC:            ${JVM_GC}"
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
echo "  - Check status:  ssh -p ${SERVER_PORT} ${SERVER_USER}@${SERVER_HOST} 'sudo systemctl status infinite-scheduler'"
echo "  - View logs:     ssh -p ${SERVER_PORT} ${SERVER_USER}@${SERVER_HOST} 'tail -f ${LOGS_DIR}/stdout.log'"
echo "  - Restart:       ssh -p ${SERVER_PORT} ${SERVER_USER}@${SERVER_HOST} 'sudo systemctl restart infinite-scheduler'"
echo "  - Stop:          ssh -p ${SERVER_PORT} ${SERVER_USER}@${SERVER_HOST} 'sudo systemctl stop infinite-scheduler'"
echo ""
