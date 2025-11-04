#!/bin/bash

# Minimal build script - downloads Node.js binary directly
# No package manager complications

set -e

echo "==========================================="
echo "Unique ID Generator - Minimal Build"
echo "==========================================="
echo ""

# Configuration
BUILD_CONTAINER="uid-gen-build"
IMAGE_NAME="unique-id-generator-base"
BASE_IMAGE="images:debian/12"
NODE_VERSION="v20.11.0"

# Clean up existing
lxc delete --force "$BUILD_CONTAINER" 2>/dev/null || true

# Launch container
echo "Launching container..."
lxc launch "$BASE_IMAGE" "$BUILD_CONTAINER"
sleep 5

# Configure network
echo "Configuring network..."
lxc config device remove "$BUILD_CONTAINER" eth0 2>/dev/null || true
lxc config device add "$BUILD_CONTAINER" eth0 nic nictype=bridged parent=lxdbr0 ipv4.address=10.10.199.199

# Download Node.js binary directly (avoid package manager issues)
echo "Downloading Node.js $NODE_VERSION binary..."
lxc exec "$BUILD_CONTAINER" -- bash -c "
    apt-get update && apt-get install -y wget xz-utils
    cd /tmp
    wget -q https://nodejs.org/dist/$NODE_VERSION/node-$NODE_VERSION-linux-x64.tar.xz
    tar -xf node-$NODE_VERSION-linux-x64.tar.xz
    mv node-$NODE_VERSION-linux-x64 /opt/node
    ln -s /opt/node/bin/node /usr/local/bin/node
    ln -s /opt/node/bin/npm /usr/local/bin/npm
"

# Verify
NODE_V=$(lxc exec "$BUILD_CONTAINER" -- node --version)
NPM_V=$(lxc exec "$BUILD_CONTAINER" -- npm --version)
echo "Installed: Node.js $NODE_V, npm $NPM_V"

# Create directories
echo "Creating directories..."
lxc exec "$BUILD_CONTAINER" -- mkdir -p /opt/unique-id-generator /var/log/unique-id-generator /var/lib/unique-id-generator

# Copy files
echo "Copying service files..."
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
lxc file push "$SCRIPT_DIR/scripts/server-simple.js" "$BUILD_CONTAINER/opt/unique-id-generator/server.js"
lxc file push "$SCRIPT_DIR/scripts/snowflake.js" "$BUILD_CONTAINER/opt/unique-id-generator/"
lxc file push "$SCRIPT_DIR/scripts/uid-cli.js" "$BUILD_CONTAINER/opt/unique-id-generator/"
lxc file push "$SCRIPT_DIR/scripts/package.json" "$BUILD_CONTAINER/opt/unique-id-generator/"

# Install dependencies
echo "Installing npm dependencies..."
lxc exec "$BUILD_CONTAINER" -- bash -c "cd /opt/unique-id-generator && npm install --production"

# Create systemd service
echo "Creating service..."
cat <<'EOF' | lxc exec "$BUILD_CONTAINER" -- tee /etc/systemd/system/unique-id-generator.service > /dev/null
[Unit]
Description=Unique ID Generator
After=network.target

[Service]
Type=simple
User=nobody
WorkingDirectory=/opt/unique-id-generator
Environment="NODE_ENV=production"
EnvironmentFile=-/opt/unique-id-generator/.env
ExecStart=/usr/local/bin/node /opt/unique-id-generator/server.js
Restart=always

[Install]
WantedBy=multi-user.target
EOF

lxc exec "$BUILD_CONTAINER" -- systemctl enable unique-id-generator

# Default config
echo -e "PORT=8080\nSHARD_ID=1\nTOTAL_SHARDS=1" | lxc exec "$BUILD_CONTAINER" -- tee /opt/unique-id-generator/.env > /dev/null

# Set permissions
lxc exec "$BUILD_CONTAINER" -- chown -R nobody:nogroup /opt/unique-id-generator /var/log/unique-id-generator /var/lib/unique-id-generator

# Test
echo "Testing..."
lxc exec "$BUILD_CONTAINER" -- timeout 1 /usr/local/bin/node /opt/unique-id-generator/server.js || true

# Create CLI symlink
echo "Setting up CLI..."
lxc exec "$BUILD_CONTAINER" -- ln -sf /opt/unique-id-generator/uid-cli.js /usr/local/bin/uid-cli
lxc exec "$BUILD_CONTAINER" -- chmod +x /opt/unique-id-generator/uid-cli.js

# Clean up
echo "Cleaning up..."
lxc exec "$BUILD_CONTAINER" -- bash -c "apt-get clean && rm -rf /var/lib/apt/lists/* /tmp/*"

# Stop and create image
echo "Creating image..."
lxc stop "$BUILD_CONTAINER"
sleep 3

lxc image delete "$IMAGE_NAME" 2>/dev/null || true
lxc publish "$BUILD_CONTAINER" --alias "$IMAGE_NAME" description="Unique ID Generator - Minimal"
lxc delete "$BUILD_CONTAINER"

echo ""
echo "✓ Build complete!"
echo "Image: $IMAGE_NAME"
echo ""