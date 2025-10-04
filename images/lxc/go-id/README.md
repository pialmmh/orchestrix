# Go ID Generator Container

High-performance unique ID generator service built with Go, providing distributed ID generation using Sonyflake (enhanced Snowflake algorithm).

## Features

- **Go-based REST API** for unique ID generation
- **Sonyflake library** (Sony's production-grade Snowflake implementation)
- **Clock rollback protection** with configurable time source
- **Small footprint**: 15-30MB (vs 150-300MB for Node.js version)
- **High concurrency**: Goroutines handle 10K+ concurrent requests
- **Low resource usage**: 512MB RAM vs 1-2GB for Node.js
- **BTRFS storage** with quota management and snapshots
- **Systemd service** for automatic startup and restart
- **Health check endpoint** for monitoring
- **Config-compatible** with Node.js version (uses same SHARD_ID format)

## Architecture

### ID Generation
- Uses **Sonyflake** library from Sony (github.com/sony/sonyflake)
- 64-bit IDs with enhanced layout vs Twitter Snowflake
- **16-bit machine ID** (vs 10-bit in Twitter Snowflake) - supports 65,536 nodes
- Clock rollback detection and handling
- Configurable time source (supports NTP)
- Time-ordered, sortable IDs

### API Endpoints
- `GET /generate` - Generate a unique ID
- `GET /health` - Health check endpoint

## Quick Start

```bash
# Build container with defaults
./startDefault.sh

# Or build with custom config
./build/build.sh /path/to/custom.conf
```

## Build Configuration

Edit `build/build.conf`:

```bash
# Required settings
STORAGE_LOCATION_ID="btrfs_local_main"  # Your BTRFS location
STORAGE_QUOTA_SIZE="5G"                  # Disk quota
SERVICE_PORT="7001"                      # API port
GO_VERSION="1.21"                        # Go version
```

## Runtime Configuration (Distributed Setup)

For distributed ID generation, set `SHARD_ID` environment variable at container launch:

```bash
# Single instance (default)
lxc exec go-id-v1 -- systemctl start go-id

# Distributed setup - Shard 1
lxc config set go-id-shard1 environment.SHARD_ID=1
lxc exec go-id-shard1 -- systemctl start go-id

# Distributed setup - Shard 2
lxc config set go-id-shard2 environment.SHARD_ID=2
lxc exec go-id-shard2 -- systemctl start go-id
```

**Note**: This uses the same `SHARD_ID` configuration as the Node.js version for compatibility.

## Prerequisites

The build system automatically checks:
- ✓ BTRFS tools installed (`btrfs-progs`)
- ✓ BTRFS kernel module loaded
- ✓ LXC/LXD installed and running
- ✓ Network bridge configured (`lxcbr0` or `lxdbr0`)
- ✓ Sufficient disk space (warns if < 10GB)

If checks fail, the build stops with clear error messages.

## Usage

### Generate ID
```bash
curl http://localhost:7001/generate
```

Response:
```json
{
  "id": 1234567890123456,
  "timestamp": 1696789012
}
```

### Health Check
```bash
curl http://localhost:7001/health
```

Response: `OK`

## Resource Usage

| Metric | Go Version | Node.js Version | Improvement |
|--------|-----------|-----------------|-------------|
| Image Size | 15-30MB | 150-300MB | ~85% smaller |
| Memory | 512MB | 1-2GB | ~75% less |
| Disk Quota | 5GB | 10GB | 50% less |
| Startup Time | <1s | 2-5s | 5x faster |
| Concurrency | 10K+ | Limited | Much better |
| Library | Sonyflake | Custom | Production-grade |

## Container Management

```bash
# Start container
lxc start go-id-v1

# Stop container
lxc stop go-id-v1

# View logs
lxc exec go-id-v1 -- tail -f /var/log/go-id/service.log

# Check service status
lxc exec go-id-v1 -- systemctl status go-id
```

## Version History

- **v1.0** - Initial Go-based implementation
  - Snowflake ID algorithm
  - REST API endpoints
  - Systemd integration
  - BTRFS storage

## Migration from Node.js

The Go version with Sonyflake provides:
- 80-85% smaller image size
- Production-grade Sonyflake library (vs custom implementation)
- Clock rollback detection and handling
- Better performance and concurrency
- Lower memory usage
- Faster startup times
- **Same config format** - uses `SHARD_ID` environment variable
- Same API compatibility

No application changes required - same endpoints and response format.

### Configuration Compatibility

Both versions use the same configuration approach:

**Node.js**: Set `SHARD_ID` in launch config
**Go**: Set `SHARD_ID` environment variable

This makes migration seamless.
