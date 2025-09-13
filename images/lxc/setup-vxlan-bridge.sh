#!/bin/bash

# VXLAN Bridge Setup for LXC Containers
# This creates a private bridge network that works over WiFi or Ethernet
# Containers can communicate without NAT, and have internet via host routing

set -e

# Configuration
VXLAN_ID=100                    # VXLAN Network Identifier
VXLAN_DEV="vxlan0"              # VXLAN interface name
BRIDGE_NAME="br-vxlan"          # Bridge name
BRIDGE_IP="10.200.200.1/24"     # Host IP on bridge
CONTAINER_NETWORK="10.200.200.0/24"  # Container network
VXLAN_PORT=4789                 # VXLAN UDP port (default)
MTU=1450                        # MTU for VXLAN (50 bytes less than physical)

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Function to print colored output
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
    print_status "Cleaning up existing configuration..."
    
    # Delete VXLAN interface if exists
    if ip link show $VXLAN_DEV &>/dev/null; then
        print_warning "Removing existing VXLAN interface $VXLAN_DEV"
        ip link delete $VXLAN_DEV
    fi
    
    # Delete bridge if exists
    if ip link show $BRIDGE_NAME &>/dev/null; then
        print_warning "Removing existing bridge $BRIDGE_NAME"
        ip link delete $BRIDGE_NAME
    fi
    
    # Remove LXD network if exists
    if lxc network list --format=json | jq -e ".[] | select(.name==\"$BRIDGE_NAME\")" &>/dev/null; then
        print_warning "Removing existing LXD network $BRIDGE_NAME"
        # Stop containers using this network
        for container in $(lxc list --format=json | jq -r ".[] | select(.devices.eth0.network==\"$BRIDGE_NAME\").name"); do
            print_warning "Stopping container $container"
            lxc stop $container --force
        done
        lxc network delete $BRIDGE_NAME
    fi
}

# Function to create VXLAN and bridge
create_vxlan_bridge() {
    print_status "Creating VXLAN interface..."
    
    # Create VXLAN interface
    # Using 'local any' allows VXLAN to work over any interface (WiFi or Ethernet)
    ip link add $VXLAN_DEV type vxlan \
        id $VXLAN_ID \
        dstport $VXLAN_PORT \
        local 0.0.0.0 \
        nolearning
    
    # Set MTU for VXLAN
    ip link set $VXLAN_DEV mtu $MTU
    
    # Bring up VXLAN interface
    ip link set $VXLAN_DEV up
    
    print_status "Creating bridge $BRIDGE_NAME..."
    
    # Create bridge
    ip link add $BRIDGE_NAME type bridge
    
    # Configure bridge settings for container use
    echo 0 > /sys/class/net/$BRIDGE_NAME/bridge/stp_state
    echo 1 > /sys/class/net/$BRIDGE_NAME/bridge/forward_delay
    
    # Add VXLAN to bridge
    ip link set $VXLAN_DEV master $BRIDGE_NAME
    
    # Set bridge MTU
    ip link set $BRIDGE_NAME mtu $MTU
    
    # Bring up bridge
    ip link set $BRIDGE_NAME up
    
    # Add IP address to bridge for host access
    ip addr add $BRIDGE_IP dev $BRIDGE_NAME
    
    print_status "Bridge $BRIDGE_NAME created with IP $BRIDGE_IP"
}

# Function to set up routing and NAT
setup_routing() {
    print_status "Setting up routing and NAT..."
    
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
    iptables -t nat -D POSTROUTING -s $CONTAINER_NETWORK ! -d $CONTAINER_NETWORK -j MASQUERADE 2>/dev/null || true
    
    # Add NAT rule for container network
    iptables -t nat -A POSTROUTING -s $CONTAINER_NETWORK -o $DEFAULT_IF -j MASQUERADE
    
    # Allow forwarding from bridge to default interface
    iptables -A FORWARD -i $BRIDGE_NAME -o $DEFAULT_IF -j ACCEPT 2>/dev/null || true
    iptables -A FORWARD -i $DEFAULT_IF -o $BRIDGE_NAME -m state --state RELATED,ESTABLISHED -j ACCEPT 2>/dev/null || true
    
    print_status "NAT and forwarding rules configured"
}

