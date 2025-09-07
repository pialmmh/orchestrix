# MikroTik RouterOS Management Agent

## Overview
Specialized Claude agent for managing MikroTik RouterOS devices with direct SSH access through the Orchestrix backend. This agent has expert-level RouterOS knowledge and can execute commands across multiple routers simultaneously.

## Agent Capabilities

### 🌐 Router Management
- Direct SSH access to MikroTik devices
- Multi-router operations and coordination
- Real-time configuration and monitoring
- RouterOS command execution and parsing

### ⚙️ Network Operations
- Interface configuration and monitoring
- IP addressing and routing setup
- OSPF, BGP, and static routing management
- Firewall and NAT rule configuration
- Wireless network management
- Quality of Service (QoS) configuration

### 🔧 System Administration
- Device backup and restore
- System monitoring and diagnostics
- User management and security
- Log analysis and troubleshooting

## How to Use This Agent

### Prerequisites

1. **Start Orchestrix Backend**
```bash
cd /home/mustafa/telcobright-projects/orchestrix
mvn spring-boot:run
```

2. **Verify Router Access**
- Ensure routers are reachable via SSH
- Confirm credentials are available in environment
- Check network connectivity

### Method 1: Direct Agent Session (Recommended)
```bash
cd /home/mustafa/telcobright-projects/orchestrix/ai-agents/ssh/networking/mikrotik
claude-code
```

### Method 2: Reference from Any Session
```bash
claude-code
# Then in session:
"Please act as the MikroTik agent defined in ai-agents/ssh/networking/mikrotik/AGENT_INSTRUCTIONS.md"
```

### Method 3: From Main Directory
```bash
cd /home/mustafa/telcobright-projects/orchestrix
claude-code --agent ai-agents/ssh/networking/mikrotik
```

## Router Infrastructure

### Current Setup
- **smsmk01**: 114.130.145.70 (TB_Mikrotik_01_Borak) - Primary router
- **smsmk02**: [To be configured]
- **smsmk03**: [To be configured]
- **smsmk04**: [To be configured]

### Technical Details
- **OS**: RouterOS 6.49.18
- **Access**: SSH port 22
- **Authentication**: admin credentials from environment
- **Certificates**: Auto-accepted for development

## Usage Examples

### Getting Started
```
Agent: Connect to smsmk01 and show me the current system status and network overview
```

Expected response: Connection status, system resources, interface overview, routing table summary

### Interface Management
```
Agent: Show me all interfaces on smsmk01 and their current status
Agent: Enable interface ether2 on smsmk01
Agent: Configure IP address 192.168.10.1/24 on ether1 of smsmk01
```

### Multi-Router Operations
```
Agent: Show interface status across all connected routers
Agent: Get system identity from all routers
Agent: Compare routing tables between smsmk01 and smsmk02
```

### Routing Configuration
```
Agent: Configure OSPF area 0 for network 192.168.1.0/24 on smsmk01
Agent: Add static route to 10.0.0.0/8 via 192.168.1.254 on smsmk01
Agent: Show me all OSPF neighbors across the network
```

### Firewall Management
```
Agent: Show current firewall rules on smsmk01
Agent: Add NAT masquerading rule for 192.168.1.0/24 network on smsmk01
Agent: Create firewall rule to allow SSH from 192.168.100.0/24
```

### System Administration
```
Agent: Create a backup of smsmk01 configuration
Agent: Show system resource usage on all routers
Agent: Check system logs for any errors on smsmk01
```

### Monitoring and Diagnostics
```
Agent: Monitor interface traffic on ether1 of smsmk01
Agent: Test connectivity to 8.8.8.8 from smsmk01
Agent: Show DHCP client leases on all routers
```

## Backend Integration

### Java Services Used
- **DeviceManager**: Multi-router connection management
- **MikroTikService**: RouterOS-specific operations
- **REST API**: Device and command endpoints

### Available Endpoints
- `POST /api/devices/connect` - Connect to router
- `POST /api/devices/command` - Execute command on specific router
- `POST /api/devices/broadcast` - Execute command on all routers
- `GET /api/devices/status` - Get connection status
- `GET /api/mikrotik/{deviceId}/interfaces` - Get interface information
- `POST /api/mikrotik/{deviceId}/command` - Execute custom RouterOS command

## Safety Features

### Built-in Protections
- Configuration backup before major changes
- Connectivity verification after modifications
- Error handling and rollback procedures
- Change documentation and audit trails

### Best Practices Enforced
- Always verify commands before execution
- Test in development before production
- Monitor network impact of changes
- Maintain configuration consistency

## Troubleshooting

### Common Issues

**Connection Failures**
- Check if Orchestrix backend is running
- Verify router IP addresses and SSH connectivity
- Confirm credentials are correct

**Command Execution Errors**
- Verify RouterOS command syntax
- Check user permissions on router
- Ensure router resources are sufficient

**Response Parsing Issues**
- RouterOS version compatibility
- Command output format changes
- Network timeout problems

### Getting Help
1. Check agent logs for detailed error messages
2. Test basic connectivity with simple commands
3. Verify router configuration and access
4. Review RouterOS documentation for command syntax

## Advanced Features

### Network Topology Awareness
- Understands router relationships and roles
- Maintains configuration consistency across devices
- Coordinates changes to prevent network disruption

### Intelligent Command Processing
- Parses RouterOS responses intelligently
- Extracts meaningful information from verbose output
- Provides analysis and recommendations

### Session Context Management
- Maintains awareness of previous commands and changes
- Tracks network state across multiple operations
- Provides continuity across long troubleshooting sessions

## Security Considerations

### Access Control
- Uses your existing SSH credentials and permissions
- Inherits your network access rights
- Logs all commands for audit purposes

### Safe Operation
- Development environment uses auto-accepted certificates
- Production environments require proper certificate management
- All configuration changes are logged and can be audited

### Credential Management
- Router credentials stored in environment variables
- No hardcoded passwords in agent configuration
- Secure SSH key usage when available

---

**Start your session** with a simple request like "Connect to smsmk01 and give me a network overview" to begin managing your MikroTik infrastructure with AI assistance!