# Standalone Binary Container Workflow

**AI Agent Guidelines for Building Containers with Pre-Built Binaries**

This workflow applies to: **LXC, Docker, Podman, and any container technology**

## Overview

**Two INDEPENDENT operations:**

### Phase 1: Binary Building (Standalone Operation)
Build or download binaries. **This is the end of the operation.**
```
Automation either:
  - Builds binary automatically (Java automation)
  - Downloads existing binary from repository

Result: Binary at standalone-binaries/go-id/go-id-binary-v.1/go-id
Operation complete. No containers involved.
NO BUILD SCRIPT in standalone-binaries/ directory.
```

### Phase 2: Container Scaffolding (Separate Operation)
Later, when needed, create containers that use existing binaries.
```bash
# User: "Scaffold go-id container based on existing binary"
# AI: Checks images/standalone-binaries/go-id/ for versions
# AI: "Found versions: v.1, v.2. Which version?"
# User: "v.1"
# AI: Creates images/lxc/go-id-alpine/ with binary from v.1
```

**Benefits:**
- 🚀 **85% smaller containers** (Alpine 25 MB vs Debian 169 MB)
- ⚡ **Binary tested before containerization**
- 🔄 **Reusable** (same binary → multiple container types)
- 📦 **Independent** (build binaries without containers)

---

## Architecture

### Phase 1: Binary Building (Standalone Operation)
```
┌─────────────────────────────────────────────────────────────┐
│  Binary Building - INDEPENDENT OPERATION                     │
│  Location: images/standalone-binaries/[app]/                │
├─────────────────────────────────────────────────────────────┤
│  NO BUILD SCRIPT - Automation handles this                  │
│                                                              │
│  Automation either:                                         │
│    Option 1: Build automatically                            │
│      1. Generate source code                                │
│      2. Compile static binary (CGO_ENABLED=0)               │
│      3. Run automated tests                                 │
│      4. Store: standalone-binaries/[app]/[version]/         │
│                                                              │
│    Option 2: Download from repository                       │
│      1. Download release binary                             │
│      2. Verify checksum                                     │
│      3. Store: standalone-binaries/[app]/[version]/         │
│                                                              │
│  OPERATION COMPLETE. Binary ready.                          │
│  No containers created. No build script.                    │
└─────────────────────────────────────────────────────────────┘

User can now:
  - Use binary directly
  - Create containers later (separate operation)
  - Request different versions
```

### Phase 2: Container Scaffolding (Separate Operation)
```
┌─────────────────────────────────────────────────────────────┐
│  Container Scaffolding - REQUIRES EXISTING BINARY            │
│  Location: images/lxc/, images/docker/, etc.                │
├─────────────────────────────────────────────────────────────┤
│  Prerequisite: Binary must exist in standalone-binaries/    │
│                                                              │
│  AI creates container scaffolding with:                     │
│    - build.sh (copies existing binary, builds container)   │
│    - launch.sh (launches container instances)              │
│    - sample-config.conf                                     │
│    - README.md                                              │
│                                                              │
│  User then runs:                                            │
│    ./build.sh  → Creates container image with binary       │
│    ./launch.sh → Deploys container instances               │
│                                                              │
│  FOLLOWS CONTAINER SCAFFOLDING CONVENTIONS                  │
└─────────────────────────────────────────────────────────────┘

Container build.sh workflow:
  1. Copy binary from standalone-binaries/[app]/[version]/
  2. Create Alpine container base
  3. Install binary at /usr/local/bin/
  4. Create service file (OpenRC/systemd)
  5. Export container image

These are INDEPENDENT operations, not a pipeline.
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

## Complete Example: Go-ID Binary and Container

### Operation 1: Build Binary (Standalone)

**User:** "Build go-id binary"

**AI:** Runs automation directly (no build script)
```
Running GoIdBinaryBuildRunner automation...
  - Generating Go source code
  - Installing dependencies (sonyflake, gorilla/mux, consul)
  - Compiling static binary (CGO_ENABLED=0)
  - Running tests (5 tests)

✓ Binary: standalone-binaries/go-id/go-id-binary-v.1/go-id
✓ Size: 18.5 MB
✓ Tests: 5/5 passed
Operation complete.
```

**End of operation.** Binary is ready, tested, and stored. Can be used directly or containerized later.
**NO build.sh created** in standalone-binaries/.

---

### Operation 2: Scaffold Container (Separate, Later)

**User:** "Scaffold go-id container based on existing binary"

**AI Agent:**
```
Checking for go-id binaries...
Found versions: v.1
Using go-id binary v.1. Proceed? (yes/no)
```

**User:** "yes"

**AI Agent:**
```
Understanding:
  - Create LXC Alpine container: images/lxc/go-id-alpine/
  - Use binary: images/standalone-binaries/go-id/go-id-binary-v.1/go-id
  - Container size: ~25 MB (Alpine + binary)
  Confirm? (yes/no)
