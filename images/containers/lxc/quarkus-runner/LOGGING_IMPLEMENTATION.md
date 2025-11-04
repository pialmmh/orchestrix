# Quarkus Runner Container - Logging Implementation Instructions

## Purpose
This document provides step-by-step instructions for implementing complete logging infrastructure in Quarkus containers. Use this as a reference when implementing or troubleshooting logging features.

---

## Requirements

### ✅ Primary Requirements (Current Focus)

1. **Local Logging**: Quarkus application logs saved to local container BTRFS storage
2. **Log Rotation**: Automatic rotation of local log files (configurable size/time)
3. **Remote Shipping**: Promtail configured to send logs to central Grafana-Loki server
4. **Auto-Start**: Promtail launched automatically on container boot
5. **Standardized Framework**: Use SLF4J API + Log4j2 implementation across ALL Java applications

### 📋 Deferred Requirements (Future)

6. **Metrics Collection**: Prometheus scraping of `/q/metrics` endpoint
7. **Metrics Storage**: Prometheus server for time-series data
8. **Visualization**: Grafana dashboards for metrics and logs
9. **Alerting**: Alert rules for errors and threshold violations

---

## Architecture Overview

```
┌─────────────────────────────────────────────────────────┐
│ Java Application (Quarkus)                              │
│                                                          │
│  ┌────────────────────────────────────────────────┐    │
│  │ Application Code                                │    │
│  │ - Uses SLF4J API for logging                   │    │
│  │ - import org.slf4j.Logger;                     │    │
│  │ - Logger log = LoggerFactory.getLogger(...)    │    │
│  └──────────────────┬─────────────────────────────┘    │
│                     │                                   │
│                     ↓                                   │
│  ┌────────────────────────────────────────────────┐    │
│  │ SLF4J API (Logging Facade)                     │    │
│  │ - Vendor-neutral API                           │    │
│  │ - Compile-time dependency only                 │    │
│  └──────────────────┬─────────────────────────────┘    │
│                     │                                   │
│                     ↓                                   │
│  ┌────────────────────────────────────────────────┐    │
│  │ Log4j2 Implementation                          │    │
│  │ - High-performance async logging               │    │
│  │ - Configured via log4j2.xml                    │    │
│  │ - Outputs JSON format                          │    │
│  │ - File: /var/log/quarkus/application.log      │    │
│  │ - Rotation: 100MB or daily                     │    │
│  │ - Keep: 7 days of logs                         │    │
│  └──────────────────┬─────────────────────────────┘    │
│                     │                                   │
│                     ↓                                   │
│  ┌────────────────────────────────────────────────┐    │
│  │ Local Log Files (BTRFS Storage)                │    │
│  │ /var/log/quarkus/                              │    │
│  │ ├── application.log          (current)         │    │
│  │ ├── application-2024-01-01.log.gz (rotated)   │    │
│  │ ├── application-2024-01-02.log.gz             │    │
│  │ └── ...                                        │    │
│  │                                                 │    │
│  │ Storage: Within container BTRFS quota          │    │
│  │ Rotation triggers: 80% quota, 100MB size, daily│    │
│  └──────────────────┬─────────────────────────────┘    │
│                     │                                   │
└─────────────────────┼───────────────────────────────────┘
                      │
                      │ (tailed by)
                      ↓
┌─────────────────────────────────────────────────────────┐
│ Promtail Service (Auto-started systemd)                 │
│                                                          │
│  ┌────────────────────────────────────────────────┐    │
│  │ Promtail Agent                                 │    │
│  │ - Tails: /var/log/quarkus/*.log               │    │
│  │ - Parses: JSON format                         │    │
│  │ - Labels: app, env, level, container          │    │
│  │ - Buffers: If Loki unavailable                │    │
│  │ - Position tracking: /var/lib/promtail/...    │    │
│  └──────────────────┬─────────────────────────────┘    │
│                     │                                   │
└─────────────────────┼───────────────────────────────────┘
                      │
                      │ HTTP Push (batched, compressed)
                      │ Every 5s or when batch full
                      ↓
┌─────────────────────────────────────────────────────────┐
│ Grafana-Loki Container (Central Log Server)             │
│                                                          │
│  ┌────────────────────────────────────────────────┐    │
│  │ Loki (Log Aggregation)                         │    │
│  │ - Receives: HTTP POST /loki/api/v1/push       │    │
│  │ - Port: 3100                                   │    │
│  │ - Storage: BTRFS 4G quota                      │    │
│  │ - Retention: 7 days                            │    │
│  │ - Indexes: By labels (not content)             │    │
│  │ - Compression: Enabled                         │    │
│  └──────────────────┬─────────────────────────────┘    │
│                     │                                   │
│                     ↓                                   │
│  ┌────────────────────────────────────────────────┐    │
│  │ Grafana (Query & Visualization)                │    │
│  │ - UI: http://grafana:3000                      │    │
│  │ - Query: {app="myapp", level="ERROR"}          │    │
│  │ - Dashboards: Pre-built log views              │    │
│  │ - Alerts: On error patterns                    │    │
│  └────────────────────────────────────────────────┘    │
└─────────────────────────────────────────────────────────┘
```

