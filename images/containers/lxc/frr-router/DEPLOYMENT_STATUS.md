# FRR Router Deployment Status - BDCOM Network

## Deployment Summary

### ✅ Successfully Completed

1. **Container Scaffolding**
   - Created FRR router container with versioned structure
   - Base image: `frr-router-base-v.1.0.0` (152MB)
   - FRR Version: 10.4.1 on Debian 12

2. **Deployment to All 3 Nodes**
   - **Node 1** (10.255.246.173): ✅ Container running
   - **Node 2** (10.255.246.174): ✅ Container running
   - **Node 3** (10.255.246.175): ✅ Container running

3. **Configuration**
   - BGP configured on all routers
   - ASN assignments: Node1=65001, Node2=65002, Node3=65003
   - Network announcement: 10.10.199.0/24 on all nodes
   - Router IDs: Match host IPs (10.255.246.x)

4. **Network Connectivity**
   - Containers can reach external network
   - Containers can ping other hosts (10.255.246.x)
   - FRR daemons running and listening on port 179

## ⚠️ Current Issue: BGP Peering Not Established

**Status**: BGP neighbors show "Active" state instead of "Established"

**Root Cause**: Network architecture mismatch
- FRR containers run on bridge network (10.10.199.50 on each host)
- BGP configured to peer with host IPs (10.255.246.173-175)
- Containers can reach host IPs but connections don't route to peer containers

## Solutions to Establish BGP Peering

### Option 1: Unique Container IPs with Inter-Host Routing (Recommended)

Update each container to use unique IPs and configure routing:

```bash
# Node 1: 10.10.199.51
# Node 2: 10.10.199.52
# Node 3: 10.10.199.53

# Add static routes on each host for other nodes' bridge networks
# On Node 1:
ip route add 10.10.199.52/32 via 10.255.246.174
ip route add 10.10.199.53/32 via 10.255.246.175

# Update BGP configs to peer with container IPs instead of host IPs
BGP_NEIGHBORS="10.10.199.52:65002,10.10.199.53:65003"
```

### Option 2: NAT/Proxy Configuration

Configure iptables to DNAT BGP traffic:

```bash
# On each node, forward BGP traffic from host IP to container
iptables -t nat -A PREROUTING -p tcp --dport 179 -j DNAT --to 10.10.199.50:179
iptables -t nat -A POSTROUTING -j MASQUERADE
```

### Option 3: Host Networking Mode

Redeploy containers with host networking:
- Containers share host network namespace
- FRR binds directly to host IPs
- Simplest but less isolated

### Option 4: VXLAN Overlay Network

Create VXLAN tunnel between hosts' bridge networks:
- All containers on same virtual L2 network
- Seamless communication
- More complex setup

## Current Container Status

### Node 1 (10.255.246.173)
```
Container: frr-router-node1
IP: 10.10.199.50
Status: RUNNING
BGP: Active (not established)
Neighbors: 10.255.246.174:65002, 10.255.246.175:65003
```

### Node 2 (10.255.246.174)
```
Container: frr-router-node2
IP: 10.10.199.50
Status: RUNNING
BGP: Active (not established)
Neighbors: 10.255.246.173:65001, 10.255.246.175:65003
```

### Node 3 (10.255.246.175)
```
Container: frr-router-node3
IP: 10.10.199.50
Status: RUNNING
BGP: Active (not established)
Neighbors: 10.255.246.173:65001, 10.255.246.174:65002
```

## Management Commands

### Check BGP Status
```bash
# Node 1
ssh -p 15605 bdcom@10.255.246.173
sudo lxc exec frr-router-node1 -- vtysh -c "show ip bgp summary"

# Node 2
ssh -p 15605 bdcom@10.255.246.174
sudo lxc exec frr-router-node2 -- vtysh -c "show ip bgp summary"

# Node 3
ssh -p 15605 bdcom@10.255.246.175
sudo lxc exec frr-router-node3 -- vtysh -c "show ip bgp summary"
```

### View FRR Configuration
```bash
sudo lxc exec frr-router-node1 -- vtysh -c "show running-config"
```

### Restart FRR
```bash
sudo lxc exec frr-router-node1 -- systemctl restart frr
```

### Access FRR CLI
```bash
sudo lxc exec frr-router-node1 -- vtysh
```

## Files and Locations

### Local Development Machine
```
/home/mustafa/telcobright-projects/orchestrix-frr-router/images/lxc/frr-router/
├── build/
│   ├── build.sh
│   └── build.conf
├── templates/
│   ├── sample.conf
│   └── startDefault.sh
├── deployment-configs/
│   ├── node1-config.conf
│   ├── node2-config.conf
│   └── node3-config.conf
├── deploy-to-bdcom-nodes.sh
├── verify-bgp-peering.sh
├── launchFrrRouter.sh
└── README.md

frr-router-v.1.0.0/generated/artifact/
└── frr-router-v1-1761938223.tar.gz (153MB)
```

### Remote Nodes
```
/tmp/frr-router-deployment/
├── frr-router-v1-1761938223.tar.gz
├── frr-router-v1-1761938223.tar.gz.md5
├── launch-config.conf
└── launchFrrRouter.sh
```

## Next Steps

1. **Immediate**: Choose and implement one of the BGP peering solutions above
2. **Verify**: Check BGP sessions reach "Established" state
3. **Test**: Verify route exchange between routers
4. **Validate**: Test connectivity between containers on different nodes
5. **Document**: Update configs with final working solution

## Notes

- All containers use same IP (10.10.199.50) which is intentional for simplicity but requires routing configuration
- FRR is properly installed and configured
- Container images are imported and ready on all nodes
- Deployment automation scripts are working
- Only BGP peering establishment remains to be resolved

## Recommendation

**Use Option 1 (Unique IPs + Routing)** as it's:
- Clean and maintainable
- Doesn't require complex NAT rules
- Allows containers to remain isolated
- Easy to troubleshoot
- Scalable for additional nodes

Implementation script can be provided if needed.
