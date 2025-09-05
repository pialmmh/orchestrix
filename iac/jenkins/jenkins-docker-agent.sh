#!/bin/bash

# Jenkins Docker Agent Management
# Complete solution for Jenkins agent using Docker

set -e

# Configuration
JENKINS_URL="http://172.82.66.90:8080"
JENKINS_USER="admin"
JENKINS_API_TOKEN="11b7d0764a66a46cdddd2e124cf138fae9"
AGENT_NAME="orchestrix-agent"
CONTAINER_NAME="jenkins-orchestrix-agent"
AGENT_WORKDIR="/var/jenkins_home"

# Colors
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
CYAN='\033[0;36m'
RED='\033[0;31m'
NC='\033[0m'

# Script directory
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"

print_header() {
    echo -e "${CYAN}======================================${NC}"
    echo -e "${CYAN}   Jenkins Docker Agent Management${NC}"
    echo -e "${CYAN}======================================${NC}"
    echo ""
}

check_docker() {
    if ! command -v docker &> /dev/null; then
        echo -e "${RED}❌ Docker is not installed${NC}"
        echo -e "${YELLOW}Installing Docker...${NC}"
        
        # Install Docker
        curl -fsSL https://get.docker.com -o /tmp/get-docker.sh
        sudo sh /tmp/get-docker.sh
        sudo usermod -aG docker $USER
        rm /tmp/get-docker.sh
        
        echo -e "${GREEN}✅ Docker installed${NC}"
        echo -e "${YELLOW}⚠️  Please log out and back in for group changes to take effect${NC}"
        echo -e "${YELLOW}   Then run this script again${NC}"
        exit 0
    fi
    
    # Check if Docker daemon is running
    if ! docker info &> /dev/null; then
        echo -e "${YELLOW}Starting Docker daemon...${NC}"
        sudo systemctl start docker
        sudo systemctl enable docker
    fi
    
    echo -e "${GREEN}✅ Docker is ready${NC}"
}

create_agent_in_jenkins() {
    echo -e "${YELLOW}Creating agent in Jenkins...${NC}"
    
    # Check if agent exists
    response=$(curl -s -o /dev/null -w "%{http_code}" \
        -u "${JENKINS_USER}:${JENKINS_API_TOKEN}" \
        "${JENKINS_URL}/computer/${AGENT_NAME}/api/json")
    
    if [ "$response" == "200" ]; then
        echo -e "${GREEN}✅ Agent '${AGENT_NAME}' already exists in Jenkins${NC}"
    else
        echo -e "${YELLOW}Creating new agent '${AGENT_NAME}'...${NC}"
        
        # Create agent via API
        cat > /tmp/node-config.json << EOF
{
    "name": "${AGENT_NAME}",
    "nodeDescription": "Docker-based Orchestrix Build Agent",
    "numExecutors": "2",
    "remoteFS": "${AGENT_WORKDIR}",
    "labelString": "docker linux orchestrix lxc-builder",
    "mode": "NORMAL",
    "": [
        "hudson.slaves.JNLPLauncher",
        "hudson.slaves.RetentionStrategy\$Always"
    ],
    "launcher": {
        "stapler-class": "hudson.slaves.JNLPLauncher",
        "\$class": "hudson.slaves.JNLPLauncher",
        "workDirSettings": {
            "disabled": false,
            "workDirPath": "",
            "internalDir": "remoting",
            "failIfWorkDirIsMissing": false
        },
        "websocket": false
    },
    "retentionStrategy": {
        "stapler-class": "hudson.slaves.RetentionStrategy\$Always",
        "\$class": "hudson.slaves.RetentionStrategy\$Always"
    }
}
EOF
        
        # Create the agent
        curl -X POST "${JENKINS_URL}/computer/doCreateItem?name=${AGENT_NAME}&type=hudson.slaves.DumbSlave" \
            -u "${JENKINS_USER}:${JENKINS_API_TOKEN}" \
            -H "Content-Type: application/x-www-form-urlencoded" \
            --data-urlencode "json@/tmp/node-config.json" \
            -s -o /dev/null
        
        rm -f /tmp/node-config.json
        echo -e "${GREEN}✅ Agent created in Jenkins${NC}"
    fi
}

