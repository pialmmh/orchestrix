# Orchestrix Automation Guidelines

## Overview
This document provides guidelines for AI agents and developers working with the Orchestrix automation framework. All automation code should follow these patterns to ensure consistency, reusability, and maintainability.

## Project Structure

### Package Organization
All automation code resides in `/home/mustafa/telcobright-projects/orchestrix/automation/` with the following structure:

```
automation/
└── src/main/java/com/orchestrix/automation/
    ├── ssh/          # SSH automation
    ├── expectj/      # ExpectJ interactive automation
    ├── vxlan/        # VXLAN service implementation
    └── [module]/     # Future modules
        ├── api/      # Interfaces and contracts
        ├── core/     # Base implementations
        └── example/  # Example usage
```

### Package Guidelines

#### 1. API Package (`api/`)
- **Purpose**: Define interfaces and contracts
- **Contents**:
  - Service interfaces
  - Data transfer objects (DTOs)
  - Exception classes
  - Constants and enums
- **Rules**:
  - No implementation logic
  - All methods must have Javadoc
  - Use nested classes for related DTOs

#### 2. Core Package (`core/`)
- **Purpose**: Provide base implementations
- **Contents**:
  - Abstract base classes
  - Common utility methods
  - Shared logic
- **Rules**:
  - Extend from API interfaces
  - Provide hooks for customization (Template Method pattern)
  - Handle resource cleanup properly
  - Use try-with-resources where applicable

#### 3. Example Package (`example/`)
- **Purpose**: Demonstrate usage patterns
- **Contents**:
  - Concrete implementations
  - Main methods with examples
  - Test scenarios
- **Rules**:
  - Self-contained examples
  - Cover common use cases
  - Include error handling

## Design Patterns

### 1. Template Method Pattern
Use for operations with common structure but varying details:

```java
public abstract class BaseService {
    public final void execute() {
        beforeExecute();
        doExecute();
        afterExecute();
    }
    
    protected abstract void doExecute();
    protected void beforeExecute() { /* Optional hook */ }
    protected void afterExecute() { /* Optional hook */ }
}
```

### 2. Builder Pattern
Use for complex configuration objects:

```java
VXLANConfig config = VXLANConfig.builder()
    .vxlanId(100)
    .multicastGroup("239.1.1.1")
    .mtu(1450)
    .build();
```

### 3. Factory Pattern
Use for creating implementation instances:

```java
public class ClientFactory {
    public static SSHClient createSSHClient(String type) {
        switch(type) {
            case "standard": return new StandardSSHClient();
            case "advanced": return new AdvancedSSHClient();
            default: throw new IllegalArgumentException();
        }
    }
}
```

## Configuration Management

### 1. Configuration Files
- Primary config: `/home/mustafa/telcobright-projects/orchestrix/pulumi/pulumi.conf`
- Format: Key-value pairs with sections
- Support environment variable overrides

### 2. Configuration Loading
```java
protected void loadConfiguration() {
    // 1. Load from file
    // 2. Override with environment variables
    // 3. Apply system properties
}
```

### 3. Security Considerations
- Never hardcode credentials
- Support multiple authentication methods
- Use secure storage for sensitive data (future)

## Error Handling

### 1. Exception Hierarchy
```java
AutomationException (base)
├── SSHException
├── ExpectException
├── VXLANException
└── ConfigurationException
```

### 2. Error Handling Rules
- Wrap low-level exceptions
- Provide context in error messages
- Clean up resources in finally blocks
- Use try-with-resources for AutoCloseable

### 3. Logging
```java
// Use SLF4J for logging
private static final Logger logger = LoggerFactory.getLogger(MyClass.class);

logger.debug("Detailed information");
logger.info("Important events");
logger.warn("Potential issues");
logger.error("Errors", exception);
```

## Resource Management

### 1. Connection Pooling
- Reuse SSH connections
- Implement connection timeout
- Handle reconnection automatically

### 2. Cleanup
- Implement AutoCloseable
- Release resources in reverse order
- Handle cleanup errors gracefully

## Testing Guidelines

### 1. Unit Tests
- Test each module independently
- Mock external dependencies
- Cover edge cases

### 2. Integration Tests
- Test with real systems when possible
- Use Docker/LXC for isolated testing
- Verify cleanup operations

