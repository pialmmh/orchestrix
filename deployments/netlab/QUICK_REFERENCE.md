# Netlab Quick Reference Card

## 🔑 Quick Access

### SSH to Nodes
```bash
ssh telcobright@10.20.0.30  # netlab01 (gateway)
ssh telcobright@10.20.0.31  # netlab02
ssh telcobright@10.20.0.32  # netlab03
# Password: (stored in deployments/netlab/.secrets/ssh-password.txt)
```

### IP Address Summary
| Node     | Management  | Overlay   | Container Subnet | AS    |
|----------|-------------|-----------|------------------|-------|
| netlab01 | 10.20.0.30  | 10.9.9.1  | 10.10.199.0/24   | 65199 |
| netlab02 | 10.20.0.31  | 10.9.9.2  | 10.10.198.0/24   | 65198 |
| netlab03 | 10.20.0.32  | 10.9.9.3  | 10.10.197.0/24   | 65197 |
| link3    | -           | 10.9.9.100| (VPN client)     | -     |
| bdcom    | -           | 10.9.9.101| (VPN client)     | -     |

---

## 🔧 Common Commands

### Check WireGuard Status
```bash
sudo wg show wg-overlay
```

### Check BGP Status
```bash
# Summary
sudo vtysh -c 'show ip bgp summary'

# All routes
sudo vtysh -c 'show ip bgp'

# Specific neighbor
sudo vtysh -c 'show ip bgp neighbor 10.9.9.2'
```

### Check Routing Table
```bash
# All routes
ip route

# BGP-learned routes only
ip route | grep 10.10
```

### Test Connectivity
```bash
# Test overlay
ping 10.9.9.2  # To netlab02
ping 10.9.9.3  # To netlab03

# Test container gateways
ping 10.10.199.1  # netlab01 lxdbr0
ping 10.10.198.1  # netlab02 lxdbr0
ping 10.10.197.1  # netlab03 lxdbr0
```

---

## 🚀 Automation Scripts

### Deploy Everything (One-Click)
```bash
mvn compile exec:java -Dexec.mainClass="com.telcobright.orchestrix.automation.example.NetlabDeployment"
```

### Add VPN Clients
```bash
# Generate new client configs
./scripts/generate-vpn-clients.sh

# Add to gateway
mvn compile exec:java -Dexec.mainClass="com.telcobright.orchestrix.automation.example.AddVpnClients"
```

### Check Status
```bash
mvn -q compile exec:java -Dexec.mainClass="com.telcobright.orchestrix.automation.example.CheckNetlabStatus"
```

---

## 📡 VPN Client Setup

### Linux/Mac
```bash
# Install WireGuard
sudo apt install wireguard  # Ubuntu/Debian
brew install wireguard-tools  # macOS

# Copy config
sudo cp deployments/netlab/output/link3-vpn.conf /etc/wireguard/netlab.conf

# Connect
sudo wg-quick up netlab

# Disconnect
sudo wg-quick down netlab

# Check status
sudo wg show
```

### Windows
1. Download: https://www.wireguard.com/install/
2. Install WireGuard
3. Open WireGuard GUI
4. Click "Import tunnel(s) from file"
5. Select `deployments/netlab/output/link3-vpn.conf`
6. Click "Activate"

### Verify VPN Connection
```bash
# After connecting
ping 10.9.9.1     # Gateway
ping 10.10.199.1  # Container network 1
ping 10.10.198.1  # Container network 2
ping 10.10.197.1  # Container network 3
```

---

## 🐳 Container Management

### Deploy Container on netlab01
```bash
ssh telcobright@10.20.0.30

# Launch container
lxc launch ubuntu:22.04 myapp

# Configure networking (bridge to lxdbr0)
lxc config device add myapp eth0 nic name=eth0 nictype=bridged parent=lxdbr0

# Get container IP
lxc list

# Access from other nodes/VPN clients
# Container IP will be in 10.10.199.0/24 range
```

### Common LXC Commands
```bash
lxc list                    # List containers
lxc start <name>            # Start container
lxc stop <name>             # Stop container
lxc exec <name> -- bash     # Shell access
lxc delete <name> --force   # Delete container
```

---

## 🔄 Rollback

### Restore VMs to Pre-Deployment State
```bash
# Stop VMs (optional)
virsh shutdown debian12_netlab_01
virsh shutdown debian12_netlab_02
virsh shutdown debian12_netlab_03

# Restore snapshots
virsh snapshot-revert debian12_netlab_01 pre-wireguard-frr-automation-20251116_023022
virsh snapshot-revert debian12_netlab_02 pre-wireguard-frr-automation-20251116_023022
virsh snapshot-revert debian12_netlab_03 pre-wireguard-frr-automation-20251116_023022

# Start VMs
virsh start debian12_netlab_01
virsh start debian12_netlab_02
virsh start debian12_netlab_03
```

