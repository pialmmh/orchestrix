#!/bin/bash

# Network connectivity fix for LXC container builds

echo "========================================="
echo "LXC Container Network Troubleshooting"
echo "========================================="

# Check which container is having issues
CONTAINER_NAME="${1:-auto-increment-build-temp}"

echo "Checking container: $CONTAINER_NAME"

# Solution 1: Check DNS in container
echo ""
echo "1. Checking DNS resolution..."
lxc exec "$CONTAINER_NAME" -- bash -c "cat /etc/resolv.conf"
lxc exec "$CONTAINER_NAME" -- bash -c "nslookup deb.debian.org" 2>/dev/null || echo "DNS lookup failed"

# Solution 2: Set Google DNS
echo ""
echo "2. Setting Google DNS..."
lxc exec "$CONTAINER_NAME" -- bash -c "echo 'nameserver 8.8.8.8' > /etc/resolv.conf"
lxc exec "$CONTAINER_NAME" -- bash -c "echo 'nameserver 8.8.4.4' >> /etc/resolv.conf"

# Solution 3: Test connectivity
echo ""
echo "3. Testing connectivity..."
lxc exec "$CONTAINER_NAME" -- ping -c 2 8.8.8.8
lxc exec "$CONTAINER_NAME" -- ping -c 2 deb.debian.org

# Solution 4: Check proxy settings
echo ""
echo "4. Checking for proxy requirements..."
if [ -n "$http_proxy" ] || [ -n "$HTTP_PROXY" ]; then
    echo "Host has proxy settings. Applying to container..."

    # Apply proxy to container
    lxc exec "$CONTAINER_NAME" -- bash -c "cat > /etc/apt/apt.conf.d/proxy.conf << EOF
Acquire::http::Proxy \"${http_proxy:-$HTTP_PROXY}\";
Acquire::https::Proxy \"${https_proxy:-$HTTPS_PROXY}\";
EOF"

    # Set environment variables
    lxc exec "$CONTAINER_NAME" -- bash -c "export http_proxy=${http_proxy:-$HTTP_PROXY}"
    lxc exec "$CONTAINER_NAME" -- bash -c "export https_proxy=${https_proxy:-$HTTPS_PROXY}"
else
    echo "No proxy detected on host"
fi

# Solution 5: Try alternative mirror
echo ""
echo "5. Trying alternative Debian mirror..."
lxc exec "$CONTAINER_NAME" -- bash -c "cp /etc/apt/sources.list /etc/apt/sources.list.backup"

# Try a different mirror (using US mirror as example)
lxc exec "$CONTAINER_NAME" -- bash -c "cat > /etc/apt/sources.list << 'EOF'
deb http://ftp.us.debian.org/debian/ bookworm main
deb-src http://ftp.us.debian.org/debian/ bookworm main

deb http://security.debian.org/debian-security bookworm-security main
deb-src http://security.debian.org/debian-security bookworm-security main

deb http://ftp.us.debian.org/debian/ bookworm-updates main
deb-src http://ftp.us.debian.org/debian/ bookworm-updates main
EOF"

# Solution 6: Update and retry
echo ""
echo "6. Updating package lists with fixes..."
lxc exec "$CONTAINER_NAME" -- apt-get update

# Test if it works now
echo ""
echo "7. Testing package installation..."
lxc exec "$CONTAINER_NAME" -- apt-get install -y curl

if [ $? -eq 0 ]; then
    echo ""
    echo "✓ Network issues resolved! You can now continue with the build."
else
    echo ""
    echo "⚠ Network issues persist. Additional troubleshooting needed:"
    echo "  - Check your internet connection"
    echo "  - Check firewall rules"
    echo "  - Try restarting LXD: sudo systemctl restart snap.lxd.daemon"
    echo "  - Check LXD network: lxc network show lxdbr0"
fi