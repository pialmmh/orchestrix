# Quarkus Runtime Container Scaffold - Summary

## What Was Created

A **generic LXC container template** for running any Quarkus application with automatic log shipping to Loki.

### Location
```
/home/mustafa/telcobright-projects/orchestrix-lxc-work/images/lxc/quarkus-runtime/
```

### Architecture
- **Single app per container** (following your convention)
- **Single config file** for build and launch
- **Java 21** runtime
- **Debian 12** base image
- **Promtail** for automatic log shipping to grafana-loki-v1
- **BTRFS storage** (no quota enforcement as requested)
- **Network:** lxdbr0 (10.10.199.x/24)

---

## File Structure

```
quarkus-runtime/
├── build/
│   ├── build.conf                        # Main config (EDIT THIS)
│   ├── build.conf.infinite-scheduler     # Preset for scheduler
│   ├── build.conf.routesphere            # Preset for routesphere
│   └── build.sh                          # Build script ✅
├── templates/
│   └── launch.conf                       # Launch template
├── configs/                              # Your launch configs (empty)
├── app/                                  # Placeholder (not used)
├── scripts/                              # Placeholder (not used)
├── launch.sh                             # Launch script ✅
├── startDefault.sh                       # Quick start script ✅
├── README.md                             # Full documentation
└── SCAFFOLD_SUMMARY.md                   # This file
```

---

## Quick Start Guide

### For infinite-scheduler

**Step 1: Build the scheduler JAR**
```bash
cd /home/mustafa/telcobright-projects/routesphere/infinite-scheduler
mvn clean package
```

**Step 2: Use preset config**
```bash
cd /home/mustafa/telcobright-projects/orchestrix-lxc-work/images/lxc/quarkus-runtime
cp build/build.conf.infinite-scheduler build/build.conf
```

**Step 3: Verify JAR path in build.conf**
```bash
nano build/build.conf
# Check APP_JAR_PATH points to your built JAR
```

**Step 4: Build and launch**
```bash
./startDefault.sh
```

This will:
1. Build the container image
2. Create test launch config
3. Launch `infinite-scheduler-test-1` instance
4. Show you access URLs

---

### For routesphere

**Step 1: Build the routesphere JAR**
```bash
cd /home/mustafa/telcobright-projects/routesphere
mvn clean package
```

**Step 2: Use preset config**
```bash
cd /home/mustafa/telcobright-projects/orchestrix-lxc-work/images/lxc/quarkus-runtime
cp build/build.conf.routesphere build/build.conf
```

**Step 3: Verify JAR path in build.conf**
```bash
nano build/build.conf
# Check APP_JAR_PATH points to your built JAR
```

**Step 4: Build and launch**
```bash
./startDefault.sh
```

---

## Manual Build & Launch (Advanced)

### Build Container

```bash
cd /home/mustafa/telcobright-projects/orchestrix-lxc-work/images/lxc/quarkus-runtime

# 1. Edit build config
nano build/build.conf

# 2. Set these values:
#    APP_NAME="your-app-name"
#    APP_JAR_PATH="/path/to/your-app.jar"
#    MEMORY_LIMIT="2GB"  (or as needed)
#    CPU_LIMIT="2"       (or as needed)
#    APP_PORT="8080"     (or as needed)

# 3. Run build
./build/build.sh

# Output: your-app-v1-TIMESTAMP.tar.gz in your-app-v.1/generated/artifact/
```

### Launch Instance

```bash
# 1. Create launch config
cp templates/launch.conf configs/my-app-prod.conf

# 2. Edit launch config
nano configs/my-app-prod.conf

# 3. Set these values:
#    INSTANCE_NAME="my-app-prod-1"
#    CONTAINER_IMAGE="your-app-v1-TIMESTAMP.tar.gz"  (use actual timestamp!)
#    LOKI_HOST="10.10.199.200"
#    STATIC_IP="10.10.199.50"  (optional)
#    MYSQL_ENABLED="true"       (if needed)

# 4. Launch
./launch.sh configs/my-app-prod.conf
```

