# Go-ID Deployment Guide

## Overview

The Go-ID container supports both single-server and multi-server deployments through the test runner automation system.

## Deployment Modes

### Single Target Deployment
Deploy to a single server for testing or low-volume production use.

**Use Case**: Development, testing, low-volume production

### Multi-Target Deployment
Deploy to multiple servers in parallel for high-availability sharded deployments.

**Use Case**: Production multi-shard ID generation, high availability

## Quick Start

### Single Server Deployment

1. Edit `test/test-runner.conf`:
```bash
# Single target configuration
SERVER_IP=192.168.1.100
SSH_USER=root
SSH_PASSWORD=your_password
CONTAINER_NAME=go-id-test
CONTAINER_IP=10.200.100.50
```

2. Run deployment:
```bash
cd /path/to/generated/test
./test-runner.sh
```

### Multi-Server Deployment (3 Shards)

1. Edit `test/test-runner.conf` or create a custom config:
```bash
# Enable parallel execution
EXECUTION_MODE=PARALLEL

# Shard 0
TARGET_1_NAME=shard-0
TARGET_1_SERVER_IP=192.168.1.100
TARGET_1_SSH_USER=root
TARGET_1_SSH_PASSWORD=password
TARGET_1_CONTAINER_NAME=go-id-shard-0
TARGET_1_CONTAINER_IP=10.200.100.50

# Shard 1
TARGET_2_NAME=shard-1
TARGET_2_SERVER_IP=192.168.1.101
TARGET_2_SSH_USER=root
TARGET_2_SSH_PASSWORD=password
TARGET_2_CONTAINER_NAME=go-id-shard-1
TARGET_2_CONTAINER_IP=10.200.100.51

# Shard 2
TARGET_3_NAME=shard-2
TARGET_3_SERVER_IP=192.168.1.102
TARGET_3_SSH_USER=root
TARGET_3_SSH_PASSWORD=password
TARGET_3_CONTAINER_NAME=go-id-shard-2
TARGET_3_CONTAINER_IP=10.200.100.52
```

2. Run deployment:
```bash
cd /path/to/generated/test
./test-runner.sh
```

**Output:**
```
Deployment Mode: Multi-Target (3 servers)
Execution Mode:  PARALLEL
Artifact:        go-id-v1-1759680570.tar.gz

Targets:
  [1] shard-0 - 192.168.1.100 (go-id-shard-0)
  [2] shard-1 - 192.168.1.101 (go-id-shard-1)
  [3] shard-2 - 192.168.1.102 (go-id-shard-2)

Proceed with deployment? (y/N):
```

## Execution Modes

### PARALLEL (Recommended for Production)
- Deploys to all servers simultaneously
- Faster deployment (time = ~single deployment time)
- Uses thread pool for concurrent operations
- Best for: Production multi-shard deployments

### SEQUENTIAL (Recommended for Testing)
- Deploys to servers one by one
- Easier to debug
- Clear error isolation
- Best for: Initial deployment, troubleshooting

## Post-Deployment Configuration

After deployment, configure shard-specific environment variables on each container:

### Shard 0
```bash
ssh root@192.168.1.100
lxc config set go-id-shard-0 environment.SHARD_ID 0
lxc config set go-id-shard-0 environment.TOTAL_SHARDS 3
lxc config set go-id-shard-0 environment.SERVICE_PORT 7010
lxc config set go-id-shard-0 environment.CONTAINER_IP 10.200.100.50
lxc restart go-id-shard-0
```

### Shard 1
```bash
ssh root@192.168.1.101
lxc config set go-id-shard-1 environment.SHARD_ID 1
lxc config set go-id-shard-1 environment.TOTAL_SHARDS 3
lxc config set go-id-shard-1 environment.SERVICE_PORT 7010
lxc config set go-id-shard-1 environment.CONTAINER_IP 10.200.100.51
lxc restart go-id-shard-1
```

### Shard 2
```bash
ssh root@192.168.1.102
lxc config set go-id-shard-2 environment.SHARD_ID 2
lxc config set go-id-shard-2 environment.TOTAL_SHARDS 3
lxc config set go-id-shard-2 environment.SERVICE_PORT 7010
lxc config set go-id-shard-2 environment.CONTAINER_IP 10.200.100.52
lxc restart go-id-shard-2
```

## Consul Service Discovery (Optional)

Enable automatic service discovery by setting CONSUL_URL on each container:

