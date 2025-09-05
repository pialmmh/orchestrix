# Complete LXD Container Building Workflow

## Overview
This document describes the complete automated workflow for building LXD containers through Jenkins, triggered by AI agent requests.

## Directory Structure
```
orchestrix/
├── jenkins/
│   ├── jenkins-config.yml      # Jenkins configuration
│   ├── Jenkinsfile             # Pipeline script
│   ├── trigger-build.py        # Python script to trigger builds
│   └── WORKFLOW.md            # This file
│
└── containers/
    ├── mysql/
    │   ├── mysql.yml           # Container configuration
    │   └── mysql_build.sh      # Build script
    ├── redis/
    ├── grafana/
    └── nginx/
```

## Workflow Steps

### 1. User Request to AI Agent
User asks: "Create a MySQL container with 4GB RAM and persistent storage"

### 2. AI Agent Creates Configuration

The AI agent will:
1. Create container directory: `containers/mysql/`
2. Generate `mysql.yml` with specifications
3. Generate `mysql_build.sh` with installation steps
4. Commit files to repository

### 3. AI Agent Triggers Jenkins Build

Using the Python script:
```python
from jenkins.trigger_build import JenkinsTrigger

trigger = JenkinsTrigger()
result = trigger.trigger_build(
    container_name="mysql",
    container_type="database",
    version="8.0"
)

print(f"Build started: {result['console_url']}")
```

Or via command line:
```bash
python jenkins/trigger-build.py mysql database 8.0
```

### 4. Jenkins Pipeline Execution

The pipeline performs these stages:
1. **Validate Input** - Check container name is provided
2. **Check Configuration** - Verify YAML and build script exist
3. **Cleanup Existing** - Remove old container if exists
4. **Execute Build Script** - Run the container-specific build script
5. **Verify Container** - Check container is running
6. **Create Backup** - Export container as tar.gz
7. **Upload to Google Drive** - Upload backup via rclone
8. **Cleanup** - Remove temporary files

### 5. Build Script Actions

Each build script (`*_build.sh`) performs:
1. Launch base Ubuntu/Debian container
2. Configure resources (CPU, memory, storage)
3. Create persistent storage mount points
4. Install required software
5. Configure the software
6. Set up monitoring exporters
7. Create backup scripts
8. Display connection information

### 6. AI Agent Reports Back

```
✅ Container build completed successfully!

Container Details:
- Name: mysql
- IP: 10.0.1.10
- Status: Running
- MySQL Port: 3306
- Root Password: changeme123

Backup Location:
- Google Drive: orchestrix/lxc-images/mysql/mysql-20250901-143022.tar.gz

Access:
- From host: mysql -h 10.0.1.10 -u root -pchangeme123
- Shell: lxc exec mysql bash

Jenkins Build: http://172.82.66.90:8080/job/LXD-Container-Builder/45/console
```

## Container Types

### Database Containers
- **MySQL**: Full MySQL 8.0 server with monitoring
- **PostgreSQL**: PostgreSQL with replication support
- **MongoDB**: NoSQL database with sharding capability
- **Redis**: In-memory cache and message broker

### Monitoring Containers
- **Grafana**: Visualization and dashboards
- **Prometheus**: Metrics collection and alerting
- **Elasticsearch**: Log aggregation and search

### Web Containers
- **Nginx**: Web server and reverse proxy
- **Apache**: Traditional web server
- **Node.js**: Application runtime

## Triggering Builds

### Method 1: AI Agent (Automated)
```python
# AI agent code
import os
import subprocess

def create_container(name, type, specs):
    # Generate configuration files
    create_yaml_config(name, specs)
    create_build_script(name, type)
    
    # Trigger Jenkins
    result = trigger_jenkins_build(name, type)
    return result
```

### Method 2: Jenkins GUI
1. Login to Jenkins: http://172.82.66.90:8080
2. Click "LXD-Container-Builder"
3. Click "Build with Parameters"
4. Fill in:
   - CONTAINER_NAME: mysql
   - CONTAINER_TYPE: database
   - VERSION: 8.0
5. Click "Build"

