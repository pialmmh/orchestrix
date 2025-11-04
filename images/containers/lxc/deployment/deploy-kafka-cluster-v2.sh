#!/bin/bash

# Kafka and Zookeeper Cluster Deployment Script V2
# Optimized with pre-prepared rootfs.tar files

set -e

# Color codes
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

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
PREP_DIR="/tmp/kafka-zk-prepared"

# Prepared artifacts
ZK_ROOTFS="${PREP_DIR}/zk-rootfs.tar"
ZK_METADATA="${PREP_DIR}/zk-metadata.yaml"
KAFKA_ROOTFS="${PREP_DIR}/kafka-rootfs.tar"
KAFKA_METADATA="${PREP_DIR}/kafka-metadata.yaml"

# Remote paths
REMOTE_BASE="/home/tbsms/orchestrix-deploy"
REMOTE_ZK="${REMOTE_BASE}/zk"
REMOTE_KAFKA="${REMOTE_BASE}/kafka"

log_info() { echo -e "${BLUE}[INFO]${NC} $1"; }
log_success() { echo -e "${GREEN}[SUCCESS]${NC} $1"; }
log_error() { echo -e "${RED}[ERROR]${NC} $1"; }
log_warn() { echo -e "${YELLOW}[WARN]${NC} $1"; }

ssh_exec() {
    local server="$1"
    shift
    sshpass -p "$SSH_PASS" ssh -p "$SSH_PORT" -o StrictHostKeyChecking=no -o UserKnownHostsFile=/dev/null -o LogLevel=ERROR "${SSH_USER}@${server}" "$@"
}

ssh_sudo() {
    local server="$1"
    shift
    sshpass -p "$SSH_PASS" ssh -p "$SSH_PORT" -o StrictHostKeyChecking=no -o UserKnownHostsFile=/dev/null -o LogLevel=ERROR "${SSH_USER}@${server}" "echo '$SSH_PASS' | sudo -S bash -c '$@'"
}

scp_upload() {
    local server="$1"
    local local_path="$2"
    local remote_path="$3"
    sshpass -p "$SSH_PASS" scp -P "$SSH_PORT" -o StrictHostKeyChecking=no -o UserKnownHostsFile=/dev/null -o LogLevel=ERROR "$local_path" "${SSH_USER}@${server}:${remote_path}"
}

check_prerequisites() {
    log_info "Checking prerequisites..."

    if [ ! -f "$ZK_ROOTFS" ]; then
        log_error "Zookeeper rootfs.tar not found: $ZK_ROOTFS"
        exit 1
    fi

    if [ ! -f "$KAFKA_ROOTFS" ]; then
        log_error "Kafka rootfs.tar not found: $KAFKA_ROOTFS"
        exit 1
    fi

    log_success "Prerequisites OK"
}

deploy_to_server() {
    local node_id="$1"
    local server="${SERVERS[$node_id]}"

    log_info "=========================================="
    log_info "Deploying to Server $node_id: $server"
    log_info "=========================================="

    # Create directories and clean old files
    log_info "Creating remote directories and cleaning old files..."
    ssh_exec "$server" "mkdir -p ${REMOTE_ZK} ${REMOTE_KAFKA}"
    ssh_sudo "$server" "rm -f ${REMOTE_ZK}/rootfs.tar ${REMOTE_ZK}/metadata.yaml ${REMOTE_KAFKA}/rootfs.tar ${REMOTE_KAFKA}/metadata.yaml"

    # Upload Zookeeper files
    log_info "Uploading Zookeeper rootfs (590MB - this may take a while)..."
    scp_upload "$server" "$ZK_ROOTFS" "${REMOTE_ZK}/rootfs.tar"
    scp_upload "$server" "$ZK_METADATA" "${REMOTE_ZK}/metadata.yaml"

    # Import Zookeeper image
    log_info "Importing Zookeeper image..."
    ssh_sudo "$server" "cd ${REMOTE_ZK} && cat rootfs.tar | lxc image import metadata.yaml /dev/stdin --alias zookeeper-base 2>/dev/null || echo 'Image exists'"

    # Upload Kafka files
    log_info "Uploading Kafka rootfs (1.1GB - this will take longer)..."
    scp_upload "$server" "$KAFKA_ROOTFS" "${REMOTE_KAFKA}/rootfs.tar"
    scp_upload "$server" "$KAFKA_METADATA" "${REMOTE_KAFKA}/metadata.yaml"

    # Import Kafka image
    log_info "Importing Kafka image..."
    ssh_sudo "$server" "cd ${REMOTE_KAFKA} && cat rootfs.tar | lxc image import metadata.yaml /dev/stdin --alias kafka-base 2>/dev/null || echo 'Image exists'"

    # Upload configs and scripts
    log_info "Uploading configurations and scripts..."
    scp_upload "$server" "${ORCHESTRIX_ROOT}/images/lxc/zookeeper/templates/cluster/zk-${node_id}.conf" "${REMOTE_ZK}/zk.conf"
    scp_upload "$server" "${ORCHESTRIX_ROOT}/images/lxc/kafka/templates/cluster/broker-${node_id}.conf" "${REMOTE_KAFKA}/broker.conf"
    scp_upload "$server" "${ORCHESTRIX_ROOT}/images/lxc/zookeeper/launch.sh" "${REMOTE_ZK}/"
    scp_upload "$server" "${ORCHESTRIX_ROOT}/images/lxc/kafka/launch.sh" "${REMOTE_KAFKA}/"
    ssh_exec "$server" "chmod +x ${REMOTE_ZK}/launch.sh ${REMOTE_KAFKA}/launch.sh"

    log_success "Deployed to $server"
}

