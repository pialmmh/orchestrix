# AI Guidelines for Orchestrix

**Purpose:** Comprehensive guidelines for AI-assisted development, automation, and infrastructure management in the Orchestrix project.

---

## 📁 Directory Structure

```
ai-guidelines/
├── README.md                    # This file - overview and quick reference
├── code-generation/             # Code generation and automation guidelines
│   └── CODE_GENERATION_AND_AUTOMATION_GUIDELINES.md
└── ssh/                         # SSH-based automation agents
    └── networking/              # Network device management agents
        └── mikrotik/           # MikroTik RouterOS agent
```

---

## 📚 What's Inside

### 1. Code Generation & Automation (`code-generation/`)

**File:** `CODE_GENERATION_AND_AUTOMATION_GUIDELINES.md`

Comprehensive guidelines for:
- **SSH-Based Automation Architecture** - Mandatory patterns for all automation
- **PTY (Pseudo-Terminal) Requirements** - When and how to use PTY
- **Java Code Generation Standards** - JSch, modern SSH, best practices
- **Container Build Automation** - LXC container patterns
- **Error Handling & Resilience** - Timeout, exit status, validation
- **Testing & Validation** - Pre/post-build checks

**Key Topics:**
- ✅ SSH-only automation (NO ProcessBuilder)
- ✅ Conditional PTY support
- ✅ JSch 0.2.20 with modern RSA signatures
- ✅ Persistent SSH sessions
- ✅ Exit status vs channel close

**Use When:** Writing Java automation code, SSH device integrations, container builders

---

### 2. SSH Automation Agents (`ssh/`)

**Purpose:** Specialized AI agents for infrastructure automation via SSH

**Current Agents:**
- **MikroTik RouterOS Agent** (`ssh/networking/mikrotik/`)
  - Network device management
  - Multi-router operations
  - Direct SSH access to routers

**Planned Agents:**
- Cisco IOS agent
- Juniper JunOS agent
- Linux server management
- Container orchestration

**Use When:** Managing network devices, routers, switches, or remote systems via SSH

---

## 🚀 Quick Start

### For Code Generation & Automation

```bash
# Read the guidelines
cat docs/ai-guidelines/code-generation/CODE_GENERATION_AND_AUTOMATION_GUIDELINES.md

# Key patterns to follow:
# 1. Always use SSH (even for localhost)
# 2. Use conditional PTY for lxc/apt-get commands
# 3. Check exit status, not channel close
# 4. Use JSch 0.2.20 with modern algorithms
```

### For SSH Agents

```bash
# Navigate to specific agent directory
cd docs/ai-guidelines/ssh/networking/mikrotik

# Start Claude Code session
claude-code

# In session:
"Connect to router and show interface status"
```

---

## 📖 Usage Scenarios

### Scenario 1: Building LXC Container with Java Automation

**Relevant Guide:** `code-generation/CODE_GENERATION_AND_AUTOMATION_GUIDELINES.md`

**What to follow:**
1. SSH-based architecture (Runner → Builder pattern)
2. PTY support for `lxc` commands
3. Exit status checking (not channel close)
4. Proper timeout configuration
5. Error handling patterns

**Example Reference:**
- See `/images/lxc/go-id/` for complete implementation
- `GoIdBuildRunner.java` - SSH session management
- `GoIdContainerBuilder.java` - Container automation

---

### Scenario 2: Managing Network Devices

**Relevant Agent:** `ssh/networking/mikrotik/`

**What to follow:**
1. Agent-specific instructions in `AGENT_INSTRUCTIONS.md`
2. Prerequisites in `README.md`
3. Connection patterns via Orchestrix backend

**Example Operations:**
- Connect to multiple routers
- Configure OSPF routing
- Create firewall rules
- Monitor system status

---

### Scenario 3: Implementing SSH Device Integration

**Relevant Guide:** `code-generation/CODE_GENERATION_AND_AUTOMATION_GUIDELINES.md` (SSH Architecture section)

**Key Implementation Points:**
```java
// 1. Create persistent SSH session
LocalSshDevice device = new LocalSshDevice();
device.connect();

// 2. Execute commands with automatic PTY detection
String result = device.executeCommand("lxc list");  // PTY auto-enabled

// 3. Explicit PTY control when needed
device.executeCommand("grep pattern file", false);  // PTY disabled

// 4. Close session when done
device.disconnect();
```

---

## 🔑 Key Principles

### Code Generation & Automation

