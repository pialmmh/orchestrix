# LXC Container Scaffolding Guide - Unified Standard

## Overview

This guide defines the **FINAL STANDARD** for creating LXC containers in the Orchestrix project. All containers must follow this structure for consistency, maintainability, and automation compatibility.

## Directory Structure

```
/home/mustafa/telcobright-projects/orchestrix/images/lxc/[container-name]/
├── REQUIREMENTS.md                     # Container specifications (optional)
├── build/                               # Build system directory
│   ├── build.sh                        # Main build script
│   └── build.conf                      # Build configuration
├── src/                                # Java automation source (if using Java)
│   └── main/
│       └── java/
│           └── com/telcobright/orchestrix/images/lxc/[containername]/
│               ├── entities/           # Configuration models
│               ├── scripts/            # Inline script classes
│               └── [ContainerName]Builder.java
├── scripts/                            # Container-specific resources
│   ├── server.js                      # Application files
│   ├── package.json                   # Dependencies
│   └── config/                        # Configuration templates
├── templates/                          # Template files for generated artifacts
│   ├── sample.conf                     # Sample launch configuration template
│   └── startDefault.sh                 # Quick start script template
├── buildContainerName.sh               # Wrapper script (delegates to build/)
├── launchContainerName.sh              # Launch script
├── README.md                           # Container documentation
└── [container-name]-v.X/               # Versioned build artifacts (created by build)
    └── generated/
        ├── [container-name]-vX-TIMESTAMP.tar.gz      # Container image
        ├── [container-name]-vX-TIMESTAMP.tar.gz.md5  # MD5 hash file
        ├── sample.conf                               # Sample launch configuration
        ├── startDefault.sh                           # Quick start script for image
        ├── publish.sh                                # Publish automation (Java/Maven)
        └── publish-config.conf                       # Publish configuration

```

## Naming Conventions

### Scripts
- **Build wrapper**: `build[ContainerName].sh` (e.g., `buildUniqueIdGenerator.sh`)
- **Launch script**: `launch[ContainerName].sh` (e.g., `launchUniqueIdGenerator.sh`)
- **Quick start**: `startDefault.sh` (always this name)

### Images
- **Base image name**: `[container-name]-base-v.X.Y.Z` (e.g., `unique-id-generator-base-v.1.0.0`)
- **Build container**: `[container-name]-build-temp` (temporary, deleted after build)

### Directories
- **Version directory**: `[container-name]-v.X.Y.Z` (e.g., `unique-id-generator-v.1.0.0`)
- **Java package**: `com.telcobright.orchestrix.images.lxc.[containername]` (no hyphens)

## Build System Architecture

### 1. Build Folder Structure
All build logic goes in the `build/` directory:

```bash
build/
├── build.sh      # Main build script - Universal entry point
└── build.conf    # Configuration variables
```

**build.sh is the universal pattern:**
- User always runs `./build/build.sh`
- Script internally handles Maven/Java execution
- Compiles automation if needed
- No need to call Maven directly
- Consistent interface across all containers

### 2. Build Configuration (build.conf)

```bash
# Build Configuration for [Container Name]
# Version: X.Y.Z

# Container Settings
CONTAINER_NAME="container-name"
VERSION="1.0.0"
BASE_IMAGE="ubuntu:22.04"
BUILD_CONTAINER_NAME="container-build-temp"
IMAGE_NAME="container-name-base-v.${VERSION}"

# Network Configuration (Build Phase)
BRIDGE="lxdbr0"
BUILD_IP="10.10.199.90/24"
GATEWAY="10.10.199.1"
DNS_PRIMARY="8.8.8.8"
DNS_SECONDARY="8.8.4.4"

# Service Configuration
SERVICE_PORT="7001"
SERVICE_USER="app"
SERVICE_GROUP="app"
DATA_DIR="/var/lib/container-name"
LOG_FILE="/var/log/container-name.log"

# Build Options
OPTIMIZE_SIZE="true"
CLEANUP_ON_FAILURE="true"
VERBOSE_OUTPUT="true"
CHECK_INTERNET="true"
FAIL_ON_NO_INTERNET="true"

# Java Automation (if applicable)
JAVA_PACKAGE="com.telcobright.orchestrix.images.lxc.containername"
BUILDER_CLASS="ContainerNameBuilder"

# Build Timeout (seconds)
BUILD_TIMEOUT="600"

# Output Directory
OUTPUT_DIR="../${CONTAINER_NAME}-v.${VERSION}"
```

