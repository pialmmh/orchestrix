# KS Network Deployment - Container Topology and Networking

## Overview
The ks_network deployment consists of a 3-node cluster with overlay networking (WireGuard + FRR BGP) and Percona MySQL master-slave replication. All nodes are connected via a full-mesh WireGuard VPN with BGP routing for container subnet announcement.

## Infrastructure Topology

### Physical Nodes

| Node | Management IP | Container Subnet | Overlay IP | Role |
|------|---------------|------------------|------------|------|
| Node 1 | 172.27.27.132 | 10.10.199.0/24 | 10.9.10.1/24 | MySQL Master, BGP Router |
| Node 2 | 172.27.27.136 | 10.10.198.0/24 | 10.9.10.2/24 | MySQL Slave, BGP Router |
| Node 3 | 172.27.27.135 | 10.10.197.0/24 | 10.9.10.3/24 | BGP Router |

### Network Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                     Management Network                          │
│                     172.27.27.0/24                              │
│   (SSH access, out-of-band management)                          │
└─────────────┬───────────────┬───────────────┬──────────────────┘
              │               │               │
    ┌─────────▼─────────┐ ┌──▼──────────┐ ┌──▼──────────┐
    │   Node 1          │ │  Node 2     │ │  Node 3     │
    │ 172.27.27.132     │ │172.27.27.136│ │172.27.27.135│
    └─────────┬─────────┘ └──┬──────────┘ └──┬──────────┘
              │               │               │
┌─────────────▼───────────────▼───────────────▼──────────────────┐
│              WireGuard Overlay Network                          │
│                   10.9.10.0/24                                  │
│   (Full-mesh VPN for inter-node container communication)       │
│                                                                 │
│   Node 1: 10.9.10.1    Node 2: 10.9.10.2    Node 3: 10.9.10.3 │
│   Port: 51820/UDP                                               │
└─────────────┬───────────────┬───────────────┬──────────────────┘
              │               │               │
    ┌─────────▼─────────┐ ┌──▼──────────┐ ┌──▼──────────┐
    │   FRR BGP         │ │  FRR BGP    │ │  FRR BGP    │
    │   AS 64512        │ │  AS 64513   │ │  AS 64514   │
    │   Router ID       │ │  Router ID  │ │  Router ID  │
    │   10.9.10.1       │ │  10.9.10.2  │ │  10.9.10.3  │
    └─────────┬─────────┘ └──┬──────────┘ └──┬──────────┘
              │               │               │
              │  BGP announces container subnets                  │
              │               │               │
    ┌─────────▼─────────┐ ┌──▼──────────┐ ┌──▼──────────┐
    │   lxdbr0          │ │  lxdbr0     │ │  lxdbr0     │
    │ 10.10.199.1/24    │ │10.10.198.1  │ │10.10.197.1  │
    └─────────┬─────────┘ └──┬──────────┘ └──┬──────────┘
              │               │               │
    ┌─────────▼─────────┐ ┌──▼──────────┐   │
    │ percona-master    │ │percona-slave│   │
    │ 10.10.199.10:3306 │ │10.10.198.10 │   │
    │                   │ │:3306        │   │
    │ GTID Replication ─┼─┼────────────►│   │
    └───────────────────┘ └─────────────┘   │
                                             │
                          (Reserved for future containers)
