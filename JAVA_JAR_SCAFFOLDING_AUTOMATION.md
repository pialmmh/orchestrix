# Java JAR Scaffolding Automation with Promtail

## Overview

✅ **YES, we have comprehensive Java JAR scaffolding automation!**

The Quarkus Runner automation provides a complete system for scaffolding Java applications with varying Java versions and Promtail integration.

---

## Architecture

### Two-Phase Approach

1. **Base Container** - Built once with specific Java version + infrastructure
2. **App Container** - Clones base and adds your JAR file

```
┌─────────────────────────────────────────┐
│   Quarkus Base Container Builder        │
│   (QuarkusBaseContainerBuilder.java)    │
│                                          │
│   • Java Version (17, 21, 22, etc.)    │
│   • Maven                                │
│   • Quarkus CLI                          │
│   • Promtail (log shipping)             │
│   • Database drivers                     │
│   • Monitoring tools                     │
│   • Systemd templates                    │
└──────────────┬──────────────────────────┘
               │
               │ Publish as LXC image
               ▼
┌─────────────────────────────────────────┐
│   Quarkus App Container Builder         │
│   (QuarkusAppContainerBuilder.java)     │
│                                          │
│   • Clones base container                │
│   • Deploys your JAR file                │
│   • Configures systemd service           │
│   • Activates Promtail for app logs     │
│   • Sets resource limits                 │
└─────────────────────────────────────────┘
```

---

## Key Components

### 1. Base Container Builder

**Location**: `/home/mustafa/telcobright-projects/orchestrix/src/main/java/com/telcobright/orchestrix/automation/api/container/lxc/app/quarkus/example/QuarkusBaseContainerBuilder.java`

**What it does:**
- Creates reusable base container with Java + infrastructure
- Supports multiple Java versions (configurable)
- Includes Promtail for log shipping to Loki
- Pre-installs Maven, database drivers, monitoring tools

**Java Version Support:**
```java
private void installJvm() throws Exception {
    switch (jvmDistribution.toLowerCase()) {
        case "openjdk":
            device.executeCommand("lxc exec " + containerName +
                " -- apt-get install -y openjdk-" + jvmVersion + "-jdk");
            break;
        case "temurin":
            // Eclipse Adoptium Temurin
            installTemurin();
            break;
        case "graalvm":
            // GraalVM for native images
            installGraalVM();
            break;
    }
}
```

**Supported Java Versions:**
- ✅ OpenJDK: 8, 11, 17, 21, 22
- ✅ Temurin (Eclipse Adoptium): 11, 17, 21
- ✅ GraalVM: 17, 21

**Default: Java 21** (as specified in config)

### 2. App Container Builder

**Location**: `/home/mustafa/telcobright-projects/orchestrix/src/main/java/com/telcobright/orchestrix/automation/api/container/lxc/app/quarkus/example/QuarkusAppContainerBuilder.java`

**What it does:**
- Clones base container
- Deploys your JAR file
- Creates systemd service
- Configures Promtail to ship app logs
- Sets resource limits (CPU, memory)

---

## Configuration

### Base Container Config

**Location**: `/home/mustafa/telcobright-projects/orchestrix/images/containers/lxc/quarkus-runner/build/build.conf`

**Key Settings:**

```bash
# ============================================
# JVM Configuration
# ============================================
JVM_DISTRIBUTION="openjdk"     # openjdk, graalvm, temurin
JVM_VERSION="21"               # Default: 21
INSTALL_GRAALVM="false"
GRAALVM_VERSION="21.0.1"

# JVM Tools
INSTALL_MAVEN="true"
MAVEN_VERSION="3.9.6"

# ============================================
# Quarkus Configuration
# ============================================
QUARKUS_VERSION="3.15.1"
INSTALL_QUARKUS_CLI="true"

# ============================================
# Promtail (Log Shipping)
# ============================================
PROMTAIL_VERSION="2.9.4"
PROMTAIL_ENABLED="true"
PROMTAIL_SCRAPE_INTERVAL="5s"
LOKI_ENDPOINT="http://grafana-loki-v.1:3100"

# ============================================
# Logging Configuration
# ============================================
LOG_FORMAT="json"              # json for Loki parsing
LOG_FILE_PATH="/var/log/quarkus/application.log"
LOG_MAX_FILE_SIZE="100MB"
LOG_MAX_BACKUP_COUNT="20"
```

