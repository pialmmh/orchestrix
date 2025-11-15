#!/bin/bash
#
# One-Click FRR BGP Deployment Script
# Based on working configuration from TelcoBright deployment
#
# Usage: ./deploy-frr-cluster.sh nodes.conf
#

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
CONFIG_FILE="${1:-nodes.conf}"

if [ ! -f "$CONFIG_FILE" ]; then
    echo "Error: Configuration file '$CONFIG_FILE' not found"
    echo "Usage: $0 nodes.conf"
    exit 1
fi

# Parse configuration file
declare -A NODE_MGMT_IP
declare -A NODE_OVERLAY_IP
declare -A NODE_AS_NUMBER
declare -A NODE_LXD_NETWORK
declare -A NODE_HOSTNAME
declare -A NODE_SSH_PORT
declare -A NODE_SSH_USER
declare -A NODE_SSH_PASS

echo "Loading configuration from $CONFIG_FILE..."
source "$CONFIG_FILE"

NODES=($(grep "^\[" "$CONFIG_FILE" | tr -d '[]'))

echo "Nodes to configure: ${NODES[@]}"
echo ""

# Function to execute SSH command
ssh_exec() {
    local node=$1
    local cmd=$2
    sshpass -p "${NODE_SSH_PASS[$node]}" ssh -p "${NODE_SSH_PORT[$node]}" \
        -o StrictHostKeyChecking=no \
        -o UserKnownHostsFile=/dev/null \
        -o LogLevel=ERROR \
        "${NODE_SSH_USER[$node]}@${NODE_MGMT_IP[$node]}" \
        "$cmd" 2>&1 | grep -v "Warning:"
}

# Function to install FRR
install_frr() {
    local node=$1
    echo "Installing FRR on $node..."
    
    ssh_exec "$node" "
        echo '${NODE_SSH_PASS[$node]}' | sudo -S bash -c '
            # Check if already installed
            if command -v vtysh &> /dev/null; then
                echo \"FRR already installed\"
                exit 0
            fi
            
            # Install FRR
            curl -s https://deb.frrouting.org/frr/keys.asc | apt-key add - 2>/dev/null
            echo \"deb https://deb.frrouting.org/frr \$(lsb_release -s -c) frr-stable\" > /etc/apt/sources.list.d/frr.list
            apt-get update -qq
            DEBIAN_FRONTEND=noninteractive apt-get install -y -qq frr frr-pythontools
            echo \"FRR installed successfully\"
        '
    "
}

# Function to enable BGP daemon
enable_bgp_daemon() {
    local node=$1
    echo "Enabling BGP daemon on $node..."
    
    ssh_exec "$node" "
        echo '${NODE_SSH_PASS[$node]}' | sudo -S bash -c '
            cat > /etc/frr/daemons << DAEMONS
bgpd=yes
ospfd=no
ospf6d=no
ripd=no
ripngd=no
isisd=no
pimd=no
ldpd=no
nhrpd=no
eigrpd=no
babeld=no
sharpd=no
pbrd=no
bfdd=no
fabricd=no
vrrpd=no
DAEMONS
            chmod 640 /etc/frr/daemons
        '
    "
}

# Function to generate BGP configuration
generate_bgp_config() {
    local node=$1
    local config_file="/tmp/frr-${node}.conf"
    
    cat > "$config_file" << BGP_CONFIG
frr defaults traditional
hostname ${NODE_HOSTNAME[$node]}
log syslog informational
no ipv6 forwarding
service integrated-vtysh-config
!
router bgp ${NODE_AS_NUMBER[$node]}
 bgp router-id ${NODE_OVERLAY_IP[$node]}
 no bgp ebgp-requires-policy
 no bgp network import-check
 !
BGP_CONFIG

    # Add neighbors
    for neighbor_node in "${NODES[@]}"; do
        if [ "$neighbor_node" != "$node" ]; then
            cat >> "$config_file" << NEIGHBOR_CONFIG
 neighbor ${NODE_OVERLAY_IP[$neighbor_node]} remote-as ${NODE_AS_NUMBER[$neighbor_node]}
 neighbor ${NODE_OVERLAY_IP[$neighbor_node]} description ${neighbor_node}-via-WireGuard
 neighbor ${NODE_OVERLAY_IP[$neighbor_node]} ebgp-multihop 2
 neighbor ${NODE_OVERLAY_IP[$neighbor_node]} timers 10 30
 !
NEIGHBOR_CONFIG
        fi
    done

    # Add address family
    cat >> "$config_file" << ADDRESS_FAMILY
 address-family ipv4 unicast
  network ${NODE_LXD_NETWORK[$node]}
ADDRESS_FAMILY

    for neighbor_node in "${NODES[@]}"; do
        if [ "$neighbor_node" != "$node" ]; then
            echo "  neighbor ${NODE_OVERLAY_IP[$neighbor_node]} activate" >> "$config_file"
        fi
    done

    cat >> "$config_file" << END_CONFIG
 exit-address-family
exit
!
line vty
!
END_CONFIG

    echo "$config_file"
}

