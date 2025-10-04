# Go ID Generator Container

High-performance unique ID generator service built with Go, providing distributed ID generation using the Snowflake algorithm.

## Features

- **Go-based REST API** for unique ID generation
- **Snowflake algorithm** for distributed, time-ordered IDs
- **Small footprint**: 10-30MB (vs 150-300MB for Node.js version)
- **High concurrency**: Goroutines handle 10K+ concurrent requests
- **Low resource usage**: 512MB RAM vs 1-2GB for Node.js
- **BTRFS storage** with quota management and snapshots
- **Systemd service** for automatic startup and restart
- **Health check endpoint** for monitoring

## Architecture

### ID Generation
- Uses Snowflake algorithm for distributed ID generation
- 64-bit IDs: timestamp (42 bits) + machine ID (10 bits) + sequence (12 bits)
- Time-ordered, sortable IDs
- 4096 IDs per millisecond per machine

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
| Image Size | 10-30MB | 150-300MB | ~90% smaller |
| Memory | 512MB | 1-2GB | ~75% less |
| Disk Quota | 5GB | 10GB | 50% less |
| Startup Time | <1s | 2-5s | 5x faster |
| Concurrency | 10K+ | Limited | Much better |

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

The Go version provides:
- 80-90% smaller image size
- Better performance and concurrency
- Lower memory usage
- Faster startup times
- Same API compatibility

No application changes required - same endpoints and response format.
