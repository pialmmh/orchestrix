# Orchestrix Global Configuration

This directory contains global configuration files for the Orchestrix project.

## Configuration Files

### java.conf
Global Java environment settings:
- **ORCHESTRIX_JAVA_VERSION**: Default Java version (21)
- **ORCHESTRIX_JAVA_VENDOR**: Java vendor preference (openjdk)
- **ORCHESTRIX_MAVEN_VERSION**: Maven version
- **ORCHESTRIX_GRADLE_VERSION**: Gradle version
- **ORCHESTRIX_JAVA_AUTO_INSTALL**: Enable auto-installation
- **ORCHESTRIX_JAVA_INTERACTIVE**: Enable interactive prompts

### orchestrix_instruction_ai_agent.md
Instructions and guidelines for AI agents working with Orchestrix.
Contains global settings, conventions, and best practices.

## Environment Variables

These environment variables override config file settings:
- `ORCHESTRIX_HOME`: Orchestrix installation directory
- `ORCHESTRIX_JAVA_VERSION`: Override Java version
- `ORCHESTRIX_INTERACTIVE`: Override interactive mode

## Loading Order

1. Environment variables (highest priority)
2. Config files in this directory
3. Component defaults (lowest priority)

## Usage Example

```java
// JavaEnvironmentAutomation automatically loads config
JavaEnvironmentAutomation javaEnv = new JavaEnvironmentAutomation();
// Uses Java 21 from config/java.conf
```

```bash
# Build scripts automatically use config
./build.sh
# Will check for Java 21 and prompt for installation if missing
```

## Adding New Configuration

1. Create config file in this directory
2. Update `orchestrix_instruction_ai_agent.md`
3. Update components to read the config
4. Document in this README