# Function to deploy BGP configuration
deploy_bgp_config() {
    local node=$1
    local config_file=$2
    
    echo "Deploying BGP configuration to $node..."
    
    # Upload config
    sshpass -p "${NODE_SSH_PASS[$node]}" scp -P "${NODE_SSH_PORT[$node]}" \
        -o StrictHostKeyChecking=no \
        -o UserKnownHostsFile=/dev/null \
        -o LogLevel=ERROR \
        "$config_file" "${NODE_SSH_USER[$node]}@${NODE_MGMT_IP[$node]}:/tmp/frr.conf" 2>/dev/null
    
    # Move to correct location with proper permissions
    ssh_exec "$node" "
        echo '${NODE_SSH_PASS[$node]}' | sudo -S bash -c '
            cp /tmp/frr.conf /etc/frr/frr.conf
            chmod 640 /etc/frr/frr.conf
            chown frr:frr /etc/frr/frr.conf
        '
    "
}

# Function to restart FRR
restart_frr() {
    local node=$1
    echo "Restarting FRR on $node..."
    
    ssh_exec "$node" "
        echo '${NODE_SSH_PASS[$node]}' | sudo -S systemctl restart frr
    "
}

# Function to verify BGP
verify_bgp() {
    local node=$1
    echo "Verifying BGP on $node..."
    
    ssh_exec "$node" "
        echo '${NODE_SSH_PASS[$node]}' | sudo -S vtysh -c 'show ip bgp summary'
    " | tail -10
}

# Main deployment
echo "╔════════════════════════════════════════════════════════════════╗"
echo "║           FRR BGP Cluster Deployment - One-Click              ║"
echo "╚════════════════════════════════════════════════════════════════╝"
echo ""

# Step 1: Install FRR on all nodes
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "📍 Step 1: Installing FRR on all nodes"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
for node in "${NODES[@]}"; do
    install_frr "$node"
done
echo "✅ FRR installation complete"
echo ""

# Step 2: Enable BGP daemon
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "📍 Step 2: Enabling BGP daemon on all nodes"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
for node in "${NODES[@]}"; do
    enable_bgp_daemon "$node"
done
echo "✅ BGP daemon enabled on all nodes"
echo ""

# Step 3: Generate and deploy configurations
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "📍 Step 3: Generating and deploying BGP configurations"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
for node in "${NODES[@]}"; do
    config_file=$(generate_bgp_config "$node")
    deploy_bgp_config "$node" "$config_file"
    rm -f "$config_file"
done
echo "✅ BGP configurations deployed"
echo ""

# Step 4: Restart FRR
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "📍 Step 4: Restarting FRR on all nodes"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
for node in "${NODES[@]}"; do
    restart_frr "$node"
done
sleep 5
echo "✅ FRR restarted on all nodes"
echo ""

# Step 5: Verify BGP
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "📍 Step 5: Verifying BGP peering"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
for node in "${NODES[@]}"; do
    echo ""
    echo "$node BGP Summary:"
    verify_bgp "$node"
done

echo ""
echo "╔════════════════════════════════════════════════════════════════╗"
echo "║              FRR BGP Deployment Complete                      ║"
echo "╚════════════════════════════════════════════════════════════════╝"
echo ""
echo "All nodes configured with BGP over WireGuard overlay"
echo "Check BGP status with: sudo vtysh -c 'show ip bgp summary'"
echo ""
