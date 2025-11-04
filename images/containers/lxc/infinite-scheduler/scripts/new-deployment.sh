#!/bin/bash

# new-deployment.sh - Creates new deployment configuration
# Located in: <app>-v<version>/deployments/

set -e

# Color codes
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
NC='\033[0m'

print_info() { echo -e "${BLUE}ℹ${NC} $1"; }
print_success() { echo -e "${GREEN}✅${NC} $1"; }
print_warning() { echo -e "${YELLOW}⚠${NC} $1"; }
print_error() { echo -e "${RED}❌${NC} $1"; }
print_question() { echo -e "${CYAN}?${NC} $1"; }

# Get current directory (should be deployments/)
DEPLOYMENTS_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
VERSION_DIR="$(dirname "$DEPLOYMENTS_DIR")"
APP_DIR="$(dirname "$VERSION_DIR")"

# Extract version and app name from path
VERSION_FOLDER=$(basename "$VERSION_DIR")
APP_NAME=$(echo "$VERSION_FOLDER" | sed 's/-v.*//')
APP_VERSION=$(echo "$VERSION_FOLDER" | sed 's/.*-v//')

# Find Java project path from build.conf
BUILD_CONF="${APP_DIR}/build/build.conf"
if [ ! -f "$BUILD_CONF" ]; then
    print_error "build.conf not found: $BUILD_CONF"
    exit 1
fi

source "$BUILD_CONF"

echo ""
echo "╔══════════════════════════════════════════════════════════════════╗"
echo "║                                                                  ║"
echo "║          Create New Deployment Configuration                     ║"
echo "║                                                                  ║"
echo "╚══════════════════════════════════════════════════════════════════╝"
echo ""
print_info "Application: ${APP_NAME} v${APP_VERSION}"
echo ""

# Interactive prompts
read -p "$(print_question 'Deployment name (e.g., link3-prod): ')" DEPLOYMENT_NAME
if [ -z "$DEPLOYMENT_NAME" ]; then
    print_error "Deployment name is required"
    exit 1
fi

DEPLOYMENT_DIR="${DEPLOYMENTS_DIR}/${DEPLOYMENT_NAME}"

if [ -d "$DEPLOYMENT_DIR" ]; then
    print_error "Deployment already exists: $DEPLOYMENT_NAME"
    exit 1
fi

# Ask if copying from previous version
COPY_FROM_PREV=""
read -p "$(print_question 'Copy from previous version deployment? [y/N]: ')" COPY_FROM_PREV

if [[ "$COPY_FROM_PREV" =~ ^[Yy]$ ]]; then
    # Find previous versions
    PREV_VERSIONS=$(find "$APP_DIR" -maxdepth 1 -type d -name "${APP_NAME}-v*" ! -name "${VERSION_FOLDER}" | sort -V)

    if [ -n "$PREV_VERSIONS" ]; then
        echo ""
        print_info "Available previous versions:"
        select prev_dir in $PREV_VERSIONS "Cancel"; do
            if [ "$prev_dir" = "Cancel" ]; then
                COPY_FROM_PREV="n"
                break
            elif [ -n "$prev_dir" ]; then
                PREV_VERSION_DIR="$prev_dir"

                # List deployments in previous version
                PREV_DEPLOYMENTS=$(find "${PREV_VERSION_DIR}/deployments" -maxdepth 1 -type d ! -path "${PREV_VERSION_DIR}/deployments" 2>/dev/null | xargs -n1 basename)

                if [ -n "$PREV_DEPLOYMENTS" ]; then
                    echo ""
                    print_info "Available deployments in $(basename $prev_dir):"
                    select prev_deploy in $PREV_DEPLOYMENTS "Cancel"; do
                        if [ "$prev_deploy" = "Cancel" ]; then
                            COPY_FROM_PREV="n"
                            break 2
                        elif [ -n "$prev_deploy" ]; then
                            PREV_DEPLOYMENT_DIR="${PREV_VERSION_DIR}/deployments/${prev_deploy}"
                            break 2
                        fi
                    done
                else
                    print_warning "No deployments found in previous version"
                    COPY_FROM_PREV="n"
                    break
                fi
            fi
        done
    else
        print_warning "No previous versions found"
        COPY_FROM_PREV="n"
    fi
fi

# Create deployment directory
mkdir -p "$DEPLOYMENT_DIR"

# If copying from previous
if [ -n "$PREV_DEPLOYMENT_DIR" ] && [ -d "$PREV_DEPLOYMENT_DIR" ]; then
    print_info "Copying configuration from: $(basename $(dirname $(dirname $PREV_DEPLOYMENT_DIR)))/deployments/$(basename $PREV_DEPLOYMENT_DIR)"

    # Copy files
    if [ -f "${PREV_DEPLOYMENT_DIR}/deployment.yaml" ]; then
        cp "${PREV_DEPLOYMENT_DIR}/deployment.yaml" "${DEPLOYMENT_DIR}/"
        # Update version in YAML
        sed -i "s/version:.*/version: ${APP_VERSION}/" "${DEPLOYMENT_DIR}/deployment.yaml"
    fi

    if [ -f "${PREV_DEPLOYMENT_DIR}/application.properties" ]; then
        cp "${PREV_DEPLOYMENT_DIR}/application.properties" "${DEPLOYMENT_DIR}/"
    fi

    print_success "Configuration copied and updated for v${APP_VERSION}"
    echo ""
    print_warning "Please review and update the configuration for version ${APP_VERSION}"
