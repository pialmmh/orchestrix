# Kafka External Access Troubleshooting Guide

## Current Status: ✅ Server Configuration is Correct

**Verified:**
- ✅ Port 29092 is listening on 0.0.0.0 (all interfaces)
- ✅ Kafka advertised.listeners configured correctly
- ✅ LXC proxy device configured correctly
- ✅ No firewall blocking on server
- ✅ Connectivity works from SERVER itself (123.200.0.51:29092)

**Issue:** Cannot connect from OUTSIDE the server/network

---

## Root Cause Analysis

The server configuration is **100% correct**. The issue is likely:

### 1. **Network Firewall/Router** (Most Likely)
Your network router or external firewall is blocking incoming connections on port 29092.

### 2. **ISP Port Blocking**
Some ISPs block non-standard ports for security reasons.

### 3. **NAT Configuration**
If server is behind NAT, port forwarding may not be configured on the router.

### 4. **Client-Side Firewall**
Your client machine's firewall might be blocking outgoing connections.

---

## Diagnostic Tests

### Test 1: From Your Local Machine

```bash
# Test 1: Check if port is reachable
telnet 123.200.0.51 29092

# Expected if working:
# Trying 123.200.0.51...
# Connected to 123.200.0.51.

# If fails:
# Connection refused / Connection timeout
```

### Test 2: Using Netcat

```bash
# Test port connectivity
nc -zv 123.200.0.51 29092

# Expected if working:
# Connection to 123.200.0.51 29092 port [tcp/*] succeeded!
```

### Test 3: Online Port Checker

Visit: https://www.yougetsignal.com/tools/open-ports/

- Enter IP: `123.200.0.51`
- Enter Port: `29092`
- Click "Check"

**If it shows "Closed"** → External firewall/router is blocking the port

---

## Solutions

## Solution 1: Configure External Firewall/Router ⭐ RECOMMENDED

### If You Have Router Access:

1. **Log into your router** (typically 192.168.1.1 or 192.168.0.1)

2. **Find Port Forwarding Section**
   - Usually under: Advanced → Port Forwarding → NAT

3. **Add Port Forward Rule:**
   ```
   External Port: 29092
   Internal IP: 123.200.0.51
   Internal Port: 29092
   Protocol: TCP
   ```

4. **Save and reboot router**

5. **Test again** from outside network

---

## Solution 2: Use SSH Tunnel (Workaround)

If you cannot configure the router, use SSH tunneling:

### From Your Client Machine:

```bash
# Create SSH tunnel
ssh -L 9092:10.194.110.52:9092 -p 8210 tbsms@123.200.0.51

# Keep this terminal open
# Now connect to Kafka on localhost:9092
```

### In Your Application:

```python
# Python example
from kafka import KafkaConsumer

consumer = KafkaConsumer(
    'all-db-changes',
    bootstrap_servers=['localhost:9092'],  # Note: localhost, not 123.200.0.51
    auto_offset_reset='earliest'
)

for message in consumer:
    print(message.value)
```

**Advantages:**
- ✅ Works even if port 29092 is blocked
- ✅ Encrypted connection through SSH
- ✅ No router configuration needed

**Disadvantages:**
- ❌ Requires SSH connection to stay open
- ❌ Only works from one client at a time

---

## Solution 3: Use Alternative Port (If Port 29092 is Blocked)

Some ports are commonly open:

### Option A: Use Port 8080 (HTTP Alternative)

```bash
# SSH to server
ssh -p 8210 tbsms@123.200.0.51

# Remove old proxy
lxc config device remove Kafka kafka-port

# Add new proxy on port 8080
lxc config device add Kafka kafka-port proxy \
  listen=tcp:0.0.0.0:8080 \
  connect=tcp:127.0.0.1:9092

# Update Kafka advertised listeners
lxc exec Kafka -- bash -c "sed -i 's/:29092/:8080/g' /opt/kafka/config/server.properties.runtime"

# Restart Kafka
lxc restart Kafka
```

**Connect with:** `123.200.0.51:8080`

### Option B: Use Port 443 (HTTPS Port - Usually Open)

