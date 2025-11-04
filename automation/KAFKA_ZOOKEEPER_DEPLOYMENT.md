# Kafka & Zookeeper Deployment Automation

## Quick Reference

### Deploy to Single Server
```bash
cd /home/mustafa/telcobright-projects/orchestrix

./automation/scripts/deploy-kafka-zookeeper.sh \
  <HOST> <USER> <PORT> <PASSWORD> true
```

### Deploy 3-Node Cluster
```bash
# Server 1
./automation/scripts/deploy-kafka-zookeeper.sh \
  123.200.0.50 tbsms 8210 "TB@l38800" true

# Server 2
./automation/scripts/deploy-kafka-zookeeper.sh \
  123.200.0.117 tbsms 8210 "TB@l38800" true

# Server 3
./automation/scripts/deploy-kafka-zookeeper.sh \
  123.200.0.51 tbsms 8210 "TB@l38800" true
```

## Prerequisites

### On Target Server (MUST be done first!)

1. **Upload orchestrix directory**:
   ```bash
   scp -P 8210 -r orchestrix user@host:/home/user/
   ```

2. **Build base images** (on each target server):
   ```bash
   ssh -p 8210 user@host

   # Build Zookeeper base image
   cd /home/user/orchestrix/images/containers/lxc/zookeeper
   ./startDefault.sh

   # Build Kafka base image
   cd /home/user/orchestrix/images/containers/lxc/kafka
   ./startDefault.sh
   ```

3. **Fix firewall if needed** (common issue on some servers):
   ```bash
   # Check if FORWARD chain blocks traffic
   sudo iptables -L FORWARD -n | grep "policy DROP"

   # If policy is DROP, add rule to allow container traffic
   sudo iptables -I FORWARD -i lxdbr0 -o lxdbr0 -j ACCEPT

   # Make permanent
   sudo apt-get install -y iptables-persistent
   sudo iptables-save > /etc/iptables/rules.v4
   ```

## File Locations

```
orchestrix/
├── automation/
│   ├── api/
│   │   ├── deployment/
│   │   │   ├── KafkaZookeeperDeployment.java  # Main automation
│   │   │   └── README.md                      # Detailed docs
│   │   └── infrastructure/
│   │       └── LxcPrerequisitesManager.java   # LXC setup
│   └── scripts/
│       └── deploy-kafka-zookeeper.sh          # Wrapper script
└── images/containers/lxc/
    ├── zookeeper/                             # Zookeeper container
    └── kafka/                                 # Kafka container
```

## Deployment Workflow

```
1. SSH Connect ──────────┐
                         │
2. Check LXC Setup ──────┤
                         │
3. Verify Base Images ───┤
                         │
4. Deploy Zookeeper ─────┤
                         │
5. Wait & Verify ────────┤
                         │
6. Generate Kafka Config ┤
                         │
7. Deploy Kafka ─────────┤
                         │
8. Wait & Verify ────────┤
                         │
9. Return Connection Info┘
```

## Successful Deployment Output

```
═══════════════════════════════════════════════════════
  Deployment Completed Successfully!
═══════════════════════════════════════════════════════

Zookeeper:
  IP:   10.91.217.236
  Port: 2181
  Connection: 10.91.217.236:2181

Kafka:
  IP:   10.91.217.7
  Port: 9092
  Connection: 10.91.217.7:9092

Verification Commands:
  lxc list
  lxc exec zookeeper-single -- systemctl status zookeeper
  lxc exec kafka-broker-1 -- systemctl status kafka
```

## Common Issues & Solutions

### ❌ Issue: "Base image not found"
**Cause**: Base images not built on target server
**Solution**: Build images first (see Prerequisites section above)

### ❌ Issue: "Kafka cannot connect to Zookeeper"
**Cause**: Firewall blocking container-to-container traffic
**Solution**: Add iptables rule (see Prerequisites section above)

### ❌ Issue: "No root device could be found"
**Cause**: LXC default profile missing devices
**Solution**: LxcPrerequisitesManager handles this automatically, but if it fails:
```bash
lxc profile device add default root disk path=/ pool=default
lxc profile device add default eth0 nic network=lxdbr0
```

### ❌ Issue: "Permission denied"
**Cause**: User doesn't have sudo access
**Solution**: Add user to sudoers or use a user with sudo rights

## Cluster Configuration

After deploying to multiple servers, use these connection strings in your applications:

### Zookeeper Ensemble
```
10.91.217.236:2181,10.189.241.196:2181,10.194.110.167:2181
```

### Kafka Bootstrap Servers
```
10.91.217.7:9092,10.189.241.28:9092,10.194.110.103:9092
```

### Application Configuration Examples

**Java (Spring Boot)**:
```yaml
spring:
  kafka:
    bootstrap-servers: 10.91.217.7:9092,10.189.241.28:9092,10.194.110.103:9092
```

