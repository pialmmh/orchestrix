# Infinite Scheduler - BDCOM SMS Application VM Deployment

## Deployment Information

- **Deployment Name**: bdcom-sms
- **Server**: 10.255.246.173
- **User**: bdcom
- **Password**: M6nthDNrxcYfPQLu
- **OS**: Ubuntu 22.04.5 LTS (64-bit Server)
- **Environment**: Production
- **Application**: infinite-scheduler v1.0.0
- **Deployment Type**: Direct JAR (non-LXC)

## Server Specifications

- **VCPU**: 8 cores
- **RAM**: 16 GB
- **Storage**: 500 GB SSD

## Infrastructure

- **MySQL**: Running on 127.0.0.1:3306
  - User: root
  - Password: Takay1takaane
  - Database: btcl_sms

- **Kafka**: Single-node on same VM
  - Bootstrap servers: localhost:9092

## Prerequisites

### Network Connectivity

The BDCOM VM (10.255.246.173) must be accessible from your deployment machine. You may need:
- VPN connection to the BDCOM network
- Proper firewall rules allowing SSH (port 22)
- Network routing configured

### On Target Server (10.255.246.173)

1. **Java 21** must be installed
   ```bash
   java -version
   # Should show: openjdk version "21" or similar
   ```

2. **MySQL** must be running
   - Already configured at 127.0.0.1:3306
   - Database: btcl_sms
   - Credentials: root / Takay1takaane

3. **Kafka** must be running
   - Single-node cluster
   - Accessible at localhost:9092

### On Deployment Machine (Local)

1. **sshpass** for password-based SSH authentication
   ```bash
   sudo apt-get install sshpass
   ```

2. **Network access** to 10.255.246.173

## Deployment Steps

### 1. Ensure Network Connectivity

Before deploying, test the connection:

```bash
ssh bdcom@10.255.246.173
# Enter password: M6nthDNrxcYfPQLu
```

If connection fails, check:
- VPN connection status
- Firewall rules
- Network routing

### 2. Review Configuration

Configuration files in this directory:

**quartz.properties**:
- Database: btcl_sms@127.0.0.1:3306
- User: root / Takay1takaane
- Thread pool: 25 threads
- Max connections: 25

**kafka.properties**:
- Bootstrap servers: localhost:9092
- Topics:
  - btcl.sms.scheduled
  - btcl.sms.sent
  - btcl.sms.failed

**deployment.yaml**:
- JVM heap: 512m - 2048m (higher due to 16GB RAM)
- Application port: 7070
- Debug port: 5005 (disabled)

### 3. Run Deployment Script

```bash
cd /home/mustafa/telcobright-projects/orchestrix/images/lxc/infinite-scheduler/infinite-scheduler-v1.0.0/deployments/bdcom-sms

./deploy-direct.sh
```

The script will:
1. Validate deployment setup
2. Test server connection
3. Check Java installation
4. Verify MySQL connection and database
5. Create Quartz tables if needed
6. Check Kafka availability
7. Create directory structure on server
8. Copy JAR file
9. Copy configuration files
10. Create startup script
11. Create systemd service (runs as 'bdcom' user)
12. Enable and start the service
13. Verify service status

## Server Directory Structure

```
/opt/infinite-scheduler/
├── app/
│   └── infinite-scheduler-1.0.0.jar
├── config/
│   ├── quartz.properties
│   └── kafka.properties
├── logs/
│   ├── infinite-scheduler.log      # Application logs
│   ├── stdout.log                  # Service stdout
│   └── stderr.log                  # Service stderr
└── bin/
    └── start.sh                    # Startup script
```

## Service Management

All service commands must be run with `sudo`:

### Start Service
```bash
ssh bdcom@10.255.246.173
sudo systemctl start infinite-scheduler
```

### Stop Service
```bash
ssh bdcom@10.255.246.173
sudo systemctl stop infinite-scheduler
```

### Restart Service
```bash
ssh bdcom@10.255.246.173
sudo systemctl restart infinite-scheduler
```

### Check Status
```bash
ssh bdcom@10.255.246.173
sudo systemctl status infinite-scheduler
```

### View Logs

**Application logs**:
```bash
ssh bdcom@10.255.246.173
tail -f /opt/infinite-scheduler/logs/stdout.log
```

**System logs**:
```bash
ssh bdcom@10.255.246.173
sudo journalctl -u infinite-scheduler -f
```

**Recent errors**:
```bash
ssh bdcom@10.255.246.173
sudo journalctl -u infinite-scheduler -n 100 --no-pager | grep ERROR
```

## Configuration

### JVM Settings

Current configuration (in `bin/start.sh`):
- Min Heap: 512m
- Max Heap: 2048m (2GB - suitable for 16GB RAM server)
- GC: G1GC
- Max GC Pause: 200ms

To modify, edit `/opt/infinite-scheduler/bin/start.sh` on the server and restart.

### Application Ports

- **Application Port**: 7070
- **Debug Port**: 5005 (disabled by default)

To enable debug mode, uncomment the debug line in `/opt/infinite-scheduler/bin/start.sh`.

### Database Configuration

Database settings in `/opt/infinite-scheduler/config/quartz.properties`:
- URL: jdbc:mysql://127.0.0.1:3306/btcl_sms
- User: root
- Password: Takay1takaane
- Max connections: 25

### Kafka Configuration

Kafka settings in `/opt/infinite-scheduler/config/kafka.properties`:
- Bootstrap servers: localhost:9092
- Consumer group: infinite-scheduler-bdcom
- Topics: btcl.sms.*

