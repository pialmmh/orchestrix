#!/bin/bash
# Build script for FusionPBX LXC container
# Creates reusable base image with FreeSWITCH and FusionPBX on Debian 12 (64-bit)

set -e

echo "==========================================="
echo "FusionPBX LXC Container Build Script"
echo "Base: Debian 12 (64-bit)"
echo "==========================================="

# Configuration
CONTAINER_NAME="fusion-pbx-build"
BASE_IMAGE="images:debian/12"
IMAGE_ALIAS="fusion-pbx-base"
IMAGE_DESCRIPTION="FusionPBX with FreeSWITCH on Debian 12 (64-bit)"

# Load build configuration
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
CONFIG_FILE="${SCRIPT_DIR}/buildFusionPBXConfig.cnf"

if [ -f "$CONFIG_FILE" ]; then
    echo "Loading configuration from: $CONFIG_FILE"
    source "$CONFIG_FILE"
fi

# Check if build container already exists
echo -n "Checking for existing build container... "
if lxc list | grep -q "$CONTAINER_NAME"; then
    echo "found"
    echo "Stopping and deleting existing build container..."
    lxc stop "$CONTAINER_NAME" --force 2>/dev/null || true
    lxc delete "$CONTAINER_NAME" --force
else
    echo "none"
fi

# Check if image already exists
echo -n "Checking for existing image... "
if lxc image list | grep -q "$IMAGE_ALIAS"; then
    echo "found"
    echo "Deleting existing image $IMAGE_ALIAS..."
    lxc image delete "$IMAGE_ALIAS"
else
    echo "none"
fi

# Create build container
echo "Creating build container from $BASE_IMAGE..."
lxc launch "$BASE_IMAGE" "$CONTAINER_NAME"

# Wait for container to be ready
echo "Waiting for container to be ready..."
sleep 5
while ! lxc exec "$CONTAINER_NAME" -- systemctl is-system-running &>/dev/null; do
    sleep 2
done

# Update system
echo "==================== System Update ===================="
lxc exec "$CONTAINER_NAME" -- apt-get update
lxc exec "$CONTAINER_NAME" -- bash -c "DEBIAN_FRONTEND=noninteractive apt-get upgrade -y"

# Install base packages
echo "==================== Installing Base Packages ===================="
lxc exec "$CONTAINER_NAME" -- bash -c "DEBIAN_FRONTEND=noninteractive apt-get install -y \
    wget \
    curl \
    gnupg \
    lsb-release \
    ca-certificates \
    software-properties-common \
    apt-transport-https \
    sudo \
    openssh-server \
    net-tools \
    iputils-ping \
    vim \
    nano \
    htop \
    git \
    jq"

# Install PostgreSQL
echo "==================== Installing PostgreSQL ===================="
lxc exec "$CONTAINER_NAME" -- bash -c "DEBIAN_FRONTEND=noninteractive apt-get install -y \
    postgresql \
    postgresql-contrib"

# Install FreeSWITCH from SignalWire repository
echo "==================== Installing FreeSWITCH ===================="
# Try SignalWire repository first, fallback to Debian packages if it fails
lxc exec "$CONTAINER_NAME" -- bash -c "
    set -e
    # Method 1: Try SignalWire repository
    echo 'Attempting to add SignalWire FreeSWITCH repository...'
    if wget --timeout=10 -O /tmp/signalwire-freeswitch-repo.gpg https://freeswitch.signalwire.com/repo/deb/debian-release/signalwire-freeswitch-repo.gpg 2>/dev/null; then
        cat /tmp/signalwire-freeswitch-repo.gpg | gpg --dearmor > /usr/share/keyrings/signalwire-freeswitch-repo.gpg
        echo 'deb [signed-by=/usr/share/keyrings/signalwire-freeswitch-repo.gpg] https://freeswitch.signalwire.com/repo/deb/debian-release/ bookworm main' > /etc/apt/sources.list.d/freeswitch.list
        apt-get update
        echo 'Installing FreeSWITCH from SignalWire repository...'
        DEBIAN_FRONTEND=noninteractive apt-get install -y freeswitch-meta-all
    else
        # Method 2: Fallback to Debian packages
        echo 'SignalWire repository unavailable, using Debian packages...'
        apt-get update
        DEBIAN_FRONTEND=noninteractive apt-get install -y \\
            freeswitch \\
            freeswitch-mod-commands \\
            freeswitch-mod-conference \\
            freeswitch-mod-console \\
            freeswitch-mod-dptools \\
            freeswitch-mod-dialplan-xml \\
            freeswitch-mod-dialplan-directory \\
            freeswitch-mod-event-socket \\
            freeswitch-mod-sofia \\
            freeswitch-mod-loopback \\
            freeswitch-mod-rtc \\
            freeswitch-mod-verto \\
            freeswitch-mod-callcenter \\
            freeswitch-mod-fifo \\
            freeswitch-mod-voicemail \\
            freeswitch-mod-directory \\
            freeswitch-mod-flite \\
            freeswitch-mod-say-en \\
            freeswitch-mod-lua \\
            freeswitch-mod-db \\
            freeswitch-mod-sndfile \\
            freeswitch-mod-native-file \\
            freeswitch-mod-local-stream \\
            freeswitch-mod-tone-stream \\
            freeswitch-mod-python3 \\
            freeswitch-mod-pgsql \\
            freeswitch-mod-png \\
            freeswitch-mod-odbc \\
            freeswitch-mod-httapi \\
            freeswitch-mod-spandsp \\
            freeswitch-mod-b64
    fi
    echo 'FreeSWITCH installation completed.'
