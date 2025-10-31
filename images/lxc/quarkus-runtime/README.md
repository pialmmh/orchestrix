# Quarkus Runtime Container

Generic LXC container template for running any Quarkus application with automatic log shipping to Loki.

## Overview

This template creates isolated Debian 12 containers for Quarkus applications following the **single-config-file convention**. Each app gets its own container with:

- ✅ Java 21 runtime
- ✅ Systemd service management
- ✅ Promtail for automatic log shipping to Loki
- ✅ Configurable resources (CPU, memory, storage)
- ✅ JSON logging support
- ✅ Network isolation on lxdbr0 (10.10.199.x/24)
- ✅ Optional MySQL, Consul integration

## Quick Start

### 1. Build Your Quarkus Application

```bash
cd /path/to/your/quarkus-app
mvn clean package
```

### 2. Configure the Container

Edit `build/build.conf`:

```bash
# Application Identity
APP_NAME="infinite-scheduler"
APP_JAR_PATH="/home/mustafa/telcobright-projects/routesphere/infinite-scheduler/target/infinite-scheduler-1.0.0.jar"

# Resources (adjust for your app)
MEMORY_LIMIT="1GB"   # For minimal apps like infinite-scheduler
CPU_LIMIT="2"
STORAGE_QUOTA_SIZE="5G"
APP_PORT="8081"

# Logging
LOKI_HOST="10.10.199.200"  # grafana-loki-v1
LOKI_PORT="3100"
```

### 3. Build the Container

```bash
cd /home/mustafa/telcobright-projects/orchestrix-lxc-work/images/lxc/quarkus-runtime
./build/build.sh
```

### 4. Launch an Instance

Edit `templates/launch.conf` or create your own config file:

```bash
INSTANCE_NAME="scheduler-prod-1"
CONTAINER_IMAGE="infinite-scheduler-v1-1730400000.tar.gz"  # Use actual timestamp
LOKI_HOST="10.10.199.200"
```

Launch:
```bash
./launch.sh templates/launch.conf
# Or with custom config:
./launch.sh /path/to/my-custom-config.conf
```

## Configuration Reference

### Build Configuration (`build/build.conf`)

**Required Settings:**

| Parameter | Description | Example |
|-----------|-------------|---------|
| `APP_NAME` | Application name | `"infinite-scheduler"` |
| `APP_JAR_PATH` | Path to built JAR | `"/path/to/app.jar"` |
| `CONTAINER_VERSION` | Version number | `"1"` |

**Resource Settings:**

| Parameter | Description | Default |
|-----------|-------------|---------|
| `MEMORY_LIMIT` | RAM limit | `"2GB"` |
| `CPU_LIMIT` | CPU cores | `"2"` |
| `STORAGE_QUOTA_SIZE` | Disk space | `"10G"` |
| `APP_PORT` | Application port | `"8080"` |

**Java Settings:**

| Parameter | Description | Default |
|-----------|-------------|---------|
| `JAVA_VERSION` | Java version | `"21"` |
| `JVM_OPTS` | JVM arguments | `"-Xms256m -Xmx1g -XX:+UseG1GC"` |

**Logging Settings:**

| Parameter | Description | Default |
|-----------|-------------|---------|
| `LOKI_HOST` | Loki server IP | `"10.10.199.200"` |
| `LOKI_PORT` | Loki port | `"3100"` |
| `LOG_LEVEL` | Log level | `"INFO"` |
| `LOG_FORMAT` | Log format | `"json"` |

### Launch Configuration (`templates/launch.conf`)

**Instance Settings:**

| Parameter | Description | Example |
|-----------|-------------|---------|
| `INSTANCE_NAME` | Unique instance name | `"scheduler-prod-1"` |
| `CONTAINER_IMAGE` | Image filename | `"app-v1-1730400000.tar.gz"` |
| `STATIC_IP` | Fixed IP (optional) | `"10.10.199.50"` |

**Runtime Overrides:**

| Parameter | Description | Example |
|-----------|-------------|---------|
| `MEMORY_LIMIT` | Override build limit | `"4GB"` |
| `HOST_PORT_APP` | Host port mapping | `"8080"` |
| `ENV_VARS` | Environment variables | `"DB_HOST=10.10.199.171,DB_PORT=3306"` |
| `MOUNT_BINDS` | Bind mounts | `"/data:/opt/app/data"` |

