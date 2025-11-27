# Java Automation Guide

One-click deployment automations for container infrastructure.

## Quick Start

### Deploy Overlay Network (WireGuard + FRR BGP)
```bash
# Deploy to any tenant
mvn compile exec:java \
  -Dexec.mainClass="com.telcobright.orchestrix.automation.overlay.OverlayNetworkDeployment" \
  -Dexec.args="deployments/ks_network"

# Default (netlab)
mvn compile exec:java \
  -Dexec.mainClass="com.telcobright.orchestrix.automation.overlay.OverlayNetworkDeployment"
```

### Deploy Kafka Cluster (KRaft Mode)
```bash
# Deploy to any tenant
mvn compile exec:java \
  -Dexec.mainClass="com.telcobright.orchestrix.automation.example.NetlabKafkaDeployment" \
  -Dexec.args="deployments/ks_network/kafka"

# Default (netlab)
mvn compile exec:java \
  -Dexec.mainClass="com.telcobright.orchestrix.automation.example.NetlabKafkaDeployment"
```

---

## Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                    Deployment Runners                           │
│  (Entry points that accept deployment path as argument)         │
├─────────────────────────────────────────────────────────────────┤
│  OverlayNetworkDeployment    │  NetlabKafkaDeployment           │
│  (WireGuard + FRR BGP)       │  (Kafka KRaft cluster)           │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                    Configuration Classes                        │
│  (Load from deployments/{tenant}/*.conf)                        │
├─────────────────────────────────────────────────────────────────┤
│  OverlayConfig               │  KafkaConfig                     │
│  CommonConfig (base)         │  (extends CommonConfig)          │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                    Core Automation Classes                      │
│  (Reusable, tenant-agnostic logic)                              │
├─────────────────────────────────────────────────────────────────┤
│  WireGuardConfigGenerator    │  KafkaClusterDeployment          │
│  NetworkConfigAutomation     │  DockerDeploymentAutomation      │
│  DockerComposeGenerator      │  KafkaClusterVerifier            │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                    SSH Device Layer                             │
├─────────────────────────────────────────────────────────────────┤
│  RemoteSshDevice - Executes commands on remote servers via SSH  │
└─────────────────────────────────────────────────────────────────┘
```

---

## Directory Structure

```
orchestrix-frr-router/
├── deployments/
│   ├── netlab/                    # Netlab tenant (local test VMs)
│   │   ├── common.conf            # Shared config (SSH, nodes)
│   │   ├── overlay-config.conf    # WireGuard + FRR config
│   │   └── kafka/
│   │       └── cluster-config.conf
│   │
│   ├── ks_network/                # KS Network tenant
│   │   ├── common.conf
│   │   ├── overlay-config.conf
│   │   └── kafka/
│   │       └── cluster-config.conf
│   │
│   └── link3/                     # Link3 tenant
│       └── ...
│
└── src/main/java/com/telcobright/orchestrix/automation/
    ├── config/
    │   ├── CommonConfig.java      # Base config class
    │   ├── OverlayConfig.java     # Overlay network config
    │   └── KafkaConfig.java       # Kafka cluster config
    │
    ├── overlay/
    │   └── OverlayNetworkDeployment.java  # WireGuard + FRR deployment
    │
    ├── cluster/kafkadocker/
    │   ├── KafkaClusterDeployment.java    # Kafka orchestrator
    │   ├── DockerDeploymentAutomation.java
    │   ├── DockerComposeGenerator.java
    │   ├── NetworkConfigAutomation.java
    │   └── KafkaClusterVerifier.java
    │
    ├── routing/
    │   ├── wireguard/
    │   │   └── WireGuardConfigGenerator.java
    │   └── NetworkingGuidelineHelper.java
    │
    └── example/
        ├── NetlabDeployment.java          # Legacy (hardcoded)
        ├── NetlabKafkaDeployment.java     # Kafka runner
        └── AddVpnClients.java             # VPN client management
```

---

## Configuration Files

### overlay-config.conf (or common.conf)
```bash
# Tenant Information
TENANT_NAME="ks_network"
TENANT_ID="ksn"
ENVIRONMENT="production"

# Node Count
NODE_COUNT=3

# SSH Defaults
SSH_PORT=22

# Node 1 Configuration
NODE1_MGMT_IP="172.27.27.132"
NODE1_SSH_USER="csas"
NODE1_SSH_PASS="your_password"
NODE1_HOSTNAME="ksn-node1"
NODE1_CONTAINER_SUBNET="10.10.196.0/24"
NODE1_OVERLAY_IP="10.9.10.1"
NODE1_BGP_AS="65196"

# Node 2 Configuration
NODE2_MGMT_IP="172.27.27.136"
NODE2_SSH_USER="csas"
NODE2_SSH_PASS="your_password"
NODE2_HOSTNAME="ksn-node2"
NODE2_CONTAINER_SUBNET="10.10.195.0/24"
NODE2_OVERLAY_IP="10.9.10.2"
NODE2_BGP_AS="65195"

# Node 3 Configuration
NODE3_MGMT_IP="172.27.27.135"
NODE3_SSH_USER="csas"
NODE3_SSH_PASS="your_password"
NODE3_HOSTNAME="ksn-node3"
NODE3_CONTAINER_SUBNET="10.10.194.0/24"
NODE3_OVERLAY_IP="10.9.10.3"
NODE3_BGP_AS="65194"

# Overlay Network
OVERLAY_NETWORK="10.9.10.0/24"
OVERLAY_PORT=51820
```

### kafka/cluster-config.conf
```bash
# Cluster Name
CLUSTER_NAME="ks_network"
NODE_COUNT=3
SSH_PORT=22

# Node 1 Configuration
NODE1_MGMT_IP="172.27.27.132"
NODE1_SSH_USER="csas"
NODE1_SSH_PASS="your_password"
NODE1_SUBNET="10.10.196.0/24"
NODE1_BRIDGE_GW="10.10.196.1"
NODE1_KAFKA_IP="10.10.196.20"
NODE1_KAFKA_ID=1
NODE1_BRIDGE_NAME="lxdbr0"

# Node 2 Configuration
NODE2_MGMT_IP="172.27.27.136"
NODE2_SSH_USER="csas"
NODE2_SSH_PASS="your_password"
NODE2_SUBNET="10.10.195.0/24"
NODE2_BRIDGE_GW="10.10.195.1"
NODE2_KAFKA_IP="10.10.195.20"
NODE2_KAFKA_ID=2
NODE2_BRIDGE_NAME="lxdbr0"

# Node 3 Configuration
NODE3_MGMT_IP="172.27.27.135"
NODE3_SSH_USER="csas"
NODE3_SSH_PASS="your_password"
NODE3_SUBNET="10.10.194.0/24"
NODE3_BRIDGE_GW="10.10.194.1"
NODE3_KAFKA_IP="10.10.194.20"
NODE3_KAFKA_ID=3
NODE3_BRIDGE_NAME="lxdbr0"

# Kafka Configuration
KAFKA_IMAGE="confluentinc/cp-kafka:8.1.0"
KAFKA_DATA_DIR="/var/lib/kafka/data"
KAFKA_WORK_DIR_PREFIX="kafka-node"
KAFKA_OFFSETS_REPLICATION_FACTOR=3
KAFKA_TRANSACTION_REPLICATION_FACTOR=3
KAFKA_TRANSACTION_MIN_ISR=2
KAFKA_AUTO_CREATE_TOPICS="true"
```

---

## Adding a New Tenant

### Step 1: Create Deployment Directory
```bash
mkdir -p deployments/new_tenant/kafka
```

### Step 2: Create Configuration Files
```bash
# Copy template
cp deployments/netlab/overlay-config.conf deployments/new_tenant/
cp deployments/netlab/kafka/cluster-config.conf deployments/new_tenant/kafka/

# Edit with tenant-specific values
vim deployments/new_tenant/overlay-config.conf
vim deployments/new_tenant/kafka/cluster-config.conf
```

### Step 3: Deploy
```bash
# Step 1: Deploy overlay network first
mvn compile exec:java \
  -Dexec.mainClass="com.telcobright.orchestrix.automation.overlay.OverlayNetworkDeployment" \
  -Dexec.args="deployments/new_tenant"

# Step 2: Deploy Kafka cluster
mvn compile exec:java \
  -Dexec.mainClass="com.telcobright.orchestrix.automation.example.NetlabKafkaDeployment" \
  -Dexec.args="deployments/new_tenant/kafka"
```

---

## Key Classes Reference

| Class | Package | Description |
|-------|---------|-------------|
| `OverlayNetworkDeployment` | automation.overlay | Deploys WireGuard overlay + FRR BGP |
| `NetlabKafkaDeployment` | automation.example | Deploys Kafka cluster (entry point) |
| `KafkaClusterDeployment` | automation.cluster.kafkadocker | Kafka deployment orchestrator |
| `WireGuardConfigGenerator` | automation.routing.wireguard | Generates WG configs |
| `KafkaConfig` | automation.config | Loads Kafka cluster config |
| `OverlayConfig` | automation.config | Loads overlay network config |
| `CommonConfig` | automation.config | Base config class |
| `RemoteSshDevice` | automation.core.device.impl | SSH command execution |

---

## Deployment Order

For a fresh tenant deployment:

1. **VPN Connection** (if remote)
   - Connect to tenant network via PPTP/WireGuard VPN

2. **Overlay Network** (required first)
   - Installs WireGuard, FRR, LXD
   - Configures LXD bridges with container subnets
   - Sets up WireGuard mesh overlay
   - Configures FRR BGP routing

3. **Kafka Cluster** (requires overlay)
   - Installs Docker if not present
   - Configures secondary IPs on bridges
   - Deploys Kafka containers (KRaft mode)
   - Verifies cluster health

---

## Troubleshooting

### SSH Connection Failed
- Check VPN connection is active
- Verify SSH credentials in config file
- Test manually: `ssh user@ip -p port`

### WireGuard Not Starting
- Check if WireGuard is installed: `which wg`
- Check config syntax: `sudo wg-quick up wg-overlay`
- View logs: `journalctl -u wg-quick@wg-overlay`

### BGP Sessions Not Establishing
- Check FRR is running: `systemctl status frr`
- Verify WireGuard peers: `sudo wg show`
- Check BGP status: `sudo vtysh -c 'show ip bgp summary'`

### Kafka Container Failing
- Check Docker logs: `sudo docker logs kafka-node`
- Verify data directory permissions: `ls -la /var/lib/kafka/data`
- Check secondary IP on bridge: `ip addr show lxdbr0`

---

## AI Agent Instructions

When asked to deploy to a new tenant:

1. **Check if config exists**: `ls deployments/{tenant}/`
2. **If not, create config files** using templates above
3. **Verify VPN connectivity** to management IPs
4. **Run overlay deployment first**, wait for success
5. **Run Kafka deployment second**
6. **Verify with health checks**:
   - `sudo wg show` - WireGuard peers
   - `sudo vtysh -c 'show ip bgp summary'` - BGP sessions
   - `kafka-topics --list --bootstrap-server {ip}:9092` - Kafka

When deploying, always use the **deployment directory argument** to specify the tenant:
```bash
-Dexec.args="deployments/{tenant}"
```
