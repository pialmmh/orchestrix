# FRR Router LXC Container

A minimal FRR (Free Range Routing) router container for announcing LXC container networks via BGP and OSPF.

## Overview

This container runs FRRouting (FRR) daemon to dynamically announce container IP addresses across multiple physical machines using BGP or OSPF routing protocols.

### Use Case

- **Machine A** has containers: 10.10.199.10, 10.10.199.11, 10.10.199.12
- **Machine B** has containers: 10.10.199.20, 10.10.199.21, 10.10.199.22
- Each machine runs an FRR router container that announces its local container prefixes
- Other machines learn these routes and can reach containers on remote machines

## Features

- **Multiple routing protocols**: BGP and OSPF support (configurable)
- **Multiple network interfaces**: Support for multi-homed routers
- **Dynamic configuration**: All routing config from launch-time config file
- **Flexible prefix announcement**: Announce entire subnets or specific /32 host routes
- **Standard FRR**: Full FRR routing suite with vtysh CLI

## Quick Start

### 1. Build the Base Image

First, build the FRR router base image:

```bash
cd images/lxc/frr-router
sudo ./buildFrrRouter.sh
```

This creates the `frr-router-base` image (one-time operation).

### 2. Launch with Default Config

```bash
sudo ./startDefault.sh
```

This launches a container named `frr-router-1` with the sample configuration.

### 3. Verify Router is Running

```bash
# Access FRR console
lxc exec frr-router-1 -- vtysh

# Check routes
lxc exec frr-router-1 -- vtysh -c "show ip route"

# Check BGP status
lxc exec frr-router-1 -- vtysh -c "show ip bgp summary"

# Check OSPF neighbors
lxc exec frr-router-1 -- vtysh -c "show ip ospf neighbor"
```

## Configuration

### Launch Configuration File

Create a configuration file with your routing parameters:

```bash
# Container settings
CONTAINER_NAME="frr-router-1"
BASE_IMAGE="frr-router-base"

# Network interfaces (multiple supported)
NETWORK_INTERFACES="eth0:lxdbr0:10.10.199.50/24"

# Gateway and DNS
GATEWAY_IP="10.10.199.1/24"
DNS_SERVERS="8.8.8.8 8.8.4.4"

# Router identity
ROUTER_ID="10.10.199.50"
ROUTER_HOSTNAME="frr-router-1"

# BGP configuration
ENABLE_BGP="true"
BGP_ASN="65001"
BGP_NEIGHBORS="10.10.199.1:65000"
BGP_NETWORKS="10.10.199.0/24"

# OSPF configuration
ENABLE_OSPF="false"
```

### Launch with Custom Config

```bash
sudo ./launchFrrRouter.sh /path/to/your-config.conf
```

## Configuration Examples

### Example 1: Simple BGP Router

Announce local container subnet via BGP:

```bash
CONTAINER_NAME="bgp-router-1"
BASE_IMAGE="frr-router-base"
NETWORK_INTERFACES="eth0:lxdbr0:10.10.199.50/24"
GATEWAY_IP="10.10.199.1/24"
DNS_SERVERS="8.8.8.8"
ROUTER_ID="10.10.199.50"

ENABLE_BGP="true"
BGP_ASN="65001"
BGP_NEIGHBORS="10.10.199.1:65000"
BGP_NETWORKS="10.10.199.0/24"
ENABLE_OSPF="false"
```

### Example 2: Announce Specific Container IPs

Announce individual container /32 routes:

```bash
CONTAINER_NAME="container-router"
BASE_IMAGE="frr-router-base"
NETWORK_INTERFACES="eth0:lxdbr0:10.10.199.50/24"
GATEWAY_IP="10.10.199.1/24"
DNS_SERVERS="8.8.8.8"
ROUTER_ID="10.10.199.50"

ENABLE_BGP="true"
BGP_ASN="65001"
BGP_NEIGHBORS="10.10.199.1:65000"
# Announce specific container IPs
BGP_NETWORKS="10.10.199.10/32,10.10.199.11/32,10.10.199.12/32"
ENABLE_OSPF="false"
```

### Example 3: Multi-interface Router

Router with multiple network interfaces:

```bash
CONTAINER_NAME="multi-router"
BASE_IMAGE="frr-router-base"
# Multiple interfaces: format interface_name:bridge:ip|interface_name:bridge:ip
NETWORK_INTERFACES="eth0:lxdbr0:10.10.199.50/24|eth1:lxdbr1:192.168.1.50/24"
GATEWAY_IP="10.10.199.1/24"
DNS_SERVERS="8.8.8.8"
ROUTER_ID="10.10.199.50"

ENABLE_BGP="true"
BGP_ASN="65001"
# Peer with routers on both networks
BGP_NEIGHBORS="10.10.199.1:65000,192.168.1.1:65002"
# Announce both networks
BGP_NETWORKS="10.10.199.0/24,192.168.1.0/24"
ENABLE_OSPF="false"
```

### Example 4: OSPF Router

Use OSPF instead of BGP:

```bash
CONTAINER_NAME="ospf-router-1"
BASE_IMAGE="frr-router-base"
NETWORK_INTERFACES="eth0:lxdbr0:10.10.199.50/24"
GATEWAY_IP="10.10.199.1/24"
DNS_SERVERS="8.8.8.8"
ROUTER_ID="10.10.199.50"

ENABLE_BGP="false"
ENABLE_OSPF="true"
OSPF_AREA="0.0.0.0"
OSPF_NETWORKS="10.10.199.0/24"
```

### Example 5: Hybrid BGP + OSPF

Run both BGP and OSPF:

```bash
CONTAINER_NAME="hybrid-router"
BASE_IMAGE="frr-router-base"
NETWORK_INTERFACES="eth0:lxdbr0:10.10.199.50/24|eth1:lxdbr1:192.168.1.50/24"
GATEWAY_IP="10.10.199.1/24"
DNS_SERVERS="8.8.8.8"
ROUTER_ID="10.10.199.50"

# BGP for external peering
ENABLE_BGP="true"
BGP_ASN="65001"
BGP_NEIGHBORS="10.10.199.1:65000"
BGP_NETWORKS="10.10.199.0/24"

# OSPF for internal routing
ENABLE_OSPF="true"
OSPF_AREA="0.0.0.0"
OSPF_NETWORKS="192.168.1.0/24"
```

## Network Interface Configuration

The `NETWORK_INTERFACES` parameter supports multiple interfaces:

**Format**: `interface_name:bridge:ip_with_mask|interface_name:bridge:ip_with_mask`

**Single interface**:
```bash
NETWORK_INTERFACES="eth0:lxdbr0:10.10.199.50/24"
```

**Multiple interfaces**:
```bash
NETWORK_INTERFACES="eth0:lxdbr0:10.10.199.50/24|eth1:lxdbr1:192.168.1.50/24|eth2:lxdbr2:172.16.0.50/24"
```

## Management Commands

### Container Management

```bash
# Stop container
lxc stop frr-router-1

# Start container
lxc start frr-router-1

# Restart container
lxc restart frr-router-1

# Delete container
lxc delete frr-router-1
```

### FRR Management

```bash
# Access FRR CLI
lxc exec frr-router-1 -- vtysh

# Show running config
lxc exec frr-router-1 -- vtysh -c "show running-config"

# Show IP routes
lxc exec frr-router-1 -- vtysh -c "show ip route"

# Show BGP summary
lxc exec frr-router-1 -- vtysh -c "show ip bgp summary"

# Show BGP neighbors
lxc exec frr-router-1 -- vtysh -c "show ip bgp neighbors"

# Show advertised routes
lxc exec frr-router-1 -- vtysh -c "show ip bgp neighbors 10.10.199.1 advertised-routes"

# Show OSPF neighbors
lxc exec frr-router-1 -- vtysh -c "show ip ospf neighbor"

# Show OSPF routes
lxc exec frr-router-1 -- vtysh -c "show ip ospf route"
```

### Reconfigure FRR

If you need to change routing configuration without recreating the container:

```bash
# Edit the config inside container
lxc exec frr-router-1 -- vim /etc/frr-launch-config.conf

# Regenerate FRR config
lxc exec frr-router-1 -- /usr/local/bin/configure-frr-from-env.sh
```

Or push a new config from host:

