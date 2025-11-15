# Orchestrix Deployments

Per-tenant deployment configurations for WireGuard overlay and FRR BGP routing.

## Directory Structure

```
deployments/
├── netlab/                    # Test tenant
│   ├── frr/                  # FRR BGP configs
│   │   ├── node1-config.conf
│   │   ├── node2-config.conf
│   │   └── node3-config.conf
│   ├── wireguard/            # WireGuard overlay configs
│   │   ├── node1.conf
│   │   ├── node2.conf
│   │   └── node3.conf
│   └── README.md
│
├── telco-client-001/         # Example production tenant
│   ├── frr/
│   │   └── [node configs...]
│   └── wireguard/
│       └── [node configs...]
│
└── README.md                 # This file
```

## Organization Pattern

### Per-Tenant Structure
Each tenant/client gets their own directory with:
- **`frr/`** - FRR BGP routing configurations
- **`wireguard/`** - WireGuard overlay network configurations

### Naming Convention
- **Tenant directories**: `tenant-name` or `client-name`
- **Node configs**: `node1-config.conf`, `node2-config.conf`, etc.
- Use consistent naming across frr/ and wireguard/ subdirectories

## Configuration Files

### FRR Configs (`frr/node*-config.conf`)
Shell variable format for FRR router deployment:

```bash
CONTAINER_NAME="frr-router-netlab01"
BASE_IMAGE="frr-router-base-v.1.0.0"
BGP_ASN="65199"
BGP_NEIGHBORS="10.9.9.2:65198,10.9.9.3:65197"
BGP_NETWORKS="10.10.199.0/24"
```

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

## Usage

### Deployment Automation
Reference configs from deployment scripts:

```bash
# Deploy FRR to node
./deploy-frr.sh \
  --host 10.20.0.30 \
  --config deployments/netlab/frr/node1-config.conf

# Deploy WireGuard overlay
./deploy-wg.sh \
  --config deployments/netlab/wireguard/node1.conf \
  --target 10.20.0.30
```

### Java Automation
```java
String configPath = "deployments/netlab/frr/node1-config.conf";
FrrConfig config = FrrConfig.parseConfig(configPath);
```

## Adding New Tenant

1. **Create tenant directory:**
   ```bash
   mkdir -p deployments/new-client/{frr,wireguard}
   ```

2. **Copy template configs:**
   ```bash
   cp deployments/netlab/frr/node1-config.conf deployments/new-client/frr/
   cp deployments/netlab/wireguard/node1.conf deployments/new-client/wireguard/
   ```

3. **Customize for tenant:**
   - Update IPs, ASNs, network ranges
   - Adjust container names
   - Modify peer configurations

4. **Document in tenant README:**
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
