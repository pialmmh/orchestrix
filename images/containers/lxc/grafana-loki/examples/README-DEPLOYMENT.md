# Grafana-Loki Deployment Examples

This directory contains example deployment configurations and patterns for various scenarios.

## Quick Start

### 1. Production Deployment

```bash
cd /home/mustafa/telcobright-projects/orchestrix/images/lxc/grafana-loki

# Use generated deployment script
./grafana-loki-v.1/generated/deploy.sh --production
```

### 2. Staging Deployment

```bash
./grafana-loki-v.1/generated/deploy.sh --staging
```

### 3. Local Development

```bash
./grafana-loki-v.1/generated/deploy.sh --local
```

### 4. Custom Deployment

```bash
# Copy example configuration
cp examples/multi-server-deployment.yml my-deployment.yml

# Edit configuration
vi my-deployment.yml

# Deploy
./grafana-loki-v.1/generated/deploy.sh my-deployment.yml
```

---

## Deployment Configurations Location

All deployment configurations are under version-specific directories:

```
grafana-loki-v.1/
└── generated/
    ├── deploy.sh                    ← Main deployment script
    ├── deployments/
    │   ├── production.yml           ← Production config
    │   ├── staging.yml              ← Staging config
    │   └── local-dev.yml            ← Local dev config
    └── artifact/                    ← Built container images
        ├── grafana-loki-v1-*.tar.gz
        └── *.md5
```

---

## Deployment Scenarios

### Scenario 1: Single Server Production

**Use Case:** Deploy to a single production server with full monitoring and backups.

**Configuration:** `grafana-loki-v.1/generated/deployments/production.yml`

**Command:**
```bash
./grafana-loki-v.1/generated/deploy.sh --production
```

**Features:**
- 30GB storage quota
- BTRFS with compression
- 7-day log retention
- Automatic backups
- Health monitoring

---

### Scenario 2: Multi-Server Deployment

**Use Case:** Deploy to multiple servers for high availability.

**Configuration:** `examples/multi-server-deployment.yml`

**Command:**
```bash
./grafana-loki-v.1/generated/deploy.sh examples/multi-server-deployment.yml
```

**Features:**
- Load balanced Loki instances
- Shared storage backend
- Centralized Grafana
- Cross-server log aggregation

---

### Scenario 3: Development Environment

**Use Case:** Quick local development setup with minimal resources.

**Configuration:** `grafana-loki-v.1/generated/deployments/local-dev.yml`

**Command:**
```bash
./grafana-loki-v.1/generated/deploy.sh --local
```

**Features:**
- Small storage (5GB)
- 1-day retention
- No backups
- Minimal monitoring

---

### Scenario 4: Staging/Testing

**Use Case:** Pre-production testing environment.

**Configuration:** `grafana-loki-v.1/generated/deployments/staging.yml`

**Command:**
```bash
./grafana-loki-v.1/generated/deploy.sh --staging
```

**Features:**
- Medium storage (10GB)
- 3-day retention
- Validation enabled
- Monitoring enabled

---

## Configuration Options Reference

### Required Fields

```yaml
deployment:
  name: <unique-name>
  version: v1.0
  environment: production|staging|development

target:
  host: <server-ip>
  port: 22

ssh:
  username: <ssh-user>
  password: <password>  # or use keyPath

container:
  name: <container-name>
  version: 1.0.0

storage:
  provider: btrfs
  quota: <size>
```

### Optional Fields

```yaml
# Resource limits
container:
  resources:
    memory: 2GB
    cpu: 2

# Service versions
services:
  grafana:
    version: "10.2.3"
  loki:
    version: "2.9.4"
    retention: 168h

# Network configuration
network:
  bridge: lxdbr0
  mode: dhcp | static
  staticIP: 10.10.199.50/24

# Deployment strategy
strategy:
  autoRollback: true
  validateAfterDeploy: true

# Security
security:
  changeDefaultPassword: true
  enableHttps: false
```

---

## Directory Structure Pattern

Following the Orchestrix scaffolding standard:

