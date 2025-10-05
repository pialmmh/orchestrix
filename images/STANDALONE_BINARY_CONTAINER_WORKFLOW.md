# Standalone Binary Container Workflow

**AI Agent Guidelines for Building Containers with Pre-Built Binaries**

This workflow applies to: **LXC, Docker, Podman, and any container technology**

## Overview

Instead of building software **inside** containers, we:
1. **Build binaries locally** (on build server using `build.sh`)
2. **Test binaries locally** (automated tests)
3. **Bundle into minimal containers** (Alpine, distroless)

**Benefits:**
- 🚀 **85% smaller containers** (Alpine 25 MB vs Debian 169 MB)
- ⚡ **Faster builds** (no dependency download in container)
- ✅ **Tested binaries** (known-good before containerization)
- 🔄 **Reusable** (same binary → multiple container types)

**Quick Start:**
```bash
# Step 1: Build binary
cd images/standalone-binaries/go-id
./build.sh

# Step 2: Create Alpine container (future automation)
# Step 3: Deploy
```

---

## Architecture

```
┌─────────────────────────────────────────────────────────────┐
│  Phase 1: Binary Building (Automation 1)                    │
│  Location: images/standalone-binaries/                      │
├─────────────────────────────────────────────────────────────┤
│  1. Generate source code                                    │
│  2. Compile static binary (CGO_ENABLED=0)                   │
│  3. Run automated tests                                     │
│  4. Store: images/standalone-binaries/[app]/[version]/      │
└─────────────────────────────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────────┐
│  Phase 2: Container Scaffolding (Automation 2)              │
│  Location: images/lxc/, images/docker/, etc.                │
├─────────────────────────────────────────────────────────────┤
│  1. Detect available binary versions                        │
│  2. Select or prompt user for version                       │
│  3. Create minimal base container (Alpine, etc.)            │
│  4. Copy binary into container                              │
│  5. Configure service (systemd, OpenRC, supervisor)         │
│  6. Export container image                                  │
└─────────────────────────────────────────────────────────────┘
```

---

## Directory Structure

```
orchestrix/
├── images/
│   ├── standalone-binaries/           # Binary artifacts (Phase 1)
│   │   ├── go-id/
│   │   │   ├── go-id-binary-v.1/
│   │   │   │   ├── go-id             # ← Binary (15-20 MB)
│   │   │   │   ├── README.md
│   │   │   │   └── test-results.json
│   │   │   └── go-id-binary-v.2/
│   │   ├── consul-exporter/
│   │   └── custom-service/
│   │
│   ├── lxc/                           # LXC containers (Phase 2)
│   │   ├── go-id-alpine/              # Alpine-based Go-ID
│   │   ├── consul-alpine/
│   │   └── ...
│   │
│   └── docker/                        # Docker containers (Phase 2)
│       ├── go-id-alpine/
│       └── ...
│
└── src/main/java/.../automation/
    ├── binary/                        # Binary building automation
    │   ├── BinaryBuilder.java         # Abstract base
    │   ├── goid/
    │   │   └── GoIdBinaryBuilder.java # Go-ID implementation
    │   └── ...
    │
    └── container/                     # Container scaffolding automation
        └── ...
```

---

## Phase 1: Binary Building Automation

### Base Classes (Reusable)

**`BinaryBuilder.java`** - Abstract base class for all binary builders
- Validates prerequisites (Go, Rust, gcc, etc.)
- Prepares build environment
- Generates source code (if needed)
- Installs dependencies
- Builds binary
- Runs automated tests

**`BinaryBuildConfig.java`** - Configuration entity
- Target OS/architecture
- Build flags and environment
- Test settings
- Output paths

**`BinaryTestResult.java`** - Test result entity
- Build time, binary size
- Tests passed/failed
- Error messages

### Implementation Pattern

```java
// 1. Create configuration
BinaryBuildConfig config = new BinaryBuildConfig("go-id", "1");
config.setOutputDirectory("/path/to/standalone-binaries/go-id/go-id-binary-v.1");
config.setTargetOS("linux");
config.setTargetArch("amd64");
config.setTestPort(7001);

// 2. Create builder (language-specific)
GoIdBinaryBuilder builder = new GoIdBinaryBuilder(device, config);

// 3. Build and test
BinaryTestResult result = builder.buildAndTest();

// Result: Tested binary ready at:
// images/standalone-binaries/go-id/go-id-binary-v.1/go-id
```

