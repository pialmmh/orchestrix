# AI Scaffolding Workflow Guide

**This document defines the mandatory workflow when AI is asked to scaffold LXC containers.**

## Workflow Steps

### Step 1: AI Prepares build.conf Template

When user asks to scaffold a container, AI MUST:

1. Identify container type (Debian Full-Stack or Alpine Binary)
2. Create directory structure: `images/lxc/[container-name]/build/`
3. Generate `build.conf` template
4. Present template to user with clear instructions
5. **WAIT** for user to fill configuration

**Do NOT proceed until user confirms configuration is ready.**

---

## build.conf Templates

### Template for Alpine Binary Container

```bash
# Build Configuration for [Service-Name] Alpine Container
# Version: 1
#
# INSTRUCTIONS:
# 1. Set BINARY_SOURCE to the absolute path of your standalone binary
# 2. Set SERVICE_PORT to the port your service listens on
# 3. Review CONTAINER_NAME and adjust if needed
# 4. Set SHARD_ID and TOTAL_SHARDS if using sharding
# 5. Save this file and notify AI when ready

# ============================================
# CONTAINER SETTINGS
# ============================================
CONTAINER_NAME="service-name"              # TODO: Confirm or change
VERSION="1"
BUILD_CONTAINER="${CONTAINER_NAME}-build-temp"
SERVICE_NAME="Service Name"                 # TODO: Human-readable name
SERVICE_DESC="Service description"          # TODO: Brief description

# ============================================
# BINARY SETTINGS
# ============================================
BINARY_NAME="service-name"                  # TODO: Binary executable name
BINARY_VERSION="1"
BINARY_SOURCE="/home/mustafa/telcobright-projects/orchestrix/images/standalone-binaries/SERVICE-NAME/SERVICE-NAME-v.1/SERVICE-NAME"
                                           # TODO: SET THIS - Full path to your binary

# ============================================
# SERVICE CONFIGURATION
# ============================================
SERVICE_PORT=7001                          # TODO: SET THIS - Port your service uses
SHARD_ID=1                                 # TODO: Set if using sharding (1 for single instance)
TOTAL_SHARDS=1                             # TODO: Total shards in cluster (1 for single instance)

# ============================================
# OPTIONAL ENVIRONMENT VARIABLES
# ============================================
# Add any environment variables your service needs
# Example:
# CONSUL_URL=""                            # Consul address (if using)
# DATABASE_URL=""                          # Database connection (if using)
```

**AI Instruction to User**:
```
I've created: images/lxc/[name]/build/build.conf

Please fill in:
1. BINARY_SOURCE: Absolute path to your standalone binary
2. SERVICE_PORT: Port your service listens on
3. CONTAINER_NAME: Confirm name (currently '[name]')
4. SERVICE_NAME/DESC: Human-readable name and description

Optional:
- SHARD_ID/TOTAL_SHARDS: If using sharding/clustering
- Add environment variables your service needs

Reply when ready to proceed.
```

---

### Template for Debian Full-Stack Container

