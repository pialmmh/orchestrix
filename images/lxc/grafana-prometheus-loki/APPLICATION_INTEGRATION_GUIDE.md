# OmniQueue Application Logging Integration Guide

**For AI Agent: Setting up logging for OmniQueue-based test applications**

## Overview

We have a centralized logging infrastructure ready to receive logs from any OmniQueue-based application. This guide explains how to integrate your test application with our logging system.

---

## Infrastructure Details

### Available Logging Containers

**Primary Container:** `grafana-prometheus-loki-v1`
- **IP Address:** `10.10.199.26`
- **Grafana UI:** http://10.10.199.26:3000 (admin/admin)
- **Loki API:** http://10.10.199.26:3100
- **Prometheus:** http://10.10.199.26:7330

**Services:**
- ✅ Grafana 10.2.3 - Visualization
- ✅ Prometheus 2.48.0 - Metrics
- ✅ Loki 2.9.4 - Log aggregation (OmniQueue-ready)
- ✅ Promtail 2.9.4 - Log shipping

**Resources:**
- Memory: 4GB
- CPU: 4 cores
- Storage: 25GB BTRFS

---

## Step 1: Configure Your Application for JSON Logging

### Required Log Format

Your application MUST output logs in JSON format with the following structure:

```json
{
  "timestamp": "2025-10-11T10:30:45.123Z",
  "level": "INFO",
  "message": "Processing order",
  "app": "your-app-name",
  "component": "your-component-name",
  "environment": "production",
  "host": "container-hostname",
  "trace_id": "550e8400-e29b-41d4-a716-446655440000",
  "queue_name": "processing",
  "event": "message_consumed",
  "backend": "rabbitmq-memory",
  "processing_time_ms": 150,
  "order_id": "ORD-12345",
  "user_id": "USR-98765"
}
```

### Standard Fields (REQUIRED)

| Field | Type | Required | Description | Example |
|-------|------|----------|-------------|---------|
| `timestamp` | string | ✅ Yes | RFC3339 format | `2025-10-11T10:30:45.123Z` |
| `level` | string | ✅ Yes | Log level | `INFO`, `WARN`, `ERROR`, `DEBUG` |
| `message` | string | ✅ Yes | Log message | `"Processing order"` |
| `app` | string | ✅ Yes | Application name | `"order-service"` |

### OmniQueue Fields (For Distributed Tracing)

| Field | Type | Required | Description | Example |
|-------|------|----------|-------------|---------|
| `trace_id` | string | Recommended | Distributed trace ID | `"550e8400-e29b-41d4..."` |
| `queue_name` | string | Recommended | Message queue name | `"processing"` |
| `event` | string | Recommended | Event type | `"message_published"`, `"message_consumed"` |
| `backend` | string | Optional | Backend type | `"rabbitmq-memory"`, `"redis"` |
| `processing_time_ms` | number | Optional | Processing duration | `150` |

### Optional Context Fields

| Field | Type | Description | Example |
|-------|------|-------------|---------|
| `component` | string | Component name | `"rest-api"`, `"worker"` |
| `environment` | string | Environment | `"production"`, `"staging"` |
| `host` | string | Container/host | `"order-service-1"` |
| `user_id` | string | User identifier | `"USR-98765"` |
| `order_id` | string | Business entity ID | `"ORD-12345"` |
| `error` | string | Error details | `"Connection timeout"` |

---

## Step 2: Configure Promtail in Your Container

### Install Promtail

Your application container needs Promtail to ship logs to Loki.

**Download and install Promtail 2.9.4:**

```bash
# Inside your container
wget -q https://github.com/grafana/loki/releases/download/v2.9.4/promtail-linux-amd64.zip
unzip promtail-linux-amd64.zip
mv promtail-linux-amd64 /usr/local/bin/promtail
chmod +x /usr/local/bin/promtail
rm promtail-linux-amd64.zip
```

### Create Promtail Configuration

Create `/etc/promtail/promtail-config.yaml`:

```yaml
server:
  http_listen_port: 9080
  grpc_listen_port: 0

positions:
  filename: /tmp/positions.yaml

clients:
  - url: http://10.10.199.26:3100/loki/api/v1/push

scrape_configs:
  - job_name: your-app-name
    static_configs:
      - targets:
          - localhost
        labels:
          job: your-app-name
          service: your-app-name
          __path__: /var/log/your-app/*.log

    pipeline_stages:
      # Parse JSON logs
      - json:
          expressions:
            timestamp: timestamp
            level: level
            message: message
            logger: loggerName
            app: app
            component: component
            environment: environment
            host: host
            trace_id: trace_id
            queue_name: queue_name
            event: event
            backend: backend
            processing_time_ms: processing_time_ms
            user_id: user_id
            order_id: order_id
            error: error

      # Use timestamp from log
      - timestamp:
          source: timestamp
          format: RFC3339

      # Create indexed labels (for fast queries)
      - labels:
          level:
          app:
          component:
          environment:
          trace_id:
          queue_name:
          event:
          backend:

      # Output the message
      - output:
          source: message
```

**IMPORTANT:** Replace `your-app-name` with your actual application name.

### Create Promtail Service

Create `/etc/systemd/system/promtail.service`:

```ini
[Unit]
Description=Promtail Log Shipper
After=network.target

[Service]
Type=simple
User=root
ExecStart=/usr/local/bin/promtail -config.file=/etc/promtail/promtail-config.yaml
Restart=always
RestartSec=10s

[Install]
WantedBy=multi-user.target
```

### Start Promtail

```bash
systemctl daemon-reload
systemctl enable promtail
systemctl start promtail
systemctl status promtail
```

---

## Step 3: Generate Trace IDs

### Generate UUID for trace_id

Your application needs to generate a unique trace ID for each request/transaction.

**Java:**
```java
import java.util.UUID;

String traceId = UUID.randomUUID().toString();
```

**Python:**
```python
import uuid

trace_id = str(uuid.uuid4())
```

**Node.js:**
```javascript
const { v4: uuidv4 } = require('uuid');

const traceId = uuidv4();
```

**Go:**
```go
import "github.com/google/uuid"

traceId := uuid.New().String()
```

### Propagate trace_id

When your application publishes to OmniQueue, include the trace_id in the metadata:

```java
// Java example
Map<String, Object> metadata = new HashMap<>();
metadata.put("trace_id", traceId);
metadata.put("queue_name", "processing");
metadata.put("event", "message_published");
metadata.put("backend", "rabbitmq-memory");

omniQueue.publish("processing", payload, metadata);
```

When consuming from OmniQueue, extract and use the trace_id from metadata:

```java
// Java example
String traceId = (String) metadata.get("trace_id");
String queueName = (String) metadata.get("queue_name");

// Include in your logs
log.info("Processing message",
    "trace_id", traceId,
    "queue_name", queueName,
    "event", "message_consumed");
```

---

## Step 4: Verify Logs Are Being Received

### Test Loki API

Check if logs are arriving:

```bash
# From host machine
curl -s -G "http://10.10.199.26:3100/loki/api/v1/query" \
  --data-urlencode 'query={app="your-app-name"}' | jq '.'
```

**Expected output:**
```json
{
  "status": "success",
  "data": {
    "resultType": "streams",
    "result": [...]
  }
}
```

### Check Available Labels

```bash
curl -s "http://10.10.199.26:3100/loki/api/v1/labels" | jq '.data'
```

You should see your labels: `app`, `trace_id`, `queue_name`, `event`, etc.

### View Latest Logs

```bash
curl -s -G "http://10.10.199.26:3100/loki/api/v1/query_range" \
  --data-urlencode 'query={app="your-app-name"}' \
  --data-urlencode 'limit=10' | jq -r '.data.result[].values[][1]'
```

---

## Step 5: Query Logs in Grafana

### Access Grafana

1. Open browser: `http://10.10.199.26:3000`
2. Login: `admin` / `admin`
3. Click **Explore** (compass icon)
4. Select **Loki** datasource

### Basic Queries

**All logs from your app:**
```logql
{app="your-app-name"}
```

**Logs for specific trace:**
```logql
{app="your-app-name", trace_id="550e8400-e29b-41d4-a716-446655440000"}
```

**Logs from specific queue:**
```logql
{app="your-app-name", queue_name="processing"}
```

**Only errors:**
```logql
{app="your-app-name", level="ERROR"}
```

**Specific event type:**
```logql
{app="your-app-name", event="message_consumed"}
```

### Advanced Queries

**Filter by message content:**
```logql
{app="your-app-name"} |= "order"
```

**Extract JSON fields:**
```logql
{app="your-app-name"} | json | processing_time_ms > 1000
```