```bash
# Push new config
lxc file push new-config.conf frr-router-1/etc/frr-launch-config.conf

# Regenerate FRR config
lxc exec frr-router-1 -- /usr/local/bin/configure-frr-from-env.sh
```

## Troubleshooting

### Check FRR Status

```bash
lxc exec frr-router-1 -- systemctl status frr
```

### Check FRR Logs

```bash
lxc exec frr-router-1 -- tail -f /var/log/frr/frr.log
```

### Debug BGP

```bash
# Enable BGP debugging
lxc exec frr-router-1 -- vtysh -c "configure terminal" -c "debug bgp updates"

# Check BGP state
lxc exec frr-router-1 -- vtysh -c "show ip bgp"
```

### Debug OSPF

```bash
# Enable OSPF debugging
lxc exec frr-router-1 -- vtysh -c "configure terminal" -c "debug ospf packet all"

# Check OSPF state
lxc exec frr-router-1 -- vtysh -c "show ip ospf"
```

### Network Connectivity

```bash
# Check interfaces
lxc exec frr-router-1 -- ip addr

# Check routes
lxc exec frr-router-1 -- ip route

# Test connectivity
lxc exec frr-router-1 -- ping 10.10.199.1
```

## Architecture

### Build Phase

1. Creates Debian 12 container
2. Installs FRR from official repository
3. Enables zebra, bgpd, ospfd daemons
4. Creates base configuration
5. Installs dynamic config generator script
6. Publishes as `frr-router-base` image

### Launch Phase

1. Creates container from base image
2. Configures network interfaces (single or multiple)
3. Sets up IP addresses and routing
4. Pushes launch configuration to container
5. Generates FRR config based on parameters
6. Starts FRR with generated configuration

### FRR Configuration Generation

The container includes `/usr/local/bin/configure-frr-from-env.sh` which:
- Reads `/etc/frr-launch-config.conf`
- Generates `/etc/frr/frr.conf` based on parameters
- Configures BGP/OSPF/static routes as specified
- Restarts FRR to apply configuration

## Configuration Reference

### Required Parameters

- `CONTAINER_NAME` - Unique container name
- `BASE_IMAGE` - Base image name (usually `frr-router-base`)
- `NETWORK_INTERFACES` - Network interface configuration
- `ROUTER_ID` - Router ID (typically an IP address)

### BGP Parameters

- `ENABLE_BGP` - Enable BGP (`true`/`false`)
- `BGP_ASN` - BGP AS number
- `BGP_NEIGHBORS` - BGP neighbors (format: `ip:asn,ip:asn`)
- `BGP_NETWORKS` - Networks to announce (format: `prefix,prefix`)

### OSPF Parameters

- `ENABLE_OSPF` - Enable OSPF (`true`/`false`)
- `OSPF_AREA` - OSPF area (default: `0.0.0.0`)
- `OSPF_NETWORKS` - Networks to advertise (format: `prefix,prefix`)

### Optional Parameters

- `GATEWAY_IP` - Default gateway
- `DNS_SERVERS` - DNS servers
- `STATIC_ROUTES` - Static routes (format: `prefix|nexthop,prefix|nexthop`)
- `MEMORY_LIMIT` - Container memory limit (e.g., `512MB`)
- `CPU_LIMIT` - Container CPU limit (e.g., `2`)
- `ROUTER_HOSTNAME` - Hostname for the router

## Files

- `buildFrrRouter.sh` - Build script (creates base image)
- `buildFrrRouterConfig.cnf` - Build configuration
- `launchFrrRouter.sh` - Launch script (creates containers)
- `sample-launch-config.conf` - Sample launch configuration
- `startDefault.sh` - Quick start with default config
- `README.md` - This file

## Network Requirements

- LXC bridge(s) configured (e.g., `lxdbr0`)
- IP forwarding enabled on host
- Proper routing/NAT configured for internet access during build

## Version

Version: 1.0.0
Base OS: Debian 12
FRR Version: 8.4 (from official FRR repository)

## Support

For issues or questions, check:
- FRR documentation: https://docs.frrouting.org/
- Container logs: `lxc exec <container> -- tail -f /var/log/frr/frr.log`
- FRR status: `lxc exec <container> -- systemctl status frr`
