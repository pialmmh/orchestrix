# Debezium Deployment Verification Report

**Verification Date:** 2025-10-28
**Remote Server:** tbsms@123.200.0.51:8210
**Deployment Status:** ✅ FULLY OPERATIONAL

---

## Executive Summary

The Debezium CDC (Change Data Capture) system has been verified and is **fully operational** on the remote production server. The connector is actively capturing real-time database changes from MySQL and publishing them to Kafka topics. All components are healthy and functioning as expected.

---

## Deployment Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                    Remote Server: 123.200.0.51                  │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  ┌──────────────────┐         ┌──────────────────┐            │
│  │  MySQL Server    │         │  Kafka Broker    │            │
│  │  Port: 3306      │────────▶│  10.194.110.52   │            │
│  │  Database:       │         │  Port: 9092      │            │
│  │  link3_sms       │         └──────────────────┘            │
│  └──────────────────┘                   ▲                      │
│           │                              │                      │
│           │                              │                      │
│           ▼                              │                      │
│  ┌──────────────────────────────────────┴──────────────────┐  │
│  │  Debezium Container (debezium-mysql-1)                  │  │
│  │  IP: 10.194.110.183                                     │  │
│  │  ┌────────────────────────────────────────────────┐    │  │
│  │  │  Kafka Connect (Debezium 3.0.4.Final)          │    │  │
│  │  │  REST API: http://10.194.110.183:8083          │    │  │
│  │  │  ┌──────────────────────────────────────┐      │    │  │
│  │  │  │  MySQL Connector: mysql-connector-1  │      │    │  │
│  │  │  │  Status: RUNNING                     │      │    │  │
│  │  │  └──────────────────────────────────────┘      │    │  │
│  │  └────────────────────────────────────────────────┘    │  │
│  └──────────────────────────────────────────────────────────┘  │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

---

## Component Status

### 1. Debezium Container
- **Container Name:** debezium-mysql-1
- **Status:** ✅ RUNNING
- **IP Address:** 10.194.110.183
- **Uptime:** 6 days (since 2025-10-21 13:13:40 UTC)
- **Memory:** 1.2GB
- **Image:** debezium-base (Debezium 3.0.4.Final)

### 2. Kafka Connect Service
- **Service:** debezium.service
- **Status:** ✅ ACTIVE (running)
- **Process ID:** 572
- **REST API:** http://10.194.110.183:8083
- **Kafka Version:** 3.9.0
- **Cluster ID:** 5m_ILBNzRKmnJnwx0tFUag

### 3. MySQL Connector
- **Connector Name:** mysql-connector-1
- **Status:** ✅ RUNNING
- **Connector State:** RUNNING
- **Task 0 State:** RUNNING
- **Type:** source
- **Connector Class:** io.debezium.connector.mysql.MySqlConnector

### 4. MySQL Database
- **Host:** 123.200.0.51:3306
- **User:** debezium
- **Active Database:** link3_sms
- **Binlog File:** master-log-bin.000008
- **Server ID:** 1

### 5. Kafka Infrastructure
- **Bootstrap Server:** 10.194.110.52:9092
- **Infrastructure Topics:**
  - ✅ debezium-offsets (connector offsets)
  - ✅ debezium-configs (connector configs)
  - ✅ debezium-status (connector status)
  - ✅ dbhistory.mysql-connector (schema history)

- **CDC Data Topics:**
  - ✅ mysql-all-changes (all database changes)
  - ✅ all-mysql-changes (alias/transformed topic)

---

## Connector Configuration

