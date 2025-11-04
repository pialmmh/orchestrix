#!/bin/bash

# Simple LXC Bridge Setup
# Creates a bridge with fixed gateway IP for containers with static IPs

set -e

# Configuration
BRIDGE_NAME="${1:-lxcbr0}"           # Bridge name (can be passed as argument)
BRIDGE_IP="${2:-10.10.199.1/24}"     # Gateway IP for the bridge
CONTAINER_NETWORK="${3:-10.10.199.0/24}"  # Container network

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

print_status() {
    echo -e "${GREEN}[+]${NC} $1"
}

print_error() {
    echo -e "${RED}[!]${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}[*]${NC} $1"
}

# Check if running as root
if [[ $EUID -ne 0 ]]; then
   print_error "This script must be run as root (use sudo)"
   exit 1
fi

# Function to clean up existing configuration
cleanup_existing() {
    print_status "Checking for existing configuration..."
    
    # Stop all containers using this bridge
    if lxc network list 2>/dev/null | grep -q $BRIDGE_NAME; then
        print_warning "Found existing LXD network $BRIDGE_NAME"
        
        # Stop containers using this network
        for container in $(lxc list --format=json | jq -r ".[] | select(.devices.eth0.network==\"$BRIDGE_NAME\").name" 2>/dev/null); do
            print_warning "Stopping container $container"
            lxc stop $container --force
        done
        
        print_warning "Removing LXD network $BRIDGE_NAME"
        lxc network delete $BRIDGE_NAME 2>/dev/null || true
    fi
    
    # Delete bridge if exists
    if ip link show $BRIDGE_NAME &>/dev/null; then
        print_warning "Removing existing bridge $BRIDGE_NAME"
        ip link delete $BRIDGE_NAME
    fi
}

# Function to create bridge
create_bridge() {
    print_status "Creating bridge $BRIDGE_NAME..."
    
    # Create bridge
    ip link add $BRIDGE_NAME type bridge
    
    # Configure bridge settings
    echo 0 > /sys/class/net/$BRIDGE_NAME/bridge/stp_state
    
    # Bring up bridge
    ip link set $BRIDGE_NAME up
    
    # Add IP address to bridge (gateway for containers)
    ip addr add $BRIDGE_IP dev $BRIDGE_NAME
    
    print_status "Bridge $BRIDGE_NAME created with gateway IP $BRIDGE_IP"
}

# Function to set up routing and NAT
setup_routing() {
    print_status "Setting up IP forwarding and NAT..."
    
    # Enable IP forwarding
    sysctl -w net.ipv4.ip_forward=1
    
    # Make IP forwarding persistent
    if ! grep -q "net.ipv4.ip_forward=1" /etc/sysctl.conf; then
        echo "net.ipv4.ip_forward=1" >> /etc/sysctl.conf
    fi
    
    # Get default interface
    DEFAULT_IF=$(ip route | grep default | awk '{print $5}' | head -n1)
    
    if [ -z "$DEFAULT_IF" ]; then
        print_error "Could not determine default network interface"
        exit 1
    fi
    
    print_status "Default interface: $DEFAULT_IF"
    
    # Remove existing NAT rules for this network if any
    iptables -t nat -D POSTROUTING -s $CONTAINER_NETWORK -j MASQUERADE 2>/dev/null || true
    
    # Add NAT rule for container network to go through default interface
    iptables -t nat -A POSTROUTING -s $CONTAINER_NETWORK -o $DEFAULT_IF -j MASQUERADE
    
    # Allow forwarding
    iptables -D FORWARD -i $BRIDGE_NAME -j ACCEPT 2>/dev/null || true
    iptables -D FORWARD -o $BRIDGE_NAME -j ACCEPT 2>/dev/null || true
    
    iptables -A FORWARD -i $BRIDGE_NAME -j ACCEPT
    iptables -A FORWARD -o $BRIDGE_NAME -j ACCEPT
    
    print_status "NAT and forwarding rules configured"
    print_status "Containers in $CONTAINER_NETWORK will have internet via $DEFAULT_IF"
}

