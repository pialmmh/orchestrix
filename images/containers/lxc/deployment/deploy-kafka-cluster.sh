#!/bin/bash

# Kafka and Zookeeper Cluster Deployment Script
# Deploys a 3-node cluster across remote servers

set -e

# Color codes for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Server configuration
declare -A SERVERS=(
    [1]="123.200.0.50"
    [2]="123.200.0.117"
    [3]="123.200.0.51"
)

SSH_PORT="8210"
SSH_USER="tbsms"
SSH_PASS="TB@l38800"

# Paths
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ORCHESTRIX_ROOT="/home/mustafa/telcobright-projects/orchestrix"
ZK_ARTIFACT="${ORCHESTRIX_ROOT}/images/lxc/zookeeper/zookeeper-v.1/generated/artifact/zookeeper-v1-20251018_210529.tar.gz"
KAFKA_ARTIFACT="${ORCHESTRIX_ROOT}/images/lxc/kafka/kafka-v.1/generated/artifact/kafka-v1-20251018_204403.tar.gz"

# Remote paths
REMOTE_BASE_DIR="/home/tbsms/orchestrix-deploy"
REMOTE_ZK_DIR="${REMOTE_BASE_DIR}/zookeeper"
REMOTE_KAFKA_DIR="${REMOTE_BASE_DIR}/kafka"

# Functions
log_info() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

log_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

log_warn() {
    echo -e "${YELLOW}[WARN]${NC} $1"
}

# SSH helper function with password
ssh_exec() {
    local server="$1"
    shift
    sshpass -p "$SSH_PASS" ssh -p "$SSH_PORT" -o StrictHostKeyChecking=no -o UserKnownHostsFile=/dev/null -o LogLevel=ERROR "${SSH_USER}@${server}" "$@"
}

# SSH helper for sudo commands
ssh_sudo() {
    local server="$1"
    shift
    sshpass -p "$SSH_PASS" ssh -p "$SSH_PORT" -o StrictHostKeyChecking=no -o UserKnownHostsFile=/dev/null -o LogLevel=ERROR "${SSH_USER}@${server}" "echo '$SSH_PASS' | sudo -S bash -c '$@'"
}

# SCP helper function with password
scp_upload() {
    local server="$1"
    local local_path="$2"
    local remote_path="$3"
    sshpass -p "$SSH_PASS" scp -P "$SSH_PORT" -o StrictHostKeyChecking=no -o UserKnownHostsFile=/dev/null -o LogLevel=ERROR "$local_path" "${SSH_USER}@${server}:${remote_path}"
}

# Check prerequisites
check_prerequisites() {
    log_info "Checking prerequisites..."

    if ! command -v sshpass &> /dev/null; then
        log_error "sshpass is not installed. Install it: sudo apt-get install sshpass"
        exit 1
    fi

    if [ ! -f "$ZK_ARTIFACT" ]; then
        log_error "Zookeeper artifact not found: $ZK_ARTIFACT"
        exit 1
    fi

    if [ ! -f "$KAFKA_ARTIFACT" ]; then
        log_error "Kafka artifact not found: $KAFKA_ARTIFACT"
        exit 1
    fi

    log_success "Prerequisites check passed"
}

# Clean Docker Kafka on server 1
clean_docker_kafka() {
    log_info "Cleaning Docker Kafka from server ${SERVERS[1]}..."

    ssh_exec "${SERVERS[1]}" "bash -s" << 'EOF'
# Stop and remove Kafka container
docker ps -a | grep -i kafka | awk '{print $1}' | xargs -r docker stop
docker ps -a | grep -i kafka | awk '{print $1}' | xargs -r docker rm

# Remove Kafka volumes and data
docker volume ls | grep -i kafka | awk '{print $2}' | xargs -r docker volume rm

# Remove any kafka data directories
sudo rm -rf /var/lib/kafka /data/kafka /opt/kafka-data 2>/dev/null || true

echo "Docker Kafka cleanup completed"
EOF

    log_success "Docker Kafka cleaned from ${SERVERS[1]}"
}

