# AI Instructions for Jenkins-Based Container Scaffolding

## Overview
When asked to "scaffold" a container in the Orchestrix project, use the Jenkins-integrated scaffolding system that:
1. Generates build scripts and container structure
2. Creates Jenkins job via API
3. Launches Jenkins agent if needed
4. Enables building through Jenkins UI

## Primary Scaffolding Command

```bash
python3 /home/mustafa/telcobright-projects/orchestrix/scaffold-container.py [container-name] [options]
```

### Options:
- `--type [dev-env|service|test]` - Container type (default: dev-env)
- `--os [debian/12|ubuntu/22.04]` - Base OS (default: debian/12)
- `--no-jenkins` - Skip Jenkins job creation
- `--trigger-build` - Automatically trigger Jenkins build

## Complete Scaffolding Process

### Step 1: Run Scaffold Script
```bash
# Example: Scaffold a PostgreSQL development container
python3 scaffold-container.py postgres-dev --type dev-env --os debian/12

# Example: Scaffold and trigger build
python3 scaffold-container.py redis-cache --type service --trigger-build
```

### Step 2: What the Script Does
1. **Creates Directory Structure**:
   ```
   images/containers/lxc/[container-name]/
   ├── build[ContainerName].sh
   ├── launch[ContainerName].sh
   ├── build[ContainerName]Config.cnf
   ├── sample-config.conf
   ├── startDefault.sh
   ├── README.md
   └── scripts/
       └── [container-name]-services.sh
   ```

2. **Creates Jenkins Job**:
   - Job name: `[container-name]-builder`
   - Uses `container-pipeline-template.groovy`
   - Configures parameters and build steps

3. **Checks Jenkins Agent**:
   - Verifies `lxd-build-agent` is running
   - Starts agent if offline
   - Prompts for setup if not configured

4. **Optionally Triggers Build**:
   - Starts Jenkins build immediately
   - Provides URL to monitor progress

## Jenkins Configuration

### Required Files:
- `jenkins/jenkins-config.yml` - Jenkins server details and API credentials
- `jenkins/container-pipeline-template.groovy` - Pipeline template for builds
- `jenkins/setup-jenkins-agent.sh` - Agent setup script

### Jenkins Credentials:
```yaml
jenkins:
  server:
    url: "http://172.82.66.90:8080"
  auth:
    username: "admin"
    api_token: "11b7d0764a66a46cdddd2e124cf138fae9"
```

## AI Response Template

When user asks to scaffold a container, respond with:

```
I'll scaffold the [container-name] container with Jenkins integration.

Running scaffolding command...
```

Then execute:
```bash
python3 /home/mustafa/telcobright-projects/orchestrix/scaffold-container.py [container-name] --type [type]
```

After completion, provide:
```
✅ Container scaffolded successfully!

📁 Structure created at: /home/mustafa/telcobright-projects/orchestrix/images/containers/lxc/[container-name]/

🔧 Jenkins job created: [container-name]-builder
📎 Jenkins URL: http://172.82.66.90:8080/job/[container-name]-builder

To build the container:
1. Via Jenkins UI: Click "Build Now" in Jenkins
2. Locally: cd images/containers/lxc/[container-name] && sudo ./build[Name].sh

Quick start after build:
sudo ./startDefault.sh
```

## Common Scaffolding Requests

### 1. Development Environment Container
```bash
python3 scaffold-container.py [name]-dev --type dev-env
```
Features to add:
- Development tools
- SSH auto-accept configuration
- Project workspace mounts

### 2. Service Container
```bash
python3 scaffold-container.py [service-name] --type service
```
Features to add:
- Service installation
- Port configuration
- Health checks

### 3. Test Container
```bash
python3 scaffold-container.py [name]-test --type test
```
Features to add:
- Test frameworks
- Result output
- Cleanup scripts

## Jenkins Build Process

When build is triggered via Jenkins:

1. **Validate Parameters** - Check container name, type, OS
2. **Check Agent** - Verify LXD agent is available
3. **Checkout Code** - Get container configuration
4. **Prepare Environment** - Make scripts executable
5. **Build Container** - Run build script
6. **Test Container** - Launch test with sample config
7. **Create Backup** - Export container image
8. **Push to Registry** - Optional registry push
9. **Generate Documentation** - Create build info

## Troubleshooting

### Jenkins Agent Not Running
```bash
# Start agent
sudo jenkins/setup-jenkins-agent.sh start

# Setup new agent
sudo jenkins/setup-jenkins-agent.sh
```

### Jenkins API Authentication Failed
- Check `jenkins-config.yml` for correct credentials
- Regenerate API token in Jenkins UI
- Update token in config file

### Build Failed in Jenkins
- Check Jenkins console output
- Verify LXD is installed on agent
- Check agent has sudo permissions

## Key Differences from Manual Scaffolding

| Manual | Jenkins-Integrated |
|--------|-------------------|
| Creates files locally | Creates files + Jenkins job |
| Build with script | Build via Jenkins UI |
| No CI/CD | Full CI/CD pipeline |
| Local only | Remote builds possible |
| No agent needed | Requires Jenkins agent |

## Important Notes

1. **Always use scaffold-container.py** for new containers
2. **Jenkins job names** follow pattern: `[container-name]-builder`
3. **Pipeline template** is reusable for all containers
4. **Agent must have LXD** installed and configured
5. **Builds run as root** via sudo in Jenkins

## Example Full Workflow

User: "Scaffold a MongoDB container for development"

AI Actions:
1. Run: `python3 scaffold-container.py mongodb-dev --type dev-env`
2. Verify Jenkins job created
3. Check agent status
4. Optionally trigger build
5. Provide user with URLs and next steps

Result:
- Container structure created
- Jenkins job configured
- Ready to build via UI
- Documentation generated