# Function to configure LXD
configure_lxd() {
    print_status "Configuring LXD to use bridge $BRIDGE_NAME..."
    
    # Create LXD network using existing bridge
    lxc network create $BRIDGE_NAME \
        ipv4.address=none \
        ipv6.address=none \
        ipv4.nat=false \
        ipv6.nat=false \
        bridge.external_interfaces=$BRIDGE_NAME \
        type=bridge \
        --force-local 2>/dev/null || {
        print_warning "LXD network may already exist, updating..."
        lxc network set $BRIDGE_NAME ipv4.address none
        lxc network set $BRIDGE_NAME ipv6.address none
        lxc network set $BRIDGE_NAME ipv4.nat false
        lxc network set $BRIDGE_NAME ipv6.nat false
    }
    
    print_status "LXD network configured"
}

# Function to create DHCP/DNS server configuration (optional)
setup_dnsmasq() {
    print_status "Setting up dnsmasq for DHCP/DNS..."
    
    # Check if dnsmasq is installed
    if ! command -v dnsmasq &>/dev/null; then
        print_warning "dnsmasq not installed. Installing..."
        apt-get update && apt-get install -y dnsmasq
    fi
    
    # Create dnsmasq configuration
    cat > /etc/dnsmasq.d/br-vxlan.conf <<EOF
# VXLAN Bridge DHCP Configuration
interface=$BRIDGE_NAME
bind-interfaces
domain=vxlan.local
dhcp-range=10.200.200.100,10.200.200.250,12h
dhcp-option=option:router,10.200.200.1
dhcp-option=option:dns-server,10.200.200.1,8.8.8.8
dhcp-authoritative
EOF
    
    # Restart dnsmasq
    systemctl restart dnsmasq || print_warning "Could not restart dnsmasq"
    
    print_status "DHCP/DNS configured on $BRIDGE_NAME"
}

# Function to create systemd service for persistence
create_systemd_service() {
    print_status "Creating systemd service for persistence..."
    
    cat > /etc/systemd/system/vxlan-bridge.service <<EOF
[Unit]
Description=VXLAN Bridge for LXC Containers
After=network-online.target
Wants=network-online.target

[Service]
Type=oneshot
RemainAfterExit=yes
ExecStart=/bin/bash -c '$0 --no-service'
ExecStop=/bin/bash -c 'ip link delete $VXLAN_DEV 2>/dev/null || true; ip link delete $BRIDGE_NAME 2>/dev/null || true'

[Install]
WantedBy=multi-user.target
EOF
    
    # Copy this script to a system location
    cp "$0" /usr/local/bin/setup-vxlan-bridge.sh
    chmod +x /usr/local/bin/setup-vxlan-bridge.sh
    
    systemctl daemon-reload
    systemctl enable vxlan-bridge.service
    
    print_status "Systemd service created and enabled"
}

# Function to test the setup
test_setup() {
    print_status "Testing the setup..."
    
    # Check if bridge is up
    if ip link show $BRIDGE_NAME | grep -q "state UP"; then
        print_status "Bridge $BRIDGE_NAME is UP"
    else
        print_error "Bridge $BRIDGE_NAME is not UP"
        return 1
    fi
    
    # Check if IP is assigned
    if ip addr show $BRIDGE_NAME | grep -q "$BRIDGE_IP"; then
        print_status "IP $BRIDGE_IP assigned to bridge"
    else
        print_error "IP not assigned to bridge"
        return 1
    fi
    
    # Check LXD network
    if lxc network list | grep -q $BRIDGE_NAME; then
        print_status "LXD network $BRIDGE_NAME exists"
    else
        print_error "LXD network not configured"
        return 1
    fi
    
    # Test with a container
    print_status "Creating test container..."
    lxc launch images:alpine/3.18 vxlan-test --network=$BRIDGE_NAME
    
    sleep 5
    
    # Get container IP
    CONTAINER_IP=$(lxc list vxlan-test --format=json | jq -r '.[0].state.network.eth0.addresses[] | select(.family=="inet").address')
    
    if [ -n "$CONTAINER_IP" ]; then
        print_status "Test container got IP: $CONTAINER_IP"
        
        # Test ping from host to container
        if ping -c 1 -W 2 $CONTAINER_IP &>/dev/null; then
            print_status "Host can ping container ✓"
        else
            print_warning "Host cannot ping container (may need time for ARP)"
        fi
        
        # Test internet from container
        if lxc exec vxlan-test -- ping -c 1 8.8.8.8 &>/dev/null; then
            print_status "Container has internet access ✓"
        else
            print_warning "Container does not have internet access"
        fi
    else
        print_error "Container did not get an IP address"
    fi
    
    # Cleanup test container
    print_status "Cleaning up test container..."
    lxc delete vxlan-test --force
    
    print_status "Setup test completed"
}

