# Orchestrix LXC Container Scaffolding Standard v2.0

## Overview

This document defines the **UNIFIED STANDARD** for all LXC containers in Orchestrix. Every container MUST follow this structure and include mandatory BTRFS storage support with versioning.

**This is the ONLY scaffolding guideline document. All containers MUST follow this standard.**

## Mandatory Requirements

1. **BTRFS Storage**: All containers MUST use BTRFS with quota management
2. **Versioning**: All containers MUST support version tracking
3. **Build Structure**: Follow the `build/` folder pattern
4. **Generated Outputs**: Use `generated/` folder for outputs
5. **Storage Monitoring**: Include automatic rotation at 80% usage
6. **Prerequisite Checking**: All builds MUST check system prerequisites before starting

## Prerequisite Checking (Mandatory)

All container builds MUST check system prerequisites before starting. The Java automation includes built-in `PrerequisiteChecker` that validates:

### Checked Prerequisites

1. **BTRFS Installation**
   - ✓ `btrfs-progs` package installed
   - ✓ `btrfs` command available
   - ✓ BTRFS version information

2. **BTRFS Kernel Module**
   - ✓ `btrfs` kernel module loaded
   - ✓ Module availability with `lsmod | grep btrfs`

3. **LXC/LXD Installation**
   - ✓ `lxc` command available
   - ✓ LXC version information
   - ✓ LXD service active (`systemctl is-active lxd`)

4. **Network Bridge**
   - ✓ `lxcbr0` or `lxdbr0` bridge configured
   - ✓ Network connectivity for containers

5. **Storage Availability**
   - ✓ Disk space at `/var/lib/lxd`, `/var/lib/lxc`, `/btrfs`
   - ⚠️  Warns if less than 10GB available
   - ✓ Quota availability for BTRFS

### Error Handling

- **Errors** (critical): Build stops immediately with clear remediation steps
- **Warnings** (non-critical): Build continues with informational messages

### Example Output

```
========================================
Checking Prerequisites
========================================
Checking BTRFS...
  ✓ BTRFS tools: /usr/bin/btrfs
    Version: btrfs-progs v6.6.3
  ✓ BTRFS module: LOADED
Checking LXC/LXD...
  ✓ LXC: /usr/bin/lxc
    Client version: 5.0.2
  ✓ LXD service: ACTIVE
Checking Network Bridge...
  ✓ Bridge: lxcbr0 found
Checking Storage...
  ✓ /var/lib/lxd: 45G available (52% used)
========================================
✓ All prerequisite checks PASSED
========================================
```

### Java Implementation

All Java builders MUST include prerequisite checking:

```java
// Step 0: Check prerequisites (MANDATORY)
PrerequisiteChecker checker = new PrerequisiteChecker(device, true);
if (!checker.checkAll()) {
    throw new Exception("Prerequisite checks failed. Fix errors and retry.");
}
```

This is automatically included in all standard builders (Quarkus, Go-ID, Grafana-Loki, etc.).

## Directory Structure

```
images/lxc/[container-name]/
├── build/                          # BUILD SYSTEM (Required)
│   ├── build.sh                   # Main build script
│   └── build.conf                  # Build configuration
│
├── scripts/                        # HELPER SCRIPTS (Required)
│   ├── configure-log-rotation.sh  # Log rotation setup
│   ├── storage-monitor.sh         # Storage monitoring
│   └── helper-functions.sh        # Common functions
│
├── [container-name]-v.[version]/  # VERSIONED OUTPUT (Generated)
│   └── generated/                 # Generated files
│       ├── launch.sh              # Launch script
│       ├── sample.conf            # Sample configuration
│       ├── README-v.X.md          # Version documentation
│       └── *.tar.gz              # Exported container
│
├── README.md                       # Container documentation
├── startDefault.sh                 # Quick start script
├── storage-requirements.conf      # Storage specifications
└── versions.conf                   # Version tracking
```

## Build Configuration Template (build/build.conf)