---

## Resource Presets

### infinite-scheduler (Minimal)
- **Memory:** 1GB
- **CPU:** 2 cores
- **Disk:** 5GB
- **Port:** 8081
- **JVM:** `-Xms256m -Xmx768m`
- **Use case:** Lightweight job scheduler

### routesphere (300 TPS)
- **Memory:** 4GB
- **CPU:** 4 cores
- **Disk:** 20GB
- **Port:** 8082
- **JVM:** `-Xms1g -Xmx3g`
- **Use case:** High-throughput SMS processing

---

## Key Features

### ✅ Java 21
All containers use Java 21 (as requested)

### ✅ Automatic Log Shipping
- Logs automatically ship to Loki at `http://10.10.199.200:3100`
- JSON log parsing for trace_id, level, etc.
- View logs in Grafana at `http://10.10.199.200:3000`

### ✅ Systemd Service
- App runs as systemd service
- Auto-restart on failure
- Managed with `systemctl`

### ✅ Single Config File
- Build: Edit `build/build.conf`
- Launch: Edit your config in `configs/`
- Follows your convention

### ✅ No Quota Enforcement
- BTRFS storage used but no hard quotas
- As requested: "quota not required"

### ✅ Optional Services
- MySQL integration (for infinite-scheduler)
- Consul integration (if needed)
- Bind mounts for data volumes
- Environment variables

---

## Common Commands

### Container Management
```bash
# List all instances
lxc list

# Stop instance
lxc stop my-app-prod-1

# Start instance
lxc start my-app-prod-1

# Delete instance
lxc delete my-app-prod-1 --force
```

### Application Management
```bash
# View logs
lxc exec my-app-prod-1 -- journalctl -u my-app -f

# Restart app
lxc exec my-app-prod-1 -- systemctl restart my-app

# Check status
lxc exec my-app-prod-1 -- systemctl status my-app

# Access shell
lxc exec my-app-prod-1 -- bash
```

### Monitoring
```bash
# Container info
lxc info my-app-prod-1

# Resource usage
lxc exec my-app-prod-1 -- top

# Disk usage
lxc exec my-app-prod-1 -- df -h
```

---

## Integration with grafana-loki-v1

All logs automatically ship to:
- **Loki:** http://10.10.199.200:3100
- **Grafana:** http://10.10.199.200:3000

### View Logs in Grafana

1. Open Grafana: http://10.10.199.200:3000
2. Login: admin/admin
3. Go to Explore
4. Select Loki datasource
5. Query: `{job="infinite-scheduler"}` or `{job="routesphere"}`

### Filter Logs
```
{job="infinite-scheduler", level="ERROR"}
{job="routesphere"} |= "trace_id"
{job="infinite-scheduler"} | json | processing_time_ms > 100
```

---

## MySQL Integration (for infinite-scheduler)

The scheduler needs MySQL. Enable in launch config:

```bash
# In configs/scheduler-prod.conf
MYSQL_ENABLED="true"
MYSQL_HOST="10.10.199.171"  # MySQL container IP
MYSQL_PORT="3306"
MYSQL_DATABASE="scheduler"
MYSQL_USER="root"
MYSQL_PASSWORD="123456"
```

Environment variables will be automatically set in the container.

---

## Production Deployment Example

### Deploy infinite-scheduler to production