"

# Install Nginx and PHP 8.2
echo "==================== Installing Nginx and PHP 8.2 ===================="
lxc exec "$CONTAINER_NAME" -- bash -c "DEBIAN_FRONTEND=noninteractive apt-get install -y \
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

# Clone FusionPBX
echo "==================== Installing FusionPBX ===================="
lxc exec "$CONTAINER_NAME" -- bash -c "cd /var/www && git clone https://github.com/fusionpbx/fusionpbx.git"
lxc exec "$CONTAINER_NAME" -- chown -R www-data:www-data /var/www/fusionpbx

# Configure Nginx for FusionPBX
echo "==================== Configuring Nginx ===================="
lxc exec "$CONTAINER_NAME" -- bash -c "cat > /etc/nginx/sites-available/fusionpbx <<'EOF'
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

lxc exec "$CONTAINER_NAME" -- ln -sf /etc/nginx/sites-available/fusionpbx /etc/nginx/sites-enabled/
lxc exec "$CONTAINER_NAME" -- rm -f /etc/nginx/sites-enabled/default
lxc exec "$CONTAINER_NAME" -- nginx -t
lxc exec "$CONTAINER_NAME" -- systemctl restart nginx

# Configure SSH (for development environment)
echo "==================== Configuring SSH ===================="
lxc exec "$CONTAINER_NAME" -- sed -i 's/#PermitRootLogin.*/PermitRootLogin yes/' /etc/ssh/sshd_config
lxc exec "$CONTAINER_NAME" -- sed -i 's/#PasswordAuthentication.*/PasswordAuthentication yes/' /etc/ssh/sshd_config
lxc exec "$CONTAINER_NAME" -- systemctl restart sshd

# Add SSH client config for auto-accept certificates (dev environment)
lxc exec "$CONTAINER_NAME" -- mkdir -p /etc/ssh/ssh_config.d
lxc exec "$CONTAINER_NAME" -- bash -c "cat > /etc/ssh/ssh_config.d/99-dev.conf <<'EOF'
Host *
    StrictHostKeyChecking no
    UserKnownHostsFile /dev/null
    CheckHostIP no
    LogLevel ERROR
EOF"

# Create default user
echo "==================== Creating Default User ===================="
lxc exec "$CONTAINER_NAME" -- useradd -m -s /bin/bash -G sudo fusionpbx || true
lxc exec "$CONTAINER_NAME" -- bash -c "echo 'fusionpbx:fusion123' | chpasswd"
lxc exec "$CONTAINER_NAME" -- bash -c "echo 'fusionpbx ALL=(ALL) NOPASSWD:ALL' > /etc/sudoers.d/fusionpbx"

# Create configuration reader script
echo "==================== Creating Configuration Reader ===================="
lxc exec "$CONTAINER_NAME" -- bash -c "cat > /usr/local/bin/apply-config.sh <<'EOF'
#!/bin/bash
# Reads configuration from mounted config file and applies settings

CONFIG_FILE=\"/mnt/config/fusion-pbx.conf\"

