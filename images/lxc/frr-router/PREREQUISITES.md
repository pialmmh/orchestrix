# FRR Router Deployment Prerequisites

Before deploying FRR router containers to remote nodes, ensure the following prerequisites are met on **each target server**.

## Required Setup on Target Servers

### 1. SSH Access

Ensure you have SSH access to all target servers:

```bash
# Test SSH access
ssh -p <PORT> <USER>@<SERVER_IP>
```

**For BDCOM nodes:**
```bash
ssh -p 15605 bdcom@10.255.246.173
ssh -p 15605 bdcom@10.255.246.174
ssh -p 15605 bdcom@10.255.246.175
```

### 2. Passwordless Sudo (Required)

**IMPORTANT**: The deployment user must have passwordless sudo access.

Add the following line to `/etc/sudoers` on each server:

```bash
# Login to each server
ssh -p 15605 bdcom@10.255.246.173

# Edit sudoers file (use visudo for safety)
sudo visudo

# Add this line at the end:
bdcom ALL=(ALL) NOPASSWD:ALL
```

**Verify passwordless sudo works:**
```bash
ssh -p 15605 bdcom@10.255.246.173 "sudo whoami"
# Should output: root (without password prompt)
```

### 3. LXC/LXD Installed

Ensure LXC/LXD is installed and initialized on each server:

```bash
# Check if LXC is installed
lxc version

# If not installed, install it:
sudo snap install lxd
sudo lxd init --auto
```

### 4. LXC Bridge Network

Ensure the bridge network `lxdbr0` exists with the correct subnet:

```bash
# Check existing networks
sudo lxc network list

# Verify lxdbr0 configuration
sudo lxc network show lxdbr0
```

**Expected configuration:**
- Network: `lxdbr0`
- IPv4 subnet: `10.10.199.0/24`
- IPv4 address: `10.10.199.1/24`

**If bridge doesn't exist or needs reconfiguration:**
```bash
sudo lxc network create lxdbr0 \
    ipv4.address=10.10.199.1/24 \
    ipv4.nat=true \
    ipv6.address=none
```

### 5. SSH Key-Based Authentication (Recommended)

While not strictly required, SSH key-based authentication is recommended for security:

```bash
# Generate SSH key (if you don't have one)
ssh-keygen -t ed25519

# Copy key to remote servers
ssh-copy-id -p 15605 bdcom@10.255.246.173
ssh-copy-id -p 15605 bdcom@10.255.246.174
ssh-copy-id -p 15605 bdcom@10.255.246.175
```

### 6. Firewall Configuration (If Applicable)

If firewalls are enabled, ensure the following ports are accessible:

- **SSH**: Port specified in config (default: 15605)
- **BGP**: Port 179 (for inter-node BGP peering)
- **LXC Bridge**: 10.10.199.0/24 subnet

## Quick Verification Script

Run this on each target server to verify prerequisites:

```bash
#!/bin/bash

echo "=== Checking Prerequisites ==="

# Check LXC
if command -v lxc &> /dev/null; then
    echo "✓ LXC installed: $(lxc version)"
else
    echo "✗ LXC not installed"
fi

# Check sudo
if sudo -n true 2>/dev/null; then
    echo "✓ Passwordless sudo configured"
else
    echo "✗ Passwordless sudo NOT configured"
fi

# Check bridge
if sudo lxc network list --format csv | grep -q "lxdbr0"; then
    echo "✓ lxdbr0 bridge exists"
    sudo lxc network get lxdbr0 ipv4.address
else
    echo "✗ lxdbr0 bridge not found"
fi

echo "=== Prerequisites Check Complete ==="
```

## Post-Deployment Configuration

After deploying FRR containers, you may need to configure routing or NAT rules to enable BGP peering between nodes. See `DEPLOYMENT_STATUS.md` for details.

## Security Notes

### Passwordless Sudo

While passwordless sudo is required for automated deployments, consider these security practices:

1. **Limit scope**: Only grant to specific deployment user
2. **Restrict commands** (optional): Limit to specific LXC commands only:
   ```
   bdcom ALL=(ALL) NOPASSWD: /snap/bin/lxc, /usr/bin/lxc
   ```
3. **Audit logs**: Monitor sudo usage via system logs
4. **Remove when not needed**: After deployment, you can require password again

### Alternative: Dedicated Deployment User

For production environments, consider creating a dedicated deployment user:

```bash
# Create deployment user
sudo useradd -m -s /bin/bash lxc-deploy

# Add to lxd group
sudo usermod -aG lxd lxc-deploy

# Configure passwordless sudo
echo "lxc-deploy ALL=(ALL) NOPASSWD: /snap/bin/lxc, /usr/bin/lxc" | sudo tee -a /etc/sudoers

# Set up SSH key for this user
sudo -u lxc-deploy ssh-keygen -t ed25519
```

## Troubleshooting

### "Permission denied" errors

**Cause**: Passwordless sudo not configured
**Fix**: Add user to sudoers as described above

### "Bridge not found" errors

**Cause**: lxdbr0 doesn't exist
**Fix**: Create bridge with correct subnet

### "Image import failed" errors

**Cause**: Insufficient disk space
**Fix**: Ensure at least 500MB free space in `/var/snap/lxd/common/lxd/`

### SSH connection issues

**Cause**: Firewall blocking SSH port
**Fix**: Configure firewall to allow port 15605:
```bash
sudo ufw allow 15605/tcp
```

## Support

For issues or questions:
- Check system logs: `journalctl -u lxd`
- Check LXC logs: `sudo lxc list` and `sudo lxc info <container>`
- Review deployment logs from the deployment script output
