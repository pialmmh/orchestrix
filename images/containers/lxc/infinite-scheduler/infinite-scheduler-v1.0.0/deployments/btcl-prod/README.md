# Infinite Scheduler - BTCL Production Deployment

## Deployment Information

- **Deployment Name**: btcl-prod
- **Server**: 114.130.145.71:50004
- **Environment**: Production
- **Application**: infinite-scheduler v1.0.0
- **Deployment Type**: Direct JAR (non-LXC)

## Prerequisites

### On Target Server (114.130.145.71)

1. **Java 21** must be installed
   ```bash
   java -version
   # Should show: openjdk version "21" or similar
   ```

2. **MySQL 8.0+** must be running and accessible
   - Host: 127.0.0.1:3306
   - Database: `scheduler` (must be created beforehand)
   - User: root
   - Password: 123456

3. **Quartz Tables** must be created in the scheduler database
   - SQL scripts should be in the application resources
   - Location: `src/main/resources/sql/`

### On Deployment Machine (Local)

1. **sshpass** for password-based SSH authentication
   ```bash
   sudo apt-get install sshpass
   ```

2. **SSH access** to the BTCL server
   - Port: 50004
   - User: Administrator
   - Password: (configured in deploy-direct.sh)

## Deployment Steps

### 1. Review Configuration

Before deploying, review and modify if needed:

**quartz.properties**:
- Database connection settings
- Thread pool configuration
- Clustering settings (if multi-node)

**deployment.yaml**:
- Server connection details
- JVM settings
- Port configurations

### 2. Run Deployment Script

```bash
cd /home/mustafa/telcobright-projects/orchestrix/images/lxc/infinite-scheduler/infinite-scheduler-v1.0.0/deployments/btcl-prod

./deploy-direct.sh
```

The script will:
1. Validate deployment setup
2. Test server connection
3. Check Java installation
4. Create directory structure on server
5. Copy JAR file
6. Copy configuration files
7. Create startup script
8. Create systemd service
9. Enable and start the service
10. Verify service status

### 3. Verify Deployment

After deployment, check the service status:

```bash
ssh -p 50004 Administrator@114.130.145.71 'systemctl status infinite-scheduler'
```

## Server Directory Structure

```
/opt/infinite-scheduler/
├── app/
│   └── infinite-scheduler-1.0.0.jar
├── config/
│   └── quartz.properties
├── logs/
│   ├── infinite-scheduler.log      # Application logs
│   ├── stdout.log                  # Service stdout
│   └── stderr.log                  # Service stderr
└── bin/
    └── start.sh                    # Startup script
```

## Service Management

### Start Service
```bash
ssh -p 50004 Administrator@114.130.145.71 'systemctl start infinite-scheduler'
```

### Stop Service
```bash
ssh -p 50004 Administrator@114.130.145.71 'systemctl stop infinite-scheduler'
```

### Restart Service
```bash
ssh -p 50004 Administrator@114.130.145.71 'systemctl restart infinite-scheduler'
```

### Check Status
```bash
ssh -p 50004 Administrator@114.130.145.71 'systemctl status infinite-scheduler'
```

### View Logs

**Application logs**:
```bash
ssh -p 50004 Administrator@114.130.145.71 'tail -f /opt/infinite-scheduler/logs/infinite-scheduler.log'
```

**System logs**:
```bash
ssh -p 50004 Administrator@114.130.145.71 'journalctl -u infinite-scheduler -f'
```

**Recent errors**:
```bash
ssh -p 50004 Administrator@114.130.145.71 'journalctl -u infinite-scheduler -n 100 --no-pager | grep ERROR'
```

## Configuration

### JVM Settings

Current configuration (in `bin/start.sh`):
- Min Heap: 512m
- Max Heap: 1024m
- GC: G1GC
- Max GC Pause: 200ms

To modify, edit `/opt/infinite-scheduler/bin/start.sh` on the server and restart the service.

### Application Ports

- **Application Port**: 7070
- **Debug Port**: 5005 (disabled by default)

To enable debug mode, uncomment the debug line in `/opt/infinite-scheduler/bin/start.sh`.

### Database Configuration

Database settings are in `/opt/infinite-scheduler/config/quartz.properties`:
- URL: jdbc:mysql://127.0.0.1:3306/scheduler
- User: root
- Password: 123456

## Troubleshooting

### Service won't start

1. Check Java installation:
   ```bash
   ssh -p 50004 Administrator@114.130.145.71 'java -version'
   ```

2. Check logs:
   ```bash
   ssh -p 50004 Administrator@114.130.145.71 'journalctl -u infinite-scheduler -n 50'
   ```

3. Verify database connectivity:
   ```bash
   ssh -p 50004 Administrator@114.130.145.71 'mysql -h 127.0.0.1 -P 3306 -u root -p123456 -e "SELECT 1"'
   ```

### Database connection errors

1. Ensure MySQL is running
2. Verify `scheduler` database exists
3. Check Quartz tables are created
4. Verify credentials in `quartz.properties`

### Out of memory errors

1. Check heap dump: `/opt/infinite-scheduler/logs/heap_dump.hprof`
2. Increase heap size in `/opt/infinite-scheduler/bin/start.sh`
3. Restart service

### Port already in use

1. Check what's using port 7070:
   ```bash
   ssh -p 50004 Administrator@114.130.145.71 'netstat -tulpn | grep 7070'
   ```

2. Either stop the conflicting service or change the port in `bin/start.sh`

## Updating the Application

To deploy a new version:

1. Stop the service:
   ```bash
   ssh -p 50004 Administrator@114.130.145.71 'systemctl stop infinite-scheduler'
   ```

2. Backup the current JAR:
   ```bash
   ssh -p 50004 Administrator@114.130.145.71 'cp /opt/infinite-scheduler/app/infinite-scheduler-1.0.0.jar /opt/infinite-scheduler/app/infinite-scheduler-1.0.0.jar.bak'
   ```

3. Copy new JAR:
   ```bash
   sshpass -p 'Takay1#$ane%%' scp -P 50004 /path/to/new.jar Administrator@114.130.145.71:/opt/infinite-scheduler/app/
   ```

4. Start the service:
   ```bash
   ssh -p 50004 Administrator@114.130.145.71 'systemctl start infinite-scheduler'
   ```

## Security Notes

- Service runs as `root` user (consider changing for production)
- Database credentials are stored in plain text in `quartz.properties`
- SSH password authentication is used (consider switching to key-based)
- Debug port is disabled by default (do not enable in production without firewall rules)

## Monitoring

### Health Check

The application should be accessible at:
- **HTTP**: http://114.130.145.71:7070 (if web interface is enabled)

### Key Metrics to Monitor

1. Service status: `systemctl is-active infinite-scheduler`
2. Memory usage: Check logs for OOM errors
3. Database connections: Check Quartz datasource pool
4. Job execution rate: Monitor Quartz scheduler metrics
5. Failed jobs: Check application logs for failures

## Support

For issues or questions, check:
1. Application logs: `/opt/infinite-scheduler/logs/infinite-scheduler.log`
2. Systemd logs: `journalctl -u infinite-scheduler`
3. Project documentation: `/home/mustafa/telcobright-projects/routesphere/infinite-scheduler/CLAUDE.md`
