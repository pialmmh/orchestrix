# Orchestrix Automation Framework

Automated deployment and management tools for LXC containers via SSH.

## Overview

This automation framework provides:
- **Remote deployment** of Kafka, Zookeeper, and other services
- **LXC/LXD management** via SSH
- **Prerequisites checking** and automated setup
- **Java API** for programmatic deployment
- **Shell scripts** for command-line usage

## Quick Links

### Documentation
- **[Kafka & Zookeeper Deployment Guide](KAFKA_ZOOKEEPER_DEPLOYMENT.md)** - Main deployment guide
- **[Usage Examples](USAGE_EXAMPLES.md)** - Code examples and common scenarios
- **[API Documentation](api/deployment/README.md)** - Java API reference

### Scripts
- **[deploy-kafka-zookeeper.sh](scripts/deploy-kafka-zookeeper.sh)** - Main deployment script

### Source Code
- **[KafkaZookeeperDeployment.java](api/deployment/KafkaZookeeperDeployment.java)** - Deployment automation
- **[LxcPrerequisitesManager.java](api/infrastructure/LxcPrerequisitesManager.java)** - LXC setup

## Directory Structure

```
automation/
├── README.md                              # This file
├── KAFKA_ZOOKEEPER_DEPLOYMENT.md          # Deployment guide
├── USAGE_EXAMPLES.md                      # Usage examples
│
├── api/
│   ├── deployment/
│   │   ├── KafkaZookeeperDeployment.java  # Main automation
│   │   └── README.md                      # API documentation
│   └── infrastructure/
│       └── LxcPrerequisitesManager.java   # LXC setup automation
│
└── scripts/
    └── deploy-kafka-zookeeper.sh          # Deployment wrapper script
```

## Quick Start

### 1. Single Server Deployment
```bash
cd /home/mustafa/telcobright-projects/orchestrix

./automation/scripts/deploy-kafka-zookeeper.sh \
  192.168.1.100 ubuntu 22 "password" true
```

### 2. Multi-Server Cluster
```bash
# Deploy to 3 servers
./automation/scripts/deploy-kafka-zookeeper.sh 123.200.0.50 tbsms 8210 "password" true
./automation/scripts/deploy-kafka-zookeeper.sh 123.200.0.117 tbsms 8210 "password" true
./automation/scripts/deploy-kafka-zookeeper.sh 123.200.0.51 tbsms 8210 "password" true
```

### 3. Using Java API
```java
import automation.api.deployment.KafkaZookeeperDeployment;

KafkaZookeeperDeployment deployment = new KafkaZookeeperDeployment(
    "123.200.0.50", "tbsms", 8210, "password", true
);

var result = deployment.deploy();
System.out.println("Kafka: " + result.getKafkaIp() + ":" + result.getKafkaPort());
```

## Features

### Current Features
- ✅ **SSH-based deployment** - Deploy to remote servers via SSH
- ✅ **LXC prerequisites** - Automatic verification and setup
- ✅ **Base image checking** - Validates required images exist
- ✅ **Auto-configuration** - Generates optimal configurations
- ✅ **Health verification** - Validates services are running
- ✅ **Error handling** - Comprehensive error reporting
- ✅ **Multi-server support** - Deploy to multiple servers
- ✅ **Detailed logging** - Full deployment logs

### Supported Services
- Kafka (version 3.9.0)
- Zookeeper (version 3.9.4)
- More services coming soon...

## Prerequisites

### Local Machine
- Java JDK 11+
- JSch library (SSH)
- sshpass (optional, for password auth)

### Remote Server
- Ubuntu/Debian Linux
- SSH access
- LXD/LXC (or will be installed)
- Sudo privileges
- Base images built (see [image building guide](../images/containers/lxc/))

## Common Tasks

### Deploy Kafka Cluster
See: [KAFKA_ZOOKEEPER_DEPLOYMENT.md](KAFKA_ZOOKEEPER_DEPLOYMENT.md)

### Build Base Images
```bash
# On remote server
cd /home/user/orchestrix/images/containers/lxc/zookeeper
./startDefault.sh

cd /home/user/orchestrix/images/containers/lxc/kafka
./startDefault.sh
```

