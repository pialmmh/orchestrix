#!/bin/bash
# Generate WireGuard VPN client configurations for Link3 and BDCOM
# Connects clients to netlab01 gateway for access to overlay and container networks

set -e

OUTPUT_DIR="deployments/netlab/output"
GATEWAY_NODE="telcobright@10.20.0.30"
GATEWAY_IP="10.20.0.30"
GATEWAY_PORT="51820"
GATEWAY_PUBKEY="cQDb4blw0nzPWC5MT5Tq4sJN9D7suD18bUuHJMagsEc="

# Client configurations
declare -A CLIENTS
CLIENTS[link3]="10.9.9.100"
CLIENTS[bdcom]="10.9.9.101"

echo "╔════════════════════════════════════════════════════════════════╗"
echo "║   WireGuard VPN Client Generator - Netlab Access             ║"
echo "╚════════════════════════════════════════════════════════════════╝"
echo ""

# Generate keys for each client
for client in "${!CLIENTS[@]}"; do
    client_ip="${CLIENTS[$client]}"
    config_file="$OUTPUT_DIR/${client}-vpn.conf"

    echo "Generating config for $client ($client_ip)..."

    # Generate private and public keys
    privkey=$(wg genkey)
    pubkey=$(echo "$privkey" | wg pubkey)

    # Create client config
    cat > "$config_file" <<EOF
# WireGuard VPN Configuration
# Client: $client
# Generated: $(date)
# Purpose: Remote access to netlab overlay and container networks

[Interface]
# Client private key
PrivateKey = $privkey

# Client VPN address
Address = $client_ip/32

# Optional: DNS for internal resolution
# DNS = 10.10.199.10

[Peer]
# Netlab Gateway (netlab01)
PublicKey = $GATEWAY_PUBKEY
Endpoint = $GATEWAY_IP:$GATEWAY_PORT

# Routes pushed to client:
# - 10.9.9.0/24: WireGuard overlay network (BGP mesh)
# - 10.10.0.0/16: Container supernet (all netlab apps/databases)
AllowedIPs = 10.9.9.0/24, 10.10.0.0/16

# Keepalive for NAT traversal
PersistentKeepalive = 25

# ============================================================
# Usage Instructions
# ============================================================
#
# Linux/Mac:
#   sudo cp $client-vpn.conf /etc/wireguard/netlab.conf
#   sudo wg-quick up netlab
#
# Windows:
#   Import this file in WireGuard GUI
#   Activate connection
#
# Verify connectivity after connection:
#   ping 10.9.9.1          # Gateway overlay IP
#   ping 10.9.9.2          # netlab02 overlay IP
#   ping 10.9.9.3          # netlab03 overlay IP
#   ping 10.10.199.1       # netlab01 lxdbr0 gateway
#   ping 10.10.198.1       # netlab02 lxdbr0 gateway
#   ping 10.10.197.1       # netlab03 lxdbr0 gateway
#
# Access containers (once they're deployed):
#   ping 10.10.199.10      # Example container in netlab01
#   mysql -h 10.10.198.20 -u root -p   # Example DB in netlab02
#
EOF

    chmod 600 "$config_file"

    echo "  ✓ Config created: $config_file"
    echo "  ✓ Client VPN IP: $client_ip"
    echo "  ✓ Client Public Key: $pubkey"
    echo ""
    echo "  Add this peer to netlab01 gateway:"
    echo "  ─────────────────────────────────────────────────────────────"
    echo "  ssh $GATEWAY_NODE"
    echo "  sudo wg set wg0 peer $pubkey allowed-ips $client_ip/32"
    echo "  sudo wg-quick save wg0"
    echo "  ─────────────────────────────────────────────────────────────"
    echo ""
done

echo "╔════════════════════════════════════════════════════════════════╗"
echo "║                   Generation Complete!                        ║"
echo "╚════════════════════════════════════════════════════════════════╝"
echo ""
echo "Generated VPN configs:"
for client in "${!CLIENTS[@]}"; do
    echo "  • $OUTPUT_DIR/${client}-vpn.conf"
done
echo ""
echo "Next steps:"
echo "  1. Add each client peer to netlab01 gateway (commands shown above)"
echo "  2. Copy client configs to Link3/BDCOM workstations"
echo "  3. Import configs in WireGuard client (Linux/Mac/Windows)"
echo "  4. Activate VPN connection"
echo "  5. Test connectivity with ping commands (shown in config files)"
echo ""
