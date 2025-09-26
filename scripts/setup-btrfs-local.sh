#!/bin/bash
# Setup script for BTRFS on Ubuntu 24.04 LTS
# This script installs BTRFS and configures storage locations for container deployment

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Configuration
BTRFS_ROOT="/home/mustafa/telcobright/btrfs"
ORCHESTRIX_CONFIG_DIR="/etc/orchestrix"
STORAGE_LOCATIONS_FILE="${ORCHESTRIX_CONFIG_DIR}/storage-locations.conf"

echo -e "${GREEN}=========================================${NC}"
echo -e "${GREEN}BTRFS Setup for Ubuntu 24.04 LTS${NC}"
echo -e "${GREEN}=========================================${NC}"

# Function to print status
print_status() {
    echo -e "${YELLOW}[*]${NC} $1"
}

print_success() {
    echo -e "${GREEN}[✓]${NC} $1"
}

print_error() {
    echo -e "${RED}[✗]${NC} $1"
}

# Check if running as root or with sudo
if [ "$EUID" -ne 0 ]; then
    echo "This script must be run with sudo"
    echo "Usage: sudo $0"
    exit 1
fi

# Get the actual user (not root)
ACTUAL_USER=${SUDO_USER:-$(whoami)}
ACTUAL_HOME="/home/${ACTUAL_USER}"

# Step 1: Check Ubuntu version
print_status "Checking Ubuntu version..."
UBUNTU_VERSION=$(lsb_release -rs)
UBUNTU_CODENAME=$(lsb_release -cs)

if [[ "$UBUNTU_VERSION" != "24.04" ]]; then
    print_error "Warning: This script is designed for Ubuntu 24.04 but you're running $UBUNTU_VERSION"
    read -p "Continue anyway? (y/N): " -n 1 -r
    echo
    if [[ ! $REPLY =~ ^[Yy]$ ]]; then
        exit 1
    fi
fi
print_success "Ubuntu $UBUNTU_VERSION ($UBUNTU_CODENAME) detected"

# Step 2: Update package lists
print_status "Updating package lists..."
apt-get update > /dev/null 2>&1
print_success "Package lists updated"

# Step 3: Install BTRFS tools
print_status "Installing BTRFS tools..."
PACKAGES="btrfs-progs"

# Check if packages are already installed
MISSING_PACKAGES=""
for pkg in $PACKAGES; do
    if ! dpkg -l | grep -q "^ii  $pkg"; then
        MISSING_PACKAGES="$MISSING_PACKAGES $pkg"
    fi
done

if [ -n "$MISSING_PACKAGES" ]; then
    apt-get install -y $MISSING_PACKAGES
    print_success "BTRFS tools installed"
else
    print_success "BTRFS tools already installed"
fi

# Step 4: Load BTRFS kernel module
print_status "Loading BTRFS kernel module..."
modprobe btrfs || true

# Check if module is loaded
if lsmod | grep -q btrfs; then
    print_success "BTRFS kernel module loaded"
else
    print_error "Failed to load BTRFS kernel module"
    exit 1
fi

# Make module load on boot
echo "btrfs" > /etc/modules-load.d/btrfs.conf
print_success "BTRFS configured to load on boot"

# Step 5: Create BTRFS storage directory structure
print_status "Setting up BTRFS storage structure..."

# Use actual user's home directory
BTRFS_ROOT="${ACTUAL_HOME}/telcobright/btrfs"

# Create directory structure
mkdir -p "${BTRFS_ROOT}/containers"
mkdir -p "${BTRFS_ROOT}/snapshots"
mkdir -p "${BTRFS_ROOT}/backups"

# Set proper ownership
chown -R ${ACTUAL_USER}:${ACTUAL_USER} "${ACTUAL_HOME}/telcobright"

print_success "Storage directories created at ${BTRFS_ROOT}"

# Step 6: Check if we have a BTRFS filesystem
print_status "Checking for BTRFS filesystems..."

# Check if the target directory is on a BTRFS filesystem
FS_TYPE=$(stat -f -c %T "${BTRFS_ROOT}" 2>/dev/null || echo "unknown")

