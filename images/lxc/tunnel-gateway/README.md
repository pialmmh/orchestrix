# Tunnel Gateway Container

Lightweight Alpine Linux container for SSH tunneling to remote services during development.

## Purpose

Acts as a local proxy/gateway for remote services (MySQL, Kafka, PostgreSQL, etc.) using SSH tunnels. Your development code connects to the container's IP address, and the container forwards traffic through SSH tunnels to remote servers.

**Benefits:**
- Remote servers see connections from localhost (via SSH)
- No need to open firewall ports or whitelist IPs
- Secure encrypted connections
- Multiple tunnels from single container
- Auto-reconnecting tunnels

## Quick Start

```bash
# 1. Build the image
cd build
./build.sh

# 2. Configure tunnels
cd ../tunnel-gateway-v.1/generated
cp sample.conf my-tunnels.conf
nano my-tunnels.conf  # Edit tunnel definitions

# 3. Launch container
./launchTunnelGateway.sh my-tunnels.conf

# 4. Use from your code
# Connect to container IP instead of remote servers
```

## Architecture

```
┌─────────────────┐
│  Java/Node App  │
│  (Development)  │
└────────┬────────┘
         │ jdbc:mysql://10.10.199.150:3306/mydb
         ↓
┌─────────────────────────────────┐
│  Container (10.10.199.150)      │
│  - Listens on 0.0.0.0:3306      │
│  - autossh tunnel active        │
└────────┬────────────────────────┘
         │ SSH Tunnel (encrypted)
         ↓
┌─────────────────────────────────┐
│  Remote MySQL Server            │
│  Connection appears from:       │
│  - localhost (via SSH)          │
└─────────────────────────────────┘
```

## Configuration Format

Edit `sample.conf`:

```bash
# Container settings
CONTAINER_NAME="tunnel-gateway-dev"
CONTAINER_IP="10.10.199.150"

# Tunnel definitions
# Format: NAME:LOCAL_PORT:SSH_HOST:SSH_USER:AUTH_TYPE:AUTH_VALUE:REMOTE_HOST:REMOTE_PORT

TUNNELS=(
    # MySQL with password
    "mysql-prod:3306:db.prod.com:dbadmin:PASSWORD:mypass123:localhost:3306"

    # Kafka with SSH key
    "kafka:9092:kafka.prod.com:kafkauser:KEY:/keys/kafka_key:localhost:9092"

    # PostgreSQL with password
    "postgres:5432:pg.staging.com:pguser:PASSWORD:pgpass456:localhost:5432"
)

# Optional SSH key mounts
BIND_MOUNTS=(
    "/home/mustafa/.ssh:/keys:ro"
)
```

## Tunnel Definition Fields

| Field | Description | Example |
|-------|-------------|---------|
| **NAME** | Tunnel identifier | `mysql-prod` |
| **LOCAL_PORT** | Port container listens on | `3306` |
| **SSH_HOST** | SSH server address | `db.example.com` |
| **SSH_USER** | SSH username | `dbadmin` |
| **AUTH_TYPE** | `PASSWORD` or `KEY` | `PASSWORD` |
| **AUTH_VALUE** | Password or key path | `mypass123` or `/keys/id_rsa` |
| **REMOTE_HOST** | Target host (via SSH) | `localhost` |
| **REMOTE_PORT** | Target port | `3306` |

## Building the Image

```bash
cd build
./build.sh

# To rebuild existing version:
./build.sh --overwrite
```

**Build output:**
- Image: `tunnel-gateway:1`
- Alias: `tunnel-gateway-base`
- Size: ~15-20 MB
- Base: Alpine Linux 3.20

## Launching Container

```bash
cd tunnel-gateway-v.1/generated

# Launch with custom config:
./launchTunnelGateway.sh /path/to/config.conf

# Launch with sample config:
./launchTunnelGateway.sh sample.conf

# Quick start with defaults:
cd ../.. && ./startDefault.sh
```

## Management Commands

### List Active Tunnels
```bash
lxc exec tunnel-gateway-dev -- /usr/local/bin/list-tunnels.sh
```

### Stop All Tunnels
```bash
lxc exec tunnel-gateway-dev -- /usr/local/bin/stop-tunnels.sh
```

### Start Tunnels
```bash
lxc exec tunnel-gateway-dev -- /usr/local/bin/start-tunnels.sh
```

### View Logs
```bash
lxc exec tunnel-gateway-dev -- cat /var/log/tunnel-gateway/startup.log
```

### Shell Access
```bash
lxc exec tunnel-gateway-dev -- bash
```

## Usage Examples

### Java Application (MySQL)

```java
// Instead of:
// jdbc:mysql://db.production.com:3306/mydb

// Connect to container:
String url = "jdbc:mysql://10.10.199.150:3306/mydb";
Connection conn = DriverManager.getConnection(url, "user", "pass");
```

