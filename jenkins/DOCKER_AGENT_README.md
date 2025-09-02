# Jenkins Docker Agent Management

## Overview
This is the **official and only supported method** for running Jenkins agents in the Orchestrix project. All agent management is done through Docker containers for reliability, isolation, and ease of management.

## Prerequisites
- Ubuntu/Debian Linux system
- Internet connection for Docker installation
- Jenkins server running at http://172.82.66.90:8080

## Quick Start

### 1. Fresh Installation
```bash
cd /home/mustafa/telcobright-projects/orchestrix/jenkins
./jenkins-docker-agent.sh setup
```

When prompted, enter the agent secret from Jenkins UI:
1. Go to http://172.82.66.90:8080/computer/orchestrix-agent
2. Copy the secret from the connection command
3. Paste it when prompted

That's it! The agent will:
- ✅ Auto-start on system boot
- ✅ Restart on failures
- ✅ Connect to Jenkins automatically

## Management Commands

All agent management is done through the `manage-agent.sh` script:

```bash
cd /home/mustafa/telcobright-projects/orchestrix/jenkins

# Start agent
./manage-agent.sh start

# Stop agent
./manage-agent.sh stop

# Restart agent
./manage-agent.sh restart

# Check status
./manage-agent.sh status

# View logs
./manage-agent.sh logs

# Enter container shell
./manage-agent.sh shell

# Update agent image
./manage-agent.sh update

# Clean up
./manage-agent.sh clean
```

## Architecture

```
┌─────────────────────────────────┐
│      Jenkins Master             │
│   http://172.82.66.90:8080      │
└─────────────┬───────────────────┘
              │ JNLP Connection
              │
┌─────────────▼───────────────────┐
│    Docker Container             │
│  jenkins-orchestrix-agent       │
│                                 │
│  - Jenkins inbound-agent        │
│  - Auto-restart on failure      │
│  - Isolated environment         │
│  - Access to Docker socket      │
│  - Access to LXC/LXD            │
└─────────────────────────────────┘
```

## Files Created

```
jenkins/
├── jenkins-docker-agent.sh    # Main setup and management script
├── docker-compose.yml         # Docker Compose configuration
├── manage-agent.sh           # Quick management commands
└── DOCKER_AGENT_README.md    # This documentation
```

## Features

### Auto-Recovery
- Container restarts automatically on failure
- Reconnects to Jenkins after network issues
- Survives system reboots

### Docker-in-Docker Support
The agent has access to Docker socket, allowing:
- Building Docker images
- Running Docker containers
- Docker Compose operations

### LXC/LXD Support
The agent can manage LXC containers:
- Create and destroy containers
- Build LXC images
- Access to LXD socket

### Project Access
- Mounts `~/telcobright-projects` into container
- Full access to Orchestrix codebase
- Can run builds and tests

## Troubleshooting

### Agent Shows Offline in Jenkins

1. Check container status:
```bash
./manage-agent.sh status
```

2. View logs:
```bash
./manage-agent.sh logs
```

3. Restart agent:
```bash
./manage-agent.sh restart
```

### Connection Issues

1. Verify Jenkins is accessible:
```bash
curl http://172.82.66.90:8080
```

2. Check agent secret is correct:
- Get new secret from Jenkins UI
- Update docker-compose.yml
- Restart agent

### Permission Issues

1. Ensure user is in docker group:
```bash
sudo usermod -aG docker $USER
# Log out and back in
```

2. Check Docker socket permissions:
```bash
ls -la /var/run/docker.sock
```

## System Requirements

- **Docker**: Version 20.10 or newer
- **Docker Compose**: Version 1.29 or newer
- **Memory**: At least 512MB for agent container
- **Disk**: 10GB free space for builds

## Security Notes

- Agent runs as root in container (required for LXC operations)
- Has access to Docker socket (required for Docker builds)
- Isolated from host system except for mounted volumes
- Credentials stored in docker-compose.yml (protect this file)

## Systemd Service

The agent runs as a systemd service:

```bash
# Check service status
sudo systemctl status jenkins-docker-agent

# View service logs
sudo journalctl -u jenkins-docker-agent -f

# Disable auto-start
sudo systemctl disable jenkins-docker-agent

# Enable auto-start
sudo systemctl enable jenkins-docker-agent
```

## Updating

To update the Jenkins agent to the latest version:

```bash
./manage-agent.sh update
```

This will:
1. Pull the latest jenkins/inbound-agent image
2. Restart the container with new image
3. Preserve all settings and workspace

## Complete Removal

To completely remove the Jenkins agent:

```bash
./jenkins-docker-agent.sh remove
```

This will:
- Stop and remove container
- Remove Docker volumes
- Disable systemd service
- Clean up configuration files

## Support

For issues or questions:
1. Check the logs: `./manage-agent.sh logs`
2. Verify Jenkins connectivity
3. Ensure Docker is running: `docker info`
4. Check this documentation

## Important Notes

⚠️ **Do not use any other agent setup methods** - Only Docker-based agents are supported
⚠️ **Protect your credentials** - The docker-compose.yml contains sensitive information
⚠️ **Regular updates** - Run `./manage-agent.sh update` monthly for security patches