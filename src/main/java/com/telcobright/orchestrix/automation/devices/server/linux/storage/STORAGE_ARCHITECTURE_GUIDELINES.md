# Container Storage Architecture Guidelines

## Overview
All containers in Orchestrix must use a managed storage backend (currently BTRFS is mandatory, with future support for LVM, Ceph, ZFS, etc.). This ensures consistent storage management, quotas, snapshots, and performance optimization.

## Architecture Principles

### 1. Interface-Based Storage Abstraction
- All storage operations through `StorageProvider` interface
- Technology-specific implementations (BTRFS, LVM, Ceph, etc.)
- Factory pattern for creating appropriate providers
- Distribution-specific installation and configuration

### 2. Storage Location Management
- Support multiple storage locations with unique IDs
- Each location represents a physical path on potentially different drives
- Examples:
  - `btrfs_ssd_main=/home/telcobright/btrfs` (SSD for performance-critical containers)
  - `btrfs_sata_archive=/sata/btrfs` (SATA for archival/backup containers)
  - `btrfs_nvme_db=/nvme/btrfs` (NVMe for database containers)

### 3. Container Storage Requirements
- **One Volume Per Container**: Each container gets exactly one storage volume
- **Storage Provider Binding**: Every container must bind to a storage provider
- **Quota Management**: Each container must specify disk quota
- **Location Selection**: Container config must specify storage location ID

## Implementation Structure

```
storage/
├── STORAGE_ARCHITECTURE_GUIDELINES.md
├── base/
│   ├── StorageProvider.java              # Base interface
│   ├── StorageLocation.java              # Location configuration
│   ├── StorageVolume.java                # Volume abstraction
│   ├── StorageConfig.java                # Storage configuration
│   └── AbstractStorageAutomation.java    # Base implementation
├── btrfs/
│   ├── BtrfsInstallAutomation.java       # BTRFS installation
│   ├── BtrfsStorageProvider.java         # BTRFS provider implementation
│   ├── BtrfsVolumeAutomation.java        # Volume management
│   └── LxcContainerBtrfsMountAutomation.java # LXC-specific mounting
├── lvm/                                   # Future: LVM support
├── ceph/                                  # Future: Ceph support
└── zfs/                                   # Future: ZFS support
```

## Container Build Configuration

### Required Configuration Fields

```properties
# Storage provider selection (mandatory)
storage.provider=btrfs

# Storage location ID (mandatory)
storage.location.id=btrfs_ssd_main

# Container root path within storage (mandatory)
storage.container.root=mysql

# Disk quota for container (mandatory)
storage.quota.size=50G

# Optional: Enable compression
storage.compression=true

# Optional: Snapshot policy
storage.snapshot.enabled=true
storage.snapshot.frequency=daily
storage.snapshot.retain=7
```

### Storage Location Configuration

System-wide storage locations are defined in `/etc/orchestrix/storage-locations.conf`:

```properties
# Storage location definitions
btrfs_ssd_main.path=/home/telcobright/btrfs
btrfs_ssd_main.type=ssd
btrfs_ssd_main.provider=btrfs

btrfs_sata_archive.path=/sata/btrfs
btrfs_sata_archive.type=sata
btrfs_sata_archive.provider=btrfs

btrfs_nvme_db.path=/nvme/btrfs
btrfs_nvme_db.type=nvme
btrfs_nvme_db.provider=btrfs
```

## Build Script Requirements

### Storage Validation
Build scripts must:
1. Verify storage provider is available
2. Validate storage location exists
3. Check available space against quota
4. Create subvolume/volume with specified quota
5. Set up proper mount bindings

### Sample Build Script Flow
```bash
# 1. Load storage configuration
STORAGE_PROVIDER="${storage_provider:-btrfs}"
STORAGE_LOCATION_ID="${storage_location_id}"
STORAGE_ROOT="${storage_container_root}"
STORAGE_QUOTA="${storage_quota_size}"

# 2. Validate storage provider
validate_storage_provider "$STORAGE_PROVIDER"

# 3. Get storage location path
STORAGE_PATH=$(get_storage_location_path "$STORAGE_LOCATION_ID")

# 4. Create storage volume
create_storage_volume "$STORAGE_PATH/$STORAGE_ROOT" "$STORAGE_QUOTA"

# 5. Configure container with storage binding
configure_container_storage "$CONTAINER_NAME" "$STORAGE_PATH/$STORAGE_ROOT"
```

## Java Automation Classes

### StorageProvider Interface
```java
public interface StorageProvider {
    boolean isInstalled(SshDevice device);
    boolean install(SshDevice device);
    StorageVolume createVolume(SshDevice device, String path, long quotaBytes);
    boolean deleteVolume(SshDevice device, String path);
    boolean setQuota(SshDevice device, String path, long quotaBytes);
    boolean createSnapshot(SshDevice device, String path, String snapshotName);
    StorageTechnology getType();
}
```

### BtrfsInstallAutomation
- Installs BTRFS tools on various distributions
- Configures kernel modules
- Sets up initial BTRFS filesystem
- Distribution-specific implementations

### LxcContainerBtrfsMountAutomation
- Creates BTRFS subvolumes for containers
- Sets quotas
- Configures LXC mount bindings
- Manages container storage lifecycle

## Storage Best Practices

### 1. Volume Organization
```
/storage-location/
├── containers/           # Container root volumes
│   ├── mysql/
│   ├── redis/
│   └── nginx/
├── snapshots/           # Snapshot storage
│   └── mysql/
│       ├── 2024-01-15_daily/
│       └── 2024-01-16_daily/
└── backups/            # Backup exports
```

### 2. Quota Guidelines
- Database containers: 50-100GB
- Application containers: 10-20GB
- Cache containers: 5-10GB
- Log containers: 20-50GB

### 3. Performance Optimization
- Use SSD locations for database and cache containers
- Use SATA for archival and backup containers
- Enable compression for text-heavy workloads
- Disable compression for already-compressed data

### 4. Snapshot Strategy
- Daily snapshots with 7-day retention for production
- Hourly snapshots with 24-hour retention for development
- Pre-deployment snapshots before updates

## Migration Path

### Adding New Storage Provider
1. Create package under `storage/<provider>/`
2. Implement `StorageProvider` interface
3. Add distribution-specific installation automation
4. Create provider-specific mount automation
5. Update factory to include new provider
6. Test with sample container

### Container Migration
1. Create snapshot of existing container
2. Export container data
3. Create new volume with storage provider
4. Import container data
5. Update container configuration
6. Verify and remove old storage

## Security Considerations

1. **Isolation**: Each container gets isolated storage volume
2. **Quotas**: Prevent resource exhaustion
3. **Permissions**: Proper ownership and permissions per container
4. **Encryption**: Support for encrypted volumes (provider-specific)
5. **Audit**: Log all storage operations

## Monitoring and Alerts

1. **Space Usage**: Monitor quota usage per container
2. **Performance**: Track IOPS and throughput
3. **Health**: Check filesystem integrity
4. **Snapshots**: Verify snapshot creation and cleanup
5. **Alerts**: Notify on quota exceeded, failures

## Future Enhancements

1. **Multi-Provider Support**: Allow containers to use multiple storage providers
2. **Tiered Storage**: Automatic data movement between storage tiers
3. **Distributed Storage**: Support for distributed filesystems
4. **Storage Migration**: Live migration between storage providers
5. **Backup Integration**: Automated backup to external storage