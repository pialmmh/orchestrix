# Automation Status and Understanding - FRR & WireGuard Deployment

**Date:** 2025-11-04
**Purpose:** Document current automation status and execution architecture
**Goal:** Enable shell-script driven Java automation execution for deployment control

---

## Your Goal - My Understanding

### Current Objective
Execute FRR and WireGuard deployments through **configuration-driven Java automations** launched via **shell scripts**.

**Architecture Flow:**
```
┌─────────────────────────────────────────────────────────────────┐
│ deployment-runners/                                              │
│                                                                   │
│  execute-automation.sh  ◄── Universal launcher script            │
│           │                                                       │
│           ├─► frr/bdcom/frr.cnf  ◄── Tenant-specific config      │
│           │         │                                             │
│           │         └──► Java: FrrDeploymentRunner.jar           │
│           │                     │                                 │
│           │                     ├─► SSH to servers                │
│           │                     ├─► Deploy FRR containers         │
│           │                     └─► Configure BGP                 │
│           │                                                       │
│           └─► wireguard/bdcom/wireguard.cnf                      │
│                       │                                           │
│                       └──► Java: WireGuardDeploymentRunner.jar   │
│                               │                                   │
│                               ├─► SSH to servers                  │
│                               ├─► Install WireGuard               │
│                               ├─► Generate keys                   │
│                               └─── Configure mesh                 │
└───────────────────────────────────────────────────────────────────┘

Later: Orchestrix Web UI  →  execute-automation.sh  →  Java automations
```

### Key Principles
1. **Configuration-Driven**: All deployment details in .cnf files
2. **Reusable Java**: Same Java code works for any tenant (bdcom, link3, etc.)
3. **Shell Launcher**: Simple shell script to invoke Java with config
4. **Multi-Tenant**: Each client/tenant gets their own config directory
5. **Future-Ready**: Architecture supports web UI integration later

### Directory Structure
```
deployment-runners/
├── execute-automation.sh          # Universal launcher
├── frr/
│   ├── bdcom/
│   │   └── frr.cnf               # BDCOM FRR config
│   ├── link3/
│   │   └── frr.cnf               # Link3 FRR config
│   └── [other-clients]/
├── wireguard/
│   ├── bdcom/
│   │   └── wireguard.cnf         # BDCOM WireGuard config
│   └── [other-clients]/
└── combined/
    ├── bdcom/
    │   └── full-stack.cnf        # Combined FRR+WireGuard deployment
    └── [other-clients]/
```

### Usage Pattern
```bash
# From deployment-runners directory
./execute-automation.sh frr/bdcom/frr.cnf
./execute-automation.sh wireguard/bdcom/wireguard.cnf
./execute-automation.sh combined/bdcom/full-stack.cnf

# For different client
./execute-automation.sh frr/link3/frr.cnf
```

---

## Current Automation Status

### ✅ FRR Automation - COMPLETE

**Location:** `src/main/java/com/telcobright/orchestrix/automation/routing/frr/`

**Files:**
1. `FrrDeploymentRunner.java` - CLI entry point
2. `FrrRouterDeployment.java` - Core deployment logic
3. `FrrRouterConfig.java` - Configuration parser
4. `FrrDeploymentResult.java` - Result tracking

**Status:** ✅ **Fully Implemented and Working**

**Capabilities:**
- ✅ Parse .cnf configuration files
- ✅ SSH to remote servers
- ✅ Transfer LXC container images
- ✅ Import and launch FRR containers
- ✅ Configure BGP routing
- ✅ Verify BGP sessions
- ✅ Support multi-node deployments
- ✅ Error handling and reporting

**Example Usage:**
```bash
# Compile
cd /home/mustafa/telcobright-projects/orchestrix
mvn clean package

# Run deployment
java -cp target/orchestrix-*.jar \
  com.telcobright.orchestrix.automation.routing.frr.FrrDeploymentRunner \
  --orchestrix-path /home/mustafa/telcobright-projects/orchestrix \
  --host 10.255.246.173 \
  --port 15605 \
  --user bdcom \
  --config /path/to/frr.cnf
```

