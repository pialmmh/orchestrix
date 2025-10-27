# Tunnel Gateway - Developer Installation Guide

## Quick Start

This installation script automates the complete setup of the tunnel-gateway container on any Linux development machine.

### Prerequisites

- Linux machine (Ubuntu, Debian, or similar)
- sudo access
- Downloaded tunnel-gateway tar.gz image

### Installation Steps

1. **Download the tunnel-gateway image** (tar.gz file)

2. **Place files in a directory:**
   ```bash
   mkdir tunnel-gateway-setup
   cd tunnel-gateway-setup
   
   # Copy the image file here
   cp /path/to/tunnel-gateway-v*.tar.gz .
   
   # Copy the installation script
   cp /path/to/install-tunnel-gateway.sh .
   ```

3. **Run the installation script:**
   ```bash
   chmod +x install-tunnel-gateway.sh
   ./install-tunnel-gateway.sh
   ```

## What the Script Does

The installation script automates:

1. ✅ **LXD Installation** - Installs LXD if not present
2. ✅ **Network Configuration** - Sets up LXD bridge networking
3. ✅ **Image Import** - Imports tunnel-gateway from tar.gz
4. ✅ **Interactive Configuration** - Guides you through tunnel setup
5. ✅ **Container Launch** - Creates and starts the container
6. ✅ **Tunnel Activation** - Starts all configured SSH tunnels

## Interactive Configuration

The script will ask you for:

### Container Settings
- **Container name** (default: tunnel-gateway-dev)
- **Container IP** (default: 10.10.199.150)

### For Each Tunnel
- **Service name** (e.g., mysql-prod, kafka-staging)
- **Local port** (port on container, e.g., 3306)
- **Remote SSH host** (server to SSH into)
- **SSH username** (default: mustafa)
- **Authentication type** (password or key)
- **Password or key path**
- **Remote host** (where service runs, often localhost)
- **Remote port** (port on remote server)

Type `done` when finished adding tunnels.

## Example Session

```bash
$ ./install-tunnel-gateway.sh

==========================================
  Tunnel Gateway - Developer Setup
  SSH Tunnel Proxy for Development
==========================================

[INFO] Checking LXD installation...
[SUCCESS] LXD is already installed

[INFO] Checking LXD initialization...
[SUCCESS] LXD bridge already configured
[INFO] Bridge IP: 10.10.199.1/24

[INFO] Looking for tunnel-gateway image...
[INFO] Found image: ./tunnel-gateway-v1-20251028-035621.tar.gz
[INFO] Importing tunnel-gateway image...
[SUCCESS] Image imported as tunnel-gateway-base

[INFO] === Configure SSH Tunnels ===

Enter container name (default: tunnel-gateway-dev):
my-dev-tunnels

Enter container IP address (default: 10.10.199.150):
10.10.199.150

[INFO] === Configure Tunnel #1 ===

Service name (e.g., mysql-prod, kafka-staging, or 'done' to finish):
mysql-prod

Local port (port on this container, e.g., 3306 for MySQL, 9092 for Kafka):
3306

Remote SSH host (IP or hostname, e.g., db.example.com):
db.production.com

SSH username (default: mustafa):
dbadmin

Authentication type (password/key) [default: password]:
password

SSH password:
[hidden]

Remote host (where the service actually runs, usually 'localhost' or same as SSH host):
localhost

Remote port (port on remote host, e.g., 3306 for MySQL):
3306

[SUCCESS] Tunnel configured: mysql-prod
  Local: 0.0.0.0:3306 -> SSH: dbadmin@db.production.com -> Remote: localhost:3306

[INFO] === Configure Tunnel #2 ===

Service name (e.g., mysql-prod, kafka-staging, or 'done' to finish):
done

[SUCCESS] Configuration saved to: tunnel-gateway-config-20251028-041530.conf

[INFO] === Launching Tunnel Gateway Container ===
[INFO] Launching container my-dev-tunnels...
[INFO] Configuring static IP: 10.10.199.150
[INFO] Pushing tunnel configuration to container...
[INFO] Starting SSH tunnels...

Starting SSH tunnels...
======================
Starting tunnel: mysql-prod
  Local: 0.0.0.0:3306 -> SSH: dbadmin@db.production.com -> Remote: localhost:3306
  Started successfully

All tunnels started

[SUCCESS] === Tunnel Gateway Ready! ===

Connection examples:
  mysql-prod: Connect to 10.10.199.150:3306

[SUCCESS] Setup complete! Your tunnel gateway is running.
```

