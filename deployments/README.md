# Orchestrix Deployments

Per-tenant deployment configurations for WireGuard overlay and FRR BGP routing.

## Directory Structure

```
deployments/
├── netlab/                    # Test tenant
│   ├── common.conf           # Shared params (SSH, network, etc.)
│   ├── .secrets/             # Passwords & keys (gitignored)
│   │   ├── ssh-password.txt  # SSH password
│   │   ├── ssh-key           # SSH private key
│   │   ├── setup-example.sh  # Setup helper script
│   │   └── README.md         # Secrets documentation
│   ├── frr/                  # FRR BGP configs
│   │   ├── node1-config.conf
│   │   ├── node2-config.conf
│   │   └── node3-config.conf
│   ├── wireguard/            # WireGuard overlay configs
│   │   ├── node1.conf
│   │   ├── node2.conf
│   │   └── node3.conf
│   ├── output/               # Generated configs (gitignored)
│   │   ├── client1.conf      # Link3 office VPN
│   │   ├── client2.conf      # BDCOM server VPN
│   │   ├── clientN.conf      # Additional clients
│   │   ├── client.conf.example
│   │   └── README.md
│   └── README.md
│
├── telco-client-001/         # Example production tenant
│   ├── common.conf           # Shared params for this tenant
│   ├── .secrets/             # Passwords & keys (gitignored)
│   ├── frr/
│   │   └── [node configs...]
│   ├── wireguard/
│   │   └── [node configs...]
│   └── output/               # Generated outputs
│       └── [client configs...]
│
└── README.md                 # This file
```

## Organization Pattern

### Per-Tenant Structure
Each tenant/client gets their own directory with:
- **`common.conf`** - Automation-agnostic shared parameters (SSH, nodes, networks)
- **`.secrets/`** - SSH passwords and private keys (gitignored)
- **`<automation>/`** - Automation-specific configs (frr/, wireguard/, kafka/, mysql/, etc.)
- **`output/`** - Generated configs and automation outputs (gitignored)
- **`README.md`** - Tenant-specific documentation (optional)

**Example automation directories:**
- `frr/` - FRR BGP routing configurations
- `wireguard/` - WireGuard overlay network configurations
- `kafka/` - Kafka cluster configurations (future)
- `mysql/` - MySQL database configurations (future)
- `lxd/` - LXD/LXC container configurations (future)

### Naming Convention
- **Tenant directories**: `tenant-name` or `client-name`
- **Node configs**: `node1-config.conf`, `node2-config.conf`, etc.
- Use consistent naming across frr/ and wireguard/ subdirectories

## Configuration Files

### Common Config (`common.conf`)
**Automation-agnostic** shared parameters used across ALL automations (FRR, WireGuard, Kafka, MySQL, etc.):

```bash
# Tenant info
TENANT_NAME="netlab"
ENVIRONMENT="test"

# SSH parameters (used by all deployments)
SSH_USER="telcobright"
SSH_PASSWORD_FILE="deployments/netlab/.secrets/ssh-password.txt"
SSH_PORT="22"

# Node definitions
NODE1_IP="10.20.0.30"
NODE2_IP="10.20.0.31"
NODE3_IP="10.20.0.32"
NODE1_HOSTNAME="netlab01"
NODE2_HOSTNAME="netlab02"
NODE3_HOSTNAME="netlab03"

# Network ranges (generic - used by all apps)
OVERLAY_NETWORK="10.9.9.0/24"
CONTAINER_SUPERNET="10.10.0.0/16"
MANAGEMENT_NETWORK="10.20.0.0/24"

# LXD/LXC defaults
LXD_BRIDGE="lxdbr0"
DEFAULT_MEMORY_LIMIT="512MB"
DEFAULT_CPU_LIMIT="1.0"

# Deployment options
VERBOSE="true"
DRY_RUN="false"
```

**Important:** `common.conf` contains NO automation-specific configuration (BGP, Kafka topics, etc.). Automation-specific configs go in their respective directories (`frr/`, `wireguard/`, `kafka/`, etc.).

**Benefits:**
- ✅ DRY principle - define once, use everywhere
- ✅ Centralized tenant configuration
- ✅ Works for ANY automation (FRR, Kafka, MySQL, etc.)
- ✅ Automation-agnostic - no coupling to specific services
- ✅ Easy to update SSH credentials for all nodes
- ✅ Consistent network parameters across all deployments
- ✅ Secure authentication (password file or SSH key)

