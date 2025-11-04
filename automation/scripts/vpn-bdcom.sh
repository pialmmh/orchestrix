#!/bin/bash

# VPN Connection Manager for BDCOM Servers
# Manages OpenVPN connection via NetworkManager to access servers at 10.255.246.x subnet

VPN_CONNECTION="Mostofa"

# Colors for output
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color

function show_usage() {
    cat <<EOF
Usage: $0 {start|stop|status|restart}

Commands:
    start    - Connect to VPN (via NetworkManager)
    stop     - Disconnect from VPN
    status   - Check VPN connection status
    restart  - Restart the VPN connection

BDCOM Servers accessible via VPN:
    Server 1: 10.255.246.173:15605 (sms-application)
    Server 2: 10.255.246.174:15605 (sms-app-master-db-dc)
    Server 3: 10.255.246.175:15605 (sms-app-slave-db-dc)

User: bdcom

GUI Access:
    You can also connect/disconnect from the system tray VPN menu.
    The connection is named: "$VPN_CONNECTION"
EOF
}

function start_vpn() {
    if is_vpn_running; then
        echo -e "${YELLOW}VPN is already connected${NC}"
        show_status
        return 1
    fi

    echo "Connecting to VPN via NetworkManager..."
    if nmcli connection up "$VPN_CONNECTION" &>/dev/null; then
        # Wait for connection to establish
        echo -n "Waiting for VPN to connect"
        for i in {1..10}; do
            sleep 1
            echo -n "."
            if ip addr show tun0 &>/dev/null; then
                echo
                echo -e "${GREEN}VPN connection established successfully${NC}"
                show_status
                return 0
            fi
        done
        echo
        echo -e "${RED}VPN connection failed to establish${NC}"
        return 1
    else
        echo -e "${RED}Failed to start VPN connection${NC}"
        echo "Try: nmcli connection up $VPN_CONNECTION"
        return 1
    fi
}

function stop_vpn() {
    if ! is_vpn_running; then
        echo -e "${YELLOW}VPN is not connected${NC}"
        return 1
    fi

    echo "Disconnecting from VPN..."
    if nmcli connection down "$VPN_CONNECTION" &>/dev/null; then
        sleep 1
        echo -e "${GREEN}VPN disconnected${NC}"
        return 0
    else
        echo -e "${RED}Failed to disconnect VPN${NC}"
        return 1
    fi
}

function is_vpn_running() {
    nmcli connection show --active | grep -q "^$VPN_CONNECTION"
}

function show_status() {
    echo
    echo "VPN Status:"
    echo "==========="

    if is_vpn_running; then
        PID=$(pgrep -f "openvpn.*$VPN_CONFIG")
        echo -e "Status: ${GREEN}Running${NC} (PID: $PID)"

        if ip addr show tun0 &>/dev/null; then
            VPN_IP=$(ip addr show tun0 | grep "inet " | awk '{print $2}' | cut -d/ -f1)
            echo "Interface: tun0"
            echo "VPN IP: $VPN_IP"
            echo
            echo "Testing connectivity to BDCOM servers..."
            test_connectivity
        else
            echo -e "${YELLOW}Warning: tun0 interface not found${NC}"
        fi

        echo
        echo "View logs: sudo tail -f $VPN_LOG"
    else
        echo -e "Status: ${RED}Not running${NC}"
    fi
}

function test_connectivity() {
    local servers=(
        "10.255.246.173:Server 1 (sms-application)"
        "10.255.246.174:Server 2 (sms-app-master-db-dc)"
        "10.255.246.175:Server 3 (sms-app-slave-db-dc)"
    )

    for server_info in "${servers[@]}"; do
        ip="${server_info%%:*}"
        name="${server_info#*:}"

        if timeout 2 bash -c "echo >/dev/tcp/$ip/15605" 2>/dev/null; then
            echo -e "  ${GREEN}✓${NC} $name - Reachable"
        else
            echo -e "  ${RED}✗${NC} $name - Not reachable"
        fi
    done
}

# Main script logic
case "${1:-}" in
    start)
        start_vpn
        ;;
    stop)
        stop_vpn
        ;;
    status)
        show_status
        ;;
    restart)
        echo "Restarting VPN connection..."
        stop_vpn
        sleep 2
        start_vpn
        ;;
    *)
        show_usage
        exit 1
        ;;
esac