## Preset Configurations

### infinite-scheduler (Minimal Scheduler)

```bash
# build/build.conf
APP_NAME="infinite-scheduler"
APP_JAR_PATH="/home/mustafa/telcobright-projects/routesphere/infinite-scheduler/target/infinite-scheduler-1.0.0.jar"
MEMORY_LIMIT="1GB"
CPU_LIMIT="2"
STORAGE_QUOTA_SIZE="5G"
APP_PORT="8081"
JVM_OPTS="-Xms256m -Xmx768m -XX:+UseG1GC"
```

**Use case:** Lightweight job scheduler
**Resources:** 1GB RAM, 2 CPU, 5GB disk
**Expected load:** Minimal background processing

### routesphere (300 TPS SMS Processing)

```bash
# build/build.conf
APP_NAME="routesphere"
APP_JAR_PATH="/home/mustafa/telcobright-projects/routesphere/target/routesphere-1.0.0.jar"
MEMORY_LIMIT="4GB"
CPU_LIMIT="4"
STORAGE_QUOTA_SIZE="20G"
APP_PORT="8082"
JVM_OPTS="-Xms1g -Xmx3g -XX:+UseG1GC -XX:MaxGCPauseMillis=200"
```

**Use case:** High-throughput SMS processing
**Resources:** 4GB RAM, 4 CPU, 20GB disk
**Expected load:** 300 transactions per second

## Complete Examples

### Example 1: Build and Launch infinite-scheduler

```bash
# Step 1: Build infinite-scheduler JAR
cd /home/mustafa/telcobright-projects/routesphere/infinite-scheduler
mvn clean package

# Step 2: Configure container build
cd /home/mustafa/telcobright-projects/orchestrix-lxc-work/images/lxc/quarkus-runtime
nano build/build.conf

# Set:
APP_NAME="infinite-scheduler"
APP_JAR_PATH="/home/mustafa/telcobright-projects/routesphere/infinite-scheduler/target/infinite-scheduler-1.0.0.jar"
MEMORY_LIMIT="1GB"
CPU_LIMIT="2"
STORAGE_QUOTA_SIZE="5G"
APP_PORT="8081"

# Step 3: Build container
./build/build.sh

# Output will show:
# Container: infinite-scheduler-v1
# Exported Image: infinite-scheduler-v1-1730400000.tar.gz

# Step 4: Create launch config
cp templates/launch.conf configs/scheduler-prod.conf
nano configs/scheduler-prod.conf

# Set:
INSTANCE_NAME="scheduler-prod-1"
CONTAINER_IMAGE="infinite-scheduler-v1-1730400000.tar.gz"  # Use actual timestamp
LOKI_HOST="10.10.199.200"
MYSQL_ENABLED="true"
MYSQL_HOST="10.10.199.171"
MYSQL_DATABASE="scheduler"

# Step 5: Launch instance
./launch.sh configs/scheduler-prod.conf

# Step 6: Verify
lxc list scheduler-prod-1
curl http://<container-ip>:8081/health
```

### Example 2: Build and Launch routesphere

```bash
# Step 1: Build routesphere JAR
cd /home/mustafa/telcobright-projects/routesphere
mvn clean package

# Step 2: Configure container build
cd /home/mustafa/telcobright-projects/orchestrix-lxc-work/images/lxc/quarkus-runtime
nano build/build.conf

# Set:
APP_NAME="routesphere"
APP_JAR_PATH="/home/mustafa/telcobright-projects/routesphere/target/routesphere-1.0.0.jar"
MEMORY_LIMIT="4GB"
CPU_LIMIT="4"
STORAGE_QUOTA_SIZE="20G"
APP_PORT="8082"

# Step 3: Build container
./build/build.sh

# Step 4: Launch multiple instances (for load balancing)
# Instance 1
cp templates/launch.conf configs/routesphere-node-1.conf
nano configs/routesphere-node-1.conf
# Set: INSTANCE_NAME="routesphere-node-1", STATIC_IP="10.10.199.50"
./launch.sh configs/routesphere-node-1.conf

# Instance 2
cp templates/launch.conf configs/routesphere-node-2.conf
nano configs/routesphere-node-2.conf
# Set: INSTANCE_NAME="routesphere-node-2", STATIC_IP="10.10.199.51"
./launch.sh configs/routesphere-node-2.conf
```