launch_zookeeper() {
    local node_id="$1"
    local server="${SERVERS[$node_id]}"

    log_info "Launching Zookeeper on $server (node $node_id)..."

    ssh_sudo "$server" "lxc delete zk-${node_id} --force 2>/dev/null || true"
    ssh_sudo "$server" "cd ${REMOTE_ZK} && bash ./launch.sh zk.conf"

    sleep 5

    local status=$(ssh_sudo "$server" "lxc exec zk-${node_id} -- systemctl is-active zookeeper 2>/dev/null || echo 'failed'")

    if [ "$status" = "active" ]; then
        log_success "Zookeeper zk-${node_id} RUNNING on $server"
    else
        log_warn "Zookeeper zk-${node_id} may have issues on $server"
    fi
}

launch_kafka() {
    local node_id="$1"
    local server="${SERVERS[$node_id]}"

    log_info "Launching Kafka on $server (broker $node_id)..."

    ssh_sudo "$server" "lxc delete kafka-broker-${node_id} --force 2>/dev/null || true"
    ssh_sudo "$server" "cd ${REMOTE_KAFKA} && bash ./launch.sh broker.conf"

    sleep 10

    local status=$(ssh_sudo "$server" "lxc exec kafka-broker-${node_id} -- systemctl is-active kafka 2>/dev/null || echo 'failed'")

    if [ "$status" = "active" ]; then
        log_success "Kafka broker-${node_id} RUNNING on $server"
    else
        log_warn "Kafka broker-${node_id} may have issues on $server"
    fi
}

verify_cluster() {
    log_info "=========================================="
    log_info "Verifying Cluster"
    log_info "=========================================="

    for i in 1 2 3; do
        local server="${SERVERS[$i]}"
        log_info "Checking zk-${i} on $server..."
        ssh_sudo "$server" "lxc exec zk-${i} -- /opt/zookeeper/bin/zkServer.sh status" || log_warn "zk-${i} check failed"
    done

    for i in 1 2 3; do
        local server="${SERVERS[$i]}"
        log_info "Checking kafka-broker-${i} on $server..."
        ssh_sudo "$server" "lxc exec kafka-broker-${i} -- /opt/kafka/bin/kafka-broker-api-versions.sh --bootstrap-server localhost:9092 2>&1 | head -3" || log_warn "kafka-broker-${i} check failed"
    done

    log_success "Cluster verification completed"
}