---

## Why SLF4J + Log4j2?

### Decision Rationale

**SLF4J (Simple Logging Facade for Java):**
- ✅ **Vendor-neutral API** - Switch implementations without code changes
- ✅ **Industry standard** - Used by 80%+ of Java projects
- ✅ **Compile-time only** - No runtime dependency on SLF4J jar
- ✅ **Parameterized logging** - `log.info("User {} logged in", username)`
- ✅ **Bridge support** - Can capture logs from other frameworks

**Log4j2 (Apache Log4j 2.x):**
- ✅ **High performance** - Async logging, low latency
- ✅ **Zero garbage collection** - Important for production
- ✅ **Flexible configuration** - XML, JSON, YAML, properties
- ✅ **JSON output native** - Perfect for Loki parsing
- ✅ **Automatic reload** - Config changes without restart
- ✅ **Rich appenders** - File, console, rolling, async
- ✅ **Powerful filters** - Route logs based on content/level

**Why NOT Quarkus default (JBoss Logging)?**
- ❌ Quarkus-specific, not portable
- ❌ Less flexible than Log4j2
- ❌ Harder to configure advanced features
- ❌ Not standard across non-Quarkus Java apps

**Why NOT Logback?**
- ⚠️ Good, but Log4j2 is faster
- ⚠️ Log4j2 has better async performance
- ⚠️ Log4j2 has zero-GC feature

---

## Implementation Steps

### Step 1: Add SLF4J + Log4j2 Dependencies (Quarkus)

**File:** `pom.xml` (or in app container build)

```xml
<dependencies>
    <!-- SLF4J API (compile-time only) -->
    <dependency>
        <groupId>org.slf4j</groupId>
        <artifactId>slf4j-api</artifactId>
        <version>2.0.9</version>
    </dependency>

    <!-- Log4j2 Implementation -->
    <dependency>
        <groupId>org.apache.logging.log4j</groupId>
        <artifactId>log4j-core</artifactId>
        <version>2.21.1</version>
    </dependency>

    <dependency>
        <groupId>org.apache.logging.log4j</groupId>
        <artifactId>log4j-slf4j2-impl</artifactId>
        <version>2.21.1</version>
    </dependency>

    <!-- Log4j2 JSON Layout (for Loki) -->
    <dependency>
        <groupId>org.apache.logging.log4j</groupId>
        <artifactId>log4j-layout-template-json</artifactId>
        <version>2.21.1</version>
    </dependency>

    <!-- Async logging (high performance) -->
    <dependency>
        <groupId>com.lmax</groupId>
        <artifactId>disruptor</artifactId>
        <version>3.4.4</version>
    </dependency>

    <!-- Exclude Quarkus default logging -->
    <dependency>
        <groupId>io.quarkus</groupId>
        <artifactId>quarkus-logging-json</artifactId>
        <exclusions>
            <exclusion>
                <groupId>org.jboss.logging</groupId>
                <artifactId>jboss-logging</artifactId>
            </exclusion>
        </exclusions>
    </dependency>
</dependencies>
```

