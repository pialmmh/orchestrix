# Jenkins Integration for Orchestrix

This directory contains Jenkins configuration, pipeline scripts, and Docker-based agent management for the Orchestrix project.

## 🚀 Quick Start - Docker Agent Setup

**IMPORTANT**: Docker-based agents are the ONLY supported method for Jenkins agents in this project.

### Fresh Installation
```bash
cd /home/mustafa/telcobright-projects/orchestrix/jenkins
./jenkins-docker-agent.sh setup
```

When prompted, enter the agent secret from Jenkins UI:
1. Go to http://172.82.66.90:8080/computer/orchestrix-agent
2. Copy the secret from the connection command
3. Paste it when prompted

The agent will automatically:
- ✅ Start on system boot
- ✅ Restart on failures
- ✅ Connect to Jenkins
- ✅ Have access to Docker and LXC

## Directory Structure

```
jenkins/
├── jenkins-docker-agent.sh    # Docker agent setup script
├── docker-compose.yml         # Docker Compose configuration
├── manage-agent.sh           # Agent management commands
├── DOCKER_AGENT_README.md    # Complete Docker agent documentation
├── jenkins-config.yml        # Jenkins server configuration
├── Jenkinsfile              # Main pipeline script
├── container-pipeline-template.groovy  # Pipeline template for containers
└── scripts/                 # Helper scripts
```

## Configuration Files

### jenkins-config.yml
Main configuration file containing:
- Jenkins server URL and credentials
- Git repository settings
- Google Drive upload configuration
- Notification settings
- Build defaults and limits

### Environment Variables
The pipeline uses these environment variables:
- `CONTAINER_NAME` - Name of the container to build
- `CONTAINER_TYPE` - Type of container (mysql, redis, grafana, etc.)
- `BUILD_VERSION` - Version tag for the container

## Usage Examples

### Via AI Agent
```
User: "Create a MySQL container with 4GB RAM"
AI: "I'll create the MySQL container configuration and trigger Jenkins build..."
```

### Via Jenkins GUI
1. Open Jenkins dashboard
2. Click "LXD-Container-Builder"
3. Click "Build with Parameters"
4. Enter container name
5. Click "Build"

### Via API
```bash
curl -X POST http://your-jenkins:8080/job/LXD-Container-Builder/buildWithParameters \
  -u username:api_token \
  --data-urlencode "CONTAINER_NAME=mysql" \
  --data-urlencode "CONTAINER_TYPE=database"
```

## Security Notes

- **Never commit** the `jenkins-config.yml` file with real credentials to Git
- Use Jenkins credentials store for sensitive data
- Rotate API tokens regularly
- Restrict Jenkins job permissions appropriately

## Agent Management

### Managing the Docker Agent

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
```

## Troubleshooting

### Agent Shows Offline
1. Check container status: `./manage-agent.sh status`
2. View logs: `./manage-agent.sh logs`
3. Restart agent: `./manage-agent.sh restart`

### Connection Issues
- Verify Jenkins is accessible: `curl http://172.82.66.90:8080`
- Check agent secret is correct in docker-compose.yml
- Ensure Docker is running: `docker info`

### Build Failures
- Check Jenkins console output
- Verify LXD is installed and running
- Ensure sufficient disk space
- Agent has Docker and LXC access built-in

## Important Notes

⚠️ **Docker-Only Policy**: Do not use any other agent setup methods - only Docker agents are supported
⚠️ **Credentials Security**: The docker-compose.yml contains sensitive information - protect it
⚠️ **Regular Updates**: Run `./manage-agent.sh update` monthly for security patches

## Support

For issues or questions:
1. Check agent logs: `./manage-agent.sh logs`
2. Review DOCKER_AGENT_README.md for detailed documentation
3. Check Jenkins build logs
4. Verify Docker and Jenkins connectivity