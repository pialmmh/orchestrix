#!/bin/bash

# Setup Docker BGP Network on BDCOM Cluster
# This script creates custom Docker networks using BGP-advertised subnets

# Load cluster configuration
source "$(dirname "$0")/bdcom-cluster.conf"

# Colors for output
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color

log_success() {
    echo -e "${GREEN}✓${NC} $1"
}

log_warning() {
    echo -e "${YELLOW}⚠${NC} $1"
}

log_error() {
    echo -e "${RED}✗${NC} $1"
}

# Function: SSH execute command on node
ssh_exec() {
    local node=$1
    local cmd=$2
    sshpass -p "${NODE_SSH_PASS[$node]}" ssh \
        -p ${NODE_SSH_PORT[$node]} \
        -o StrictHostKeyChecking=no \
        -o UserKnownHostsFile=/dev/null \
        -o LogLevel=ERROR \
        ${NODE_SSH_USER[$node]}@${NODE_MGMT_IP[$node]} \
        "$cmd"
}

# Function: Check if Docker is installed
check_docker() {
    local node=$1
    echo "Checking Docker installation on $node..."

    if ssh_exec "$node" "which docker >/dev/null 2>&1"; then
        local version=$(ssh_exec "$node" "docker --version")
        log_success "Docker installed: $version"
        return 0
    else
        log_warning "Docker not installed on $node"
        return 1
    fi
}

# Function: Install Docker
install_docker() {
    local node=$1
    echo "Installing Docker on $node..."

    ssh_exec "$node" "echo '${NODE_SSH_PASS[$node]}' | sudo -S bash -c '
        # Remove old versions
        apt-get remove -y docker docker-engine docker.io containerd runc 2>/dev/null || true

        # Install prerequisites
        apt-get update -qq
        DEBIAN_FRONTEND=noninteractive apt-get install -y -qq \
            ca-certificates \
            curl \
            gnupg \
            lsb-release

        # Add Docker GPG key
        mkdir -p /etc/apt/keyrings
        curl -fsSL https://download.docker.com/linux/ubuntu/gpg | gpg --dearmor -o /etc/apt/keyrings/docker.gpg

        # Add Docker repository
        echo \"deb [arch=\$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.gpg] https://download.docker.com/linux/ubuntu \$(lsb_release -cs) stable\" > /etc/apt/sources.list.d/docker.list

        # Install Docker
        apt-get update -qq
        DEBIAN_FRONTEND=noninteractive apt-get install -y -qq docker-ce docker-ce-cli containerd.io docker-compose-plugin

        # Enable Docker service
        systemctl enable docker
        systemctl start docker

        echo \"Docker installed successfully\"
    '"

    if [ $? -eq 0 ]; then
        log_success "Docker installed on $node"
    else
        log_error "Failed to install Docker on $node"
    fi
}

# Function: Setup Docker BGP network
setup_docker_network() {
    local node=$1
    local subnet="${NODE_LXD_NETWORK[$node]}"

    # Extract network parameters
    local base_ip=$(echo $subnet | cut -d'/' -f1 | cut -d'.' -f1-3)
    local gateway="${base_ip}.1"
    local ip_range="${base_ip}.100/25"

    echo "Setting up Docker BGP network on $node..."
    echo "  Subnet: $subnet"
    echo "  Gateway: $gateway"
    echo "  IP Range: ${base_ip}.100-${base_ip}.199 (Docker containers)"

    # Create Docker network
    local result=$(ssh_exec "$node" "echo '${NODE_SSH_PASS[$node]}' | sudo -S docker network create \
        --driver=bridge \
        --subnet=$subnet \
        --gateway=$gateway \
        --ip-range=$ip_range \
        --opt \"com.docker.network.bridge.name\"=\"docker-bgp\" \
        bgp-net 2>&1")

    if echo "$result" | grep -q "already exists\|Error response"; then
        log_warning "Network already exists on $node, recreating..."
        ssh_exec "$node" "echo '${NODE_SSH_PASS[$node]}' | sudo -S docker network rm bgp-net 2>/dev/null"
        ssh_exec "$node" "echo '${NODE_SSH_PASS[$node]}' | sudo -S docker network create \
            --driver=bridge \
            --subnet=$subnet \
            --gateway=$gateway \
            --ip-range=$ip_range \
            --opt \"com.docker.network.bridge.name\"=\"docker-bgp\" \
            bgp-net" >/dev/null 2>&1
    fi

    # Verify network
    local verify=$(ssh_exec "$node" "sudo docker network inspect bgp-net 2>/dev/null | grep -A1 Subnet | grep -v Subnet | tr -d ' \",'")

    if [ ! -z "$verify" ]; then
        log_success "Docker BGP network created on $node"
        log_success "  Network ID: $(ssh_exec "$node" "sudo docker network ls | grep bgp-net | awk '{print \$1}'")"
        log_success "  Bridge: docker-bgp"
    else
        log_error "Failed to create Docker BGP network on $node"
    fi
}

