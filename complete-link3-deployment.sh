#!/bin/bash
# Complete Link3 Deployment: FRR BGP + WireGuard
# This script completes the full deployment

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
TIMESTAMP=$(date +%Y%m%d_%H%M%S)

echo "========================================"
echo "Complete Link3 Deployment"
echo "========================================"
echo ""

#
# Step 1: Configure FRR BGP on all nodes
#
echo "[Step 1/3] Configuring FRR BGP on all 3 nodes..."
echo ""

for node_num in 1 2 3; do
    case $node_num in
        1)
            host="123.200.0.50"
            hostname="SMSAppDBmaster"
            router_id="123.200.0.50"
            bgp_asn="65196"
            subnet="10.10.196.0/24"
            ;;
        2)
            host="123.200.0.117"
            hostname="spark"
            router_id="123.200.0.117"
            bgp_asn="65195"
            subnet="10.10.195.0/24"
            ;;
        3)
            host="123.200.0.51"
            hostname="SMSApplication"
            router_id="123.200.0.51"
            bgp_asn="65194"
            subnet="10.10.194.0/24"
            ;;
    esac

    echo "[Node $node_num] Configuring BGP on $hostname ($host)..."

    # Configure FRR
    sshpass -p 'TB@l38800' ssh -p 8210 -o StrictHostKeyChecking=no \
        -o UserKnownHostsFile=/dev/null -o LogLevel=ERROR tbsms@$host << ENDCONFIG
echo 'TB@l38800' | sudo -S tee /etc/frr/frr.conf > /dev/null << 'FRRCONF'
frr version 8.1
frr defaults traditional
hostname $hostname
log syslog informational
no ipv6 forwarding
service integrated-vtysh-config

router bgp $bgp_asn
 bgp router-id $router_id
 bgp log-neighbor-changes
 no bgp ebgp-requires-policy
 no bgp network import-check
 no bgp default ipv4-unicast

 neighbor BGP_PEERS peer-group
 neighbor BGP_PEERS remote-as external
 neighbor BGP_PEERS capability extended-nexthop

 neighbor 123.200.0.50 peer-group BGP_PEERS
 neighbor 123.200.0.117 peer-group BGP_PEERS
 neighbor 123.200.0.51 peer-group BGP_PEERS

 address-family ipv4 unicast
  neighbor BGP_PEERS activate
  neighbor BGP_PEERS soft-reconfiguration inbound
  network $subnet
 exit-address-family
!

line vty
!
FRRCONF

echo 'TB@l38800' | sudo -S sed -i 's/^bgpd=no/bgpd=yes/' /etc/frr/daemons
echo 'TB@l38800' | sudo -S systemctl restart frr
sleep 3
ENDCONFIG

    echo "[Node $node_num] ✓ BGP configured"
    echo ""
done

#
# Step 2: Deploy WireGuard on Node 3
#
echo "[Step 2/3] Deploying WireGuard VPN on Node 3 (123.200.0.51)..."
echo ""

sshpass -p 'TB@l38800' ssh -p 8210 -o StrictHostKeyChecking=no \
    -o UserKnownHostsFile=/dev/null -o LogLevel=ERROR tbsms@123.200.0.51 << 'ENDWG'
echo 'TB@l38800' | sudo -S apt-get update -qq
echo 'TB@l38800' | sudo -S apt-get install -y wireguard wireguard-tools

# Enable IP forwarding
echo 'TB@l38800' | sudo -S sed -i 's/#net.ipv4.ip_forward=1/net.ipv4.ip_forward=1/' /etc/sysctl.conf
echo 'TB@l38800' | sudo -S sysctl -p > /dev/null

# Generate keys
echo 'TB@l38800' | sudo -S mkdir -p /etc/wireguard
echo 'TB@l38800' | sudo -S bash -c 'cd /etc/wireguard && wg genkey | tee wg0_private.key | wg pubkey > wg0_public.key'
echo 'TB@l38800' | sudo -S chmod 600 /etc/wireguard/wg0_private.key

SERVER_PRIVATE_KEY=$(echo 'TB@l38800' | sudo -S cat /etc/wireguard/wg0_private.key)

# Create WireGuard config
echo 'TB@l38800' | sudo -S tee /etc/wireguard/wg0.conf > /dev/null <<WGCONF
[Interface]
Address = 10.100.0.1/24
ListenPort = 51820
PrivateKey = $SERVER_PRIVATE_KEY

# Enable packet forwarding for VPN clients
PostUp = iptables -A FORWARD -i wg0 -j ACCEPT; iptables -A FORWARD -o wg0 -j ACCEPT
PostDown = iptables -D FORWARD -i wg0 -j ACCEPT; iptables -D FORWARD -o wg0 -j ACCEPT

# Example client (uncomment and add client public keys as needed)
# [Peer]
# PublicKey = <client-public-key>
# AllowedIPs = 10.100.0.10/32
WGCONF

echo 'TB@l38800' | sudo -S chmod 600 /etc/wireguard/wg0.conf

# Start WireGuard
echo 'TB@l38800' | sudo -S wg-quick up wg0 2>/dev/null || true
echo 'TB@l38800' | sudo -S systemctl enable wg-quick@wg0

# Show public key for client configuration
echo ""
echo "=== WireGuard Server Public Key ==="
echo 'TB@l38800' | sudo -S cat /etc/wireguard/wg0_public.key
echo ""
ENDWG

echo "[Node 3] ✓ WireGuard deployed"
echo ""

#
# Step 3: Verify Deployment
#
echo "[Step 3/3] Verifying deployment..."
echo ""

echo "=== FRR BGP Status ==="
for node_num in 1 2 3; do
    case $node_num in
        1) host="123.200.0.50"; hostname="SMSAppDBmaster";;
        2) host="123.200.0.117"; hostname="spark";;
        3) host="123.200.0.51"; hostname="SMSApplication";;
    esac

    echo ""
    echo "Node $node_num ($hostname):"
    sshpass -p 'TB@l38800' ssh -p 8210 -o StrictHostKeyChecking=no \
        -o UserKnownHostsFile=/dev/null -o LogLevel=ERROR tbsms@$host \
        "echo 'TB@l38800' | sudo -S vtysh -c 'show ip bgp summary'" 2>&1 | grep -A 10 "BGP router identifier" || echo "  BGP not ready yet"
done

echo ""
echo "=== WireGuard Status ==="
sshpass -p 'TB@l38800' ssh -p 8210 -o StrictHostKeyChecking=no \
    -o UserKnownHostsFile=/dev/null -o LogLevel=ERROR tbsms@123.200.0.51 \
    "echo 'TB@l38800' | sudo -S wg show" 2>&1 || echo "WireGuard not running"

echo ""
echo "========================================"
echo "Deployment Complete!"
echo "========================================"
echo ""
echo "Next Steps:"
echo "1. Wait 30-60 seconds for BGP peers to establish"
echo "2. Verify BGP: ssh -p 8210 tbsms@123.200.0.51 \"sudo vtysh -c 'show ip bgp summary'\""
echo "3. Configure WireGuard clients with server endpoint: 123.200.0.51:51820"
echo "4. Push route to clients: AllowedIPs = 10.10.0.0/16"
echo ""
