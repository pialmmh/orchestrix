# BTRFS Storage Setup for Orchestrix

## Overview

BTRFS storage configured for LXC container testing on your local PC.

**Setup Date**: 2025-10-10

## Configuration

### BTRFS Filesystem
- **Type**: Loopback file (safe for testing)
- **Size**: 50GB (sparse file, grows as needed)
- **Location**: `/home/telcobright/btrfs-storage.img`
- **Mount Point**: `/home/telcobright/btrfs-storage`
- **Filesystem**: BTRFS with zstd compression
- **Quota**: Enabled

### Storage Location Configuration
- **Config File**: `/etc/orchestrix/storage-locations.conf`
- **Storage ID**: `btrfs_local_main`
- **Path**: `/home/telcobright/btrfs-storage`

## Verification

```bash
# Check mount
df -Th /home/telcobright/btrfs-storage

# Output:
# Filesystem     Type   Size  Used Avail Use% Mounted on
# /dev/loop27    btrfs   50G  5.8M   50G   1% /home/telcobright/btrfs-storage

# Check BTRFS info
sudo btrfs filesystem show /home/telcobright/btrfs-storage

# Check quota
sudo btrfs qgroup show /home/telcobright/btrfs-storage
```

## Directory Structure

```
/home/telcobright/btrfs-storage/
├── containers/          # Container subvolumes
└── snapshots/           # BTRFS snapshots
```

## Features Enabled

- ✅ BTRFS filesystem with compression (zstd)
- ✅ Quota management enabled
- ✅ Subvolume support
- ✅ Snapshot support
- ✅ Compression enabled

## Usage in Containers

Containers using this storage will get:
- **Quota enforcement** - Hard limits on disk usage
- **Compression** - Automatic data compression
- **Snapshots** - Point-in-time backups
- **Subvolumes** - Isolated storage per container

## Commands

### Check Storage Usage
```bash
sudo btrfs filesystem usage /home/telcobright/btrfs-storage
```

### List Subvolumes
```bash
sudo btrfs subvolume list /home/telcobright/btrfs-storage
```

### Check Quotas
```bash
sudo btrfs qgroup show /home/telcobright/btrfs-storage
```

### Create Manual Snapshot
```bash
sudo btrfs subvolume snapshot -r \
  /home/telcobright/btrfs-storage/containers/grafana-loki-v.1 \
  /home/telcobright/btrfs-storage/snapshots/grafana-loki-backup-$(date +%Y%m%d)
```

## Automatic Mount on Boot (Optional)

To mount automatically on system boot:

```bash
# Get UUID
sudo blkid /home/telcobright/btrfs-storage.img

# Add to /etc/fstab
/home/telcobright/btrfs-storage.img  /home/telcobright/btrfs-storage  btrfs  loop,compress=zstd  0  0
```

## Manual Mount (Current Setup)

Currently using manual mount. To remount after reboot:

```bash
sudo mount -o loop,compress=zstd /home/telcobright/btrfs-storage.img /home/telcobright/btrfs-storage
sudo btrfs quota enable /home/telcobright/btrfs-storage
```

## Cleanup (If Needed)

To remove the BTRFS setup:

```bash
# Unmount
sudo umount /home/telcobright/btrfs-storage

# Remove image file
sudo rm /home/telcobright/btrfs-storage.img

# Remove mount point
sudo rmdir /home/telcobright/btrfs-storage

# Remove config
sudo rm /etc/orchestrix/storage-locations.conf
```

## Testing with Grafana-Loki

Now you can build the Grafana-Loki container with real BTRFS:

```bash
cd /home/mustafa/telcobright-projects/orchestrix/images/containers/lxc/grafana-loki
./build/build.sh
```

Expected behavior:
- ✅ Prerequisites check will detect BTRFS
- ✅ Build will create BTRFS subvolume with 20GB quota
- ✅ Container will use BTRFS compression
- ✅ Snapshot will be created after build

## Storage Sizing

With 50GB BTRFS:
- Grafana-Loki (20GB quota): Can fit 2-3 instances
- Remaining space: ~30GB for other containers
- Sparse file: Only uses actual disk space as data is written

## Notes

- **Sparse File**: The 50GB file is sparse - it only uses actual disk space as data is written
- **Compression**: zstd compression saves significant space for logs
- **Quota**: Each container has a hard limit preventing storage overflow
- **Snapshots**: Instant snapshots with minimal space overhead

## Troubleshooting

### Mount Not Persistent After Reboot
```bash
# Remount manually
sudo mount -o loop,compress=zstd /home/telcobright/btrfs-storage.img /home/telcobright/btrfs-storage
sudo btrfs quota enable /home/telcobright/btrfs-storage
```

### Quota Not Working
```bash
# Re-enable quota
sudo btrfs quota enable /home/telcobright/btrfs-storage
```

### Check Space Usage
```bash
# Overall filesystem
df -h /home/telcobright/btrfs-storage

# Per-subvolume quotas
sudo btrfs qgroup show /home/telcobright/btrfs-storage
```

## Status

✅ **BTRFS storage is ready for testing**

You can now build containers with full BTRFS support including:
- Quota management
- Compression
- Snapshots
- Storage monitoring