```bash
#!/bin/bash
# Build configuration for [Container-Name]
# All parameters MUST be defined here

# ============================================
# BASE CONFIGURATION
# ============================================
BASE_IMAGE="images:debian/12"
CONTAINER_VERSION="1"
BUILD_TIMEOUT="1800"
CONTAINER_NAME_PREFIX="container-name"

# ============================================
# STORAGE CONFIGURATION (MANDATORY)
# ============================================
STORAGE_PROVIDER="btrfs"              # Only btrfs currently
STORAGE_LOCATION_ID=""                 # REQUIRED - no default
STORAGE_CONTAINER_ROOT="container-name"
STORAGE_QUOTA_SIZE=""                  # REQUIRED - e.g., "30G"
STORAGE_COMPRESSION="true"
STORAGE_SNAPSHOT_ENABLED="true"
STORAGE_SNAPSHOT_FREQUENCY="daily"
STORAGE_SNAPSHOT_RETAIN="7"

# ============================================
# STORAGE MONITORING (MANDATORY)
# ============================================
STORAGE_MONITOR_ENABLED="true"
STORAGE_ROTATION_THRESHOLD="80"        # Rotate at 80% usage
STORAGE_FORCE_CLEANUP_THRESHOLD="90"   # Aggressive at 90%
STORAGE_CHECK_INTERVAL="300"           # Check every 5 minutes

# ============================================
# LOG ROTATION (MANDATORY)
# ============================================
LOG_ROTATION_ENABLED="true"
LOKI_RETENTION_PERIOD="168h"          # 7 days default
SYSTEM_LOG_ROTATE_DAYS="7"
SYSTEM_LOG_COMPRESS="true"

# ============================================
# RESOURCE LIMITS
# ============================================
MEMORY_LIMIT="2GB"
CPU_LIMIT="2"

# ============================================
# NETWORK CONFIGURATION
# ============================================
NETWORK_BRIDGE="lxdbr0"
NETWORK_IP=""                         # Empty for DHCP

# ============================================
# SERVICE CONFIGURATION
# ============================================
SERVICE_PORTS="8080"                  # Service-specific ports
ENABLE_SSH="true"
SSH_AUTO_ACCEPT="true"

# ============================================
# BUILD OPTIONS
# ============================================
CLEAN_BUILD="true"
EXPORT_CONTAINER="true"
EXPORT_PATH="/tmp"
CREATE_SNAPSHOT="true"
START_SERVICES="true"
```

## How to Build a Container

To build any container following this standard:

1. **Set required storage parameters** in `build/build.conf`:
```bash
STORAGE_LOCATION_ID="btrfs_ssd_main"  # Required
STORAGE_QUOTA_SIZE="30G"               # Required
```

2. **Run the build**:
```bash
cd images/lxc/[container-name]
./build/build.sh                       # Uses build/build.conf
# OR with custom config:
./build/build.sh /path/to/custom.conf
```

3. **Quick start** (for development):
```bash
./startDefault.sh                      # Uses defaults
```

The build process will:
- Validate storage configuration
- Create BTRFS subvolume with quota
- Build and configure the container
- Export to `/tmp/[container]-v.[version]-[timestamp].tar.gz`
- Generate launch scripts in `[container]-v.[version]/generated/`
- Create BTRFS snapshot for backup

## Build Script Structure (build/build.sh)

