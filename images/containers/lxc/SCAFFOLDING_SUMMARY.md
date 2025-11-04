# LXC Container Scaffolding - Quick Reference

## Interactive Scaffolding Workflow (MANDATORY)

When you ask AI to scaffold a new container, this workflow is followed:

### Step 1: AI Prepares build.conf
```
User: "Scaffold a new Alpine container for [service-name]"

AI: Creates build/build.conf template
    Asks user to fill required parameters:
    - BINARY_SOURCE (path to binary)
    - SERVICE_PORT (service port)
    - CONTAINER_NAME (confirm name)

    WAITS for user confirmation
```

### Step 2: User Fills build.conf
```
User: Reviews and fills configuration
      Sets paths, ports, limits
      Confirms: "Done, ready to proceed"
```

### Step 3: AI Generates Scaffolding
```
AI: Creates build/build.sh based on config
    Creates directory structure
    Creates templates
    Creates README
    Provides build commands
```

**Why?** User controls all configuration before generation. No assumptions about paths/ports.

---

## Container Types

### 1. Full-Stack Containers (Debian)
- **Base**: `images:debian/12`
- **Use**: Complex services, databases, monitoring
- **Build**: Compile/install inside container
- **Size**: 100-200 MB

### 2. Binary Containers (Alpine)
- **Base**: `images:alpine/3.20`
- **Use**: Go, Rust, static binaries
- **Build**: Copy pre-built binary
- **Size**: 3-10 MB (85-95% reduction)

## Two-Phase Binary Container Approach

### Phase 1: Build Binary (Standalone)
```bash
# Location: images/standalone-binaries/[name]/
# NO build.sh - Java automation builds

mvn exec:java \
  -Dexec.mainClass="com.telcobright.orchestrix.automation.binary.[name].[Name]BinaryBuildRunner" \
  -Dexec.args="1"

# Output: images/standalone-binaries/[name]/[name]-v.1/[binary]
```

### Phase 2: Scaffold Container (References Binary)
```bash
# Location: images/lxc/[name]/
# HAS build.sh - copies existing binary

mvn exec:java \
  -Dexec.mainClass="com.telcobright.orchestrix.automation.scaffold.[name].[Name]AlpineScaffoldRunner" \
  -Dexec.args="1"

# Output: images/lxc/[name]/[name]-v.1/generated/artifact/[name]-v1-[timestamp].tar.gz
```

**Critical**: These are **independent operations**, not a pipeline!

## Directory Structure

```
images/lxc/[container-name]/
├── build/
│   ├── build.sh              # Uses ABSOLUTE paths
│   └── build.conf            # References standalone binary
├── templates/
│   ├── sample.conf          # Source template
│   └── startDefault.sh      # Source template
├── [name]-v.1/              # VERSION 1 (Generated)
│   └── generated/           # Version-specific!
│       ├── artifact/        # Container images
│       ├── sample.conf      # Copy from template
│       └── startDefault.sh  # Copy from template
└── [name]-v.2/              # VERSION 2 (Generated)
    └── generated/           # Separate directory!
```

**Critical**: Each version has its own `generated/` - never use shared directory!

## Mandatory Requirements

1. ✅ **BTRFS Storage**: All containers use BTRFS with quotas
2. ✅ **Versioning**: Support version tracking ([name]-v.X)
3. ✅ **Build Structure**: Follow `build/` folder pattern
4. ✅ **Version-Specific Generated**: `[name]-v.X/generated/` NOT shared
5. ✅ **Absolute Paths**: Build scripts use absolute paths (not relative)
6. ✅ **Prerequisite Checking**: Check BTRFS, LXC, bridge, storage before build

## Alpine Build Script Pattern