## Directory Structure

```
quarkus-runtime/
├── build/
│   ├── build.conf              # Build configuration (edit this)
│   └── build.sh                # Build script
├── templates/
│   └── launch.conf             # Launch template
├── configs/                    # Your custom launch configs (create this)
│   ├── scheduler-prod.conf
│   └── routesphere-node-1.conf
├── infinite-scheduler-v.1/     # Generated after build
│   └── generated/
│       └── artifact/
│           └── infinite-scheduler-v1-TIMESTAMP.tar.gz
├── launch.sh                   # Launch script
└── README.md                   # This file
```

## Networking

### IP Address Assignment

- **Network:** lxdbr0 (10.10.199.0/24)
- **Gateway:** 10.10.199.1
- **DHCP Range:** Automatically assigned
- **Static IP:** Configure in launch.conf

### Port Mapping

Containers use host networking on lxdbr0. Access services directly via container IP:

```bash
# Application
http://<container-ip>:<APP_PORT>

# Promtail metrics
http://<container-ip>:<PROMTAIL_PORT>
```

### Available Container IPs

Check existing allocations:
```bash
lxc list -c n4
```

Reserve static IPs in launch config to avoid conflicts.

## Logging

### Automatic Log Shipping

All logs automatically ship to Loki via Promtail:

1. **Application logs** → Systemd journal → Promtail → Loki
2. **JSON logs** → Parsed for trace_id, level, etc.
3. **Viewable in Grafana** at http://10.10.199.200:3000

### Log Query Examples

**View all logs for an app:**
```
{job="infinite-scheduler"}
```

**Filter by log level:**
```
{job="infinite-scheduler", level="ERROR"}
```

**Filter by trace ID:**
```
{job="infinite-scheduler"} |= "trace_id=\"550e8400-e29b-41d4...\""
```

### Manual Log Access

```bash
# View live application logs
lxc exec <instance-name> -- journalctl -u <app-name> -f

# View Promtail logs
lxc exec <instance-name> -- journalctl -u promtail -f

# Check log files
lxc exec <instance-name> -- ls -lh /var/log/<app-name>/
```

## Database Integration

### MySQL Configuration

For apps that need MySQL (like infinite-scheduler):

```bash
# In launch.conf
MYSQL_ENABLED="true"
MYSQL_HOST="10.10.199.171"      # MySQL container IP
MYSQL_PORT="3306"
MYSQL_DATABASE="scheduler"
MYSQL_USER="root"
MYSQL_PASSWORD="123456"
```

Environment variables automatically set:
- `MYSQL_HOST`
- `MYSQL_PORT`
- `MYSQL_DATABASE`
- `MYSQL_USER`
- `MYSQL_PASSWORD`

### Database Connection in Quarkus

```properties
# application.properties
quarkus.datasource.jdbc.url=jdbc:mysql://${MYSQL_HOST}:${MYSQL_PORT}/${MYSQL_DATABASE}
quarkus.datasource.username=${MYSQL_USER}
quarkus.datasource.password=${MYSQL_PASSWORD}
```

## Operations

### Container Lifecycle

```bash
# List all instances
lxc list

# Stop instance
lxc stop <instance-name>

# Start instance
lxc start <instance-name>

# Restart instance
lxc restart <instance-name>

# Delete instance
lxc stop <instance-name>
lxc delete <instance-name>
```

### Service Management

```bash
# Check service status
lxc exec <instance-name> -- systemctl status <app-name>

# Restart application
lxc exec <instance-name> -- systemctl restart <app-name>

# View service logs
lxc exec <instance-name> -- journalctl -u <app-name> -n 100
```

### Resource Monitoring

```bash
# Container resource usage
lxc info <instance-name>

# Live resource monitoring
lxc exec <instance-name> -- top

# Memory usage
lxc exec <instance-name> -- free -h

# Disk usage
lxc exec <instance-name> -- df -h
```

## Troubleshooting

### Build Issues

**Problem:** JAR file not found

```
ERROR: APP_JAR_PATH not set or JAR file not found
```

