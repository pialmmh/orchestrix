# Consul LXC Container

Lightweight Alpine-based Consul container for service mesh and service discovery. Optimized for clustering across multiple servers.

## Container Information

- **Base Image**: Alpine Linux 3.20
- **Container Size**: ~5-8 MB
- **Consul Version**: 1.17.0 (configurable)
- **Type**: Binary Container (stateless)

## Features

- ✅ **Lightweight**: Alpine-based, minimal footprint
- ✅ **Clustering**: Designed for multi-node deployment
- ✅ **Flexible Configuration**: All parameters via runtime config
- ✅ **Auto-Discovery**: Nodes automatically join cluster
- ✅ **Web UI**: Built-in Consul UI enabled
- ✅ **Health Checks**: Service health monitoring

## Port Configuration

For **same-server testing**, each node uses different ports to avoid conflicts:

| Node | HTTP/UI | DNS | Server RPC | Serf LAN | Serf WAN |
|------|---------|-----|------------|----------|----------|
| Node 1 | 8500 | 8600 | 8300 | 8301 | 8302 |
| Node 2 | 8510 | 8610 | 8310 | 8311 | 8312 |
| Node 3 | 8520 | 8620 | 8320 | 8321 | 8322 |

For **production across 3 servers**, all nodes can use standard ports (8500, etc.) since they're on different machines.

## Quick Start

### 1. Build Container

```bash
cd /home/mustafa/telcobright-projects/orchestrix/images/lxc/consul
./build/build.sh
```

This will:
- Download Consul binary (if not exists)
- Create Alpine container
- Export to `consul-v.1/generated/artifact/`

### 2. Test Locally (3-Node Cluster)

```bash
cd consul-v.1/generated
./startDefault.sh
```

This automatically:
- Launches 3 Consul nodes
- Configures cluster formation
- Shows all access URLs

**Consul UI**: Open `http://NODE_IP:8500/ui` in browser

### 3. Check Cluster Status

```bash
# View cluster members
lxc exec consul-node-1 -- consul members

# Check leader election
lxc exec consul-node-1 -- consul operator raft list-peers

# View logs
lxc exec consul-node-1 -- tail -f /var/log/consul.log
```

### 4. Stop/Cleanup

```bash
# Stop cluster
lxc stop consul-node-1 consul-node-2 consul-node-3

# Delete cluster
lxc delete consul-node-1 consul-node-2 consul-node-3 --force
```

---

## Production Deployment

### Deploy Across 3 Servers

#### Server 1:

```bash
# Copy configs
scp consul-v.1/generated/artifact/consul-v1-*.tar.gz server1:/tmp/
scp consul-v.1/generated/node1.conf server1:/tmp/
scp launchConsul.sh server1:/tmp/

# On server1:
./launchConsul.sh /tmp/node1.conf consul-node-1

# Get IP address
lxc list consul-node-1
# Note the IP (e.g., 10.10.199.100)
```

#### Server 2:

```bash
# Copy configs
scp consul-v.1/generated/artifact/consul-v1-*.tar.gz server2:/tmp/
scp consul-v.1/generated/node2.conf server2:/tmp/
scp launchConsul.sh server2:/tmp/

# Update node2.conf with Server 1 IP
# Edit: RETRY_JOIN='["10.10.199.100"]'

# On server2:
./launchConsul.sh /tmp/node2.conf consul-node-2

# Get IP address
lxc list consul-node-2
# Note the IP (e.g., 10.10.199.101)
```

#### Server 3:

```bash
# Copy configs
scp consul-v.1/generated/artifact/consul-v1-*.tar.gz server3:/tmp/
scp consul-v.1/generated/node3.conf server3:/tmp/
scp launchConsul.sh server3:/tmp/

# Update node3.conf with Server 1 & 2 IPs
# Edit: RETRY_JOIN='["10.10.199.100","10.10.199.101"]'

# On server3:
./launchConsul.sh /tmp/node3.conf consul-node-3
```

Cluster will automatically form when all 3 nodes are up!

---

## Configuration

### Runtime Configuration (sample.conf)

Each node is configured at runtime via a `.conf` file:

```bash
# Container Settings
CONTAINER_NAME="consul-node-1"              # Unique container name
IMAGE_PATH="/path/to/consul-v1-TIMESTAMP.tar.gz"

# Consul Configuration
NODE_ID="consul-node-1"                     # Unique node ID
DATACENTER="dc1"                            # Datacenter name
SERVER_MODE="true"                          # true for server
BOOTSTRAP_EXPECT="3"                        # Expected servers

# Network
BIND_ADDR="0.0.0.0"                         # Bind to container IP
RETRY_JOIN='["10.10.199.101","10.10.199.102"]'  # Other nodes

# Ports (defaults shown)
HTTP_PORT="8500"                            # HTTP API & UI
DNS_PORT="8600"                             # DNS interface
SERVER_RPC_PORT="8300"                      # Server RPC
SERF_LAN_PORT="8301"                        # Serf LAN
SERF_WAN_PORT="8302"                        # Serf WAN

# Resources
MEMORY_LIMIT="512MB"
CPU_LIMIT="1"

# Logging
LOG_LEVEL="INFO"                            # DEBUG, INFO, WARN, ERROR
```

### Clustering Configuration