```bash
#!/bin/bash
set -e

CONFIG_FILE="${1:-$(dirname "$0")/build.conf}"
source "$CONFIG_FILE"

# ABSOLUTE PATHS (Critical!)
BASE_DIR="/home/mustafa/telcobright-projects/orchestrix/images/lxc/${CONTAINER_NAME}"
IMAGE_FILE="${BASE_DIR}/${CONTAINER_NAME}-v.${VERSION}/generated/artifact/${CONTAINER_NAME}-v${VERSION}-${TIMESTAMP}.tar.gz"

# Build Alpine container
lxc launch images:alpine/3.20 ${BUILD_CONTAINER}
sleep 5

# Copy binary
lxc file push ${BINARY_SOURCE} ${BUILD_CONTAINER}/usr/local/bin/${BINARY_NAME}

# Export with absolute path
lxc export ${BUILD_CONTAINER} "${IMAGE_FILE}"

# Copy templates to VERSION-SPECIFIC directory
cp ${BASE_DIR}/templates/sample.conf ${BASE_DIR}/${CONTAINER_NAME}-v.${VERSION}/generated/
cp ${BASE_DIR}/templates/startDefault.sh ${BASE_DIR}/${CONTAINER_NAME}-v.${VERSION}/generated/
```

## Build Configuration (Alpine)

```bash
# Container Settings
CONTAINER_NAME="service-name"
VERSION="1"

# Binary Settings
BINARY_SOURCE="/home/mustafa/telcobright-projects/orchestrix/images/standalone-binaries/service-name/service-name-v.1/service-name"

# Service Configuration
SERVICE_PORT=7001
```

## Critical Rules

### Binary Containers
- ❌ NO build.sh in binary directories (automation builds)
- ✅ HAS build.sh in container directories (references binary)
- ✅ Binary path: Absolute path to standalone binary
- ✅ Phases are independent: Scaffold multiple times from same binary
- ❌ Never build binary inside Alpine container
- ❌ Never use relative paths in build scripts

### Directory Rules
- ✅ Each version: `[name]-v.X/generated/`
- ❌ Never use: `generated/` (shared directory)
- ✅ Templates in: `templates/`
- ✅ Artifacts in: `[name]-v.X/generated/artifact/`

### Path Rules
- ✅ Use: `${BASE_DIR}/${CONTAINER_NAME}-v.${VERSION}/generated/`
- ❌ Don't use: `../generated/` or `./generated/`
- ✅ Reason: LXC snap paths (/var/lib/snapd/hostfs/) break relative paths

## Quick Commands

### Build Binary
```bash
cd /home/mustafa/telcobright-projects/orchestrix
mvn compile
mvn exec:java -Dexec.mainClass="..." -Dexec.args="1"
```

### Scaffold Container
```bash
cd /home/mustafa/telcobright-projects/orchestrix
mvn exec:java -Dexec.mainClass="..." -Dexec.args="1"
```

### Build Container Image
```bash
cd /home/mustafa/telcobright-projects/orchestrix/images/lxc/[name]
./build/build.sh
```

## Output Structure

After build:
```
[name]-v.1/
└── generated/
    ├── artifact/
    │   ├── [name]-v1-[timestamp].tar.gz (6-10 MB)
    │   └── [name]-v1-[timestamp].tar.gz.md5
    ├── sample.conf
    └── startDefault.sh
```

## Automation Classes

### Binary Building
- **Base**: `BinaryBuilder.java`
- **Concrete**: `[Name]BinaryBuilder.java`
- **Runner**: `[Name]BinaryBuildRunner.java`

### Container Scaffolding
- **Base**: `AlpineContainerScaffold.java`
- **Concrete**: `[Name]AlpineScaffoldRunner.java`

## Common Mistakes

| ❌ Wrong | ✅ Correct |
|---------|-----------|
| Shared `generated/` | Version-specific `[name]-v.1/generated/` |
| Relative paths `../generated` | Absolute `${BASE_DIR}/[name]-v.1/generated/` |
| Build binary in container | Copy pre-built binary |
| `images:alpine/3.18` | `images:alpine/3.20` |
| build.sh in binary dir | build.sh only in container dir |

## References

- Full Guideline: `/home/mustafa/telcobright-projects/orchestrix/images/lxc/CONTAINER_SCAFFOLDING_STANDARD.md`
- Example: `/home/mustafa/telcobright-projects/orchestrix/images/lxc/go-id/`
- Binary Example: `/home/mustafa/telcobright-projects/orchestrix/images/standalone-binaries/go-id/`
