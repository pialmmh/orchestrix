# Fix Jenkins Agent Connection

## The Issue
Your Docker agent is running but can't connect because Jenkins JNLP/TCP port is not enabled.

## Solution

### Option 1: Enable JNLP Port in Jenkins (Recommended)

1. **Open Jenkins in browser**: http://172.82.66.90:8080
2. **Navigate to**: Manage Jenkins → Configure Global Security
3. **Find "Agents" section**
4. **TCP port for inbound agents**: Select "Fixed" and enter port `50000`
5. **Click "Save"**

### Option 2: Use WebSocket Connection (Alternative)

If you can't enable TCP port, update the Docker Compose to use WebSocket:

```bash
cd /home/mustafa/telcobright-projects/orchestrix/jenkins

# Stop current agent
docker-compose down

# Edit docker-compose.yml and add this environment variable:
# JENKINS_WEB_SOCKET=true

# Restart agent
docker-compose up -d
```

## Verify Connection

After fixing:

1. **Check agent status**:
```bash
./manage-agent.sh logs
```

2. **Check Jenkins UI**: http://172.82.66.90:8080/computer/orchestrix-agent/
   - Should show as "Connected" or "Online"

## Still Having Issues?

1. **Get fresh secret from Jenkins**:
   - Go to: http://172.82.66.90:8080/computer/orchestrix-agent/
   - Copy the secret
   - Update in docker-compose.yml
   - Restart: `docker-compose restart`

2. **Check firewall**:
   - Ensure port 50000 is open if using TCP
   - Or port 8080 is accessible if using WebSocket

3. **View detailed logs**:
```bash
docker logs -f jenkins-orchestrix-agent
```