## Code Style

### 1. Naming Conventions
- Interfaces: `SSHClient`, `VXLANService`
- Base classes: `BaseSSHClient`, `BaseVXLANService`
- Implementations: `OrchestriXSSHClient`
- Exceptions: `SSHException`, `VXLANException`

### 2. Method Organization
```java
public class MyClass {
    // Constants
    // Fields
    // Constructors
    // Public methods
    // Protected methods
    // Private methods
    // Inner classes
}
```

### 3. Documentation
- All public APIs must have Javadoc
- Include usage examples
- Document exceptions thrown
- Add @since tags for new features

## Common Libraries

### Required Dependencies (pom.xml)
```xml
<dependencies>
    <!-- SSH -->
    <dependency>
        <groupId>com.jcraft</groupId>
        <artifactId>jsch</artifactId>
        <version>0.1.55</version>
    </dependency>
    
    <!-- ExpectJ -->
    <dependency>
        <groupId>net.sourceforge.expectj</groupId>
        <artifactId>expectj</artifactId>
        <version>2.0.7</version>
    </dependency>
    
    <!-- Logging -->
    <dependency>
        <groupId>org.slf4j</groupId>
        <artifactId>slf4j-api</artifactId>
        <version>2.0.9</version>
    </dependency>
    
    <!-- Testing -->
    <dependency>
        <groupId>junit</groupId>
        <artifactId>junit</artifactId>
        <version>4.13.2</version>
        <scope>test</scope>
    </dependency>
</dependencies>
```

## Adding New Modules

### Steps to Add a New Automation Module

1. **Create Package Structure**:
```bash
mkdir -p automation/src/main/java/com/orchestrix/automation/[module]/{api,core,example}
```

2. **Define API Interface**:
```java
package com.orchestrix.automation.[module].api;

public interface [Module]Service {
    // Define contract
}
```

3. **Create Base Implementation**:
```java
package com.orchestrix.automation.[module].core;

public abstract class Base[Module]Service implements [Module]Service {
    // Common implementation
}
```

4. **Add Examples**:
```java
package com.orchestrix.automation.[module].example;

public class [Module]Example {
    public static void main(String[] args) {
        // Usage examples
    }
}
```

5. **Update Documentation**:
- Add module description to this file
- Create README in module directory
- Update main project documentation

## Best Practices

### 1. Reusability
- Extract common logic to base classes
- Use composition over inheritance
- Create utility classes for shared functions

### 2. Flexibility
- Support multiple configuration sources
- Provide sensible defaults
- Allow runtime customization

### 3. Reliability
- Implement retry logic for network operations
- Add timeouts to prevent hanging
- Validate inputs early

### 4. Observability
- Log important operations
- Expose metrics for monitoring
- Provide health check endpoints

### 5. Security
- Never log sensitive data
- Validate all external inputs
- Use secure defaults
- Support encryption where needed

## Example Implementation Checklist

When implementing a new service:

- [ ] Define clear API interface
- [ ] Create abstract base class with hooks
- [ ] Implement resource management (AutoCloseable)
- [ ] Add comprehensive error handling
- [ ] Load configuration from standard location
- [ ] Provide usage examples
- [ ] Document all public methods
- [ ] Add unit tests
- [ ] Update this guidelines document

## Common Pitfalls to Avoid

1. **Hardcoding values** - Use configuration
2. **Ignoring cleanup** - Always release resources
3. **Swallowing exceptions** - Log and rethrow appropriately
4. **Blocking operations** - Use timeouts
5. **Tight coupling** - Use interfaces and dependency injection
6. **Missing documentation** - Document as you code
7. **Inconsistent naming** - Follow conventions
8. **Resource leaks** - Use try-with-resources

## Support and Questions

For questions or clarifications:
1. Check existing examples in `example/` packages
2. Review base implementations in `core/` packages
3. Consult API documentation in `api/` packages
4. Follow patterns from existing modules

## Version History

- v1.0.0 (2024-01): Initial automation framework
  - SSH automation support
  - ExpectJ integration
  - VXLAN service implementation

---

**Note for AI Agents**: When working with this codebase, always follow these guidelines to maintain consistency. Create reusable components that future implementations can build upon. Focus on clean abstractions and proper separation of concerns.