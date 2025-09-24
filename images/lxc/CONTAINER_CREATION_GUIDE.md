# Container Creation Guide - Shell vs Java Based Systems

## Overview
Orchestrix supports two container creation systems. Choose based on your needs:
- **Shell-based**: Simple, direct, good for basic containers
- **Java-based**: Advanced, with verification, better error handling

---

## SHELL-BASED SYSTEM (Traditional)

### When to Use
- Simple containers with basic setup
- Quick prototypes
- Containers without complex verification needs
- When you prefer direct shell scripting

### Directory Structure
```
/home/mustafa/telcobright-projects/orchestrix/images/lxc/[container-name]/
├── build[ContainerName].sh           # Build script (creates base image)
├── build[ContainerName]Config.cnf    # Build configuration
├── launch[ContainerName].sh          # Launch script (starts containers)
├── startDefault.sh                   # Quick start wrapper
├── scripts/                          # Container-specific scripts/configs
├── README.md                         # Documentation
└── [container-name]-v.X.X.X/        # Generated after build
    └── generated/
        └── sample.conf               # Sample launch configuration
```

### File Naming Convention
- Build script: `build[ContainerName].sh` (e.g., `buildAutoIncrement.sh`)
- Build config: `build[ContainerName]Config.cnf` (e.g., `buildAutoIncrementConfig.cnf`)
- Launch script: `launch[ContainerName].sh` (e.g., `launchAutoIncrement.sh`)
- Base image name: `[container-name]-base` (e.g., `auto-increment-service-base`)

### Step-by-Step Creation Process

#### 1. Create Directory Structure
```bash
CONTAINER_NAME="my-service"
mkdir -p /home/mustafa/telcobright-projects/orchestrix/images/lxc/${CONTAINER_NAME}/scripts
cd /home/mustafa/telcobright-projects/orchestrix/images/lxc/${CONTAINER_NAME}
```

#### 2. Create Build Configuration
`buildMyServiceConfig.cnf`:
```bash
# Build Configuration for My Service Container

# Container settings
BASE_IMAGE="images:debian/12"    # ALWAYS use Debian 12
CONTAINER_NAME="my-service-base"
BUILD_CONTAINER="my-service-build-temp"

# MANDATORY NETWORK CONFIGURATION - MUST BE FILLED
# All containers use bridge-based private network 10.10.199.0/24
CONTAINER_IP="10.10.199.XX/24"     # REQUIRED: Must include /24 (e.g., 10.10.199.50/24)
GATEWAY_IP="10.10.199.1/24"        # REQUIRED: Must include /24 (e.g., 10.10.199.1/24)
BRIDGE_NAME="lxdbr0"                # Bridge interface name
DNS_SERVERS="8.8.8.8 8.8.4.4"      # DNS servers for the container

# Service configuration
SERVICE_PORT="8080"
SERVICE_USER="appuser"

# Build options
CLEANUP_BUILD_CONTAINER="true"
OPTIMIZE_SIZE="true"
```

#### 3. Create Build Script
`buildMyService.sh`:

**IMPORTANT**: Copy network validation functions from `/home/mustafa/telcobright-projects/orchestrix/common/lxc-network-validation-template.sh`

