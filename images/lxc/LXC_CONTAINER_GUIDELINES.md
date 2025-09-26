# LXC Container Creation Guidelines

## Overview
Comprehensive guidelines for creating LXC containers with BTRFS storage support, versioning, and standardized structure.

## Container Requirements

### 1. Storage Requirements (MANDATORY)

All containers MUST use BTRFS storage with the following configuration:

#### Required Storage Parameters
```properties
# Storage provider (mandatory - only btrfs currently)
storage.provider=btrfs

# Storage location ID (mandatory)
storage.location.id=btrfs_ssd_main

# Container root path (mandatory)
storage.container.root=<container-name>

# Disk quota (mandatory)
storage.quota.size=<size>G

# Compression (recommended)
storage.compression=true

# Snapshot configuration (optional)
storage.snapshot.enabled=true
storage.snapshot.frequency=daily
storage.snapshot.retain=7
```

#### Storage Location Types
- `btrfs_ssd_main` - Primary SSD storage for performance-critical containers
- `btrfs_sata_archive` - SATA storage for archival/backup containers
- `btrfs_nvme_db` - NVMe storage for database containers
- `btrfs_local_main` - Local development storage

### 2. Versioning Requirements

All containers MUST support versioning:

```bash
# Version configuration in build script
CONTAINER_VERSION="${version:-1.0.0}"
VERSION_TAG="${version_tag:-latest}"

# Image naming convention
IMAGE_NAME="${container-name}-v${CONTAINER_VERSION}"
EXPORT_PATH="/tmp/${IMAGE_NAME}.tar.gz"

# Container naming with version
CONTAINER_NAME="${container-name}-${VERSION_TAG}"
```

### 3. Directory Structure

```
images/lxc/<container-name>/
├── build<ContainerName>.sh          # Build script
├── launch<ContainerName>.sh         # Launch script
├── sample-config.conf               # Sample configuration
├── startDefault.sh                  # Quick start script
├── versions.conf                    # Version tracking
├── storage-requirements.conf       # Storage requirements
├── scripts/                         # Container-specific scripts
│   ├── install-<service>.sh
│   └── configure-<service>.sh
└── README.md                       # Documentation
```

### 4. Build Script Requirements

```bash
#!/bin/bash
# Build script for <container-name> with BTRFS storage

set -e

# Load configuration
CONFIG_FILE="${1:-./build<ContainerName>Config.cnf}"

# Version management
CONTAINER_VERSION="${version:-1.0.0}"
VERSION_TAG="${version_tag:-latest}"
BUILD_DATE=$(date +%Y%m%d)

# Storage configuration (MANDATORY)
STORAGE_PROVIDER="${storage_provider:-btrfs}"
STORAGE_LOCATION_ID="${storage_location_id}"
STORAGE_ROOT="${storage_container_root:-<container-name>}"
STORAGE_QUOTA="${storage_quota_size}"

# Validate storage configuration
if [ -z "$STORAGE_LOCATION_ID" ]; then
    echo "Error: storage.location.id is required"
    exit 1
fi

if [ -z "$STORAGE_QUOTA" ]; then
    echo "Error: storage.quota.size is required"
    exit 1
fi

# Setup BTRFS storage
setup_btrfs_storage() {
    local storage_path=$(get_storage_path "$STORAGE_LOCATION_ID")
    local volume_path="${storage_path}/containers/${STORAGE_ROOT}"

    # Create BTRFS subvolume
    sudo btrfs subvolume create "$volume_path"

    # Set quota
    set_btrfs_quota "$volume_path" "$STORAGE_QUOTA"

    echo "$volume_path"
}

# Configure container with storage
VOLUME_PATH=$(setup_btrfs_storage)
lxc config device add $CONTAINER_NAME root disk source=$VOLUME_PATH path=/
```

### 5. Launch Script Requirements

```bash
#!/bin/bash
# Launch script with BTRFS storage support

# Validate storage in config
STORAGE_LOCATION_ID="${storage_location_id}"
STORAGE_QUOTA="${storage_quota_size}"

if [ -z "$STORAGE_LOCATION_ID" ]; then
    echo "Error: storage.location.id is required"
    exit 1
fi

# Setup storage for container
setup_container_storage() {
    local volume_path="${STORAGE_PATH}/containers/${STORAGE_ROOT}-${CONTAINER_NAME}"

    # Create BTRFS subvolume if not exists
    if ! sudo btrfs subvolume show "$volume_path" &> /dev/null; then
        sudo btrfs subvolume create "$volume_path"
        set_quota "$volume_path" "$STORAGE_QUOTA"
    fi

    # Bind to container
    lxc config device add $CONTAINER_NAME root disk source=$volume_path path=/
}
```

### 6. Configuration File Template

