#!/bin/bash

# Clarity LXD Container Build Script
# Monitoring stack with Prometheus and Grafana for Orchestrix

set -e  # Exit on error

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Configuration from build config
CONTAINER_NAME="clarity"
DEBIAN_VERSION="12"
PROMETHEUS_PORT=9090
GRAFANA_PORT=3300
HOST_CONFIG_BASE="/home/telcobright/app/orchestrix/config"
HOST_DATA_BASE="/home/telcobright/app/orchestrix/data"

echo -e "${GREEN}================================================${NC}"
echo -e "${GREEN}Building Clarity Monitoring Container${NC}"
echo -e "${GREEN}================================================${NC}"

# Function to check if container exists
container_exists() {
    lxc info "$1" &>/dev/null
}

# Function to wait for container network
wait_for_network() {
    local container=$1
    local max_wait=30
    local count=0
    
    echo -e "${YELLOW}Waiting for container network...${NC}"
    while [ $count -lt $max_wait ]; do
        if lxc exec "$container" -- ping -c 1 8.8.8.8 &>/dev/null; then
            echo -e "${GREEN}Network is ready${NC}"
            return 0
        fi
        sleep 1
        count=$((count + 1))
    done
    
    echo -e "${RED}Network timeout${NC}"
    return 1
}

# 1. Clean up existing container if exists
if container_exists "$CONTAINER_NAME"; then
    echo -e "${YELLOW}Removing existing container...${NC}"
    lxc stop "$CONTAINER_NAME" --force 2>/dev/null || true
    lxc delete "$CONTAINER_NAME" --force
fi

# 2. Create container
echo -e "${GREEN}Creating container...${NC}"
lxc launch images:debian/12 "$CONTAINER_NAME"

# 3. Wait for container to be ready
sleep 5
wait_for_network "$CONTAINER_NAME"

# 4. Configure container resources
echo -e "${GREEN}Configuring container resources...${NC}"
lxc config set "$CONTAINER_NAME" limits.cpu 2
lxc config set "$CONTAINER_NAME" limits.memory 2GB
lxc config set "$CONTAINER_NAME" boot.autostart true

# 5. Create host directories for persistent storage
echo -e "${GREEN}Creating persistent storage directories...${NC}"
sudo mkdir -p ${HOST_CONFIG_BASE}/{prometheus,grafana,alertmanager}
sudo mkdir -p ${HOST_DATA_BASE}/{prometheus,grafana}
sudo mkdir -p ${HOST_DATA_BASE}/backup

# Set proper permissions (will be adjusted after installation)
sudo chmod 755 ${HOST_CONFIG_BASE}
sudo chmod 755 ${HOST_DATA_BASE}

# 6. Add storage devices
echo -e "${GREEN}Adding storage devices...${NC}"
lxc config device add "$CONTAINER_NAME" prometheus-data disk \
    source=${HOST_DATA_BASE}/prometheus \
    path=/var/lib/prometheus

lxc config device add "$CONTAINER_NAME" grafana-data disk \
    source=${HOST_DATA_BASE}/grafana \
    path=/var/lib/grafana

lxc config device add "$CONTAINER_NAME" config disk \
    source=${HOST_CONFIG_BASE} \
    path=/etc/orchestrix

lxc config device add "$CONTAINER_NAME" backup disk \
    source=${HOST_DATA_BASE}/backup \
    path=/backup

# 7. Update system
echo -e "${GREEN}Updating system packages...${NC}"
lxc exec "$CONTAINER_NAME" -- apt-get update
lxc exec "$CONTAINER_NAME" -- apt-get upgrade -y
lxc exec "$CONTAINER_NAME" -- apt-get install -y wget curl gnupg software-properties-common apt-transport-https ca-certificates

