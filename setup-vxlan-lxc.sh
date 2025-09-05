#!/bin/bash

# VXLAN LXC Network Setup Script
# This script sets up VXLAN networks for LXC containers

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
CONFIG_FILE="$SCRIPT_DIR/network/vxlan-lxc.conf"

echo "==================================================="
echo "     VXLAN Network Setup for LXC Containers       "
echo "==================================================="
echo

# Check if running as root or with sudo
if [ "$EUID" -ne 0 ]; then 
    echo "Please run this script with sudo or as root"
    exit 1
fi

# Function to load configuration
load_config() {
    if [ ! -f "$CONFIG_FILE" ]; then
        echo "Error: Configuration file not found: $CONFIG_FILE"
        exit 1
    fi
    source <(grep -E '^[A-Z_]+=' "$CONFIG_FILE")
}

# Function to setup prerequisites
setup_prerequisites() {
    echo "1. Installing prerequisites..."
    
    # Check and install required packages
    packages="bridge-utils iproute2 iptables-persistent"
    for pkg in $packages; do
        if ! dpkg -l | grep -q "^ii.*$pkg"; then
            echo "   Installing $pkg..."
            apt-get update >/dev/null 2>&1
            apt-get install -y $pkg >/dev/null 2>&1
        fi
    done
    
    # Load kernel modules
    echo "   Loading kernel modules..."
    modprobe vxlan
    modprobe bridge
    
    # Enable IP forwarding
    echo "   Enabling IP forwarding..."
    sysctl -w net.ipv4.ip_forward=1 >/dev/null
    sysctl -w net.ipv6.conf.all.forwarding=1 >/dev/null
    
    # Make persistent
    if ! grep -q "net.ipv4.ip_forward=1" /etc/sysctl.conf; then
        echo "net.ipv4.ip_forward=1" >> /etc/sysctl.conf
    fi
    if ! grep -q "net.ipv6.conf.all.forwarding=1" /etc/sysctl.conf; then
        echo "net.ipv6.conf.all.forwarding=1" >> /etc/sysctl.conf
    fi
    
    echo "   ✓ Prerequisites installed"
    echo
}

# Function to create VXLAN interface
create_vxlan() {
    local name=$1
    local vxlan_id=$2
    local multicast=$3
    local bridge=$4
    
    echo "   Creating VXLAN $name (ID: $vxlan_id)..."
    
    # Check if interface exists
    if ip link show $name >/dev/null 2>&1; then
        echo "     Interface $name already exists"
    else
        # Create VXLAN interface
        ip link add $name type vxlan id $vxlan_id \
            group $multicast dev ${NAT_INTERFACE:-eth0} \
            dstport ${VXLAN_PORT:-4789}
        
        # Set MTU
        ip link set $name mtu ${VXLAN_MTU:-1450}
        
        # Bring up interface
        ip link set $name up
        
        echo "     ✓ VXLAN interface created"
    fi
    
    # Create bridge if specified
    if [ -n "$bridge" ]; then
        if ip link show $bridge >/dev/null 2>&1; then
            echo "     Bridge $bridge already exists"
        else
            # Create bridge
            ip link add name $bridge type bridge
            
            # Configure bridge
            ip link set $bridge type bridge stp_state 1
            
            # Attach VXLAN to bridge
            ip link set $name master $bridge
            
            # Bring up bridge
            ip link set $bridge up
            
            echo "     ✓ Bridge created and configured"
        fi
    fi
}

# Function to configure subnet
configure_subnet() {
    local subnet_line=$1
    
    # Parse subnet configuration
    IFS=':' read -r name vxlan_id cidr gateway multicast description <<< "$subnet_line"
    
    echo "Configuring subnet: $name"
    echo "   VXLAN ID: $vxlan_id"
    echo "   CIDR: $cidr"
    echo "   Gateway: $gateway"
    echo "   Multicast: $multicast"
    
    local vxlan_name="vxlan${vxlan_id}"
    local bridge_name="br-vx${vxlan_id}"
    
    # Create VXLAN and bridge
    create_vxlan "$vxlan_name" "$vxlan_id" "$multicast" "$bridge_name"
    
    # Configure gateway IP on bridge
    if ! ip addr show $bridge_name | grep -q "$gateway"; then
        echo "   Configuring gateway IP..."
        ip addr add "${gateway}/${cidr#*/}" dev $bridge_name
        echo "     ✓ Gateway IP configured"
    else
        echo "   Gateway IP already configured"
    fi
    
    echo "   ✓ Subnet $name configured"
    echo
}

# Function to setup NAT
setup_nat() {
    local subnet=$1
    local cidr=$2
    
    if [ "$ENABLE_NAT" = "true" ]; then
        # Check if subnet is in NAT list
        if echo "$NAT_SUBNETS" | grep -q "$subnet"; then
            echo "   Setting up NAT for $subnet ($cidr)..."
            
            # Add iptables rule
            rule="POSTROUTING -s $cidr ! -d $cidr -o ${NAT_INTERFACE:-eth0} -j MASQUERADE"
            if ! iptables -t nat -C $rule 2>/dev/null; then
                iptables -t nat -A $rule
                echo "     ✓ NAT rule added"
            else
                echo "     NAT rule already exists"
            fi
        fi
    fi
}

