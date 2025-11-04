# Container Networking Guidelines for Java Automation & Deployments

**Purpose:** Comprehensive networking standards for container deployments across all client environments.
**Audience:** Java automation developers, deployment engineers, AI agents
**Last Updated:** 2025-11-04

---

## Table of Contents

1. [Core Networking Principles](#core-networking-principles)
2. [Network Architecture](#network-architecture)
3. [IP Allocation Scheme](#ip-allocation-scheme)
4. [BGP Routing Configuration](#bgp-routing-configuration)
5. [WireGuard Overlay Network](#wireguard-overlay-network)
6. [Container Bridge Networks](#container-bridge-networks)
7. [Java Automation Specifications](#java-automation-specifications)
8. [Deployment Workflow](#deployment-workflow)
9. [Configuration Templates](#configuration-templates)
10. [Troubleshooting Guide](#troubleshooting-guide)

---

## Core Networking Principles

### 1. Fixed IP Addresses
- **All containers have fixed IP addresses** across all client deployments
- IP addresses **must not change** between container restarts or redeployments
- Application configurations use direct IPs (no DNS dependencies)

### 2. No Port Forwarding
- **Never use port forwarding** from host to container
- Containers use **bridge networks** with direct IP access
- Both Docker and LXC containers get routable IPs

### 3. Immutable Application IPs
- **Do not change application IPs** (Kafka, MySQL, Java apps, etc.)
- Configuration files hardcode container IPs
- IP changes require deployment of new configuration across all services

### 4. BGP-Based Routing
- FRR (Free Range Routing) runs on all host VMs/servers
- BGP announces container subnets between hosts
- Enables cross-host container communication without NAT

### 5. Developer VPN Access
- WireGuard VPN provides secure developer access
- VPN pushes routes to container networks (10.10.0.0/16)
- **Never push default gateway** (breaks client internet)

---

## Network Architecture

### Three-Layer Design

```
┌─────────────────────────────────────────────────────────────────┐
│ Developer Access Layer (10.9.9.0/24)                            │
│ ┌──────────┐  ┌──────────┐  ┌──────────┐                       │
│ │ Dev-PC   │  │ Dev-PC   │  │ Dev-PC   │                       │
│ │ .254     │  │ .253     │  │ .252     │                       │
│ └────┬─────┘  └────┬─────┘  └────┬─────┘                       │
│      │             │             │                               │
│      └─────────────┴─────────────┘                               │
│                    │                                             │
│             WireGuard Tunnel                                     │
└────────────────────┴─────────────────────────────────────────────┘
                     │
┌────────────────────┴─────────────────────────────────────────────┐
│ Overlay Network Layer (10.9.9.0/24)                             │
│                                                                   │
│  ┌─────────────┐      ┌─────────────┐      ┌─────────────┐     │
│  │   VM1/Host1 │◄────►│   VM2/Host2 │◄────►│   VM3/Host3 │     │
│  │  10.9.9.1   │      │  10.9.9.2   │      │  10.9.9.3   │     │
│  │  (FRR+WG)   │      │  (FRR+WG)   │      │  (FRR+WG)   │     │
│  └──────┬──────┘      └──────┬──────┘      └──────┬──────┘     │
│         │                    │                    │              │
│    BGP Peering          BGP Peering          BGP Peering        │
└─────────┴────────────────────┴────────────────────┴──────────────┘
          │                    │                    │
┌─────────┴────────────────────┴────────────────────┴──────────────┐
│ Container Network Layer (10.10.0.0/16)                           │
│                                                                   │
│  ┌─────────────────┐  ┌─────────────────┐  ┌─────────────────┐ │
│  │   VM1 Subnet    │  │   VM2 Subnet    │  │   VM3 Subnet    │ │
│  │ 10.10.199.0/24  │  │ 10.10.198.0/24  │  │ 10.10.197.0/24  │ │
│  ├─────────────────┤  ├─────────────────┤  ├─────────────────┤ │
│  │ Docker: 100-199 │  │ Docker: 100-199 │  │ Docker: 100-199 │ │
│  │ LXD:    200-254 │  │ LXD:    200-254 │  │ LXD:    200-254 │ │
│  └─────────────────┘  └─────────────────┘  └─────────────────┘ │
│                                                                   │
│  Kafka, MySQL, Redis, Java Apps, etc.                           │
└───────────────────────────────────────────────────────────────────┘
```

### Traffic Flow

**Developer → Container:**
```
Developer PC (10.9.9.254)
    ↓ WireGuard Tunnel
VM3 WireGuard (10.9.9.3)
    ↓ Pushed Route: 10.10.0.0/16 via 10.9.9.3
VM3 Host Routing
    ↓ Local if same host, or BGP if different host
Container (10.10.197.100)
```

**Container → Container (Same Host):**
```
Container1 (10.10.199.100)
    ↓ Bridge network
Host1 routing (local)
    ↓ Bridge network
Container2 (10.10.199.101)
```

**Container → Container (Different Hosts):**
```
Container1@VM1 (10.10.199.100)
    ↓ Bridge network
Host1 routing
    ↓ BGP route lookup: 10.10.198.0/24 via 10.9.9.2
WireGuard overlay (encrypted)
    ↓ 10.9.9.1 → 10.9.9.2
Host2 routing
    ↓ Bridge network
Container2@VM2 (10.10.198.100)
```

---

## IP Allocation Scheme

### Subnet Allocation Pattern

**Rule:** Subnets start at `10.10.199.0/24` and **decrement** for each new host.

| Host ID | Management IP | Overlay IP | Container Subnet | Purpose |
|---------|--------------|------------|------------------|---------|
| VM1/Host1 | varies | 10.9.9.1 | 10.10.199.0/24 | First host |
| VM2/Host2 | varies | 10.9.9.2 | 10.10.198.0/24 | Second host |
| VM3/Host3 | varies | 10.9.9.3 | 10.10.197.0/24 | Third host |
| VM4/Host4 | varies | 10.9.9.4 | 10.10.196.0/24 | Fourth host |
| VM5/Host5 | varies | 10.9.9.5 | 10.10.195.0/24 | Fifth host |
| ... | ... | ... | ... | ... |

**Formula:**
```java
// Java code for calculating subnet
int hostNumber = 1; // First host
String containerSubnet = "10.10." + (200 - hostNumber) + ".0/24";
// VM1: 10.10.199.0/24
// VM2: 10.10.198.0/24
// VM3: 10.10.197.0/24
```

### IP Range Allocation Within Each Subnet

Each /24 subnet is divided as follows:

```
10.10.199.0/24 (Example for VM1)
├── 10.10.199.0         Reserved (network address)
├── 10.10.199.1         Bridge gateway (docker-bgp / lxdbr0)
├── 10.10.199.2-99      Reserved for infrastructure
├── 10.10.199.100-199   Docker containers (100 IPs)
└── 10.10.199.200-254   LXD containers (55 IPs)
```

**IP Assignment Rules:**

| Range | Use | Assignment Method |
|-------|-----|------------------|
| .0 | Network address | Auto |
| .1 | Bridge gateway | Static (bridge config) |
| .2-.99 | Reserved | Manual (infra services) |
| .100-.199 | Docker containers | Static or DHCP range |
| .200-.254 | LXD containers | Static or DHCP range |
| .255 | Broadcast | Auto |

### Developer VPN IP Allocation

**Subnet:** `10.9.9.0/24`

| Range | Use | Assignment |
|-------|-----|-----------|
| 10.9.9.0 | Network address | Auto |
| 10.9.9.1-10 | Host WireGuard servers | Static |
| 10.9.9.11-249 | Reserved | Future use |
| 10.9.9.250-254 | Developer VPN clients | Static (descending) |

**Developer IP Assignment:**
- First developer: 10.9.9.254
- Second developer: 10.9.9.253
- Third developer: 10.9.9.252
- And so on...

---

## BGP Routing Configuration

### BGP AS Number Assignment

Each host gets a **unique BGP AS number** in the private range (64512-65534).

**Pattern:** Start at 65199 and decrement.

| Host | Overlay IP | AS Number | Container Subnet Advertised |
|------|-----------|-----------|----------------------------|
| VM1 | 10.9.9.1 | 65199 | 10.10.199.0/24 |
| VM2 | 10.9.9.2 | 65198 | 10.10.198.0/24 |
| VM3 | 10.9.9.3 | 65197 | 10.10.197.0/24 |
| VM4 | 10.9.9.4 | 65196 | 10.10.196.0/24 |
| ... | ... | ... | ... |

**Formula:**
```java
int hostNumber = 1;
int asNumber = 65200 - hostNumber;
// VM1: AS 65199
// VM2: AS 65198
```

### BGP Configuration Template

**Critical Settings (MUST HAVE):**
1. `no bgp ebgp-requires-policy` - Allows route exchange without policies
2. `neighbor X.X.X.X ebgp-multihop 2` - Required for BGP over WireGuard
3. `neighbor X.X.X.X activate` - Activates neighbor in address-family
4. `no bgp network import-check` - Allows advertising non-existent routes

**Example FRR Config (VM1):**
```
frr defaults traditional
hostname vm1-bgp
log syslog informational
no ipv6 forwarding
service integrated-vtysh-config
!
router bgp 65199
 bgp router-id 10.9.9.1
 no bgp ebgp-requires-policy
 no bgp network import-check
 !
 ! Neighbor: VM2
 neighbor 10.9.9.2 remote-as 65198
 neighbor 10.9.9.2 description VM2-via-WireGuard
 neighbor 10.9.9.2 ebgp-multihop 2
 neighbor 10.9.9.2 timers 10 30
 !
 ! Neighbor: VM3
 neighbor 10.9.9.3 remote-as 65197
 neighbor 10.9.9.3 description VM3-via-WireGuard
 neighbor 10.9.9.3 ebgp-multihop 2
 neighbor 10.9.9.3 timers 10 30
 !
 address-family ipv4 unicast
  network 10.10.199.0/24
  neighbor 10.9.9.2 activate
  neighbor 10.9.9.3 activate
 exit-address-family
exit
!
line vty
!
```

### BGP Verification Commands

```bash
# Check BGP session status
sudo vtysh -c 'show ip bgp summary'

# Check learned routes
sudo vtysh -c 'show ip bgp'

# Check kernel routing table
ip route show proto bgp

# Test connectivity to remote container
ping 10.10.198.100  # Container on VM2
```

---

## WireGuard Overlay Network

### WireGuard Network Design

**Subnet:** `10.9.9.0/24`
**Purpose:** Encrypted overlay for BGP peering and developer VPN

### Host WireGuard Configuration

**Each host runs WireGuard server:**
- Listens on UDP port: 51820
- Interface name: `wg-overlay`
- Full mesh topology (all hosts peer with each other)

**VM1 WireGuard Config Example:**
```ini
[Interface]
Address = 10.9.9.1/24
ListenPort = 51820
PrivateKey = [GENERATED_PRIVATE_KEY]

# Peer: VM2
[Peer]
PublicKey = [VM2_PUBLIC_KEY]
Endpoint = [VM2_MGMT_IP]:51820
AllowedIPs = 10.9.9.2/32
PersistentKeepalive = 25

# Peer: VM3
[Peer]
PublicKey = [VM3_PUBLIC_KEY]
Endpoint = [VM3_MGMT_IP]:51820
AllowedIPs = 10.9.9.3/32
PersistentKeepalive = 25

# Developer VPN Client 1
[Peer]
PublicKey = [DEV1_PUBLIC_KEY]
AllowedIPs = 10.9.9.254/32
PersistentKeepalive = 25
```

### Developer VPN Client Configuration

**CRITICAL: Do not push default gateway**

```ini
[Interface]
Address = 10.9.9.254/32
PrivateKey = [CLIENT_PRIVATE_KEY]

[Peer]
PublicKey = [SERVER_PUBLIC_KEY]
Endpoint = [SERVER_PUBLIC_IP]:51820
AllowedIPs = 10.9.9.0/24, 10.10.0.0/16
PersistentKeepalive = 25
```

**Key Points:**
- `AllowedIPs = 10.9.9.0/24, 10.10.0.0/16` - Only route these networks
- **NOT** `0.0.0.0/0` - Would route all traffic through VPN (breaks internet)
- Client can reach all containers on 10.10.x.x
- Client can reach all host overlay IPs on 10.9.9.x

---

## Container Bridge Networks

### Docker Bridge Network

**Network Name:** `bgp-net` (custom network)
**Default Docker network:** NOT USED (uses NAT)

**Creation Command:**
```bash
docker network create \
  --driver=bridge \
  --subnet=10.10.199.0/24 \
  --gateway=10.10.199.1 \
  --ip-range=10.10.199.100/25 \
  --opt "com.docker.network.bridge.name"="docker-bgp" \
  bgp-net
```

**Configuration Details:**
- Subnet: Matches host's assigned container subnet
- Gateway: `.1` address of subnet
- IP Range: `.100-.199` for automatic assignment
- Bridge name: `docker-bgp` (visible in `ip addr`)

**Running Container with Fixed IP:**
```bash
docker run -d \
  --name kafka-broker \
  --network bgp-net \
  --ip 10.10.199.100 \
  --restart unless-stopped \
  confluentinc/cp-kafka:latest
```

**Docker Compose Example:**
```yaml
version: '3.8'

services:
  kafka:
    image: confluentinc/cp-kafka:latest
    container_name: kafka-broker
    networks:
      bgp-net:
        ipv4_address: 10.10.199.100
    restart: unless-stopped

  mysql:
    image: mysql:8.0
    container_name: mysql-db
    networks:
      bgp-net:
        ipv4_address: 10.10.199.101
    restart: unless-stopped

networks:
  bgp-net:
    external: true
```

### LXD Bridge Network

**Network Name:** `lxdbr0` or custom
**Configuration Method:** LXD network commands

**Creation:**
```bash
lxc network create lxdbr0 \
  ipv4.address=10.10.199.1/24 \
  ipv4.nat=false \
  ipv4.dhcp=true \
  ipv4.dhcp.ranges=10.10.199.200-10.10.199.254 \
  dns.domain=lxd
```

**Key Settings:**
- `ipv4.nat=false` - **CRITICAL**: Disable NAT, use routing
- `ipv4.dhcp.ranges` - Assign IPs in LXD range (200-254)

**Launching Container with Fixed IP:**
```bash
lxc launch ubuntu:22.04 app-server \
  --network lxdbr0 \
  -c ipv4.address=10.10.199.200
```

**Using Cloud-Init for Static IP:**
```yaml
#cloud-config
network:
  version: 2
  ethernets:
    eth0:
      addresses:
        - 10.10.199.200/24
      gateway4: 10.10.199.1
      nameservers:
        addresses:
          - 8.8.8.8
          - 8.8.4.4
```

---

## Java Automation Specifications

### Network Configuration Class

```java
package com.telcobright.orchestrix.automation.network;

/**
 * Network configuration calculator for container deployments
 */
public class ContainerNetworkConfig {

    // Constants
    private static final String OVERLAY_PREFIX = "10.9.9.";
    private static final String CONTAINER_PREFIX = "10.10.";
    private static final int BASE_SUBNET_THIRD_OCTET = 199;
    private static final int BASE_AS_NUMBER = 65199;

    // Host identification
    private final int hostNumber;

    public ContainerNetworkConfig(int hostNumber) {
        if (hostNumber < 1 || hostNumber > 199) {
            throw new IllegalArgumentException("Host number must be between 1 and 199");
        }
        this.hostNumber = hostNumber;
    }

    /**
     * Get overlay network IP for this host
     * @return WireGuard overlay IP (e.g., 10.9.9.1)
     */
    public String getOverlayIp() {
        return OVERLAY_PREFIX + hostNumber;
    }

    /**
     * Get container subnet for this host
     * @return Container subnet (e.g., 10.10.199.0/24)
     */
    public String getContainerSubnet() {
        int thirdOctet = BASE_SUBNET_THIRD_OCTET - (hostNumber - 1);
        return CONTAINER_PREFIX + thirdOctet + ".0/24";
    }

    /**
     * Get container subnet prefix (without /24)
     * @return Subnet prefix (e.g., 10.10.199.0)
     */
    public String getContainerSubnetPrefix() {
        return getContainerSubnet().replace("/24", "");
    }

    /**
     * Get container network base (for IP construction)
     * @return Network base (e.g., 10.10.199)
     */
    public String getContainerNetworkBase() {
        int thirdOctet = BASE_SUBNET_THIRD_OCTET - (hostNumber - 1);
        return CONTAINER_PREFIX + thirdOctet;
    }

    /**
     * Get bridge gateway IP
     * @return Gateway IP (e.g., 10.10.199.1)
     */
    public String getBridgeGatewayIp() {
        return getContainerNetworkBase() + ".1";
    }

    /**
     * Get Docker IP range start
     * @return First Docker IP (e.g., 10.10.199.100)
     */
    public String getDockerRangeStart() {
        return getContainerNetworkBase() + ".100";
    }

    /**
     * Get Docker IP range end
     * @return Last Docker IP (e.g., 10.10.199.199)
     */
    public String getDockerRangeEnd() {
        return getContainerNetworkBase() + ".199";
    }

    /**
     * Get LXD IP range start
     * @return First LXD IP (e.g., 10.10.199.200)
     */
    public String getLxdRangeStart() {
        return getContainerNetworkBase() + ".200";
    }

    /**
     * Get LXD IP range end
     * @return Last LXD IP (e.g., 10.10.199.254)
     */
    public String getLxdRangeEnd() {
        return getContainerNetworkBase() + ".254";
    }

    /**
     * Get Docker CIDR range for auto-assignment
     * @return CIDR range (e.g., 10.10.199.100/25)
     */
    public String getDockerIpRangeCidr() {
        return getDockerRangeStart() + "/25";
    }

    /**
     * Get BGP AS number for this host
     * @return AS number (e.g., 65199)
     */
    public int getBgpAsNumber() {
        return BASE_AS_NUMBER - (hostNumber - 1);
    }

    /**
     * Get BGP router ID (same as overlay IP)
     * @return Router ID (e.g., 10.9.9.1)
     */
    public String getBgpRouterId() {
        return getOverlayIp();
    }

    /**
     * Validate if IP is in Docker range
     */
    public boolean isDockerIp(String ip) {
        return ip.startsWith(getContainerNetworkBase() + ".") &&
               isInRange(ip, 100, 199);
    }

    /**
     * Validate if IP is in LXD range
     */
    public boolean isLxdIp(String ip) {
        return ip.startsWith(getContainerNetworkBase() + ".") &&
               isInRange(ip, 200, 254);
    }

    private boolean isInRange(String ip, int min, int max) {
        String[] parts = ip.split("\\.");
        if (parts.length != 4) return false;
        int lastOctet = Integer.parseInt(parts[3]);
        return lastOctet >= min && lastOctet <= max;
    }

    @Override
    public String toString() {
        return String.format(
            "Host %d: Overlay=%s, Subnet=%s, AS=%d",
            hostNumber, getOverlayIp(), getContainerSubnet(), getBgpAsNumber()
        );
    }
}
```

### Usage Example

```java
// Create config for VM1
ContainerNetworkConfig vm1 = new ContainerNetworkConfig(1);

System.out.println("Overlay IP: " + vm1.getOverlayIp());           // 10.9.9.1
System.out.println("Container Subnet: " + vm1.getContainerSubnet()); // 10.10.199.0/24
System.out.println("Docker Range: " + vm1.getDockerRangeStart() + "-" + vm1.getDockerRangeEnd()); // 10.10.199.100-199
System.out.println("LXD Range: " + vm1.getLxdRangeStart() + "-" + vm1.getLxdRangeEnd()); // 10.10.199.200-254
System.out.println("BGP AS: " + vm1.getBgpAsNumber());              // 65199

// Use in deployment automation
String dockerCreateCommand = String.format(
    "docker network create --driver=bridge --subnet=%s --gateway=%s --ip-range=%s --opt \"com.docker.network.bridge.name\"=\"docker-bgp\" bgp-net",
    vm1.getContainerSubnet(),
    vm1.getBridgeGatewayIp(),
    vm1.getDockerIpRangeCidr()
);
```

### Deployment Configuration Builder

```java
package com.telcobright.orchestrix.automation.deployment;

public class ContainerDeploymentConfig {

    private String containerName;
    private String image;
    private String ipAddress;
    private int hostNumber;
    private Map<String, String> environment = new HashMap<>();
    private List<String> volumes = new ArrayList<>();

    public ContainerDeploymentConfig(String containerName, int hostNumber) {
        this.containerName = containerName;
        this.hostNumber = hostNumber;
    }

    public ContainerDeploymentConfig withImage(String image) {
        this.image = image;
        return this;
    }

    public ContainerDeploymentConfig withIpAddress(String ipAddress) {
        ContainerNetworkConfig networkConfig = new ContainerNetworkConfig(hostNumber);

        // Validate IP is in correct range
        if (!networkConfig.isDockerIp(ipAddress) && !networkConfig.isLxdIp(ipAddress)) {
            throw new IllegalArgumentException(
                String.format("IP %s is not in valid range for host %d", ipAddress, hostNumber)
            );
        }

        this.ipAddress = ipAddress;
        return this;
    }

    public ContainerDeploymentConfig withEnvironment(String key, String value) {
        this.environment.put(key, value);
        return this;
    }

    public ContainerDeploymentConfig withVolume(String hostPath, String containerPath) {
        this.volumes.add(hostPath + ":" + containerPath);
        return this;
    }

    /**
     * Generate Docker run command
     */
    public String generateDockerRunCommand() {
        StringBuilder cmd = new StringBuilder("docker run -d");
        cmd.append(" --name ").append(containerName);
        cmd.append(" --network bgp-net");
        cmd.append(" --ip ").append(ipAddress);
        cmd.append(" --restart unless-stopped");

        // Environment variables
        for (Map.Entry<String, String> env : environment.entrySet()) {
            cmd.append(" -e ").append(env.getKey()).append("=").append(env.getValue());
        }

        // Volumes
        for (String volume : volumes) {
            cmd.append(" -v ").append(volume);
        }

        cmd.append(" ").append(image);

        return cmd.toString();
    }

    /**
     * Generate docker-compose.yml entry
     */
    public String generateDockerComposeEntry() {
        StringBuilder yaml = new StringBuilder();
        yaml.append("  ").append(containerName).append(":\n");
        yaml.append("    image: ").append(image).append("\n");
        yaml.append("    container_name: ").append(containerName).append("\n");
        yaml.append("    networks:\n");
        yaml.append("      bgp-net:\n");
        yaml.append("        ipv4_address: ").append(ipAddress).append("\n");
        yaml.append("    restart: unless-stopped\n");

        if (!environment.isEmpty()) {
            yaml.append("    environment:\n");
            for (Map.Entry<String, String> env : environment.entrySet()) {
                yaml.append("      ").append(env.getKey()).append(": ").append(env.getValue()).append("\n");
            }
        }

        if (!volumes.isEmpty()) {
            yaml.append("    volumes:\n");
            for (String volume : volumes) {
                yaml.append("      - ").append(volume).append("\n");
            }
        }

        return yaml.toString();
    }
}
```

### Usage in Deployment Automation

```java
// Example: Deploy Kafka cluster across 3 hosts
public class KafkaClusterDeployment {

    public void deployKafkaCluster() {
        // Host 1: ZooKeeper
        ContainerDeploymentConfig zk1 = new ContainerDeploymentConfig("zookeeper-1", 1)
            .withImage("confluentinc/cp-zookeeper:latest")
            .withIpAddress("10.10.199.100")
            .withEnvironment("ZOOKEEPER_CLIENT_PORT", "2181")
            .withEnvironment("ZOOKEEPER_TICK_TIME", "2000");

        // Host 1: Kafka Broker 1
        ContainerDeploymentConfig kafka1 = new ContainerDeploymentConfig("kafka-broker-1", 1)
            .withImage("confluentinc/cp-kafka:latest")
            .withIpAddress("10.10.199.101")
            .withEnvironment("KAFKA_BROKER_ID", "1")
            .withEnvironment("KAFKA_ZOOKEEPER_CONNECT", "10.10.199.100:2181")
            .withEnvironment("KAFKA_ADVERTISED_LISTENERS", "PLAINTEXT://10.10.199.101:9092");

        // Host 2: Kafka Broker 2
        ContainerDeploymentConfig kafka2 = new ContainerDeploymentConfig("kafka-broker-2", 2)
            .withImage("confluentinc/cp-kafka:latest")
            .withIpAddress("10.10.198.101")
            .withEnvironment("KAFKA_BROKER_ID", "2")
            .withEnvironment("KAFKA_ZOOKEEPER_CONNECT", "10.10.199.100:2181")
            .withEnvironment("KAFKA_ADVERTISED_LISTENERS", "PLAINTEXT://10.10.198.101:9092");

        // Host 3: Kafka Broker 3
        ContainerDeploymentConfig kafka3 = new ContainerDeploymentConfig("kafka-broker-3", 3)
            .withImage("confluentinc/cp-kafka:latest")
            .withIpAddress("10.10.197.101")
            .withEnvironment("KAFKA_BROKER_ID", "3")
            .withEnvironment("KAFKA_ZOOKEEPER_CONNECT", "10.10.199.100:2181")
            .withEnvironment("KAFKA_ADVERTISED_LISTENERS", "PLAINTEXT://10.10.197.101:9092");

        // Execute deployments
        executeOnHost(1, zk1.generateDockerRunCommand());
        executeOnHost(1, kafka1.generateDockerRunCommand());
        executeOnHost(2, kafka2.generateDockerRunCommand());
        executeOnHost(3, kafka3.generateDockerRunCommand());
    }

    private void executeOnHost(int hostNumber, String command) {
        // SSH execution logic
        // Use ContainerNetworkConfig to get host overlay IP if needed
    }
}
```

---

## Deployment Workflow

### Pre-Deployment Checklist

1. **Host Infrastructure Ready**
   - [ ] FRR installed and BGP daemon enabled
   - [ ] WireGuard installed and overlay network configured
   - [ ] BGP peering established between all hosts
   - [ ] Routes being exchanged (verify with `show ip bgp summary`)

2. **Container Network Setup**
   - [ ] Docker bridge network created (`bgp-net`)
   - [ ] LXD bridge network created (`lxdbr0`)
   - [ ] Bridge NAT disabled
   - [ ] IP ranges configured correctly

3. **Network Validation**
   - [ ] Ping between host overlay IPs works
   - [ ] BGP routes in kernel routing table
   - [ ] Test connectivity between containers on different hosts

### Container Deployment Steps

**Step 1: Calculate Network Configuration**
```java
ContainerNetworkConfig networkConfig = new ContainerNetworkConfig(hostNumber);
String containerIp = networkConfig.getDockerRangeStart(); // or specific IP
```

**Step 2: Create Container Deployment Config**
```java
ContainerDeploymentConfig deployment = new ContainerDeploymentConfig("my-app", hostNumber)
    .withImage("my-app:latest")
    .withIpAddress(containerIp)
    .withEnvironment("DB_HOST", "10.10.199.101") // MySQL on VM1
    .withEnvironment("KAFKA_BOOTSTRAP", "10.10.199.101:9092");
```

**Step 3: Deploy Container**
```java
String deployCommand = deployment.generateDockerRunCommand();
// Execute via SSH or local shell
```

**Step 4: Verify Deployment**
```bash
# Check container is running
docker ps | grep my-app

# Check container IP
docker inspect my-app | grep IPAddress

# Test connectivity from another host
ping 10.10.199.100

# Test application access
curl http://10.10.199.100:8080/health
```

### Multi-Host Deployment Pattern

```java
public class MultiHostDeployment {

    private List<HostConfig> hosts;

    public void deployDistributedApplication() {
        // Phase 1: Deploy data layer (databases, caches)
        deployDataLayer();

        // Wait for data layer to be healthy
        waitForHealthy(getDataLayerContainers());

        // Phase 2: Deploy message layer (Kafka, RabbitMQ)
        deployMessageLayer();

        // Wait for message layer
        waitForHealthy(getMessageLayerContainers());

        // Phase 3: Deploy application layer
        deployApplicationLayer();

        // Phase 4: Deploy API gateway
        deployApiGateway();

        // Phase 5: Verify end-to-end
        verifyEndToEnd();
    }

    private void deployDataLayer() {
        for (HostConfig host : hosts) {
            if (host.shouldHostDatabase()) {
                ContainerDeploymentConfig db = new ContainerDeploymentConfig("mysql", host.getNumber())
                    .withImage("mysql:8.0")
                    .withIpAddress(allocateIp(host, "mysql"))
                    .withEnvironment("MYSQL_ROOT_PASSWORD", "secret");

                executeDeployment(host, db);
            }
        }
    }

    private String allocateIp(HostConfig host, String service) {
        // Use IP allocation strategy
        ContainerNetworkConfig network = new ContainerNetworkConfig(host.getNumber());
        // Allocate from appropriate range based on service type
        return network.getDockerRangeStart(); // Or use IP allocation manager
    }
}
```

---

## Configuration Templates

### Complete Host Setup Script

```bash
#!/bin/bash
# setup-container-host.sh
# Sets up a host for container deployments with BGP routing

set -e

# Configuration
HOST_NUMBER=1
MGMT_IP="192.168.1.10"
OVERLAY_IP="10.9.9.$HOST_NUMBER"
CONTAINER_SUBNET="10.10.$((199 - HOST_NUMBER + 1)).0/24"
CONTAINER_GATEWAY="10.10.$((199 - HOST_NUMBER + 1)).1"
DOCKER_IP_RANGE="10.10.$((199 - HOST_NUMBER + 1)).100/25"
BGP_AS_NUMBER=$((65199 - HOST_NUMBER + 1))

echo "=== Container Host Setup ==="
echo "Host Number: $HOST_NUMBER"
echo "Overlay IP: $OVERLAY_IP"
echo "Container Subnet: $CONTAINER_SUBNET"
echo "BGP AS: $BGP_AS_NUMBER"

# Install FRR
echo "Installing FRR..."
curl -s https://deb.frrouting.org/frr/keys.asc | sudo apt-key add -
echo "deb https://deb.frrouting.org/frr $(lsb_release -s -c) frr-stable" | sudo tee /etc/apt/sources.list.d/frr.list
sudo apt-get update
sudo apt-get install -y frr frr-pythontools

# Enable BGP daemon
echo "Enabling BGP daemon..."
sudo sed -i 's/bgpd=no/bgpd=yes/' /etc/frr/daemons

# Install WireGuard
echo "Installing WireGuard..."
sudo apt-get install -y wireguard wireguard-tools

# Generate WireGuard keys
echo "Generating WireGuard keys..."
wg genkey | sudo tee /etc/wireguard/private.key | wg pubkey | sudo tee /etc/wireguard/public.key

echo "Private key: $(sudo cat /etc/wireguard/private.key)"
echo "Public key: $(sudo cat /etc/wireguard/public.key)"

# Install Docker
echo "Installing Docker..."
curl -fsSL https://get.docker.com | sh
sudo usermod -aG docker $USER

# Create Docker BGP network
echo "Creating Docker BGP network..."
docker network create \
  --driver=bridge \
  --subnet=$CONTAINER_SUBNET \
  --gateway=$CONTAINER_GATEWAY \
  --ip-range=$DOCKER_IP_RANGE \
  --opt "com.docker.network.bridge.name"="docker-bgp" \
  bgp-net

echo "=== Setup Complete ==="
echo ""
echo "Next steps:"
echo "1. Configure WireGuard with peers"
echo "2. Configure BGP with neighbors"
echo "3. Start WireGuard: sudo wg-quick up wg-overlay"
echo "4. Start FRR: sudo systemctl restart frr"
echo "5. Verify BGP: sudo vtysh -c 'show ip bgp summary'"
```

### Application Configuration Template

```yaml
# application.yml - Microservice configuration
# Use direct container IPs for all service dependencies

spring:
  datasource:
    # MySQL on VM1
    url: jdbc:mysql://10.10.199.101:3306/mydb
    username: app_user
    password: ${DB_PASSWORD}

  redis:
    # Redis on VM2
    host: 10.10.198.100
    port: 6379

kafka:
  bootstrap-servers:
    # Kafka cluster across 3 hosts
    - 10.10.199.101:9092  # VM1
    - 10.10.198.101:9092  # VM2
    - 10.10.197.101:9092  # VM3

external-services:
  # Another microservice on VM3
  user-service: http://10.10.197.100:8080

  # Payment service on VM2
  payment-service: http://10.10.198.102:8080
```

---

## Troubleshooting Guide

### Issue: Container cannot reach container on different host

**Symptoms:**
```bash
# From container on VM1
curl http://10.10.198.100:8080
# Timeout or connection refused
```

**Diagnosis Steps:**

1. **Check BGP is advertising route**
   ```bash
   sudo vtysh -c 'show ip bgp'
   # Should show 10.10.198.0/24 route
   ```

2. **Check route in kernel**
   ```bash
   ip route get 10.10.198.100
   # Should show: 10.10.198.100 via 10.9.9.2 dev wg-overlay
   ```

3. **Check WireGuard tunnel**
   ```bash
   sudo wg show
   # Should show handshake with peer
   ping 10.9.9.2
   # Should work
   ```

4. **Check bridge forwarding**
   ```bash
   # On destination host
   sudo iptables -L -n -v | grep docker-bgp
   # Should allow forwarding
   ```

**Solutions:**

- **BGP not advertising:** Add `no bgp ebgp-requires-policy` to FRR config
- **No kernel route:** Check `no bgp network import-check` in FRR
- **WireGuard down:** Restart with `sudo wg-quick up wg-overlay`
- **Bridge firewall:** `sudo iptables -I FORWARD -i docker-bgp -j ACCEPT`

### Issue: Developer VPN cannot reach containers

**Symptoms:**
```bash
# From developer PC
ping 10.10.199.100
# No response
```

**Diagnosis:**

1. **Check VPN connection**
   ```bash
   sudo wg show
   # Should show handshake
   ```

2. **Check allowed IPs**
   ```bash
   # In client config, should have:
   AllowedIPs = 10.9.9.0/24, 10.10.0.0/16
   ```

3. **Check routing on client**
   ```bash
   ip route | grep 10.10
   # Should show route via wg interface
   ```

4. **Check server allows client IP**
   ```bash
   # On VPN server
   sudo wg show
   # Should show peer with AllowedIPs = 10.9.9.254/32
   ```

**Solutions:**

- **Wrong AllowedIPs:** Update client config
- **Server not accepting:** Add peer to server config
- **Firewall blocking:** Check UFW/iptables on server

### Issue: Container has wrong IP after restart

**Symptoms:**
```bash
docker inspect myapp | grep IPAddress
# Shows 10.10.199.150 instead of expected 10.10.199.100
```

**Cause:** Container started without `--ip` flag

**Solution:**

Always use explicit IP assignment:
```bash
docker run -d \
  --name myapp \
  --network bgp-net \
  --ip 10.10.199.100 \  # REQUIRED
  myapp:latest
```

Or use docker-compose with static IP:
```yaml
services:
  myapp:
    networks:
      bgp-net:
        ipv4_address: 10.10.199.100  # REQUIRED
```

### Issue: BGP session not establishing

**Symptoms:**
```bash
sudo vtysh -c 'show ip bgp summary'
# Shows "Active" or "Connect" instead of established
```

**Diagnosis:**

1. **Check overlay connectivity**
   ```bash
   ping 10.9.9.2  # Peer overlay IP
   # Should work
   ```

2. **Check BGP port open**
   ```bash
   telnet 10.9.9.2 179
   # Should connect
   ```

3. **Check for ebgp-multihop**
   ```bash
   sudo vtysh -c 'show running-config'
   # Should have: neighbor 10.9.9.2 ebgp-multihop 2
   ```

**Solution:**

Add ebgp-multihop to config:
```bash
sudo vtysh
configure terminal
router bgp 65199
  neighbor 10.9.9.2 ebgp-multihop 2
  exit
write memory
```

---

## Quick Reference

### Network Allocation Formulas

```
Overlay IP (Host N):      10.9.9.N
Container Subnet (Host N): 10.10.(200-N).0/24
Bridge Gateway (Host N):   10.10.(200-N).1
Docker Range (Host N):     10.10.(200-N).100-199
LXD Range (Host N):        10.10.(200-N).200-254
BGP AS (Host N):           65200-N
```

### Common Commands

```bash
# Network verification
ping 10.9.9.2                                    # Test overlay
sudo vtysh -c 'show ip bgp summary'             # Check BGP
ip route show proto bgp                          # Check routes
docker network inspect bgp-net                   # Check Docker network

# Container operations
docker run --network bgp-net --ip X.X.X.X ...   # Run with IP
docker network ls                                # List networks
docker inspect <container> | grep IPAddress      # Check container IP

# BGP operations
sudo vtysh -c 'show ip bgp'                     # Show BGP table
sudo vtysh -c 'show ip bgp neighbors'           # Show neighbors
sudo systemctl restart frr                       # Restart FRR

# WireGuard operations
sudo wg show                                     # Show tunnels
sudo wg-quick up wg-overlay                     # Start tunnel
sudo wg-quick down wg-overlay                   # Stop tunnel
```

### IP Allocation Quick Table

| Host | Overlay | Subnet | Docker IPs | LXD IPs | BGP AS |
|------|---------|--------|------------|---------|---------|
| VM1 | 10.9.9.1 | 10.10.199.0/24 | .100-.199 | .200-.254 | 65199 |
| VM2 | 10.9.9.2 | 10.10.198.0/24 | .100-.199 | .200-.254 | 65198 |
| VM3 | 10.9.9.3 | 10.10.197.0/24 | .100-.199 | .200-.254 | 65197 |
| VM4 | 10.9.9.4 | 10.10.196.0/24 | .100-.199 | .200-.254 | 65196 |
| VM5 | 10.9.9.5 | 10.10.195.0/24 | .100-.199 | .200-.254 | 65195 |

---

## References

- [FRR Documentation](https://docs.frrouting.org/)
- [WireGuard Documentation](https://www.wireguard.com/)
- [Docker Networking](https://docs.docker.com/network/)
- [LXD Networking](https://linuxcontainers.org/lxd/docs/latest/networks/)
- [BGP Hop Count Fix](/home/mustafa/telcobright-projects/orchestrix-frr-router/BGP_HOP_COUNT_FIX.md)
- [FRR Deployment Guide](/home/mustafa/telcobright-projects/orchestrix-frr-router/FRR_DEPLOYMENT_GUIDE.md)
- [Docker Bridge Setup](/home/mustafa/telcobright-projects/orchestrix-frr-router/DOCKER_BRIDGE_NETWORK_SETUP.md)

---

**Document Version:** 1.0
**Last Updated:** 2025-11-04
**Maintained By:** Orchestrix Team
**Status:** Active - Reference for all deployments
