# LXC Container Scaffold Template v2

## ⚠️ CRITICAL REQUIREMENTS

### 1. Directory Structure (MANDATORY)
Every container MUST follow this exact structure:

```
container-name/
├── REQUIREMENTS.md           # Container specifications
├── build/                    # Build scripts directory
│   ├── build.sh             # Main build script
│   └── build.conf           # Build configuration with ALL parameters
└── container-name-v.X/      # Versioned output (auto-generated)
    └── generated/           # Generated artifacts
        ├── launch.sh        # Runtime launch script
        ├── sample.conf      # Sample runtime configuration
        ├── README-v.X.md    # Version documentation
        └── container-name-v.X.tar.gz  # Packaged container
```

### 2. Networking Architecture (BRIDGE MODE ONLY)
- **NO NAT between containers and host**
- Bridge mode only (e.g., lxdbr0 with ipv4.nat=false)
- Direct IP routing: Container → Bridge → Host → Internet
- Ideal for VoIP/SIP applications (FreeSWITCH, Kamailio, Asterisk)
- Containers get real IPs on bridge network (e.g., 10.10.199.0/24)

### 3. Build Configuration (build.conf)
ALL build parameters MUST be in `build/build.conf`:

```bash
# Base image configuration
BASE_IMAGE="images:debian/12"
CONTAINER_VERSION="1"

# Repository configurations (if needed)
REPO_URL=""
REPO_GPG_KEY=""
REPO_USERNAME=""  # If authentication required
REPO_TOKEN=""     # If authentication required

# Package lists
PACKAGES_CORE="curl wget git vim"
PACKAGES_BUILD="gcc make build-essential"
PACKAGES_ADDITIONAL=""

# Service configurations
ENABLE_SSH="true"
DEFAULT_USER="container-user"
DEFAULT_PASSWORD="changeme"

# Build behavior
CHECK_INTERNET="true"
FAIL_ON_NO_INTERNET="true"
```

### 4. Internet Connectivity Check
Build script MUST check internet connectivity:

```bash
# Check internet connectivity
check_internet() {
    echo "Checking internet connectivity..."
    if ! lxc exec "$BUILD_CONTAINER" -- ping -c 1 8.8.8.8 &>/dev/null; then
        echo "⚠️  WARNING: No internet connectivity detected!"
        echo "Container cannot reach external networks."
        echo "This may be due to:"
        echo "  1. Bridge NAT is disabled (required for VoIP)"
        echo "  2. Missing MASQUERADE rule"
        echo ""
        echo "To fix temporarily for build:"
        echo "  sudo iptables -t nat -A POSTROUTING -s 10.10.199.0/24 -o <your-interface> -j MASQUERADE"

        if [ "$FAIL_ON_NO_INTERNET" = "true" ]; then
            echo "Build cannot continue without internet. Exiting."
            exit 1
        fi
    else
        echo "✓ Internet connectivity confirmed"
    fi
}
```

## Build Script Template (build/build.sh)

