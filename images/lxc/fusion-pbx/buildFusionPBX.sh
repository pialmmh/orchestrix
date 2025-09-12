#!/bin/bash

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
DEFAULT_CONFIG="${SCRIPT_DIR}/sample-config.conf"
CONFIG_FILE="${1:-$DEFAULT_CONFIG}"

if [ ! -f "$CONFIG_FILE" ]; then
    echo "Error: Config file not found: $CONFIG_FILE"
    echo "Usage: $0 [config-file]"
    exit 1
fi

echo "Using configuration file: $CONFIG_FILE"

source "$CONFIG_FILE"

BASE_IMAGE_NAME="${BASE_IMAGE_NAME:-fusion-pbx-base}"
BASE_IMAGE_USER="${BASE_IMAGE_USER:-ubuntu}"

echo "==================== Starting FusionPBX Base Image Build ===================="
echo "Base image name: $BASE_IMAGE_NAME"
echo "Base image user: $BASE_IMAGE_USER"

EXISTING_CONTAINER=$(lxc list --format=json | jq -r ".[] | select(.name==\"$BASE_IMAGE_NAME\") | .name")
if [ ! -z "$EXISTING_CONTAINER" ]; then
    echo "Container $BASE_IMAGE_NAME already exists. Deleting..."
    lxc delete -f "$BASE_IMAGE_NAME"
fi

EXISTING_IMAGE=$(lxc image list --format=json | jq -r ".[] | select(.aliases[].name==\"$BASE_IMAGE_NAME\") | .aliases[].name" | head -n1)
if [ ! -z "$EXISTING_IMAGE" ]; then
    echo "Image $BASE_IMAGE_NAME already exists. Deleting..."
    lxc image delete "$BASE_IMAGE_NAME"
fi

echo "Creating base container from Debian 12 (Bookworm)..."
lxc launch images:debian/12 "$BASE_IMAGE_NAME"

echo "Waiting for container to be ready..."
sleep 5

while ! lxc exec "$BASE_IMAGE_NAME" -- systemctl is-system-running &>/dev/null; do
    echo "Waiting for system to be ready..."
    sleep 2
done

echo "==================== Installing FusionPBX Dependencies ===================="

lxc exec "$BASE_IMAGE_NAME" -- bash -c "apt-get update"
lxc exec "$BASE_IMAGE_NAME" -- bash -c "DEBIAN_FRONTEND=noninteractive apt-get install -y \
    wget \
    curl \
    gnupg \
    lsb-release \
    ca-certificates \
    software-properties-common \
    sudo \
    openssh-server \
    net-tools \
    iputils-ping \
    vim \
    nano \
    htop \
    git"

echo "==================== Installing PostgreSQL ===================="
lxc exec "$BASE_IMAGE_NAME" -- bash -c "DEBIAN_FRONTEND=noninteractive apt-get install -y postgresql postgresql-contrib"

echo "==================== Installing FreeSWITCH ===================="
# Using SignalWire repository for Debian 12
lxc exec "$BASE_IMAGE_NAME" -- bash -c "apt-get install -y gnupg apt-transport-https lsb-release curl"
lxc exec "$BASE_IMAGE_NAME" -- bash -c "curl -1sLf 'https://freeswitch.signalwire.com/repo/deb/debian-release/signalwire-freeswitch-repo.gpg' | gpg --dearmor -o /usr/share/keyrings/signalwire-freeswitch-repo.gpg"
lxc exec "$BASE_IMAGE_NAME" -- bash -c "echo 'deb [signed-by=/usr/share/keyrings/signalwire-freeswitch-repo.gpg] https://freeswitch.signalwire.com/repo/deb/debian-release/ bookworm main' > /etc/apt/sources.list.d/freeswitch.list"
lxc exec "$BASE_IMAGE_NAME" -- bash -c "apt-get update"
lxc exec "$BASE_IMAGE_NAME" -- bash -c "DEBIAN_FRONTEND=noninteractive apt-get install -y freeswitch-meta-all"

echo "==================== Installing Web Server and PHP ===================="
lxc exec "$BASE_IMAGE_NAME" -- bash -c "DEBIAN_FRONTEND=noninteractive apt-get install -y \
    nginx \
    php8.2-fpm \
    php8.2-pgsql \
    php8.2-curl \
    php8.2-xml \
    php8.2-gd \
    php8.2-mbstring \
    php8.2-zip \
    php8.2-bcmath \
    php8.2-intl \
    php8.2-ldap \
    php8.2-soap \
    php8.2-cli"

echo "==================== Downloading FusionPBX ===================="
lxc exec "$BASE_IMAGE_NAME" -- bash -c "cd /var/www && git clone https://github.com/fusionpbx/fusionpbx.git"
lxc exec "$BASE_IMAGE_NAME" -- bash -c "chown -R www-data:www-data /var/www/fusionpbx"

echo "==================== Configuring Nginx for FusionPBX ===================="
lxc exec "$BASE_IMAGE_NAME" -- bash -c "cat > /etc/nginx/sites-available/fusionpbx <<'EOF'
server {
    listen 80;
    listen [::]:80;
    server_name _;
    
    root /var/www/fusionpbx;
    index index.php;
    
    location / {
        try_files \\\$uri \\\$uri/ /index.php?\\\$query_string;
    }
    
    location ~ \\.php$ {
        include snippets/fastcgi-php.conf;
        fastcgi_pass unix:/var/run/php/php8.2-fpm.sock;
    }
    
    location ~ /\\.ht {
        deny all;
    }
}
EOF"