### Java Application (Kafka)

```java
// Instead of:
// props.put("bootstrap.servers", "kafka.production.com:9092");

// Connect to container:
props.put("bootstrap.servers", "10.10.199.150:9092");
KafkaProducer<String, String> producer = new KafkaProducer<>(props);
```

### Node.js Application

```javascript
// Instead of:
// host: 'postgres.staging.com'

// Connect to container:
const client = new Client({
  host: '10.10.199.150',
  port: 5432,
  user: 'pguser',
  password: 'pgpass',
  database: 'mydb'
});
```

## Authentication Methods

### Password Authentication

```bash
TUNNELS=(
    "mysql:3306:db.example.com:user:PASSWORD:mypassword:localhost:3306"
)
```

**Note:** Passwords are stored in plaintext in config file. Ensure proper file permissions:
```bash
chmod 600 sample.conf
```

### SSH Key Authentication

```bash
# 1. Mount SSH keys into container
BIND_MOUNTS=(
    "/home/mustafa/.ssh:/keys:ro"
)

# 2. Reference key in tunnel definition
TUNNELS=(
    "mysql:3306:db.example.com:user:KEY:/keys/id_rsa:localhost:3306"
)
```

**SSH Key Requirements:**
- No passphrase (or use ssh-agent)
- Proper permissions (chmod 600)
- Public key installed on remote server

## Network Configuration

**Default IP:** `10.10.199.150`

**IP Range:** Use any IP from `10.10.199.x` range (Orchestrix LXC private network)

**Bridge:** Uses standard LXD bridge (lxdbr0)

**Static IP:** Configured via LXC device override

## Troubleshooting

### Tunnel Not Starting

```bash
# Check logs
lxc exec tunnel-gateway-dev -- cat /var/log/tunnel-gateway/startup.log

# Check SSH connectivity
lxc exec tunnel-gateway-dev -- ssh -v user@remote-server

# Check autossh processes
lxc exec tunnel-gateway-dev -- ps aux | grep autossh
```

### Port Already in Use

Check if port is already bound:
```bash
lxc exec tunnel-gateway-dev -- netstat -tlnp | grep :3306
```

### SSH Authentication Failed

**For password auth:**
- Verify username/password
- Check if password auth is enabled on SSH server

**For key auth:**
- Verify key is mounted correctly
- Check key permissions (must be 600)
- Verify public key is in `~/.ssh/authorized_keys` on remote server

### Cannot Connect from Application

1. Verify container is running:
   ```bash
   lxc list tunnel-gateway-dev
   ```

2. Verify tunnels are active:
   ```bash
   lxc exec tunnel-gateway-dev -- /usr/local/bin/list-tunnels.sh
   ```

3. Test connection from host:
   ```bash
   telnet 10.10.199.150 3306
   ```

## Security Considerations

### Development Use Only

This container is designed for **development environments** with:
- `StrictHostKeyChecking no` (auto-accepts SSH keys)
- Password authentication support
- Plaintext password storage in config

### Production Recommendations

For production use:
1. Use SSH key authentication only
2. Enable `StrictHostKeyChecking`
3. Store passwords in encrypted vault
4. Implement certificate-based authentication
5. Use VPN instead of SSH tunnels

## Technical Details

**Base Image:** Alpine Linux 3.20
**Size:** ~15-20 MB
**Packages:**
- openssh-client (SSH client)
- autossh (auto-reconnecting tunnels)
- sshpass (password authentication)
- bash (shell scripting)
- curl (utilities)

**Auto-reconnection:**
- ServerAliveInterval: 30 seconds
- ServerAliveCountMax: 3 attempts
- autossh monitors and restarts failed tunnels

**Startup:** Tunnels start automatically on container boot via `/etc/local.d/tunnels.start`

## Directory Structure

```
tunnel-gateway/
├── build/
│   ├── build.sh              # Build script
│   └── build.conf            # Build configuration
├── templates/
│   ├── launchTunnelGateway.sh   # Launch script
│   ├── sample.conf              # Sample configuration
│   └── startDefault.sh          # Quick start script
├── tunnel-gateway-v.1/
│   └── generated/
│       ├── launchTunnelGateway.sh  # Generated launch script
│       ├── sample.conf             # Generated sample config
│       └── artifact/
│           └── tunnel-gateway-v1-*.tar.gz  # Exported image
└── README.md                 # This file
```

## Version History

**v.1** - Initial release
- Alpine Linux 3.20 base
- Password and SSH key authentication
- Auto-reconnecting tunnels
- Multiple tunnel support
- Static IP configuration

## Support

For issues or questions, refer to:
- Orchestrix documentation: `/home/mustafa/telcobright-projects/orchestrix/`
- Container scaffolding standard: `CONTAINER_SCAFFOLD_TEMPLATE.md`
