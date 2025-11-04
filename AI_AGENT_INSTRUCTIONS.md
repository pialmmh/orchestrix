# AI Agent Instructions for Orchestrix Container Development

## IMPORTANT: Two Systems Available
We have two container creation systems. See `/home/mustafa/telcobright-projects/orchestrix/images/containers/lxc/CONTAINER_CREATION_GUIDE.md` for detailed comparison.

### Quick Decision:
- **Shell-based**: Use for simple containers, quick prototypes
- **Java-based**: Use for complex containers with verification needs

## Custom Commands for AI Assistant

### /scaffold <container-name>
When user types `/scaffold <container-name>`, execute the scaffolding process:

1. **Run the scaffold script**:
   ```bash
   /home/mustafa/telcobright-projects/orchestrix/scaffold <container-name>
   ```

2. **If first time**: Script will create REQUIREMENTS.md and ask user to fill it
3. **If requirements exist**: Script will generate all files automatically

This is an AI instruction command, not a shell command. When you see `/scaffold`, treat it as a direct instruction to run the scaffolding process.

## Role
You are an AI assistant specialized in scaffolding and building LXC containers for the Orchestrix project. When asked to "scaffold" a container, follow the standardized template and patterns established in this project.

## Key Context Files
When scaffolding containers, refer to:
1. `/home/mustafa/telcobright-projects/orchestrix/images/containers/lxc/CONTAINER_CREATION_GUIDE.md` - **PRIMARY GUIDE**
2. `/home/mustafa/telcobright-projects/orchestrix/images/containers/lxc/CONTAINER_SCAFFOLD_TEMPLATE.md` - Java system details
3. `/home/mustafa/telcobright-projects/orchestrix/images/containers/lxc/auto-increment-service/` - Shell example
4. `/home/mustafa/telcobright-projects/orchestrix/images/containers/lxc/dev-env/` - Java example

## Core Requirements for Every Container

### 1. Two-Phase Architecture
- **Build Phase**: Creates reusable base image (one-time)
- **Launch Phase**: Starts containers with configs from anywhere

### 2. File Structure Depends on System

#### Shell-Based:
```
/home/mustafa/telcobright-projects/orchestrix/images/containers/lxc/[container-name]/
├── build[ContainerName].sh         # Build base image
├── build[ContainerName]Config.cnf  # Build config
├── launch[ContainerName].sh        # Launch with any config
├── startDefault.sh                 # Quick start (launch only)
├── scripts/                        # Internal scripts
└── README.md                       # Documentation
```

#### Java-Based:
```
/home/mustafa/telcobright-projects/orchestrix/images/containers/lxc/[container-name]/
├── build.yaml                      # Build configuration
├── [ContainerName]Builder.java     # Custom builder
├── build/
│   └── build.sh                    # Wrapper script
└── README.md                       # Documentation
```

### 3. Configuration Philosophy
- **Single config file**: All parameters in one file
- **Arbitrary location**: Config can be anywhere (`/tmp/`, `~/`, `/opt/`, etc.)
- **Bind mounts**: Defined in config, not hardcoded
- **Optional services**: Container works without all features

### 4. SSH Settings for Dev Containers
```bash
StrictHostKeyChecking no
UserKnownHostsFile /dev/null
CheckHostIP no
LogLevel ERROR
```

## Scaffolding Process

### Step 0: Choose System (Shell vs Java)
**Ask user**: "Would you like to use the shell-based system (simpler) or Java-based system (advanced)?"
Or make intelligent choice based on complexity.

### Step 1: Understand Requirements
When user requests a container, identify:
- Purpose (dev, testing, production)
- Required services/tools
- Networking needs
- Storage requirements

### Step 2: Create Directory Structure
```bash
mkdir -p /home/mustafa/telcobright-projects/orchestrix/images/containers/lxc/[container-name]/scripts
```

### Step 3: Generate Files
- **For Shell-based**: Follow templates in CONTAINER_CREATION_GUIDE.md Shell section
  - **IMPORTANT**: Copy network validation functions from `/home/mustafa/telcobright-projects/orchestrix/common/lxc-network-validation-template.sh`
  - Copy the functions INLINE into the build script (do not source the file)
  - Functions to copy: `validate_network_config()`, `configure_container_network()`, `test_and_wait_for_internet()`
- **For Java-based**: Follow templates in CONTAINER_CREATION_GUIDE.md Java section

### Step 4: Key Implementations
- Base image name: `[container-name]-base`
- Config loading: `source "$CONFIG_FILE"` (from command line argument)
- Bind mounts: Loop through `BIND_MOUNTS` array from config
- Services: Make optional with existence checks

### Step 5: Documentation
Create clear README with:
- Quick start (startDefault.sh)
- Custom configuration examples
- Service descriptions
- Management commands

## Example Scaffold Commands

### Example 1: Database Container
User: "Scaffold a PostgreSQL development container"

Response: Create postgres-dev/ with:
- PostgreSQL 15 installation
- Auto-accept SSH certificates
- Bind mounts for data and configs
- Optional replication setup
- Port 5432 exposed

### Example 2: Web Server Container  
User: "Scaffold an nginx container for testing"

Response: Create nginx-test/ with:
- Nginx latest
- Bind mounts for web content
- Config from host
- Optional SSL setup
- Ports 80/443 exposed

## Validation Checklist

Before completing scaffold:
- [ ] All files follow naming convention
- [ ] Build script creates `[name]-base` image
- [ ] Launch accepts config from ANY path
- [ ] Sample config documents all options
- [ ] Services are optional
- [ ] README has quick start
- [ ] startDefault.sh works standalone
- [ ] Scripts are executable (chmod +x)

## Standard Success Response

```
✅ Scaffolded [container-name] container

Structure created:
/home/mustafa/telcobright-projects/orchestrix/images/containers/lxc/[container-name]/

Quick start:
cd [container-name]
sudo ./startDefault.sh

Or with custom config:
sudo ./launch[Name].sh /path/to/config.conf

Key features:
- [List main features]
- [List services]
- [List bind mount options]
```

## Common Patterns to Follow

### 1. Config Loading Pattern
```bash
# In launch script
CONFIG_FILE="$1"
if [ ! -f "$CONFIG_FILE" ]; then
    echo "Error: Config file not found: $CONFIG_FILE"
    exit 1
fi
source "$CONFIG_FILE"
```

### 2. Bind Mount Pattern
```bash
for MOUNT in "${BIND_MOUNTS[@]}"; do
    IFS=':' read -r HOST_PATH CONTAINER_PATH <<< "$MOUNT"
    lxc config device add ${CONTAINER_NAME} ... source="${HOST_PATH}" path="${CONTAINER_PATH}"
done
```

### 3. Optional Service Pattern
```bash
if [ -n "${SERVICE_CONFIG}" ]; then
    # Configure service
else
    echo "Service not configured (optional)"
fi
```

## Remember
- Never hardcode paths
- Always make services optional
- Config files can be ANYWHERE
- Follow established naming patterns
- Keep it simple and clean
- Document everything clearly