**Action for AI:** Update `QuarkusBaseContainerBuilder.java` to pre-download these JARs into Maven cache during base container build.

---

### Step 2: Create Log4j2 Configuration Template

**File:** `/etc/quarkus/log4j2.xml.template`

This file is created in the base container and used by all apps.

```xml
<?xml version="1.0" encoding="UTF-8"?>
<Configuration status="WARN" monitorInterval="30">
    <!-- monitorInterval: Auto-reload config every 30 seconds if changed -->

    <Properties>
        <!-- Variables for easy configuration -->
        <Property name="LOG_DIR">/var/log/quarkus</Property>
        <Property name="LOG_FILE">application</Property>
        <Property name="APP_NAME">${env:APP_NAME:-unknown-app}</Property>
        <Property name="CONTAINER_NAME">${env:CONTAINER_NAME:-unknown-container}</Property>
        <Property name="LOG_PATTERN">%d{yyyy-MM-dd HH:mm:ss.SSS} [%t] %-5level %logger{36} - %msg%n</Property>
    </Properties>

    <Appenders>
        <!-- Console Appender (for debugging) -->
        <Console name="Console" target="SYSTEM_OUT">
            <PatternLayout pattern="${LOG_PATTERN}"/>
        </Console>

        <!-- Rolling File Appender (main logs) -->
        <RollingFile name="RollingFile"
                     fileName="${LOG_DIR}/${LOG_FILE}.log"
                     filePattern="${LOG_DIR}/${LOG_FILE}-%d{yyyy-MM-dd}-%i.log.gz"
                     immediateFlush="false">

            <!-- JSON Layout for Loki parsing -->
            <JsonTemplateLayout eventTemplateUri="classpath:LogstashJsonEventLayoutV1.json">
                <EventTemplateAdditionalField key="app" value="${APP_NAME}"/>
                <EventTemplateAdditionalField key="container" value="${CONTAINER_NAME}"/>
                <EventTemplateAdditionalField key="env" value="${env:ENV:-dev}"/>
            </JsonTemplateLayout>

            <Policies>
                <!-- Rotate daily -->
                <TimeBasedTriggeringPolicy interval="1" modulate="true"/>

                <!-- Rotate when file reaches 100MB -->
                <SizeBasedTriggeringPolicy size="100MB"/>
            </Policies>

            <!-- Keep maximum 7 days of logs -->
            <DefaultRolloverStrategy max="7">
                <!-- Delete files older than 7 days -->
                <Delete basePath="${LOG_DIR}" maxDepth="1">
                    <IfFileName glob="${LOG_FILE}-*.log.gz"/>
                    <IfLastModified age="7d"/>
                </Delete>
            </DefaultRolloverStrategy>
        </RollingFile>

        <!-- Async Wrapper (for performance) -->
        <Async name="AsyncFile" includeLocation="true">
            <AppenderRef ref="RollingFile"/>
        </Async>
    </Appenders>

    <Loggers>
        <!-- Application loggers -->
        <Logger name="com.yourcompany" level="DEBUG" additivity="false">
            <AppenderRef ref="AsyncFile"/>
            <AppenderRef ref="Console"/>
        </Logger>

        <!-- Quarkus framework -->
        <Logger name="io.quarkus" level="INFO" additivity="false">
            <AppenderRef ref="AsyncFile"/>
        </Logger>

        <!-- Hibernate (if using JPA) -->
        <Logger name="org.hibernate" level="WARN" additivity="false">
            <AppenderRef ref="AsyncFile"/>
        </Logger>

        <!-- Root logger (catches everything else) -->
        <Root level="INFO">
            <AppenderRef ref="AsyncFile"/>
            <AppenderRef ref="Console"/>
        </Root>
    </Loggers>
</Configuration>
```

