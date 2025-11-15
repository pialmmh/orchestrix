# Kafka Cluster Docker Deployment

Automated shell scripts for deploying Apache Kafka clusters using Docker Compose with host networking mode.

## Overview

This deployment approach uses:
- **Docker Compose** with `network_mode: host`
- **KRaft mode** (no ZooKeeper required)
- **Secondary IPs** on LXD bridges for Kafka brokers
- **BGP routing** for cross-host container communication
- **Reserved IPs** following networking guidelines (.20, .21, .22 for Kafka brokers)

## Architecture

```
Server 1 (10.255.246.173)              Server 2 (10.255.246.174)              Server 3 (10.255.246.175)
┌────────────────────────┐             ┌────────────────────────┐             ┌────────────────────────┐
│ lxdbr0: 10.10.199.1/24 │             │ lxdbr0: 10.10.198.1/24 │             │ lxdbr0: 10.10.197.1/24 │
│   └─ 10.10.199.20 (2nd)│             │   └─ 10.10.198.20 (2nd)│             │   └─ 10.10.197.20 (2nd)│
│                        │             │                        │             │                        │
│ ┌──────────────────┐   │             │ ┌──────────────────┐   │             │ ┌──────────────────┐   │
│ │ Kafka Broker 1   │◄──┼─────────────┼─┤ Kafka Broker 2   │◄──┼─────────────┼─┤ Kafka Broker 3   │   │
│ │ Node ID: 1       │   │   BGP/WG    │ │ Node ID: 2       │   │   BGP/WG    │ │ Node ID: 3       │   │
│ │ 10.10.199.20:9092│   │             │ │ 10.10.198.20:9092│   │             │ │ 10.10.197.20:9092│   │
│ │ (host network)   │   │             │ │ (host network)   │   │             │ │ (host network)   │   │
│ └──────────────────┘   │             │ └──────────────────┘   │             │ └──────────────────┘   │
└────────────────────────┘             └────────────────────────┘             └────────────────────────┘
```

## Files

```
kafka-cluster-docker/
├── README.md                  # This file
├── cluster-config.conf        # Configuration file (EDIT THIS)
├── setup-node.sh             # Setup single node
├── deploy-cluster.sh         # Deploy entire cluster
├── verify-cluster.sh         # Verify cluster health
├── manage-cluster.sh         # Start/stop/restart nodes
└── cleanup-cluster.sh        # Remove cluster
```

## Prerequisites

1. **Network Infrastructure:**
   - FRR + BGP configured on all hosts
   - WireGuard overlay network active
   - LXD bridges (lxdbr0) configured with proper subnets
   - BGP announcing container subnets

2. **Software on Control Machine:**
   - `bash`
   - `sshpass` (for automated SSH)

3. **Software on Target Servers:**
   - Docker and Docker Compose v2
   - `nc` (netcat) for port checking

4. **Network Connectivity:**
   - SSH access to all target servers
   - BGP routes between hosts operational

## Quick Start

### 1. Configure the Cluster

Edit `cluster-config.conf` with your environment details:

```bash
nano cluster-config.conf
```

Key settings to modify:
- `NODE*_MGMT_IP` - Management IPs of your servers
- `NODE*_SSH_USER` and `NODE*_SSH_PASS` - SSH credentials
- `NODE*_KAFKA_IP` - Reserved IPs for Kafka brokers (typically .20, .21, .22)
- `NODE*_SUBNET` - Container subnets per host

### 2. Deploy the Cluster

Run the deployment script:

```bash
bash deploy-cluster.sh
```

This will:
1. Add secondary IPs to bridges on all nodes
2. Create Kafka data directories
3. Generate and deploy docker-compose.yml files
4. Start Kafka containers on all nodes
5. Wait for cluster formation
6. Verify all brokers are reachable

**Expected output:**
```
Kafka Cluster Deployment Complete!
========================================
Broker Endpoints:
  Node 1: 10.10.199.20:9092
  Node 2: 10.10.198.20:9092
  Node 3: 10.10.197.20:9092

Bootstrap Servers: 10.10.199.20:9092,10.10.198.20:9092,10.10.197.20:9092
```

### 3. Verify the Cluster

Run the verification script:

```bash
bash verify-cluster.sh
```

This performs comprehensive tests:
- Broker connectivity check
- Topic listing
- Test topic creation with replication factor 3
- Message production and consumption
- Cross-node network connectivity

## Manual Operations

### Setup Individual Node

To setup or reconfigure a single node:

```bash
bash setup-node.sh <node_number>

# Examples:
bash setup-node.sh 1    # Setup node 1
bash setup-node.sh 2    # Setup node 2
```

### Manage Cluster Lifecycle

**Start/Stop/Restart all nodes:**

```bash
bash manage-cluster.sh start     # Start all nodes
bash manage-cluster.sh stop      # Stop all nodes
bash manage-cluster.sh restart   # Restart all nodes
bash manage-cluster.sh status    # Show status of all nodes
```

**Manage individual nodes:**