```bash
#!/bin/bash

set -e

SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
CONFIG_FILE="${SCRIPT_DIR}/buildMyServiceConfig.cnf"
VERSION="1.0.0"

echo "========================================="
echo "My Service Container Builder"
echo "Version: ${VERSION}"
echo "========================================="

source "$CONFIG_FILE"

# =============================================================================
# COPY NETWORK FUNCTIONS FROM:
# /home/mustafa/telcobright-projects/orchestrix/common/lxc-network-validation-template.sh
#
# Copy these three functions inline (DO NOT source the file):
# - validate_network_config()
# - configure_container_network()
# - test_and_wait_for_internet()
# =============================================================================

# [INSERT NETWORK VALIDATION FUNCTIONS HERE]

# Check LXC installation
check_lxc() {
    if ! command -v lxc &> /dev/null; then
        echo "ERROR: LXC/LXD is not installed"
        exit 1
    fi
    echo "✓ LXC/LXD is installed"
}

# Clean up existing containers/images
cleanup_existing() {
    if lxc info "$BUILD_CONTAINER" &>/dev/null 2>&1; then
        echo "Cleaning up existing build container..."
        lxc delete --force "$BUILD_CONTAINER"
    fi

    if lxc info "$CONTAINER_NAME" &>/dev/null 2>&1; then
        echo "WARNING: Base container '$CONTAINER_NAME' already exists"
        read -p "Delete and rebuild? (y/N): " -n 1 -r
        echo
        if [[ $REPLY =~ ^[Yy]$ ]]; then
            lxc delete --force "$CONTAINER_NAME"
        else
            exit 1
        fi
    fi
}

# Create and configure container
create_build_container() {
    echo "Creating build container..."
    lxc launch "$BASE_IMAGE" "$BUILD_CONTAINER"

    # Configure network using the copied function
    configure_container_network "$BUILD_CONTAINER"

    # Test internet connectivity using the copied function
    test_and_wait_for_internet "$BUILD_CONTAINER"
}

# Install dependencies
install_dependencies() {
    echo "Installing dependencies..."

    lxc exec "$BUILD_CONTAINER" -- bash -c "
        set -e
        apt-get update
        DEBIAN_FRONTEND=noninteractive apt-get install -y \\
            curl \\
            systemd \\
            # Add your packages here
    "

    echo "✓ Dependencies installed"
}

# Setup service
setup_service() {
    echo "Setting up service..."

    # Copy service files
    if [ -d "$SCRIPT_DIR/scripts" ]; then
        for file in "$SCRIPT_DIR/scripts"/*; do
            if [ -f "$file" ]; then
                lxc file push "$file" "$BUILD_CONTAINER/opt/$(basename $file)"
            fi
        done
    fi

    # Configure service
    lxc exec "$BUILD_CONTAINER" -- bash -c "
        # Service setup commands here
        echo 'Service configured'
    "

    echo "✓ Service configured"
}

# Optimize container
optimize_container() {
    if [ "$OPTIMIZE_SIZE" = "true" ]; then
        echo "Optimizing container size..."

        lxc exec "$BUILD_CONTAINER" -- bash -c "
            apt-get clean
            apt-get autoremove -y
            rm -rf /var/lib/apt/lists/*
            rm -rf /tmp/* /var/tmp/*
            find /var/log -type f -exec truncate -s 0 {} \;
        "

        echo "✓ Container optimized"
    fi
}

# Create base image
create_base_image() {
    echo "Creating base image..."

    lxc stop "$BUILD_CONTAINER"
    lxc copy "$BUILD_CONTAINER" "$CONTAINER_NAME"

    echo "✓ Base image created: $CONTAINER_NAME"
}

# Cleanup build container
cleanup() {
    if [ "$CLEANUP_BUILD_CONTAINER" = "true" ]; then
        echo "Cleaning up build container..."
        lxc delete "$BUILD_CONTAINER"
        echo "✓ Cleanup complete"
    fi
}

# Generate versioned files
create_generated_files() {
    echo "Creating generated files..."

    VERSION_DIR="${SCRIPT_DIR}/${CONTAINER_NAME}-v.${VERSION}"
    GENERATED_DIR="${VERSION_DIR}/generated"
    mkdir -p "$GENERATED_DIR"

    # Copy build config
    cp "$CONFIG_FILE" "${VERSION_DIR}/buildConfig.cnf"

    # Create sample configuration
    cat > "${GENERATED_DIR}/sample.conf" << 'EOF'
#!/bin/bash

# Runtime Configuration
CONTAINER_NAME="my-service-1"
BASE_IMAGE="my-service-base"

# Network configuration
USE_BRIDGE="true"
BRIDGE_NAME="lxdbr0"
STATIC_IP=""  # Leave empty for DHCP

# Service configuration
SERVICE_PORT="8080"

# Resource limits (optional)
MEMORY_LIMIT=""
CPU_LIMIT=""

# Bind mounts (host:container)
BIND_MOUNTS=()
EOF

    echo "✓ Generated files created in ${VERSION_DIR}"
}

# Print summary
print_summary() {
    echo ""
    echo "========================================="
    echo "Build Complete!"
    echo "========================================="
    echo "Base image: $CONTAINER_NAME"
    echo "Version: $VERSION"
    echo ""
    echo "To launch:"
    echo "  ./launchMyService.sh ${CONTAINER_NAME}-v.${VERSION}/generated/sample.conf"
    echo ""
    echo "Or quick start:"
    echo "  ./startDefault.sh"
}

# Main execution
main() {
    validate_network_config  # MUST validate network first
    check_lxc
    cleanup_existing
    create_build_container   # Will loop until internet works
    install_dependencies
    setup_service
    optimize_container
    create_base_image
    cleanup
    create_generated_files
    print_summary
}

main "$@"
```

