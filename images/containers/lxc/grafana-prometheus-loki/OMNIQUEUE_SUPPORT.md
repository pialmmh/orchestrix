# Grafana-Loki Container - OmniQueue Distributed Tracing Support

This Grafana-Loki container is now configured as a **centralized log aggregation and distributed tracing system** for ALL applications using OmniQueue or requiring distributed tracing capabilities.

## Overview

The container supports high-cardinality labels and distributed tracing fields, allowing you to:
- Track requests across multiple services using `trace_id`
- Query logs by queue name, event type, backend, and custom labels
- Visualize distributed traces in Grafana dashboards
- Analyze performance metrics and error rates

## Supported Distributed Tracing Labels

### Core Tracing Labels (Indexed for Fast Queries)

The following labels are automatically indexed by Loki for fast queries:

| Label | Description | Example |
|-------|-------------|---------|
| `trace_id` | Unique identifier for distributed trace | `550e8400-e29b-41d4-a716-446655440000` |
| `queue_name` | Message queue name | `processing`, `notifications`, `analytics` |
| `event` | Event type | `message_published`, `message_consumed`, `processing_complete` |
| `backend` | Backend type | `rabbitmq-memory`, `redis`, `kafka` |
| `app` | Application name | `order-service`, `payment-service` |
| `component` | Component within app | `rest-api`, `background-worker`, `scheduler` |
| `environment` | Environment | `production`, `staging`, `development` |
| `host` | Container/host identifier | `order-service-1`, `worker-pod-xyz` |
| `level` | Log level | `INFO`, `WARN`, `ERROR`, `DEBUG` |

### Additional JSON Fields (Not Indexed, But Queryable)

These fields can be included in logs and queried using LogQL's `json` filter:

- `processing_time_ms` - Processing duration in milliseconds
- `user_id` - User identifier
- `order_id` - Business entity identifier
- `error` - Error details
- `retry_count` - Number of retries
- Any custom fields your application needs

## Configuration Limits

The Loki configuration has been enhanced to support high-cardinality distributed tracing:

```yaml
limits_config:
  # Unlimited streams for high cardinality
  max_streams_per_user: 0
  max_global_streams_per_user: 0

  # Support for many labels
  max_label_names_per_series: 30
  max_label_value_length: 2048  # Long trace IDs
  max_label_name_length: 1024

  # Query performance
  max_chunks_per_query: 2000000
  max_query_series: 500

  # Query result caching enabled (500MB, 24h validity)
```

## How Applications Should Send Logs

### Method 1: Promtail with JSON Logs (Recommended)

**Step 1**: Configure your application to log in JSON format with distributed tracing fields:

```json
{
  "timestamp": "2025-10-11T10:30:45.123Z",
  "level": "INFO",
  "message": "Processing order",
  "app": "order-service",
  "component": "order-processor",
  "environment": "production",
  "host": "order-service-pod-1",
  "trace_id": "550e8400-e29b-41d4-a716-446655440000",
  "queue_name": "order-processing",
  "event": "message_consumed",
  "backend": "rabbitmq-memory",
  "processing_time_ms": 150,
  "order_id": "ORD-12345",
  "user_id": "USR-98765"
}
```

**Step 2**: Configure Promtail to extract and label these fields:

```yaml
scrape_configs:
  - job_name: my-app
    static_configs:
      - targets:
          - localhost
        labels:
          job: my-app
          __path__: /var/log/my-app/*.log

    pipeline_stages:
      - json:
          expressions:
            timestamp: timestamp
            level: level
            message: message
            app: app
            component: component
            environment: environment
            host: host
            trace_id: trace_id
            queue_name: queue_name
            event: event
            backend: backend
            processing_time_ms: processing_time_ms
            # Add any custom fields here

      - timestamp:
          source: timestamp
          format: RFC3339

      - labels:
          level:
          app:
          component:
          environment:
          host:
          trace_id:
          queue_name:
          event:
          backend:

      - output:
          source: message
```

**Step 3**: Point Promtail to this Grafana-Loki container:

```yaml
clients:
  - url: http://<LOKI_IP>:3100/loki/api/v1/push
```

### Method 2: Direct API Push

Send logs directly to Loki API (for applications without Promtail):

```bash
curl -X POST http://<LOKI_IP>:3100/loki/api/v1/push \
  -H "Content-Type: application/json" \
  -d '{
    "streams": [
      {
        "stream": {
          "app": "order-service",
          "trace_id": "550e8400-e29b-41d4-a716-446655440000",
          "queue_name": "order-processing",
          "event": "message_consumed",
          "level": "INFO"
        },
        "values": [
          ["'"$(date +%s)000000000"'", "Processing order ORD-12345"]
        ]
      }
    ]
  }'
```

