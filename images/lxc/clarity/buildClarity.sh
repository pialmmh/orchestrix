#!/bin/bash

# buildClarity.sh - Build LXC container with Prometheus and Grafana
# This script creates a Debian 12 based LXC container with monitoring stack

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Load configuration from .cnf file if exists
CONFIG_FILE="$(dirname "$0")/buildClarityConfig.cnf"
if [ -f "$CONFIG_FILE" ]; then
    echo -e "${GREEN}Loading configuration from ${CONFIG_FILE}${NC}"
    source "$CONFIG_FILE"
else
    echo -e "${YELLOW}Config file not found. Using default values.${NC}"
    # Default Configuration (fallback if no config file)
    CONTAINER_NAME="clarity"
    DEBIAN_VERSION="12"
    ARCH="amd64"
    BACKUP_DIR="$(pwd)"
    HOST_CONFIG_BASE="/opt/orchestrix/deployments"
    HOST_DATA_BASE="/data/orchestrix"
    PROMETHEUS_PORT=9090
    GRAFANA_PORT=3300
    RCLONE_UPLOAD_ENABLED="false"
fi

# Build the backup filename with timestamp
BUILD_TIMESTAMP=$(date +%Y%m%d-%H%M%S)
BACKUP_FILE="${BACKUP_DIR}/${CONTAINER_NAME}-${BUILD_TIMESTAMP}.tar.gz"

# Function to print colored messages
print_message() {
    local color=$1
    local message=$2
    echo -e "${color}${message}${NC}"
}

# Function to check if running as root
check_root() {
    if [[ $EUID -ne 0 ]]; then
        print_message "$RED" "This script must be run as root (use sudo)"
        exit 1
    fi
}

# Function to check and install LXD
check_install_lxd() {
    print_message "$YELLOW" "Checking LXD installation..."
    
    if ! command -v lxc &> /dev/null; then
        print_message "$YELLOW" "LXD not found. Installing LXD..."
        
        # Detect OS and install accordingly
        if [[ -f /etc/debian_version ]]; then
            # Debian/Ubuntu
            apt-get update
            apt-get install -y snapd
            snap install lxd
            
            # Initialize LXD with default settings
            print_message "$YELLOW" "Initializing LXD with default settings..."
            lxd init --auto
            
            # Add current user to lxd group
            usermod -aG lxd $SUDO_USER 2>/dev/null || true
            
        elif [[ -f /etc/redhat-release ]]; then
            # RHEL/CentOS/Fedora
            dnf install -y snapd
            snap install lxd
            lxd init --auto
        else
            print_message "$RED" "Unsupported OS. Please install LXD manually."
            exit 1
        fi
        
        print_message "$GREEN" "LXD installed successfully!"
    else
        print_message "$GREEN" "LXD is already installed."
    fi
}

# Function to check if container exists
check_container_exists() {
    if lxc list --format=json | jq -r '.[].name' | grep -q "^${CONTAINER_NAME}$"; then
        return 0
    else
        return 1
    fi
}

# Function to create container
create_container() {
    print_message "$YELLOW" "Creating container '${CONTAINER_NAME}' with Debian ${DEBIAN_VERSION}..."
    
    # Create container
    lxc launch images:debian/${DEBIAN_VERSION}/${ARCH} ${CONTAINER_NAME}
    
    # Wait for container to be ready
    print_message "$YELLOW" "Waiting for container to be ready..."
    sleep 10
    
    # Wait for network to be up
    lxc exec ${CONTAINER_NAME} -- bash -c "until ping -c1 google.com &>/dev/null; do sleep 1; done"
    
    print_message "$GREEN" "Container created successfully!"
}

