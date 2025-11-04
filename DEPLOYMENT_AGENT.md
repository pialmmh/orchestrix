# Deployment Agent - Interactive Automation Assistant

## Overview

The Deployment Agent is an AI-powered assistant that helps you execute deployment tasks using Orchestrix's Java automation framework. It analyzes available automations, creates execution plans, and guides you through deployment processes.

## Quick Start

### Activate the Agent

```bash
/deploy
```

Then describe what you want to deploy:

```
Setup LXD with bridge networking on 192.168.1.100
```

The agent will:
1. Analyze available automations
2. Create an execution plan
3. Request SSH credentials
4. Execute the deployment
5. Verify the results

## Use Cases

### 1. LXD Container Host Setup

**Request:**
```
Setup complete LXD environment with bridge networking on server 192.168.1.50
```

**What the Agent Does:**
- Detects Linux distribution (Ubuntu, Debian, CentOS, RHEL)
- Installs LXD if not present
- Configures snap paths
- Enables IP forwarding
- Initializes LXD with default bridge (lxdbr0)
- Configures NAT masquerading
- Verifies all components

**Automations Used:**
- `ConfigureLxdDefaultBridgeAutomation` (composite)
- `LxdInstallerAutomation`
- `SnapInstallAutomation`
- `IpForwardAutomation`
- `LxdBridgeConfigureAutomation`
- `MasqueradeAutomation`

### 2. Deploy LXC Container

**Request:**
```
Deploy an LXC container named 'consul-server-1' from debian/12 image
```

**What the Agent Does:**
- Verifies LXD prerequisites
- Creates container from specified image
- Configures networking
- Starts the container
- Verifies container status

**Automations Used:**
- `LxcContainerDeployment`
- `LxcPrerequisitePreparer`

### 3. Network Configuration

**Request:**
```
Configure VXLAN networking for distributed containers
```

**What the Agent Does:**
- Analyzes VXLAN requirements
- Sets up VXLAN tunnels
- Configures subnet allocation
- Establishes inter-container networking

**Automations Used:**
- `VXLANService`
- `LXCNetworkManager`
- `SubnetManager`

### 4. Firewall and NAT

**Request:**
```
Configure NAT masquerading for 10.0.3.0/24 network
```

**What the Agent Does:**
- Detects firewall system (iptables/nftables)
- Creates masquerade rules
- Persists configuration
- Verifies NAT functionality

**Automations Used:**
- `MasqueradeAutomation`
- `IptablesMasqueradeAutomation` (distribution-specific)

### 5. MikroTik Router Configuration

**Request:**
```
Add static route on MikroTik router 192.168.1.1
```

**What the Agent Does:**
- Connects to RouterOS via SSH
- Creates static route entry
- Verifies route installation

**Automations Used:**
- `StaticRouteCreatorSimple`
- `MikroTikSshClient`

## Available Automations

### Linux Server Unit Automations

#### LXD Installation
- **Location:** `automation/devices/server/linux/lxdinstall/`
- **Purpose:** Install LXD on various Linux distributions
- **Distributions:** Ubuntu 22.04, Debian 12, CentOS 7, RHEL 8, Default
- **Factory:** `LxdInstallerAutomationFactory`

#### Snap Installation
- **Location:** `automation/devices/server/linux/snapinstall/`
- **Purpose:** Install and configure Snap package manager
- **Distributions:** Ubuntu 22.04, Debian 12, CentOS 7, RHEL 8, Default
- **Factory:** `SnapInstallAutomationFactory`

#### IP Forwarding
- **Location:** `automation/devices/server/linux/ipforward/`
- **Purpose:** Enable IP forwarding for routing/NAT
- **Distributions:** Ubuntu 22.04, Debian 12, CentOS 7, RHEL 8, Default
- **Factory:** `IpForwardAutomationFactory`

#### LXD Bridge Configuration
- **Location:** `automation/devices/server/linux/lxdbridge/`
- **Purpose:** Configure LXD network bridges
- **Distributions:** Ubuntu 22.04, Debian 12, CentOS 7, RHEL 8, Default
- **Factory:** `LxdBridgeConfigureAutomationFactory`

#### NAT Masquerading
- **Location:** `automation/devices/server/linux/firewall/nat/masquerade/`
- **Purpose:** Configure NAT masquerading for containers
- **Distributions:** Distribution-specific implementations
- **Factory:** `MasqueradeAutomationFactory`

#### Package Management
- **Location:** `automation/devices/server/linux/packageinstall/`
- **Purpose:** Install system packages (apt, yum, dnf)

#### Service Management
- **Location:** `automation/devices/server/linux/servicemanagement/`
- **Purpose:** Manage systemd and SysVinit services
- **Implementations:**
  - `SystemdServiceManagement`
  - `SysVInitServiceManagement`

