# FRR Deployment Automation Guide

## Overview
This document contains all the steps and configurations needed to deploy FRR with BGP over WireGuard overlay networks. This guide is based on the working configuration from our deployment.

## Table of Contents
1. [Prerequisites](#prerequisites)
2. [FRR Installation Steps](#frr-installation-steps)
3. [BGP Configuration](#bgp-configuration)
4. [Configuration Templates](#configuration-templates)
5. [Deployment Process](#deployment-process)
6. [Verification Commands](#verification-commands)
7. [Java Automation Structure](#java-automation-structure)

---

## Prerequisites

### System Requirements
- Ubuntu/Debian-based Linux servers
- SSH access with sudo privileges
- WireGuard overlay network configured
- Python 3 or Java runtime for automation

### Required Packages
- `frr` - FRRouting package
- `frr-pythontools` - Python tools for FRR
- `sshpass` - For automated SSH (automation only)

### Network Information Needed
- Node IP addresses (management/public IPs)
- Overlay IP addresses (WireGuard IPs)
- AS numbers for each node
- LXD/Container network CIDRs to advertise

---

## FRR Installation Steps

### Step 1: Add FRR Repository
```bash
curl -s https://deb.frrouting.org/frr/keys.asc | sudo apt-key add -
echo "deb https://deb.frrouting.org/frr $(lsb_release -s -c) frr-stable" | sudo tee /etc/apt/sources.list.d/frr.list
```

### Step 2: Update Package List
```bash
sudo apt-get update
```

### Step 3: Install FRR
```bash
DEBIAN_FRONTEND=noninteractive sudo apt-get install -y frr frr-pythontools
```

### Step 4: Enable BGP Daemon
Edit `/etc/frr/daemons`:
```
bgpd=yes
ospfd=no
ospf6d=no
ripd=no
ripngd=no
isisd=no
pimd=no
ldpd=no
nhrpd=no
eigrpd=no
babeld=no
sharpd=no
pbrd=no
bfdd=no
fabricd=no
vrrpd=no
```

Then set permissions:
```bash
sudo chmod 640 /etc/frr/daemons
```

---

## BGP Configuration

### Key BGP Settings We Use

1. **`no bgp ebgp-requires-policy`**
   - CRITICAL: Allows BGP route exchange without explicit route-maps
   - Without this, BGP neighbors will not exchange routes

2. **`no bgp network import-check`**
   - Allows advertising networks that may not exist in routing table yet
   - Useful when advertising container networks

3. **`neighbor X.X.X.X ebgp-multihop 2`**
   - CRITICAL for BGP over WireGuard/overlay networks
   - BGP peers are not directly connected, they go through WireGuard tunnel
   - Default EBGP TTL is 1, which fails for non-direct connections
   - Setting to 2 allows BGP packets to traverse the overlay network
   - Alternative: `neighbor X.X.X.X ttl-security hops 2`

4. **`neighbor X.X.X.X activate`** (in address-family)
   - REQUIRED: Activates the neighbor for the address family
   - Must be present in `address-family ipv4 unicast` section
   - Without this, routes will not be exchanged even if session is established

5. **Timers**: `timers 10 30`
   - Keepalive: 10 seconds
   - Holdtime: 30 seconds
   - Faster convergence for our use case

### BGP Configuration Template

For Node 1 (AS 65196, Overlay IP 10.9.9.1, LXD Network 10.10.199.0/24):

```
frr defaults traditional
hostname node1-bgp
log syslog informational
no ipv6 forwarding
service integrated-vtysh-config
!
router bgp 65196
 bgp router-id 10.9.9.1
 no bgp ebgp-requires-policy
 no bgp network import-check
 !
 neighbor 10.9.9.2 remote-as 65195
 neighbor 10.9.9.2 description Node2-via-WireGuard
 neighbor 10.9.9.2 ebgp-multihop 2
 neighbor 10.9.9.2 timers 10 30
 !
 neighbor 10.9.9.3 remote-as 65194
 neighbor 10.9.9.3 description Node3-via-WireGuard
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

### Configuration File Location
- File: `/etc/frr/frr.conf`
- Permissions: `640`
- Owner: `frr:frr`

### Apply Configuration
```bash
sudo systemctl restart frr
```

---

## Configuration Templates

### Node 2 Template (AS 65195)
```
frr defaults traditional
hostname node2-bgp
log syslog informational
no ipv6 forwarding
service integrated-vtysh-config
!
router bgp 65195
 bgp router-id 10.9.9.2
 no bgp ebgp-requires-policy
 no bgp network import-check
 !
 neighbor 10.9.9.1 remote-as 65196
 neighbor 10.9.9.1 description Node1-via-WireGuard
 neighbor 10.9.9.1 ebgp-multihop 2
 neighbor 10.9.9.1 timers 10 30
 !
 neighbor 10.9.9.3 remote-as 65194
 neighbor 10.9.9.3 description Node3-via-WireGuard
 neighbor 10.9.9.3 ebgp-multihop 2
 neighbor 10.9.9.3 timers 10 30
 !
 address-family ipv4 unicast
  network 10.10.198.0/24
  neighbor 10.9.9.1 activate
  neighbor 10.9.9.3 activate
 exit-address-family
exit
!
line vty
!
```

### Node 3 Template (AS 65194)
```
frr defaults traditional
hostname node3-bgp
log syslog informational
no ipv6 forwarding
service integrated-vtysh-config
!
router bgp 65194
 bgp router-id 10.9.9.3
 no bgp ebgp-requires-policy
 no bgp network import-check
 !
 neighbor 10.9.9.1 remote-as 65196
 neighbor 10.9.9.1 description Node1-via-WireGuard
 neighbor 10.9.9.1 ebgp-multihop 2
 neighbor 10.9.9.1 timers 10 30
 !
 neighbor 10.9.9.2 remote-as 65195
 neighbor 10.9.9.2 description Node2-via-WireGuard
 neighbor 10.9.9.2 ebgp-multihop 2
 neighbor 10.9.9.2 timers 10 30
 !
 address-family ipv4 unicast
  network 10.10.197.0/24
  neighbor 10.9.9.1 activate
  neighbor 10.9.9.2 activate
 exit-address-family
exit
!
line vty
!
```

---

## Deployment Process

### 1. Prepare Node Inventory
Create a configuration file (`nodes.conf`):
```ini
[node1]
mgmt_ip=123.200.0.50
overlay_ip=10.9.9.1
as_number=65196
lxd_network=10.10.199.0/24
hostname=node1-bgp
ssh_port=8210
ssh_user=tbsms
ssh_pass=TB@l38800

[node2]
mgmt_ip=123.200.0.117
overlay_ip=10.9.9.2
as_number=65195
lxd_network=10.10.198.0/24
hostname=node2-bgp
ssh_port=8210
ssh_user=tbsms
ssh_pass=TB@l38800

[node3]
mgmt_ip=123.200.0.51
overlay_ip=10.9.9.3
as_number=65194
lxd_network=10.10.197.0/24
hostname=node3-bgp
ssh_port=8210
ssh_user=tbsms
ssh_pass=TB@l38800
```

### 2. Deployment Sequence

#### Step 1: Install FRR on all nodes
```bash
for node in node1 node2 node3; do
    install_frr $node
done
```

#### Step 2: Configure BGP daemons
```bash
for node in node1 node2 node3; do
    enable_bgp_daemon $node
done
```

#### Step 3: Generate and deploy BGP configurations
```bash
for node in node1 node2 node3; do
    generate_bgp_config $node
    deploy_bgp_config $node
done
```

#### Step 4: Restart FRR services
```bash
for node in node1 node2 node3; do
    restart_frr $node
done
```

#### Step 5: Verify BGP peering
```bash
for node in node1 node2 node3; do
    verify_bgp_peering $node
done
```

---

## Verification Commands

### Check BGP Status
```bash
sudo vtysh -c 'show ip bgp summary'
```

Expected output shows neighbors in "Established" state with prefix counts.

### Check BGP Routes
```bash
sudo vtysh -c 'show ip bgp'
```

Expected: Routes from all nodes visible with next-hop via overlay IPs.

### Check Kernel Routing Table
```bash
ip route show | grep 10.10
```

Expected: Routes installed for all LXD networks via WireGuard overlay.

### Test Container Connectivity
```bash
# From Node 2 container, ping Node 3 container
lxc exec <container-name> -- ping 10.10.197.11
```

### Check FRR Service Status
```bash
sudo systemctl status frr
```

### View FRR Logs
```bash
sudo journalctl -u frr -f
```

---

## Java Automation Structure

### Suggested Package Structure
```
com.telcobright.network.frr/
├── model/
│   ├── Node.java
│   ├── BGPConfig.java
│   ├── BGPNeighbor.java
│   └── DeploymentResult.java
├── service/
│   ├── SSHExecutor.java
│   ├── FRRInstaller.java
│   ├── BGPConfigurator.java
│   └── DeploymentOrchestrator.java
├── util/
│   ├── ConfigParser.java
│   ├── TemplateEngine.java
│   └── ValidationUtil.java
└── FRRDeploymentMain.java
```

### Key Classes Overview

#### 1. Node.java
```java
public class Node {
    private String name;
    private String mgmtIP;
    private String overlayIP;
    private int asNumber;
    private String lxdNetwork;
    private String hostname;
    private int sshPort;
    private String sshUser;
    private String sshPassword;
    private List<BGPNeighbor> neighbors;
    // Getters, setters, constructors
}
```

#### 2. BGPConfig.java
```java
public class BGPConfig {
    private int asNumber;
    private String routerId;
    private List<String> networks;
    private List<BGPNeighbor> neighbors;
    private boolean ebgpRequiresPolicy = false;
    private boolean networkImportCheck = false;
    // Getters, setters, methods
}
```

#### 3. BGPNeighbor.java
```java
public class BGPNeighbor {
    private String ip;
    private int remoteAS;
    private String description;
    private int keepaliveTimer = 10;
    private int holdTimer = 30;
    // Getters, setters
}
```

#### 4. SSHExecutor.java
```java
public class SSHExecutor {
    public String executeCommand(Node node, String command);
    public void uploadFile(Node node, String localPath, String remotePath);
    public void downloadFile(Node node, String remotePath, String localPath);
}
```

#### 5. FRRInstaller.java
```java
public class FRRInstaller {
    private SSHExecutor sshExecutor;

    public boolean installFRR(Node node);
    public boolean enableBGPDaemon(Node node);
    public boolean verifyInstallation(Node node);
}
```

#### 6. BGPConfigurator.java
```java
public class BGPConfigurator {
    private SSHExecutor sshExecutor;
    private TemplateEngine templateEngine;

    public String generateConfig(Node node, List<Node> allNodes);
    public boolean deployConfig(Node node, String config);
    public boolean restartFRR(Node node);
    public BGPStatus verifyBGP(Node node);
}
```

#### 7. DeploymentOrchestrator.java
```java
public class DeploymentOrchestrator {
    private FRRInstaller installer;
    private BGPConfigurator configurator;

    public DeploymentResult deployAll(List<Node> nodes);
    public DeploymentResult deployNode(Node node, List<Node> allNodes);
    public VerificationResult verifyDeployment(List<Node> nodes);
}
```

### Deployment Workflow in Java

```java
public class FRRDeploymentMain {
    public static void main(String[] args) {
        // 1. Load configuration
        List<Node> nodes = ConfigParser.parseNodesConfig("nodes.conf");

        // 2. Create orchestrator
        DeploymentOrchestrator orchestrator = new DeploymentOrchestrator();

        // 3. Deploy to all nodes
        DeploymentResult result = orchestrator.deployAll(nodes);

        // 4. Verify deployment
        VerificationResult verification = orchestrator.verifyDeployment(nodes);

        // 5. Report results
        System.out.println(result.getSummary());
        System.out.println(verification.getSummary());
    }
}
```

---

## Critical Settings Summary

1. **FRR Package**: Install `frr` and `frr-pythontools`
2. **Enable BGP Daemon**: Set `bgpd=yes` in `/etc/frr/daemons`
3. **BGP Policy**: Add `no bgp ebgp-requires-policy` to router bgp config (CRITICAL)
4. **EBGP Multihop**: Add `neighbor X.X.X.X ebgp-multihop 2` for overlay networks (CRITICAL)
5. **Neighbor Activation**: Add `neighbor X.X.X.X activate` in address-family section (REQUIRED)
6. **Network Import**: Add `no bgp network import-check` if needed
7. **File Permissions**: `/etc/frr/frr.conf` must be `640` with owner `frr:frr`
8. **Restart Service**: `systemctl restart frr` after config changes
9. **Verification**: Use `vtysh -c 'show ip bgp summary'` to verify

---

## Troubleshooting

### BGP Neighbors Not Establishing
- Check WireGuard overlay connectivity first
- Verify AS numbers are different for EBGP
- Ensure `no bgp ebgp-requires-policy` is set
- Check firewall allows TCP port 179

### BGP Session Stuck in "Active" or "Connect" State
**Problem**: BGP neighbors show "Active" or "Connect" but never reach "Established"
**Cause**: EBGP TTL issue - BGP peers are not directly connected (going through WireGuard overlay)
**Symptom**: BGP packets have TTL=1 by default for EBGP, which gets decremented when passing through the WireGuard interface

**Solution**: Add `ebgp-multihop 2` to neighbor configuration:
```
router bgp 65196
 neighbor 10.9.9.2 ebgp-multihop 2
```

**Verification**:
```bash
# Check BGP session state
sudo vtysh -c 'show ip bgp summary'

# Should show "Established" instead of "Active" or "Connect"
```

### Routes Not Being Exchanged
- Verify `neighbor X.X.X.X activate` in address-family
- Check `no bgp ebgp-requires-policy` is present
- Ensure `ebgp-multihop 2` is configured for overlay networks
- Ensure networks are advertised with `network` command

### Routes Not Installing in Kernel
- Check if BGP learned the routes: `show ip bgp`
- Verify administrative distance
- Check for conflicting static routes

---

## Example: One-Click Deployment Script

See `deploy-frr-cluster.sh` in this directory for a complete bash implementation
that can be converted to Java.

---

## License
Internal use - TelcoBright Projects

## Author
Auto-generated from working FRR deployment configurations
