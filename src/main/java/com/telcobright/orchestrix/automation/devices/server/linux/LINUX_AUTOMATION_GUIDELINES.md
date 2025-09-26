# Linux Automation Implementation Guidelines

## Purpose
This document provides guidelines for implementing Linux automations in the Orchestrix project. Both AI assistants and human developers should follow these patterns to maintain consistency.

## Core Architecture

### Three-Layer Architecture

```
┌─────────────────────────────────────────────┐
│         COMPOSITE AUTOMATIONS               │  Layer 3: Orchestration
│   (Combine multiple unit automations)       │
└─────────────────────────────────────────────┘
                    ↓
┌─────────────────────────────────────────────┐
│          UNIT AUTOMATIONS                   │  Layer 2: Single-Purpose Tasks
│   (Individual, reusable components)         │
└─────────────────────────────────────────────┘
                    ↓
┌─────────────────────────────────────────────┐
│         BASE INFRASTRUCTURE                 │  Layer 1: Core Framework
│   (Abstract classes, interfaces, utils)     │
└─────────────────────────────────────────────┘
```

## Directory Organization

```
src/main/java/com/telcobright/orchestrix/automation/devices/server/linux/
│
├── base/                                 # Layer 1: Core Framework
│   ├── LinuxAutomation.java            # Base interface ALL automations extend
│   ├── AbstractLinuxAutomation.java    # Abstract class with common functionality
│   ├── LinuxDistribution.java          # Enum of supported distributions
│   └── SystemDetector.java             # Utility to detect Linux distribution
│
├── common/                              # Shared utilities
│   └── SystemDetector.java             # OS detection logic
│
├── composite/                           # Layer 3: Composite Automations
│   └── Configure[Feature]Automation.java  # Orchestrates multiple units
│
└── [automation-type]/                  # Layer 2: Unit Automations
    ├── [Type]Automation.java           # Interface defining the contract
    ├── [Type]AutomationFactory.java    # Factory for creating instances
    └── impl/                            # Distribution-specific implementations
        ├── Ubuntu2204[Type]Automation.java
        ├── Debian12[Type]Automation.java
        ├── CentOS7[Type]Automation.java
        ├── RHEL8[Type]Automation.java
        └── Default[Type]Automation.java    # Fallback for unknown distros
```

## Implementation Rules

### 1. Unit Automations (Single Responsibility)

**RULE**: Each unit automation does ONE thing only.

**Good Examples**:
- `LxdInstallerAutomation` - ONLY installs LXD
- `IpForwardAutomation` - ONLY enables IP forwarding
- `SnapInstallAutomation` - ONLY manages snap packages

**Bad Example**:
- `LxdSetupAutomation` - Does installation, configuration, and networking (too many responsibilities)

### 2. Distribution-Specific Implementations

**RULE**: Create specific implementations for each supported distribution.

**Pattern**:
```java
Ubuntu2204[Type]Automation  // Ubuntu 22.04 specific
Debian12[Type]Automation     // Debian 12 specific
CentOS7[Type]Automation      // CentOS 7 specific
RHEL8[Type]Automation        // RHEL 8 specific
Default[Type]Automation      // Fallback for any Linux
```

**Implementation Example**:
```java
public class Ubuntu2204PackageInstallAutomation extends AbstractLinuxAutomation
    implements PackageInstallAutomation {

    @Override
    protected String getPackageManagerCommand() {
        return "apt";  // Ubuntu uses apt
    }
}

public class CentOS7PackageInstallAutomation extends AbstractLinuxAutomation
    implements PackageInstallAutomation {

    @Override
    protected String getPackageManagerCommand() {
        return "yum";  // CentOS 7 uses yum
    }
}
```

### 3. Factory Pattern

**RULE**: Every unit automation MUST have a factory.

**Factory Template**:
```java
public class [Type]AutomationFactory {

    public static [Type]Automation create[Type]Automation(
            LinuxDistribution distribution, boolean useSudo) {

        switch (distribution.getFamily()) {
            case DEBIAN:
                if (distribution == LinuxDistribution.UBUNTU_2204)
                    return new Ubuntu2204[Type]Automation(useSudo);
                if (distribution == LinuxDistribution.DEBIAN_12)
                    return new Debian12[Type]Automation(useSudo);
                break;
            case RHEL:
                if (distribution == LinuxDistribution.CENTOS_7)
                    return new CentOS7[Type]Automation(useSudo);
                if (distribution == LinuxDistribution.RHEL_8)
                    return new RHEL8[Type]Automation(useSudo);
                break;
        }

        return new Default[Type]Automation(useSudo);  // Fallback
    }
}
```