## Querying Distributed Traces

### Basic Trace Queries

**Find all logs for a specific trace:**
```logql
{trace_id="550e8400-e29b-41d4-a716-446655440000"}
```

**Find traces in a specific queue:**
```logql
{queue_name="order-processing"}
```

**Find specific events:**
```logql
{event="message_consumed"}
```

**Find errors in a trace:**
```logql
{trace_id="550e8400-e29b-41d4-a716-446655440000", level="ERROR"}
```

### Advanced Queries with JSON Filtering

**Find slow operations (>1 second):**
```logql
{app="order-service"} | json | processing_time_ms > 1000
```

**Find traces for specific user:**
```logql
{app="order-service"} | json | user_id = "USR-98765"
```

**Calculate average processing time:**
```logql
avg_over_time({app="order-service"} | json | processing_time_ms > 0 [5m])
```

**Count events by queue:**
```logql
sum by (queue_name, event) (count_over_time({app="order-service"}[1m]))
```

### Multi-Service Trace Queries

**Track a request across multiple services:**
```logql
{trace_id="550e8400-e29b-41d4-a716-446655440000"} | json | line_format "{{.timestamp}} | {{.app}} | {{.component}} | {{.event}} | {{.message}}"
```

**Find errors across all services in a trace:**
```logql
{trace_id="550e8400-e29b-41d4-a716-446655440000", level="ERROR"} | json
```

## Using the OmniQueue Dashboard

A pre-configured Grafana dashboard for distributed tracing is available.

### Dashboard Features

1. **Variables (filters)**:
   - `$app` - Select application
   - `$trace_id` - Select specific trace
   - `$queue_name` - Filter by queue (multi-select)

2. **Panels**:
   - **Trace Journey**: Complete log timeline for selected trace
   - **Queue Event Volume**: Graph showing events per queue over time
   - **Processing Time**: Average processing time statistics
   - **Error Rate**: Error count with color thresholds
   - **Recent Events Table**: Latest events by queue

### Accessing the Dashboard

The dashboard is automatically imported during container build:
1. Open Grafana: `http://<GRAFANA_IP>:3000`
2. Navigate to **Dashboards** → **Browse**
3. Select **OmniQueue Distributed Tracing**
4. Use variables at top to filter by app, trace, queue

## Integration Examples

### Java (Quarkus/Spring Boot) with SLF4J

```java
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

public class OrderService {
    private static final Logger log = LoggerFactory.getLogger(OrderService.class);

    public void processOrder(Order order, String traceId) {
        // Set trace context
        MDC.put("trace_id", traceId);
        MDC.put("queue_name", "order-processing");
        MDC.put("event", "message_consumed");
        MDC.put("backend", "rabbitmq-memory");
        MDC.put("order_id", order.getId());

        long startTime = System.currentTimeMillis();

        try {
            log.info("Processing order: {}", order.getId());
            // ... processing logic ...

            long processingTime = System.currentTimeMillis() - startTime;
            MDC.put("processing_time_ms", String.valueOf(processingTime));
            log.info("Order processed successfully");
        } catch (Exception e) {
            log.error("Order processing failed", e);
        } finally {
            MDC.clear();
        }
    }
}
```

Configure Logback/Log4j2 for JSON output with MDC fields.

### Python with structlog

```python
import structlog
import uuid

log = structlog.get_logger()

def process_order(order, trace_id=None):
    if not trace_id:
        trace_id = str(uuid.uuid4())

    log = log.bind(
        trace_id=trace_id,
        queue_name="order-processing",
        event="message_consumed",
        backend="rabbitmq-memory",
        order_id=order.id
    )

    import time
    start_time = time.time()

    try:
        log.info("Processing order", order_id=order.id)
        # ... processing logic ...

        processing_time_ms = int((time.time() - start_time) * 1000)
        log.info("Order processed successfully",
                processing_time_ms=processing_time_ms)
    except Exception as e:
        log.error("Order processing failed", error=str(e))
```

Configure structlog with JSON processor.

### Node.js with Winston

