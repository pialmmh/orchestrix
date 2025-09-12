# FusionPBX LXC Container

A reusable Debian 12-based LXC container for FusionPBX - a powerful and highly configurable multi-tenant PBX and voice switch solution.

## Quick Start

```bash
./startDefault.sh
```

This will build the base image (if needed) and launch a container with default settings.

## Components Included

- **FusionPBX**: Web-based PBX administration interface
- **FreeSWITCH**: The telephony engine
- **PostgreSQL**: Database backend
- **Nginx**: Web server
- **PHP 8.2**: PHP runtime with required extensions
- **SSH Server**: Remote access capability

## Architecture

This container follows a two-phase approach:

1. **Build Phase** (`buildFusionPBX.sh`): Creates a reusable base image with all software installed
2. **Launch Phase** (`launchFusionPBX.sh`): Launches containers from the base image with custom configurations

## Files

- `buildFusionPBX.sh`: Builds the base image
- `launchFusionPBX.sh`: Launches a container from the base image
- `sample-config.conf`: Sample configuration file
- `startDefault.sh`: Quick start script with defaults
- `README.md`: This documentation

## Usage

### Building the Base Image

```bash
./buildFusionPBX.sh [config-file]
```

If no config file is specified, uses `sample-config.conf`.

### Launching a Container

```bash
./launchFusionPBX.sh [config-file]
```

The config file can be located anywhere in the filesystem.

### Custom Configuration

1. Copy `sample-config.conf` to your desired location:
   ```bash
   cp sample-config.conf ~/my-fusion-config.conf
   ```

2. Edit the configuration as needed

3. Launch with your config:
   ```bash
   ./launchFusionPBX.sh ~/my-fusion-config.conf
   ```

## Configuration Options

### Basic Settings
- `CONTAINER_NAME`: Name for the container instance
- `CONTAINER_IP`: Static IP address (optional, uses DHCP if not set)
- `BASE_IMAGE_NAME`: Name of the base image
- `BASE_IMAGE_USER`: Default user in the container

### Mount Points (all optional)
- `WORKSPACE_MOUNT`: Development workspace directory
- `DATA_MOUNT`: PostgreSQL data persistence
- `FREESWITCH_CONF_MOUNT`: FreeSWITCH configuration
- `FUSIONPBX_APP_MOUNT`: FusionPBX application files

### Service Configuration
- `DB_PASSWORD`: PostgreSQL password
- `ADMIN_PASSWORD`: FusionPBX admin password
- `SIP_PROFILE_INTERNAL_IP`: Internal SIP profile IP
- `SIP_PROFILE_EXTERNAL_IP`: External SIP profile IP

### SSH Tunnels (optional)
- `SSH_TUNNEL_HOST`: Remote SSH server
- `SSH_TUNNEL_USER`: SSH username
- `SSH_KEY_PATH`: Path to SSH key
- `SSH_TUNNEL_PORTS`: Port mappings (format: `local:remote:host`)

## Access Methods

### Web Interface
- URL: `http://<container-ip>`
- First-time setup: `http://<container-ip>/install.php`

### SSH Access
```bash
ssh ubuntu@<container-ip>
```
Default password: `debian`

### Container Shell
```bash
lxc exec fusion-pbx-dev -- bash
```

### FreeSWITCH CLI
From within the container:
```bash
fs_cli
```

### PostgreSQL Access
From within the container:
```bash
psql -U postgres
```

## Container Management

### Stop Container
```bash
lxc stop fusion-pbx-dev
```

### Start Container
```bash
lxc start fusion-pbx-dev
```

### Restart Container
```bash
lxc restart fusion-pbx-dev
```

### Delete Container
```bash
lxc delete fusion-pbx-dev --force
```

### View Container Info
```bash
lxc info fusion-pbx-dev
```

### List All Containers
```bash
lxc list
```

## First-Time Setup

1. Launch the container
2. Access the web interface at `http://<container-ip>/install.php`
3. Follow the installation wizard:
   - Database: PostgreSQL
   - Database Host: localhost
   - Database Name: fusionpbx
   - Database Username: postgres
   - Database Password: (from your config or leave blank)
4. Create admin account
5. Complete the setup

## Persistence

To persist your PBX data across container recreations:

1. Set `DATA_MOUNT` in your config to persist PostgreSQL data
2. Set `FREESWITCH_CONF_MOUNT` to persist FreeSWITCH configurations
3. Set `FUSIONPBX_APP_MOUNT` to persist FusionPBX application files

## Development Workflow

For development work:

1. Set `WORKSPACE_MOUNT` to your project directory
2. Mount custom FreeSWITCH modules or dialplans
3. Use SSH tunnels to connect to external services

## Troubleshooting

### Container Won't Start
```bash
# Check container status
lxc info fusion-pbx-dev

# View logs
lxc console fusion-pbx-dev --show-log
```

### Services Not Running
```bash
# Check service status
lxc exec fusion-pbx-dev -- systemctl status postgresql
lxc exec fusion-pbx-dev -- systemctl status freeswitch
lxc exec fusion-pbx-dev -- systemctl status nginx
lxc exec fusion-pbx-dev -- systemctl status php8.1-fpm
```

### Network Issues
```bash
# Check IP assignment
lxc list fusion-pbx-dev

# Check network configuration
lxc config show fusion-pbx-dev
```

### FreeSWITCH Issues
```bash
# Check FreeSWITCH logs
lxc exec fusion-pbx-dev -- tail -f /var/log/freeswitch/freeswitch.log

# Test SIP registration
lxc exec fusion-pbx-dev -- fs_cli -x "sofia status"
```

## Security Notes

- Default SSH configuration accepts all host keys (development only)
- Change default passwords immediately
- Configure firewall rules as needed
- Use SSH keys instead of passwords for production

## Advanced Usage

### Custom FreeSWITCH Modules
Mount your custom modules directory and update the FreeSWITCH configuration.

### High Availability
Use multiple containers with shared PostgreSQL backend.

### Integration with External Services
Use SSH tunnels to connect to:
- External databases
- Message queues
- Monitoring systems
- CDR processing systems

## Support

For issues or questions:
1. Check the logs in `/var/log/` within the container
2. Review FusionPBX documentation: https://docs.fusionpbx.com
3. Check FreeSWITCH documentation: https://freeswitch.org/confluence/