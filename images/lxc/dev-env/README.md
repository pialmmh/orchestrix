# Development Environment Container

General-purpose LXC container for development environment with optional SSH tunneling and other services.

## Quick Start

### Option 1: Instant Start (Using Defaults)
```bash
sudo ./startDefault.sh
```
This automatically builds the image (if needed) and starts a container with sample config.

### Option 2: Custom Configuration

#### 1. Build Base Image (One-time)
```bash
sudo ./buildDevEnv.sh
```

#### 2. Launch Container
```bash
# Copy and modify sample config
cp sample-config.conf my-dev.conf
nano my-dev.conf

# Launch with your config
sudo ./launchDevEnv.sh my-dev.conf
```

## Files

- `startDefault.sh` - Quick start with defaults (builds & launches)
- `buildDevEnv.sh` - Build script (creates base image)
- `buildDevEnvConfig.cnf` - Build configuration
- `launchDevEnv.sh` - Launch script (starts containers)
- `sample-config.conf` - Sample runtime configuration (with SSH info)
- `scripts/` - Management scripts (used internally)

## Configuration

Edit `sample-config.conf` to define:
- Container name
- Environment variables
- Bind mounts for your projects
- SSH tunnels to remote services (optional)

### SSH Client Configuration (Pre-configured)
The container's SSH client automatically accepts all certificates without prompts:
- No password/confirmation prompts for new hosts
- No warnings about changed host keys
- Immediate connection to any server
- Perfect for development environments

### Optional SSH Tunnels
If you need to tunnel to remote services, uncomment and configure:
```bash
TUNNELS=(
    "Kafka|103.95.96.76|9092|9092|user|pass|22|Kafka broker"
    "MySQL|10.0.1.50|3306|3306|dbuser|dbpass|22|Database"
)
```

## Management

```bash
# Check status
lxc exec dev-env -- /usr/local/bin/dev-env-services.sh status

# Access shell
lxc exec dev-env -- bash

# View logs
lxc exec dev-env -- tail -f /var/log/ssh-tunnels/*.log

# Stop container
lxc stop dev-env
```

## Launch from Any Location

You can launch containers with config files from anywhere:
```bash
sudo ./launchDevEnv.sh /tmp/my-config.conf
sudo ./launchDevEnv.sh /opt/configs/dev.conf
sudo ./launchDevEnv.sh ~/development.conf
```