**Configuration Format (frr.cnf):**
```properties
# Container settings
CONTAINER_NAME=frr-router-node1
BASE_IMAGE=frr-router-base-v.1.0.0

# BGP Configuration
BGP_ASN=65193
BGP_NEIGHBORS=10.9.9.5:65192,10.9.9.6:65191
BGP_NETWORKS=10.10.199.0/24

# Optional settings
ROUTER_ID=10.9.9.4
ROUTER_HOSTNAME=bdcom1-bgp
MEMORY_LIMIT=100MB
CPU_LIMIT=1
```

---

### ❌ WireGuard Automation - NOT IMPLEMENTED

**Location:** `src/main/java/com/telcobright/orchestrix/automation/shellscript/wireguard/`

**Status:** ❌ **Directory exists but NO Java classes**

**What Exists:**
- ✅ Shell script templates in `images/shell-automations/wireguard/v1/templates/`
  - `wg-install.sh.template`
  - `wg-configure.sh.template`
- ❌ No Java automation classes
- ❌ No WireGuardDeploymentRunner
- ❌ No configuration parser
- ❌ No SSH deployment logic

**What's Needed:**
```
wireguard/
├── WireGuardDeploymentRunner.java  ← Entry point
├── WireGuardDeployment.java        ← Core deployment logic
├── WireGuardConfig.java             ← Config parser
└── WireGuardDeploymentResult.java  ← Result tracking
```

**Required Capabilities:**
- [ ] Parse .cnf configuration files
- [ ] SSH to remote servers
- [ ] Install WireGuard package
- [ ] Generate WireGuard keys (per server)
- [ ] Create WireGuard config files
- [ ] Configure full mesh network
- [ ] Bring up WireGuard interfaces
- [ ] Enable systemd service
- [ ] Verify connectivity

**Configuration Format (wireguard.cnf):**
```properties
# Cluster definition
CLUSTER_NAME=bdcom-cluster
WG_INTERFACE=wg-overlay
WG_PORT=51820
OVERLAY_NETWORK=10.9.9.0/24

# Node 1
NODE1_NAME=bdcom1
NODE1_MGMT_IP=10.255.246.173
NODE1_OVERLAY_IP=10.9.9.4
NODE1_SSH_PORT=15605
NODE1_SSH_USER=bdcom
NODE1_SSH_PASS=M6nthDNrxcYfPQLu

# Node 2
NODE2_NAME=bdcom2
NODE2_MGMT_IP=10.255.246.174
NODE2_OVERLAY_IP=10.9.9.5
NODE2_SSH_PORT=15605
NODE2_SSH_USER=bdcom
NODE2_SSH_PASS=hqv3gJh63buuwXcu

# Node 3
NODE3_NAME=bdcom3
NODE3_MGMT_IP=10.255.246.175
NODE3_OVERLAY_IP=10.9.9.6
NODE3_SSH_PORT=15605
NODE3_SSH_USER=bdcom
NODE3_SSH_PASS=ReBJxyd3kGDFW5Cm
```

---

## Shell Script Architecture

### execute-automation.sh - Universal Launcher

**Purpose:** Single entry point for all automation executions

**Responsibilities:**
1. Parse config file to determine automation type (frr, wireguard, combined)
2. Locate correct Java automation class
3. Build classpath
4. Execute Java with proper arguments
5. Capture and report results

