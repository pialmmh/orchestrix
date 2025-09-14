# BTCL SBC Network Automation

This automation creates and configures LXC bridge networks for FusionPBX deployment on BTCL SBC servers.

## Features

- Creates LXC bridge network in bridge mode (no NAT)
- Perfect for VoIP applications (no NAT traversal issues)
- Configurable via YAML
- SSH-based remote execution
- Dry run mode for testing
- Automatic rollback on failure

## Configuration

Edit `src/main/resources/btcl-sbc-network.yml`:

```yaml
ssh:
  host: "192.168.1.100"  # Your server IP
  username: "root"
  keyPath: "~/.ssh/id_rsa"
  useKey: true

network:
  lxcInterfaceName: "lxcbr0"
  lxcInternalIp: "10.0.3.1"
  subnetMaskPrefixLen: 24
```

## Usage

### Build the Project

```bash
cd /home/mustafa/telcobright-projects/orchestrix/automation
mvn clean compile
```

### Run the Automation

```bash
# Using default config
mvn exec:java -Dexec.mainClass="com.orchestrix.automation.runners.btclsbc.BtclRunner"

# Using custom config file
mvn exec:java -Dexec.mainClass="com.orchestrix.automation.runners.btclsbc.BtclRunner" \
  -Dexec.args="/path/to/custom-config.yml"

# Dry run mode (no actual changes)
# Set dryRun: true in the YAML config
```

### Direct Java Execution

```bash
java -cp target/classes:target/dependency/* \
  com.orchestrix.automation.runners.btclsbc.BtclRunner \
  /path/to/config.yml
```

## Network Architecture

```
Internet
    |
Host Server
    |
lxcbr0 (10.0.3.1/24) - Bridge Mode (No NAT)
    |
    +-- FusionPBX-01 (10.0.3.100)
    +-- FusionPBX-02 (10.0.3.101)
    +-- FusionPBX-03 (10.0.3.102)
```

## What This Automation Does

1. **Deletes existing interface** with the same name (if any)
2. **Creates LXD network** in bridge mode:
   - IPv4 address: 10.0.3.1/24
   - NAT: Disabled
   - Firewall: Disabled
   - Routing: Enabled
3. **Configures IP forwarding** on the host
4. **Verifies configuration**

## Container Deployment

After network creation, deploy FusionPBX containers:

```bash
# Launch container with static IP
lxc launch fusion-pbx-v.1 fusionpbx-01
lxc config device override fusionpbx-01 eth0
lxc config device set fusionpbx-01 eth0 network=lxcbr0
lxc config device set fusionpbx-01 eth0 ipv4.address=10.0.3.100
```

## Troubleshooting

### Check Network Status

```bash
# On the server
lxc network list
lxc network show lxcbr0
ip addr show lxcbr0
```

### Test Connectivity

```bash
# From host to container
ping 10.0.3.100

# From container to host
lxc exec fusionpbx-01 -- ping 10.0.3.1
```

### Enable Internet Access (Temporary)

If containers need internet during setup:

```bash
# Add NAT rule temporarily
sudo iptables -t nat -A POSTROUTING -s 10.0.3.0/24 -o eth0 -j MASQUERADE

# Remove after setup
sudo iptables -t nat -D POSTROUTING -s 10.0.3.0/24 -o eth0 -j MASQUERADE
```

## Benefits for VoIP

- **No NAT traversal**: Direct IP routing
- **Clean SIP/RTP flow**: No packet manipulation
- **Better call quality**: No NAT-related issues
- **Simplified configuration**: No STUN/TURN needed