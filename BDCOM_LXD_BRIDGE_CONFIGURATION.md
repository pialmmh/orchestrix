# BDCOM LXD Bridge Configuration Summary

**Date:** 2025-11-04
**Status:** ✅ Completed
**Configured By:** AI Assistant (Claude)

---

## Overview

All three BDCOM servers have been configured with LXD bridge networks according to the Orchestrix networking guidelines (`/home/mustafa/telcobright-projects/orchestrix/images/networking_guideline_claude.md`).

---

## Configuration Summary

| Server | Management IP | Host # | Container Subnet | Bridge IP | DHCP Range | Overlay IP | BGP AS |
|--------|--------------|--------|------------------|-----------|------------|------------|---------|
| Server 1 | 10.255.246.173 | 1 | 10.10.199.0/24 | 10.10.199.1 | .200-.254 | 10.9.9.1 | 65199 |
| Server 2 | 10.255.246.174 | 2 | 10.10.198.0/24 | 10.10.198.1 | .200-.254 | 10.9.9.2 | 65198 |
| Server 3 | 10.255.246.175 | 3 | 10.10.197.0/24 | 10.10.197.1 | .200-.254 | 10.9.9.3 | 65197 |

---

## IP Allocation Scheme

### Allocation Formulas

According to networking guidelines:
```
Container Subnet:  10.10.(200-hostNumber).0/24
Bridge Gateway:    10.10.(200-hostNumber).1
Overlay IP:        10.9.9.hostNumber
BGP AS Number:     65200-hostNumber
```

### IP Ranges Within Each Subnet

Each /24 subnet is divided as follows:

```
10.10.X.0/24 (where X = 199, 198, or 197)
├── 10.10.X.0         Reserved (network address)
├── 10.10.X.1         Bridge gateway (lxdbr0)
├── 10.10.X.2-99      Reserved for infrastructure
├── 10.10.X.100-199   Docker containers (100 IPs)
└── 10.10.X.200-254   LXD containers (55 IPs)
```

---

## Server-Specific Configurations

### Server 1 (10.255.246.173)

**LXD Bridge Configuration:**
```yaml
networks:
- config:
    ipv4.address: 10.10.199.1/24
    ipv4.nat: "false"
    ipv4.dhcp.ranges: 10.10.199.200-10.10.199.254
  name: lxdbr0
  type: bridge
```

**Verified Status:**
```
✓ Bridge IP: 10.10.199.1/24
✓ NAT: Disabled
✓ DHCP Range: 10.10.199.200-254 (LXD containers)
✓ Bridge Interface: UP
```

**Container IP Ranges:**
- Docker containers: `10.10.199.100` - `10.10.199.199`
- LXD containers: `10.10.199.200` - `10.10.199.254`

---

### Server 2 (10.255.246.174)

**LXD Bridge Configuration:**
```yaml
networks:
- config:
    ipv4.address: 10.10.198.1/24
    ipv4.nat: "false"
    ipv4.dhcp.ranges: 10.10.198.200-10.10.198.254
  name: lxdbr0
  type: bridge
```

**Verified Status:**
```
✓ Bridge IP: 10.10.198.1/24
✓ NAT: Disabled
✓ DHCP Range: 10.10.198.200-254 (LXD containers)
✓ Bridge Interface: UP
```

**Container IP Ranges:**
- Docker containers: `10.10.198.100` - `10.10.198.199`
- LXD containers: `10.10.198.200` - `10.10.198.254`

---

### Server 3 (10.255.246.175)

**LXD Bridge Configuration:**
```yaml
networks:
- config:
    ipv4.address: 10.10.197.1/24
    ipv4.nat: "false"
    ipv4.dhcp.ranges: 10.10.197.200-10.10.197.254
  name: lxdbr0
  type: bridge
```

**Verified Status:**
```
✓ Bridge IP: 10.10.197.1/24
✓ NAT: Disabled
✓ DHCP Range: 10.10.197.200-254 (LXD containers)
✓ Bridge Interface: UP
```

