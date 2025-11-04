# Debezium Connector JSON File - Complete Guide

## 📁 File Location & Bind Mount

### Bind Mount Configuration

```yaml
Type: Disk (bind mount)
Host Path:      /home/tbsms/debezium-config/
Container Path: /etc/debezium/connectors/
Access: Read/Write (changes reflect immediately)
```

### How It Works

```
┌─────────────────────────────────────────────────────────────┐
│  Host Machine (123.200.0.51)                                │
│                                                              │
│  /home/tbsms/debezium-config/                               │
│  ├── mysql-connector.json          ← EDIT THIS FILE        │
│  ├── example-include-databases.json                         │
│  ├── example-include-tables.json                            │
│  ├── example-exclude-tables.json                            │
│  ├── README.md                                              │
│  ├── QUICK_START.txt                                        │
│  ├── show-config.sh                                         │
│  └── update-connector.sh                                    │
│                                                              │
│           ↕ BIND MOUNT (real-time sync)                     │
│                                                              │
│  ┌────────────────────────────────────────────────────────┐ │
│  │ Debezium Container                                     │ │
│  │                                                        │ │
│  │ /etc/debezium/connectors/                             │ │
│  │ ├── mysql-connector.json     ← SAME FILE (mounted)   │ │
│  │ ├── example-include-databases.json                    │ │
│  │ └── ... (all files from host)                         │ │
│  └────────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────┘
```

**Key Point:** When you edit `/home/tbsms/debezium-config/mysql-connector.json` on the host, the changes are **immediately visible** inside the container at `/etc/debezium/connectors/mysql-connector.json`.

---

## 📄 Current Active Configuration

### File: `/home/tbsms/debezium-config/mysql-connector.json`

```json
{
  "name": "mysql-all-databases-connector",
  "config": {
    "connector.class": "io.debezium.connector.mysql.MySqlConnector",
    "database.hostname": "123.200.0.51",
    "database.port": "3306",
    "database.user": "root",
    "database.password": "Takay1#$ane",
    "database.server.id": "1",
    "topic.prefix": "dbserver1",
    "schema.history.internal.kafka.bootstrap.servers": "10.194.110.52:9092",
    "schema.history.internal.kafka.topic": "dbhistory.all-databases",
    "database.exclude.list": "mysql,sys,information_schema,performance_schema",
    "snapshot.mode": "schema_only",
    "tasks.max": "1",
    "transforms": "route",
    "transforms.route.type": "org.apache.kafka.connect.transforms.RegexRouter",
    "transforms.route.regex": ".*",
    "transforms.route.replacement": "all-db-changes"
  }
}
```

---

## 🔧 Configuration Fields Explained

### Basic Connector Settings

| Field | Value | Description |
|-------|-------|-------------|
| `name` | `mysql-all-databases-connector` | Unique connector name |
| `connector.class` | `io.debezium.connector.mysql.MySqlConnector` | MySQL connector class |

### MySQL Connection Settings

| Field | Value | Description |
|-------|-------|-------------|
| `database.hostname` | `123.200.0.51` | MySQL server IP/hostname |
| `database.port` | `3306` | MySQL port |
| `database.user` | `root` | MySQL username |
| `database.password` | `Takay1#$ane` | MySQL password |
| `database.server.id` | `1` | Unique server ID (required for binlog) |

### Topic Configuration

| Field | Value | Description |
|-------|-------|-------------|
| `topic.prefix` | `dbserver1` | Prefix for topic names (before transform) |
| `transforms.route.replacement` | `all-db-changes` | **ACTUAL TOPIC NAME** (after transform) |

### Database/Table Filtering

| Field | Value | Description |
|-------|-------|-------------|
| `database.exclude.list` | `mysql,sys,...` | Databases to EXCLUDE (system DBs) |
| `database.include.list` | _(empty)_ | Databases to INCLUDE (empty = all) |
| `table.include.list` | _(empty)_ | Tables to INCLUDE (empty = all) |
| `table.exclude.list` | _(empty)_ | Tables to EXCLUDE |

### Schema History

| Field | Value | Description |
|-------|-------|-------------|
| `schema.history.internal.kafka.bootstrap.servers` | `10.194.110.52:9092` | Kafka for storing schema history |
| `schema.history.internal.kafka.topic` | `dbhistory.all-databases` | Topic for schema history |

### Snapshot Configuration

| Field | Value | Options |
|-------|-------|---------|
| `snapshot.mode` | `schema_only` | `initial` = capture existing data<br>`schema_only` = only new changes<br>`never` = skip snapshot completely |

### Transform Configuration (Topic Routing)

| Field | Value | Description |
|-------|-------|-------------|
| `transforms` | `route` | Transform name |
| `transforms.route.type` | `RegexRouter` | Use regex to route topics |
| `transforms.route.regex` | `.*` | Match all topics |
| `transforms.route.replacement` | `all-db-changes` | Send everything to this topic |

