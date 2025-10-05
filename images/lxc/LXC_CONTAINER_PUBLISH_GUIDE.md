# LXC Container Publish Guide

## Overview

This guide explains how to publish LXC container images to Google Drive using the automated publish workflow in Orchestrix. The system provides:

- **MD5 verification** - Automatic hash generation and verification
- **Database tracking** - Full artifact inventory and publish records
- **Java automation** - Robust publish workflow using PublishManager
- **Rclone integration** - Google Drive upload with progress tracking
- **Mirrored directory structure** - Google Drive mirrors local build structure

## Directory Structure

### Local Structure
```
orchestrix/
└── images/
    └── lxc/
        └── go-id/
            ├── build/              # Git tracked - Build configuration
            │   ├── build.sh
            │   └── build.conf
            ├── templates/          # Git tracked - Template files
            │   ├── sample.conf
            │   └── startDefault.sh
            ├── .gitignore          # Excludes go-id-v.* directories
            └── go-id-v.1/          # Generated - Not in git
                └── generated/
                    ├── go-id-v1-TIMESTAMP.tar.gz
                    ├── go-id-v1-TIMESTAMP.tar.gz.md5
                    ├── sample.conf
                    ├── startDefault.sh
                    ├── publish.sh
                    └── publish-config.conf
```

### Google Drive Structure (Mirrored)
```
gdrive:
└── images/
    └── lxc/
        └── go-id/
            └── go-id-v.1/
                └── generated/
                    ├── go-id-v1-TIMESTAMP.tar.gz
                    └── go-id-v1-TIMESTAMP.tar.gz.md5
```

**Key Points:**
1. **Build configuration** (`build/`, `templates/`) is backed up to Git
2. **Generated artifacts** (`go-id-v.*/`) are excluded from Git via `.gitignore`
3. **Google Drive path** mirrors the local structure from `images/lxc/...`
4. Only the container image and MD5 hash are uploaded (not scripts)

## Architecture

### Publish Workflow

```
1. Build Container
   ↓
2. Export to generated/ folder
   ↓
3. Generate MD5 hash file
   ↓
4. Create publish.sh (calls Java/Maven)
   ↓
5. User runs publish.sh
   ↓
6. Prompt for upload confirmation
   ↓
7. Java automation executes:
   - Create artifact record in DB
   - Upload to Google Drive via rclone
   - Download to temp for verification
   - Verify MD5 hash matches
   - Delete temp copy
   - Update publish record
```

### Database Schema

**publish_location** (singular)
- Stores rclone remote configurations
- Type: gdrive (Google Drive via rclone)
- Contains remote name and target directory

**artifact** (singular)
- Stores artifact metadata (image name, size, MD5, path)
- Tracks artifact type (lxc-container), name, version
- Build timestamp for traceability

**artifact_publish** (singular)
- Links artifacts to publish locations
- Tracks verification status and timestamp
- Prevents duplicate publishes

## Prerequisites

### 1. Rclone Setup

Install rclone:
```bash
curl https://rclone.org/install.sh | sudo bash
```

Configure Google Drive remote:
```bash
rclone config

# Follow prompts:
# - Choose: n (New remote)
# - Name: gdrive
# - Storage: drive (Google Drive)
# - Complete OAuth flow
```

Verify remote:
```bash
rclone listremotes
# Should show: gdrive:
```

### 2. Database Setup

Run the schema migration:
```bash
mysql -h 127.0.0.1 -u root -p orchestrix < src/main/resources/db/migration/artifact_publish_schema.sql
```

Verify tables:
```bash
mysql -h 127.0.0.1 -u root -p orchestrix -e "SHOW TABLES LIKE '%publish%'; SHOW TABLES LIKE 'artifact';"
```

Expected tables:
- `publish_location`
- `artifact`
- `artifact_publish`

### 3. Publish Location Configuration

The default publish location is auto-created during schema migration:

```sql
-- Default location
INSERT IGNORE INTO publish_location (name, type, rclone_remote, rclone_target_dir, is_active)
VALUES ('Default Google Drive', 'gdrive', 'gdrive', 'lxc-containers', TRUE);
```

To add custom locations:
```sql
INSERT INTO publish_location (name, type, rclone_remote, rclone_target_dir, is_active)
VALUES ('Production GDrive', 'gdrive', 'gdrive-prod', 'production/lxc-containers', TRUE);
```

## Build Configuration

Update `build/build.conf` to enable publish automation:

```bash
# Publish Configuration
RCLONE_REMOTE="gdrive"
# Note: Target directory will mirror local structure automatically
# Result: gdrive:images/lxc/go-id/go-id-v.X/generated
GENERATE_UPLOAD_SCRIPT="true"  # Enable publish script generation
```

The publish system automatically constructs the Google Drive path to mirror the local structure:
- **Local**: `.../orchestrix/images/lxc/go-id/go-id-v.1/generated/`
- **GDrive**: `gdrive:images/lxc/go-id/go-id-v.1/generated/`

## Build Process

### 1. Run Container Build

The build script (`build.sh`) encapsulates all build automation (Maven/Java internally):

```bash
cd /home/mustafa/telcobright-projects/orchestrix/images/lxc/go-id
./build/build.sh

# Or with custom config:
./build/build.sh /path/to/custom-build.conf
```

**What build.sh does:**
1. Loads configuration from `build.conf`
2. Compiles Java automation if needed
3. Runs SSH-based container build via Maven exec
4. Creates versioned output in `go-id-v.X/generated/`

### 2. Generated Artifacts

After build completes, check the versioned `generated/` folder:

```bash
ls -lh go-id-v.1/generated/

# Output:
# go-id-v1-1749837291.tar.gz
# go-id-v1-1749837291.tar.gz.md5
# sample.conf
# startDefault.sh
# publish.sh
# publish-config.conf
```

### 3. Verify MD5 Hash

```bash
cd go-id-v.1/generated/
md5sum -c go-id-v1-*.tar.gz.md5

# Output should show: OK
```

## Publish Process

### 1. Review Publish Configuration

```bash
cat go-id-v.1/generated/publish-config.conf
```

Expected content:
```bash
# Artifact Information
ARTIFACT_TYPE=lxc-container
ARTIFACT_NAME=go-id
ARTIFACT_VERSION=v1
ARTIFACT_FILE=go-id-v1-1749837291.tar.gz
ARTIFACT_PATH=/path/to/go-id-v.1/generated/go-id-v1-1749837291.tar.gz
BUILD_TIMESTAMP=1749837291

# Publish Location (mirrors local structure)
RCLONE_REMOTE=gdrive
RCLONE_TARGET_DIR=images/lxc/go-id/go-id-v.1/generated

# Database Connection
DB_HOST=127.0.0.1
DB_PORT=3306
DB_NAME=orchestrix
DB_USER=root
DB_PASSWORD=123456
```

### 2. Run Publish Script

```bash
cd go-id-v.1/generated/
./publish.sh
```

### 3. Publish Workflow

The script will:

1. **Display artifact information:**
   ```
   === LXC Container Publish ===

   Artifact Type:    lxc-container
   Name:             go-id
   Version:          v1
   File:             go-id-v1-1749837291.tar.gz
   Build Timestamp:  1749837291

   Publish Location: gdrive:images/lxc/go-id/go-id-v.1/generated
   ```

2. **Prompt for confirmation:**
   ```
   Do you want to upload this artifact? (y/N):
   ```

3. **Execute Java automation:**
   - Navigate to Orchestrix project
   - Run Maven exec:java with PublishRunner
   - PublishRunner loads config and executes PublishManager

4. **Publish automation steps:**
   ```
   [1/4] Uploading artifact...
   ✓ Upload completed: gdrive:images/lxc/go-id/go-id-v.1/generated/go-id-v1-1749837291.tar.gz

   [2/4] Downloading for verification...
   ✓ Download completed: /tmp/verify-go-id-v1-1749837291.tar.gz

   [3/4] Verifying MD5 hash...
   ✓ MD5 verification successful: go-id-v1-1749837291.tar.gz

   [4/4] Cleaning up temp file...
   ✓ Temp file deleted

   === Publish Successful ===
   ✓ Remote Path: gdrive:images/lxc/go-id/go-id-v.1/generated/go-id-v1-1749837291.tar.gz
   ✓ MD5 Verified: true
   ```

## Java Automation Components

### 1. PublishManager

Main orchestrator in `com.telcobright.orchestrix.automation.publish.PublishManager`:

**Key Methods:**
- `createArtifact()` - Create artifact metadata with MD5 hash
- `generateHashFile()` - Write .md5 file
- `validatePublishLocation()` - Check rclone remote exists
- `publishArtifact()` - Execute full publish workflow with verification

