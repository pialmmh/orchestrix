#!/bin/bash
# Configure log rotation and retention for Grafana-Loki container

set -e

# Load parameters from environment or defaults
LOKI_RETENTION_PERIOD="${1:-${LOKI_RETENTION_PERIOD:-168h}}"  # 7 days default
LOKI_RETENTION_DELETE_DELAY="${2:-${LOKI_RETENTION_DELETE_DELAY:-2h}}"
COMPACTOR_WORKING_DIR="${3:-/var/lib/loki/compactor}"
COMPACTOR_RETENTION_ENABLED="${4:-true}"

echo "Configuring Loki with log rotation and retention..."
echo "  Retention Period: $LOKI_RETENTION_PERIOD"
echo "  Delete Delay: $LOKI_RETENTION_DELETE_DELAY"
echo "  Compactor Directory: $COMPACTOR_WORKING_DIR"

# Create enhanced Loki configuration with rotation and retention
cat << EOF > /etc/loki/config.yaml
auth_enabled: false

server:
  http_listen_port: 3100
  grpc_listen_port: 9096
  log_level: info

common:
  path_prefix: /var/lib/loki
  storage:
    filesystem:
      chunks_directory: /var/lib/loki/chunks
      rules_directory: /var/lib/loki/rules
  replication_factor: 1
  ring:
    instance_addr: 127.0.0.1
    kvstore:
      store: inmemory

schema_config:
  configs:
    - from: 2020-10-24
      store: boltdb-shipper
      object_store: filesystem
      schema: v11
      index:
        prefix: index_
        period: 24h

storage_config:
  boltdb_shipper:
    active_index_directory: /var/lib/loki/boltdb-shipper-active
    cache_location: /var/lib/loki/boltdb-shipper-cache
    cache_ttl: 24h
    shared_store: filesystem
  filesystem:
    directory: /var/lib/loki/chunks

compactor:
  working_directory: $COMPACTOR_WORKING_DIR
  shared_store: filesystem
  compaction_interval: 10m
  retention_enabled: $COMPACTOR_RETENTION_ENABLED
  retention_delete_delay: $LOKI_RETENTION_DELETE_DELAY
  retention_delete_worker_count: 150

limits_config:
  enforce_metric_name: false
  reject_old_samples: true
  reject_old_samples_max_age: 168h
  ingestion_rate_mb: 10
  ingestion_burst_size_mb: 20
  retention_period: $LOKI_RETENTION_PERIOD
  max_query_length: 721h
  max_query_parallelism: 32

chunk_store_config:
  max_look_back_period: $LOKI_RETENTION_PERIOD

table_manager:
  retention_deletes_enabled: true
  retention_period: $LOKI_RETENTION_PERIOD

ruler:
  storage:
    type: local
    local:
      directory: /var/lib/loki/rules
  alertmanager_url: http://localhost:9093

analytics:
  reporting_enabled: false
EOF

# Create logrotate configuration for system logs
cat << 'EOF' > /etc/logrotate.d/grafana-loki
# Grafana logs
/var/log/grafana/*.log {
    daily
    rotate 7
    compress
    delaycompress
    missingok
    notifempty
    create 640 grafana grafana
    sharedscripts
    postrotate
        systemctl reload grafana-server > /dev/null 2>&1 || true
    endscript
}

# Loki service logs
/var/log/loki/*.log {
    daily
    rotate 7
    compress
    delaycompress
    missingok
    notifempty
    create 640 root root
    sharedscripts
    postrotate
        systemctl reload loki > /dev/null 2>&1 || true
    endscript
}

# Promtail logs
/var/log/promtail/*.log {
    daily
    rotate 3
    compress
    delaycompress
    missingok
    notifempty
    create 640 root root
}
EOF

# Create directories for log rotation
mkdir -p /var/log/loki /var/log/promtail $COMPACTOR_WORKING_DIR
chmod 755 /var/log/loki /var/log/promtail $COMPACTOR_WORKING_DIR

echo "Log rotation and retention configured successfully"