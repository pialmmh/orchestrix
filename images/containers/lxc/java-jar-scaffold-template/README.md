# Java JAR Alpine Container Template

Package pre-built JAR files into minimal Alpine Linux containers with JRE, following the same pattern as Consul binary packaging.

## Overview

This template creates lightweight Alpine containers for Java applications by:
1. Taking a **pre-built JAR** (like Consul takes a pre-built binary)
2. Installing minimal JRE (OpenJDK headless)
3. Configuring systemd service
4. Optional Promtail for log shipping
5. Publishing as reusable LXC image

## Quick Start

```bash
# 1. Build your JAR (separately, outside this workflow)
cd /path/to/your/java/project
mvn clean package

# 2. Place JAR in expected location
mkdir -p /home/mustafa/telcobright-projects/orchestrix/images/standalone-binaries/my-app/my-app-jar-v.1/
cp target/my-app.jar /home/mustafa/telcobright-projects/orchestrix/images/standalone-binaries/my-app/my-app-jar-v.1/my-app.jar

# 3. Configure and build container
cd /home/mustafa/telcobright-projects/orchestrix/images/lxc/java-jar-scaffold-template
vim build/build.conf  # Set APP_NAME, VERSION, JAVA_VERSION
./startDefault.sh
```

## Directory Structure

```
java-jar-scaffold-template/
├── build/
│   ├── build.sh          # Build script (creates Alpine image with JRE)
│   └── build.conf        # Build configuration
├── launch.sh             # Launch containers from image
├── launch.conf           # Launch configuration
├── startDefault.sh       # Quick start script
└── README.md            # This file
```

## JAR Placement Convention

Following the Consul binary pattern, JARs must be pre-built and placed in:

```
standalone-binaries/
└── [app-name]/
    └── [app-name]-jar-v.[version]/
        └── [app-name].jar
```

Example:
```
standalone-binaries/
└── my-service/
    └── my-service-jar-v.1/
        └── my-service.jar
```

## Configuration

### Build Configuration (`build/build.conf`)

```bash
# Application
APP_NAME="my-java-app"
VERSION="1"

# Java Configuration
JAVA_VERSION="21"              # Supported: 11, 17, 21, 22
JVM_OPTS="-Xms256m -Xmx512m"
MAIN_CLASS=""                  # Empty for executable JAR

# Promtail (optional)
PROMTAIL_ENABLED="true"
LOKI_ENDPOINT="http://grafana-loki:3100"
```

### Launch Configuration (`launch.conf`)

```bash
# Instance
INSTANCE_ID="1"

# Resources
MEMORY_LIMIT="1GB"
CPU_LIMIT="2"

# Bind mounts (optional)
MOUNT_BINDS="/host/config:/app/config"
```

## Java Version Support

| Version | Package | Status |
|---------|---------|--------|
| Java 11 | openjdk11-jre-headless | ✅ Supported |
| Java 17 | openjdk17-jre-headless | ✅ Supported |
| Java 21 | openjdk21-jre-headless | ✅ Default |
| Java 22 | openjdk22-jre-headless | ✅ Supported |

## Build Process

1. **Verify JAR exists** (must be pre-built)
2. **Create Alpine container**
3. **Install OpenJDK JRE** (headless, minimal)
4. **Copy JAR to /app/**
5. **Create OpenRC service**
6. **Install Promtail** (if enabled)
7. **Publish as image**
8. **Export tarball**

## Service Management

The Java application runs as an OpenRC service:

```bash
# Start service
lxc exec my-app-1 -- rc-service my-app start

# Stop service
lxc exec my-app-1 -- rc-service my-app stop

# Check status
lxc exec my-app-1 -- rc-service my-app status

# View logs
lxc exec my-app-1 -- tail -f /var/log/app/my-app.log
```

## JVM Options

Configure JVM options in `build.conf`:

```bash
# Memory settings
JVM_OPTS="-Xms256m -Xmx512m"

# Garbage collection
JVM_OPTS="-XX:+UseG1GC -XX:MaxGCPauseMillis=200"

# Debug mode
JVM_OPTS="-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005"
```

## Promtail Integration

When enabled, Promtail automatically:
- Ships logs to Loki
- Parses Java log format
- Adds container labels
- Handles log rotation

Promtail config location: `/etc/promtail/config.yaml`

## Examples

### Spring Boot Application

```bash
# 1. Build Spring Boot JAR
cd /path/to/spring-app
mvn clean package

# 2. Place in expected location
mkdir -p .../standalone-binaries/spring-app/spring-app-jar-v.1/
cp target/spring-app-1.0.jar .../standalone-binaries/spring-app/spring-app-jar-v.1/spring-app.jar

# 3. Configure
vim build/build.conf
# APP_NAME="spring-app"
# VERSION="1"
# JAVA_VERSION="21"
# SERVICE_PORT="8080"

# 4. Build and launch
./startDefault.sh
```

### Quarkus Application

```bash
# 1. Build Quarkus JAR
cd /path/to/quarkus-app
mvn clean package

# 2. Use quarkus-runner JAR
cp target/quarkus-app-1.0-runner.jar .../standalone-binaries/quarkus-app/quarkus-app-jar-v.1/quarkus-app.jar

# 3. Configure for Quarkus
vim build/build.conf
# JVM_OPTS="-Xms128m -Xmx256m"  # Quarkus optimized

# 4. Build and launch
./startDefault.sh
```

## Automation Integration

This template integrates with the JavaJarAlpineScaffold automation:

```java
// Use existing automation
JavaJarAlpineScaffold scaffold = new JavaJarAlpineScaffold(device, config);
scaffold.scaffoldContainer();
```

## Comparison with Consul Pattern

| Aspect | Consul Binary | Java JAR |
|--------|--------------|----------|
| Artifact | Pre-built binary | Pre-built JAR |
| Location | standalone-binaries/consul/ | standalone-binaries/[app]/ |
| Runtime | Direct execution | JRE required |
| Base Image | Alpine | Alpine |
| Service | OpenRC | OpenRC |
| Size | ~100MB | ~200MB (with JRE) |

## Troubleshooting

### JAR not found
```
ERROR: JAR file not found: /path/to/jar
```
**Solution**: Build JAR first, place in correct location

### Java version not available
```
ERROR: package 'openjdk22-jre-headless' not found
```
**Solution**: Use supported version (11, 17, 21)

### Out of memory
```
Exception: java.lang.OutOfMemoryError
```
**Solution**: Increase JVM_OPTS memory settings

### Port already in use
```
Bind for 0.0.0.0:8080 failed: port is already allocated
```
**Solution**: Change SERVICE_PORT in launch.conf

## See Also

- [Consul Binary Packaging](../consul/README.md)
- [Container Scaffolding Standard](../CONTAINER_SCAFFOLDING_STANDARD.md)
- [JavaJarAlpineScaffold.java](../../../src/main/java/com/telcobright/orchestrix/automation/scaffold/JavaJarAlpineScaffold.java)