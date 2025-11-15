# BDCOM Cluster - IP Allocation Summary

**Date:** 2025-11-04
**Cluster:** BDCOM SMS Application Cluster
**Servers:** 3 nodes (10.255.246.173-175)
**Status:** Network configured, IP standardization in progress

---

## Table of Contents

1. [Network Overview](#network-overview)
2. [Current IP Allocations](#current-ip-allocations)
3. [Reserved IP Assignments](#reserved-ip-assignments)
4. [Migration Plan](#migration-plan)
5. [Configuration Examples](#configuration-examples)

---

## Network Overview

### Server Configuration

| Server | Management IP | Hostname | Container Subnet | Bridge IP | Overlay IP | BGP AS |
|--------|--------------|----------|------------------|-----------|------------|---------|
| **Server 1** | 10.255.246.173 | sms-application | 10.10.199.0/24 | 10.10.199.1 | 10.9.9.4 | 65193 |
| **Server 2** | 10.255.246.174 | sms-app-master-db-dc | 10.10.198.0/24 | 10.10.198.1 | 10.9.9.5 | 65192 |
| **Server 3** | 10.255.246.175 | sms-app-slave-db-dc | 10.10.197.0/24 | 10.10.197.1 | 10.9.9.6 | 65191 |

### IP Range Allocation per Server

Each server has a /24 subnet divided as follows:

```
Server 1 (10.10.199.0/24):
├── .0              Network address
├── .1              Bridge gateway (lxdbr0)
├── .2-.99          Reserved for infrastructure & standard applications
├── .100-.199       Docker containers (dynamic allocation)
└── .200-.254       LXD containers (dynamic allocation)

Server 2 (10.10.198.0/24):
├── .0              Network address
├── .1              Bridge gateway (lxdbr0)
├── .2-.99          Reserved for infrastructure & standard applications
├── .100-.199       Docker containers (dynamic allocation)
└── .200-.254       LXD containers (dynamic allocation)

Server 3 (10.10.197.0/24):
├── .0              Network address
├── .1              Bridge gateway (lxdbr0)
├── .2-.99          Reserved for infrastructure & standard applications
├── .100-.199       Docker containers (dynamic allocation)
└── .200-.254       LXD containers (dynamic allocation)
```

---

## Current IP Allocations

### Server 1 (10.255.246.173) - Current State

| Container Name | Current IP | Type | Status | Notes |
|---------------|-----------|------|--------|-------|
| MySQL | 10.10.199.246 | LXD | Running | ⚠️ Should move to 10.10.199.10 |
| SMS-Admin | 10.10.199.37 | LXD | Running | Custom app, can keep .37 |
| frr-router-node1 | 10.10.199.50 | LXD | Running | ✅ Correct (Nginx LB range) |
| zookeeper-single | 10.10.199.190 | LXD | Running | ⚠️ Should move to 10.10.199.25 |
| kafka-broker-1 | - | LXD | Stopped | Should use 10.10.199.20 |

### Server 2 (10.255.246.174) - Current State

| Container Name | Current IP | Type | Status | Notes |
|---------------|-----------|------|--------|-------|
| MySQL | 10.10.198.224 | LXD | Running | ⚠️ Should move to 10.10.198.10 |
| frr-router-node2 | 10.10.199.55 | LXD | Running | ⚠️ Wrong subnet! Should be 10.10.198.x |
| zk-2 | 10.10.199.95 | LXD | Running | ⚠️ Wrong subnet! Should be 10.10.198.25 |
| kafka-broker-2 | - | LXD | Stopped | Should use 10.10.198.20 |

### Server 3 (10.255.246.175) - Current State

| Container Name | Current IP | Type | Status | Notes |
|---------------|-----------|------|--------|-------|
| frr-router-node3 | 10.10.199.65 | LXD | Running | ⚠️ Wrong subnet! Should be 10.10.197.x |
| zk-3 | 10.10.199.80 | LXD | Running | ⚠️ Wrong subnet! Should be 10.10.197.25 |
| kafka-broker-3 | - | LXD | Stopped | Should use 10.10.197.20 |

---

## Reserved IP Assignments

### Standard Applications - Reserved IPs

Following the Orchestrix networking guidelines, here are the reserved IP assignments for BDCOM cluster:

#### Database Services

| Application | Server 1 | Server 2 | Server 3 | Purpose |
|------------|----------|----------|----------|---------|
| **MySQL Primary** | 10.10.199.10 | 10.10.198.10 | 10.10.197.10 | MySQL cluster nodes |
| **MySQL Secondary** | 10.10.199.11 | 10.10.198.11 | 10.10.197.11 | MySQL read replicas (if needed) |
| **Redis Master** | 10.10.199.18 | 10.10.198.18 | 10.10.197.18 | Redis instances (if needed) |

#### Message Queue / Stream Processing

| Application | Server 1 | Server 2 | Server 3 | Purpose |
|------------|----------|----------|----------|---------|
| **Kafka Broker 1** | 10.10.199.20 | - | - | Kafka broker on server 1 |
| **Kafka Broker 2** | - | 10.10.198.20 | - | Kafka broker on server 2 |
| **Kafka Broker 3** | - | - | 10.10.197.20 | Kafka broker on server 3 |
| **ZooKeeper Node 1** | 10.10.199.25 | - | - | ZK ensemble member 1 |
| **ZooKeeper Node 2** | - | 10.10.198.25 | - | ZK ensemble member 2 |
| **ZooKeeper Node 3** | - | - | 10.10.197.25 | ZK ensemble member 3 |

#### Web Servers & Application Services

| Application | Server 1 | Server 2 | Server 3 | Purpose |
|------------|----------|----------|----------|---------|
| **FRR Router** | 10.10.199.50 | 10.10.198.50 | 10.10.197.50 | BGP routers (LXC) |
| **SMS Admin UI** | 10.10.199.37 | - | - | SMS administration interface |

---

## Detailed IP Allocation Map

### Server 1: 10.10.199.0/24

#### Infrastructure Range (.1-.9)
| IP | Assignment | Status |
|----|-----------|--------|
| 10.10.199.1 | Bridge gateway (lxdbr0) | ✅ Configured |

#### Database Services (.10-.19)
| IP | Assignment | Current State | Action Required |
|----|-----------|--------------|-----------------|
| 10.10.199.10 | **MySQL Primary** | Currently at .246 | 🔄 Reconfigure container |
| 10.10.199.11 | MySQL Secondary | Not deployed | - |
| 10.10.199.18 | Redis Master | Not deployed | - |

#### Message Queue (.20-.29)
| IP | Assignment | Current State | Action Required |
|----|-----------|--------------|-----------------|
| 10.10.199.20 | **Kafka Broker 1** | Container stopped | 🔄 Reconfigure & start |
| 10.10.199.25 | **ZooKeeper Node 1** | Currently at .190 | 🔄 Reconfigure container |

#### Custom Applications (.30-.99)
| IP | Assignment | Current State | Action Required |
|----|-----------|--------------|-----------------|
| 10.10.199.37 | **SMS-Admin** | Running | ✅ Keep as-is |
| 10.10.199.50 | **FRR Router Node 1** | Running | ✅ Correct |

#### LXD Dynamic Range (.200-.254)
| IP | Current Assignment | Action Required |
|----|-------------------|-----------------|
| 10.10.199.190 | zookeeper-single (old) | 🔄 Migrate to .25 |
| 10.10.199.246 | MySQL (old) | 🔄 Migrate to .10 |

---

### Server 2: 10.10.198.0/24

#### Infrastructure Range (.1-.9)
| IP | Assignment | Status |
|----|-----------|--------|
| 10.10.198.1 | Bridge gateway (lxdbr0) | ✅ Configured |

#### Database Services (.10-.19)
| IP | Assignment | Current State | Action Required |
|----|-----------|--------------|-----------------|
| 10.10.198.10 | **MySQL Primary/Replica** | Currently at .224 | 🔄 Reconfigure container |
| 10.10.198.18 | Redis Master | Not deployed | - |

#### Message Queue (.20-.29)
| IP | Assignment | Current State | Action Required |
|----|-----------|--------------|-----------------|
| 10.10.198.20 | **Kafka Broker 2** | Container stopped | 🔄 Reconfigure & start |
| 10.10.198.25 | **ZooKeeper Node 2** | Wrong subnet (.199.95) | 🔄 Recreate container |

#### Web Servers (.50-.59)
| IP | Assignment | Current State | Action Required |
|----|-----------|--------------|-----------------|
| 10.10.198.50 | **FRR Router Node 2** | Wrong subnet (.199.55) | 🔄 Recreate container |

#### LXD Dynamic Range (.200-.254)
| IP | Current Assignment | Action Required |
|----|-------------------|-----------------|
| 10.10.198.224 | MySQL (old) | 🔄 Migrate to .10 |

---

### Server 3: 10.10.197.0/24

#### Infrastructure Range (.1-.9)
| IP | Assignment | Status |
|----|-----------|--------|
| 10.10.197.1 | Bridge gateway (lxdbr0) | ✅ Configured |

#### Database Services (.10-.19)
| IP | Assignment | Current State | Action Required |
|----|-----------|--------------|-----------------|
| 10.10.197.10 | **MySQL Primary/Replica** | Not deployed | 📋 Deploy if needed |
| 10.10.197.18 | Redis Master | Not deployed | - |

#### Message Queue (.20-.29)
| IP | Assignment | Current State | Action Required |
|----|-----------|--------------|-----------------|
| 10.10.197.20 | **Kafka Broker 3** | Container stopped | 🔄 Reconfigure & start |
| 10.10.197.25 | **ZooKeeper Node 3** | Wrong subnet (.199.80) | 🔄 Recreate container |

#### Web Servers (.50-.59)
| IP | Assignment | Current State | Action Required |
|----|-----------|--------------|-----------------|
| 10.10.197.50 | **FRR Router Node 3** | Wrong subnet (.199.65) | 🔄 Recreate container |

---

## Migration Plan

### Priority 1: Critical Database Services

**MySQL Cluster Configuration**

| Container | Current IP | Target IP | Action |
|----------|-----------|-----------|---------|
| MySQL @ Server 1 | 10.10.199.246 | 10.10.199.10 | Reconfigure IP |
| MySQL @ Server 2 | 10.10.198.224 | 10.10.198.10 | Reconfigure IP |
| MySQL @ Server 3 | Not deployed | 10.10.197.10 | Deploy if needed |

**Steps:**
1. Backup all MySQL databases
2. Update LXC container network config to use reserved IP
3. Restart containers
4. Update application connection strings
5. Verify replication (if configured)

**Connection String Update:**
```yaml
# Before
spring.datasource.url=jdbc:mysql://10.10.199.246:3306/smsdb

# After
spring.datasource.url=jdbc:mysql://10.10.199.10:3306/smsdb
```

---

### Priority 2: Kafka & ZooKeeper Cluster

**Kafka Cluster Configuration**

| Container | Current IP | Target IP | Action |
|----------|-----------|-----------|---------|
| kafka-broker-1 @ Server 1 | Stopped | 10.10.199.20 | Reconfigure & start |
| kafka-broker-2 @ Server 2 | Stopped | 10.10.198.20 | Reconfigure & start |
| kafka-broker-3 @ Server 3 | Stopped | 10.10.197.20 | Reconfigure & start |

**ZooKeeper Ensemble Configuration**

| Container | Current IP | Target IP | Action |
|----------|-----------|-----------|---------|
| zookeeper-single @ Server 1 | 10.10.199.190 | 10.10.199.25 | Reconfigure IP |
| zk-2 @ Server 2 | 10.10.199.95 | 10.10.198.25 | Recreate (wrong subnet) |
| zk-3 @ Server 3 | 10.10.199.80 | 10.10.197.25 | Recreate (wrong subnet) |

**ZooKeeper Ensemble Connection String:**
```
# After migration
zookeeper.connect=10.10.199.25:2181,10.10.198.25:2181,10.10.197.25:2181
```

**Kafka Broker Configuration:**
```properties
# broker.properties for Broker 1 (Server 1)
broker.id=1
listeners=PLAINTEXT://10.10.199.20:9092
advertised.listeners=PLAINTEXT://10.10.199.20:9092
zookeeper.connect=10.10.199.25:2181,10.10.198.25:2181,10.10.197.25:2181

# broker.properties for Broker 2 (Server 2)
broker.id=2
listeners=PLAINTEXT://10.10.198.20:9092
advertised.listeners=PLAINTEXT://10.10.198.20:9092
zookeeper.connect=10.10.199.25:2181,10.10.198.25:2181,10.10.197.25:2181

# broker.properties for Broker 3 (Server 3)
broker.id=3
listeners=PLAINTEXT://10.10.197.20:9092
advertised.listeners=PLAINTEXT://10.10.197.20:9092
zookeeper.connect=10.10.199.25:2181,10.10.198.25:2181,10.10.197.25:2181
```

---

### Priority 3: FRR Router Containers

**FRR Router Configuration**

| Container | Current IP | Target IP | Action |
|----------|-----------|-----------|---------|
| frr-router-node1 @ Server 1 | 10.10.199.50 | 10.10.199.50 | ✅ Already correct |
| frr-router-node2 @ Server 2 | 10.10.199.55 | 10.10.198.50 | Recreate (wrong subnet) |
| frr-router-node3 @ Server 3 | 10.10.199.65 | 10.10.197.50 | Recreate (wrong subnet) |

**Note:** FRR routers on Server 2 and 3 are in wrong subnets. They should be recreated in their correct subnets.

---

## Configuration Examples

### MySQL Cluster Setup

**Server 1 - Master Database:**
```ini
# /etc/mysql/my.cnf
[mysqld]
server-id=1
bind-address=10.10.199.10
report-host=10.10.199.10
log-bin=mysql-bin
binlog-do-db=smsdb
```

**Server 2 - Replica Database:**
```ini
# /etc/mysql/my.cnf
[mysqld]
server-id=2
bind-address=10.10.198.10
report-host=10.10.198.10
relay-log=mysql-relay-bin
read-only=1
```

**Replication Setup:**
```sql
-- On Server 1 (Master)
CREATE USER 'repl'@'10.10.198.%' IDENTIFIED BY 'password';
GRANT REPLICATION SLAVE ON *.* TO 'repl'@'10.10.198.%';
FLUSH PRIVILEGES;

-- On Server 2 (Replica)
CHANGE MASTER TO
    MASTER_HOST='10.10.199.10',
    MASTER_USER='repl',
    MASTER_PASSWORD='password',
    MASTER_LOG_FILE='mysql-bin.000001',
    MASTER_LOG_POS=154;
START SLAVE;
```

---

### ZooKeeper Ensemble Configuration

**Server 1 - zk-1 (10.10.199.25):**
```properties
# /etc/zookeeper/zoo.cfg
tickTime=2000
dataDir=/var/lib/zookeeper
clientPort=2181
initLimit=5
syncLimit=2
server.1=10.10.199.25:2888:3888
server.2=10.10.198.25:2888:3888
server.3=10.10.197.25:2888:3888

# /var/lib/zookeeper/myid
1
```

**Server 2 - zk-2 (10.10.198.25):**
```properties
# /etc/zookeeper/zoo.cfg
tickTime=2000
dataDir=/var/lib/zookeeper
clientPort=2181
initLimit=5
syncLimit=2
server.1=10.10.199.25:2888:3888
server.2=10.10.198.25:2888:3888
server.3=10.10.197.25:2888:3888

# /var/lib/zookeeper/myid
2
```

**Server 3 - zk-3 (10.10.197.25):**
```properties
# /etc/zookeeper/zoo.cfg
tickTime=2000
dataDir=/var/lib/zookeeper
clientPort=2181
initLimit=5
syncLimit=2
server.1=10.10.199.25:2888:3888
server.2=10.10.198.25:2888:3888
server.3=10.10.197.25:2888:3888

# /var/lib/zookeeper/myid
3
```

---

### SMS Application Configuration

**Spring Boot application.yml:**
```yaml
spring:
  application:
    name: sms-application

  datasource:
    # MySQL cluster - use master for writes
    master:
      url: jdbc:mysql://10.10.199.10:3306/smsdb
      username: sms_user
      password: ${DB_PASSWORD}

    # MySQL replica - use for reads
    slave:
      url: jdbc:mysql://10.10.198.10:3306/smsdb
      username: sms_user
      password: ${DB_PASSWORD}

  kafka:
    bootstrap-servers:
      - 10.10.199.20:9092
      - 10.10.198.20:9092
      - 10.10.197.20:9092
    producer:
      key-serializer: org.apache.kafka.common.serialization.StringSerializer
      value-serializer: org.apache.kafka.common.serialization.StringSerializer
    consumer:
      group-id: sms-app-group
      key-deserializer: org.apache.kafka.common.serialization.StringDeserializer
      value-deserializer: org.apache.kafka.common.serialization.StringDeserializer

# Admin panel URL
sms:
  admin:
    url: http://10.10.199.37:8080
```

---

## Migration Commands

### Reconfigure LXC Container IP

**Example: Migrate MySQL from .246 to .10 on Server 1**

```bash
# Stop the container
sudo lxc stop MySQL

# Edit the container config
sudo lxc config device set MySQL eth0 ipv4.address 10.10.199.10

# Start the container
sudo lxc start MySQL

# Verify IP
sudo lxc exec MySQL -- ip addr show eth0
```

### Create New Container with Reserved IP

**Example: Deploy ZooKeeper Node 2 on Server 2**

```bash
# Launch container with reserved IP
sudo lxc launch ubuntu:22.04 zk-2 -c ipv4.address=10.10.198.25

# Or launch first, then configure
sudo lxc launch ubuntu:22.04 zk-2
sudo lxc stop zk-2
sudo lxc config device set zk-2 eth0 ipv4.address 10.10.198.25
sudo lxc start zk-2
```

---

## Quick Reference

### Reserved IPs Summary

| Service | Server 1 | Server 2 | Server 3 |
|---------|----------|----------|----------|
| MySQL | 10.10.199.10 | 10.10.198.10 | 10.10.197.10 |
| Kafka | 10.10.199.20 | 10.10.198.20 | 10.10.197.20 |
| ZooKeeper | 10.10.199.25 | 10.10.198.25 | 10.10.197.25 |
| FRR Router | 10.10.199.50 | 10.10.198.50 | 10.10.197.50 |

### Connection Strings

```properties
# MySQL Cluster
mysql.master=10.10.199.10:3306
mysql.slave=10.10.198.10:3306

# Kafka Cluster
kafka.brokers=10.10.199.20:9092,10.10.198.20:9092,10.10.197.20:9092

# ZooKeeper Ensemble
zookeeper.servers=10.10.199.25:2181,10.10.198.25:2181,10.10.197.25:2181
```

---

## Verification Checklist

After migration, verify:

- [ ] **MySQL Cluster**
  - [ ] Master accessible at 10.10.199.10
  - [ ] Slave accessible at 10.10.198.10
  - [ ] Replication status: `SHOW SLAVE STATUS\G`
  - [ ] Applications can connect to master
  - [ ] Read queries work on slave

- [ ] **Kafka Cluster**
  - [ ] All 3 brokers online: `kafka-broker-controller-metrics`
  - [ ] Topic creation works
  - [ ] Producer can send messages
  - [ ] Consumer can receive messages
  - [ ] Cross-broker replication working

- [ ] **ZooKeeper Ensemble**
  - [ ] All 3 nodes in ensemble: `echo stat | nc 10.10.199.25 2181`
  - [ ] Leader elected
  - [ ] Followers synced
  - [ ] Kafka connected to ZK

- [ ] **FRR Routers**
  - [ ] BGP sessions established
  - [ ] Routes advertised correctly
  - [ ] Cross-host container connectivity working

- [ ] **Application Services**
  - [ ] SMS Admin accessible at 10.10.199.37
  - [ ] Connection strings updated
  - [ ] All services operational

---

## Summary

**Current Status:**
- 🟢 Network infrastructure: Configured and operational
- 🟡 IP allocations: Partially standardized
- 🔴 Container IPs: Migration needed

**Required Actions:**
1. Migrate MySQL containers to .10 addresses
2. Reconfigure/recreate ZooKeeper containers in correct subnets
3. Reconfigure/recreate Kafka brokers with reserved IPs
4. Fix FRR router containers on Server 2 & 3
5. Update all application connection strings

**Benefits After Migration:**
- ✅ Consistent IP addressing across all deployments
- ✅ Simplified troubleshooting (know services by IP)
- ✅ Predictable cluster formation
- ✅ Configuration portability
- ✅ Self-documenting infrastructure

---

**Reference:** See `/home/mustafa/telcobright-projects/orchestrix/images/networking_guideline_claude.md` for complete networking guidelines.

**Last Updated:** 2025-11-04