```bash
# 1. Build JAR
cd /home/mustafa/telcobright-projects/routesphere/infinite-scheduler
mvn clean package

# 2. Build container (one time)
cd /home/mustafa/telcobright-projects/orchestrix-lxc-work/images/lxc/quarkus-runtime
cp build/build.conf.infinite-scheduler build/build.conf
./build/build.sh

# 3. Create production config
cat > configs/scheduler-prod.conf << 'EOF'
INSTANCE_NAME="scheduler-prod-1"
CONTAINER_IMAGE="infinite-scheduler-v1-1730400000.tar.gz"  # Use actual timestamp
STATIC_IP="10.10.199.25"
HOST_PORT_APP="8081"
CONTAINER_PORT_APP="8081"
LOKI_HOST="10.10.199.200"
LOKI_PORT="3100"
MEMORY_LIMIT="1GB"
CPU_LIMIT="2"
MYSQL_ENABLED="true"
MYSQL_HOST="10.10.199.171"
MYSQL_DATABASE="scheduler"
ENV_VARS="SCHEDULER_FETCH_INTERVAL=25,SCHEDULER_LOOKAHEAD_WINDOW=30"
EOF

# 4. Launch
./launch.sh configs/scheduler-prod.conf

# 5. Verify
curl http://10.10.199.25:8081/health
lxc exec scheduler-prod-1 -- journalctl -u infinite-scheduler -f
```

---

## Deploy routesphere with load balancing

```bash
# 1. Build JAR
cd /home/mustafa/telcobright-projects/routesphere
mvn clean package

# 2. Build container (one time)
cd /home/mustafa/telcobright-projects/orchestrix-lxc-work/images/lxc/quarkus-runtime
cp build/build.conf.routesphere build/build.conf
./build/build.sh

# 3. Launch 3 instances for load balancing
for i in {1..3}; do
  cat > configs/routesphere-node-$i.conf << EOF
INSTANCE_NAME="routesphere-node-$i"
CONTAINER_IMAGE="routesphere-v1-TIMESTAMP.tar.gz"  # Update timestamp
STATIC_IP="10.10.199.$((49+i))"
HOST_PORT_APP="8082"
LOKI_HOST="10.10.199.200"
MEMORY_LIMIT="4GB"
CPU_LIMIT="4"
EOF

  ./launch.sh configs/routesphere-node-$i.conf
done

# Verify all instances
lxc list | grep routesphere
```

---

## Troubleshooting

### Build fails - JAR not found
```
ERROR: APP_JAR_PATH not set or JAR file not found
```
**Solution:** Build your Quarkus app first with `mvn clean package`

### Launch fails - Image not found
```
ERROR: Container image not found
```
**Solution:** Check timestamp in CONTAINER_IMAGE matches actual file:
```bash
ls -lh */generated/artifact/*.tar.gz
# Update launch.conf with correct filename
```

### App not starting
```bash
# Check logs
lxc exec <instance> -- journalctl -u <app-name> -n 100

# Common causes:
# - Port already in use
# - Missing database connection
# - Wrong JAR file
```

### Logs not in Grafana
```bash
# Check Promtail
lxc exec <instance> -- systemctl status promtail

# Verify Loki endpoint
lxc exec <instance> -- cat /etc/promtail/promtail-config.yaml | grep url

# Test connectivity
lxc exec <instance> -- curl http://10.10.199.200:3100/ready
```

---

## Next Steps

1. **Test with infinite-scheduler:**
   - Build the JAR
   - Run `./startDefault.sh`
   - Verify in Grafana

2. **Test with routesphere:**
   - Build the JAR
   - Switch config: `cp build/build.conf.routesphere build/build.conf`
   - Run `./startDefault.sh`

3. **Create production configs:**
   - Use `configs/` directory
   - Set static IPs
   - Enable MySQL if needed
   - Configure bind mounts

4. **Monitor and scale:**
   - Watch logs in Grafana
   - Monitor resource usage
   - Add more instances as needed

---

## Documentation

- **Full README:** `README.md` (comprehensive guide)
- **Build Config:** `build/build.conf` (main configuration)
- **Launch Template:** `templates/launch.conf` (instance configuration)
- **Logging Guide:** `/images/lxc/grafana-loki/APPLICATION_INTEGRATION_GUIDE.md`

---

## Support

For issues or questions, check:
1. README.md (this directory)
2. Container logs: `lxc exec <instance> -- journalctl -u <app>`
3. Build logs: Output from `./build/build.sh`

---

**Status:** ✅ Ready to use

**Created:** 2025-11-01

**Java Version:** 21

**Tested With:** N/A (awaiting first build)