### 3. Build Script (build/build.sh)

**Universal build.sh pattern** - Encapsulates Maven/Java automation:

```bash
#!/bin/bash
# Build script for [Container Name]
# Universal entry point - encapsulates Maven/Java

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ORCHESTRIX_HOME="/home/mustafa/telcobright-projects/orchestrix"

# Load configuration
CONFIG_FILE="${1:-${SCRIPT_DIR}/build.conf}"

if [ ! -f "$CONFIG_FILE" ]; then
    echo "Error: Configuration file not found: $CONFIG_FILE"
    exit 1
fi

source "$CONFIG_FILE"

echo "Building $CONTAINER_NAME_PREFIX v$CONTAINER_VERSION..."

# Navigate to Orchestrix home
cd "$ORCHESTRIX_HOME"

# Compile Java automation if needed
if [ ! -d "target/classes" ]; then
    echo "Compiling Java automation..."
    mvn compile -DskipTests
fi

# Run Java automation (SSH-based container build)
mvn exec:java \
    -Dexec.mainClass="com.telcobright.orchestrix.automation.api.container.lxc.app.[containername].example.[ContainerName]BuildRunner" \
    -Dexec.args="$CONFIG_FILE"

echo "Build complete!"
```

### 4. Wrapper Scripts (Optional)

Simple wrapper in main directory for convenience:

**buildContainerName.sh**:
```bash
#!/bin/bash
# Wrapper - delegates to build/build.sh
exec "$(dirname "$0")/build/build.sh" "$@"
```

## Java Automation Structure

For containers using Java automation, follow this package structure:

### Package Organization
```
src/main/java/com/telcobright/orchestrix/images/lxc/[containername]/
├── entities/                    # Configuration models
│   ├── ContainerConfig.java    # Main configuration
│   └── RuntimeConfig.java      # Runtime-specific config
├── scripts/                     # Inline script classes
│   ├── NetworkSetup.java       # Network configuration scripts
│   ├── ServiceInstaller.java   # Service installation scripts
│   └── SystemConfig.java       # System configuration scripts
└── ContainerNameBuilder.java   # Main orchestrator
```

### Key Principles

1. **Inline Scripts**: All bash scripts are inline Java strings for visibility
2. **Retryable Tasks**: Each script class represents one retryable task
3. **Configuration Models**: Separate entities for build vs runtime config
4. **Script Segmentation**: Break scripts into logical, retryable segments

### Example Script Class

```java
public class NetworkSetup {
    private static final String CONFIGURE_NETWORK_SCRIPT = """
        #!/bin/bash
        # Configure container network

        CONTAINER="%s"
        BRIDGE="%s"
        IP_ADDRESS="%s"

        # Script implementation...
        """;

    private final ContainerConfig config;

    public NetworkSetup(ContainerConfig config) {
        this.config = config;
    }

    public String getConfigureNetworkScript() {
        return String.format(CONFIGURE_NETWORK_SCRIPT,
            config.getContainerName(),
            config.getBridge(),
            config.getIpAddress());
    }
}
```

## Launch Configuration

### Runtime vs Build Configuration

- **Build Config**: Used once to create the base image
- **Launch Config**: Used every time a container is started

### Sample Launch Configuration

```bash
# Launch configuration for [Container Name]
# Runtime parameters for container instances

# Container identification
CONTAINER_NAME="my-container-instance-1"
BASE_IMAGE="container-name-base-v.1.0.0"

# Network configuration
BRIDGE_NAME="lxdbr0"
CONTAINER_IP="10.10.199.51/24"
GATEWAY_IP="10.10.199.1"
DNS_SERVERS="8.8.8.8 8.8.4.4"

# Service configuration
SERVICE_PORT=7001

# Runtime-specific configuration
# (e.g., shard ID, cluster settings, etc.)
RUNTIME_PARAM_1="value1"
RUNTIME_PARAM_2="value2"

# Bind mounts (optional)
# HOST_DATA_DIR="/data/my-container"
# HOST_LOG_DIR="/var/log/containers/my-container"
```

## Versioning

### Version Format
Use semantic versioning: `X.Y.Z`
- **X**: Major version (breaking changes)
- **Y**: Minor version (new features)
- **Z**: Patch version (bug fixes)

