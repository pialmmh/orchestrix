# Jenkins Instances

Centralized configuration for multiple Jenkins instances.

## Structure

```
instances/
├── default.yml              # Points to active instance
├── massivegrid01/          # Primary Jenkins instance
│   ├── config.yml          # Server settings, credentials
│   ├── agents.yml          # Agent definitions
│   ├── docker-compose.yml  # Docker agent setup
│   └── dev-env-01.conf     # LXC agent config
└── aws01/                  # Future AWS Jenkins (example)
    └── config.yml
```

## Quick Start

### Use the management script:
```bash
cd /home/mustafa/telcobright-projects/orchestrix/jenkins

# List all instances
./jenkins-instance.sh list

# Switch to an instance
./jenkins-instance.sh use massivegrid01

# Start agents
./jenkins-instance.sh start

# Check status
./jenkins-instance.sh status
```

### Manual Docker agent start:
```bash
cd instances/massivegrid01
docker-compose up -d
```

### LXC agent start:
```bash
cd /home/mustafa/telcobright-projects/orchestrix/images/containers/lxc/dev-env/run
sudo ./launch.sh ../../jenkins/instances/massivegrid01/dev-env-01.conf
```

## Configuration Files

### config.yml
Main instance configuration:
- Jenkins URL and ports
- Admin credentials
- Agent definitions with secrets
- Pipeline configurations
- Build defaults

### agents.yml
Detailed agent specifications:
- Docker agents
- LXC agents
- Cloud agents (AWS, Azure)
- Agent-specific settings

### docker-compose.yml
Docker agent deployment:
- Container configuration
- Volume mounts
- Network settings

### dev-env-01.conf
LXC dev-env agent configuration:
- Jenkins connection details
- Project mounts
- Environment variables

## Adding New Instance

```bash
# Create new instance
./jenkins-instance.sh create production http://jenkins.prod.com:8080

# Edit configuration
nano instances/production/config.yml

# Switch to it
./jenkins-instance.sh use production
```

## Security Note

The secrets are stored in plain text for development convenience. For production:
- Use Jenkins credentials store
- Encrypt sensitive files
- Use environment variables
- Implement proper secret management