1. **SSH-Only Automation**
   - ❌ Never use ProcessBuilder for system commands
   - ✅ Always use SSH (even for localhost)
   - ✅ Maintain persistent sessions

2. **PTY Management**
   - ✅ Auto-detect PTY need (lxc, apt-get, docker, systemctl)
   - ✅ Explicit control available when needed
   - ✅ Check exit status, not channel close

3. **Modern SSH Standards**
   - ✅ JSch 0.2.20 (not 0.1.55)
   - ✅ RSA SHA-256/512 signatures
   - ✅ Proper timeout configuration

### SSH Agents

1. **Specialized Focus**
   - Each agent has specific domain expertise
   - Clear capabilities and limitations
   - Direct access to relevant systems

2. **Safety First**
   - Test in development environments
   - Backup before changes
   - Validate modifications
   - Document all changes

3. **Service Dependencies**
   - Check prerequisites before starting
   - Ensure backend services running
   - Verify connectivity and credentials

---

## 📋 Common Patterns

### Pattern 1: SSH-Based Container Builder

```java
// Runner - Establishes connection
LocalSshDevice device = new LocalSshDevice();
device.connect();

// Builder - Uses connection
ContainerBuilder builder = new ContainerBuilder(device, config);
builder.build();

// Cleanup
device.disconnect();
```

### Pattern 2: Conditional PTY Execution

```java
// Auto-detection (recommended)
device.executeCommand("lxc init debian-12 test");  // PTY enabled
device.executeCommand("ls -la");                    // PTY disabled

// Explicit control
device.executeCommand("custom-command", true);      // Force PTY
```

### Pattern 3: Proper Timeout & Exit Status

```java
// Wait for exit status (not channel close)
while (channel.getExitStatus() == -1 && waited < maxWait) {
    Thread.sleep(100);
    waited++;
}

// Check timeout
if (channel.getExitStatus() == -1) {
    throw new Exception("Command timeout");
}

// Check exit code
int exitStatus = channel.getExitStatus();
if (exitStatus != 0) {
    throw new Exception("Command failed: " + exitStatus);
}
```

---

## 🛠️ Tools & Technologies

### Code Generation
- **Language:** Java 21
- **SSH Library:** JSch 0.2.20 (com.github.mwiede)
- **Build Tool:** Maven
- **Containers:** LXC/LXD 6.5
- **Storage:** BTRFS with quotas

### SSH Agents
- **Backend:** Spring Boot 2.7.14
- **Database:** MySQL 8.0
- **Connectivity:** SSH via Orchestrix backend
- **Devices:** MikroTik RouterOS (currently)

---

## 📝 Contributing

### Adding Code Generation Guidelines

1. Update `code-generation/CODE_GENERATION_AND_AUTOMATION_GUIDELINES.md`
2. Add examples and patterns
3. Document common pitfalls
4. Include working code samples

### Creating New SSH Agents

1. Create directory: `ssh/[category]/[agent-name]/`
2. Required files:
   - `AGENT_INSTRUCTIONS.md` - Complete agent behavior
   - `README.md` - Usage guide and prerequisites
3. Follow existing agent structure (see `ssh/networking/mikrotik/`)

---

## 🔍 Quick Reference

### File Locations

| What | Where |
|------|-------|
| Code generation guidelines | `code-generation/CODE_GENERATION_AND_AUTOMATION_GUIDELINES.md` |
| MikroTik agent | `ssh/networking/mikrotik/AGENT_INSTRUCTIONS.md` |
| This overview | `README.md` (this file) |

### Common Commands

```bash
# Read code generation guidelines
cat docs/ai-guidelines/code-generation/CODE_GENERATION_AND_AUTOMATION_GUIDELINES.md

# Start MikroTik agent
cd docs/ai-guidelines/ssh/networking/mikrotik && claude-code

# Check agent prerequisites
cat docs/ai-guidelines/ssh/networking/mikrotik/README.md
```

---

## 🎯 When to Use Each Guide

| Task | Use This Guide |
|------|----------------|
| Build LXC container with Java | `code-generation/` |
| SSH automation patterns | `code-generation/` (SSH Architecture) |
| PTY requirements | `code-generation/` (PTY Requirements) |
| JSch configuration | `code-generation/` (Java Standards) |
| Manage MikroTik routers | `ssh/networking/mikrotik/` |
| Network device automation | `ssh/networking/` |

---

**Last Updated:** 2025-10-05

For questions or suggestions, update this documentation and create a PR.
