# Kafka & Zookeeper Deployment Results

## Deployment Summary

**Date:** 2025-10-18
**Method:** Java automation framework via SSH
**Status:** ✓ SUCCESS

---

## Deployed Containers

### 1. Zookeeper Container

**Container Details:**
- **Name:** `zookeeper-single`
- **Base Image:** `zookeeper-base`
- **Version:** Apache Zookeeper 3.9.4
- **IP Address:** `10.10.199.44`
- **Status:** RUNNING

**Ports:**
- **Client Port:** 2181 (for client connections)
- **Peer Port:** 2888 (for follower-leader communication)
- **Leader Port:** 3888 (for leader election)

**Configuration:**
- Server ID: 1
- Data Directory: `/data/zookeeper`
- Heap Size: 512MB (Xms512M/Xmx512M)
- Auto-start: Enabled

**Connection String:**
```
10.10.199.44:2181
```

---

### 2. Kafka Broker Container

**Container Details:**
- **Name:** `kafka-broker-1`
- **Base Image:** `kafka-base`
- **Version:** Apache Kafka 3.9.0
- **IP Address:** `10.10.199.201`
- **Status:** RUNNING

**Ports:**
- **Broker Port:** 9092 (PLAINTEXT listener)

**Configuration:**
- Broker ID: 1
- Zookeeper Connect: `10.10.199.44:2181/kafka`
- Listeners: `PLAINTEXT://0.0.0.0:9092`
- Advertised Listeners: `PLAINTEXT://kafka-broker-1:9092`
- Log Directory: `/data/kafka-logs`
- Heap Size: 1GB (Xms1G/Xmx1G)
- Auto-start: Enabled

**Bootstrap Server:**
```
10.10.199.201:9092
```

---

## Verification Commands

### Check Container Status
```bash
# List all containers
lxc list

# Check Zookeeper container
lxc info zookeeper-single

# Check Kafka container
lxc info kafka-broker-1
```

### Check Service Status
```bash
# Zookeeper service status
lxc exec zookeeper-single -- systemctl status zookeeper

# Kafka service status
lxc exec kafka-broker-1 -- systemctl status kafka
```

### View Logs
```bash
# Zookeeper logs
lxc exec zookeeper-single -- journalctl -u zookeeper -f

# Kafka logs
lxc exec kafka-broker-1 -- journalctl -u kafka -f
```

---

## Testing the Deployment

### Test 1: List Topics
```bash
lxc exec kafka-broker-1 -- /opt/kafka/bin/kafka-topics.sh --list --bootstrap-server localhost:9092
```

**Expected Output:** Empty list (no topics created yet) or list of topics

### Test 2: Create a Test Topic
```bash
lxc exec kafka-broker-1 -- /opt/kafka/bin/kafka-topics.sh \
  --create \
  --topic test-topic \
  --bootstrap-server localhost:9092 \
  --partitions 1 \
  --replication-factor 1
```

**Expected Output:** `Created topic test-topic.`

### Test 3: Describe Topic
```bash
lxc exec kafka-broker-1 -- /opt/kafka/bin/kafka-topics.sh \
  --describe \
  --topic test-topic \
  --bootstrap-server localhost:9092
```

### Test 4: Produce Messages
```bash
lxc exec kafka-broker-1 -- /opt/kafka/bin/kafka-console-producer.sh \
  --topic test-topic \
  --bootstrap-server localhost:9092
```
Type messages and press Enter. Press Ctrl+C to exit.

### Test 5: Consume Messages
```bash
lxc exec kafka-broker-1 -- /opt/kafka/bin/kafka-console-consumer.sh \
  --topic test-topic \
  --from-beginning \
  --bootstrap-server localhost:9092
```

---

## Connection Details for Applications

### From Host Machine (Outside Containers)

**Zookeeper:**
```
Host: 10.10.199.44
Port: 2181
Connection String: 10.10.199.44:2181
```

**Kafka:**
```
Host: 10.10.199.201
Port: 9092
Bootstrap Server: 10.10.199.201:9092
```

### From Within LXC Network

**Zookeeper:**
```
Connection String: zookeeper-single:2181
```

**Kafka:**
```
Bootstrap Server: kafka-broker-1:9092
```

### Python Example
```python
from kafka import KafkaProducer, KafkaConsumer

# Producer
producer = KafkaProducer(
    bootstrap_servers=['10.10.199.201:9092'],
    value_serializer=lambda v: json.dumps(v).encode('utf-8')
)

# Consumer
consumer = KafkaConsumer(
    'test-topic',
    bootstrap_servers=['10.10.199.201:9092'],
    auto_offset_reset='earliest',
    value_deserializer=lambda m: json.loads(m.decode('utf-8'))
)
```

