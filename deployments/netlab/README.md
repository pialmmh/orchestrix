# Netlab Deployment Configuration

Test tenant for network infrastructure automation testing.

## Overview

**Tenant:** netlab
**Purpose:** Testing environment for WireGuard overlay + FRR BGP + LXD container networking
**Nodes:** 3 VMs (netlab01, netlab02, netlab03)

## Directory Structure

```
netlab/
├── configs/                    # Configuration files (source of truth)
│   ├── wireguard/             # WireGuard overlay configs
│   │   ├── node1.conf         # Node 1: 10.20.0.30 → 10.9.9.1
│   │   ├── node2.conf         # Node 2: 10.20.0.31 → 10.9.9.2
│   │   └── node3.conf         # Node 3: 10.20.0.32 → 10.9.9.3
│   │
│   └── frr/                   # FRR BGP routing configs
│       ├── node1-config.conf  # AS 65199, subnet 10.10.199.0/24
│       ├── node2-config.conf  # AS 65198, subnet 10.10.198.0/24
│       └── node3-config.conf  # AS 65197, subnet 10.10.197.0/24
│
├── data/                       # Runtime data (logs, state files)
│
├── logs/                       # Deployment logs
│
└── README.md                   # This file
```

## Configuration Organization Pattern

Following Orchestrix standard patterns (per DEPLOYMENT_ORGANIZATION.md):

### 1. **Per-Client Structure**
- Each tenant/client has own directory: `deployments/{tenant-name}/`
- Subdirectories: `configs/`, `data/`, `logs/`

### 2. **Per-Automation Organization**
- Configs grouped by automation type: `configs/wireguard/`, `configs/frr/`
- One config file per deployment target (node1, node2, node3)

### 3. **Config as Source of Truth**
- Configuration files stored here are the authoritative source
- Deployment automation reads from these configs
- Generated configs (with keys) written to `data/` at runtime

## Node Configuration

| Node | Management IP | Overlay IP | Container Subnet | BGP AS | SSH |
|------|--------------|------------|------------------|--------|-----|
| netlab01 | 10.20.0.30 | 10.9.9.1 | 10.10.199.0/24 | 65199 | telcobright@10.20.0.30 (pass: a) |
| netlab02 | 10.20.0.31 | 10.9.9.2 | 10.10.198.0/24 | 65198 | telcobright@10.20.0.31 (pass: a) |
| netlab03 | 10.20.0.32 | 10.9.9.3 | 10.10.197.0/24 | 65197 | telcobright@10.20.0.32 (pass: a) |

## WireGuard Configuration Files

Located in: `configs/wireguard/`

**Format:** Standard WireGuard INI format
- `[Interface]` section with overlay IP and port
- `[Peer]` sections for each remote node
- Template placeholders for keys (replaced during deployment)

**Key Features:**
- Mesh topology (full connectivity between all nodes)
- Overlay network: 10.9.9.0/24
- AllowedIPs: Peer overlay IP (/32) + peer container subnet (/24)
- Endpoint: Management IPs (10.20.0.x:51820)

**Deployment Process:**
1. Generate WireGuard keypairs for each node
2. Replace placeholders with actual keys
3. Deploy to `/etc/wireguard/wg-overlay.conf` on each node
4. Start with: `wg-quick up wg-overlay`

## FRR Configuration Files

Located in: `configs/frr/`

**Format:** Shell config format (key=value)
- Container name
- Base image reference
- BGP ASN
- BGP neighbors (overlay IPs + ASNs)
- Networks to advertise (container subnets)

**Key Features:**
- BGP neighbors specified as: `overlay_ip:asn`
- Networks in CIDR format
- Router ID auto-detected from host
- EBGP multihop required for overlay

**Deployment Process:**
1. Parse config file
2. Generate full FRR frr.conf with BGP settings
3. Deploy FRR router container or configure on host
4. Verify BGP peering: `vtysh -c 'show ip bgp summary'`

## Deployment Methods

### Method 1: One-Click Java Automation

```bash
cd /home/mustafa/telcobright-projects/orchestrix-frr-router
./deploy-netlab.sh
```

Automatically deploys WireGuard + FRR + LXD to all 3 nodes.

### Method 2: Per-Automation Deployment