### App Container Config Example

```bash
# Base container to extend
BASE_CONTAINER="quarkus-runner-base"

# App details
APP_NAME="my-service"
APP_VERSION="1.0"
APP_JAR_PATH="/path/to/my-service-1.0.jar"

# Resources
MEMORY_LIMIT="1GB"
CPU_LIMIT="2"

# JVM settings
JVM_MIN_HEAP="256m"
JVM_MAX_HEAP="768m"

# Ports
HTTP_PORT="8080"
METRICS_PORT="9000"
```

---

## Usage

### Step 1: Build Base Container (Once per Java version)

```bash
cd /path/to/orchestrix/images/containers/lxc/quarkus-runner

# Edit build/build.conf to set Java version
vim build/build.conf
# Set: JVM_VERSION="21"  or "17" or "22"

# Build base container
./build/build.sh
```

This creates:
- LXC container with Java + tools
- LXC image: `quarkus-runner-base`
- Includes Promtail configured

### Step 2: Deploy Your JAR

```bash
# Create app config
cat > my-app.conf <<EOF
BASE_CONTAINER="quarkus-runner-base"
APP_NAME="my-service"
APP_VERSION="1.0"
APP_JAR_PATH="/path/to/my-service.jar"
MEMORY_LIMIT="1GB"
CPU_LIMIT="2"
HTTP_PORT="8080"
EOF

# Build app container
java -cp ... QuarkusAppContainerBuilder my-app.conf
```

This creates:
- Container: `my-service-v1.0`
- Systemd service: `my-service.service`
- Promtail configured for app logs
- Ready to start

### Step 3: Start Application

```bash
lxc start my-service-v1.0
lxc exec my-service-v1.0 -- systemctl status my-service
```

---

## Promtail Integration

**Automatic log shipping to Loki:**

### What Promtail Does:

1. **Scrapes application logs** from `/var/log/quarkus/application.log`
2. **Ships to Loki** at configured endpoint
3. **Adds labels**:
   - `container=<container-name>`
   - `app=<app-name>`
   - `version=<app-version>`
   - `host=<hostname>`
4. **JSON parsing** - parses structured JSON logs

### Promtail Configuration (Auto-generated):

```yaml
server:
  http_listen_port: 9080
  grpc_listen_port: 0

positions:
  filename: /tmp/positions.yaml

clients:
  - url: http://grafana-loki-v.1:3100/loki/api/v1/push

scrape_configs:
  - job_name: quarkus-app
    static_configs:
      - targets:
          - localhost
        labels:
          job: quarkus
          container: my-service-v1.0
          app: my-service
          __path__: /var/log/quarkus/*.log
    pipeline_stages:
      - json:
          expressions:
            timestamp: timestamp
            level: level
            message: message
      - labels:
          level:
      - timestamp:
          source: timestamp
          format: RFC3339
```

### Promtail Service (Systemd):

```ini
[Unit]
Description=Promtail Log Shipper
After=network.target

[Service]
Type=simple
User=root
ExecStart=/opt/promtail/promtail -config.file=/etc/promtail/config.yaml
Restart=always
RestartSec=10s

[Install]
WantedBy=multi-user.target
```

---

## Java Version Examples

### Using Java 17

```bash
# Edit build.conf
JVM_VERSION="17"
JVM_DISTRIBUTION="openjdk"

# Build base
./build/build.sh

# Publish as
IMAGE_ALIAS="quarkus-runner-base-java17"
```

### Using Java 21 (Default)

```bash
# Edit build.conf
JVM_VERSION="21"
JVM_DISTRIBUTION="openjdk"

# Build base
./build/build.sh

# Publish as
IMAGE_ALIAS="quarkus-runner-base"  # Default
```

### Using GraalVM

```bash
# Edit build.conf
JVM_DISTRIBUTION="graalvm"
JVM_VERSION="21"
INSTALL_GRAALVM="true"

# Build base
./build/build.sh

# Can now build native images
```

---

## Features

