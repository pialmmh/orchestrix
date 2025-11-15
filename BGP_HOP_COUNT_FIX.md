# BGP Hop Count / TTL Issue - Fix Documentation

## Problem Summary

**Issue**: BGP neighbors stuck in "Active" or "Connect" state, never reaching "Established"

**Root Cause**: EBGP TTL limitation when BGP peers communicate through WireGuard overlay network

## Technical Explanation

### Why This Happens

1. **EBGP Default Behavior**:
   - EBGP (External BGP) packets have TTL=1 by default
   - This is a security feature assuming EBGP peers are directly connected

2. **Overlay Network Impact**:
   - BGP peers (10.9.9.1, 10.9.9.2, 10.9.9.3) communicate through WireGuard tunnel
   - When BGP packet enters WireGuard interface, TTL gets decremented
   - By the time packet reaches the peer, TTL=0, packet is dropped

3. **Result**:
   - BGP TCP connection (port 179) fails to establish
   - BGP session remains in "Active" or "Connect" state
   - No routes are exchanged

### Network Topology

```
Node 1 (10.9.9.1)
    ↓
wg-overlay interface (TTL decremented)
    ↓
Node 2 (10.9.9.2)

Without ebgp-multihop: TTL=1 → TTL=0 (dropped)
With ebgp-multihop 2:  TTL=2 → TTL=1 (accepted)
```

## The Fix

### Solution: Add `ebgp-multihop 2` to Neighbor Configuration

**Command**:
```
router bgp 65196
 neighbor 10.9.9.2 ebgp-multihop 2
 neighbor 10.9.9.3 ebgp-multihop 2
```

**What This Does**:
- Sets EBGP TTL to 2 instead of 1
- Allows BGP packets to traverse 1 hop (the WireGuard interface)
- Packets still arrive with TTL=1, which is acceptable

### Alternative Solution

**TTL Security**:
```
router bgp 65196
 neighbor 10.9.9.2 ttl-security hops 2
```

This is more secure but requires both sides to be configured identically.

## Complete Working Configuration

### Node 1 (AS 65196, IP 10.9.9.1)

```
router bgp 65196
 bgp router-id 10.9.9.1
 no bgp ebgp-requires-policy
 no bgp network import-check
 !
 neighbor 10.9.9.2 remote-as 65195
 neighbor 10.9.9.2 description Node2-via-WireGuard
 neighbor 10.9.9.2 ebgp-multihop 2
 neighbor 10.9.9.2 timers 10 30
 !
 neighbor 10.9.9.3 remote-as 65194
 neighbor 10.9.9.3 description Node3-via-WireGuard
 neighbor 10.9.9.3 ebgp-multihop 2
 neighbor 10.9.9.3 timers 10 30
 !
 address-family ipv4 unicast
  network 10.10.199.0/24
  neighbor 10.9.9.2 activate
  neighbor 10.9.9.3 activate
 exit-address-family
```

### Node 2 (AS 65195, IP 10.9.9.2)

```
router bgp 65195
 bgp router-id 10.9.9.2
 no bgp ebgp-requires-policy
 no bgp network import-check
 !
 neighbor 10.9.9.1 remote-as 65196
 neighbor 10.9.9.1 description Node1-via-WireGuard
 neighbor 10.9.9.1 ebgp-multihop 2
 neighbor 10.9.9.1 timers 10 30
 !
 neighbor 10.9.9.3 remote-as 65194
 neighbor 10.9.9.3 description Node3-via-WireGuard
 neighbor 10.9.9.3 ebgp-multihop 2
 neighbor 10.9.9.3 timers 10 30
 !
 address-family ipv4 unicast
  network 10.10.198.0/24
  neighbor 10.9.9.1 activate
  neighbor 10.9.9.3 activate
 exit-address-family
```

### Node 3 (AS 65194, IP 10.9.9.3)

```
router bgp 65194
 bgp router-id 10.9.9.3
 no bgp ebgp-requires-policy
 no bgp network import-check
 !
 neighbor 10.9.9.1 remote-as 65196
 neighbor 10.9.9.1 description Node1-via-WireGuard
 neighbor 10.9.9.1 ebgp-multihop 2
 neighbor 10.9.9.1 timers 10 30
 !
 neighbor 10.9.9.2 remote-as 65195
 neighbor 10.9.9.2 description Node2-via-WireGuard
 neighbor 10.9.9.2 ebgp-multihop 2
 neighbor 10.9.9.2 timers 10 30
 !
 address-family ipv4 unicast
  network 10.10.197.0/24
  neighbor 10.9.9.1 activate
  neighbor 10.9.9.2 activate
 exit-address-family
```

