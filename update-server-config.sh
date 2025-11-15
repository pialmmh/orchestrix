#!/bin/bash
# Update WireGuard server configuration with client peers

set -e

SERVER_HOST="123.200.0.51"
SERVER_PORT="8210"
SERVER_USER="tbsms"
SERVER_PASS="TB@l38800"
WG_CONF="/etc/wireguard/wg-overlay.conf"

echo "Extracting client public keys..."
echo ""

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

# Create temporary file with peer configurations
TEMP_PEERS=$(mktemp)

for i in {1..5}; do
    CLIENT_FILE="$SCRIPT_DIR/wireguard-clients/client$i.conf"

    if [ ! -f "$CLIENT_FILE" ]; then
        echo "Error: $CLIENT_FILE not found"
        continue
    fi

    PRIV_KEY=$(grep "PrivateKey" "$CLIENT_FILE" | cut -d= -f2 | tr -d ' ')
    PUB_KEY=$(echo "$PRIV_KEY" | wg pubkey)
    IP=$(grep "Address" "$CLIENT_FILE" | cut -d= -f2 | tr -d ' ')

    echo "client$i: $IP -> $PUB_KEY"

    cat >> "$TEMP_PEERS" << PEER

# Client: client$i
[Peer]
PublicKey = $PUB_KEY
AllowedIPs = $IP
PEER

done

echo ""
echo "Peer configurations:"
cat "$TEMP_PEERS"

echo ""
echo "Updating server configuration..."

# Upload and append peers to server config
sshpass -p "$SERVER_PASS" scp -P $SERVER_PORT -o StrictHostKeyChecking=no "$TEMP_PEERS" ${SERVER_USER}@${SERVER_HOST}:/tmp/new_peers.txt

sshpass -p "$SERVER_PASS" ssh -p $SERVER_PORT -o StrictHostKeyChecking=no ${SERVER_USER}@${SERVER_HOST} \
    "echo '$SERVER_PASS' | sudo -S bash -c 'cat /tmp/new_peers.txt >> $WG_CONF'"

# Restart WireGuard
echo "Restarting WireGuard interface..."
sshpass -p "$SERVER_PASS" ssh -p $SERVER_PORT -o StrictHostKeyChecking=no ${SERVER_USER}@${SERVER_HOST} \
    "echo '$SERVER_PASS' | sudo -S wg-quick down wg-overlay && echo '$SERVER_PASS' | sudo -S wg-quick up wg-overlay"

echo ""
echo "Verifying peers..."
sshpass -p "$SERVER_PASS" ssh -p $SERVER_PORT -o StrictHostKeyChecking=no ${SERVER_USER}@${SERVER_HOST} \
    "echo '$SERVER_PASS' | sudo -S wg show wg-overlay"

rm "$TEMP_PEERS"

echo ""
echo "✓ Server configuration updated successfully!"
