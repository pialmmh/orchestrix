# Clarity LXC Container - Prometheus & Grafana Monitoring Stack

## Overview

The Clarity container is a pre-configured LXC container that provides a complete monitoring solution with Prometheus and Grafana. It's designed to be easily deployable, with persistent storage for configurations and data on the host system.

## What It Does

The `buildClarity.sh` script automates the entire process of:

1. **Container Creation**: Creates a Debian 12 (64-bit) based LXC container
2. **Software Installation**: Installs and configures Prometheus and Grafana
3. **Integration**: Automatically configures Grafana to use Prometheus as its data source
4. **Persistence**: Sets up bind mounts for configuration and data directories
5. **Backup**: Creates a portable tar.gz backup of the container
6. **Service Management**: Configures systemd services for automatic startup

## Prerequisites

- Ubuntu/Debian-based Linux system (host machine)
- Root or sudo access
- Internet connection for downloading packages
- At least 2GB free disk space
- At least 1GB RAM available for the container

## Quick Start

### 1. Basic Installation

```bash
# Navigate to the clarity directory
cd ~/telcobright-projects/orchestrix/images/lxc/clarity

# Run the build script
sudo bash buildClarity.sh
```

### 2. Overwrite Existing Container

If you need to rebuild the container from scratch:

```bash
# This will delete the existing container and rebuild it
sudo bash buildClarity.sh --overwrite
# or use the short form
sudo bash buildClarity.sh -o
```

### 3. View Help

```bash
bash buildClarity.sh --help
```

## Directory Structure

After installation, the following directories are created on the host:

```
/opt/orchestrix/deployments/clarity/
├── prometheus/         # Prometheus configuration files
│   └── prometheus.yml  # Main Prometheus config
└── grafana/           # Grafana configuration files
    └── datasources.yml # Prometheus datasource config

/data/orchestrix/clarity/
├── prometheus/        # Prometheus time-series data
└── grafana/          # Grafana dashboards and database
```

## Container Details

### Installed Software

| Component | Description | Default Port |
|-----------|-------------|--------------|
| Prometheus | Time-series database and monitoring system | 9090 |
| Grafana | Visualization and analytics platform | 3300 |

### Default Credentials