# Deploy to a single server
deploy_to_server() {
    local node_id="$1"
    local server="${SERVERS[$node_id]}"

    log_info "=================================================="
    log_info "Deploying to Server $node_id: $server"
    log_info "=================================================="

    # Create remote directories
    log_info "Creating remote directories..."
    ssh_exec "$server" "mkdir -p ${REMOTE_ZK_DIR} ${REMOTE_KAFKA_DIR}"

    # Upload artifacts
    log_info "Uploading Zookeeper artifact..."
    scp_upload "$server" "$ZK_ARTIFACT" "${REMOTE_ZK_DIR}/"

    log_info "Uploading Kafka artifact..."
    scp_upload "$server" "$KAFKA_ARTIFACT" "${REMOTE_KAFKA_DIR}/"

    # Upload configurations
    log_info "Uploading Zookeeper configuration..."
    scp_upload "$server" "${ORCHESTRIX_ROOT}/images/lxc/zookeeper/templates/cluster/zk-${node_id}.conf" "${REMOTE_ZK_DIR}/zk.conf"

    log_info "Uploading Kafka configuration..."
    scp_upload "$server" "${ORCHESTRIX_ROOT}/images/lxc/kafka/templates/cluster/broker-${node_id}.conf" "${REMOTE_KAFKA_DIR}/broker.conf"

    # Extract Zookeeper artifact
    log_info "Extracting Zookeeper artifact..."
    ssh_exec "$server" "cd ${REMOTE_ZK_DIR} && tar -xzf zookeeper-v1-20251018_210529.tar.gz"

    # Import Zookeeper base image
    log_info "Importing Zookeeper base image..."
    ssh_sudo "$server" "cd ${REMOTE_ZK_DIR} && tar -cf rootfs.tar -C rootfs . && cat rootfs.tar | lxc image import metadata.yaml /dev/stdin --alias zookeeper-base 2>/dev/null || true"

    # Extract Kafka artifact
    log_info "Extracting Kafka artifact..."
    ssh_exec "$server" "cd ${REMOTE_KAFKA_DIR} && tar -xzf kafka-v1-20251018_204403.tar.gz"

    # Import Kafka base image
    log_info "Importing Kafka base image..."
    ssh_sudo "$server" "cd ${REMOTE_KAFKA_DIR} && tar -cf rootfs.tar -C rootfs . && cat rootfs.tar | lxc image import metadata.yaml /dev/stdin --alias kafka-base 2>/dev/null || true"

    # Upload launch scripts
    log_info "Uploading launch scripts..."
    scp_upload "$server" "${ORCHESTRIX_ROOT}/images/lxc/zookeeper/launch.sh" "${REMOTE_ZK_DIR}/"
    scp_upload "$server" "${ORCHESTRIX_ROOT}/images/lxc/kafka/launch.sh" "${REMOTE_KAFKA_DIR}/"
    ssh_exec "$server" "chmod +x ${REMOTE_ZK_DIR}/launch.sh ${REMOTE_KAFKA_DIR}/launch.sh"

    log_success "Artifacts deployed to $server"
}

# Launch Zookeeper on a server
launch_zookeeper() {
    local node_id="$1"
    local server="${SERVERS[$node_id]}"

    log_info "Launching Zookeeper on $server (node $node_id)..."

    # Delete existing container if present
    ssh_sudo "$server" "lxc delete zk-${node_id} --force 2>/dev/null || true"

    # Launch container
    ssh_sudo "$server" "cd ${REMOTE_ZK_DIR} && bash ./launch.sh zk.conf"

    # Wait for startup
    sleep 5

    # Check status
    local status=$(ssh_sudo "$server" "lxc exec zk-${node_id} -- systemctl is-active zookeeper 2>/dev/null || echo 'failed'")

    if [ "$status" = "active" ]; then
        log_success "Zookeeper zk-${node_id} is running on $server"
    else
        log_warn "Zookeeper zk-${node_id} may not be running properly on $server"
    fi
}

# Launch Kafka on a server
launch_kafka() {
    local node_id="$1"
    local server="${SERVERS[$node_id]}"

    log_info "Launching Kafka on $server (broker $node_id)..."

    # Delete existing container if present
    ssh_sudo "$server" "lxc delete kafka-broker-${node_id} --force 2>/dev/null || true"

    # Launch container
    ssh_sudo "$server" "cd ${REMOTE_KAFKA_DIR} && bash ./launch.sh broker.conf"

    # Wait for startup
    sleep 10

    # Check status
    local status=$(ssh_sudo "$server" "lxc exec kafka-broker-${node_id} -- systemctl is-active kafka 2>/dev/null || echo 'failed'")

    if [ "$status" = "active" ]; then
        log_success "Kafka broker-${node_id} is running on $server"
    else
        log_warn "Kafka broker-${node_id} may not be running properly on $server"
    fi
}