**CRITICAL**: The build script MUST include:
1. Copy network functions from `/home/mustafa/telcobright-projects/orchestrix/common/lxc-network-validation-template.sh`
2. Call `validate_network_config()` - Check IPs are in 10.10.199.0/24 with /24 notation
3. Call `configure_container_network "$BUILD_CONTAINER"` - Setup static IP
4. Call `test_and_wait_for_internet "$BUILD_CONTAINER"` - Loop until internet works

**DO NOT source the template file** - Copy functions inline to keep build script self-sufficient

#### 4. Create Launch Script
`launchMyService.sh`:
```bash
#!/bin/bash

set -e

if [ $# -eq 0 ]; then
    echo "Usage: $0 <config-file>"
    echo "Example: $0 ./my-service-v.1.0.0/generated/sample.conf"
    exit 1
fi

CONFIG_FILE="$1"

if [ ! -f "$CONFIG_FILE" ]; then
    echo "ERROR: Configuration file not found: $CONFIG_FILE"
    exit 1
fi

echo "========================================="
echo "My Service Container Launcher"
echo "========================================="

source "$CONFIG_FILE"

# Set defaults
CONTAINER_NAME="${CONTAINER_NAME:-my-service-1}"
BASE_IMAGE="${BASE_IMAGE:-my-service-base}"

# Check prerequisites
if ! command -v lxc &> /dev/null; then
    echo "ERROR: LXC/LXD is not installed"
    exit 1
fi

if ! lxc image list --format json | grep -q "\"$BASE_IMAGE\""; then
    echo "ERROR: Base image '$BASE_IMAGE' not found"
    echo "Please run the build script first"
    exit 1
fi

# Check if container exists
if lxc info "$CONTAINER_NAME" &>/dev/null 2>&1; then
    echo "WARNING: Container '$CONTAINER_NAME' already exists"
    read -p "Delete and recreate? (y/N): " -n 1 -r
    echo
    if [[ $REPLY =~ ^[Yy]$ ]]; then
        lxc delete --force "$CONTAINER_NAME"
    else
        exit 1
    fi
fi

# Launch container
echo "Launching container: $CONTAINER_NAME"
lxc launch "$BASE_IMAGE" "$CONTAINER_NAME"

# Configure network
if [ "$USE_BRIDGE" = "true" ] && [ -n "$BRIDGE_NAME" ]; then
    echo "Configuring bridge network..."

    if [ -n "$STATIC_IP" ]; then
        lxc config device add "$CONTAINER_NAME" eth0 nic \
            nictype=bridged \
            parent="$BRIDGE_NAME" \
            ipv4.address="$STATIC_IP"
    else
        lxc config device add "$CONTAINER_NAME" eth0 nic \
            nictype=bridged \
            parent="$BRIDGE_NAME"
    fi
fi

# Setup bind mounts
if [ ${#BIND_MOUNTS[@]} -gt 0 ]; then
    echo "Setting up bind mounts..."

    for mount in "${BIND_MOUNTS[@]}"; do
        IFS=':' read -r host_path container_path <<< "$mount"
        host_path="${host_path/#\~/$HOME}"
        mkdir -p "$host_path"

        mount_name=$(basename "$container_path" | tr '/' '-')
        lxc config device add "$CONTAINER_NAME" "$mount_name" disk \
            source="$host_path" \
            path="$container_path"
    done
fi

# Set resource limits
if [ -n "$MEMORY_LIMIT" ]; then
    lxc config set "$CONTAINER_NAME" limits.memory "$MEMORY_LIMIT"
fi

if [ -n "$CPU_LIMIT" ]; then
    lxc config set "$CONTAINER_NAME" limits.cpu "$CPU_LIMIT"
fi

# Get container IP
sleep 3
CONTAINER_IP=$(lxc list "$CONTAINER_NAME" -c 4 --format csv | grep -oE '[0-9]+\.[0-9]+\.[0-9]+\.[0-9]+' | head -1)

# Print summary
echo ""
echo "========================================="
echo "Container Launch Complete!"
echo "========================================="
echo "Container: $CONTAINER_NAME"
if [ -n "$CONTAINER_IP" ]; then
    echo "IP Address: $CONTAINER_IP"
    echo "Service URL: http://${CONTAINER_IP}:${SERVICE_PORT}"
fi
echo ""
echo "Commands:"
echo "  Shell:  lxc exec $CONTAINER_NAME -- bash"
echo "  Stop:   lxc stop $CONTAINER_NAME"
echo "  Delete: lxc delete --force $CONTAINER_NAME"
```