**Action for AI:**
1. Create this file in `QuarkusBaseContainerBuilder.installLog4j2Configuration()`
2. Make it editable per-app (copy to app container, customize APP_NAME)

---

### Step 3: Configure Local Log Rotation (Backup)

**File:** `/etc/logrotate.d/quarkus`

This is a **backup** rotation mechanism. Log4j2 handles primary rotation, but logrotate ensures old files are cleaned even if Log4j2 fails.

```bash
/var/log/quarkus/*.log {
    # Rotate daily
    daily

    # Keep 7 days
    rotate 7

    # Force rotation if file exceeds 100MB
    maxsize 100M

    # Compress old logs
    compress
    delaycompress

    # Don't error if file is missing
    missingok

    # Don't rotate empty files
    notifempty

    # Create new log file with permissions
    create 0640 quarkus quarkus

    # Share scripts between multiple files
    sharedscripts

    # Post-rotation hook (optional)
    postrotate
        # Log4j2 handles file rotation, no action needed
        # Could notify monitoring here
    endscript
}
```

**Action for AI:** Create this file in `QuarkusBaseContainerBuilder.configureLogrotate()`

---

### Step 4: Create Promtail Configuration

**File:** `/etc/promtail/config.yml`

This is the **active** configuration (not template) that Promtail will use.

```yaml
# Promtail server configuration
server:
  http_listen_port: 9080
  grpc_listen_port: 0

# Position tracking (where Promtail left off reading)
positions:
  filename: /var/lib/promtail/positions.yaml

# Loki endpoint (central log server)
clients:
  - url: http://grafana-loki-v.1:3100/loki/api/v1/push
    # Batching configuration
    batchwait: 1s
    batchsize: 1048576  # 1MB

    # Retry configuration
    backoff_config:
      min_period: 500ms
      max_period: 5m
      max_retries: 10

    # Timeout
    timeout: 10s

# Scrape configuration
scrape_configs:
  - job_name: quarkus
    static_configs:
      - targets:
          - localhost
        labels:
          job: quarkus-app
          container: ${CONTAINER_NAME}
          app: ${APP_NAME}
          env: ${ENV}
          __path__: /var/log/quarkus/*.log

    # Pipeline stages (how to parse logs)
    pipeline_stages:
      # Parse JSON logs from Log4j2
      - json:
          expressions:
            timestamp: "@timestamp"
            level: level
            message: message
            logger: logger_name
            thread: thread_name
            app: app
            container: container

      # Extract labels from JSON
      - labels:
          level:
          logger:
          app:
          container:

      # Use timestamp from log
      - timestamp:
          source: timestamp
          format: RFC3339Nano

      # Output the message
      - output:
          source: message
```

**Action for AI:**
1. Create this file in `QuarkusBaseContainerBuilder.configurePromtail()`
2. Replace `${CONTAINER_NAME}`, `${APP_NAME}`, `${ENV}` with actual values during app container build

---

### Step 5: Create Promtail Systemd Service

**File:** `/etc/systemd/system/promtail.service`

This ensures Promtail starts automatically on container boot.

```ini
[Unit]
Description=Promtail Log Shipper for Grafana Loki
Documentation=https://grafana.com/docs/loki/latest/clients/promtail/
After=network-online.target
Wants=network-online.target

[Service]
Type=simple
User=root
Group=root

# Working directory
WorkingDirectory=/opt/promtail

# Start command
ExecStart=/opt/promtail/promtail \
    -config.file=/etc/promtail/config.yml \
    -config.expand-env=true

# Restart configuration
Restart=on-failure
RestartSec=10s

# Resource limits
LimitNOFILE=65536

# Logging
StandardOutput=journal
StandardError=journal
SyslogIdentifier=promtail

[Install]
WantedBy=multi-user.target
```