```

## Network Configuration Details

### 1. Management Network (172.27.27.0/24)
- **Purpose**: SSH access, out-of-band management
- **SSH User**: csas (password authentication configured)
- **Access**: Direct SSH from authorized networks
- **Firewall**: Internal network, all ports open between nodes

### 2. WireGuard Overlay Network (10.9.10.0/24)
- **Purpose**: Encrypted inter-node communication for containers
- **Protocol**: WireGuard VPN (UDP port 51820)
- **Topology**: Full-mesh (each node peers with all others)
- **Configuration**: `overlay-config.conf`
- **External VPN Access**: Port 51820/UDP must be open in BTCL network firewall for remote clients
- **Key Files**: `/etc/wireguard/wg-overlay.conf` on each node

**Peer Relationships**:
```
Node 1 (10.9.10.1) ←→ Node 2 (10.9.10.2)
Node 1 (10.9.10.1) ←→ Node 3 (10.9.10.3)
Node 2 (10.9.10.2) ←→ Node 3 (10.9.10.3)
```

### 3. Container Networks (lxdbr0 bridges)
- **Node 1**: 10.10.199.0/24 (gateway: 10.10.199.1)
- **Node 2**: 10.10.198.0/24 (gateway: 10.10.198.1)
- **Node 3**: 10.10.197.0/24 (gateway: 10.10.197.1)
- **Purpose**: Container IP assignment via DHCP or static configuration
- **Routing**: Announced via BGP over WireGuard overlay

### 4. BGP Routing (FRR)
- **Protocol**: BGP (Border Gateway Protocol)
- **Purpose**: Automatic route distribution for container subnets
- **Configuration**: Each node runs FRR router

| Node | AS Number | Router ID | Announced Subnet |
|------|-----------|-----------|------------------|
| Node 1 | 64512 | 10.9.10.1 | 10.10.199.0/24 |
| Node 2 | 64513 | 10.9.10.2 | 10.10.198.0/24 |
| Node 3 | 64514 | 10.9.10.3 | 10.10.197.0/24 |

**BGP Neighbors**: All nodes peer with each other over WireGuard IPs

## Deployed Services

### Percona MySQL 5.7 Master-Slave Cluster

#### Master (Node 1)
- **Container**: percona-master
- **IP**: 10.10.199.10:3306
- **Server ID**: 1
- **Role**: Read/Write
- **GTID**: Enabled with auto-positioning
- **InnoDB Buffer Pool**: 4GB
- **Binary Log Retention**: 7 days

#### Slave (Node 2)
- **Container**: percona-slave1
- **IP**: 10.10.198.10:3306
- **Server ID**: 2
- **Role**: Read-Only (replication)
- **Replication User**: repl@'%' (password in cluster-config.conf)
- **Replication Method**: GTID-based with MASTER_AUTO_POSITION=1

#### MySQL User Access
- **root@localhost**: Full access via socket
- **root@'%'**: Full access from any host
- **root@'10.10.%'**: Full access from container subnet (10.10.0.0/16)
- **root@'10.9.%'**: Full access from overlay network (10.9.0.0/16)
- **repl@'%'**: Replication user (REPLICATION SLAVE, REPLICATION CLIENT)

**Note**: Root password is stored in `percona/cluster-config.conf` (not in code)

## Accessing Services

### From VPN Client
1. Connect to WireGuard VPN (port 51820/UDP)
2. Receive pushed routes for 10.10.0.0/16 and 10.9.10.0/24
3. Access MySQL directly:
   ```bash
   mysql -h 10.10.199.10 -u root -p
   mysql -h 10.10.198.10 -u root -p
   ```

### From Any Node
All container subnets are reachable from any node via BGP routing:
```bash
# From Node 3, access MySQL on Node 1
mysql -h 10.10.199.10 -u root -p

# From Node 1, access MySQL slave on Node 2
mysql -h 10.10.198.10 -u root -p
```

### SSH Access to Nodes
```bash
# Node 1 (MySQL Master)
ssh csas@172.27.27.132

# Node 2 (MySQL Slave)
ssh csas@172.27.27.136

# Node 3
ssh csas@172.27.27.135
```

**Security Note**: SSH passwords are not stored in code. Configure SSH keys or use secure credential management.

## Configuration Files

### Overlay Network
- **Config**: `deployments/ks_network/overlay-config.conf`
- **Generated Files**: `/etc/wireguard/wg-overlay.conf` on each node
- **Deployment Class**: `OverlayNetworkDeployment.java`

### Percona MySQL Cluster
- **Config**: `deployments/ks_network/percona/cluster-config.conf`
- **Deployment Class**: `KsNetworkPerconaDeployment.java`
- **Replication Setup**: `SetupPerconaReplication.java`
- **Network Access**: `ConfigurePerconaNetworkAccess.java`

### Generated Docker Compose
- **Master**: `~/percona-master/docker-compose.yml` on Node 1
- **Slave**: `~/percona-slave1/docker-compose.yml` on Node 2

## Deployment Steps (For Future Reference)

### 1. Deploy Overlay Network
```bash
mvn compile exec:java \
  -Dexec.mainClass="com.telcobright.orchestrix.automation.overlay.OverlayNetworkDeployment" \
  -Dexec.args="deployments/ks_network"
```

### 2. Deploy Percona MySQL Cluster
```bash
mvn exec:java \
  -Dexec.mainClass="com.telcobright.orchestrix.automation.example.KsNetworkPerconaDeployment"
```

### 3. Setup Replication (if needed manually)
```bash
mvn exec:java \
  -Dexec.mainClass="com.telcobright.orchestrix.automation.example.SetupPerconaReplication"
```

### 4. Configure Network Access (if needed)
```bash
mvn exec:java \
  -Dexec.mainClass="com.telcobright.orchestrix.automation.example.ConfigurePerconaNetworkAccess"