```bash
# Build Configuration for [Service-Name] Debian Container
# Version: 1
#
# INSTRUCTIONS:
# 1. Set STORAGE_LOCATION_ID (required for BTRFS)
# 2. Set STORAGE_QUOTA_SIZE (e.g., "30G")
# 3. Set SERVICE_PORTS (comma-separated if multiple)
# 4. Review resource limits (MEMORY_LIMIT, CPU_LIMIT)
# 5. Save this file and notify AI when ready

# ============================================
# BASE CONFIGURATION
# ============================================
BASE_IMAGE="images:debian/12"
CONTAINER_VERSION="1"
BUILD_TIMEOUT="1800"
CONTAINER_NAME_PREFIX="service-name"       # TODO: Confirm or change

# ============================================
# STORAGE CONFIGURATION (MANDATORY)
# ============================================
STORAGE_PROVIDER="btrfs"
STORAGE_LOCATION_ID=""                     # TODO: REQUIRED - e.g., "btrfs_local_main"
STORAGE_CONTAINER_ROOT="service-name"
STORAGE_QUOTA_SIZE=""                      # TODO: REQUIRED - e.g., "30G", "50G"
STORAGE_COMPRESSION="true"
STORAGE_SNAPSHOT_ENABLED="true"
STORAGE_SNAPSHOT_FREQUENCY="daily"
STORAGE_SNAPSHOT_RETAIN="7"

# ============================================
# STORAGE MONITORING (MANDATORY)
# ============================================
STORAGE_MONITOR_ENABLED="true"
STORAGE_ROTATION_THRESHOLD="80"            # Rotate at 80% usage
STORAGE_FORCE_CLEANUP_THRESHOLD="90"       # Aggressive at 90%
STORAGE_CHECK_INTERVAL="300"               # Check every 5 minutes

# ============================================
# LOG ROTATION (MANDATORY)
# ============================================
LOG_ROTATION_ENABLED="true"
LOKI_RETENTION_PERIOD="168h"               # 7 days default
SYSTEM_LOG_ROTATE_DAYS="7"
SYSTEM_LOG_COMPRESS="true"

# ============================================
# RESOURCE LIMITS
# ============================================
MEMORY_LIMIT="2GB"                         # TODO: Adjust based on service needs
CPU_LIMIT="2"                              # TODO: Adjust based on service needs

# ============================================
# NETWORK CONFIGURATION
# ============================================
NETWORK_BRIDGE="lxdbr0"
NETWORK_IP=""                              # Empty for DHCP, or set static IP

# ============================================
# SERVICE CONFIGURATION
# ============================================
SERVICE_PORTS="8080"                       # TODO: SET THIS - Comma-separated if multiple
ENABLE_SSH="true"
SSH_AUTO_ACCEPT="true"                     # Auto-accept SSH for dev environments

# ============================================
# BUILD OPTIONS
# ============================================
CLEAN_BUILD="true"
EXPORT_CONTAINER="true"
EXPORT_PATH="/tmp"
CREATE_SNAPSHOT="true"
START_SERVICES="true"

# ============================================
# SERVICE-SPECIFIC CONFIGURATION
# ============================================
# Add service-specific variables here
# Example for MySQL:
# MYSQL_ROOT_PASSWORD=""                   # TODO: Set if needed
# MYSQL_DATABASE=""
```

**AI Instruction to User**:
```
I've created: images/lxc/[name]/build/build.conf

Required (MUST fill):
1. STORAGE_LOCATION_ID: BTRFS storage location (e.g., "btrfs_local_main")
2. STORAGE_QUOTA_SIZE: Disk quota (e.g., "30G" for 30 gigabytes)
3. SERVICE_PORTS: Ports your service uses

Recommended:
- MEMORY_LIMIT: Adjust based on service needs (default 2GB)
- CPU_LIMIT: Adjust based on service needs (default 2 cores)
- Add service-specific configuration at bottom

Reply when ready to proceed.
```

---

## Step 2: User Fills Configuration

User reviews the template and fills in required values:

**For Alpine Binary Container**:
- ✅ Sets `BINARY_SOURCE` to binary path
- ✅ Sets `SERVICE_PORT`
- ✅ Reviews `CONTAINER_NAME`
- ✅ Optionally adds environment variables

**For Debian Full-Stack Container**:
- ✅ Sets `STORAGE_LOCATION_ID`
- ✅ Sets `STORAGE_QUOTA_SIZE`
- ✅ Sets `SERVICE_PORTS`
- ✅ Reviews resource limits
- ✅ Adds service-specific configuration

User confirms: "Done, ready to proceed" or "Configuration filled"

---

## Step 3: AI Generates Build Script and Scaffolding

Once user confirms, AI proceeds to:

### 1. Read the filled build.conf
```bash
# AI reads the file to get configuration values
```

### 2. Generate build.sh

