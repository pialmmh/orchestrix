#!/bin/bash

# Jenkins Installation Script for Oracle Cloud Server (Ubuntu/Debian)
# This script installs Jenkins with all prerequisites

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

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

# Function to detect OS
detect_os() {
    if [[ -f /etc/os-release ]]; then
        . /etc/os-release
        OS=$NAME
        VER=$VERSION_ID
    else
        print_message "$RED" "Cannot detect OS"
        exit 1
    fi
    print_message "$GREEN" "Detected OS: $OS $VER"
}

# Main installation
main() {
    print_message "$BLUE" "================================"
    print_message "$BLUE" "Jenkins Installation Script"
    print_message "$BLUE" "================================"
    
    # Check root
    check_root
    
    # Detect OS
    detect_os
    
    # Update system
    print_message "$YELLOW" "Updating system packages..."
    apt update
    apt upgrade -y
    
    # Install Java 11
    print_message "$YELLOW" "Installing Java 11..."
    apt install openjdk-11-jdk -y
    
    # Verify Java installation
    java -version
    print_message "$GREEN" "Java installed successfully!"
    
    # Install required packages
    print_message "$YELLOW" "Installing required packages..."
    apt install -y \
        curl \
        wget \
        gnupg \
        lsb-release \
        software-properties-common \
        apt-transport-https \
        ca-certificates \
        git \
        unzip
    
    # Add Jenkins repository
    print_message "$YELLOW" "Adding Jenkins repository..."
    curl -fsSL https://pkg.jenkins.io/debian-stable/jenkins.io-2023.key | tee \
        /usr/share/keyrings/jenkins-keyring.asc > /dev/null
    
    echo deb [signed-by=/usr/share/keyrings/jenkins-keyring.asc] \
        https://pkg.jenkins.io/debian-stable binary/ | tee \
        /etc/apt/sources.list.d/jenkins.list > /dev/null
    
    # Update package index
    print_message "$YELLOW" "Updating package index..."
    apt update
    
    # Install Jenkins
    print_message "$YELLOW" "Installing Jenkins..."
    apt install jenkins -y
    
    # Start and enable Jenkins
    print_message "$YELLOW" "Starting Jenkins service..."
    systemctl start jenkins
    systemctl enable jenkins
    
    # Configure firewall (UFW)
    print_message "$YELLOW" "Configuring firewall..."
    if command -v ufw &> /dev/null; then
        ufw allow 8080/tcp comment 'Jenkins Web UI'
        ufw allow 50000/tcp comment 'Jenkins JNLP Agents'
        ufw allow 22/tcp comment 'SSH'
        print_message "$GREEN" "Firewall rules added!"
    else
        print_message "$YELLOW" "UFW not found. Please configure firewall manually."
    fi
    
    # Install additional tools
    print_message "$YELLOW" "Installing additional tools..."
    
    # Install Docker (for containerized builds)
    if ! command -v docker &> /dev/null; then
        print_message "$YELLOW" "Installing Docker..."
        curl -fsSL https://get.docker.com -o get-docker.sh
        sh get-docker.sh
        usermod -aG docker jenkins
        systemctl restart jenkins
        rm get-docker.sh
        print_message "$GREEN" "Docker installed!"
    fi
    
    # Install rclone (for Google Drive uploads)
    if ! command -v rclone &> /dev/null; then
        print_message "$YELLOW" "Installing rclone..."
        curl https://rclone.org/install.sh | bash
        print_message "$GREEN" "rclone installed!"
    fi
    
    # Install LXD/LXC tools (for container management)
    print_message "$YELLOW" "Installing LXD/LXC tools..."
    if ! command -v lxc &> /dev/null; then
        snap install lxd
        lxd init --auto
        usermod -aG lxd jenkins
        print_message "$GREEN" "LXD installed!"
    fi
    
    # Create Jenkins workspace directory
    mkdir -p /var/lib/jenkins/workspace
    chown -R jenkins:jenkins /var/lib/jenkins/workspace
    
    # Get Jenkins initial admin password
    print_message "$YELLOW" "Waiting for Jenkins to generate initial admin password..."
    sleep 10
    
    if [[ -f /var/lib/jenkins/secrets/initialAdminPassword ]]; then
        JENKINS_PASSWORD=$(cat /var/lib/jenkins/secrets/initialAdminPassword)
        print_message "$GREEN" "================================"
        print_message "$GREEN" "Jenkins Installation Complete!"
        print_message "$GREEN" "================================"
        print_message "$YELLOW" "Jenkins Web UI: http://$(curl -s ifconfig.me):8080"
        print_message "$YELLOW" "Initial Admin Password: $JENKINS_PASSWORD"
        print_message "$GREEN" "================================"
    else
        print_message "$YELLOW" "Initial password file not found yet."
        print_message "$YELLOW" "Run this command to get it:"
        print_message "$BLUE" "sudo cat /var/lib/jenkins/secrets/initialAdminPassword"
    fi
    
    # Check Jenkins status
    print_message "$YELLOW" "\nJenkins Service Status:"
    systemctl status jenkins --no-pager | head -n 10
    
    # Oracle Cloud specific instructions
    print_message "$YELLOW" "\n================================"
    print_message "$YELLOW" "Oracle Cloud Configuration:"
    print_message "$YELLOW" "================================"
    print_message "$BLUE" "1. Go to Oracle Cloud Console"
    print_message "$BLUE" "2. Navigate to: Networking → Virtual Cloud Networks → Your VCN"
    print_message "$BLUE" "3. Click on Security Lists → Default Security List"
    print_message "$BLUE" "4. Add Ingress Rules:"
    print_message "$BLUE" "   - Port 8080 (Jenkins Web UI)"
    print_message "$BLUE" "   - Port 50000 (Jenkins Agents)"
    print_message "$BLUE" "5. Source CIDR: 0.0.0.0/0 (or restrict to your IP)"
    print_message "$YELLOW" "================================"
    
    # Save installation info
    cat > /root/jenkins_install_info.txt << EOF
Jenkins Installation Information
================================
Date: $(date)
Jenkins URL: http://$(curl -s ifconfig.me):8080
Initial Password: ${JENKINS_PASSWORD:-Check /var/lib/jenkins/secrets/initialAdminPassword}
Jenkins Home: /var/lib/jenkins
Jenkins User: jenkins
Service: systemctl status jenkins

Installed Components:
- Java 11
- Jenkins (latest stable)
- Docker
- rclone
- LXD/LXC

Firewall Ports:
- 8080 (Jenkins Web UI)
- 50000 (JNLP Agents)
- 22 (SSH)

Next Steps:
1. Configure Oracle Cloud Security Lists
2. Access Jenkins Web UI
3. Complete initial setup wizard
4. Install suggested plugins
5. Create admin user
EOF
    
    print_message "$GREEN" "Installation info saved to: /root/jenkins_install_info.txt"
    print_message "$GREEN" "\nJenkins is ready! Access it at: http://$(curl -s ifconfig.me):8080"
}

# Run main function
main "$@"