# Function to install Prometheus and Grafana
install_monitoring_stack() {
    print_message "$YELLOW" "Installing Prometheus and Grafana..."
    
    # Update package lists
    lxc exec ${CONTAINER_NAME} -- apt-get update
    
    # Install dependencies
    lxc exec ${CONTAINER_NAME} -- apt-get install -y \
        wget \
        curl \
        gnupg \
        software-properties-common \
        apt-transport-https \
        ca-certificates \
        lsb-release
    
    # Install Prometheus
    print_message "$YELLOW" "Installing Prometheus..."
    lxc exec ${CONTAINER_NAME} -- bash -c "
        # Create prometheus user
        useradd --no-create-home --shell /bin/false prometheus || true
        
        # Download and install Prometheus
        cd /tmp
        PROM_VERSION=\$(curl -s https://api.github.com/repos/prometheus/prometheus/releases/latest | grep tag_name | cut -d '\"' -f 4 | sed 's/v//')
        wget https://github.com/prometheus/prometheus/releases/download/v\${PROM_VERSION}/prometheus-\${PROM_VERSION}.linux-amd64.tar.gz
        tar xvf prometheus-\${PROM_VERSION}.linux-amd64.tar.gz
        
        # Move binaries
        cp prometheus-\${PROM_VERSION}.linux-amd64/prometheus /usr/local/bin/
        cp prometheus-\${PROM_VERSION}.linux-amd64/promtool /usr/local/bin/
        
        # Set permissions
        chown prometheus:prometheus /usr/local/bin/prometheus
        chown prometheus:prometheus /usr/local/bin/promtool
        
        # Create directories
        mkdir -p /etc/prometheus
        mkdir -p /var/lib/prometheus
        
        # Copy console files
        cp -r prometheus-\${PROM_VERSION}.linux-amd64/consoles /etc/prometheus
        cp -r prometheus-\${PROM_VERSION}.linux-amd64/console_libraries /etc/prometheus
        
        # Set ownership
        chown -R prometheus:prometheus /etc/prometheus
        chown -R prometheus:prometheus /var/lib/prometheus
        
        # Cleanup
        rm -rf prometheus-\${PROM_VERSION}.linux-amd64*
    "
    
    # Install Grafana
    print_message "$YELLOW" "Installing Grafana..."
    lxc exec ${CONTAINER_NAME} -- bash -c "
        # Add Grafana GPG key
        wget -q -O - https://packages.grafana.com/gpg.key | apt-key add -
        
        # Add Grafana repository
        echo 'deb https://packages.grafana.com/oss/deb stable main' > /etc/apt/sources.list.d/grafana.list
        
        # Update and install Grafana
        apt-get update
        apt-get install -y grafana
        
        # Stop Grafana immediately after install (we'll configure it first)
        systemctl stop grafana-server || true
        systemctl disable grafana-server || true
        
        # Configure Grafana to use configured port
        sed -i "s/;http_port = 3000/http_port = ${GRAFANA_PORT}/" /etc/grafana/grafana.ini
        # Also ensure it's set in [server] section
        if ! grep -q "^http_port = ${GRAFANA_PORT}" /etc/grafana/grafana.ini; then
            echo "http_port = ${GRAFANA_PORT}" >> /etc/grafana/grafana.ini
        fi
    "
    
    print_message "$GREEN" "Prometheus and Grafana installed successfully!"
}

# Function to configure bind mounts for config files
configure_bind_mounts() {
    print_message "$YELLOW" "Configuring bind mounts for configuration files..."
    
    # Stop container to add devices
    lxc stop ${CONTAINER_NAME}
    
    # Create host directories if they don't exist
    mkdir -p "${HOST_CONFIG_BASE}/${CONTAINER_NAME}/prometheus"
    mkdir -p "${HOST_CONFIG_BASE}/${CONTAINER_NAME}/grafana"
    mkdir -p "${HOST_DATA_BASE}/${CONTAINER_NAME}/prometheus"
    mkdir -p "${HOST_DATA_BASE}/${CONTAINER_NAME}/grafana"
    
    # Add devices for config directories (read-only)
    lxc config device add ${CONTAINER_NAME} prometheus-config disk \
        source="${HOST_CONFIG_BASE}/${CONTAINER_NAME}/prometheus" \
        path=/etc/prometheus/config
    
    lxc config device add ${CONTAINER_NAME} grafana-config disk \
        source="${HOST_CONFIG_BASE}/${CONTAINER_NAME}/grafana" \
        path=/etc/grafana/config
    
    # Add devices for data directories (read-write)
    lxc config device add ${CONTAINER_NAME} prometheus-data disk \
        source="${HOST_DATA_BASE}/${CONTAINER_NAME}/prometheus" \
        path=/var/lib/prometheus
    
    lxc config device add ${CONTAINER_NAME} grafana-data disk \
        source="${HOST_DATA_BASE}/${CONTAINER_NAME}/grafana" \
        path=/var/lib/grafana
    
    # Start container
    lxc start ${CONTAINER_NAME}
    sleep 5
    
    # Set proper permissions on host directories for Grafana
    # Grafana user in container typically has UID 472
    GRAFANA_UID=472
    GRAFANA_GID=472
    chown -R ${GRAFANA_UID}:${GRAFANA_GID} "${HOST_DATA_BASE}/${CONTAINER_NAME}/grafana"
    chmod 755 "${HOST_DATA_BASE}/${CONTAINER_NAME}/grafana"
    
    # Create default Prometheus config if not exists
    if [ ! -f "${HOST_CONFIG_BASE}/${CONTAINER_NAME}/prometheus/prometheus.yml" ]; then
        cat > "${HOST_CONFIG_BASE}/${CONTAINER_NAME}/prometheus/prometheus.yml" << 'EOF'
global:
  scrape_interval: 15s
  evaluation_interval: 15s

scrape_configs:
  - job_name: 'prometheus'
    static_configs:
      - targets: ['localhost:9090']
EOF
    fi
    
    # Create systemd services
    print_message "$YELLOW" "Creating systemd services..."
    
    # Prometheus service
    lxc exec ${CONTAINER_NAME} -- bash -c "cat > /etc/systemd/system/prometheus.service << 'EOF'
[Unit]
Description=Prometheus Server
After=network.target

[Service]
User=prometheus
Group=prometheus
Type=simple
ExecStart=/usr/local/bin/prometheus \\
    --config.file /etc/prometheus/config/prometheus.yml \\
    --storage.tsdb.path /var/lib/prometheus/ \\
    --web.console.templates=/etc/prometheus/consoles \\
    --web.console.libraries=/etc/prometheus/console_libraries

[Install]
WantedBy=multi-user.target
EOF"
    
    # Create Grafana provisioning directories and files
    print_message "$YELLOW" "Configuring Grafana to use Prometheus as datasource..."
    
    # First create directories on the host for the bind mount
    mkdir -p "${HOST_DATA_BASE}/${CONTAINER_NAME}/grafana/dashboards"
    
    # Get Grafana user ID from container (usually 472)
    GRAFANA_UID=$(lxc exec ${CONTAINER_NAME} -- id -u grafana 2>/dev/null || echo "472")
    GRAFANA_GID=$(lxc exec ${CONTAINER_NAME} -- id -g grafana 2>/dev/null || echo "472")
    
    # Set ownership on host directories to match container's grafana user
    chown -R ${GRAFANA_UID}:${GRAFANA_GID} "${HOST_DATA_BASE}/${CONTAINER_NAME}/grafana"
    
    # Create provisioning directories in container
    lxc exec ${CONTAINER_NAME} -- bash -c "
        mkdir -p /etc/grafana/provisioning/datasources
        mkdir -p /etc/grafana/provisioning/dashboards
        chown -R grafana:grafana /etc/grafana/provisioning
    "
    
    # Create Prometheus datasource configuration
    lxc exec ${CONTAINER_NAME} -- bash -c "cat > /etc/grafana/provisioning/datasources/prometheus.yml << 'EOF'
apiVersion: 1

datasources:
  - name: Prometheus
    type: prometheus
    access: proxy
    url: http://localhost:9090
    isDefault: true
    editable: true
    jsonData:
      timeInterval: '15s'
      queryTimeout: '60s'
      httpMethod: POST
EOF"
    
    # Create dashboard provisioning configuration
    lxc exec ${CONTAINER_NAME} -- bash -c "cat > /etc/grafana/provisioning/dashboards/default.yml << 'EOF'
apiVersion: 1

providers:
  - name: 'default'
    orgId: 1
    folder: ''
    type: file
    disableDeletion: false
    updateIntervalSeconds: 10
    allowUiUpdates: true
    options:
      path: /var/lib/grafana/dashboards
EOF"
    
    # Create a basic system monitoring dashboard on the host (since it's bind mounted)
    cat > "${HOST_DATA_BASE}/${CONTAINER_NAME}/grafana/dashboards/system-monitoring.json" << 'EOF'
{
  \"dashboard\": {
    \"id\": null,
    \"title\": \"System Monitoring\",
    \"tags\": [\"prometheus\", \"system\"],
    \"timezone\": \"browser\",
    \"panels\": [
      {
        \"id\": 1,
        \"gridPos\": {\"h\": 8, \"w\": 12, \"x\": 0, \"y\": 0},
        \"type\": \"graph\",
        \"title\": \"Prometheus Scrape Duration\",
        \"datasource\": \"Prometheus\",
        \"targets\": [
          {
            \"expr\": \"prometheus_target_interval_length_seconds\",
            \"refId\": \"A\"
          }
        ]
      },
      {
        \"id\": 2,
        \"gridPos\": {\"h\": 8, \"w\": 12, \"x\": 12, \"y\": 0},
        \"type\": \"graph\",
        \"title\": \"Up Targets\",
        \"datasource\": \"Prometheus\",
        \"targets\": [
          {
            \"expr\": \"up\",
            \"refId\": \"A\"
          }
        ]
      }
    ],
    \"schemaVersion\": 16,
    \"version\": 0
  },
  \"overwrite\": true
}
EOF
    
    # Set proper permissions on host files
    chown ${GRAFANA_UID}:${GRAFANA_GID} "${HOST_DATA_BASE}/${CONTAINER_NAME}/grafana/dashboards/system-monitoring.json"
    
    # Set proper permissions in container for provisioning files
    lxc exec ${CONTAINER_NAME} -- chown -R grafana:grafana /etc/grafana/provisioning
    
    # Also create host-side Grafana configuration for persistence
    if [ ! -f "${HOST_CONFIG_BASE}/${CONTAINER_NAME}/grafana/datasources.yml" ]; then
        mkdir -p "${HOST_CONFIG_BASE}/${CONTAINER_NAME}/grafana"
        cat > "${HOST_CONFIG_BASE}/${CONTAINER_NAME}/grafana/datasources.yml" << 'EOF'