```bash
#!/bin/bash
set -e

# Load configuration
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
CONFIG_FILE="$SCRIPT_DIR/build.conf"

if [ ! -f "$CONFIG_FILE" ]; then
    echo "Error: build.conf not found!"
    exit 1
fi

source "$CONFIG_FILE"

# Set defaults
CONTAINER_VERSION="${CONTAINER_VERSION:-1}"
BASE_NAME="$(basename $(dirname $SCRIPT_DIR))"
BUILD_CONTAINER="${BASE_NAME}-build"
IMAGE_NAME="${BASE_NAME}-v.${CONTAINER_VERSION}"

echo "==========================================="
echo "Building $BASE_NAME v.$CONTAINER_VERSION"
echo "==========================================="

# Check for existing build
if lxc list | grep -q "$BUILD_CONTAINER"; then
    echo "Cleaning up existing build container..."
    lxc delete "$BUILD_CONTAINER" --force
fi

# Launch build container
echo "Launching build container..."
lxc launch "$BASE_IMAGE" "$BUILD_CONTAINER"

# Wait for container
echo "Waiting for container to be ready..."
sleep 5
while ! lxc exec "$BUILD_CONTAINER" -- systemctl is-system-running &>/dev/null; do
    sleep 2
done

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
        echo "  sudo iptables -t nat -A POSTROUTING -s 10.10.199.0/24 -o wlo1 -j MASQUERADE"
        echo ""
        echo "Replace 'wlo1' with your internet interface from: ip route | grep default"
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

if [ "$CHECK_INTERNET" = "true" ]; then
    check_internet
fi

# System update
echo "Updating system packages..."
lxc exec "$BUILD_CONTAINER" -- apt-get update
lxc exec "$BUILD_CONTAINER" -- bash -c "DEBIAN_FRONTEND=noninteractive apt-get upgrade -y"

# Install packages
if [ -n "$PACKAGES_CORE" ]; then
    echo "Installing core packages..."
    lxc exec "$BUILD_CONTAINER" -- bash -c "DEBIAN_FRONTEND=noninteractive apt-get install -y $PACKAGES_CORE"
fi

# [Container-specific installation steps here]

# Create user
if [ "$ENABLE_SSH" = "true" ]; then
    echo "Setting up SSH access..."
    lxc exec "$BUILD_CONTAINER" -- bash -c "DEBIAN_FRONTEND=noninteractive apt-get install -y openssh-server"
    lxc exec "$BUILD_CONTAINER" -- useradd -m -s /bin/bash "$DEFAULT_USER" || true
    lxc exec "$BUILD_CONTAINER" -- bash -c "echo '$DEFAULT_USER:$DEFAULT_PASSWORD' | chpasswd"
fi

# Clean up
echo "Cleaning up..."
lxc exec "$BUILD_CONTAINER" -- apt-get clean
lxc exec "$BUILD_CONTAINER" -- rm -rf /var/lib/apt/lists/*

# Stop and publish
echo "Creating image..."
lxc stop "$BUILD_CONTAINER"
lxc publish "$BUILD_CONTAINER" --alias "$IMAGE_NAME" --description "${BASE_NAME} version ${CONTAINER_VERSION}"
lxc delete "$BUILD_CONTAINER"

# Generate artifacts
OUTPUT_DIR="$(dirname $SCRIPT_DIR)/${BASE_NAME}-v.${CONTAINER_VERSION}/generated"
mkdir -p "$OUTPUT_DIR"

# Generate launch.sh
cat > "$OUTPUT_DIR/launch.sh" << 'EOF'
#!/bin/bash
# Auto-generated launch script
CONFIG_FILE="${1:-$(dirname "$0")/sample.conf}"
source "$CONFIG_FILE"
# Launch logic here
EOF
chmod +x "$OUTPUT_DIR/launch.sh"

# Generate sample.conf
cat > "$OUTPUT_DIR/sample.conf" << EOF
# Runtime configuration for ${BASE_NAME} v.${CONTAINER_VERSION}
CONTAINER_NAME="${BASE_NAME}-instance-01"
STATIC_IP="10.10.199.100"
NETWORK="lxdbr0"
EOF

echo ""
echo "==========================================="
echo "Build Complete!"
echo "==========================================="
echo "Image: $IMAGE_NAME"
echo "Artifacts: $OUTPUT_DIR"
echo ""
```

## Runtime Configuration (generated/sample.conf)

```bash
# Runtime configuration (can be placed ANYWHERE)
CONTAINER_NAME="my-container-01"
NETWORK="lxdbr0"
STATIC_IP="10.10.199.50"

# Bind mounts (host:container)
BIND_MOUNTS=(
    "/host/data:/container/data"
    "~/workspace:/workspace"
)

# Service settings
SERVICE_PORT="8080"
ENABLE_FEATURE_X="true"
```

## Key Principles

1. **Build vs Runtime Separation**
   - build/ contains build scripts and config
   - generated/ contains runtime scripts
   - Build config ≠ Runtime config

2. **Version Management**
   - Each version in separate directory
   - Can have multiple versions
   - Easy rollback/testing

3. **Bridge Networking Only**
   - No NAT between containers
   - Direct IP routing
   - Perfect for VoIP/RTC

4. **Configuration Philosophy**
   - ALL build params in build.conf
   - Runtime config can be ANYWHERE
   - No hardcoded values

5. **Internet During Build**
   - Always check connectivity
   - Provide clear fix instructions
   - Handle gracefully

## Common Issues and Solutions

### No Internet During Build
```bash
# Temporary fix for build only:
sudo iptables -t nat -A POSTROUTING -s 10.10.199.0/24 -o wlo1 -j MASQUERADE

# After build, remove it:
sudo iptables -t nat -D POSTROUTING -s 10.10.199.0/24 -o wlo1 -j MASQUERADE
```

### Bridge Without NAT
```bash
# Create bridge without NAT (for VoIP)
lxc network create lxdbr0 ipv4.address=10.10.199.1/24 ipv4.nat=false

# Or disable NAT on existing bridge
lxc network set lxdbr0 ipv4.nat=false
```

## Migration from Old Structure

To migrate existing containers:

1. Create `build/` directory
2. Move build scripts to `build/`
3. Create `build.conf` with all parameters
4. Update build script to generate artifacts
5. Test versioned output

## Example Implementations

- `/home/mustafa/telcobright-projects/orchestrix/images/lxc/dev-env/` - Development environment
- `/home/mustafa/telcobright-projects/orchestrix/images/lxc/fusion-pbx/` - FusionPBX (to be migrated)