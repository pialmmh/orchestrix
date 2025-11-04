# Debezium Remote Deployment Results

**Deployment Date:** 2025-10-21
**Remote Server:** 123.200.0.51:8210 (tbsms@123.200.0.51)
**Deployment Status:** PARTIAL SUCCESS - Manual MySQL configuration required

---

## Executive Summary

The Debezium container has been successfully deployed to the remote server and is fully operational. The Kafka Connect service is running and connected to the Kafka cluster. However, the MySQL connector cannot be registered yet because MySQL does not allow connections from the Debezium container's IP address (10.194.110.183). Manual MySQL access configuration is required to complete the setup.

---

## Deployment Details

### Container Information
- **Container Name:** debezium-mysql-1
- **Container IP:** 10.194.110.183
- **Status:** RUNNING
- **Memory:** 2GB
- **CPU:** 2 cores
- **Image:** debezium-base (Debezium 3.0.4.Final)
- **REST API:** http://10.194.110.183:8083

### Services Status
- **Kafka Connect:** ✅ ACTIVE and HEALTHY
- **REST API:** ✅ RESPONDING (version 3.9.0)
- **Kafka Connection:** ✅ CONNECTED to 10.194.110.52:9092
- **MySQL Connector:** ⚠️ NOT REGISTERED (access denied)

### Kafka Topics Created
The following Debezium infrastructure topics have been successfully created:
- `debezium-offsets` - Stores connector offset data
- `debezium-configs` - Stores connector configurations
- `debezium-status` - Stores connector status information

---

## Configuration Applied

### Kafka Configuration
```bash
KAFKA_BOOTSTRAP_SERVERS="10.194.110.52:9092"
GROUP_ID="debezium-cluster"
OFFSET_STORAGE_TOPIC="debezium-offsets"
CONFIG_STORAGE_TOPIC="debezium-configs"
STATUS_STORAGE_TOPIC="debezium-status"
CONNECT_REST_PORT="8083"
CONNECT_HEAP_OPTS="-Xms1G -Xmx1G"
```

### MySQL Configuration
```bash
DB_TYPE="mysql"
DB_HOST="123.200.0.51"
DB_PORT="3306"
DB_USER="root"
DB_PASSWORD="Takay1takaane$"
DB_NAME=""  # Empty = all databases
DB_SERVER_ID="1"
KAFKA_TOPIC_PREFIX="dbserver1"
CONNECTOR_NAME="mysql-connector-1"
SNAPSHOT_MODE="initial"
```

---

## Issue Encountered

### MySQL Access Denied

**Error Message:**
```
Access denied for user 'root'@'10.194.110.183' (using password: YES)
```

**Root Cause:**
MySQL 5.7.44 on the remote server is configured to only allow root connections from localhost. The Debezium container at 10.194.110.183 cannot connect.

**Impact:**
The MySQL connector cannot be registered, preventing CDC events from being captured.

---

## Manual Steps Required

### Step 1: Grant MySQL Access to Debezium Container

On the remote server (123.200.0.51), execute:

```bash
mysql -u root -p < ~/mysql-grant-debezium.sql
```

Or manually in MySQL:

```sql
CREATE USER IF NOT EXISTS 'root'@'10.194.110.183' IDENTIFIED BY 'Takay1takaane$';
GRANT ALL PRIVILEGES ON *.* TO 'root'@'10.194.110.183' WITH GRANT OPTION;
GRANT REPLICATION SLAVE, REPLICATION CLIENT ON *.* TO 'root'@'10.194.110.183';

-- Grant access to entire LXC subnet for flexibility
CREATE USER IF NOT EXISTS 'root'@'10.194.110.%' IDENTIFIED BY 'Takay1takaane$';
GRANT ALL PRIVILEGES ON *.* TO 'root'@'10.194.110.%' WITH GRANT OPTION;
GRANT REPLICATION SLAVE, REPLICATION CLIENT ON *.* TO 'root'@'10.194.110.%';

FLUSH PRIVILEGES;
```

### Step 2: Verify MySQL Binlog Configuration

Check MySQL CDC prerequisites:

```bash
./check-mysql-binlog.sh
```

Required settings:
- `log_bin` = ON
- `binlog_format` = ROW
- `binlog_row_image` = FULL
- `server_id` = 1 (non-zero)

If binlog is not enabled, edit `/etc/mysql/my.cnf`:

```ini
[mysqld]
server-id=1
log_bin=mysql-bin
binlog_format=ROW
binlog_row_image=FULL
expire_logs_days=10
```

Restart MySQL:
```bash
sudo systemctl restart mysql
```

### Step 3: Register MySQL Connector

After granting access:

```bash
./register-mysql-connector.sh
```

### Step 4: Verify CDC Functionality

Test with sample data:

```bash
# Create test database and table
mysql -u root -p -e "CREATE DATABASE testdb; USE testdb; CREATE TABLE users (id INT PRIMARY KEY, name VARCHAR(100)); INSERT INTO users VALUES (1, 'Test User');"

# Check Kafka topics
lxc exec kafka-broker-1 -- /opt/kafka/bin/kafka-topics.sh --list --bootstrap-server localhost:9092 | grep dbserver1

# Consume CDC events
lxc exec kafka-broker-1 -- /opt/kafka/bin/kafka-console-consumer.sh \
  --bootstrap-server localhost:9092 \
  --topic dbserver1.testdb.users \
  --from-beginning \
  --max-messages 1
```

---

## Files Created on Remote Server

