#!/bin/bash

# deploy.sh - Deploys Quarkus application to LXC container
# Usage: ./deploy.sh <path-to-deployment-folder>

set -e

# Color codes
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

print_info() { echo -e "${BLUE}ℹ${NC} $1"; }
print_success() { echo -e "${GREEN}✅${NC} $1"; }
print_warning() { echo -e "${YELLOW}⚠${NC} $1"; }
print_error() { echo -e "${RED}❌${NC} $1"; }

if [ $# -lt 1 ]; then
    print_error "Usage: $0 <path-to-deployment-folder>"
    echo ""
    echo "Example:"
    echo "  $0 infinite-scheduler-v1.0.0/deployments/link3-prod"
    exit 1
fi

DEPLOYMENT_PATH="$1"

if [ ! -d "$DEPLOYMENT_PATH" ]; then
    print_error "Deployment folder not found: $DEPLOYMENT_PATH"
    exit 1
fi

# Load deployment.yaml (simple YAML parser)
parse_yaml() {
    local prefix=$2
    local s='[[:space:]]*'
    local w='[a-zA-Z0-9_]*'
    local fs=$(echo @|tr @ '\034')
    sed -ne "s|^\($s\):|\1|" \
         -e "s|^\($s\)\($w\)$s:$s[\"']\(.*\)[\"']$s\$|\1$fs\2$fs\3|p" \
         -e "s|^\($s\)\($w\)$s:$s\(.*\)$s\$|\1$fs\2$fs\3|p" $1 |
    awk -F$fs '{
        indent = length($1)/2;
        vname[indent] = $2;
        for (i in vname) {if (i > indent) {delete vname[i]}}
        if (length($3) > 0) {
            vn=""; for (i=0; i<indent; i++) {vn=(vn)(vname[i])("_")}
            printf("%s%s%s=\"%s\"\n", "'$prefix'",vn, $2, $3);
        }
    }'
}

DEPLOYMENT_YAML="${DEPLOYMENT_PATH}/deployment.yaml"
if [ ! -f "$DEPLOYMENT_YAML" ]; then
    print_error "deployment.yaml not found in: $DEPLOYMENT_PATH"
    exit 1
fi

print_info "Loading deployment configuration..."
eval $(parse_yaml "$DEPLOYMENT_YAML" "deploy_")

# Extract version from path
VERSION_FOLDER=$(basename $(dirname "$DEPLOYMENT_PATH"))
APP_NAME=$(echo "$VERSION_FOLDER" | sed 's/-v.*//')
APP_VERSION=$(echo "$VERSION_FOLDER" | sed 's/.*-v//')

# Find artifacts
ARTIFACTS_DIR="$(dirname $(dirname $DEPLOYMENT_PATH))/generated/artifacts"
TARBALL="${ARTIFACTS_DIR}/${APP_NAME}-${APP_VERSION}.tar.gz"

if [ ! -f "$TARBALL" ]; then
    print_error "Tarball not found: $TARBALL"
    print_info "Run build.sh first"
    exit 1
fi

echo ""
print_info "═══════════════════════════════════════════════════════════"
print_info "  Deploying: ${APP_NAME} v${APP_VERSION}"
print_info "  Deployment: ${deploy_name}"
if [ -n "$deploy_remote_host" ]; then
    print_info "  Target: ${deploy_remote_host}"
else
    print_info "  Target: Local"
fi
print_info "═══════════════════════════════════════════════════════════"
echo ""

# Determine if local or remote deployment
if [ -n "$deploy_remote_host" ]; then
    REMOTE_CMD="ssh ${deploy_remote_user}@${deploy_remote_host}"
    LXC_CMD="$REMOTE_CMD lxc"
else
    LXC_CMD="lxc"
fi

# Check if container exists
print_info "Checking for existing container..."
if [ -n "$deploy_remote_host" ]; then
    CONTAINER_EXISTS=$($LXC_CMD list -c n --format csv | grep -c "^${deploy_container_name}\$" || echo "0")
else
    CONTAINER_EXISTS=$(lxc list -c n --format csv | grep -c "^${deploy_container_name}\$" || echo "0")
fi

if [ "$CONTAINER_EXISTS" != "0" ]; then
    print_warning "Container ${deploy_container_name} already exists"
    read -p "Delete and recreate? (y/N): " -n 1 -r
    echo
    if [[ $REPLY =~ ^[Yy]$ ]]; then
        print_info "Deleting existing container..."
        $LXC_CMD stop "${deploy_container_name}" --force
        $LXC_CMD delete "${deploy_container_name}"
        print_success "Container deleted"
    else
        print_info "Deployment cancelled"
        exit 0
    fi
fi

# Import image
IMAGE_ALIAS="${APP_NAME}-${APP_VERSION}"
print_info "Importing image..."

