# Docker Bridge Network with BGP-Advertised Subnets

## Overview

Instead of using Docker's default NAT networking with port forwarding, configure Docker to use custom bridge networks with IPs from the BGP-advertised subnets. This allows:

- ✓ Direct IP access to containers from any server
- ✓ No port forwarding needed
- ✓ No port conflicts
- ✓ Docker and LXD containers coexist on same subnet
- ✓ Containers reachable via BGP routing

---

## Network Architecture

```
BDCOM1 (10.255.246.173)
├── Docker Bridge: docker-br0
├── Subnet: 10.10.196.0/24
├── Docker Containers: 10.10.196.100-199
├── LXD Containers: 10.10.196.200-254
└── BGP advertises: 10.10.196.0/24

BDCOM2 (10.255.246.174)
├── Docker Bridge: docker-br0
├── Subnet: 10.10.195.0/24
├── Docker Containers: 10.10.195.100-199
├── LXD Containers: 10.10.195.200-254
└── BGP advertises: 10.10.195.0/24

BDCOM3 (10.255.246.175)
├── Docker Bridge: docker-br0
├── Subnet: 10.10.194.0/24
├── Docker Containers: 10.10.194.100-199
├── LXD Containers: 10.10.194.200-254
└── BGP advertises: 10.10.194.0/24
```

### Result
- Container at 10.10.196.100 on BDCOM1 can reach 10.10.195.150 on BDCOM2 directly
- No NAT, no port forwarding, just routing via BGP

---

## Step-by-Step Configuration

### Method 1: Create Custom Docker Network (Recommended)

This method creates a custom Docker network that uses the BGP subnet while keeping default Docker networking intact.

#### BDCOM1 Configuration

```bash
# Create custom bridge network using BGP subnet
docker network create \
  --driver=bridge \
  --subnet=10.10.196.0/24 \
  --gateway=10.10.196.1 \
  --ip-range=10.10.196.100/25 \
  --opt "com.docker.network.bridge.name"="docker-bgp" \
  bgp-net

# Verify network created
docker network ls
docker network inspect bgp-net
```

#### Run Containers on Custom Network

```bash
# Run container with specific IP from BGP subnet
docker run -d \
  --name nginx-app \
  --network bgp-net \
  --ip 10.10.196.100 \
  -p 80:80 \
  nginx:latest

# Or let Docker assign IP from range
docker run -d \
  --name app2 \
  --network bgp-net \
  myapp:latest

# Check container IP
docker inspect nginx-app | grep IPAddress
```

#### Configure Host Routing

```bash
# Add route to Docker bridge (if needed)
# Usually automatic, but verify:
ip route show | grep 10.10.196.0

# Should show:
# 10.10.196.0/24 dev docker-bgp proto kernel scope link src 10.10.196.1
```

#### BDCOM2 Configuration

```bash
docker network create \
  --driver=bridge \
  --subnet=10.10.195.0/24 \
  --gateway=10.10.195.1 \
  --ip-range=10.10.195.100/25 \
  --opt "com.docker.network.bridge.name"="docker-bgp" \
  bgp-net
```

#### BDCOM3 Configuration

```bash
docker network create \
  --driver=bridge \
  --subnet=10.10.194.0/24 \
  --gateway=10.10.194.1 \
  --ip-range=10.10.194.100/25 \
  --opt "com.docker.network.bridge.name"="docker-bgp" \
  bgp-net
```

---

### Method 2: Use LXD Bridge Directly (Advanced)

This method shares the same bridge between Docker and LXD.

⚠️ **Warning:** More complex, requires careful IP management

#### Create LXD Bridge First

```bash
# On BDCOM1
sudo lxc network create lxdbr0 \
  ipv4.address=10.10.196.1/24 \
  ipv4.nat=false \
  ipv4.dhcp.ranges=10.10.196.200-10.10.196.254

# Verify bridge
ip addr show lxdbr0
```

#### Configure Docker to Use LXD Bridge

```bash
# Stop Docker
sudo systemctl stop docker

# Edit Docker daemon config
sudo nano /etc/docker/daemon.json
```

