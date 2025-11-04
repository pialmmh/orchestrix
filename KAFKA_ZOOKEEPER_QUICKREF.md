# Kafka & Zookeeper Quick Reference

## Connection Details

### Zookeeper
- **Container:** zookeeper-single
- **IP:** 10.10.199.44
- **Port:** 2181
- **Connection:** `10.10.199.44:2181`

### Kafka
- **Container:** kafka-broker-1
- **IP:** 10.10.199.201
- **Port:** 9092
- **Bootstrap Server:** `10.10.199.201:9092`

---

## Quick Commands

### Container Management
```bash
# List containers
lxc list | grep -E "zookeeper|kafka"

# Start services
lxc start zookeeper-single
lxc start kafka-broker-1

# Stop services
lxc stop kafka-broker-1
lxc stop zookeeper-single

# Access shell
lxc exec zookeeper-single -- bash
lxc exec kafka-broker-1 -- bash
```

### Service Management
```bash
# Check status
lxc exec zookeeper-single -- systemctl status zookeeper
lxc exec kafka-broker-1 -- systemctl status kafka

# Restart services
lxc exec zookeeper-single -- systemctl restart zookeeper
lxc exec kafka-broker-1 -- systemctl restart kafka

# View logs
lxc exec zookeeper-single -- journalctl -u zookeeper -f
lxc exec kafka-broker-1 -- journalctl -u kafka -f
```

### Kafka Operations
```bash
# List topics
lxc exec kafka-broker-1 -- /opt/kafka/bin/kafka-topics.sh --list --bootstrap-server localhost:9092

# Create topic
lxc exec kafka-broker-1 -- /opt/kafka/bin/kafka-topics.sh --create --topic my-topic --bootstrap-server localhost:9092 --partitions 3 --replication-factor 1

# Describe topic
lxc exec kafka-broker-1 -- /opt/kafka/bin/kafka-topics.sh --describe --topic my-topic --bootstrap-server localhost:9092

# Delete topic
lxc exec kafka-broker-1 -- /opt/kafka/bin/kafka-topics.sh --delete --topic my-topic --bootstrap-server localhost:9092

# Produce messages
lxc exec kafka-broker-1 -- /opt/kafka/bin/kafka-console-producer.sh --topic my-topic --bootstrap-server localhost:9092

# Consume messages
lxc exec kafka-broker-1 -- /opt/kafka/bin/kafka-console-consumer.sh --topic my-topic --from-beginning --bootstrap-server localhost:9092
```

---

## Application Connection Examples

### Python
```python
from kafka import KafkaProducer, KafkaConsumer
producer = KafkaProducer(bootstrap_servers=['10.10.199.201:9092'])
consumer = KafkaConsumer('my-topic', bootstrap_servers=['10.10.199.201:9092'])
```

### Java
```java
Properties props = new Properties();
props.put("bootstrap.servers", "10.10.199.201:9092");
KafkaProducer<String, String> producer = new KafkaProducer<>(props);
```

### Node.js (kafka-node)
```javascript
const kafka = require('kafka-node');
const client = new kafka.KafkaClient({kafkaHost: '10.10.199.201:9092'});
```

---

## Troubleshooting

### Test connectivity
```bash
ping 10.10.199.44
ping 10.10.199.201
lxc exec kafka-broker-1 -- nc -zv 10.10.199.44 2181
```

### View full logs
```bash
lxc exec zookeeper-single -- journalctl -u zookeeper -n 100 --no-pager
lxc exec kafka-broker-1 -- journalctl -u kafka -n 100 --no-pager
```

### Check network listeners
```bash
lxc exec zookeeper-single -- netstat -tuln | grep 2181
lxc exec kafka-broker-1 -- netstat -tuln | grep 9092
```

---

**Deployment Method:** Java automation via SSH
**Documentation:** `/home/mustafa/telcobright-projects/orchestrix/images/containers/lxc/kafka/DEPLOYMENT_RESULTS.md`
