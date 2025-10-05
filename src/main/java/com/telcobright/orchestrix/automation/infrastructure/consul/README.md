# Consul Cluster Automation

Reusable automation for setting up minimal Consul clusters. No DNS support - IP-based service discovery only.

## Quick Start

### Command Line Setup

```bash
# Setup 3-server HA cluster
mvn exec:java \
  -Dexec.mainClass="com.telcobright.orchestrix.automation.infrastructure.consul.example.ConsulSetupRunner" \
  -Dexec.args="192.168.1.100 192.168.1.101 192.168.1.102"

# Setup 3 servers + 2 clients
mvn exec:java \
  -Dexec.mainClass="com.telcobright.orchestrix.automation.infrastructure.consul.example.ConsulSetupRunner" \
  -Dexec.args="192.168.1.100 192.168.1.101 192.168.1.102 192.168.1.103:client 192.168.1.104:client"
```

### Programmatic Usage

```java
// Create configuration
ConsulClusterConfig config = new ConsulClusterConfig();
config.setDatacenter("dc1");
config.setEnableDNS(false);  // IP-based only

// Add nodes
config.addNode("server-1", "192.168.1.100", ConsulNodeRole.SERVER);
config.addNode("server-2", "192.168.1.101", ConsulNodeRole.SERVER);
config.addNode("server-3", "192.168.1.102", ConsulNodeRole.SERVER);
config.addNode("client-1", "192.168.1.103", ConsulNodeRole.CLIENT);

// Create automation
LocalSshDevice device = new LocalSshDevice("localhost", 22, "user");
device.connect();
ConsulClusterAutomation automation = ConsulClusterAutomationFactory.create(device);

// Setup cluster
automation.setupCluster(config);

device.disconnect();
```

## Features

✅ **Minimal Configuration**: No DNS, just HTTP API
✅ **Reusable**: Works with any project
✅ **Automated**: Full cluster setup in ~5 minutes
✅ **Flexible**: 1-7 servers, unlimited clients
✅ **HA Ready**: 3+ servers for high availability
✅ **Multi-OS Support**: Auto-detects and supports both Debian/Ubuntu (systemd) and Alpine (OpenRC)

## Architecture

```
┌─────────────┐  ┌─────────────┐  ┌─────────────┐
│  Server 1   │  │  Server 2   │  │  Server 3   │
│  (Leader)   │──│  (Follower) │──│  (Follower) │
│ 192.168.1.  │  │ 192.168.1.  │  │ 192.168.1.  │
│    100      │  │    101      │  │    102      │
└──────┬──────┘  └──────┬──────┘  └──────┬──────┘
       │                │                │
       └────────────────┼────────────────┘
                        │
           ┌────────────┴────────────┐
           │                         │
    ┌──────▼──────┐          ┌──────▼──────┐
    │  Client 1   │          │  Client 2   │
    │ 192.168.1.  │          │ 192.168.1.  │
    │    103      │          │    104      │
    └─────────────┘          └─────────────┘
```

## Configuration Options

### ConsulClusterConfig

- `datacenter` - Datacenter name (default: "dc1")
- `consulVersion` - Consul version to install (default: "1.17.0")
- `enableUI` - Enable web UI (default: true)
- `enableDNS` - Enable DNS interface (default: false)
- `logLevel` - Log level: INFO, DEBUG, WARN (default: INFO)

### ConsulNode

- `name` - Node name (e.g., "server-1")
- `ipAddress` - Node IP address
- `role` - SERVER or CLIENT
- `port` - HTTP API port (default: 8500)

## Service Integration

### Configure Containers

After cluster setup, configure your containers:

```bash
# Set Consul URL (use any server IP)
lxc config set go-id-shard-0 environment.CONSUL_URL "http://192.168.1.100:8500"
lxc config set go-id-shard-0 environment.SERVICE_NAME "go-id"
lxc config set go-id-shard-0 environment.CONTAINER_IP "10.200.100.50"

# Restart to register
lxc restart go-id-shard-0
```

### Query Services

```bash
# List all services
curl http://192.168.1.100:8500/v1/catalog/services

# Get service instances
curl http://192.168.1.100:8500/v1/catalog/service/go-id

# Health check
curl http://192.168.1.100:8500/v1/health/service/go-id?passing
```

## Resource Usage

**Per Server Node:**
- RAM: ~100-150MB
- CPU: <5%
- Disk: ~50MB

**Per Client Node:**
- RAM: ~50-80MB
- CPU: <2%
- Disk: ~20MB

**3 Servers + 2 Clients Total:**
- RAM: ~460MB
- Disk: ~190MB

## Verification

```bash
# Check cluster members
ssh root@192.168.1.100 'consul members'

# Check raft peers (servers only)
ssh root@192.168.1.100 'consul operator raft list-peers'

# Access UI
firefox http://192.168.1.100:8500/ui
```

## API Reference

### ConsulClusterAutomation

```java
// Install Consul on a node
automation.installConsul(node, config);

// Configure a node
automation.configureNode(node, config);

// Start Consul service
automation.startConsul(node);

// Verify cluster health
automation.verifyCluster(config);

// Check if installed
boolean installed = automation.isConsulInstalled(node);

// Get version
String version = automation.getConsulVersion(node);
```

## Operating System Support

The automation **automatically detects** the OS on each node and uses the appropriate commands:

### Debian/Ubuntu (systemd)
- Uses `apt` for dependencies
- Creates systemd service files
- Uses `systemctl` for service management
- ~230 MB per Consul server node

### Alpine Linux (OpenRC)
- Uses `apk` for dependencies
- Creates OpenRC service files
- Uses `rc-service` and `rc-update` for service management
- ~125 MB per Consul server node (45% smaller!)

**Recommendation**: Use Alpine for infrastructure containers (Consul, CoreDNS) to save storage and memory. The automation handles all OS differences transparently.

## Best Practices

1. **Use 3 servers** for HA (survives 1 failure)
2. **Use odd numbers** of servers (1, 3, 5, 7)
3. **Co-locate with apps** to save resources
4. **Disable DNS** if using IP-based discovery
5. **Monitor via UI** at http://server:8500/ui
6. **Use Alpine** for infrastructure to save 45% storage per node

## Troubleshooting

### Cluster Not Forming

```bash
# Check logs
ssh root@192.168.1.100 'journalctl -u consul -f'

# Verify config
ssh root@192.168.1.100 'cat /etc/consul.d/consul.hcl'

# Check network
ssh root@192.168.1.100 'netstat -tuln | grep 8500'
```

### Service Not Registering

```bash
# Test from container
lxc exec <container> -- curl http://192.168.1.100:8500/v1/status/leader

# Check environment
lxc config show <container> | grep CONSUL_URL
```

## Integration Examples

### Go-ID Multi-Shard

```java
// Setup Consul first
ConsulClusterConfig config = new ConsulClusterConfig();
config.addNode("server-1", "192.168.1.100", ConsulNodeRole.SERVER);
config.addNode("server-2", "192.168.1.101", ConsulNodeRole.SERVER);
config.addNode("server-3", "192.168.1.102", ConsulNodeRole.SERVER);

automation.setupCluster(config);

// Then configure Go-ID containers
for (int i = 0; i < 3; i++) {
    device.executeCommand(
        "lxc config set go-id-shard-" + i +
        " environment.CONSUL_URL \"http://192.168.1.100:8500\""
    );
}
```

### Other Microservices

Same pattern works for any service that supports Consul registration:
- API gateways
- Database clusters
- Message queues
- Cache layers