---

## 📝 How to Edit the Configuration

### Method 1: Edit on Host Machine (Recommended)

```bash
# SSH to the server
ssh -p 8210 tbsms@123.200.0.51

# Edit the file
nano /home/tbsms/debezium-config/mysql-connector.json

# Or use vim
vim /home/tbsms/debezium-config/mysql-connector.json
```

### Method 2: Edit from Container

```bash
# SSH to the server
ssh -p 8210 tbsms@123.200.0.51

# Edit inside container (changes reflect on host too!)
lxc exec Debezium -- nano /etc/debezium/connectors/mysql-connector.json
```

**Important:** Both methods edit the **same file** due to bind mount!

---

## 🔄 How to Apply Changes

After editing the JSON file, you need to **re-register** the connector:

```bash
# Step 1: SSH to server
ssh -p 8210 tbsms@123.200.0.51

# Step 2: Delete old connector
lxc exec Debezium -- curl -X DELETE \
  http://localhost:8083/connectors/mysql-all-databases-connector

# Step 3: Register with new configuration
lxc exec Debezium -- curl -X POST \
  -H "Content-Type: application/json" \
  --data @/etc/debezium/connectors/mysql-connector.json \
  http://localhost:8083/connectors

# Step 4: Verify connector is running
lxc exec Debezium -- curl -s \
  http://localhost:8083/connectors/mysql-all-databases-connector/status | jq
```

### Quick Update Script

There's a helper script available:

```bash
ssh -p 8210 tbsms@123.200.0.51
lxc exec Debezium -- /etc/debezium/connectors/update-connector.sh
```

---

## 📚 Configuration Examples

### Example 1: Monitor Specific Databases Only

**File:** `example-include-databases.json`

```json
{
  "name": "mysql-connector-1",
  "config": {
    "connector.class": "io.debezium.connector.mysql.MySqlConnector",
    "database.hostname": "123.200.0.51",
    "database.port": "3306",
    "database.user": "root",
    "database.password": "Takay1#$ane",
    "database.server.id": "1",
    "topic.prefix": "dbserver1",

    "database.include.list": "link3_sms,l3ventures_aggregator",

    "snapshot.mode": "schema_only",
    "schema.history.internal.kafka.bootstrap.servers": "10.194.110.52:9092",
    "schema.history.internal.kafka.topic": "dbhistory.mysql-connector"
  }
}
```

**Result:** Only captures changes from `link3_sms` and `l3ventures_aggregator` databases.

---

### Example 2: Monitor Specific Tables Only

**File:** `example-include-tables.json`

```json
{
  "name": "mysql-connector-1",
  "config": {
    "connector.class": "io.debezium.connector.mysql.MySqlConnector",
    "database.hostname": "123.200.0.51",
    "database.port": "3306",
    "database.user": "root",
    "database.password": "Takay1#$ane",
    "database.server.id": "1",
    "topic.prefix": "dbserver1",

    "table.include.list": "link3_sms.audit_log,link3_sms.users,l3ventures_aggregator.orders",

    "snapshot.mode": "schema_only",
    "schema.history.internal.kafka.bootstrap.servers": "10.194.110.52:9092",
    "schema.history.internal.kafka.topic": "dbhistory.mysql-connector"
  }
}
```

**Result:** Only captures changes from these 3 specific tables.

---

### Example 3: Exclude Specific Tables

**File:** `example-exclude-tables.json`

```json
{
  "name": "mysql-connector-1",
  "config": {
    "connector.class": "io.debezium.connector.mysql.MySqlConnector",
    "database.hostname": "123.200.0.51",
    "database.port": "3306",
    "database.user": "root",
    "database.password": "Takay1#$ane",
    "database.server.id": "1",
    "topic.prefix": "dbserver1",

    "database.exclude.list": "mysql,sys,information_schema,performance_schema",
    "table.exclude.list": "link3_sms.temp_logs,link3_sms.cache_table",

    "snapshot.mode": "schema_only",
    "schema.history.internal.kafka.bootstrap.servers": "10.194.110.52:9092",
    "schema.history.internal.kafka.topic": "dbhistory.mysql-connector"
  }
}
```

**Result:** Monitors all databases except system DBs and excludes specific temp tables.

---

### Example 4: Separate Topic Per Database

```json
{
  "name": "mysql-connector-1",
  "config": {
    "connector.class": "io.debezium.connector.mysql.MySqlConnector",
    "database.hostname": "123.200.0.51",
    "database.port": "3306",
    "database.user": "root",
    "database.password": "Takay1#$ane",
    "database.server.id": "1",

    "topic.prefix": "dbserver1",
    "database.exclude.list": "mysql,sys,information_schema,performance_schema",
    "snapshot.mode": "schema_only",

    "schema.history.internal.kafka.bootstrap.servers": "10.194.110.52:9092",
    "schema.history.internal.kafka.topic": "dbhistory.mysql-connector"
  }
}
```