else
    # Fresh deployment - ask questions
    read -p "$(print_question 'Remote SSH host (empty for local): ')" REMOTE_HOST
    read -p "$(print_question 'Remote SSH user [ubuntu]: ')" REMOTE_USER
    REMOTE_USER=${REMOTE_USER:-ubuntu}

    read -p "$(print_question 'Container name ['${APP_NAME}-${DEPLOYMENT_NAME}']: ')" CONTAINER_NAME
    CONTAINER_NAME=${CONTAINER_NAME:-${APP_NAME}-${DEPLOYMENT_NAME}}

    read -p "$(print_question 'Container IP (with CIDR, e.g., 10.10.199.27/24) [DHCP]: ')" CONTAINER_IP

    if [ -n "$CONTAINER_IP" ]; then
        read -p "$(print_question 'Gateway [10.10.199.1]: ')" GATEWAY
        GATEWAY=${GATEWAY:-10.10.199.1}
    fi

    read -p "$(print_question 'CPU cores [2]: ')" CPU_CORES
    CPU_CORES=${CPU_CORES:-2}

    read -p "$(print_question 'Memory [2GB]: ')" MEMORY
    MEMORY=${MEMORY:-2GB}

    # Create deployment.yaml
    cat > "${DEPLOYMENT_DIR}/deployment.yaml" << EOF
# Deployment configuration for ${DEPLOYMENT_NAME}
# Application: ${APP_NAME} v${APP_VERSION}

name: ${APP_NAME}-${DEPLOYMENT_NAME}
version: ${APP_VERSION}

# Remote server configuration
remote:
  host: ${REMOTE_HOST}
  user: ${REMOTE_USER}
  use_ssh_agent: true

# Container configuration
container:
  name: ${CONTAINER_NAME}

  network:
    ip: ${CONTAINER_IP}
    gateway: ${GATEWAY}
    bridge: lxdbr0

  resources:
    cpu: ${CPU_CORES}
    memory: ${MEMORY}

# Post-deployment commands
post_deploy:
  - systemctl enable ${APP_NAME}.service
  - systemctl start ${APP_NAME}.service
  - sleep 5
  - systemctl status ${APP_NAME}.service

EOF

    # Copy application.properties from Java project
    JAVA_APP_PROPS="${JAVA_PROJECT_PATH}/src/main/resources/application.properties"

    if [ -f "$JAVA_APP_PROPS" ]; then
        print_info "Copying application.properties from Java project..."
        cp "$JAVA_APP_PROPS" "${DEPLOYMENT_DIR}/application.properties"

        # Add header
        sed -i '1i# Application properties for '${DEPLOYMENT_NAME}'\n# Customize below for this deployment\n' "${DEPLOYMENT_DIR}/application.properties"

        print_success "application.properties copied from: $JAVA_APP_PROPS"
    else
        print_warning "application.properties not found in Java project, creating template..."

        cat > "${DEPLOYMENT_DIR}/application.properties" << EOF
# Application properties for ${DEPLOYMENT_NAME}
# Customize below for this deployment

# HTTP Configuration
quarkus.http.port=7070
quarkus.http.host=0.0.0.0

# Database Configuration
quarkus.datasource.db-kind=mysql
quarkus.datasource.jdbc.url=jdbc:mysql://127.0.0.1:3306/${DB_NAME}
quarkus.datasource.username=${DB_USER}
quarkus.datasource.password=CHANGE_ME

# Connection Pool
quarkus.datasource.jdbc.max-size=20
quarkus.datasource.jdbc.min-size=5

# Logging
quarkus.log.level=INFO
quarkus.log.file.enable=true
quarkus.log.file.path=/opt/${APP_NAME}/logs/application.log

EOF
    fi
fi

# Create README
cat > "${DEPLOYMENT_DIR}/README.md" << EOF
# ${DEPLOYMENT_NAME} - ${APP_NAME} v${APP_VERSION}

## Configuration Files

- \`deployment.yaml\` - LXC container and deployment configuration
- \`application.properties\` - Quarkus application configuration (overrides JAR)

## Customize

1. Edit \`application.properties\`:
   - Update database URL, credentials
   - Adjust ports, logging, etc.

2. Edit \`deployment.yaml\` (if needed):
   - Update remote host, container name
   - Adjust resources (CPU, memory)

## Deploy

\`\`\`bash
cd ../..
./scripts/deploy.sh ${VERSION_FOLDER}/deployments/${DEPLOYMENT_NAME}
\`\`\`

## Verify

After deployment:

\`\`\`bash
# Check status
lxc exec ${CONTAINER_NAME} -- systemctl status ${APP_NAME}.service

# View logs
lxc exec ${CONTAINER_NAME} -- journalctl -u ${APP_NAME}.service -f
\`\`\`

EOF

echo ""
print_success "═══════════════════════════════════════════════════════════"
print_success "  Deployment Configuration Created!"
print_success "═══════════════════════════════════════════════════════════"
echo ""
print_info "Location: ${DEPLOYMENT_DIR}"
print_info "Files created:"
print_info "  - deployment.yaml"
print_info "  - application.properties"
print_info "  - README.md"
echo ""
print_info "Next steps:"
print_info "  1. Edit configuration:"
print_info "     vim ${DEPLOYMENT_NAME}/application.properties"
print_info "     vim ${DEPLOYMENT_NAME}/deployment.yaml"
print_info "  2. Deploy:"
print_info "     cd ../.."
print_info "     ./scripts/deploy.sh ${VERSION_FOLDER}/deployments/${DEPLOYMENT_NAME}"
echo ""