# Grafana datasource configuration
# This file is automatically loaded by Grafana
apiVersion: 1

datasources:
  - name: Prometheus
    type: prometheus
    access: proxy
    url: http://localhost:9090
    isDefault: true
    editable: true
    jsonData:
      timeInterval: '15s'
      queryTimeout: '60s'
      httpMethod: POST
EOF
    fi
    
    # Enable and start services
    lxc exec ${CONTAINER_NAME} -- systemctl daemon-reload
    lxc exec ${CONTAINER_NAME} -- systemctl enable prometheus
    lxc exec ${CONTAINER_NAME} -- systemctl start prometheus
    lxc exec ${CONTAINER_NAME} -- systemctl enable grafana-server
    lxc exec ${CONTAINER_NAME} -- systemctl restart grafana-server  # Restart to load provisioning
    
    # Wait for services to be ready
    print_message "$YELLOW" "Waiting for services to start..."
    sleep 5
    
    # Check if Grafana is running with timeout
    print_message "$YELLOW" "Checking Grafana status..."
    WAIT_COUNT=0
    MAX_WAIT=30  # Maximum 30 seconds
    
    while [ $WAIT_COUNT -lt $MAX_WAIT ]; do
        if lxc exec ${CONTAINER_NAME} -- systemctl is-active grafana-server >/dev/null 2>&1; then
            print_message "$GREEN" "Grafana is running!"
            break
        fi
        sleep 1
        WAIT_COUNT=$((WAIT_COUNT + 1))
    done
    
    if [ $WAIT_COUNT -eq $MAX_WAIT ]; then
        print_message "$YELLOW" "Grafana took longer than expected to start. Checking status..."
        lxc exec ${CONTAINER_NAME} -- systemctl status grafana-server --no-pager || true
    fi
    
    print_message "$GREEN" "Bind mounts and Grafana-Prometheus integration configured successfully!"
    print_message "$YELLOW" "Config directories:"
    print_message "$YELLOW" "  Prometheus config: ${HOST_CONFIG_BASE}/${CONTAINER_NAME}/prometheus"
    print_message "$YELLOW" "  Grafana config: ${HOST_CONFIG_BASE}/${CONTAINER_NAME}/grafana"
    print_message "$YELLOW" "Data directories:"
    print_message "$YELLOW" "  Prometheus data: ${HOST_DATA_BASE}/${CONTAINER_NAME}/prometheus"
    print_message "$YELLOW" "  Grafana data: ${HOST_DATA_BASE}/${CONTAINER_NAME}/grafana"
}

