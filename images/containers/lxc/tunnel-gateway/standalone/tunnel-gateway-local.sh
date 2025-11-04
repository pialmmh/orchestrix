#!/bin/bash
set -e

# Tunnel Gateway - Standalone Script (No LXC Required)
# Creates SSH tunnels directly on your local Ubuntu machine

# Color output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Trap Ctrl+C to clean up tunnels
cleanup() {
    echo ""
    echo -e "${YELLOW}Stopping all tunnels...${NC}"
    if [ ${#TUNNEL_PIDS[@]} -gt 0 ]; then
        for pid in "${TUNNEL_PIDS[@]}"; do
            if kill -0 "$pid" 2>/dev/null; then
                kill "$pid" 2>/dev/null || true
            fi
        done
        echo -e "${GREEN}All tunnels stopped${NC}"
    fi
    exit 0
}

trap cleanup SIGINT SIGTERM

# Arrays to track tunnel information
declare -a TUNNEL_PIDS
declare -a TUNNEL_NAMES
declare -a TUNNEL_LOCAL_PORTS
declare -a TUNNEL_SSH_HOSTS
declare -a TUNNEL_REMOTE_DESTS

# Check for config file argument or use default
if [ -z "$1" ]; then
    # Use default config file in the same directory as this script
    SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
    CONFIG_FILE="${SCRIPT_DIR}/local-tunnels.conf"

    if [ ! -f "$CONFIG_FILE" ]; then
        echo -e "${RED}No config file specified and default not found${NC}"
        echo ""
        echo -e "${YELLOW}Usage: $0 [config-file]${NC}"
        echo ""
        echo "Example: $0 my-tunnels.conf"
        echo ""
        echo "Default config file: local-tunnels.conf (in same directory)"
        echo "Expected at: $CONFIG_FILE"
        echo ""
        echo "The config file should use INI format with tunnel definitions."
        exit 1
    fi

    echo -e "${GREEN}Using default config: local-tunnels.conf${NC}"
else
    CONFIG_FILE="$1"

    if [ ! -f "$CONFIG_FILE" ]; then
        echo -e "${RED}Error: Config file '$CONFIG_FILE' not found${NC}"
        exit 1
    fi
fi

echo ""
echo "╔════════════════════════════════════════════════════════════════╗"
echo "║           Tunnel Gateway - Standalone Mode                    ║"
echo "║           SSH Tunneling Without Containers                    ║"
echo "╚════════════════════════════════════════════════════════════════╝"
echo ""

# Check dependencies
echo -e "${BLUE}Checking dependencies...${NC}"
MISSING_DEPS=()

if ! command -v ssh &> /dev/null; then
    MISSING_DEPS+=("openssh-client")
fi

if ! command -v sshpass &> /dev/null; then
    MISSING_DEPS+=("sshpass")
fi

if ! command -v netstat &> /dev/null && ! command -v ss &> /dev/null; then
    MISSING_DEPS+=("net-tools")
fi

if [ ${#MISSING_DEPS[@]} -gt 0 ]; then
    echo -e "${RED}Missing dependencies:${NC} ${MISSING_DEPS[*]}"
    echo ""
    echo "Install with:"
    echo "  sudo apt update"
    echo "  sudo apt install -y ${MISSING_DEPS[*]}"
    echo ""
    exit 1
fi

echo -e "${GREEN}✓ All dependencies installed${NC}"
echo ""

# Function to check if port is already in use
check_port() {
    local port=$1
    if netstat -tlnp 2>/dev/null | grep -q ":$port " || ss -tlnp 2>/dev/null | grep -q ":$port "; then
        return 0  # Port is in use
    else
        return 1  # Port is free
    fi
}

# Function to start a tunnel
start_tunnel() {
    local name="${tunnel[name]}"
    local ssh_addr="${tunnel[sshAddress]}"
    local ssh_user="${tunnel[sshUsername]}"
    local ssh_pass="${tunnel[sshPassword]}"
    local ssh_key="${tunnel[sshKeyFile]}"
    local ssh_port="${tunnel[sshPort]:-22}"
    local local_port="${tunnel[localPort]}"
    local remote_host="${tunnel[remoteHost]:-localhost}"
    local remote_port="${tunnel[remotePort]}"

    # Validate required fields
    if [ -z "$ssh_addr" ] || [ -z "$ssh_user" ] || [ -z "$local_port" ] || [ -z "$remote_port" ]; then
        echo -e "  ${RED}✗ Error: Missing required fields for tunnel '$name'${NC}"
        return 1
    fi

    echo ""
    echo -e "${BLUE}Starting tunnel: $name${NC}"
    echo "  SSH: $ssh_user@$ssh_addr:$ssh_port"
    echo "  Forward: 127.0.0.1:$local_port -> $remote_host:$remote_port"

    # Check if port is already in use
    if check_port "$local_port"; then
        echo -e "  ${YELLOW}⚠ Warning: Port $local_port is already in use${NC}"
        echo "  Skipping this tunnel"
        return 1
    fi

    # Build SSH command
    local ssh_opts="-N -p $ssh_port"
    ssh_opts="$ssh_opts -o StrictHostKeyChecking=no"
    ssh_opts="$ssh_opts -o UserKnownHostsFile=/dev/null"
    ssh_opts="$ssh_opts -o ServerAliveInterval=30"
    ssh_opts="$ssh_opts -o ServerAliveCountMax=3"
    ssh_opts="$ssh_opts -o LogLevel=ERROR"
    ssh_opts="$ssh_opts -L 127.0.0.1:${local_port}:${remote_host}:${remote_port}"

    # Start tunnel with appropriate auth in background
    if [ -n "$ssh_key" ]; then
        # Key authentication
        ssh $ssh_opts -i "$ssh_key" "${ssh_user}@${ssh_addr}" &
        TUNNEL_PID=$!
    elif [ -n "$ssh_pass" ]; then
        # Password authentication
        sshpass -p "$ssh_pass" ssh $ssh_opts "${ssh_user}@${ssh_addr}" &
        TUNNEL_PID=$!
    else
        echo -e "  ${RED}✗ Error: No authentication method specified (need sshPassword or sshKeyFile)${NC}"
        return 1
    fi

    # Wait a moment for tunnel to establish
    sleep 1

    # Check if process is still running
    if kill -0 "$TUNNEL_PID" 2>/dev/null; then
        TUNNEL_PIDS+=("$TUNNEL_PID")
        TUNNEL_NAMES+=("$name")
        TUNNEL_LOCAL_PORTS+=("$local_port")
        TUNNEL_SSH_HOSTS+=("$ssh_user@$ssh_addr:$ssh_port")
        TUNNEL_REMOTE_DESTS+=("$remote_host:$remote_port")
        echo -e "  ${GREEN}✓ Started (PID: $TUNNEL_PID)${NC}"
    else
        echo -e "  ${RED}✗ Failed to start (check SSH credentials/connectivity)${NC}"
        return 1
    fi
}

# Parse INI file and start tunnels
echo -e "${BLUE}Loading configuration from: $CONFIG_FILE${NC}"
echo ""

current_section=""
declare -A tunnel
tunnel_count=0

while IFS= read -r line || [ -n "$line" ]; do
    # Skip comments and empty lines
    [[ "$line" =~ ^[[:space:]]*# ]] && continue
    [[ -z "${line// }" ]] && continue

    # Check for section header [name]
    if [[ "$line" =~ ^\[(.+)\]$ ]]; then
        # If we have a previous tunnel, start it
        if [ -n "$current_section" ]; then
            start_tunnel && ((tunnel_count++))
        fi

        # Start new section
        current_section="${BASH_REMATCH[1]}"
        unset tunnel
        declare -A tunnel
        tunnel[name]="$current_section"
        continue
    fi

    # Parse key = value
    if [[ "$line" =~ ^[[:space:]]*([^=]+)[[:space:]]*=[[:space:]]*(.*)$ ]]; then
        key="${BASH_REMATCH[1]// }"
        value="${BASH_REMATCH[2]}"
        # Remove trailing comments
        value="${value%% #*}"
        # Trim whitespace
        value="${value## }"
        value="${value%% }"
        tunnel[$key]="$value"
    fi
done < "$CONFIG_FILE"

# Start the last tunnel
if [ -n "$current_section" ]; then
    start_tunnel && ((tunnel_count++))
fi

echo ""
echo "════════════════════════════════════════════════════════════════"
if [ $tunnel_count -eq 0 ]; then
    echo -e "${RED}No tunnels started!${NC}"
    echo "Check your configuration file and SSH connectivity."
    exit 1
fi

echo -e "${GREEN}✓ $tunnel_count tunnel(s) active${NC}"
echo "════════════════════════════════════════════════════════════════"
echo ""

# Show tunnel forwarding summary
echo "Port Forwarding Summary:"
echo "───────────────────────────────────────────────────────────────"
printf "%-20s %-25s → %-30s\n" "Tunnel" "Local" "Remote (via SSH)"
echo "───────────────────────────────────────────────────────────────"
for i in "${!TUNNEL_NAMES[@]}"; do
    local_info="127.0.0.1:${TUNNEL_LOCAL_PORTS[$i]}"
    ssh_info="${TUNNEL_SSH_HOSTS[$i]}"
    remote_info="${TUNNEL_REMOTE_DESTS[$i]}"
    printf "%-20s ${GREEN}%-25s${NC} → ${BLUE}%-30s${NC}\n" "${TUNNEL_NAMES[$i]}" "$local_info" "$remote_info"
    if [ $i -lt $((${#TUNNEL_NAMES[@]} - 1)) ]; then
        echo ""
    fi
done
echo "───────────────────────────────────────────────────────────────"
echo ""

echo "Your applications connect to:"
for i in "${!TUNNEL_NAMES[@]}"; do
    echo -e "  ${TUNNEL_NAMES[$i]}: ${GREEN}127.0.0.1:${TUNNEL_LOCAL_PORTS[$i]}${NC}"
done

echo ""
echo "════════════════════════════════════════════════════════════════"
echo -e "${GREEN}Tunnels are running!${NC}"
echo "════════════════════════════════════════════════════════════════"
echo ""
echo -e "${YELLOW}Press Ctrl+C to stop all tunnels${NC}"
echo ""

# Keep script running and monitor tunnels
while true; do
    sleep 5

    # Check if any tunnels died
    for i in "${!TUNNEL_PIDS[@]}"; do
        pid="${TUNNEL_PIDS[$i]}"
        if ! kill -0 "$pid" 2>/dev/null; then
            echo -e "${RED}⚠ Warning: Tunnel (PID $pid) died${NC}"
            unset 'TUNNEL_PIDS[$i]'
        fi
    done

    # If all tunnels died, exit
    if [ ${#TUNNEL_PIDS[@]} -eq 0 ]; then
        echo -e "${RED}All tunnels died. Exiting.${NC}"
        exit 1
    fi
done