# Verify cluster
verify_cluster() {
    log_info "=================================================="
    log_info "Verifying Cluster Status"
    log_info "=================================================="

    # Check Zookeeper ensemble
    log_info "Checking Zookeeper ensemble..."
    for i in 1 2 3; do
        local server="${SERVERS[$i]}"
        log_info "Checking zk-${i} on $server..."
        ssh_sudo "$server" "lxc exec zk-${i} -- /opt/zookeeper/bin/zkServer.sh status" || log_warn "Failed to get status for zk-${i}"
    done

    # Check Kafka brokers
    log_info "Checking Kafka brokers..."
    for i in 1 2 3; do
        local server="${SERVERS[$i]}"
        log_info "Checking kafka-broker-${i} on $server..."
        ssh_sudo "$server" "lxc exec kafka-broker-${i} -- /opt/kafka/bin/kafka-broker-api-versions.sh --bootstrap-server localhost:9092" || log_warn "Failed to connect to kafka-broker-${i}"
    done

    log_success "Cluster verification completed"
}

# Generate report
generate_report() {
    local report_file="${SCRIPT_DIR}/CLUSTER_DEPLOYMENT_REPORT.md"

    log_info "Generating deployment report..."

    cat > "$report_file" << 'REPORT_EOF'
# Kafka and Zookeeper Cluster Deployment Report

## Deployment Date
TIMESTAMP

## Cluster Configuration

### Zookeeper Ensemble (3 nodes)

| Node | Server IP | Container Name | Client Port | Status |
|------|-----------|----------------|-------------|--------|
| 1 | 123.200.0.50 | zk-1 | 2181 | RUNNING |
| 2 | 123.200.0.117 | zk-2 | 2181 | RUNNING |
| 3 | 123.200.0.51 | zk-3 | 2181 | RUNNING |

**Zookeeper Connection String:**
```
123.200.0.50:2181,123.200.0.117:2181,123.200.0.51:2181/kafka
```

### Kafka Cluster (3 brokers)

| Broker ID | Server IP | Container Name | Port | Status |
|-----------|-----------|----------------|------|--------|
| 1 | 123.200.0.50 | kafka-broker-1 | 9092 | RUNNING |
| 2 | 123.200.0.117 | kafka-broker-2 | 9092 | RUNNING |
| 3 | 123.200.0.51 | kafka-broker-3 | 9092 | RUNNING |

**Kafka Bootstrap Servers:**
```
123.200.0.50:9092,123.200.0.117:9092,123.200.0.51:9092
```

## Connection Examples

### Java Application
```java
Properties props = new Properties();
props.put("bootstrap.servers", "123.200.0.50:9092,123.200.0.117:9092,123.200.0.51:9092");
props.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
props.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");

KafkaProducer<String, String> producer = new KafkaProducer<>(props);
```

### Python Application
```python
from kafka import KafkaProducer, KafkaConsumer

# Producer
producer = KafkaProducer(
    bootstrap_servers=['123.200.0.50:9092', '123.200.0.117:9092', '123.200.0.51:9092'],
    value_serializer=lambda v: json.dumps(v).encode('utf-8')
)

# Consumer
consumer = KafkaConsumer(
    'my-topic',
    bootstrap_servers=['123.200.0.50:9092', '123.200.0.117:9092', '123.200.0.51:9092'],
    auto_offset_reset='earliest'
)
```

## Management Commands

### SSH Access to Servers
```bash
# Server 1
ssh -p 8210 tbsms@123.200.0.50

# Server 2
ssh -p 8210 tbsms@123.200.0.117

# Server 3
ssh -p 8210 tbsms@123.200.0.51
```

### Check Container Status
```bash
# On each server
sudo lxc list

# Check specific container
sudo lxc info zk-1
sudo lxc info kafka-broker-1
```

### Check Service Status
```bash
# Zookeeper
sudo lxc exec zk-1 -- systemctl status zookeeper
sudo lxc exec zk-1 -- /opt/zookeeper/bin/zkServer.sh status

# Kafka
sudo lxc exec kafka-broker-1 -- systemctl status kafka
sudo lxc exec kafka-broker-1 -- /opt/kafka/bin/kafka-broker-api-versions.sh --bootstrap-server localhost:9092
```

### View Logs
```bash
# Zookeeper logs
sudo lxc exec zk-1 -- journalctl -u zookeeper -f

# Kafka logs
sudo lxc exec kafka-broker-1 -- journalctl -u kafka -f
```

### Kafka Operations
```bash
# List topics (from any Kafka container)
sudo lxc exec kafka-broker-1 -- /opt/kafka/bin/kafka-topics.sh --list --bootstrap-server localhost:9092

# Create topic with replication
sudo lxc exec kafka-broker-1 -- /opt/kafka/bin/kafka-topics.sh \
  --create \
  --topic my-topic \
  --bootstrap-server localhost:9092 \
  --partitions 3 \
  --replication-factor 3

# Describe topic
sudo lxc exec kafka-broker-1 -- /opt/kafka/bin/kafka-topics.sh \
  --describe \
  --topic my-topic \
  --bootstrap-server localhost:9092

# Check cluster info
sudo lxc exec kafka-broker-1 -- /opt/kafka/bin/kafka-broker-api-versions.sh \
  --bootstrap-server localhost:9092
```

## Troubleshooting

### Restart Services
```bash
# Restart Zookeeper
sudo lxc exec zk-1 -- systemctl restart zookeeper

# Restart Kafka
sudo lxc exec kafka-broker-1 -- systemctl restart kafka
```

### Check Network Connectivity
```bash
# From Kafka container to Zookeeper
sudo lxc exec kafka-broker-1 -- nc -zv 123.200.0.50 2181
sudo lxc exec kafka-broker-1 -- nc -zv 123.200.0.117 2181
sudo lxc exec kafka-broker-1 -- nc -zv 123.200.0.51 2181

# Between Kafka brokers
sudo lxc exec kafka-broker-1 -- nc -zv 123.200.0.117 9092
sudo lxc exec kafka-broker-1 -- nc -zv 123.200.0.51 9092
```

### Recreate Container
```bash
# Delete container
sudo lxc delete kafka-broker-1 --force

# Relaunch
cd /home/tbsms/orchestrix-deploy/kafka
sudo ./launch.sh broker.conf
```

## Configuration Files Location

### On Remote Servers
- Zookeeper: `/home/tbsms/orchestrix-deploy/zookeeper/`
- Kafka: `/home/tbsms/orchestrix-deploy/kafka/`

### On Local Machine
- Zookeeper configs: `/home/mustafa/telcobright-projects/orchestrix/images/lxc/zookeeper/templates/cluster/`
- Kafka configs: `/home/mustafa/telcobright-projects/orchestrix/images/lxc/kafka/templates/cluster/`

## Important Notes

1. **Firewall**: Ensure ports 2181, 2888, 3888 (Zookeeper) and 9092 (Kafka) are open between servers
2. **Data Persistence**: Data is stored in `/data/zookeeper` and `/data/kafka-logs` within containers
3. **Heap Size**: Zookeeper: 1GB, Kafka: 2GB - adjust based on workload
4. **Replication**: For production topics, use replication factor of 3 for high availability
5. **Monitoring**: Consider adding Prometheus and Grafana for cluster monitoring

## Next Steps

1. Test cluster by creating a replicated topic
2. Set up monitoring and alerting
3. Configure backup strategy for Zookeeper data
4. Tune Kafka broker settings based on workload
5. Set up SSL/TLS if security is required

---

*Deployment completed successfully*
REPORT_EOF

    # Replace timestamp
    sed -i "s/TIMESTAMP/$(date '+%Y-%m-%d %H:%M:%S')/" "$report_file"

    log_success "Report generated: $report_file"
}

