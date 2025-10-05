# Minimal Consul Setup for Small Cloud (4-5 Servers)

## Architecture Overview

For a small cloud with 4-5 servers running Go-ID and other microservices, we recommend:

### Option 1: Single Consul Server (Simplest - No HA)
**Best for**: Development, testing, non-critical production

```
Server 1: Consul Server (in LXC container)
Server 2: App Server (Go-ID shard-0) + Consul Client
Server 3: App Server (Go-ID shard-1) + Consul Client
Server 4: App Server (Go-ID shard-2) + Consul Client
Server 5: App Server (other services) + Consul Client
```

### Option 2: 3-Node Consul Cluster (Recommended - HA)
**Best for**: Production with high availability

```
Server 1: Consul Server 1 (leader) + App Server
Server 2: Consul Server 2 (follower) + App Server
Server 3: Consul Server 3 (follower) + App Server
Server 4: App Server + Consul Client
Server 5: App Server + Consul Client
```

## Recommended Setup: Option 2 (3-Node Cluster)

This provides:
- High availability (survives 1 server failure)
- Minimal overhead (Consul uses ~100MB RAM per server)
- Co-located with apps (no dedicated servers needed)

## Quick Setup Guide

### Step 1: Install Consul on All Servers

```bash
# On each server (Ubuntu/Debian)
wget https://releases.hashicorp.com/consul/1.17.0/consul_1.17.0_linux_amd64.zip
unzip consul_1.17.0_linux_amd64.zip
sudo mv consul /usr/local/bin/
sudo mkdir -p /etc/consul.d /var/lib/consul
sudo useradd -r -s /bin/false consul
sudo chown -R consul:consul /var/lib/consul
```

### Step 2: Configure Consul Servers (Server 1, 2, 3)

Create `/etc/consul.d/server.hcl` on each server:

```hcl
# Server 1 (192.168.1.100)
datacenter = "dc1"
data_dir = "/var/lib/consul"
log_level = "INFO"

server = true
bootstrap_expect = 3

bind_addr = "192.168.1.100"
client_addr = "0.0.0.0"

retry_join = ["192.168.1.100", "192.168.1.101", "192.168.1.102"]

ui_config {
  enabled = true
}

# Performance tuning for small cluster
performance {
  raft_multiplier = 1
}
```

**Server 2 and 3**: Same config but change `bind_addr` to their IPs:
- Server 2: `bind_addr = "192.168.1.101"`
- Server 3: `bind_addr = "192.168.1.102"`

### Step 3: Configure Consul Clients (Server 4, 5)

Create `/etc/consul.d/client.hcl`:

```hcl
# Server 4 (192.168.1.103)
datacenter = "dc1"
data_dir = "/var/lib/consul"
log_level = "INFO"

server = false

bind_addr = "192.168.1.103"
client_addr = "0.0.0.0"

retry_join = ["192.168.1.100", "192.168.1.101", "192.168.1.102"]
```

### Step 4: Create Systemd Service

Create `/etc/systemd/system/consul.service` on all servers:

```ini
[Unit]
Description=Consul Agent
Documentation=https://www.consul.io/
After=network-online.target
Wants=network-online.target

[Service]
Type=notify
User=consul
Group=consul
ExecStart=/usr/local/bin/consul agent -config-dir=/etc/consul.d/
ExecReload=/bin/kill -HUP $MAINPID
KillMode=process
KillSignal=SIGTERM
Restart=on-failure
LimitNOFILE=65536

[Install]
WantedBy=multi-user.target
```

### Step 5: Start Consul

```bash
# On all servers
sudo systemctl daemon-reload
sudo systemctl enable consul
sudo systemctl start consul

# Check status
sudo systemctl status consul
consul members
```

Expected output:
```
Node        Address              Status  Type    Build   Protocol  DC   Partition  Segment
server-1    192.168.1.100:8301   alive   server  1.17.0  2         dc1  default    <all>
server-2    192.168.1.101:8301   alive   server  1.17.0  2         dc1  default    <all>
server-3    192.168.1.102:8301   alive   server  1.17.0  2         dc1  default    <all>
server-4    192.168.1.103:8301   alive   client  1.17.0  2         dc1  default    <default>
server-5    192.168.1.104:8301   alive   client  1.17.0  2         dc1  default    <default>
```