```bash
# On each server
lxc config set <container-name> environment.CONSUL_URL http://consul-server:8500
lxc config set <container-name> environment.SERVICE_NAME go-id
lxc restart <container-name>
```

The service will auto-register with:
- Service ID: `go-id-shard-{id}` (e.g., go-id-shard-0)
- Tags: `["shard-{id}", "v1"]`
- Metadata: `{"shard_id": "0", "total_shards": "3"}`
- Health check: HTTP /health endpoint

## Load Balancing

### Option 1: Round-Robin (Application Level)
Configure your application to maintain a list of all shard endpoints:
```
http://192.168.1.100:7010
http://192.168.1.101:7010
http://192.168.1.102:7010
```

### Option 2: HAProxy
```haproxy
frontend go-id-frontend
    bind *:7010
    default_backend go-id-shards

backend go-id-shards
    balance roundrobin
    server shard-0 192.168.1.100:7010 check
    server shard-1 192.168.1.101:7010 check
    server shard-2 192.168.1.102:7010 check
```

### Option 3: Consul (Recommended)
Let Consul handle service discovery and load balancing:
```bash
# Query all healthy shards
curl http://consul:8500/v1/health/service/go-id?passing
```

## Verification

### Check Container Status on Each Server
```bash
# Server 1
ssh root@192.168.1.100 'lxc list go-id-shard-0'

# Server 2
ssh root@192.168.1.101 'lxc list go-id-shard-1'

# Server 3
ssh root@192.168.1.102 'lxc list go-id-shard-2'
```

### Test Each Shard
```bash
# Test shard 0
curl http://192.168.1.100:7010/shard-info

# Test shard 1
curl http://192.168.1.101:7010/shard-info

# Test shard 2
curl http://192.168.1.102:7010/shard-info
```

Expected response:
```json
{
  "shard_id": 0,
  "total_shards": 3,
  "service": "go-id",
  "version": "v1"
}
```

### Generate Test IDs
```bash
# From each shard
curl http://192.168.1.100:7010/api/next-id/user
curl http://192.168.1.101:7010/api/next-id/user
curl http://192.168.1.102:7010/api/next-id/user
```

All IDs should be unique across all shards due to embedded shard_id in Sonyflake.

## Troubleshooting

### Deployment Fails on One Server
With PARALLEL mode, other deployments continue. Check logs:
```bash
# The deployment summary shows which servers failed
# Re-run with SEQUENTIAL mode for better debugging
```

### Container Won't Start
```bash
ssh root@<server-ip>
lxc info <container-name>
lxc exec <container-name> -- journalctl -u go-id -n 50
```

### Service Not Responding
```bash
# Check if service is running
ssh root@<server-ip> 'lxc exec <container-name> -- systemctl status go-id'

# Check if port is listening
ssh root@<server-ip> 'lxc exec <container-name> -- netstat -tuln | grep 7010'
```

### Wrong Shard ID
If all shards generate same IDs, they have the same shard_id:
```bash
# Check environment variables
lxc config show <container-name> | grep environment

# Fix and restart
lxc config set <container-name> environment.SHARD_ID <correct-id>
lxc restart <container-name>
```

## Examples

See `examples/` directory for:
- `test-runner-3-shards.conf` - Complete 3-shard deployment example
- Multi-environment deployment configs

## Architecture

```
                    ┌──────────────┐
                    │ Load Balancer│
                    │  (HAProxy/   │
                    │   Consul)    │
                    └──────┬───────┘
                           │
        ┌──────────────────┼──────────────────┐
        │                  │                  │
        ▼                  ▼                  ▼
   ┌─────────┐       ┌─────────┐       ┌─────────┐
   │ Shard 0 │       │ Shard 1 │       │ Shard 2 │
   │ ID: 0   │       │ ID: 1   │       │ ID: 2   │
   │192.168  │       │192.168  │       │192.168  │
   │.1.100   │       │.1.101   │       │.1.102   │
   └─────────┘       └─────────┘       └─────────┘
```

## Best Practices

1. **Use PARALLEL mode** for production deployments (saves time)
2. **Use SEQUENTIAL mode** for initial deployment (easier debugging)
3. **Always set SHARD_ID** correctly on each container
4. **Enable Consul** for automatic service discovery
5. **Monitor all shards** - one failed shard reduces capacity by 1/N
6. **Test all shards** before going live
7. **Keep shard count** as a power of 2 for better distribution (2, 4, 8, 16...)
