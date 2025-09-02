#!/bin/bash

# manage-tunnels.sh - Helper script to manage SSH tunnels from the host
# This script provides easy commands to control tunnels in the container

CONTAINER_NAME="${1:-ssh-tunnel}"
shift

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
CYAN='\033[0;36m'
NC='\033[0m'

# Check if container exists and is running
check_container() {
    if ! lxc list --format=json | jq -r '.[].name' | grep -q "^${CONTAINER_NAME}$"; then
        echo -e "${RED}Container '${CONTAINER_NAME}' does not exist${NC}"
        exit 1
    fi
    
    STATUS=$(lxc list ${CONTAINER_NAME} --format=json | jq -r '.[0].status')
    if [ "$STATUS" != "Running" ]; then
        echo -e "${RED}Container '${CONTAINER_NAME}' is not running${NC}"
        echo -e "${YELLOW}Start it with: lxc start ${CONTAINER_NAME}${NC}"
        exit 1
    fi
}

# Show usage
show_usage() {
    echo -e "${CYAN}SSH Tunnel Manager${NC}"
    echo -e "Usage: $0 [container-name] <command> [options]"
    echo ""
    echo "Commands:"
    echo "  status              Show status of all tunnels"
    echo "  start [service]     Start tunnel(s)"
    echo "  stop [service]      Stop tunnel(s)"
    echo "  restart [service]   Restart tunnel(s)"
    echo "  list                List configured tunnels"
    echo "  test [service]      Test tunnel connectivity"
    echo "  logs [service]      Show tunnel logs"
    echo "  edit                Edit tunnel configuration"
    echo "  add                 Add a new tunnel interactively"
    echo "  remove <service>    Remove a tunnel configuration"
    echo "  monitor             Show real-time tunnel monitoring"
    echo ""
    echo "Examples:"
    echo "  $0 status                    # Show all tunnel status"
    echo "  $0 ssh-tunnel start MySQL    # Start MySQL tunnel"
    echo "  $0 ssh-tunnel test Redis     # Test Redis tunnel"
    echo "  $0 ssh-tunnel logs Kafka     # Show Kafka tunnel logs"
    exit 0
}

# Get container IP
get_container_ip() {
    lxc list ${CONTAINER_NAME} --format=json | jq -r '.[0].state.network.eth0.addresses[] | select(.family=="inet").address'
}

# Status command
cmd_status() {
    echo -e "${CYAN}Tunnel Status for ${CONTAINER_NAME}:${NC}"
    lxc exec ${CONTAINER_NAME} -- /usr/local/bin/tunnel-manager.sh status
    
    echo ""
    echo -e "${CYAN}Container IP: $(get_container_ip)${NC}"
}

# Start command
cmd_start() {
    local service=$1
    if [ -n "$service" ]; then
        echo -e "${YELLOW}Starting tunnel: $service${NC}"
        lxc exec ${CONTAINER_NAME} -- bash -c "
            source /usr/local/bin/tunnel-manager.sh
            grep '^$service|' /etc/ssh-tunnels/tunnels.conf | while IFS='|' read -r svc srv rp lp user pass port desc; do
                start_tunnel \"\$svc\" \"\$srv\" \"\$rp\" \"\$lp\" \"\$user\" \"\$pass\" \"\${port:-22}\"
            done
        "
    else
        echo -e "${YELLOW}Starting all tunnels...${NC}"
        lxc exec ${CONTAINER_NAME} -- systemctl start ssh-tunnels
    fi
    sleep 2
    cmd_status
}

# Stop command
cmd_stop() {
    local service=$1
    if [ -n "$service" ]; then
        echo -e "${YELLOW}Stopping tunnel: $service${NC}"
        lxc exec ${CONTAINER_NAME} -- bash -c "
            source /usr/local/bin/tunnel-manager.sh
            stop_tunnel '$service'
        "
    else
        echo -e "${YELLOW}Stopping all tunnels...${NC}"
        lxc exec ${CONTAINER_NAME} -- systemctl stop ssh-tunnels
    fi
    sleep 1
    cmd_status
}

# Restart command
cmd_restart() {
    local service=$1
    cmd_stop "$service"
    sleep 2
    cmd_start "$service"
}

# List tunnels
cmd_list() {
    echo -e "${CYAN}Configured Tunnels:${NC}"
    echo -e "${YELLOW}Service | Remote Server | Remote Port | Local Port | User | SSH Port | Description${NC}"
    echo "--------------------------------------------------------------------------------"
    lxc exec ${CONTAINER_NAME} -- bash -c "
        grep -v '^#' /etc/ssh-tunnels/tunnels.conf | grep -v '^\$' | while IFS='|' read -r svc srv rp lp user pass port desc; do
            printf '%-12s | %-15s | %-11s | %-10s | %-8s | %-8s | %s\n' \
                \"\$svc\" \"\$srv\" \"\$rp\" \"\$lp\" \"\$user\" \"\${port:-22}\" \"\$desc\"
        done
    "
}

# Test tunnel
cmd_test() {
    local service=$1
    CONTAINER_IP=$(get_container_ip)
    
    if [ -z "$service" ]; then
        echo -e "${CYAN}Testing all tunnels...${NC}"
        lxc exec ${CONTAINER_NAME} -- /usr/local/bin/check-tunnels.sh
    else
        echo -e "${CYAN}Testing tunnel: $service${NC}"
        PORT=$(lxc exec ${CONTAINER_NAME} -- grep "^$service|" /etc/ssh-tunnels/tunnels.conf | cut -d'|' -f4)
        if [ -n "$PORT" ]; then
            echo -e "Testing connection to ${CONTAINER_IP}:${PORT}..."
            if nc -z -w2 ${CONTAINER_IP} ${PORT} 2>/dev/null; then
                echo -e "${GREEN}✓ $service tunnel is working (port $PORT accessible)${NC}"
            else
                echo -e "${RED}✗ $service tunnel is not accessible on port $PORT${NC}"
            fi
        else
            echo -e "${RED}Service '$service' not found in configuration${NC}"
        fi
    fi
}

