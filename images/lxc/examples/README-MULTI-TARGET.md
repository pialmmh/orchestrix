# Multi-Target Publish and Deployment Guide

## Overview

The Orchestrix automation framework supports publishing artifacts to multiple locations and deploying containers to multiple servers, with configurable parallel or sequential execution.

## Features

### Multi-Target Publishing
- Publish artifacts to multiple cloud storage providers simultaneously
- Supports: Google Drive, Dropbox, S3, and any rclone-supported backend
- Execution modes: PARALLEL (faster) or SEQUENTIAL (safer)
- MD5 verification for each publish location
- Detailed progress reporting

### Multi-Target Deployment
- Deploy containers to multiple servers in one command
- Perfect for multi-shard deployments
- Execution modes: PARALLEL (faster) or SEQUENTIAL (safer)
- Per-target container configuration
- Comprehensive deployment verification

## Configuration Format

### Publish Configuration

#### Multi-Target Format
```bash
# Execution mode
EXECUTION_MODE=PARALLEL  # or SEQUENTIAL

# Location 1
LOCATION_1_NAME=google-drive-primary
LOCATION_1_RCLONE_REMOTE=gdrive
LOCATION_1_RCLONE_TARGET_DIR=path/to/dir

# Location 2
LOCATION_2_NAME=dropbox-backup
LOCATION_2_RCLONE_REMOTE=dropbox
LOCATION_2_RCLONE_TARGET_DIR=another/path

# Add more locations as needed...
```

#### Single-Target Format (Backward Compatible)
```bash
# Simple single location
RCLONE_REMOTE=gdrive
RCLONE_TARGET_DIR=path/to/dir
```

### Deployment Configuration

#### Multi-Target Format
```bash
# Execution mode
EXECUTION_MODE=PARALLEL  # or SEQUENTIAL

# Target 1
TARGET_1_NAME=shard-0
TARGET_1_SERVER_IP=192.168.1.100
TARGET_1_SSH_USER=root
TARGET_1_SSH_PASSWORD=password
TARGET_1_CONTAINER_NAME=go-id-shard-0
TARGET_1_CONTAINER_IP=10.200.100.50

# Target 2
TARGET_2_NAME=shard-1
TARGET_2_SERVER_IP=192.168.1.101
TARGET_2_SSH_USER=root
TARGET_2_SSH_KEY_PATH=/path/to/key
TARGET_2_CONTAINER_NAME=go-id-shard-1
TARGET_2_CONTAINER_IP=10.200.100.51

# Add more targets as needed...
```

#### Single-Target Format (Backward Compatible)
```bash
SERVER_IP=192.168.1.100
SSH_USER=root
SSH_PASSWORD=password
CONTAINER_NAME=go-id-test
CONTAINER_IP=10.200.100.50
```

## Usage

### Publishing

#### Using Multi-Target Runner
```bash
cd /path/to/orchestrix
mvn exec:java \
    -Dexec.mainClass="com.telcobright.orchestrix.automation.publish.PublishRunner" \
    -Dexec.args="/path/to/publish-config.conf"
```

The PublishRunner automatically detects:
- Single-target configs (backward compatible)
- Multi-target configs (new format)

#### Example: Publish to 3 Locations in Parallel
```bash
# Edit config
EXECUTION_MODE=PARALLEL
LOCATION_1_NAME=gdrive-primary
LOCATION_1_RCLONE_REMOTE=gdrive
...
LOCATION_2_NAME=dropbox-backup
...
LOCATION_3_NAME=s3-archive
...

# Run publish
mvn exec:java \
    -Dexec.mainClass="com.telcobright.orchestrix.automation.publish.PublishRunner" \
    -Dexec.args="./publish-config.conf"
```

### Deployment

#### Using Multi-Target Runner
```bash
cd /path/to/orchestrix
mvn exec:java \
    -Dexec.mainClass="com.telcobright.orchestrix.automation.deploy.MultiDeploymentRunner" \
    -Dexec.args="/path/to/test-runner.conf"
```

#### Using Single-Target Runner (Backward Compatible)
```bash
cd /path/to/orchestrix
mvn exec:java \
    -Dexec.mainClass="com.telcobright.orchestrix.automation.deploy.DeploymentRunner" \
    -Dexec.args="/path/to/test-runner.conf"
```

