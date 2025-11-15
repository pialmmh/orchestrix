# BDCOM Cluster Reconfiguration Summary

## Overview
Reconfigured BDCOM cluster (3 nodes) to follow the container networking guideline with single VPN gateway architecture.

**Date:** 2025-11-10
**Status:** ✅ Complete and Tested

---

## Architecture Changes

### Old Architecture (Incorrect)
- ❌ All 3 nodes accepted VPN clients
- ❌ Overlay IPs: 10.9.9.4, 10.9.9.5, 10.9.9.6
- ❌ VPN Client IPs: 10.9.9.244 down to 10.9.9.235
- ❌ BGP AS: 65193, 65192, 65191
- ❌ WireGuard AllowedIPs used /24 for mesh peers (caused routing conflicts)

### New Architecture (Per Guideline)
- ✅ Only Node 3 is VPN gateway
- ✅ Overlay IPs: 10.9.9.1, 10.9.9.2, 10.9.9.3
- ✅ VPN Client IPs: 10.9.9.254 down to 10.9.9.245
- ✅ BGP AS: 65199, 65198, 65197
- ✅ WireGuard AllowedIPs use /32 for mesh peer IPs + VPN client range for Node 3

---

## Network Topology

```
┌────────────────────────────────────────────────────────────────┐
│ Developer PC (10.9.9.254/32)                                   │
│ VPN Client connects ONLY to Node 3                            │
└────────────┬───────────────────────────────────────────────────┘
             │ WireGuard VPN (through OpenVPN tunnel)
             │
┌────────────┴───────────────────────────────────────────────────┐
│ Node 3: VPN Gateway + BGP Peer                                 │
│ - Management IP: 10.255.246.175                                │
│ - Overlay IP: 10.9.9.3                                         │
│ - BGP AS: 65197                                                │
│ - Container Subnet: 10.10.197.0/24                             │
│ - Accepts VPN clients: 10.9.9.254-245                          │
└────────────┬───────────────────────────────────────────────────┘
             │ WireGuard Mesh (encrypted overlay)
    ┌────────┴────────┐
    │                 │
┌───┴──────────┐ ┌───┴──────────┐
│ Node 1       │ │ Node 2       │
│ BGP Peer     │ │ BGP Peer     │
├──────────────┤ ├──────────────┤
│ Mgmt:        │ │ Mgmt:        │
│ 10.255.246.  │ │ 10.255.246.  │
│ 173          │ │ 174          │
│              │ │              │
│ Overlay:     │ │ Overlay:     │
│ 10.9.9.1     │ │ 10.9.9.2     │
│              │ │              │
│ BGP AS:      │ │ BGP AS:      │
│ 65199        │ │ 65198        │
│              │ │              │
│ Containers:  │ │ Containers:  │
│ 10.10.199.   │ │ 10.10.198.   │
│ 0/24         │ │ 0/24         │
└──────────────┘ └──────────────┘
```

---

## Configuration Files Created

### WireGuard Overlay Configurations

**Node 1:** `/etc/wireguard/wg-overlay.conf`
- Overlay IP: 10.9.9.1/24
- Peers: Node 2 (10.9.9.2/32), Node 3 (10.9.9.3/32 + VPN range)
- Role: BGP mesh only

**Node 2:** `/etc/wireguard/wg-overlay.conf`
- Overlay IP: 10.9.9.2/24
- Peers: Node 1 (10.9.9.1/32), Node 3 (10.9.9.3/32 + VPN range)
- Role: BGP mesh only

**Node 3:** `/etc/wireguard/wg-overlay.conf`
- Overlay IP: 10.9.9.3/24
- Peers: Node 1 (10.9.9.1/32), Node 2 (10.9.9.2/32), VPN Clients (10.9.9.254-245)
- Role: BGP mesh + VPN gateway

### FRR BGP Configurations

