#!/bin/bash
# Add WireGuard Client Profiles to Link3 Server
# This script adds multiple client configurations to the wg-overlay interface

set -e

SERVER_HOST="123.200.0.51"
SERVER_PORT="8210"
SERVER_USER="tbsms"
SERVER_PASS="TB@l38800"
WG_INTERFACE="wg-overlay"
WG_CONF="/etc/wireguard/${WG_INTERFACE}.conf"

# Client configuration
CLIENT_IP_START=250  # Start from 10.9.9.250
CLIENT_COUNT=5       # Number of clients to create

echo "=========================================="
echo "Adding WireGuard Client Profiles"
echo "=========================================="
echo "Server: $SERVER_HOST"
echo "Interface: $WG_INTERFACE"
echo "Clients to create: $CLIENT_COUNT (IPs: 10.9.9.$CLIENT_IP_START - 10.9.9.$((CLIENT_IP_START + CLIENT_COUNT - 1)))"
echo "=========================================="
echo ""

# Create temporary directory for client configs
mkdir -p ./wireguard-clients
cd ./wireguard-clients

echo "Step 1: Getting server public key..."
SERVER_PUBLIC_KEY=$(sshpass -p "$SERVER_PASS" ssh -p $SERVER_PORT -o StrictHostKeyChecking=no ${SERVER_USER}@${SERVER_HOST} \
    "echo '$SERVER_PASS' | sudo -S cat /etc/wireguard/server_public.key 2>/dev/null || echo '$SERVER_PASS' | sudo -S wg show $WG_INTERFACE public-key")

echo "Server Public Key: $SERVER_PUBLIC_KEY"
echo ""

# Backup current configuration
echo "Step 2: Backing up current WireGuard configuration..."
sshpass -p "$SERVER_PASS" ssh -p $SERVER_PORT -o StrictHostKeyChecking=no ${SERVER_USER}@${SERVER_HOST} \
    "echo '$SERVER_PASS' | sudo -S cp $WG_CONF ${WG_CONF}.backup.\$(date +%Y%m%d_%H%M%S)"
echo "✓ Backup created"
echo ""

# Generate client configurations
echo "Step 3: Generating client configurations..."
echo ""

for i in $(seq 1 $CLIENT_COUNT); do
    CLIENT_NAME="client${i}"
    CLIENT_IP="10.9.9.$((CLIENT_IP_START + i - 1))"

    echo "  Creating $CLIENT_NAME ($CLIENT_IP)..."

    # Generate client keys locally
    CLIENT_PRIVATE_KEY=$(wg genkey)
    CLIENT_PUBLIC_KEY=$(echo "$CLIENT_PRIVATE_KEY" | wg pubkey)

    # Create client configuration file
    cat > ${CLIENT_NAME}.conf << EOF
[Interface]
Address = ${CLIENT_IP}/32
PrivateKey = ${CLIENT_PRIVATE_KEY}
DNS = 8.8.8.8

[Peer]
PublicKey = ${SERVER_PUBLIC_KEY}
Endpoint = ${SERVER_HOST}:51820
AllowedIPs = 10.9.9.0/24, 10.10.0.0/16
PersistentKeepalive = 25
EOF

    echo "    ✓ Client config: ${CLIENT_NAME}.conf"

    # Add peer to server configuration
    sshpass -p "$SERVER_PASS" ssh -p $SERVER_PORT -o StrictHostKeyChecking=no ${SERVER_USER}@${SERVER_HOST} \
        "echo '$SERVER_PASS' | sudo -S tee -a $WG_CONF > /dev/null" << PEER

# Client: ${CLIENT_NAME}
[Peer]
PublicKey = ${CLIENT_PUBLIC_KEY}
AllowedIPs = ${CLIENT_IP}/32
PEER

    echo "    ✓ Added to server configuration"
done

echo ""
echo "Step 4: Reloading WireGuard interface..."
sshpass -p "$SERVER_PASS" ssh -p $SERVER_PORT -o StrictHostKeyChecking=no ${SERVER_USER}@${SERVER_HOST} \
    "echo '$SERVER_PASS' | sudo -S wg syncconf $WG_INTERFACE <(echo '$SERVER_PASS' | sudo -S wg-quick strip $WG_INTERFACE)"

echo "✓ Configuration reloaded"
echo ""

echo "Step 5: Verifying peers..."
sshpass -p "$SERVER_PASS" ssh -p $SERVER_PORT -o StrictHostKeyChecking=no ${SERVER_USER}@${SERVER_HOST} \
    "echo '$SERVER_PASS' | sudo -S wg show $WG_INTERFACE peers"

echo ""
echo "=========================================="
echo "Client Profiles Created Successfully!"
echo "=========================================="
echo ""
echo "Client configuration files are in: $(pwd)/"
echo ""
ls -1 client*.conf | while read conf; do
    echo "  - $conf"
done
echo ""
echo "To use a client configuration:"
echo "  1. Copy the .conf file to your device"
echo "  2. Import it into your WireGuard client"
echo "  3. Connect!"
echo ""
echo "Testing connection:"
echo "  ping 10.10.197.183  # Debezium container on Link3"
echo "  ping 10.10.199.20   # Kafka broker on BDCOM"
echo ""