#### Example: Deploy 3-Shard System in Parallel
```bash
# Edit config
EXECUTION_MODE=PARALLEL
TARGET_1_NAME=shard-0
TARGET_1_SERVER_IP=192.168.1.100
TARGET_1_CONTAINER_NAME=go-id-shard-0
...
TARGET_2_NAME=shard-1
TARGET_2_SERVER_IP=192.168.1.101
TARGET_2_CONTAINER_NAME=go-id-shard-1
...
TARGET_3_NAME=shard-2
TARGET_3_SERVER_IP=192.168.1.102
TARGET_3_CONTAINER_NAME=go-id-shard-2
...

# Run deployment
mvn exec:java \
    -Dexec.mainClass="com.telcobright.orchestrix.automation.deploy.MultiDeploymentRunner" \
    -Dexec.args="./test-runner.conf"
```

## Execution Modes

### PARALLEL
- Executes all targets simultaneously
- Faster completion time
- Uses thread pool for concurrent operations
- Best for: Production deployments, bulk operations

### SEQUENTIAL
- Executes targets one by one
- Easier to debug
- Clear error isolation
- Best for: Testing, troubleshooting, limited bandwidth

## Example Scenarios

### Scenario 1: Publish to Primary + Backup
```bash
EXECUTION_MODE=SEQUENTIAL
LOCATION_1_NAME=primary
LOCATION_1_RCLONE_REMOTE=gdrive
LOCATION_1_RCLONE_TARGET_DIR=production/artifacts

LOCATION_2_NAME=backup
LOCATION_2_RCLONE_REMOTE=gdrive-backup
LOCATION_2_RCLONE_TARGET_DIR=backups/artifacts
```

### Scenario 2: Deploy 5-Shard Microservice
```bash
EXECUTION_MODE=PARALLEL
# Configure TARGET_1 through TARGET_5
# Each with unique shard ID, IP, and server
```

### Scenario 3: Deploy to Development + Staging + Production
```bash
EXECUTION_MODE=SEQUENTIAL
TARGET_1_NAME=development
TARGET_1_SERVER_IP=192.168.1.10
...
TARGET_2_NAME=staging
TARGET_2_SERVER_IP=192.168.1.20
...
TARGET_3_NAME=production
TARGET_3_SERVER_IP=192.168.1.30
...
```

## Benefits

1. **Time Savings**: Parallel execution drastically reduces deployment time
2. **Consistency**: Same artifact deployed to all servers
3. **Reliability**: Built-in MD5 verification and error handling
4. **Flexibility**: Mix password and key-based authentication
5. **Visibility**: Detailed progress and summary reporting
6. **Safety**: Sequential mode for careful deployments

## Architecture

### Base Classes
- `MultiTargetExecutor<T>`: Generic executor for parallel/sequential tasks
- `Task<T>`: Functional interface for executable tasks
- `TaskResult<T>`: Result wrapper with success/failure info
- `ExecutionMode`: Enum for PARALLEL/SEQUENTIAL

### Publish Classes
- `PublishConfig`: Multi-target publish configuration
- `PublishRunner`: Entry point for publishing
- `PublishManager`: Handles actual publish operations

### Deploy Classes
- `MultiDeploymentConfig`: Multi-target deployment configuration
- `DeploymentTarget`: Single deployment target definition
- `MultiDeploymentRunner`: Entry point for multi-target deployment
- `DeploymentRunner`: Single-target deployment (backward compatible)
- `DeploymentManager`: Handles actual deployment operations

## Backward Compatibility

The system maintains full backward compatibility with existing single-target configurations:

- `PublishRunner` detects and handles both formats
- `DeploymentRunner` continues to work with single-target configs
- `MultiDeploymentRunner` can also handle single-target configs
- No changes required to existing scripts or configurations

## Summary Output

Both runners provide comprehensive summary:

```
========================================
Execution Summary
========================================
✓ google-drive-primary - SUCCESS (4523ms)
✓ dropbox-backup - SUCCESS (3891ms)
✗ s3-archive - FAILED (1205ms): Connection timeout
========================================
Total: 3 | Success: 2 | Failed: 1
Total time: 9619ms | Avg time: 3206ms
========================================
```
