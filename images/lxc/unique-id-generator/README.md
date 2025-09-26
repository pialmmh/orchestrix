# Unique ID Generator Container

LXC container for generating unique identifiers with BTRFS storage support.

## Features

- **BTRFS Storage Backend**: Mandatory storage provider with quota management
- **Multiple Storage Locations**: Support for SSD, SATA, NVMe storage tiers
- **Snapshot Support**: Automatic snapshots with configurable retention
- **HTTP API**: Simple REST endpoint for ID generation
- **Resource Limits**: Configurable memory and CPU limits
- **Port Forwarding**: Accessible from host system

## Storage Requirements

This container **requires** BTRFS storage. Before building or launching:

1. Ensure BTRFS is installed on your system
2. Configure storage locations in `/etc/orchestrix/storage-locations.conf`
3. Specify a storage location ID in your configuration

### Example Storage Configuration

Create `/etc/orchestrix/storage-locations.conf`:

```properties
# SSD storage for performance-critical containers
btrfs_ssd_main.path=/home/telcobright/btrfs
btrfs_ssd_main.type=ssd
btrfs_ssd_main.provider=btrfs

# SATA storage for general use
btrfs_sata_archive.path=/sata/btrfs
btrfs_sata_archive.type=sata
btrfs_sata_archive.provider=btrfs
```

## Quick Start

```bash
# Make scripts executable
chmod +x *.sh

# Run with defaults (builds if needed)
./startDefault.sh
```

## Building the Container

```bash
# Build with custom configuration
./buildUniqueIdGenerator.sh my-config.conf

# Or build with sample configuration
cp sample-config.conf build.conf
# Edit build.conf as needed
./buildUniqueIdGenerator.sh build.conf
```

## Launching from Image

```bash
# Create launch configuration
cat > launch.conf << EOF
container.name=uid-gen-prod
image.path=/tmp/unique-id-generator-base.tar.gz
storage.location.id=btrfs_ssd_main
storage.container.root=uid-generator-prod
storage.quota.size=20G
port=7001
EOF

# Launch the container
./launchUniqueIdGenerator.sh launch.conf
```

## Configuration Options

### Required Parameters

- `container.name`: Unique name for the container instance
- `image.path`: Path to the exported container image (.tar.gz)
- `storage.location.id`: Storage location identifier (e.g., btrfs_ssd_main)
- `storage.container.root`: Root directory name within storage location
- `storage.quota.size`: Disk quota (e.g., 10G, 500M, 1T)

### Optional Parameters

- `port`: Service port (default: 7001)
- `memory_limit`: Container memory limit (default: 512MB)
- `cpu_limit`: Number of CPU cores (default: 1)
- `storage.compression`: Enable BTRFS compression (true/false)
- `storage.snapshot.enabled`: Enable automatic snapshots
- `storage.snapshot.frequency`: Snapshot frequency (hourly/daily/weekly/monthly)
- `storage.snapshot.retain`: Number of snapshots to retain
- `bind_mounts`: Additional bind mounts (format: host:container,host2:container2)
- `environment_vars`: Environment variables (format: KEY=value,KEY2=value2)

## API Endpoints

### Generate Unique ID
```bash
curl http://localhost:7001/generate
```

Response:
```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "timestamp": 1704067200,
  "type": "uuid4"
}
```

### Health Check
```bash
curl http://localhost:7001/health
```

Response: `OK`

## Storage Management

### View Storage Usage
```bash
# Check subvolume quota
sudo btrfs qgroup show /path/to/storage/location

# View subvolume information
sudo btrfs subvolume show /path/to/storage/containers/unique-id-generator
```

### Manual Snapshots
```bash
# Create snapshot
sudo btrfs subvolume snapshot -r \
  /path/to/storage/containers/unique-id-generator \
  /path/to/storage/snapshots/unique-id-generator/manual_$(date +%Y%m%d)

# List snapshots
ls /path/to/storage/snapshots/unique-id-generator/
```

### Storage Cleanup
```bash
# Delete old snapshots
sudo btrfs subvolume delete /path/to/storage/snapshots/unique-id-generator/old_snapshot

# Remove container storage (after container deletion)
sudo btrfs subvolume delete /path/to/storage/containers/unique-id-generator
```

## Container Management

### Access Container
```bash
lxc exec <container-name> -- /bin/bash
```

### View Logs
```bash
lxc exec <container-name> -- journalctl -u unique-id-generator -f
```

### Stop Container
```bash
lxc stop <container-name>
```

### Remove Container
```bash
lxc delete <container-name> --force
```

## Troubleshooting

### BTRFS Not Available
```bash
# Install BTRFS tools
sudo apt-get update
sudo apt-get install -y btrfs-progs btrfs-tools

# Load kernel module
sudo modprobe btrfs
```

### Storage Location Not Found
Ensure `/etc/orchestrix/storage-locations.conf` exists and contains valid location definitions.

### Quota Not Working
```bash
# Enable quota on filesystem
sudo btrfs quota enable /path/to/btrfs/mount

# Verify quota is enabled
sudo btrfs qgroup show /path/to/btrfs/mount
```

### Port Already in Use
Change the `port` parameter in your configuration file to an available port.

## Architecture

```
/storage-location/
├── containers/
│   └── unique-id-generator/     # Container root volume
├── snapshots/
│   └── unique-id-generator/     # Container snapshots
│       ├── daily_20240101/
│       └── daily_20240102/
└── backups/                     # Export backups
```

## Security Notes

- Container runs with resource limits (memory, CPU)
- BTRFS quotas prevent storage exhaustion
- Network isolation via LXD bridge
- SSH configured for development (disable in production)

## Development

To modify the unique ID generation logic, edit the generator script:

```bash
lxc exec <container-name> -- vi /opt/unique-id-generator/generator.py
lxc exec <container-name> -- systemctl restart unique-id-generator
```