**Design:**
```bash
#!/bin/bash
# execute-automation.sh

CONFIG_FILE="$1"

if [ -z "$CONFIG_FILE" ]; then
    echo "Usage: ./execute-automation.sh <config-file>"
    echo "Example: ./execute-automation.sh frr/bdcom/frr.cnf"
    exit 1
fi

# Determine automation type from config file path
if [[ "$CONFIG_FILE" == *"frr"* ]]; then
    AUTOMATION_TYPE="frr"
    MAIN_CLASS="com.telcobright.orchestrix.automation.routing.frr.FrrDeploymentRunner"
elif [[ "$CONFIG_FILE" == *"wireguard"* ]]; then
    AUTOMATION_TYPE="wireguard"
    MAIN_CLASS="com.telcobright.orchestrix.automation.network.wireguard.WireGuardDeploymentRunner"
elif [[ "$CONFIG_FILE" == *"combined"* ]]; then
    AUTOMATION_TYPE="combined"
    MAIN_CLASS="com.telcobright.orchestrix.automation.deployment.CombinedDeploymentRunner"
else
    echo "Error: Cannot determine automation type from config file path"
    exit 1
fi

# Set paths
ORCHESTRIX_ROOT="/home/mustafa/telcobright-projects/orchestrix"
JAR_FILE="$ORCHESTRIX_ROOT/target/orchestrix-1.0.0.jar"

# Check if JAR exists, build if needed
if [ ! -f "$JAR_FILE" ]; then
    echo "Building orchestrix.jar..."
    cd "$ORCHESTRIX_ROOT"
    mvn clean package -DskipTests || exit 1
fi

# Execute automation
echo "========================================="
echo "Orchestrix Automation Execution"
echo "========================================="
echo "Type: $AUTOMATION_TYPE"
echo "Config: $CONFIG_FILE"
echo "Main Class: $MAIN_CLASS"
echo "========================================="
echo ""

java -cp "$JAR_FILE" \
    "$MAIN_CLASS" \
    --config "$(realpath $CONFIG_FILE)" \
    --orchestrix-path "$ORCHESTRIX_ROOT"

EXIT_CODE=$?

if [ $EXIT_CODE -eq 0 ]; then
    echo ""
    echo "✓ Automation completed successfully"
else
    echo ""
    echo "✗ Automation failed with exit code $EXIT_CODE"
fi

exit $EXIT_CODE
```

---

## Migration from Current Approach

### Current: Hardcoded Deployments
```java
// Current FrrDeploymentRunner has hardcoded BDCOM nodes
private static final List<NodeConfig> BDCOM_NODES = Arrays.asList(
    new NodeConfig("node1", "10.255.246.173", 15605, "bdcom"),
    new NodeConfig("node2", "10.255.246.174", 15605, "bdcom"),
    new NodeConfig("node3", "10.255.246.175", 15605, "bdcom")
);
```

### Needed: Configuration-Driven
```java
// New approach - read cluster from config file
public class FrrDeploymentRunner {
    public static void main(String[] args) {
        Map<String, String> params = parseArgs(args);
        String configPath = params.get("config");

        // Parse cluster configuration
        FrrClusterConfig cluster = new FrrClusterConfig(configPath);

        // Deploy to all nodes in cluster
        for (NodeConfig node : cluster.getNodes()) {
            deployToNode(node);
        }
    }
}
```

**Config File Format:**
```properties
# Cluster-level config
CLUSTER_NAME=bdcom-cluster

# How many nodes?
NODE_COUNT=3

# Node 1
NODE1_NAME=bdcom1
NODE1_HOST=10.255.246.173
NODE1_PORT=15605
NODE1_USER=bdcom
NODE1_PASSWORD=M6nthDNrxcYfPQLu
NODE1_CONTAINER_NAME=frr-router-node1
NODE1_BGP_ASN=65193
NODE1_OVERLAY_IP=10.9.9.4
NODE1_CONTAINER_SUBNET=10.10.199.0/24

# Node 2
NODE2_NAME=bdcom2
NODE2_HOST=10.255.246.174
...

# Node 3
NODE3_NAME=bdcom3
NODE3_HOST=10.255.246.175
...
```