# Function to create backup
create_backup() {
    print_message "$YELLOW" "Creating backup of container..."
    
    # Stop container for consistent backup
    lxc stop ${CONTAINER_NAME}
    
    # Export container
    lxc export ${CONTAINER_NAME} "${BACKUP_FILE}"
    
    # Start container again
    lxc start ${CONTAINER_NAME}
    
    print_message "$GREEN" "Backup created: ${BACKUP_FILE}"
    
    # Also create a metadata file
    cat > "${BACKUP_FILE}.info" << EOF
Container: ${CONTAINER_NAME}
Created: $(date)
Debian Version: ${DEBIAN_VERSION}
Architecture: ${ARCH}
Includes: Prometheus, Grafana
Config Base: ${HOST_CONFIG_BASE}/${CONTAINER_NAME}
Data Base: ${HOST_DATA_BASE}/${CONTAINER_NAME}
EOF
    
    print_message "$GREEN" "Metadata saved: ${BACKUP_FILE}.info"
    
    # Upload to Google Drive using rclone if enabled
    if [ "$RCLONE_UPLOAD_ENABLED" = "true" ]; then
        upload_to_gdrive
    fi
}

# Function to upload backup to Google Drive using rclone
upload_to_gdrive() {
    print_message "$YELLOW" "Uploading backup to Google Drive..."
    
    # Check if rclone is installed
    if ! command -v rclone &> /dev/null; then
        print_message "$RED" "rclone is not installed. Skipping upload."
        print_message "$YELLOW" "Install rclone with: sudo apt install rclone"
        return 1
    fi
    
    # Check if remote exists
    if ! rclone listremotes | grep -q "^${RCLONE_REMOTE}:$"; then
        print_message "$RED" "rclone remote '${RCLONE_REMOTE}' not found."
        print_message "$YELLOW" "Available remotes:"
        rclone listremotes
        return 1
    fi
    
    # Create remote directory if it doesn't exist
    rclone mkdir "${RCLONE_REMOTE}:${RCLONE_REMOTE_PATH}" 2>/dev/null || true
    
    # Upload the backup file
    print_message "$YELLOW" "Uploading ${BACKUP_FILE} to ${RCLONE_REMOTE}:${RCLONE_REMOTE_PATH}/"
    
    if rclone copy "${BACKUP_FILE}" "${RCLONE_REMOTE}:${RCLONE_REMOTE_PATH}/" \
        --progress \
        --transfers ${RCLONE_TRANSFERS:-4} \
        --checkers ${RCLONE_CHECKERS:-8} \
        --contimeout 60s \
        --timeout 300s \
        --retries 3 \
        --low-level-retries 10 \
        --stats 10s; then
        
        print_message "$GREEN" "Backup uploaded successfully!"
        
        # Also upload the metadata file
        rclone copy "${BACKUP_FILE}.info" "${RCLONE_REMOTE}:${RCLONE_REMOTE_PATH}/" || true
        
        # List uploaded files
        print_message "$YELLOW" "Files in Google Drive:"
        rclone ls "${RCLONE_REMOTE}:${RCLONE_REMOTE_PATH}/" | tail -5
        
        # Generate shareable link (if possible)
        print_message "$YELLOW" "Backup available at: ${RCLONE_REMOTE}:${RCLONE_REMOTE_PATH}/$(basename ${BACKUP_FILE})"
    else
        print_message "$RED" "Upload failed!"
        return 1
    fi
}