**Action for AI:**
1. Create this file in `QuarkusBaseContainerBuilder.createPromtailService()`
2. Enable service: `systemctl enable promtail.service`
3. DO NOT start yet (container not fully built)

---

### Step 6: Update QuarkusBaseContainerBuilder.java

**Location:** `src/main/java/.../automation/containers/quarkus/QuarkusBaseContainerBuilder.java`

**Add these methods:**

```java
private void installLog4j2Configuration() throws Exception {
    logger.info("Installing Log4j2 configuration...");

    String log4j2Config = createLog4j2ConfigTemplate();
    device.executeCommand("echo '" + log4j2Config + "' | lxc exec " + containerName +
        " -- tee /etc/quarkus/log4j2.xml.template");
}

private void configureLogrotate() throws Exception {
    logger.info("Configuring logrotate...");

    String logrotateConfig = String.join("\n",
        "/var/log/quarkus/*.log {",
        "    daily",
        "    rotate 7",
        "    maxsize 100M",
        "    compress",
        "    delaycompress",
        "    missingok",
        "    notifempty",
        "    create 0640 quarkus quarkus",
        "    sharedscripts",
        "}"
    );

    device.executeCommand("echo '" + logrotateConfig + "' | lxc exec " + containerName +
        " -- tee /etc/logrotate.d/quarkus");
}

private void configurePromtailService() throws Exception {
    logger.info("Creating Promtail systemd service...");

    String serviceContent = String.join("\n",
        "[Unit]",
        "Description=Promtail Log Shipper",
        "After=network-online.target",
        "",
        "[Service]",
        "Type=simple",
        "User=root",
        "WorkingDirectory=/opt/promtail",
        "ExecStart=/opt/promtail/promtail -config.file=/etc/promtail/config.yml -config.expand-env=true",
        "Restart=on-failure",
        "RestartSec=10s",
        "",
        "[Install]",
        "WantedBy=multi-user.target"
    );

    device.executeCommand("echo '" + serviceContent + "' | lxc exec " + containerName +
        " -- tee /etc/systemd/system/promtail.service");

    // Enable service (will start on next boot)
    device.executeCommand("lxc exec " + containerName + " -- systemctl enable promtail.service");
}

private void configurePromtailConfig() throws Exception {
    logger.info("Creating Promtail configuration...");

    String lokiEndpoint = config.getProperty("LOKI_ENDPOINT", "http://grafana-loki-v.1:3100");

    String promtailConfig = String.join("\n",
        "server:",
        "  http_listen_port: 9080",
        "",
        "positions:",
        "  filename: /var/lib/promtail/positions.yaml",
        "",
        "clients:",
        "  - url: " + lokiEndpoint + "/loki/api/v1/push",
        "    batchwait: 1s",
        "    batchsize: 1048576",
        "    timeout: 10s",
        "",
        "scrape_configs:",
        "  - job_name: quarkus",
        "    static_configs:",
        "      - targets:",
        "          - localhost",
        "        labels:",
        "          job: quarkus-app",
        "          container: ${CONTAINER_NAME}",
        "          app: ${APP_NAME}",
        "          env: ${ENV:-dev}",
        "          __path__: /var/log/quarkus/*.log",
        "    pipeline_stages:",
        "      - json:",
        "          expressions:",
        "            timestamp: '@timestamp'",
        "            level: level",
        "            message: message",
        "            logger: logger_name",
        "      - labels:",
        "          level:",
        "          logger:",
        "      - timestamp:",
        "          source: timestamp",
        "          format: RFC3339Nano"
    );

    device.executeCommand("echo '" + promtailConfig + "' | lxc exec " + containerName +
        " -- tee /etc/promtail/config.yml");

    // Create positions directory
    device.executeCommand("lxc exec " + containerName + " -- mkdir -p /var/lib/promtail");
}
```

**Add to build() method (in correct order):**

```java
// After installPromtail()
configurePromtailConfig();
configurePromtailService();

// After createQuarkusUser()
installLog4j2Configuration();
configureLogrotate();
```

