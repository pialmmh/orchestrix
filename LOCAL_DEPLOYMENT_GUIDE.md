# Local BTRFS Container Deployment Guide for Ubuntu 24.04

This guide walks you through setting up and running BTRFS-backed containers on your local Ubuntu 24.04 machine.

## Quick Start

```bash
# 1. Setup BTRFS and prerequisites
sudo ./scripts/setup-btrfs-local.sh

# 2. Run interactive test
./scripts/test-local-deployment.sh
```

## Prerequisites

Your system needs:
- Ubuntu 24.04 LTS (Noble Numbat)
- Sudo privileges
- At least 50GB free disk space
- SSH service installed

## Step-by-Step Setup

### 1. Install and Configure BTRFS

Run the automated setup script:

```bash
sudo ./scripts/setup-btrfs-local.sh
```

This script will:
- ✅ Install BTRFS tools
- ✅ Create storage directory structure at `~/telcobright/btrfs`
- ✅ Configure storage locations in `/etc/orchestrix/storage-locations.conf`
- ✅ Install and initialize LXD
- ✅ Setup network bridge (lxdbr0)
- ✅ Enable IP forwarding
- ✅ Create a 50GB BTRFS loop device (if not on BTRFS filesystem)

### 2. Setup SSH Access

The test runner uses SSH to connect to localhost:

```bash
# Generate SSH key if needed
ssh-keygen -t rsa -N "" -f ~/.ssh/id_rsa

# Add to authorized_keys
cat ~/.ssh/id_rsa.pub >> ~/.ssh/authorized_keys
chmod 600 ~/.ssh/authorized_keys

# Test connection
ssh localhost whoami
```

### 3. Compile the Project

```bash
# If using Maven
mvn compile

# If using Gradle
gradle compileJava
```

### 4. Run Test Deployment

Use the interactive test script:

```bash
./scripts/test-local-deployment.sh
```

Select options:
1. **Check prerequisites** - Verify all requirements
2. **Setup prerequisites** - Auto-configure missing components
3. **Run test deployment** - Deploy a test container
4. **Deploy unique-id-generator** - Deploy the sample service

## Manual Deployment

### Using Configuration Files

```bash
# Deploy with default test configuration
java -cp target/classes:src/main/resources:src/test/resources \
  com.telcobright.orchestrix.testrunner.storage.LocalBtrfsDeploymentRunner \
  src/test/resources/testrunner/local-btrfs-deployment.properties

# Deploy unique-id-generator
java -cp target/classes:src/main/resources:src/test/resources \
  com.telcobright.orchestrix.testrunner.storage.LocalBtrfsDeploymentRunner \
  src/test/resources/testrunner/unique-id-local-deployment.properties
```

### Custom Configuration

Create your own configuration file:

```properties
# my-container.properties
ssh.host=localhost
ssh.username=mustafa
use_sudo=true

storage.location.id=btrfs_local_main
storage.container.root=my-app
storage.quota.size=15G

container.name=my-app-container
container.image.path=/path/to/image.tar.gz
container.limits.memory=2G
container.limits.cpu=2

lxc.prepare_prerequisites=false
```

Deploy:
```bash
java -cp target/classes:src/main/resources \
  com.telcobright.orchestrix.testrunner.storage.LocalBtrfsDeploymentRunner \
  my-container.properties
```

## Storage Structure

After setup, your storage structure will be:

```
~/telcobright/btrfs/
├── containers/       # Container root volumes
│   ├── test-container/
│   └── unique-id-generator/
├── snapshots/       # BTRFS snapshots
│   └── test-container/
└── backups/        # Container exports
```

## Managing Containers

### View Containers

```bash
# List LXC containers
lxc list

# View BTRFS volumes
sudo btrfs subvolume list ~/telcobright/btrfs

# Check storage usage
sudo btrfs qgroup show ~/telcobright/btrfs
```

### Access Container