## Verification Steps

### 1. Check BGP Session State

**Before Fix**:
```bash
sudo vtysh -c 'show ip bgp summary'

Neighbor        V    AS   MsgRcvd MsgSent   TblVer  InQ OutQ  Up/Down State/PfxRcd
10.9.9.2        4 65195         0       0        0    0    0 never    Active
10.9.9.3        4 65194         0       0        0    0    0 never    Active
```

**After Fix**:
```bash
sudo vtysh -c 'show ip bgp summary'

Neighbor        V    AS   MsgRcvd MsgSent   TblVer  InQ OutQ  Up/Down State/PfxRcd
10.9.9.2        4 65195        45      48        0    0    0 00:20:15        1
10.9.9.3        4 65194        42      45        0    0    0 00:20:12        1
```

### 2. Check BGP Routes

```bash
sudo vtysh -c 'show ip bgp'

   Network          Next Hop            Metric LocPrf Weight Path
*> 10.10.197.0/24   10.9.9.3                 0             0 65194 i
*> 10.10.198.0/24   10.9.9.2                 0             0 65195 i
*> 10.10.199.0/24   0.0.0.0                  0         32768 i
```

### 3. Verify Kernel Routes

```bash
ip route show | grep 10.10

10.10.197.0/24 via 10.9.9.3 dev wg-overlay proto bgp metric 20
10.10.198.0/24 via 10.9.9.2 dev wg-overlay proto bgp metric 20
10.10.199.0/24 dev lxdbr0 proto kernel scope link src 10.10.199.1
```

## How to Apply the Fix

### Method 1: Using vtysh (Interactive)

```bash
sudo vtysh

configure terminal
router bgp 65196
  neighbor 10.9.9.2 ebgp-multihop 2
  neighbor 10.9.9.3 ebgp-multihop 2
  exit
write memory
exit
```

### Method 2: Edit Configuration File

1. Edit `/etc/frr/frr.conf`:
```bash
sudo nano /etc/frr/frr.conf
```

2. Add `ebgp-multihop 2` line for each neighbor

3. Restart FRR:
```bash
sudo systemctl restart frr
```

### Method 3: Using Deployment Script

The updated `deploy-frr-cluster.sh` now automatically includes `ebgp-multihop 2` in all BGP neighbor configurations.

## Critical BGP Settings for Overlay Networks

When deploying BGP over WireGuard or any overlay network, ensure ALL of these settings are present:

1. **`no bgp ebgp-requires-policy`** - Allows route exchange without explicit policies
2. **`neighbor X.X.X.X ebgp-multihop 2`** - Allows BGP over non-direct connections
3. **`neighbor X.X.X.X activate`** - Activates neighbor in address-family (REQUIRED)
4. **`no bgp network import-check`** - Allows advertising networks not in routing table

## Troubleshooting

### Symptom: BGP stuck in "Active" state
```
Diagnosis:
- Check WireGuard connectivity: ping 10.9.9.2
- Check FRR logs: journalctl -u frr -f
- Verify firewall allows TCP port 179
- Confirm ebgp-multihop is configured

Fix: Add ebgp-multihop 2
```

### Symptom: BGP Established but no routes
```
Diagnosis:
- Check if neighbors are activated in address-family
- Verify no bgp ebgp-requires-policy is set
- Check if networks are advertised

Fix: Add neighbor X.X.X.X activate in address-family
```

### Symptom: Routes learned but not in kernel
```
Diagnosis:
- show ip bgp (should show routes)
- ip route show (should show BGP routes)
- Check for route conflicts

Fix: Usually administrative distance issue
```

## When is ebgp-multihop NOT Needed?

EBGP multihop is **NOT needed** when:
- BGP peers are directly connected (same subnet, no tunnel)
- Using IBGP (Internal BGP) - same AS number
- Physical interfaces with no intermediate hops

EBGP multihop **IS needed** when:
- BGP over VPN tunnels (WireGuard, IPsec, GRE)
- BGP over VXLAN overlays
- BGP peers separated by L2 switches that decrement TTL
- Any scenario where packets traverse an interface before reaching peer

## References

- FRR Documentation: https://docs.frrouting.org/en/latest/bgp.html
- RFC 4271: BGP-4 Specification
- `deploy-frr-cluster.sh`: Automated deployment with all fixes
- `FRR_DEPLOYMENT_GUIDE.md`: Complete configuration guide