**For Alpine Binary Container**:
```bash
#!/bin/bash
set -e

# Load configuration
CONFIG_FILE="${1:-$(dirname "$0")/build.conf}"
source "$CONFIG_FILE"

# ABSOLUTE PATHS (Critical for LXC snap)
BASE_DIR="/home/mustafa/telcobright-projects/orchestrix/images/lxc/${CONTAINER_NAME}"
TIMESTAMP=$(date +%s)
IMAGE_FILE="${BASE_DIR}/${CONTAINER_NAME}-v.${VERSION}/generated/artifact/${CONTAINER_NAME}-v${VERSION}-${TIMESTAMP}.tar.gz"

# Check if binary exists
if [ ! -f "${BINARY_SOURCE}" ]; then
    echo "ERROR: Binary not found: ${BINARY_SOURCE}"
    echo "Please build the binary first or update BINARY_SOURCE in build.conf"
    exit 1
fi

echo "========================================="
echo "Building Alpine Container: ${CONTAINER_NAME}"
echo "========================================="
echo "Version: ${VERSION}"
echo "Binary: ${BINARY_SOURCE}"
echo "========================================="

# Clean existing container
if lxc info ${BUILD_CONTAINER} >/dev/null 2>&1; then
    echo "Removing existing build container..."
    lxc stop ${BUILD_CONTAINER} --force >/dev/null 2>&1 || true
    lxc delete ${BUILD_CONTAINER} --force
fi

# Create Alpine container
echo "Creating Alpine container..."
lxc launch images:alpine/3.20 ${BUILD_CONTAINER}
sleep 5

# Copy binary
echo "Copying binary to container..."
lxc file push ${BINARY_SOURCE} ${BUILD_CONTAINER}/usr/local/bin/${BINARY_NAME}
lxc exec ${BUILD_CONTAINER} -- chmod +x /usr/local/bin/${BINARY_NAME}

# Create startup script (OpenRC for Alpine)
echo "Creating startup script..."
cat << 'EOF' | lxc exec ${BUILD_CONTAINER} -- tee /etc/init.d/${BINARY_NAME}
#!/sbin/openrc-run

name="${SERVICE_NAME}"
description="${SERVICE_DESC}"
command="/usr/local/bin/${BINARY_NAME}"
command_background=true
pidfile="/var/run/${BINARY_NAME}.pid"

depend() {
    need net
}

start_pre() {
    # Environment variables
    export SERVICE_PORT=${SERVICE_PORT}
    export SHARD_ID=${SHARD_ID}
    export TOTAL_SHARDS=${TOTAL_SHARDS}
}
EOF

lxc exec ${BUILD_CONTAINER} -- chmod +x /etc/init.d/${BINARY_NAME}
lxc exec ${BUILD_CONTAINER} -- rc-update add ${BINARY_NAME} default

# Stop and export
echo "Stopping container..."
lxc stop ${BUILD_CONTAINER}

echo "Exporting image to: ${IMAGE_FILE}"
lxc export ${BUILD_CONTAINER} "${IMAGE_FILE}"

# Generate MD5
md5sum "${IMAGE_FILE}" > "${IMAGE_FILE}.md5"

# Copy templates to VERSION-SPECIFIC generated directory
cp ${BASE_DIR}/templates/sample.conf ${BASE_DIR}/${CONTAINER_NAME}-v.${VERSION}/generated/
cp ${BASE_DIR}/templates/startDefault.sh ${BASE_DIR}/${CONTAINER_NAME}-v.${VERSION}/generated/
chmod +x ${BASE_DIR}/${CONTAINER_NAME}-v.${VERSION}/generated/startDefault.sh

# Clean up
lxc delete ${BUILD_CONTAINER}

echo "========================================="
echo "Build Complete!"
echo "========================================="
echo "Image: ${IMAGE_FILE}"
echo "Size: $(du -h ${IMAGE_FILE} | cut -f1)"
echo ""
echo "Test with:"
echo "  cd ${BASE_DIR}/${CONTAINER_NAME}-v.${VERSION}/generated"
echo "  ./startDefault.sh"
echo "========================================="
```