# Function to show container info
show_container_info() {
    print_message "$GREEN" "Container '${CONTAINER_NAME}' is ready!"
    print_message "$YELLOW" "Access information:"
    
    # Get container IP (with retry logic as container might need time to get IP)
    CONTAINER_IP=""
    for i in {1..10}; do
        CONTAINER_IP=$(lxc list ${CONTAINER_NAME} --format=json | jq -r '.[0].state.network.eth0.addresses[] | select(.family=="inet").address' 2>/dev/null || true)
        if [ -n "$CONTAINER_IP" ]; then
            break
        fi
        sleep 1
    done
    
    if [ -z "$CONTAINER_IP" ]; then
        CONTAINER_IP="<not available>"
        print_message "$YELLOW" "  Container IP: Not yet assigned (check with: lxc list clarity)"
    else
        print_message "$YELLOW" "  Container IP: ${CONTAINER_IP}"
    fi
    print_message "$YELLOW" "  Prometheus: http://${CONTAINER_IP}:${PROMETHEUS_PORT}"
    print_message "$YELLOW" "  Grafana: http://${CONTAINER_IP}:${GRAFANA_PORT}"
    print_message "$YELLOW" "  Grafana default login: admin/admin"
}

# Function to overwrite container (delete and rebuild)
overwrite_container() {
    print_message "$YELLOW" "Checking for container '${CONTAINER_NAME}'..."
    
    if check_container_exists; then
        print_message "$YELLOW" "Stopping and removing existing container '${CONTAINER_NAME}'..."
        
        # Stop container if running
        lxc stop ${CONTAINER_NAME} --force 2>/dev/null || true
        
        # Delete container
        lxc delete ${CONTAINER_NAME} --force
        
        print_message "$GREEN" "Container '${CONTAINER_NAME}' removed successfully!"
    else
        print_message "$YELLOW" "Container '${CONTAINER_NAME}' does not exist."
    fi
    
    # Always clean directories for overwrite - no confirmation needed
    print_message "$YELLOW" "Cleaning existing config and data directories..."
    rm -rf "${HOST_CONFIG_BASE}/${CONTAINER_NAME}"
    rm -rf "${HOST_DATA_BASE}/${CONTAINER_NAME}"
    print_message "$GREEN" "Directories cleaned for fresh installation!"
}

