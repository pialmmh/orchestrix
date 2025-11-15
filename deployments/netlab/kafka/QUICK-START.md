# Netlab Kafka Cluster - Quick Start Guide

**One-Click Docker Compose-Based Kafka Deployment**

## Overview

This deployment uses **shell scripts** (copied from Link3 reference) to deploy a 3-node Kafka cluster using **Docker Compose** with **KRaft mode** (no Zookeeper).

## Architecture

```
netlab01 (10.20.0.30)          netlab02 (10.20.0.31)          netlab03 (10.20.0.32)
├─ lxdbr0: 10.10.199.1/24      ├─ lxdbr0: 10.10.198.1/24      ├─ lxdbr0: 10.10.197.1/24
├─ Secondary IP: .20           ├─ Secondary IP: .20           ├─ Secondary IP: .20
└─ Kafka Broker 1              └─ Kafka Broker 2              └─ Kafka Broker 3
   10.10.199.20:9092              10.10.198.20:9092              10.10.197.20:9092
   (host network)                  (host network)                  (host network)
```

**Connection String:**
```
10.10.199.20:9092,10.10.198.20:9092,10.10.197.20:9092
```

## Prerequisites

### 1. Netlab Overlay Network (MUST BE RUNNING)
```bash
# Deploy if not already done
mvn compile exec:java \
  -Dexec.mainClass="com.telcobright.orchestrix.automation.example.NetlabDeployment"

# Verify
ssh telcobright@10.20.0.30 'sudo wg show wg-overlay'
ssh telcobright@10.20.0.30 'sudo vtysh -c "show ip bgp"'
```

### 2. Docker on All Nodes
All 3 netlab nodes need Docker and Docker Compose installed.

**Verify:**
```bash
ssh telcobright@10.20.0.30 'docker --version && docker compose version'
ssh telcobright@10.20.0.31 'docker --version && docker compose version'
ssh telcobright@10.20.0.32 'docker --version && docker compose version'
```

**Install if needed (run on each node):**
```bash
# On each netlab node
curl -fsSL https://get.docker.com | sudo sh
sudo usermod -aG docker $USER
```

## Quick Deployment

### Step 1: Navigate to Kafka Directory
```bash
cd /home/mustafa/telcobright-projects/orchestrix-frr-router/deployments/netlab/kafka
```

### Step 2: Deploy the Cluster
```bash
bash deploy-cluster.sh
```

This will:
1. ✅ Add secondary IPs (.20) to lxdbr0 on all nodes
2. ✅ Create Kafka data directories
3. ✅ Generate docker-compose.yml on each node
4. ✅ Start Kafka containers (KRaft mode)
5. ✅ Verify all brokers are reachable

**Expected Output:**
```
Kafka Cluster Deployment Complete!
========================================
Broker Endpoints:
  Node 1: 10.10.199.20:9092
  Node 2: 10.10.198.20:9092
  Node 3: 10.10.197.20:9092

Bootstrap Servers: 10.10.199.20:9092,10.10.198.20:9092,10.10.197.20:9092
```

### Step 3: Verify Deployment
```bash
bash verify-cluster.sh
```

This performs comprehensive tests:
- ✅ Broker connectivity
- ✅ Topic creation with replication factor 3
- ✅ Message production and consumption
- ✅ Cross-node network connectivity

## Usage

### Create a Topic
```bash
kafka-topics.sh --create --topic my-topic \
  --bootstrap-server 10.10.199.20:9092,10.10.198.20:9092,10.10.197.20:9092 \
  --partitions 3 --replication-factor 3
```

### List Topics
```bash
kafka-topics.sh --list \
  --bootstrap-server 10.10.199.20:9092
```

### Produce Messages
```bash
kafka-console-producer.sh --topic my-topic \
  --bootstrap-server 10.10.199.20:9092
```

### Consume Messages
```bash
kafka-console-consumer.sh --topic my-topic \
  --bootstrap-server 10.10.199.20:9092 \
  --from-beginning
```

## Management

### Check Cluster Status
```bash
bash manage-cluster.sh status
```

### Start/Stop/Restart Cluster
```bash
bash manage-cluster.sh start      # Start all nodes
bash manage-cluster.sh stop       # Stop all nodes
bash manage-cluster.sh restart    # Restart all nodes
```

### Manage Individual Nodes
```bash
bash manage-cluster.sh start 1    # Start node 1 only
bash manage-cluster.sh stop 2     # Stop node 2 only
bash manage-cluster.sh restart 3  # Restart node 3 only
bash manage-cluster.sh logs 1     # View node 1 logs
```

## Access from VPN

```bash
# Connect to netlab VPN
cd /home/mustafa/telcobright-projects/routesphere/routesphere-core/tools/wireguard
./wg-connect.sh netlab/client1.conf

# Now you can access Kafka from your workstation
kafka-topics.sh --list --bootstrap-server 10.10.199.20:9092
```

## Configuration

**Config File:** `cluster-config.conf`

Key settings (already configured for netlab):
- Cluster Name: `netlab`
- Nodes: 10.20.0.30, 10.20.0.31, 10.20.0.32
- Kafka IPs: 10.10.199.20, 10.10.198.20, 10.10.197.20
- SSH Port: 22
- User: telcobright

## Cleanup

```bash
bash cleanup-cluster.sh
```

Options:
- **Keep data**: Remove containers/IPs, preserve data for redeployment
- **Delete data**: Complete cleanup including all Kafka data

## Key Features

✅ **KRaft Mode**: No Zookeeper needed (modern Kafka 3.x+)
✅ **Host Networking**: No Docker bridge conflicts with LXD
✅ **BGP Routing**: Cross-node connectivity via FRR overlay
✅ **Reserved IPs**: Following netlab networking guidelines (.20 for Kafka)
✅ **Reusable**: Same scripts work for Link3, BDCOM, BTCL, etc.

## Troubleshooting

### Deployment Fails
```bash
# Check overlay network
ssh telcobright@10.20.0.30 'sudo wg show wg-overlay'
ssh telcobright@10.20.0.30 'sudo vtysh -c "show ip bgp"'

# Check Docker
ssh telcobright@10.20.0.30 'docker ps'
```

### Cannot Reach Kafka
```bash
# Test network connectivity
ping 10.10.199.20

# Check if port is listening
nc -zv 10.10.199.20 9092

# View logs
bash manage-cluster.sh logs 1
```

### Brokers Can't See Each Other
```bash
# Check BGP routes
ssh telcobright@10.20.0.30 'ip route show proto bgp'

# Verify all container subnets are announced
ssh telcobright@10.20.0.30 'sudo vtysh -c "show ip bgp"'
```

## Files

```
deployments/netlab/kafka/
├── README.md                  # Comprehensive documentation
├── QUICK-START.md            # This file
├── cluster-config.conf        # Configuration (✓ Ready for netlab)
├── deploy-cluster.sh         # One-click deployment
├── setup-node.sh             # Setup single node
├── verify-cluster.sh         # Cluster verification
├── manage-cluster.sh         # Start/stop/restart
├── cleanup-cluster.sh        # Remove cluster
└── example-usage.sh          # Usage examples
```

## Reference

- **Link3 Deployment**: Original reference deployment
- **Networking Guidelines**: `/home/mustafa/telcobright-projects/orchestrix/images/networking_guideline_claude.md`
- **Netlab Overview**: `../DEPLOYMENT_SUMMARY.md`

---

**Status**: ✅ Ready to deploy
**Last Updated**: 2025-11-16
**Tested With**: Confluent Kafka 8.1.0, KRaft mode