**Count by level:**
```logql
sum by (level) (count_over_time({app="your-app-name"}[5m]))
```

**Rate of logs:**
```logql
rate({app="your-app-name"}[1m])
```

---

## Step 6: Use OmniQueue Dashboard

### Access Dashboard

1. Go to **Dashboards** → **Browse**
2. Click **"OmniQueue Distributed Tracing"**

**Or direct URL:**
`http://10.10.199.26:3000/d/1df0b343-f70d-413b-b359-fcb1803739a7/omniqueue-distributed-tracing`

### Dashboard Features

**Filters (top of dashboard):**
- **Application** - Select your app from dropdown
- **Trace ID** - Select specific trace to view journey
- **Queue Name** - Filter by queue (multi-select)

**Panels:**
1. **Trace Journey** - Complete timeline of selected trace across all services
2. **Queue Event Volume** - Graph showing events per queue over time
3. **Processing Time** - Average processing time statistics
4. **Error Rate** - Error count with color-coded thresholds
5. **Recent Events Table** - Latest events by queue

---

## Example Implementation (Java with Quarkus)

### 1. Add Dependencies

```xml
<dependency>
    <groupId>io.quarkus</groupId>
    <artifactId>quarkus-logging-json</artifactId>
</dependency>
```

### 2. Configure Logging (application.properties)

```properties
# JSON logging
quarkus.log.console.json=true
quarkus.log.file.enable=true
quarkus.log.file.path=/var/log/my-app/application.log
quarkus.log.file.format=%d{yyyy-MM-dd HH:mm:ss,SSS} %-5p [%c{3.}] (%t) %s%e%n
quarkus.log.file.rotation.max-file-size=100M
quarkus.log.file.rotation.max-backup-index=5

# Standard fields
quarkus.log.console.json.additional-field."app".value=my-app
quarkus.log.console.json.additional-field."component".value=worker
quarkus.log.console.json.additional-field."environment".value=production
quarkus.log.console.json.additional-field."host".value=${HOSTNAME:localhost}

quarkus.log.file.json=true
quarkus.log.file.json.additional-field."app".value=my-app
quarkus.log.file.json.additional-field."component".value=worker
quarkus.log.file.json.additional-field."environment".value=production
quarkus.log.file.json.additional-field."host".value=${HOSTNAME:localhost}
```

### 3. Use MDC for OmniQueue Fields

```java
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import java.util.UUID;

public class OrderProcessor {
    private static final Logger log = LoggerFactory.getLogger(OrderProcessor.class);

    public void processOrder(String payload, Map<String, Object> metadata) {
        // Extract or generate trace ID
        String traceId = (String) metadata.getOrDefault("trace_id", UUID.randomUUID().toString());

        // Set MDC context
        MDC.put("trace_id", traceId);
        MDC.put("queue_name", "order-processing");
        MDC.put("event", "message_consumed");
        MDC.put("backend", "rabbitmq-memory");

        long startTime = System.currentTimeMillis();

        try {
            log.info("Processing order: {}", payload);

            // Your processing logic here...

            long duration = System.currentTimeMillis() - startTime;
            MDC.put("processing_time_ms", String.valueOf(duration));
            log.info("Order processed successfully");

        } catch (Exception e) {
            log.error("Order processing failed", e);
        } finally {
            MDC.clear();
        }
    }
}
```

---

## Testing Your Integration

### 1. Generate Test Logs

Run your application and generate some activity:

```bash
# Example: Generate 10 test events
for i in {1..10}; do
    curl -X POST http://your-app:8080/api/orders \
      -H "Content-Type: application/json" \
      -d '{"orderId": "ORD-'$i'", "amount": 100}'
    sleep 1
done
```

### 2. Check Promtail Status

```bash
systemctl status promtail
journalctl -u promtail -f
```

### 3. Verify in Loki

```bash
curl -s -G "http://10.10.199.26:3100/loki/api/v1/query" \
  --data-urlencode 'query={app="my-app"}' | jq '.data.result | length'
```

Should return a number > 0.

### 4. View in Grafana

1. Open Grafana Explore
2. Query: `{app="my-app"}`
3. You should see your logs with all fields

### 5. Test Distributed Tracing

1. Generate activity with same trace_id across multiple services
2. Go to OmniQueue dashboard
3. Select your app from "Application" filter
4. Select a trace_id from "Trace ID" filter
5. View the complete trace journey across all services