# Function: Show network info
show_network_info() {
    local node=$1
    echo ""
    echo "=== Docker BGP Network Info: $node ==="
    ssh_exec "$node" "echo '${NODE_SSH_PASS[$node]}' | sudo -S docker network inspect bgp-net 2>/dev/null" | grep -A5 "IPAM\|Subnet\|Gateway"
}

# Function: Deploy test container
deploy_test_container() {
    local node=$1
    local subnet="${NODE_LXD_NETWORK[$node]}"
    local base_ip=$(echo $subnet | cut -d'/' -f1 | cut -d'.' -f1-3)
    local container_ip="${base_ip}.100"

    echo ""
    echo "Deploying test nginx container on $node..."
    echo "  Container IP: $container_ip"

    # Stop and remove if exists
    ssh_exec "$node" "echo '${NODE_SSH_PASS[$node]}' | sudo -S docker rm -f test-nginx 2>/dev/null" >/dev/null

    # Run test container
    ssh_exec "$node" "echo '${NODE_SSH_PASS[$node]}' | sudo -S docker run -d \
        --name test-nginx \
        --network bgp-net \
        --ip $container_ip \
        --restart unless-stopped \
        nginx:latest" >/dev/null 2>&1

    if [ $? -eq 0 ]; then
        log_success "Test container deployed at $container_ip"
        log_success "  Access from any server: curl http://$container_ip"
    else
        log_error "Failed to deploy test container"
    fi
}

# Main execution
main() {
    echo "========================================"
    echo "Docker BGP Network Setup"
    echo "========================================"
    echo ""

    if [ -z "${NODES}" ]; then
        log_error "Configuration not loaded. Make sure bdcom-cluster.conf exists."
        exit 1
    fi

    # Process each node
    for node in "${NODES[@]}"; do
        echo ""
        echo "========================================="
        echo "Processing: $node (${NODE_MGMT_IP[$node]})"
        echo "========================================="

        # Check/Install Docker
        if ! check_docker "$node"; then
            echo ""
            read -p "Install Docker on $node? (y/n) " -n 1 -r
            echo
            if [[ $REPLY =~ ^[Yy]$ ]]; then
                install_docker "$node"
            else
                log_warning "Skipping $node"
                continue
            fi
        fi

        # Setup Docker network
        echo ""
        setup_docker_network "$node"

        # Show network info
        show_network_info "$node"

        # Ask to deploy test container
        echo ""
        read -p "Deploy test nginx container on $node? (y/n) " -n 1 -r
        echo
        if [[ $REPLY =~ ^[Yy]$ ]]; then
            deploy_test_container "$node"
        fi
    done

    echo ""
    echo "========================================"
    echo "Setup Complete!"
    echo "========================================"
    echo ""
    echo "Next steps:"
    echo "1. Run containers using: docker run --network bgp-net --ip <IP> <image>"
    echo "2. Containers will be reachable from all servers via BGP"
    echo "3. No port forwarding needed!"
    echo ""
    echo "Example:"
    echo "  docker run -d --name myapp --network bgp-net --ip 10.10.196.101 nginx"
    echo "  curl http://10.10.196.101  # From any server!"
    echo ""
}

# Run main
main "$@"