```

**User:** "yes"

**AI Agent:** [Creates container scaffolding following CONTAINER_SCAFFOLD_TEMPLATE.md]
```
Created: images/lxc/go-id-alpine/
  - buildGoIdAlpine.sh         # Builds container with existing binary
  - launchGoIdAlpine.sh        # Launches instances
  - sample-config.conf          # Configuration
  - README.md                   # Documentation

Container scaffolding ready.
```

**What buildGoIdAlpine.sh does:**
1. Copies binary from `standalone-binaries/go-id/go-id-binary-v.1/go-id`
2. Creates Alpine container base image
3. Installs binary into container at `/usr/local/bin/go-id`
4. Creates OpenRC service file
5. Exports container image: `go-id-alpine`

**Note:** buildGoIdAlpine.sh does NOT compile Go code. It uses the pre-built binary.

---

### Operation 3: Build Container Image

```bash
# Build container (copies existing binary into Alpine)
cd images/lxc/go-id-alpine
./buildGoIdAlpine.sh
```

### Operation 4: Deploy Container

```bash
# Launch container instance
cd images/lxc/go-id-alpine
./launchGoIdAlpine.sh config.conf
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

### When User Says: "Build [app] binary" or "Get [app] binary"

**This is a standalone operation. No build script involved.**

1. Check if automation exists: `automation/binary/[app]/`
   - If exists: Run automation to build binary
   - If not exists: Ask "Download from repository or create builder?"

2. **If building automatically:**
   - Run Java automation (e.g., GoIdBinaryBuildRunner)
   - Binary is compiled, tested, and stored
   - No shell script executed

3. **If downloading:**
   - Download from artifact repository
   - Verify checksum
   - Store in `standalone-binaries/[app]/[version]/`

4. Report results: binary path, size, tests (if built)
5. **Operation complete.** No container scaffolding.

**Example (automatic build):**
```
User: "Build go-id binary version 1"

AI: Running binary builder automation...
    [Compiles Go code, runs tests]
    ✓ Binary: standalone-binaries/go-id/go-id-binary-v.1/go-id
    ✓ Size: 18.5 MB
    ✓ Tests: 5/5 passed
    Operation complete.
```

**NO build.sh script** in standalone-binaries directory.

---

### When User Says: "Scaffold [app] container based on existing binary"

**This is a separate operation. Requires binary to exist first.**

1. **Check for binary existence:**
   ```bash
   ls images/standalone-binaries/[app]/
   ```

2. **If no binary found:**
   - Respond: "No binary found for [app]. Build one first?"
   - Stop. Do not proceed.

3. **If binary found - detect versions:**
   ```bash
   # Example: Found go-id-binary-v.1/ and go-id-binary-v.2/
   ```

4. **If only 1 version:**
   - Auto-select that version
   - Confirm with user: "Using [app] binary v.X. Proceed?"

5. **If multiple versions:**
   - List versions to user
   - Ask: "Which version? (1, 2, 3...)"
   - Wait for user selection

6. **Discuss understanding:**
   - "I will create [container-type] container in images/[type]/[app]-alpine/"
   - "Using binary from images/standalone-binaries/[app]/[app]-binary-v.X/"
   - "Container will be ~25 MB (Alpine + binary)"
   - Ask: "Confirm?"

7. **After confirmation:**
   - Follow container scaffolding guidelines
   - Create in appropriate location (images/lxc/, images/docker/, etc.)
   - Copy binary from standalone-binaries
   - Create service files, configs, scripts
   - Report: container location, size, ready for deployment

**Example Flow:**
```
User: "Scaffold go-id container based on existing binary"

AI: Checking for go-id binaries...
AI: Found versions: v.1, v.2
AI: Which version? (1 or 2)

User: "1"

AI: Understanding:
    - Create LXC Alpine container: images/lxc/go-id-alpine/
    - Use binary: images/standalone-binaries/go-id/go-id-binary-v.1/go-id
    - Container size: ~25 MB
    - Confirm?

User: "yes"

AI: [Creates container scaffolding]
AI: Container ready at images/lxc/go-id-alpine/
```

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