**For Debian Full-Stack Container**:
- Use Java automation runner
- Include BTRFS setup
- Include storage monitoring
- Include log rotation

### 3. Create Directory Structure
```bash
mkdir -p images/lxc/[container-name]/build
mkdir -p images/lxc/[container-name]/templates
mkdir -p images/lxc/[container-name]/scripts
mkdir -p images/lxc/[container-name]/[container-name]-v.1/generated/artifact
```

### 4. Create Templates
- `templates/sample.conf` - Launch configuration template
- `templates/startDefault.sh` - Quick start script template

### 5. Create README.md
- Document the container
- List APIs/endpoints (if applicable)
- Provide build commands
- Provide launch commands

### 6. Set Permissions
```bash
chmod +x images/lxc/[container-name]/build/build.sh
chmod +x images/lxc/[container-name]/templates/startDefault.sh
```

---

## Step 4: AI Provides Next Steps

AI provides user with:

```
✅ Scaffolding Complete!

Structure created at: images/lxc/[container-name]/

To build the container:
  cd images/lxc/[container-name]
  ./build/build.sh

The build will:
1. Create Alpine/Debian container
2. Copy binary / Install services
3. Configure startup
4. Export to: [container-name]-v.1/generated/artifact/

Generated files:
- Container image (.tar.gz)
- MD5 checksum
- sample.conf (configuration template)
- startDefault.sh (quick start script)
```

---

## Critical Rules for AI

### Before Generating Anything:
1. ✅ Create build.conf template
2. ✅ Present to user with clear instructions
3. ✅ **WAIT** for user confirmation
4. ❌ Never assume configuration values
5. ❌ Never generate build.sh without filled build.conf

### When Generating:
1. ✅ Read filled build.conf to get actual values
2. ✅ Use absolute paths in build scripts
3. ✅ Use version-specific generated directories
4. ✅ Create proper directory structure first
5. ✅ Set executable permissions

### Container Type Specific:
**Alpine Binary**:
- ✅ No BTRFS configuration
- ✅ No storage monitoring
- ✅ Simple, fast build script
- ✅ References standalone binary

**Debian Full-Stack**:
- ✅ Include BTRFS configuration
- ✅ Include storage monitoring
- ✅ Include log rotation
- ✅ Use Java automation

---

## Example Complete Interaction

```
User: "Scaffold an Alpine container for my Rust API service"

AI: "I'll scaffold an Alpine binary container for your Rust API.

     Created: images/lxc/rust-api/build/build.conf

     Please fill in these required values:

     1. BINARY_SOURCE - Absolute path to your Rust binary
        Example: /home/mustafa/.../rust-api-v.1/rust-api

     2. SERVICE_PORT - Port your API listens on
        Example: 8080

     3. Review CONTAINER_NAME (currently 'rust-api')

     The file is ready for editing. Let me know when done."

User: "Done. Set binary path to /path/to/binary and port to 8080"

AI: "Generating scaffolding...

     ✅ Created build/build.sh
     ✅ Created templates/
     ✅ Created directory structure
     ✅ Created README.md

     To build:
       cd images/lxc/rust-api
       ./build/build.sh

     This will create a ~5-10 MB Alpine container with your Rust API."
```

---

## Validation Checklist

Before completing scaffolding, AI must verify:

- [ ] build.conf exists and is filled by user
- [ ] build.sh generated with correct paths from build.conf
- [ ] Directory structure created
- [ ] Templates created
- [ ] README.md created
- [ ] Executable permissions set
- [ ] Absolute paths used throughout
- [ ] Version-specific generated directories

---

## References

- Main Guideline: `CONTAINER_SCAFFOLDING_STANDARD.md`
- Quick Reference: `SCAFFOLDING_SUMMARY.md`
- Example: `images/lxc/go-id/`