if [ ! -f \"\\\$CONFIG_FILE\" ]; then
    echo \"No configuration file found at \\\$CONFIG_FILE, using defaults\"
    exit 0
fi

echo \"Applying configuration from \\\$CONFIG_FILE\"
source \"\\\$CONFIG_FILE\"

# Apply network configuration
if [ ! -z \"\\\$STATIC_IP\" ]; then
    echo \"Setting static IP: \\\$STATIC_IP\"
    # This would be handled by LXD device config
fi

# Apply SIP profile configuration
if [ ! -z \"\\\$SIP_INTERNAL_IP\" ]; then
    echo \"Configuring SIP internal profile: \\\$SIP_INTERNAL_IP\"
    sed -i \"s/<param name=\\\"rtp-ip\\\" value=\\\".*\\\"\\/>/param name=\\\"rtp-ip\\\" value=\\\"\\\$SIP_INTERNAL_IP\\\"\\/>/g\" /etc/freeswitch/sip_profiles/internal.xml
    sed -i \"s/<param name=\\\"sip-ip\\\" value=\\\".*\\\"\\/>/param name=\\\"sip-ip\\\" value=\\\"\\\$SIP_INTERNAL_IP\\\"\\/>/g\" /etc/freeswitch/sip_profiles/internal.xml
fi

if [ ! -z \"\\\$SIP_EXTERNAL_IP\" ]; then
    echo \"Configuring SIP external profile: \\\$SIP_EXTERNAL_IP\"
    sed -i \"s/<param name=\\\"ext-rtp-ip\\\" value=\\\".*\\\"\\/>/param name=\\\"ext-rtp-ip\\\" value=\\\"\\\$SIP_EXTERNAL_IP\\\"\\/>/g\" /etc/freeswitch/sip_profiles/external.xml
    sed -i \"s/<param name=\\\"ext-sip-ip\\\" value=\\\".*\\\"\\/>/param name=\\\"ext-sip-ip\\\" value=\\\"\\\$SIP_EXTERNAL_IP\\\"\\/>/g\" /etc/freeswitch/sip_profiles/external.xml
fi

# Apply database configuration
if [ ! -z \"\\\$DB_PASSWORD\" ]; then
    echo \"Setting PostgreSQL password\"
    sudo -u postgres psql -c \"ALTER USER postgres PASSWORD '\\\$DB_PASSWORD';\"
fi

# Apply domain configuration
if [ ! -z \"\\\$DOMAIN_NAME\" ]; then
    echo \"Setting domain name: \\\$DOMAIN_NAME\"
    # This would be configured during FusionPBX setup
fi

# Restart services if configuration changed
if [ ! -z \"\\\$RESTART_SERVICES\" ] && [ \"\\\$RESTART_SERVICES\" = \"true\" ]; then
    echo \"Restarting services...\"
    systemctl restart postgresql
    systemctl restart freeswitch
    systemctl restart nginx
    systemctl restart php8.2-fpm
fi

echo \"Configuration applied successfully\"
EOF"

lxc exec "$CONTAINER_NAME" -- chmod +x /usr/local/bin/apply-config.sh

# Create startup service
echo "==================== Creating Startup Service ===================="
lxc exec "$CONTAINER_NAME" -- bash -c "cat > /etc/systemd/system/fusion-pbx-startup.service <<'EOF'
[Unit]
Description=FusionPBX Container Startup Configuration
After=network.target postgresql.service

[Service]
Type=oneshot
ExecStart=/usr/local/bin/apply-config.sh
RemainAfterExit=yes

[Install]
WantedBy=multi-user.target
EOF"

lxc exec "$CONTAINER_NAME" -- systemctl daemon-reload
lxc exec "$CONTAINER_NAME" -- systemctl enable fusion-pbx-startup.service

# Enable services
echo "==================== Enabling Services ===================="
lxc exec "$CONTAINER_NAME" -- systemctl enable postgresql
lxc exec "$CONTAINER_NAME" -- systemctl enable freeswitch
lxc exec "$CONTAINER_NAME" -- systemctl enable nginx
lxc exec "$CONTAINER_NAME" -- systemctl enable php8.2-fpm
lxc exec "$CONTAINER_NAME" -- systemctl enable ssh

# Clean up
echo "==================== Cleaning Up ===================="
lxc exec "$CONTAINER_NAME" -- apt-get clean
lxc exec "$CONTAINER_NAME" -- rm -rf /var/lib/apt/lists/*

# Stop container
echo "==================== Creating Image ===================="
echo "Stopping container..."
lxc stop "$CONTAINER_NAME"

# Create image
echo "Publishing image as $IMAGE_ALIAS..."
lxc publish "$CONTAINER_NAME" --alias "$IMAGE_ALIAS" --description "$IMAGE_DESCRIPTION" --public

# Delete build container
echo "Deleting build container..."
lxc delete "$CONTAINER_NAME"

echo ""
echo "==========================================="
echo "Build Complete!"
echo "==========================================="
echo "Image created: $IMAGE_ALIAS"
echo ""
echo "To launch a container:"
echo "  ./launchFusionPBX.sh /path/to/config.conf"
echo ""
echo "Or use quick start:"
echo "  ./startDefault.sh"
echo ""
echo "FusionPBX will be available at:"
echo "  http://<container-ip>"
echo "  Initial setup: http://<container-ip>/install.php"
echo ""