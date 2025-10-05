# Code Generation and Terminal-Based Automation Guidelines

**Project:** Orchestrix
**Last Updated:** 2025-10-05

---

## Table of Contents

1. [SSH-Based Automation Architecture](#ssh-based-automation-architecture)
2. [PTY (Pseudo-Terminal) Requirements](#pty-pseudo-terminal-requirements)
3. [Java Code Generation Standards](#java-code-generation-standards)
4. [Container Build Automation](#container-build-automation)
5. [Error Handling and Resilience](#error-handling-and-resilience)
6. [Testing and Validation](#testing-and-validation)

---

## SSH-Based Automation Architecture

### Core Principles

**MANDATORY:** All automation MUST use SSH-based execution, even for localhost operations.

**NO ProcessBuilder/Runtime.exec()** - These tools hang on certain LXC commands and don't provide proper session management.

### Architecture Pattern

```
Runner (Entry Point)
  ↓
Creates SSH Session
  ↓
Passes to Builder (Core Logic)
  ↓
Builder uses SSH for ALL commands
  ↓
Session closes on completion
```

### Implementation Example

```java
// Runner class - establishes connection
public class MyBuildRunner {
    public static void main(String[] args) {
        LocalSshDevice device = new LocalSshDevice();

        // Connect ONCE
        boolean connected = device.connect();
        if (!connected || !device.isConnected()) {
            throw new Exception("SSH connection REQUIRED");
        }

        // Pass connected device to builder
        MyBuilder builder = new MyBuilder(device, configFile);
        builder.build();

        // Disconnect when done
        device.disconnect();
    }
}

// Builder class - uses provided connection
public class MyBuilder {
    private final LocalSshDevice device;

    public MyBuilder(LocalSshDevice device, String config) {
        if (device == null || !device.isConnected()) {
            throw new IllegalArgumentException("Connected SSH device required");
        }
        this.device = device;
    }

    public void build() throws Exception {
        // ALL commands via SSH
        device.executeCommand("sudo lxc init debian-12 my-container");
        device.executeCommand("sudo lxc start my-container");
    }
}
```

### Why SSH for Localhost?

1. **Consistent execution model** - Same code path for local and remote
2. **Proper session management** - Single persistent session vs multiple processes
3. **PTY support** - Interactive commands (lxc, apt-get) require terminal
4. **No hanging** - ProcessBuilder hangs on `lxc init` indefinitely
5. **Better debugging** - SSH logs show all executed commands

---

## PTY (Pseudo-Terminal) Requirements

### What is PTY?

PTY simulates an interactive terminal session. Commands like `lxc`, `apt-get`, `docker` expect a terminal environment for:
- Progress bars
- Color output
- Terminal control codes
- Interactive prompts

### When PTY is Required

**ALWAYS enable PTY for:**
- `lxc` commands (init, start, exec, etc.)
- `apt-get`, `apt` package operations
- `docker` commands
- `systemctl` operations
- Any command showing progress bars

**Without PTY:**
```bash
# This HANGS forever
ssh localhost "sudo lxc init debian-12 test"
```

**With PTY:**
```bash
# This WORKS
ssh -t localhost "sudo lxc init debian-12 test"
```

### Java Implementation

#### Current: PTY Always On

```java
private String executeViaSsh(String command) {
    ChannelExec channel = (ChannelExec) session.openChannel("exec");

    // Enable PTY for all commands
    channel.setPty(true);

    channel.setCommand(command);

    // Provide empty stdin (PTY expects it)
    channel.setInputStream(new ByteArrayInputStream(new byte[0]));

    channel.connect();

    // CRITICAL: Wait for exit status, NOT channel close
    // PTY keeps channels open even after command completes
    while (channel.getExitStatus() == -1 && waited < maxWait) {
        Thread.sleep(100);
        waited++;
    }

    int exitStatus = channel.getExitStatus();
    channel.disconnect();

    return outputStream.toString();
}
```

#### Recommended: Conditional PTY

```java
public String executeCommand(String command) throws Exception {
    return executeCommand(command, shouldUsePty(command));
}

public String executeCommand(String command, boolean usePty) throws Exception {
    ChannelExec channel = (ChannelExec) session.openChannel("exec");

    if (usePty) {
        channel.setPty(true);
        channel.setInputStream(new ByteArrayInputStream(new byte[0]));
    }

    channel.setCommand(command);
    channel.connect();

    // Wait for exit status availability
    while (channel.getExitStatus() == -1 && waited < maxWait) {
        Thread.sleep(100);
        waited++;
    }

    channel.disconnect();
    return outputStream.toString();
}

private boolean shouldUsePty(String command) {
    // Enable PTY for commands that need terminal
    return command.contains("lxc ") ||
           command.contains("apt-get") ||
           command.contains("apt ") ||
           command.contains("docker ") ||
           command.contains("systemctl");
}
```

### PTY Trade-offs

**Pros:**
- ✅ Commands that expect terminal work properly
- ✅ Progress bars and interactive output visible
- ✅ Consistent with manual SSH usage
- ✅ No command hanging issues

**Cons:**
- ❌ Terminal control codes in output (`\033[0m`, `\r\n`)
- ❌ Color codes may complicate parsing
- ❌ Channels stay open after command completion

**Solutions:**
1. Check `getExitStatus()` instead of `isClosed()`
2. Strip ANSI codes if needed: `output.replaceAll("\\x1B\\[[;\\d]*m", "")`
3. Use conditional PTY for commands requiring clean output

---

## Java Code Generation Standards

### JSch Library

**Version:** Use `com.github.mwiede:jsch:0.2.20` (NOT old `com.jcraft:jsch:0.1.55`)

**Why:** Modern fork supports:
- RSA SHA-256/512 signatures (OpenSSH 8.8+ requirement)
- Better algorithm negotiation
- Security updates

### Maven Dependency

```xml
<dependency>
    <groupId>com.github.mwiede</groupId>
    <artifactId>jsch</artifactId>
    <version>0.2.20</version>
</dependency>
```

### SSH Configuration

```java
public boolean connect() {
    JSch jsch = new JSch();

    // Use JSch-compatible key (not encrypted)
    String jschKey = System.getProperty("user.home") + "/.ssh/id_rsa_jsch";
    File jschKeyFile = new File(jschKey);
    if (jschKeyFile.exists()) {
        jsch.addIdentity(jschKey);
    }

    Session session = jsch.getSession(username, host, port);

    // Configure for modern OpenSSH
    Properties config = new Properties();
    config.put("StrictHostKeyChecking", "no");
    config.put("PreferredAuthentications", "publickey,password");
    config.put("CheckHostIP", "no");
    config.put("UserKnownHostsFile", "/dev/null");

    // Support modern RSA signatures
    config.put("server_host_key", "ssh-rsa,rsa-sha2-256,rsa-sha2-512");
    config.put("PubkeyAcceptedAlgorithms", "rsa-sha2-256,rsa-sha2-512,ssh-rsa");

    session.setConfig(config);
    session.setTimeout(10000);
    session.connect();

    return session.isConnected();
}
```

### SSH Key Setup

**For localhost automation:**

```bash
# Generate JSch-compatible key (no passphrase)
ssh-keygen -t rsa -b 2048 -f ~/.ssh/id_rsa_jsch -N ""

# Authorize for localhost
cat ~/.ssh/id_rsa_jsch.pub >> ~/.ssh/authorized_keys
chmod 600 ~/.ssh/authorized_keys

# Test
ssh -i ~/.ssh/id_rsa_jsch localhost whoami
```

---

## Container Build Automation

### Build Flow

```
1. Create SSH Connection (Runner)
   ↓
2. Check Prerequisites (Builder)
   ↓
3. Setup Storage (BTRFS)
   ↓
4. Create Container via SSH
   ↓
5. Start Container via SSH
   ↓
6. Configure Container (execute commands via SSH)
   ↓
7. Stop and Export Container
   ↓
8. Close SSH Connection
```

### Network Check Pattern

**DON'T** wait for ping (requires ping package):
```java
// BAD - ping not installed yet
device.executeCommand("lxc exec container -- ping -c 1 8.8.8.8");
```

**DO** check IP assignment:
```java
// GOOD - verify container has network
String status = device.executeCommand("lxc list container -c s --format csv");
if (!status.contains("RUNNING")) {
    throw new Exception("Container failed to start");
}

// Wait for network initialization
Thread.sleep(10000);
```

### Command Execution Pattern

```java
// Single persistent session throughout build
public void build() throws Exception {
    // All commands use same SSH session
    device.executeCommand("sudo lxc init debian-12 " + containerName);
    device.executeCommand("sudo lxc start " + containerName);

    // Wait for network
    Thread.sleep(10000);

    // Execute in container
    device.executeCommand("sudo lxc exec " + containerName + " -- apt-get update");
    device.executeCommand("sudo lxc exec " + containerName + " -- apt-get install -y golang-1.21");

    // Copy files into container
    device.executeCommand("echo '" + serviceCode + "' | sudo lxc exec " +
                         containerName + " -- tee /opt/app/main.go");

    // Compile inside container
    device.executeCommand("sudo lxc exec " + containerName +
                         " -- bash -c 'cd /opt/app && go build -o service main.go'");

    // Stop and export
    device.executeCommand("sudo lxc stop " + containerName);
    device.executeCommand("sudo lxc publish " + containerName + " --alias " + imageName);
}
```

---

## Error Handling and Resilience

### Timeout Configuration

```java
// Short commands (status checks): 60 seconds
int maxWait = 600; // 60 seconds

// Long commands (apt-get, compilation): 5 minutes
int maxWait = 3000; // 300 seconds

while (channel.getExitStatus() == -1 && waited < maxWait) {
    Thread.sleep(100);
    waited++;
}

if (channel.getExitStatus() == -1) {
    channel.disconnect();
    throw new Exception("Command execution timeout");
}
```

### Exit Status Checking

```java
int exitStatus = channel.getExitStatus();

if (exitStatus != 0) {
    logger.warning("Command failed with exit " + exitStatus + ": " + errorStream);
    throw new Exception("Command failed: " + command);
}
```

### Connection Validation

```java
// Verify ACTUAL SSH session exists (not ProcessBuilder fallback)
if (!connected || !device.isConnected()) {
    throw new Exception(
        "SSH connection REQUIRED. No ProcessBuilder fallback allowed.\n" +
        "Ensure:\n" +
        "1. SSH server running: sudo systemctl start ssh\n" +
        "2. SSH key exists: ls -la ~/.ssh/id_rsa_jsch\n" +
        "3. Key authorized: cat ~/.ssh/id_rsa_jsch.pub >> ~/.ssh/authorized_keys\n" +
        "4. Test manually: ssh -i ~/.ssh/id_rsa_jsch localhost whoami"
    );
}
```

---

## Testing and Validation

### Pre-Build Checks

```java
public void checkPrerequisites() throws Exception {
    // Verify SSH connection
    String whoami = device.executeCommand("whoami");
    if (whoami == null || whoami.trim().isEmpty()) {
        throw new Exception("SSH connection test failed");
    }

    // Check LXC
    String lxcVersion = device.executeCommand("lxc version");
    if (lxcVersion == null || !lxcVersion.contains("Client version")) {
        throw new Exception("LXC not available");
    }

    // Check storage
    String storageInfo = device.executeCommand("df -h /var/lib/lxd");
    // Parse and validate...
}
```

### Post-Build Validation

```java
public void validateBuild() throws Exception {
    // Verify image created
    String images = device.executeCommand("lxc image list");
    if (!images.contains(imageName)) {
        throw new Exception("Image not found: " + imageName);
    }

    // Test launch
    String testContainer = containerName + "-test";
    device.executeCommand("lxc launch " + imageName + " " + testContainer);

    Thread.sleep(10000);

    String status = device.executeCommand("lxc list " + testContainer + " -c s --format csv");
    if (!status.contains("RUNNING")) {
        throw new Exception("Test container failed to start");
    }

    // Cleanup
    device.executeCommand("lxc delete " + testContainer + " --force");
}
```

### Manual Testing Commands

```bash
# Test SSH automation
ssh -i ~/.ssh/id_rsa_jsch localhost "sudo lxc version"

# Test PTY requirement
ssh -t -i ~/.ssh/id_rsa_jsch localhost "sudo lxc init debian-12 test-pty"

# Verify no hanging
timeout 10 ssh -i ~/.ssh/id_rsa_jsch localhost "sudo lxc list"

# Cleanup
ssh -i ~/.ssh/id_rsa_jsch localhost "sudo lxc delete test-pty --force"
```

---

## Common Pitfalls and Solutions

### Pitfall 1: Using ProcessBuilder for LXC Commands

❌ **Problem:**
```java
Runtime.getRuntime().exec("lxc init debian-12 test");
// Hangs indefinitely
```

✅ **Solution:**
```java
device.executeCommand("sudo lxc init debian-12 test");
// Works via SSH with PTY
```

### Pitfall 2: Waiting for Channel Close with PTY

❌ **Problem:**
```java
while (!channel.isClosed()) {
    Thread.sleep(100);
}
// Never returns with PTY enabled
```

✅ **Solution:**
```java
while (channel.getExitStatus() == -1) {
    Thread.sleep(100);
}
// Returns immediately when command completes
```

### Pitfall 3: Old JSch Version

❌ **Problem:**
```xml
<dependency>
    <groupId>com.jcraft</groupId>
    <artifactId>jsch</artifactId>
    <version>0.1.55</version>
</dependency>
<!-- Auth fails: "signature algorithm ssh-rsa not in PubkeyAcceptedAlgorithms" -->
```

✅ **Solution:**
```xml
<dependency>
    <groupId>com.github.mwiede</groupId>
    <artifactId>jsch</artifactId>
    <version>0.2.20</version>
</dependency>
<!-- Supports modern RSA signatures -->
```

### Pitfall 4: Testing SSH with Fresh Connections

❌ **Problem:**
```bash
# Each call creates new connection - NOT how LocalSshDevice works
ssh localhost "lxc list"
ssh localhost "lxc start test"
```

✅ **Solution:**
```java
// LocalSshDevice maintains ONE session
device.connect();  // Once
device.executeCommand("lxc list");    // Channel 1 on session
device.executeCommand("lxc start test"); // Channel 2 on session
device.disconnect(); // Once
```

---

## Quick Reference

### Essential Commands

```bash
# Setup SSH for localhost automation
ssh-keygen -t rsa -b 2048 -f ~/.ssh/id_rsa_jsch -N ""
cat ~/.ssh/id_rsa_jsch.pub >> ~/.ssh/authorized_keys
chmod 600 ~/.ssh/authorized_keys

# Test SSH
ssh -i ~/.ssh/id_rsa_jsch localhost whoami

# Test PTY requirement
ssh -t -i ~/.ssh/id_rsa_jsch localhost "sudo lxc init debian-12 test"

# Cleanup test containers
sudo lxc list | grep test | awk '{print $2}' | xargs -I {} sudo lxc delete {} --force
```

### Code Template

```java
// 1. Runner establishes connection
LocalSshDevice device = new LocalSshDevice();
if (!device.connect() || !device.isConnected()) {
    throw new Exception("SSH required");
}

// 2. Builder uses connection
MyBuilder builder = new MyBuilder(device, config);
builder.build();

// 3. Close connection
device.disconnect();
```

### Build Script Template

```bash
#!/bin/bash
# Run WITHOUT sudo (uses current user's SSH key)
JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn exec:java \
    -Dexec.mainClass="com.example.MyBuildRunner" \
    -Dexec.args="$CONFIG_FILE" \
    -Dexec.classpathScope=compile
```

---

## Version History

- **2025-10-05**: Initial version
  - SSH-based automation architecture
  - PTY requirements and implementation
  - JSch 0.2.20 upgrade guidelines
  - Container build automation patterns