**First Node (Bootstrap)**:
```bash
RETRY_JOIN='[]'                             # Empty - no nodes to join
```

**Second Node**:
```bash
RETRY_JOIN='["10.10.199.100"]'              # Join first node
```

**Third Node**:
```bash
RETRY_JOIN='["10.10.199.100","10.10.199.101"]'  # Join both nodes
```

---

## API Endpoints

### Consul HTTP API

All nodes expose Consul HTTP API on port 8500:

**Health Checks**:
```bash
curl http://NODE_IP:8500/v1/status/leader
curl http://NODE_IP:8500/v1/status/peers
```

**Service Registration**:
```bash
curl -X PUT http://NODE_IP:8500/v1/agent/service/register \
  -d '{"Name": "my-service", "Port": 8080}'
```

**Service Discovery**:
```bash
curl http://NODE_IP:8500/v1/catalog/services
curl http://NODE_IP:8500/v1/catalog/service/my-service
```

**Key-Value Store**:
```bash
# Write
curl -X PUT http://NODE_IP:8500/v1/kv/my-key -d 'my-value'

# Read
curl http://NODE_IP:8500/v1/kv/my-key
```

### Consul Web UI

Access via browser:
- `http://NODE_IP:8500/ui`

Features:
- Service catalog
- Health checks
- Key-value editor
- Nodes and datacenters
- Access control (ACLs)

---

## Using Consul with Other Services

### Example: Go-ID with Consul

Go-ID can register with Consul for service discovery:

```bash
# In go-id container config:
CONSUL_URL="http://CONSUL_NODE_IP:8500"
SERVICE_NAME="go-id"
```

Go-ID will:
- Register itself with Consul
- Report health status
- Deregister on shutdown

---

## Monitoring

### Check Cluster Health

```bash
# Cluster members
lxc exec consul-node-1 -- consul members

# Raft peers (leader election)
lxc exec consul-node-1 -- consul operator raft list-peers

# Who is leader?
lxc exec consul-node-1 -- consul operator raft list-peers | grep leader
```

### View Logs

```bash
# Real-time logs
lxc exec consul-node-1 -- tail -f /var/log/consul.log

# Error logs
lxc exec consul-node-1 -- tail -f /var/log/consul-error.log

# Last 100 lines
lxc exec consul-node-1 -- tail -100 /var/log/consul.log
```

### Common Issues

**Node not joining cluster**:
```bash
# Check if node can reach others
lxc exec consul-node-2 -- ping CONSUL_NODE_1_IP

# Check Consul logs
lxc exec consul-node-2 -- tail -f /var/log/consul.log
```

**Wrong node count**:
```bash
# Verify BOOTSTRAP_EXPECT matches number of servers
# All servers should have same value (e.g., 3)
```

---

## Architecture

### Deployment Patterns

**3-Server Production** (Recommended):
- 3 servers in different availability zones
- Each runs 1 Consul server node
- Provides fault tolerance (can lose 1 server)

**5-Server High Availability**:
- 5 servers across zones
- Can tolerate 2 server failures

**Development/Testing**:
- 3 nodes on same machine (use startDefault.sh)
- Good for local testing

### Ports

**Standard Consul Ports** (Production - Different Servers):

| Port | Protocol | Purpose |
|------|----------|---------|
| 8500 | HTTP | API and Web UI |
| 8600 | DNS | DNS interface |
| 8300 | TCP | Server RPC |
| 8301 | TCP/UDP | Serf LAN |
| 8302 | TCP/UDP | Serf WAN |

**Same-Server Testing Ports** (All 3 nodes on one machine):

| Component | Node 1 | Node 2 | Node 3 |
|-----------|--------|--------|--------|
| HTTP/UI | 8500 | 8510 | 8520 |
| DNS | 8600 | 8610 | 8620 |
| Server RPC | 8300 | 8310 | 8320 |
| Serf LAN | 8301 | 8311 | 8321 |
| Serf WAN | 8302 | 8312 | 8322 |

---

## Troubleshooting

### Cluster Not Forming

1. Check network connectivity between nodes
2. Verify RETRY_JOIN IPs are correct
3. Check BOOTSTRAP_EXPECT matches server count
4. View logs for errors

### Leader Election Issues

```bash
# Force new election (if needed)
lxc exec consul-node-1 -- consul operator raft list-peers
```

### Container Won't Start

```bash
# Check container logs
lxc info consul-node-1 --show-log

# Check Consul service
lxc exec consul-node-1 -- rc-status
```

---

## Files

- `build/build.sh` - Build script
- `build/build.conf` - Build configuration
- `launchConsul.sh` - Launch script
- `consul-v.1/generated/node1.conf` - Node 1 config
- `consul-v.1/generated/node2.conf` - Node 2 config
- `consul-v.1/generated/node3.conf` - Node 3 config
- `consul-v.1/generated/sample.conf` - Configuration template
- `consul-v.1/generated/startDefault.sh` - Local 3-node cluster
- `consul-v.1/generated/artifact/*.tar.gz` - Container image

---

## References

- [Consul Official Documentation](https://developer.hashicorp.com/consul/docs)
- [Consul HTTP API](https://developer.hashicorp.com/consul/api-docs)
- [Orchestrix LXC Guidelines](../CONTAINER_SCAFFOLDING_STANDARD.md)