# Function to show usage information
show_usage() {
    echo "Usage: $0 [OPTIONS]"
    echo ""
    echo "Options:"
    echo "  --cleanup    Remove all VXLAN/bridge configuration"
    echo "  --no-dhcp    Skip dnsmasq DHCP/DNS setup"
    echo "  --no-service Skip systemd service creation"
    echo "  --test-only  Only run tests on existing setup"
    echo "  --help       Show this help message"
    echo ""
    echo "Network Configuration:"
    echo "  Bridge Name: $BRIDGE_NAME"
    echo "  Bridge IP:   $BRIDGE_IP"
    echo "  Container Network: $CONTAINER_NETWORK"
    echo "  VXLAN ID:    $VXLAN_ID"
    echo ""
    echo "Container Usage:"
    echo "  Launch:  lxc launch images:debian/12 mycontainer --network=$BRIDGE_NAME"
    echo "  Attach:  lxc network attach $BRIDGE_NAME <container> eth0"
}

# Main execution
main() {
    local CLEANUP=0
    local NO_DHCP=0
    local NO_SERVICE=0
    local TEST_ONLY=0
    
    # Parse arguments
    while [[ $# -gt 0 ]]; do
        case $1 in
            --cleanup)
                CLEANUP=1
                shift
                ;;
            --no-dhcp)
                NO_DHCP=1
                shift
                ;;
            --no-service)
                NO_SERVICE=1
                shift
                ;;
            --test-only)
                TEST_ONLY=1
                shift
                ;;
            --help)
                show_usage
                exit 0
                ;;
            *)
                print_error "Unknown option: $1"
                show_usage
                exit 1
                ;;
        esac
    done
    
    echo "================================"
    echo "VXLAN Bridge Setup for LXC"
    echo "================================"
    echo ""
    
    if [ $CLEANUP -eq 1 ]; then
        cleanup_existing
        print_status "Cleanup completed"
        exit 0
    fi
    
    if [ $TEST_ONLY -eq 1 ]; then
        test_setup
        exit 0
    fi
    
    # Main setup
    cleanup_existing
    create_vxlan_bridge
    setup_routing
    configure_lxd
    
    if [ $NO_DHCP -eq 0 ]; then
        setup_dnsmasq
    fi
    
    if [ $NO_SERVICE -eq 0 ]; then
        create_systemd_service
    fi
    
    # Test the setup
    test_setup
    
    echo ""
    echo "================================"
    echo "Setup Complete!"
    echo "================================"
    echo ""
    echo "Network Details:"
    echo "  Bridge: $BRIDGE_NAME ($BRIDGE_IP)"
    echo "  Containers will get IPs in: $CONTAINER_NETWORK"
    echo ""
    echo "Launch containers with:"
    echo "  lxc launch images:debian/12 mycontainer --network=$BRIDGE_NAME"
    echo ""
    echo "Benefits:"
    echo "  ✓ No NAT between containers"
    echo "  ✓ Host can access containers directly"
    echo "  ✓ Works over WiFi or Ethernet"
    echo "  ✓ Internet access via host routing"
    echo "  ✓ Perfect for VoIP (FreeSWITCH, Kamailio)"
}

# Run main function
main "$@"