### 4. Composite Automations

**RULE**: Complex tasks use composite automations that orchestrate unit automations.

**Example Structure**:
```java
public class ConfigureLxdDefaultBridgeAutomation extends AbstractLinuxAutomation {

    // Unit automations as dependencies
    private LxdInstallerAutomation lxdInstaller;
    private SnapInstallAutomation snapInstaller;
    private IpForwardAutomation ipForward;
    private LxdBridgeConfigureAutomation bridgeConfig;

    public ConfigureLxdDefaultBridgeAutomation(LinuxDistribution dist, boolean sudo) {
        // Use factories to get correct implementations
        this.lxdInstaller = LxdInstallerAutomationFactory.create(dist, sudo);
        this.snapInstaller = SnapInstallAutomationFactory.create(dist, sudo);
        this.ipForward = IpForwardAutomationFactory.create(dist, sudo);
        this.bridgeConfig = LxdBridgeConfigureAutomationFactory.create(dist, sudo);
    }

    @Override
    public boolean execute(SshDevice device) {
        // Step 1: Install LXD
        if (!lxdInstaller.execute(device)) return false;

        // Step 2: Configure snap if needed
        if (snapInstaller.isSnapInstalled(device)) {
            snapInstaller.configureSnapPath(device);
        }

        // Step 3: Enable IP forwarding
        if (!ipForward.execute(device)) return false;

        // Step 4: Configure bridge
        return bridgeConfig.execute(device);
    }
}
```

## Naming Conventions

### Package Names
- **Pattern**: lowercase, no underscores, no version numbers
- **Examples**: `ipforward`, `lxdinstall`, `snapinstall`, `firewall`

### Interface Names
- **Pattern**: `[Function]Automation`
- **Examples**: `IpForwardAutomation`, `LxdInstallerAutomation`

### Implementation Classes
- **Pattern**: `[Distribution][Version][Function]Automation`
- **Examples**: `Ubuntu2204IpForwardAutomation`, `Debian12LxdInstallerAutomation`

### Factory Classes
- **Pattern**: `[Function]AutomationFactory`
- **Examples**: `IpForwardAutomationFactory`, `LxdInstallerAutomationFactory`

### Composite Classes
- **Pattern**: `Configure[Feature]Automation`
- **Examples**: `ConfigureLxdDefaultBridgeAutomation`, `ConfigureWebServerAutomation`

## Method Patterns

### Unit Automation Interface Methods

```java
public interface [Function]Automation extends LinuxAutomation {
    // Check state
    boolean is[Thing]Installed(SshDevice device);
    boolean is[Thing]Running(SshDevice device);
    boolean is[Thing]Configured(SshDevice device);

    // Perform actions
    boolean install[Thing](SshDevice device);
    boolean configure[Thing](SshDevice device, ConfigObject config);
    boolean start[Thing](SshDevice device);
    boolean stop[Thing](SshDevice device);

    // Get information
    String get[Thing]Version(SshDevice device);
    String get[Thing]Status(SshDevice device);
    List<String> list[Things](SshDevice device);
}
```

## Common Implementations

### Package Installation
```java
// Ubuntu/Debian
executeCommand(device, "apt-get update");
executeCommand(device, "apt-get install -y " + packageName);

// CentOS/RHEL
executeCommand(device, "yum install -y " + packageName);

// Check installation
executeCommand(device, "which " + commandName);
```

### Service Management
```java
// Systemd (most modern Linux)
executeCommand(device, "systemctl start " + serviceName);
executeCommand(device, "systemctl enable " + serviceName);
executeCommand(device, "systemctl status " + serviceName);

// SysV Init (older systems)
executeCommand(device, "service " + serviceName + " start");
executeCommand(device, "chkconfig " + serviceName + " on");
```

### Configuration Files
```java
// Check if file exists
executeCommand(device, "test -f " + filePath + " && echo exists");

// Backup before modifying
executeCommand(device, "cp " + filePath + " " + filePath + ".bak");

// Modify configuration
executeCommand(device, "sed -i 's/old/new/g' " + filePath);
```

## When to Create What

### Create a Unit Automation When:
1. You have a single, specific task (install X, configure Y)
2. The task is reusable across different contexts
3. The implementation varies by distribution
4. It can be tested independently