**Solution:**
1. Build your Quarkus app first: `mvn clean package`
2. Set correct path in build.conf
3. Verify file exists: `ls -lh /path/to/your/app.jar`

**Problem:** Java version mismatch

```
ERROR: Java 21 not available
```

**Solution:**
Container installs Java 21 automatically. If it fails, check internet connectivity:
```bash
lxc exec <container-name> -- ping -c 3 debian.org
```

### Launch Issues

**Problem:** Image not found

```
ERROR: Container image not found: app-v1-TIMESTAMP.tar.gz
```

**Solution:**
1. Check available images: `ls -lh */generated/artifact/*.tar.gz`
2. Update CONTAINER_IMAGE in launch.conf with actual filename
3. Use tab completion for timestamp

**Problem:** Instance already exists

```
ERROR: Instance scheduler-prod-1 already exists
```

**Solution:**
```bash
# Option 1: Delete existing
lxc stop scheduler-prod-1
lxc delete scheduler-prod-1
./launch.sh configs/scheduler-prod.conf

# Option 2: Use different name
# Edit launch.conf: INSTANCE_NAME="scheduler-prod-2"
```

### Runtime Issues

**Problem:** Application not starting

```bash
# Check service status
lxc exec <instance-name> -- systemctl status <app-name>

# View detailed logs
lxc exec <instance-name> -- journalctl -u <app-name> -n 100 --no-pager

# Common causes:
# 1. Port already in use
# 2. Missing database connection
# 3. Configuration error
```

**Problem:** Logs not appearing in Grafana

```bash
# Check Promtail status
lxc exec <instance-name> -- systemctl status promtail

# Verify Loki endpoint
lxc exec <instance-name> -- cat /etc/promtail/promtail-config.yaml | grep url

# Test Loki connectivity
lxc exec <instance-name> -- curl http://10.10.199.200:3100/ready
```

**Problem:** Cannot connect to database

```bash
# Verify MySQL container is running
lxc list mysql

# Test connectivity from app container
lxc exec <instance-name> -- nc -zv 10.10.199.171 3306

# Check MySQL credentials
lxc exec <instance-name> -- env | grep MYSQL
```

## Best Practices

1. **One app per container** - Don't run multiple apps in one container
2. **Use version control** - Keep launch configs in git
3. **Static IPs for prod** - Use STATIC_IP in launch.conf for production instances
4. **Resource sizing** - Start small, monitor, then scale up
5. **Test in dev first** - Always test new builds in dev instance before prod
6. **Monitor logs** - Set up Grafana dashboards for your apps
7. **Backup configs** - Keep copies of working launch configs

## Advanced Usage

### Running Multiple Instances (Load Balancing)

```bash
# Launch 3 routesphere instances
for i in {1..3}; do
    cp templates/launch.conf configs/routesphere-node-$i.conf
    sed -i "s/INSTANCE_NAME=.*/INSTANCE_NAME=\"routesphere-node-$i\"/" configs/routesphere-node-$i.conf
    ./launch.sh configs/routesphere-node-$i.conf
done
```

### Custom Environment Variables

```bash
# In launch.conf
ENV_VARS="DB_POOL_SIZE=20,CACHE_TTL=300,API_KEY=abc123"
```

### Bind Mounting Configuration

```bash
# In launch.conf
MOUNT_BINDS="/data/app-config:/opt/app/config,/data/app-logs:/opt/app/logs"
```

## Resource Recommendations

| App Type | RAM | CPU | Disk | Example |
|----------|-----|-----|------|---------|
| Minimal scheduler | 1GB | 2 | 5GB | infinite-scheduler |
| Low-traffic API | 2GB | 2 | 10GB | Internal tools |
| Medium-traffic API | 4GB | 4 | 20GB | routesphere (300 TPS) |
| High-traffic API | 8GB+ | 8+ | 50GB+ | Payment gateway |

## Support & Documentation

- **Container Standards:** `/images/lxc/CONTAINER_SCAFFOLDING_STANDARD.md`
- **Logging Guide:** `/images/lxc/grafana-loki/APPLICATION_INTEGRATION_GUIDE.md`
- **LXC Documentation:** https://documentation.ubuntu.com/lxd/

## License

Internal TelcoBright project - all rights reserved.