---

### Step 7: Update QuarkusAppContainerBuilder.java

**Location:** `src/main/java/.../automation/containers/quarkus/QuarkusAppContainerBuilder.java`

**Add method to activate Promtail in app containers:**

```java
private void activatePromtailForApp() throws Exception {
    logger.info("Activating Promtail for app container...");

    // Update Promtail config with app-specific values
    device.executeCommand("lxc exec " + containerName +
        " -- sed -i 's/\\${CONTAINER_NAME}/" + containerName + "/g' /etc/promtail/config.yml");
    device.executeCommand("lxc exec " + containerName +
        " -- sed -i 's/\\${APP_NAME}/" + appName + "/g' /etc/promtail/config.yml");

    String env = config.getProperty("DEPLOYMENT_ENV", "dev");
    device.executeCommand("lxc exec " + containerName +
        " -- sed -i 's/\\${ENV:-dev}/" + env + "/g' /etc/promtail/config.yml");

    // Start Promtail service
    device.executeCommand("lxc exec " + containerName + " -- systemctl start promtail.service");

    // Verify it's running
    String status = device.executeCommand("lxc exec " + containerName +
        " -- systemctl is-active promtail.service");

    if (!status.trim().equals("active")) {
        throw new Exception("Promtail failed to start!");
    }

    logger.info("Promtail is active and shipping logs");
}
```

**Add to build() method:**

```java
// After createAppService()
activatePromtailForApp();
```

---

## Testing Procedure

### Test 1: Local Logging

```bash
# 1. Build base container
sudo ./scripts/orchestrix-quarkus.sh build-base

# 2. Build test app
sudo ./scripts/orchestrix-quarkus.sh build-app testapp 1.0.0 /path/to/test.jar

# 3. Start container
lxc start testapp-v1.0.0

# 4. Check logs are being written
lxc exec testapp-v1.0.0 -- ls -lh /var/log/quarkus/
lxc exec testapp-v1.0.0 -- tail -f /var/log/quarkus/application.log

# Expected: JSON formatted logs
```

### Test 2: Log Rotation

```bash
# 1. Generate lots of logs
lxc exec testapp-v1.0.0 -- bash -c "for i in {1..100000}; do logger 'Test log message'; done"

# 2. Check rotation happened
lxc exec testapp-v1.0.0 -- ls -lh /var/log/quarkus/

# Expected: application-2024-01-01-1.log.gz files
```

### Test 3: Promtail Shipping

```bash
# 1. Check Promtail is running
lxc exec testapp-v1.0.0 -- systemctl status promtail

# 2. Check Promtail logs
lxc exec testapp-v1.0.0 -- journalctl -u promtail -f

# 3. Verify in Grafana
# Open: http://grafana:3000
# Query: {app="testapp"}

# Expected: Logs visible in Grafana
```

### Test 4: BTRFS Storage

```bash
# 1. Check storage usage
sudo btrfs qgroup show /home/telcobright/btrfs

# 2. Watch storage grow as logs accumulate
watch 'sudo btrfs qgroup show /home/telcobright/btrfs | grep testapp'

# Expected: Usage increases, stays under quota
```

---

## Configuration Reference

### Environment Variables (App Container)

```bash
# Set during app container build
APP_NAME="myapp"                    # Application identifier
CONTAINER_NAME="myapp-v1.0.0"       # Container identifier
ENV="production"                    # Environment (dev/staging/prod)
LOG_LEVEL="INFO"                    # Root log level
```

### Log4j2 Properties

```bash
# In application.properties or system properties
log4j2.configurationFile=/etc/quarkus/log4j2.xml
log4j2.contextSelector=org.apache.logging.log4j.core.async.AsyncLoggerContextSelector
```

### Promtail Configuration

