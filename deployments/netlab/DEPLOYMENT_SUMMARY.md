# Netlab Deployment Summary

**Deployment Date:** 2025-11-16
**Status:** ✅ COMPLETE
**Environment:** Netlab Development/Testing

---

## 🎯 Deployment Overview

Successfully deployed a full networking stack on 3 netlab VMs:
- **LXD Container Platform**: For app/service deployment
- **WireGuard Overlay Mesh**: Secure inter-node communication (10.9.9.0/24)
- **FRR BGP Routing**: Dynamic route propagation for container subnets
- **VPN Client Access**: Remote access for Link3 and BDCOM workstations

---

## 🖥️ Infrastructure

### VM Snapshots (Rollback Available)
```bash
Snapshot name: pre-wireguard-frr-automation-20251116_023022

# Restore if needed:
virsh snapshot-revert debian12_netlab_01 pre-wireguard-frr-automation-20251116_023022
virsh snapshot-revert debian12_netlab_02 pre-wireguard-frr-automation-20251116_023022
virsh snapshot-revert debian12_netlab_03 pre-wireguard-frr-automation-20251116_023022
```

### Node Configuration

| Node     | Management IP | Overlay IP | Container Subnet  | BGP AS |
|----------|---------------|------------|-------------------|--------|
| netlab01 | 10.20.0.30    | 10.9.9.1   | 10.10.199.0/24    | 65199  |
| netlab02 | 10.20.0.31    | 10.9.9.2   | 10.10.198.0/24    | 65198  |
| netlab03 | 10.20.0.32    | 10.9.9.3   | 10.10.197.0/24    | 65197  |

---

## 🔐 WireGuard Overlay Network

### Interface: `wg-overlay`
- **Overlay Network:** 10.9.9.0/24
- **Port:** 51820 (UDP)
- **Protocol:** WireGuard mesh (full connectivity between all nodes)

### Active Peers

#### Mesh Nodes (Inter-VM)
```
netlab01 ↔ netlab02 ✅ Active (handshake working)
netlab01 ↔ netlab03 ✅ Active (handshake working)
netlab02 ↔ netlab03 ✅ Active (handshake working)
```

#### VPN Clients (Remote Access)
```
link3 (10.9.9.100) ⏳ Configured (waiting for client connection)
bdcom (10.9.9.101) ⏳ Configured (waiting for client connection)
```

### Gateway Public Key (netlab01)
```
cQDb4blw0nzPWC5MT5Tq4sJN9D7suD18bUuHJMagsEc=
```

---

## 🌐 BGP Routing (FRR)

### Configuration
- **Protocol:** BGP (eBGP between different AS numbers)
- **BGP Timers:** 10s keepalive, 30s hold
- **Router IDs:** Auto-assigned from overlay IPs

### Peering Status
```
✅ netlab01 (AS 65199):
   - Peer: 10.9.9.2 (AS 65198) → State: Established, PfxRcvd: 2
   - Peer: 10.9.9.3 (AS 65197) → State: Established, PfxRcvd: 2

✅ netlab02 (AS 65198):
   - Peer: 10.9.9.1 (AS 65199) → State: Established, PfxRcvd: 2
   - Peer: 10.9.9.3 (AS 65197) → State: Established, PfxRcvd: 2

✅ netlab03 (AS 65197):
   - Peer: 10.9.9.1 (AS 65199) → State: Established, PfxRcvd: 2
   - Peer: 10.9.9.2 (AS 65198) → State: Established, PfxRcvd: 2
```

### Advertised Routes
Each node advertises its container subnet:
- **netlab01:** 10.10.199.0/24
- **netlab02:** 10.10.198.0/24
- **netlab03:** 10.10.197.0/24

All nodes learn all 3 subnets via BGP (2 from peers + 1 local).

---

## 🔧 LXD Container Platform

### Bridge Configuration
Each node has `lxdbr0` configured for container networking:
- **netlab01:** lxdbr0 → 10.10.199.0/24 (gateway: 10.10.199.1)
- **netlab02:** lxdbr0 → 10.10.198.0/24 (gateway: 10.10.198.1)
- **netlab03:** lxdbr0 → 10.10.197.0/24 (gateway: 10.10.197.1)

Containers deployed on any node will:
1. Get IP from node's subnet
2. Default gateway: node's lxdbr0 IP
3. Reach containers on other nodes via BGP routing over WireGuard overlay

