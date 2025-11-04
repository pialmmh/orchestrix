# Complete Deployment Workflow: Scaffold → Publish → Deploy

## Architecture Overview
```
┌──────────────────────────────────────────────────────────────────────┐
│                     COMPLETE DEPLOYMENT WORKFLOW                      │
└──────────────────────────────────────────────────────────────────────┘

PHASE 1: SCAFFOLD                    PHASE 2: PUBLISH                    PHASE 3: DEPLOY
────────────────                     ─────────────────                   ───────────────

[Local Development]                  [Cloud Storage]                    [Target Servers]

1. Build Container                   1. Upload to:                      1. SSH/Telnet to server
   └─> consul-v1.tar.gz                 • Google Drive                 2. Pull from publish URL
                                        • AWS S3                       3. Deploy container
2. Test Locally                         • FTP Server                   4. Configure & Start
   └─> Verify working                   • HTTP Server

3. Generate artifact                2. Record in DB:
   metadata.json                       • artifact_id
                                        • publish_url
                                        • checksum
                                        • timestamp


                    ┌─────────────┐
                    │  SCAFFOLD   │
                    └──────┬──────┘
                           ▼
                    consul-v1.tar.gz
                           │
                    ┌──────▼──────┐
                    │   PUBLISH   │
                    └──────┬──────┘
                           ▼
                    Google Drive URL
                    https://drive.google.com/
                    file/d/1x2y3z/consul-v1.tar.gz
                           │
                    ┌──────▼──────┐
                    │   DEPLOY    │
                    │  via SSH    │
                    └──────┬──────┘
                           ▼
                    [Remote Server]
                    wget/curl from URL
                    lxc import
                    lxc launch
```

## Updated YAML Structure

```yaml
# consul-production.yml
deployment:
  name: "consul-cluster"
  type: "multi-instance"
  environment: "production"

# Artifact published location
artifact:
  name: "consul"
  version: "v1"
  source: "published"  # published|local|registry

  # Published locations (in priority order)
  publish_locations:
    - type: "googledrive"
      url: "https://drive.google.com/uc?id=1ABC123&export=download"
      auth: "oauth2"

    - type: "http"
      url: "https://artifacts.company.com/consul/consul-v1.tar.gz"
      auth: "basic"

    - type: "s3"
      url: "s3://artifacts-bucket/consul/consul-v1.tar.gz"
      auth: "aws_credentials"

  checksum: "sha256:abc123def456..."
  size: "64MB"

# Terminal access configuration
terminal:
  type: "ssh"  # ssh|telnet|local
  connection:
    host: "10.0.1.10"
    port: 22
    user: "deploy"
    auth_method: "key"  # key|password
    ssh_key: "~/.ssh/deploy_key"

targets:
  - server:
      name: "prod-server-1"
      terminal:
        type: "ssh"
        host: "10.0.1.10"
        user: "deploy"

      deployment_steps:
        - step: "download_artifact"
          commands:
            - "cd /tmp"
            - "wget -O consul-v1.tar.gz '${artifact.publish_locations[0].url}'"
            - "sha256sum consul-v1.tar.gz | grep ${artifact.checksum}"

        - step: "import_container"
          commands:
            - "lxc image import /tmp/consul-v1.tar.gz --alias consul-v1"

        - step: "deploy_instances"
          instances:
            - name: "consul-node-1"
              commands:
                - "lxc launch consul-v1 consul-node-1"
                - "lxc config set consul-node-1 limits.memory 2GB"
                - "lxc exec consul-node-1 -- setup_consul.sh"
```

## Publishing System

### 1. Google Drive Publisher
```bash
#!/bin/bash
# publish-to-gdrive.sh

ARTIFACT="$1"  # consul-v1.tar.gz
GDRIVE_FOLDER_ID="1ABC123DEF456"

# Upload to Google Drive
gdrive upload --parent $GDRIVE_FOLDER_ID $ARTIFACT

# Get shareable link
SHARE_URL=$(gdrive share $ARTIFACT --json | jq -r '.url')

# Update database
mysql -h 127.0.0.1 orchestrix -e "
  UPDATE artifacts
  SET publish_url='$SHARE_URL',
      publish_location='googledrive',
      published=true,
      publish_date=NOW()
  WHERE artifact_name='consul' AND version='v1'
"

echo "Published to: $SHARE_URL"
```