#### 5. Create startDefault.sh
```bash
#!/bin/bash

set -e

SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

echo "========================================="
echo "My Service Quick Start"
echo "========================================="

# Check if base image exists
if ! lxc image list --format json | grep -q '"my-service-base"'; then
    echo "ERROR: Base image not found. Please run buildMyService.sh first"
    exit 1
fi

# Find latest version directory
VERSION_DIR=$(ls -d "$SCRIPT_DIR"/my-service-v.* 2>/dev/null | sort -V | tail -1)

if [ -z "$VERSION_DIR" ] || [ ! -f "$VERSION_DIR/generated/sample.conf" ]; then
    echo "ERROR: No generated configuration found."
    echo "Please run buildMyService.sh first"
    exit 1
fi

# Create temporary config for default container
DEFAULT_CONFIG="/tmp/my-service-default.conf"
cp "$VERSION_DIR/generated/sample.conf" "$DEFAULT_CONFIG"
sed -i 's/CONTAINER_NAME="[^"]*"/CONTAINER_NAME="my-service-default"/' "$DEFAULT_CONFIG"

# Launch with default config
bash "$SCRIPT_DIR/launchMyService.sh" "$DEFAULT_CONFIG"

# Cleanup
rm -f "$DEFAULT_CONFIG"

echo "Default container 'my-service-default' is running"
```

#### 6. Make Scripts Executable
```bash
chmod +x build*.sh launch*.sh startDefault.sh
```

### Key Rules for Shell-Based System

1. **ALWAYS use Debian 12**: `BASE_IMAGE="images:debian/12"`
2. **Network validation**: Copy functions from `/home/mustafa/telcobright-projects/orchestrix/common/lxc-network-validation-template.sh` inline
3. **IPs must include /24**: All IPs must be in format `10.10.199.XX/24`
4. **Separate build and launch**: Never combine them
5. **startDefault.sh only launches**: Must check for base image first
6. **Config from anywhere**: Launch script accepts config path as argument
7. **Versioned output**: Build creates `container-name-v.X.X.X/generated/`
8. **Optional services**: Use conditionals for optional features
9. **Bind mounts in config**: Never hardcode paths
10. **Self-sufficient scripts**: Copy functions inline, don't source external files

---

## JAVA-BASED SYSTEM (Advanced)

### When to Use
- Complex containers requiring verification
- Containers with multiple service dependencies
- When you need better error handling
- For production-grade containers

### Directory Structure
```
/home/mustafa/telcobright-projects/orchestrix/images/lxc/[container-name]/
├── REQUIREMENTS.md                  # Container specifications
├── build.yaml                       # Build configuration
├── [ContainerName]Builder.java      # Custom builder class
├── build/
│   └── build.sh                     # Wrapper script
└── [container-name]-v.X/           # Generated after build
    └── generated/
        ├── launch.sh
        ├── sample.conf
        └── README-v.X.md
```

### Step-by-Step Creation Process

