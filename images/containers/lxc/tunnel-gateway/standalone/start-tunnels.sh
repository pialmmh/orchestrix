#!/bin/bash
# Simple Tunnel Starter - Production MySQL & Kafka
# Run this script to start both tunnels

echo "════════════════════════════════════════════════════════════════"
echo "Starting SSH Tunnels..."
echo "════════════════════════════════════════════════════════════════"
echo ""

# Start MySQL tunnel
echo "Starting MySQL tunnel..."
sshpass -p "TB@l38800" ssh -N -f -p 8210 \
    -o StrictHostKeyChecking=no \
    -o UserKnownHostsFile=/dev/null \
    -o ServerAliveInterval=30 \
    -o ServerAliveCountMax=3 \
    -o LogLevel=ERROR \
    -L 127.0.0.1:13306:localhost:3306 \
    tbsms@123.200.0.51

if [ $? -eq 0 ]; then
    echo "✓ MySQL tunnel started: 127.0.0.1:13306 → 123.200.0.51:3306"
else
    echo "✗ MySQL tunnel failed (port may be in use)"
fi

echo ""

# Start Kafka tunnel
echo "Starting Kafka tunnel..."
sshpass -p "TB@l38800" ssh -N -f -p 8210 \
    -o StrictHostKeyChecking=no \
    -o UserKnownHostsFile=/dev/null \
    -o ServerAliveInterval=30 \
    -o ServerAliveCountMax=3 \
    -o LogLevel=ERROR \
    -L 127.0.0.1:19092:localhost:29092 \
    tbsms@123.200.0.50

if [ $? -eq 0 ]; then
    echo "✓ Kafka tunnel started: 127.0.0.1:19092 → 123.200.0.50:29092"
else
    echo "✗ Kafka tunnel failed (port may be in use)"
fi

echo ""
echo "════════════════════════════════════════════════════════════════"
echo "Active Tunnels:"
echo "════════════════════════════════════════════════════════════════"

# Show active tunnel processes
ps aux | grep -E "ssh.*123.200.0.(50|51)" | grep -v grep | awk '{print "PID " $2 ": " $11 " " $12 " " $13}'

echo ""
echo "Listening Ports:"
netstat -tln 2>/dev/null | grep -E "127.0.0.1:(13306|19092)" || echo "No ports listening"

echo ""
echo "════════════════════════════════════════════════════════════════"
echo "Your Applications Connect To:"
echo "════════════════════════════════════════════════════════════════"
echo "MySQL:  127.0.0.1:13306"
echo "  JDBC: jdbc:mysql://127.0.0.1:13306/link3_sms"
echo ""
echo "Kafka:  127.0.0.1:19092"
echo "  Bootstrap: 127.0.0.1:19092"
echo ""
echo "════════════════════════════════════════════════════════════════"
echo "To stop tunnels: pkill -f 'ssh.*123.200.0'"
echo "════════════════════════════════════════════════════════════════"
