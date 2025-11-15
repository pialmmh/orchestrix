# Netlab One-Click Deployment Guide

## Overview

Automated deployment of complete networking stack to 3 netlab VMs:
- **LXD bridge** with container subnets (10.10.199.x, 198.x, 197.x)
- **WireGuard overlay** mesh network (10.9.9.1/2/3)
- **FRR BGP routing** for container subnet advertisement

## Prerequisites

### Netlab VMs
- netlab01: 10.20.0.30 (user: telcobright, pass: a)
- netlab02: 10.20.0.31 (user: telcobright, pass: a)
- netlab03: 10.20.0.32 (user: telcobright, pass: a)

### Requirements
- VMs must be accessible via SSH
- VMs must have internet access
- Debian 12 with sudo access

## One-Click Deployment

```bash
cd /home/mustafa/telcobright-projects/orchestrix-frr-router
./deploy-netlab.sh
```

That's it! The script will:
1. ✅ Connect to all 3 VMs
2. ✅ Install prerequisites (WireGuard, FRR, LXD)
3. ✅ Configure LXD bridges with proper subnets
4. ✅ Deploy WireGuard overlay mesh
5. ✅ Configure FRR BGP routing
6. ✅ Verify deployment

**Duration:** ~5-10 minutes

## What Gets Deployed

### Network Architecture

```
┌─────────────────┐  ┌─────────────────┐  ┌─────────────────┐
│   netlab01      │  │   netlab02      │  │   netlab03      │
│   10.20.0.30    │  │   10.20.0.31    │  │   10.20.0.32    │
├─────────────────┤  ├─────────────────┤  ├─────────────────┤
│ Overlay:        │  │ Overlay:        │  │ Overlay:        │
│   10.9.9.1      │══│   10.9.9.2      │══│   10.9.9.3      │
│                 │  │                 │  │                 │
│ BGP AS: 65199   │  │ BGP AS: 65198   │  │ BGP AS: 65197   │
│                 │  │                 │  │                 │
│ Containers:     │  │ Containers:     │  │ Containers:     │
│ 10.10.199.0/24  │  │ 10.10.198.0/24  │  │ 10.10.197.0/24  │
└─────────────────┘  └─────────────────┘  └─────────────────┘
```

### Per-Node Configuration

**netlab01 (10.20.0.30):**
- Overlay IP: 10.9.9.1/24
- Container subnet: 10.10.199.0/24
- LXD bridge: lxdbr0 (10.10.199.1/24)
- BGP AS: 65199
- Router ID: 10.9.9.1

**netlab02 (10.20.0.31):**
- Overlay IP: 10.9.9.2/24
- Container subnet: 10.10.198.0/24
- LXD bridge: lxdbr0 (10.10.198.1/24)
- BGP AS: 65198
- Router ID: 10.9.9.2

**netlab03 (10.20.0.32):**
- Overlay IP: 10.9.9.3/24
- Container subnet: 10.10.197.0/24
- LXD bridge: lxdbr0 (10.10.197.1/24)
- BGP AS: 65197
- Router ID: 10.9.9.3

## Verification

### SSH to any node:
```bash
ssh telcobright@10.20.0.30  # password: a
```

### Check WireGuard overlay:
```bash
sudo wg show
```
Expected: 2 peers connected (the other 2 nodes)

### Check BGP status:
```bash
sudo vtysh -c 'show ip bgp summary'
```
Expected: 2 BGP neighbors in "Established" state

### Check BGP routes:
```bash
sudo vtysh -c 'show ip bgp'
```
Expected: See routes for all 3 container subnets (199.0/24, 198.0/24, 197.0/24)

### Test overlay connectivity:
```bash
# From netlab01
ping 10.9.9.2  # Should reach netlab02
ping 10.9.9.3  # Should reach netlab03
```

### Check LXD bridge:
```bash
sudo lxc network show lxdbr0
ip addr show lxdbr0
```

## Troubleshooting

### WireGuard not connecting:
```bash
sudo wg-quick down wg-overlay
sudo wg-quick up wg-overlay
sudo wg show
```

### BGP not establishing:
```bash
# Check FRR status
sudo systemctl status frr

# View FRR logs
sudo journalctl -u frr -n 50

# Enter FRR shell
sudo vtysh
show ip bgp neighbors
```

### LXD bridge issues:
```bash
# Check bridge status
sudo lxc network list
sudo lxc network show lxdbr0

# Restart LXD
sudo systemctl restart lxd
```

## Manual Deployment Steps

If one-click fails, deploy manually:

### 1. Install prerequisites:
```bash
sudo apt-get update
sudo apt-get install -y wireguard wireguard-tools frr lxd
```

### 2. Configure LXD:
```bash
sudo lxd init --auto
sudo lxc network create lxdbr0 \
  ipv4.address=10.10.199.1/24 \
  ipv4.nat=false \
  dns.mode=managed
```

### 3. Generate WireGuard keys:
```bash
wg genkey | tee privatekey | wg pubkey > publickey
```

### 4. Create WireGuard config:
```bash
sudo nano /etc/wireguard/wg-overlay.conf
# Use configs from FullClusterDeploymentExample.java
```

### 5. Start WireGuard:
```bash
sudo wg-quick up wg-overlay
```

### 6. Configure FRR:
```bash
sudo nano /etc/frr/frr.conf
# Use configs from deployment

sudo sed -i 's/bgpd=no/bgpd=yes/' /etc/frr/daemons
sudo systemctl restart frr
```

## Testing Container Connectivity

After deployment, test cross-node container communication:

```bash
# On netlab01, create test container
sudo lxc launch ubuntu:22.04 test1 -n lxdbr0
sudo lxc exec test1 -- ip addr

# On netlab02, create test container
sudo lxc launch ubuntu:22.04 test2 -n lxdbr0
sudo lxc exec test2 -- ip addr

# Test connectivity
sudo lxc exec test1 -- ping <test2-ip>
```

If containers can ping each other across nodes, BGP routing is working! 🎉

## Files Created

On each node:
- `/etc/wireguard/wg-overlay.conf` - WireGuard mesh configuration
- `/etc/frr/frr.conf` - FRR BGP configuration
- `/etc/frr/daemons` - FRR enabled daemons

## Cleanup

To remove deployment:
```bash
# On each node
sudo wg-quick down wg-overlay
sudo systemctl stop frr
sudo lxc network delete lxdbr0
sudo apt-get remove --purge wireguard frr lxd
```

## Next Steps

After successful deployment:
1. Test container creation and cross-node communication
2. Deploy your Java applications in containers
3. Test application connectivity across nodes
4. Monitor BGP routes as containers are added/removed
5. Add VPN gateway configuration for developer access

## Reference

- Networking Guideline: `/home/mustafa/telcobright-projects/orchestrix/images/networking_guideline_claude.md`
- Example Config: `src/main/java/.../example/FullClusterDeploymentExample.java`
- Network Diagram: `~/Downloads/NETWORK_DIAGRAM_SIMPLE.txt`

## Support

For issues:
1. Check logs on VMs: `sudo journalctl -xe`
2. Verify SSH connectivity: `ssh telcobright@10.20.0.30`
3. Re-run deployment script
4. Manual deployment steps above
