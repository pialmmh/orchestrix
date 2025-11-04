#!/bin/bash
# Quick BTRFS setup for local development
# Run with: sudo ./quick-btrfs-setup.sh

set -e

echo "========================================="
echo "Quick BTRFS Setup for Orchestrix"
echo "========================================="

# Configuration
BTRFS_DIR="/home/telcobright/btrfs"
BTRFS_IMAGE="${BTRFS_DIR}/btrfs.img"
BTRFS_SIZE="20G"
MOUNT_POINT="${BTRFS_DIR}"
CONFIG_DIR="/etc/orchestrix"
CONFIG_FILE="${CONFIG_DIR}/storage-locations.conf"

# Install BTRFS tools
echo "Installing BTRFS tools..."
apt-get update
apt-get install -y btrfs-progs

# Create directory structure
echo "Creating directory structure..."
mkdir -p "${BTRFS_DIR}"
mkdir -p "${CONFIG_DIR}"

# Check if already mounted
if mount | grep -q "${MOUNT_POINT}"; then
    echo "BTRFS already mounted at ${MOUNT_POINT}"
else
    # Create BTRFS image file
    echo "Creating BTRFS image (${BTRFS_SIZE})..."
    if [ ! -f "${BTRFS_IMAGE}" ]; then
        truncate -s ${BTRFS_SIZE} "${BTRFS_IMAGE}"
        mkfs.btrfs "${BTRFS_IMAGE}"
    fi

    # Mount BTRFS
    echo "Mounting BTRFS..."
    mount -o loop "${BTRFS_IMAGE}" "${MOUNT_POINT}"
fi

# Enable quota
echo "Enabling BTRFS quota..."
btrfs quota enable "${MOUNT_POINT}" 2>/dev/null || true

# Create container directories
echo "Creating container directories..."
mkdir -p "${MOUNT_POINT}/containers"
mkdir -p "${MOUNT_POINT}/snapshots"
mkdir -p "${MOUNT_POINT}/backups"

# Set permissions
echo "Setting permissions..."
chown -R $(logname):$(logname) "${BTRFS_DIR}"
chmod 755 "${BTRFS_DIR}"

# Create storage locations config
echo "Creating storage configuration..."
cat > "${CONFIG_FILE}" << EOF
# Orchestrix Storage Locations Configuration
# Generated: $(date)

# Local BTRFS storage for development
btrfs_local_main.path=${MOUNT_POINT}
btrfs_local_main.type=local
btrfs_local_main.filesystem=btrfs
btrfs_local_main.compression=true
btrfs_local_main.auto_mount=true
btrfs_local_main.mount_options=loop,compress=lzo
EOF

# Add to fstab for persistence (optional)
echo ""
echo "To make BTRFS mount persistent, add to /etc/fstab:"
echo "${BTRFS_IMAGE} ${MOUNT_POINT} btrfs loop,compress=lzo 0 0"

# Verify setup
echo ""
echo "========================================="
echo "BTRFS Setup Complete!"
echo "========================================="
echo "Storage location: ${MOUNT_POINT}"
echo "Config file: ${CONFIG_FILE}"
echo ""
echo "Verify with:"
echo "  btrfs filesystem show ${MOUNT_POINT}"
echo "  btrfs subvolume list ${MOUNT_POINT}"
echo ""
echo "Test quota:"
echo "  btrfs qgroup show ${MOUNT_POINT}"
echo ""
echo "Now you can build containers with:"
echo "  cd /home/mustafa/telcobright-projects/orchestrix/images/containers/lxc/grafana-loki"
echo "  sudo ./build/build.sh"
echo "========================================="

# Show current status
btrfs filesystem df "${MOUNT_POINT}"