## Integrate with Go-ID Containers

### Option A: Consul on Host, Services in LXC

**Configure Go-ID containers to register with host Consul:**

```bash
# On Server 2 (running go-id-shard-0)
# Get host IP (as seen from container)
HOST_IP="192.168.1.101"

# Configure container
lxc config set go-id-shard-0 environment.CONSUL_URL "http://${HOST_IP}:8500"
lxc config set go-id-shard-0 environment.SERVICE_NAME "go-id"
lxc config set go-id-shard-0 environment.CONTAINER_IP "10.200.100.50"
lxc restart go-id-shard-0
```

### Option B: Consul Client Inside LXC (More Isolated)

**Run Consul client inside each LXC container:**

1. Install Consul in LXC container:
```bash
lxc exec go-id-shard-0 -- bash -c "
wget -q https://releases.hashicorp.com/consul/1.17.0/consul_1.17.0_linux_amd64.zip
unzip consul_1.17.0_linux_amd64.zip -d /usr/local/bin/
rm consul_1.17.0_linux_amd64.zip
"
```

2. Configure Consul client in container:
```bash
lxc exec go-id-shard-0 -- mkdir -p /etc/consul.d

lxc exec go-id-shard-0 -- bash -c 'cat > /etc/consul.d/client.hcl <<EOF
datacenter = "dc1"
data_dir = "/var/lib/consul"
server = false
bind_addr = "10.200.100.50"
retry_join = ["192.168.1.100", "192.168.1.101", "192.168.1.102"]
EOF'
```

3. Configure Go-ID to use local Consul:
```bash
lxc config set go-id-shard-0 environment.CONSUL_URL "http://127.0.0.1:8500"
```

## Recommended Configuration (Simplest)

**For 4-5 servers, use Option A (Consul on host):**

```
Server 1 (192.168.1.100): Consul Server + Go-ID shard-0 in LXC
Server 2 (192.168.1.101): Consul Server + Go-ID shard-1 in LXC
Server 3 (192.168.1.102): Consul Server + Go-ID shard-2 in LXC
Server 4 (192.168.1.103): Consul Client + Other services in LXC
Server 5 (192.168.1.104): Consul Client + Other services in LXC
```

## Configuration Script for Go-ID Integration

```bash
#!/bin/bash
# deploy-goid-with-consul.sh

# Servers with Consul
SERVERS=(
  "server-1:192.168.1.100:10.200.100.50:go-id-shard-0:0"
  "server-2:192.168.1.101:10.200.100.51:go-id-shard-1:1"
  "server-3:192.168.1.102:10.200.100.52:go-id-shard-2:2"
)

for server_info in "${SERVERS[@]}"; do
  IFS=':' read -r name host_ip container_ip container_name shard_id <<< "$server_info"

  echo "Configuring ${container_name} on ${name}..."

  ssh root@${host_ip} <<EOF
    # Set Consul URL (host Consul)
    lxc config set ${container_name} environment.CONSUL_URL "http://${host_ip}:8500"
    lxc config set ${container_name} environment.SERVICE_NAME "go-id"

    # Set shard configuration
    lxc config set ${container_name} environment.SHARD_ID "${shard_id}"
    lxc config set ${container_name} environment.TOTAL_SHARDS "3"
    lxc config set ${container_name} environment.CONTAINER_IP "${container_ip}"
    lxc config set ${container_name} environment.SERVICE_PORT "7010"

    # Restart container
    lxc restart ${container_name}

    echo "✓ ${container_name} configured"
EOF
done

echo ""
echo "Waiting for services to register..."
sleep 10

echo ""
echo "Checking Consul service registration:"
curl -s http://192.168.1.100:8500/v1/catalog/service/go-id | jq '.[].ServiceID'
```

## Verify Setup

### Check Consul Cluster Health
```bash
consul members
consul operator raft list-peers
```

