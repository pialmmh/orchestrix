# Automation Package Refactoring Plan

## Overview
Refactor automation package into 3 clear packages:
1. **api/** - Public interfaces and contracts
2. **core/** - Internal reusable logic
3. **example/** - Example implementations and runners

## Current Structure (92 Java files)
```
automation/
├── core/
│   ├── BaseAutomation.java
│   └── LocalCommandExecutor.java
├── model/
│   ├── AutomationConfig.java
│   ├── AutomationOperationResult.java
│   └── CommandResult.java
├── devices/
│   ├── networking/mikrotik/
│   └── server/linux/...
├── storage/
│   ├── base/
│   ├── btrfs/
│   └── runners/
├── containers/
│   └── quarkus/
├── runners/
├── runner/storage/
└── [Top-level runners: SshRunner.java, TerminalRunner.java, etc.]
```

## New Structure

### 1. api/ - Public API Package
**Purpose**: Clear, documented interfaces for external consumers

```
automation/api/
├── AutomationAPI.java                    # Main facade interface
├── LinuxAutomationAPI.java              # Linux-specific automation
├── ContainerAutomationAPI.java          # Container operations
├── StorageAutomationAPI.java            # Storage operations
├── NetworkAutomationAPI.java            # Network device automation
└── model/                               # API models/DTOs
    ├── AutomationConfig.java
    ├── AutomationOperationResult.java
    └── CommandResult.java
```

**Requirements**:
- All API methods must have JavaDoc comments
- Clear method signatures
- Examples in documentation
- No implementation details exposed

### 2. core/ - Core Internal Logic
**Purpose**: Reusable internal implementations

```
automation/core/
├── base/                                # Base classes
│   ├── BaseAutomation.java
│   ├── AbstractLinuxAutomation.java
│   ├── LocalCommandExecutor.java
│   └── AutomationContext.java
├── linux/                               # Linux automation logic
│   ├── base/
│   │   ├── LinuxDistribution.java
│   │   └── LinuxAutomation.java
│   ├── lxd/
│   ├── lxdbridge/
│   ├── packageinstall/
│   ├── snapinstall/
│   ├── ipforward/
│   ├── servicemanagement/
│   └── networkconfig/
├── container/                           # Container deployment logic
│   ├── base/
│   │   ├── ContainerDeploymentAutomation.java
│   │   ├── AbstractContainerDeployment.java
│   │   ├── ContainerConfig.java
│   │   ├── ContainerInfo.java
│   │   └── ContainerStatus.java
│   ├── lxc/
│   ├── docker/
│   ├── kubernetes/
│   └── podman/
├── storage/                             # Storage automation
│   ├── base/
│   │   ├── StorageProvider.java
│   │   ├── StorageVolume.java
│   │   ├── StorageVolumeConfig.java
│   │   ├── StorageLocation.java
│   │   └── StorageTechnology.java
│   └── btrfs/
│       ├── BtrfsStorageProvider.java
│       ├── BtrfsInstallAutomation.java
│       └── LxcContainerBtrfsMountAutomation.java
├── network/                             # Network automation
│   └── mikrotik/
│       ├── StaticRouteCreatorSimple.java
│       └── DstNatIpToIp.java
└── util/                                # Internal utilities
    └── SystemDetector.java
```

### 3. example/ - Examples and Runners
**Purpose**: Demonstrate API usage with practical examples

```
automation/example/
├── runners/                             # Terminal/SSH runners
│   ├── SshRunner.java
│   ├── TerminalRunner.java
│   ├── SshSeriesAutomationRunner.java
│   ├── TerminalSeriesAutomationRunner.java
│   ├── LinuxIpForwardEnablerTerminalRunner.java
│   ├── LxcLxdInstallTerminalRunner.java
│   ├── LxdBridgeNetworkingTerminalRunner.java
│   └── SnapInstallerTerminalRunner.java
├── containers/                          # Container builders
│   └── quarkus/
│       ├── QuarkusBaseContainerBuilder.java
│       └── QuarkusAppContainerBuilder.java
└── storage/                             # Storage examples
    └── GrafanaLokiBuildRunner.java
```

## Migration Steps

### Phase 1: Create API Package
1. Create `automation/api/` structure
2. Define API interfaces with full JavaDoc
3. Create API facade classes
4. Move model classes to `api/model/`

### Phase 2: Reorganize Core
1. Create `automation/core/base/`
2. Move base classes from current `core/` and `devices/server/linux/base/`
3. Create `automation/core/linux/` and move all Linux automation
4. Create `automation/core/container/` and move all container logic
5. Create `automation/core/storage/` and move storage logic
6. Create `automation/core/network/` and move MikroTik automation
7. Create `automation/core/util/` for utilities

### Phase 3: Create Examples Package
1. Create `automation/example/` structure
2. Move all runners to `example/runners/`
3. Move container builders to `example/containers/`
4. Move storage runners to `example/storage/`

### Phase 4: Update Imports
1. Update all import statements across the codebase
2. Update package declarations
3. Fix compilation errors
4. Run full build and tests

### Phase 5: Clean Up
1. Delete old empty directories
2. Update documentation
3. Create migration guide for external consumers

## API Documentation Standards

Every API method must have:
```java
/**
 * Brief one-line description of what this method does.
 *
 * <p>More detailed explanation if needed, including:
 * - What the method accomplishes
 * - Any important behavior or side effects
 * - Example use cases
 *
 * @param paramName Description of parameter
 * @return Description of return value
 * @throws ExceptionType When and why this exception is thrown
 *
 * @example
 * <pre>
 * AutomationAPI api = new AutomationAPI();
 * AutomationOperationResult result = api.methodName(param);
 * if (result.isSuccess()) {
 *     // Handle success
 * }
 * </pre>
 */
public AutomationOperationResult methodName(String paramName) throws Exception {
    // Implementation
}
```

## Breaking Changes
- Package names will change for all classes
- External consumers must update imports
- Provide migration script or guide

## Benefits
1. **Clear API Boundaries**: Anyone can see available APIs at a glance
2. **Better Organization**: Core logic separated from examples
3. **Easier Maintenance**: Clear separation of concerns
4. **Better Documentation**: API methods fully documented
5. **Easier Testing**: Clear interfaces for mocking
6. **Example-Driven Learning**: Examples show how to use the API

## Estimated Impact
- Files to move: ~92 Java files
- New API interfaces: ~5-10 files
- Updated imports: ~200+ import statements
- Documentation: ~100+ JavaDoc comments

## Timeline
1. Phase 1 (API): 2-3 hours
2. Phase 2 (Core): 3-4 hours
3. Phase 3 (Examples): 1-2 hours
4. Phase 4 (Imports): 2-3 hours
5. Phase 5 (Cleanup): 1 hour

**Total: 9-13 hours of work**

## Approval Required
This is a major refactoring. Please review and approve before proceeding.