#### User Management
- **Location:** `automation/devices/server/linux/usermanagement/`
- **Purpose:** Create and manage system users

#### Kernel Parameters
- **Location:** `automation/devices/server/linux/kernelparams/`
- **Purpose:** Configure kernel parameters via sysctl

#### Network Configuration
- **Location:** `automation/devices/server/linux/networkconfig/`
- **Purpose:** Configure network interfaces

### Composite Automations

#### Configure LXD Default Bridge
- **Class:** `ConfigureLxdDefaultBridgeAutomation`
- **Location:** `automation/devices/server/linux/composite/`
- **Purpose:** Complete LXD setup with bridge networking
- **Steps:**
  1. Detect distribution
  2. Install LXD
  3. Configure snap paths
  4. Enable IP forwarding
  5. Initialize LXD bridge
  6. Configure NAT masquerading
  7. Verify all components

### Container Deployment Automations

#### LXC Container Deployment
- **Class:** `LxcContainerDeployment`
- **Location:** `automation/devices/server/linux/containerdeploy/lxc/`
- **Features:**
  - Create from image
  - Configure resources (CPU, memory)
  - Network configuration
  - Storage management
  - Start/stop/delete operations

#### Docker Container Deployment
- **Class:** `DockerContainerDeployment`
- **Location:** `automation/devices/server/linux/containerdeploy/docker/`
- **Features:**
  - Pull images
  - Container lifecycle management
  - Network and volume configuration

#### LXC Prerequisites Preparation
- **Class:** `LxcPrerequisitePreparer`
- **Purpose:** Verify and setup LXC requirements
- **Checks:**
  - LXD installed
  - LXD initialized
  - Default storage pool exists
  - Default network bridge exists

### Network Automations

#### VXLAN Service
- **Class:** `VXLANService`
- **Location:** `automation/vxlan/api/`
- **Purpose:** Create and manage VXLAN tunnels

#### LXC Network Manager
- **Class:** `LXCNetworkManager`
- **Purpose:** Manage LXC container networking

#### Subnet Manager
- **Class:** `SubnetManager`
- **Purpose:** Allocate and manage IP subnets

### Device-Specific Automations

#### MikroTik Router
- **Location:** `automation/devices/networking/mikrotik/`
- **Automations:**
  - `StaticRouteCreatorSimple` - Create static routes
  - `DstNatIpToIp` - Configure destination NAT

## Automation Framework Architecture

### Base Interface: LinuxAutomation

All Linux automations implement:

```java
public interface LinuxAutomation {
    boolean execute(SshDevice device) throws Exception;
    boolean verify(SshDevice device) throws Exception;
    Map<String, String> getStatus(SshDevice device);
    LinuxDistribution getSupportedDistribution();
    boolean isCompatible(LinuxDistribution distribution);
    String getName();
    String getDescription();
}
```

### Factory Pattern

Distribution-specific implementations are selected via factories:

```java
LxdInstallerAutomation installer =
    LxdInstallerAutomationFactory.createLxdInstallerAutomation(
        LinuxDistribution.UBUNTU_22_04,
        true  // useSudo
    );
```

### Execution Runners

#### SSH Runner
Execute automations on remote hosts:
```java
SshDevice device = new SshDevice(
    "192.168.1.100",  // host
    22,               // port
    "ubuntu",         // username
    "password",       // password or null
    "/path/to/key"    // key path or null
);

automation.execute(device);
```

#### Terminal Runner
Execute automations locally:
```java
// For local execution
automation.execute(localDevice);
```

#### Series Runner
Execute multiple automations in sequence:
```java
SshSeriesAutomationRunner runner = new SshSeriesAutomationRunner();
runner.addAutomation(automation1);
runner.addAutomation(automation2);
runner.execute(device);
```

## SSH Credential Management

### Authentication Methods

#### Password Authentication
```
Host: 192.168.1.100
Port: 22
Username: ubuntu
Password: mypassword
Sudo: yes
```

#### Private Key Authentication
```
Host: 192.168.1.100
Port: 22
Username: ubuntu
Private Key: /home/user/.ssh/id_rsa
Sudo: yes
```

### Sudo Requirements

Most server automations require sudo privileges:
- Installing packages
- Configuring system services
- Modifying network settings
- Managing firewall rules

## Supported Linux Distributions

### Ubuntu 22.04
- Full support for all automations
- Snap-based LXD installation
- Systemd service management
- APT package management

### Debian 12
- Full support for all automations
- Snap or native LXD installation
- Systemd service management
- APT package management

### CentOS 7
- Full support for all automations
- Snap-based LXD installation
- SysVinit/Systemd hybrid
- YUM package management

### RHEL 8
- Full support for all automations
- Snap-based LXD installation
- Systemd service management
- DNF package management

