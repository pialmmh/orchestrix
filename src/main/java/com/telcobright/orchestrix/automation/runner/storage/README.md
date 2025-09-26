# Storage Deployment Automation Runner

## Overview

The Storage Deployment Automation Runner provides config-driven execution of container deployments with BTRFS storage management. It reuses SSH automation classes to:

1. Connect to remote servers via SSH
2. Install and configure BTRFS storage
3. Create storage volumes with quotas
4. Deploy containers (LXC, Docker, Podman)
5. Configure storage mounts for containers
6. Manage snapshots and backups

## Architecture

```
BtrfsContainerDeploymentRunner
├── SSH Connection (SshDevice)
├── Storage Provider (BtrfsStorageProvider)
│   ├── BtrfsInstallAutomation
│   └── Volume Management
├── Container Deployment
│   ├── LxcContainerDeployment
│   ├── LxcPrerequisitePreparer
│   └── LxcContainerBtrfsMountAutomation
└── Configuration (Properties file)
```

## Configuration

### Required Configuration

```properties
# SSH Connection
ssh.host=192.168.1.100
ssh.username=admin
ssh.password=password  # or ssh.key_path=/path/to/key

# Storage
storage.location.id=btrfs_ssd_main
storage.container.root=myapp
storage.quota.size=20G

# Container
container.name=myapp-container
container.image.path=/tmp/container.tar.gz
```

### Full Configuration Example

See `src/main/resources/runner/storage/btrfs-container-deployment.properties`

## Usage

### 1. Command Line Execution

```bash
# Run with configuration file
java -cp orchestrix.jar \
  com.telcobright.orchestrix.automation.runner.storage.BtrfsContainerDeploymentRunner \
  deployment.properties

# Or use the test script
./scripts/test-storage-deployment.sh deployment.properties
```

### 2. Programmatic Execution

```java
// Create runner with config file
BtrfsContainerDeploymentRunner runner =
    new BtrfsContainerDeploymentRunner("config.properties");

// Execute deployment
boolean success = runner.execute();
```

### 3. Interactive Testing

```bash
# Run interactive test mode
./scripts/test-storage-deployment.sh -i

# This allows you to:
# - Test individual components
# - Create storage volumes
# - Deploy containers
# - Manage snapshots
```

## Execution Flow

1. **Connect to Device**: Establishes SSH connection using provided credentials
2. **Setup Storage Provider**: Installs BTRFS if needed, verifies health
3. **Prepare Storage Volume**: Creates BTRFS subvolume with quota
4. **Setup Container Deployment**: Configures container with image and settings
5. **Deploy Container**: Creates and starts the container
6. **Configure Storage Mount**: Binds BTRFS volume to container
7. **Verify Deployment**: Checks container and storage status

## Storage Locations

Before running, ensure storage locations are configured on the target system:

```bash
# Create storage locations config on target
sudo mkdir -p /etc/orchestrix
sudo vi /etc/orchestrix/storage-locations.conf
```

Example configuration:
```properties
btrfs_ssd_main.path=/home/telcobright/btrfs
btrfs_ssd_main.type=ssd
btrfs_ssd_main.provider=btrfs
```

## Testing

### Unit Test Individual Components

```java
// Test BTRFS installation
BtrfsInstallAutomation installer = new BtrfsInstallAutomation(
    LinuxDistribution.DEBIAN, true);
installer.execute(device);

// Test storage volume creation
BtrfsStorageProvider provider = new BtrfsStorageProvider(true);
StorageVolume volume = provider.createVolume(device, config);

// Test container deployment
LxcContainerDeployment deployment = new LxcContainerDeployment(
    containerConfig, true);
deployment.execute(device);
```

### Integration Test

```bash
# Create test configuration
cat > test-deployment.properties << EOF
ssh.host=localhost
ssh.username=test
ssh.password=test
storage.location.id=btrfs_ssd_main
storage.container.root=test-container
storage.quota.size=5G
container.name=test-container
container.image.path=/tmp/test.tar.gz
EOF

# Run deployment
./scripts/test-storage-deployment.sh test-deployment.properties
```

## Monitoring and Verification

### Check Deployment Status

```java
Map<String, String> status = runner.getDeploymentStatus();
System.out.println("Container: " + status.get("container"));
System.out.println("State: " + status.get("state"));
System.out.println("Storage: " + status.get("volume_path"));
```

### Verify Storage

```bash
# On target system
sudo btrfs subvolume list /path/to/storage
sudo btrfs qgroup show /path/to/storage
```

### Container Status

```bash
# LXC containers
lxc list
lxc info <container-name>

# Docker containers
docker ps
docker inspect <container-name>
```

## Error Handling

The runner provides detailed error messages at each stage:

1. **Connection Errors**: Check SSH credentials and network
2. **Storage Errors**: Verify BTRFS installation and disk space
3. **Container Errors**: Check image availability and resources
4. **Permission Errors**: Ensure sudo is configured if needed

## Advanced Features

### Snapshots

```java
// Create snapshot
provider.createSnapshot(device, volumePath, "backup-" + timestamp);

// List snapshots
List<String> snapshots = provider.listSnapshots(device, volumePath);

// Restore from snapshot
provider.restoreSnapshot(device, volumePath, snapshotName);
```

### Prerequisites Preparation

For LXC deployments, enable automatic prerequisite preparation:

```properties
lxc.prepare_prerequisites=true
lxc.bridge.network=10.0.3.0/24
lxc.bridge.name=lxdbr0
```

This will automatically:
- Install LXD
- Configure network bridge
- Enable IP forwarding
- Setup NAT masquerading

## Troubleshooting

### BTRFS Not Installing
```bash
# Check package manager
which apt-get || which yum

# Install manually
sudo apt-get update
sudo apt-get install -y btrfs-progs
```

### Storage Location Not Found
```bash
# Create storage location config
sudo mkdir -p /etc/orchestrix
echo "btrfs_ssd_main.path=/home/btrfs" | \
  sudo tee -a /etc/orchestrix/storage-locations.conf
```

### Container Deployment Fails
```bash
# Check LXD status
lxc version
sudo systemctl status lxd

# Initialize LXD if needed
sudo lxd init --auto
```

## Security Considerations

1. **SSH Keys**: Prefer key-based authentication over passwords
2. **Sudo**: Use specific sudo rules instead of full access
3. **Quotas**: Always set storage quotas to prevent exhaustion
4. **Network**: Use private bridges for container networking

## Future Enhancements

- Docker deployment support
- Podman deployment support
- LVM storage provider
- ZFS storage provider
- Ceph distributed storage
- Automated backup scheduling
- Multi-node deployment support