#### 1. Create build.yaml
```yaml
name: my-service
version: 1
base_image: debian/12
timeout: 300

packages:
  core:
    - curl
    - wget
    - systemd

  build:
    - build-essential

  additional:
    - nginx
    - postgresql-client

custom:
  verify: true
  service_port: 8080
  service_name: my-service
```

#### 2. Create Custom Builder Class
`MyServiceBuilder.java`:
```java
package com.orchestrix.builder.containers;

import com.orchestrix.builder.*;

public class MyServiceBuilder extends LXCBuilder {

    public MyServiceBuilder(BuildConfig config) {
        super(config);
    }

    @Override
    protected void performCustomSetup(String buildContainer) {
        System.out.println("\nPerforming custom setup...");

        // Install service
        System.out.println("  - Installing service");
        execute("lxc exec " + buildContainer + " -- apt-get install -y my-package");

        // Configure service
        System.out.println("  - Configuring service");
        String config = """
            server {
                listen 8080;
                server_name localhost;
            }
            """;
        execute(String.format(
            "lxc exec %s -- bash -c 'cat > /etc/nginx/sites-available/default << EOF\n%s\nEOF'",
            buildContainer, config
        ));

        // Enable service
        execute("lxc exec " + buildContainer + " -- systemctl enable nginx");
    }

    @Override
    protected void performVerification(String buildContainer) {
        if (!config.getCustomBoolean("verify", true)) {
            return;
        }

        System.out.println("\nVerifying installation...");

        // Verify service
        executeWithVerification(
            "lxc exec " + buildContainer + " -- nginx -v",
            "nginx version",
            10
        );

        System.out.println(GREEN + "  ✓ Verification passed" + RESET);
    }

    @Override
    protected String getLaunchScriptContent() {
        return """
            #!/bin/bash
            set -e

            CONFIG_FILE="${1:-$(dirname "$0")/sample.conf}"
            source "$CONFIG_FILE"

            # Launch container
            lxc launch ${IMAGE_NAME} ${CONTAINER_NAME}

            # Apply configurations
            for mount in "${BIND_MOUNTS[@]}"; do
                lxc config device add ${CONTAINER_NAME} $(basename ${mount%%:*}) disk \\
                    source=${mount%%:*} path=${mount#*:}
            done

            echo "Container ${CONTAINER_NAME} launched"
            """;
    }

    @Override
    protected String getSampleConfigContent() {
        return String.format("""
            # Configuration for %s v.%d
            CONTAINER_NAME=%s-instance-01
            IMAGE_NAME=%s:%d

            # Bind Mounts
            BIND_MOUNTS=(
                "/data/%s:/var/lib/%s"
            )
            """, containerBase, version, containerBase, containerBase, version, containerBase, containerBase);
    }
}
```

#### 3. Register Builder in CLI
Add to `BuilderCLI.java`:
```java
case "my-service":
    return new MyServiceBuilder(config);
```

#### 4. Create Build Wrapper
`build/build.sh`:
```bash
#!/bin/bash
cd /home/mustafa/telcobright-projects/orchestrix/builder
java -cp "target/*:lib/*" com.orchestrix.builder.BuilderCLI \\
    --config ../images/lxc/my-service/build.yaml \\
    --builder my-service \\
    "$@"
```

### Java Builder Base Methods Available

```java
// Execution
execute("command");                          // Run with output
executeWithVerification("cmd", "expected", timeout); // Verify output

// Container Management
safeDeleteImage("image:tag");
safeDeleteContainer("container");
waitForNetwork("container");

// Package Installation
installPackages("container", "description", "pkg1", "pkg2");

// Image Creation
createImage("build-container", "image:tag");
generateArtifacts();
```

---

## DECISION MATRIX: Shell vs Java

| Criteria | Shell-Based | Java-Based |
|----------|------------|------------|
| **Setup Speed** | Fast | Slower (needs compilation) |
| **Error Handling** | Basic | Advanced |
| **Verification** | Manual | Automatic (ExpectJ) |
| **Debugging** | Shell debugging | Stack traces |
| **Complexity Limit** | Medium | High |
| **Reusability** | Copy/paste | Inheritance |
| **Type Safety** | None | Compile-time |
| **IDE Support** | Basic | Full |
| **Learning Curve** | Low | Medium |
| **Best For** | Simple containers | Production containers |