```

## Troubleshooting Utilities

### Check Container Status
```bash
mvn exec:java \
  -Dexec.mainClass="com.telcobright.orchestrix.automation.example.CheckPerconaContainer"
```

### Fix Slave Issues
- **FixSlaveReadOnly.java**: Disable super-read-only mode for setup
- **ReinitializePerconaSlave.java**: Clean data directory and reinitialize
- **ResetSlavePassword.java**: Reset root password using skip-grant-tables

### Check Replication Status
On Node 2 (slave):
```bash
ssh csas@172.27.27.136
sudo docker exec percona-slave1 mysql -h10.10.198.10 -uroot -p -e 'SHOW SLAVE STATUS\G'
```

Look for:
- `Slave_IO_Running: Yes`
- `Slave_SQL_Running: Yes`
- `Seconds_Behind_Master: 0`

## Firewall Requirements

### External Access (BTCL Network)
- **Port 51820/UDP**: WireGuard VPN for external clients

### Internal (Between Nodes)
- **All ports open**: Nodes 172.27.27.132, 172.27.27.136, 172.27.27.135

## Network Subnet Allocation

| Subnet | Purpose | CIDR |
|--------|---------|------|
| 172.27.27.0/24 | Management network | /24 |
| 10.9.10.0/24 | WireGuard overlay | /24 |
| 10.10.199.0/24 | Node 1 containers (lxdbr0) | /24 |
| 10.10.198.0/24 | Node 2 containers (lxdbr0) | /24 |
| 10.10.197.0/24 | Node 3 containers (lxdbr0) | /24 |
| 10.10.0.0/16 | Full container range (reserved) | /16 |
| 10.9.0.0/16 | Full overlay range (reserved) | /16 |

## Replication Verification

### Test Data Replication
On master (Node 1):
```bash
mysql -h 10.10.199.10 -u root -p -e "CREATE DATABASE test_repl; USE test_repl; CREATE TABLE test (id INT, data VARCHAR(50)); INSERT INTO test VALUES (1, 'replication test');"
```

On slave (Node 2):
```bash
mysql -h 10.10.198.10 -u root -p -e "USE test_repl; SELECT * FROM test;"
```

Should show the inserted data, confirming replication is working.

### Check GTID Execution
```bash
# Master
mysql -h 10.10.199.10 -u root -p -e "SELECT @@GLOBAL.gtid_executed;"

# Slave
mysql -h 10.10.198.10 -u root -p -e "SELECT @@GLOBAL.gtid_executed;"
```

Both should show matching GTID sets.

## Future Expansion

### Adding More Nodes
1. Allocate subnet (e.g., 10.10.196.0/24 for Node 4)
2. Add WireGuard peer configuration to overlay-config.conf
3. Configure BGP AS number (e.g., 64515)
4. Deploy overlay network
5. Configure lxdbr0 bridge

### Deploying Additional Services
- Kafka cluster (planned)
- Redis cache
- Application containers
- Monitoring stack

All containers will automatically be reachable via BGP-routed subnets.

## Maintenance

### WireGuard VPN
- **Start**: `sudo systemctl start wg-quick@wg-overlay`
- **Stop**: `sudo systemctl stop wg-quick@wg-overlay`
- **Status**: `sudo wg show wg-overlay`

### FRR BGP
- **Check routes**: `sudo vtysh -c 'show ip route'`
- **Check BGP**: `sudo vtysh -c 'show bgp summary'`
- **Check neighbors**: `sudo vtysh -c 'show bgp neighbors'`

### MySQL Containers
- **Master logs**: `sudo docker logs percona-master`
- **Slave logs**: `sudo docker logs percona-slave1`
- **Restart master**: `cd ~/percona-master && sudo docker compose restart`
- **Restart slave**: `cd ~/percona-slave1 && sudo docker compose restart`

## Security Considerations

1. **SSH Access**: Use SSH keys instead of password authentication in production
2. **MySQL Root**: Change default root password in cluster-config.conf
3. **Replication User**: Use strong password for repl user
4. **Firewall**: Restrict WireGuard VPN access to authorized IPs
5. **Network Isolation**: Management network should be separated from production traffic

## Contact and Support

For issues or questions:
- Check troubleshooting utilities in `src/main/java/.../example/`
- Review container logs: `sudo docker logs <container-name>`
- Check network connectivity: `ping <target-ip>`
- Verify BGP routes: `sudo vtysh -c 'show ip route'`
