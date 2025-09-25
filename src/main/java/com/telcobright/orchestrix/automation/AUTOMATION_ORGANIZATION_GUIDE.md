# Automation Classes Organization Guide

## Overview
This document defines the standard package structure and organization for automation classes in the Orchestrix project.

## Package Structure

```
com.telcobright.orchestrix.automation/
├── core/                          # Core automation framework components
│   ├── BaseAutomation.java       # Base class for all automations
│   └── LocalCommandExecutor.java # Local command execution utilities
│
├── model/                         # Data models and configurations
│   ├── AutomationConfig.java     # Configuration models
│   └── CommandResult.java        # Result models
│
├── runners/                       # Runner implementations for series execution
│   ├── *TerminalRunner.java      # Implements TerminalRunner interface
│   └── *Runner.java              # Legacy runners (to be migrated)
│
├── devices/                       # Device-specific automations
│   ├── networking/               # Network device automations
│   │   └── mikrotik/            # MikroTik router automations
│   │       ├── DstNatIpToIp.java
│   │       ├── StaticRouteCreatorSimple.java
│   │       └── VpnConfigurator.java (future)
│   │
│   └── server/                   # Server automations
│       ├── platform/            # Platform-level installations
│       │   ├── LxcLxdInstallDebian12Automation.java
│       │   ├── LxdBridgeNetworkingAutomation.java
│       │   ├── DockerInstallAutomation.java (future)
│       │   └── SnapInstallAutomation.java (future)
│       │
│       └── container/           # Container-specific automations
│           └── deployment/      # Container deployment automations
│               ├── lxc/        # LXC container deployments
│               │   ├── UniqueIdGeneratorDeployment.java (future)
│               │   └── FreeSwitchDeployment.java (future)
│               │
│               └── docker/      # Docker container deployments
│                   └── GrafanaLokiDeployment.java (future)
│
├── TerminalRunner.java           # Interface for terminal-based runners
├── TerminalSeriesAutomationRunner.java  # Series execution coordinator
├── SshRunner.java                # Legacy SSH runner interface
└── SshSeriesAutomationRunner.java # Legacy series runner
```

## Naming Conventions

### Package Names
- All lowercase
- Use plural for categories (devices, runners)
- Use singular for specific implementations (mikrotik, docker)

### Class Names
1. **Automation Classes**: `{Purpose}{Target}Automation`
   - Example: `LxcLxdInstallDebian12Automation`
   - Example: `DockerInstallAutomation`

2. **Runner Classes**: `{Purpose}TerminalRunner` or `{Purpose}Runner`
   - Example: `LxdBridgeNetworkingTerminalRunner`
   - Example: `LxcLxdInstallTerminalRunner`

3. **Deployment Classes**: `{Application}Deployment`
   - Example: `UniqueIdGeneratorDeployment`
   - Example: `GrafanaLokiDeployment`

## Organization Rules

### 1. Device Type Separation
Automations are first organized by device type:
- **networking**: Network devices (routers, switches)
- **server**: Server/host machines

### 2. Function-Based Grouping
Within device types, group by function:
- **platform**: System-level installations and configurations
- **container**: Container-specific operations
- **deployment**: Application deployments

### 3. Technology Separation
Further organize by specific technology:
- **mikrotik**: MikroTik-specific automations
- **lxc**: LXC container operations
- **docker**: Docker container operations

## Migration Guidelines

When adding new automation classes:

1. **Determine Device Type**: Is it for networking equipment or servers?
2. **Identify Function**: Platform setup, container management, or deployment?
3. **Select Technology**: What specific technology does it target?
4. **Place in Correct Package**: Follow the structure above
5. **Update Imports**: Ensure all imports reflect new package location
6. **Create Runner if Needed**: Add corresponding runner in `runners/` package

## Import Updates

When moving classes, update imports in:
- The moved class itself (package declaration)
- Runner implementations that use the automation
- Test classes
- Any other classes that reference the automation

### Example Import Changes:
```java
// Old
import com.telcobright.orchestrix.automation.LxcLxdInstallDebian12Automation;

// New
import com.telcobright.orchestrix.automation.devices.server.platform.LxcLxdInstallDebian12Automation;
```

## Future Additions

### Planned Automations to Add:

1. **Networking**
   - `VpnConfigurator` (MikroTik VPN setup)
   - `FirewallRuleManager` (Firewall configuration)

2. **Server Platform**
   - `DockerInstallAutomation` (Docker installation)
   - `SnapInstallAutomation` (Snap package manager)
   - `KubernetesInstallAutomation` (K8s setup)

3. **Container Deployments**
   - LXC: `UniqueIdGeneratorDeployment`, `FreeSwitchDeployment`
   - Docker: `GrafanaLokiDeployment`, `PrometheusDeployment`

## Benefits of This Structure

1. **Clear Organization**: Easy to locate automations by type and function
2. **Scalability**: Structure supports growth without reorganization
3. **Maintainability**: Related automations are grouped together
4. **Reusability**: Common patterns in each category can be shared
5. **Discovery**: New developers can quickly understand the codebase

## Testing Structure

Test classes should mirror the main structure:
```
testrunners/
├── networking/
│   └── mikrotik/
├── server/
│   ├── platform/
│   └── container/
└── series/           # Series automation tests
```