### Version Artifacts
Each build creates:
1. Container image with timestamp: `[container-name]-vX-TIMESTAMP.tar.gz`
2. MD5 hash file: `[container-name]-vX-TIMESTAMP.tar.gz.md5`
3. Sample configuration: `sample.conf`
4. Quick start script: `startDefault.sh`
5. Publish automation: `publish.sh` and `publish-config.conf`

### Generated Folder Structure
The build process creates a versioned directory structure: `[container-name]-v.X/generated/`

This ensures each version has its own isolated build artifacts and can be maintained separately.

**Example:** For go-id version 1, the structure is:
```
go-id-v.1/
└── generated/
    ├── go-id-v1-1749837291.tar.gz
    ├── go-id-v1-1749837291.tar.gz.md5
    ├── sample.conf
    ├── startDefault.sh
    ├── publish.sh
    └── publish-config.conf
```

The `generated/` folder contains all artifacts needed to distribute and launch the container:

**Container Image Files:**
- `[container-name]-vX-TIMESTAMP.tar.gz` - LXC container image
- `[container-name]-vX-TIMESTAMP.tar.gz.md5` - MD5 hash file for verification

**Launch Files:**
- `sample.conf` - Sample configuration with all parameters documented (copied from templates/)
- `startDefault.sh` - Self-contained launch script that:
  - Imports the container image from the same directory
  - Reads configuration from sample.conf
  - Creates and starts the container
  - Configures network, ports, mounts, and environment variables

**Publish Files:**
- `publish.sh` - Publish automation script that:
  - Prompts user for upload confirmation
  - Uses Java/Maven to execute PublishManager
  - Uploads to Google Drive via rclone
  - Downloads and verifies MD5 hash
  - Updates database with publish records
- `publish-config.conf` - Configuration for publish automation containing artifact metadata and DB connection info

This folder can be distributed as a complete package for deploying the container on any system.

### Templates Folder
The `templates/` folder contains template files that are copied to each versioned build:

- `sample.conf` - Template configuration file
- `startDefault.sh` - Template quick start script

These templates are copied during the build process to `[container-name]-v.X/generated/`

## Key Development Principles

### 1. Configuration Flexibility
- **Build anywhere**: Accept config files from any filesystem location
- **Single config file**: All parameters in one file for simplicity
- **Optional services**: Containers work without all features configured

### 2. Automation Standards
- **Idempotent builds**: Running build twice produces same result
- **Atomic operations**: Either complete success or rollback
- **Verbose output**: Clear feedback during build process
- **Retry logic**: Transient failures handled automatically

### 3. Development Workflow
1. Create directory structure
2. Define build configuration in `build/build.conf`
3. Implement build logic in `build/build.sh` (or Java)
4. Create wrapper scripts
5. Test with `startDefault.sh`
6. Document in README.md

### 4. Testing Requirements
- Build must be repeatable
- Container must start with default config
- All services must have health checks
- Logs must be accessible

## Networking Architecture

### Bridge Mode Only (NO NAT)
- **NO NAT between containers and host** for production
- Bridge mode only (e.g., lxdbr0 with ipv4.nat=false)
- Direct IP routing: Container → Bridge → Host → Internet
- Ideal for VoIP/SIP applications (FreeSWITCH, Kamailio, Asterisk)
- Containers get real IPs on bridge network (e.g., 10.10.199.0/24)

### Internet Connectivity During Build

Build scripts MUST check internet connectivity and handle gracefully:

```bash
# Check internet connectivity
check_internet() {
    echo "Checking internet connectivity..."
    if ! lxc exec "$BUILD_CONTAINER" -- ping -c 1 8.8.8.8 &>/dev/null; then
        echo ""
        echo "⚠️  WARNING: No internet connectivity detected!"
        echo "==========================================="
        echo "The container cannot reach external networks."
        echo ""
        echo "This is expected in bridge mode without NAT."
        echo "To enable internet for build only, run:"
        echo ""
        echo "  sudo iptables -t nat -A POSTROUTING -s 10.10.199.0/24 -o $(ip route | grep default | awk '{print $5}') -j MASQUERADE"
        echo ""
        echo "==========================================="

        if [ "$FAIL_ON_NO_INTERNET" = "true" ]; then
            echo ""
            echo "Build cannot continue without internet. Exiting."
            lxc delete "$BUILD_CONTAINER" --force
            exit 1
        fi
    else
        echo "✓ Internet connectivity confirmed"
    fi
}
```