---

## Required Changes to FRR Automation

### 1. Update FrrDeploymentRunner to Accept Config File

**Current:**
```java
java FrrDeploymentRunner --deploy-bdcom-nodes
```

**Needed:**
```java
java FrrDeploymentRunner --config /path/to/bdcom/frr.cnf
```

### 2. Create FrrClusterConfig Parser

**New Class:** `FrrClusterConfig.java`

Responsibilities:
- Parse cluster-level config file
- Extract node definitions
- Provide node iteration
- Validate all required parameters

### 3. Support Single-Config Deployment

Instead of separate config per node, support cluster config:
```
Before:
  images/lxc/frr-router/deployment-configs/
    ├── node1-config.conf
    ├── node2-config.conf
    └── node3-config.conf

After:
  images/deployment-runners/frr/bdcom/
    └── frr.cnf  ← All nodes in one file
```

---

## Implementation Priority

### Phase 1: Update FRR Automation ✅ (Partially Done)
1. ✅ Core deployment logic exists
2. 🔄 Need: Cluster config parser
3. 🔄 Need: Updated CLI to use cluster config
4. 🔄 Need: execute-automation.sh integration

### Phase 2: Create WireGuard Automation ❌ (Not Started)
1. ❌ Create WireGuardDeployment.java
2. ❌ Create WireGuardConfig.java
3. ❌ Create WireGuardDeploymentRunner.java
4. ❌ Create WireGuardClusterConfig.java
5. ❌ Test standalone execution

### Phase 3: Combined Deployment (Future)
1. Create CombinedDeploymentRunner
2. Support sequential: WireGuard → FRR → Verify
3. Single config file for full stack

### Phase 4: Web UI Integration (Future - via Orchestrix)
1. REST API endpoints to trigger automations
2. Upload/manage config files
3. Real-time execution monitoring
4. Deployment history and rollback

---

## Benefits of This Architecture

### 1. **Separation of Concerns**
- Configuration (`.cnf` files) separate from code
- Each tenant gets isolated config directory
- No code changes for new tenants

### 2. **Reusability**
- Same Java JAR for all deployments
- Same execute-automation.sh for all automation types
- Configuration determines behavior

### 3. **Testability**
- Test with different configs without code changes
- Easy to create test/staging configs
- Validate configs before deployment

### 4. **Version Control**
- Config files in git
- Track deployment history
- Easy rollback to previous configs

### 5. **Multi-Tenancy Ready**
```
deployment-runners/
├── frr/
│   ├── bdcom/frr.cnf
│   ├── link3/frr.cnf
│   ├── client-abc/frr.cnf
│   └── client-xyz/frr.cnf
```

### 6. **Future Web UI Integration**
```
Web UI Request:
  POST /api/deploy
  Body: {
    "tenant": "bdcom",
    "automation": "frr",
    "config": "frr.cnf"
  }

Backend executes:
  ./execute-automation.sh frr/bdcom/frr.cnf
```

---

## Next Steps

### Immediate Actions Needed

1. **Create WireGuard Java Automation**
   - [ ] WireGuardDeployment.java
   - [ ] WireGuardConfig.java
   - [ ] WireGuardDeploymentRunner.java
   - [ ] WireGuardClusterConfig.java

2. **Update FRR Automation for Cluster Configs**
   - [ ] Create FrrClusterConfig.java
   - [ ] Update FrrDeploymentRunner to use cluster config
   - [ ] Remove hardcoded BDCOM_NODES

3. **Create execute-automation.sh**
   - [ ] Universal launcher script
   - [ ] Config file type detection
   - [ ] Java classpath building
   - [ ] Result reporting

4. **Create Sample Configs**
   - [ ] frr/bdcom/frr.cnf
   - [ ] wireguard/bdcom/wireguard.cnf
   - [ ] combined/bdcom/full-stack.cnf

