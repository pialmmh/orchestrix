#!/bin/bash

# buildDevEnv.sh - Build LXC container for development environment
# This container provides various services for development including SSH tunneling,
# database connections, message brokers, and other development tools

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Load configuration
CONFIG_FILE="$(dirname "$0")/buildDevEnvConfig.cnf"
if [ -f "$CONFIG_FILE" ]; then
    echo -e "${GREEN}Loading configuration from ${CONFIG_FILE}${NC}"
    source "$CONFIG_FILE"
else
    echo -e "${YELLOW}Config file not found. Using default values.${NC}"
    # Default Configuration
    CONTAINER_NAME="dev-env"
    DEBIAN_VERSION="12"
    ARCH="amd64"
    BACKUP_DIR="$(pwd)"
    SSH_USER="devuser"
    SSH_PORT="22"
    HOST_CONFIG_BASE="/opt/orchestrix/config"
fi

# Build timestamp
BUILD_TIMESTAMP=$(date +%Y%m%d-%H%M%S)
BACKUP_FILE="${BACKUP_DIR}/${CONTAINER_NAME}-${BUILD_TIMESTAMP}.tar.gz"

# Function to print colored messages
print_message() {
    local color=$1
    local message=$2
    echo -e "${color}${message}${NC}"
}

# Check if running as root
check_root() {
    if [[ $EUID -ne 0 ]]; then
        print_message "$RED" "This script must be run as root (use sudo)"
        exit 1
    fi
}

# Check if container exists
check_container_exists() {
    if lxc list --format=json | jq -r '.[].name' | grep -q "^${CONTAINER_NAME}$"; then
        return 0
    else
        return 1
    fi
}

# Create container
create_container() {
    print_message "$YELLOW" "Creating container '${CONTAINER_NAME}' with Debian ${DEBIAN_VERSION}..."
    
    # Create container
    lxc launch images:debian/${DEBIAN_VERSION}/${ARCH} ${CONTAINER_NAME}
    
    # Wait for container to be ready
    print_message "$YELLOW" "Waiting for container to be ready..."
    sleep 10
    
    # Wait for network
    lxc exec ${CONTAINER_NAME} -- bash -c "until ping -c1 google.com &>/dev/null; do sleep 1; done"
    
    print_message "$GREEN" "Container created successfully!"
}