```yaml
# File: /etc/promtail/config.yml
clients:
  - url: http://grafana-loki-v.1:3100/loki/api/v1/push

scrape_configs:
  - job_name: quarkus
    static_configs:
      - labels:
          app: ${APP_NAME}
          container: ${CONTAINER_NAME}
          env: ${ENV}
```

---

## Troubleshooting

### Problem: Logs not being written

**Check:**
```bash
# 1. Check directory exists and permissions
lxc exec container -- ls -ld /var/log/quarkus/
lxc exec container -- ls -l /var/log/quarkus/

# 2. Check Log4j2 configuration
lxc exec container -- cat /etc/quarkus/log4j2.xml

# 3. Check Java process
lxc exec container -- ps aux | grep java
```

### Problem: Promtail not running

**Check:**
```bash
# 1. Check service status
lxc exec container -- systemctl status promtail

# 2. Check service logs
lxc exec container -- journalctl -u promtail --no-pager -n 50

# 3. Check configuration
lxc exec container -- /opt/promtail/promtail -config.file=/etc/promtail/config.yml -dry-run
```

### Problem: Logs not reaching Loki

**Check:**
```bash
# 1. Network connectivity
lxc exec container -- ping -c 3 grafana-loki-v.1

# 2. Loki endpoint
lxc exec container -- curl -v http://grafana-loki-v.1:3100/ready

# 3. Promtail positions (what it last read)
lxc exec container -- cat /var/lib/promtail/positions.yaml

# 4. Check Loki logs
lxc exec grafana-loki-v.1 -- journalctl -u loki -f
```

---

## Implementation Checklist

Use this checklist to verify complete implementation:

### Base Container Build
- [ ] Log4j2 dependencies pre-downloaded to Maven cache
- [ ] `/etc/quarkus/log4j2.xml.template` created
- [ ] `/etc/logrotate.d/quarkus` created
- [ ] `/opt/promtail/promtail` binary installed
- [ ] `/etc/promtail/config.yml` template created
- [ ] `/etc/systemd/system/promtail.service` created
- [ ] Promtail service enabled (not started)
- [ ] `/var/log/quarkus/` directory created
- [ ] `/var/lib/promtail/` directory created
- [ ] Permissions set correctly (quarkus user owns logs)

### App Container Build
- [ ] Log4j2 configuration customized with APP_NAME
- [ ] Promtail config updated with container name
- [ ] Promtail service started
- [ ] Promtail status verified (active)
- [ ] Test log written
- [ ] Log file exists in /var/log/quarkus/
- [ ] Promtail shipping to Loki verified

### End-to-End Validation
- [ ] Application logs visible in /var/log/quarkus/application.log
- [ ] Logs in JSON format
- [ ] Log rotation working (create large logs, verify rotation)
- [ ] Promtail running (systemctl status promtail)
- [ ] Logs visible in Grafana (query {app="appname"})
- [ ] Storage within BTRFS quota
- [ ] Old logs compressed (.gz files exist)

---

## Next Steps (Deferred)

After logging is complete, implement metrics:

1. **Prometheus Server Setup**
   - Install Prometheus in separate container
   - Configure scraping of `/q/metrics` endpoints
   - Set retention and storage

2. **Grafana Dashboards**
   - Create JVM metrics dashboard (heap, GC, threads)
   - Create HTTP metrics dashboard (requests, latency)
   - Create application metrics dashboard (custom counters/gauges)

3. **Alerting**
   - Alert on error log patterns
   - Alert on metric thresholds (high CPU, memory)
   - Alert on log shipping failures

---

## References

- Log4j2 Documentation: https://logging.apache.org/log4j/2.x/
- SLF4J User Manual: https://www.slf4j.org/manual.html
- Promtail Configuration: https://grafana.com/docs/loki/latest/clients/promtail/configuration/
- Loki API: https://grafana.com/docs/loki/latest/api/
- Quarkus Logging Guide: https://quarkus.io/guides/logging

---

**Document Version:** 1.0
**Last Updated:** 2024-10-02
**Owner:** Orchestrix Automation Team