**Deploy WireGuard:**
```bash
# Run WireGuard deployment automation
# Reads from: deployments/netlab/configs/wireguard/
# Generates keys and deploys to nodes
```

**Deploy FRR:**
```bash
# Run FRR deployment automation
# Reads from: deployments/netlab/configs/frr/
# Deploys BGP routing to nodes
```

### Method 3: Manual Deployment

**WireGuard:**
```bash
# Generate keys
wg genkey | tee privatekey | wg pubkey > publickey

# Copy config to node
scp configs/wireguard/node1.conf telcobright@10.20.0.30:/tmp/

# SSH to node and deploy
ssh telcobright@10.20.0.30
sudo cp /tmp/node1.conf /etc/wireguard/wg-overlay.conf
# Replace placeholders with actual keys
sudo wg-quick up wg-overlay
```

**FRR:**
```bash
# Use existing FRR deployment automation
cd /home/mustafa/telcobright-projects/orchestrix-frr-router

# Deploy to node1
./deploy-frr.sh \
  --host 10.20.0.30 \
  --config ../orchestrix/deployments/netlab/configs/frr/node1-config.conf
```

## Verification

**Check WireGuard:**
```bash
ssh telcobright@10.20.0.30
sudo wg show
ping 10.9.9.2  # Test overlay connectivity
```

**Check FRR BGP:**
```bash
ssh telcobright@10.20.0.30
sudo vtysh -c 'show ip bgp summary'
sudo vtysh -c 'show ip bgp'
```

**Expected Results:**
- WireGuard: 2 peers connected per node
- BGP: 2 neighbors in "Established" state
- Routes: All 3 container subnets (199, 198, 197) visible

## Config File Modification

To update configurations:

1. Edit files in `configs/wireguard/` or `configs/frr/`
2. Re-run deployment automation
3. Or manually copy and restart services

**DO NOT** edit `/etc/wireguard/` or `/etc/frr/` directly on nodes - changes will be overwritten on next deployment.

## Relation to Other Orchestrix Components

Following established patterns:

**Similar to:**
- `/images/containers/lxc/frr-router/deployment-configs/` (FRR container configs)
- `/deployments/telco-client-001/` (client deployment structure)

**Used by:**
- `NetlabDeployment.java` - One-click deployment automation
- `FrrDeploymentRunner.java` - FRR-specific deployment
- WireGuard deployment automation

**References:**
- Networking guideline: `/images/networking_guideline_claude.md`
- Deployment organization: `/DEPLOYMENT_ORGANIZATION.md`
- FRR deployment guide: `/images/containers/lxc/frr-router/DEPLOYMENT_GUIDE.md`

## Network Topology

```
         Internet
             │
             │ NAT (wlo1)
             │
      ┌──────┴──────┐
      │ Host        │
      │ 10.20.0.1   │
      └──────┬──────┘
             │ lxdbr0
       ┌─────┼─────┐
       │     │     │
   ┌───┴───┐ │ ┌───┴───┐
   │ VM 1  │ │ │ VM 2  │ │ VM 3  │
   │ .30   │ │ │ .31   │ │ .32   │
   └───┬───┘ │ └───┬───┘ └───┬───┘
       │     │     │         │
       └─────┼─────┴─────────┘
             │ WireGuard Overlay (10.9.9.0/24)
             │
      ┌──────┴──────┐
      │ BGP Mesh    │
      │ Full Conn.  │
      └─────────────┘
             │
    ┌────────┼────────┐
    │        │        │
10.10.199   198      197
 /24        /24      /24
(Containers per node)
```

## Notes

- **AllowedIPs Critical:** Must use /32 for overlay IPs to prevent routing conflicts
- **Container subnets:** Use /24 in AllowedIPs for proper routing
- **EBGP multihop:** Required for BGP over WireGuard (not directly connected)
- **Router ID:** Auto-detected from overlay IP if not specified
- **Port consistency:** WireGuard on 51820, management SSH on 22

## References

- [Networking Guideline](/home/mustafa/telcobright-projects/orchestrix/images/networking_guideline_claude.md)
- [Network Diagram](/home/mustafa/Downloads/NETWORK_DIAGRAM_SIMPLE.txt)
- [Deployment Guide](/home/mustafa/telcobright-projects/orchestrix-frr-router/NETLAB_DEPLOYMENT_GUIDE.md)