**SSH Authentication Methods:**

⚠️ **NEVER commit passwords directly in config files!**

Secrets are stored in the **`.secrets/`** directory within each tenant folder:

```
deployments/netlab/.secrets/
├── ssh-password.txt      # SSH password (gitignored)
├── ssh-key              # SSH private key (gitignored)
├── ssh-key.pub          # SSH public key (gitignored)
├── setup-example.sh     # Quick setup script
└── README.md            # Detailed documentation
```

**Quick Setup:**

1. **Using Password File** (for temporary/test use):
   ```bash
   cd deployments/netlab/.secrets
   ./setup-example.sh
   # Choose option 1, enter password

   # Or manually:
   echo "a" > ssh-password.txt
   chmod 600 ssh-password.txt
   ```

2. **Using SSH Key** (recommended for production):
   ```bash
   cd deployments/netlab/.secrets
   ./setup-example.sh
   # Choose option 2, follow instructions

   # Or manually:
   ssh-keygen -t ed25519 -f ssh-key -N "" -C "netlab-automation"
   ssh-copy-id -i ssh-key.pub telcobright@10.20.0.30
   ssh-copy-id -i ssh-key.pub telcobright@10.20.0.31
   ssh-copy-id -i ssh-key.pub telcobright@10.20.0.32
   chmod 600 ssh-key
   ```

See `.secrets/README.md` for detailed security instructions.

### FRR Configs (`frr/node*-config.conf`)
**FRR-specific** parameters for BGP routing (automation-specific, NOT in common.conf):

```bash
# Container settings
CONTAINER_NAME="frr-router-netlab01"
BASE_IMAGE="frr-router-base-v.1.0.0"

# BGP-specific configuration (FRR automation only)
BGP_ASN="65199"
BGP_NEIGHBORS="10.9.9.2:65198,10.9.9.3:65197"
BGP_NETWORKS="10.10.199.0/24"
BGP_TIMERS="10 30"
```

These configs are loaded AFTER `common.conf` and contain only FRR/BGP-specific parameters.

### WireGuard Configs (`wireguard/node*.conf`)
Standard WireGuard INI format:

```ini
[Interface]
PrivateKey = REPLACE_WITH_GENERATED_PRIVATE_KEY
Address = 10.9.9.1/24
ListenPort = 51820

[Peer]
PublicKey = REPLACE_WITH_PEER_PUBLIC_KEY
Endpoint = 10.20.0.31:51820
AllowedIPs = 10.9.9.2/32, 10.10.198.0/24
```

### Output Directory (`output/`)
Generated configurations and automation outputs:

```
output/
├── client1.conf              # VPN client config (Link3 office)
├── client2.conf              # VPN client config (BDCOM server)
├── clientN.conf              # Additional VPN clients
├── bgp-topology.txt          # Generated BGP topology
├── deployment-summary.txt    # Last deployment summary
├── client.conf.example       # Template for client configs
└── README.md                 # Output documentation
```

**⚠️ Security:** Output files contain private keys and are automatically gitignored.

**Generated by:**
- Java automation: `NetlabDeployment.java`, `WireGuardConfigGenerator.java`
- Shell scripts: `deploy-netlab.sh`

## Usage

### Deployment Automation
Reference configs from deployment scripts:

```bash
# Source common config first
source deployments/netlab/common.conf

# Method 1: Using password file
if [ -n "$SSH_PASSWORD_FILE" ] && [ -f "$SSH_PASSWORD_FILE" ]; then
  SSH_PASSWORD=$(cat "$SSH_PASSWORD_FILE")

  ./deploy-frr.sh \
    --host $NODE1_IP \
    --user $SSH_USER \
    --password "$SSH_PASSWORD" \
    --config deployments/netlab/frr/node1-config.conf
fi

# Method 2: Using SSH key file (recommended)
if [ -n "$SSH_KEY_FILE" ] && [ -f "$SSH_KEY_FILE" ]; then
  ./deploy-frr.sh \
    --host $NODE1_IP \
    --user $SSH_USER \
    --key-file "$SSH_KEY_FILE" \
    --config deployments/netlab/frr/node1-config.conf
fi

# Deploy WireGuard overlay
./deploy-wg.sh \
  --config deployments/netlab/wireguard/node1.conf \
  --target $NODE1_IP
```