```bash
bash manage-cluster.sh start 1    # Start node 1 only
bash manage-cluster.sh stop 2     # Stop node 2 only
bash manage-cluster.sh restart 3  # Restart node 3 only
bash manage-cluster.sh logs 1     # Show logs of node 1
```

### Check Cluster Status

```bash
bash manage-cluster.sh status
```

### View Node Logs

```bash
bash manage-cluster.sh logs 1     # Node 1 logs
bash manage-cluster.sh logs 2     # Node 2 logs
bash manage-cluster.sh logs 3     # Node 3 logs
```

## Cleanup and Removal

To remove the cluster:

```bash
bash cleanup-cluster.sh
```

You'll be prompted:
1. Confirm cluster removal
2. Choose whether to delete Kafka data directories

**Options:**
- **Keep data:** Containers and IPs removed, data preserved for redeployment
- **Delete data:** Complete cleanup including all Kafka data

## Configuration Reference

### cluster-config.conf

#### Node Configuration

Each node requires these parameters:

```bash
NODE1_MGMT_IP="10.255.246.173"      # SSH access IP
NODE1_SSH_USER="bdcom"               # SSH username
NODE1_SSH_PASS="password"            # SSH password
NODE1_SUBNET="10.10.199.0/24"        # Container subnet for this host
NODE1_BRIDGE_GW="10.10.199.1"        # Bridge gateway IP
NODE1_KAFKA_IP="10.10.199.20"        # Reserved IP for Kafka broker
NODE1_KAFKA_ID=1                     # Unique Kafka node ID
NODE1_BRIDGE_NAME="lxdbr0"           # Bridge interface name
```

#### Kafka Settings

```bash
KAFKA_IMAGE="confluentinc/cp-kafka:8.1.0"
KAFKA_DATA_DIR="/var/lib/kafka/data"
KAFKA_OFFSETS_REPLICATION_FACTOR=3
KAFKA_TRANSACTION_REPLICATION_FACTOR=3
KAFKA_TRANSACTION_MIN_ISR=2
KAFKA_AUTO_CREATE_TOPICS="true"
```

## Network Configuration Details

### Why Host Networking?

Using `network_mode: host` resolves Docker bridge conflicts with LXD bridges:

**Problem:** Docker bridge and LXD bridge both trying to use same subnet
**Solution:** Host networking + secondary IPs on LXD bridge

**Advantages:**
- No network namespace isolation - Kafka binds directly to host IPs
- No Docker bridge conflicts
- Simple routing via BGP
- Native performance (no NAT overhead)

**Trade-off:**
- One Kafka instance per physical host (acceptable for production)

### IP Address Assignment

Following the networking guidelines:

```
10.10.X.0/24 subnet breakdown:
├── .1           Bridge gateway
├── .2-.19       Infrastructure (reserved for FRR, DNS, etc.)
├── .20-.22      Kafka brokers (RESERVED)
├── .23-.99      Other standard applications
├── .100-.199    Docker containers (dynamic)
└── .200-.254    LXD containers (dynamic)
```

**Kafka uses reserved IPs** (.20, .21, .22) as standard applications.

### How It Works

1. **LXD bridge** `lxdbr0` has primary IP: `10.10.199.1/24`
2. **Secondary IP** added: `10.10.199.20/24` (for Kafka)
3. **Docker container** uses `network_mode: host`, binds to `10.10.199.20`
4. **BGP** announces `10.10.199.0/24` to other hosts
5. **Remote hosts** can reach `10.10.199.20` via BGP routes through WireGuard overlay

## Troubleshooting

### Issue: SSH Connection Failed

**Symptoms:**
```
ssh: connect to host X.X.X.X port 15605: Connection timed out
```

**Solutions:**
- Verify SSH_PORT in cluster-config.conf
- Check firewall rules on target servers
- Test manual SSH: `ssh -p 15605 user@host`

### Issue: Bridge Not Found

**Symptoms:**
```
Error: Bridge lxdbr0 not found
```

**Solutions:**
- Verify LXD bridge exists: `lxc network list`
- Check bridge name in config: `ip addr show`
- Create bridge if needed: `lxc network create lxdbr0`

### Issue: IP Already in Use

**Symptoms:**
```
RTNETLINK answers: File exists
```

**Solution:**
- This is normal if IP was already added
- Check current IPs: `ip addr show lxdbr0 | grep inet`
- Remove if needed: `sudo ip addr del 10.10.199.20/24 dev lxdbr0`

### Issue: Kafka Not Starting

**Symptoms:**
- Container starts but Kafka not responding
- Port 9092 not accessible

**Diagnosis:**
```bash
# Check logs
bash manage-cluster.sh logs 1

# Check if port is listening
nc -zv 10.10.199.20 9092

# Check container status
bash manage-cluster.sh status
```

**Solutions:**
- Verify all controller quorum voters are correct
- Check data directory permissions
- Ensure no port conflicts with existing services

### Issue: Brokers Can't See Each Other

**Symptoms:**
```
Connection to node 2 (/10.10.198.20:9093) could not be established
```

**Diagnosis:**
```bash
# Test network connectivity
ping 10.10.198.20

# Check BGP routes
ip route show proto bgp

# Verify BGP sessions
sudo vtysh -c 'show ip bgp summary'
```