get_agent_secret() {
    echo -e "${YELLOW}Getting agent secret from Jenkins...${NC}"
    
    # Try to get the secret via API (this might not work depending on Jenkins version)
    # If it doesn't work, we'll prompt the user
    
    echo -e "${YELLOW}Please get the agent secret from Jenkins UI:${NC}"
    echo -e "${CYAN}1. Go to: ${JENKINS_URL}/computer/${AGENT_NAME}${NC}"
    echo -e "${CYAN}2. Look for the secret in the connection command${NC}"
    echo -e "${CYAN}3. Copy the secret string${NC}"
    echo ""
    read -p "Enter the agent secret: " AGENT_SECRET
    
    if [ -z "$AGENT_SECRET" ]; then
        echo -e "${RED}❌ Secret is required${NC}"
        exit 1
    fi
    
    echo -e "${GREEN}✅ Secret configured${NC}"
}

setup_docker_compose() {
    echo -e "${YELLOW}Setting up Docker Compose configuration...${NC}"
    
    # Create docker-compose file
    cat > ${SCRIPT_DIR}/docker-compose.yml << EOF
version: '3.8'

services:
  jenkins-agent:
    image: jenkins/inbound-agent:alpine-jdk11
    container_name: ${CONTAINER_NAME}
    restart: unless-stopped
    user: root
    environment:
      - JENKINS_URL=${JENKINS_URL}
      - JENKINS_AGENT_NAME=${AGENT_NAME}
      - JENKINS_SECRET=${AGENT_SECRET}
      - JENKINS_AGENT_WORKDIR=${AGENT_WORKDIR}
      - DOCKER_HOST=unix:///var/run/docker.sock
    volumes:
      # Agent workspace
      - jenkins-agent-workspace:${AGENT_WORKDIR}
      
      # Docker socket for Docker-in-Docker
      - /var/run/docker.sock:/var/run/docker.sock
      
      # LXC/LXD access (if needed)
      - /var/snap/lxd:/var/snap/lxd:ro
      - /var/lib/lxd:/var/lib/lxd:ro
      
      # Host tools
      - /usr/bin/docker:/usr/bin/docker:ro
      - /usr/bin/lxc:/usr/bin/lxc:ro
      
      # Project files
      - ${HOME}/telcobright-projects:/home/jenkins/projects
      
    networks:
      - jenkins-net
    logging:
      driver: "json-file"
      options:
        max-size: "10m"
        max-file: "3"

volumes:
  jenkins-agent-workspace:
    name: jenkins-agent-workspace

networks:
  jenkins-net:
    name: jenkins-net
    driver: bridge
EOF
    
    echo -e "${GREEN}✅ Docker Compose configuration created${NC}"
}

create_systemd_service() {
    echo -e "${YELLOW}Creating systemd service for auto-start...${NC}"
    
    sudo tee /etc/systemd/system/jenkins-docker-agent.service > /dev/null << EOF
[Unit]
Description=Jenkins Docker Agent
Requires=docker.service
After=docker.service network-online.target
Wants=network-online.target

[Service]
Type=simple
Restart=always
RestartSec=30
User=${USER}
Group=docker
WorkingDirectory=${SCRIPT_DIR}
ExecStart=/usr/bin/docker-compose up
ExecStop=/usr/bin/docker-compose down
StandardOutput=journal
StandardError=journal

[Install]
WantedBy=multi-user.target
EOF
    
    sudo systemctl daemon-reload
    echo -e "${GREEN}✅ Systemd service created${NC}"
}

start_agent() {
    echo -e "${YELLOW}Starting Jenkins agent...${NC}"
    
    cd ${SCRIPT_DIR}
    
    # Stop any existing container
    docker-compose down 2>/dev/null || true
    
    # Start the agent
    docker-compose up -d
    
    # Wait for container to be ready
    sleep 5
    
    # Check if running
    if docker ps | grep -q ${CONTAINER_NAME}; then
        echo -e "${GREEN}✅ Agent container is running${NC}"
        
        # Enable auto-start
        sudo systemctl enable jenkins-docker-agent.service 2>/dev/null || true
        echo -e "${GREEN}✅ Auto-start enabled${NC}"
    else
        echo -e "${RED}❌ Failed to start agent container${NC}"
        echo -e "${YELLOW}Check logs with: docker-compose logs${NC}"
        exit 1
    fi
}

