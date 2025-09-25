# Orchestrix AI Agent Instructions

## Purpose
This document provides instructions and context for AI agents working with the Orchestrix project. It defines global settings, conventions, and configurations that should be followed across all Orchestrix components.

## Global Configuration Directory

The `/config` directory contains all global Orchestrix configurations:

```
orchestrix/config/
├── orchestrix_instruction_ai_agent.md  # This file - AI agent instructions
├── java.conf                          # Global Java version and settings
├── network.conf                       # (Future) Network configurations
├── containers.conf                    # (Future) Container defaults
├── security.conf                      # (Future) Security settings
└── automation.conf                    # (Future) Automation preferences
```

## Key Global Settings

### 1. Java Environment
**Configuration File**: `java.conf`

- **Default Java Version**: 21 (OpenJDK)
- **Always use Java 21** for all Orchestrix components unless explicitly overridden
- **Auto-installation**: Enabled with interactive prompts
- **Vendor**: OpenJDK (preferred for consistency)

When creating or modifying Java-based components:
1. Always check `config/java.conf` for the current Java version
2. Use `ORCHESTRIX_JAVA_VERSION` environment variable if available
3. Fall back to Java 21 if configuration is not accessible

### 2. Container Standards
All LXC containers must:
- Follow the scaffolding guide in `images/lxc/LXC_CONTAINER_SCAFFOLDING_GUIDE.md`
- Use versioned directories (e.g., `container-name-v.1.0.0/`)
- Implement Java automation with inline scripts for visibility
- Support configuration from any filesystem location
- Use the global Java version from `config/java.conf`

### 3. Network Architecture
- **Bridge mode only** (no NAT) for production containers
- **Subnet**: 10.10.199.0/24 for container network
- **Bridge**: lxdbr0 (standard)
- Temporary NAT only during build for package installation

### 4. Automation Principles
- **Reusability**: Create reusable automation classes in `orchestrix/automation/`
- **Interactive Mode**: Always prompt for confirmation before system changes
- **Error Handling**: Implement retry logic with clear error messages
- **Logging**: Use proper logging levels (INFO, WARNING, ERROR)

## Java Installation Rules

When Java components are missing:

1. **Check Global Config First**
   - Read `config/java.conf` for version and settings
   - Use `ORCHESTRIX_JAVA_VERSION` (default: 21)

2. **Interactive Installation**
   - If `ORCHESTRIX_JAVA_INTERACTIVE=true`, always prompt user
   - Show what will be installed and why
   - Allow user to abort or continue

3. **Sudo/Root Context**
   - Detect if running as root/sudo
   - If Java missing for sudo user, prompt:
     ```
     Java 21 is not installed for the root user.
     Would you like to install it now? (y/N):
     ```
   - If NO: Abort with clear message
   - If YES: Proceed with installation

4. **Installation Commands**
   ```bash
   # Ubuntu/Debian
   sudo apt-get update
   sudo apt-get install -y openjdk-21-jdk

   # RHEL/Fedora
   sudo dnf install -y java-21-openjdk-devel

   # macOS
   brew install openjdk@21
   ```

## File Path Conventions

- **Global Config**: `/orchestrix/config/`
- **Automation**: `/orchestrix/automation/src/main/java/com/telcobright/orchestrix/automation/`
- **Container Images**: `/orchestrix/images/lxc/[container-name]/`
- **Build Outputs**: `[container-name]-v.X.Y.Z/generated/`

## Environment Variables

Orchestrix components should recognize these environment variables:

```bash
ORCHESTRIX_HOME=/path/to/orchestrix
ORCHESTRIX_JAVA_VERSION=21
ORCHESTRIX_JAVA_HOME=/path/to/java
ORCHESTRIX_CONFIG_DIR=$ORCHESTRIX_HOME/config
ORCHESTRIX_INTERACTIVE=true
```

## AI Agent Behavior Guidelines

When working on Orchestrix:

1. **Always check global config** before making assumptions about versions or settings
2. **Use interactive prompts** for system-level changes
3. **Follow the LXC scaffolding guide** exactly for new containers
4. **Maintain backward compatibility** when updating shared components
5. **Document configuration changes** in this file
6. **Create reusable automation** rather than one-off scripts
7. **Use Java 21** as the default for all Java components

## Configuration Loading Order

1. Environment variables (highest priority)
2. Global config files in `/orchestrix/config/`
3. Component-specific config files
4. Hardcoded defaults (lowest priority)

## Future Configuration Files

These configuration files will be added as needed:

- `network.conf` - Network settings, bridge configurations
- `containers.conf` - Default container settings, naming conventions
- `security.conf` - Security policies, SSH settings
- `automation.conf` - Automation preferences, retry policies
- `database.conf` - Database connections, credentials
- `logging.conf` - Logging levels, output formats

## Updating This Document

When adding new global configurations:

1. Create the config file in `/orchestrix/config/`
2. Update this document with the new configuration
3. Update relevant automation to read from the config
4. Maintain backward compatibility

---

**Last Updated**: 2025-09-25
**Version**: 1.0.0
**Maintained By**: Orchestrix Team