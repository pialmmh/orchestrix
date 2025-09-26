#!/bin/bash
# Check script to verify what needs to be set up for BTRFS

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

echo -e "${GREEN}=========================================${NC}"
echo -e "${GREEN}BTRFS Setup Check for Ubuntu 24.04 LTS${NC}"
echo -e "${GREEN}=========================================${NC}"

# Function to print status
print_check() {
    local status=$1
    local message=$2
    if [ "$status" = "ok" ]; then
        echo -e "${GREEN}[✓]${NC} $message"
    elif [ "$status" = "warning" ]; then
        echo -e "${YELLOW}[!]${NC} $message"
    else
        echo -e "${RED}[✗]${NC} $message"
    fi
}

# Check Ubuntu version
echo -e "\n${YELLOW}System Information:${NC}"
UBUNTU_VERSION=$(lsb_release -rs)
UBUNTU_CODENAME=$(lsb_release -cs)
print_check "ok" "Ubuntu $UBUNTU_VERSION ($UBUNTU_CODENAME)"

# Check BTRFS tools
echo -e "\n${YELLOW}BTRFS Status:${NC}"
if command -v btrfs &> /dev/null; then
    BTRFS_VERSION=$(btrfs --version | head -1)
    print_check "ok" "BTRFS tools installed: $BTRFS_VERSION"
else
    print_check "fail" "BTRFS tools not installed"
fi

# Check BTRFS module
if lsmod | grep -q btrfs; then
    print_check "ok" "BTRFS kernel module loaded"
else
    print_check "warning" "BTRFS kernel module not loaded"
fi

# Check storage directories
echo -e "\n${YELLOW}Storage Structure:${NC}"
BTRFS_ROOT="$HOME/telcobright/btrfs"

if [ -d "$BTRFS_ROOT" ]; then
    print_check "ok" "Storage root exists: $BTRFS_ROOT"

    # Check subdirectories
    [ -d "$BTRFS_ROOT/containers" ] && print_check "ok" "  - containers/ directory exists" || print_check "fail" "  - containers/ missing"
    [ -d "$BTRFS_ROOT/snapshots" ] && print_check "ok" "  - snapshots/ directory exists" || print_check "fail" "  - snapshots/ missing"
    [ -d "$BTRFS_ROOT/backups" ] && print_check "ok" "  - backups/ directory exists" || print_check "fail" "  - backups/ missing"

    # Check if it's a BTRFS filesystem
    FS_TYPE=$(stat -f -c %T "$BTRFS_ROOT" 2>/dev/null || echo "unknown")
    if [ "$FS_TYPE" = "btrfs" ]; then
        print_check "ok" "Storage is on BTRFS filesystem"
    else
        print_check "warning" "Storage is on $FS_TYPE filesystem (not BTRFS)"

        # Check for loop device
        if [ -f "$HOME/telcobright/btrfs.img" ]; then
            print_check "ok" "BTRFS loop device file exists"

            # Check if mounted
            if mount | grep -q "$BTRFS_ROOT"; then
                print_check "ok" "BTRFS loop device is mounted"
            else
                print_check "warning" "BTRFS loop device not mounted"
            fi
        else
            print_check "warning" "No BTRFS loop device found"
        fi
    fi
else
    print_check "fail" "Storage root does not exist: $BTRFS_ROOT"
fi

# Check Orchestrix configuration
echo -e "\n${YELLOW}Orchestrix Configuration:${NC}"
if [ -f "/etc/orchestrix/storage-locations.conf" ]; then
    print_check "ok" "Storage locations config exists"

    # Check if we can read it
    if [ -r "/etc/orchestrix/storage-locations.conf" ]; then
        echo "  Storage locations defined:"
        grep "\.path=" /etc/orchestrix/storage-locations.conf | while read line; do
            location=$(echo $line | cut -d'=' -f1 | cut -d'.' -f1)
            path=$(echo $line | cut -d'=' -f2)
            echo "    - $location: $path"
        done
    else
        print_check "warning" "Cannot read config (permission denied)"
    fi
else
    print_check "fail" "Storage locations config missing"
fi