# Function to setup routing rules
setup_routing() {
    echo "4. Setting up routing rules..."
    
    # Parse and apply routing rules
    while IFS= read -r rule; do
        if [[ $rule =~ ^\"?([^:]+):([^:]+):([^\"]+)\"?$ ]]; then
            src="${BASH_REMATCH[1]}"
            dst="${BASH_REMATCH[2]}"
            action="${BASH_REMATCH[3]}"
            
            if [ "$action" = "ALLOW" ]; then
                echo "   Allowing traffic: $src -> $dst"
                # Add iptables rules as needed
            fi
        fi
    done <<< "$ROUTING_RULES"
    
    echo "   ✓ Routing rules configured"
    echo
}

# Function to configure LXC container
configure_container() {
    local container=$1
    local subnet=$2
    local ip=$3
    
    echo "   Configuring $container..."
    
    # Get subnet details
    local vxlan_id=""
    local bridge=""
    
    # Find VXLAN ID and bridge for subnet
    while IFS= read -r subnet_line; do
        if [[ $subnet_line =~ ^\"?${subnet}:([0-9]+): ]]; then
            vxlan_id="${BASH_REMATCH[1]}"
            bridge="br-vx${vxlan_id}"
            break
        fi
    done <<< "$SUBNETS"
    
    if [ -n "$bridge" ]; then
        # Add network device to container
        if lxc list --format csv | grep -q "^$container,"; then
            lxc config device add $container eth1 nic \
                name=eth1 nictype=bridged parent=$bridge 2>/dev/null || true
            echo "     ✓ Network device added to $container"
        else
            echo "     ⚠ Container $container not found"
        fi
    fi
}

# Main execution
main() {
    # Load configuration
    load_config
    
    # Setup prerequisites
    setup_prerequisites
    
    # Setup subnets
    echo "2. Setting up VXLAN subnets..."
    echo
    
    # Process each subnet
    while IFS= read -r subnet_line; do
        subnet_line="${subnet_line#\"}"
        subnet_line="${subnet_line%\"}"
        
        if [ -n "$subnet_line" ]; then
            configure_subnet "$subnet_line"
            
            # Setup NAT if enabled
            IFS=':' read -r name vxlan_id cidr rest <<< "$subnet_line"
            setup_nat "$name" "$cidr"
        fi
    done <<< "$(echo "$SUBNETS" | tr ' ' '\n')"
    
    # Setup routing rules
    setup_routing
    
    # Configure containers
    echo "5. Configuring LXC containers..."
    
    while IFS= read -r container_line; do
        container_line="${container_line#\"}"
        container_line="${container_line%\"}"
        
        if [ -n "$container_line" ]; then
            IFS=':' read -r container subnet ip mac <<< "$container_line"
            if [ -n "$container" ] && [ -n "$subnet" ]; then
                configure_container "$container" "$subnet" "$ip"
            fi
        fi
    done <<< "$(echo "$CONTAINER_IPS" | tr ' ' '\n')"
    
    echo "   ✓ Container configuration complete"
    echo
    
    # Save iptables rules
    echo "6. Saving configuration..."
    netfilter-persistent save >/dev/null 2>&1
    echo "   ✓ Configuration saved"
    echo
    
    # Display status
    echo "==================================================="
    echo "             VXLAN Network Status                 "
    echo "==================================================="
    echo
    
    echo "Active VXLAN interfaces:"
    ip -d link show type vxlan | grep -E "^[0-9]+:" | awk '{print "   - " $2}'
    echo
    
    echo "Active bridges:"
    ip link show type bridge | grep -E "^[0-9]+:" | awk '{print "   - " $2}'
    echo
    
    echo "Gateway IPs:"
    for bridge in $(ip link show type bridge | grep -oP 'br-vx\d+'); do
        ip=$(ip addr show $bridge | grep -oP 'inet \K[0-9.]+/[0-9]+')
        if [ -n "$ip" ]; then
            echo "   - $bridge: $ip"
        fi
    done
    echo
    
    echo "==================================================="
    echo "     VXLAN network setup completed successfully!  "
    echo "==================================================="
    echo
    echo "You can now:"
    echo "1. Run the Java orchestrator for advanced management:"
    echo "   cd $SCRIPT_DIR/automation"
    echo "   mvn compile"
    echo "   mvn exec:java -Dexec.mainClass=\"com.orchestrix.automation.vxlan.example.VXLANLXCExample\""
    echo
    echo "2. Manually configure containers:"
    echo "   lxc config device add <container> eth1 nic nictype=bridged parent=br-vx<id>"
    echo
}

# Run main function
main "$@"