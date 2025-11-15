#!/bin/bash
# Quick connect to BDCOM server with X11 forwarding
# Usage: ./connect-bdcom-x11.sh

SERVER="10.255.246.173"
PORT="15605"
USER="bdcom"

echo "╔════════════════════════════════════════════════════════════════╗"
echo "║         Connecting to BDCOM Server with X11 Forwarding        ║"
echo "╚════════════════════════════════════════════════════════════════╝"
echo ""
echo "Server: $SERVER"
echo "Port: $PORT"
echo "User: $USER"
echo "X11 Forwarding: Enabled (Trusted)"
echo ""
echo "After connecting, you can run:"
echo "  - wireshark &           (Start Wireshark GUI)"
echo "  - xterm &               (Open X terminal)"
echo "  - xeyes &               (Test X11 with simple app)"
echo ""
echo "Connecting..."
echo ""

# Connect with trusted X11 forwarding and compression
ssh -Y -C \
    -p "$PORT" \
    -o StrictHostKeyChecking=no \
    -o UserKnownHostsFile=/dev/null \
    -o ServerAliveInterval=60 \
    -o ServerAliveCountMax=3 \
    "$USER@$SERVER"