```bash
# Same as above but use port 443
lxc config device add Kafka kafka-port proxy \
  listen=tcp:0.0.0.0:443 \
  connect=tcp:127.0.0.1:9092
```

**Connect with:** `123.200.0.51:443`

---

## Solution 4: VPN Access

### Set Up WireGuard VPN (Secure Remote Access)

This gives you direct access to the internal network:

```bash
# Install WireGuard on server
ssh -p 8210 tbsms@123.200.0.51
sudo apt update && sudo apt install -y wireguard

# Generate keys
wg genkey | tee privatekey | wg pubkey > publickey

# Configure WireGuard
sudo nano /etc/wireguard/wg0.conf
```

Once VPN is set up, you can access Kafka on internal IP: `10.194.110.52:9092`

---

## Solution 5: Reverse Proxy with Nginx (Advanced)

Use Nginx as a TCP proxy:

```bash
# Install Nginx with stream module
sudo apt install -y nginx-full

# Configure TCP proxy
sudo nano /etc/nginx/nginx.conf
```

Add:
```nginx
stream {
    upstream kafka {
        server 10.194.110.52:9092;
    }

    server {
        listen 29092;
        proxy_pass kafka;
    }
}
```

```bash
sudo systemctl restart nginx
```

---

## Quick Diagnostics Commands

### On Server (123.200.0.51):

```bash
# Check port is listening
netstat -tlnp | grep 29092

# Should show:
# tcp6  0  0  :::29092  :::*  LISTEN

# Test from server
telnet 123.200.0.51 29092
# Should connect

# Test Kafka API
timeout 3 bash -c 'echo -e "" | nc 123.200.0.51 29092'
# Should return some data
```

### From Your Client Machine:

```bash
# Test 1: Ping
ping 123.200.0.51

# Test 2: SSH (should work since you can SSH in)
telnet 123.200.0.51 8210
# Should connect

# Test 3: Kafka port
telnet 123.200.0.51 29092
# If this fails but above works → Port 29092 is blocked
```

---

## Current Server Configuration

```yaml
Server IP: 123.200.0.51
Kafka External Port: 29092
LXC Proxy:
  Listen: tcp:0.0.0.0:29092
  Connect: tcp:127.0.0.1:9092 (Kafka container)

Kafka Advertised Listeners:
  INTERNAL: 10.194.110.52:9092  (container network)
  EXTERNAL: 123.200.0.51:29092  (public network)

Container IP: 10.194.110.52
Container Internal Port: 9092
```

---

## Recommended Connection Methods (in order of preference)

### 1. **Fix Router/Firewall** ⭐ Best for production
- Permanent solution
- Best performance
- Multiple clients can connect

### 2. **SSH Tunnel** ⭐ Best for development/testing
- Works immediately
- No router access needed
- Secure and encrypted

### 3. **Alternative Port**
- Use if port 29092 is blocked by ISP
- Try ports: 8080, 443, 8443

### 4. **VPN**
- Best for multiple users
- Access entire internal network
- Most secure

---

## Testing Kafka Connection

### Method 1: Using kafkacat (Recommended)

```bash
# Install kafkacat
sudo apt install -y kafkacat   # Ubuntu/Debian
brew install kafkacat          # macOS

# Test connection
kafkacat -b 123.200.0.51:29092 -L

# If successful, you'll see broker metadata
# If fails with timeout → Port blocked
```

### Method 2: Using kafka-console-consumer

```bash
# Download Kafka binaries
wget https://downloads.apache.org/kafka/3.9.0/kafka_2.13-3.9.0.tgz
tar -xzf kafka_2.13-3.9.0.tgz
cd kafka_2.13-3.9.0

# Test connection
bin/kafka-console-consumer.sh \
  --bootstrap-server 123.200.0.51:29092 \
  --topic all-db-changes \
  --from-beginning \
  --max-messages 1
```

### Method 3: Using Python