**Node 1:** `/etc/frr/frr.conf`
- BGP AS: 65199
- Router ID: 10.9.9.1
- Advertises: 10.10.199.0/24
- Neighbors: 10.9.9.2 (AS 65198), 10.9.9.3 (AS 65197)

**Node 2:** `/etc/frr/frr.conf`
- BGP AS: 65198
- Router ID: 10.9.9.2
- Advertises: 10.10.198.0/24
- Neighbors: 10.9.9.1 (AS 65199), 10.9.9.3 (AS 65197)

**Node 3:** `/etc/frr/frr.conf`
- BGP AS: 65197
- Router ID: 10.9.9.3
- Advertises: 10.10.197.0/24
- Neighbors: 10.9.9.1 (AS 65199), 10.9.9.2 (AS 65198)

### VPN Client Configurations

Located: `/home/mustafa/telcobright-projects/routesphere/routesphere-core/tools/tunel-gateway/wireguard-client-configs/bdcom/`

| Client    | IP Address   | Config File      |
|-----------|--------------|------------------|
| client1   | 10.9.9.254   | client1.conf     |
| client2   | 10.9.9.253   | client2.conf     |
| client3   | 10.9.9.252   | client3.conf     |
| client4   | 10.9.9.251   | client4.conf     |
| client5   | 10.9.9.250   | client5.conf     |
| client6   | 10.9.9.249   | client6.conf     |
| client7   | 10.9.9.248   | client7.conf     |
| client8   | 10.9.9.247   | client8.conf     |
| client9   | 10.9.9.246   | client9.conf     |
| client10  | 10.9.9.245   | client10.conf    |

---

## Key Technical Details

### WireGuard AllowedIPs Configuration

**Critical Fix:** Changed from /24 to /32 for mesh peer overlay IPs to prevent routing conflicts.

**Node 1 & 2 peer for Node 3:**
```ini
AllowedIPs = 10.9.9.3/32, 10.9.9.245/28, 10.10.197.0/24
```
- `10.9.9.3/32` - Node 3 overlay IP
- `10.9.9.245/28` - VPN client range (10.9.9.240-255, allows traffic forwarding)
- `10.10.197.0/24` - Node 3 container subnet

**Node 3 peer for Node 1:**
```ini
AllowedIPs = 10.9.9.1/32, 10.10.199.0/24
```

**Node 3 VPN client peers:**
```ini
AllowedIPs = 10.9.9.254/32  # Individual /32 for each client
```

### BGP Configuration

**Critical Settings:**
```
no bgp ebgp-requires-policy    # Required for route exchange
no bgp network import-check    # Required for advertising routes
ebgp-multihop 2                # Required for BGP over WireGuard
```

---

## Verification Tests

### Mesh Connectivity ✅
```bash
# From Node 1
ping 10.9.9.2  # Node 2: SUCCESS
ping 10.9.9.3  # Node 3: SUCCESS
```

### BGP Sessions ✅
```bash
# From any node
sudo vtysh -c 'show bgp summary'
# All neighbors showing "Established" state
# Each node receiving 2 prefixes from 2 peers
```

### VPN Client Connectivity ✅
```bash
# From VPN client (10.9.9.254)
ping 10.9.9.3          # Node 3 overlay: SUCCESS
ping 10.10.197.20      # Node 3 Kafka: SUCCESS
ping 10.10.198.20      # Node 2 Kafka: SUCCESS (crosses mesh)
ping 10.10.199.10      # Node 1 MySQL: SUCCESS (crosses mesh)
```

---

## Services Accessible via VPN

### Kafka Cluster
- **Node 1:** 10.10.199.20:9092
- **Node 2:** 10.10.198.20:9092
- **Node 3:** 10.10.197.20:9092

### MySQL Servers
- **Node 1:** 10.10.199.10:3306
- **Node 2:** 10.10.198.10:3306

### Container IP Ranges
- **Node 1:** 10.10.199.0/24
- **Node 2:** 10.10.198.0/24
- **Node 3:** 10.10.197.0/24

---

## Quick Reference Commands