### Fix Container Networking
```bash
# If containers can't communicate
sudo iptables -I FORWARD -i lxdbr0 -o lxdbr0 -j ACCEPT
sudo apt-get install -y iptables-persistent
sudo iptables-save > /etc/iptables/rules.v4
```

### Verify Deployment
```bash
ssh user@host "lxc list"
ssh user@host "lxc exec kafka-broker-1 -- systemctl status kafka"
ssh user@host "lxc exec zookeeper-single -- systemctl status zookeeper"
```

## Troubleshooting

### Issue: Base image not found
**Solution**: Build base images on target server first

### Issue: Containers can't communicate
**Solution**: Add firewall rule (see Common Tasks above)

### Issue: Service fails to start
**Solution**: Check logs:
```bash
ssh user@host "lxc exec <container> -- journalctl -u <service> -n 100"
```

### Issue: LXC profile missing devices
**Solution**: LxcPrerequisitesManager handles this automatically

## Examples

### Example 1: Dev Environment
```bash
./automation/scripts/deploy-kafka-zookeeper.sh \
  dev-server.local admin 22 "devpass" true
```

### Example 2: Production Cluster
```bash
for server in prod-kafka-{1,2,3}.example.com; do
  ./automation/scripts/deploy-kafka-zookeeper.sh \
    $server deploy 22 "$PROD_PASSWORD" true &
done
wait
```

### Example 3: With Environment Variables
```bash
export DEPLOY_HOST=staging.example.com
export DEPLOY_USER=deploy
export DEPLOY_PORT=22
export DEPLOY_PASSWORD="secure-password"

./automation/scripts/deploy-kafka-zookeeper.sh
```

### Example 4: Java Integration
```java
List<String> servers = Arrays.asList(
    "server1.example.com",
    "server2.example.com",
    "server3.example.com"
);

List<DeploymentResult> results = servers.parallelStream()
    .map(host -> {
        var deployment = new KafkaZookeeperDeployment(
            host, "deploy", 22, password, true
        );
        return deployment.deploy();
    })
    .collect(Collectors.toList());

// Collect connection strings
String zkEnsemble = results.stream()
    .map(r -> r.getZookeeperIp() + ":2181")
    .collect(Collectors.joining(","));

String kafkaBootstrap = results.stream()
    .map(r -> r.getKafkaIp() + ":9092")
    .collect(Collectors.joining(","));
```

## Architecture

```
┌─────────────────┐
│  Local Machine  │
│  Java/Shell     │
└────────┬────────┘
         │ SSH
         ▼
┌─────────────────┐
│  Remote Server  │
│  LXC/LXD        │
│                 │
│  ┌──────────┐  │
│  │ Zookeeper│  │
│  └────┬─────┘  │
│       │         │
│  ┌────▼─────┐  │
│  │  Kafka   │  │
│  └──────────┘  │
└─────────────────┘
```

## Performance & Limitations

### Performance
- Deployment time: ~30-60 seconds per server
- Parallel deployment: Yes, multiple servers simultaneously
- Network overhead: Minimal (SSH only)

### Limitations
- Requires sudo access on target
- Debian/Ubuntu only
- Single broker per server
- Development/testing focus (not production hardened)

## Future Enhancements

- [ ] Support for SSH key authentication
- [ ] Automatic firewall configuration
- [ ] Clustered Kafka deployment
- [ ] SSL/TLS configuration
- [ ] SASL authentication
- [ ] Monitoring integration
- [ ] Docker support
- [ ] Kubernetes deployment

## Support & Contributing

### Getting Help
1. Check the documentation links above
2. Review [USAGE_EXAMPLES.md](USAGE_EXAMPLES.md)
3. Check deployment logs in /tmp/
4. Review error messages and stack traces

### Contributing
To add new automation:
1. Create new class in `api/deployment/`
2. Follow KafkaZookeeperDeployment pattern
3. Add wrapper script in `scripts/`
4. Document in README and usage examples

## Version History

### 2025-10-29 - v1.0
- Initial release
- Kafka & Zookeeper automation
- LXC prerequisites management
- SSH-based deployment
- Comprehensive documentation
- Tested with 3-node cluster

## License

Internal use only - TelcoBright Projects

---

**Last Updated**: 2025-10-29
**Maintainer**: Orchestrix Team
**Status**: Production Ready (Dev/Test environments)
