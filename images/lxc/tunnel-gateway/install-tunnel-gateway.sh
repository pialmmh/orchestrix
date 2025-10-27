#!/bin/bash
set -e

# Color codes for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Print colored messages
info() { echo -e "${BLUE}[INFO]${NC} $1"; }
success() { echo -e "${GREEN}[SUCCESS]${NC} $1"; }
warn() { echo -e "${YELLOW}[WARN]${NC} $1"; }
error() { echo -e "${RED}[ERROR]${NC} $1"; }

# Banner
echo "=========================================="
echo "  Tunnel Gateway - Developer Setup"
echo "  SSH Tunnel Proxy for Development"
echo "=========================================="
echo ""

# Step 1: Check/Install LXD
info "Checking LXD installation..."
if ! command -v lxc &> /dev/null; then
    warn "LXD not found. Installing LXD..."
    echo ""
    echo "This script will install LXD using snap. Continue? (y/n)"
    read -r INSTALL_LXD
    if [[ "$INSTALL_LXD" =~ ^[Yy]$ ]]; then
        sudo snap install lxd
        success "LXD installed successfully"
    else
        error "LXD is required. Exiting."
        exit 1
    fi
else
    success "LXD is already installed"
fi

# Step 2: Initialize LXD if needed
info "Checking LXD initialization..."
if ! sudo lxc network list --format=csv 2>/dev/null | grep -q "lxdbr0"; then
    warn "LXD bridge not configured. Initializing LXD..."
    echo ""
    info "Enter bridge IPv4 address (default: 10.10.199.1/24):"
    read -r BRIDGE_IP
    BRIDGE_IP=${BRIDGE_IP:-10.10.199.1/24}
    
    sudo lxd init --auto \
        --network-address="${BRIDGE_IP%/*}" \
        --network-port=8443 \
        --storage-backend=dir
    
    success "LXD initialized with bridge at $BRIDGE_IP"
else
    success "LXD bridge already configured"
    BRIDGE_INFO=$(sudo lxc network get lxdbr0 ipv4.address 2>/dev/null || echo "10.10.199.1/24")
    info "Bridge IP: $BRIDGE_INFO"
fi

# Step 3: Import tunnel-gateway image
info "Looking for tunnel-gateway image..."

# Find the tar.gz file
IMAGE_FILE=$(find . -name "tunnel-gateway-v*.tar.gz" -type f 2>/dev/null | head -1)

if [ -z "$IMAGE_FILE" ]; then
    error "No tunnel-gateway tar.gz file found in current directory"
    echo ""
    echo "Please ensure you have downloaded the tunnel-gateway image file."
    echo "Expected format: tunnel-gateway-v*.tar.gz"
    exit 1
fi

info "Found image: $IMAGE_FILE"

# Check if image already imported
if sudo lxc image list --format=csv | grep -q "tunnel-gateway-base"; then
    warn "tunnel-gateway-base image already exists"
    echo "Delete and reimport? (y/n)"
    read -r REIMPORT
    if [[ "$REIMPORT" =~ ^[Yy]$ ]]; then
        sudo lxc image delete tunnel-gateway-base 2>/dev/null || true
        info "Importing tunnel-gateway image..."
        sudo lxc image import "$IMAGE_FILE" --alias tunnel-gateway-base
        success "Image imported as tunnel-gateway-base"
    else
        success "Using existing image"
    fi
else
    info "Importing tunnel-gateway image..."
    sudo lxc image import "$IMAGE_FILE" --alias tunnel-gateway-base
    success "Image imported as tunnel-gateway-base"
fi

# Step 4: Interactive tunnel configuration
echo ""
info "=== Configure SSH Tunnels ==="
echo ""
echo "You will now configure SSH tunnels for your services."
echo "Each tunnel forwards a local port to a remote service through SSH."
echo ""

# Container configuration
info "Enter container name (default: tunnel-gateway-dev):"
read -r CONTAINER_NAME
CONTAINER_NAME=${CONTAINER_NAME:-tunnel-gateway-dev}