### Java Automation
```java
// Load common config
String tenantDir = "deployments/netlab";
Map<String, String> commonConfig = loadShellConfig(tenantDir + "/common.conf");

// Use common params
String sshUser = commonConfig.get("SSH_USER");
String node1Ip = commonConfig.get("NODE1_IP");

// Handle SSH authentication methods
String sshPassword = commonConfig.get("SSH_PASSWORD");
String sshPasswordFile = commonConfig.get("SSH_PASSWORD_FILE");
String sshKeyFile = commonConfig.get("SSH_KEY_FILE");

// Load password from file if specified
if (sshPasswordFile != null && !sshPasswordFile.isEmpty()) {
    sshPassword = Files.readString(Path.of(sshPasswordFile)).trim();
}

// Create SSH connection
SSHConnection ssh;
if (sshKeyFile != null && !sshKeyFile.isEmpty()) {
    // Use SSH key authentication
    ssh = new SSHConnection(node1Ip, sshUser, sshKeyFile);
} else {
    // Use password authentication
    ssh = new SSHConnection(node1Ip, sshUser, sshPassword);
}

// Load automation-specific config
String frrConfigPath = tenantDir + "/frr/node1-config.conf";
FrrConfig config = FrrConfig.parseConfig(frrConfigPath);
```

## Adding New Tenant

1. **Create tenant directory:**
   ```bash
   mkdir -p deployments/new-client/{.secrets,frr,wireguard,output}
   ```

2. **Copy common config template:**
   ```bash
   cp deployments/netlab/common.conf deployments/new-client/
   # Edit and customize for new client
   nano deployments/new-client/common.conf
   ```

3. **Set up secrets directory:**
   ```bash
   cp deployments/netlab/.secrets/README.md deployments/new-client/.secrets/
   cp deployments/netlab/.secrets/setup-example.sh deployments/new-client/.secrets/
   chmod +x deployments/new-client/.secrets/setup-example.sh
   touch deployments/new-client/.secrets/.gitkeep

   # Run setup to create credentials
   cd deployments/new-client/.secrets
   ./setup-example.sh
   ```

4. **Copy automation configs:**
   ```bash
   cp deployments/netlab/frr/node1-config.conf deployments/new-client/frr/
   cp deployments/netlab/wireguard/node1.conf deployments/new-client/wireguard/
   ```

5. **Set up output directory:**
   ```bash
   cp deployments/netlab/output/README.md deployments/new-client/output/
   cp deployments/netlab/output/client.conf.example deployments/new-client/output/
   touch deployments/new-client/output/.gitkeep
   ```

6. **Customize for tenant:**
   - Update IPs, ASNs, network ranges in configs
   - Adjust container names
   - Modify peer configurations
   - Verify SSH credentials paths in common.conf

7. **Document in tenant README:**
   ```bash
   echo "# New Client Deployment" > deployments/new-client/README.md
   ```

## Examples

### Netlab Test Environment
- **Tenant:** `netlab`
- **Nodes:** 3 VMs (netlab01/02/03)
- **Management:** 10.20.0.30-32
- **Overlay:** 10.9.9.1-3
- **Purpose:** Testing automation before production deployment

See: `deployments/netlab/README.md`

### Telco Client 001
- **Tenant:** `telco-client-001`
- **Nodes:** Production cluster
- **Purpose:** Production FRR + WireGuard deployment

## Best Practices

1. **One config per node** - Separate files for each deployment target
2. **Consistent naming** - Use same node names across frr/ and wireguard/
3. **Document tenant** - Include README.md in each tenant directory
4. **Version control** - Commit configs to track deployment history
5. **Template reuse** - Copy from netlab/ as starting point

## Related Documentation

- FRR Router Guide: `/images/containers/lxc/frr-router/DEPLOYMENT_GUIDE.md`
- Networking Guideline: `/images/networking_guideline_claude.md`
- WireGuard Config Generator: `/src/main/java/.../wireguard/WireGuardConfigGenerator.java`
- Deployment Example: `NETLAB_DEPLOYMENT_GUIDE.md`