lxc exec "$BASE_IMAGE_NAME" -- bash -c "ln -sf /etc/nginx/sites-available/fusionpbx /etc/nginx/sites-enabled/"
lxc exec "$BASE_IMAGE_NAME" -- bash -c "rm -f /etc/nginx/sites-enabled/default"
lxc exec "$BASE_IMAGE_NAME" -- bash -c "nginx -t && systemctl restart nginx"

echo "==================== Setting up SSH Configuration ===================="
lxc exec "$BASE_IMAGE_NAME" -- bash -c "sed -i 's/#PermitRootLogin.*/PermitRootLogin yes/' /etc/ssh/sshd_config"
lxc exec "$BASE_IMAGE_NAME" -- bash -c "sed -i 's/#PasswordAuthentication.*/PasswordAuthentication yes/' /etc/ssh/sshd_config"
lxc exec "$BASE_IMAGE_NAME" -- bash -c "systemctl restart sshd"

lxc exec "$BASE_IMAGE_NAME" -- bash -c "mkdir -p /etc/ssh/ssh_config.d"
lxc exec "$BASE_IMAGE_NAME" -- bash -c "cat > /etc/ssh/ssh_config.d/99-dev-env.conf <<'EOF'
Host *
    StrictHostKeyChecking no
    UserKnownHostsFile /dev/null
    CheckHostIP no
    LogLevel ERROR
EOF"

echo "==================== Creating Default User ===================="
lxc exec "$BASE_IMAGE_NAME" -- bash -c "useradd -m -s /bin/bash -G sudo $BASE_IMAGE_USER || true"
lxc exec "$BASE_IMAGE_NAME" -- bash -c "echo '$BASE_IMAGE_USER:debian' | chpasswd"
lxc exec "$BASE_IMAGE_NAME" -- bash -c "echo '$BASE_IMAGE_USER ALL=(ALL) NOPASSWD:ALL' > /etc/sudoers.d/$BASE_IMAGE_USER"

echo "==================== Creating Initialization Script ===================="
lxc exec "$BASE_IMAGE_NAME" -- bash -c "cat > /usr/local/bin/fusion-pbx-init.sh <<'EOF'
#!/bin/bash

CONFIG_FILE=\"/mnt/config/config.conf\"

if [ -f \"\\\$CONFIG_FILE\" ]; then
    source \"\\\$CONFIG_FILE\"
    
    # Set PostgreSQL password if provided
    if [ ! -z \"\\\$DB_PASSWORD\" ]; then
        sudo -u postgres psql -c \"ALTER USER postgres PASSWORD '\\\$DB_PASSWORD';\"
    fi
    
    # Set FusionPBX admin password if provided
    if [ ! -z \"\\\$ADMIN_PASSWORD\" ]; then
        echo \"Admin password will be set during first-time setup: \\\$ADMIN_PASSWORD\"
    fi
    
    # Configure network settings if provided
    if [ ! -z \"\\\$SIP_PROFILE_INTERNAL_IP\" ]; then
        # Update FreeSWITCH SIP profiles
        sed -i \"s/\\\\\\$\\\\{local_ip_v4\\\\}/\\\$SIP_PROFILE_INTERNAL_IP/g\" /etc/freeswitch/vars.xml
    fi
fi

# Ensure services are running
systemctl start postgresql
systemctl start freeswitch
systemctl start nginx
systemctl start php8.2-fpm

echo \"FusionPBX is ready. Access at http://\\\$(hostname -I | awk '{print \\\$1}')\"
echo \"First-time setup: http://\\\$(hostname -I | awk '{print \\\$1}')/install.php\"
EOF"

lxc exec "$BASE_IMAGE_NAME" -- bash -c "chmod +x /usr/local/bin/fusion-pbx-init.sh"

echo "==================== Creating systemd Service ===================="
lxc exec "$BASE_IMAGE_NAME" -- bash -c "cat > /etc/systemd/system/fusion-pbx-init.service <<'EOF'
[Unit]
Description=FusionPBX Initialization Service
After=network.target postgresql.service

[Service]
Type=oneshot
ExecStart=/usr/local/bin/fusion-pbx-init.sh
RemainAfterExit=yes

[Install]
WantedBy=multi-user.target
EOF"

lxc exec "$BASE_IMAGE_NAME" -- bash -c "systemctl daemon-reload"
lxc exec "$BASE_IMAGE_NAME" -- bash -c "systemctl enable fusion-pbx-init.service"

echo "==================== Cleaning up ===================="
lxc exec "$BASE_IMAGE_NAME" -- bash -c "apt-get clean"
lxc exec "$BASE_IMAGE_NAME" -- bash -c "rm -rf /var/lib/apt/lists/*"

echo "==================== Stopping container for snapshot ===================="
lxc stop "$BASE_IMAGE_NAME"

echo "Creating image from container..."
lxc publish "$BASE_IMAGE_NAME" --alias "$BASE_IMAGE_NAME" --public

echo "Removing temporary container..."
lxc delete "$BASE_IMAGE_NAME"

echo "==================== FusionPBX Base Image Build Complete ===================="
echo "Image created: $BASE_IMAGE_NAME"
echo ""
echo "You can now launch containers using: ./launchFusionPBX.sh [config-file]"
echo "Or use the quick start: ./startDefault.sh"