**Workflow:**
```java
// 1. Validate publish location
publishManager.validatePublishLocation(publishLocation);

// 2. Upload
remotePath = rcloneManager.upload(artifactFile, remote, targetDir);

// 3. Download to temp
tempFile = File.createTempFile("verify-", artifact.getFileName());
rcloneManager.download(remotePath, tempFile);

// 4. Verify MD5
boolean verified = MD5Util.verify(tempFile, artifact.getMd5Hash());

// 5. Cleanup
tempFile.delete();

// 6. Update DB
publishRecord.setVerified(verified);
artifactPublishDAO.save(publishRecord);
```

### 2. RcloneManager

Handles all rclone operations in `com.telcobright.orchestrix.automation.publish.util.RcloneManager`:

**Key Methods:**
- `isRcloneInstalled()` - Check rclone availability
- `listRemotes()` - Get configured remotes
- `validateRemote(name)` - Verify remote exists
- `upload(file, remote, dir)` - Upload with progress
- `download(remotePath, localFile)` - Download for verification
- `remoteFileExists(path)` - Check file existence
- `delete(remotePath)` - Remove remote file

### 3. MD5Util

MD5 hash utilities in `com.telcobright.orchestrix.automation.publish.util.MD5Util`:

**Key Methods:**
- `generate(file)` - Generate MD5 hash
- `verify(file, expectedHash)` - Verify file matches hash
- `writeHashFile(file)` - Create .md5 file (md5sum format)
- `readHashFile(path)` - Parse .md5 file

## Database Queries

### Check Published Artifacts

```sql
SELECT
    a.artifact_type,
    a.name,
    a.version,
    a.file_name,
    a.md5_hash,
    pl.name as location_name,
    ap.remote_path,
    ap.verified,
    ap.published_at,
    ap.verification_timestamp
FROM artifact a
JOIN artifact_publish ap ON a.id = ap.artifact_id
JOIN publish_location pl ON ap.publish_location_id = pl.id
ORDER BY ap.published_at DESC;
```

### Check Publish Locations

```sql
SELECT * FROM publish_location WHERE is_active = TRUE;
```

### Find Artifacts by Type

```sql
SELECT * FROM artifact
WHERE artifact_type = 'lxc-container'
ORDER BY build_timestamp DESC;
```

## Verification

### 1. Check Remote File

```bash
rclone ls gdrive:images/lxc/go-id/go-id-v.1/generated/
```

### 2. Download and Verify

```bash
# Download
rclone copy gdrive:images/lxc/go-id/go-id-v.1/generated/go-id-v1-*.tar.gz ./download/

# Verify MD5
cd download/
md5sum go-id-v1-*.tar.gz
# Compare with hash in database
```

### 3. Check Database Records

```bash
mysql -h 127.0.0.1 -u root -p orchestrix -e "
SELECT a.file_name, a.md5_hash, ap.verified, ap.remote_path
FROM artifact a
JOIN artifact_publish ap ON a.id = ap.artifact_id
WHERE a.file_name LIKE 'go-id%'
ORDER BY ap.published_at DESC LIMIT 1;"
```

## Troubleshooting

### Issue: Rclone Remote Not Found

**Error:**
```
Error: Rclone remote 'gdrive' not found
```

**Solution:**
```bash
# List configured remotes
rclone listremotes

# If missing, configure
rclone config
```

### Issue: MD5 Verification Failed

**Error:**
```
MD5 verification FAILED - upload corrupted!
```

**Solution:**
1. Check network connection
2. Re-upload with `--checksum` flag
3. Verify local file MD5:
   ```bash
   md5sum generated/go-id-v1-*.tar.gz
   cat generated/go-id-v1-*.tar.gz.md5
   ```

### Issue: Database Connection Failed

**Error:**
```
Could not connect to database
```

**Solution:**
```bash
# Verify MySQL is running
mysql -h 127.0.0.1 -u root -p

# Check credentials in publish-config.conf
# Update DB_PASSWORD if needed
```

### Issue: Maven Execution Failed

**Error:**
```
mvn exec:java ... failed
```

**Solution:**
```bash
# Navigate to project root
cd /home/mustafa/telcobright-projects/orchestrix

# Compile first
mvn clean compile

# Run publish manually
mvn exec:java \
  -Dexec.mainClass="com.telcobright.orchestrix.automation.publish.PublishRunner" \
  -Dexec.args="/path/to/publish-config.conf"
```

## Distribution

### Distributing Published Containers

