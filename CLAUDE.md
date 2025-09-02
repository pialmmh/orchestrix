# Orchestrix Project - AI Assistant Context

## Project Overview
Orchestrix is an LXC container orchestration system for development and testing environments. All containers follow a standardized build and launch pattern.

## When Asked to "Scaffold" a Container

Immediately refer to and follow:
1. `/home/mustafa/telcobright-projects/orchestrix/images/lxc/CONTAINER_SCAFFOLD_TEMPLATE.md`
2. `/home/mustafa/telcobright-projects/orchestrix/AI_AGENT_INSTRUCTIONS.md`

## Key Project Standards

### Container Architecture
- **Two-phase approach**: Build image once, launch many containers
- **Config from anywhere**: Accept config files from any filesystem location
- **Single config file**: All parameters (mounts, env vars, services) in one file
- **Optional services**: Containers work without all features configured

### File Naming Convention
- Build script: `build[ContainerName].sh`
- Launch script: `launch[ContainerName].sh`
- Config: `build[ContainerName]Config.cnf`
- Quick start: `startDefault.sh`
- Base image: `[container-name]-base`

### Directory Structure
```
/home/mustafa/telcobright-projects/orchestrix/images/lxc/[container-name]/
├── startDefault.sh
├── build[Name].sh
├── launch[Name].sh
├── sample-config.conf
├── README.md
└── scripts/
```

### SSH Configuration for Dev Environments
All development containers must auto-accept SSH certificates:
```bash
StrictHostKeyChecking no
UserKnownHostsFile /dev/null
CheckHostIP no
LogLevel ERROR
```

## Example Reference Implementation
See `/home/mustafa/telcobright-projects/orchestrix/images/lxc/dev-env/` for a complete implementation following all standards.

## Quick Scaffold Process

1. Create directory structure
2. Copy and adapt templates from CONTAINER_SCAFFOLD_TEMPLATE.md
3. Customize for specific purpose
4. Ensure config can be loaded from anywhere
5. Make all services optional
6. Add comprehensive documentation
7. Test with startDefault.sh

## Common Container Requests

### Development Containers
- Include SSH auto-accept configuration
- Bind mount for project workspaces
- Development tools and utilities
- Optional service tunnels

### Service Containers
- Specific service (database, cache, queue)
- Data persistence via bind mounts
- Configuration from host
- Health checks and monitoring

### Testing Containers
- Isolated test environment
- Test framework installation
- Result output to host
- Cleanup capabilities

## Important Rules
1. **Never hardcode paths** - Everything from config
2. **Services are optional** - Container starts without them
3. **Config from anywhere** - Support any path
4. **Clean and minimal** - Only essential files
5. **Follow patterns** - Consistency across all containers

## Validation Before Completion
- [ ] Files follow naming convention
- [ ] Base image name correct
- [ ] Launch accepts any config path
- [ ] Sample config fully documented
- [ ] README has quick start
- [ ] startDefault.sh works
- [ ] All scripts executable

## Need More Context?
Check these files:
- Template: `images/lxc/CONTAINER_SCAFFOLD_TEMPLATE.md`
- Instructions: `AI_AGENT_INSTRUCTIONS.md`
- Reference: `images/lxc/dev-env/` directory