# Function to configure LXD
configure_lxd() {
    print_status "Configuring LXD to use bridge $BRIDGE_NAME..."
    
    # Create LXD network using the existing bridge
    # No DHCP, no NAT from LXD - we handle everything manually
    lxc network create $BRIDGE_NAME \
        ipv4.address=none \
        ipv6.address=none \
        ipv4.nat=false \
        ipv6.nat=false \
        ipv4.dhcp=false \
        ipv6.dhcp=false \
        bridge.external_interfaces=$BRIDGE_NAME \
        raw.dnsmasq="dhcp-ignore=tag:!known" \
        dns.mode=none || {
        print_warning "LXD network may already exist, updating..."
        lxc network set $BRIDGE_NAME ipv4.address none
        lxc network set $BRIDGE_NAME ipv6.address none
        lxc network set $BRIDGE_NAME ipv4.nat false
        lxc network set $BRIDGE_NAME ipv6.nat false
        lxc network set $BRIDGE_NAME ipv4.dhcp false
        lxc network set $BRIDGE_NAME ipv6.dhcp false
    }
    
    print_status "LXD network configured (no DHCP, static IPs only)"
}

# Function to save iptables rules
save_iptables() {
    print_status "Saving iptables rules..."
    
    # For Debian/Ubuntu
    if command -v netfilter-persistent &>/dev/null; then
        netfilter-persistent save
    elif command -v iptables-save &>/dev/null; then
        iptables-save > /etc/iptables/rules.v4 2>/dev/null || \
        iptables-save > /etc/iptables.rules 2>/dev/null || \
        print_warning "Could not save iptables rules automatically"
    fi
}

# Function to create systemd service for persistence
create_systemd_service() {
    print_status "Creating systemd service for persistence..."
    
    cat > /etc/systemd/system/lxc-bridge.service <<EOF
[Unit]
Description=LXC Bridge for Static IP Containers
After=network-online.target
Wants=network-online.target
Before=lxd.service snap.lxd.daemon.service

[Service]
Type=oneshot
RemainAfterExit=yes
ExecStart=/usr/local/bin/setup-lxc-bridge.sh $BRIDGE_NAME $BRIDGE_IP $CONTAINER_NETWORK
ExecStop=/bin/bash -c 'ip link delete $BRIDGE_NAME 2>/dev/null || true'

[Install]
WantedBy=multi-user.target
EOF
    
    # Copy this script to system location
    cp "$0" /usr/local/bin/setup-lxc-bridge.sh
    chmod +x /usr/local/bin/setup-lxc-bridge.sh
    
    systemctl daemon-reload
    systemctl enable lxc-bridge.service
    
    print_status "Systemd service created and enabled"
}

# Function to update container launcher scripts
update_launcher_scripts() {
    print_status "Creating updated launcher configuration..."
    
    cat > /home/mustafa/telcobright-projects/orchestrix/images/lxc/bridge-config.sh <<'EOF'
#!/bin/bash

# Bridge Configuration for Static IP Containers
# Source this file in your launcher scripts

# Bridge network configuration
export DEFAULT_BRIDGE="lxcbr0"
export DEFAULT_GATEWAY="10.10.199.1"
export DEFAULT_NETMASK="24"
export DEFAULT_DNS="8.8.8.8,8.8.4.4"

# Function to configure static IP for container
configure_static_ip() {
    local CONTAINER_NAME="$1"
    local CONTAINER_IP="$2"
    local BRIDGE="${3:-$DEFAULT_BRIDGE}"
    local GATEWAY="${4:-$DEFAULT_GATEWAY}"
    local NETMASK="${5:-$DEFAULT_NETMASK}"
    local DNS="${6:-$DEFAULT_DNS}"
    
    if [ -z "$CONTAINER_IP" ]; then
        echo "No static IP specified, container will need manual configuration"
        return
    fi
    
    echo "Configuring static IP $CONTAINER_IP for $CONTAINER_NAME"
    
    # Attach container to bridge
    lxc network attach $BRIDGE $CONTAINER_NAME eth0 2>/dev/null || \
    lxc config device override $CONTAINER_NAME eth0 network=$BRIDGE
    
    # Configure static IP inside container
    lxc exec $CONTAINER_NAME -- bash -c "cat > /etc/netplan/10-lxc.yaml <<NETPLAN
network:
  version: 2
  ethernets:
    eth0:
      addresses: [$CONTAINER_IP/$NETMASK]
      gateway4: $GATEWAY
      nameservers:
        addresses: [$DNS]
NETPLAN"
    
    # Apply network configuration
    lxc exec $CONTAINER_NAME -- netplan apply 2>/dev/null || \
    lxc exec $CONTAINER_NAME -- bash -c "
        ip addr flush dev eth0
        ip addr add $CONTAINER_IP/$NETMASK dev eth0
        ip link set eth0 up
        ip route add default via $GATEWAY
        echo 'nameserver ${DNS%%,*}' > /etc/resolv.conf
    "
}
EOF
    
    chmod +x /home/mustafa/telcobright-projects/orchestrix/images/lxc/bridge-config.sh
    
    print_status "Helper script created at bridge-config.sh"
}

