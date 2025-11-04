# Kafka & Zookeeper Deployment Automation

This directory contains automated deployment tools for Kafka and Zookeeper containers via SSH.

## Overview

The deployment automation:
- Connects to remote servers via SSH
- Verifies LXC/LXD prerequisites
- Deploys Zookeeper containers
- Deploys Kafka containers configured to connect to Zookeeper
- Returns connection information for the deployed services

## Files

- **KafkaZookeeperDeployment.java** - Main deployment automation class
- **README.md** - This documentation file

## Quick Start

### Using the Shell Script (Recommended)

```bash
cd /home/mustafa/telcobright-projects/orchestrix

# Deploy to a single server
./automation/scripts/deploy-kafka-zookeeper.sh \
  123.200.0.50 tbsms 8210 "TB@l38800" true
```

### Using Java Directly

```bash
cd /home/mustafa/telcobright-projects/orchestrix

# Compile (if needed)
javac -cp ".:lib/*" \
  automation/api/infrastructure/LxcPrerequisitesManager.java \
  automation/api/deployment/KafkaZookeeperDeployment.java

# Run deployment
java -cp ".:automation/api:automation/api/deployment:automation/api/infrastructure:lib/*" \
  automation.api.deployment.KafkaZookeeperDeployment \
  123.200.0.50 tbsms 8210 "TB@l38800" true
```

### Using Environment Variables

```bash
export DEPLOY_HOST=123.200.0.50
export DEPLOY_USER=tbsms
export DEPLOY_PORT=8210
export DEPLOY_PASSWORD="TB@l38800"
export DEPLOY_USE_SUDO=true

./automation/scripts/deploy-kafka-zookeeper.sh
```

## Prerequisites

### Local Machine
- Java JDK 11 or higher
- JSch library (in lib/ directory)
- sshpass (for password-based SSH)

### Remote Server
- Ubuntu/Debian Linux
- SSH server running
- LXD/LXC installed (or will be installed automatically)
- User with sudo privileges
- Zookeeper and Kafka base images built (see images/containers/lxc/zookeeper and images/containers/lxc/kafka)

### Building Base Images

Before deploying, you must build the base images on the target server:

```bash
# On the remote server, or via SSH:
cd /home/<user>/orchestrix/images/containers/lxc/zookeeper
./startDefault.sh

cd /home/<user>/orchestrix/images/containers/lxc/kafka
./startDefault.sh
```

## Deployment Process

The automation follows these steps:

1. **SSH Connection** - Establishes SSH connection to remote server
2. **LXC Prerequisites** - Verifies and sets up LXC/LXD if needed
3. **Base Image Check** - Ensures zookeeper-base and kafka-base images exist
4. **Deploy Zookeeper** - Launches Zookeeper container and waits for it to start
5. **Verify Zookeeper** - Checks that Zookeeper service is running
6. **Configure Kafka** - Generates Kafka configuration with Zookeeper IP
7. **Deploy Kafka** - Launches Kafka broker container
8. **Verify Kafka** - Checks that Kafka service is running and connected

## Output

### Success
```
═══════════════════════════════════════════════════════
  Deployment Completed Successfully!
═══════════════════════════════════════════════════════

Zookeeper:
  IP:   10.91.217.236
  Port: 2181
  Connection: 10.91.217.236:2181

Kafka:
  IP:   10.91.217.7
  Port: 9092
  Connection: 10.91.217.7:9092
```

### Failure
The deployment will fail with detailed error messages if:
- SSH connection cannot be established
- LXC prerequisites are not met
- Base images are missing
- Container launch fails
- Services fail to start

## Known Issues and Solutions

### Issue 1: Container-to-Container Communication Blocked

**Symptom**: Kafka fails to connect to Zookeeper with timeout errors

**Cause**: iptables FORWARD chain blocks traffic between containers on lxdbr0

**Solution**: Add firewall rule to allow container-to-container communication:

```bash
# On the remote server
sudo iptables -I FORWARD -i lxdbr0 -o lxdbr0 -j ACCEPT

# Make it permanent
sudo apt-get install -y iptables-persistent
sudo iptables-save > /etc/iptables/rules.v4
```

The deployment automation does NOT automatically add this rule. You must add it manually if needed.

### Issue 2: Missing Base Images

**Symptom**: Deployment fails with "Base image not found"

**Solution**: Build the base images first:
```bash
# SSH to the target server
ssh -p 8210 user@host

# Build Zookeeper base image
cd /home/user/orchestrix/images/containers/lxc/zookeeper
./startDefault.sh

# Build Kafka base image
cd /home/user/orchestrix/images/containers/lxc/kafka
./startDefault.sh
```

