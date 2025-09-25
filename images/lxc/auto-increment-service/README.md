# Sequence Service LXC Container

A lightweight, high-performance sequence generation service running in LXC. Provides REST API for generating sequential IDs (int/long) and random UUIDs (8/12/16/22 characters).

## Features

- **Multiple ID Types**:
  - `int`: Sequential 32-bit integers (1 to 2,147,483,647)
  - `long`: Sequential 64-bit integers (1 to 9,223,372,036,854,775,807)
  - `uuid8`: Random 8-character alphanumeric strings
  - `uuid12`: Random 12-character alphanumeric strings
  - `uuid16`: Random 16-character alphanumeric strings
  - `uuid22`: Random 22-character alphanumeric strings

- **Batch Operations**: Get ranges of sequential IDs or arrays of UUIDs
- **Persistent State**: Survives container restarts
- **Multi-Entity Support**: Track different sequences for different entities
- **Type Safety**: Prevents type mismatches for registered entities
- **CLI Management**: Reset entities, change types, set counter values
- **Bridge Networking**: Static IP in 10.10.199.0/24 subnet
- **Portable**: Export/import containers with state intact

## Quick Start

```bash
# Build the base image (one-time)
sudo ./buildAutoIncrement.sh

# Start default container
sudo ./startDefault.sh
```

## API Endpoints

### Get Next ID
```bash
GET /api/next-id/<entity>?dataType=<type>

# Examples:
curl 'http://10.10.199.50:7001/api/next-id/users?dataType=int'
curl 'http://10.10.199.50:7001/api/next-id/sessions?dataType=uuid16'
```

### Get Batch
```bash
GET /api/next-batch/<entity>?dataType=<type>&batchSize=<n>

# Sequential types return range:
curl 'http://10.10.199.50:7001/api/next-batch/orders?dataType=long&batchSize=100'
# Response: {"startValue": "1", "endValue": "100"}

# UUID types return array:
curl 'http://10.10.199.50:7001/api/next-batch/tokens?dataType=uuid8&batchSize=5'
# Response: {"values": ["A7k9Bx2m", "Qp4Ld8Zn", ...]}
```

### Get Status
```bash
GET /api/status/<entity>
GET /api/list  # List all entities
GET /api/types  # Show available types
```

## CLI Management

The `sequence-reset` command provides powerful entity management:

```bash
# Show help
lxc exec <container> -- sequence-reset -h

# Reset all entities (delete everything)
lxc exec <container> -- sequence-reset

# Reset specific entity
lxc exec <container> -- sequence-reset -e users

# Set counter value (int/long types only)
lxc exec <container> -- sequence-reset -e users -v 1000

# Change entity type
lxc exec <container> -- sequence-reset -e tokens -t uuid16

# Create new entity with specific type and value
lxc exec <container> -- sequence-reset -e invoices -t long -v 2000

# Force mode (skip confirmation)
lxc exec <container> -- sequence-reset -f -e users
```

## Custom Deployment

### Create Configuration File

```bash
cat > my-sequence.conf << 'EOF'
#!/bin/bash

# Container settings
CONTAINER_NAME="sequence-prod"
BASE_IMAGE="sequence-service-base"

# Network (mandatory /24 notation)
CONTAINER_IP="10.10.199.52/24"
GATEWAY_IP="10.10.199.1/24"
BRIDGE_NAME="lxdbr0"
DNS_SERVERS="8.8.8.8 8.8.4.4"

# Service
SERVICE_PORT="7001"

# Resource limits (optional)
MEMORY_LIMIT="256MB"
CPU_LIMIT="1"

# Bind mounts (optional)
BIND_MOUNTS=(
    "~/sequence-data:/var/lib/sequence-service"
)
EOF
```

### Launch Container

```bash
sudo ./launchSequenceService.sh my-sequence.conf
```

## State Management

### Backup with State
```bash
lxc export <container-name> sequence-backup.tar.gz
```

### Restore on Another Machine
```bash
lxc import sequence-backup.tar.gz
```

### External State Directory
Mount a host directory for persistent state across container rebuilds:

```bash
BIND_MOUNTS=(
    "/data/sequence-state:/var/lib/sequence-service"
)
```

## Architecture

- **Service**: Node.js Express server on port 7001
- **State Storage**: JSON file at `/var/lib/sequence-service/state.json`
- **Process Manager**: systemd with automatic restart
- **User**: Runs as nobody:nogroup for security
- **Logging**: `/var/log/sequence-service.log`

## Network Requirements

- Bridge: `lxdbr0` must exist
- Subnet: `10.10.199.0/24` (mandatory)
- IP Forwarding: Must be enabled
- NAT: Required for internet access during build

```bash
# Enable IP forwarding
sudo sysctl -w net.ipv4.ip_forward=1

# Setup NAT (if not already configured)
sudo iptables -t nat -A POSTROUTING -s 10.10.199.0/24 -j MASQUERADE
```

## Type Transitions

When changing entity types via CLI:

- **Numeric to UUID**: Counter is reset (UUIDs don't have counters)
- **UUID to Numeric**: Counter starts at 1 (or value specified with -v)
- **Between UUID types**: No state change (each request generates new random string)
- **Between Numeric types**: Counter value preserved if within range

## Performance

- Handles thousands of requests per second
- Minimal memory footprint (~50MB)
- Sub-millisecond response times
- File-based persistence with write batching

## Troubleshooting

### Check Service Status
```bash
lxc exec <container> -- systemctl status sequence-service
```

### View Logs
```bash
lxc exec <container> -- tail -f /var/log/sequence-service.log
```

### Test Connectivity
```bash
# From host
curl http://$(lxc list <container> -c 4 --format csv | grep -oE '[0-9]+\.[0-9]+\.[0-9]+\.[0-9]+'):7001/health
```

### Reset All Data
```bash
lxc exec <container> -- sequence-reset -f
```

## Version History

- **v2.0.0**: Added UUID support, batch API, CLI entity management
- **v1.1.0**: Added batch API for sequential IDs
- **v1.0.0**: Initial release with int/long support