Add:
```json
{
  "bridge": "lxdbr0",
  "fixed-cidr": "10.10.196.100/25",
  "default-address-pools": [
    {
      "base": "10.10.196.0/24",
      "size": 24
    }
  ]
}
```

```bash
# Start Docker
sudo systemctl start docker

# Verify Docker using lxdbr0
docker network inspect bridge | grep "com.docker.network.bridge.name"
```

---

## IP Allocation Strategy

### Recommended Subnet Division (per server)

```
10.10.196.0/24 (BDCOM1 example)
├── 10.10.196.1       → Bridge gateway
├── 10.10.196.2-99    → Reserved for infrastructure
├── 10.10.196.100-199 → Docker containers
└── 10.10.196.200-254 → LXD containers
```

### IP Range Configuration

**BDCOM1 (10.10.196.0/24):**
- Gateway: 10.10.196.1
- Docker range: 10.10.196.100-10.10.196.199
- LXD range: 10.10.196.200-10.10.196.254

**BDCOM2 (10.10.195.0/24):**
- Gateway: 10.10.195.1
- Docker range: 10.10.195.100-10.10.195.199
- LXD range: 10.10.195.200-10.10.195.254

**BDCOM3 (10.10.194.0/24):**
- Gateway: 10.10.194.1
- Docker range: 10.10.194.100-10.10.194.199
- LXD range: 10.10.194.200-10.10.194.254

---

## Docker Compose with Custom Network

### Example: docker-compose.yml

```yaml
version: '3.8'

services:
  nginx:
    image: nginx:latest
    container_name: nginx-app
    networks:
      bgp-net:
        ipv4_address: 10.10.196.100
    ports:
      - "80:80"
    restart: unless-stopped

  mysql:
    image: mysql:8.0
    container_name: mysql-db
    networks:
      bgp-net:
        ipv4_address: 10.10.196.101
    environment:
      MYSQL_ROOT_PASSWORD: password
    restart: unless-stopped

  app:
    image: myapp:latest
    container_name: myapp
    networks:
      bgp-net:
        ipv4_address: 10.10.196.102
    depends_on:
      - mysql
    restart: unless-stopped

networks:
  bgp-net:
    external: true
```

### Deploy

```bash
# Create network first (if not exists)
docker network create \
  --driver=bridge \
  --subnet=10.10.196.0/24 \
  --gateway=10.10.196.1 \
  --ip-range=10.10.196.100/25 \
  --opt "com.docker.network.bridge.name"="docker-bgp" \
  bgp-net

# Deploy stack
docker-compose up -d

# Verify containers
docker ps
docker inspect nginx-app | grep IPAddress
```

---

## Testing Connectivity

### Test 1: Container-to-Container (Same Server)

```bash
# On BDCOM1
docker exec nginx-app ping 10.10.196.101
# Should work (same subnet)
```

### Test 2: Container-to-Container (Different Servers)

```bash
# From container on BDCOM1 to container on BDCOM2
docker exec nginx-app ping 10.10.195.100

# Should work via BGP routing!
```

### Test 3: External Access to Container

```bash
# From BDCOM2, access container on BDCOM1
curl http://10.10.196.100

# From BDCOM3, access container on BDCOM1
curl http://10.10.196.100

# All should work via BGP routing!
```

### Test 4: Verify Routing Path

```bash
# From BDCOM2, trace route to container on BDCOM1
traceroute 10.10.196.100

# Expected path:
# 10.10.195.1 (local gateway)
# 10.9.9.4 (BDCOM1 overlay IP)
# 10.10.196.100 (container)
```

---

## Benefits of This Approach

### 1. No Port Conflicts
```bash
# Traditional Docker (port forwarding)
docker run -p 8080:80 nginx  # Port 8080 now blocked for other apps

# With BGP networking
docker run --network bgp-net --ip 10.10.196.100 nginx  # Port 80 available
docker run --network bgp-net --ip 10.10.196.101 nginx  # Port 80 available
# No conflicts! Each container has unique IP
```

