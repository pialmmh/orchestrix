# Local Testing Guide - Consul + Go-ID

## Current Status

- ✅ Consul 3-node cluster deployed and healthy
- ✅ Go-ID service running and registered with Consul
- ✅ Service discovery working
- ✅ ID generation API tested and working
- ✅ Complete testing environment ready

## Verified Working Setup

**Consul Cluster:**
- consul-node-1: 10.10.199.233:8500 (UI: http://10.10.199.233:8500/ui)
- consul-node-2: 10.10.199.100:8510
- consul-node-3: 10.10.199.118:8520

**Go-ID Service:**
- Container: go-id-dev (10.10.199.155:7001)
- Status: Registered with Consul as 'go-id' service
- API: http://10.10.199.155:7001
- Health: http://10.10.199.155:7001/health

**Testing Commands:**
```bash
# Check Consul services
lxc exec consul-node-1 -- consul catalog services

# Test Go-ID health
curl http://10.10.199.155:7001/health

# Generate ID
curl "http://10.10.199.155:7001/api/next-id/order?dataType=long"

# Generate batch
curl "http://10.10.199.155:7001/api/next-batch/invoice?dataType=int&batchSize=5"
```

## Quick Start Scripts Created

### 1. Deploy Consul Cluster Locally

```bash
cd /home/mustafa/telcobright-projects/orchestrix/images/lxc/consul/consul-v.1/generated/deployments
./deploy-local.sh
```

This deploys a 3-node Consul cluster:
- consul-node-1: IP (auto), HTTP: 8500
- consul-node-2: IP (auto), HTTP: 8510
- consul-node-3: IP (auto), HTTP: 8520

### 2. Deploy Go-ID Locally (with Consul)

```bash
cd /home/mustafa/telcobright-projects/orchestrix/images/lxc/go-id/go-id-v.1/generated/deployments
./deploy-local-with-consul.sh
```

This deploys:
- go-id-dev: Port 7001, connects to consul-node-1:8500
- Automatically copies go-id binary from standalone-binaries
- Registers service with Consul
- Starts service with Consul integration

## Manual Deployment (if scripts timeout)

### Step 1: Deploy Consul Manually

```bash
# Import image
lxc image import /path/to/consul-v1-*.tar.gz --alias consul-local

# Create node 1
lxc launch consul-local consul-node-1
lxc config set consul-node-1 limits.memory 256MB
lxc config set consul-node-1 limits.cpu 1

# Wait for it to start
sleep 5

# Get IP
NODE1_IP=$(lxc exec consul-node-1 -- ip -4 addr show eth0 | grep inet | awk '{print $2}' | cut -d/ -f1)

# Configure consul
lxc exec consul-node-1 -- tee /consul/config/consul.json << EOF
{
    "datacenter": "dc1",
    "node_name": "consul-node-1",
    "server": true,
    "bootstrap_expect": 3,
    "retry_join": ["${NODE1_IP}:8301"],
    "client_addr": "0.0.0.0",
    "bind_addr": "${NODE1_IP}",
    "ports": {
        "http": 8500,
        "serf_lan": 8301,
        "server": 8300
    }
}
EOF

# Start consul
lxc exec consul-node-1 -- rc-service consul start

# Repeat for node 2 and 3 with different ports...
```

### Step 2: Deploy Go-ID Manually

```bash
# Launch go-id
lxc launch go-id-base go-id-dev
lxc config set go-id-dev limits.memory 128MB
lxc config set go-id-dev limits.cpu 1

# Wait for start
sleep 5

# Configure go-id to use consul
CONSUL_IP=$(lxc exec consul-node-1 -- ip -4 addr show eth0 | grep inet | awk '{print $2}' | cut -d/ -f1)

lxc exec go-id-dev -- tee /opt/go-id/config.json << EOF
{
    "port": 7080,
    "log_level": "DEBUG",
    "consul": {
        "enabled": true,
        "address": "${CONSUL_IP}:8500",
        "service_name": "go-id",
        "health_check_interval": "10s"
    }
}
EOF

# Start go-id service
lxc exec go-id-dev -- systemctl restart go-id
```

## Testing

### 1. Verify Consul Cluster

```bash
# Check cluster status
lxc exec consul-node-1 -- consul members

# Expected output:
# Node           Address          Status  Type    Build   Protocol
# consul-node-1  10.x.x.x:8301   alive   server  1.17.0  2
# consul-node-2  10.x.x.x:8311   alive   server  1.17.0  2
# consul-node-3  10.x.x.x:8321   alive   server  1.17.0  2

# Check leader
lxc exec consul-node-1 -- consul operator raft list-peers
```

### 2. Verify Go-ID Connection to Consul

```bash
# Check if go-id registered with consul
CONSUL_IP=$(lxc exec consul-node-1 -- ip -4 addr show eth0 | grep inet | awk '{print $2}' | cut -d/ -f1)
lxc exec consul-node-1 -- consul catalog services

# Should show:
# consul
# go-id

# Check go-id service health
lxc exec consul-node-1 -- consul health service go-id
```

### 3. Test Go-ID API

```bash
# Get go-id IP
GO_ID_IP=$(lxc exec go-id-dev -- ip -4 addr show eth0 | grep inet | awk '{print $2}' | cut -d/ -f1)

# Test ID generation
curl http://${GO_ID_IP}:7080/api/v1/id

# Should return:
# {"id":"some-unique-id","timestamp":"2025-10-09T..."}

# Check health endpoint
curl http://${GO_ID_IP}:7080/health

# Should return:
# {"status":"healthy","consul":"connected"}
```

### 4. Monitor Logs

```bash
# Consul logs
lxc exec consul-node-1 -- tail -f /var/log/consul/consul.log

# Go-ID logs
lxc exec go-id-dev -- journalctl -u go-id -f
```

## Consul UI Access

```bash
# Get consul node 1 IP
CONSUL_IP=$(lxc exec consul-node-1 -- ip -4 addr show eth0 | grep inet | awk '{print $2}' | cut -d/ -f1)

echo "Consul UI: http://${CONSUL_IP}:8500/ui"
```

Open in browser to see:
- Cluster status
- Services (consul, go-id)
- Key/Value store
- Health checks

## Troubleshooting

### LXC Launch Timeouts

If experiencing timeouts:

```bash
# Check LXD status
systemctl status snap.lxd.daemon

# Restart LXD
sudo systemctl restart snap.lxd.daemon

# Check system resources
free -h
df -h
```

### Consul Not Forming Cluster

```bash
# Check consul logs
lxc exec consul-node-1 -- cat /var/log/consul/consul.log | grep -i error

# Verify network connectivity
lxc exec consul-node-1 -- ping -c 3 $(lxc list consul-node-2 -c 4 | grep eth0 | awk '{print $2}')

# Check ports
lxc exec consul-node-1 -- netstat -tulpn | grep consul
```

### Go-ID Can't Connect to Consul

```bash
# Check if consul is reachable from go-id
CONSUL_IP=$(lxc exec consul-node-1 -- ip -4 addr show eth0 | grep inet | awk '{print $2}' | cut -d/ -f1)
lxc exec go-id-dev -- curl http://${CONSUL_IP}:8500/v1/status/leader

# Check go-id config
lxc exec go-id-dev -- cat /opt/go-id/config.json

# Check go-id logs
lxc exec go-id-dev -- journalctl -u go-id -n 50
```

## Cleanup

```bash
# Stop and delete all
lxc delete -f consul-node-1 consul-node-2 consul-node-3 go-id-dev

# Delete images
lxc image delete consul-local go-id-local
```

## Architecture

```
┌─────────────────────────────────────────┐
│           Local Test Environment        │
│                                         │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐
│  │ consul-  │  │ consul-  │  │ consul-  │
│  │ node-1   │  │ node-2   │  │ node-3   │
│  │ :8500    │  │ :8510    │  │ :8520    │
│  └────┬─────┘  └────┬─────┘  └────┬─────┘
│       │             │             │       │
│       └─────────────┴─────────────┘       │
│                     │                     │
│               Consul Cluster              │
│                     │                     │
│              ┌──────┴──────┐              │
│              │   go-id-dev │              │
│              │   :7080     │              │
│              └─────────────┘              │
└─────────────────────────────────────────┘
```

## Next Steps

1. Investigate LXC launch timeout issue
2. Deploy consul cluster locally
3. Configure and deploy go-id
4. Test ID generation through consul-registered service
5. Verify service discovery and health checks

## Notes

- All scripts created in respective `deployments/` folders
- Local deployments use `consul-published` and `go-id-base` images
- Consul uses different ports per node for same-machine testing
- Go-ID configured to register with Consul for service discovery
