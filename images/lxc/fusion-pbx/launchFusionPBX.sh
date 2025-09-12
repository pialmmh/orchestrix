#!/bin/bash

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
DEFAULT_CONFIG="${SCRIPT_DIR}/sample-config.conf"
CONFIG_FILE="${1:-$DEFAULT_CONFIG}"

if [ ! -f "$CONFIG_FILE" ]; then
    echo "Error: Config file not found: $CONFIG_FILE"
    echo "Usage: $0 [config-file]"
    echo ""
    echo "Config file can be located anywhere in the filesystem."
    echo "If no config file is specified, uses: $DEFAULT_CONFIG"
    exit 1
fi

echo "Using configuration file: $CONFIG_FILE"
CONFIG_ABS_PATH="$(cd "$(dirname "$CONFIG_FILE")" && pwd)/$(basename "$CONFIG_FILE")"

source "$CONFIG_FILE"

BASE_IMAGE_NAME="${BASE_IMAGE_NAME:-fusion-pbx-base}"
CONTAINER_NAME="${CONTAINER_NAME:-fusion-pbx-dev}"
CONTAINER_IP="${CONTAINER_IP:-}"
WORKSPACE_MOUNT="${WORKSPACE_MOUNT:-}"
CONFIG_MOUNT="${CONFIG_MOUNT:-}"
DATA_MOUNT="${DATA_MOUNT:-}"
FREESWITCH_CONF_MOUNT="${FREESWITCH_CONF_MOUNT:-}"
FUSIONPBX_APP_MOUNT="${FUSIONPBX_APP_MOUNT:-}"

echo "==================== Launching FusionPBX Container ===================="
echo "Container name: $CONTAINER_NAME"
echo "Base image: $BASE_IMAGE_NAME"
if [ ! -z "$CONTAINER_IP" ]; then
    echo "Container IP: $CONTAINER_IP"
fi

EXISTING_IMAGE=$(lxc image list --format=json | jq -r ".[] | select(.aliases[].name==\"$BASE_IMAGE_NAME\") | .aliases[].name" | head -n1)
if [ -z "$EXISTING_IMAGE" ]; then
    echo "Error: Base image $BASE_IMAGE_NAME not found!"
    echo "Please run buildFusionPBX.sh first to create the base image."
    exit 1
fi

EXISTING_CONTAINER=$(lxc list --format=json | jq -r ".[] | select(.name==\"$CONTAINER_NAME\") | .name")
if [ ! -z "$EXISTING_CONTAINER" ]; then
    echo "Container $CONTAINER_NAME already exists."
    read -p "Do you want to delete and recreate it? (y/n): " -n 1 -r
    echo
    if [[ $REPLY =~ ^[Yy]$ ]]; then
        echo "Stopping and deleting existing container..."
        lxc stop "$CONTAINER_NAME" --force 2>/dev/null || true
        lxc delete "$CONTAINER_NAME" --force
    else
        echo "Exiting without changes."
        exit 0
    fi
fi

echo "Creating container from image..."
lxc launch "$BASE_IMAGE_NAME" "$CONTAINER_NAME"

if [ ! -z "$CONTAINER_IP" ]; then
    echo "Setting static IP address: $CONTAINER_IP"
    
    DEFAULT_NETWORK=$(lxc network list --format=json | jq -r '.[] | select(.type=="bridge" and .managed==true) | .name' | head -n1)
    if [ -z "$DEFAULT_NETWORK" ]; then
        DEFAULT_NETWORK="lxdbr0"
    fi
    
    lxc config device override "$CONTAINER_NAME" eth0 ipv4.address="$CONTAINER_IP"
    
    lxc restart "$CONTAINER_NAME" --force
    
    echo "Waiting for container to get IP address..."
    for i in {1..30}; do
        CURRENT_IP=$(lxc list "$CONTAINER_NAME" --format=json | jq -r '.[0].state.network.eth0.addresses[] | select(.family=="inet").address' 2>/dev/null)
        if [ "$CURRENT_IP" = "$CONTAINER_IP" ]; then
            echo "Container IP configured successfully: $CURRENT_IP"
            break
        fi
        sleep 1
    done
fi

echo "Waiting for container to be ready..."
sleep 3

while ! lxc exec "$CONTAINER_NAME" -- systemctl is-system-running &>/dev/null; do
    echo "Waiting for system to be ready..."
    sleep 2