---

## 📡 VPN Client Access

### Generated Client Configs

#### Link3 Office
**File:** `deployments/netlab/output/link3-vpn.conf`
**VPN IP:** 10.9.9.100/32
**Public Key:** BGrS8YySugLalbl52ZJ+pb17pB0jkoysWLrJ3dF1qQw=

#### BDCOM Office
**File:** `deployments/netlab/output/bdcom-vpn.conf`
**VPN IP:** 10.9.9.101/32
**Public Key:** myRnf8u15pSPefbIlCYuKs4P621KbcWv1+lWMNv6EGk=

### Client Setup Instructions

#### Linux/Mac
```bash
# Copy config to WireGuard directory
sudo cp link3-vpn.conf /etc/wireguard/netlab.conf

# Activate VPN
sudo wg-quick up netlab

# Verify connectivity
ping 10.9.9.1          # Gateway overlay IP
ping 10.10.199.1       # netlab01 containers
ping 10.10.198.1       # netlab02 containers
ping 10.10.197.1       # netlab03 containers
```

#### Windows
1. Install WireGuard from https://www.wireguard.com/install/
2. Open WireGuard GUI
3. Click "Import tunnel(s) from file"
4. Select `link3-vpn.conf` or `bdcom-vpn.conf`
5. Click "Activate"
6. Test connectivity using ping

### Routes Pushed to Clients
When connected via VPN, clients will have routes for:
- **10.9.9.0/24** → WireGuard overlay mesh
- **10.10.0.0/16** → All container subnets (supernet)

This allows direct access to:
- All overlay IPs (10.9.9.1/2/3)
- All container networks (10.10.197-199.0/24)
- Any containers deployed in the future within 10.10.0.0/16

---

## 🧪 Verification Commands

### On Netlab Nodes (SSH)
```bash
# SSH to any node
ssh telcobright@10.20.0.30  # netlab01
ssh telcobright@10.20.0.31  # netlab02
ssh telcobright@10.20.0.32  # netlab03

# Check WireGuard status
sudo wg show wg-overlay

# Check BGP neighbors
sudo vtysh -c 'show ip bgp summary'

# Check BGP routes
sudo vtysh -c 'show ip bgp'

# Check routing table
ip route | grep 10.10

# Test overlay connectivity
ping 10.9.9.2   # From node1 to node2
ping 10.9.9.3   # From node1 to node3
```

### From VPN Clients (After Connection)
```bash
# Test overlay access
ping 10.9.9.1    # Gateway (netlab01)
ping 10.9.9.2    # netlab02
ping 10.9.9.3    # netlab03

# Test container network access
ping 10.10.199.1  # netlab01 lxdbr0
ping 10.10.198.1  # netlab02 lxdbr0
ping 10.10.197.1  # netlab03 lxdbr0

# Check WireGuard client status
sudo wg show     # Linux/Mac
# Or check WireGuard GUI on Windows
```

---

## 📂 Deployment Files

### Configuration
- `deployments/netlab/common.conf` - Common infrastructure config
- `deployments/netlab/frr/node*-config.conf` - Per-node FRR configs
- `deployments/netlab/.secrets/ssh-password.txt` - SSH credentials (gitignored)

### VPN Clients
- `deployments/netlab/output/link3-vpn.conf` - Link3 VPN config
- `deployments/netlab/output/bdcom-vpn.conf` - BDCOM VPN config
- `deployments/netlab/output/client.conf.example` - Template

### Automation Scripts
- `src/.../example/NetlabDeployment.java` - Main deployment automation
- `src/.../example/AddVpnClients.java` - VPN client peer setup
- `src/.../example/CheckNetlabStatus.java` - Status verification
- `scripts/generate-vpn-clients.sh` - VPN config generator

---

## 🚀 Next Steps

### 1. Deploy Test Containers
```bash
# Example: Deploy MySQL on netlab01
lxc launch ubuntu:22.04 mysql-test
lxc config device add mysql-test eth0 nic name=eth0 nictype=bridged parent=lxdbr0
lxc exec mysql-test -- apt update && apt install mysql-server -y

# Container will get IP in 10.10.199.0/24 range
# Accessible from all nodes and VPN clients
```

### 2. Test Cross-Node Connectivity
```bash
# Deploy containers on different nodes
# Verify they can reach each other via overlay + BGP
```