1. **Share Google Drive link:**
   - Generate shareable link in Google Drive
   - Share with team members

2. **Download on target system:**
   ```bash
   # Using rclone (mirrored structure)
   rclone copy gdrive:images/lxc/go-id/go-id-v.1/generated/go-id-v1-*.tar.gz ./

   # Using shared link (if configured)
   wget "https://drive.google.com/uc?export=download&id=FILE_ID" -O go-id-v1.tar.gz
   ```

3. **Verify download:**
   ```bash
   # If .md5 file is available
   md5sum -c go-id-v1-*.tar.gz.md5

   # Or verify against database
   mysql -h 127.0.0.1 -u root -p orchestrix -e "
   SELECT md5_hash FROM artifact WHERE file_name = 'go-id-v1-*.tar.gz';"
   ```

4. **Import and launch:**
   ```bash
   # Extract sample.conf and startDefault.sh from published artifact
   # Or use launchGoId.sh with custom config

   lxc image import go-id-v1-*.tar.gz --alias go-id-base
   ./launchGoId.sh my-config.conf
   ```

## Git Backup Strategy

### What is Backed Up to Git

**Tracked in Git:**
- `build/` - Build configuration and scripts
- `templates/` - Template files for generated artifacts
- `.gitignore` - Excludes generated artifacts

**Example `.gitignore`:**
```
# Exclude all versioned build artifacts
go-id-v.*

# Keep build configuration and templates
!build/
!templates/
```

### What is NOT in Git

**Generated artifacts** (excluded via `.gitignore`):
- `go-id-v.1/` - All versioned output directories
- `go-id-v.2/` - Future versions
- All files in `generated/` folders

**Why:**
1. Build artifacts are large binary files
2. They can be regenerated from source
3. They are stored in Google Drive for distribution
4. Git is optimized for source code, not binaries

### Backup Locations

| Content | Git | Google Drive | Local |
|---------|-----|--------------|-------|
| Build config (`build/`) | ✓ | ✗ | ✓ |
| Templates (`templates/`) | ✓ | ✗ | ✓ |
| Container images | ✗ | ✓ | ✓ |
| MD5 hashes | ✗ | ✓ | ✓ |
| Launch scripts (generated) | ✗ | ✗ | ✓ |

## Best Practices

### 1. Version Management
- Use semantic versioning (v1, v2, v3)
- Include build timestamp in filename
- Keep version history in database
- Each version gets its own directory (`go-id-v.1/`, `go-id-v.2/`)

### 2. Security
- Never commit database passwords to git
- Use environment variables for sensitive data
- Restrict Google Drive access appropriately

### 3. Storage Management
- Regularly clean old artifacts from Google Drive
- Archive historical versions
- Monitor storage usage

### 4. Verification
- Always verify MD5 after download
- Check database records for publish history
- Test container launch after distribution

## Quick Reference

### Build and Publish Workflow

```bash
# 1. Configure build
vim images/lxc/go-id/build/build.conf

# 2. Build container (build.sh encapsulates Maven/Java)
cd images/lxc/go-id
./build/build.sh

# 3. Verify generated artifacts
ls -lh go-id-v.1/generated/

# 4. Publish to Google Drive
cd go-id-v.1/generated/
./publish.sh

# 5. Verify in database
mysql -h 127.0.0.1 -u root -p orchestrix -e "
SELECT * FROM artifact_publish ORDER BY published_at DESC LIMIT 1;"
```

**Note:** `build.sh` is the universal entry point. It internally:
- Compiles Java automation if needed
- Runs Maven exec with proper configuration
- No need to call Maven directly

### Key Files

- **Build Config**: `build/build.conf`
- **Templates**: `templates/sample.conf`, `templates/startDefault.sh`
- **Versioned Output**: `go-id-v.X/generated/`
- **Publish Script**: `go-id-v.X/generated/publish.sh`
- **Publish Config**: `go-id-v.X/generated/publish-config.conf`
- **MD5 Hash**: `go-id-v.X/generated/[image].tar.gz.md5`
- **Launch Config**: `go-id-v.X/generated/sample.conf`
- **Quick Start**: `go-id-v.X/generated/startDefault.sh`

---

**This guide provides complete instructions for publishing LXC containers in Orchestrix.**

For container scaffolding details, see: [LXC_CONTAINER_SCAFFOLDING_GUIDE.md](LXC_CONTAINER_SCAFFOLDING_GUIDE.md)