if [ "$FS_TYPE" = "btrfs" ]; then
    print_success "${BTRFS_ROOT} is on a BTRFS filesystem"
else
    print_status "${BTRFS_ROOT} is on $FS_TYPE filesystem"

    # Offer to create a loop device for testing
    echo -e "${YELLOW}The target directory is not on a BTRFS filesystem.${NC}"
    echo "For testing, we can create a BTRFS loop device (file-based)."
    read -p "Create a 50GB BTRFS loop device for testing? (y/N): " -n 1 -r
    echo

    if [[ $REPLY =~ ^[Yy]$ ]]; then
        LOOP_FILE="${ACTUAL_HOME}/telcobright/btrfs.img"
        LOOP_SIZE="50G"

        print_status "Creating ${LOOP_SIZE} loop device at ${LOOP_FILE}..."

        # Create sparse file
        truncate -s ${LOOP_SIZE} "${LOOP_FILE}"

        # Create BTRFS filesystem
        mkfs.btrfs -f "${LOOP_FILE}"

        # Mount the loop device
        LOOP_MOUNT="${ACTUAL_HOME}/telcobright/btrfs"
        mkdir -p "${LOOP_MOUNT}"

        # Mount it
        mount -o loop "${LOOP_FILE}" "${LOOP_MOUNT}"

        # Add to fstab for persistence
        echo "${LOOP_FILE} ${LOOP_MOUNT} btrfs loop,defaults,noatime,compress=lzo 0 0" >> /etc/fstab

        # Create subdirectories
        mkdir -p "${LOOP_MOUNT}/containers"
        mkdir -p "${LOOP_MOUNT}/snapshots"
        mkdir -p "${LOOP_MOUNT}/backups"

        # Set ownership
        chown -R ${ACTUAL_USER}:${ACTUAL_USER} "${LOOP_MOUNT}"

        print_success "BTRFS loop device created and mounted"
    fi
fi

# Step 7: Create Orchestrix configuration
print_status "Creating Orchestrix storage configuration..."

mkdir -p "${ORCHESTRIX_CONFIG_DIR}"

# Create storage locations configuration
cat > "${STORAGE_LOCATIONS_FILE}" << EOF
# Orchestrix Storage Locations Configuration
# Generated on $(date)
# System: Ubuntu ${UBUNTU_VERSION} (${UBUNTU_CODENAME})

# Primary BTRFS storage location (local development)
btrfs_local_main.path=${BTRFS_ROOT}
btrfs_local_main.type=local
btrfs_local_main.provider=btrfs
btrfs_local_main.description=Local BTRFS storage for development

# Containers storage
btrfs_local_containers.path=${BTRFS_ROOT}/containers
btrfs_local_containers.type=local
btrfs_local_containers.provider=btrfs
btrfs_local_containers.description=Container root volumes

# Snapshots storage
btrfs_local_snapshots.path=${BTRFS_ROOT}/snapshots
btrfs_local_snapshots.type=local
btrfs_local_snapshots.provider=btrfs
btrfs_local_snapshots.description=Container snapshots

# Backups storage
btrfs_local_backups.path=${BTRFS_ROOT}/backups
btrfs_local_backups.type=local
btrfs_local_backups.provider=btrfs
btrfs_local_backups.description=Container backups and exports
EOF

print_success "Storage locations configured at ${STORAGE_LOCATIONS_FILE}"

# Step 8: Install LXD if not present
print_status "Checking LXD installation..."

if ! command -v lxc &> /dev/null; then
    print_status "LXD not found, installing..."

    # For Ubuntu 24.04, use snap
    snap install lxd --classic

    # Add user to lxd group
    usermod -aG lxd ${ACTUAL_USER}

    print_success "LXD installed via snap"

    # Initialize LXD
    print_status "Initializing LXD..."
    lxd init --auto \
        --network-address=127.0.0.1 \
        --network-port=8443 \
        --trust-password=admin

    print_success "LXD initialized"