done

CONFIG_DIR="$(dirname "$CONFIG_ABS_PATH")"
lxc config device add "$CONTAINER_NAME" config-mount disk source="$CONFIG_DIR" path=/mnt/config

if [ ! -z "$WORKSPACE_MOUNT" ]; then
    if [ -d "$WORKSPACE_MOUNT" ]; then
        echo "Mounting workspace: $WORKSPACE_MOUNT -> /workspace"
        lxc config device add "$CONTAINER_NAME" workspace disk source="$WORKSPACE_MOUNT" path=/workspace
    else
        echo "Warning: Workspace directory not found: $WORKSPACE_MOUNT"
    fi
fi

if [ ! -z "$DATA_MOUNT" ]; then
    DATA_ABS_PATH="$(cd "$(dirname "$DATA_MOUNT")" 2>/dev/null && pwd)/$(basename "$DATA_MOUNT")" || DATA_ABS_PATH="$DATA_MOUNT"
    
    if [ ! -d "$DATA_ABS_PATH" ]; then
        echo "Creating data directory: $DATA_ABS_PATH"
        mkdir -p "$DATA_ABS_PATH"
    fi
    
    echo "Mounting data directory: $DATA_ABS_PATH -> /var/lib/postgresql"
    lxc config device add "$CONTAINER_NAME" data-mount disk source="$DATA_ABS_PATH" path=/var/lib/postgresql
fi

if [ ! -z "$FREESWITCH_CONF_MOUNT" ]; then
    FREESWITCH_ABS_PATH="$(cd "$(dirname "$FREESWITCH_CONF_MOUNT")" 2>/dev/null && pwd)/$(basename "$FREESWITCH_CONF_MOUNT")" || FREESWITCH_ABS_PATH="$FREESWITCH_CONF_MOUNT"
    
    if [ ! -d "$FREESWITCH_ABS_PATH" ]; then
        echo "Creating FreeSWITCH config directory: $FREESWITCH_ABS_PATH"
        mkdir -p "$FREESWITCH_ABS_PATH"
    fi
    
    echo "Mounting FreeSWITCH config: $FREESWITCH_ABS_PATH -> /etc/freeswitch"
    lxc config device add "$CONTAINER_NAME" freeswitch-conf disk source="$FREESWITCH_ABS_PATH" path=/etc/freeswitch
fi

if [ ! -z "$FUSIONPBX_APP_MOUNT" ]; then
    FUSIONPBX_ABS_PATH="$(cd "$(dirname "$FUSIONPBX_APP_MOUNT")" 2>/dev/null && pwd)/$(basename "$FUSIONPBX_APP_MOUNT")" || FUSIONPBX_ABS_PATH="$FUSIONPBX_APP_MOUNT"
    
    if [ ! -d "$FUSIONPBX_ABS_PATH" ]; then
        echo "Creating FusionPBX app directory: $FUSIONPBX_ABS_PATH"
        mkdir -p "$FUSIONPBX_ABS_PATH"
    fi
    
    echo "Mounting FusionPBX app: $FUSIONPBX_ABS_PATH -> /var/www/fusionpbx"
    lxc config device add "$CONTAINER_NAME" fusionpbx-app disk source="$FUSIONPBX_ABS_PATH" path=/var/www/fusionpbx
fi

echo "Running initialization script..."
lxc exec "$CONTAINER_NAME" -- /usr/local/bin/fusion-pbx-init.sh

if [ ! -z "$SSH_TUNNEL_HOST" ] && [ ! -z "$SSH_TUNNEL_USER" ] && [ ! -z "$SSH_TUNNEL_PORTS" ]; then
    echo "==================== Setting up SSH Tunnels ===================="
    
    lxc exec "$CONTAINER_NAME" -- bash -c "mkdir -p /home/ubuntu/.ssh"
    
    if [ ! -z "$SSH_KEY_PATH" ] && [ -f "$SSH_KEY_PATH" ]; then
        echo "Copying SSH key..."
        lxc file push "$SSH_KEY_PATH" "$CONTAINER_NAME/home/ubuntu/.ssh/tunnel_key"
        lxc exec "$CONTAINER_NAME" -- chmod 600 /home/ubuntu/.ssh/tunnel_key
        lxc exec "$CONTAINER_NAME" -- chown ubuntu:ubuntu /home/ubuntu/.ssh/tunnel_key
        SSH_KEY_OPT="-i /home/ubuntu/.ssh/tunnel_key"
    else
        SSH_KEY_OPT=""
    fi
    
    IFS=',' read -ra PORTS <<< "$SSH_TUNNEL_PORTS"
    for PORT_MAP in "${PORTS[@]}"; do
        IFS=':' read -ra PARTS <<< "$PORT_MAP"
        LOCAL_PORT="${PARTS[0]}"
        REMOTE_HOST="${PARTS[1]:-127.0.0.1}"
        REMOTE_PORT="${PARTS[2]:-$LOCAL_PORT}"
        
        SERVICE_NAME="ssh-tunnel-${LOCAL_PORT}"
        
        lxc exec "$CONTAINER_NAME" -- bash -c "cat > /etc/systemd/system/${SERVICE_NAME}.service <<EOF
