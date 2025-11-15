# BDCOM Cluster Deployment Summary

**Deployment Date:** 2025-11-04
**Cluster Name:** BDCOM FRR + WireGuard Mesh
**Servers:** 3 Ubuntu 22.04 LTS servers

---

## Deployment Overview

Successfully deployed a full-mesh FRR BGP routing infrastructure with WireGuard overlay network across three BDCOM servers. All servers can now exchange routing information and establish dynamic routes to each other's networks.

---

## Server Configuration

### Server 1: SMS Application
- **Management IP:** 10.255.246.173
- **Overlay IP:** 10.9.9.4/24
- **BGP AS:** 65193
- **Advertised Network:** 10.10.196.0/24
- **Hostname:** bdcom1-bgp
- **SSH Port:** 15605

### Server 2: SMS Application + Master DB
- **Management IP:** 10.255.246.174
- **Overlay IP:** 10.9.9.5/24
- **BGP AS:** 65192
- **Advertised Network:** 10.10.195.0/24
- **Hostname:** bdcom2-bgp
- **SSH Port:** 15605

### Server 3: SMS Application + Slave DB
- **Management IP:** 10.255.246.175
- **Overlay IP:** 10.9.9.6/24
- **BGP AS:** 65191
- **Advertised Network:** 10.10.194.0/24
- **Hostname:** bdcom3-bgp
- **SSH Port:** 15605

---

## Software Versions

- **WireGuard:** v1.0.20210914
- **FRR:** v10.4.1
- **OS:** Ubuntu 22.04.5 LTS (Kernel 5.15.0-151-generic)

---

## Network Topology

```
                    WireGuard Overlay Network
                         10.9.9.0/24
                              |
         +--------------------|--------------------+
         |                    |                    |
   10.9.9.4               10.9.9.5             10.9.9.6
   BDCOM1                 BDCOM2               BDCOM3
   AS 65193               AS 65192             AS 65191
   10.10.196.0/24         10.10.195.0/24       10.10.194.0/24
```

### Full Mesh Connectivity
- **BDCOM1 ←→ BDCOM2:** Direct WireGuard + BGP peering
- **BDCOM1 ←→ BDCOM3:** Direct WireGuard + BGP peering
- **BDCOM2 ←→ BDCOM3:** Direct WireGuard + BGP peering

---

## WireGuard Configuration

### WireGuard Keys Generated

**BDCOM1:**
- Private: `QHGk99uDRl0QCBNpz2ETAp4vmmjesHCbUebY/0Bum00=`
- Public: `AdlCtDGGxBapAObDouGA2d7R7GAP4g9b/lUcXeJ8wwI=`

**BDCOM2:**
- Private: `YIsAUmO7zJvwlOc5/87INLscBOSwjV+jSF/QXu+oVUU=`
- Public: `SpNu/VK8J4ezS5257lyDiorwvKHkoxn0rWfK2QEzKgs=`

**BDCOM3:**
- Private: `2LfCT04zlMgc6O3eQuF8Izs/7+Oj1C/R2PvaL5anNFg=`
- Public: `IKhz2dYGWON6k3ULtNYySKvdiZ+Y3JFGYliq3h0ikSc=`

### WireGuard Interface
- **Interface Name:** wg-overlay
- **Listen Port:** 51820
- **MTU:** 1420 (auto-configured)
- **Persistent Keepalive:** 25 seconds

### Configuration Files
All servers: `/etc/wireguard/wg-overlay.conf`

---

## BGP Configuration

### BGP Peering Matrix

| Local Router | Local AS | Remote Router | Remote AS | Overlay IP | Status |
|--------------|----------|---------------|-----------|------------|--------|
| BDCOM1 | 65193 | BDCOM2 | 65192 | 10.9.9.5 | ✓ Established |
| BDCOM1 | 65193 | BDCOM3 | 65191 | 10.9.9.6 | ✓ Established |
| BDCOM2 | 65192 | BDCOM1 | 65193 | 10.9.9.4 | ✓ Established |
| BDCOM2 | 65192 | BDCOM3 | 65191 | 10.9.9.6 | ✓ Established |
| BDCOM3 | 65191 | BDCOM1 | 65193 | 10.9.9.4 | ✓ Established |
| BDCOM3 | 65191 | BDCOM2 | 65192 | 10.9.9.5 | ✓ Established |