## Database Schema

The deployment script automatically:
1. Checks if `btcl_sms` database exists
2. Creates it if needed
3. Checks for Quartz tables (qrtz_*)
4. Applies schema if tables don't exist

Manual schema application (if needed):
```bash
ssh bdcom@10.255.246.173
mysql -h 127.0.0.1 -u root -pTakay1takaane btcl_sms < /path/to/quartz-mysql-schema.sql
```

## Troubleshooting

### Connection Issues

**Cannot SSH to server**:
1. Check VPN connection
2. Verify firewall rules
3. Test basic connectivity: `ping 10.255.246.173`

**SSH authentication fails**:
- Verify password: M6nthDNrxcYfPQLu
- Try manual SSH: `ssh bdcom@10.255.246.173`

### Service Won't Start

1. Check Java installation:
   ```bash
   ssh bdcom@10.255.246.173 'java -version'
   ```

2. Check logs:
   ```bash
   ssh bdcom@10.255.246.173 'sudo journalctl -u infinite-scheduler -n 50'
   ```

3. Verify database connectivity:
   ```bash
   ssh bdcom@10.255.246.173 'mysql -h 127.0.0.1 -u root -pTakay1takaane -e "SELECT 1"'
   ```

### Database Connection Errors

1. Ensure MySQL is running:
   ```bash
   ssh bdcom@10.255.246.173 'sudo systemctl status mysql'
   ```

2. Verify database exists:
   ```bash
   ssh bdcom@10.255.246.173 'mysql -h 127.0.0.1 -u root -pTakay1takaane -e "SHOW DATABASES"'
   ```

3. Check Quartz tables:
   ```bash
   ssh bdcom@10.255.246.173 'mysql -h 127.0.0.1 -u root -pTakay1takaane btcl_sms -e "SHOW TABLES LIKE \"qrtz_%\""'
   ```

### Kafka Connection Errors

1. Check if Kafka is running:
   ```bash
   ssh bdcom@10.255.246.173 'netstat -tulpn | grep 9092'
   ```

2. Test Kafka connectivity:
   ```bash
   ssh bdcom@10.255.246.173 'telnet localhost 9092'
   ```

3. Check Kafka logs (location varies by installation)

### Out of Memory Errors

1. Check heap dump: `/opt/infinite-scheduler/logs/heap_dump.hprof`
2. Increase heap size in `/opt/infinite-scheduler/bin/start.sh`:
   - Current max: 2048m
   - Can increase to 4096m or 8192m on this 16GB server
3. Restart service

### Port Already in Use

1. Check what's using port 7070:
   ```bash
   ssh bdcom@10.255.246.173 'sudo netstat -tulpn | grep 7070'
   ```

2. Either:
   - Stop the conflicting service
   - Change the port in `bin/start.sh` (modify `-Dapp.port=7070`)

## Updating the Application

To deploy a new version:

1. Stop the service:
   ```bash
   ssh bdcom@10.255.246.173 'sudo systemctl stop infinite-scheduler'
   ```

2. Backup the current JAR:
   ```bash
   ssh bdcom@10.255.246.173 'cp /opt/infinite-scheduler/app/infinite-scheduler-1.0.0.jar /opt/infinite-scheduler/app/infinite-scheduler-1.0.0.jar.bak'
   ```

3. Copy new JAR:
   ```bash
   sshpass -p 'M6nthDNrxcYfPQLu' scp /path/to/new.jar bdcom@10.255.246.173:/opt/infinite-scheduler/app/
   ```

4. Start the service:
   ```bash
   ssh bdcom@10.255.246.173 'sudo systemctl start infinite-scheduler'
   ```

## Security Considerations

- Service runs as 'bdcom' user (non-root)
- Database credentials stored in plain text in config files
- SSH password authentication used (consider SSH keys for production)
- Debug port disabled by default
- PrivateTmp and NoNewPrivileges enabled for systemd service

## Monitoring

### Health Checks

Monitor these aspects:

1. **Service Status**:
   ```bash
   ssh bdcom@10.255.246.173 'sudo systemctl is-active infinite-scheduler'
   ```

2. **Resource Usage**:
   ```bash
   ssh bdcom@10.255.246.173 'top -p $(pgrep -f infinite-scheduler)'
   ```

3. **Log for Errors**:
   ```bash
   ssh bdcom@10.255.246.173 'tail -100 /opt/infinite-scheduler/logs/stderr.log'
   ```

### Key Metrics

- Service uptime
- Memory usage (heap and non-heap)
- Database connection pool utilization
- Kafka consumer lag
- Job execution rate
- Failed job count

## Network Access Requirements

To successfully deploy, ensure:

1. **From deployment machine to BDCOM VM**:
   - SSH (port 22) access to 10.255.246.173

2. **On BDCOM VM**:
   - MySQL accessible at 127.0.0.1:3306
   - Kafka accessible at localhost:9092
   - Port 7070 available for application

## Next Steps After Deployment

1. Verify service is running: `sudo systemctl status infinite-scheduler`
2. Check logs for any errors: `tail -f /opt/infinite-scheduler/logs/stdout.log`
3. Monitor Kafka topics for scheduled jobs
4. Monitor database for Quartz job entries
5. Set up monitoring/alerting for the service

## Support

For issues or questions:
- Check application logs: `/opt/infinite-scheduler/logs/`
- Check systemd logs: `sudo journalctl -u infinite-scheduler`
- Review Quartz documentation for job scheduling issues
- Review Kafka documentation for messaging issues
