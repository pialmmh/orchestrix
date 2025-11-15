# Kafka Cluster - Quick Reference Card

## Directory Contents

```
kafka-cluster-docker/
├── README.md                  # Complete documentation
├── QUICK-REFERENCE.md         # This file
├── cluster-config.conf        # Configuration (EDIT THIS FIRST!)
├── setup-node.sh             # Setup single node
├── deploy-cluster.sh         # Deploy entire cluster
├── verify-cluster.sh         # Verify cluster health
├── manage-cluster.sh         # Start/stop/restart/status/logs
├── cleanup-cluster.sh        # Remove cluster
└── example-usage.sh          # Usage examples
```

## Quick Start (3 Steps)

```bash
# 1. Edit configuration
nano cluster-config.conf

# 2. Deploy
bash deploy-cluster.sh

# 3. Verify
bash verify-cluster.sh
```

## Common Commands

### Deployment
```bash
bash deploy-cluster.sh              # Deploy full cluster
bash setup-node.sh 1                # Setup single node
```

### Management
```bash
bash manage-cluster.sh start        # Start all
bash manage-cluster.sh stop         # Stop all
bash manage-cluster.sh restart      # Restart all
bash manage-cluster.sh status       # Show status
bash manage-cluster.sh logs 1       # Show node 1 logs
```

### Testing
```bash
bash verify-cluster.sh              # Full verification
```

### Cleanup
```bash
bash cleanup-cluster.sh             # Remove cluster
```

## Configuration Quick Edit

Important settings in `cluster-config.conf`:

```bash
# For each node, set:
NODE1_MGMT_IP="10.255.246.173"      # Server IP
NODE1_SSH_USER="bdcom"               # SSH user
NODE1_SSH_PASS="password"            # SSH password
NODE1_KAFKA_IP="10.10.199.20"        # Kafka IP (reserved .20)
NODE1_KAFKA_ID=1                     # Unique ID (1,2,3)
```

## Connection Strings

After deployment, use these connection strings in your applications:

```
Bootstrap Servers:
10.10.199.20:9092,10.10.198.20:9092,10.10.197.20:9092
```

## Troubleshooting Quick Checks

```bash
# 1. Check containers running
bash manage-cluster.sh status

# 2. Check network connectivity
ssh -p 15605 bdcom@10.255.246.173 'ping -c 1 10.10.198.20 && ping -c 1 10.10.197.20'

# 3. Check Kafka ports
ssh -p 15605 bdcom@10.255.246.173 'nc -zv 10.10.199.20 9092'

# 4. Check logs
bash manage-cluster.sh logs 1 | tail -50

# 5. Full verification
bash verify-cluster.sh
```

## Kafka Operations from Server

SSH to any server and run:

```bash
# Create topic
sudo docker exec kafka-node kafka-topics \
  --create --topic my-topic \
  --bootstrap-server 10.10.199.20:9092 \
  --replication-factor 3 --partitions 3

# List topics
sudo docker exec kafka-node kafka-topics \
  --list --bootstrap-server 10.10.199.20:9092

# Describe topic
sudo docker exec kafka-node kafka-topics \
  --describe --topic my-topic \
  --bootstrap-server 10.10.199.20:9092

# Produce message
echo "test" | sudo docker exec -i kafka-node kafka-console-producer \
  --topic my-topic --bootstrap-server 10.10.199.20:9092

# Consume messages
sudo docker exec kafka-node kafka-console-consumer \
  --topic my-topic --from-beginning --max-messages 10 \
  --bootstrap-server 10.10.199.20:9092
```

## Network Architecture

```
Host: lxdbr0 (10.10.199.1/24)
  └─ Secondary IP: 10.10.199.20/24
      └─ Kafka Container (host network mode)
          └─ Binds to: 10.10.199.20:9092
              └─ Reachable via BGP from other hosts
```

## Emergency Operations

### Restart failing node
```bash
bash manage-cluster.sh stop 1
bash manage-cluster.sh start 1
bash manage-cluster.sh logs 1
```

### Reset cluster (keep data)
```bash
bash cleanup-cluster.sh    # Answer: yes, no (keep data)
bash deploy-cluster.sh
bash verify-cluster.sh
```

### Complete wipe and redeploy
```bash
bash cleanup-cluster.sh    # Answer: yes, yes (delete data)
bash deploy-cluster.sh
bash verify-cluster.sh
```

## Health Check One-Liner

```bash
bash verify-cluster.sh 2>&1 | grep -E "(✓|✗|OK|FAILED)"
```

## Files and Locations

### On Control Machine
- Scripts: `/home/mustafa/telcobright-projects/orchestrix/images/shell-automations/kafka-cluster-docker/`

### On Target Servers
- Working dir: `~/kafka-node-{1,2,3}/`
- Docker compose: `~/kafka-node-{1,2,3}/docker-compose.yml`
- Data: `/var/lib/kafka/data/`
- Container: `kafka-node` (same name on all servers)

## Getting Help

```bash
# View full documentation
less README.md

# View usage examples
less example-usage.sh

# Script help
bash manage-cluster.sh          # Shows usage
bash setup-node.sh              # Shows usage
```

## Version Info

- Kafka Image: `confluentinc/cp-kafka:8.1.0`
- Mode: KRaft (no ZooKeeper)
- Network: Host mode with secondary IPs
- Replication: Factor 3, Min ISR 2

---

**Quick Tip:** Always run `bash verify-cluster.sh` after any changes to ensure cluster health!