```properties
# ============================================
# Container Configuration
# ============================================
container.name=<name>
container.version=1.0.0
version.tag=latest

# ============================================
# MANDATORY: Storage Configuration (BTRFS)
# ============================================
storage.provider=btrfs
storage.location.id=btrfs_ssd_main
storage.container.root=<container-name>
storage.quota.size=20G
storage.compression=true

# Snapshot configuration
storage.snapshot.enabled=true
storage.snapshot.frequency=daily
storage.snapshot.retain=7

# ============================================
# Service Configuration
# ============================================
service.port=<port>
service.version=<service-version>

# ============================================
# Container Resources
# ============================================
memory_limit=2G
cpu_limit=2

# ============================================
# Network Configuration
# ============================================
network.bridge=lxdbr0
network.ip=auto

# ============================================
# Bind Mounts (optional)
# ============================================
# Format: host_path:container_path,host_path2:container_path2
bind_mounts=

# ============================================
# Environment Variables (optional)
# ============================================
environment_vars=
```

### 7. Storage Requirements File

Create `storage-requirements.conf`:

```properties
# Storage Requirements for <container-name>

# Minimum storage quota
min.quota.size=10G

# Recommended storage quota
recommended.quota.size=20G

# Storage type preference (ssd, sata, nvme)
preferred.storage.type=ssd

# Data directories that need persistence
data.directories=/var/lib/<service>,/var/log/<service>

# Estimated growth rate (GB per month)
growth.rate=2

# Snapshot requirements
snapshot.required=true
snapshot.frequency=daily
snapshot.retention.days=7

# Backup requirements
backup.required=true
backup.frequency=weekly
```

### 8. Version Tracking

Create `versions.conf`:

```properties
# Version history for <container-name>

# Current version
current.version=1.0.0
current.build.date=20240115

# Version history
version.1.0.0.date=20240115
version.1.0.0.changes=Initial release with BTRFS support

# Service versions
service.<service-name>.version=<version>
debian.version=bookworm
```

### 9. README Requirements

Each container README must include:

1. **Storage Requirements** section
2. **Version Information** section
3. **BTRFS Operations** section
4. **Quota Management** section
5. **Snapshot/Restore** procedures

### 10. Validation Checklist

Before container is considered complete:

- [ ] Storage configuration validated
- [ ] BTRFS subvolume creation tested
- [ ] Quota enforcement verified
- [ ] Snapshot functionality tested
- [ ] Version tagging implemented
- [ ] Storage requirements documented
- [ ] README includes storage sections
- [ ] Sample config has all storage params
- [ ] Build script validates storage config
- [ ] Launch script handles storage binding

## Storage Quota Guidelines

### Service Types and Recommended Quotas

| Service Type | Min Quota | Recommended | Max Expected |
|-------------|-----------|-------------|--------------|
| Database | 20G | 50G | 100G |
| Logging/Monitoring | 10G | 30G | 50G |
| Application | 5G | 10G | 20G |
| Cache | 5G | 10G | 15G |
| Web Server | 2G | 5G | 10G |
| Development | 10G | 20G | 50G |

### Specific Service Recommendations

- **Grafana**: 10G (logs and dashboards)
- **Loki**: 30G (log aggregation)
- **Prometheus**: 20G (metrics storage)
- **MySQL/PostgreSQL**: 50G (database)
- **Redis**: 10G (cache)
- **Elasticsearch**: 50G (search index)
- **GitLab**: 100G (code + CI artifacts)

## BTRFS Operations

### Common Operations in Scripts

```bash
# Create subvolume
sudo btrfs subvolume create /path/to/volume

# Set quota (in bytes)
sudo btrfs qgroup limit ${QUOTA_BYTES} /path/to/volume

# Create snapshot
sudo btrfs subvolume snapshot -r /path/to/volume /path/to/snapshot

# Delete subvolume
sudo btrfs subvolume delete /path/to/volume

# Check quota usage
sudo btrfs qgroup show /path/to/volume

# Enable compression
sudo btrfs property set /path/to/volume compression lzo
```

## Migration from Old Containers

To migrate existing containers to new guidelines:

1. Add storage configuration to existing scripts
2. Create storage-requirements.conf
3. Update sample-config.conf with storage params
4. Add version management
5. Test BTRFS subvolume creation
6. Update README with storage sections
7. Validate quota enforcement

## Testing Requirements

Each container must pass:

1. **Storage Creation Test**: Verify BTRFS subvolume created
2. **Quota Test**: Verify quota limits enforced
3. **Snapshot Test**: Create and restore from snapshot
4. **Version Test**: Build with different versions
5. **Launch Test**: Deploy from exported image
6. **Persistence Test**: Data survives container restart

## Security Considerations

1. **Quota Enforcement**: Prevent resource exhaustion
2. **Isolation**: Each container gets separate subvolume
3. **Permissions**: Proper ownership on volumes
4. **Snapshots**: Regular snapshots for recovery
5. **Monitoring**: Track storage usage

## Support Matrix

| Feature | Required | Status |
|---------|----------|--------|
| BTRFS Storage | Yes | Mandatory |
| Quota Management | Yes | Mandatory |
| Versioning | Yes | Mandatory |
| Snapshots | Recommended | Optional |
| Compression | Recommended | Optional |
| Multi-location | No | Future |

## Example Implementation

See `images/lxc/unique-id-generator/` for reference implementation following all guidelines.