**Python**:
```python
from kafka import KafkaProducer

producer = KafkaProducer(
    bootstrap_servers=['10.91.217.7:9092', '10.189.241.28:9092', '10.194.110.103:9092']
)
```

**Node.js**:
```javascript
const { Kafka } = require('kafkajs')

const kafka = new Kafka({
  brokers: ['10.91.217.7:9092', '10.189.241.28:9092', '10.194.110.103:9092']
})
```

## Service Management

### Check Status
```bash
ssh -p 8210 user@host "lxc exec zookeeper-single -- systemctl status zookeeper"
ssh -p 8210 user@host "lxc exec kafka-broker-1 -- systemctl status kafka"
```

### View Logs
```bash
ssh -p 8210 user@host "lxc exec zookeeper-single -- journalctl -u zookeeper -f"
ssh -p 8210 user@host "lxc exec kafka-broker-1 -- journalctl -u kafka -f"
```

### Restart Services
```bash
ssh -p 8210 user@host "lxc exec zookeeper-single -- systemctl restart zookeeper"
ssh -p 8210 user@host "lxc exec kafka-broker-1 -- systemctl restart kafka"
```

### Delete Containers (Clean Up)
```bash
ssh -p 8210 user@host "lxc delete zookeeper-single --force"
ssh -p 8210 user@host "lxc delete kafka-broker-1 --force"
```

## Testing the Deployment

```bash
# Create test topic
ssh -p 8210 user@host "lxc exec kafka-broker-1 -- \
  /opt/kafka/bin/kafka-topics.sh --create \
  --topic test-topic --partitions 3 --replication-factor 1 \
  --bootstrap-server localhost:9092"

# List topics
ssh -p 8210 user@host "lxc exec kafka-broker-1 -- \
  /opt/kafka/bin/kafka-topics.sh --list \
  --bootstrap-server localhost:9092"

# Produce messages
ssh -p 8210 user@host "lxc exec kafka-broker-1 -- \
  /opt/kafka/bin/kafka-console-producer.sh \
  --topic test-topic --bootstrap-server localhost:9092"

# Consume messages
ssh -p 8210 user@host "lxc exec kafka-broker-1 -- \
  /opt/kafka/bin/kafka-console-consumer.sh \
  --topic test-topic --from-beginning \
  --bootstrap-server localhost:9092"
```

## Architecture

```
┌─────────────────────────────────────────────┐
│  Local Machine                              │
│  ┌──────────────────────────────────────┐  │
│  │ KafkaZookeeperDeployment.java        │  │
│  │  - SSH Connection                    │  │
│  │  - Command Execution                 │  │
│  │  - Configuration Generation          │  │
│  └──────────────────────────────────────┘  │
└─────────────────┬───────────────────────────┘
                  │ SSH (JSch)
                  ▼
┌─────────────────────────────────────────────┐
│  Remote Server (123.200.0.50:8210)          │
│                                             │
│  ┌─────────────────────────────────────┐   │
│  │ LXC Bridge (lxdbr0)                 │   │
│  │  10.91.217.0/24                     │   │
│  │                                     │   │
│  │  ┌──────────────┐  ┌─────────────┐ │   │
│  │  │ zookeeper-   │  │ kafka-      │ │   │
│  │  │ single       │  │ broker-1    │ │   │
│  │  │ :2181        │◄─┤ :9092       │ │   │
│  │  └──────────────┘  └─────────────┘ │   │
│  └─────────────────────────────────────┘   │
└─────────────────────────────────────────────┘
```

## Security Notes

- Passwords are passed as command-line arguments (visible in process list)
- For production, consider using SSH keys instead
- Containers are not exposed outside the host (only accessible via lxdbr0)
- Services run as root inside containers
- No authentication configured on Kafka/Zookeeper (suitable for dev/test only)

## Performance Notes

- Each container gets 1GB heap for Kafka
- Zookeeper uses default memory settings
- Single broker per server (not clustered Kafka)
- Standalone Zookeeper (not clustered)
- Suitable for development and testing, not production

## Future Improvements

- [ ] Support for SSH key-based authentication
- [ ] Automatic firewall rule application
- [ ] Clustered Kafka configuration
- [ ] Clustered Zookeeper ensemble
- [ ] SSL/TLS configuration
- [ ] SASL authentication
- [ ] Monitoring integration
- [ ] Backup and restore automation

## Success Rate

Tested on:
- ✓ Ubuntu 20.04 LTS
- ✓ Ubuntu 22.04 LTS
- ✓ Different network configurations (ZFS, Dir storage)
- ✓ Servers with existing LXC containers
- ✓ Fresh server installations

Known limitations:
- Requires sudo access
- Assumes Debian-based Linux
- Requires existing orchestrix directory on target
- Requires base images pre-built

---

**Created**: 2025-10-29
**Last Updated**: 2025-10-29
**Status**: Production Ready (Development/Testing use)
