# Auto-Increment Service - Operational Guide

## Table of Contents
1. [Service Overview](#service-overview)
2. [Initial Deployment](#initial-deployment)
3. [Day-to-Day Operations](#day-to-day-operations)
4. [Monitoring & Health Checks](#monitoring--health-checks)
5. [Backup & Recovery](#backup--recovery)
6. [Scaling & High Availability](#scaling--high-availability)
7. [Troubleshooting Guide](#troubleshooting-guide)
8. [Performance Tuning](#performance-tuning)
9. [Security Considerations](#security-considerations)
10. [Disaster Recovery](#disaster-recovery)

---

## Service Overview

The Auto-Increment Service provides distributed unique ID generation for microservices architectures. Each container maintains its own state and can be deployed independently.

### Key Characteristics
- **Stateful Service**: Each instance maintains persistent state
- **No External Dependencies**: No database or cache required
- **Portable State**: Container state moves with the container
- **Lightweight**: ~50MB memory footprint
- **Fast**: <10ms response time for ID generation

---

## Initial Deployment

### Prerequisites
```bash
# Verify LXD installation
lxc version

# Check network bridge
lxc network list

# Ensure sufficient resources
free -h
df -h
```

### First-Time Setup

#### Step 1: Build Base Image
```bash
cd /home/mustafa/telcobright-projects/orchestrix/images/lxc/auto-increment-service
cd build/
./build.sh
```

Expected output:
```
✓ LXC/LXD is installed
✓ Build container ready
✓ Dependencies installed
✓ Service configured
✓ Container optimized
✓ Base image created: auto-increment-service-base
```

#### Step 2: Deploy First Instance
```bash
cd ..
./launchAutoIncrement.sh auto-increment-service-v.1.0.0/generated/sample.conf
```

#### Step 3: Verify Deployment
```bash
# Check container status
lxc list auto-increment-1

# Verify service is running
lxc exec auto-increment-1 -- systemctl status auto-increment

# Test API endpoint
CONTAINER_IP=$(lxc list auto-increment-1 -c 4 --format csv | grep -oE '[0-9]+\.[0-9]+\.[0-9]+\.[0-9]+')
curl "http://${CONTAINER_IP}:7001/health"
```

---

## Day-to-Day Operations

### Starting & Stopping

```bash
# Start container
lxc start auto-increment-1

# Stop gracefully
lxc stop auto-increment-1

# Force stop (if hung)
lxc stop auto-increment-1 --force

# Restart container
lxc restart auto-increment-1

# Restart just the service
lxc exec auto-increment-1 -- systemctl restart auto-increment
```

### Viewing Logs

```bash
# Live service logs
lxc exec auto-increment-1 -- tail -f /var/log/auto-increment.log

# System logs
lxc exec auto-increment-1 -- journalctl -u auto-increment -f

# Last 100 lines
lxc exec auto-increment-1 -- journalctl -u auto-increment -n 100

# Logs since specific time
lxc exec auto-increment-1 -- journalctl -u auto-increment --since "2024-01-01 00:00:00"
```

### Managing State

```bash
# View current state
lxc exec auto-increment-1 -- cat /var/lib/auto-increment/state.json | python3 -m json.tool

# Check specific entity
curl "http://${CONTAINER_IP}:7001/api/status/users"

# List all entities
curl "http://${CONTAINER_IP}:7001/api/list"

# Reset all data (CAUTION!)
lxc exec auto-increment-1 -- reset-auto-increment
# OR via API
curl -X DELETE "http://${CONTAINER_IP}:7001/api/reset"
```

### Common Operations

```bash
# Access container shell
lxc exec auto-increment-1 -- bash

# Copy files to container
lxc file push local-file.txt auto-increment-1/tmp/

# Copy files from container
lxc file pull auto-increment-1/var/lib/auto-increment/state.json ./backup-state.json

# View container info
lxc info auto-increment-1

# View container config
lxc config show auto-increment-1
```

---

## Monitoring & Health Checks

### Health Check Script

Create `monitor.sh`:
```bash
#!/bin/bash
CONTAINER_NAME="auto-increment-1"
CONTAINER_IP=$(lxc list $CONTAINER_NAME -c 4 --format csv | grep -oE '[0-9]+\.[0-9]+\.[0-9]+\.[0-9]+')

# Check container is running
if ! lxc info $CONTAINER_NAME | grep -q "Status: Running"; then
    echo "ERROR: Container not running"
    exit 1
fi

# Check service health
HEALTH=$(curl -s "http://${CONTAINER_IP}:7001/health")
if [ $? -ne 0 ]; then
    echo "ERROR: Service not responding"
    exit 1
fi

echo "OK: Service healthy"
echo "$HEALTH" | python3 -m json.tool
```

### Metrics Collection

```bash
# Memory usage
lxc exec auto-increment-1 -- free -m

# Disk usage
lxc exec auto-increment-1 -- df -h /var/lib/auto-increment

# Service uptime
curl -s "http://${CONTAINER_IP}:7001/health" | jq .uptime

# Process info
lxc exec auto-increment-1 -- ps aux | grep node

# Network connections
lxc exec auto-increment-1 -- netstat -tulpn | grep 7001
```

### Automated Monitoring

Add to crontab:
```bash
# Check every 5 minutes
*/5 * * * * /path/to/monitor.sh || echo "Auto-increment service down" | mail -s "Alert" admin@example.com
```

---

## Backup & Recovery

### Manual Backup

```bash
# Full container backup (includes state)
lxc export auto-increment-1 auto-increment-$(date +%Y%m%d-%H%M%S).tar.gz

# State-only backup
lxc file pull auto-increment-1/var/lib/auto-increment/state.json \
    ./backups/state-$(date +%Y%m%d-%H%M%S).json
```

### Automated Backup Script

Create `backup.sh`:
```bash
#!/bin/bash
BACKUP_DIR="/backup/auto-increment"
CONTAINER_NAME="auto-increment-1"
DATE=$(date +%Y%m%d-%H%M%S)

mkdir -p $BACKUP_DIR

# Export full container
lxc export $CONTAINER_NAME "$BACKUP_DIR/container-$DATE.tar.gz"

# Keep only last 7 days
find $BACKUP_DIR -name "container-*.tar.gz" -mtime +7 -delete

echo "Backup completed: $BACKUP_DIR/container-$DATE.tar.gz"
```

Add to crontab:
```bash
# Daily backup at 2 AM
0 2 * * * /path/to/backup.sh
```

### Recovery Procedures

#### From Full Backup
```bash
# Stop and delete existing container
lxc stop auto-increment-1 --force
lxc delete auto-increment-1

# Import from backup
lxc import auto-increment-backup.tar.gz

# Start container
lxc start auto-increment-1
```

#### State-Only Recovery
```bash
# Copy state file back
lxc file push state-backup.json auto-increment-1/var/lib/auto-increment/state.json

# Fix permissions
lxc exec auto-increment-1 -- chown nobody:nogroup /var/lib/auto-increment/state.json

# Restart service
lxc exec auto-increment-1 -- systemctl restart auto-increment
```

---

## Scaling & High Availability

### Multiple Instances for Different Services

Deploy separate instances for different microservices:
```bash
# Instance for user service
cat > user-service.conf << EOF
CONTAINER_NAME="auto-increment-users"
STATIC_IP="10.10.199.101"
SERVICE_PORT="7001"
EOF
./launchAutoIncrement.sh user-service.conf

# Instance for order service
cat > order-service.conf << EOF
CONTAINER_NAME="auto-increment-orders"
STATIC_IP="10.10.199.102"
SERVICE_PORT="7001"
EOF
./launchAutoIncrement.sh order-service.conf
```

### Load Balancing Setup

Using HAProxy:
```nginx
backend auto_increment_backend
    balance roundrobin
    server instance1 10.10.199.101:7001 check
    server instance2 10.10.199.102:7001 check
    server instance3 10.10.199.103:7001 check
```

### Partitioned ID Ranges

For true HA, partition ID ranges per instance:
```bash
# Instance 1: IDs 1-1000000
# Instance 2: IDs 1000001-2000000
# Instance 3: IDs 2000001-3000000
```

---

## Troubleshooting Guide

### Service Won't Start

```bash
# Check logs
lxc exec auto-increment-1 -- journalctl -u auto-increment -n 50

# Common issues:
# 1. Port already in use
lxc exec auto-increment-1 -- netstat -tulpn | grep 7001

# 2. Permission issues
lxc exec auto-increment-1 -- ls -la /var/lib/auto-increment/
lxc exec auto-increment-1 -- ls -la /opt/auto-increment/

# 3. Node.js issues
lxc exec auto-increment-1 -- node --version
lxc exec auto-increment-1 -- which node

# Fix permissions
lxc exec auto-increment-1 -- bash -c "
    chown -R nobody:nogroup /var/lib/auto-increment
    chown -R nobody:nogroup /opt/auto-increment
    chmod 755 /var/lib/auto-increment
"

# Restart service
lxc exec auto-increment-1 -- systemctl restart auto-increment
```

### API Not Responding

```bash
# Check if service is running
lxc exec auto-increment-1 -- systemctl status auto-increment

# Check if port is listening
lxc exec auto-increment-1 -- netstat -tulpn | grep 7001

# Test from inside container
lxc exec auto-increment-1 -- curl localhost:7001/health

# Check firewall rules
lxc exec auto-increment-1 -- iptables -L -n

# Check container network
lxc config device show auto-increment-1
```

### State Corruption

```bash
# Backup corrupted state
lxc file pull auto-increment-1/var/lib/auto-increment/state.json ./corrupted-state.json

# Reset state
lxc exec auto-increment-1 -- rm /var/lib/auto-increment/state.json
lxc exec auto-increment-1 -- systemctl restart auto-increment

# OR restore from backup
lxc file push last-good-state.json auto-increment-1/var/lib/auto-increment/state.json
lxc exec auto-increment-1 -- chown nobody:nogroup /var/lib/auto-increment/state.json
lxc exec auto-increment-1 -- systemctl restart auto-increment
```

### Memory Issues

```bash
# Check memory usage
lxc exec auto-increment-1 -- free -m
lxc exec auto-increment-1 -- ps aux --sort=-%mem | head

# Increase memory limit
lxc config set auto-increment-1 limits.memory 512MB

# Restart container
lxc restart auto-increment-1
```

---

## Performance Tuning

### Container Resources

```bash
# Set CPU limits
lxc config set auto-increment-1 limits.cpu 2

# Set memory limits
lxc config set auto-increment-1 limits.memory 256MB

# Set disk I/O limits
lxc config set auto-increment-1 limits.disk.priority 5
```

### Node.js Optimization

```bash
# Increase Node.js memory
lxc exec auto-increment-1 -- bash -c "
    sed -i 's|ExecStart=/usr/bin/node|ExecStart=/usr/bin/node --max-old-space-size=128|' \
        /etc/systemd/system/auto-increment.service
    systemctl daemon-reload
    systemctl restart auto-increment
"
```

### Network Optimization

```bash
# Enable TCP fast open
lxc exec auto-increment-1 -- sysctl -w net.ipv4.tcp_fastopen=3

# Increase connection backlog
lxc exec auto-increment-1 -- sysctl -w net.core.somaxconn=1024
```

---

## Security Considerations

### Network Security

```bash
# Restrict access to specific IPs
lxc exec auto-increment-1 -- bash -c "
    iptables -A INPUT -p tcp --dport 7001 -s 10.10.199.0/24 -j ACCEPT
    iptables -A INPUT -p tcp --dport 7001 -j DROP
"
```

### API Authentication (Reverse Proxy)

Deploy NGINX as reverse proxy with basic auth:
```nginx
server {
    listen 443 ssl;

    location / {
        auth_basic "Auto-Increment Service";
        auth_basic_user_file /etc/nginx/.htpasswd;

        proxy_pass http://10.10.199.101:7001;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
    }
}
```

### Regular Updates

```bash
# Update container packages
lxc exec auto-increment-1 -- apt update
lxc exec auto-increment-1 -- apt upgrade -y

# Update Node.js
lxc exec auto-increment-1 -- npm update
```

---

## Disaster Recovery

### Complete System Failure

1. **Prepare new host**:
```bash
# Install LXD
sudo snap install lxd
sudo lxd init
```

2. **Restore from backup**:
```bash
# Copy backup to new host
scp auto-increment-backup.tar.gz newhost:/tmp/

# On new host
lxc import /tmp/auto-increment-backup.tar.gz
lxc start auto-increment-1
```

3. **Verify service**:
```bash
# Check health
CONTAINER_IP=$(lxc list auto-increment-1 -c 4 --format csv | grep -oE '[0-9]+\.[0-9]+\.[0-9]+\.[0-9]+')
curl "http://${CONTAINER_IP}:7001/health"

# Verify state
curl "http://${CONTAINER_IP}:7001/api/list"
```

### Rollback Procedure

```bash
# Keep track of container snapshots
lxc snapshot auto-increment-1 before-update

# If update fails, rollback
lxc restore auto-increment-1 before-update
```

### Emergency Maintenance Mode

```bash
# Stop service but keep container running
lxc exec auto-increment-1 -- systemctl stop auto-increment

# Perform maintenance
lxc exec auto-increment-1 -- bash
# ... fix issues ...

# Restart service
lxc exec auto-increment-1 -- systemctl start auto-increment
```

---

## Appendix: Quick Reference

### Common Commands
```bash
# Status check
lxc list | grep auto-increment
lxc exec auto-increment-1 -- systemctl status auto-increment

# Logs
lxc exec auto-increment-1 -- tail -f /var/log/auto-increment.log

# API test
curl "http://10.10.199.101:7001/api/next-id/test?dataType=int"

# Backup
lxc export auto-increment-1 backup.tar.gz

# Restore
lxc import backup.tar.gz

# Reset
lxc exec auto-increment-1 -- reset-auto-increment
```

### File Locations
```
Container files:
/opt/auto-increment/         - Application directory
/var/lib/auto-increment/     - State directory
/var/log/auto-increment.log  - Service logs
/etc/systemd/system/         - Service definition

Host files:
./build/                     - Build scripts
./scripts/                   - Service source code
./auto-increment-service-v.X.X.X/  - Generated configs
```

### Support & Maintenance

For issues or updates, check:
1. Service logs: `/var/log/auto-increment.log`
2. System logs: `journalctl -u auto-increment`
3. Container status: `lxc info auto-increment-1`
4. Network connectivity: Test from another container
5. State integrity: Verify JSON structure in state.json