**Solutions:**
- Restart BGP if routes missing: `sudo systemctl restart frr`
- Check WireGuard is up: `sudo wg show`
- Verify firewall allows BGP traffic

### Issue: Data Directory Permission Denied

**Symptoms:**
```
Permission denied: '/var/lib/kafka/data'
```

**Solution:**
```bash
# Fix permissions on each node
sudo chown -R 1000:1000 /var/lib/kafka/data
sudo chmod -R 755 /var/lib/kafka/data
```

## Production Recommendations

### 1. Data Persistence

The default configuration stores data at `/var/lib/kafka/data` on each host.

**For production:**
- Use dedicated disk volumes
- Consider RAID for redundancy
- Implement backup strategy

### 2. Security

**Current setup uses plain SSH passwords** - suitable for testing.

**For production:**
- Use SSH key-based authentication
- Remove passwords from config file
- Use secrets management (Vault, etc.)
- Enable Kafka authentication (SASL/SSL)

### 3. Monitoring

Add monitoring by:
- Deploying Prometheus + Grafana
- Using Kafka JMX metrics
- Setting up alerts for broker failures
- Monitoring disk space and network

### 4. Tuning

Kafka performance tuning:
```bash
# In cluster-config.conf, add more environment variables:
KAFKA_NUM_NETWORK_THREADS: 8
KAFKA_NUM_IO_THREADS: 8
KAFKA_SOCKET_SEND_BUFFER_BYTES: 102400
KAFKA_SOCKET_RECEIVE_BUFFER_BYTES: 102400
KAFKA_LOG_RETENTION_HOURS: 168
KAFKA_LOG_SEGMENT_BYTES: 1073741824
```

### 5. Backup

Regular backups:
```bash
# Stop broker
bash manage-cluster.sh stop 1

# Backup data
tar -czf kafka-node-1-backup-$(date +%Y%m%d).tar.gz /var/lib/kafka/data

# Start broker
bash manage-cluster.sh start 1
```

## Client Connection Examples

### Java (Spring Boot)

```yaml
spring:
  kafka:
    bootstrap-servers:
      - 10.10.199.20:9092
      - 10.10.198.20:9092
      - 10.10.197.20:9092
    consumer:
      group-id: my-consumer-group
    producer:
      retries: 3
```

### Python

```python
from kafka import KafkaProducer, KafkaConsumer

# Producer
producer = KafkaProducer(
    bootstrap_servers=['10.10.199.20:9092', '10.10.198.20:9092', '10.10.197.20:9092']
)

# Consumer
consumer = KafkaConsumer(
    'my-topic',
    bootstrap_servers=['10.10.199.20:9092', '10.10.198.20:9092', '10.10.197.20:9092'],
    group_id='my-consumer-group'
)
```

### Command Line

```bash
# Produce messages
echo "test message" | kafka-console-producer \
  --bootstrap-server 10.10.199.20:9092,10.10.198.20:9092,10.10.197.20:9092 \
  --topic my-topic

# Consume messages
kafka-console-consumer \
  --bootstrap-server 10.10.199.20:9092,10.10.198.20:9092,10.10.197.20:9092 \
  --topic my-topic \
  --from-beginning
```

## Advanced Operations

### Add More Nodes

To expand beyond 3 nodes:

1. Edit `cluster-config.conf`:
   ```bash
   NODE_COUNT=4

   NODE4_MGMT_IP="10.255.246.176"
   NODE4_SSH_USER="bdcom"
   NODE4_SSH_PASS="password"
   NODE4_KAFKA_IP="10.10.196.21"
   NODE4_KAFKA_ID=4
   # ... etc
   ```

2. Update CONTROLLER_QUORUM_VOTERS to include new node

3. Deploy new node:
   ```bash
   bash setup-node.sh 4
   ```

### Rolling Restart

For zero-downtime updates:

```bash
bash manage-cluster.sh restart 1
sleep 30  # Wait for node 1 to rejoin
bash manage-cluster.sh restart 2
sleep 30  # Wait for node 2 to rejoin
bash manage-cluster.sh restart 3
```

### Migrate to Different IPs

1. Edit `cluster-config.conf` with new IPs
2. Run cleanup (keep data): `bash cleanup-cluster.sh` → no to delete data
3. Redeploy: `bash deploy-cluster.sh`

## References

- [Networking Guidelines](/home/mustafa/telcobright-projects/orchestrix/images/networking_guideline_claude.md)
- [Apache Kafka Documentation](https://kafka.apache.org/documentation/)
- [KRaft Mode](https://kafka.apache.org/documentation/#kraft)
- [Confluent Platform](https://docs.confluent.io/platform/current/overview.html)

## Support

For issues or questions:
1. Check logs: `bash manage-cluster.sh logs <node>`
2. Run verification: `bash verify-cluster.sh`
3. Review networking guidelines
4. Check BGP/WireGuard status on hosts

---

**Version:** 1.0
**Last Updated:** 2025-11-05
**Tested With:** Confluent Kafka 8.1.0, Docker Compose v2