```bash
#!/bin/bash
# Build script following Orchestrix standards
# Uses Java automation with mandatory prerequisite checking

set -e

# 1. INITIALIZATION
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
CONTAINER_DIR="$(dirname "$SCRIPT_DIR")"
ORCHESTRIX_HOME="/home/mustafa/telcobright-projects/orchestrix"
CONFIG_FILE="${1:-${SCRIPT_DIR}/build.conf}"

# 2. LOAD AND VALIDATE CONFIG
source "$CONFIG_FILE"

# Validate mandatory storage parameters
if [ -z "$STORAGE_LOCATION_ID" ] || [ -z "$STORAGE_QUOTA_SIZE" ]; then
    echo "Error: Storage parameters required"
    exit 1
fi

# 3. RUN JAVA AUTOMATION WITH PREREQUISITE CHECKS
# All container builds MUST use Java automation which includes:
# - Automatic prerequisite checking (BTRFS, LXC, Bridge, Storage)
# - Standardized build process
# - Error handling and validation

cd "$ORCHESTRIX_HOME"

# Compile if needed
if [ ! -d "target/classes" ]; then
    mvn compile -DskipTests
fi

# Run Java automation (automatically checks prerequisites before build)
sudo mvn exec:java \
    -Dexec.mainClass="com.telcobright.orchestrix.automation.api.container.lxc.app.[container].example.[Container]Builder" \
    -Dexec.args="$CONFIG_FILE" \
    -Dexec.classpathScope=compile

# 4. BUILD METADATA (Handled by Java automation)
BUILD_DATE=$(date +%Y%m%d_%H%M%S)
CONTAINER_NAME="${CONTAINER_NAME_PREFIX}-v.${CONTAINER_VERSION}"
GENERATED_DIR="${CONTAINER_DIR}/${CONTAINER_NAME}/generated"

# 4. STORAGE SETUP (MANDATORY)
# - Create BTRFS subvolume
# - Set quota
# - Enable compression if configured

# 5. CONTAINER CREATION
# - Initialize container
# - Configure storage binding
# - Set resource limits
# - Configure network

# 6. SERVICE INSTALLATION
# - Install packages
# - Configure services
# - Setup log rotation
# - Install storage monitor

# 7. EXPORT AND SNAPSHOT
# - Stop container
# - Export to tar.gz
# - Create BTRFS snapshot

# 8. GENERATE OUTPUTS
mkdir -p "${GENERATED_DIR}"
# - Generate launch.sh
# - Generate sample.conf
# - Generate README-v.X.md
```

## Storage Requirements File (storage-requirements.conf)

```properties
# Storage Requirements for [Container-Name]

# Minimum and recommended quotas
min.quota.size=10G
recommended.quota.size=30G
max.quota.size=100G

# Storage type preference
preferred.storage.type=ssd

# Data directories needing persistence
data.directories=/var/lib/service,/var/log/service

# Growth estimation
growth.rate.monthly=5G

# Snapshot requirements
snapshot.required=true
snapshot.frequency=daily
snapshot.retention.days=7

# Backup requirements
backup.required=true
backup.frequency=weekly

# Monitoring thresholds
alert.threshold.warning=70
alert.threshold.critical=85
```

## Version Tracking File (versions.conf)

```properties
# Version tracking for [Container-Name]

# Current version
current.version=1.0.0
current.build.date=20240127

# Version history
version.1.0.0.date=20240127
version.1.0.0.changes=Initial release with BTRFS support

# Service versions
service.primary.version=X.Y.Z
debian.version=bookworm

# Compatibility
min.orchestrix.version=2.0
```

## Generated Output Structure

After build, the following structure is created:

```
[container-name]-v.[version]/
└── generated/
    ├── launch.sh           # Auto-generated launch script
    ├── sample.conf         # Configuration template
    ├── README-v.X.md       # Version-specific docs
    └── exports/
        └── [name]-v.X-[timestamp].tar.gz
```

## Sample Configuration (generated/sample.conf)

```bash
# Auto-generated configuration
CONTAINER_NAME="container-prod"
IMAGE_PATH="/tmp/container-v.1-20240127.tar.gz"
CONTAINER_VERSION="1"

# Storage (MANDATORY)
STORAGE_LOCATION_ID="btrfs_ssd_main"
STORAGE_QUOTA_SIZE="30G"

# Ports
SERVICE_PORT="8080"

# Resources
MEMORY_LIMIT="2GB"
CPU_LIMIT="2"
```

## Naming Conventions

### Container Names
- **Directory**: `[service-name]` (lowercase, hyphens)
- **Container**: `[service-name]-v.[version]`
- **Export**: `[service-name]-v.[version]-[timestamp].tar.gz`

### Scripts
- **Build**: `build/build.sh` (always)
- **Config**: `build/build.conf` (always)
- **Quick Start**: `startDefault.sh` (optional)

### Storage Paths
- **Containers**: `/[storage]/containers/[name]-v[version]`
- **Snapshots**: `/[storage]/snapshots/[name]/v[version]_[date]`
- **Backups**: `/[storage]/backups/[name]/`

## Build Process Workflow

1. **Pre-flight Checks**
   - Validate configuration
   - Check storage availability
   - Verify BTRFS setup