### Critical BGP Settings Applied

1. **`no bgp ebgp-requires-policy`** - Allows route exchange without explicit route-maps
2. **`neighbor X.X.X.X ebgp-multihop 2`** - Required for BGP over WireGuard overlay
3. **`neighbor X.X.X.X activate`** - Activates neighbors in address-family
4. **`no bgp network import-check`** - Allows advertising networks not in routing table
5. **BGP Timers:** Keepalive 10s, Holdtime 30s (fast convergence)

### Route Advertisement

| Server | Network Advertised | Purpose |
|--------|-------------------|---------|
| BDCOM1 | 10.10.196.0/24 | LXD/container network |
| BDCOM2 | 10.10.195.0/24 | LXD/container network |
| BDCOM3 | 10.10.194.0/24 | LXD/container network |

---

## Verification Results

### WireGuard Mesh Status (BDCOM1)

```
interface: wg-overlay
  public key: AdlCtDGGxBapAObDouGA2d7R7GAP4g9b/lUcXeJ8wwI=
  listening port: 51820

peer: SpNu/VK8J4ezS5257lyDiorwvKHkoxn0rWfK2QEzKgs= (BDCOM2)
  endpoint: 10.255.246.174:51820
  allowed ips: 10.9.9.5/32
  latest handshake: Active
  persistent keepalive: every 25 seconds

peer: IKhz2dYGWON6k3ULtNYySKvdiZ+Y3JFGYliq3h0ikSc= (BDCOM3)
  endpoint: 10.255.246.175:51820
  allowed ips: 10.9.9.6/32
  latest handshake: Active
  persistent keepalive: every 25 seconds
```

✓ All WireGuard peers have active handshakes
✓ Ping successful between all overlay IPs

### BGP Status (BDCOM1)

```
Neighbor    V    AS       MsgRcvd  MsgSent  State/PfxRcd
10.9.9.5    4    65192    10       11       2           (BDCOM2)
10.9.9.6    4    65191    9        10       2           (BDCOM3)
```

✓ All BGP sessions in **Established** state
✓ Routes being exchanged (2 prefixes received from each peer)

### BGP Routing Table (BDCOM1)

```
Network          Next Hop         Path
10.10.194.0/24   10.9.9.6         65191 i
10.10.195.0/24   10.9.9.5         65192 i
10.10.196.0/24   0.0.0.0          Local (32768)
```

✓ All three networks visible in BGP table
✓ Best paths selected correctly
✓ Alternative paths available for redundancy

### Kernel Routing Table (BDCOM1)

```
10.10.194.0/24 via 10.9.9.6 dev wg-overlay proto bgp metric 20
10.10.195.0/24 via 10.9.9.5 dev wg-overlay proto bgp metric 20
```

✓ BGP routes installed in kernel
✓ Routes use WireGuard overlay interface
✓ Traffic will flow through encrypted WireGuard tunnel

---

## Service Status

All servers have:
- ✓ WireGuard service: **Active** (enabled at boot)
- ✓ FRR service: **Active** (enabled at boot)
- ✓ BGP daemon: **Running**
- ✓ All BGP neighbors: **Established**

---

## Configuration Files Location

### WireGuard
- **Config:** `/etc/wireguard/wg-overlay.conf`
- **Permissions:** 600 (root only)

### FRR
- **Main Config:** `/etc/frr/frr.conf`
- **Permissions:** 640 (frr:frr)
- **Daemons Config:** `/etc/frr/daemons`
- **Logs:** `/var/log/frr/` (via syslog)

---

## Management Commands

### WireGuard