## Configuration File

The script generates a configuration file (e.g., `tunnel-gateway-config-20251028-041530.conf`) that you can:

- **Reuse** for identical setups on other machines
- **Modify** for different environments
- **Version control** with your project

## Common Use Cases

### 1. MySQL Development Database

```
Service name: mysql-prod
Local port: 3306
SSH host: db.production.com
SSH user: dbuser
Auth: password
Remote host: localhost
Remote port: 3306
```

**Usage in your code:**
```java
jdbc:mysql://10.10.199.150:3306/mydb
```

### 2. Kafka Development Cluster

```
Service name: kafka-staging
Local port: 9092
SSH host: kafka.staging.com
SSH user: kafkauser
Auth: key
Key path: /home/user/.ssh/kafka_key
Remote host: localhost
Remote port: 9092
```

**Usage in your code:**
```java
bootstrap.servers=10.10.199.150:9092
```

### 3. Multiple Services

Configure multiple tunnels in one session:
- MySQL on port 3306
- PostgreSQL on port 5432
- Redis on port 6379
- Kafka on port 9092

All accessible through the same container IP!

## Management Commands

After installation, manage your tunnel gateway:

```bash
# List active tunnels
sudo lxc exec my-dev-tunnels -- /usr/local/bin/list-tunnels.sh

# Stop all tunnels
sudo lxc exec my-dev-tunnels -- /usr/local/bin/stop-tunnels.sh

# Start tunnels
sudo lxc exec my-dev-tunnels -- /usr/local/bin/start-tunnels.sh

# Shell access
sudo lxc exec my-dev-tunnels -- bash

# Container management
sudo lxc stop my-dev-tunnels
sudo lxc start my-dev-tunnels
sudo lxc delete my-dev-tunnels --force
```

## Benefits

✅ **No Firewall Changes** - Remote servers see connections from localhost  
✅ **No IP Whitelisting** - Works from any network via SSH  
✅ **Secure Encrypted** - All traffic encrypted through SSH  
✅ **Multiple Services** - One container, many tunnels  
✅ **Auto-Reconnecting** - Tunnels restart on connection loss  
✅ **Lightweight** - Alpine Linux base (~11 MB)  
✅ **Easy Setup** - Automated installation script  

## Troubleshooting

### LXD Installation Issues

If snap install fails:
```bash
sudo apt update
sudo apt install snapd
sudo snap install lxd
```

### Bridge Already Exists

If you get "bridge already exists":
- The script will detect and use existing bridge
- You can keep your current network configuration

### Image Import Fails

Ensure you have the correct tar.gz file:
```bash
ls -lh tunnel-gateway-v*.tar.gz
```

### Tunnel Connection Failed

Check SSH connectivity manually:
```bash
ssh username@remote-host
```

Verify tunnel is running:
```bash
sudo lxc exec container-name -- ps aux | grep ssh
```

### Cannot Connect to Container IP

Verify container is running:
```bash
sudo lxc list
```

Test network connectivity:
```bash
ping 10.10.199.150
```

## Security Notes

⚠️ **Development Use Only**

This tool is designed for development environments:
- Passwords stored in plaintext in config
- Auto-accepts SSH host keys
- No certificate validation

For production:
- Use SSH key authentication only
- Enable StrictHostKeyChecking
- Store credentials in encrypted vault
- Consider VPN instead of SSH tunnels

## Support

For issues or questions:
- Check tunnel-gateway documentation
- Review container logs: `sudo lxc exec container-name -- cat /var/log/tunnel-gateway/startup.log`
- Verify SSH connectivity to remote hosts
