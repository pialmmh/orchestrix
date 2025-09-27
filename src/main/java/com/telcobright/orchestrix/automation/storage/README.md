# Storage Automation Module

## Overview

This module provides storage automation capabilities for Orchestrix, with a focus on BTRFS storage management for LXC containers.

## Package Structure

```
storage/
├── base/                      # Core storage interfaces and abstractions
│   ├── StorageProvider.java         # Main storage provider interface
│   ├── StorageProviderFactory.java  # Factory for creating storage providers
│   ├── StorageLocation.java         # Storage location configuration
│   ├── StorageVolume.java           # Storage volume representation
│   ├── StorageVolumeConfig.java     # Volume configuration
│   └── StorageTechnology.java       # Storage technology enum
│
├── btrfs/                     # BTRFS-specific implementations
│   ├── BtrfsStorageProvider.java           # BTRFS storage provider
│   ├── BtrfsInstallAutomation.java         # BTRFS installation automation
│   └── LxcContainerBtrfsMountAutomation.java # LXC container mount automation
│
└── STORAGE_ARCHITECTURE_GUIDELINES.md  # Architecture documentation
```

## Key Components

### StorageProvider Interface
- Core interface for all storage providers
- Supports volume creation, deletion, snapshots, and quota management
- Technology-agnostic design allows for multiple implementations

### BtrfsStorageProvider
- Production implementation using BTRFS
- Features:
  - Subvolume management
  - Quota enforcement
  - Snapshot/restore capabilities
  - Compression support
  - Storage monitoring

### Storage Locations
Predefined storage locations with unique IDs:
- `btrfs_ssd_main` - Primary SSD storage
- `btrfs_sata_archive` - SATA archive storage
- `btrfs_nvme_db` - NVMe database storage
- `btrfs_local_main` - Local development storage

## Usage Examples

### Creating a Storage Volume

```java
// Get storage provider
StorageProvider provider = StorageProviderFactory.getProvider(StorageTechnology.BTRFS);

// Configure volume
StorageVolumeConfig config = new StorageVolumeConfig();
config.setLocationId("btrfs_ssd_main");
config.setContainerName("grafana-loki");
config.setQuotaSize("30G");
config.setCompression(true);

// Create volume
StorageVolume volume = provider.createVolume(sshAutomation, config);
```

### Mounting to LXC Container

```java
LxcContainerBtrfsMountAutomation mountAutomation = new LxcContainerBtrfsMountAutomation();
mountAutomation.setContainerName("grafana-loki-v1");
mountAutomation.setVolumePath(volume.getPath());
mountAutomation.setMountPath("/");

// Execute mount
mountAutomation.run(sshAutomation);
```

### Setting Storage Quota

```java
provider.setQuota(sshAutomation, volume, "30G");
```

### Creating Snapshots

```java
String snapshotPath = provider.createSnapshot(
    sshAutomation,
    volume,
    "backup-" + System.currentTimeMillis()
);
```

## Storage Requirements

All LXC containers MUST:
1. Use BTRFS storage with quota management
2. Have a defined storage location ID
3. Specify quota size (e.g., "10G", "30G")
4. Support snapshot/restore operations
5. Include storage monitoring

## Configuration

Storage locations are configured in `/etc/orchestrix/storage-locations.conf`:

```properties
btrfs_ssd_main.path=/mnt/btrfs-ssd
btrfs_ssd_main.type=ssd
btrfs_ssd_main.compression=true

btrfs_sata_archive.path=/mnt/btrfs-sata
btrfs_sata_archive.type=sata
btrfs_sata_archive.compression=false
```

## Testing

Run storage automation tests:

```java
LocalBtrfsDeploymentRunner runner = new LocalBtrfsDeploymentRunner();
runner.testBtrfsSetup();
runner.testContainerWithStorage();
```

## Future Enhancements

- [ ] ZFS storage provider
- [ ] LVM storage provider
- [ ] Remote storage support (NFS, iSCSI)
- [ ] Storage migration automation
- [ ] Multi-location volume spanning
- [ ] Storage health monitoring dashboard

## Related Documentation

- [Storage Architecture Guidelines](./STORAGE_ARCHITECTURE_GUIDELINES.md)
- [Container Scaffolding Standard](../../../../../images/lxc/CONTAINER_SCAFFOLDING_STANDARD.md)
- [LXC Container Guidelines](../../../../../images/lxc/LXC_CONTAINER_GUIDELINES.md)