```json
{
  "name": "mysql-connector-1",
  "config": {
    "connector.class": "io.debezium.connector.mysql.MySqlConnector",
    "database.hostname": "123.200.0.51",
    "database.port": "3306",
    "database.user": "debezium",
    "database.password": "********",
    "database.server.id": "1",
    "topic.prefix": "dbserver1",
    "snapshot.mode": "schema_only",
    "database.exclude.list": "mysql,sys,information_schema,performance_schema",
    "schema.history.internal.kafka.bootstrap.servers": "10.194.110.52:9092",
    "schema.history.internal.kafka.topic": "dbhistory.mysql-connector",
    "transforms": "route",
    "transforms.route.type": "org.apache.kafka.connect.transforms.RegexRouter",
    "transforms.route.regex": ".*",
    "transforms.route.replacement": "mysql-all-changes"
  }
}
```

### Key Configuration Details:
- **Snapshot Mode:** schema_only - Only captures schema, not existing data
- **Topic Routing:** All changes are routed to single topic: `mysql-all-changes`
- **Database Filtering:** Excludes system databases (mysql, sys, information_schema, performance_schema)
- **Server ID:** 1 (unique identifier for this connector)

---

## CDC Functionality Verification

### Sample CDC Event (JSON Format)

```json
{
  "before": null,
  "after": {
    "id": 2053497,
    "user_identifier": "azizur.jiko@chaldal.com",
    "action": "POST /FREESWITCHREST/user-live-calls-summary",
    "timestamp": 1761150079000,
    "response": "200 OK",
    "details": "IP Address: 103.185.176.230"
  },
  "source": {
    "version": "3.0.4.Final",
    "connector": "mysql",
    "name": "dbserver1",
    "ts_ms": 1761128478000,
    "snapshot": "false",
    "db": "link3_sms",
    "table": "audit_log",
    "server_id": 1,
    "file": "master-log-bin.000008",
    "pos": 8237867,
    "row": 0,
    "thread": 2440
  },
  "transaction": null,
  "op": "c",
  "ts_ms": 1761128478867
}
```

### CDC Event Analysis:
- **Database:** link3_sms
- **Table:** audit_log
- **Operation:** "c" (create/insert)
- **Binlog Position:** master-log-bin.000008, position 8237867
- **Data Captured:** Full row data in "after" field
- **Timestamp:** Event captured at 1761128478867 (UTC milliseconds)

### Live CDC Metrics:
- ✅ **Offset Commits:** Every 60 seconds
- ✅ **Messages Processed:** 20-27 messages per minute
- ✅ **Latency:** Real-time (sub-second capture)
- ✅ **Topic:** mysql-all-changes

---

## MySQL Binlog Configuration

The connector is successfully reading from MySQL binlog:
- **Binlog File:** master-log-bin.000008
- **Position Tracking:** ✅ Active
- **Format:** ROW (required for Debezium)
- **Snapshot:** false (capturing only new changes)

---

## Operational Commands

### Container Management

```bash
# SSH to remote server
ssh -p 8210 tbsms@123.200.0.51

# Check container status
lxc list debezium-mysql-1

# View Debezium logs (real-time)
lxc exec debezium-mysql-1 -- journalctl -u debezium.service -f

# View recent logs (last 100 lines)
lxc exec debezium-mysql-1 -- journalctl -u debezium.service -n 100

# Restart container
lxc restart debezium-mysql-1

# Stop container
lxc stop debezium-mysql-1

# Start container
lxc start debezium-mysql-1
```

### Kafka Connect API

```bash
# Check API health
lxc exec debezium-mysql-1 -- curl http://localhost:8083/

# List all connectors
lxc exec debezium-mysql-1 -- curl http://localhost:8083/connectors

# Get connector status
lxc exec debezium-mysql-1 -- curl http://localhost:8083/connectors/mysql-connector-1/status

# Get connector configuration
lxc exec debezium-mysql-1 -- curl http://localhost:8083/connectors/mysql-connector-1

# Delete connector (if needed)
lxc exec debezium-mysql-1 -- curl -X DELETE http://localhost:8083/connectors/mysql-connector-1

# Restart connector
lxc exec debezium-mysql-1 -- curl -X POST http://localhost:8083/connectors/mysql-connector-1/restart
```

### Kafka Topic Management