---

## Troubleshooting

### Logs Not Appearing

**Check Promtail:**
```bash
systemctl status promtail
journalctl -u promtail -n 50
```

**Check log file exists:**
```bash
ls -la /var/log/your-app/
tail -f /var/log/your-app/application.log
```

**Verify JSON format:**
```bash
tail -1 /var/log/your-app/application.log | jq '.'
```

Should parse as valid JSON.

**Test Loki connection:**
```bash
curl -v http://10.10.199.26:3100/ready
```

Should return "ready".

### Labels Not Indexed

Check your Promtail config `labels:` section. Only fields listed there will be indexed.

```yaml
- labels:
    level:
    app:
    trace_id:    # Make sure this is here
    queue_name:  # And this
    event:       # And this
```

Restart Promtail after changes:
```bash
systemctl restart promtail
```

### Dashboard Shows No Data

1. Check datasource is configured: Configuration → Data Sources → Loki
2. Verify URL is `http://localhost:3100`
3. Test connection (should show green checkmark)
4. Check time range in dashboard (top right)
5. Verify your app name in "Application" filter

---

## Performance Guidelines

### Log Volume

- Keep log volume reasonable (<1000 logs/sec per app)
- Use appropriate log levels (DEBUG only in development)
- Avoid logging sensitive data (passwords, tokens, PII)

### Label Cardinality

- Don't use high-cardinality values as labels (like timestamps, IDs)
- Use labels for: app, level, component, trace_id, queue_name, event
- Use JSON fields (not labels) for: user_id, order_id, processing_time_ms

**Good Labels (indexed):**
- `app`, `component`, `environment`, `trace_id`, `queue_name`, `event`, `backend`

**Bad Labels (high cardinality):**
- ❌ `timestamp`, `user_id`, `order_id`, `request_id`
- Use these as JSON fields instead!

### Query Performance

- Always include `app` label in queries: `{app="my-app"}`
- Use time ranges: `{app="my-app"}[1h]`
- Limit results: `{app="my-app"} | limit 100`

---

## Reference

### Container Access

| Service | URL | Purpose |
|---------|-----|---------|
| Grafana | http://10.10.199.26:3000 | UI (admin/admin) |
| Loki API | http://10.10.199.26:3100 | Log ingestion & queries |
| Prometheus | http://10.10.199.26:7330 | Metrics |

### Example Application

See working example at:
- Container: `quarkus-hello-v1` (10.10.199.221)
- Code: `/home/mustafa/telcobright-projects/orchestrix/images/lxc/quarkus-hello/`
- Docs: `/home/mustafa/telcobright-projects/orchestrix/images/lxc/quarkus-hello/OMNIQUEUE_READY.md`

### Documentation

- Loki config: `/home/mustafa/telcobright-projects/orchestrix/images/lxc/grafana-prometheus-loki/OMNIQUEUE_SUPPORT.md`
- LogQL syntax: https://grafana.com/docs/loki/latest/logql/

---

## Success Criteria

Your integration is successful when:

1. ✅ Application outputs JSON logs with all required fields
2. ✅ Promtail is running and shipping logs
3. ✅ Logs appear in Loki API queries
4. ✅ Logs are visible in Grafana Explore
5. ✅ OmniQueue dashboard shows your app
6. ✅ Trace IDs are indexed and searchable
7. ✅ You can trace a request across services

---

## Quick Start Checklist

- [ ] Configure application for JSON logging
- [ ] Add standard fields: timestamp, level, message, app
- [ ] Add OmniQueue fields: trace_id, queue_name, event
- [ ] Install Promtail in container
- [ ] Create `/etc/promtail/promtail-config.yaml`
- [ ] Point Promtail to `http://10.10.199.26:3100/loki/api/v1/push`
- [ ] Configure pipeline_stages for JSON parsing
- [ ] Add labels for: app, trace_id, queue_name, event
- [ ] Start Promtail service
- [ ] Generate test logs
- [ ] Verify in Loki: `curl http://10.10.199.26:3100/loki/api/v1/query`
- [ ] View in Grafana Explore
- [ ] Check OmniQueue dashboard

---

**Need help?** Check the example implementation in `quarkus-hello-v1` container or review the OMNIQUEUE_SUPPORT.md documentation.

**File saved:** `/tmp/shared-instruction/omniqueue-logging-integration-guide.md`