### Check Go-ID Service Registration
```bash
# List all go-id services
curl http://192.168.1.100:8500/v1/catalog/service/go-id | jq

# Check health
curl http://192.168.1.100:8500/v1/health/service/go-id?passing | jq
```

Expected output:
```json
[
  {
    "ServiceID": "go-id-shard-0",
    "ServiceName": "go-id",
    "ServiceAddress": "10.200.100.50",
    "ServicePort": 7010,
    "ServiceTags": ["shard-0", "v1"],
    "ServiceMeta": {
      "shard_id": "0",
      "total_shards": "3"
    }
  },
  {
    "ServiceID": "go-id-shard-1",
    ...
  },
  {
    "ServiceID": "go-id-shard-2",
    ...
  }
]
```

### Access Consul UI
```
http://192.168.1.100:8500/ui
```

## Service Discovery from Applications

### Query Healthy Go-ID Shards
```bash
# Get all healthy shards
curl http://192.168.1.100:8500/v1/health/service/go-id?passing

# Get specific shard
curl http://192.168.1.100:8500/v1/health/service/go-id?passing&tag=shard-0
```

### DNS-based Discovery
```bash
# Consul provides DNS interface on port 8600
dig @192.168.1.100 -p 8600 go-id.service.consul

# Get specific shard
dig @192.168.1.100 -p 8600 shard-0.go-id.service.consul
```

## Resource Usage

**Per Consul Server:**
- RAM: ~100-150MB
- CPU: <5% (idle), 10-20% (under load)
- Disk: ~50MB (data)
- Network: Minimal (gossip protocol)

**Per Consul Client:**
- RAM: ~50-80MB
- CPU: <2%
- Disk: ~20MB

**Total overhead for 3 servers + 2 clients:**
- RAM: ~460MB total across all servers
- Disk: ~190MB total
- Negligible CPU impact

## Troubleshooting

### Consul Cluster Not Forming
```bash
# Check logs
sudo journalctl -u consul -f

# Common issues:
# 1. Firewall blocking ports 8300-8302, 8500, 8600
# 2. Wrong bind_addr
# 3. retry_join pointing to wrong IPs
```

### Service Not Registering
```bash
# Check container can reach Consul
lxc exec go-id-shard-0 -- curl http://192.168.1.100:8500/v1/status/leader

# Check environment variables
lxc config show go-id-shard-0 | grep environment

# Check Go-ID logs
lxc exec go-id-shard-0 -- journalctl -u go-id -n 50
```

### Container Can't Reach Host Consul
```bash
# Option 1: Use host IP (not 127.0.0.1)
# Option 2: Use host.lxd special hostname
lxc config set go-id-shard-0 environment.CONSUL_URL "http://host.lxd:8500"

# Option 3: Add static route
lxc exec go-id-shard-0 -- ip route add 192.168.1.0/24 via <gateway>
```

## Production Checklist

- [ ] 3 Consul servers running and in sync
- [ ] All servers have retry_join configured
- [ ] Firewall allows ports 8300-8302, 8500, 8600
- [ ] Consul UI accessible
- [ ] All Go-ID shards registered in Consul
- [ ] Health checks passing for all services
- [ ] DNS queries working
- [ ] Automatic failover tested (stop 1 Consul server)
- [ ] Consul backups configured (snapshot agent)

## Backup and Recovery

### Create Snapshot
```bash
consul snapshot save backup.snap
```

### Restore Snapshot
```bash
consul snapshot restore backup.snap
```

### Automated Backups
```bash
# Add to crontab on one of the servers
0 2 * * * /usr/local/bin/consul snapshot save /backups/consul-$(date +\%Y\%m\%d).snap
```

## Summary

For a **4-5 server cloud running Go-ID**:

✅ **Simplest**: 1 Consul server + 4 clients
✅ **Recommended**: 3 Consul servers + 2 clients (HA)
✅ **Resource overhead**: ~100MB RAM per server
✅ **Setup time**: ~15 minutes for full cluster
✅ **Maintenance**: Minimal (set and forget)

This setup provides:
- Automatic service discovery for all shards
- Health checking with automatic failover
- Load balancing via Consul DNS
- Web UI for monitoring
- Survives 1 server failure (with 3-node cluster)