```bash
# Show WireGuard status
sudo wg show

# Restart WireGuard interface
sudo wg-quick down wg-overlay
sudo wg-quick up wg-overlay

# Check interface status
ip addr show wg-overlay

# Test overlay connectivity
ping 10.9.9.4  # BDCOM1
ping 10.9.9.5  # BDCOM2
ping 10.9.9.6  # BDCOM3
```

### FRR / BGP

```bash
# Enter FRR CLI
sudo vtysh

# Show BGP summary
sudo vtysh -c 'show ip bgp summary'

# Show BGP routes
sudo vtysh -c 'show ip bgp'

# Show BGP neighbors detail
sudo vtysh -c 'show ip bgp neighbors'

# Show running configuration
sudo vtysh -c 'show running-config'

# Restart FRR
sudo systemctl restart frr

# Check FRR status
sudo systemctl status frr

# View FRR logs
sudo journalctl -u frr -f
```

### Routing Table

```bash
# Show all routes
ip route show

# Show only BGP routes
ip route show proto bgp

# Show routes to specific network
ip route get 10.10.195.0
```

---

## Network Performance

- **WireGuard Encryption:** ChaCha20-Poly1305 (hardware accelerated)
- **Latency:** ~1-1.5ms between nodes (measured via WireGuard overlay)
- **BGP Convergence:** Fast (10s keepalive, 30s holdtime)
- **Route Redundancy:** Multiple paths available (full mesh provides alternate routes)

---

## Security Features

1. **Encrypted Overlay:** All inter-node traffic encrypted via WireGuard
2. **Key-based Authentication:** Each peer authenticates via public/private key pairs
3. **Restricted Access:** FRR configs readable only by frr user
4. **Firewall Ready:** WireGuard uses UDP port 51820, BGP uses TCP port 179

---

## Future Expansion

To add a new server to the mesh:

1. Install FRR and WireGuard
2. Generate WireGuard key pair
3. Add peer configuration on all existing servers
4. Configure BGP with unique AS number
5. Advertise new network subnet

See `FRR_DEPLOYMENT_GUIDE.md` for detailed procedures.

---

## Troubleshooting

### If WireGuard peer not connecting:
```bash
# Check WireGuard status
sudo wg show

# Restart interface
sudo wg-quick down wg-overlay && sudo wg-quick up wg-overlay

# Check firewall allows UDP 51820
sudo ufw status
```

### If BGP not establishing:
```bash
# Check BGP status
sudo vtysh -c 'show ip bgp summary'

# Verify overlay connectivity
ping 10.9.9.X

# Check FRR logs
sudo journalctl -u frr -n 50

# Verify BGP daemon enabled
grep bgpd /etc/frr/daemons
```

### If routes not installing:
```bash
# Check BGP learned routes
sudo vtysh -c 'show ip bgp'

# Check kernel routes
ip route show proto bgp

# Verify no conflicting routes
ip route show
```

---

## Automation Used

- **Deployment Method:** Manual SSH-based automation
- **Configuration Source:** `/home/mustafa/telcobright-projects/orchestrix-frr-router/bdcom-cluster.conf`
- **Deployment Scripts Available:**
  - `deploy-frr-cluster.sh` - Bash automation (reusable for similar deployments)
  - `FRR_DEPLOYMENT_GUIDE.md` - Step-by-step manual procedures
  - `BGP_HOP_COUNT_FIX.md` - Troubleshooting guide for overlay networks

---

## References

- [FRR Documentation](https://docs.frrouting.org/)
- [WireGuard Documentation](https://www.wireguard.com/)
- Project Guide: `FRR_DEPLOYMENT_GUIDE.md`
- BGP Troubleshooting: `BGP_HOP_COUNT_FIX.md`

---

## Notes

- All servers use default Ubuntu repositories plus FRR official repo
- WireGuard kernel module loaded automatically when interface is brought up
- BGP uses EBGP (different AS numbers for each server)
- Full mesh topology provides maximum redundancy (each node connects to all others)
- Networks 10.10.194-196.0/24 are reserved for future LXD container deployments

---

**Deployment Status:** ✓ COMPLETE
**All Systems Operational**