---

## 🐛 Troubleshooting

### WireGuard Interface Down
```bash
# Check if interface exists
ip link show wg-overlay

# Bring up manually
sudo ip link set wg-overlay up

# Check for errors
sudo journalctl -u wg-quick@wg-overlay -n 50
```

### BGP Session Down
```bash
# Check FRR service
sudo systemctl status frr

# Restart FRR
sudo systemctl restart frr

# Check logs
sudo journalctl -u frr -n 50

# Verify BGP config
sudo vtysh -c 'show run'
```

### Cannot Ping Containers
```bash
# 1. Check BGP routes are learned
sudo vtysh -c 'show ip bgp'

# 2. Check kernel routing
ip route | grep 10.10

# 3. Test overlay first
ping 10.9.9.2  # Must work before containers work

# 4. Check container bridge
ip addr show lxdbr0
```

### VPN Client Cannot Connect
```bash
# On client side
sudo wg show  # Check if interface is up

# On gateway (netlab01)
sudo wg show wg-overlay  # Verify client peer exists

# Check firewall
sudo ufw allow 51820/udp  # Or equivalent iptables rule
```

---

## 📁 Important Files

### Configuration
```
deployments/netlab/common.conf              - Common config
deployments/netlab/frr/node1-config.conf    - netlab01 FRR config
deployments/netlab/frr/node2-config.conf    - netlab02 FRR config
deployments/netlab/frr/node3-config.conf    - netlab03 FRR config
deployments/netlab/.secrets/ssh-password.txt - SSH password (gitignored)
```

### VPN Clients
```
deployments/netlab/output/link3-vpn.conf    - Link3 VPN config
deployments/netlab/output/bdcom-vpn.conf    - BDCOM VPN config
```

### Documentation
```
deployments/netlab/DEPLOYMENT_SUMMARY.md    - Full deployment details
deployments/netlab/QUICK_REFERENCE.md       - This file
deployments/netlab/README.md                - Project overview
```

---

## 📊 Monitoring

### Check All Systems (One Command)
```bash
# Run on netlab01
{
  echo "=== WireGuard ==="
  sudo wg show wg-overlay | head -10

  echo -e "\n=== BGP Summary ==="
  sudo vtysh -c 'show ip bgp summary' | tail -5

  echo -e "\n=== Routes ==="
  ip route | grep 10.10

  echo -e "\n=== Containers ==="
  lxc list
}
```

### Performance Check
```bash
# Measure latency between nodes
ping -c 5 10.9.9.2  # To netlab02 via overlay
ping -c 5 10.9.9.3  # To netlab03 via overlay

# Check bandwidth
iperf3 -s  # On one node
iperf3 -c 10.9.9.2  # From another node
```

---

## 🎯 Common Tasks

### Add New VPN Client "developer1"
```bash
# 1. Edit scripts/generate-vpn-clients.sh
#    Add: CLIENTS[developer1]="10.9.9.102"

# 2. Generate config
./scripts/generate-vpn-clients.sh

# 3. Manually add peer to gateway
ssh telcobright@10.20.0.30
sudo wg set wg-overlay peer <PUBLIC_KEY> allowed-ips 10.9.9.102/32

# 4. Verify
sudo wg show wg-overlay
```

### Deploy MySQL Container Accessible from VPN
```bash
# On netlab01
lxc launch ubuntu:22.04 mysql-prod
lxc config device add mysql-prod eth0 nic name=eth0 nictype=bridged parent=lxdbr0
lxc exec mysql-prod -- apt update
lxc exec mysql-prod -- apt install mysql-server -y

# Get container IP (e.g., 10.10.199.10)
lxc list

# From VPN client
mysql -h 10.10.199.10 -u root -p
```

### Check Cross-Node Container Connectivity
```bash
# Deploy container on node1
ssh telcobright@10.20.0.30
lxc launch ubuntu:22.04 app1
# Get IP: 10.10.199.10

# Deploy container on node2
ssh telcobright@10.20.0.31
lxc launch ubuntu:22.04 app2
# Get IP: 10.10.198.10

# Test connectivity
lxc exec app1 -- ping 10.10.198.10  # From node1 to node2 container
lxc exec app2 -- ping 10.10.199.10  # From node2 to node1 container
```

---

## 📞 Quick Links

- **Full Documentation:** `deployments/netlab/DEPLOYMENT_SUMMARY.md`
- **Project Root:** `/home/mustafa/telcobright-projects/orchestrix-frr-router`
- **Deployment Log:** `/tmp/netlab-deployment.log`
- **Git Branch:** `frr-router`

---

**Last Updated:** 2025-11-16
**Deployment Status:** ✅ OPERATIONAL