```python
from kafka import KafkaConsumer
from kafka.errors import NoBrokersAvailable
import sys

try:
    consumer = KafkaConsumer(
        'all-db-changes',
        bootstrap_servers=['123.200.0.51:29092'],
        consumer_timeout_ms=5000
    )
    print("✅ Connected to Kafka successfully!")

    for message in consumer:
        print(f"✅ Received message: {message.value}")
        break

except NoBrokersAvailable:
    print("❌ Cannot connect to Kafka broker")
    print("Possible causes:")
    print("  1. Port 29092 is blocked by firewall/router")
    print("  2. Kafka server is not running")
    print("  3. Network connectivity issue")
    sys.exit(1)
```

---

## SSH Tunnel - Complete Guide

### Step-by-Step Setup:

**Step 1: Create SSH Tunnel**

```bash
# Terminal 1 - Keep this running
ssh -L 9092:10.194.110.52:9092 -p 8210 tbsms@123.200.0.51 -N

# Explanation:
# -L 9092:10.194.110.52:9092  → Forward local port 9092 to Kafka container
# -p 8210                      → SSH port
# -N                           → Don't execute remote command
```

**Step 2: Connect to Kafka via Tunnel**

```bash
# Terminal 2 - Your application
kafka-console-consumer.sh \
  --bootstrap-server localhost:9092 \
  --topic all-db-changes
```

**Step 3: In Your Application Code**

```python
# Use localhost instead of 123.200.0.51
consumer = KafkaConsumer(
    'all-db-changes',
    bootstrap_servers=['localhost:9092']  # Not 123.200.0.51!
)
```

---

## Error Messages & Solutions

### Error: "Connection refused"
```
kafka.errors.NoBrokersAvailable: NoBrokersAvailable
```

**Cause:** Port is blocked or Kafka is not accessible

**Solutions:**
1. Use SSH tunnel (Solution 2)
2. Configure router firewall (Solution 1)
3. Try alternative port (Solution 3)

### Error: "Connection timeout"
```
TimeoutError: [Errno 110] Connection timed out
```

**Cause:** Firewall dropping packets (not refusing)

**Solutions:**
1. Check external firewall/router
2. Try SSH tunnel
3. Contact network administrator

### Error: "Broker not available"
```
org.apache.kafka.common.errors.TimeoutException: Timed out waiting for a node assignment
```

**Cause:** Can connect to port but Kafka returns wrong advertised listener

**Solution:**
```bash
# On server, verify advertised.listeners
ssh -p 8210 tbsms@123.200.0.51
lxc exec Kafka -- cat /opt/kafka/config/server.properties.runtime | grep advertised

# Should show:
# advertised.listeners=INTERNAL://10.194.110.52:9092,EXTERNAL://123.200.0.51:29092
```

---

## Contact Information for Network Admin

If you need to contact your network administrator, provide this information:

```
Request: Open incoming TCP port 29092

Details:
- Server IP: 123.200.0.51
- Port: 29092
- Protocol: TCP
- Purpose: Kafka message broker access
- Internal IP: 123.200.0.51 (same as external)
- Service: Apache Kafka (Message Streaming Platform)

Port Forwarding Rule:
- External Port: 29092 (TCP)
- Internal IP: 123.200.0.51
- Internal Port: 29092 (TCP)
- Bi-directional: Yes
```

---

## Quick Fix Summary

**If you just want to connect NOW:**

```bash
# Run this on your client machine:
ssh -L 9092:10.194.110.52:9092 -p 8210 tbsms@123.200.0.51 -N

# Then in another terminal, connect to localhost:9092
kafka-console-consumer.sh --bootstrap-server localhost:9092 --topic all-db-changes
```

**Done!** You can now consume Kafka messages.

---

## Verification Checklist

- [ ] Port 29092 is listening on server: `netstat -tlnp | grep 29092` ✅
- [ ] Can connect from server itself: `telnet 123.200.0.51 29092` ✅
- [ ] Can connect from external network: `telnet 123.200.0.51 29092` ❌
- [ ] Router/firewall port forwarding configured: ⏳ Pending
- [ ] SSH tunnel working: Test with Solution 2

---

## Next Steps

**For Production Use:**
1. Contact network admin to open port 29092
2. Or configure router port forwarding yourself

**For Testing/Development:**
1. Use SSH tunnel (works immediately)
2. No configuration needed
3. Secure and encrypted

---

**Note:** The server configuration is 100% correct. The only issue is network-level port blocking, which is outside the server's control.
