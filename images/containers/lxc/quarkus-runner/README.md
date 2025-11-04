# Quarkus Runner Base Container

## Overview

This container provides a complete Quarkus application runtime environment as a **base image**. App-specific containers extend this base by adding application JARs.

## Architecture

```
quarkus-runner-base (this container)
  ↓ (clone and add JAR)
myapp-v1.0.0
myapp-v2.0.0
otherapp-v1.0.0
```

## Included Components

### Runtime Environment
- **JVM**: OpenJDK 21 (configurable: OpenJDK, Temurin, GraalVM)
- **Maven**: 3.9.6
- **Quarkus**: 3.15.1
- **Debian**: 12 (Bookworm)

### Monitoring & Logging
- **Promtail**: 2.9.4 (ships logs to Grafana-Loki)
- **SmallRye Health**: Health check endpoints
- **Micrometer**: Metrics for Prometheus

### Storage
- **BTRFS**: Quota-managed storage with 80% auto-rotation
- **Default quota**: 15G (base), 5-10G (apps)
- **Compression**: Enabled
- **Snapshots**: Enabled

### Application Structure
```
/opt/quarkus/          # Application JARs
/var/lib/quarkus/      # Application data
/var/log/quarkus/      # Application logs (shipped to Loki)
/etc/quarkus/          # Configuration files
/run/secrets/          # Mounted secrets
```

## Quick Start

### 1. Build Base Container (One-time)

```bash
cd /home/mustafa/telcobright-projects/orchestrix/images/lxc/quarkus-runner

# Option A: Quick start with defaults
sudo ./startDefault.sh

# Option B: Custom configuration
sudo ./build/build.sh build/build.conf
```

**Build time**: ~5-10 minutes (downloads JVM, Maven, etc.)

**Result**: LXC image named `quarkus-runner-base`

### 2. Verify Base Container

```bash
# List images
lxc image list | grep quarkus-runner-base

# Should see: quarkus-runner-base
```

### 3. Build Your Quarkus App

```bash
cd /path/to/your-quarkus-app
mvn clean package -Pproduction

# JAR created at: target/myapp-runner.jar
```

### 4. Create App Container from Base

```bash
# Copy sample config
cp sample-config.conf configs/myapp-build.conf

# Edit configuration
vim configs/myapp-build.conf
# Set:
#   APP_NAME="myapp"
#   APP_VERSION="1.0.0"
#   APP_JAR_PATH="/path/to/target/myapp-runner.jar"
#   STORAGE_QUOTA_SIZE="5G"
#   MEMORY_LIMIT="1GB"

# Build app container
sudo ./build/build.sh configs/myapp-build.conf
```

**Build time**: ~30 seconds (clones base, adds JAR)

**Result**: Container `myapp-v1.0.0` ready to run

### 5. Start and Test App

```bash
# Start container
lxc start myapp-v1.0.0

# Check health
lxc exec myapp-v1.0.0 -- curl http://localhost:8080/q/health

# View logs (live)
lxc exec myapp-v1.0.0 -- tail -f /var/log/quarkus/application.log

# View logs in Grafana (pushed by Promtail)
# http://grafana:3000 (filter: app=myapp)

# Check metrics
lxc exec myapp-v1.0.0 -- curl http://localhost:9000/q/metrics
```

## Configuration

### Base Container Configuration

See `build/build.conf` for full options:

```bash
# JVM
JVM_DISTRIBUTION="openjdk"     # openjdk, graalvm, temurin
JVM_VERSION="21"

# Quarkus
QUARKUS_VERSION="3.15.1"

# Storage (MANDATORY)
STORAGE_LOCATION_ID="btrfs_local_main"
STORAGE_QUOTA_SIZE="15G"

# Resources
MEMORY_LIMIT="2GB"
CPU_LIMIT="2"
```

### App Container Configuration

See `sample-config.conf` for full options:

```bash
# Base
BASE_CONTAINER="quarkus-runner-base"

# App Identity
APP_NAME="myapp"
APP_VERSION="1.0.0"
APP_JAR_PATH="/path/to/myapp-runner.jar"

# Storage (per app)
STORAGE_QUOTA_SIZE="5G"

# Resources (per app)
MEMORY_LIMIT="1GB"
CPU_LIMIT="1"

# Logging
LOG_LABELS="app=myapp,team=backend"
```

## Logging to Grafana-Loki

Logs are automatically shipped to Grafana-Loki via Promtail.

### Quarkus Application Logging

Configure your `application.properties`:

```properties
# JSON structured logging (required for Loki parsing)
quarkus.log.console.json=true
quarkus.log.file.enable=true
quarkus.log.file.path=/var/log/quarkus/application.log
quarkus.log.file.rotation.max-file-size=100M
```

