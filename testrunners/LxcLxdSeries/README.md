# LXC/LXD Series Automation Test

This test demonstrates the use of `SshSeriesAutomationRunner` to execute multiple SSH automation tasks sequentially using a single SSH connection for improved performance.

## Overview

The SshSeriesAutomationRunner pattern allows you to:
- Maintain a single SSH connection across multiple automation tasks
- Execute runners sequentially with configurable stop-on-failure behavior
- Pass runner-specific configurations
- Track execution results for each runner

## Architecture

### Core Components

1. **SshRunner Interface** (`com.telcobright.orchestrix.automation.SshRunner`)
   - Contract for SSH automation tasks
   - Methods: `execute()`, `verify()`, `shouldSkip()`, `getStatus()`

2. **SshSeriesAutomationRunner** (`com.telcobright.orchestrix.automation.SshSeriesAutomationRunner`)
   - Manages single SSH connection
   - Executes multiple SshRunner implementations sequentially
   - Provides execution summary

3. **Runner Implementations**
   - `LxcLxdInstallRunner`: Installs LXC/LXD on Debian systems
   - `LxdBridgeNetworkingRunner`: Configures LXD bridge networking

## Configuration

Edit `config.properties` to configure:

```properties
# SSH Connection
ssh.host=103.95.96.76
ssh.port=22
ssh.username=telcobright
ssh.password=***

# Series Settings
series.stop.on.failure=true

# Which Runners to Execute
run.install=true
run.bridge.config=true

# Installation Configuration
install.lxc=true
install.lxd=true
install.use.snap=false

# Bridge Configuration
bridge.name=lxdbr0
bridge.gateway.ip=10.0.8.1
bridge.network.cidr=10.0.8.0/24
```

## Running the Test

```bash
# Run with default config
./runTest.sh

# Run with custom config
./runTest.sh /path/to/custom-config.properties
```

## Benefits

1. **Performance**: Single SSH connection for all tasks
2. **Modularity**: Each runner is independent and reusable
3. **Flexibility**: Configure which runners to execute
4. **Error Handling**: Stop-on-failure option for early termination
5. **Detailed Reporting**: Track success/failure for each runner

## Example Output

```
=========================================
LXC/LXD Series Automation Test
=========================================
Starting series automation with 2 runners
-----------------------------------------
========================================
Runner 1/2: LxcLxdInstall
Description: Install and configure LXC/LXD on Debian 12 systems
========================================
✓ LxcLxdInstall completed successfully

========================================
Runner 2/2: LxdBridgeNetworking
Description: Configure LXD bridge networking (lxdbr0) with NAT/masquerade and DHCP
========================================
✓ LxdBridgeNetworking completed successfully

========================================
EXECUTION SUMMARY
========================================
✓ SUCCESS - LxcLxdInstall
✓ SUCCESS - LxdBridgeNetworking
----------------------------------------
Total: 2 | Success: 2 | Failed: 0
========================================
```

## Creating Custom Runners

To create a new runner:

1. Implement the `SshRunner` interface
2. Add configuration parameters to config.properties
3. Register the runner in TestLxcLxdSeriesAutomation.java

```java
public class MyCustomRunner implements SshRunner {
    @Override
    public String getName() {
        return "MyCustomTask";
    }

    @Override
    public boolean execute(SshDevice sshDevice, Map<String, String> config) {
        // Your automation logic here
        return true;
    }
}
```

## Use Cases

This pattern is ideal for:
- Multi-step server provisioning
- Complex deployment workflows
- System configuration pipelines
- Testing and validation sequences