# Function to show example usage
show_usage() {
    echo ""
    echo "================================"
    echo "Setup Complete!"
    echo "================================"
    echo ""
    echo "Bridge Configuration:"
    echo "  Name: $BRIDGE_NAME"
    echo "  Gateway IP: $BRIDGE_IP"
    echo "  Network: $CONTAINER_NETWORK"
    echo ""
    echo "To launch a container with static IP:"
    echo ""
    echo "1. In your config file (e.g., sample-config.conf):"
    echo "   CONTAINER_IP=\"10.10.199.100\""
    echo "   CONTAINER_GATEWAY=\"10.10.199.1\""
    echo "   CONTAINER_NETMASK=\"24\""
    echo "   CONTAINER_DNS=\"8.8.8.8,8.8.4.4\""
    echo ""
    echo "2. In your launcher script, after creating container:"
    echo "   lxc network attach $BRIDGE_NAME \$CONTAINER_NAME eth0"
    echo "   lxc exec \$CONTAINER_NAME -- bash -c \"ip addr add \$CONTAINER_IP/24 dev eth0\""
    echo "   lxc exec \$CONTAINER_NAME -- bash -c \"ip route add default via 10.10.199.1\""
    echo ""
    echo "3. Or use the helper function from bridge-config.sh:"
    echo "   source /path/to/bridge-config.sh"
    echo "   configure_static_ip \$CONTAINER_NAME \$CONTAINER_IP"
    echo ""
    echo "Benefits:"
    echo "  ✓ No NAT between containers (direct communication)"
    echo "  ✓ Static IPs as configured in your config files"
    echo "  ✓ Internet access via host routing"
    echo "  ✓ Works with WiFi and Ethernet"
    echo "  ✓ Perfect for VoIP applications"
}

# Main execution
main() {
    echo "================================"
    echo "LXC Bridge Setup"
    echo "================================"
    echo ""
    
    # Parse special arguments
    if [ "$1" == "--cleanup" ]; then
        cleanup_existing
        print_status "Cleanup completed"
        exit 0
    fi
    
    if [ "$1" == "--help" ]; then
        echo "Usage: $0 [BRIDGE_NAME] [GATEWAY_IP/MASK] [NETWORK/MASK]"
        echo ""
        echo "Defaults:"
        echo "  BRIDGE_NAME: lxcbr0"
        echo "  GATEWAY_IP:  10.10.199.1/24"
        echo "  NETWORK:     10.10.199.0/24"
        echo ""
        echo "Options:"
        echo "  --cleanup    Remove bridge and configuration"
        echo "  --help       Show this help"
        exit 0
    fi
    
    # Setup steps
    cleanup_existing
    create_bridge
    setup_routing
    configure_lxd
    save_iptables
    create_systemd_service
    update_launcher_scripts
    show_usage
}

# Run main function
main "$@"