```bash
# List all topics
lxc exec kafka-broker-1 -- /opt/kafka/bin/kafka-topics.sh --list --bootstrap-server localhost:9092

# List Debezium topics only
lxc exec kafka-broker-1 -- /opt/kafka/bin/kafka-topics.sh --list --bootstrap-server localhost:9092 | grep -E '(debezium|mysql)'

# Consume CDC events from beginning
lxc exec kafka-broker-1 -- /opt/kafka/bin/kafka-console-consumer.sh \
  --bootstrap-server localhost:9092 \
  --topic mysql-all-changes \
  --from-beginning \
  --max-messages 10

# Consume latest CDC events (tail mode)
lxc exec kafka-broker-1 -- /opt/kafka/bin/kafka-console-consumer.sh \
  --bootstrap-server localhost:9092 \
  --topic mysql-all-changes

# Get topic details
lxc exec kafka-broker-1 -- /opt/kafka/bin/kafka-topics.sh \
  --describe \
  --topic mysql-all-changes \
  --bootstrap-server localhost:9092
```

### Database Monitoring

```bash
# Connect to MySQL from Debezium container
lxc exec debezium-mysql-1 -- bash -c "apt-get update && apt-get install -y mysql-client && mysql -h 123.200.0.51 -u debezium -p"

# Note: Password is configured in connector config
```

---

## Monitoring & Health Checks

### Daily Health Check Routine

1. **Container Status**
   ```bash
   lxc list debezium-mysql-1 -c ns4
   ```
   Expected: STATUS=RUNNING, IP=10.194.110.183

2. **Service Status**
   ```bash
   lxc exec debezium-mysql-1 -- systemctl status debezium.service
   ```
   Expected: Active: active (running)

3. **Connector Status**
   ```bash
   lxc exec debezium-mysql-1 -- curl -s http://localhost:8083/connectors/mysql-connector-1/status
   ```
   Expected: connector.state=RUNNING, tasks[0].state=RUNNING

4. **CDC Flow Test**
   ```bash
   lxc exec kafka-broker-1 -- /opt/kafka/bin/kafka-console-consumer.sh \
     --bootstrap-server localhost:9092 \
     --topic mysql-all-changes \
     --max-messages 1 \
     --timeout-ms 5000
   ```
   Expected: Should show recent CDC event within 5 seconds

### Key Metrics to Monitor

- **Offset Lag:** Check if connector is keeping up with binlog
- **Error Count:** Monitor connector logs for errors
- **Memory Usage:** Keep container memory under 2GB limit
- **Disk Space:** Monitor binlog position vs available storage
- **Network:** Ensure connectivity to MySQL and Kafka

---

## Troubleshooting Guide

### Issue: Connector Status Shows FAILED

**Symptoms:**
```bash
curl http://localhost:8083/connectors/mysql-connector-1/status
# Shows: "state": "FAILED"
```

**Resolution:**
1. Check connector logs:
   ```bash
   lxc exec debezium-mysql-1 -- journalctl -u debezium.service -n 200 | grep ERROR
   ```

2. Common causes:
   - MySQL connectivity issues
   - Binlog format not set to ROW
   - Database permissions issues
   - Kafka broker unavailable

3. Restart connector:
   ```bash
   lxc exec debezium-mysql-1 -- curl -X POST http://localhost:8083/connectors/mysql-connector-1/restart
   ```

### Issue: No CDC Events in Kafka

**Symptoms:**
- Connector shows RUNNING
- But no messages in kafka topics

**Resolution:**
1. Verify MySQL binlog is enabled and active
2. Check if data is actually changing in MySQL
3. Verify topic routing configuration
4. Check Kafka broker connectivity

### Issue: High Memory Usage

**Symptoms:**
```bash
lxc list debezium-mysql-1
# Shows memory near or above 2GB limit
```

**Resolution:**
1. Adjust heap settings in connector config
2. Restart container to free memory:
   ```bash
   lxc restart debezium-mysql-1
   ```

### Issue: Container Won't Start