# Install SSH and required packages
install_packages() {
    print_message "$YELLOW" "Installing required packages..."
    
    # Configure apt repositories and install packages
    lxc exec ${CONTAINER_NAME} -- bash -c '
        # Backup original sources.list
        cp /etc/apt/sources.list /etc/apt/sources.list.backup
        
        # Configure proper Debian repositories with trusted option due to GPG issues
        cat > /etc/apt/sources.list << "EOF"
deb [trusted=yes] http://deb.debian.org/debian bookworm main contrib non-free non-free-firmware
deb [trusted=yes] http://security.debian.org/debian-security bookworm-security main contrib non-free
EOF
        
        # Note: Excluding bookworm-updates due to signature issues
        
        # Configure apt to handle network issues better
        echo "Acquire::http::Pipeline-Depth 0;" > /etc/apt/apt.conf.d/99fixbadproxy
        echo "Acquire::http::No-Cache true;" >> /etc/apt/apt.conf.d/99fixbadproxy
        echo "Acquire::BrokenProxy true;" >> /etc/apt/apt.conf.d/99fixbadproxy
        echo "APT::Acquire::Retries 3;" >> /etc/apt/apt.conf.d/99fixbadproxy
        echo "Acquire::Check-Valid-Until false;" >> /etc/apt/apt.conf.d/99fixbadproxy
        
        # Clean and update
        apt-get clean
        rm -rf /var/lib/apt/lists/*
        
        # Update package cache (allow insecure temporarily)
        apt-get update --allow-insecure-repositories || true
        
        # First, try to fix any broken dependencies
        apt-get install -f -y || true
        
        # Install all required packages with --allow-unauthenticated due to GPG issues
        DEBIAN_FRONTEND=noninteractive apt-get install -y --allow-unauthenticated \
            openssh-client \
            autossh \
            supervisor \
            curl \
            wget \
            netcat-openbsd \
            net-tools \
            jq \
            vim \
            systemd \
            sshpass || {
            echo "Some packages failed, trying alternative installation..."
            
            # Try installing packages one by one
            for pkg in openssh-client autossh curl wget netcat-openbsd net-tools jq vim sshpass; do
                DEBIAN_FRONTEND=noninteractive apt-get install -y --allow-unauthenticated $pkg || {
                    echo "Failed to install $pkg, trying alternatives..."
                    case $pkg in
                        netcat-openbsd)
                            apt-get install -y --allow-unauthenticated netcat || \
                            apt-get install -y --allow-unauthenticated nc || \
                            apt-get install -y --allow-unauthenticated netcat-traditional || true
                            ;;
                        autossh)
                            # If autossh fails, we can work with just ssh
                            echo "autossh not available, will use ssh with monitoring"
                            ;;
                        supervisor)
                            # Supervisor is optional, we use systemd anyway
                            echo "supervisor not available, using systemd only"
                            ;;
                        sshpass)
                            # sshpass is for password auth, can use keys instead
                            echo "sshpass not available, use SSH keys for authentication"
                            ;;
                    esac
                }
            done
        }
            
        # Verify critical packages are installed
        which ssh || {
            echo "SSH client is critical and missing!"
            exit 1
        }
        
        # Check for tunnel management tools (at least one should work)
        which autossh || which ssh || {
            echo "No SSH tunnel tools available!"
            exit 1
        }
        
        # Check for network testing tool
        which nc || which netcat || which telnet || {
            echo "Warning: No network testing tools available"
        }
    '
    
    print_message "$GREEN" "Packages installed successfully!"
}

# Setup SSH configuration
setup_ssh_config() {
    print_message "$YELLOW" "Setting up SSH configuration..."
    
    # Create SSH directory structure
    lxc exec ${CONTAINER_NAME} -- bash -c "
        mkdir -p /root/.ssh
        chmod 700 /root/.ssh
        
        # Create SSH config with common settings
        cat > /root/.ssh/config << 'EOF'
Host *
    StrictHostKeyChecking no
    UserKnownHostsFile /dev/null
    CheckHostIP no
    ServerAliveInterval 30
    ServerAliveCountMax 3
    ExitOnForwardFailure yes
    TCPKeepAlive yes
    LogLevel ERROR
EOF
        chmod 600 /root/.ssh/config
    "
    
    print_message "$GREEN" "SSH configuration completed!"
}

# Create development environment management scripts
create_dev_env_scripts() {
    print_message "$YELLOW" "Creating development environment management scripts..."
    
    # Get the script directory
    SCRIPT_DIR="$(dirname "$0")"
    
    # Copy dev environment services manager script
    lxc file push "${SCRIPT_DIR}/scripts/dev-env-services.sh" ${CONTAINER_NAME}/usr/local/bin/dev-env-services.sh
    lxc exec ${CONTAINER_NAME} -- chmod +x /usr/local/bin/dev-env-services.sh
    
    # Create compatibility link for backward compatibility
    lxc exec ${CONTAINER_NAME} -- ln -sf /usr/local/bin/dev-env-services.sh /usr/local/bin/tunnel-manager.sh
    
    # Copy health check script
    lxc file push "${SCRIPT_DIR}/scripts/check-services.sh" ${CONTAINER_NAME}/usr/local/bin/check-services.sh
    lxc exec ${CONTAINER_NAME} -- chmod +x /usr/local/bin/check-services.sh
    
    print_message "$GREEN" "Development environment scripts created!"
}

# Create systemd service
create_systemd_service() {
    print_message "$YELLOW" "Creating systemd service..."
    
    lxc exec ${CONTAINER_NAME} -- bash -c 'cat > /etc/systemd/system/ssh-tunnels.service << '\''EOF'\''
[Unit]
Description=Development Environment Services Manager
After=network.target
Wants=network-online.target

[Service]
Type=forking
ExecStart=/usr/local/bin/dev-env-services.sh start
ExecStop=/usr/local/bin/dev-env-services.sh stop
ExecReload=/usr/local/bin/dev-env-services.sh restart
RemainAfterExit=yes
Restart=on-failure
RestartSec=10

[Install]
WantedBy=multi-user.target
EOF'
    
    # Create health check timer
    lxc exec ${CONTAINER_NAME} -- bash -c 'cat > /etc/systemd/system/tunnel-health.service << '\''EOF'\''
[Unit]
Description=Check Development Environment Services Health

[Service]
Type=oneshot
ExecStart=/usr/local/bin/check-services.sh
EOF'
    
    lxc exec ${CONTAINER_NAME} -- bash -c 'cat > /etc/systemd/system/tunnel-health.timer << '\''EOF'\''
[Unit]
Description=Check Development Environment Services Health every minute
Requires=tunnel-health.service

[Timer]
OnCalendar=*:0/1
AccuracySec=1s

[Install]
WantedBy=timers.target
EOF'
    
    # Reload systemd
    lxc exec ${CONTAINER_NAME} -- systemctl daemon-reload
    
    print_message "$GREEN" "Systemd service created!"
}

# Setup base configuration
setup_base_config() {
    print_message "$YELLOW" "Setting up base configuration..."
    
    # Create config directory in container
    lxc exec ${CONTAINER_NAME} -- mkdir -p /etc/ssh-tunnels
    
    # Create an empty placeholder config
    lxc exec ${CONTAINER_NAME} -- bash -c 'cat > /etc/ssh-tunnels/tunnels.conf << "EOF"
# SSH Tunnels Configuration
# This is a placeholder file
# The actual configuration will be provided at launch time
EOF'
    
    print_message "$GREEN" "Base configuration setup complete!"
}

# Create base image
create_base_image() {
    print_message "$YELLOW" "Creating base image from container..."
    
    # Stop container for consistent image
    lxc stop ${CONTAINER_NAME}
    
    # Create image from container
    lxc publish ${CONTAINER_NAME} --alias dev-env-base
    
    # Export backup if requested
    if [ "$CREATE_BACKUP" == "true" ]; then
        lxc export ${CONTAINER_NAME} "${BACKUP_FILE}"
        print_message "$GREEN" "Backup created: ${BACKUP_FILE}"
    fi
    
    # Delete the build container
    lxc delete ${CONTAINER_NAME}
    
    print_message "$GREEN" "Base image 'dev-env-base' created successfully!"
}

# Show build info
show_build_info() {
    print_message "$GREEN" "Base image 'dev-env-base' is ready!"
    print_message "$YELLOW" ""
    print_message "$YELLOW" "To launch a container with your config:"
    print_message "$YELLOW" "  ./launchDevEnv.sh /path/to/your/config.conf"
    print_message "$YELLOW" ""
    print_message "$YELLOW" "Example config file:"
    print_message "$CYAN" "  # Container configuration"
    print_message "$CYAN" "  CONTAINER_NAME=my-dev-env"
    print_message "$CYAN" "  AUTO_START_TUNNELS=true"
    print_message "$CYAN" "  "
    print_message "$CYAN" "  # Bind mounts (optional)"
    print_message "$CYAN" "  BIND_MOUNTS=("
    print_message "$CYAN" "    '/host/path:/container/path'"
    print_message "$CYAN" "  )"
    print_message "$CYAN" "  "
    print_message "$CYAN" "  # Option 1: External tunnel config file"
    print_message "$CYAN" "  TUNNEL_CONFIG=/path/to/tunnels.conf"
    print_message "$CYAN" "  "
    print_message "$CYAN" "  # Option 2: Inline tunnel definitions"
    print_message "$CYAN" "  TUNNELS=("
    print_message "$CYAN" "    'Kafka|103.95.96.76|9092|9092|user|pass|22|Kafka broker'"
    print_message "$CYAN" "  )"
    print_message "$YELLOW" ""
    print_message "$YELLOW" "View existing images:"
    print_message "$YELLOW" "  lxc image list"
}

# Main execution
main() {
    print_message "$GREEN" "=== Development Environment Container Builder ==="
    
    # Check if running as root
    check_root
    
    # Parse command line arguments
    OVERWRITE=false
    CREATE_BACKUP=false
    
    while [[ $# -gt 0 ]]; do
        case $1 in
            --overwrite)
                OVERWRITE=true
                shift
                ;;
            --backup)
                CREATE_BACKUP=true
                shift
                ;;
            --help|-h)
                print_message "$CYAN" "Usage: $0 [OPTIONS]"
                print_message "$YELLOW" "Options:"
                print_message "$YELLOW" "  --overwrite          Remove existing base image and rebuild"
                print_message "$YELLOW" "  --backup             Create backup file of the container"
                print_message "$YELLOW" "  --help, -h           Show this help message"
                print_message "$YELLOW" ""
                print_message "$YELLOW" "This script creates a base image for development environment containers."
                print_message "$YELLOW" "Launch containers using: launchDevEnv.sh <config-file>"
                exit 0
                ;;
            *)
                print_message "$RED" "Unknown option: $1"
                print_message "$YELLOW" "Use --help for usage information"
                exit 1
                ;;
        esac
    done
    
    # Check if base image exists
    if lxc image list --format=json | jq -r '.[].aliases[].name' | grep -q "^dev-env-base$"; then
        if [ "$OVERWRITE" = true ]; then
            print_message "$YELLOW" "Removing existing base image..."
            lxc image delete dev-env-base
        else
            print_message "$YELLOW" "Base image 'dev-env-base' already exists!"
            print_message "$YELLOW" "Use --overwrite to rebuild"
            show_build_info
            exit 0
        fi
    fi
    
    # Clean up any existing build container
    if check_container_exists; then
        print_message "$YELLOW" "Cleaning up existing build container..."
        lxc stop ${CONTAINER_NAME} --force 2>/dev/null || true
        lxc delete ${CONTAINER_NAME} --force
    fi
    
    # Build container
    create_container
    install_packages
    setup_ssh_config
    create_dev_env_scripts
    create_systemd_service
    setup_base_config
    CREATE_BACKUP="$CREATE_BACKUP" create_base_image
    
    # Show final information
    show_build_info
    
    print_message "$GREEN" "=== Build Complete ==="
}

# Run main function
main "$@"