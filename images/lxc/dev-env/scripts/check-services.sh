#!/bin/bash

# Check tunnel health and restart if needed

CONFIG_FILE="/etc/ssh-tunnels/tunnels.conf"

check_port() {
    local port=$1
    nc -z -w1 127.0.0.1 $port 2>/dev/null
    return $?
}

echo "Checking tunnel health..."

while IFS='|' read -r service remote_server remote_port local_port rest; do
    [[ "$service" =~ ^#.*$ ]] && continue
    [[ -z "$service" ]] && continue
    
    service=$(echo "$service" | xargs)
    local_port=$(echo "$local_port" | xargs)
    
    if check_port "$local_port"; then
        echo "✓ $service (port $local_port) is accessible"
    else
        echo "✗ $service (port $local_port) is not accessible - restarting tunnel"
        /usr/local/bin/tunnel-manager.sh restart
        break
    fi
done < "$CONFIG_FILE"