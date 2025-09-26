# Grafana-Loki Container

Production-ready LXC container with Grafana, Loki, and Promtail for log aggregation and visualization.

## Features

- **Grafana** 10.2.3 - Visualization and dashboarding
- **Loki** 2.9.4 - Log aggregation system
- **Promtail** 2.9.4 - Log collector agent
- **BTRFS Storage** - Mandatory storage backend with quota management
- **Version Management** - Support for multiple versions
- **Automatic Integration** - Loki pre-configured as Grafana datasource
- **Snapshot Support** - Automatic snapshots with retention
- **Compression** - BTRFS compression for efficient storage

## Storage Requirements

### Minimum Requirements
- **Storage Type**: BTRFS (mandatory)
- **Minimum Quota**: 10GB (development)
- **Recommended Quota**: 30GB (production)
- **Storage Location**: SSD recommended for performance

### Storage Sizing Guide

| Environment | Quota | Daily Logs | Retention | Use Case |
|------------|-------|------------|-----------|----------|
| Development | 10GB | < 500MB | 7 days | Testing, small datasets |
| Staging | 20GB | 1-2GB | 14 days | Pre-production testing |
| Production (Small) | 30GB | 2-5GB | 30 days | Small applications |
| Production (Medium) | 50GB | 5-10GB | 30 days | Medium applications |
| Production (Large) | 100GB+ | 10GB+ | 30+ days | Large deployments |

## Quick Start

```bash
# Make scripts executable
chmod +x *.sh

# Run with defaults (builds if needed)
./startDefault.sh
```

Access Grafana at: http://localhost:3000 (admin/admin)

## Building the Container

### With Custom Configuration

```bash
# Create configuration file
cp sample-config.conf my-config.conf
# Edit my-config.conf as needed

# Build container
./buildGrafanaLoki.sh my-config.conf
```

### Configuration Options

Required parameters:
- `storage.location.id` - BTRFS storage location
- `storage.quota.size` - Disk quota (e.g., 30G)

Optional parameters:
- `grafana.version` - Grafana version (default: 10.2.3)
- `loki.version` - Loki version (default: 2.9.4)
- `grafana.port` - Grafana port (default: 3000)
- `loki.port` - Loki port (default: 3100)

## Launching from Image

```bash
# Create launch configuration
cat > launch.conf << EOF
container.name=grafana-loki-prod
container.version=1.0.0
image.path=/tmp/grafana-loki-v1.0.0.tar.gz
storage.location.id=btrfs_ssd_main
storage.quota.size=30G
grafana.port=3000
loki.port=3100
EOF

# Launch container
./launchGrafanaLoki.sh launch.conf
```

## Version Management

### Current Version
- Container: 1.0.0
- Grafana: 10.2.3
- Loki: 2.9.4
- Promtail: 2.9.4

### Building Specific Versions

```bash
# Build version 1.0.0
version=1.0.0 version_tag=v1.0.0 ./buildGrafanaLoki.sh

# Build latest development
version=1.1.0-dev version_tag=dev ./buildGrafanaLoki.sh
```

### Version History
See `versions.conf` for complete version history and compatibility matrix.

## Service Endpoints

### Grafana (Port 3000)
- URL: http://localhost:3000
- Default credentials: admin/admin
- Features:
  - Dashboards and visualization
  - Loki datasource pre-configured
  - Explore view for log analysis

### Loki API (Port 3100)
- URL: http://localhost:3100
- Endpoints:
  - `/ready` - Readiness check
  - `/metrics` - Prometheus metrics
  - `/loki/api/v1/push` - Push logs
  - `/loki/api/v1/query` - Query logs

### Promtail (Port 9080)
- URL: http://localhost:9080
- Endpoints:
  - `/ready` - Readiness check
  - `/metrics` - Prometheus metrics
  - `/targets` - Scrape targets

## Storage Management

### BTRFS Operations

#### Check Storage Usage
```bash
sudo btrfs qgroup show /path/to/storage/location
```

#### Create Manual Snapshot
```bash
sudo btrfs subvolume snapshot -r \
  /path/to/storage/containers/grafana-loki \
  /path/to/storage/snapshots/grafana-loki/manual_$(date +%Y%m%d)
```

#### Restore from Snapshot
```bash
# Stop container
lxc stop grafana-loki-01

# Restore snapshot
sudo btrfs subvolume delete /path/to/storage/containers/grafana-loki
sudo btrfs subvolume snapshot \
  /path/to/storage/snapshots/grafana-loki/backup_20240115 \
  /path/to/storage/containers/grafana-loki

# Start container
lxc start grafana-loki-01
```

### Monitoring Storage

```bash
# Check quota usage
lxc exec grafana-loki-01 -- df -h /

# Check Loki storage
lxc exec grafana-loki-01 -- du -sh /var/lib/loki

# Check Grafana storage
lxc exec grafana-loki-01 -- du -sh /var/lib/grafana
```

## Container Management

### Access Container
```bash
lxc exec grafana-loki-01 -- /bin/bash
```

### View Logs
```bash
# Grafana logs
lxc exec grafana-loki-01 -- journalctl -u grafana-server -f

# Loki logs
lxc exec grafana-loki-01 -- journalctl -u loki -f

# Promtail logs
lxc exec grafana-loki-01 -- journalctl -u promtail -f
```

### Service Management
```bash
# Restart services
lxc exec grafana-loki-01 -- systemctl restart grafana-server
lxc exec grafana-loki-01 -- systemctl restart loki
lxc exec grafana-loki-01 -- systemctl restart promtail

# Check service status
lxc exec grafana-loki-01 -- systemctl status grafana-server loki promtail
```