[Unit]
Description=SSH Tunnel for port ${LOCAL_PORT}
After=network.target

[Service]
Type=simple
User=ubuntu
ExecStart=/usr/bin/ssh -N -o StrictHostKeyChecking=no -o UserKnownHostsFile=/dev/null -o ServerAliveInterval=30 -o ServerAliveCountMax=3 -L ${LOCAL_PORT}:${REMOTE_HOST}:${REMOTE_PORT} ${SSH_KEY_OPT} ${SSH_TUNNEL_USER}@${SSH_TUNNEL_HOST}
Restart=always
RestartSec=10

[Install]
WantedBy=multi-user.target
EOF"
        
        lxc exec "$CONTAINER_NAME" -- systemctl daemon-reload
        lxc exec "$CONTAINER_NAME" -- systemctl enable "${SERVICE_NAME}"
        lxc exec "$CONTAINER_NAME" -- systemctl start "${SERVICE_NAME}"
        
        echo "SSH tunnel configured: localhost:${LOCAL_PORT} -> ${SSH_TUNNEL_HOST}:${REMOTE_PORT}"
    done
fi

echo ""
echo "==================== FusionPBX Container Launch Complete ===================="
echo ""
echo "Container: $CONTAINER_NAME"

CONTAINER_IP=$(lxc list "$CONTAINER_NAME" --format=json | jq -r '.[0].state.network.eth0.addresses[] | select(.family=="inet").address' 2>/dev/null)
echo "IP Address: ${CONTAINER_IP:-Not assigned}"
echo ""
echo "Access Methods:"
echo "  - Web Interface: http://${CONTAINER_IP:-<container-ip>}"
echo "  - First-time setup: http://${CONTAINER_IP:-<container-ip>}/install.php"
echo "  - SSH: ssh ubuntu@${CONTAINER_IP:-<container-ip>} (password: debian)"
echo "  - Shell: lxc exec $CONTAINER_NAME -- bash"
echo ""
echo "Default Credentials:"
echo "  - SSH: ubuntu/debian"
echo "  - PostgreSQL: postgres/(set via config)"
echo "  - FusionPBX Admin: (set during first-time setup)"
echo ""

if [ ! -z "$WORKSPACE_MOUNT" ]; then
    echo "Workspace mounted at: /workspace"
fi

if [ ! -z "$DATA_MOUNT" ]; then
    echo "PostgreSQL data mounted from: $DATA_MOUNT"
fi

if [ ! -z "$FREESWITCH_CONF_MOUNT" ]; then
    echo "FreeSWITCH config mounted from: $FREESWITCH_CONF_MOUNT"
fi

if [ ! -z "$FUSIONPBX_APP_MOUNT" ]; then
    echo "FusionPBX app mounted from: $FUSIONPBX_APP_MOUNT"
fi

echo ""
echo "Services Status:"
lxc exec "$CONTAINER_NAME" -- systemctl is-active postgresql && echo "  - PostgreSQL: active" || echo "  - PostgreSQL: inactive"
lxc exec "$CONTAINER_NAME" -- systemctl is-active freeswitch && echo "  - FreeSWITCH: active" || echo "  - FreeSWITCH: inactive"
lxc exec "$CONTAINER_NAME" -- systemctl is-active nginx && echo "  - Nginx: active" || echo "  - Nginx: inactive"
lxc exec "$CONTAINER_NAME" -- systemctl is-active php8.2-fpm && echo "  - PHP-FPM: active" || echo "  - PHP-FPM: inactive"