**Result:** Each table gets its own topic:
- `dbserver1.link3_sms.audit_log`
- `dbserver1.link3_sms.users`
- `dbserver1.l3ventures_aggregator.orders`

**Note:** Remove the `transforms` section to get separate topics!

---

### Example 5: Separate Topic Per Table

```json
{
  "name": "mysql-connector-1",
  "config": {
    "connector.class": "io.debezium.connector.mysql.MySqlConnector",
    "database.hostname": "123.200.0.51",
    "database.port": "3306",
    "database.user": "root",
    "database.password": "Takay1#$ane",
    "database.server.id": "1",

    "topic.prefix": "mysql",
    "database.exclude.list": "mysql,sys,information_schema,performance_schema",
    "snapshot.mode": "schema_only",

    "transforms": "route",
    "transforms.route.type": "org.apache.kafka.connect.transforms.RegexRouter",
    "transforms.route.regex": "mysql\\.(.*)\\..*",
    "transforms.route.replacement": "$1",

    "schema.history.internal.kafka.bootstrap.servers": "10.194.110.52:9092",
    "schema.history.internal.kafka.topic": "dbhistory.mysql-connector"
  }
}
```

**Result:** One topic per database:
- `link3_sms` (all tables from link3_sms)
- `l3ventures_aggregator` (all tables from l3ventures_aggregator)

---

## 🎯 Common Configuration Scenarios

### Scenario 1: "I want to capture ALL databases, ALL tables in ONE topic"
**Current Configuration** ✅ (already set up!)

```json
{
  "database.exclude.list": "mysql,sys,information_schema,performance_schema",
  "transforms.route.replacement": "all-db-changes"
}
```

### Scenario 2: "I want to capture only link3_sms database"

**Edit:** `/home/tbsms/debezium-config/mysql-connector.json`

```json
{
  "config": {
    ...
    "database.include.list": "link3_sms",
    "database.exclude.list": "",  // Remove this line
    ...
  }
}
```

### Scenario 3: "I want to exclude audit_log table"

```json
{
  "config": {
    ...
    "table.exclude.list": "link3_sms.audit_log",
    ...
  }
}
```

### Scenario 4: "I want each database in a separate topic"

```json
{
  "config": {
    ...
    // REMOVE the entire "transforms" section:
    // "transforms": "route",
    // "transforms.route.type": ...
    // "transforms.route.regex": ...
    // "transforms.route.replacement": ...
    ...
  }
}
```

**Result:** Topics will be:
- `dbserver1.link3_sms.audit_log`
- `dbserver1.link3_sms.users`
- etc.

### Scenario 5: "I want to capture existing data too"

```json
{
  "config": {
    ...
    "snapshot.mode": "initial",  // Change from "schema_only"
    ...
  }
}
```

**Warning:** This will read ALL existing data from ALL tables and send to Kafka!

---

## 🔐 Security Best Practices

### 1. Protect the Configuration File

```bash
# SSH to server
ssh -p 8210 tbsms@123.200.0.51

# Set proper permissions (only tbsms user can read)
chmod 600 /home/tbsms/debezium-config/mysql-connector.json

# Verify
ls -l /home/tbsms/debezium-config/mysql-connector.json
# Should show: -rw------- 1 tbsms tbsms
```

### 2. Use Dedicated MySQL User (Recommended)

Instead of using `root`, create a dedicated user:

```sql
-- On MySQL server
CREATE USER 'debezium'@'10.194.110.183' IDENTIFIED BY 'secure_password_here';
GRANT SELECT, RELOAD, SHOW DATABASES, REPLICATION SLAVE, REPLICATION CLIENT
  ON *.* TO 'debezium'@'10.194.110.183';
FLUSH PRIVILEGES;
```

Then update connector config:

```json
{
  "config": {
    "database.user": "debezium",
    "database.password": "secure_password_here",
    ...
  }
}
```

### 3. Backup Configuration

```bash
# SSH to server
ssh -p 8210 tbsms@123.200.0.51

# Create backup
cp /home/tbsms/debezium-config/mysql-connector.json \
   /home/tbsms/debezium-config/mysql-connector.json.backup-$(date +%Y%m%d)
```

---

## 🛠️ Helper Scripts Available

### 1. `show-config.sh` - View Current Connector Config

```bash
ssh -p 8210 tbsms@123.200.0.51
lxc exec Debezium -- /etc/debezium/connectors/show-config.sh
```

Shows the currently running connector configuration (from Kafka Connect API).

### 2. `update-connector.sh` - Update Connector