| File | Location | Purpose |
|------|----------|---------|
| Launch script | `~/orchestrix/debezium/launch.sh` | Container launch script |
| Configuration | `~/orchestrix/debezium/mysql-connector.conf` | Container configuration |
| MySQL grants | `~/mysql-grant-debezium.sql` | SQL script to grant access |
| Connector registration | `~/register-mysql-connector.sh` | Script to register connector |
| MySQL check | `~/check-mysql-binlog.sh` | Check MySQL binlog config |
| Deployment summary | `~/DEBEZIUM_DEPLOYMENT_SUMMARY.md` | Complete deployment guide |

---

## Useful Commands

### Container Management
```bash
# Check container status
lxc list debezium-mysql-1

# View Debezium logs
lxc exec debezium-mysql-1 -- journalctl -u debezium.service -f

# Restart container
lxc restart debezium-mysql-1

# Stop container
lxc stop debezium-mysql-1
```

### Kafka Connect API
```bash
# Check API health
lxc exec debezium-mysql-1 -- curl http://localhost:8083/

# List connectors
lxc exec debezium-mysql-1 -- curl http://localhost:8083/connectors

# Check connector status
lxc exec debezium-mysql-1 -- curl http://localhost:8083/connectors/mysql-connector-1/status

# Delete connector
lxc exec debezium-mysql-1 -- curl -X DELETE http://localhost:8083/connectors/mysql-connector-1
```

### Kafka Topic Management
```bash
# List all topics
lxc exec kafka-broker-1 -- /opt/kafka/bin/kafka-topics.sh --list --bootstrap-server localhost:9092

# List Debezium topics only
lxc exec kafka-broker-1 -- /opt/kafka/bin/kafka-topics.sh --list --bootstrap-server localhost:9092 | grep -E '(debezium|dbserver)'

# Consume CDC events from a topic
lxc exec kafka-broker-1 -- /opt/kafka/bin/kafka-console-consumer.sh \
  --bootstrap-server localhost:9092 \
  --topic dbserver1.<database>.<table> \
  --from-beginning
```

---

## Expected Kafka Topics After Full Setup

Once the MySQL connector is successfully registered:

- `debezium-offsets` ✅ (already exists)
- `debezium-configs` ✅ (already exists)
- `debezium-status` ✅ (already exists)
- `dbhistory.mysql-connector` (will be created)
- `dbserver1.<database>.<table>` (one per table)

Example topics for database 'myapp':
- `dbserver1.myapp.users`
- `dbserver1.myapp.orders`
- `dbserver1.myapp.products`

---

## Architecture Overview

```
[MySQL Server]                [Debezium Container]           [Kafka Cluster]
123.200.0.51:3306    <---->   10.194.110.183:8083    <---->  10.194.110.52:9092

                              ┌─────────────────────┐
                              │  Kafka Connect      │
                              │  Debezium 3.0.4     │
                              ├─────────────────────┤
                              │  MySQL Connector    │
                              │  (to be registered) │
                              └─────────────────────┘

[Binlog Reader] ──> [Change Events] ──> [Kafka Topics]
```

---

## Troubleshooting

### Issue: MySQL Access Denied
**Solution:** Execute Step 1 to grant MySQL access

### Issue: Connector Registration Fails
**Check:**
1. MySQL grants are applied
2. MySQL is reachable from container: `lxc exec debezium-mysql-1 -- mysql -h 123.200.0.51 -u root -p`
3. Debezium logs: `lxc exec debezium-mysql-1 -- journalctl -u debezium.service -n 100`

### Issue: No CDC Events in Kafka
**Check:**
1. MySQL binlog is enabled: `./check-mysql-binlog.sh`
2. Connector status is RUNNING
3. Connector has no errors

### Issue: Connector Shows FAILED Status
**Check:**
1. Connector status for error details
2. MySQL connectivity from container
3. Debezium service logs

---

## Next Steps

1. ✅ Deploy Debezium container - COMPLETED
2. ⚠️ Grant MySQL access - **REQUIRED**
3. ⚠️ Verify MySQL binlog configuration - **REQUIRED**
4. ⚠️ Register MySQL connector - **PENDING**
5. ⚠️ Test CDC with sample data - **PENDING**
6. ⚠️ Monitor connector health - **PENDING**

---

## Remote Server Access

```bash
# SSH to remote server
ssh -p 8210 tbsms@123.200.0.51
Password: TB@l38800

# View deployment summary
cat ~/DEBEZIUM_DEPLOYMENT_SUMMARY.md

# Execute manual steps
mysql -u root -p < ~/mysql-grant-debezium.sql
./check-mysql-binlog.sh
./register-mysql-connector.sh
```

---

## Support Information

**Local Artifact Location:**
- `/home/mustafa/telcobright-projects/orchestrix/images/lxc/debezium/debezium-v.1/generated/artifact/debezium-v1-20251021_175841.tar.gz`

**Documentation:**
- `/home/mustafa/telcobright-projects/orchestrix/images/lxc/debezium/README.md`
- `/home/mustafa/telcobright-projects/orchestrix/images/lxc/debezium/DEPLOYMENT_GUIDE.md`

**Remote Summary:**
- `~/DEBEZIUM_DEPLOYMENT_SUMMARY.md` (on 123.200.0.51)

---

## Conclusion

The Debezium container deployment was successful. The container is running, Kafka Connect is healthy, and infrastructure topics are created. The final step requires MySQL access configuration on the remote server to enable CDC functionality. Once MySQL access is granted and the connector is registered, the system will be fully operational and ready to capture database changes.

**Deployment Agent:** Orchestrix Deployment Agent v2.0
**Timestamp:** 2025-10-21 13:15:00 UTC