### Network Setup Commands

```bash
# Create bridge without NAT (for VoIP/production)
lxc network create lxdbr0 ipv4.address=10.10.199.1/24 ipv4.nat=false

# Or disable NAT on existing bridge
lxc network set lxdbr0 ipv4.nat=false

# Temporary internet access for builds only
sudo iptables -t nat -A POSTROUTING -s 10.10.199.0/24 -o $(ip route | grep default | awk '{print $5}') -j MASQUERADE

# Remove NAT rule after build
sudo iptables -t nat -D POSTROUTING -s 10.10.199.0/24 -o $(ip route | grep default | awk '{print $5}') -j MASQUERADE
```

## Common Patterns

### Service Containers
```bash
# Install service
apt-get update && apt-get install -y service-name

# Configure service
cat > /etc/service.conf <<EOF
configuration here
EOF

# Enable service
systemctl enable service
```

### Development Containers
```bash
# SSH configuration for dev environments
cat > /etc/ssh/ssh_config.d/99-dev.conf <<EOF
StrictHostKeyChecking no
UserKnownHostsFile /dev/null
CheckHostIP no
LogLevel ERROR
EOF
```

### Application Containers
```bash
# Copy application files
lxc file push local/app.jar container/opt/app/

# Create systemd service
cat > /etc/systemd/system/app.service <<EOF
[Unit]
Description=Application Service
After=network.target

[Service]
Type=simple
User=app
ExecStart=/usr/bin/java -jar /opt/app/app.jar
Restart=always

[Install]
WantedBy=multi-user.target
EOF
```

## Validation Checklist

Before considering a container complete:

- [ ] Directory structure follows standard
- [ ] Naming conventions followed exactly
- [ ] Build creates versioned artifacts
- [ ] Launch script accepts any config path
- [ ] Sample config fully documented
- [ ] README.md includes quick start
- [ ] startDefault.sh works without parameters
- [ ] All scripts are executable
- [ ] Version info generated correctly
- [ ] Image name includes version
- [ ] Java package structure correct (if applicable)
- [ ] Build is idempotent
- [ ] Container starts successfully
- [ ] Services have health checks
- [ ] Logs are accessible

## Quick Reference

### Create New Container
```bash
CONTAINER="my-service"
cd /home/mustafa/telcobright-projects/orchestrix/images/lxc/
mkdir -p $CONTAINER/{build,templates,scripts,src/main/java}
cd $CONTAINER

# Create build system
vim build/build.conf
vim build/build.sh      # Copy universal pattern from this guide
chmod +x build/build.sh

# Create templates
vim templates/sample.conf
vim templates/startDefault.sh
chmod +x templates/startDefault.sh

# Optional wrapper
vim build${CONTAINER^}.sh
chmod +x build${CONTAINER^}.sh

# Build container
./build/build.sh

# Test
cd ${CONTAINER}-v.1/generated
./startDefault.sh
```

### Universal Build Pattern

**Always use build.sh as entry point:**
```bash
# Standard build
./build/build.sh

# Custom config
./build/build.sh /path/to/custom.conf
```

**Never call Maven directly** - build.sh handles:
- Java compilation
- Maven execution
- Configuration loading
- Error handling

### File Templates
All templates follow the patterns shown above. Key files:
- `build/build.conf` - Build configuration
- `build/build.sh` - Build implementation
- `buildContainerName.sh` - Build wrapper
- `launchContainerName.sh` - Launch script
- `startDefault.sh` - Quick start

## Important Rules

1. **NEVER hardcode paths** - Everything configurable
2. **NEVER mix build and runtime** - Clear separation
3. **ALWAYS version** - Every build creates version
4. **ALWAYS document** - README for users, comments for developers
5. **ALWAYS test** - startDefault.sh must work

## Migration from Old Structure

For existing containers:
1. Create `build/` directory
2. Move build logic to `build/build.sh`
3. Extract config to `build/build.conf`
4. Update wrapper scripts
5. Add versioning to image names
6. Test with startDefault.sh

---

**This is the FINAL STANDARD for LXC container scaffolding in Orchestrix.**

All new containers must follow this guide. Existing containers should be migrated when updated.