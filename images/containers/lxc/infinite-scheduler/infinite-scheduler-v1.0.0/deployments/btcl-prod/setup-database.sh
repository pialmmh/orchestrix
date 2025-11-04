#!/bin/bash

# Database Setup Script for infinite-scheduler
# Target: BTCL Production Server (114.130.145.71:50004)
# Deployment: btcl-prod

set -e

# ============================================================================
# CONFIGURATION
# ============================================================================

SERVER_HOST="114.130.145.71"
SERVER_PORT="50004"
SERVER_USER="Administrator"
SERVER_PASSWORD="Takay1#\$ane%%"

DB_HOST="127.0.0.1"
DB_PORT="3306"
DB_NAME="scheduler"
DB_USER="root"
DB_PASSWORD="123456"

DEPLOYMENT_DIR="/home/mustafa/telcobright-projects/orchestrix/images/lxc/infinite-scheduler/infinite-scheduler-v1.0.0/deployments/btcl-prod"
SCHEMA_FILE="quartz-mysql-schema.sql"

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

print_header "Database Setup for Infinite Scheduler"

# Check if schema file exists
if [ ! -f "${DEPLOYMENT_DIR}/${SCHEMA_FILE}" ]; then
    print_error "Schema file not found: ${DEPLOYMENT_DIR}/${SCHEMA_FILE}"
    exit 1
fi
print_success "Schema file found: ${SCHEMA_FILE}"

# Check if sshpass is installed
if ! command -v sshpass &> /dev/null; then
    print_warning "sshpass not installed. Installing..."
    sudo apt-get update && sudo apt-get install -y sshpass
fi

# ============================================================================
# TEST SERVER CONNECTION
# ============================================================================

print_header "Testing server connection"

if sshpass -p "${SERVER_PASSWORD}" ssh -o StrictHostKeyChecking=no -o ConnectTimeout=10 -p ${SERVER_PORT} ${SERVER_USER}@${SERVER_HOST} "echo 'Connection successful'" &>/dev/null; then
    print_success "Connected to ${SERVER_HOST}:${SERVER_PORT}"
else
    print_error "Failed to connect to ${SERVER_HOST}:${SERVER_PORT}"
    exit 1
fi

# ============================================================================
# TEST DATABASE CONNECTION
# ============================================================================

print_header "Testing database connection"

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

print_header "Checking if scheduler database exists"

DB_EXISTS=$(sshpass -p "${SERVER_PASSWORD}" ssh -p ${SERVER_PORT} ${SERVER_USER}@${SERVER_HOST} \
    "mysql -h ${DB_HOST} -P ${DB_PORT} -u ${DB_USER} -p${DB_PASSWORD} -e 'SHOW DATABASES LIKE \"${DB_NAME}\"' | grep ${DB_NAME}" || echo "")

if [ -n "$DB_EXISTS" ]; then
    print_warning "Database '${DB_NAME}' already exists"
    echo -n "Do you want to drop and recreate it? (yes/no): "
    read -r CONFIRM
    if [ "$CONFIRM" = "yes" ]; then
        print_info "Dropping existing database..."
        sshpass -p "${SERVER_PASSWORD}" ssh -p ${SERVER_PORT} ${SERVER_USER}@${SERVER_HOST} \
            "mysql -h ${DB_HOST} -P ${DB_PORT} -u ${DB_USER} -p${DB_PASSWORD} -e 'DROP DATABASE ${DB_NAME}'"
        print_success "Database dropped"
    else
        print_info "Skipping database creation"
        echo -n "Do you want to apply schema to existing database? (yes/no): "
        read -r APPLY_SCHEMA
        if [ "$APPLY_SCHEMA" != "yes" ]; then
            print_info "Skipping schema application. Exiting."
            exit 0
        fi
    fi
fi

# ============================================================================
# CREATE DATABASE
# ============================================================================

if [ -z "$DB_EXISTS" ] || [ "$CONFIRM" = "yes" ]; then
    print_header "Creating scheduler database"

    sshpass -p "${SERVER_PASSWORD}" ssh -p ${SERVER_PORT} ${SERVER_USER}@${SERVER_HOST} \
        "mysql -h ${DB_HOST} -P ${DB_PORT} -u ${DB_USER} -p${DB_PASSWORD} -e 'CREATE DATABASE ${DB_NAME} CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci'"

    print_success "Database '${DB_NAME}' created"
fi

# ============================================================================
# COPY SCHEMA FILE TO SERVER
# ============================================================================

print_header "Copying schema file to server"

sshpass -p "${SERVER_PASSWORD}" ssh -p ${SERVER_PORT} ${SERVER_USER}@${SERVER_HOST} "mkdir -p /tmp/scheduler-setup"

sshpass -p "${SERVER_PASSWORD}" scp -P ${SERVER_PORT} "${DEPLOYMENT_DIR}/${SCHEMA_FILE}" \
    "${SERVER_USER}@${SERVER_HOST}:/tmp/scheduler-setup/"

print_success "Schema file copied to server"

# ============================================================================
# APPLY SCHEMA
# ============================================================================

print_header "Applying Quartz schema"

sshpass -p "${SERVER_PASSWORD}" ssh -p ${SERVER_PORT} ${SERVER_USER}@${SERVER_HOST} \
    "mysql -h ${DB_HOST} -P ${DB_PORT} -u ${DB_USER} -p${DB_PASSWORD} ${DB_NAME} < /tmp/scheduler-setup/${SCHEMA_FILE}"

print_success "Quartz schema applied successfully"

# ============================================================================
# VERIFY TABLES
# ============================================================================

print_header "Verifying Quartz tables"

TABLES=$(sshpass -p "${SERVER_PASSWORD}" ssh -p ${SERVER_PORT} ${SERVER_USER}@${SERVER_HOST} \
    "mysql -h ${DB_HOST} -P ${DB_PORT} -u ${DB_USER} -p${DB_PASSWORD} ${DB_NAME} -e 'SHOW TABLES' | grep QRTZ" || echo "")

if [ -z "$TABLES" ]; then
    print_error "No Quartz tables found in database"
    exit 1
fi

echo ""
print_info "Quartz tables created:"
echo "$TABLES"
echo ""

TABLE_COUNT=$(echo "$TABLES" | wc -l)
print_success "Successfully created ${TABLE_COUNT} Quartz tables"

# ============================================================================
# CLEANUP
# ============================================================================

print_header "Cleaning up"

sshpass -p "${SERVER_PASSWORD}" ssh -p ${SERVER_PORT} ${SERVER_USER}@${SERVER_HOST} \
    "rm -rf /tmp/scheduler-setup"

print_success "Cleanup completed"

# ============================================================================
# SUMMARY
# ============================================================================

print_header "Database Setup Summary"

echo ""
echo "Server:        ${SERVER_HOST}:${SERVER_PORT}"
echo "Database:      ${DB_NAME}"
echo "Host:          ${DB_HOST}:${DB_PORT}"
echo "User:          ${DB_USER}"
echo "Tables:        ${TABLE_COUNT} Quartz tables created"
echo ""

print_header "Database setup completed successfully!"

echo ""
print_info "Next steps:"
echo "  1. Review the deployment configuration in README.md"
echo "  2. Run the deployment script: ./deploy-direct.sh"
echo ""