**Resolution:**
1. Check container logs:
   ```bash
   lxc info debezium-mysql-1 --show-log
   ```

2. Check system resources on host
3. Verify network bridge configuration

---

## Performance Characteristics

### Observed Performance (Production)

- **Throughput:** 20-27 events/minute (current load on link3_sms.audit_log)
- **Latency:** Sub-second event capture and publishing
- **Memory:** 1.2GB stable (under 2GB limit)
- **CPU:** Minimal (background processing)
- **Network:** Low bandwidth usage
- **Uptime:** 6+ days continuous operation

### Capacity Estimates

Based on current configuration:
- **Max Throughput:** ~10,000 events/second (Debezium capability)
- **Scalability:** Can handle multiple tables across databases
- **Binlog Retention:** Monitor and adjust based on connector lag

---

## Security Considerations

### MySQL User Permissions

The `debezium` user has been granted:
- REPLICATION SLAVE (read binlog)
- REPLICATION CLIENT (access binlog metadata)
- SELECT on monitored databases
- Access from container IP: 10.194.110.183

### Network Security

- Container uses private network (10.194.110.0/24)
- MySQL access restricted to specific user
- Kafka Connect REST API only accessible within container network
- No external ports exposed

### Data Security

- Database password stored in connector configuration
- CDC events contain full row data - consider data sensitivity
- No encryption configured for Kafka topics (consider if handling sensitive data)

---

## Maintenance Recommendations

### Daily
- ✅ Monitor connector status (automated health check)
- ✅ Verify CDC events flowing to Kafka

### Weekly
- 📋 Review Debezium service logs for errors
- 📋 Check MySQL binlog file rotation
- 📋 Monitor Kafka topic disk usage

### Monthly
- 📋 Review connector configuration for optimization
- 📋 Check for Debezium version updates
- 📋 Audit CDC topic retention policies
- 📋 Verify backup and disaster recovery procedures

### As Needed
- 📋 Add new tables/databases to monitoring
- 📋 Adjust connector configuration for performance
- 📋 Scale Kafka brokers if needed

---

## Next Steps & Enhancements

### Optional Improvements

1. **Multi-Connector Setup**
   - Add connectors for other databases (PostgreSQL, MongoDB)
   - Configure database-specific topic routing

2. **Monitoring Dashboard**
   - Set up Grafana dashboard for Debezium metrics
   - Configure alerting for connector failures

3. **High Availability**
   - Deploy multiple Kafka Connect workers
   - Configure connector task distribution

4. **Data Processing Pipeline**
   - Add Kafka Streams for real-time processing
   - Integrate with data warehouse (Elasticsearch, etc.)

5. **Schema Registry**
   - Implement Confluent Schema Registry
   - Use Avro format for better schema management

---

## Deployment History

- **2025-10-21:** Initial deployment (partial - MySQL access issue)
- **2025-10-21:** MySQL access configured, connector registered successfully
- **2025-10-28:** Full verification - All systems operational

---

## Support & Documentation

### Local Documentation
- `/home/mustafa/telcobright-projects/orchestrix/images/lxc/debezium/README.md`
- `/home/mustafa/telcobright-projects/orchestrix/images/lxc/debezium/REMOTE_DEPLOYMENT_RESULTS.md`

### Remote Documentation
- `~/DEBEZIUM_DEPLOYMENT_SUMMARY.md` (on remote server)

### External Resources
- Debezium Documentation: https://debezium.io/documentation/
- Kafka Connect API: https://kafka.apache.org/documentation/#connect
- MySQL CDC Guide: https://debezium.io/documentation/reference/stable/connectors/mysql.html

---

## Contact & Escalation

For issues or questions:
1. Check this document first
2. Review Debezium service logs
3. Consult Debezium documentation
4. Contact platform team

---

**Report Generated:** 2025-10-28
**Verification Agent:** Orchestrix Deployment Agent v2.1
**Status:** ✅ PRODUCTION READY - FULLY OPERATIONAL