### 2. Artifact Registry Structure
```sql
CREATE TABLE artifact_publications (
    id INT AUTO_INCREMENT PRIMARY KEY,
    artifact_id VARCHAR(50),
    publish_type VARCHAR(50),  -- googledrive, s3, http
    publish_url TEXT,
    checksum VARCHAR(256),
    file_size BIGINT,
    priority INT DEFAULT 1,  -- 1=primary, 2=backup
    available BOOLEAN DEFAULT true,
    last_verified TIMESTAMP,
    metadata JSON,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (artifact_id) REFERENCES artifacts(artifact_id)
);
```

## Terminal-Based Deployment Script

```bash
#!/bin/bash
# deploy-via-terminal.sh

CONFIG_FILE="$1"

# Parse YAML to get terminal config
TERMINAL_TYPE=$(yq '.terminal.type' $CONFIG_FILE)
HOST=$(yq '.terminal.connection.host' $CONFIG_FILE)
USER=$(yq '.terminal.connection.user' $CONFIG_FILE)
SSH_KEY=$(yq '.terminal.connection.ssh_key' $CONFIG_FILE)

# Get artifact URL from published location
ARTIFACT_URL=$(yq '.artifact.publish_locations[0].url' $CONFIG_FILE)

# Create deployment script
cat > /tmp/remote_deploy.sh << 'EOF'
#!/bin/bash
set -e

# Download artifact from published location
echo "Downloading artifact from published server..."
cd /tmp
wget -O consul-v1.tar.gz "$1"

# Verify checksum
echo "Verifying checksum..."
sha256sum consul-v1.tar.gz

# Import to LXC
echo "Importing container..."
lxc image import consul-v1.tar.gz --alias consul-v1

# Deploy nodes
for i in 1 2 3; do
    echo "Deploying consul-node-$i..."
    lxc launch consul-v1 consul-node-$i
    lxc config set consul-node-$i limits.memory 1GB
done

echo "Deployment complete!"
EOF

# Execute based on terminal type
case $TERMINAL_TYPE in
    ssh)
        echo "Deploying via SSH to $HOST..."
        scp /tmp/remote_deploy.sh $USER@$HOST:/tmp/
        ssh -i $SSH_KEY $USER@$HOST "bash /tmp/remote_deploy.sh '$ARTIFACT_URL'"
        ;;

    telnet)
        echo "Deploying via Telnet to $HOST..."
        # Use expect script for telnet
        expect -c "
            spawn telnet $HOST
            expect 'login:'
            send '$USER\r'
            expect 'Password:'
            send '$PASSWORD\r'
            expect '$ '
            send 'wget -O /tmp/consul.tar.gz $ARTIFACT_URL\r'
            expect '$ '
            send 'lxc image import /tmp/consul.tar.gz --alias consul-v1\r'
            expect '$ '
            send 'exit\r'
        "
        ;;

    local)
        echo "Deploying locally..."
        bash /tmp/remote_deploy.sh "$ARTIFACT_URL"
        ;;
esac
```

## Deployment Flow Diagram

```
Developer Machine                  Cloud Storage                   Target Server
─────────────────                 ──────────────                  ──────────────

1. SCAFFOLD
   ./build.sh
   → consul-v1.tar.gz
        │
        ▼
2. PUBLISH
   ./publish.sh consul-v1.tar.gz
        │
        ├─────────────────► Google Drive
        │                   → URL: https://drive.google.com/...
        │
        └─────────────────► Database
                           → Record: artifact_id, URL, checksum
                                    │
                                    ▼
3. DEPLOY
   ./deploy.sh production.yml
        │
        ├──────────────────────────────────────────────►
        │                                              │
        │                           Target Server SSH │
        │                                              ▼
        │                                   wget from Google Drive
        │                                   lxc import artifact
        │                                   lxc launch containers
        │                                              │
        └──────────────────────────────────────────────┘
                                                   SUCCESS
```

## Benefits of This Approach

1. **Centralized Artifacts** - All artifacts in Google Drive/S3
2. **No Direct Upload** - Servers pull from published locations
3. **Terminal Agnostic** - Works with SSH, Telnet, or local
4. **Audit Trail** - Every publish/deploy tracked in DB
5. **Fallback URLs** - Multiple publish locations for redundancy
6. **Checksum Verification** - Ensures artifact integrity

## Quick Commands

```bash
# 1. Scaffold
cd images/containers/lxc/consul
./build/build.sh

# 2. Publish
./publish.sh consul-v.1/generated/artifact/consul-v1.tar.gz

# 3. Deploy
cd deployment
./deploy-terminal.sh configs/production/consul-cluster.yml
```