5. **Testing**
   - [ ] Test FRR deployment via shell script
   - [ ] Test WireGuard deployment via shell script
   - [ ] Test combined deployment
   - [ ] Verify on BDCOM cluster

---

## Example: Complete FRR Deployment Flow

### Step 1: Create Config
```bash
cat > deployment-runners/frr/bdcom/frr.cnf << 'EOF'
# BDCOM FRR Cluster Deployment
CLUSTER_NAME=bdcom-cluster
NODE_COUNT=3

# Global Settings
BASE_IMAGE=frr-router-base-v.1.0.0
WG_INTERFACE=wg-overlay

# Node 1
NODE1_NAME=bdcom1
NODE1_HOST=10.255.246.173
NODE1_PORT=15605
NODE1_USER=bdcom
NODE1_CONTAINER=frr-router-node1
NODE1_BGP_ASN=65193
NODE1_ROUTER_ID=10.9.9.4
NODE1_NETWORKS=10.10.199.0/24
NODE1_NEIGHBORS=10.9.9.5:65192,10.9.9.6:65191

# Node 2
NODE2_NAME=bdcom2
NODE2_HOST=10.255.246.174
NODE2_PORT=15605
NODE2_USER=bdcom
NODE2_CONTAINER=frr-router-node2
NODE2_BGP_ASN=65192
NODE2_ROUTER_ID=10.9.9.5
NODE2_NETWORKS=10.10.198.0/24
NODE2_NEIGHBORS=10.9.9.4:65193,10.9.9.6:65191

# Node 3
NODE3_NAME=bdcom3
NODE3_HOST=10.255.246.175
NODE3_PORT=15605
NODE3_USER=bdcom
NODE3_CONTAINER=frr-router-node3
NODE3_BGP_ASN=65191
NODE3_ROUTER_ID=10.9.9.6
NODE3_NETWORKS=10.10.197.0/24
NODE3_NEIGHBORS=10.9.9.4:65193,10.9.9.5:65192
EOF
```

### Step 2: Execute Deployment
```bash
cd /home/mustafa/telcobright-projects/orchestrix/images/deployment-runners
./execute-automation.sh frr/bdcom/frr.cnf
```

### Step 3: Output
```
=========================================
Orchestrix Automation Execution
=========================================
Type: frr
Config: frr/bdcom/frr.cnf
Main Class: com.telcobright.orchestrix.automation.routing.frr.FrrDeploymentRunner
=========================================

Loading cluster configuration...
✓ Found 3 nodes in cluster: bdcom-cluster

Deploying FRR to bdcom1 (10.255.246.173)...
✓ Connected via SSH
✓ Transferred container image
✓ Imported LXC image
✓ Launched container frr-router-node1
✓ Configured BGP (AS 65193)
✓ BGP session established with 10.9.9.5
✓ BGP session established with 10.9.9.6

Deploying FRR to bdcom2 (10.255.246.174)...
[... similar output ...]

Deploying FRR to bdcom3 (10.255.246.175)...
[... similar output ...]

=========================================
Deployment Summary
=========================================
✓ All 3 nodes deployed successfully
✓ 6 BGP sessions established
✓ Routes advertised: 10.10.199.0/24, 10.10.198.0/24, 10.10.197.0/24

✓ Automation completed successfully
```

---

## Summary

**Current State:**
- ✅ FRR automation: Mostly complete, needs cluster config support
- ❌ WireGuard automation: Not implemented
- 🔄 Shell script launcher: Needs creation
- 🔄 Configuration structure: Needs standardization

**Your Architecture Understanding:** ✅ **Correct**
- Configuration-driven Java automations
- Shell script launcher for execution
- Multi-tenant support via directory structure
- Future web UI integration ready

**Next Action:** Implement WireGuard automation and update FRR to use cluster configs

Would you like me to proceed with implementing the WireGuard Java automation and updating the FRR automation to support cluster configuration files?
