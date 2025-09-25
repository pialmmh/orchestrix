# LXD Bridge Networking Automation Test

This test suite automates the configuration and testing of LXD bridge networking on remote servers using SSH.

## Overview

The `LxdBridgeNetworkingAutomation` class provides automated setup of:
- LXD network bridges with custom IP ranges
- DHCP configuration
- NAT/masquerade rules for internet access
- IP forwarding
- Persistent iptables rules

## Directory Structure

```
testrunners/LxdBridgeNetworking/
├── TestLxdBridgeNetworkingAutomation.java  # Main test class
├── config.properties                        # Default configuration (localhost)
├── config-remote.properties                # Sample remote server configuration
├── runTest.sh                              # Shell script to run tests
└── README.md                               # This file
```

## Quick Start

### 1. Configure Test Parameters

Edit `config.properties` with your target server details:

```properties
# SSH Connection Settings
ssh.host=103.95.96.76
ssh.port=22
ssh.username=telcobright
ssh.password=Takay1#$ane%%

# LXD Bridge Configuration
bridge.name=lxdbr0
bridge.gateway=10.10.199.1

# Test Scenarios to Run
test.default=false
test.custom=true
test.update=false
test.verification=true
test.getconfig=true
test.cleanup=false
```

### 2. Run Tests

```bash
# Run with default configuration
./runTest.sh

# Run with specific configuration
./runTest.sh config-remote.properties
```

## Configuration Options

### SSH Settings
- `ssh.host` - Target server hostname/IP
- `ssh.port` - SSH port (default: 22)
- `ssh.username` - SSH username
- `ssh.password` - SSH password

### Bridge Settings
- `bridge.name` - LXD bridge name (default: lxdbr0)
- `bridge.gateway` - Gateway IP for the bridge

### Test Selection
- `test.default` - Run default configuration test
- `test.custom` - Run custom configuration test
- `test.update` - Test updating existing bridge
- `test.verification` - Verify configuration
- `test.getconfig` - Get current configuration
- `test.cleanup` - Remove bridge (destructive)

## Test Scenarios

### 1. Default Configuration Test
Creates a bridge with default settings (lxdbr0, 10.10.199.1/24)

### 2. Custom Configuration Test
Creates a bridge with specified settings from config file

### 3. Update Configuration Test
Updates an existing bridge with new IP settings

### 4. Verification Test
Verifies:
- Bridge exists in LXD
- IP forwarding is enabled
- NAT/masquerade rules are configured

### 5. Get Configuration Test
Retrieves and displays current bridge configuration

### 6. Cleanup Test
Removes the bridge and associated iptables rules (use with caution)

## Manual Execution

### Compile
```bash
cd /home/mustafa/telcobright-projects/orchestrix
mvn compile
javac -cp "target/classes:lib/*" -d target/test-classes \
      testrunners/LxdBridgeNetworking/TestLxdBridgeNetworkingAutomation.java
```

### Run
```bash
java -cp "target/classes:target/test-classes:lib/*" \
     testrunners.LxdBridgeNetworking.TestLxdBridgeNetworkingAutomation \
     config.properties
```

## Requirements

- Java 21+
- Maven 3.6+
- Target server must have:
  - LXD installed
  - SSH access enabled
  - sudo/root privileges for network configuration

## Troubleshooting

### Connection Failed
- Verify SSH credentials in config file
- Check target server is reachable
- Ensure SSH service is running on target

### Bridge Creation Failed
- Verify LXD is installed: `lxd --version`
- Check user has permission to manage LXD
- Ensure bridge name doesn't conflict

### NAT/Masquerade Issues
- Verify iptables is installed
- Check IP forwarding is enabled
- Ensure user has sudo/root access

## Safety Notes

- Always test on non-production servers first
- The cleanup test is destructive - it removes bridges
- Back up existing network configuration before testing
- Verify bridge IP ranges don't conflict with existing networks

## Example Output

```
=========================================
LXD Bridge Networking Automation Test
=========================================
Target Host: 192.168.1.100:22
Username: ubuntu
Bridge Name: lxdbr0
Gateway IP: 10.10.199.1

Connecting to 192.168.1.100...
SSH Connection: Connection successful

--- Test 2: Custom Configuration ---
Bridge: lxdbr0
Gateway: 10.10.199.1
✓ Custom configuration successful
✓ Custom configuration verified

--- Test 4: Verification ---
✓ Configuration verification passed

Manual verification:
✓ Bridge lxdbr0 exists
✓ IP forwarding enabled
✓ NAT rules configured for 10.10.199.0/24

=========================================
All tests completed successfully!
=========================================
```

## Related Files

- Main automation class: `/src/main/java/com/telcobright/orchestrix/automation/LxdBridgeNetworkingAutomation.java`
- SSH device class: `/src/main/java/com/telcobright/orchestrix/device/SshDevice.java`