2. **Storage Preparation**
   - Create BTRFS subvolume
   - Set quota limits
   - Enable features (compression, snapshots)

3. **Container Build**
   - Initialize from base image
   - Bind storage volume
   - Install services
   - Configure monitoring

4. **Post-build**
   - Export container
   - Create snapshot
   - Generate launch scripts
   - Update version tracking

## Storage Monitoring

All containers MUST include:

1. **Automatic Rotation**: Trigger at 80% usage
2. **Progressive Cleanup**:
   - 80%: Rotate logs
   - 85%: Delete debug logs
   - 90%: Aggressive cleanup
   - 95%: Emergency mode

3. **Monitoring Service**: `storage-monitor.service`

## Testing Requirements

Each container must pass:

1. **Build Test**: Clean build completes
2. **Storage Test**: Quota enforcement works
3. **Rotation Test**: Logs rotate at threshold
4. **Snapshot Test**: Can create/restore
5. **Launch Test**: Can deploy from export
6. **Service Test**: All services start

## Migration Checklist

For existing containers:

- [ ] Create `build/` folder structure
- [ ] Move build logic to `build/build.sh`
- [ ] Create `build/build.conf` with all parameters
- [ ] Add storage configuration
- [ ] Add storage monitoring
- [ ] Create `storage-requirements.conf`
- [ ] Create `versions.conf`
- [ ] Update to generate outputs in `generated/`
- [ ] Test quota enforcement
- [ ] Test rotation at 80%

## Validation Script

```bash
#!/bin/bash
# Validate container structure

CONTAINER_DIR="$1"

# Check required files
[ -f "$CONTAINER_DIR/build/build.sh" ] || echo "Missing build.sh"
[ -f "$CONTAINER_DIR/build/build.conf" ] || echo "Missing build.conf"
[ -f "$CONTAINER_DIR/storage-requirements.conf" ] || echo "Missing storage requirements"
[ -f "$CONTAINER_DIR/versions.conf" ] || echo "Missing versions"

# Check storage parameters in build.conf
grep -q "STORAGE_LOCATION_ID=" "$CONTAINER_DIR/build/build.conf" || echo "Missing storage location"
grep -q "STORAGE_QUOTA_SIZE=" "$CONTAINER_DIR/build/build.conf" || echo "Missing quota"
grep -q "STORAGE_MONITOR_ENABLED=" "$CONTAINER_DIR/build/build.conf" || echo "Missing monitor"
```

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

- **Grafana-Loki**: 30G (logs and dashboards)
- **Prometheus**: 20G (metrics storage)
- **MySQL/PostgreSQL**: 50G (database)
- **Redis**: 10G (cache)
- **Elasticsearch**: 50G (search index)
- **GitLab**: 100G (code + CI artifacts)
- **FusionPBX**: 20G (telephony data)
- **Development Environment**: 20G (code + tools)

## Common Issues and Solutions

### Storage Location Not Found
```bash
# Check available locations
cat /etc/orchestrix/storage-locations.conf

# Add new location
echo "btrfs_local_main.path=/path/to/btrfs" >> /etc/orchestrix/storage-locations.conf
```

### Quota Not Enforcing
```bash
# Enable quota on filesystem
sudo btrfs quota enable /path/to/btrfs

# Verify quota
sudo btrfs qgroup show /path/to/btrfs
```

### Rotation Not Triggering
```bash
# Check monitor service
systemctl status storage-monitor

# View monitor logs
journalctl -u storage-monitor -f
```

## Compliance

All containers MUST:
1. ✅ Use BTRFS storage with quotas
2. ✅ Include storage monitoring
3. ✅ Follow `build/` folder structure
4. ✅ Generate outputs in `generated/`
5. ✅ Support versioning
6. ✅ Include rotation at 80%
7. ✅ Document storage requirements
8. ✅ Pass all testing requirements

## References

- [Storage Architecture Guidelines](./storage/STORAGE_ARCHITECTURE_GUIDELINES.md)
- [Linux Automation Guidelines](./automation/LINUX_AUTOMATION_GUIDELINES.md)
- [Example: Grafana-Loki](./grafana-loki/)
- [Example: Fusion-PBX](./fusion-pbx/)