### Stop/Start Container
```bash
# Stop
lxc stop grafana-loki-01

# Start
lxc start grafana-loki-01

# Restart
lxc restart grafana-loki-01
```

## Configuration Files

### Loki Configuration
Location: `/etc/loki/config.yaml`
- Storage: Local filesystem
- Retention: 30 days default
- Replication: Disabled (single instance)

### Promtail Configuration
Location: `/etc/loki/promtail.yaml`
- Targets: System logs (`/var/log/*log`)
- Push to: http://localhost:3100

### Grafana Configuration
Location: `/etc/grafana/grafana.ini`
- Default settings
- Loki datasource: `/etc/grafana/provisioning/datasources/loki.yaml`

## Customization

### Add External Log Sources

```bash
# Mount external log directory
lxc config device add grafana-loki-01 logs disk \
  source=/host/path/to/logs \
  path=/external-logs

# Update Promtail config to scrape external logs
lxc exec grafana-loki-01 -- vi /etc/loki/promtail.yaml
# Add new job under scrape_configs

# Restart Promtail
lxc exec grafana-loki-01 -- systemctl restart promtail
```

### Install Grafana Plugins

```bash
# Install plugin
lxc exec grafana-loki-01 -- grafana-cli plugins install <plugin-id>

# Restart Grafana
lxc exec grafana-loki-01 -- systemctl restart grafana-server
```

### Configure Retention

Edit Loki config:
```bash
lxc exec grafana-loki-01 -- vi /etc/loki/config.yaml
# Modify retention settings

# Restart Loki
lxc exec grafana-loki-01 -- systemctl restart loki
```

## Troubleshooting

### Services Not Starting

```bash
# Check service logs
lxc exec grafana-loki-01 -- journalctl -xe

# Check disk space
lxc exec grafana-loki-01 -- df -h

# Check service status
lxc exec grafana-loki-01 -- systemctl status grafana-server loki promtail
```

### Storage Issues

```bash
# Check BTRFS quota
sudo btrfs qgroup show /path/to/storage

# Check if quota exceeded
lxc exec grafana-loki-01 -- du -sh /*

# Increase quota if needed
sudo btrfs qgroup limit 50G /path/to/storage/containers/grafana-loki
```

### Network Issues

```bash
# Check port bindings
lxc config device show grafana-loki-01

# Check if ports are listening
lxc exec grafana-loki-01 -- netstat -tlpn

# Test connectivity
curl http://localhost:3000/api/health
curl http://localhost:3100/ready
```

### Performance Issues

1. Check resource usage:
```bash
lxc info grafana-loki-01
```

2. Increase resources:
```bash
lxc config set grafana-loki-01 limits.memory 4GB
lxc config set grafana-loki-01 limits.cpu 4
```

3. Enable compression if not enabled:
```bash
sudo btrfs property set /path/to/volume compression lzo
```

## Backup and Recovery

### Backup Procedure

1. Create snapshot:
```bash
sudo btrfs subvolume snapshot -r \
  /path/to/storage/containers/grafana-loki \
  /path/to/storage/snapshots/grafana-loki/backup_$(date +%Y%m%d)
```

2. Export container:
```bash
lxc export grafana-loki-01 grafana-loki-backup.tar.gz
```

3. Backup critical configs:
```bash
lxc exec grafana-loki-01 -- tar czf /tmp/configs.tar.gz \
  /etc/grafana /etc/loki /var/lib/grafana/grafana.db
lxc file pull grafana-loki-01/tmp/configs.tar.gz ./
```

### Recovery Procedure

1. Import container:
```bash
lxc import grafana-loki-backup.tar.gz
```

2. Or restore from snapshot:
```bash
sudo btrfs subvolume snapshot \
  /path/to/storage/snapshots/grafana-loki/backup_20240115 \
  /path/to/storage/containers/grafana-loki-recovered
```

3. Start container:
```bash
lxc start grafana-loki-01
```

## Security Considerations

1. **Change Default Passwords**: Change Grafana admin password on first login
2. **Network Isolation**: Use private network bridge for production
3. **Resource Limits**: Set appropriate CPU/memory limits
4. **Storage Quotas**: Enforce quotas to prevent disk exhaustion
5. **Regular Updates**: Keep services updated for security patches

## Performance Tuning

### Loki Optimization
- Adjust chunk_idle_period for write efficiency
- Configure max_chunk_age for query performance
- Set appropriate retention periods

### Grafana Optimization
- Enable query caching
- Optimize dashboard refresh rates
- Limit concurrent queries

### Storage Optimization
- Enable BTRFS compression
- Regular cleanup of old logs
- Monitor snapshot space usage

## Integration Examples

### Send Application Logs to Loki

```bash
# Install Promtail on application server
# Configure to push to Loki endpoint
# Example Promtail config:
clients:
  - url: http://grafana-loki-server:3100/loki/api/v1/push

scrape_configs:
  - job_name: myapp
    static_configs:
      - targets:
          - localhost
        labels:
          app: myapp
          __path__: /var/log/myapp/*.log
```

### Query Logs via API

```bash
# Query recent logs
curl -G -s "http://localhost:3100/loki/api/v1/query_range" \
  --data-urlencode 'query={job="varlogs"}' \
  --data-urlencode 'start=2024-01-15T00:00:00Z' \
  --data-urlencode 'end=2024-01-15T23:59:59Z'
```

## Support

For issues or questions:
1. Check logs: `journalctl -xe`
2. Verify storage: `btrfs qgroup show`
3. Check service status: `systemctl status`
4. Review configuration files in `/etc/`

## License

Components included:
- Grafana: Apache License 2.0
- Loki: Apache License 2.0
- Promtail: Apache License 2.0
- Container scripts: Project license