---

## COMMON REQUIREMENTS FOR BOTH SYSTEMS

### 1. Base Image
**ALWAYS use Debian 12**: `images:debian/12` or `debian/12`

### 2. Network Configuration (MANDATORY)
**ALL containers MUST use bridge-based private network 10.10.199.0/24**

#### Network Requirements:
- **Subnet**: 10.10.199.0/24 (MANDATORY)
- **Gateway**: 10.10.199.1/24 (default, MUST include /24)
- **Bridge**: lxdbr0 (or custom bridge)
- **IP Assignment**: Static IPs only, no DHCP
- **DNS**: 8.8.8.8, 8.8.4.4 (Google DNS default)
- **Notation**: ALL IPs MUST include /24 suffix (e.g., 10.10.199.50/24)

#### Build Script Requirements:
1. **Format validation**: Check IPs include /24 notation
2. **Subnet validation**: Check IP and gateway are in 10.10.199.0/24
3. **Internet test**: Loop until container can ping google.com
4. **User prompting**: Guide user to fix NAT/masquerading if needed
5. **Exit on error**: Don't proceed without valid network config

#### IP Configuration Rules:
- User MUST specify IPs with /24: `CONTAINER_IP="10.10.199.50/24"`
- Script MUST validate /24 is present
- Script MUST strip /24 when passing to LXC commands: `ipv4.address="${CONTAINER_IP%/24}"`
- Only /24 subnet is allowed - no /16, /8, /32, etc.

#### Common Network Setup Commands:
```bash
# Enable IP forwarding
sudo sysctl -w net.ipv4.ip_forward=1

# Add NAT/Masquerading for the subnet
sudo iptables -t nat -A POSTROUTING -s 10.10.199.0/24 -j MASQUERADE

# Make persistent
echo "net.ipv4.ip_forward=1" | sudo tee -a /etc/sysctl.conf
```

### 3. SSH Configuration (Dev Containers)
```bash
Host *
    StrictHostKeyChecking no
    UserKnownHostsFile /dev/null
    CheckHostIP no
    LogLevel ERROR
```

### 4. Directory Structure
- Scripts in `scripts/` subdirectory
- Versioned output in `container-name-v.X/generated/`
- Configuration samples with all options documented

### 5. Resource Management
- Support memory and CPU limits
- Cleanup build artifacts
- Optimize container size

### 6. Documentation
- README.md with quick start
- Sample configurations with comments
- API/service endpoints documented

---

## VALIDATION CHECKLIST

Before considering a container complete:

- [ ] Base image uses Debian 12
- [ ] Build and launch are separated
- [ ] startDefault.sh only launches (checks for base image)
- [ ] Configuration can be loaded from any path
- [ ] Bind mounts are configured in sample.conf
- [ ] Services are optional (container works without them)
- [ ] Scripts are executable
- [ ] README includes quick start instructions
- [ ] Sample config is fully documented
- [ ] Base image name follows convention
- [ ] Version directory is created after build

---

## EXAMPLES IN THE CODEBASE

### Shell-Based Examples:
- `/home/mustafa/telcobright-projects/orchestrix/images/lxc/auto-increment-service/`
- `/home/mustafa/telcobright-projects/orchestrix/images/lxc/fusion-pbx/`

### Java-Based Examples:
- `/home/mustafa/telcobright-projects/orchestrix/images/lxc/dev-env/`
- Check `com.orchestrix.builder.containers` package

---

## AI AGENT INSTRUCTIONS

When asked to create a new container:

1. **Ask for requirements**: Purpose, services, network needs
2. **Choose system**: Shell for simple, Java for complex
3. **Use Debian 12**: Always `images:debian/12`
4. **Follow naming**: `build[Name].sh`, `launch[Name].sh`
5. **Separate concerns**: Build creates image, launch uses it
6. **Make startDefault simple**: Only launch, check prerequisites
7. **Document everything**: README, sample configs, inline comments
8. **Test commands**: Provide test curl/commands in summary
9. **Version output**: Always create versioned directory
10. **Validate checklist**: Run through validation before completing