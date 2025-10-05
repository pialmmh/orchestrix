#!/bin/bash
# Automated Consul Cluster Setup for Small Cloud (3-5 servers)
# Sets up a 3-node Consul cluster with optional clients

set -e

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

echo -e "${GREEN}=== Consul Cluster Setup ===${NC}\n"

# Configuration
CONSUL_VERSION="1.17.0"
DATACENTER="dc1"

# Server configuration (IP addresses)
# Format: "hostname:ip_address:role"
# role: server or client
NODES=(
  "server-1:192.168.1.100:server"
  "server-2:192.168.1.101:server"
  "server-3:192.168.1.102:server"
  "server-4:192.168.1.103:client"
  "server-5:192.168.1.104:client"
)

# SSH configuration
SSH_USER="root"
SSH_KEY_PATH=""  # Leave empty to use password auth

# Extract server IPs for retry_join
SERVER_IPS=()
for node in "${NODES[@]}"; do
  IFS=':' read -r hostname ip role <<< "$node"
  if [ "$role" == "server" ]; then
    SERVER_IPS+=("$ip")
  fi
done

RETRY_JOIN_LIST=$(printf '"%s", ' "${SERVER_IPS[@]}")
RETRY_JOIN_LIST="[${RETRY_JOIN_LIST%, }]"

echo "Cluster Configuration:"
echo "  Datacenter: ${DATACENTER}"
echo "  Consul Version: ${CONSUL_VERSION}"
echo "  Servers: ${#SERVER_IPS[@]}"
echo "  Total Nodes: ${#NODES[@]}"
echo ""

read -p "Proceed with installation? (y/N): " -n 1 -r
echo
if [[ ! $REPLY =~ ^[Yy]$ ]]; then
  echo -e "${YELLOW}Installation cancelled${NC}"
  exit 0
fi

# Function to execute SSH command
ssh_exec() {
  local host=$1
  shift
  if [ -n "$SSH_KEY_PATH" ]; then
    ssh -i "$SSH_KEY_PATH" -o StrictHostKeyChecking=no "${SSH_USER}@${host}" "$@"
  else
    ssh -o StrictHostKeyChecking=no "${SSH_USER}@${host}" "$@"
  fi
}

# Function to copy file via SCP
scp_copy() {
  local src=$1
  local host=$2
  local dest=$3
  if [ -n "$SSH_KEY_PATH" ]; then
    scp -i "$SSH_KEY_PATH" -o StrictHostKeyChecking=no "$src" "${SSH_USER}@${host}:${dest}"
  else
    scp -o StrictHostKeyChecking=no "$src" "${SSH_USER}@${host}:${dest}"
  fi
}

# Install Consul on each node
for node in "${NODES[@]}"; do
  IFS=':' read -r hostname ip role <<< "$node"

  echo -e "\n${GREEN}[${hostname}] Installing Consul...${NC}"

  ssh_exec "$ip" bash <<'EOF'
    # Download Consul
    cd /tmp
    wget -q https://releases.hashicorp.com/consul/1.17.0/consul_1.17.0_linux_amd64.zip
    unzip -o consul_1.17.0_linux_amd64.zip

    # Install binary
    sudo mv consul /usr/local/bin/
    sudo chmod +x /usr/local/bin/consul

    # Create directories
    sudo mkdir -p /etc/consul.d /var/lib/consul

    # Create consul user
    sudo useradd -r -s /bin/false consul 2>/dev/null || true
    sudo chown -R consul:consul /var/lib/consul

    # Cleanup
    rm consul_1.17.0_linux_amd64.zip

    echo "✓ Consul installed"
EOF

  # Generate configuration based on role
  if [ "$role" == "server" ]; then
    echo -e "${GREEN}[${hostname}] Configuring as Consul SERVER${NC}"

    cat > /tmp/consul-config.hcl <<EOF
datacenter = "${DATACENTER}"
data_dir = "/var/lib/consul"
log_level = "INFO"

server = true
bootstrap_expect = ${#SERVER_IPS[@]}

bind_addr = "${ip}"
client_addr = "0.0.0.0"

retry_join = ${RETRY_JOIN_LIST}

ui_config {
  enabled = true
}

performance {
  raft_multiplier = 1
}
EOF
  else
    echo -e "${GREEN}[${hostname}] Configuring as Consul CLIENT${NC}"

    cat > /tmp/consul-config.hcl <<EOF
datacenter = "${DATACENTER}"
data_dir = "/var/lib/consul"
log_level = "INFO"

server = false

bind_addr = "${ip}"
client_addr = "0.0.0.0"

retry_join = ${RETRY_JOIN_LIST}
EOF
  fi

  # Copy configuration
  scp_copy /tmp/consul-config.hcl "$ip" /tmp/consul-config.hcl
  ssh_exec "$ip" "sudo mv /tmp/consul-config.hcl /etc/consul.d/consul.hcl"
  ssh_exec "$ip" "sudo chown consul:consul /etc/consul.d/consul.hcl"

  # Create systemd service
  ssh_exec "$ip" bash <<'EOF'
    sudo tee /etc/systemd/system/consul.service > /dev/null <<'UNIT'
[Unit]
Description=Consul Agent
Documentation=https://www.consul.io/
After=network-online.target
Wants=network-online.target

[Service]
Type=notify
User=consul
Group=consul
ExecStart=/usr/local/bin/consul agent -config-dir=/etc/consul.d/
ExecReload=/bin/kill -HUP $MAINPID
KillMode=process
KillSignal=SIGTERM
Restart=on-failure
LimitNOFILE=65536

[Install]
WantedBy=multi-user.target
UNIT

    sudo systemctl daemon-reload
    echo "✓ Systemd service created"
EOF

  echo -e "${GREEN}[${hostname}] ✓ Configuration complete${NC}"
done

# Start Consul on all servers first
echo -e "\n${GREEN}Starting Consul servers...${NC}"
for node in "${NODES[@]}"; do
  IFS=':' read -r hostname ip role <<< "$node"
  if [ "$role" == "server" ]; then
    echo "  Starting Consul on ${hostname}..."
    ssh_exec "$ip" "sudo systemctl enable consul && sudo systemctl start consul"
  fi
done

echo "Waiting for cluster to form (15 seconds)..."
sleep 15

# Start Consul clients
echo -e "\n${GREEN}Starting Consul clients...${NC}"
for node in "${NODES[@]}"; do
  IFS=':' read -r hostname ip role <<< "$node"
  if [ "$role" == "client" ]; then
    echo "  Starting Consul on ${hostname}..."
    ssh_exec "$ip" "sudo systemctl enable consul && sudo systemctl start consul"
  fi
done

echo "Waiting for clients to join (5 seconds)..."
sleep 5

# Verify cluster
echo -e "\n${GREEN}=== Cluster Status ===${NC}\n"

FIRST_SERVER_IP="${SERVER_IPS[0]}"
ssh_exec "$FIRST_SERVER_IP" "consul members"

echo ""
ssh_exec "$FIRST_SERVER_IP" "consul operator raft list-peers"

echo -e "\n${GREEN}=== Installation Complete ===${NC}\n"
echo "Consul UI: http://${FIRST_SERVER_IP}:8500/ui"
echo ""
echo -e "${YELLOW}Next steps:${NC}"
echo "1. Verify all nodes are visible: consul members"
echo "2. Access Consul UI: http://${FIRST_SERVER_IP}:8500/ui"
echo "3. Configure your services to register with Consul"
echo ""
echo "Example for Go-ID container:"
echo "  lxc config set go-id-shard-0 environment.CONSUL_URL \"http://${FIRST_SERVER_IP}:8500\""
echo ""