### Example: Go-ID Binary Builder

Located: `automation/binary/goid/GoIdBinaryBuilder.java`

```bash
# Build binary
mvn exec:java \
  -Dexec.mainClass="com.telcobright.orchestrix.automation.binary.goid.GoIdBinaryBuildRunner" \
  -Dexec.args="1"

# Output:
#   Binary: images/standalone-binaries/go-id/go-id-binary-v.1/go-id
#   Size: 18.5 MB
#   Tests: 5/5 passed
```

---

## Phase 2: Container Scaffolding Automation

### AI Agent Decision Tree

When user requests: **"Scaffold [app] container"**

#### Step 1: Check for Binary
```
Does images/standalone-binaries/[app]/ exist?
├─ YES → Continue to Step 2
└─ NO  → Ask user: "No binary found. Build one first?"
```

#### Step 2: Version Selection
```
How many versions in images/standalone-binaries/[app]/?
├─ 0 versions → Error: "No binary found"
├─ 1 version  → Auto-select that version
└─ 2+ versions → Ask user: "Which version? (1, 2, 3...)"
```

#### Step 3: Container Creation
```
1. Create minimal base container (Alpine for infrastructure, Debian for apps)
2. Copy binary from images/standalone-binaries/[app]/[version]/[binary]
3. Create service file (systemd/OpenRC based on OS)
4. Configure environment variables
5. Export container image
```

### Container Scaffolding Base Class (To Be Created)

```java
public abstract class BinaryContainerScaffolder {

    /**
     * Detect available binary versions
     */
    protected List<String> detectBinaryVersions(String binaryName);

    /**
     * Prompt user if multiple versions
     */
    protected String selectVersion(String binaryName, List<String> versions);

    /**
     * Create container with binary
     */
    public abstract ContainerImage scaffoldContainer(
        String binaryName,
        String version,
        ScaffoldConfig config
    );
}
```

---

## When to Use This Workflow

### ✅ Use Standalone Binaries For:

**Infrastructure Services:**
- Consul, CoreDNS, etcd
- Exporters (Prometheus, metrics)
- Proxies (Nginx, HAProxy)
- Custom Go/Rust microservices

**Benefits:**
- Services that need to be **small and fast**
- Go, Rust, C/C++ statically-linked binaries
- Infrastructure you control (can rebuild easily)

### ❌ Don't Use For:

**Complex Applications:**
- Large Java applications (JVM needed anyway)
- Applications with many dynamic dependencies
- Third-party software you don't control
- Development environments (need full toolchain)

**Use Traditional Container Build Instead:**
- When you need package managers in container
- When software updates via apt/yum
- When binary size doesn't matter

---

## Complete Example: Go-ID Container

### Step 1: Build Binary (Phase 1)

```bash
cd images/standalone-binaries/go-id

# Build version 1 (default)
./build.sh

# Or build specific version
./build.sh 2

# Result:
#   ✓ Binary: go-id-binary-v.1/go-id
#   ✓ Size: 18.5 MB
#   ✓ Tests: 5/5 passed
```

### Step 2: Scaffold Alpine Container (Phase 2)

```bash
# Future automation (to be created):
mvn exec:java \
  -Dexec.mainClass="com.telcobright.orchestrix.automation.container.lxc.BinaryContainerScaffolder" \
  -Dexec.args="go-id alpine"

# AI Agent Actions:
#   1. Detect versions: found go-id-binary-v.1
#   2. Auto-select v.1 (only version)
#   3. Create Alpine container
#   4. Copy binary (18.5 MB)
#   5. Create OpenRC service
#   6. Export: go-id-alpine (total: ~25 MB)
```

### Step 3: Deploy

```bash
# Same deployment as before - binary runs inside Alpine
lxc launch go-id-alpine my-go-id-instance
```

---

## Size Comparison

### Traditional Approach (Build in Container)
```
Debian 12 base:        105 MB
+ Go installation:     150 MB
+ Build dependencies:   50 MB
+ Source code:          5 MB
+ Built binary:         18 MB
─────────────────────────────
Total (compressed):    169 MB
```