### 2. Direct Access
```bash
# Access container directly by IP from anywhere
curl http://10.10.196.100:80
curl http://10.10.196.100:443
curl http://10.10.196.100:8080

# No need to remember mapped ports!
```

### 3. Service Discovery
```bash
# In your application config, use direct IPs
DATABASE_HOST=10.10.195.101
REDIS_HOST=10.10.194.102
API_HOST=10.10.196.103

# Works across all servers via BGP!
```

### 4. Load Balancing
```bash
# Run same service on multiple servers
BDCOM1: nginx at 10.10.196.100
BDCOM2: nginx at 10.10.195.100
BDCOM3: nginx at 10.10.194.100

# Load balancer can use all three IPs directly
```

---

## Important Considerations

### 1. IP Address Management

**Keep a spreadsheet or database:**
```
Server  | Service     | IP            | Ports
--------|-------------|---------------|-------
BDCOM1  | nginx       | 10.10.196.100 | 80,443
BDCOM1  | mysql       | 10.10.196.101 | 3306
BDCOM2  | redis       | 10.10.195.100 | 6379
BDCOM2  | postgres    | 10.10.195.101 | 5432
```

### 2. Firewall Rules

```bash
# Allow traffic on Docker bridge
sudo ufw allow in on docker-bgp
sudo ufw allow out on docker-bgp

# Or specific services
sudo ufw allow from 10.10.0.0/16 to any port 80
sudo ufw allow from 10.10.0.0/16 to any port 443
```

### 3. DNS (Optional but Recommended)

Set up internal DNS for easier management:
```bash
# Instead of remembering IPs
curl http://10.10.196.100

# Use hostnames
curl http://nginx.bdcom1.local
```

### 4. Container Restart Policy

Containers with static IPs should have restart policy:
```yaml
services:
  app:
    restart: unless-stopped
    networks:
      bgp-net:
        ipv4_address: 10.10.196.100
```

---

## Migration from Port Forwarding

### Before (Port Forwarding)
```bash
# Run with port mapping
docker run -d -p 8080:80 --name nginx nginx

# Access via host IP
curl http://10.255.246.173:8080
```

### After (Direct IP)
```bash
# Create custom network (one-time)
docker network create \
  --driver=bridge \
  --subnet=10.10.196.0/24 \
  --gateway=10.10.196.1 \
  --ip-range=10.10.196.100/25 \
  --opt "com.docker.network.bridge.name"="docker-bgp" \
  bgp-net

# Run with custom network
docker run -d \
  --network bgp-net \
  --ip 10.10.196.100 \
  --name nginx \
  nginx

# Access via container IP from ANYWHERE
curl http://10.10.196.100
```

---

## Troubleshooting

### Container IP Not Reachable from Other Servers

**Check 1: Verify container IP**
```bash
docker inspect <container> | grep IPAddress
```

**Check 2: Verify host can reach container**
```bash
ping 10.10.196.100
```

**Check 3: Verify BGP is advertising the subnet**
```bash
sudo vtysh -c 'show ip bgp'
# Should show 10.10.196.0/24
```

**Check 4: Verify routing on remote server**
```bash
# On BDCOM2
ip route get 10.10.196.100
# Should route via 10.9.9.4 (BDCOM1's overlay IP)
```

**Check 5: Check firewall**
```bash
sudo iptables -L -n -v | grep 10.10.196
```

### Docker Network Creation Fails

**Error: "Pool overlaps with other one on this address space"**

```bash
# Check existing networks
docker network ls
docker network inspect bridge

# Remove conflicting network
docker network rm <network-name>

# Or use different subnet
```

### Container Loses IP on Restart

**Solution: Always use static IP assignment**
```bash
docker run -d \
  --network bgp-net \
  --ip 10.10.196.100 \
  --restart unless-stopped \
  nginx
```

---

## Deployment Automation Script

### `setup-docker-bgp-network.sh`