### Method 3: Command Line
```bash
# Using curl
curl -X POST http://172.82.66.90:8080/job/LXD-Container-Builder/buildWithParameters \
  -u admin:11b7d0764a66a46cdddd2e124cf138fae9 \
  --data-urlencode "CONTAINER_NAME=mysql" \
  --data-urlencode "CONTAINER_TYPE=database"

# Using Python script
python jenkins/trigger-build.py mysql database 8.0
```

### Method 4: API Call
```python
import requests

response = requests.post(
    "http://172.82.66.90:8080/job/LXD-Container-Builder/buildWithParameters",
    auth=("admin", "11b7d0764a66a46cdddd2e124cf138fae9"),
    params={
        "CONTAINER_NAME": "mysql",
        "CONTAINER_TYPE": "database",
        "VERSION": "8.0"
    }
)
```

## Monitoring Builds

### Real-time Console Output
```
http://172.82.66.90:8080/job/LXD-Container-Builder/lastBuild/console
```

### Build History
```
http://172.82.66.90:8080/job/LXD-Container-Builder/
```

### Pipeline View
Shows visual progress through stages

## Backup and Recovery

### Automatic Backups
Every build creates a backup:
- Local: `/tmp/lxd-backups/[container]-[timestamp].tar.gz`
- Remote: Google Drive `orchestrix/lxc-images/[container]/`

### Manual Backup
```bash
lxc export mysql mysql-backup.tar.gz
rclone copy mysql-backup.tar.gz pialmmhtb:orchestrix/lxc-images/mysql/
```

### Restore from Backup
```bash
# Download from Google Drive
rclone copy pialmmhtb:orchestrix/lxc-images/mysql/mysql-20250901.tar.gz .

# Import container
lxc import mysql-20250901.tar.gz

# Start container
lxc start mysql
```

## Troubleshooting

### Build Failed - Container Exists
- The pipeline will automatically stop and delete existing containers
- If manual cleanup needed: `lxc delete mysql --force`

### Build Failed - Network Timeout
- Check LXD bridge: `lxc network list`
- Verify DNS: `lxc exec mysql -- ping 8.8.8.8`

### Build Failed - Disk Space
- Check space: `df -h`
- Clean old images: `lxc image list` and `lxc image delete <fingerprint>`

### Upload to Google Drive Failed
- Verify rclone config: `rclone listremotes`
- Test connection: `rclone ls pialmmhtb:`
- Check permissions: `rclone about pialmmhtb:`

## Security Considerations

1. **Credentials**: Never commit real passwords to Git
2. **API Tokens**: Rotate Jenkins tokens regularly
3. **Container Isolation**: Use unprivileged containers
4. **Network Security**: Configure firewall rules
5. **Backup Encryption**: Consider encrypting backups before upload

## Example: Complete MySQL Container Request

### User Request
"I need a MySQL 8.0 database container with:
- 4GB RAM, 2 CPUs
- 50GB storage
- Daily backups
- Monitoring enabled
- Orchestrix database created"

### AI Agent Response
```
Creating MySQL container configuration...
✓ Generated containers/mysql/mysql.yml
✓ Generated containers/mysql/mysql_build.sh
✓ Triggered Jenkins build #45

Jenkins build started: http://172.82.66.90:8080/job/LXD-Container-Builder/45/console

[After 3-5 minutes]

✅ MySQL container created successfully!

Container: mysql
IP: 10.0.1.10:3306
Database: orchestrix
User: orchestrix / orchpass123
Root: root / changeme123

Monitoring: http://10.0.1.10:9104/metrics
Backup: Google Drive/orchestrix/lxc-images/mysql/

Connect: mysql -h 10.0.1.10 -u orchestrix -porchpass123 orchestrix
```

## Next Steps

1. **Add More Container Types**
   - Create templates in `containers/` directory
   - Each needs a `.yml` and `_build.sh` file

2. **Customize Configurations**
   - Edit YAML files for different specifications
   - Modify build scripts for specific software

3. **Set Up Monitoring**
   - Configure Prometheus to scrape exporters
   - Create Grafana dashboards

4. **Implement CI/CD**
   - Add Git webhooks for automatic builds
   - Create staging/production pipelines