```javascript
const winston = require('winston');
const { v4: uuidv4 } = require('uuid');

const logger = winston.createLogger({
  format: winston.format.json(),
  defaultMeta: {
    app: 'order-service',
    component: 'api',
    environment: 'production'
  },
  transports: [
    new winston.transports.File({ filename: '/var/log/app/order-service.log' })
  ]
});

async function processOrder(order, traceId = null) {
  traceId = traceId || uuidv4();
  const startTime = Date.now();

  const logContext = {
    trace_id: traceId,
    queue_name: 'order-processing',
    event: 'message_consumed',
    backend: 'rabbitmq-memory',
    order_id: order.id
  };

  try {
    logger.info('Processing order', logContext);
    // ... processing logic ...

    const processingTime = Date.now() - startTime;
    logger.info('Order processed successfully', {
      ...logContext,
      processing_time_ms: processingTime
    });
  } catch (error) {
    logger.error('Order processing failed', {
      ...logContext,
      error: error.message
    });
  }
}
```

## Performance Considerations

### Query Performance Tips

1. **Always use indexed labels** in query selectors:
   ```logql
   {app="order-service", trace_id="xyz"}  # Fast
   {app="order-service"} | json | order_id = "xyz"  # Slower
   ```

2. **Use specific time ranges** to limit data scanned:
   ```logql
   {trace_id="xyz"}[1h]  # Last hour only
   ```

3. **Leverage query caching** - identical queries within 24h use cache

4. **Limit result sets** when exploring:
   ```bash
   curl -G "http://<LOKI_IP>:3100/loki/api/v1/query_range" \
     --data-urlencode 'query={app="order-service"}' \
     --data-urlencode 'limit=100'
   ```

### Retention and Storage

Current configuration:
- **Retention period**: 720h (30 days) - configurable in `build.conf`
- **Retention delete delay**: 2h - configurable
- **Compaction interval**: 10 minutes
- **Storage**: Filesystem with BTRFS

To adjust retention, edit `build/build.conf`:
```bash
LOKI_RETENTION_PERIOD="720h"  # Change to desired value
LOKI_RETENTION_DELETE_DELAY="2h"
```

Then rebuild the container.

## Troubleshooting

### Logs not appearing in Loki

1. **Check Promtail is running** in application container:
   ```bash
   systemctl status promtail
   ```

2. **Verify Promtail can reach Loki**:
   ```bash
   curl http://<LOKI_IP>:3100/ready
   ```

3. **Check Promtail logs** for errors:
   ```bash
   journalctl -u promtail -n 50
   ```

4. **Verify log format** - must be valid JSON if using json pipeline

### Labels not indexed

1. Ensure labels are defined in Promtail's `pipeline_stages` → `labels` section
2. Label names must match exactly (case-sensitive)
3. Check Loki limits - max 30 labels per series

### High cardinality warnings

If you see "maximum streams limit exceeded":
1. This container has `max_streams_per_user: 0` (unlimited)
2. Check if you're creating labels from high-cardinality values (e.g., timestamps)
3. Use JSON fields instead of labels for high-cardinality data

### Query timeouts

1. Reduce time range: `[1h]` instead of `[24h]`
2. Add more specific label selectors
3. Use `limit` parameter to reduce result size
4. Check if query cache is enabled (it is by default)

## Reference: Example Quarkus Hello Container

See `/home/mustafa/telcobright-projects/orchestrix/images/lxc/quarkus-hello/OMNIQUEUE_READY.md` for a complete example of an OmniQueue-ready application container with:
- JSON logging configuration
- Promtail setup with label extraction
- Example Java code with trace context

## Container Details

**Grafana-Loki Container Specs**:
- **Grafana**: 12.2.0 (port 3000)
- **Loki**: 2.9.4 (port 3100)
- **Promtail**: 2.9.4 (example in quarkus-hello)
- **Nginx**: For log viewer proxy (port 8080)
- **Storage**: BTRFS with quota
- **Base Image**: Debian 12

**Services**:
- `grafana-server.service` - Grafana UI
- `loki.service` - Log aggregation backend
- `nginx.service` - Reverse proxy

**Configuration Files**:
- Loki: `/etc/loki/loki-config.yaml`
- Grafana datasource: `/etc/grafana/provisioning/datasources/loki.yaml`
- Dashboard: `/etc/grafana/provisioning/dashboards/omniqueue-dashboard.json`

## Additional Resources

**Query Language**:
- LogQL documentation: https://grafana.com/docs/loki/latest/logql/

**API Reference**:
- Query API: `http://<LOKI_IP>:3100/loki/api/v1/query_range`
- Labels API: `http://<LOKI_IP>:3100/loki/api/v1/labels`
- Push API: `http://<LOKI_IP>:3100/loki/api/v1/push`

**Simple Log Viewer**:
- Built-in viewer: `http://<LOKI_IP>:8080/logs.html`
- 20 logs per page, pagination, auto-refresh every 30s
