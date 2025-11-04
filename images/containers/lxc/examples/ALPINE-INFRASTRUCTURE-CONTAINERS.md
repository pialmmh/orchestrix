# Alpine-based Infrastructure Containers

Guide for building minimal infrastructure containers (Consul, CoreDNS) using Alpine Linux.

## Why Alpine for Infrastructure?

✅ **Perfect for Go binaries** - Consul and CoreDNS are statically compiled
✅ **Minimal size** - 3 MB base vs 105 MB (Debian)
✅ **Security** - Smaller attack surface
✅ **Fast** - Less to download and decompress
✅ **Production-ready** - Used by Docker, Kubernetes

⚠️ **Not recommended for:**
- Application containers (Go-ID, etc.) - may need system libraries
- Complex dependencies - glibc vs musl compatibility issues
- Dynamic linking - prefer Debian for ease

## Container Sizes

### Consul

| Distribution | Image Size | Runtime | Total (3 nodes) |
|--------------|-----------|---------|-----------------|
| **Alpine** | **125 MB** | 30 MB | **155 MB** |
| Debian 12 | 230 MB | 30 MB | 260 MB |
| **Savings** | **105 MB** | - | **105 MB (40%)** |

### CoreDNS

| Distribution | Image Size | Runtime | Total (2 nodes) |
|--------------|-----------|---------|-----------------|
| **Alpine** | **50 MB** | 10 MB | **60 MB** |
| Debian 12 | 153 MB | 10 MB | 163 MB |
| **Savings** | **103 MB** | - | **103 MB (63%)** |

## Quick Start: Alpine Consul Container

### Build Script

```bash
#!/bin/bash
# Build Alpine-based Consul container

CONTAINER_NAME="alpine-consul-build"
CONSUL_VERSION="1.17.0"

# Create container from Alpine
lxc launch images:alpine/3.18 $CONTAINER_NAME

# Wait for network
sleep 5

# Install Consul
lxc exec $CONTAINER_NAME -- sh -c "
  apk add --no-cache wget ca-certificates

  cd /tmp
  wget https://releases.hashicorp.com/consul/${CONSUL_VERSION}/consul_${CONSUL_VERSION}_linux_amd64.zip
  unzip consul_${CONSUL_VERSION}_linux_amd64.zip
  mv consul /usr/local/bin/
  chmod +x /usr/local/bin/consul
  rm consul_${CONSUL_VERSION}_linux_amd64.zip

  # Create directories
  mkdir -p /etc/consul.d /var/lib/consul

  # Create consul user
  adduser -D -s /bin/false consul
  chown -R consul:consul /var/lib/consul
"

# Stop and export
lxc stop $CONTAINER_NAME
lxc publish $CONTAINER_NAME --alias alpine-consul
lxc delete $CONTAINER_NAME

echo "Alpine Consul image created: alpine-consul"
```

### Configuration (same as Debian)

The Consul configuration is identical to Debian-based setup:

```hcl
# /etc/consul.d/consul.hcl
datacenter = "dc1"
data_dir = "/var/lib/consul"
server = true
bootstrap_expect = 3
bind_addr = "192.168.1.100"
client_addr = "0.0.0.0"

retry_join = ["192.168.1.100", "192.168.1.101", "192.168.1.102"]

ui_config {
  enabled = true
}

ports {
  http = 8500
  dns = -1  # DNS disabled
}
```

### OpenRC Service (Alpine uses OpenRC, not systemd)

```bash
# /etc/init.d/consul
#!/sbin/openrc-run

name="consul"
description="Consul Agent"
command="/usr/local/bin/consul"
command_args="agent -config-dir=/etc/consul.d"
command_user="consul:consul"
pidfile="/run/${RC_SVCNAME}.pid"
command_background="yes"

depend() {
    need net
    after firewall
}
```

Enable and start:
```bash
lxc exec alpine-consul-server-1 -- rc-update add consul default
lxc exec alpine-consul-server-1 -- rc-service consul start
```

## Quick Start: Alpine CoreDNS Container

### Build Script

```bash
#!/bin/bash
# Build Alpine-based CoreDNS container

CONTAINER_NAME="alpine-coredns-build"
COREDNS_VERSION="1.11.1"

# Create container from Alpine
lxc launch images:alpine/3.18 $CONTAINER_NAME

# Wait for network
sleep 5

# Install CoreDNS
lxc exec $CONTAINER_NAME -- sh -c "
  apk add --no-cache wget ca-certificates

  cd /tmp
  wget https://github.com/coredns/coredns/releases/download/v${COREDNS_VERSION}/coredns_${COREDNS_VERSION}_linux_amd64.tgz
  tar xzf coredns_${COREDNS_VERSION}_linux_amd64.tgz
  mv coredns /usr/local/bin/
  chmod +x /usr/local/bin/coredns
  rm coredns_${COREDNS_VERSION}_linux_amd64.tgz

  # Create config directory
  mkdir -p /etc/coredns

  # Create coredns user
  adduser -D -s /bin/false coredns
"

# Stop and export
lxc stop $CONTAINER_NAME
lxc publish $CONTAINER_NAME --alias alpine-coredns
lxc delete $CONTAINER_NAME

echo "Alpine CoreDNS image created: alpine-coredns"
```

### CoreDNS Configuration

```
# /etc/coredns/Corefile
. {
    forward . 8.8.8.8 8.8.4.4
    cache 30
    errors
    log
    health :8080
}

# Custom zone example
example.local {
    file /etc/coredns/example.local.zone
    errors
    log
}
```

### OpenRC Service

