# AI Instructions for Unique ID Generator Container

## Directory Structure Pattern

When building or updating the unique-id-generator container, follow this structure:

```
unique-id-generator/
├── build-minimal.sh                    # Main build script
├── scripts/                            # Source code directory
│   ├── server-simple.js               # Simplified API server (auto-registers entities)
│   ├── snowflake.js                   # Snowflake ID generator implementation
│   ├── uid-cli.js                     # CLI tool for maintenance
│   └── package.json                   # Node.js dependencies
└── unique-id-generator-v.X.X.X/       # Version-specific directory
    └── generated/                      # Runtime configuration
        ├── sample.conf                 # Sample configuration
        ├── launch.sh                   # Launch script (requires config)
        ├── launch_default.sh           # Quick launch with sample.conf
        ├── HOW-TO.md                  # User guide
        └── API-REFERENCE.md           # API documentation
```

## Building Process

### 1. Build Script (`build-minimal.sh`)
- Downloads Node.js v20.11.0 binary directly (avoids package manager issues)
- Creates directories: `/opt/unique-id-generator`, `/var/lib/unique-id-generator`, `/var/log/unique-id-generator`
- Copies: `server-simple.js` as `server.js`, `snowflake.js`, `uid-cli.js`, `package.json`
- Installs npm dependencies
- Creates systemd service with `User=nobody`
- Sets proper permissions (nobody:nogroup)
- Creates symlink: `/usr/local/bin/uid-cli`
- Image name: `unique-id-generator-base` (no version in image name)

### 2. Key Implementation Files

#### `server-simple.js`
- Simplified API with only ID generation endpoints
- Auto-registers entities on first use
- Returns error if entity exists with different type
- Endpoints: `/api/next-id/:entity`, `/api/next-batch/:entity`, `/api/parse-snowflake/:id`, `/api/types`, `/health`

#### `snowflake.js`
- 64-bit ID generator: 41 bits timestamp, 10 bits shard, 12 bits sequence
- Supports 1024 shards, 4096 IDs/ms per shard
- Custom epoch: 2024-01-01
- Methods: `generate()`, `generateBatch()`, `parse()`, `getStats()`, `getInfo()`

#### `uid-cli.js`
- CLI tool for maintenance operations
- Commands: list, status, init, reset, delete, clear, info, help
- Uses environment variables: DATA_DIR, SHARD_ID, TOTAL_SHARDS

## Version Management

### Creating New Version
When user asks to "scaffold version X.X.X":

1. Create directory: `unique-id-generator-v.X.X.X/generated/`
2. Copy and update these files:
   - `sample.conf` - Update version number in comments
   - `launch.sh` - Main launch script
   - `launch_default.sh` - Quick launch wrapper
   - `HOW-TO.md` - User guide
   - `API-REFERENCE.md` - API documentation

3. Update `sample.conf` with:
   - Version-specific comments
   - Default `CONTAINER_NAME="uid-gen-vX"`
   - `BASE_IMAGE="unique-id-generator-base"` (no version in image name)

### Launch Scripts Pattern

#### `launch.sh`
- Requires config file as argument
- Sources config file for all variables
- Launches container from base image
- Creates `/opt/unique-id-generator/.env` with runtime config
- Mounts host directories if specified
- Tests service health after launch

#### `launch_default.sh`
- Simple wrapper that calls `launch.sh` with `sample.conf`
- Provides quick start option

## Configuration Variables

Required in config file:
- `CONTAINER_NAME` - Unique container name
- `BASE_IMAGE` - Image to use (default: unique-id-generator-base)

Sharding:
- `SHARD_ID` - Shard number (1-1024)
- `TOTAL_SHARDS` - Total shard count

Optional:
- `CONTAINER_IP` - Fixed IP address
- `BRIDGE_NAME` - Network bridge (default: lxdbr0)
- `SERVICE_PORT` - Service port (default: 8080)
- `HOST_DATA_DIR` - Host mount for data persistence
- `HOST_LOG_DIR` - Host mount for logs
- `DATA_DIR` - Internal data directory
- `LOG_LEVEL` - Logging level (debug/info/warn/error)

## API Design Principles

1. **Simplicity**: Only essential endpoints for ID generation
2. **Auto-registration**: Entities register automatically on first use
3. **Type safety**: Once registered, entity type cannot change
4. **Error handling**: Clear error messages for type mismatches
5. **No maintenance endpoints**: All admin tasks via CLI tool

## Common Issues and Solutions

### Node.js Installation
- Problem: Package manager conflicts between NodeSource and Debian
- Solution: Download Node.js binary directly from nodejs.org

### Permissions
- Problem: Service can't write to data directory
- Solution: Create directories in build, set ownership to nobody:nogroup

### Network Configuration
- Problem: Fixed IP not in bridge subnet
- Solution: Use correct subnet (10.10.199.0/24 for lxdbr0)

## Testing Checklist

After building:
1. Launch container with `launch_default.sh`
2. Test health endpoint: `curl http://<IP>:8080/health`
3. Generate Snowflake ID: `curl 'http://<IP>:8080/api/next-id/test?dataType=snowflake'`
4. Test CLI: `lxc exec <container> -- uid-cli list`
5. Check logs: `lxc exec <container> -- journalctl -u unique-id-generator`

## Important Notes

1. **Never include version in image name** - Always use `unique-id-generator-base`
2. **Each version has its own generated/ folder** with launch scripts and docs
3. **Build script overwrites the image** but versions remain separate
4. **Launch scripts accept config from anywhere** in the filesystem
5. **CLI tool is installed globally** as `/usr/local/bin/uid-cli`
6. **Service runs as nobody:nogroup** for security
7. **State file at** `/var/lib/unique-id-generator/state.json`

## Quick Commands for Development

```bash
# Build image
./build-minimal.sh

# Launch with version's default config
cd unique-id-generator-v.1.0.0/generated/
./launch_default.sh

# Launch with custom config
./launch.sh /path/to/custom.conf

# Test API
curl 'http://10.10.199.51:8080/api/next-id/test?dataType=snowflake'

# Access CLI
lxc exec uid-gen-v1 -- uid-cli list

# Check logs
lxc exec uid-gen-v1 -- journalctl -u unique-id-generator -f

# Delete container
lxc delete --force uid-gen-v1
```