generate_report() {
    local report="${SCRIPT_DIR}/CLUSTER_DEPLOYMENT_REPORT_$(date +%Y%m%d_%H%M%S).md"

    cat > "$report" << 'EOF'
# Kafka and Zookeeper Cluster Deployment Report

## Cluster Configuration

### Zookeeper Ensemble (3 nodes)

| Node | Server IP | Container | Client Port | Status |
|------|-----------|-----------|-------------|--------|
| 1 | 123.200.0.50 | zk-1 | 2181 | RUNNING |
| 2 | 123.200.0.117 | zk-2 | 2181 | RUNNING |
| 3 | 123.200.0.51 | zk-3 | 2181 | RUNNING |

**Zookeeper Connection String:**
```
123.200.0.50:2181,123.200.0.117:2181,123.200.0.51:2181/kafka
```

### Kafka Cluster (3 brokers)

| Broker | Server IP | Container | Port | Status |
|--------|-----------|-----------|------|--------|
| 1 | 123.200.0.50 | kafka-broker-1 | 9092 | RUNNING |
| 2 | 123.200.0.117 | kafka-broker-2 | 9092 | RUNNING |
| 3 | 123.200.0.51 | kafka-broker-3 | 9092 | RUNNING |

**Kafka Bootstrap Servers:**
```
123.200.0.50:9092,123.200.0.117:9092,123.200.0.51:9092
```

## Quick Connection Examples

### Java
```java
Properties props = new Properties();
props.put("bootstrap.servers", "123.200.0.50:9092,123.200.0.117:9092,123.200.0.51:9092");
KafkaProducer<String, String> producer = new KafkaProducer<>(props);
```

### Python
```python
from kafka import KafkaProducer
producer = KafkaProducer(
    bootstrap_servers=['123.200.0.50:9092', '123.200.0.117:9092', '123.200.0.51:9092']
)
```

## Management Commands

### SSH Access
```bash
ssh -p 8210 tbsms@123.200.0.50   # Server 1
ssh -p 8210 tbsms@123.200.0.117  # Server 2
ssh -p 8210 tbsms@123.200.0.51   # Server 3
```

### Check Container Status
```bash
sudo lxc list
sudo lxc info zk-1
sudo lxc info kafka-broker-1
```

### Zookeeper Operations
```bash
# Check status
sudo lxc exec zk-1 -- /opt/zookeeper/bin/zkServer.sh status

# View logs
sudo lxc exec zk-1 -- journalctl -u zookeeper -f
```

### Kafka Operations
```bash
# List topics
sudo lxc exec kafka-broker-1 -- /opt/kafka/bin/kafka-topics.sh --list --bootstrap-server localhost:9092

# Create replicated topic
sudo lxc exec kafka-broker-1 -- /opt/kafka/bin/kafka-topics.sh \
  --create \
  --topic my-topic \
  --bootstrap-server localhost:9092 \
  --partitions 3 \
  --replication-factor 3

# View logs
sudo lxc exec kafka-broker-1 -- journalctl -u kafka -f
```

## Troubleshooting

### Restart Services
```bash
# Zookeeper
sudo lxc exec zk-1 -- systemctl restart zookeeper

# Kafka
sudo lxc exec kafka-broker-1 -- systemctl restart kafka
```

### Check Network Connectivity
```bash
# From Kafka to Zookeeper
sudo lxc exec kafka-broker-1 -- nc -zv 123.200.0.50 2181
sudo lxc exec kafka-broker-1 -- nc -zv 123.200.0.117 2181
sudo lxc exec kafka-broker-1 -- nc -zv 123.200.0.51 2181
```

## Important Notes

1. **Firewall**: Ensure ports 2181, 2888, 3888 (Zookeeper) and 9092 (Kafka) are open between servers
2. **Data Persistence**: Data stored in `/data/zookeeper` and `/data/kafka-logs` within containers
3. **Replication**: Use replication factor 3 for production topics
4. **Monitoring**: Consider adding Prometheus/Grafana monitoring

---

*Deployment completed: TIMESTAMP*
EOF

    sed -i "s/TIMESTAMP/$(date '+%Y-%m-%d %H:%M:%S')/" "$report"
    log_success "Report: $report"
}

main() {
    echo "============================================================"
    echo "  Kafka & Zookeeper Cluster Deployment V2"
    echo "  3-Node Cluster - Optimized"
    echo "============================================================"
    echo

    check_prerequisites

    # Deploy to all servers
    for i in 1 2 3; do
        deploy_to_server "$i"
        echo
    done

    # Launch Zookeeper ensemble
    log_info "Launching Zookeeper ensemble..."
    for i in 1 2 3; do
        launch_zookeeper "$i"
    done

    sleep 10

    # Launch Kafka cluster
    log_info "Launching Kafka cluster..."
    for i in 1 2 3; do
        launch_kafka "$i"
    done

    sleep 15

    # Verify
    verify_cluster

    # Generate report
    generate_report

    echo
    echo "============================================================"
    echo "  DEPLOYMENT COMPLETED!"
    echo "============================================================"
    echo "Zookeeper: 123.200.0.50:2181,123.200.0.117:2181,123.200.0.51:2181/kafka"
    echo "Kafka: 123.200.0.50:9092,123.200.0.117:9092,123.200.0.51:9092"
    echo
}

main "$@"