### Unknown/Default
- Fallback implementations
- Basic functionality
- May require manual intervention

## Agent Capabilities

### 1. Discovery and Analysis
The agent can:
- Scan the entire automation codebase
- Identify relevant automations for your request
- Understand dependencies between automations
- Detect gaps in automation coverage

### 2. Execution Planning
The agent creates detailed plans including:
- Prerequisites verification
- Automation sequence
- Expected outcomes
- Failure recovery steps
- Verification methods

### 3. Interactive Execution
The agent:
- Requests necessary credentials
- Provides real-time progress updates
- Handles errors gracefully
- Suggests corrective actions
- Verifies successful completion

### 4. Gap Identification
When automation doesn't exist, the agent:
- Identifies the missing capability
- Recommends automation architecture
- Suggests which existing automations to extend
- Provides implementation guidance

## Example Interactions

### Example 1: Complete LXD Setup

**User Request:**
```
/deploy

Setup LXD on debian server 192.168.50.10 with default bridge networking
```

**Agent Response:**
```
📋 DEPLOYMENT PLAN

Task: Setup LXD with default bridge networking on Debian server

Available Automations:
✅ ConfigureLxdDefaultBridgeAutomation - Complete LXD setup
  ├─ LxdInstallerAutomation (Debian 12)
  ├─ SnapInstallAutomation (Debian 12)
  ├─ IpForwardAutomation (Debian 12)
  ├─ LxdBridgeConfigureAutomation (Debian 12)
  └─ MasqueradeAutomation (Debian 12)

Execution Sequence:
1. Detect Debian version
2. Install LXD via snap (if not present)
3. Configure snap binary paths
4. Enable IPv4 forwarding (persistent)
5. Initialize LXD with preseed configuration
   - Bridge: lxdbr0
   - Network: 10.0.3.1/24
   - DHCP: 10.0.3.2-10.0.3.254
   - Storage: dir backend
6. Configure iptables NAT masquerading
7. Verify all components operational

Expected Results:
✅ LXD installed and running
✅ Bridge lxdbr0 created and operational
✅ IP forwarding enabled
✅ NAT configured for internet access
✅ System ready for container deployments

SSH Requirements:
- Host: 192.168.50.10
- Port: 22 (default)
- Username: [Required]
- Authentication: password or private key
- Sudo: Yes (required for system configuration)

Please provide SSH credentials:
```

**User Provides Credentials:**
```
Username: admin
Password: secretpass
```

**Agent Executes:**
```
🚀 EXECUTING DEPLOYMENT

Connecting to 192.168.50.10...
✅ Connected successfully

Step 1/7: Detecting distribution...
✅ Detected: Debian 12 (bookworm)

Step 2/7: Checking LXD installation...
⚠️  LXD not found, installing...
✅ LXD installed via snap

Step 3/7: Configuring snap paths...
✅ Snap paths configured

Step 4/7: Enabling IP forwarding...
✅ IPv4 forwarding enabled (persistent)

Step 5/7: Initializing LXD bridge...
✅ LXD initialized with bridge lxdbr0
   Network: 10.0.3.1/24
   DHCP: 10.0.3.2-10.0.3.254

Step 6/7: Configuring NAT masquerading...
✅ NAT masquerading configured
   Source: 10.0.3.0/24
   Interface: eth0

Step 7/7: Verifying configuration...
✅ LXD service running
✅ Bridge operational
✅ IP forwarding enabled
✅ NAT configured

🎉 DEPLOYMENT COMPLETE

System is ready for LXC container deployments.

Quick test:
  lxc launch ubuntu:22.04 test-container
```

### Example 2: Gap Identification

**User Request:**
```
/deploy

Setup Kubernetes cluster on 3 nodes
```

**Agent Response:**
```
📋 DEPLOYMENT ANALYSIS

Task: Setup Kubernetes cluster on 3 nodes

Available Automations:
✅ SSH connectivity and device management
✅ Linux server configuration (various)
✅ Container runtime setup (LXD, Docker)
❌ Kubernetes installation
❌ Kubernetes cluster initialization
❌ Kubernetes node joining
❌ CNI plugin configuration

GAP ANALYSIS:

Missing Automations Required:
1. KubernetesInstallerAutomation
   - Install kubeadm, kubelet, kubectl
   - Configure container runtime (containerd/docker)
   - Distribution-specific implementations

2. KubernetesClusterInitAutomation
   - Initialize control plane
   - Generate join tokens
   - Configure networking

3. KubernetesNodeJoinAutomation
   - Join worker nodes to cluster
   - Configure node labels
   - Verify node readiness

4. KubernetesCniAutomation
   - Install CNI plugin (Calico, Flannel, Weave)
   - Configure pod networking

RECOMMENDATION:

Create new automation package:
automation/devices/server/linux/kubernetes/

Suggested Structure:
├── KubernetesInstallAutomation.java
├── impl/
│   ├── Ubuntu2204KubernetesInstaller.java
│   ├── Debian12KubernetesInstaller.java
│   └── DefaultKubernetesInstaller.java
├── KubernetesClusterAutomation.java
└── KubernetesInstallerFactory.java

Would you like me to:
1. Create a detailed specification for these automations?
2. Generate skeleton code following the existing pattern?
3. Provide installation scripts to use until automations are built?
```