**Container IP Ranges:**
- Docker containers: `10.10.197.100` - `10.10.197.199`
- LXD containers: `10.10.197.200` - `10.10.197.254`

---

## Key Configuration Features

### 1. NAT Disabled
All bridges have `ipv4.nat: "false"` configured, which means:
- No port forwarding required
- Containers get direct IP addresses
- BGP routing will announce these subnets

### 2. Static IP Support
Containers can be launched with static IPs:

**LXD Container:**
```bash
lxc launch ubuntu:22.04 mycontainer --config "ipv4.address=10.10.199.200"
```

**Docker Container:**
```bash
docker run -d --name mycontainer --network bgp-net --ip 10.10.199.100 nginx
```

### 3. DHCP Ranges
- LXD DHCP: `.200-.254` (55 IPs per host)
- Docker containers use static IPs in `.100-.199` range

---

## Integration with BGP Routing

These bridge configurations work with FRR BGP routing as follows:

1. **FRR runs on each host** with the overlay IP (10.9.9.x)
2. **BGP announces** the container subnet to other hosts
3. **Containers can communicate** across hosts without NAT
4. **Developers connect via VPN** and get routes pushed to access containers

### BGP Configuration (Already Deployed)

From previous deployment (`BDCOM_DEPLOYMENT_SUMMARY.md`):

| Server | Overlay IP | BGP AS | Subnet Announced |
|--------|-----------|---------|------------------|
| Server 1 | 10.9.9.4 | 65193 | 10.10.196.0/24 |
| Server 2 | 10.9.9.5 | 65192 | 10.10.197.0/24 |
| Server 3 | 10.9.9.6 | 65191 | 10.10.198.0/24 |

⚠️ **NOTE:** The existing BGP configuration uses **different subnets** than the LXD bridge configuration. This needs to be updated to match.

---

## Next Steps

### 1. Update BGP Configuration

The FRR BGP configuration needs to be updated to announce the correct subnets:

**Current BGP Announcements (INCORRECT):**
- Server 1: Announces 10.10.196.0/24 (should be 10.10.199.0/24)
- Server 2: Announces 10.10.197.0/24 (should be 10.10.198.0/24)
- Server 3: Announces 10.10.198.0/24 (should be 10.10.197.0/24)

**Required Updates:**

```bash
# Server 1 - Update FRR to announce 10.10.199.0/24
sudo vtysh -c 'conf t' \
  -c 'router bgp 65193' \
  -c 'address-family ipv4 unicast' \
  -c 'no network 10.10.196.0/24' \
  -c 'network 10.10.199.0/24' \
  -c 'exit' \
  -c 'exit' \
  -c 'write memory'

# Server 2 - Update FRR to announce 10.10.198.0/24
sudo vtysh -c 'conf t' \
  -c 'router bgp 65192' \
  -c 'address-family ipv4 unicast' \
  -c 'no network 10.10.197.0/24' \
  -c 'network 10.10.198.0/24' \
  -c 'exit' \
  -c 'exit' \
  -c 'write memory'

# Server 3 - Update FRR to announce 10.10.197.0/24
sudo vtysh -c 'conf t' \
  -c 'router bgp 65191' \
  -c 'address-family ipv4 unicast' \
  -c 'no network 10.10.198.0/24' \
  -c 'network 10.10.197.0/24' \
  -c 'exit' \
  -c 'exit' \
  -c 'write memory'
```

### 2. Update Overlay IPs (Optional but Recommended)

For consistency with networking guidelines, update overlay IPs:

**Current:**
- Server 1: 10.9.9.4
- Server 2: 10.9.9.5
- Server 3: 10.9.9.6

**Recommended:**
- Server 1: 10.9.9.1
- Server 2: 10.9.9.2
- Server 3: 10.9.9.3

### 3. Update AS Numbers (Optional but Recommended)

For consistency with networking guidelines:

**Current:**
- Server 1: AS 65193
- Server 2: AS 65192
- Server 3: AS 65191