show_status() {
    echo ""
    echo -e "${CYAN}=== Agent Status ===${NC}"
    
    # Container status
    if docker ps | grep -q ${CONTAINER_NAME}; then
        echo -e "${GREEN}✅ Container: Running${NC}"
        
        # Get container details
        container_id=$(docker ps -q -f name=${CONTAINER_NAME})
        echo -e "${CYAN}   ID: ${container_id:0:12}${NC}"
        
        # Get IP
        container_ip=$(docker inspect -f '{{range .NetworkSettings.Networks}}{{.IPAddress}}{{end}}' ${CONTAINER_NAME} 2>/dev/null || echo "N/A")
        echo -e "${CYAN}   IP: ${container_ip}${NC}"
    else
        echo -e "${RED}❌ Container: Not running${NC}"
    fi
    
    # Jenkins connection status
    echo -e "${CYAN}   Jenkins: ${JENKINS_URL}${NC}"
    echo -e "${CYAN}   Agent: ${JENKINS_URL}/computer/${AGENT_NAME}/${NC}"
    
    echo ""
}

create_management_commands() {
    echo -e "${YELLOW}Creating management commands...${NC}"
    
    # Create management script
    cat > ${SCRIPT_DIR}/manage-agent.sh << 'EOF'
#!/bin/bash

# Jenkins Docker Agent Management Commands

CONTAINER_NAME="jenkins-orchestrix-agent"
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"

case "$1" in
    start)
        echo "Starting Jenkins agent..."
        cd ${SCRIPT_DIR} && docker-compose up -d
        ;;
    stop)
        echo "Stopping Jenkins agent..."
        cd ${SCRIPT_DIR} && docker-compose down
        ;;
    restart)
        echo "Restarting Jenkins agent..."
        cd ${SCRIPT_DIR} && docker-compose restart
        ;;
    status)
        echo "Agent Status:"
        docker ps -a | grep ${CONTAINER_NAME}
        ;;
    logs)
        docker-compose logs -f --tail=100
        ;;
    shell)
        echo "Entering agent container..."
        docker exec -it ${CONTAINER_NAME} /bin/bash
        ;;
    update)
        echo "Updating agent image..."
        cd ${SCRIPT_DIR}
        docker-compose pull
        docker-compose up -d
        ;;
    clean)
        echo "Cleaning up..."
        cd ${SCRIPT_DIR}
        docker-compose down -v
        docker system prune -f
        ;;
    *)
        echo "Usage: $0 {start|stop|restart|status|logs|shell|update|clean}"
        exit 1
        ;;
esac
EOF
    
    chmod +x ${SCRIPT_DIR}/manage-agent.sh
    echo -e "${GREEN}✅ Management commands created${NC}"
}

# Main execution
main() {
    print_header
    
    # Parse command
    case "${1:-setup}" in
        setup)
            check_docker
            create_agent_in_jenkins
            get_agent_secret
            setup_docker_compose
            create_systemd_service
            create_management_commands
            start_agent
            show_status
            
            echo -e "${GREEN}=== ✅ Setup Complete ===${NC}"
            echo ""
            echo -e "${CYAN}Management Commands:${NC}"
            echo -e "  ${YELLOW}./manage-agent.sh start${NC}    - Start agent"
            echo -e "  ${YELLOW}./manage-agent.sh stop${NC}     - Stop agent"
            echo -e "  ${YELLOW}./manage-agent.sh restart${NC}  - Restart agent"
            echo -e "  ${YELLOW}./manage-agent.sh status${NC}   - Check status"
            echo -e "  ${YELLOW}./manage-agent.sh logs${NC}     - View logs"
            echo -e "  ${YELLOW}./manage-agent.sh shell${NC}    - Enter container"
            echo -e "  ${YELLOW}./manage-agent.sh update${NC}   - Update agent image"
            echo ""
            echo -e "${GREEN}Agent should be online at: ${JENKINS_URL}/computer/${AGENT_NAME}/${NC}"
            ;;
            
        start|stop|restart|status|logs|shell|update|clean)
            ${SCRIPT_DIR}/manage-agent.sh $1
            ;;
            
        remove)
            echo -e "${YELLOW}Removing Jenkins Docker agent...${NC}"
            cd ${SCRIPT_DIR}
            docker-compose down -v
            sudo systemctl disable jenkins-docker-agent.service 2>/dev/null || true
            sudo rm -f /etc/systemd/system/jenkins-docker-agent.service
            echo -e "${GREEN}✅ Agent removed${NC}"
            ;;
            
        *)
            echo "Usage: $0 {setup|start|stop|restart|status|logs|remove}"
            exit 1
            ;;
    esac
}

main "$@"