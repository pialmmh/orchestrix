# Dev-Env Container Requirements & Services

## Container Overview
LXC-based development environment container providing CI/CD integration, remote service tunneling, and containerization tools.

---

## Core Services

### 1. Operating System
- **Base**: Debian 12 (Bookworm) 
- **Architecture**: amd64
- **Init System**: systemd

### 2. Container Management
- **LXC/LXD**: Full container orchestration
- **Docker**: Container build and runtime
- **Privileges**: Root access for container operations

---

---

## Development Tools

### 1. Version Control
- **Git**: Source code management
- **SSH Client**: Repository access

### 2. Java Development (via SDKMAN)
- **SDKMAN**: Java version manager
- **Java 21 Temurin**: Default JDK
- **Java 17 Temurin**: Alternative JDK
- **Switch versions**: `sdk use java <version>`

### 3. Build Environment
- **Make**: Build automation
- **GCC**: Compilation tools

### 3. Utilities
- **curl/wget**: HTTP operations
- **jq**: JSON processing
- **vim**: Text editing
- **netcat**: Network testing

---

## CI/CD Integration

### 1. Jenkins Agent (Docker-based)
- **Deployment**: Runs as Docker container inside LXC
- **Image**: `jenkins/inbound-agent:latest-jdk17`
- **Protocol**: JNLP (port 50000)
- **No Java needed in LXC**: Java runs inside Docker container
- **Features**:
  - Multiple agents with different Java versions
  - Easy updates (docker pull)
  - Better isolation
  - Auto-restart via Docker

### 2. Agent Configuration
- Jenkins master URL
- Agent name (must match Jenkins config)  
- Agent secret (from Jenkins UI)
- Docker manages the agent lifecycle

---

## Network Services

### 1. SSH Tunneling (Optional)
- **Purpose**: Access remote databases/services
- **Manager**: autossh with auto-reconnection
- **Supported Services**:
  - MySQL/PostgreSQL databases
  - Redis/Memcached
  - Kafka/RabbitMQ
  - Any TCP service

### 2. SSH Configuration
- Auto-accepts all certificates (dev mode)
- No host key checking
- Immediate connections without prompts

---

## Storage & Mounts

### 1. Bind Mounts
- Project directories
- Configuration files
- Log directories
- Shared resources

### 2. Volumes
- Jenkins workspace: `/var/jenkins`
- Container runtime: `/var/lib/docker`
- SSH tunnels: `/var/run/ssh-tunnels`

---

## Configuration Management

### 1. Unified Configuration
- **Single config file** controls all services
- **Runtime location**: `/etc/dev-env-runtime.conf`
- **Hot-reload**: Services read config on start

### 2. Configurable Items
- Container name
- Jenkins agent settings
- SSH tunnel definitions
- Bind mount mappings
- Environment variables

---

## System Services

### 1. systemd Services  
- `docker-jenkins-agent.service` - Docker container for Jenkins agent
- `ssh-tunnels.service` - SSH tunnel manager
- `tunnel-health.timer` - Connection monitor

### 2. Service Management
- Auto-start on boot
- Automatic restart on failure
- Health check monitoring
- Centralized logging

---

## Security Features

### 1. Container Isolation
- Namespace separation
- Resource limits
- Network isolation

### 2. Development Mode
- SSH auto-accept (for dev environments only)
- Passwordless sudo (container internal)
- Docker socket access (for builds)

---

## Resource Requirements

### 1. Minimum
- CPU: 1 core
- RAM: 512MB
- Disk: 2GB

### 2. Recommended
- CPU: 2+ cores
- RAM: 2GB+
- Disk: 10GB+

---

## Use Cases

1. **Jenkins Build Agent**: Automated CI/CD builds
2. **Development Environment**: Isolated dev workspace
3. **Service Bridge**: Tunnel to remote services
4. **Container Builder**: Docker/LXC image creation
5. **Test Environment**: Integration testing platform