### Issue 3: LXC Profile Missing Devices

**Symptom**: Container creation fails with "No root device could be found"

**Solution**: The LxcPrerequisitesManager should handle this, but if it fails manually add:
```bash
lxc profile device add default root disk path=/ pool=default
lxc profile device add default eth0 nic network=lxdbr0
```

## Multi-Server Cluster Deployment

To deploy a 3-node Kafka + Zookeeper cluster:

```bash
# Deploy to Server 1
./automation/scripts/deploy-kafka-zookeeper.sh \
  123.200.0.50 tbsms 8210 "password" true \
  > /tmp/server1-deploy.log 2>&1

# Deploy to Server 2
./automation/scripts/deploy-kafka-zookeeper.sh \
  123.200.0.117 tbsms 8210 "password" true \
  > /tmp/server2-deploy.log 2>&1

# Deploy to Server 3
./automation/scripts/deploy-kafka-zookeeper.sh \
  123.200.0.51 tbsms 8210 "password" true \
  > /tmp/server3-deploy.log 2>&1
```

After deployment, gather the IP addresses from each server and use them as:
- **Zookeeper Ensemble**: `<ip1>:2181,<ip2>:2181,<ip3>:2181`
- **Kafka Bootstrap Servers**: `<ip1>:9092,<ip2>:9092,<ip3>:9092`

## API Usage (Java)

```java
import automation.api.deployment.KafkaZookeeperDeployment;
import automation.api.deployment.KafkaZookeeperDeployment.DeploymentResult;

// Create deployment instance
KafkaZookeeperDeployment deployment = new KafkaZookeeperDeployment(
    "123.200.0.50",  // host
    "tbsms",         // user
    8210,            // port
    "password",      // password
    true             // useSudo
);

// Execute deployment
DeploymentResult result = deployment.deploy();

// Check result
if (result.isSuccess()) {
    System.out.println("Zookeeper: " + result.getZookeeperIp() + ":" + result.getZookeeperPort());
    System.out.println("Kafka: " + result.getKafkaIp() + ":" + result.getKafkaPort());
} else {
    System.err.println("Deployment failed: " + result.getError());
}
```

## Testing Deployed Services

After deployment, verify the services:

```bash
# SSH to the server
ssh -p 8210 user@host

# Check Zookeeper
lxc exec zookeeper-single -- systemctl status zookeeper
lxc exec zookeeper-single -- /opt/zookeeper/bin/zkServer.sh status

# Check Kafka
lxc exec kafka-broker-1 -- systemctl status kafka
lxc exec kafka-broker-1 -- /opt/kafka/bin/kafka-topics.sh --list --bootstrap-server localhost:9092

# Create test topic
lxc exec kafka-broker-1 -- /opt/kafka/bin/kafka-topics.sh \
  --create --topic test --partitions 1 --replication-factor 1 \
  --bootstrap-server localhost:9092

# Verify topic
lxc exec kafka-broker-1 -- /opt/kafka/bin/kafka-topics.sh \
  --describe --topic test --bootstrap-server localhost:9092
```

## Troubleshooting

### Enable Debug Output

The deployment automatically shows detailed output. For even more details, add debug logging:

```java
// In KafkaZookeeperDeployment.java, set printOutput=true in all executeCommand calls
```

### Check Logs on Remote Server

```bash
# Zookeeper logs
ssh -p 8210 user@host "lxc exec zookeeper-single -- journalctl -u zookeeper -n 100"

# Kafka logs
ssh -p 8210 user@host "lxc exec kafka-broker-1 -- journalctl -u kafka -n 100"
```

### Verify Container Network

```bash
# Check container IPs
ssh -p 8210 user@host "lxc list"

# Test connectivity between containers
ssh -p 8210 user@host "lxc exec kafka-broker-1 -- ping -c 3 <zookeeper-ip>"
```

## Related Documentation

- Container Build Scripts: `/home/mustafa/telcobright-projects/orchestrix/images/containers/lxc/`
- LXC Prerequisites: `/home/mustafa/telcobright-projects/orchestrix/automation/api/infrastructure/`
- Connection Info: `/tmp/kafka-zookeeper-connection-info.md` (generated after successful deployment)

## Support

For issues or questions:
1. Check the logs at /tmp/server*-deploy.log
2. Verify prerequisites are met on the target server
3. Ensure base images are built
4. Check firewall rules if containers cannot communicate

## Change Log

### 2025-10-29
- Initial creation
- Added support for remote deployment via SSH
- Integrated with LxcPrerequisitesManager
- Added comprehensive error handling
- Documented firewall issue and solution
- Tested with 3-node cluster deployment

## License

Internal use only - TelcoBright Projects