**Recommended:**
- Server 1: AS 65199
- Server 2: AS 65198
- Server 3: AS 65197

### 4. Setup Docker BGP Networks

Configure Docker custom bridge networks on each server:

```bash
# Server 1
docker network create \
  --driver=bridge \
  --subnet=10.10.199.0/24 \
  --gateway=10.10.199.1 \
  --ip-range=10.10.199.100/25 \
  --opt "com.docker.network.bridge.name"="docker-bgp" \
  bgp-net

# Server 2
docker network create \
  --driver=bridge \
  --subnet=10.10.198.0/24 \
  --gateway=10.10.198.1 \
  --ip-range=10.10.198.100/25 \
  --opt "com.docker.network.bridge.name"="docker-bgp" \
  bgp-net

# Server 3
docker network create \
  --driver=bridge \
  --subnet=10.10.197.0/24 \
  --gateway=10.10.197.1 \
  --ip-range=10.10.197.100/25 \
  --opt "com.docker.network.bridge.name"="docker-bgp" \
  bgp-net
```

### 5. Test Container Connectivity

**Deploy test containers:**
```bash
# Server 1: Deploy test container
lxc launch ubuntu:22.04 test1 --config "ipv4.address=10.10.199.200"

# Server 2: Deploy test container
lxc launch ubuntu:22.04 test2 --config "ipv4.address=10.10.198.200"

# Server 3: Deploy test container
lxc launch ubuntu:22.04 test3 --config "ipv4.address=10.10.197.200"
```

**Verify cross-host connectivity:**
```bash
# From Server 1, ping container on Server 2
lxc exec test1 -- ping -c 3 10.10.198.200

# From Server 2, ping container on Server 3
lxc exec test2 -- ping -c 3 10.10.197.200

# From Server 3, ping container on Server 1
lxc exec test3 -- ping -c 3 10.10.199.200
```

---

## Verification Commands

### Check LXD Bridge Configuration
```bash
# On each server
sudo lxc network show lxdbr0
```

### Check Bridge Interface Status
```bash
# On each server
ip addr show lxdbr0
ip route show dev lxdbr0
```

### Check BGP Routes
```bash
# On each server
sudo vtysh -c 'show ip bgp summary'
sudo vtysh -c 'show ip bgp'
ip route show proto bgp
```

### List LXD Containers
```bash
# On each server
sudo lxc list
```

---

## Troubleshooting

### Bridge Not Created
```bash
# Reinitialize LXD if needed
sudo lxd init --preseed < preseed.yaml
```

### BGP Not Announcing Routes
```bash
# Check FRR configuration
sudo vtysh -c 'show running-config'

# Restart FRR
sudo systemctl restart frr
```

### Containers Can't Reach Other Hosts
```bash
# Verify BGP routes are in kernel routing table
ip route show proto bgp

# Test overlay connectivity
ping 10.9.9.X  # Replace X with overlay IP of other server

# Check iptables rules (should not have NAT)
sudo iptables -t nat -L -n -v
```

---

## Documentation References

1. **Networking Guidelines**: `/home/mustafa/telcobright-projects/orchestrix/images/networking_guideline_claude.md`
2. **FRR Deployment Guide**: `FRR_DEPLOYMENT_GUIDE.md`
3. **BDCOM BGP Deployment**: `BDCOM_DEPLOYMENT_SUMMARY.md`
4. **Docker Bridge Setup**: `DOCKER_BRIDGE_NETWORK_SETUP.md`

---

## Conclusion

✅ **All three BDCOM servers configured successfully**
✅ **LXD bridges match networking guidelines**
✅ **NAT disabled for direct container access**
✅ **Ready for BGP route announcement**
⚠️ **BGP configuration needs updating to match new subnets**

The LXD bridge configurations are now consistent with the Orchestrix networking guidelines and ready for production container deployments.

---

**Next Action Required:**
Update BGP configuration to announce the correct subnets (10.10.199.0/24, 10.10.198.0/24, 10.10.197.0/24)