- **Grafana Login**: admin / admin (you'll be prompted to change on first login)
- **Prometheus**: No authentication by default

### Network Configuration

The container uses bridge networking by default. To find the container's IP address:

```bash
# List container with IP
lxc list clarity

# Or get just the IP
lxc list clarity --format=json | jq -r '.[0].state.network.eth0.addresses[] | select(.family=="inet").address'
```

## Usage Guide

### Accessing the Services

After the container is built and running, you can access:

1. **Prometheus**: `http://<container-ip>:9090`
2. **Grafana**: `http://<container-ip>:3300`

Replace `<container-ip>` with the actual IP address of your container.

### Managing the Container

```bash
# Start the container
lxc start clarity

# Stop the container
lxc stop clarity

# Restart the container
lxc restart clarity

# View container status
lxc info clarity

# Access container shell
lxc exec clarity -- bash

# View container logs
lxc console clarity --show-log
```

### Configuration Management

#### Prometheus Configuration

Edit the Prometheus configuration on the host:

```bash
sudo nano /opt/orchestrix/deployments/clarity/prometheus/prometheus.yml
```

Then restart Prometheus inside the container:

```bash
lxc exec clarity -- systemctl restart prometheus
```

#### Grafana Configuration

Grafana configurations are stored at:
- Host: `/opt/orchestrix/deployments/clarity/grafana/`
- Container: `/etc/grafana/config/`

The datasource configuration is automatically set up to use Prometheus.

### Adding Monitoring Targets

To add new targets for Prometheus to scrape:

1. Edit the Prometheus configuration:
```yaml
# /opt/orchestrix/deployments/clarity/prometheus/prometheus.yml
scrape_configs:
  - job_name: 'my-application'
    static_configs:
      - targets: ['192.168.1.100:8080']
```

2. Restart Prometheus:
```bash
lxc exec clarity -- systemctl restart prometheus
```

### Backup and Restore

#### Backup Location

The build script automatically creates a backup after successful container creation:
- Location: `./clarity-YYYYMMDD-HHMMSS.tar.gz`
- Metadata: `./clarity-YYYYMMDD-HHMMSS.tar.gz.info`

#### Manual Backup

```bash
# Stop and export container
lxc stop clarity
lxc export clarity clarity-backup.tar.gz
lxc start clarity
```

#### Restore from Backup

```bash
# Import container from backup
lxc import clarity-backup.tar.gz
lxc start clarity
```

## Troubleshooting

### Container Won't Start

```bash
# Check container status
lxc info clarity

# View error logs
lxc console clarity --show-log

# Check LXD service
systemctl status snap.lxd.daemon
```

### Services Not Running

```bash
# Check Prometheus status
lxc exec clarity -- systemctl status prometheus

# Check Grafana status
lxc exec clarity -- systemctl status grafana-server

# View service logs
lxc exec clarity -- journalctl -u prometheus -n 50
lxc exec clarity -- journalctl -u grafana-server -n 50
```

### Permission Issues

If you encounter permission errors:

```bash
# Fix Prometheus permissions
lxc exec clarity -- chown -R prometheus:prometheus /var/lib/prometheus
lxc exec clarity -- chown -R prometheus:prometheus /etc/prometheus

# Fix Grafana permissions
lxc exec clarity -- chown -R grafana:grafana /var/lib/grafana
lxc exec clarity -- chown -R grafana:grafana /etc/grafana
```

### Network Issues

```bash
# Check container network
lxc network list
lxc network show lxdbr0

# Restart container networking
lxc restart clarity
```

## Advanced Configuration

### Resource Limits

Set CPU and memory limits for the container:

```bash
# Set memory limit to 2GB
lxc config set clarity limits.memory 2GB

# Set CPU limit to 2 cores
lxc config set clarity limits.cpu 2

# View current limits
lxc config show clarity
```

### Autostart Configuration

The container is configured to autostart with the host system. To modify:

```bash
# Disable autostart
lxc config set clarity boot.autostart false

# Enable autostart with delay
lxc config set clarity boot.autostart true
lxc config set clarity boot.autostart.delay 10
```

### Custom Bind Mounts

To add additional bind mounts:

```bash
# Add a custom directory mount
lxc config device add clarity mydata disk \
  source=/host/path/to/data \
  path=/container/path/to/data
```

## Integration with Orchestrix

This container is designed to work with the Orchestrix configuration management system:

1. **Artifact Management**: The container can be exported as an artifact and stored in repositories
2. **Configuration Profiles**: Different configurations for dev/staging/production environments
3. **Deployment Automation**: Can be deployed using Orchestrix agents

### Export as Artifact

```bash
# The backup file created by buildClarity.sh can be used as an artifact
ls -la clarity-*.tar.gz

# Move to orchestrix images directory
mv clarity-*.tar.gz ../../
```

## Security Considerations

1. **Change Default Passwords**: Always change the Grafana admin password on first login
2. **Network Security**: Consider using firewall rules to restrict access to monitoring ports
3. **Data Sensitivity**: Be aware that Prometheus stores metrics data in plain text
4. **Container Isolation**: LXC provides OS-level isolation but shares the kernel with the host

## Maintenance

### Regular Tasks

1. **Monitor Disk Usage**: Prometheus data can grow over time
   ```bash
   du -sh /data/orchestrix/clarity/prometheus
   ```

2. **Update Software**: Periodically update Prometheus and Grafana
   ```bash
   lxc exec clarity -- apt update
   lxc exec clarity -- apt upgrade
   ```

3. **Backup Data**: Regular backups of configuration and data directories
   ```bash
   tar -czf clarity-data-backup.tar.gz /data/orchestrix/clarity/
   ```

### Data Retention

Prometheus is configured with default retention. To modify:

```bash
# Edit the systemd service
lxc exec clarity -- nano /etc/systemd/system/prometheus.service

# Add retention flags to ExecStart:
# --storage.tsdb.retention.time=30d
# --storage.tsdb.retention.size=10GB

# Reload and restart
lxc exec clarity -- systemctl daemon-reload
lxc exec clarity -- systemctl restart prometheus
```

## Support and Contribution

For issues, questions, or contributions related to the Clarity container:

1. Check this documentation first
2. Review the build script source code
3. Contact the Orchestrix development team

## Version History

- **v1.0.0**: Initial release with Prometheus and Grafana
  - Debian 12 base
  - Automatic Prometheus-Grafana integration
  - Persistent storage configuration
  - Backup functionality

## License

This container build script and configuration are part of the Orchestrix project.

---

**Note**: This container is designed for monitoring and observability. Ensure you comply with your organization's monitoring and data retention policies when deploying in production environments.