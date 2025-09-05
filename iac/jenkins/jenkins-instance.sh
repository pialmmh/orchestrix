#!/bin/bash
# Jenkins Instance Manager
# Manages multiple Jenkins instances and their agents

set -e

INSTANCES_DIR="$(dirname "$0")/instances"
DEFAULT_CONFIG="$INSTANCES_DIR/default.yml"

# Get active instance from default.yml
get_active_instance() {
    grep "active_instance:" "$DEFAULT_CONFIG" | awk '{print $2}'
}

# Load instance configuration
load_instance_config() {
    local instance=$1
    INSTANCE_DIR="$INSTANCES_DIR/$instance"
    CONFIG_FILE="$INSTANCE_DIR/config.yml"
    
    if [ ! -f "$CONFIG_FILE" ]; then
        echo "Error: Instance '$instance' not found"
        exit 1
    fi
    
    # Extract key values
    JENKINS_URL=$(grep "jenkins_url:" "$CONFIG_FILE" | head -1 | awk '{print $2}')
    echo "Loaded instance: $instance ($JENKINS_URL)"
}

# Command functions
cmd_list() {
    echo "Available Jenkins instances:"
    echo "----------------------------"
    active_instance=$(get_active_instance)
    
    # List all directories that have config.yml
    for dir in "$INSTANCES_DIR"/*/; do
        if [ -f "$dir/config.yml" ]; then
            instance=$(basename "$dir")
            url=$(grep "jenkins_url:" "$dir/config.yml" | head -1 | awk '{print $2}')
            active=""
            [ "$instance" = "$active_instance" ] && active=" [ACTIVE]"
            echo "  $instance: $url$active"
        fi
    done
}

cmd_use() {
    local instance=$1
    if [ ! -d "$INSTANCES_DIR/$instance" ]; then
        echo "Error: Instance '$instance' does not exist"
        exit 1
    fi
    
    # Update default.yml
    sed -i "s/active_instance:.*/active_instance: $instance/" "$DEFAULT_CONFIG"
    echo "Switched to instance: $instance"
}

cmd_start() {
    local instance=${1:-$(get_active_instance)}
    load_instance_config "$instance"
    
    echo "Starting agents for instance: $instance"
    echo "Configuration: $CONFIG_FILE"
    echo ""
    echo "Agents configured:"
    awk '/^agents:/{flag=1;next}/^[^ ]/{flag=0}flag&&/^  [^ ]/{print "  - "$1}' "$CONFIG_FILE" | sed 's/:$//' 
    echo ""
    echo "Use these secrets to configure your agents"
}

cmd_stop() {
    local instance=${1:-$(get_active_instance)}
    load_instance_config "$instance"
    
    echo "Stopping agents for instance: $instance"
    echo "Stop Docker agents: docker stop jenkins-agent"
    echo "Stop LXC agents: sudo lxc stop dev-env-*"
}

cmd_status() {
    local instance=${1:-$(get_active_instance)}
    load_instance_config "$instance"
    
    echo "Status for instance: $instance"
    echo "--------------------------------"
    echo "Jenkins URL: $JENKINS_URL"
    echo ""
    
    # Check Docker agents
    echo "Docker Agents:"
    docker ps --format "table {{.Names}}\t{{.Status}}" | grep jenkins || echo "  None running"
    echo ""
    
    # Check LXC agents
    echo "LXC Agents:"
    sudo lxc list --format=csv -c n,s | grep dev-env || echo "  None running"
}

cmd_config() {
    local instance=${1:-$(get_active_instance)}
    load_instance_config "$instance"
    
    echo "Configuration for: $instance"
    echo "----------------------------"
    cat "$CONFIG_FILE"
}

cmd_create() {
    local name=$1
    local url=$2
    
    if [ -z "$name" ] || [ -z "$url" ]; then
        echo "Usage: $0 create <instance-name> <jenkins-url>"
        exit 1
    fi
    
    mkdir -p "$INSTANCES_DIR/$name"
    
    # Create basic config
    cat > "$INSTANCES_DIR/$name/config.yml" << EOF
# $name Jenkins Instance Configuration

# Jenkins Server
jenkins_url: $url
jenkins_web_port: 8080
jenkins_jnlp_port: 50000

# Admin Credentials
admin_user: admin
admin_api_token: # Add token here

# Agents
agents:
  # agent-name:
  #   secret: # Add secret from Jenkins
  #   type: docker
  #   workdir: /var/jenkins
EOF
    
    echo "Created instance: $name"
    echo "Edit configuration: $INSTANCES_DIR/$name/config.yml"
}

# Main command handler
case "${1:-help}" in
    list)
        cmd_list
        ;;
    use)
        cmd_use "$2"
        ;;
    start)
        cmd_start "$2"
        ;;
    stop)
        cmd_stop "$2"
        ;;
    status)
        cmd_status "$2"
        ;;
    config)
        cmd_config "$2"
        ;;
    create)
        cmd_create "$2" "$3"
        ;;
    help|*)
        echo "Jenkins Instance Manager"
        echo "Usage: $0 <command> [instance]"
        echo ""
        echo "Commands:"
        echo "  list              List all instances"
        echo "  use <instance>    Switch to instance"
        echo "  start [instance]  Start agents for instance"
        echo "  stop [instance]   Stop agents for instance"
        echo "  status [instance] Show instance status"
        echo "  config [instance] Show instance configuration"
        echo "  create <name> <url> Create new instance"
        echo ""
        echo "Active instance: $(get_active_instance)"
        ;;
esac