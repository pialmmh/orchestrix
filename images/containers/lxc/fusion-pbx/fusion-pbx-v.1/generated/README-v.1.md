# FusionPBX Container v.1

## Quick Start

```bash
# Launch with default configuration
./launch.sh sample.conf

# Or use custom configuration
./launch.sh /path/to/your/config.conf
```

## Features

- FusionPBX with FreeSWITCH
- PostgreSQL database
- Nginx web server
- PHP 8.2
- Bridge networking without NAT (perfect for VoIP)
- Persistent storage via bind mounts
- SSH access for development

## Configuration

Edit `sample.conf` to customize:
- Static IP address
- Domain name
- SIP profiles
- Persistent storage locations

## Network Architecture

- Bridge mode without NAT
- Direct IP routing for VoIP
- No NAT traversal issues
- Clean SIP/RTP flow

## Access

After launching:
- Web UI: http://<container-ip>
- Initial Setup: http://<container-ip>/install.php
- SSH: fusionpbx@<container-ip> (password: fusion123)

## Persistent Storage

Mount points for data persistence:
- PostgreSQL: /var/lib/postgresql
- FreeSWITCH: /var/lib/freeswitch
- Recordings: /var/lib/freeswitch/recordings
- FusionPBX: /var/www/fusionpbx/app

## Build Information

- Base: Debian 12 (64-bit)
- Build Date: Sun Sep 14 09:12:34 PM +06 2025
- Version: 1
