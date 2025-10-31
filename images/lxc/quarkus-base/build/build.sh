#!/bin/bash
set -e

# ============================================
# Quarkus Base Image Build Script
# ============================================
# Creates base image with Java 21 + Promtail
# Built once, reused by all Quarkus app containers

echo "==========================================="
echo "Building Quarkus Base Image"
echo "==========================================="
echo "Java: 21"
echo "Promtail: 2.9.4"
echo "Base: Debian 12"
echo "==========================================="
echo ""

# Configuration
CONTAINER_NAME="quarkus-base-temp"
IMAGE_ALIAS="quarkus-base-v1"
JAVA_VERSION="21"
PROMTAIL_VERSION="2.9.4"

# Cleanup any existing temp container
if lxc list | grep -q "^| $CONTAINER_NAME "; then
    echo "Cleaning up existing temp container..."
    lxc stop "$CONTAINER_NAME" --force 2>/dev/null || true
    lxc delete "$CONTAINER_NAME" --force 2>/dev/null || true
fi

# Launch base container
echo "Launching Debian 12 container..."
lxc launch images:debian/12 "$CONTAINER_NAME"

# Wait for network
echo "Waiting for network..."
sleep 10
for i in {1..30}; do
    if lxc exec "$CONTAINER_NAME" -- ping -c 1 8.8.8.8 &> /dev/null; then
        echo "✓ Network ready"
        break
    fi
    if [ $i -eq 30 ]; then
        echo "ERROR: Network not ready after 30 seconds"
        exit 1
    fi
    sleep 1
done

# Update and install dependencies
echo ""
echo "Installing dependencies..."
lxc exec "$CONTAINER_NAME" -- bash -c "
apt-get update && \
apt-get install -y \
    wget \
    curl \
    unzip \
    netcat-openbsd \
    systemd \
    logrotate \
    gzip \
    ca-certificates \
    gnupg
"

# Install Java 21 from Adoptium
echo ""
echo "Installing Java ${JAVA_VERSION} from Adoptium..."
lxc exec "$CONTAINER_NAME" -- bash -c "
wget -qO - https://packages.adoptium.net/artifactory/api/gpg/key/public | gpg --dearmor -o /usr/share/keyrings/adoptium.gpg && \
echo 'deb [signed-by=/usr/share/keyrings/adoptium.gpg] https://packages.adoptium.net/artifactory/deb bookworm main' > /etc/apt/sources.list.d/adoptium.list && \
apt-get update && \
apt-get install -y temurin-${JAVA_VERSION}-jdk
"

# Verify Java
lxc exec "$CONTAINER_NAME" -- java --version
echo "✓ Java ${JAVA_VERSION} installed"

# Install Promtail
echo ""
echo "Installing Promtail ${PROMTAIL_VERSION}..."
PROMTAIL_URL="https://github.com/grafana/loki/releases/download/v${PROMTAIL_VERSION}/promtail-linux-amd64.zip"

lxc exec "$CONTAINER_NAME" -- bash -c "
cd /tmp && \
wget -q ${PROMTAIL_URL} && \
unzip -q promtail-linux-amd64.zip && \
mv promtail-linux-amd64 /usr/local/bin/promtail && \
chmod +x /usr/local/bin/promtail && \
rm promtail-linux-amd64.zip
"

# Verify Promtail
lxc exec "$CONTAINER_NAME" -- promtail --version
echo "✓ Promtail ${PROMTAIL_VERSION} installed"

# Create standard directories
echo ""
echo "Creating standard directories..."
lxc exec "$CONTAINER_NAME" -- mkdir -p /etc/promtail /var/log
echo "✓ Directories created"

# Stop container
echo ""
echo "Stopping container..."
lxc stop "$CONTAINER_NAME"

# Delete any existing image with same alias
if lxc image list | grep -q "$IMAGE_ALIAS"; then
    echo "Deleting existing image: $IMAGE_ALIAS"
    lxc image delete "$IMAGE_ALIAS"
fi

# Publish as image
echo "Publishing as image: $IMAGE_ALIAS..."
lxc publish "$CONTAINER_NAME" --alias "$IMAGE_ALIAS" --public=false

# Delete temp container
echo "Cleaning up temp container..."
lxc delete "$CONTAINER_NAME"

# Show image info
echo ""
echo "==========================================="
echo "BUILD COMPLETED"
echo "==========================================="
echo "Image: $IMAGE_ALIAS"
lxc image info "$IMAGE_ALIAS"
echo ""
echo "This image contains:"
echo "  ✓ Debian 12"
echo "  ✓ Java ${JAVA_VERSION} JDK"
echo "  ✓ Promtail ${PROMTAIL_VERSION}"
echo "  ✓ Common utilities"
echo ""
echo "Use in app build.conf:"
echo "  BASE_IMAGE=\"local:quarkus-base-v1\""
echo ""
echo "==========================================="