# Function to show usage
show_usage() {
    echo "Usage: $0 [OPTIONS]"
    echo ""
    echo "Options:"
    echo "  -o, --overwrite Overwrite (delete and rebuild) the clarity container"
    echo "  -h, --help      Show this help message"
    echo ""
    echo "Examples:"
    echo "  sudo $0              # Build the clarity container"
    echo "  sudo $0 --overwrite  # Overwrite (rebuild) the clarity container"
    exit 0
}

# Main execution
main() {
    # Parse command line arguments
    if [[ $# -gt 0 ]]; then
        case "$1" in
            -o|--overwrite)
                print_message "$YELLOW" "=== Clarity Container Overwrite ==="
                check_root
                check_install_lxd
                overwrite_container
                print_message "$GREEN" "=== Container Deleted. Rebuilding... ==="
                # Continue with normal build process
                # Don't exit, let it fall through to rebuild
                ;;
            -h|--help)
                show_usage
                ;;
            *)
                print_message "$RED" "Unknown option: $1"
                show_usage
                ;;
        esac
    fi
    
    print_message "$GREEN" "=== Clarity Container Builder ==="
    
    # Check if running as root
    check_root
    
    # Check and install LXD if needed
    check_install_lxd
    
    # Check if container exists
    if check_container_exists; then
        print_message "$YELLOW" "Container '${CONTAINER_NAME}' already exists!"
        print_message "$YELLOW" "To rebuild, first delete it with: lxc delete --force ${CONTAINER_NAME}"
        
        # Show existing container info
        show_container_info
        exit 0
    fi
    
    # Create container
    create_container
    
    # Install monitoring stack
    install_monitoring_stack
    
    # Configure bind mounts
    configure_bind_mounts
    
    # Create backup
    create_backup
    
    # Show final information
    show_container_info
    
    print_message "$GREEN" "=== Build Complete ==="
}

# Run main function
main "$@"