info "Enter container IP address (default: 10.10.199.150):"
read -r CONTAINER_IP
CONTAINER_IP=${CONTAINER_IP:-10.10.199.150}

# Array to store tunnel definitions
TUNNELS=()

# Interactive tunnel configuration loop
TUNNEL_NUM=1
while true; do
    echo ""
    info "=== Configure Tunnel #$TUNNEL_NUM ==="
    echo ""
    
    echo "Service name (e.g., mysql-prod, kafka-staging, or 'done' to finish):"
    read -r SERVICE_NAME
    
    if [ "$SERVICE_NAME" = "done" ] || [ -z "$SERVICE_NAME" ]; then
        break
    fi
    
    echo "Local port (port on this container, e.g., 3306 for MySQL, 9092 for Kafka):"
    read -r LOCAL_PORT
    
    echo "Remote SSH host (IP or hostname, e.g., db.example.com):"
    read -r SSH_HOST
    
    echo "SSH username (default: mustafa):"
    read -r SSH_USER
    SSH_USER=${SSH_USER:-mustafa}
    
    echo "Authentication type (password/key) [default: password]:"
    read -r AUTH_TYPE
    AUTH_TYPE=${AUTH_TYPE:-password}
    AUTH_TYPE=$(echo "$AUTH_TYPE" | tr '[:lower:]' '[:upper:]')
    
    if [ "$AUTH_TYPE" = "PASSWORD" ]; then
        echo "SSH password:"
        read -s SSH_PASSWORD
        AUTH_VALUE="$SSH_PASSWORD"
        echo ""
    else
        echo "Path to SSH private key (e.g., /home/user/.ssh/id_rsa):"
        read -r KEY_PATH
        AUTH_VALUE="$KEY_PATH"
        AUTH_TYPE="KEY"
    fi
    
    echo "Remote host (where the service actually runs, usually 'localhost' or same as SSH host):"
    read -r REMOTE_HOST
    REMOTE_HOST=${REMOTE_HOST:-localhost}
    
    echo "Remote port (port on remote host, e.g., 3306 for MySQL):"
    read -r REMOTE_PORT
    
    # Add tunnel to array
    TUNNEL_DEF="${SERVICE_NAME}:${LOCAL_PORT}:${SSH_HOST}:${SSH_USER}:${AUTH_TYPE}:${AUTH_VALUE}:${REMOTE_HOST}:${REMOTE_PORT}"
    TUNNELS+=("$TUNNEL_DEF")
    
    success "Tunnel configured: $SERVICE_NAME"
    echo "  Local: 0.0.0.0:$LOCAL_PORT -> SSH: $SSH_USER@$SSH_HOST -> Remote: $REMOTE_HOST:$REMOTE_PORT"
    
    TUNNEL_NUM=$((TUNNEL_NUM + 1))
done