### On BDCOM Nodes

```bash
# Check WireGuard status
sudo wg show wg-overlay

# Check BGP status
sudo vtysh -c 'show bgp summary'

# Check learned routes
sudo vtysh -c 'show ip bgp'

# Check kernel routing table
ip route show proto bgp

# Restart services
sudo systemctl restart frr
sudo wg-quick down wg-overlay && sudo wg-quick up wg-overlay
```

### On Developer PC

```bash
# Connect VPN (client 1)
cd /home/mustafa/telcobright-projects/routesphere/routesphere-core/tools/tunel-gateway/wireguard-client-configs/bdcom
./wg-connect.sh 1

# Disconnect VPN
./wg-disconnect.sh 1

# Check VPN status
sudo wg show client1

# Test connectivity
ping 10.9.9.3          # VPN gateway
ping 10.10.199.20      # Node 1 Kafka
ping 10.10.198.20      # Node 2 Kafka
ping 10.10.197.20      # Node 3 Kafka
```

---

## Issues Fixed

### Issue 1: Routing Conflicts with /24 AllowedIPs
**Problem:** Using `10.9.9.0/24` in mesh peer AllowedIPs caused routing loops.
**Solution:** Changed to `/32` for specific peer IPs, added VPN client range separately.

### Issue 2: Node 1 Isolation
**Problem:** Node 1 couldn't communicate with other nodes despite handshakes.
**Root Cause:** Incorrect AllowedIPs prevented traffic flow.
**Solution:** Corrected AllowedIPs configuration.

### Issue 3: VPN Client Traffic Not Forwarded
**Problem:** VPN clients could reach Node 3 but not other nodes.
**Root Cause:** Nodes 1 & 2 didn't accept traffic from VPN client IPs.
**Solution:** Added VPN client range (10.9.9.245/28) to Node 3's AllowedIPs on other nodes.

---

## Compliance with Networking Guideline

✅ **IP Allocation:**
- Overlay IPs follow pattern: 10.9.9.1, 10.9.9.2, 10.9.9.3
- VPN clients use developer range: 10.9.9.254 down to 10.9.9.250
- Container subnets: 10.10.199.0/24, 10.10.198.0/24, 10.10.197.0/24

✅ **BGP AS Numbers:**
- Following pattern: 65199, 65198, 65197 (65200 - host_number)

✅ **Single VPN Gateway:**
- Only Node 3 accepts VPN clients
- Nodes 1 & 2 are BGP mesh peers only

✅ **WireGuard Configuration:**
- Mesh peers use /32 for overlay IPs
- VPN gateway forwards traffic via correct AllowedIPs
- No default route pushed to VPN clients

✅ **BGP Configuration:**
- All required settings present (ebgp-multihop, no network-import-check, etc.)
- Routes properly advertised and learned

---

## Backup Files

All original configurations were backed up before changes:
- `/etc/wireguard/wg-overlay.conf.backup-old`
- `/etc/frr/frr.conf.backup-old`

---

## Status

🟢 **OPERATIONAL**

All services tested and working:
- ✅ WireGuard mesh between all nodes
- ✅ BGP sessions established
- ✅ Routes being exchanged
- ✅ VPN client connectivity to all containers
- ✅ Cross-node container communication

---

## References

- **Networking Guideline:** `/home/mustafa/telcobright-projects/orchestrix/images/networking_guideline_claude.md`
- **FRR Configs:** `/home/mustafa/telcobright-projects/orchestrix-frr-router/bdcom-node{1,2,3}-frr.conf`
- **WireGuard Configs:** `/home/mustafa/telcobright-projects/orchestrix-frr-router/bdcom-node{1,2,3}-wg-overlay-v2.conf`
- **VPN Client Configs:** `/home/mustafa/telcobright-projects/routesphere/routesphere-core/tools/tunel-gateway/wireguard-client-configs/bdcom/`

---

**Reconfiguration completed successfully on 2025-11-10**
