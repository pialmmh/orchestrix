# Network Configuration

This directory contains network-specific configurations for Orchestrix, separate from infrastructure-as-code.

## Files

### vxlan-lxc.conf
Complete VXLAN network configuration for LXC containers including:
- Subnet definitions (management, development, production, database, monitoring)
- Container IP assignments
- DHCP pool ranges
- Inter-subnet routing rules
- NAT configuration
- Bridge settings

## Network Architecture

```
Physical Network (eth0)
    │
    ├── VXLAN 100 ─► Bridge br-vx100 ─► Management (10.100.0.0/24)
    ├── VXLAN 101 ─► Bridge br-vx101 ─► Development (10.101.0.0/24)
    ├── VXLAN 102 ─► Bridge br-vx102 ─► Production (10.102.0.0/24)
    ├── VXLAN 103 ─► Bridge br-vx103 ─► Database (10.103.0.0/24)
    └── VXLAN 104 ─► Bridge br-vx104 ─► Monitoring (10.104.0.0/24)
```

## Usage

### Shell Script Setup
```bash
# Quick setup using shell script
sudo ../setup-vxlan-lxc.sh
```

### Java Orchestrator
```bash
cd ../automation
mvn compile
mvn exec:java -Dexec.mainClass="com.orchestrix.automation.vxlan.example.VXLANLXCExample"
```

### Manual Container Assignment
```bash
# Assign container to subnet
lxc config device add <container> eth1 nic nictype=bridged parent=br-vx101

# Configure IP inside container
lxc exec <container> -- ip addr add 10.101.0.20/24 dev eth1
lxc exec <container> -- ip route add default via 10.101.0.1
```

## Configuration Format

### Subnet Definition
```
SUBNET_NAME:VXLAN_ID:CIDR:GATEWAY:MULTICAST_GROUP:DESCRIPTION
```

### Container IP Assignment
```
CONTAINER_NAME:SUBNET_NAME:IP_ADDRESS:MAC_ADDRESS(optional)
```

### DHCP Pool
```
SUBNET_NAME:START_IP:END_IP
```

### Routing Rules
```
SOURCE_SUBNET:DEST_SUBNET:ALLOW/DENY
```

## Network Isolation

Each subnet is isolated by default. Traffic between subnets is controlled by routing rules:
- Management subnet can reach all others
- Development can access database and monitoring
- Production can access database and monitoring
- Database can only reach monitoring

## IP Management

- **Static IPs**: Defined in CONTAINER_IPS for specific containers
- **Dynamic IPs**: Allocated from DHCP_POOLS ranges
- **Reserved IPs**: Gateway IPs (.1) are reserved for bridges

## Troubleshooting

### Check VXLAN Status
```bash
ip -d link show type vxlan
```

### Check Bridge Status
```bash
bridge link show
ip addr show br-vx101
```

### Check Container Network
```bash
lxc config device show <container>
lxc exec <container> -- ip addr show
```

### Monitor Traffic
```bash
tcpdump -i vxlan101 -n
bridge monitor
```