## Wrapper Scripts

For quick execution without Java code:

### LXC Prerequisites Setup
```bash
cd automation/scripts
./setup-lxc.sh <host> <user> <port> <password> [keyPath]
```

**Example:**
```bash
./setup-lxc.sh 192.168.1.100 ubuntu 22 mypass
./setup-lxc.sh 192.168.1.100 ubuntu 22 "" /home/user/.ssh/id_rsa
```

### Local Execution
```bash
./setup-lxc.sh
```

## Troubleshooting

### SSH Connection Failed
**Error:** Cannot connect to remote host

**Solutions:**
- Verify SSH service is running: `systemctl status sshd`
- Check firewall rules: `sudo ufw status`
- Test manual connection: `ssh user@host`
- Verify credentials are correct

### Permission Denied
**Error:** sudo: no tty present

**Solutions:**
- Ensure sudo is installed
- Add user to sudoers: `usermod -aG sudo username`
- Use NOPASSWD in sudoers for automation user

### Distribution Detection Failed
**Error:** Unknown Linux distribution

**Solutions:**
- Specify distribution explicitly in automation
- Use default/fallback implementations
- Check `/etc/os-release` file exists

### Automation Verification Failed
**Error:** Component installed but verification fails

**Solutions:**
- Check service status manually
- Review system logs: `journalctl -xe`
- Verify network connectivity
- Check for conflicting configurations

## Best Practices

### 1. Always Verify Prerequisites
Before executing automations:
- Check target system requirements
- Verify SSH connectivity
- Ensure sufficient disk space
- Confirm sudo access

### 2. Use Composite Automations
Prefer composite automations that:
- Handle error recovery
- Verify prerequisites
- Provide comprehensive validation
- Log detailed progress

### 3. Test on Non-Production First
- Use test environments
- Verify automation behavior
- Check for side effects
- Document unexpected behavior

### 4. Monitor Execution
- Watch progress output
- Check for warnings
- Review verification results
- Save execution logs

### 5. Maintain Idempotency
- Automations should be safe to re-run
- Check before making changes
- Don't fail if already configured
- Verify desired state

## Integration with CI/CD

### Jenkins Pipeline Example
```groovy
pipeline {
    agent any
    stages {
        stage('Setup LXD') {
            steps {
                sh '''
                    cd automation/scripts
                    ./setup-lxc.sh ${TARGET_HOST} ${SSH_USER} 22 ${SSH_PASS}
                '''
            }
        }
    }
}
```

### GitLab CI Example
```yaml
deploy-lxd:
  script:
    - cd automation/scripts
    - ./setup-lxc.sh $TARGET_HOST $SSH_USER 22 $SSH_PASS
  only:
    - master
```

## API Reference

### SshDevice
```java
public class SshDevice {
    public SshDevice(String host, int port, String username,
                     String password, String privateKeyPath);
}
```

### LinuxAutomation Interface
```java
boolean execute(SshDevice device) throws Exception;
boolean verify(SshDevice device) throws Exception;
Map<String, String> getStatus(SshDevice device);
```

### Factory Methods
```java
LxdInstallerAutomationFactory.createLxdInstallerAutomation(
    LinuxDistribution distribution, boolean useSudo)

SnapInstallAutomationFactory.createSnapInstallAutomation(
    LinuxDistribution distribution, boolean useSudo)

IpForwardAutomationFactory.createIpForwardAutomation(
    LinuxDistribution distribution, boolean useSudo)
```

## Contributing

To add new automations:

1. Create automation class implementing `LinuxAutomation`
2. Extend `AbstractLinuxAutomation` for common functionality
3. Create distribution-specific implementations
4. Add factory class for distribution selection
5. Create wrapper script for CLI execution
6. Add tests and documentation
7. Update this document

## See Also

- [README.md](README.md) - Main project documentation
- [LXC Prerequisites Automation](automation/LXC_PREREQUISITES_AUTOMATION.md)
- [Container Scaffolding Standard](images/containers/lxc/CONTAINER_SCAFFOLDING_STANDARD.md)
- [Local Testing Guide](images/containers/lxc/LOCAL_TESTING_GUIDE.md)

## Support

For issues or questions:
1. Check this documentation
2. Review automation source code
3. Use `/deploy` agent for guidance
4. Test in isolated environment first