if [ ${#TUNNELS[@]} -eq 0 ]; then
    error "No tunnels configured. Exiting."
    exit 1
fi

# Step 5: Generate configuration file
info "Generating configuration file..."

CONFIG_FILE="tunnel-gateway-config-$(date +%Y%m%d-%H%M%S).conf"

cat > "$CONFIG_FILE" << CONFIGEOF
# Tunnel Gateway Configuration
# Generated: $(date)

# Container Configuration
CONTAINER_NAME="$CONTAINER_NAME"
CONTAINER_IP="$CONTAINER_IP"

# SSH Tunnel Definitions
# Format: NAME:LOCAL_PORT:SSH_HOST:SSH_USER:AUTH_TYPE:AUTH_VALUE:REMOTE_HOST:REMOTE_PORT
TUNNELS=(
CONFIGEOF

# Add each tunnel to config
for tunnel in "${TUNNELS[@]}"; do
    echo "    \"$tunnel\"" >> "$CONFIG_FILE"
done

cat >> "$CONFIG_FILE" << 'CONFIGEOF'
)

# Optional: Bind mounts for SSH keys
# Add SSH key directories if using key authentication
BIND_MOUNTS=()

# Auto-start tunnels on container boot
AUTO_START="true"
CONFIGEOF

success "Configuration saved to: $CONFIG_FILE"

# Step 6: Launch container
echo ""
info "=== Launching Tunnel Gateway Container ==="
echo ""

# Check if container already exists
if sudo lxc list --format=csv -c n | grep -q "^${CONTAINER_NAME}$"; then
    warn "Container $CONTAINER_NAME already exists"
    echo "Delete and recreate? (y/n)"
    read -r DELETE_EXISTING
    if [[ "$DELETE_EXISTING" =~ ^[Yy]$ ]]; then
        info "Deleting existing container..."
        sudo lxc delete "$CONTAINER_NAME" --force
        success "Existing container deleted"
    else
        error "Cannot proceed with existing container. Exiting."
        exit 1
    fi
fi

# Launch container
info "Launching container $CONTAINER_NAME..."
sudo lxc launch tunnel-gateway-base "$CONTAINER_NAME"
sleep 3

# Configure static IP
info "Configuring static IP: $CONTAINER_IP"
sudo lxc config device override "$CONTAINER_NAME" eth0 ipv4.address="$CONTAINER_IP"
sudo lxc restart "$CONTAINER_NAME"
sleep 3

# Push tunnel configuration
info "Pushing tunnel configuration to container..."
TEMP_CONF="/tmp/tunnels-$$.conf"
cat > "$TEMP_CONF" << TUNNELEOF
# Tunnel configuration
TUNNELS=(
TUNNELEOF

for tunnel in "${TUNNELS[@]}"; do
    echo "    \"$tunnel\"" >> "$TEMP_CONF"
done

echo ")" >> "$TEMP_CONF"

sudo lxc file push "$TEMP_CONF" "$CONTAINER_NAME/etc/tunnel-gateway/tunnels.conf"
rm -f "$TEMP_CONF"

# Start tunnels
info "Starting SSH tunnels..."
sudo lxc exec "$CONTAINER_NAME" -- /usr/local/bin/start-tunnels.sh

sleep 2

# Verify container status
echo ""
success "=== Tunnel Gateway Ready! ==="
echo ""

info "Container Information:"
sudo lxc list "$CONTAINER_NAME" --format=table --columns=ns4

echo ""
info "Active Tunnels:"
sudo lxc exec "$CONTAINER_NAME" -- /usr/local/bin/list-tunnels.sh

echo ""
info "=== Usage Instructions ==="
echo ""
echo "Your services are now accessible through the tunnel gateway at:"
echo "  Container IP: $CONTAINER_IP"
echo ""
echo "Connection examples:"
for tunnel in "${TUNNELS[@]}"; do
    IFS=':' read -r name local_port _ _ _ _ _ _ <<< "$tunnel"
    echo "  $name: Connect to $CONTAINER_IP:$local_port"
done

echo ""
info "Management Commands:"
echo "  List tunnels:   sudo lxc exec $CONTAINER_NAME -- /usr/local/bin/list-tunnels.sh"
echo "  Stop tunnels:   sudo lxc exec $CONTAINER_NAME -- /usr/local/bin/stop-tunnels.sh"
echo "  Start tunnels:  sudo lxc exec $CONTAINER_NAME -- /usr/local/bin/start-tunnels.sh"
echo "  Shell access:   sudo lxc exec $CONTAINER_NAME -- bash"
echo "  Stop container: sudo lxc stop $CONTAINER_NAME"
echo "  Start container: sudo lxc start $CONTAINER_NAME"
echo "  Delete container: sudo lxc delete $CONTAINER_NAME --force"

echo ""
success "Setup complete! Your tunnel gateway is running."
echo ""
info "Configuration saved to: $CONFIG_FILE"
info "You can use this file to recreate the same setup later."
