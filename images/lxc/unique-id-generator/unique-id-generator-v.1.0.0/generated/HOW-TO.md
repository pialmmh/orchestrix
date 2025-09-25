# Unique ID Generator v1.0.0 - How To Guide

## Quick Start

### 1. Build the Container Image (One Time)
```bash
cd /home/mustafa/telcobright-projects/orchestrix/images/lxc/unique-id-generator
./build-minimal.sh
```
This creates the `unique-id-generator-base` image.

### 2. Launch Container with Default Settings
```bash
cd unique-id-generator-v.1.0.0/generated/
./launch_default.sh
```

### 3. Test the Service
```bash
# Get container IP
lxc list uid-gen-v1

# Generate a Snowflake ID
curl 'http://<IP>:8080/api/next-id/user?dataType=snowflake'
```

## Configuration

### Single Instance Setup
Edit `sample.conf`:
```bash
CONTAINER_NAME="uid-production"
SHARD_ID="1"
TOTAL_SHARDS="1"
```

Launch:
```bash
./launch.sh sample.conf
```

### Multi-Shard Setup (Distributed)

Create config files for each shard:

**shard1.conf:**
```bash
CONTAINER_NAME="uid-shard-1"
SHARD_ID="1"
TOTAL_SHARDS="3"
HOST_DATA_DIR="/opt/uid-data/shard-1"
```

**shard2.conf:**
```bash
CONTAINER_NAME="uid-shard-2"
SHARD_ID="2"
TOTAL_SHARDS="3"
HOST_DATA_DIR="/opt/uid-data/shard-2"
```

**shard3.conf:**
```bash
CONTAINER_NAME="uid-shard-3"
SHARD_ID="3"
TOTAL_SHARDS="3"
HOST_DATA_DIR="/opt/uid-data/shard-3"
```

Launch all shards:
```bash
./launch.sh shard1.conf
./launch.sh shard2.conf
./launch.sh shard3.conf
```

## ID Generation Examples

### Generate Single ID
```bash
# Snowflake ID (recommended - time-ordered, globally unique)
curl 'http://10.10.199.51:8080/api/next-id/user?dataType=snowflake'
# Response: {"entityName":"user","dataType":"snowflake","value":"229599377285447680","shard":1}

# Sequential integer
curl 'http://10.10.199.51:8080/api/next-id/order?dataType=int'
# Response: {"entityName":"order","dataType":"int","value":1,"shard":1}
```

### Generate Batch of IDs
```bash
# Batch of 100 Snowflake IDs
curl 'http://10.10.199.51:8080/api/next-batch/event?dataType=snowflake&batchSize=100'

# Batch of 50 sequential integers
curl 'http://10.10.199.51:8080/api/next-batch/invoice?dataType=int&batchSize=50'
```

### Parse Snowflake ID
```bash
curl 'http://10.10.199.51:8080/api/parse-snowflake/229599377285447680'
# Response shows timestamp, date, shardId, sequence
```

## CLI Management

### Inside Container
```bash
# Access container shell
lxc exec uid-gen-v1 -- bash

# List all entities
uid-cli list

# Initialize entity with starting value
uid-cli init product int 1000

# Check entity status
uid-cli status product

# Reset counter
uid-cli reset product 5000

# Delete entity
uid-cli delete session
```

### From Host
```bash
# Run CLI commands directly
lxc exec uid-gen-v1 -- uid-cli list
lxc exec uid-gen-v1 -- uid-cli init customer snowflake
```

## Monitoring

### Check Service Health
```bash
curl http://10.10.199.51:8080/health
```

### View Logs
```bash
# Service logs
lxc exec uid-gen-v1 -- journalctl -u unique-id-generator -f

# All system logs
lxc exec uid-gen-v1 -- tail -f /var/log/unique-id-generator/*.log
```

### Service Status
```bash
lxc exec uid-gen-v1 -- systemctl status unique-id-generator
```

## Data Persistence

### With Host Mount
Configure in your .conf file:
```bash
HOST_DATA_DIR="/opt/uid-generator/data"
HOST_LOG_DIR="/opt/uid-generator/logs"
```

### Backup State
```bash
# Copy state file from container
lxc file pull uid-gen-v1/var/lib/unique-id-generator/state.json ./backup-state.json
```

### Restore State
```bash
# Push state file to container
lxc file push ./backup-state.json uid-gen-v1/var/lib/unique-id-generator/state.json
lxc exec uid-gen-v1 -- chown nobody:nogroup /var/lib/unique-id-generator/state.json
lxc exec uid-gen-v1 -- systemctl restart unique-id-generator
```

## Container Management

### Stop Container
```bash
lxc stop uid-gen-v1
```

### Start Container
```bash
lxc start uid-gen-v1
```

### Delete Container
```bash
lxc delete --force uid-gen-v1
```

### List All Containers
```bash
lxc list | grep uid
```

## Troubleshooting

### Service Won't Start
```bash
# Check logs
lxc exec uid-gen-v1 -- journalctl -u unique-id-generator -n 50

# Check permissions
lxc exec uid-gen-v1 -- ls -la /var/lib/unique-id-generator
```

### Permission Errors
```bash
# Fix data directory permissions
lxc exec uid-gen-v1 -- chown -R nobody:nogroup /var/lib/unique-id-generator
lxc exec uid-gen-v1 -- systemctl restart unique-id-generator
```

### Network Issues
```bash
# Check container IP
lxc list uid-gen-v1

# Test from inside container
lxc exec uid-gen-v1 -- curl localhost:8080/health
```

## Performance Tips

1. **Use Snowflake IDs** for best performance (4096 IDs/ms per shard)
2. **Use batch generation** for bulk operations
3. **Deploy multiple shards** for high-throughput scenarios
4. **Mount data directory** on SSD for better I/O performance
5. **Monitor state file size** - it grows with number of entities

## ID Types Comparison

| Type | Format | Example | Use Case |
|------|--------|---------|----------|
| snowflake | 64-bit number | 229599377285447680 | High-performance, time-ordered |
| int | 32-bit sequential | 1, 2, 3... | Simple counters |
| long | 64-bit sequential | 1, 2, 3... | Large counters |
| uuid8 | 8 chars | aB3xY9kL | Short random IDs |
| uuid16 | 16 chars | aB3xY9kLmN2pQ4rS | Medium random IDs |

## Sharding Patterns

### Sequential IDs with 3 Shards
- Shard 1: 1, 4, 7, 10, 13...
- Shard 2: 2, 5, 8, 11, 14...
- Shard 3: 3, 6, 9, 12, 15...

### Snowflake IDs
Each shard generates completely independent IDs with shard ID embedded in the bit structure.