```bash
# /etc/init.d/coredns
#!/sbin/openrc-run

name="coredns"
description="CoreDNS Server"
command="/usr/local/bin/coredns"
command_args="-conf /etc/coredns/Corefile"
command_user="coredns:coredns"
pidfile="/run/${RC_SVCNAME}.pid"
command_background="yes"

depend() {
    need net
}
```

## Complete Infrastructure Setup (Alpine)

### Recommended Architecture

```
Server 1:  Alpine Consul (125 MB) + Go-ID shard-0 (Debian, 169 MB)
Server 2:  Alpine Consul (125 MB) + Go-ID shard-1 (Debian, 169 MB)
Server 3:  Alpine Consul (125 MB) + Go-ID shard-2 (Debian, 169 MB)
Server 4:  Alpine CoreDNS (50 MB) + Other services
Server 5:  Alpine CoreDNS (50 MB) + Other services

Infrastructure: 525 MB (vs 766 MB on Debian) = 241 MB saved!
```

### Storage Breakdown (5 servers)

```
# Alpine-based infrastructure:
3 × Consul images:        375 MB (3 × 125 MB shared)
2 × CoreDNS images:       100 MB (2 × 50 MB shared)
3 × Go-ID images:         507 MB (3 × 169 MB, Debian OK here)
Runtime overhead:         ~100 MB
─────────────────────────────────────────────────
Total:                   1082 MB

# Debian-based infrastructure:
3 × Consul images:        690 MB (3 × 230 MB)
2 × CoreDNS images:       306 MB (2 × 153 MB)
3 × Go-ID images:         507 MB (3 × 169 MB)
Runtime overhead:         ~100 MB
─────────────────────────────────────────────────
Total:                   1603 MB

Savings: 521 MB (32% reduction!)
```

## Automation Integration

### Alpine Support is Built-In! ✅

The `ConsulClusterAutomation` framework **automatically detects** Alpine vs Debian and handles all differences:

```java
// Same code works for both Alpine and Debian!
ConsulClusterConfig config = new ConsulClusterConfig();
config.addNode("server-1", "192.168.1.100", ConsulNodeRole.SERVER);
config.addNode("server-2", "192.168.1.101", ConsulNodeRole.SERVER);
config.addNode("server-3", "192.168.1.102", ConsulNodeRole.SERVER);

ConsulClusterAutomation automation = ConsulClusterAutomationFactory.create(device);
automation.setupCluster(config);  // Auto-detects OS and uses appropriate commands
```

The automation handles:
- ✅ **OS Detection**: Reads `/etc/os-release` to identify Alpine vs Debian
- ✅ **Package Management**: Uses `apk` for Alpine, `apt` for Debian
- ✅ **Service Management**: Creates OpenRC services for Alpine, systemd for Debian
- ✅ **User Creation**: Uses `adduser -D` for Alpine, `useradd -r` for Debian

**No manual intervention required!** Just provide Alpine container IPs and the automation does the rest.

## Best Practices

### When to Use Alpine

✅ **Use Alpine for:**
- Consul (infrastructure service)
- CoreDNS (infrastructure service)
- Nginx (reverse proxy)
- HAProxy (load balancer)
- Redis (cache)
- Any Go-based infrastructure

❌ **Avoid Alpine for:**
- Application containers (Go-ID, Java apps)
- Containers with complex dependencies
- When team is unfamiliar with Alpine/musl

### Package Management

```bash
# Alpine uses apk (not apt)
apk update
apk add package-name
apk search package-name
apk del package-name

# Common packages:
apk add ca-certificates  # SSL certificates
apk add curl wget        # Download tools
apk add bash             # If you need bash (not default)
```

### Service Management

```bash
# Alpine uses OpenRC (not systemd)
rc-update add service-name default  # Enable on boot
rc-service service-name start       # Start service
rc-service service-name stop        # Stop service
rc-service service-name status      # Check status
```

## Migration Path

### Phase 1: Infrastructure Only (Now)
1. Build Alpine Consul containers
2. Deploy 3-node cluster
3. Test thoroughly
4. Keep applications on Debian

### Phase 2: Add CoreDNS (Future)
1. Build Alpine CoreDNS containers
2. Deploy 2-node DNS setup
3. Integrate with Consul
4. Keep applications on Debian

### Phase 3: Evaluate Others (Optional)
- Consider Alpine for: HAProxy, Nginx, Redis
- Keep on Debian: Go-ID, Java apps, complex services

## Troubleshooting

### Common Issues

**Issue: glibc not found**
- Solution: Use statically compiled binaries (Consul, CoreDNS already are)

**Issue: bash not available**
- Solution: Use sh or `apk add bash`

**Issue: systemd commands don't work**
- Solution: Use OpenRC commands instead

**Issue: Package not found**
- Solution: Search Alpine packages: https://pkgs.alpinelinux.org/

## Size Summary

| Infrastructure | Alpine | Debian | Savings |
|----------------|--------|--------|---------|
| **1 Consul node** | 125 MB | 230 MB | 105 MB (45%) |
| **3 Consul nodes** | 155 MB | 260 MB | 105 MB (40%) |
| **1 CoreDNS node** | 50 MB | 153 MB | 103 MB (67%) |
| **2 CoreDNS nodes** | 60 MB | 163 MB | 103 MB (63%) |
| **Full stack (5 servers)** | 1082 MB | 1603 MB | 521 MB (32%) |

## Recommendation

✅ **Use Alpine for Consul and CoreDNS**
- 32% storage savings on infrastructure
- Production-proven for these services
- No compatibility issues (Go binaries)
- Faster deployment (smaller images)

✅ **Keep Debian for Applications**
- Go-ID and similar services
- Better compatibility
- Easier troubleshooting
- Team familiarity

**Best of both worlds!** 🎉