### View Logs in Grafana

1. Open Grafana: http://grafana-loki:3000
2. Query: `{app="myapp"}`
3. Filter by level: `{app="myapp"} |= "ERROR"`

## Health Checks & Monitoring

### Health Endpoints (Quarkus SmallRye Health)

```bash
# Liveness (is app running?)
curl http://localhost:8080/q/health/live

# Readiness (is app ready for traffic?)
curl http://localhost:8080/q/health/ready

# Full health check
curl http://localhost:8080/q/health
```

### Metrics (Micrometer/Prometheus)

```bash
# Prometheus metrics
curl http://localhost:9000/q/metrics

# Scrape from Prometheus
# Add to prometheus.yml:
#   - targets: ['myapp-v1.0.0:9000']
```

## Storage Management

### Check Storage Usage

```bash
# Container storage quota
lxc exec myapp-v1.0.0 -- df -h /

# BTRFS quota
sudo btrfs qgroup show /home/telcobright/btrfs
```

### Auto-Rotation at 80%

When storage reaches 80% of quota:
- Logs automatically rotated
- Oldest logs compressed/deleted
- Triggered by storage monitor (inherited from base)

## Deployment Workflow

### Development → Staging → Production

```bash
# 1. Build app JAR
mvn clean package -Pproduction

# 2. Build container
sudo ./build/build.sh configs/myapp-build.conf

# 3. Export (creates /tmp/myapp-v1.0.0-*.tar.gz)
lxc export myapp-v1.0.0 /tmp/myapp-v1.0.0.tar.gz

# 4. Deploy to server
scp /tmp/myapp-v1.0.0.tar.gz user@server:/tmp/
ssh user@server "lxc import /tmp/myapp-v1.0.0.tar.gz"
ssh user@server "lxc start myapp-v1.0.0"
```

## Versioning

### Multiple Versions Side-by-Side

```bash
# Build v1.0.0
APP_VERSION=1.0.0 ./build/build.sh configs/myapp-build.conf

# Build v1.0.1
APP_VERSION=1.0.1 ./build/build.sh configs/myapp-build.conf

# Run both
lxc start myapp-v1.0.0
lxc start myapp-v1.0.1

# Test v1.0.1
# If good: stop v1.0.0, promote v1.0.1
# If bad: rollback to v1.0.0
```

## Troubleshooting

### Base container not found

```bash
# List images
lxc image list

# If quarkus-runner-base missing, build it:
sudo ./startDefault.sh
```

### JAR not found

```bash
# Verify JAR exists
ls -lh /path/to/your-app/target/myapp-runner.jar

# Build JAR first
cd /path/to/your-app
mvn clean package
```

### Container won't start

```bash
# Check logs
lxc info myapp-v1.0.0 --show-log

# Check inside container
lxc exec myapp-v1.0.0 -- bash
systemctl status myapp.service
journalctl -u myapp.service -f
```

### Health check fails

```bash
# Check if app is running
lxc exec myapp-v1.0.0 -- ps aux | grep java

# Check application logs
lxc exec myapp-v1.0.0 -- cat /var/log/quarkus/application.log

# Check if port is listening
lxc exec myapp-v1.0.0 -- netstat -tlnp | grep 8080
```

## Java Automation

All builds use Java automation classes (not shell scripts):

- **QuarkusBaseContainerBuilder**: Builds base container
- **QuarkusAppContainerBuilder**: Builds app containers from base

This ensures:
- ✅ Reusable, testable code
- ✅ Type-safe configuration
- ✅ Better error handling
- ✅ Consistent with Orchestrix automation patterns

## Components Version Matrix

| Component | Version | Configurable |
|-----------|---------|--------------|
| Debian | 12 (Bookworm) | No |
| JVM | OpenJDK 21 | Yes (17, 21, GraalVM) |
| Maven | 3.9.6 | Yes |
| Quarkus | 3.15.1 | Yes |
| Promtail | 2.9.4 | Yes |
| Storage | BTRFS | No (mandatory) |

## Future Enhancements

- [ ] GraalVM native image support
- [ ] Multi-stage builds (separate build/runtime)
- [ ] Automatic version promotion
- [ ] Blue-green deployment automation
- [ ] Container health monitoring dashboard
- [ ] Automatic rollback on failed health checks

## Support

For issues or questions:
- Container scaffolding: `/home/mustafa/telcobright-projects/orchestrix/images/lxc/CONTAINER_SCAFFOLDING_STANDARD.md`
- Storage automation: `/home/mustafa/telcobright-projects/orchestrix/src/main/java/com/telcobright/orchestrix/automation/storage/README.md`
- Java automation: `src/main/java/com/telcobright/orchestrix/automation/containers/quarkus/`
