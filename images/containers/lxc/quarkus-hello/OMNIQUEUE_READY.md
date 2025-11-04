# Quarkus Hello Container - OmniQueue Ready ✅

This container has been updated to meet all OmniQueue distributed tracing requirements.

## Changes Made

### 1. Application Configuration (`app/src/main/resources/application.properties`)

**Added JSON Log Fields for Distributed Tracing:**
```properties
# Additional JSON fields for distributed tracing context
quarkus.log.console.json.additional-field."app".value=quarkus-hello
quarkus.log.console.json.additional-field."component".value=rest-api
quarkus.log.console.json.additional-field."environment".value=production
quarkus.log.console.json.additional-field."host".value=${HOSTNAME:localhost}

quarkus.log.file.json.additional-field."app".value=quarkus-hello
quarkus.log.file.json.additional-field."component".value=rest-api
quarkus.log.file.json.additional-field."environment".value=production
quarkus.log.file.json.additional-field."host".value=${HOSTNAME:localhost}
```

**Increased Log File Size:**
- Changed from: `quarkus.log.file.rotation.max-file-size=10M`
- Changed to: `quarkus.log.file.rotation.max-file-size=100M`

**Enabled Console JSON Logging:**
- Changed from: `quarkus.log.console.json=false`
- Changed to: `quarkus.log.console.json=true`

### 2. Promtail Configuration (`scripts/promtail-config.yaml`)

**Enhanced JSON Field Extraction:**
Added extraction for distributed tracing fields:
- `app` - Application name
- `component` - Component name
- `environment` - Environment (production/staging/dev)
- `host` - Container hostname
- `trace_id` - Distributed trace ID (for OmniQueue)
- `queue_name` - Queue name (for OmniQueue)
- `event` - Event type (for OmniQueue)
- `backend` - Backend type (for OmniQueue)
- `processing_time_ms` - Processing time
- `error` - Error information

**Enhanced Label Indexing:**
Added Loki labels for fast queries:
```yaml
- labels:
    level:
    app:
    component:
    trace_id:         # NEW - Critical for distributed tracing
    queue_name:       # NEW - Query by queue
    event:            # NEW - Query by event type
    backend:          # NEW - Query by backend type
```

### 3. Resource Configuration (`build/build.conf`)

**Increased Memory for OmniQueue Workloads:**
- Changed from: `MEMORY_LIMIT="1GB"`
- Changed to: `MEMORY_LIMIT="2GB"`

## OmniQueue Compatibility Checklist

- [x] Debian 12 base image
- [x] Java 17 (compatible with OmniQueue)
- [x] JSON logging enabled
- [x] Structured log fields: `app`, `component`, `environment`, `host`
- [x] Distributed tracing fields: `trace_id`, `queue_name`, `event`, `backend`
- [x] Promtail configured to extract and index tracing fields
- [x] Log file rotation: 100MB max
- [x] Memory: 2GB (minimum for OmniQueue)
- [x] CPU: 2 cores
- [x] Loki integration ready

## Using with OmniQueue Applications

When deploying an application that uses OmniQueue:

### 1. Application Code

Ensure your application logs include trace context:
```java
Map<String, Object> metadata = new HashMap<>();
metadata.put("trace_id", UUID.randomUUID().toString());
metadata.put("queue_name", "processing");
metadata.put("user_id", "12345");

omniQueue.publish("processing", payload, metadata);
```

### 2. Query Logs in Grafana

**By Trace ID (see entire request journey):**
```logql
{app="quarkus-hello", trace_id="abc-123-xyz"}
```

**By Queue:**
```logql
{app="quarkus-hello", queue_name="processing"}
```

**By Event:**
```logql
{app="quarkus-hello", event="message_published"}
```

**Performance Analysis:**
```logql
{app="quarkus-hello"} | json | processing_time_ms > 1000
```

## Container Details

**Build Info:**
- Version: 1
- Size: 338M
- Storage: BTRFS with 10G quota
- Image: `quarkus-hello-v1-*.tar.gz`

**Resource Limits:**
- Memory: 2GB
- CPU: 2 cores
- Log file size: 100MB per file
- Log retention: 5 backup files

**Network Ports:**
- Application: 8080
- Promtail metrics: 9080

**Services:**
- Quarkus Hello (systemd: quarkus-hello.service)
- Promtail (systemd: promtail.service)

## Log Fields Reference

All logs now include these fields in JSON format:

**Standard Fields:**
- `timestamp` - Log timestamp (RFC3339)
- `level` - Log level (INFO, WARN, ERROR, DEBUG)
- `logger` - Logger name
- `message` - Log message

**Context Fields:**
- `app` - Application name ("quarkus-hello")
- `component` - Component name ("rest-api")
- `environment` - Environment ("production")
- `host` - Container hostname

**Distributed Tracing Fields (OmniQueue):**
- `trace_id` - Distributed trace ID
- `queue_name` - Message queue name
- `event` - Event type (message_published, message_consumed, etc.)
- `backend` - Backend type (rabbitmq-memory, redis, kafka)
- `processing_time_ms` - Processing duration
- `error` - Error details if any

## Testing

Generate test requests:
```bash
# Test application
curl http://CONTAINER_IP:8080/hello?name=Test

# View logs in Loki
curl -G "http://LOKI_IP:3100/loki/api/v1/query_range" \
  --data-urlencode 'query={app="quarkus-hello"}' \
  --data-urlencode 'limit=10'
```

## References

- OmniQueue Requirements: `/tmp/shared-instruction/omniqueue-lxc-container-packaging.md`
- Build Configuration: `build/build.conf`
- Promtail Configuration: `scripts/promtail-config.yaml`
- Application Configuration: `app/src/main/resources/application.properties`