```
grafana-loki/                        ← Container directory
├── build/                           ← Build scripts
│   ├── build.conf
│   └── build.sh
├── examples/                        ← Example configs (this directory)
│   ├── README-DEPLOYMENT.md         ← This file
│   └── multi-server-deployment.yml  ← Example config
├── grafana-loki-v.1/               ← Version 1.0
│   └── generated/                   ← Generated artifacts
│       ├── artifact/                ← Built images
│       │   └── grafana-loki-v1-*.tar.gz
│       ├── deployments/             ← Deployment configs
│       │   ├── production.yml
│       │   ├── staging.yml
│       │   └── local-dev.yml
│       ├── deploy.sh                ← Deployment script
│       ├── sample.conf
│       └── startDefault.sh
├── launchGrafanaLoki.sh            ← Launch helper
├── README.md                        ← Main documentation
├── scripts/                         ← Helper scripts
├── startDefault.sh                  ← Quick start
└── templates/                       ← Config templates
```

---

## Advanced Deployment Patterns

### Pattern 1: Deploy from CI/CD

```bash
#!/bin/bash
# Jenkins/GitLab CI deployment

# Build container
cd /path/to/orchestrix/images/lxc/grafana-loki
./build/build.sh

# Deploy to staging
./grafana-loki-v.1/generated/deploy.sh --staging

# Run validation tests
# ... test commands ...

# If tests pass, deploy to production
./grafana-loki-v.1/generated/deploy.sh --production
```

### Pattern 2: Blue-Green Deployment

```bash
# Deploy to blue environment
./grafana-loki-v.1/generated/deploy.sh deployments/production-blue.yml

# Validate
# ... validation ...

# Switch traffic to blue
# ... traffic switch ...

# Deploy to green (old environment)
./grafana-loki-v.1/generated/deploy.sh deployments/production-green.yml
```

### Pattern 3: Canary Deployment

```bash
# Deploy canary (10% traffic)
./grafana-loki-v.1/generated/deploy.sh deployments/production-canary.yml

# Monitor metrics
# ... monitoring ...

# If successful, roll out to 100%
./grafana-loki-v.1/generated/deploy.sh --production
```

---

## Troubleshooting

### Deployment Failed

```bash
# Check logs
cat /var/log/grafana-loki-deployment.log

# Re-run with dry-run
./grafana-loki-v.1/generated/deploy.sh --production --dry-run

# Validate configuration
python3 -c "import yaml; yaml.safe_load(open('deployments/production.yml'))"
```

### SSH Connection Issues

```bash
# Test SSH manually
ssh bdcom@10.255.246.175

# Check SSH key permissions
chmod 600 ~/.ssh/id_rsa

# Use password authentication
# Edit config: ssh.useKey: false
```

### Storage Issues

```bash
# Check available disk space
ssh bdcom@10.255.246.175 df -h

# Reduce storage quota
# Edit config: storage.quota: 10G
```

---

## Best Practices

1. **Version Control Configs**
   - Keep deployment configs in git
   - Use `.gitignore` for sensitive data
   - Separate configs per environment

2. **Security**
   - Use SSH keys instead of passwords
   - Change default Grafana password immediately
   - Restrict network access with firewall rules

3. **Monitoring**
   - Enable storage monitoring
   - Configure health checks
   - Set up alerting for critical events

4. **Backup**
   - Enable automatic snapshots
   - Test restore procedures
   - Keep backups offsite

5. **Testing**
   - Always dry-run first
   - Test in staging before production
   - Validate after deployment

---

## Support

- **Container Documentation**: `../README.md`
- **Build Documentation**: `../build/README.md`
- **Scaffolding Standard**: `../CONTAINER_SCAFFOLDING_STANDARD.md`
- **Java Automation**: `/home/mustafa/telcobright-projects/orchestrix/automation/`

---

## Changelog

### v1.0 (2025-11-01)
- Initial deployment configurations
- Production, staging, and local-dev configs
- Multi-server deployment example
- Deployment automation script