### ✅ Java Version Management
- Multiple distributions: OpenJDK, Temurin, GraalVM
- Multiple versions: 8, 11, 17, 21, 22
- Easy switching via config

### ✅ Promtail Integration
- Auto-configured log shipping
- JSON log parsing
- Automatic labels
- Systemd service management

### ✅ Resource Management
- CPU limits
- Memory limits
- JVM heap configuration
- Container quotas

### ✅ Monitoring
- SmallRye Health endpoints
- Prometheus metrics
- Health checks
- Readiness probes

### ✅ Database Support
- PostgreSQL driver pre-installed
- MySQL driver pre-installed
- JDBC connection pooling

### ✅ Development Tools
- Maven pre-configured
- Quarkus CLI installed
- Git, vim, curl, wget
- Debug port support

---

## Directory Structure

```
orchestrix/
├── src/main/java/com/telcobright/orchestrix/automation/api/container/lxc/app/quarkus/
│   └── example/
│       ├── QuarkusBaseContainerBuilder.java   # Build base with Java + Promtail
│       └── QuarkusAppContainerBuilder.java     # Deploy JAR to container
│
└── images/containers/lxc/quarkus-runner/
    ├── build/
    │   ├── build.sh                            # Build script
    │   └── build.conf                          # Java version config
    ├── README.md                               # Documentation
    ├── LOGGING_IMPLEMENTATION.md               # Promtail details
    ├── versions.conf                           # Version tracking
    └── sample-config.conf                      # Example config
```

---

## Build Process

### Base Container Build Steps:

1. ✅ Create LXC container (Debian 12)
2. ✅ Update system packages
3. ✅ Install development tools
4. ✅ Install Java (version from config)
5. ✅ Install Maven
6. ✅ Install Quarkus CLI
7. ✅ Install Promtail
8. ✅ Configure Promtail service
9. ✅ Install monitoring tools
10. ✅ Create systemd templates
11. ✅ Install database drivers
12. ✅ Setup Maven cache
13. ✅ Publish as LXC image

### App Container Build Steps:

1. ✅ Clone base container
2. ✅ Deploy JAR file
3. ✅ Configure application
4. ✅ Set resource limits
5. ✅ Create systemd service
6. ✅ Activate Promtail for app
7. ✅ Test container
8. ✅ Export container

---

## Example: Complete Workflow

### Scenario: Deploy Spring Boot app with Java 21

```bash
# 1. Build base container (once)
cd /home/mustafa/telcobright-projects/orchestrix/images/containers/lxc/quarkus-runner
vim build/build.conf
# Verify: JVM_VERSION="21"
./build/build.sh

# 2. Build your Spring Boot app
cd /path/to/my-spring-app
mvn clean package
# Creates: target/my-app-1.0.jar

# 3. Create app config
cat > app-config.conf <<EOF
BASE_CONTAINER="quarkus-runner-base"
APP_NAME="my-spring-app"
APP_VERSION="1.0"
APP_JAR_PATH="/path/to/my-spring-app/target/my-app-1.0.jar"
MEMORY_LIMIT="2GB"
CPU_LIMIT="4"
HTTP_PORT="8080"
METRICS_PORT="9000"
JVM_MIN_HEAP="512m"
JVM_MAX_HEAP="1536m"
EOF

# 4. Deploy to container
java -cp orchestrix.jar:... \
  com.telcobright.orchestrix.automation.api.container.lxc.app.quarkus.example.QuarkusAppContainerBuilder \
  app-config.conf

# 5. Start application
lxc start my-spring-app-v1.0
lxc exec my-spring-app-v1.0 -- systemctl start my-spring-app

# 6. Verify
curl http://<container-ip>:8080/actuator/health
curl http://<container-ip>:9000/metrics

# 7. Logs automatically ship to Loki via Promtail
```

---

## See Also

- **Source Code**: `src/main/java/com/telcobright/orchestrix/automation/api/container/lxc/app/quarkus/`
- **Documentation**: `images/containers/lxc/quarkus-runner/README.md`
- **Logging Details**: `images/containers/lxc/quarkus-runner/LOGGING_IMPLEMENTATION.md`
- **Version History**: `images/containers/lxc/quarkus-runner/versions.conf`
