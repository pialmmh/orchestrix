# Container Scaffolding Template for Orchestrix

## Standard Container Architecture

All Orchestrix LXC containers follow a two-phase approach:
1. **Build Phase**: Creates a reusable base image with all necessary software
2. **Launch Phase**: Launches containers from the base image with custom configurations from anywhere on the filesystem

## Required Files Structure

When scaffolding a new container, create these files:

```
container-name/
├── startDefault.sh          # Quick start with defaults
├── buildContainerName.sh    # Build script (creates base image)
├── buildContainerNameConfig.cnf  # Build configuration
├── launchContainerName.sh   # Launch script (accepts config from anywhere)
├── sample-config.conf       # Sample runtime configuration
├── README.md               # Documentation
└── scripts/                # Internal management scripts
    └── container-services.sh  # Service management inside container
```

## Core Principles

### 1. Configuration Management
- **Build once, launch many**: Base image is built once, containers launched with different configs
- **Arbitrary config location**: Launch script must accept config files from ANY location
- **All parameters in config**: Container name, bind mounts, environment vars, services - everything comes from single config file
- **No embedded configs**: Base image should have placeholder configs only

### 2. SSH Configuration (for Dev Environments)
```bash
# Auto-accept all certificates without prompts
Host *
    StrictHostKeyChecking no
    UserKnownHostsFile /dev/null
    CheckHostIP no
    LogLevel ERROR
    ServerAliveInterval 30
    ServerAliveCountMax 3
```

### 3. Service Management
- Services should be optional (container works without them)
- Use systemd for service management
- Include health checks and auto-restart capabilities

## File Templates

### 1. buildContainerName.sh Template
```bash
#!/bin/bash
# Build script for [CONTAINER_PURPOSE] LXC container
# Creates reusable base image

set -e

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

# Load configuration
CONFIG_FILE="$(dirname "$0")/build[ContainerName]Config.cnf"
[Load config or use defaults]

# Container name for build
CONTAINER_NAME="[container-name]-build"
BASE_IMAGE_NAME="[container-name]-base"

# Functions:
# - check_root()
# - create_container() 
# - install_packages()
# - setup_configuration()
# - create_base_image() - publishes as BASE_IMAGE_NAME
# - cleanup_build_container()

# Main execution:
# 1. Check if base image exists (--overwrite to rebuild)
# 2. Create temporary build container
# 3. Install packages and configure
# 4. Create base image from container
# 5. Delete build container
# 6. Show success message
```

### 2. launchContainerName.sh Template
```bash
#!/bin/bash
# Launch [CONTAINER_PURPOSE] container with arbitrary config file

set -e

# Default container name
DEFAULT_CONTAINER_NAME="[container-name]"
BASE_IMAGE="[container-name]-base"

# Parse arguments:
# - First arg: config file path (required, can be anywhere)
# - --name: Override container name

# Load config from PROVIDED PATH
source "$CONFIG_FILE"

# Key features:
# 1. Check base image exists
# 2. Remove existing container if needed
# 3. Launch from base image
# 4. Setup bind mounts from config
# 5. Apply environment variables
# 6. Configure services (if any)
# 7. Show container info
```

### 3. sample-config.conf Template
```bash
#!/bin/bash
# Sample [CONTAINER_PURPOSE] Configuration

# Container name
CONTAINER_NAME=[container-name]

# Environment variables
CONTAINER_ENV_VARS=(
    "LOG_LEVEL=info"
    "ENVIRONMENT=development"
)

# Bind mounts (mount any host dir/file)
BIND_MOUNTS=(
    # "/host/path:/container/path"
)

# Service-specific configuration
# [Define any services, should be optional]
```

### 4. startDefault.sh Template
```bash
#!/bin/bash
# Quick start with default configuration

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"

# Check root
# Build image if needed (call buildContainerName.sh)
# Launch with sample config (call launchContainerName.sh sample-config.conf)
# Show status
```

## Scaffolding Checklist

When asked to scaffold a new container:

1. **Identify Purpose**
   - What services/tools does it provide?
   - Is it for dev, testing, or production?
   - What packages need to be installed?

2. **Create File Structure**
   - All files in `/home/mustafa/telcobright-projects/orchestrix/images/lxc/[container-name]/`
   - Follow naming convention: build[Name].sh, launch[Name].sh

3. **Key Implementation Points**
   - [ ] Build script creates base image named `[container-name]-base`
   - [ ] Launch script accepts config from ANY filesystem location
   - [ ] Sample config has all options documented
   - [ ] Services are optional (container works without them)
   - [ ] Bind mounts configured from config file
   - [ ] Environment variables passed from config
   - [ ] README includes quick start and customization
   - [ ] startDefault.sh for one-command startup

4. **For Dev Environments**
   - [ ] SSH client auto-accepts certificates
   - [ ] No user prompts for connections
   - [ ] Logging set to ERROR only

5. **Documentation Requirements**
   - [ ] README with Quick Start section
   - [ ] Sample config with inline documentation
   - [ ] File purpose descriptions

## Example Usage for AI Agent

When user says: "Scaffold a container for PostgreSQL development"

The AI should:
1. Create directory: `/home/mustafa/telcobright-projects/orchestrix/images/lxc/postgres-dev/`
2. Generate all required files following templates
3. Customize for PostgreSQL (packages, ports, configs)
4. Ensure config can be loaded from anywhere
5. Make services optional
6. Add SSH auto-accept for dev environment
7. Create comprehensive sample-config.conf

## Standard Responses

When scaffolding is complete, show:
```
✅ Container scaffolded: [container-name]

Files created:
- startDefault.sh (quick start)
- build[Name].sh (builds base image)
- launch[Name].sh (launches with any config)
- sample-config.conf (example configuration)
- README.md (documentation)

Quick start:
sudo ./startDefault.sh

Custom config:
sudo ./launch[Name].sh /path/to/your/config.conf
```

## Important Notes

1. **Never hardcode paths** - All paths come from config
2. **Make everything optional** - Container should start even with minimal config
3. **Use base images** - Build once, launch many times
4. **Config from anywhere** - Must support `/tmp/`, `~/`, `/opt/`, etc.
5. **Clean structure** - Only essential files, no clutter
6. **Consistent naming** - Follow the established patterns
7. **SSH for dev** - Dev containers auto-accept all certificates