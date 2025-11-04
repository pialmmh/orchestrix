# Go-ID Alpine Container

Minimal Alpine Linux container with Go-ID standalone binary.

## Size Comparison

| Type | Base OS | Binary | Total | Savings |
|------|---------|--------|-------|---------|
| Original | Debian 12 | Go runtime | ~169 MB | - |
| **Alpine** | Alpine 3.18 | Standalone | **~25 MB** | **85%** |

## Quick Start

Build the container:
```bash
cd build
./build.sh
```

Test with default configuration:
```bash
cd go-id-v.1/generated
./startDefault.sh
```

## Directory Structure

```
go-id/
├── build/
│   ├── build.sh          # Build script (copies binary)
│   └── build.conf        # Build configuration
├── launchGoId.sh    # Launch script
├── templates/
│   ├── sample.conf       # Sample configuration
│   └── startDefault.sh   # Quick start script
└── go-id-v.1/
    └── generated/
        ├── artifact/     # Container images
        ├── publish/      # Publishing scripts
        └── test/         # Test runners
```

## Binary Source

This container uses a pre-built standalone binary from:
```
/home/mustafa/telcobright-projects/orchestrix/images/standalone-binaries/go-id/go-id-binary-v.1/go-id
```

To rebuild the binary:
```bash
cd /home/mustafa/telcobright-projects/orchestrix/images/standalone-binaries/go-id
mvn exec:java -Dexec.mainClass="com.telcobright.orchestrix.automation.binary.goid.GoIdBinaryBuildRunner" -Dexec.args="1"
```

## Configuration

Edit `sample.conf` to configure:
- Container name
- Service port
- Shard ID (for clustering)
- Consul registration (optional)

## API Endpoints

- `GET /health` - Health check
- `GET /shard-info` - Shard configuration
- `GET /api/next-id/:entityName` - Generate single ID
- `GET /api/next-batch/:entityName?batchSize=N` - Generate batch
- `GET /api/status/:entityName` - Entity status
- `GET /api/list` - List all entities
- `GET /api/types` - Available ID types

## Deployment

Launch on remote server:
```bash
./launchGoId.sh /path/to/custom-config.conf
```

Multiple instances (sharding):
```bash
# Shard 1
SHARD_ID=1 ./launchGoId.sh shard1.conf

# Shard 2
SHARD_ID=2 ./launchGoId.sh shard2.conf

# Shard 3
SHARD_ID=3 ./launchGoId.sh shard3.conf
```

## Monitoring

Check container status:
```bash
lxc list go-id
```

View logs:
```bash
lxc exec go-id-instance -- tail -f /var/log/Go-ID.log
```

## Troubleshooting

### Container won't start
- Check if binary exists: `ls -la /home/mustafa/telcobright-projects/orchestrix/images/standalone-binaries/go-id/go-id-binary-v.1/go-id`
- Build binary if missing (see Binary Source above)

### Service not responding
- Check if service is running: `lxc exec CONTAINER -- rc-status`
- Check logs: `lxc exec CONTAINER -- cat /var/log/Go-ID.log`

### Port conflict
- Change SERVICE_PORT in config file
- Ensure port is not in use: `ss -tulpn | grep PORT`