### 3. Add More VPN Clients
```bash
# Generate new client configs
./scripts/generate-vpn-clients.sh

# Add peers to gateway
mvn compile exec:java -Dexec.mainClass="com.telcobright.orchestrix.automation.example.AddVpnClients"
```

### 4. Configure DNS (Optional)
Set up internal DNS for easier container access:
```bash
# Add DNS server in VPN client configs
DNS = 10.10.199.10  # If DNS service is deployed
```

---

## 📊 Network Topology

```
┌─────────────────────────────────────────────────────────────────┐
│                    WireGuard Overlay Mesh                       │
│                        (10.9.9.0/24)                           │
│                                                                 │
│  ┌──────────────┐    ┌──────────────┐    ┌──────────────┐    │
│  │  netlab01    │◄──►│  netlab02    │◄──►│  netlab03    │    │
│  │  10.9.9.1    │    │  10.9.9.2    │    │  10.9.9.3    │    │
│  │  AS 65199    │    │  AS 65198    │    │  AS 65197    │    │
│  └──────┬───────┘    └──────┬───────┘    └──────┬───────┘    │
│         │                   │                   │             │
│         │ BGP Peering       │                   │             │
│         └───────────────────┴───────────────────┘             │
│                                                                 │
│  ┌──────────────┐    ┌──────────────┐                         │
│  │  link3       │    │  bdcom       │                         │
│  │  10.9.9.100  │    │  10.9.9.101  │  (VPN Clients)         │
│  └──────────────┘    └──────────────┘                         │
└─────────────────────────────────────────────────────────────────┘
                              │
                              │ BGP Routes
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                   Container Subnets                             │
│                    (10.10.0.0/16)                              │
│                                                                 │
│  ┌──────────────┐    ┌──────────────┐    ┌──────────────┐    │
│  │ 10.10.199/24 │    │ 10.10.198/24 │    │ 10.10.197/24 │    │
│  │  (lxdbr0)    │    │  (lxdbr0)    │    │  (lxdbr0)    │    │
│  │  netlab01    │    │  netlab02    │    │  netlab03    │    │
│  │              │    │              │    │              │    │
│  │ [containers] │    │ [containers] │    │ [containers] │    │
│  └──────────────┘    └──────────────┘    └──────────────┘    │
└─────────────────────────────────────────────────────────────────┘

Management Network: 10.20.0.0/24 (SSH access)
```

---

## 🔒 Security Notes

1. **SSH Access:** Password-based SSH (`deployments/netlab/.secrets/ssh-password.txt`)
2. **WireGuard Keys:** Private keys stored in VPN client configs (600 permissions)
3. **Network Isolation:** Container subnets isolated per node, connected via BGP
4. **VPN Access Control:** Only authorized client public keys can connect
5. **Firewall:** Consider adding iptables rules for production use

---

## 📝 Troubleshooting

### WireGuard Not Working
```bash
# Check interface status
sudo wg show wg-overlay

# Check if interface is up
ip link show wg-overlay

# Restart WireGuard
sudo ip link set wg-overlay down
sudo ip link set wg-overlay up
```

### BGP Sessions Down
```bash
# Check FRR status
sudo systemctl status frr

# Check BGP config
sudo vtysh -c 'show run'

# Restart FRR
sudo systemctl restart frr
```

### Cannot Reach Containers
```bash
# Check BGP routes are learned
sudo vtysh -c 'show ip bgp'

# Check kernel routing table
ip route show

# Test overlay connectivity first
ping 10.9.9.2  # From node1 to node2
```

### VPN Client Cannot Connect
```bash
# On gateway (netlab01), check peers
sudo wg show wg-overlay

# Verify client public key is added
# Check firewall allows UDP 51820
sudo ufw status  # Or iptables -L
```

---

## 📞 Support

**Project:** Orchestrix FRR Router
**Repository:** `/home/mustafa/telcobright-projects/orchestrix-frr-router`
**Branch:** `frr-router`

**Deployment Log:** `/tmp/netlab-deployment.log`
**Status Check:** `mvn compile exec:java -Dexec.mainClass="CheckNetlabStatus"`

---

**Deployment executed successfully at 2025-11-16 02:45 AM**
**Total deployment time: ~3 minutes**
✅ **Status: OPERATIONAL**
