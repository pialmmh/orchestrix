# Grafana-Loki Deployment Structure

This document describes the deployment directory organization following the **go-id pattern**.

## Directory Structure

```
grafana-loki/                                    ← Container root
├── build/                                       ← Build configuration
│   ├── build.conf                              ← Build parameters
│   └── build.sh                                ← Build script
│
├── examples/                                    ← Deployment examples
│   ├── README-DEPLOYMENT.md                    ← Deployment guide
│   └── multi-server-deployment.yml             ← HA deployment example
│
├── grafana-loki-v.1/                           ← Version 1.0 directory
│   └── generated/                              ← Generated artifacts
│       ├── artifact/                           ← Built container images
│       │   ├── grafana-loki-v1-*.tar.gz       ← Container image
│       │   └── *.md5                           ← Checksums
│       ├── deployments/                        ← Deployment configs
│       │   ├── production.yml                  ← Production config
│       │   ├── staging.yml                     ← Staging config
│       │   └── local-dev.yml                   ← Local dev config
│       ├── deploy.sh                           ← Deployment script
│       ├── sample.conf                         ← Sample configuration
│       └── startDefault.sh                     ← Quick start script
│
├── launchGrafanaLoki.sh                        ← Launch helper
├── README.md                                   ← Main documentation
├── scripts/                                    ← Helper scripts
│   ├── configure-log-rotation.sh
│   └── storage-monitor.sh
├── startDefault.sh                             ← Top-level quick start
├── templates/                                  ← Configuration templates
│   ├── sample.conf
│   └── startDefault.sh
└── versions.conf                               ← Version tracking
```

---

## Comparison with go-id Pattern

### ✓ Exact Match

| go-id Pattern | grafana-loki Implementation | Status |
|---------------|----------------------------|--------|
| `build/` | `build/` | ✅ |
| `build/build.conf` | `build/build.conf` | ✅ |
| `build/build.sh` | `build/build.sh` | ✅ |
| `examples/` | `examples/` | ✅ |
| `examples/README-DEPLOYMENT.md` | `examples/README-DEPLOYMENT.md` | ✅ |
| `{name}-v.1/` | `grafana-loki-v.1/` | ✅ |
| `{name}-v.1/generated/` | `grafana-loki-v.1/generated/` | ✅ |
| `{name}-v.1/generated/artifact/` | `grafana-loki-v.1/generated/artifact/` | ✅ |
| `{name}-v.1/generated/deployments/` | `grafana-loki-v.1/generated/deployments/` | ✅ |
| `{name}-v.1/generated/deploy.sh` | `grafana-loki-v.1/generated/deploy.sh` | ✅ |
| `{name}-v.1/generated/sample.conf` | `grafana-loki-v.1/generated/sample.conf` | ✅ |
| `{name}-v.1/generated/startDefault.sh` | `grafana-loki-v.1/generated/startDefault.sh` | ✅ |
| `launch{Name}.sh` | `launchGrafanaLoki.sh` | ✅ |
| `README.md` | `README.md` | ✅ |
| `scripts/` | `scripts/` | ✅ |
| `startDefault.sh` | `startDefault.sh` | ✅ |
| `templates/` | `templates/` | ✅ |

---

## Usage

### Quick Start (Default Configuration)

```bash
cd /home/mustafa/telcobright-projects/orchestrix/images/lxc/grafana-loki

# Quick start with defaults
./startDefault.sh
```

### Production Deployment

```bash
cd /home/mustafa/telcobright-projects/orchestrix/images/lxc/grafana-loki

# Deploy to production
./grafana-loki-v.1/generated/deploy.sh --production
```

### Staging Deployment

```bash
./grafana-loki-v.1/generated/deploy.sh --staging
```

### Local Development

```bash
./grafana-loki-v.1/generated/deploy.sh --local
```

### Custom Deployment

```bash
# Copy example configuration
cp grafana-loki-v.1/generated/deployments/production.yml my-custom.yml

# Edit configuration
vi my-custom.yml

# Deploy
./grafana-loki-v.1/generated/deploy.sh my-custom.yml
```

---

## Deployment Configurations

All deployment configurations are stored in:
```
grafana-loki-v.1/generated/deployments/
```

### Available Configurations

| File | Environment | Purpose |
|------|-------------|---------|
| `production.yml` | Production | Full deployment with monitoring, backups |
| `staging.yml` | Staging | Pre-production testing environment |
| `local-dev.yml` | Development | Local development setup |

### Configuration Format

All configurations use YAML format with the following structure:

```yaml
deployment:
  name: <deployment-name>
  version: v1.0
  environment: production|staging|development

target:
  host: <server-ip>
  port: 22

ssh:
  username: <user>
  password: <password>

container:
  name: <container-name>
  version: 1.0.0
  resources:
    memory: 2GB
    cpu: 2

storage:
  provider: btrfs
  quota: 30G

services:
  grafana:
    version: "10.2.3"
  loki:
    version: "2.9.4"
  promtail:
    version: "2.9.4"
```

---

## Artifact Management

Built container images are stored in:
```
grafana-loki-v.1/generated/artifact/
```

### Artifact Naming Convention

```
grafana-loki-v1-<timestamp>.tar.gz
grafana-loki-v1-<timestamp>.tar.gz.md5
```

Example:
```
grafana-loki-v1-1730489123.tar.gz
grafana-loki-v1-1730489123.tar.gz.md5
```

### Building New Artifacts

```bash
cd /home/mustafa/telcobright-projects/orchestrix/images/lxc/grafana-loki

# Build container
./build/build.sh

# Artifacts will be created in:
# grafana-loki-v.1/generated/artifact/
```

---

## Version Management

### Current Version: v1.0

**Location:** `grafana-loki-v.1/`

**Service Versions:**
- Grafana: 10.2.3
- Loki: 2.9.4
- Promtail: 2.9.4
- Debian: 12 (Bookworm)

### Future Versions

When creating v2.0:

```bash
# Create new version directory
mkdir -p grafana-loki-v.2/generated/{artifact,deployments}

# Copy deployment configs from v1.0
cp -r grafana-loki-v.1/generated/deployments/* \
      grafana-loki-v.2/generated/deployments/

# Update version in configs
# Edit grafana-loki-v.2/generated/deployments/*.yml

# Update build configuration
# Edit build/build.conf

# Build v2.0
./build/build.sh
```

---

## Directory Purpose

### `build/`
Contains build scripts and configuration for creating container images.

### `examples/`
Example deployment configurations and documentation for various scenarios.

### `grafana-loki-v.1/generated/`
Auto-generated deployment artifacts for version 1.0:
- **artifact/**: Built container images (.tar.gz)
- **deployments/**: Environment-specific configurations
- **deploy.sh**: Main deployment automation script

### `scripts/`
Helper scripts for maintenance and operations:
- Log rotation configuration
- Storage monitoring
- Service-specific scripts

### `templates/`
Configuration templates for new deployments.

---

## Integration with Orchestrix Automation

The deployment scripts integrate with the Orchestrix Java automation framework:

```
/home/mustafa/telcobright-projects/orchestrix/
├── automation/                          ← Java automation framework
│   └── src/main/java/.../grafanaloki/
│       └── GrafanaLokiDeployer.java    ← Main deployer class
│
└── images/lxc/grafana-loki/            ← This container
    └── grafana-loki-v.1/generated/
        └── deploy.sh                    ← Calls Java automation
```

When you run:
```bash
./grafana-loki-v.1/generated/deploy.sh --production
```

The script:
1. Validates prerequisites (Java, Maven)
2. Compiles automation framework (if needed)
3. Calls `GrafanaLokiDeployer.java` with configuration
4. Executes deployment steps via Java automation

---

## Best Practices

### 1. Version Isolation
- Keep each version in its own directory (`grafana-loki-v.X/`)
- Never mix artifacts from different versions
- Maintain separate deployment configs per version

### 2. Artifact Management
- Always verify MD5 checksums
- Keep old artifacts for rollback
- Clean up old artifacts periodically

### 3. Configuration Management
- Keep deployment configs in version control
- Never commit passwords (use `.gitignore`)
- Use environment-specific configs (prod, staging, dev)

### 4. Deployment Safety
- Always run `--dry-run` first
- Test in staging before production
- Keep previous artifacts for rollback

---

## Troubleshooting

### Missing Deployment Config

```bash
# List available configs
ls -la grafana-loki-v.1/generated/deployments/

# Copy example
cp grafana-loki-v.1/generated/deployments/production.yml my-config.yml
```

### Build Artifacts Not Found

```bash
# Check artifact directory
ls -la grafana-loki-v.1/generated/artifact/

# Rebuild if needed
./build/build.sh
```

### Deployment Script Not Executable

```bash
# Make executable
chmod +x grafana-loki-v.1/generated/deploy.sh
```

---

## Support

- **Deployment Guide**: `examples/README-DEPLOYMENT.md`
- **Container README**: `README.md`
- **Build Documentation**: `build/README.md` (if exists)
- **Scaffolding Standard**: `../CONTAINER_SCAFFOLDING_STANDARD.md`

---

## Changelog

### v1.0 (2025-11-01)
- Initial directory structure following go-id pattern
- Production, staging, and local-dev deployment configs
- Integrated deployment script with Java automation
- Multi-server deployment example
- Artifact management with MD5 checksums
