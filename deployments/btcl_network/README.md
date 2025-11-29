# BTCL Network Deployment

## Overview
Production deployment of WireGuard overlay network + FRR BGP routing + Kafka 3-broker cluster for BTCL infrastructure.

## Network Architecture

### Management Network (Existing)
- **Node 1** (sms-Kafka01@sbc01): 114.130.145.75:50005
- **Node 2** (sms-Kafka02@sbc04): 114.130.145.75:50006
- **Node 3** (sms-kafka03@sms01): 114.130.145.70:50010

### WireGuard Overlay Network
```
Overlay Network: 10.9.20.0/24
Port: 51820/UDP

Node 1: 10.9.20.1 (AS 65101)
Node 2: 10.9.20.2 (AS 65102)
Node 3: 10.9.20.3 (AS 65103)
```

### Container Networks
```
Node 1: 10.10.101.0/24 (bridge: lxdbr0, gateway: 10.10.101.1)
Node 2: 10.10.102.0/24 (bridge: lxdbr0, gateway: 10.10.102.1)
Node 3: 10.10.103.0/24 (bridge: lxdbr0, gateway: 10.10.103.1)
```

### Kafka Cluster
```
Broker 1: 10.10.101.20 (KRaft ID: 1)
Broker 2: 10.10.102.20 (KRaft ID: 2)
Broker 3: 10.10.103.20 (KRaft ID: 3)

Replication Factor: 3
Min ISR: 2
Mode: KRaft (no Zookeeper)
```

## Prerequisites

### Access Requirements
- SSH access to all 3 nodes via custom ports
- User: `telcobright`
- Passwordless sudo configured on all nodes

### Verification
```bash
mvn exec:java -Dexec.mainClass="com.telcobright.orchestrix.automation.example.CheckBtclSudoAccess"
```

## Deployment Steps

### 1. Deploy Overlay Network
Deploys WireGuard + FRR BGP on all 3 nodes:
```bash
mvn compile exec:java \
  -Dexec.mainClass="com.telcobright.orchestrix.automation.overlay.OverlayNetworkDeployment" \
  -Dexec.args="deployments/btcl_network"
```

This will:
- Install WireGuard and FRR on all nodes
- Configure WireGuard mesh network (10.9.20.0/24)
- Set up BGP peering between nodes
- Enable automatic route advertisement

### 2. Configure Container Bridges
```bash
# TODO: Create bridge configuration automation
```

### 3. Deploy Kafka Cluster
```bash
# TODO: Create Kafka deployment automation for BTCL
```

### 4. Generate VPN Client Configs
For developer access to the overlay network:
```bash
# TODO: Create VPN client config generator
```

## Network Topology

```
                    Internet
                        |
          +-------------+-------------+
          |                           |
    114.130.145.75              114.130.145.70
          |                           |
    +-----+-----+                     |
    |           |                     |
  :50005     :50006                :50010
    |           |                     |
 [Node 1]    [Node 2]             [Node 3]
10.9.20.1   10.9.20.2            10.9.20.3
    |           |                     |
    +-----WireGuard Mesh (10.9.20.0/24)-----+
    |           |                     |
 [lxdbr0]    [lxdbr0]              [lxdbr0]
10.10.101.1 10.10.102.1          10.10.103.1
    |           |                     |
[Kafka-1]   [Kafka-2]             [Kafka-3]
.101.20     .102.20               .103.20
```

## BGP Configuration

Each node runs FRR and announces its local container subnet:
- Node 1 (AS 65101) announces 10.10.101.0/24
- Node 2 (AS 65102) announces 10.10.102.0/24
- Node 3 (AS 65103) announces 10.10.103.0/24

BGP neighbors:
- Node 1 ↔ Node 2, Node 1 ↔ Node 3
- Node 2 ↔ Node 1, Node 2 ↔ Node 3
- Node 3 ↔ Node 1, Node 3 ↔ Node 2

## VPN Access for Developers

VPN gateway is configured on Node 1 (10.9.20.1).

Client IP range: 10.9.20.100-10.9.20.200

Routes pushed to clients:
- 10.9.20.0/24 (overlay network)
- 10.10.101.0/24 (node 1 containers)
- 10.10.102.0/24 (node 2 containers)
- 10.10.103.0/24 (node 3 containers)

## Firewall Rules

### Required Inbound Rules
```bash
# WireGuard
ufw allow 51820/udp

# Kafka (if external access needed)
ufw allow from 10.9.20.0/24 to any port 9092
ufw allow from 10.10.0.0/16 to any port 9092
```

## Troubleshooting

### Check WireGuard Status
```bash
ssh -p 50005 telcobright@114.130.145.75 "sudo wg show"
```

### Check FRR BGP Status
```bash
ssh -p 50005 telcobright@114.130.145.75 "sudo vtysh -c 'show bgp summary'"
```

### Check Kafka Broker
```bash
ssh -p 50005 telcobright@114.130.145.75 "docker ps | grep kafka"
```

### Verify Connectivity
```bash
# From Node 1, ping Node 2 overlay IP
ssh -p 50005 telcobright@114.130.145.75 "ping -c 3 10.9.20.2"

# From Node 1, ping Node 3 Kafka broker
ssh -p 50005 telcobright@114.130.145.75 "ping -c 3 10.10.103.20"
```

## Configuration Files

- `overlay-config.conf` - WireGuard + BGP configuration
- `kafka/cluster-config.conf` - Kafka cluster configuration

## Security Notes

- All credentials stored in configuration files (NOT committed to git)
- SSH access restricted to telcobright user with passwordless sudo
- WireGuard provides encrypted overlay network
- Kafka accessible only via overlay network (no public exposure)
- VPN gateway for secure developer access

## Support

For issues or questions, contact TelcoBright DevOps team.