if [ -n "$deploy_remote_host" ]; then
    # Remote deployment - copy tarball
    print_info "Copying tarball to remote host..."
    scp "$TARBALL" "${deploy_remote_user}@${deploy_remote_host}:/tmp/${APP_NAME}-${APP_VERSION}.tar.gz"
    $REMOTE_CMD "lxc image import /tmp/${APP_NAME}-${APP_VERSION}.tar.gz --alias ${IMAGE_ALIAS} && rm /tmp/${APP_NAME}-${APP_VERSION}.tar.gz"
else
    # Local deployment
    lxc image import "$TARBALL" --alias "$IMAGE_ALIAS"
fi

print_success "Image imported: $IMAGE_ALIAS"

# Create container
print_info "Creating container: ${deploy_container_name}..."
$LXC_CMD launch "$IMAGE_ALIAS" "${deploy_container_name}"
sleep 5
print_success "Container created"

# Configure network if specified
if [ -n "$deploy_container_network_ip" ]; then
    print_info "Configuring static IP: ${deploy_container_network_ip}..."

    IP="${deploy_container_network_ip%/*}"
    NETMASK="${deploy_container_network_ip##*/}"
    GATEWAY="${deploy_container_network_gateway}"

    $LXC_CMD exec "${deploy_container_name}" -- bash -c "cat > /etc/network/interfaces.d/eth0 <<EOF
auto eth0
iface eth0 inet static
    address $IP/$NETMASK
    gateway $GATEWAY
EOF
systemctl restart networking
"
    print_success "Network configured"
fi

# Set resource limits
if [ -n "$deploy_container_resources_cpu" ]; then
    print_info "Setting CPU limit: ${deploy_container_resources_cpu}"
    $LXC_CMD config set "${deploy_container_name}" limits.cpu "${deploy_container_resources_cpu}"
fi

if [ -n "$deploy_container_resources_memory" ]; then
    print_info "Setting memory limit: ${deploy_container_resources_memory}"
    $LXC_CMD config set "${deploy_container_name}" limits.memory "${deploy_container_resources_memory}"
fi

# Inject application.properties
APP_PROPS="${DEPLOYMENT_PATH}/application.properties"
if [ -f "$APP_PROPS" ]; then
    print_info "Injecting deployment-specific application.properties..."

    if [ -n "$deploy_remote_host" ]; then
        # Copy to remote first, then to container
        scp "$APP_PROPS" "${deploy_remote_user}@${deploy_remote_host}:/tmp/application.properties"
        $REMOTE_CMD "lxc file push /tmp/application.properties ${deploy_container_name}/opt/${APP_NAME}/config/application.properties && rm /tmp/application.properties"
    else
        lxc file push "$APP_PROPS" "${deploy_container_name}/opt/${APP_NAME}/config/application.properties"
    fi

    print_success "application.properties injected (will override JAR defaults)"
fi

# Run post-deployment commands
if [ -n "$deploy_post_deploy" ]; then
    print_info "Running post-deployment commands..."
    $LXC_CMD exec "${deploy_container_name}" -- systemctl enable "${APP_NAME}.service"
    $LXC_CMD exec "${deploy_container_name}" -- systemctl start "${APP_NAME}.service"
    sleep 5

    SERVICE_STATUS=$($LXC_CMD exec "${deploy_container_name}" -- systemctl is-active "${APP_NAME}.service" || echo "inactive")

    if [ "$SERVICE_STATUS" = "active" ]; then
        print_success "Service is running"
    else
        print_warning "Service status: $SERVICE_STATUS"
    fi
fi

# Cleanup image
$LXC_CMD image delete "$IMAGE_ALIAS" || true

echo ""
print_success "═══════════════════════════════════════════════════════════"
print_success "  Deployment Complete!"
print_success "═══════════════════════════════════════════════════════════"
echo ""
print_info "Container: ${deploy_container_name}"
if [ -n "$deploy_container_network_ip" ]; then
    print_info "IP: ${deploy_container_network_ip%/*}"
fi
echo ""
print_info "Useful commands:"
echo ""

if [ -n "$deploy_remote_host" ]; then
    print_info "Check status:"
    echo "  ssh ${deploy_remote_user}@${deploy_remote_host} 'lxc exec ${deploy_container_name} -- systemctl status ${APP_NAME}.service'"
    echo ""
    print_info "View logs:"
    echo "  ssh ${deploy_remote_user}@${deploy_remote_host} 'lxc exec ${deploy_container_name} -- journalctl -u ${APP_NAME}.service -f'"
else
    print_info "Check status:"
    echo "  lxc exec ${deploy_container_name} -- systemctl status ${APP_NAME}.service"
    echo ""
    print_info "View logs:"
    echo "  lxc exec ${deploy_container_name} -- journalctl -u ${APP_NAME}.service -f"
fi

echo ""

