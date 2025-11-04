# LXC Prerequisites Automation

Automated setup and verification of LXC/LXD prerequisites for container deployments.

## Overview

The LXC Prerequisites Manager automatically checks and creates all necessary infrastructure for LXC container deployments:

- ✅ LXD installation verification
- ✅ LXD initialization
- ✅ Default storage pool (`default`)
- ✅ Default network bridge (`lxdbr0`)
- ✅ System readiness verification

## Files

- **Java Class**: `automation/api/infrastructure/LxcPrerequisitesManager.java`
- **Wrapper Script**: `automation/scripts/setup-lxc.sh`

## Usage

### Local Execution

```bash
cd /path/to/orchestrix/automation/scripts
./setup-lxc.sh
```

### Remote SSH Execution

```bash
./setup-lxc.sh <host> <user> <port> <password> [privateKeyPath]
```

**Example:**
```bash
./setup-lxc.sh 192.168.1.100 ubuntu 22 mypassword
./setup-lxc.sh 192.168.1.100 ubuntu 22 "" /path/to/key.pem
```

## What It Checks

### 1. LXD Installation
```bash
✓ LXD installed
```
Verifies that LXD is installed on the system (via snap).

**If not installed**: Reports error and suggests installation command.

### 2. LXD Initialization
```bash
✓ LXD already initialized
```
Checks if LXD has been initialized with default configuration.

**If not initialized**: Automatically initializes LXD with:
- Storage pool: `default` (dir driver)
- Network: `lxdbr0` (10.10.199.1/24)
- Default profile with eth0 and root disk

### 3. Storage Pool
```bash
✓ Storage pool 'default' exists
  Driver: dir
  Source: /var/snap/lxd/common/lxd/storage-pools/default
  Used by: 6 containers
```
Verifies the default storage pool exists and shows details.

**If not found**: Creates a new `default` storage pool with dir driver.

### 4. Network Bridge
```bash
✓ Network bridge 'lxdbr0' exists
  IPv4: 10.10.199.1/24
  IPv6: fd42:9836:e5dc:4e38::1/64
  Used by: 6 containers
```
Verifies the default network bridge exists with proper configuration.

**If not found**: Creates `lxdbr0` with:
- IPv4: 10.10.199.1/24
- NAT enabled
- IPv6 disabled

### 5. System Readiness
```bash
✓ Storage and network prerequisites verified
✓ System ready for LXC deployments
```
Confirms all prerequisites are met.

## Example Output

### Successful Run (All Prerequisites Met)

```
═══════════════════════════════════════════════════════
  LXC Prerequisites Setup - Local Execution
═══════════════════════════════════════════════════════

═══════════════════════════════════════════════════════
  LXC/LXD Prerequisites Check
═══════════════════════════════════════════════════════

Host: localhost

1. Checking LXD Installation
───────────────────────────────────────────────────────
✓ LXD installed

2. Checking LXD Initialization
───────────────────────────────────────────────────────
✓ LXD already initialized

3. Checking Storage Pool
───────────────────────────────────────────────────────
✓ Storage pool 'default' exists
  Driver: dir
  Source: /var/snap/lxd/common/lxd/storage-pools/default
  Used by: 6 containers

4. Checking Network Bridge
───────────────────────────────────────────────────────
✓ Network bridge 'lxdbr0' exists
  IPv4: 10.10.199.1/24
  IPv6: fd42:9836:e5dc:4e38::1/64
  Used by: 6 containers

5. System Readiness Check
───────────────────────────────────────────────────────
  ✓ Storage and network prerequisites verified
  ✓ System ready for LXC deployments
✓ System ready for LXC deployments

═══════════════════════════════════════════════════════
  All Prerequisites Met ✓
═══════════════════════════════════════════════════════

✓ Prerequisites setup completed successfully
```

### First-Time Setup (Initialization Needed)

```
═══════════════════════════════════════════════════════
  LXC/LXD Prerequisites Check
═══════════════════════════════════════════════════════

Host: newserver.example.com

1. Checking LXD Installation
───────────────────────────────────────────────────────
✓ LXD installed

2. Checking LXD Initialization
───────────────────────────────────────────────────────
⚠ LXD not initialized, initializing now...
✓ LXD initialized

3. Checking Storage Pool
───────────────────────────────────────────────────────
✓ Storage pool 'default' exists
  Driver: dir
  Source: /var/snap/lxd/common/lxd/storage-pools/default
  Used by: 0 containers

4. Checking Network Bridge
───────────────────────────────────────────────────────
✓ Network bridge 'lxdbr0' exists
  IPv4: 10.10.199.1/24
  IPv6: none
  Used by: 0 containers

5. System Readiness Check
───────────────────────────────────────────────────────
  ✓ Storage and network prerequisites verified
  ✓ System ready for LXC deployments
✓ System ready for LXC deployments

═══════════════════════════════════════════════════════
  All Prerequisites Met ✓
═══════════════════════════════════════════════════════
```

## Return Codes

- **0**: All prerequisites met successfully
- **1**: Prerequisites check or setup failed

## Dependencies

- **Java 11+**: For running the automation
- **jsch.jar**: For SSH connections (Maven: `com.github.mwiede:jsch:0.2.20`)
- **LXD/LXC**: Target system must have LXD available (snap)

## Technical Details

### Package Structure

```
automation/
├── api/
│   └── infrastructure/
│       ├── LxcPrerequisitesManager.java
│       └── LxcPrerequisitesManager.class
└── scripts/
    └── setup-lxc.sh
```

### Java Class Features

- **Automatic compilation**: Script compiles Java if source is newer
- **Local execution**: Direct bash commands
- **SSH execution**: JSch-based remote execution
- **Idempotent**: Safe to run multiple times
- **Non-destructive**: Only creates missing components
- **Detailed logging**: Full execution log returned

### Configuration

All configuration is hardcoded for consistency:

- **Storage Pool**: `default` (dir driver)
- **Network**: `lxdbr0` (10.10.199.1/24)
- **IPv6**: Disabled
- **NAT**: Enabled on network

## Integration with Deployment

This automation is meant to be run before any LXC container deployments to ensure the system is ready.

**Example deployment workflow:**
```bash
# Step 1: Ensure prerequisites
./setup-lxc.sh

# Step 2: Deploy containers
./deploy-consul-cluster.sh
./deploy-go-id.sh
```

## Troubleshooting

### "LXD not installed"

```
ERROR: LXD is not installed. Please install: snap install lxd
```

**Solution**: Install LXD via snap:
```bash
sudo snap install lxd
```

### "jsch.jar not found"

```
ERROR: jsch.jar not found in Maven repository
Please install: mvn dependency:get -Dartifact=com.github.mwiede:jsch:0.2.20
```

**Solution**: Download jsch via Maven:
```bash
mvn dependency:get -Dartifact=com.github.mwiede:jsch:0.2.20
```

### SSH Connection Failed

If SSH fails:
1. Check SSH credentials
2. Verify SSH service is running on target
3. Test manual SSH connection first
4. Check firewall rules

## See Also

- [Container Scaffolding Standard](../images/containers/lxc/CONTAINER_SCAFFOLDING_STANDARD.md)
- [Local Testing Guide](../images/containers/lxc/LOCAL_TESTING_GUIDE.md)
- [Artifact Deployment Manager](api/deployment/ArtifactDeploymentManager.java)
- [Artifact Publish Manager](api/publish/ArtifactPublishManager.java)