else
    print_success "LXD already installed"
fi

# Step 9: Configure network bridge for containers
print_status "Configuring LXD network bridge..."

# Check if lxdbr0 exists
if ! lxc network show lxdbr0 &>/dev/null; then
    lxc network create lxdbr0 \
        ipv4.address=10.0.3.1/24 \
        ipv4.nat=true \
        ipv6.address=none
    print_success "Created lxdbr0 network bridge"
else
    print_success "Network bridge lxdbr0 already exists"
fi

# Step 10: Enable IP forwarding
print_status "Enabling IP forwarding..."

# Enable immediately
sysctl -w net.ipv4.ip_forward=1 > /dev/null

# Make permanent
sed -i 's/#net.ipv4.ip_forward=1/net.ipv4.ip_forward=1/' /etc/sysctl.conf
grep -q "net.ipv4.ip_forward=1" /etc/sysctl.conf || echo "net.ipv4.ip_forward=1" >> /etc/sysctl.conf

print_success "IP forwarding enabled"

# Step 11: Verify BTRFS setup
print_status "Verifying BTRFS setup..."

echo ""
echo "BTRFS Version:"
btrfs --version

echo ""
echo "BTRFS Filesystems:"
mount -t btrfs

echo ""
echo "Storage Locations:"
cat "${STORAGE_LOCATIONS_FILE}" | grep ".path=" | while read line; do
    location=$(echo $line | cut -d'=' -f1 | cut -d'.' -f1)
    path=$(echo $line | cut -d'=' -f2)
    echo "  - $location: $path"
done

# Step 12: Create test script
print_status "Creating test script..."

TEST_SCRIPT="${ACTUAL_HOME}/test-btrfs-setup.sh"
cat > "${TEST_SCRIPT}" << 'TESTEOF'
#!/bin/bash
# Test BTRFS setup

echo "Testing BTRFS setup..."

# Test 1: Check BTRFS tools
echo -n "1. BTRFS tools: "
if command -v btrfs &> /dev/null; then
    echo "✓ Installed"
else
    echo "✗ Not found"
fi

# Test 2: Check kernel module
echo -n "2. BTRFS module: "
if lsmod | grep -q btrfs; then
    echo "✓ Loaded"
else
    echo "✗ Not loaded"
fi

# Test 3: Check storage directories
echo -n "3. Storage dirs: "
if [ -d "$HOME/telcobright/btrfs/containers" ]; then
    echo "✓ Exist"
else
    echo "✗ Not found"
fi

# Test 4: Check configuration
echo -n "4. Config file: "
if [ -f "/etc/orchestrix/storage-locations.conf" ]; then
    echo "✓ Exists"
else
    echo "✗ Not found"
fi

# Test 5: Check LXD
echo -n "5. LXD: "
if command -v lxc &> /dev/null; then
    echo "✓ Installed"
else
    echo "✗ Not found"
fi

echo ""
echo "Storage location: $HOME/telcobright/btrfs"
TESTEOF

chmod +x "${TEST_SCRIPT}"
chown ${ACTUAL_USER}:${ACTUAL_USER} "${TEST_SCRIPT}"

print_success "Test script created at ${TEST_SCRIPT}"

# Final summary
echo ""
echo -e "${GREEN}=========================================${NC}"
echo -e "${GREEN}BTRFS Setup Complete!${NC}"
echo -e "${GREEN}=========================================${NC}"
echo ""
echo "Summary:"
echo "  • BTRFS tools installed"
echo "  • Storage configured at: ${BTRFS_ROOT}"
echo "  • Config file: ${STORAGE_LOCATIONS_FILE}"
echo "  • LXD installed and configured"
echo "  • Network bridge: lxdbr0 (10.0.3.0/24)"
echo ""
echo "Next steps:"
echo "  1. Log out and back in for group changes to take effect"
echo "  2. Run test script: ${TEST_SCRIPT}"
echo "  3. Deploy containers using the test runner"
echo ""
echo -e "${YELLOW}Note: If you created a loop device, it will be mounted on reboot.${NC}"