```bash
#!/bin/bash

# Load cluster configuration
source bdcom-cluster.conf

setup_docker_network() {
    local node=$1
    local subnet="${NODE_LXD_NETWORK[$node]}"
    local gateway="${NODE_OVERLAY_IP[$node]}"

    # Extract first three octets for IP range
    local base_ip=$(echo $subnet | cut -d'.' -f1-3)
    local ip_range="${base_ip}.100/25"
    local gw="${base_ip}.1"

    echo "Setting up Docker BGP network on $node..."

    sshpass -p "${NODE_SSH_PASS[$node]}" ssh \
        -p ${NODE_SSH_PORT[$node]} \
        -o StrictHostKeyChecking=no \
        ${NODE_SSH_USER[$node]}@${NODE_MGMT_IP[$node]} \
        "echo '${NODE_SSH_PASS[$node]}' | sudo -S docker network create \
            --driver=bridge \
            --subnet=$subnet \
            --gateway=$gw \
            --ip-range=$ip_range \
            --opt \"com.docker.network.bridge.name\"=\"docker-bgp\" \
            bgp-net 2>/dev/null && \
         echo 'Docker BGP network created successfully' || \
         echo 'Network already exists or error occurred'"
}

# Setup on all nodes
for node in "${NODES[@]}"; do
    setup_docker_network "$node"
done

echo "Docker BGP network setup complete on all nodes!"
```

---

## Example: Multi-Server Application

### Application Stack

**BDCOM1 - Frontend:**
```yaml
version: '3.8'
services:
  nginx:
    image: nginx:latest
    networks:
      bgp-net:
        ipv4_address: 10.10.196.100
    volumes:
      - ./nginx.conf:/etc/nginx/nginx.conf
networks:
  bgp-net:
    external: true
```

**BDCOM2 - Backend API:**
```yaml
version: '3.8'
services:
  api:
    image: myapi:latest
    networks:
      bgp-net:
        ipv4_address: 10.10.195.100
    environment:
      DATABASE_HOST: 10.10.195.101
networks:
  bgp-net:
    external: true
```

**BDCOM3 - Database:**
```yaml
version: '3.8'
services:
  postgres:
    image: postgres:14
    networks:
      bgp-net:
        ipv4_address: 10.10.194.100
    environment:
      POSTGRES_PASSWORD: secret
networks:
  bgp-net:
    external: true
```

### nginx.conf (on BDCOM1)
```nginx
upstream api_backend {
    server 10.10.195.100:8080;  # API on BDCOM2
}

server {
    listen 80;

    location /api/ {
        proxy_pass http://api_backend;
    }
}
```

**Result:** Frontend on BDCOM1 can directly communicate with backend on BDCOM2 and database on BDCOM3, all via BGP routing!

---

## Comparison: Port Forwarding vs Bridge Network

| Feature | Port Forwarding | Bridge Network (BGP) |
|---------|----------------|---------------------|
| Container Access | Host IP + Port | Container IP directly |
| Port Conflicts | Yes | No |
| Cross-Server Access | Via host IPs | Direct container IPs |
| NAT Overhead | Yes | No |
| IP Management | Simple | Requires planning |
| Scalability | Limited by ports | Limited by subnet size |
| Service Discovery | Complex | Simple (direct IPs) |
| Load Balancing | Complex | Easy (multiple IPs) |

---

## Conclusion

Using Docker with custom bridge networks on BGP-advertised subnets provides:

✅ **Direct container access** - No port forwarding needed
✅ **No port conflicts** - Each container has unique IP
✅ **Cross-server communication** - Containers talk directly via BGP
✅ **Clean architecture** - No NAT, simple routing
✅ **Scalability** - Easy to add more containers
✅ **Coexistence** - Docker and LXD share same subnets

This approach transforms your Docker deployment from a port-forwarded mess into a clean, routable infrastructure where containers are first-class network citizens.

---

## Next Steps

1. Create custom Docker network on each server
2. Migrate existing containers to use new network
3. Update application configs to use direct IPs
4. Remove port forwarding configurations
5. Enjoy clean, routable container networking!
