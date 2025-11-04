# Deployment Automation Structure

## Overview
Config-driven deployment system that supports three scenarios:
1. Single artifact, single deployment
2. Single artifact, multiple deployments (same server, different params)
3. Single artifact, cluster deployment

## Directory Structure
```
/home/mustafa/telcobright-projects/orchestrix/deployment/
├── configs/                    # Deployment configurations
│   ├── templates/              # Config templates
│   │   ├── single-deploy.yaml
│   │   ├── multi-instance.yaml
│   │   └── cluster-deploy.yaml
│   ├── environments/           # Environment-specific configs
│   │   ├── dev/
│   │   ├── staging/
│   │   └── production/
│   └── active/                 # Currently active deployments
│
├── manifests/                  # Artifact manifests from DB
│   ├── binaries/              # Binary artifact manifests
│   └── containers/            # Container image manifests
│
├── scripts/                   # Deployment scripts
│   ├── deploy.sh              # Main deployment entry point
│   ├── validate.sh            # Config validation
│   └── rollback.sh            # Rollback mechanism
│
├── java/                      # Java automation classes
│   ├── DeploymentManager.java
│   ├── ConfigLoader.java
│   ├── ArtifactTracker.java
│   └── DeploymentExecutor.java
│
├── logs/                      # Deployment logs
│   └── deployments/
│
└── state/                     # Deployment state tracking
    ├── current/               # Current deployment states
    └── history/               # Historical deployments
```

## Configuration Schema

### Base Deployment Config (YAML)
```yaml
deployment:
  name: "service-name"
  version: "1.0"
  type: "single|multi-instance|cluster"
  environment: "dev|staging|production"

artifact:
  type: "binary|container"
  name: "go-id-service"
  version: "v1"
  source: "database|local|registry"
  artifact_id: "DB_ARTIFACT_ID"  # From database tracking

targets:
  - server:
      host: "192.168.1.100"
      user: "deploy"
      ssh_key: "/path/to/key"
    instances:
      - name: "instance-1"
        params:
          port: 8080
          memory: "512M"
          env_vars:
            NODE_ENV: "production"

monitoring:
  health_check_url: "http://{host}:{port}/health"
  startup_timeout: 60

rollback:
  enabled: true
  keep_versions: 3
```

## Deployment Scenarios

### 1. Single Deployment
```yaml
# configs/environments/dev/go-id-single.yaml
deployment:
  name: "go-id-service"
  type: "single"

artifact:
  type: "binary"
  name: "go-id"
  version: "v1"
  artifact_id: "GOID_BIN_001"

targets:
  - server:
      host: "localhost"
    instances:
      - name: "go-id-main"
        params:
          port: 8080
```

### 2. Multi-Instance (Same Server)
```yaml
# configs/environments/dev/consul-multi.yaml
deployment:
  name: "consul-nodes"
  type: "multi-instance"

artifact:
  type: "container"
  name: "consul"
  version: "v1"
  artifact_id: "CONSUL_LXC_001"

targets:
  - server:
      host: "localhost"
    instances:
      - name: "consul-node-1"
        params:
          port: 8500
          node_id: "node-1"
      - name: "consul-node-2"
        params:
          port: 8510
          node_id: "node-2"
      - name: "consul-node-3"
        params:
          port: 8520
          node_id: "node-3"
```

### 3. Cluster Deployment
```yaml
# configs/environments/production/app-cluster.yaml
deployment:
  name: "app-cluster"
  type: "cluster"

artifact:
  type: "container"
  name: "app-service"
  version: "v2"
  artifact_id: "APP_LXC_002"

targets:
  - server:
      host: "10.0.1.10"
    instances:
      - name: "app-node-1"
        params:
          port: 8080
          role: "master"
  - server:
      host: "10.0.1.11"
    instances:
      - name: "app-node-2"
        params:
          port: 8080
          role: "worker"
  - server:
      host: "10.0.1.12"
    instances:
      - name: "app-node-3"
        params:
          port: 8080
          role: "worker"
```

## Database Integration

### Artifact Tracking Table
```sql
CREATE TABLE artifacts (
    artifact_id VARCHAR(50) PRIMARY KEY,
    type ENUM('binary', 'container'),
    name VARCHAR(100),
    version VARCHAR(20),
    path VARCHAR(500),
    checksum VARCHAR(64),
    metadata JSON,
    created_at TIMESTAMP,
    published BOOLEAN,
    publish_date TIMESTAMP
);

CREATE TABLE deployments (
    deployment_id VARCHAR(50) PRIMARY KEY,
    artifact_id VARCHAR(50),
    config_file VARCHAR(500),
    environment VARCHAR(20),
    status ENUM('pending','running','success','failed','rolled_back'),
    started_at TIMESTAMP,
    completed_at TIMESTAMP,
    deployment_log TEXT,
    FOREIGN KEY (artifact_id) REFERENCES artifacts(artifact_id)
);
```

## Java Classes Structure

### DeploymentManager.java
```java
package automation.deployment;

public class DeploymentManager {
    private ConfigLoader configLoader;
    private ArtifactTracker artifactTracker;
    private DeploymentExecutor executor;

    public DeploymentResult deploy(String configFile) {
        // 1. Load and validate config
        DeploymentConfig config = configLoader.load(configFile);

        // 2. Fetch artifact info from DB
        Artifact artifact = artifactTracker.getArtifact(config.getArtifactId());

        // 3. Execute deployment based on type
        switch(config.getType()) {
            case SINGLE:
                return executor.deploySingle(config, artifact);
            case MULTI_INSTANCE:
                return executor.deployMultiInstance(config, artifact);
            case CLUSTER:
                return executor.deployCluster(config, artifact);
        }
    }
}
```

## Shell Script Entry Points

### deploy.sh
```bash
#!/bin/bash
# Main deployment script
CONFIG_FILE=$1
ENVIRONMENT=${2:-dev}

java -cp /path/to/automation.jar \
     automation.deployment.DeploymentRunner \
     --config "$CONFIG_FILE" \
     --env "$ENVIRONMENT"
```

## Future UI Integration Points

1. **Config Generation**: UI will generate YAML configs in `configs/active/`
2. **Status API**: Java classes expose REST endpoints for deployment status
3. **Log Streaming**: Deployment logs available via WebSocket
4. **Rollback UI**: Trigger rollback through UI calling existing scripts
5. **Database Dashboard**: Show artifact tracking and deployment history

## Benefits of This Structure

1. **Separation of Concerns**: Config, logic, and data clearly separated
2. **UI-Ready**: Structure can be directly used by future UI
3. **Database Integration**: Tracks artifacts and deployments
4. **Scalable**: Handles all three deployment scenarios
5. **Auditable**: Complete deployment history and logs
6. **Rollback Support**: Built-in rollback mechanism