```bash
ssh -p 8210 tbsms@123.200.0.51
lxc exec Debezium -- /etc/debezium/connectors/update-connector.sh
```

Automatically:
1. Deletes old connector
2. Registers new connector from JSON file
3. Shows status

---

## 📊 Connector Status & Monitoring

### Check Connector Status

```bash
ssh -p 8210 tbsms@123.200.0.51

# Check if connector exists
lxc exec Debezium -- curl -s http://localhost:8083/connectors

# Check connector status
lxc exec Debezium -- curl -s \
  http://localhost:8083/connectors/mysql-all-databases-connector/status | jq

# Get current configuration
lxc exec Debezium -- curl -s \
  http://localhost:8083/connectors/mysql-all-databases-connector | jq
```

### Expected Output (Healthy)

```json
{
  "name": "mysql-all-databases-connector",
  "connector": {
    "state": "RUNNING",
    "worker_id": "127.0.1.1:8083"
  },
  "tasks": [
    {
      "id": 0,
      "state": "RUNNING",
      "worker_id": "127.0.1.1:8083"
    }
  ],
  "type": "source"
}
```

---

## ⚠️ Important Notes

### 1. File Sync is Immediate

When you edit `/home/tbsms/debezium-config/mysql-connector.json` on the host:
- Changes are **instantly visible** inside container
- But connector **doesn't auto-reload**
- You **must re-register** the connector to apply changes

### 2. Connector Name Must Match

When re-registering, the `"name"` field in JSON must match the connector name you're deleting:

```bash
# If connector name in JSON is "mysql-all-databases-connector"
curl -X DELETE http://localhost:8083/connectors/mysql-all-databases-connector
```

### 3. Database Permissions Required

MySQL user must have these permissions:

```sql
GRANT SELECT, RELOAD, SHOW DATABASES, REPLICATION SLAVE, REPLICATION CLIENT
  ON *.* TO 'your_user'@'10.194.110.183';
```

### 4. Binlog Must Be Enabled

Check MySQL binlog:

```sql
SHOW VARIABLES LIKE 'log_bin%';
-- log_bin should be ON

SHOW VARIABLES LIKE 'binlog_format';
-- binlog_format should be ROW
```

---

## 🔍 Troubleshooting

### Problem: "Connector registration fails"

**Check 1:** Validate JSON syntax

```bash
# On host
cat /home/tbsms/debezium-config/mysql-connector.json | jq .

# If error, JSON is invalid
```

**Check 2:** Check MySQL connectivity

```bash
lxc exec Debezium -- apt-get update && apt-get install -y mysql-client
lxc exec Debezium -- mysql -h 123.200.0.51 -u root -p
# Enter password: Takay1#$ane
```

### Problem: "Connector shows FAILED state"

```bash
# Get detailed error
lxc exec Debezium -- curl -s \
  http://localhost:8083/connectors/mysql-all-databases-connector/status | jq '.tasks[0]'

# Check Debezium logs
lxc exec Debezium -- journalctl -u debezium.service -n 100
```

### Problem: "Changes made but not applied"

**Solution:** You forgot to re-register the connector!

```bash
# Always re-register after editing JSON
lxc exec Debezium -- curl -X DELETE \
  http://localhost:8083/connectors/mysql-all-databases-connector

lxc exec Debezium -- curl -X POST \
  -H "Content-Type: application/json" \
  --data @/etc/debezium/connectors/mysql-connector.json \
  http://localhost:8083/connectors
```

---

## 📋 Quick Reference

### File Locations

| Location | Path |
|----------|------|
| **Host** | `/home/tbsms/debezium-config/mysql-connector.json` |
| **Container** | `/etc/debezium/connectors/mysql-connector.json` |
| **Same File?** | ✅ Yes (bind mounted) |

### Common Commands

```bash
# Edit config
nano /home/tbsms/debezium-config/mysql-connector.json

# Re-register connector
lxc exec Debezium -- /etc/debezium/connectors/update-connector.sh

# Check status
lxc exec Debezium -- curl -s http://localhost:8083/connectors/mysql-all-databases-connector/status | jq

# View logs
lxc exec Debezium -- journalctl -u debezium.service -f
```

### Important Fields Quick Reference

| What You Want | Field to Change | Example Value |
|---------------|----------------|---------------|
| Change topic name | `transforms.route.replacement` | `"my-topic-name"` |
| Monitor specific DBs | `database.include.list` | `"db1,db2,db3"` |
| Exclude specific tables | `table.exclude.list` | `"db.table1,db.table2"` |
| Capture existing data | `snapshot.mode` | `"initial"` |
| Only new changes | `snapshot.mode` | `"schema_only"` |
| Separate topic per table | Remove `transforms` section | _(remove entire section)_ |

---

**Remember:** Every time you edit the JSON file, you must **re-register** the connector for changes to take effect!
