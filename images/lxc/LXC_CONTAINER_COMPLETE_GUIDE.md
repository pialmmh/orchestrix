# LXC Container Complete Guide - Orchestrix Project

**AUTHORITATIVE REFERENCE FOR ALL LXC CONTAINER DEVELOPMENT**

This is the single, unified guide for creating, building, and deploying LXC containers in the Orchestrix project. All containers MUST follow this standard.

---

## Table of Contents

1. [Prerequisites and System Setup](#prerequisites-and-system-setup)
2. [Architecture Overview](#architecture-overview)
3. [Directory Structure Standard](#directory-structure-standard)
4. [Naming Conventions](#naming-conventions)
5. [Storage Architecture](#storage-architecture)
6. [Network Configuration](#network-configuration)
7. [Build System](#build-system)
8. [Launch System](#launch-system)
9. [Scaffolding Process](#scaffolding-process)
10. [Validation and Testing](#validation-and-testing)
11. [Common Patterns and Examples](#common-patterns-and-examples)
12. [Troubleshooting](#troubleshooting)

---

## Prerequisites and System Setup

### Required System Components

#### 1. LXD/LXC Installation
```bash
# Install LXD via snap
sudo snap install lxd

# Initialize LXD
sudo lxd init

# Verify installation
lxc version
# Expected: Client version: 6.5+, Server version: 6.5+
```

#### 2. BTRFS Storage (Mandatory)
All containers MUST use BTRFS storage with quotas.

```bash
# Install BTRFS tools
sudo apt-get update
sudo apt-get install -y btrfs-progs

# Verify BTRFS kernel module
lsmod | grep btrfs

# Load module if not loaded
sudo modprobe btrfs

# Create BTRFS filesystem (example on separate partition)
sudo mkfs.btrfs /dev/sdX

# Or create BTRFS on existing directory (loop device)
truncate -s 100G /home/telcobright/btrfs.img
sudo mkfs.btrfs /home/telcobright/btrfs.img
sudo mkdir -p /home/telcobright/btrfs
sudo mount -o loop /home/telcobright/btrfs.img /home/telcobright/btrfs

# Enable BTRFS quotas (REQUIRED)
sudo btrfs quota enable /home/telcobright/btrfs

# Verify BTRFS setup
df -Th | grep btrfs
sudo btrfs filesystem show
sudo btrfs qgroup show /home/telcobright/btrfs
```

#### 3. Bridge Network Configuration

**IMPORTANT**: Orchestrix uses **bridge mode WITHOUT NAT** for production containers (ideal for VoIP/SIP applications). Internet access during build requires temporary NAT configuration.

```bash
# Create bridge without NAT (production mode)
lxc network create lxdbr0 \
  ipv4.address=10.10.199.1/24 \
  ipv4.nat=false \
  ipv6.address=none

# Or modify existing bridge
lxc network set lxdbr0 ipv4.nat=false

# Verify bridge configuration
lxc network show lxdbr0
ip addr show lxdbr0
```

#### 4. Internet Access for Container Builds

Since containers use bridge mode without NAT, they need temporary internet access for package downloads during build:

```bash
# Enable IP forwarding (if not already enabled)
sudo sysctl -w net.ipv4.ip_forward=1

# Make permanent
echo 'net.ipv4.ip_forward=1' | sudo tee -a /etc/sysctl.conf

# Add temporary NAT rule for builds
DEFAULT_IFACE=$(ip route | grep default | awk '{print $5}')
sudo iptables -t nat -A POSTROUTING -s 10.10.199.0/24 -o $DEFAULT_IFACE -j MASQUERADE

# Verify NAT rule
sudo iptables -t nat -L POSTROUTING -n -v

# To remove NAT after builds (optional)
# sudo iptables -t nat -D POSTROUTING -s 10.10.199.0/24 -o $DEFAULT_IFACE -j MASQUERADE
```

#### 5. Storage Location Configuration

Create system-wide storage location registry:

```bash
# Create orchestrix config directory
sudo mkdir -p /etc/orchestrix

# Define storage locations
sudo tee /etc/orchestrix/storage-locations.conf <<EOF
# Storage location definitions for Orchestrix
# Format: location_id.property=value

# Main SSD storage
btrfs_ssd_main.path=/home/telcobright/btrfs
btrfs_ssd_main.type=ssd
btrfs_ssd_main.provider=btrfs

# Example: SATA archive storage
# btrfs_sata_archive.path=/sata/btrfs
# btrfs_sata_archive.type=sata
# btrfs_sata_archive.provider=btrfs

# Example: NVMe database storage
# btrfs_nvme_db.path=/nvme/btrfs
# btrfs_nvme_db.type=nvme
# btrfs_nvme_db.provider=btrfs
EOF
```

### Prerequisite Verification Script

```bash
#!/bin/bash
# Verify all prerequisites for Orchestrix container development

echo "=== Orchestrix Prerequisites Check ==="
echo ""

# Check LXD
if command -v lxc &> /dev/null; then
    echo "✓ LXC installed: $(lxc version --format=compact)"
else
    echo "✗ LXC not installed"
fi

# Check BTRFS tools
if command -v btrfs &> /dev/null; then
    echo "✓ BTRFS tools: $(btrfs --version | head -n1)"
else
    echo "✗ BTRFS tools not installed"
fi

# Check BTRFS module
if lsmod | grep -q btrfs; then
    echo "✓ BTRFS kernel module loaded"
else
    echo "✗ BTRFS kernel module not loaded"
fi

# Check bridge
if lxc network list | grep -q lxdbr0; then
    echo "✓ Bridge lxdbr0 exists"
    BRIDGE_IP=$(lxc network get lxdbr0 ipv4.address 2>/dev/null || echo "Not configured")
    NAT_STATUS=$(lxc network get lxdbr0 ipv4.nat 2>/dev/null || echo "unknown")
    echo "  IP: $BRIDGE_IP, NAT: $NAT_STATUS"
else
    echo "✗ Bridge lxdbr0 not found"
fi

# Check IP forwarding
IP_FORWARD=$(sysctl -n net.ipv4.ip_forward)
if [ "$IP_FORWARD" = "1" ]; then
    echo "✓ IP forwarding enabled"
else
    echo "⚠ IP forwarding disabled (needed for internet access)"
fi

# Check storage locations
if [ -f /etc/orchestrix/storage-locations.conf ]; then
    echo "✓ Storage locations configured: /etc/orchestrix/storage-locations.conf"
    LOCATION_COUNT=$(grep -c "\.path=" /etc/orchestrix/storage-locations.conf)
    echo "  Locations defined: $LOCATION_COUNT"
else
    echo "⚠ Storage locations not configured"
fi

echo ""
echo "=== End Prerequisites Check ==="
```

---

## Architecture Overview

### Two-Phase Container Lifecycle

1. **Build Phase** (One-time)
   - Creates base container image with all software installed
   - Configures services and dependencies
   - Exports container as reusable image
   - Creates versioned artifacts

2. **Launch Phase** (Repeatable)
   - Launches containers from base image
   - Applies runtime configuration
   - Configures networking and storage bindings
   - Starts services

### Key Principles

1. **Configuration Flexibility**: Accept config files from ANY filesystem location
2. **Single Config File**: All parameters in one file
3. **Optional Services**: Containers work without all features configured
4. **Versioning**: Every build creates versioned artifacts
5. **Storage Isolation**: Each container gets dedicated storage volume with quota
6. **Idempotent Builds**: Running build twice produces same result
7. **Atomic Operations**: Either complete success or rollback

---

## Directory Structure Standard

All containers MUST follow this exact structure:

```
/home/mustafa/telcobright-projects/orchestrix/images/lxc/[container-name]/
│
├── build/                              # BUILD SYSTEM (Required)
│   ├── build.sh                        # Main build script
│   └── build.conf                      # Build configuration
│
├── scripts/                            # HELPER SCRIPTS (Optional)
│   ├── server.js                       # Application files
│   ├── package.json                    # Dependencies
│   ├── configure-log-rotation.sh       # Log rotation setup
│   └── storage-monitor.sh              # Storage monitoring
│
├── src/                                # JAVA AUTOMATION (Optional)
│   └── main/java/com/telcobright/orchestrix/images/lxc/[containername]/
│       ├── entities/                   # Configuration models
│       │   ├── BuildConfig.java
│       │   └── RuntimeConfig.java
│       ├── scripts/                    # Inline script classes
│       │   ├── NetworkSetup.java
│       │   ├── ServiceInstaller.java
│       │   └── SystemConfig.java
│       └── [ContainerName]Builder.java # Main orchestrator
│
├── [container-name]-v.[version]/       # VERSIONED OUTPUT (Generated)
│   ├── buildConfig.cnf                 # Build config snapshot
│   ├── README-v.X.Y.Z.md               # Version-specific docs
│   └── generated/                      # Generated artifacts
│       ├── launch.sh                   # Launch script
│       ├── sample.conf                 # Sample configuration
│       ├── version.info                # Version metadata
│       └── [container-name]-v.X.Y.Z.tar.gz
│
├── README.md                           # Container documentation
├── startDefault.sh                     # Quick start script
├── storage-requirements.conf           # Storage specifications
└── versions.conf                       # Version tracking
```

---

## Naming Conventions

### Scripts
- **Build script**: `build/build.sh` (always this path)
- **Build config**: `build/build.conf` (always this path)
- **Launch script**: `generated/launch.sh` (auto-generated)
- **Quick start**: `startDefault.sh` (always this name)

### Container Names
- **Directory**: `[service-name]` (lowercase, hyphens, e.g., `unique-id-generator`)
- **Base image**: `[container-name]-base-v.X.Y.Z` (e.g., `unique-id-generator-base-v.1.0.0`)
- **Build container**: `[container-name]-build-temp` (temporary, deleted after build)
- **Runtime container**: User-specified (e.g., `prod-id-gen-1`)

### Files and Directories
- **Version directory**: `[container-name]-v.X.Y.Z/` (e.g., `unique-id-generator-v.1.0.0/`)
- **Export file**: `[container-name]-v.X.Y.Z-[timestamp].tar.gz`
- **Java package**: `com.telcobright.orchestrix.images.lxc.[containername]` (no hyphens)

### Version Format
Semantic versioning: `MAJOR.MINOR.PATCH`
- **MAJOR**: Breaking changes
- **MINOR**: New features (backward compatible)
- **PATCH**: Bug fixes

---

## Storage Architecture

### Overview

All containers MUST use BTRFS storage with quota management. This ensures:
- Predictable disk usage per container
- Snapshot capability for backups
- Compression for space efficiency
- Isolation between containers

### Storage Location Structure

```
/storage-location/                   # e.g., /home/telcobright/btrfs/
├── containers/                      # Container root volumes
│   ├── mysql/                       # One subvolume per container
│   ├── redis/
│   └── unique-id-generator/
├── snapshots/                       # Snapshot storage
│   └── mysql/
│       ├── 2024-01-15_daily/
│       └── 2024-01-16_daily/
└── backups/                         # Backup exports
    └── mysql/
        └── mysql-backup-20240115.tar.gz
```

### Container Storage Configuration

Every container build configuration MUST include these storage parameters:

```bash
# In build/build.conf

# MANDATORY Storage Configuration
STORAGE_PROVIDER="btrfs"                    # Only btrfs currently supported
STORAGE_LOCATION_ID="btrfs_ssd_main"        # REQUIRED - no default
STORAGE_CONTAINER_ROOT="container-name"     # Container's subvolume name
STORAGE_QUOTA_SIZE="30G"                    # REQUIRED - e.g., "10G", "50G", "100G"

# Optional Storage Features
STORAGE_COMPRESSION="true"                  # Enable BTRFS compression
STORAGE_SNAPSHOT_ENABLED="true"             # Auto-snapshot on build
STORAGE_SNAPSHOT_FREQUENCY="daily"          # Snapshot schedule
STORAGE_SNAPSHOT_RETAIN="7"                 # Days to keep snapshots

# Storage Monitoring (Mandatory)
STORAGE_MONITOR_ENABLED="true"              # Enable monitoring
STORAGE_ROTATION_THRESHOLD="80"             # Rotate logs at 80% usage
STORAGE_FORCE_CLEANUP_THRESHOLD="90"        # Aggressive cleanup at 90%
STORAGE_CHECK_INTERVAL="300"                # Check every 5 minutes
```

### Recommended Quotas by Container Type

| Container Type | Minimum | Recommended | Maximum |
|---------------|---------|-------------|---------|
| Database (MySQL, PostgreSQL) | 20G | 50G | 100G |
| Logging/Monitoring (Grafana, Loki) | 10G | 30G | 50G |
| Cache (Redis, Memcached) | 5G | 10G | 15G |
| Application/Service | 5G | 10G | 20G |
| Web Server (Nginx, Apache) | 2G | 5G | 10G |
| Development Environment | 10G | 20G | 50G |
| ID Generator | 5G | 10G | 15G |

### Storage Monitoring and Rotation

Containers MUST implement automatic log rotation at 80% storage usage:

```bash
# Storage monitoring thresholds
# 80%: Rotate logs, clean temp files
# 85%: Delete debug logs
# 90%: Aggressive cleanup (old backups, cache)
# 95%: Emergency mode (alert, stop non-critical services)
```

### Storage Requirements File Template

Every container should include `storage-requirements.conf`:

```properties
# Storage Requirements for [Container-Name]

# Quota specifications
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

# Monitoring thresholds
alert.threshold.warning=70
alert.threshold.critical=85
```

---

## Network Configuration

### Network Architecture

Orchestrix uses **bridge mode WITHOUT NAT** for production containers:
- Direct IP routing: Container → Bridge → Host → Internet
- No NAT between containers and host
- Ideal for VoIP/SIP applications (FreeSWITCH, Kamailio, Asterisk)
- Containers get real IPs on bridge network (e.g., 10.10.199.0/24)

### Network Configuration Requirements

All containers MUST include these network parameters in `build/build.conf`:

```bash
# Network Configuration
BRIDGE_NAME="lxdbr0"                    # Bridge interface name
CONTAINER_IP="10.10.199.50/24"          # MUST include /24 notation
GATEWAY_IP="10.10.199.1/24"             # MUST include /24 notation
DNS_SERVERS="8.8.8.8 8.8.4.4"           # Space-separated DNS servers
```

### Network Validation Functions

Build scripts MUST include these validation functions (copy inline from `/home/mustafa/telcobright-projects/orchestrix/common/lxc-network-validation-template.sh`):

```bash
# -----------------------------------------------------------------------------
# FUNCTION: validate_network_config
# Validates network configuration meets requirements
# -----------------------------------------------------------------------------
validate_network_config() {
    echo ""
    echo "Validating network configuration..."

    # Check if IP and Gateway are set
    if [ -z "$CONTAINER_IP" ] || [ "$CONTAINER_IP" = "" ]; then
        echo "ERROR: CONTAINER_IP is not set in config file"
        echo "Please set a valid IP with /24 notation (e.g., 10.10.199.50/24)"
        exit 1
    fi

    if [ -z "$GATEWAY_IP" ] || [ "$GATEWAY_IP" = "" ]; then
        echo "ERROR: GATEWAY_IP is not set in config file"
        echo "Please set the gateway IP with /24 notation (e.g., 10.10.199.1/24)"
        exit 1
    fi

    # Check if /24 is included
    if [[ ! "$CONTAINER_IP" =~ /24$ ]]; then
        echo "ERROR: CONTAINER_IP must include /24 subnet mask"
        echo "Current value: $CONTAINER_IP"
        echo "Expected format: 10.10.199.50/24"
        exit 1
    fi

    if [[ ! "$GATEWAY_IP" =~ /24$ ]]; then
        echo "ERROR: GATEWAY_IP must include /24 subnet mask"
        echo "Current value: $GATEWAY_IP"
        echo "Expected format: 10.10.199.1/24"
        exit 1
    fi

    # Extract IP without /24 for validation
    CONTAINER_IP_ONLY="${CONTAINER_IP%/24}"
    GATEWAY_IP_ONLY="${GATEWAY_IP%/24}"

    # Extract first 3 octets for /24 subnet comparison
    CONTAINER_SUBNET=$(echo "$CONTAINER_IP_ONLY" | cut -d. -f1-3)
    GATEWAY_SUBNET=$(echo "$GATEWAY_IP_ONLY" | cut -d. -f1-3)

    # Check if both are in 10.10.199 subnet
    if [ "$CONTAINER_SUBNET" != "10.10.199" ]; then
        echo "ERROR: CONTAINER_IP ($CONTAINER_IP) is not in 10.10.199.0/24 subnet"
        exit 1
    fi

    if [ "$GATEWAY_SUBNET" != "10.10.199" ]; then
        echo "ERROR: GATEWAY_IP ($GATEWAY_IP) is not in 10.10.199.0/24 subnet"
        exit 1
    fi

    # Check if IPs are in same subnet
    if [ "$CONTAINER_SUBNET" != "$GATEWAY_SUBNET" ]; then
        echo "ERROR: CONTAINER_IP and GATEWAY_IP are not in the same subnet"
        echo "Container subnet: ${CONTAINER_SUBNET}.0/24"
        echo "Gateway subnet: ${GATEWAY_SUBNET}.0/24"
        exit 1
    fi

    echo "✓ Network configuration validated"
    echo "  Container IP: $CONTAINER_IP"
    echo "  Gateway IP: $GATEWAY_IP"
    echo "  Bridge: $BRIDGE_NAME"
}

# -----------------------------------------------------------------------------
# FUNCTION: configure_container_network
# Configures bridge networking for the container with static IP
# -----------------------------------------------------------------------------
configure_container_network() {
    local container_name="$1"

    if [ -z "$container_name" ]; then
        echo "ERROR: Container name not provided to configure_container_network"
        exit 1
    fi

    echo "Configuring network (IP: $CONTAINER_IP)..."

    # Remove default network device if exists
    lxc config device remove "$container_name" eth0 2>/dev/null || true

    # Add bridge network device with static IP (use IP without /24 for LXC config)
    lxc config device add "$container_name" eth0 nic \
        nictype=bridged \
        parent="$BRIDGE_NAME" \
        ipv4.address="${CONTAINER_IP%/24}"

    echo "Waiting for container to be ready..."
    sleep 5

    # Configure network inside container
    echo "Configuring container network settings..."

    # Set DNS servers
    lxc exec "$container_name" -- bash -c "echo 'nameserver ${DNS_SERVERS%% *}' > /etc/resolv.conf"
    lxc exec "$container_name" -- bash -c "echo 'nameserver ${DNS_SERVERS##* }' >> /etc/resolv.conf"

    # Add default route (use IP without /24)
    lxc exec "$container_name" -- bash -c "ip route add default via ${GATEWAY_IP%/24} 2>/dev/null || true"
}

# -----------------------------------------------------------------------------
# FUNCTION: test_and_wait_for_internet
# Tests internet connectivity and provides guidance to fix network issues
# Loops until internet connectivity is established
# -----------------------------------------------------------------------------
test_and_wait_for_internet() {
    local container_name="$1"

    if [ -z "$container_name" ]; then
        echo "ERROR: Container name not provided to test_and_wait_for_internet"
        exit 1
    fi

    echo ""
    echo "Testing internet connectivity..."

    while true; do
        if lxc exec "$container_name" -- ping -c 1 google.com &>/dev/null; then
            echo "✓ Internet connectivity confirmed"
            break
        else
            echo ""
            echo "⚠ Container cannot reach the internet!"
            echo ""
            echo "Please check:"
            echo "1. Bridge '$BRIDGE_NAME' exists and is configured"
            echo "2. IP forwarding is enabled: sysctl net.ipv4.ip_forward"
            echo "3. NAT/Masquerading is configured for the bridge"
            echo ""
            echo "Common fixes:"
            echo "  sudo sysctl -w net.ipv4.ip_forward=1"
            echo "  sudo iptables -t nat -A POSTROUTING -s 10.10.199.0/24 -j MASQUERADE"
            echo ""
            echo "To check current settings:"
            echo "  sudo sysctl net.ipv4.ip_forward"
            echo "  sudo iptables -t nat -L POSTROUTING -n -v"
            echo ""
            echo "To make IP forwarding permanent:"
            echo "  echo 'net.ipv4.ip_forward=1' | sudo tee -a /etc/sysctl.conf"
            echo ""
            echo "To check bridge configuration:"
            echo "  lxc network show $BRIDGE_NAME"
            echo ""
            read -p "Press Enter to retry after fixing the network issue..."

            # Retry DNS and routing configuration
            lxc exec "$container_name" -- bash -c "echo 'nameserver 8.8.8.8' > /etc/resolv.conf"
            lxc exec "$container_name" -- bash -c "echo 'nameserver 8.8.4.4' >> /etc/resolv.conf"
            lxc exec "$container_name" -- bash -c "ip route add default via ${GATEWAY_IP%/24} 2>/dev/null || true"
        fi
    done

    echo "✓ Container ready with network access"
}
```

### SSH Configuration for Development Containers

Development containers should auto-accept SSH certificates:

```bash
# In build script, configure SSH client
lxc exec "$BUILD_CONTAINER" -- bash -c 'cat > /etc/ssh/ssh_config.d/99-dev.conf <<EOF
Host *
    StrictHostKeyChecking no
    UserKnownHostsFile /dev/null
    CheckHostIP no
    LogLevel ERROR
EOF'
```

---

## Build System

### Build Configuration Template (build/build.conf)

Every container MUST have a `build/build.conf` file with ALL parameters:

```bash
#!/bin/bash
# Build configuration for [Container-Name]
# Version: X.Y.Z

# ============================================
# BASE CONFIGURATION
# ============================================
BASE_IMAGE="debian-12"                      # Use local cached image (or "images:debian/12")
CONTAINER_VERSION="1.0.0"                   # Semantic version
BUILD_TIMEOUT="1800"                        # Timeout in seconds (30 min)
CONTAINER_NAME_PREFIX="container-name"      # Container name prefix

# ============================================
# STORAGE CONFIGURATION (MANDATORY)
# ============================================
STORAGE_PROVIDER="btrfs"                    # Only btrfs currently
STORAGE_LOCATION_ID="btrfs_ssd_main"        # REQUIRED - no default
STORAGE_CONTAINER_ROOT="container-name"     # Subvolume name
STORAGE_QUOTA_SIZE="30G"                    # REQUIRED - e.g., "10G", "50G"
STORAGE_COMPRESSION="true"                  # Enable compression
STORAGE_SNAPSHOT_ENABLED="true"             # Create snapshot on build
STORAGE_SNAPSHOT_FREQUENCY="daily"          # Snapshot schedule
STORAGE_SNAPSHOT_RETAIN="7"                 # Days to keep

# ============================================
# STORAGE MONITORING (MANDATORY)
# ============================================
STORAGE_MONITOR_ENABLED="true"
STORAGE_ROTATION_THRESHOLD="80"             # Rotate at 80% usage
STORAGE_FORCE_CLEANUP_THRESHOLD="90"        # Aggressive at 90%
STORAGE_CHECK_INTERVAL="300"                # Check every 5 minutes

# ============================================
# LOG ROTATION (MANDATORY)
# ============================================
LOG_ROTATION_ENABLED="true"
LOKI_RETENTION_PERIOD="168h"                # 7 days default
SYSTEM_LOG_ROTATE_DAYS="7"
SYSTEM_LOG_COMPRESS="true"

# ============================================
# RESOURCE LIMITS
# ============================================
MEMORY_LIMIT="2GB"                          # Memory limit
CPU_LIMIT="2"                               # CPU cores

# ============================================
# NETWORK CONFIGURATION
# ============================================
BRIDGE_NAME="lxdbr0"
CONTAINER_IP="10.10.199.50/24"              # MUST include /24
GATEWAY_IP="10.10.199.1/24"                 # MUST include /24
DNS_SERVERS="8.8.8.8 8.8.4.4"               # Space-separated

# ============================================
# SERVICE CONFIGURATION
# ============================================
SERVICE_PORTS="8080"                        # Service-specific ports
ENABLE_SSH="true"                           # Enable SSH server
SSH_AUTO_ACCEPT="true"                      # Auto-accept SSH certs (dev only)

# ============================================
# BUILD OPTIONS
# ============================================
CLEAN_BUILD="true"                          # Clean previous build
EXPORT_CONTAINER="true"                     # Export after build
EXPORT_PATH="/tmp"                          # Export destination
CREATE_SNAPSHOT="true"                      # Snapshot after build
START_SERVICES="true"                       # Start services
CHECK_INTERNET="true"                       # Test internet
FAIL_ON_NO_INTERNET="true"                  # Fail if no internet

# ============================================
# JAVA CONFIGURATION (if applicable)
# ============================================
JAVA_VERSION="21"                           # Java version to install
JAVA_HOME="/usr/lib/jvm/java-1.21.0-openjdk-amd64"
```

### Build Script Template (build/build.sh)

```bash
#!/bin/bash
# Build script for [Container Name]
# Following Orchestrix standards

set -e  # Exit on error

# =============================================================================
# 1. INITIALIZATION
# =============================================================================
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
CONTAINER_DIR="$(dirname "$SCRIPT_DIR")"
CONFIG_FILE="${1:-${SCRIPT_DIR}/build.conf}"

echo "==================================================================="
echo "  Building [Container Name]"
echo "==================================================================="
echo ""

# Load configuration
if [ ! -f "$CONFIG_FILE" ]; then
    echo "Error: Config file not found: $CONFIG_FILE"
    exit 1
fi

source "$CONFIG_FILE"

# =============================================================================
# 2. NETWORK VALIDATION FUNCTIONS
# Copy inline from /home/mustafa/telcobright-projects/orchestrix/common/lxc-network-validation-template.sh
# =============================================================================
validate_network_config() {
    # ... (see Network Configuration section above)
}

configure_container_network() {
    # ... (see Network Configuration section above)
}

test_and_wait_for_internet() {
    # ... (see Network Configuration section above)
}

# =============================================================================
# 3. PREREQUISITE CHECKS
# =============================================================================
check_prerequisites() {
    echo "Checking prerequisites..."

    # Check LXC
    if ! command -v lxc &> /dev/null; then
        echo "Error: LXC not installed"
        exit 1
    fi
    echo "✓ LXC: $(lxc version --format=compact)"

    # Check BTRFS
    if ! command -v btrfs &> /dev/null; then
        echo "Error: BTRFS tools not installed"
        exit 1
    fi
    echo "✓ BTRFS tools: $(btrfs --version | head -n1)"

    # Check BTRFS module
    if ! lsmod | grep -q btrfs; then
        echo "Error: BTRFS kernel module not loaded"
        exit 1
    fi
    echo "✓ BTRFS module: LOADED"

    # Check bridge
    if ! lxc network list | grep -q "$BRIDGE_NAME"; then
        echo "Error: Bridge $BRIDGE_NAME not found"
        exit 1
    fi
    echo "✓ Bridge: $BRIDGE_NAME found"

    # Check storage location
    if [ -z "$STORAGE_LOCATION_ID" ]; then
        echo "Error: STORAGE_LOCATION_ID not set"
        exit 1
    fi

    if [ -z "$STORAGE_QUOTA_SIZE" ]; then
        echo "Error: STORAGE_QUOTA_SIZE not set"
        exit 1
    fi

    # Get storage path from system config
    STORAGE_PATH=$(grep "^${STORAGE_LOCATION_ID}.path=" /etc/orchestrix/storage-locations.conf 2>/dev/null | cut -d= -f2)
    if [ -z "$STORAGE_PATH" ]; then
        echo "Error: Storage location '$STORAGE_LOCATION_ID' not found in /etc/orchestrix/storage-locations.conf"
        exit 1
    fi

    if [ ! -d "$STORAGE_PATH" ]; then
        echo "Error: Storage path does not exist: $STORAGE_PATH"
        exit 1
    fi
    echo "✓ Storage: $STORAGE_PATH"

    # Check available space
    AVAILABLE=$(df -h "$STORAGE_PATH" | awk 'NR==2 {print $4}')
    USAGE=$(df -h "$STORAGE_PATH" | awk 'NR==2 {print $5}')
    echo "✓ Storage: $AVAILABLE available ($USAGE used)"
}

# =============================================================================
# 4. BUILD METADATA
# =============================================================================
setup_metadata() {
    BUILD_DATE=$(date +%Y%m%d_%H%M%S)
    BUILD_CONTAINER="${CONTAINER_NAME_PREFIX}-build-temp"
    BASE_IMAGE_NAME="${CONTAINER_NAME_PREFIX}-base-v.${CONTAINER_VERSION}"
    GENERATED_DIR="${CONTAINER_DIR}/${CONTAINER_NAME_PREFIX}-v.${CONTAINER_VERSION}/generated"
    EXPORT_FILE="${EXPORT_PATH}/${CONTAINER_NAME_PREFIX}-v.${CONTAINER_VERSION}-${BUILD_DATE}.tar.gz"

    echo ""
    echo "Build Configuration:"
    echo "  Container: $BUILD_CONTAINER"
    echo "  Base Image: $BASE_IMAGE_NAME"
    echo "  Version: $CONTAINER_VERSION"
    echo "  Export: $EXPORT_FILE"
    echo ""
}

# =============================================================================
# 5. CLEANUP FUNCTIONS
# =============================================================================
cleanup_existing() {
    echo "Cleaning up existing containers/images..."

    # Delete build container if exists
    if lxc list --format=csv -c n | grep -q "^${BUILD_CONTAINER}$"; then
        echo "Deleting existing build container: $BUILD_CONTAINER"
        lxc delete "$BUILD_CONTAINER" --force
    fi

    # Delete base image if CLEAN_BUILD=true
    if [ "$CLEAN_BUILD" = "true" ]; then
        if lxc image list --format=csv -c l | grep -q "^${BASE_IMAGE_NAME}$"; then
            echo "Deleting existing base image: $BASE_IMAGE_NAME"
            lxc image delete "$BASE_IMAGE_NAME"
        fi
    fi
}

cleanup_on_error() {
    echo ""
    echo "Error occurred. Cleaning up..."
    lxc delete "$BUILD_CONTAINER" --force 2>/dev/null || true
    exit 1
}

trap cleanup_on_error ERR

# =============================================================================
# 6. STORAGE SETUP
# =============================================================================
setup_storage() {
    echo "Setting up BTRFS storage..."

    STORAGE_SUBVOLUME_PATH="${STORAGE_PATH}/containers/${STORAGE_CONTAINER_ROOT}"

    # Create containers directory if not exists
    sudo mkdir -p "${STORAGE_PATH}/containers"

    # Check if subvolume already exists
    if sudo btrfs subvolume show "$STORAGE_SUBVOLUME_PATH" &>/dev/null; then
        if [ "$CLEAN_BUILD" = "true" ]; then
            echo "Deleting existing subvolume: $STORAGE_SUBVOLUME_PATH"
            sudo btrfs subvolume delete "$STORAGE_SUBVOLUME_PATH"
        else
            echo "Warning: Subvolume already exists: $STORAGE_SUBVOLUME_PATH"
            echo "Using existing subvolume (set CLEAN_BUILD=true to recreate)"
            return
        fi
    fi

    # Create BTRFS subvolume
    echo "Creating BTRFS subvolume: $STORAGE_SUBVOLUME_PATH"
    sudo btrfs subvolume create "$STORAGE_SUBVOLUME_PATH"

    # Set quota
    if [ -n "$STORAGE_QUOTA_SIZE" ]; then
        echo "Setting quota: $STORAGE_QUOTA_SIZE"
        sudo btrfs quota enable "$STORAGE_PATH" 2>/dev/null || true
        sudo btrfs qgroup limit "$STORAGE_QUOTA_SIZE" "$STORAGE_SUBVOLUME_PATH"
    fi

    # Enable compression if configured
    if [ "$STORAGE_COMPRESSION" = "true" ]; then
        echo "Enabling compression"
        sudo chattr +c "$STORAGE_SUBVOLUME_PATH"
    fi

    echo "✓ Storage setup complete"
}

# =============================================================================
# 7. CONTAINER CREATION
# =============================================================================
create_build_container() {
    echo "Creating container from image: $BASE_IMAGE"
    lxc init "$BASE_IMAGE" "$BUILD_CONTAINER"

    # Bind storage volume
    echo "Binding storage volume to container..."
    lxc config device add "$BUILD_CONTAINER" storage disk \
        source="$STORAGE_SUBVOLUME_PATH" \
        path="/mnt/storage"

    # Set resource limits
    if [ -n "$MEMORY_LIMIT" ]; then
        lxc config set "$BUILD_CONTAINER" limits.memory="$MEMORY_LIMIT"
    fi

    if [ -n "$CPU_LIMIT" ]; then
        lxc config set "$BUILD_CONTAINER" limits.cpu="$CPU_LIMIT"
    fi

    # Start container
    echo "Starting container..."
    lxc start "$BUILD_CONTAINER"

    echo "Waiting for container to be ready..."
    sleep 5
}

# =============================================================================
# 8. NETWORK CONFIGURATION
# =============================================================================
configure_network() {
    validate_network_config
    configure_container_network "$BUILD_CONTAINER"

    if [ "$CHECK_INTERNET" = "true" ]; then
        test_and_wait_for_internet "$BUILD_CONTAINER"
    fi
}

# =============================================================================
# 9. SERVICE INSTALLATION
# =============================================================================
install_services() {
    echo ""
    echo "Installing services..."

    # Update packages
    echo "Updating package lists..."
    lxc exec "$BUILD_CONTAINER" -- apt-get update

    # Install base packages
    echo "Installing base packages..."
    lxc exec "$BUILD_CONTAINER" -- apt-get install -y \
        curl \
        wget \
        vim \
        net-tools \
        iputils-ping \
        systemd

    # Install service-specific packages here
    # Example:
    # lxc exec "$BUILD_CONTAINER" -- apt-get install -y nodejs npm

    echo "✓ Services installed"
}

# =============================================================================
# 10. SERVICE CONFIGURATION
# =============================================================================
configure_services() {
    echo ""
    echo "Configuring services..."

    # Configure SSH if enabled
    if [ "$ENABLE_SSH" = "true" ]; then
        echo "Configuring SSH..."
        lxc exec "$BUILD_CONTAINER" -- apt-get install -y openssh-server

        if [ "$SSH_AUTO_ACCEPT" = "true" ]; then
            lxc exec "$BUILD_CONTAINER" -- bash -c 'cat > /etc/ssh/ssh_config.d/99-dev.conf <<EOF
Host *
    StrictHostKeyChecking no
    UserKnownHostsFile /dev/null
    CheckHostIP no
    LogLevel ERROR
EOF'
        fi

        lxc exec "$BUILD_CONTAINER" -- systemctl enable ssh
    fi

    # Add service-specific configuration here

    echo "✓ Services configured"
}

# =============================================================================
# 11. LOG ROTATION SETUP
# =============================================================================
setup_log_rotation() {
    if [ "$LOG_ROTATION_ENABLED" != "true" ]; then
        return
    fi

    echo ""
    echo "Setting up log rotation..."

    lxc exec "$BUILD_CONTAINER" -- bash -c 'cat > /etc/logrotate.d/container <<EOF
/var/log/*.log {
    daily
    rotate '$SYSTEM_LOG_ROTATE_DAYS'
    compress
    missingok
    notifempty
    create 0640 root root
}
EOF'

    echo "✓ Log rotation configured"
}

# =============================================================================
# 12. STORAGE MONITORING SETUP
# =============================================================================
setup_storage_monitor() {
    if [ "$STORAGE_MONITOR_ENABLED" != "true" ]; then
        return
    fi

    echo ""
    echo "Setting up storage monitoring..."

    # Create monitoring script
    lxc exec "$BUILD_CONTAINER" -- bash -c 'cat > /usr/local/bin/storage-monitor.sh <<EOF
#!/bin/bash
# Storage monitoring and rotation script

THRESHOLD='$STORAGE_ROTATION_THRESHOLD'
CLEANUP_THRESHOLD='$STORAGE_FORCE_CLEANUP_THRESHOLD'

while true; do
    USAGE=\$(df /mnt/storage | awk "NR==2 {print \$5}" | sed "s/%//")

    if [ "\$USAGE" -ge "\$CLEANUP_THRESHOLD" ]; then
        echo "CRITICAL: Storage at \${USAGE}% - aggressive cleanup"
        # Aggressive cleanup
        find /var/log -name "*.log.*" -mtime +1 -delete
        journalctl --vacuum-time=1d
    elif [ "\$USAGE" -ge "\$THRESHOLD" ]; then
        echo "WARNING: Storage at \${USAGE}% - rotating logs"
        # Rotate logs
        logrotate -f /etc/logrotate.conf
    fi

    sleep '$STORAGE_CHECK_INTERVAL'
done
EOF'

    lxc exec "$BUILD_CONTAINER" -- chmod +x /usr/local/bin/storage-monitor.sh

    # Create systemd service
    lxc exec "$BUILD_CONTAINER" -- bash -c 'cat > /etc/systemd/system/storage-monitor.service <<EOF
[Unit]
Description=Storage Monitoring Service
After=network.target

[Service]
Type=simple
ExecStart=/usr/local/bin/storage-monitor.sh
Restart=always

[Install]
WantedBy=multi-user.target
EOF'

    lxc exec "$BUILD_CONTAINER" -- systemctl enable storage-monitor

    echo "✓ Storage monitoring configured"
}

# =============================================================================
# 13. CONTAINER OPTIMIZATION
# =============================================================================
optimize_container() {
    echo ""
    echo "Optimizing container..."

    # Clean package cache
    lxc exec "$BUILD_CONTAINER" -- apt-get clean

    # Remove unnecessary packages
    lxc exec "$BUILD_CONTAINER" -- apt-get autoremove -y

    # Clear logs
    lxc exec "$BUILD_CONTAINER" -- bash -c "find /var/log -type f -exec truncate -s 0 {} \;"

    # Clear bash history
    lxc exec "$BUILD_CONTAINER" -- bash -c "history -c"

    echo "✓ Container optimized"
}

# =============================================================================
# 14. CREATE BASE IMAGE
# =============================================================================
create_base_image() {
    echo ""
    echo "Stopping container..."
    lxc stop "$BUILD_CONTAINER"

    echo "Creating base image: $BASE_IMAGE_NAME"
    lxc publish "$BUILD_CONTAINER" --alias="$BASE_IMAGE_NAME" \
        description="Base image for ${CONTAINER_NAME_PREFIX} v${CONTAINER_VERSION}"

    echo "✓ Base image created: $BASE_IMAGE_NAME"
}

# =============================================================================
# 15. EXPORT CONTAINER
# =============================================================================
export_container() {
    if [ "$EXPORT_CONTAINER" != "true" ]; then
        return
    fi

    echo ""
    echo "Exporting container image..."

    # Create export directory
    mkdir -p "$EXPORT_PATH"

    # Export image
    lxc image export "$BASE_IMAGE_NAME" "$EXPORT_FILE"

    echo "✓ Container exported: $EXPORT_FILE"
    echo "  Size: $(du -h "$EXPORT_FILE" | cut -f1)"
}

# =============================================================================
# 16. CREATE SNAPSHOT
# =============================================================================
create_snapshot() {
    if [ "$CREATE_SNAPSHOT" != "true" ]; then
        return
    fi

    echo ""
    echo "Creating BTRFS snapshot..."

    SNAPSHOT_DIR="${STORAGE_PATH}/snapshots/${STORAGE_CONTAINER_ROOT}"
    SNAPSHOT_NAME="${CONTAINER_VERSION}_${BUILD_DATE}"

    sudo mkdir -p "$SNAPSHOT_DIR"
    sudo btrfs subvolume snapshot -r "$STORAGE_SUBVOLUME_PATH" "${SNAPSHOT_DIR}/${SNAPSHOT_NAME}"

    echo "✓ Snapshot created: ${SNAPSHOT_DIR}/${SNAPSHOT_NAME}"
}

# =============================================================================
# 17. GENERATE OUTPUT FILES
# =============================================================================
generate_outputs() {
    echo ""
    echo "Generating output files..."

    mkdir -p "$GENERATED_DIR"

    # Generate version info
    cat > "${GENERATED_DIR}/version.info" <<EOF
version=$CONTAINER_VERSION
build_date=$BUILD_DATE
base_image=$BASE_IMAGE_NAME
export_file=$EXPORT_FILE
EOF

    # Generate sample launch config
    cat > "${GENERATED_DIR}/sample.conf" <<EOF
# Launch configuration for ${CONTAINER_NAME_PREFIX}
# Generated on $BUILD_DATE

# Container identification
CONTAINER_NAME="prod-${CONTAINER_NAME_PREFIX}-1"
BASE_IMAGE="$BASE_IMAGE_NAME"
CONTAINER_VERSION="$CONTAINER_VERSION"

# Storage (MANDATORY)
STORAGE_LOCATION_ID="$STORAGE_LOCATION_ID"
STORAGE_QUOTA_SIZE="$STORAGE_QUOTA_SIZE"

# Network
BRIDGE_NAME="$BRIDGE_NAME"
CONTAINER_IP="10.10.199.51/24"
GATEWAY_IP="$GATEWAY_IP"
DNS_SERVERS="$DNS_SERVERS"

# Resources
MEMORY_LIMIT="$MEMORY_LIMIT"
CPU_LIMIT="$CPU_LIMIT"

# Service ports
SERVICE_PORT="$SERVICE_PORTS"
EOF

    # Copy build config
    cp "$CONFIG_FILE" "${CONTAINER_DIR}/${CONTAINER_NAME_PREFIX}-v.${CONTAINER_VERSION}/buildConfig.cnf"

    echo "✓ Output files generated in: $GENERATED_DIR"
}

# =============================================================================
# 18. CLEANUP
# =============================================================================
final_cleanup() {
    echo ""
    echo "Cleaning up build container..."
    lxc delete "$BUILD_CONTAINER" --force
}

# =============================================================================
# 19. SUMMARY
# =============================================================================
print_summary() {
    echo ""
    echo "==================================================================="
    echo "  Build Complete!"
    echo "==================================================================="
    echo ""
    echo "Base Image: $BASE_IMAGE_NAME"
    echo "Version: $CONTAINER_VERSION"
    echo "Export: $EXPORT_FILE"
    echo "Generated Files: $GENERATED_DIR"
    echo ""
    echo "To launch a container:"
    echo "  cd ${CONTAINER_DIR}/${CONTAINER_NAME_PREFIX}-v.${CONTAINER_VERSION}/generated"
    echo "  ./launch.sh sample.conf"
    echo ""
    echo "==================================================================="
}

# =============================================================================
# MAIN EXECUTION
# =============================================================================
main() {
    check_prerequisites
    setup_metadata
    cleanup_existing
    setup_storage
    create_build_container
    configure_network
    install_services
    configure_services
    setup_log_rotation
    setup_storage_monitor
    optimize_container
    create_base_image
    export_container
    create_snapshot
    generate_outputs
    final_cleanup
    print_summary
}

main "$@"
```

---

## Launch System

### Launch Script Template (generated/launch.sh)

This script is auto-generated during build and placed in the `generated/` folder:

```bash
#!/bin/bash
# Auto-generated launch script for [Container Name]
# Version: X.Y.Z

set -e

CONFIG_FILE="$1"

if [ -z "$CONFIG_FILE" ]; then
    echo "Usage: $0 <config-file>"
    echo ""
    echo "Example:"
    echo "  $0 /path/to/myconfig.conf"
    echo "  $0 ./sample.conf"
    exit 1
fi

if [ ! -f "$CONFIG_FILE" ]; then
    echo "Error: Config file not found: $CONFIG_FILE"
    exit 1
fi

# Load configuration
source "$CONFIG_FILE"

# Validate required parameters
if [ -z "$CONTAINER_NAME" ]; then
    echo "Error: CONTAINER_NAME not set in config"
    exit 1
fi

if [ -z "$BASE_IMAGE" ]; then
    echo "Error: BASE_IMAGE not set in config"
    exit 1
fi

echo "Launching container: $CONTAINER_NAME"
echo "Base image: $BASE_IMAGE"

# Check if container already exists
if lxc list --format=csv -c n | grep -q "^${CONTAINER_NAME}$"; then
    echo "Error: Container already exists: $CONTAINER_NAME"
    echo "To recreate, first delete it: lxc delete $CONTAINER_NAME --force"
    exit 1
fi

# Check if base image exists
if ! lxc image list --format=csv -c l | grep -q "^${BASE_IMAGE}$"; then
    echo "Error: Base image not found: $BASE_IMAGE"
    echo ""
    echo "Available images:"
    lxc image list
    exit 1
fi

# Create container from base image
echo "Creating container from base image..."
lxc init "$BASE_IMAGE" "$CONTAINER_NAME"

# Configure network
echo "Configuring network..."
lxc config device remove "$CONTAINER_NAME" eth0 2>/dev/null || true
lxc config device add "$CONTAINER_NAME" eth0 nic \
    nictype=bridged \
    parent="${BRIDGE_NAME}" \
    ipv4.address="${CONTAINER_IP%/24}"

# Set resource limits
if [ -n "$MEMORY_LIMIT" ]; then
    lxc config set "$CONTAINER_NAME" limits.memory="$MEMORY_LIMIT"
fi

if [ -n "$CPU_LIMIT" ]; then
    lxc config set "$CONTAINER_NAME" limits.cpu="$CPU_LIMIT"
fi

# Start container
echo "Starting container..."
lxc start "$CONTAINER_NAME"

echo "Waiting for container to be ready..."
sleep 5

# Configure network inside container
lxc exec "$CONTAINER_NAME" -- bash -c "echo 'nameserver ${DNS_SERVERS%% *}' > /etc/resolv.conf"
lxc exec "$CONTAINER_NAME" -- bash -c "echo 'nameserver ${DNS_SERVERS##* }' >> /etc/resolv.conf"
lxc exec "$CONTAINER_NAME" -- bash -c "ip route add default via ${GATEWAY_IP%/24} 2>/dev/null || true"

# Test connectivity
echo "Testing connectivity..."
if lxc exec "$CONTAINER_NAME" -- ping -c 1 google.com &>/dev/null; then
    echo "✓ Container has internet connectivity"
else
    echo "⚠ Container cannot reach internet (this may be expected in bridge mode)"
fi

echo ""
echo "Container launched successfully!"
echo ""
echo "Container name: $CONTAINER_NAME"
echo "IP address: $CONTAINER_IP"
echo ""
echo "To access:"
echo "  lxc exec $CONTAINER_NAME -- bash"
echo ""
echo "To stop:"
echo "  lxc stop $CONTAINER_NAME"
echo ""
echo "To delete:"
echo "  lxc delete $CONTAINER_NAME --force"
echo ""
```

---

## Scaffolding Process

### Step-by-Step Guide

#### 1. Create Directory Structure

```bash
CONTAINER_NAME="my-service"
cd /home/mustafa/telcobright-projects/orchestrix/images/lxc/

mkdir -p "$CONTAINER_NAME"/{build,scripts}
cd "$CONTAINER_NAME"
```

#### 2. Create Build Configuration

```bash
vim build/build.conf
# Copy template from "Build System" section above
# Customize values for your container
```

#### 3. Create Build Script

```bash
vim build/build.sh
# Copy template from "Build System" section above
# Customize service installation and configuration sections
chmod +x build/build.sh
```

#### 4. Create startDefault.sh

```bash
cat > startDefault.sh <<'EOF'
#!/bin/bash
# Quick start script using default configuration

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

echo "Building container with default configuration..."
"${SCRIPT_DIR}/build/build.sh" "${SCRIPT_DIR}/build/build.conf"
EOF

chmod +x startDefault.sh
```

#### 5. Create Storage Requirements

```bash
cat > storage-requirements.conf <<'EOF'
# Storage Requirements for [Container-Name]

min.quota.size=10G
recommended.quota.size=30G
max.quota.size=100G

preferred.storage.type=ssd

data.directories=/var/lib/service,/var/log/service

growth.rate.monthly=5G

snapshot.required=true
snapshot.frequency=daily
snapshot.retention.days=7

alert.threshold.warning=70
alert.threshold.critical=85
EOF
```

#### 6. Create Version Tracking

```bash
cat > versions.conf <<'EOF'
# Version tracking for [Container-Name]

current.version=1.0.0
current.build.date=$(date +%Y%m%d)

version.1.0.0.date=$(date +%Y%m%d)
version.1.0.0.changes=Initial release

service.primary.version=X.Y.Z
debian.version=bookworm

min.orchestrix.version=2.0
EOF
```

#### 7. Create README

```bash
cat > README.md <<'EOF'
# [Container Name]

Description of the container and its purpose.

## Quick Start

```bash
./startDefault.sh
```

## Custom Configuration

1. Copy sample config:
```bash
cp [container-name]-v.X.Y.Z/generated/sample.conf my-config.conf
```

2. Edit configuration:
```bash
vim my-config.conf
```

3. Launch with custom config:
```bash
[container-name]-v.X.Y.Z/generated/launch.sh my-config.conf
```

## Services

- **Service 1**: Description (port XXXX)
- **Service 2**: Description (port YYYY)

## Storage

- **Minimum**: 10G
- **Recommended**: 30G
- **Data directories**: /var/lib/service, /var/log/service

## Network

- **Default IP range**: 10.10.199.0/24
- **Required ports**: XXXX, YYYY

## Management

### Access container
```bash
lxc exec <container-name> -- bash
```

### View logs
```bash
lxc exec <container-name> -- journalctl -u <service>
```

### Check storage usage
```bash
sudo btrfs qgroup show /path/to/storage
```

## Version

Current version: 1.0.0
Last updated: $(date +%Y-%m-%d)
EOF
```

#### 8. Test Build

```bash
./startDefault.sh
```

---

## Validation and Testing

### Build Validation Checklist

Before considering a container complete:

- [ ] Directory structure follows standard
- [ ] `build/build.sh` exists and is executable
- [ ] `build/build.conf` exists with ALL required parameters
- [ ] Storage configuration includes `STORAGE_LOCATION_ID` and `STORAGE_QUOTA_SIZE`
- [ ] Network configuration includes `/24` notation for IPs
- [ ] Network validation functions are inline in build script
- [ ] Log rotation configured
- [ ] Storage monitoring configured
- [ ] `startDefault.sh` exists and works
- [ ] `storage-requirements.conf` exists
- [ ] `versions.conf` exists
- [ ] `README.md` exists with quick start instructions
- [ ] Build completes without errors
- [ ] Base image created with correct name
- [ ] Container exported to `/tmp/`
- [ ] Generated files created in `generated/` folder
- [ ] Sample launch config generated
- [ ] Version info file created

### Runtime Validation Checklist

- [ ] Container launches successfully
- [ ] Network connectivity works
- [ ] Services start automatically
- [ ] Storage quota enforced
- [ ] Logs rotate at 80% usage
- [ ] Storage monitor service running
- [ ] Container accessible via IP
- [ ] Services accessible on configured ports

### Test Commands

```bash
# Test build
cd /path/to/container
./startDefault.sh

# Verify base image created
lxc image list | grep <container-name>

# Verify export exists
ls -lh /tmp/<container-name>*.tar.gz

# Test launch
cd <container-name>-v.X.Y.Z/generated
./launch.sh sample.conf

# Verify container running
lxc list

# Test network
lxc exec <container-name> -- ping -c 3 google.com

# Check storage quota
sudo btrfs qgroup show /path/to/storage | grep <container-name>

# Verify services
lxc exec <container-name> -- systemctl status <service>

# Check storage monitor
lxc exec <container-name> -- systemctl status storage-monitor

# Cleanup test
lxc delete <container-name> --force
```

---

## Common Patterns and Examples

### Pattern 1: Database Container

```bash
# In build/build.sh - install_services() function

install_services() {
    echo "Installing PostgreSQL..."
    lxc exec "$BUILD_CONTAINER" -- apt-get update
    lxc exec "$BUILD_CONTAINER" -- apt-get install -y postgresql-15

    echo "Configuring PostgreSQL data directory on storage volume..."
    lxc exec "$BUILD_CONTAINER" -- systemctl stop postgresql
    lxc exec "$BUILD_CONTAINER" -- mv /var/lib/postgresql /mnt/storage/
    lxc exec "$BUILD_CONTAINER" -- ln -s /mnt/storage/postgresql /var/lib/postgresql
    lxc exec "$BUILD_CONTAINER" -- chown -R postgres:postgres /mnt/storage/postgresql

    lxc exec "$BUILD_CONTAINER" -- systemctl enable postgresql
}

# Recommended quota: 50G
STORAGE_QUOTA_SIZE="50G"
```

### Pattern 2: Application Container with Systemd Service

```bash
# In build/build.sh - configure_services() function

configure_services() {
    echo "Copying application files..."
    # Copy app files from scripts/ directory
    lxc file push "$CONTAINER_DIR/scripts/app.jar" "$BUILD_CONTAINER/opt/app/"

    echo "Creating application user..."
    lxc exec "$BUILD_CONTAINER" -- useradd -r -s /bin/false appuser

    echo "Creating systemd service..."
    lxc exec "$BUILD_CONTAINER" -- bash -c 'cat > /etc/systemd/system/myapp.service <<EOF
[Unit]
Description=My Application Service
After=network.target

[Service]
Type=simple
User=appuser
WorkingDirectory=/opt/app
ExecStart=/usr/bin/java -jar /opt/app/app.jar
Restart=always
RestartSec=10

[Install]
WantedBy=multi-user.target
EOF'

    lxc exec "$BUILD_CONTAINER" -- systemctl enable myapp
}
```

### Pattern 3: Web Server Container

```bash
# In build/build.sh - install_services() function

install_services() {
    echo "Installing Nginx..."
    lxc exec "$BUILD_CONTAINER" -- apt-get update
    lxc exec "$BUILD_CONTAINER" -- apt-get install -y nginx

    echo "Configuring Nginx..."
    lxc exec "$BUILD_CONTAINER" -- bash -c 'cat > /etc/nginx/sites-available/default <<EOF
server {
    listen 80 default_server;
    server_name _;

    root /mnt/storage/www;
    index index.html;

    location / {
        try_files \$uri \$uri/ =404;
    }
}
EOF'

    # Create web root on storage
    lxc exec "$BUILD_CONTAINER" -- mkdir -p /mnt/storage/www
    lxc exec "$BUILD_CONTAINER" -- chown -R www-data:www-data /mnt/storage/www

    lxc exec "$BUILD_CONTAINER" -- systemctl enable nginx
}

# Recommended quota: 5G
STORAGE_QUOTA_SIZE="5G"
```

### Pattern 4: Node.js Application Container

```bash
# In build/build.sh - install_services() function

install_services() {
    echo "Installing Node.js..."
    lxc exec "$BUILD_CONTAINER" -- apt-get update
    lxc exec "$BUILD_CONTAINER" -- apt-get install -y nodejs npm

    echo "Setting up application..."
    lxc exec "$BUILD_CONTAINER" -- mkdir -p /opt/app

    # Copy application files
    lxc file push "$CONTAINER_DIR/scripts/package.json" "$BUILD_CONTAINER/opt/app/"
    lxc file push "$CONTAINER_DIR/scripts/server.js" "$BUILD_CONTAINER/opt/app/"

    # Install dependencies
    lxc exec "$BUILD_CONTAINER" -- bash -c "cd /opt/app && npm install --production"

    # Move node_modules to storage for persistence
    lxc exec "$BUILD_CONTAINER" -- mv /opt/app/node_modules /mnt/storage/
    lxc exec "$BUILD_CONTAINER" -- ln -s /mnt/storage/node_modules /opt/app/node_modules
}

configure_services() {
    echo "Creating Node.js service..."
    lxc exec "$BUILD_CONTAINER" -- bash -c 'cat > /etc/systemd/system/nodeapp.service <<EOF
[Unit]
Description=Node.js Application
After=network.target

[Service]
Type=simple
User=nobody
WorkingDirectory=/opt/app
ExecStart=/usr/bin/node server.js
Restart=always
Environment=NODE_ENV=production
Environment=PORT=8080

[Install]
WantedBy=multi-user.target
EOF'

    lxc exec "$BUILD_CONTAINER" -- systemctl enable nodeapp
}
```

---

## Troubleshooting

### Common Build Issues

#### Issue 1: Container Creation Timeout

**Symptom:**
```
Failed to execute local command: Command timed out after 30 minutes: sudo lxc init debian-12 ...
```

**Causes:**
1. AppArmor blocking LXD
2. Slow image download
3. LXD daemon issues

**Solutions:**
```bash
# Check kernel logs for AppArmor denials
dmesg | grep -i apparmor | tail -20

# Reload AppArmor if denials found
sudo systemctl reload apparmor
sudo snap restart lxd

# Cache Debian image locally
lxc image copy images:debian/12 local: --alias debian-12

# Update build.conf to use local image
BASE_IMAGE="debian-12"

# Test container creation speed
time lxc init debian-12 test-speed
# Should complete in <5 seconds

# Cleanup test
lxc delete test-speed --force
```

#### Issue 2: No Internet in Container

**Symptom:**
```
⚠ Container cannot reach the internet!
ping: google.com: Name or service not known
```

**Solutions:**
```bash
# Check IP forwarding
sudo sysctl net.ipv4.ip_forward
# Should show: net.ipv4.ip_forward = 1

# Enable if disabled
sudo sysctl -w net.ipv4.ip_forward=1

# Make permanent
echo 'net.ipv4.ip_forward=1' | sudo tee -a /etc/sysctl.conf

# Add NAT rule for build
DEFAULT_IFACE=$(ip route | grep default | awk '{print $5}')
sudo iptables -t nat -A POSTROUTING -s 10.10.199.0/24 -o $DEFAULT_IFACE -j MASQUERADE

# Verify NAT rule
sudo iptables -t nat -L POSTROUTING -n -v | grep 10.10.199

# Check bridge configuration
lxc network show lxdbr0
```

#### Issue 3: Storage Location Not Found

**Symptom:**
```
Error: Storage location 'btrfs_ssd_main' not found in /etc/orchestrix/storage-locations.conf
```

**Solutions:**
```bash
# Create config directory
sudo mkdir -p /etc/orchestrix

# Create storage locations config
sudo tee /etc/orchestrix/storage-locations.conf <<EOF
btrfs_ssd_main.path=/home/telcobright/btrfs
btrfs_ssd_main.type=ssd
btrfs_ssd_main.provider=btrfs
EOF

# Verify file
cat /etc/orchestrix/storage-locations.conf

# Check path exists
ls -la /home/telcobright/btrfs

# If doesn't exist, create BTRFS filesystem
sudo mkdir -p /home/telcobright/btrfs
truncate -s 100G /home/telcobright/btrfs.img
sudo mkfs.btrfs /home/telcobright/btrfs.img
sudo mount -o loop /home/telcobright/btrfs.img /home/telcobright/btrfs
sudo btrfs quota enable /home/telcobright/btrfs
```

#### Issue 4: Quota Not Enforcing

**Symptom:**
```
Container exceeds quota but not limited
```

**Solutions:**
```bash
# Check if quota is enabled
sudo btrfs quota enable /home/telcobright/btrfs

# Rescan quotas
sudo btrfs quota rescan /home/telcobright/btrfs

# Check quota status
sudo btrfs qgroup show /home/telcobright/btrfs

# Manually set quota on subvolume
SUBVOLUME="/home/telcobright/btrfs/containers/my-container"
sudo btrfs qgroup limit 30G "$SUBVOLUME"

# Verify quota applied
sudo btrfs qgroup show "$SUBVOLUME"
```

#### Issue 5: Network IP Must Include /24

**Symptom:**
```
ERROR: CONTAINER_IP must include /24 subnet mask
Current value: 10.10.199.50
Expected format: 10.10.199.50/24
```

**Solution:**
```bash
# Edit build/build.conf
vim build/build.conf

# Change:
CONTAINER_IP="10.10.199.50"
GATEWAY_IP="10.10.199.1"

# To:
CONTAINER_IP="10.10.199.50/24"
GATEWAY_IP="10.10.199.1/24"
```

### Common Runtime Issues

#### Issue 1: Container Won't Start

```bash
# Check container status
lxc list

# View detailed info
lxc info <container-name>

# Check logs
lxc info <container-name> --show-log

# Try starting with verbose output
lxc start <container-name> --debug

# Check for conflicting containers
lxc list | grep <ip-address>
```

#### Issue 2: Service Not Running

```bash
# Access container
lxc exec <container-name> -- bash

# Check service status
systemctl status <service-name>

# View service logs
journalctl -u <service-name> -n 50

# Restart service
systemctl restart <service-name>

# Check if service is enabled
systemctl is-enabled <service-name>
```

#### Issue 3: Storage Monitor Not Working

```bash
# Check if service exists
lxc exec <container-name> -- systemctl status storage-monitor

# View monitor logs
lxc exec <container-name> -- journalctl -u storage-monitor -f

# Manually check storage usage
lxc exec <container-name> -- df -h /mnt/storage

# Test rotation script manually
lxc exec <container-name> -- /usr/local/bin/storage-monitor.sh
```

### Debug Mode

To enable detailed logging during build:

```bash
# In build/build.conf
VERBOSE_OUTPUT="true"

# Run build with bash debug mode
bash -x ./build/build.sh

# Or add to build script
set -x  # Enable debug
set +x  # Disable debug
```

---

## Quick Reference

### Build a Container

```bash
cd /home/mustafa/telcobright-projects/orchestrix/images/lxc/<container-name>
./startDefault.sh
```

### Launch a Container

```bash
cd <container-name>-v.X.Y.Z/generated
./launch.sh sample.conf
```

### Manage Containers

```bash
# List containers
lxc list

# Access container
lxc exec <name> -- bash

# Stop container
lxc stop <name>

# Delete container
lxc delete <name> --force

# View container info
lxc info <name>
```

### Manage Images

```bash
# List images
lxc image list

# Delete image
lxc image delete <alias>

# Export image
lxc image export <alias> /tmp/
```

### Manage Storage

```bash
# Show BTRFS filesystems
sudo btrfs filesystem show

# Show quotas
sudo btrfs qgroup show /path/to/btrfs

# Show subvolumes
sudo btrfs subvolume list /path/to/btrfs

# Check usage
sudo btrfs filesystem usage /path/to/btrfs
```

### Manage Network

```bash
# List networks
lxc network list

# Show network config
lxc network show lxdbr0

# Check IP forwarding
sysctl net.ipv4.ip_forward

# View NAT rules
sudo iptables -t nat -L -n -v
```

---

## Appendix: File Templates Location

All templates and reference files are located in:

- **Main guide**: `/home/mustafa/telcobright-projects/orchestrix/images/lxc/LXC_CONTAINER_COMPLETE_GUIDE.md` (this file)
- **Network validation template**: `/home/mustafa/telcobright-projects/orchestrix/common/lxc-network-validation-template.sh`
- **Storage architecture**: `/home/mustafa/telcobright-projects/orchestrix/src/main/java/com/telcobright/orchestrix/automation/storage/STORAGE_ARCHITECTURE_GUIDELINES.md`
- **Example container**: `/home/mustafa/telcobright-projects/orchestrix/images/lxc/dev-env/`

---

**END OF GUIDE**

*Last Updated: $(date +%Y-%m-%d)*
*Version: 2.0*
*Orchestrix Project - TelcoBright*
