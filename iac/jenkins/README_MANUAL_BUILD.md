# Clarity LXC Manual Build System

## ✅ Configuration Loaded
Your Jenkins configuration is loaded from `jenkins-config.yml`:
- **Jenkins URL**: http://172.82.66.90:8080
- **Username**: admin
- **API Token**: Configured ✓
- **rclone Remote**: pialmmhtb

## 🚀 Quick Start

### 1. Setup Jenkins Agent (One-time setup)
```bash
./setup-jenkins-agent.sh
# This will install Java, create agent directory, and setup systemd service
```

### 2. Install Jenkins Job
```bash
./install-jenkins-job.sh
# This creates the "clarity-lxc-manual-build" job in Jenkins
```

### 3. Trigger Manual Builds

#### Option A: Command Line (Recommended)
```bash
# Production release
./trigger-manual-build.sh -v 1.0.0 -t RELEASE -c -r "Initial production release"

# Development build (no upload)
./trigger-manual-build.sh -v 1.0.1 -t DEVELOPMENT -n

# Snapshot with wait for completion
./trigger-manual-build.sh -v 1.1.0-beta -t SNAPSHOT -w
```

#### Option B: Jenkins UI
1. Go to http://172.82.66.90:8080
2. Click on "clarity-lxc-manual-build"
3. Click "Build with Parameters"
4. Set version, type, and options
5. Click "Build"

## 📦 Version Structure

### Build Types
- **RELEASE**: Production-ready versions (uploaded to latest/)
- **SNAPSHOT**: Testing/preview versions
- **DEVELOPMENT**: Development builds

### Naming Convention
```
clarity-v{VERSION}-{BUILD_TYPE}-{TIMESTAMP}.tar.gz
Example: clarity-v1.0.0-RELEASE-20240901-143022.tar.gz
```

### Google Drive Organization
```
pialmmhtb:orchestrix/lxc-images/clarity/
├── 1.0.0/
│   ├── clarity-v1.0.0-RELEASE-*.tar.gz
│   ├── metadata.json
│   └── VERSION
├── 1.1.0/
├── latest/  (latest RELEASE only)
└── ...
```

## 🔧 Available Scripts

| Script | Purpose |
|--------|---------|
| `setup-jenkins-agent.sh` | Setup PC as Jenkins agent |
| `install-jenkins-job.sh` | Create Jenkins job |
| `trigger-manual-build.sh` | Trigger versioned builds |
| `test-setup.sh` | Verify environment |

## 📝 Build Parameters

| Parameter | Description | Default |
|-----------|-------------|---------|
| VERSION | Version number (e.g., 1.0.0) | 1.0.0 |
| BUILD_TYPE | RELEASE/SNAPSHOT/DEVELOPMENT | DEVELOPMENT |
| CLEAN_BUILD | Delete existing container | true |
| UPLOAD_TO_GDRIVE | Upload to Google Drive | true |
| KEEP_CONTAINER_RUNNING | Keep container active | true |
| RELEASE_NOTES | Version notes | (optional) |

## 🔍 Monitoring

- **Build History**: http://172.82.66.90:8080/job/clarity-lxc-manual-build/
- **Container Status**: `sudo lxc list clarity`
- **Google Drive**: Check uploads with `rclone ls pialmmhtb:orchestrix/lxc-images/clarity/`

## ⚡ Quick Commands

```bash
# Check last build
curl -s -u admin:11b7d0764a66a46cdddd2e124cf138fae9 \
  http://172.82.66.90:8080/job/clarity-lxc-manual-build/lastBuild/api/json | jq .result

# List versions in Google Drive
rclone ls pialmmhtb:orchestrix/lxc-images/clarity/ --max-depth 1

# Check container
sudo lxc list clarity
```

## 🆘 Troubleshooting

1. **Jenkins not reachable**: Check firewall, Jenkins service
2. **Agent won't connect**: Verify agent secret in Jenkins UI
3. **Upload fails**: Test with `rclone ls pialmmhtb:`
4. **Build fails**: Check sudo permissions for LXC

---
Configuration is loaded from `jenkins-config.yml` - all scripts use this automatically!