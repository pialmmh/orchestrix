# Grafana-Loki Deployment Configurations

This directory contains deployment configurations for Grafana-Loki container following the **Orchestrix Container Scaffolding Standard v2.0**.

## Structure

```
deployment/grafana-loki/
├── README.md                        ← This file
├── production.yaml.example          ← Template configuration
├── v1.0/                            ← Version 1.0 configs
│   ├── production.yaml
│   ├── staging.yaml
│   └── README.md
└── v2.0/                            ← Future version configs
    └── ...
```

## Quick Start

### 1. Navigate to Orchestrix Project Root

```bash
cd /home/mustafa/telcobright-projects/orchestrix
```

### 2. Run Deployment

```bash
# Deploy with default config (v1.0/production.yaml)
./scripts/deploy-grafana-loki.sh

# Deploy with specific version
./scripts/deploy-grafana-loki.sh --version v1.0

# Deploy with custom config
./scripts/deploy-grafana-loki.sh deployment/grafana-loki/v1.0/staging.yaml

# Dry run
./scripts/deploy-grafana-loki.sh --dry-run
```

## Creating New Configuration

### For Current Version (v1.0)

```bash
cd /home/mustafa/telcobright-projects/orchestrix

# Copy example template
cp deployment/grafana-loki/production.yaml.example \
   deployment/grafana-loki/v1.0/my-config.yaml

# Edit configuration
vi deployment/grafana-loki/v1.0/my-config.yaml

# Deploy
./scripts/deploy-grafana-loki.sh deployment/grafana-loki/v1.0/my-config.yaml
```

### For New Version (v2.0)

```bash
cd /home/mustafa/telcobright-projects/orchestrix

# Create new version directory
mkdir -p deployment/grafana-loki/v2.0

# Copy from previous version
cp deployment/grafana-loki/v1.0/production.yaml \
   deployment/grafana-loki/v2.0/production.yaml

# Update version-specific settings
vi deployment/grafana-loki/v2.0/production.yaml

# Deploy
./scripts/deploy-grafana-loki.sh --version v2.0
```

## Configuration Reference

### Required Fields

All configurations **MUST** include these fields per scaffolding standard:

```yaml
# SSH connection
ssh:
  host: <server-ip>
  username: <ssh-user>
  password: <password>  # or use keyPath

# Container identity
container:
  name: <container-name>
  version: <version>
  baseImage: images:debian/12

# BTRFS storage (MANDATORY)
storage:
  provider: btrfs
  path: <mount-path>
  quota: <size>
```

### Storage Configuration

**BTRFS is MANDATORY** per Container Scaffolding Standard v2.0:

```yaml
storage:
  provider: btrfs                    # Required
  quota: 30G                         # Required

  # File-based (loop device) - Recommended for testing
  useLoopDevice: true
  loopDeviceSize: 50G
  loopDeviceImage: /opt/btrfs-logger.img

  # Partition-based - Recommended for production
  # useLoopDevice: false
  # path: /mnt/btrfs-partition
```

### Storage Monitoring

Per scaffolding standard, storage monitoring is recommended:

```yaml
storage:
  monitoring:
    enabled: true
    rotationThreshold: 80          # Rotate at 80% usage
    forceCleanupThreshold: 90      # Force cleanup at 90%
    checkInterval: 300             # Check every 5 minutes
```

## Version Management

### Version Compatibility

| Config Version | Container Version | Grafana | Loki | Notes |
|----------------|-------------------|---------|------|-------|
| v1.0 | 1.0.0 | 10.2.3 | 2.9.4 | Current stable |
| v2.0 | 2.0.0 | TBD | TBD | Future |

### Upgrading Versions

```bash
# Deploy v1.0
./scripts/deploy-grafana-loki.sh --version v1.0

# Later, upgrade to v2.0
./scripts/deploy-grafana-loki.sh --version v2.0
```

## Directory Locations

This configuration references the following Orchestrix directories:

```
/home/mustafa/telcobright-projects/orchestrix/
├── scripts/
│   └── deploy-grafana-loki.sh          ← Deployment script
├── deployment/grafana-loki/
│   ├── v1.0/                           ← Version-specific configs
│   └── production.yaml.example         ← Template
├── automation/                         ← Java automation framework
│   └── src/main/java/.../grafanaloki/
└── images/containers/lxc/grafana-loki/            ← Container definition
    ├── build/                          ← Build scripts
    ├── grafana-loki-v.1/              ← Version 1.0 artifacts
    └── README.md                       ← Container docs
```

## Scaffolding Standards Compliance

This deployment configuration follows:

✓ **Container Scaffolding Standard v2.0**
- BTRFS mandatory storage
- Version management
- Absolute paths (not relative)
- Prerequisite checking
- Storage monitoring at 80% threshold

✓ **Orchestrix Automation Guidelines**
- Java-based automation
- SSH automation
- Modular deployment steps
- Rollback support

✓ **Configuration Structure**
- Version-based organization
- Template-driven configs
- Environment separation (prod/staging)

## Deployment Prerequisites

The deployment script will auto-install/configure:
- ✓ LXD/LXC (if not present)
- ✓ BTRFS tools (if not present)
- ✓ BTRFS filesystem (per config)
- ✓ LXD bridge network

Required on deployment machine:
- Java 17 or 21
- Maven 3.6+
- SSH access to target server

## Troubleshooting

### Configuration Not Found

```bash
# Error: Configuration file not found

# Solution: Create from template
cp deployment/grafana-loki/production.yaml.example \
   deployment/grafana-loki/v1.0/production.yaml
```

### SSH Connection Failed

```bash
# Verify server is reachable
ping 10.255.246.175

# Test SSH manually
ssh bdcom@10.255.246.175
```

### Storage Setup Failed

```bash
# Check disk space on target server
ssh bdcom@10.255.246.175 df -h

# Reduce storage quota if needed
# Edit config: storage.quota and storage.loopDeviceSize
```

## Security Best Practices

⚠️ **Important:**

1. **Never commit passwords to git**
   ```bash
   # .gitignore already includes:
   deployment/*/v*/production.yaml
   deployment/*/v*/staging.yaml
   ```

2. **Use SSH keys in production**
   ```yaml
   ssh:
     useKey: true
     keyPath: ~/.ssh/production-key
   ```

3. **Restrict file permissions**
   ```bash
   chmod 600 deployment/grafana-loki/v1.0/production.yaml
   ```

4. **Change Grafana password**
   After deployment, immediately change the default admin/admin password.

## Support

- **Container Documentation**: `images/containers/lxc/grafana-loki/README.md`
- **Scaffolding Guide**: `images/containers/lxc/CONTAINER_SCAFFOLDING_STANDARD.md`
- **Automation Guide**: `AUTOMATION_GUIDELINES.md`
- **Deployment Design**: `/tmp/shared-instruction/grafana-loki-one-click-deployment-design.md`

## Related Documentation

- Container build: `images/containers/lxc/grafana-loki/build/build.sh`
- Java automation: `automation/src/main/java/com/telcobright/orchestrix/automation/`
- Shared instructions: `/tmp/shared-instruction/`