```bash
# Execute shell in container
lxc exec <container-name> -- /bin/bash

# View container info
lxc info <container-name>
```

### Stop and Remove

```bash
# Stop container
lxc stop <container-name>

# Remove container
lxc delete <container-name> --force

# Remove BTRFS volume
sudo btrfs subvolume delete ~/telcobright/btrfs/containers/<container-name>
```

## BTRFS Operations

### Create Snapshot

```bash
sudo btrfs subvolume snapshot -r \
  ~/telcobright/btrfs/containers/my-app \
  ~/telcobright/btrfs/snapshots/my-app/backup-$(date +%Y%m%d)
```

### Restore from Snapshot

```bash
# Delete current volume
sudo btrfs subvolume delete ~/telcobright/btrfs/containers/my-app

# Restore from snapshot
sudo btrfs subvolume snapshot \
  ~/telcobright/btrfs/snapshots/my-app/backup-20240115 \
  ~/telcobright/btrfs/containers/my-app
```

### Check Quota Usage

```bash
sudo btrfs qgroup show ~/telcobright/btrfs
```

## Troubleshooting

### BTRFS Module Not Loading

```bash
sudo modprobe btrfs
echo "btrfs" | sudo tee /etc/modules-load.d/btrfs.conf
```

### SSH Connection Failed

```bash
# Ensure SSH service is running
sudo systemctl start ssh

# Check SSH key permissions
chmod 700 ~/.ssh
chmod 600 ~/.ssh/authorized_keys
chmod 600 ~/.ssh/id_rsa
```

### LXD Not Initialized

```bash
sudo lxd init --auto
```

### Container Network Issues

```bash
# Check bridge
lxc network show lxdbr0

# Recreate if needed
lxc network delete lxdbr0
lxc network create lxdbr0 ipv4.address=10.0.3.1/24 ipv4.nat=true
```

### Storage Volume Errors

```bash
# Check filesystem
df -T ~/telcobright/btrfs

# If using loop device, check mount
mount | grep btrfs

# Remount if needed
sudo mount -o loop ~/telcobright/btrfs.img ~/telcobright/btrfs
```

## Testing Components Individually

### Test BTRFS Installation

```bash
# Check version
btrfs --version

# Check kernel module
lsmod | grep btrfs

# Test subvolume creation
sudo btrfs subvolume create ~/telcobright/btrfs/test
sudo btrfs subvolume delete ~/telcobright/btrfs/test
```

### Test Storage Provider

```java
// Interactive test
./scripts/test-storage-deployment.sh -i
```

### Test Container Deployment

```bash
# Simple LXC test
lxc launch ubuntu:22.04 test-manual
lxc exec test-manual -- echo "Hello from container"
lxc delete test-manual --force
```

## Configuration Files

### Storage Locations
- `/etc/orchestrix/storage-locations.conf` - System-wide storage configuration

### Test Configurations
- `src/test/resources/testrunner/local-btrfs-deployment.properties` - Basic test deployment
- `src/test/resources/testrunner/unique-id-local-deployment.properties` - Unique ID generator

## Next Steps

1. **Deploy Your Container**: Create a configuration file for your container
2. **Setup Automation**: Schedule automated deployments and backups
3. **Monitor Resources**: Set up monitoring for storage and container health
4. **Scale Out**: Deploy to remote servers using the same automation

## Support

If you encounter issues:

1. Check prerequisites: `./scripts/test-local-deployment.sh` → Option 1
2. Review logs: `journalctl -xe`
3. Verify BTRFS: `sudo btrfs filesystem show`
4. Check LXD: `lxc version`

## Security Notes

- SSH keys are used for localhost authentication
- Containers run with configured resource limits
- BTRFS quotas prevent storage exhaustion
- Network isolation via LXD bridge

## Performance Tips

- Enable BTRFS compression for text-heavy workloads
- Use SSD storage for better performance
- Set appropriate quotas to prevent overcommit
- Regular snapshots for quick recovery