# Orchestrix Observability Stack

Complete observability solution for Orchestrix using Grafana Loki Stack.

## Components

- **Loki**: Log aggregation system
- **Prometheus**: Metrics collection and alerting
- **Grafana**: Visualization and dashboards
- **Tempo**: Distributed tracing
- **Promtail**: Log collector agent
- **Node Exporter**: System metrics exporter

## Quick Start

```bash
# Start the stack
./build.sh up

# View status
./build.sh status

# View logs
./build.sh logs

# Stop the stack
./build.sh down

# Restart the stack
./build.sh restart

# Clean everything (WARNING: Deletes all data)
./build.sh clean
```

## Access Points

| Service | URL | Credentials |
|---------|-----|-------------|
| Grafana | http://localhost:3000 | admin / orchestrix123 |
| Loki API | http://localhost:3100 | - |
| Prometheus | http://localhost:9090 | - |
| Tempo | http://localhost:3200 | - |

## Features

### Log Aggregation
- Collects logs from Docker containers
- System logs monitoring
- Orchestrix application logs
- Structured log parsing with labels

### Metrics Collection
- Host system metrics (CPU, Memory, Disk, Network)
- Container metrics
- Service health monitoring
- Custom application metrics support

### Distributed Tracing
- OpenTelemetry support (OTLP)
- Jaeger compatibility
- Trace to logs correlation

### Dashboards
- Pre-configured Orchestrix Overview dashboard
- Service health monitoring
- Log rate analysis
- System resource utilization

### Alerting
- CPU usage alerts
- Memory usage alerts
- Disk space alerts
- Service availability alerts
- Log ingestion rate monitoring

## Configuration

### Adding Custom Log Sources

Edit `configs/promtail-config.yaml` to add new log sources:

```yaml
- job_name: my_application
  static_configs:
    - targets:
        - localhost
      labels:
        job: myapp
        __path__: /path/to/logs/*.log
```

### Adding Metrics Endpoints

Edit `configs/prometheus.yml` to add new metrics targets:

```yaml
- job_name: 'my-service'
  static_configs:
    - targets: ['localhost:8080']
      labels:
        instance: 'my-service-1'
```

### Custom Dashboards

Place new dashboard JSON files in `grafana/dashboards/` and restart Grafana.

## Sending Application Logs

### From Docker Containers
Logs are automatically collected from all Docker containers.

### From Applications
Configure your application to write logs to `/var/log/orchestrix/` or send directly to Loki:

```bash
curl -X POST http://localhost:3100/loki/api/v1/push \
  -H "Content-Type: application/json" \
  -d '{
    "streams": [
      {
        "stream": {
          "job": "myapp",
          "level": "info"
        },
        "values": [
          ["1234567890000000000", "Log message here"]
        ]
      }
    ]
  }'
```

## Sending Metrics

Push metrics to Prometheus using the Pushgateway or expose a `/metrics` endpoint.

## Storage

Data is persisted in Docker volumes:
- `orchestrix-loki-data`: Log data
- `orchestrix-prometheus-data`: Metrics data
- `orchestrix-grafana-data`: Dashboards and settings
- `orchestrix-tempo-data`: Trace data

## Troubleshooting

### Port Conflicts
If ports are already in use, stop conflicting services or modify port mappings in `docker-compose.yml`.

### High Memory Usage
Adjust retention policies in configuration files:
- Loki: `configs/loki-config.yaml`
- Prometheus: `configs/prometheus.yml`

### Missing Logs
Check Promtail configuration and ensure log files have proper permissions.

## Integration with Orchestrix

The stack is pre-configured to monitor Orchestrix components:
- LXC container logs
- Application metrics
- System performance
- Service health

To enable monitoring for a new Orchestrix component:
1. Configure the component to export metrics
2. Add log paths to Promtail configuration
3. Update Prometheus scrape configs
4. Create custom dashboards in Grafana

## Backup and Restore

### Backup
```bash
docker run --rm -v orchestrix-grafana-data:/data -v $(pwd):/backup alpine tar czf /backup/grafana-backup.tar.gz -C /data .
docker run --rm -v orchestrix-prometheus-data:/data -v $(pwd):/backup alpine tar czf /backup/prometheus-backup.tar.gz -C /data .
```

### Restore
```bash
docker run --rm -v orchestrix-grafana-data:/data -v $(pwd):/backup alpine tar xzf /backup/grafana-backup.tar.gz -C /data
docker run --rm -v orchestrix-prometheus-data:/data -v $(pwd):/backup alpine tar xzf /backup/prometheus-backup.tar.gz -C /data
```

## Resources

- [Loki Documentation](https://grafana.com/docs/loki/latest/)
- [Prometheus Documentation](https://prometheus.io/docs/)
- [Grafana Documentation](https://grafana.com/docs/grafana/latest/)
- [Tempo Documentation](https://grafana.com/docs/tempo/latest/)