# 8. Install Prometheus
echo -e "${GREEN}Installing Prometheus...${NC}"
lxc exec "$CONTAINER_NAME" -- bash -c "
    # Create prometheus user
    useradd --no-create-home --shell /bin/false prometheus || true
    
    # Download and install Prometheus
    cd /tmp
    PROM_VERSION=\$(curl -s https://api.github.com/repos/prometheus/prometheus/releases/latest | grep tag_name | cut -d '\"' -f 4 | sed 's/v//')
    wget https://github.com/prometheus/prometheus/releases/download/v\${PROM_VERSION}/prometheus-\${PROM_VERSION}.linux-amd64.tar.gz
    tar xvf prometheus-\${PROM_VERSION}.linux-amd64.tar.gz
    
    # Copy binaries
    cp prometheus-\${PROM_VERSION}.linux-amd64/prometheus /usr/local/bin/
    cp prometheus-\${PROM_VERSION}.linux-amd64/promtool /usr/local/bin/
    
    # Create directories
    mkdir -p /etc/prometheus
    mkdir -p /var/lib/prometheus
    
    # Copy configuration files
    cp -r prometheus-\${PROM_VERSION}.linux-amd64/consoles /etc/prometheus
    cp -r prometheus-\${PROM_VERSION}.linux-amd64/console_libraries /etc/prometheus
    
    # Set permissions
    chown -R prometheus:prometheus /etc/prometheus
    chown -R prometheus:prometheus /var/lib/prometheus
    chown prometheus:prometheus /usr/local/bin/prometheus
    chown prometheus:prometheus /usr/local/bin/promtool
    
    # Cleanup
    rm -rf prometheus-\${PROM_VERSION}.linux-amd64*
"

# 9. Configure Prometheus
echo -e "${GREEN}Configuring Prometheus...${NC}"
cat > ${HOST_CONFIG_BASE}/prometheus/prometheus.yml << 'EOF'
global:
  scrape_interval: 15s
  evaluation_interval: 15s

alerting:
  alertmanagers:
    - static_configs:
        - targets: []

rule_files:
  - "/etc/prometheus/rules/*.yml"

scrape_configs:
  - job_name: 'prometheus'
    static_configs:
      - targets: ['localhost:9090']

  - job_name: 'node'
    static_configs:
      - targets: ['localhost:9100']
  
  - job_name: 'grafana'
    static_configs:
      - targets: ['localhost:3300']

  # Add MySQL exporter if mysql container exists
  - job_name: 'mysql'
    static_configs:
      - targets: ['10.0.1.10:9104']
    honor_labels: true
EOF

# Create systemd service for Prometheus
lxc exec "$CONTAINER_NAME" -- bash -c "
cat > /etc/systemd/system/prometheus.service << 'EOF'
[Unit]
Description=Prometheus
Wants=network-online.target
After=network-online.target

[Service]
User=prometheus
Group=prometheus
Type=simple
ExecStart=/usr/local/bin/prometheus \
    --config.file /etc/orchestrix/prometheus/prometheus.yml \
    --storage.tsdb.path /var/lib/prometheus/ \
    --storage.tsdb.retention.time=30d \
    --storage.tsdb.retention.size=10GB \
    --web.console.templates=/etc/prometheus/consoles \
    --web.console.libraries=/etc/prometheus/console_libraries \
    --web.enable-lifecycle

Restart=always
RestartSec=5

[Install]
WantedBy=multi-user.target
EOF

systemctl daemon-reload
systemctl enable prometheus
systemctl start prometheus
"

# 10. Install Node Exporter
echo -e "${GREEN}Installing Node Exporter...${NC}"
lxc exec "$CONTAINER_NAME" -- bash -c "
    # Create user
    useradd --no-create-home --shell /bin/false node_exporter || true
    
    # Download and install
    cd /tmp
    NODE_VERSION=\$(curl -s https://api.github.com/repos/prometheus/node_exporter/releases/latest | grep tag_name | cut -d '\"' -f 4 | sed 's/v//')
    wget https://github.com/prometheus/node_exporter/releases/download/v\${NODE_VERSION}/node_exporter-\${NODE_VERSION}.linux-amd64.tar.gz
    tar xvf node_exporter-\${NODE_VERSION}.linux-amd64.tar.gz
    
    # Copy binary
    cp node_exporter-\${NODE_VERSION}.linux-amd64/node_exporter /usr/local/bin/
    chown node_exporter:node_exporter /usr/local/bin/node_exporter
    
    # Cleanup
    rm -rf node_exporter-\${NODE_VERSION}.linux-amd64*
    
    # Create systemd service
    cat > /etc/systemd/system/node_exporter.service << 'EOF'
[Unit]
Description=Node Exporter
Wants=network-online.target
After=network-online.target

[Service]
User=node_exporter
Group=node_exporter
Type=simple
ExecStart=/usr/local/bin/node_exporter

Restart=always
RestartSec=5

[Install]
WantedBy=default.target
EOF

    systemctl daemon-reload
    systemctl enable node_exporter
    systemctl start node_exporter
"

# 11. Install Grafana
echo -e "${GREEN}Installing Grafana...${NC}"
lxc exec "$CONTAINER_NAME" -- bash -c "
    # Add Grafana repository
    wget -q -O - https://packages.grafana.com/gpg.key | apt-key add -
    echo 'deb https://packages.grafana.com/oss/deb stable main' > /etc/apt/sources.list.d/grafana.list
    
    # Install Grafana
    apt-get update
    apt-get install -y grafana
    
    # Configure Grafana to use custom port
    sed -i 's/;http_port = 3000/http_port = ${GRAFANA_PORT}/' /etc/grafana/grafana.ini
    sed -i 's/;http_addr =/http_addr = 0.0.0.0/' /etc/grafana/grafana.ini
    
    # Set admin credentials
    sed -i 's/;admin_user = admin/admin_user = admin/' /etc/grafana/grafana.ini
    sed -i 's/;admin_password = admin/admin_password = admin/' /etc/grafana/grafana.ini
    
    # Disable user signup
    sed -i 's/;allow_sign_up = true/allow_sign_up = false/' /etc/grafana/grafana.ini
    
    # Set data path
    sed -i 's|;data = /var/lib/grafana|data = /var/lib/grafana|' /etc/grafana/grafana.ini
    
    # Enable and start Grafana
    systemctl daemon-reload
    systemctl enable grafana-server
    systemctl start grafana-server
"

# 12. Configure Grafana datasource
echo -e "${GREEN}Configuring Grafana datasource...${NC}"
sleep 10  # Wait for Grafana to start

lxc exec "$CONTAINER_NAME" -- bash -c "
    # Create datasource provisioning
    mkdir -p /etc/grafana/provisioning/datasources
    cat > /etc/grafana/provisioning/datasources/prometheus.yml << 'EOF'
apiVersion: 1

datasources:
  - name: Prometheus
    type: prometheus
    access: proxy
    url: http://localhost:${PROMETHEUS_PORT}
    isDefault: true
    editable: true
EOF
    
    # Create dashboard provisioning directory
    mkdir -p /etc/grafana/provisioning/dashboards
    cat > /etc/grafana/provisioning/dashboards/dashboard.yml << 'EOF'
apiVersion: 1

providers:
  - name: 'default'
    orgId: 1
    folder: ''
    type: file
    disableDeletion: false
    updateIntervalSeconds: 10
    options:
      path: /var/lib/grafana/dashboards
EOF
    
    # Create dashboards directory
    mkdir -p /var/lib/grafana/dashboards
    
    # Restart Grafana to apply provisioning
    systemctl restart grafana-server
"

# 13. Fix permissions for mounted volumes
echo -e "${GREEN}Fixing permissions...${NC}"
lxc exec "$CONTAINER_NAME" -- bash -c "
    # Get Prometheus UID/GID
    PROM_UID=\$(id -u prometheus)
    PROM_GID=\$(id -g prometheus)
    
    # Get Grafana UID/GID
    GRAF_UID=\$(id -u grafana)
    GRAF_GID=\$(id -g grafana)
    
    # Set ownership
    chown -R \${PROM_UID}:\${PROM_GID} /var/lib/prometheus || true
    chown -R \${GRAF_UID}:\${GRAF_GID} /var/lib/grafana || true
"

# Also fix on host
sudo chown -R 65534:65534 ${HOST_DATA_BASE}/prometheus || true
sudo chown -R 472:472 ${HOST_DATA_BASE}/grafana || true

# 14. Create backup script
echo -e "${GREEN}Creating backup script...${NC}"
lxc exec "$CONTAINER_NAME" -- bash -c "
    cat > /usr/local/bin/clarity-backup.sh << 'BACKUP'
#!/bin/bash
BACKUP_DIR='/backup'
DATE=\$(date +%Y%m%d_%H%M%S)
RETENTION_DAYS=7

# Create backup directory
mkdir -p \${BACKUP_DIR}/clarity_\${DATE}

# Backup Prometheus data
tar czf \${BACKUP_DIR}/clarity_\${DATE}/prometheus_data.tar.gz -C /var/lib prometheus/

# Backup Grafana data
tar czf \${BACKUP_DIR}/clarity_\${DATE}/grafana_data.tar.gz -C /var/lib grafana/

# Backup configurations
tar czf \${BACKUP_DIR}/clarity_\${DATE}/configs.tar.gz -C /etc orchestrix/

# Create single archive
cd \${BACKUP_DIR}
tar czf clarity_backup_\${DATE}.tar.gz clarity_\${DATE}/
rm -rf clarity_\${DATE}/

# Remove old backups
find \${BACKUP_DIR} -name 'clarity_backup_*.tar.gz' -mtime +\${RETENTION_DAYS} -delete

echo \"Backup completed: clarity_backup_\${DATE}.tar.gz\"
BACKUP
    
    chmod +x /usr/local/bin/clarity-backup.sh
    
    # Add to crontab
    (crontab -l 2>/dev/null; echo '0 3 * * * /usr/local/bin/clarity-backup.sh') | crontab -
"

# 15. Configure firewall (optional)
echo -e "${GREEN}Configuring firewall rules...${NC}"
lxc exec "$CONTAINER_NAME" -- bash -c "
    # Install ufw if needed
    apt-get install -y ufw || true
    
    # Configure firewall
    ufw default deny incoming
    ufw default allow outgoing
    ufw allow ${PROMETHEUS_PORT}/tcp comment 'Prometheus'
    ufw allow ${GRAFANA_PORT}/tcp comment 'Grafana'
    ufw allow 9100/tcp comment 'Node Exporter'
    ufw allow 22/tcp comment 'SSH'
    
    # Enable firewall (non-interactive)
    echo 'y' | ufw enable || true
"

# 16. Show container information
echo -e "${GREEN}================================================${NC}"
echo -e "${GREEN}Clarity Container Setup Complete!${NC}"
echo -e "${GREEN}================================================${NC}"

# Get container IP
CONTAINER_IP=$(lxc list "$CONTAINER_NAME" -c 4 --format csv | cut -d' ' -f1)

echo -e "${YELLOW}Container Information:${NC}"
echo "  Name: $CONTAINER_NAME"
echo "  IP Address: $CONTAINER_IP"
echo "  OS: Debian $DEBIAN_VERSION"
echo ""
echo -e "${YELLOW}Service URLs:${NC}"
echo "  Prometheus: http://$CONTAINER_IP:${PROMETHEUS_PORT}"
echo "  Grafana: http://$CONTAINER_IP:${GRAFANA_PORT}"
echo "  Node Exporter: http://$CONTAINER_IP:9100/metrics"
echo ""
echo -e "${YELLOW}Credentials:${NC}"
echo "  Grafana Login: admin / admin"
echo "  (Change password on first login)"
echo ""
echo -e "${YELLOW}Data Persistence:${NC}"
echo "  Prometheus Data: ${HOST_DATA_BASE}/prometheus"
echo "  Grafana Data: ${HOST_DATA_BASE}/grafana"
echo "  Configurations: ${HOST_CONFIG_BASE}"
echo "  Backups: ${HOST_DATA_BASE}/backup"
echo ""
echo -e "${YELLOW}Access Container:${NC}"
echo "  Shell: lxc exec $CONTAINER_NAME bash"
echo ""
echo -e "${YELLOW}Service Management:${NC}"
echo "  Prometheus: systemctl [status|restart] prometheus"
echo "  Grafana: systemctl [status|restart] grafana-server"
echo "  Node Exporter: systemctl [status|restart] node_exporter"
echo ""
echo -e "${YELLOW}Next Steps:${NC}"
echo "  1. Access Grafana at http://$CONTAINER_IP:${GRAFANA_PORT}"
echo "  2. Change admin password"
echo "  3. Import dashboards from Grafana.com"
echo "  4. Add more Prometheus targets in ${HOST_CONFIG_BASE}/prometheus/prometheus.yml"
echo ""
echo -e "${GREEN}================================================${NC}"