# Main execution
main() {
    echo "============================================================"
    echo "  Kafka & Zookeeper Cluster Deployment"
    echo "  3-Node Cluster Across Remote Servers"
    echo "============================================================"
    echo

    # Check prerequisites
    check_prerequisites

    # Clean Docker Kafka from server 1
    log_info "Step 1: Cleaning Docker Kafka from server 1..."
    clean_docker_kafka
    echo

    # Deploy to all servers
    log_info "Step 2: Deploying artifacts to all servers..."
    for i in 1 2 3; do
        deploy_to_server "$i"
        echo
    done

    # Launch Zookeeper ensemble
    log_info "Step 3: Launching Zookeeper ensemble..."
    for i in 1 2 3; do
        launch_zookeeper "$i"
        echo
    done

    # Wait for ensemble to stabilize
    log_info "Waiting for Zookeeper ensemble to stabilize..."
    sleep 10

    # Launch Kafka cluster
    log_info "Step 4: Launching Kafka cluster..."
    for i in 1 2 3; do
        launch_kafka "$i"
        echo
    done

    # Wait for cluster to stabilize
    log_info "Waiting for Kafka cluster to stabilize..."
    sleep 15

    # Verify cluster
    log_info "Step 5: Verifying cluster..."
    verify_cluster
    echo

    # Generate report
    generate_report

    echo
    echo "============================================================"
    echo "  Deployment Completed Successfully!"
    echo "============================================================"
    echo
    echo "Zookeeper Ensemble: 123.200.0.50:2181,123.200.0.117:2181,123.200.0.51:2181/kafka"
    echo "Kafka Bootstrap: 123.200.0.50:9092,123.200.0.117:9092,123.200.0.51:9092"
    echo
    echo "See CLUSTER_DEPLOYMENT_REPORT.md for full details"
    echo
}

# Run main function
main "$@"