### Java Example
```java
Properties props = new Properties();
props.put("bootstrap.servers", "10.10.199.201:9092");
props.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
props.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");

KafkaProducer<String, String> producer = new KafkaProducer<>(props);
```

---

## Deployment Automation

The deployment was performed using the Orchestrix Java automation framework:

**Location:** `/home/mustafa/telcobright-projects/orchestrix/automation/api/deployment/KafkaZookeeperDeployment.java`

**SSH Credentials Used:**
- Host: 127.0.0.1 (localhost)
- User: mustafa
- Port: 22
- Authentication: Password-based
- Sudo: Required for LXC operations

**Deployment Steps Executed:**
1. ✓ Established SSH connection
2. ✓ Verified LXC prerequisites (LXD, storage pool, network)
3. ✓ Checked base images availability
4. ✓ Deployed Zookeeper container
5. ✓ Verified Zookeeper service status
6. ✓ Created Kafka configuration with Zookeeper IP
7. ✓ Deployed Kafka container
8. ✓ Verified Kafka service status and Zookeeper connectivity

---

## Container Management

### Start Containers
```bash
lxc start zookeeper-single
lxc start kafka-broker-1
```

### Stop Containers
```bash
lxc stop kafka-broker-1
lxc stop zookeeper-single
```

### Restart Containers
```bash
lxc restart zookeeper-single
lxc restart kafka-broker-1
```

### Delete Containers
```bash
# Stop and delete Kafka first
lxc delete kafka-broker-1 --force

# Then delete Zookeeper
lxc delete zookeeper-single --force
```

### Access Container Shell
```bash
# Zookeeper
lxc exec zookeeper-single -- bash

# Kafka
lxc exec kafka-broker-1 -- bash
```

---

## Troubleshooting

### Zookeeper Issues

**Check if Zookeeper is listening:**
```bash
lxc exec zookeeper-single -- netstat -tuln | grep 2181
```

**Check Zookeeper data directory:**
```bash
lxc exec zookeeper-single -- ls -la /data/zookeeper
```

**Restart Zookeeper service:**
```bash
lxc exec zookeeper-single -- systemctl restart zookeeper
```

### Kafka Issues

**Check if Kafka is listening:**
```bash
lxc exec kafka-broker-1 -- netstat -tuln | grep 9092
```

**Check Kafka logs for errors:**
```bash
lxc exec kafka-broker-1 -- journalctl -u kafka -n 100 --no-pager
```

**Check Kafka-Zookeeper connection:**
```bash
lxc exec kafka-broker-1 -- cat /etc/kafka/runtime.conf
```

**Restart Kafka service:**
```bash
lxc exec kafka-broker-1 -- systemctl restart kafka
```

### Network Issues

**Test connectivity from Kafka to Zookeeper:**
```bash
lxc exec kafka-broker-1 -- ping -c 3 10.10.199.44
lxc exec kafka-broker-1 -- nc -zv 10.10.199.44 2181
```

**Test connectivity from host to containers:**
```bash
ping -c 3 10.10.199.44
ping -c 3 10.10.199.201
```

---

## Performance Tuning

### Increase Heap Size

**Zookeeper:**
Edit `/home/mustafa/telcobright-projects/orchestrix/images/lxc/zookeeper/templates/sample.conf`:
```bash
ZOO_HEAP_OPTS="-Xms1G -Xmx1G"
```

**Kafka:**
Edit `/home/mustafa/telcobright-projects/orchestrix/images/lxc/kafka/templates/runtime.conf`:
```bash
KAFKA_HEAP_OPTS="-Xms2G -Xmx2G"
```

Then recreate the containers.

---

## Next Steps

1. **Create additional topics** for your application needs
2. **Configure retention policies** for topics
3. **Set up monitoring** using Prometheus and Grafana
4. **Deploy Kafka Connect** for data integration
5. **Deploy Kafka Streams** applications for stream processing
6. **Configure security** (SSL/TLS, SASL authentication)
7. **Set up multi-broker cluster** for high availability

---

## Additional Resources

- **Zookeeper Documentation:** https://zookeeper.apache.org/doc/r3.9.4/
- **Kafka Documentation:** https://kafka.apache.org/39/documentation.html
- **Orchestrix Container Scaffolding:** `/home/mustafa/telcobright-projects/orchestrix/images/lxc/CONTAINER_SCAFFOLDING_STANDARD.md`

---

## Deployment Log

All deployment operations were logged and can be reviewed in:
- Container logs: `journalctl -u <service-name>`
- LXC logs: `/var/log/lxd/`
- Deployment script output: Captured during execution

**Deployment completed successfully on:** 2025-10-18 19:27:08

**Total deployment time:** ~3 minutes
- Zookeeper: ~1.5 minutes
- Kafka: ~1.5 minutes