# Check LXD
echo -e "\n${YELLOW}LXD Status:${NC}"
if command -v lxc &> /dev/null; then
    LXC_VERSION=$(lxc version)
    print_check "ok" "LXD installed: $LXC_VERSION"

    # Check if LXD is initialized
    if lxc profile show default &>/dev/null 2>&1; then
        print_check "ok" "LXD is initialized"

        # Check network bridge
        if lxc network show lxdbr0 &>/dev/null 2>&1; then
            BRIDGE_INFO=$(lxc network get lxdbr0 ipv4.address 2>/dev/null || echo "unknown")
            print_check "ok" "Network bridge lxdbr0: $BRIDGE_INFO"
        else
            print_check "fail" "Network bridge lxdbr0 not found"
        fi
    else
        print_check "warning" "LXD not initialized"
    fi
else
    print_check "fail" "LXD not installed"
fi

# Check IP forwarding
echo -e "\n${YELLOW}Network Configuration:${NC}"
IP_FORWARD=$(cat /proc/sys/net/ipv4/ip_forward)
if [ "$IP_FORWARD" = "1" ]; then
    print_check "ok" "IP forwarding enabled"
else
    print_check "warning" "IP forwarding disabled"
fi

# Check SSH
echo -e "\n${YELLOW}SSH Configuration:${NC}"
if systemctl is-active --quiet ssh || systemctl is-active --quiet sshd; then
    print_check "ok" "SSH service running"
else
    print_check "fail" "SSH service not running"
fi

if [ -f "$HOME/.ssh/id_rsa" ]; then
    print_check "ok" "SSH key exists"
else
    print_check "fail" "SSH key missing"
fi

if [ -f "$HOME/.ssh/authorized_keys" ]; then
    if grep -q "$(cat $HOME/.ssh/id_rsa.pub 2>/dev/null || echo 'nonexistent')" "$HOME/.ssh/authorized_keys" 2>/dev/null; then
        print_check "ok" "SSH key in authorized_keys"
    else
        print_check "warning" "SSH key not in authorized_keys"
    fi
else
    print_check "fail" "No authorized_keys file"
fi

# Test SSH connection
if ssh -o ConnectTimeout=2 -o StrictHostKeyChecking=no localhost whoami &>/dev/null; then
    print_check "ok" "SSH to localhost works"
else
    print_check "warning" "SSH to localhost failed"
fi

# Summary
echo -e "\n${GREEN}=========================================${NC}"
echo -e "${GREEN}Setup Check Complete${NC}"
echo -e "${GREEN}=========================================${NC}"

# Count issues
ISSUES=0
WARNINGS=0

echo -e "\nWhat needs to be done:"

if [ ! -d "$BTRFS_ROOT" ]; then
    echo "  • Create storage directories"
    ISSUES=$((ISSUES + 1))
fi

if [ ! -f "/etc/orchestrix/storage-locations.conf" ]; then
    echo "  • Create storage configuration"
    ISSUES=$((ISSUES + 1))
fi

if [ "$FS_TYPE" != "btrfs" ] && [ ! -f "$HOME/telcobright/btrfs.img" ]; then
    echo "  • Create BTRFS loop device (for testing)"
    WARNINGS=$((WARNINGS + 1))
fi

if ! lxc profile show default &>/dev/null 2>&1; then
    echo "  • Initialize LXD"
    ISSUES=$((ISSUES + 1))
fi

if [ "$IP_FORWARD" != "1" ]; then
    echo "  • Enable IP forwarding"
    WARNINGS=$((WARNINGS + 1))
fi

if [ $ISSUES -eq 0 ] && [ $WARNINGS -eq 0 ]; then
    echo -e "${GREEN}  ✓ Everything is configured!${NC}"
    echo -e "\nYou can now run test deployments."
else
    if [ $ISSUES -gt 0 ]; then
        echo -e "\n${RED}Found $ISSUES critical issues that need fixing.${NC}"
    fi
    if [ $WARNINGS -gt 0 ]; then
        echo -e "${YELLOW}Found $WARNINGS warnings that should be addressed.${NC}"
    fi
    echo -e "\nRun with sudo to fix: ${YELLOW}sudo ./scripts/setup-btrfs-local.sh${NC}"
fi