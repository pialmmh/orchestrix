# MikroTik Networking Agent Setup

## How to Use This Agent

### Option 1: Direct Agent Session
```bash
cd /home/mustafa/telcobright-projects/orchestrix/ai-agents/ssh/networking
claude-code
```

### Option 2: From Main Orchestrix Directory
```bash
cd /home/mustafa/telcobright-projects/orchestrix
claude-code --agent ai-agents/ssh/networking
```

### Option 3: Context Reference
In any Claude Code session, reference this agent:
```
Please act as the MikroTik networking agent defined in ai-agents/ssh/networking/AGENT_INSTRUCTIONS.md
```

## Prerequisites

### 1. Backend Running
```bash
# Start the Orchestrix backend
cd /home/mustafa/telcobright-projects/orchestrix
mvn spring-boot:run
```

### 2. Router Credentials
The agent has access to:
- smsmk01: 114.130.145.70:22 (admin/Takay1#$ane%%)
- Additional routers to be configured

### 3. Java Backend Services
- DeviceManager: Multi-router operations
- MikroTikService: RouterOS-specific commands  
- REST endpoints: /api/devices/* and /api/mikrotik/*

## Session Examples

### Connect and Get Status
```
Agent: Connect to smsmk01 and show me the current system status
```

### Multi-Router Operations  
```
Agent: Show interface status on all connected routers
```

### Configuration Tasks
```
Agent: Configure OSPF area 0 on the 192.168.1.0/24 network for smsmk01
```

### Monitoring
```
Agent: Monitor CPU and memory usage across all routers
```

## Agent Capabilities

✅ **Router Management**: Connect, disconnect, status monitoring  
✅ **Command Execution**: Direct RouterOS command execution  
✅ **Configuration**: Interface, IP, routing, OSPF, firewall setup  
✅ **Multi-Router**: Broadcast commands, compare configs  
✅ **Safety**: Configuration backup, error handling  
✅ **Context Awareness**: Network topology understanding  

## Files in This Directory

- `AGENT_INSTRUCTIONS.md` - Complete agent behavior and knowledge
- `README.md` - This usage guide
- `router-context.json` - (Future) Router state and topology data
- `command-history.log` - (Future) Session command logging

## Quick Start

1. **Start backend**: `mvn spring-boot:run` in orchestrix directory
2. **Start agent session**: `cd ai-agents/ssh/networking && claude-code`  
3. **Begin with**: "Connect to smsmk01 and show me the network overview"

The agent will handle all backend integration automatically and provide expert RouterOS assistance!