### Standalone Binary Approach
```
Alpine base:            5 MB
+ Pre-built binary:    18 MB
─────────────────────────────
Total (compressed):    25 MB

Savings: 144 MB (85% reduction!)
```

---

## AI Agent Instructions

### When User Says: "Build [app] binary"

1. Check if `images/standalone-binaries/[app]/build.sh` exists
2. If not, check if `automation/binary/[app]/` exists
   - If automation exists, create build.sh wrapper
   - If not, ask: "No builder found. Create one?"
3. Run build.sh script
4. Report results: binary path, size, tests

**Example:**
```bash
cd images/standalone-binaries/go-id
./build.sh          # Build version 1
./build.sh 2        # Build version 2
```

### When User Says: "Scaffold [app] container"

1. Check `images/standalone-binaries/[app]/`
   - If missing → "No binary found. Build one first?"
   - If found → Continue

2. List versions, prompt if needed

3. Ask: "Container type? (lxc-alpine, lxc-debian, docker, podman)"

4. Create container scaffolding automation if needed

5. Execute scaffolding

6. Report: container image, size, ready for deployment

### When User Says: "Deploy [app]"

Use existing deployment automation (same as before)

---

## Migration Guide

### Existing Debian-Based Containers → Alpine + Binary

**Before:**
```
images/lxc/go-id/               # Debian 12, builds Go inside
  └── build script creates 169 MB container
```

**After:**
```
images/standalone-binaries/go-id/
  └── go-id-binary-v.1/         # 18 MB tested binary
      └── go-id

images/lxc/go-id-alpine/        # Alpine, copies binary
  └── scaffold script creates 25 MB container
```

**Migration Steps:**
1. Build binary: `GoIdBinaryBuildRunner`
2. Create new Alpine scaffolder
3. Test Alpine container
4. Deprecate old Debian build
5. Update documentation

---

## Best Practices

### Binary Building

1. **Always test locally** before containerization
2. **Use static linking** (CGO_ENABLED=0 for Go)
3. **Version binaries** (v.1, v.2, v.3...)
4. **Store test results** with binary
5. **Tag binaries** with git commit hash

### Container Scaffolding

1. **Alpine for infrastructure** (Consul, DNS, proxies)
2. **Debian for applications** (if needed)
3. **Minimal base images** (no build tools)
4. **Copy only binary** (no source code)
5. **Use multi-stage if needed** (Docker)

### Security

1. **Scan binaries** before containerization
2. **Run as non-root** in container
3. **Read-only filesystems** where possible
4. **Minimal attack surface** (no compilers in container)

---

## Troubleshooting

### Binary Tests Fail

**Problem:** Tests fail during binary build

**Solution:**
1. Check prerequisites (Go version, dependencies)
2. Review test output in BinaryTestResult
3. Test binary manually: `./go-id &`
4. Check ports not in use

### Multiple Versions Confusion

**Problem:** AI agent doesn't know which version to use

**Solution:**
1. AI should list all versions
2. Ask user to select
3. Default to latest if user doesn't care

### Binary Not Found During Scaffolding

**Problem:** Scaffolder can't find binary

**Solution:**
1. Check path: `images/standalone-binaries/[app]/[version]/[binary-name]`
2. Verify binary exists: `ls -lh [path]`
3. Build binary first if missing

---

## Future Enhancements

- [ ] Automatic binary caching (by git hash)
- [ ] Binary signing and verification
- [ ] Multi-architecture builds (ARM64)
- [ ] Docker multi-stage build integration
- [ ] Binary update automation
- [ ] Rollback to previous versions
- [ ] Performance benchmarking

---

## Summary

**Two-Phase Approach:**
1. **Build binaries locally** → Test → Store in `images/standalone-binaries/`
2. **Scaffold containers** → Copy binary → Export minimal image

**Key Benefits:**
- 85% smaller containers
- Tested before containerization
- Reusable across container technologies
- Faster builds and deployments

**AI Agent Role:**
- Build binaries when requested
- Detect available binaries when scaffolding
- Prompt for versions when multiple exist
- Create minimal containers with pre-built binaries