# Show logs
cmd_logs() {
    local service=$1
    if [ -n "$service" ]; then
        echo -e "${CYAN}Logs for $service tunnel:${NC}"
        lxc exec ${CONTAINER_NAME} -- tail -n 50 /var/log/ssh-tunnels/${service}.log 2>/dev/null || \
            echo -e "${RED}No logs found for $service${NC}"
    else
        echo -e "${CYAN}Recent tunnel activity:${NC}"
        lxc exec ${CONTAINER_NAME} -- bash -c "tail -n 20 /var/log/ssh-tunnels/*.log 2>/dev/null | head -50"
    fi
}

# Edit configuration
cmd_edit() {
    echo -e "${CYAN}Editing tunnel configuration...${NC}"
    lxc exec ${CONTAINER_NAME} -- nano /etc/ssh-tunnels/tunnels.conf
    echo -e "${YELLOW}Configuration updated. Restart tunnels to apply changes.${NC}"
}

# Add new tunnel interactively
cmd_add() {
    echo -e "${CYAN}Add New SSH Tunnel${NC}"
    read -p "Service name: " service
    read -p "Remote server IP: " server
    read -p "Remote port: " remote_port
    read -p "Local port (default: same as remote): " local_port
    local_port=${local_port:-$remote_port}
    read -p "SSH username: " ssh_user
    read -s -p "SSH password (leave empty for key auth): " ssh_pass
    echo
    read -p "SSH port (default: 22): " ssh_port
    ssh_port=${ssh_port:-22}
    read -p "Description: " description
    
    # Add to configuration
    NEW_LINE="${service}|${server}|${remote_port}|${local_port}|${ssh_user}|${ssh_pass}|${ssh_port}|${description}"
    lxc exec ${CONTAINER_NAME} -- bash -c "echo '$NEW_LINE' >> /etc/ssh-tunnels/tunnels.conf"
    
    echo -e "${GREEN}Tunnel added successfully!${NC}"
    echo -e "${YELLOW}Start it with: $0 start $service${NC}"
}

# Remove tunnel
cmd_remove() {
    local service=$1
    if [ -z "$service" ]; then
        echo -e "${RED}Please specify a service to remove${NC}"
        exit 1
    fi
    
    echo -e "${YELLOW}Removing tunnel: $service${NC}"
    lxc exec ${CONTAINER_NAME} -- bash -c "
        grep -v '^${service}|' /etc/ssh-tunnels/tunnels.conf > /tmp/tunnels.conf.tmp
        mv /tmp/tunnels.conf.tmp /etc/ssh-tunnels/tunnels.conf
    "
    cmd_stop "$service"
    echo -e "${GREEN}Tunnel removed${NC}"
}

# Monitor tunnels
cmd_monitor() {
    echo -e "${CYAN}Monitoring SSH Tunnels (Press Ctrl+C to exit)${NC}"
    echo ""
    
    while true; do
        clear
        echo -e "${CYAN}=== SSH Tunnel Monitor - $(date) ===${NC}"
        echo ""
        
        # Show status
        lxc exec ${CONTAINER_NAME} -- /usr/local/bin/tunnel-manager.sh status
        
        echo ""
        echo -e "${CYAN}Port Accessibility:${NC}"
        CONTAINER_IP=$(get_container_ip)
        
        lxc exec ${CONTAINER_NAME} -- bash -c "
            grep -v '^#' /etc/ssh-tunnels/tunnels.conf | grep -v '^\$' | while IFS='|' read -r svc srv rp lp rest; do
                if nc -z -w1 127.0.0.1 \$lp 2>/dev/null; then
                    echo -e '✓ '\$svc' (port '\$lp')'
                else
                    echo -e '✗ '\$svc' (port '\$lp')'
                fi
            done
        "
        
        echo ""
        echo -e "${CYAN}System Resources:${NC}"
        lxc exec ${CONTAINER_NAME} -- bash -c "
            echo \"CPU: \$(top -bn1 | grep 'Cpu(s)' | awk '{print \$2}')\"
            echo \"Memory: \$(free -h | grep Mem | awk '{print \$3 \" / \" \$2}')\"
            echo \"Connections: \$(netstat -an | grep ESTABLISHED | wc -l)\"
        "
        
        sleep 5
    done
}

# Main execution
check_container

case "${1:-help}" in
    status)
        cmd_status
        ;;
    start)
        cmd_start "$2"
        ;;
    stop)
        cmd_stop "$2"
        ;;
    restart)
        cmd_restart "$2"
        ;;
    list)
        cmd_list
        ;;
    test)
        cmd_test "$2"
        ;;
    logs)
        cmd_logs "$2"
        ;;
    edit)
        cmd_edit
        ;;
    add)
        cmd_add
        ;;
    remove)
        cmd_remove "$2"
        ;;
    monitor)
        cmd_monitor
        ;;
    help|--help|-h)
        show_usage
        ;;
    *)
        echo -e "${RED}Unknown command: $1${NC}"
        show_usage
        ;;
esac