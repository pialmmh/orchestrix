# AI Agents for Orchestrix - Human Instructions

## Overview

This directory contains specialized AI agents that extend Claude Code with domain-specific knowledge and capabilities. Each agent is designed for specific tasks and has direct access to relevant systems and infrastructure.

## Available Agents

### 🌐 MikroTik RouterOS Agent
**Location**: `ssh/networking/mikrotik/`  
**Purpose**: MikroTik RouterOS network management with direct SSH access  
**Capabilities**: Router configuration, monitoring, multi-device operations  
**Access**: 4 MikroTik routers via Orchestrix backend

### 🌐 Future Networking Agents
**Planned**: `ssh/networking/cisco/`, `ssh/networking/juniper/`, etc.  
**Purpose**: Vendor-specific network device management

### 🚀 Future Agent Categories
- **Container Management**: LXC/Docker orchestration
- **Database Admin**: MySQL/PostgreSQL operations  
- **Security Audit**: Infrastructure security scanning
- **DevOps Pipeline**: CI/CD automation

## How to Use Agents

### Method 1: Direct Agent Session (Recommended)
```bash
# Navigate to the specific agent directory
cd /home/mustafa/telcobright-projects/orchestrix/ai-agents/ssh/networking/mikrotik

# Start Claude Code session
claude-code
```

The agent will automatically load its specialized instructions and context.

### Method 2: Agent Reference from Any Session
```bash
# From any directory
claude-code

# Then in the session:
"Please act as the MikroTik agent defined in ai-agents/ssh/networking/mikrotik/AGENT_INSTRUCTIONS.md"
```

### Method 3: Agent Parameter (If Supported)
```bash
# From orchestrix root
claude-code --agent ai-agents/ssh/networking/mikrotik
```

## Agent Directory Structure

Each agent follows this standard structure:
```
ai-agents/
├── [category]/
│   └── [subcategory]/
│       └── [agent-name]/
│           ├── AGENT_INSTRUCTIONS.md    # Complete agent behavior
│           ├── README.md               # Usage guide and setup  
│           ├── context.json            # State and configuration
│           └── history.log             # Session logs (optional)
```

Example: `ai-agents/ssh/networking/mikrotik/`

## MikroTik Agent Quick Start

### Prerequisites
1. **Start Orchestrix backend**: `mvn spring-boot:run`
2. **Verify router connectivity**: Check SSH access to routers
3. **Confirm credentials**: Available in environment/CLAUDE.md

### Usage Examples
```bash
cd ai-agents/ssh/networking/mikrotik
claude-code

# In session:
"Connect to smsmk01 and show me the network overview"
"Configure OSPF area 0 for network 192.168.1.0/24"
"Show interface status across all routers"
"Create firewall rule to allow SSH from management network"
```

## Creating New Agents

### 1. Directory Setup
```bash
mkdir -p ai-agents/[category]/[subcategory]/[agent-name]
cd ai-agents/[category]/[subcategory]/[agent-name]
```

### 2. Required Files
- **AGENT_INSTRUCTIONS.md**: Complete agent behavior, knowledge, and capabilities
- **README.md**: Usage guide, prerequisites, and examples

### 3. Agent Instructions Template
```markdown
# [Agent Name] Agent

## Agent Role
You are a specialized [domain] assistant with [specific capabilities].

## Your Capabilities
- [Capability 1]: [Description]
- [Capability 2]: [Description]
- [Integration points]: [Backend services]

## Technical Context
- [System access]: [What systems agent can access]
- [Available tools]: [Tools and services]
- [Service endpoints]: [APIs and interfaces]

## Interaction Guidelines
- [Response format]: [How to structure responses]
- [Safety requirements]: [Safety and security measures]
- [Error handling]: [How to handle errors]
```

## Best Practices

### ✅ Do's
- **Start backend services** before using agents that require them
- **Use specific agent directories** for focused sessions
- **Provide context** about your infrastructure and goals
- **Test commands** in development environments first
- **Document changes** made during agent sessions

### ❌ Don'ts
- **Don't mix agents** in the same session without clear context
- **Don't skip prerequisites** listed in agent README files
- **Don't make production changes** without proper backups
- **Don't ignore agent safety warnings**

## Integration with Orchestrix

### Backend Services
Agents can access:
- **Spring Boot API**: REST endpoints for system operations
- **Database**: MySQL for persistent data
- **Device Management**: SSH/Telnet connections to infrastructure
- **Container Orchestration**: LXC management
- **File System**: Configuration and log access

### Service Dependencies
Before using agents, ensure required services are running:
```bash
# Backend API
mvn spring-boot:run

# Database
mysql -h 127.0.0.1:3306 -u root -p123456

# Container services (if needed)
lxc list
```

## Session Management

### Starting a Session
1. **Check prerequisites** in agent README
2. **Start required services** 
3. **Navigate to agent directory**
4. **Launch claude-code**
5. **Begin with context** about your current task

### During Sessions
- **Maintain context** of previous commands and changes
- **Verify results** of critical operations
- **Document important configurations** or discoveries
- **Handle errors** gracefully and ask for clarification

### Ending Sessions
- **Review changes** made during the session
- **Backup configurations** if modified
- **Document session outcomes**
- **Disconnect from resources** cleanly

## Troubleshooting

### Common Issues

**Agent Not Loading Properly**
- Verify AGENT_INSTRUCTIONS.md exists and is readable
- Check claude-code is run from correct directory
- Ensure prerequisites are met

**Backend Connection Failures**
- Confirm services are running (`mvn spring-boot:run`)
- Check network connectivity to target systems
- Verify credentials and access permissions

**Command Execution Errors**
- Review agent-specific syntax requirements
- Check system logs for detailed error messages
- Verify user permissions for requested operations

### Getting Help

1. **Check agent README.md** for specific guidance
2. **Review AGENT_INSTRUCTIONS.md** for capabilities and limitations
3. **Examine session context** for missing prerequisites
4. **Test with simpler commands** to isolate issues

## Security Considerations

### Access Control
- Agents inherit your user permissions
- SSH keys and credentials are used from your environment
- Production access requires proper authorization

### Safety Measures
- **Development first**: Test in non-production environments
- **Configuration backups**: Always backup before changes
- **Change validation**: Verify modifications work as expected
- **Access logging**: Monitor agent actions in production

### Credential Management
- Credentials stored in CLAUDE.md (not in agent files)
- SSH certificates auto-accepted in development only
- Production systems require proper certificate management

## Examples

### MikroTik Network Management Session
```bash
cd ai-agents/ssh/networking/mikrotik
claude-code

# In session:
"Connect to all MikroTik routers and show me interface status"
"Configure OSPF on the main router for area 0"
"Create a firewall rule to allow SSH from management network"
"Monitor system resources across all routers"
```

### Future Infrastructure Audit Session
```bash
cd ai-agents/security/audit
claude-code

# In session:
"Scan all containers for security vulnerabilities"
"Check SSH key permissions across all servers" 
"Generate security compliance report"
```

---

**Remember**: Each agent is a specialized assistant with deep knowledge of its domain. Use them for expert-level automation and analysis within their areas of expertise.