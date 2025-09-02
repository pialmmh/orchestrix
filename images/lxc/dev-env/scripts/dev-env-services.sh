#!/bin/bash

# Tunnel Manager - Manages SSH tunnels based on configuration

CONFIG_FILE="/etc/ssh-tunnels/tunnels.conf"
PID_DIR="/var/run/ssh-tunnels"
LOG_DIR="/var/log/ssh-tunnels"

# Create directories
mkdir -p "$PID_DIR"
mkdir -p "$LOG_DIR"

# Load configuration
if [ ! -f "$CONFIG_FILE" ]; then
    echo "Configuration file not found: $CONFIG_FILE"
    exit 1
fi

start_tunnel() {
    local service=$1
    local remote_server=$2
    local remote_port=$3
    local local_port=$4
    local ssh_user=$5
    local ssh_pass=$6
    local ssh_port=${7:-22}
    
    echo "Starting tunnel for $service..."
    
    # Kill existing tunnel if running
    stop_tunnel "$service"
    
    # Check if autossh is available
    if which autossh >/dev/null 2>&1; then
        # Use autossh for auto-reconnection
        if [ -n "$ssh_pass" ] && which sshpass >/dev/null 2>&1; then
            # Use sshpass for password authentication
            AUTOSSH_GATETIME=0 AUTOSSH_PORT=0 sshpass -p "$ssh_pass" \
                autossh -M 0 -f -N \
                -o "ServerAliveInterval=30" \
                -o "ServerAliveCountMax=3" \
                -o "StrictHostKeyChecking=no" \
                -o "UserKnownHostsFile=/dev/null" \
                -o "CheckHostIP=no" \
                -o "LogLevel=ERROR" \
                -o "ExitOnForwardFailure=yes" \
                -L 0.0.0.0:${local_port}:localhost:${remote_port} \
                -p ${ssh_port} \
                ${ssh_user}@${remote_server} \
                2>&1 | tee -a "$LOG_DIR/${service}.log" &
        else
            # Use key-based authentication or no sshpass available
            AUTOSSH_GATETIME=0 AUTOSSH_PORT=0 autossh -M 0 -f -N \
                -o "ServerAliveInterval=30" \
                -o "ServerAliveCountMax=3" \
                -o "StrictHostKeyChecking=no" \
                -o "UserKnownHostsFile=/dev/null" \
                -o "CheckHostIP=no" \
                -o "LogLevel=ERROR" \
                -o "ExitOnForwardFailure=yes" \
                -L 0.0.0.0:${local_port}:localhost:${remote_port} \
                -p ${ssh_port} \
                ${ssh_user}@${remote_server} \
                2>&1 | tee -a "$LOG_DIR/${service}.log" &
        fi
    else
        # Fall back to regular ssh with while loop for reconnection
        echo "Using ssh (autossh not available)..."
        if [ -n "$ssh_pass" ] && which sshpass >/dev/null 2>&1; then
            # Use sshpass for password authentication
            while true; do
                sshpass -p "$ssh_pass" ssh -N \
                    -o "ServerAliveInterval=30" \
                    -o "ServerAliveCountMax=3" \
                    -o "StrictHostKeyChecking=no" \
                    -o "UserKnownHostsFile=/dev/null" \
                    -o "CheckHostIP=no" \
                    -o "LogLevel=ERROR" \
                    -o "ExitOnForwardFailure=yes" \
                    -L 0.0.0.0:${local_port}:localhost:${remote_port} \
                    -p ${ssh_port} \
                    ${ssh_user}@${remote_server} \
                    2>&1 | tee -a "$LOG_DIR/${service}.log"
                echo "Tunnel disconnected, reconnecting in 10 seconds..."
                sleep 10
            done &
        else
            # Use key-based authentication or password without sshpass
            if [ -n "$ssh_pass" ]; then
                echo "Warning: Password provided but sshpass not available. Use SSH keys instead."
            fi
            while true; do
                ssh -N \
                    -o "ServerAliveInterval=30" \
                    -o "ServerAliveCountMax=3" \
                    -o "StrictHostKeyChecking=no" \
                    -o "UserKnownHostsFile=/dev/null" \
                    -o "CheckHostIP=no" \
                    -o "LogLevel=ERROR" \
                    -o "ExitOnForwardFailure=yes" \
                    -L 0.0.0.0:${local_port}:localhost:${remote_port} \
                    -p ${ssh_port} \
                    ${ssh_user}@${remote_server} \
                    2>&1 | tee -a "$LOG_DIR/${service}.log"
                echo "Tunnel disconnected, reconnecting in 10 seconds..."
                sleep 10
            done &
        fi
    fi
    
    # Save PID
    echo $! > "$PID_DIR/${service}.pid"
    echo "Tunnel for $service started (PID: $!)"
}

stop_tunnel() {
    local service=$1
    
    if [ -f "$PID_DIR/${service}.pid" ]; then
        local pid=$(cat "$PID_DIR/${service}.pid")
        if kill -0 $pid 2>/dev/null; then
            kill $pid
            echo "Tunnel for $service stopped"
        fi
        rm -f "$PID_DIR/${service}.pid"
    fi
    
    # Also kill any orphaned ssh processes for this service
    pkill -f "L 0.0.0.0:.*${service}" 2>/dev/null || true
}

status_tunnel() {
    local service=$1
    
    if [ -f "$PID_DIR/${service}.pid" ]; then
        local pid=$(cat "$PID_DIR/${service}.pid")
        if kill -0 $pid 2>/dev/null; then
            echo "$service: Running (PID: $pid)"
            return 0
        else
            echo "$service: Stopped (stale PID file)"
            return 1
        fi
    else
        echo "$service: Stopped"
        return 1
    fi
}

# Main command processing
case "${1:-start}" in
    start)
        echo "Starting all tunnels..."
        # Parse config and start tunnels
        while IFS='|' read -r service remote_server remote_port local_port ssh_user ssh_pass ssh_port desc; do
            # Skip comments and empty lines
            [[ "$service" =~ ^#.*$ ]] && continue
            [[ -z "$service" ]] && continue
            
            # Trim whitespace
            service=$(echo "$service" | xargs)
            remote_server=$(echo "$remote_server" | xargs)
            remote_port=$(echo "$remote_port" | xargs)
            local_port=$(echo "$local_port" | xargs)
            ssh_user=$(echo "$ssh_user" | xargs)
            ssh_pass=$(echo "$ssh_pass" | xargs)
            ssh_port=$(echo "${ssh_port:-22}" | xargs)
            
            start_tunnel "$service" "$remote_server" "$remote_port" "$local_port" "$ssh_user" "$ssh_pass" "$ssh_port"
            sleep 2
        done < "$CONFIG_FILE"
        ;;
    
    stop)
        echo "Stopping all tunnels..."
        for pidfile in "$PID_DIR"/*.pid; do
            [ -f "$pidfile" ] || continue
            service=$(basename "$pidfile" .pid)
            stop_tunnel "$service"
        done
        ;;
    
    restart)
        $0 stop
        sleep 2
        $0 start
        ;;
    
    status)
        echo "Tunnel Status:"
        echo "------------------------"
        while IFS='|' read -r service rest; do
            [[ "$service" =~ ^#.*$ ]] && continue
            [[ -z "$service" ]] && continue
            service=$(echo "$service" | xargs)
            status_tunnel "$service"
        done < "$CONFIG_FILE"
        ;;
    
    *)
        echo "Usage: $0 {start|stop|restart|status}"
        exit 1
        ;;
esac