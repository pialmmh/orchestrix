# Go-ID Standalone Binary

Statically-linked Go binary for distributed unique ID generation.

## How Binary is Obtained

Binaries in this directory are either:
1. **Built automatically** by the automation system
2. **Downloaded** from a release/artifact repository

**No build script exists here.** Binaries are managed by automation or downloaded.

## Prerequisites for Auto-Building

If automation builds the binary:
- Go 1.21+ installed on build machine
- SSH access to localhost (for automation)

## What Gets Built

- **Binary**: `go-id-binary-v.{VERSION}/go-id`
- **Size**: ~18-20 MB (statically linked)
- **Target**: Linux AMD64
- **Tests**: 5 automated tests run during build

## Build Process

1. **Generate source code** (Go service with Sonyflake)
2. **Install dependencies** (sonyflake, gorilla/mux, consul API)
3. **Compile binary** (CGO_ENABLED=0 for static linking)
4. **Run tests**:
   - Binary startup
   - Health endpoint
   - Shard info
   - ID generation
   - Batch generation

## Output Structure

```
go-id/
├── build.sh                    # ← Build script
├── README.md                   # ← This file
├── go-id-binary-v.1/
│   ├── go-id                  # ← Binary (18 MB)
│   └── test-results.json      # Test results (if saved)
└── go-id-binary-v.2/
    └── go-id
```

## Test Binary Manually

```bash
# Start binary
export SERVICE_PORT=7001
export SHARD_ID=1
export TOTAL_SHARDS=1
./go-id-binary-v.1/go-id &

# Test health endpoint
curl http://localhost:7001/health

# Generate ID
curl http://localhost:7001/api/next-id/test-entity

# Stop
killall go-id
```

## Use with Containers

This binary is designed for Alpine-based containers:

### LXC Container
```bash
# Alpine container (25 MB total)
lxc launch images:alpine/3.18 go-id-alpine
lxc file push go-id-binary-v.1/go-id go-id-alpine/usr/local/bin/
lxc exec go-id-alpine -- chmod +x /usr/local/bin/go-id
```

### Docker
```dockerfile
FROM alpine:3.18
COPY go-id-binary-v.1/go-id /usr/local/bin/go-id
RUN chmod +x /usr/local/bin/go-id
CMD ["/usr/local/bin/go-id"]
```

## Size Comparison

| Approach | Base | Binary | Total | Savings |
|----------|------|--------|-------|---------|
| **Debian + Go** | 105 MB | 18 MB + 150 MB Go | 169 MB | - |
| **Alpine + Binary** | 5 MB | 18 MB | **25 MB** | **85%** |

## Dependencies (Built Into Binary)

- github.com/sony/sonyflake - Distributed ID generation
- github.com/gorilla/mux - HTTP routing
- github.com/hashicorp/consul/api - Service discovery

## Troubleshooting

### Build Fails: "Go not installed"
```bash
# Install Go 1.21+
wget https://go.dev/dl/go1.21.0.linux-amd64.tar.gz
sudo tar -C /usr/local -xzf go1.21.0.linux-amd64.tar.gz
export PATH=$PATH:/usr/local/go/bin
```

### Tests Fail: "Port already in use"
```bash
# Kill existing go-id processes
killall go-id

# Or change test port in build config
```

### Binary Won't Run
```bash
# Check if statically linked
ldd go-id-binary-v.1/go-id
# Should say: "not a dynamic executable"

# Check architecture
file go-id-binary-v.1/go-id
# Should say: "ELF 64-bit LSB executable, x86-64"
```

## API Endpoints

Once running, the binary exposes:

- `GET /health` - Health check
- `GET /shard-info` - Shard configuration
- `GET /api/next-id/:entityName` - Generate single ID
- `GET /api/next-batch/:entityName?batchSize=N` - Generate batch
- `GET /api/status/:entityName` - Entity status
- `GET /api/list` - List all entities
- `GET /api/types` - Available ID types

## Environment Variables

- `SERVICE_PORT` - HTTP port (default: 7001)
- `SHARD_ID` - Shard number (1-based)
- `TOTAL_SHARDS` - Total shards in cluster
- `CONSUL_URL` - Consul URL for registration (optional)
- `CONTAINER_IP` - IP for Consul registration (optional)

## Next Steps

After building:

1. **Test locally** (see "Test Binary Manually" above)
2. **Create Alpine container** (use binary scaffolding automation)
3. **Deploy to cluster** (3 shards recommended)
4. **Configure load balancer** (round-robin across shards)

## Version Management

- **v.1** - Initial release
- **v.2+** - Future versions with new features

Each version is independently built and tested.