### Create a Composite Automation When:
1. You need to orchestrate multiple unit automations
2. There's a specific sequence or workflow
3. You need coordinated error handling
4. The combined result represents a complete feature

### Use Existing Automation When:
1. The functionality already exists
2. Minor modification can achieve your goal
3. The existing automation covers your use case

## Distribution Support

### Currently Supported
| Distribution | Constant | Family | Package Manager |
|-------------|----------|--------|-----------------|
| Ubuntu 22.04 | UBUNTU_2204 | DEBIAN | apt |
| Ubuntu 20.04 | UBUNTU_2004 | DEBIAN | apt |
| Debian 12 | DEBIAN_12 | DEBIAN | apt |
| Debian 11 | DEBIAN_11 | DEBIAN | apt |
| CentOS 7 | CENTOS_7 | RHEL | yum |
| RHEL 8 | RHEL_8 | RHEL | dnf/yum |
| RHEL 9 | RHEL_9 | RHEL | dnf |

### Adding New Distribution Support
1. Add constant to `LinuxDistribution.java`
2. Create implementation class for each unit automation
3. Update factories to include new distribution
4. Test on actual distribution

## Examples of Proper Implementation

### Example 1: Creating a New Unit Automation

**Task**: Create automation to manage system timezone

**Step 1**: Create interface
```java
// File: timezone/TimezoneAutomation.java
public interface TimezoneAutomation extends LinuxAutomation {
    boolean setTimezone(SshDevice device, String timezone);
    String getCurrentTimezone(SshDevice device);
    List<String> listAvailableTimezones(SshDevice device);
}
```

**Step 2**: Create implementations for each distribution
```java
// File: timezone/impl/Ubuntu2204TimezoneAutomation.java
public class Ubuntu2204TimezoneAutomation extends AbstractLinuxAutomation
    implements TimezoneAutomation {

    @Override
    public boolean setTimezone(SshDevice device, String timezone) {
        return executeCommand(device, "timedatectl set-timezone " + timezone) != null;
    }
}
```

**Step 3**: Create factory
```java
// File: timezone/TimezoneAutomationFactory.java
public class TimezoneAutomationFactory {
    public static TimezoneAutomation createTimezoneAutomation(...) {
        // Factory implementation
    }
}
```

### Example 2: Using in Composite

```java
public class ConfigureServerEnvironmentAutomation extends AbstractLinuxAutomation {
    private TimezoneAutomation timezone;
    private LocaleAutomation locale;
    private NtpAutomation ntp;

    @Override
    public boolean execute(SshDevice device) {
        // Set timezone
        if (!timezone.setTimezone(device, "UTC")) return false;

        // Configure locale
        if (!locale.setLocale(device, "en_US.UTF-8")) return false;

        // Setup NTP
        return ntp.configureNtp(device);
    }
}
```

## Testing Guidelines

### Unit Testing
- Test each distribution implementation separately
- Mock SshDevice for unit tests
- Verify command construction
- Test error handling

### Integration Testing
- Test on actual Linux distributions
- Use Docker/LXC containers for testing
- Verify idempotency (run multiple times)
- Test failure scenarios

## Common Pitfalls to Avoid

1. **DON'T** combine multiple responsibilities in one automation
2. **DON'T** hardcode distribution-specific commands in base classes
3. **DON'T** skip the factory pattern
4. **DON'T** forget the Default implementation
5. **DON'T** use distribution version in package names
6. **DON'T** mix composite and unit logic

## Checklist for New Automations

- [ ] Single responsibility defined
- [ ] Interface extends LinuxAutomation
- [ ] Implementation for each supported distribution
- [ ] Default implementation for unknown distributions
- [ ] Factory class created
- [ ] Factory includes all distributions
- [ ] Package name follows convention
- [ ] Class names follow convention
- [ ] Methods follow naming patterns
- [ ] Error handling implemented
- [ ] Logging added for debugging
- [ ] Idempotent (safe to run multiple times)
- [ ] Verify method accurately checks state
- [ ] Documentation comments added

## Summary

Follow these guidelines to ensure:
1. **Consistency** across all automations
2. **Reusability** of components
3. **Maintainability** of code
4. **Extensibility** for new distributions
5. **Testability** of individual components

When in doubt, look at existing implementations like:
- `ipforward/` - Simple unit automation
- `lxdinstall/` - Installation automation
- `composite/ConfigureLxdDefaultBridgeAutomation` - Composite example