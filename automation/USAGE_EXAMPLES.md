# Kafka & Zookeeper Deployment - Usage Examples

## Table of Contents
1. [Single Server Deployment](#single-server-deployment)
2. [3-Node Cluster Deployment](#3-node-cluster-deployment)
3. [With Environment Variables](#with-environment-variables)
4. [Programmatic Usage (Java)](#programmatic-usage-java)
5. [Post-Deployment Testing](#post-deployment-testing)
6. [Common Scenarios](#common-scenarios)

---

## Single Server Deployment

### Basic Deployment
```bash
cd /home/mustafa/telcobright-projects/orchestrix

./automation/scripts/deploy-kafka-zookeeper.sh \
  192.168.1.100 ubuntu 22 "password" true
```

### With Log Output
```bash
./automation/scripts/deploy-kafka-zookeeper.sh \
  192.168.1.100 ubuntu 22 "password" true \
  2>&1 | tee /tmp/deployment-$(date +%Y%m%d-%H%M%S).log
```

### Production Server Example
```bash
./automation/scripts/deploy-kafka-zookeeper.sh \
  123.200.0.50 tbsms 8210 "TB@l38800" true
```

---

## 3-Node Cluster Deployment

### Sequential Deployment
```bash
# Deploy to each server one by one
./automation/scripts/deploy-kafka-zookeeper.sh \
  123.200.0.50 tbsms 8210 "TB@l38800" true \
  > /tmp/server1-deploy.log 2>&1

./automation/scripts/deploy-kafka-zookeeper.sh \
  123.200.0.117 tbsms 8210 "TB@l38800" true \
  > /tmp/server2-deploy.log 2>&1

./automation/scripts/deploy-kafka-zookeeper.sh \
  123.200.0.51 tbsms 8210 "TB@l38800" true \
  > /tmp/server3-deploy.log 2>&1

# Extract connection information
echo "Zookeeper Ensemble:"
grep "Zookeeper IP:" /tmp/server*-deploy.log | awk '{print $3":2181"}' | paste -sd ","

echo "Kafka Bootstrap Servers:"
grep "Kafka IP:" /tmp/server*-deploy.log | awk '{print $3":9092"}' | paste -sd ","
```

### Parallel Deployment (Background)
```bash
# Start all deployments in parallel
./automation/scripts/deploy-kafka-zookeeper.sh \
  123.200.0.50 tbsms 8210 "TB@l38800" true \
  > /tmp/server1-deploy.log 2>&1 &

./automation/scripts/deploy-kafka-zookeeper.sh \
  123.200.0.117 tbsms 8210 "TB@l38800" true \
  > /tmp/server2-deploy.log 2>&1 &

./automation/scripts/deploy-kafka-zookeeper.sh \
  123.200.0.51 tbsms 8210 "TB@l38800" true \
  > /tmp/server3-deploy.log 2>&1 &

# Wait for all to complete
wait

# Check status
echo "Deployment Status:"
for i in 1 2 3; do
  echo -n "Server $i: "
  if grep -q "Deployment Completed Successfully" /tmp/server${i}-deploy.log; then
    echo "✓ Success"
  else
    echo "✗ Failed"
  fi
done
```

---

## With Environment Variables

### Using .env File
```bash
# Create .env file
cat > kafka-deploy.env << 'EOF'
export DEPLOY_HOST=123.200.0.50
export DEPLOY_USER=tbsms
export DEPLOY_PORT=8210
export DEPLOY_PASSWORD="TB@l38800"
export DEPLOY_USE_SUDO=true
EOF

# Source and deploy
source kafka-deploy.env
./automation/scripts/deploy-kafka-zookeeper.sh
```

### Multi-Server with Variables
```bash
# Server configs
declare -A servers=(
  [server1]="123.200.0.50 tbsms 8210 TB@l38800"
  [server2]="123.200.0.117 tbsms 8210 TB@l38800"
  [server3]="123.200.0.51 tbsms 8210 TB@l38800"
)

# Deploy to all
for name in "${!servers[@]}"; do
  echo "Deploying to $name..."
  ./automation/scripts/deploy-kafka-zookeeper.sh \
    ${servers[$name]} true \
    > /tmp/${name}-deploy.log 2>&1 &
done

wait
echo "All deployments complete!"
```

---

## Programmatic Usage (Java)

### Simple Deployment
```java
import automation.api.deployment.KafkaZookeeperDeployment;

public class DeployExample {
    public static void main(String[] args) {
        // Create deployment
        KafkaZookeeperDeployment deployment =
            new KafkaZookeeperDeployment(
                "123.200.0.50",
                "tbsms",
                8210,
                "TB@l38800",
                true
            );

        // Execute
        var result = deployment.deploy();

        // Check result
        if (result.isSuccess()) {
            System.out.println("Success!");
            System.out.println("Zookeeper: " +
                result.getZookeeperIp() + ":" +
                result.getZookeeperPort());
            System.out.println("Kafka: " +
                result.getKafkaIp() + ":" +
                result.getKafkaPort());
        } else {
            System.err.println("Failed: " + result.getError());
        }
    }
}
```

### Multi-Server Deployment
```java
import automation.api.deployment.KafkaZookeeperDeployment;
import java.util.*;
import java.util.concurrent.*;

public class ClusterDeploy {
    public static void main(String[] args) throws Exception {
        // Server configurations
        List<ServerConfig> servers = Arrays.asList(
            new ServerConfig("123.200.0.50", "tbsms", 8210, "TB@l38800"),
            new ServerConfig("123.200.0.117", "tbsms", 8210, "TB@l38800"),
            new ServerConfig("123.200.0.51", "tbsms", 8210, "TB@l38800")
        );

        // Deploy in parallel
        ExecutorService executor = Executors.newFixedThreadPool(3);
        List<Future<KafkaZookeeperDeployment.DeploymentResult>> futures =
            new ArrayList<>();

        for (ServerConfig config : servers) {
            futures.add(executor.submit(() -> {
                KafkaZookeeperDeployment deployment =
                    new KafkaZookeeperDeployment(
                        config.host, config.user, config.port,
                        config.password, true
                    );
                return deployment.deploy();
            }));
        }

        // Collect results
        List<String> zookeeperIps = new ArrayList<>();
        List<String> kafkaIps = new ArrayList<>();

        for (Future<KafkaZookeeperDeployment.DeploymentResult> future : futures) {
            var result = future.get();
            if (result.isSuccess()) {
                zookeeperIps.add(result.getZookeeperIp() + ":2181");
                kafkaIps.add(result.getKafkaIp() + ":9092");
            }
        }

        executor.shutdown();

        // Print connection strings
        System.out.println("Zookeeper Ensemble: " +
            String.join(",", zookeeperIps));
        System.out.println("Kafka Bootstrap: " +
            String.join(",", kafkaIps));
    }

    static class ServerConfig {
        String host, user, password;
        int port;

        ServerConfig(String host, String user, int port, String password) {
            this.host = host;
            this.user = user;
            this.port = port;
            this.password = password;
        }
    }
}
```

---

## Post-Deployment Testing

### Quick Verification
```bash
# Set server details
SERVER="123.200.0.50"
USER="tbsms"
PORT="8210"
PASSWORD="TB@l38800"

# Function for SSH commands
ssh_exec() {
    sshpass -p "$PASSWORD" ssh -o StrictHostKeyChecking=no -p $PORT $USER@$SERVER "$1"
}

# Check containers
ssh_exec "lxc list | grep -E '(zookeeper|kafka)'"

# Check Zookeeper
ssh_exec "lxc exec zookeeper-single -- systemctl is-active zookeeper"

# Check Kafka
ssh_exec "lxc exec kafka-broker-1 -- systemctl is-active kafka"

# List topics
ssh_exec "lxc exec kafka-broker-1 -- \
  /opt/kafka/bin/kafka-topics.sh --list --bootstrap-server localhost:9092"
```

### Create and Test Topic
```bash
# Create test topic
sshpass -p "$PASSWORD" ssh -o StrictHostKeyChecking=no -p $PORT $USER@$SERVER \
  "lxc exec kafka-broker-1 -- /opt/kafka/bin/kafka-topics.sh \
    --create --topic test-deployment \
    --partitions 3 --replication-factor 1 \
    --bootstrap-server localhost:9092"

# Describe topic
sshpass -p "$PASSWORD" ssh -o StrictHostKeyChecking=no -p $PORT $USER@$SERVER \
  "lxc exec kafka-broker-1 -- /opt/kafka/bin/kafka-topics.sh \
    --describe --topic test-deployment \
    --bootstrap-server localhost:9092"

# Produce test message
echo "Hello from deployment test" | \
sshpass -p "$PASSWORD" ssh -o StrictHostKeyChecking=no -p $PORT $USER@$SERVER \
  "lxc exec kafka-broker-1 -- /opt/kafka/bin/kafka-console-producer.sh \
    --topic test-deployment --bootstrap-server localhost:9092"

# Consume test message
sshpass -p "$PASSWORD" ssh -o StrictHostKeyChecking=no -p $PORT $USER@$SERVER \
  "lxc exec kafka-broker-1 -- timeout 5 /opt/kafka/bin/kafka-console-consumer.sh \
    --topic test-deployment --from-beginning \
    --bootstrap-server localhost:9092"
```

### Performance Test
```bash
# Produce 100k messages
sshpass -p "$PASSWORD" ssh -o StrictHostKeyChecking=no -p $PORT $USER@$SERVER \
  "lxc exec kafka-broker-1 -- /opt/kafka/bin/kafka-producer-perf-test.sh \
    --topic perf-test --num-records 100000 --record-size 1024 \
    --throughput -1 --producer-props bootstrap.servers=localhost:9092"

# Consume and measure
sshpass -p "$PASSWORD" ssh -o StrictHostKeyChecking=no -p $PORT $USER@$SERVER \
  "lxc exec kafka-broker-1 -- /opt/kafka/bin/kafka-consumer-perf-test.sh \
    --topic perf-test --messages 100000 \
    --bootstrap-server localhost:9092"
```

---

## Common Scenarios

### Scenario 1: Dev Environment Setup
```bash
#!/bin/bash
# Setup Kafka cluster for development environment

echo "Setting up Kafka development cluster..."

# Deploy to local dev servers
./automation/scripts/deploy-kafka-zookeeper.sh \
  dev-server-1.local admin 22 "devpass" true

# Get connection info
KAFKA_IP=$(grep "Kafka IP:" /tmp/last-deploy.log | awk '{print $3}')
echo "export KAFKA_BOOTSTRAP=$KAFKA_IP:9092" >> ~/.bashrc

echo "Dev cluster ready! Connection: $KAFKA_IP:9092"
```

### Scenario 2: CI/CD Pipeline
```yaml
# .gitlab-ci.yml
deploy-kafka-test:
  stage: deploy
  script:
    - cd orchestrix
    - |
      ./automation/scripts/deploy-kafka-zookeeper.sh \
        $TEST_SERVER $TEST_USER $TEST_PORT "$TEST_PASSWORD" true
    - echo "KAFKA_BOOTSTRAP=$(extract-kafka-ip)" > kafka.env
  artifacts:
    reports:
      dotenv: kafka.env
  only:
    - test-branch
```

### Scenario 3: Disaster Recovery
```bash
#!/bin/bash
# Redeploy Kafka cluster after failure

SERVERS=("123.200.0.50" "123.200.0.117" "123.200.0.51")
USER="tbsms"
PORT="8210"
PASSWORD="TB@l38800"

echo "Starting disaster recovery deployment..."

for SERVER in "${SERVERS[@]}"; do
  echo "Deploying to $SERVER..."

  # Clean up old containers first
  sshpass -p "$PASSWORD" ssh -p $PORT $USER@$SERVER \
    "lxc delete kafka-broker-1 --force 2>/dev/null; \
     lxc delete zookeeper-single --force 2>/dev/null" || true

  # Deploy fresh
  ./automation/scripts/deploy-kafka-zookeeper.sh \
    $SERVER $USER $PORT "$PASSWORD" true \
    > /tmp/recovery-${SERVER}.log 2>&1 &
done

wait
echo "All servers redeployed!"
```

### Scenario 4: Staging Environment
```bash
#!/bin/bash
# Deploy to staging with validation

set -e

# Deploy
./automation/scripts/deploy-kafka-zookeeper.sh \
  staging.example.com deploy 22 "$STAGING_PASSWORD" true \
  | tee /tmp/staging-deploy.log

# Validate
if ! grep -q "Deployment Completed Successfully" /tmp/staging-deploy.log; then
  echo "Deployment failed!"
  exit 1
fi

# Extract IPs
KAFKA_IP=$(grep "Kafka IP:" /tmp/staging-deploy.log | awk '{print $3}')
ZK_IP=$(grep "Zookeeper IP:" /tmp/staging-deploy.log | awk '{print $3}')

# Update application config
cat > staging-config.yaml << EOF
kafka:
  bootstrap-servers: ${KAFKA_IP}:9092
zookeeper:
  connect: ${ZK_IP}:2181
EOF

echo "Staging deployment complete!"
echo "Config written to staging-config.yaml"
```

### Scenario 5: Monitoring Integration
```bash
#!/bin/bash
# Deploy with monitoring setup

# Deploy Kafka
./automation/scripts/deploy-kafka-zookeeper.sh \
  $HOST $USER $PORT "$PASSWORD" true

# Enable JMX metrics on Kafka
sshpass -p "$PASSWORD" ssh -p $PORT $USER@$HOST \
  "lxc exec kafka-broker-1 -- \
    sed -i 's/KAFKA_JMX_OPTS=.*/KAFKA_JMX_OPTS=\"-Dcom.sun.management.jmxremote.port=9999\"/' \
    /etc/systemd/system/kafka.service"

# Restart to apply
sshpass -p "$PASSWORD" ssh -p $PORT $USER@$HOST \
  "lxc exec kafka-broker-1 -- systemctl restart kafka"

echo "Kafka deployed with JMX on port 9999"
```

---

## Troubleshooting Examples

### Check Deployment Logs
```bash
# View detailed logs
tail -f /tmp/server1-deploy.log

# Search for errors
grep -i error /tmp/server*-deploy.log

# Extract key info
grep -E "(Zookeeper IP|Kafka IP|Error|Failed)" /tmp/server*-deploy.log
```

### Validate Connectivity
```bash
#!/bin/bash
# Test cluster connectivity

SERVERS=("123.200.0.50" "123.200.0.117" "123.200.0.51")

for SERVER in "${SERVERS[@]}"; do
  echo "Testing $SERVER..."

  # Get container IPs
  ZK_IP=$(sshpass -p "$PASSWORD" ssh -p $PORT $USER@$SERVER \
    "lxc exec zookeeper-single -- hostname -I | awk '{print \$1}'")

  KAFKA_IP=$(sshpass -p "$PASSWORD" ssh -p $PORT $USER@$SERVER \
    "lxc exec kafka-broker-1 -- hostname -I | awk '{print \$1}'")

  # Test ping
  sshpass -p "$PASSWORD" ssh -p $PORT $USER@$SERVER \
    "lxc exec kafka-broker-1 -- ping -c 1 $ZK_IP" && \
    echo "  ✓ Kafka can reach Zookeeper" || \
    echo "  ✗ Connectivity issue"
done
```

---

**Last Updated**: 2025-10-29
