# Clarity Implementation Roadmap

## Quick Start Guide

### MVP Features (Week 1-2)
1. **Basic Agent**
   - Download artifacts from Google Drive
   - Apply simple configurations
   - Start/stop services
   - Report status

2. **Configuration Structure**
   - YAML-based configuration files
   - Profile management (dev/staging/prod/mock)
   - Client-artifact mapping

3. **Google Drive Repository**
   - OAuth2 authentication
   - File download capability
   - Folder structure navigation

### Initial Implementation Steps

## 1. Agent Development (Orchestrix Agent)

### Core Agent Features
```go
// agent/main.go
type Agent struct {
    ServerID     string
    ConfigPath   string
    CacheDir     string
    Repositories map[string]Repository
    Deployments  map[string]Deployment
}

type Deployment struct {
    ArtifactID   string
    Profile      string
    Version      string
    Status       DeploymentStatus
    ConfigPath   string
}
```

### Agent Workflow Implementation
```yaml
# agent-workflow.yml
workflow:
  1_download:
    input: 
      - artifact_id
      - repository_config
      - profile_name
    actions:
      - authenticate_repository
      - download_artifact
      - verify_checksum
      - extract_if_needed
    output: local_artifact_path

  2_configure:
    input:
      - artifact_path
      - profile_config
    actions:
      - download_profile_config
      - apply_env_variables
      - deploy_config_files
      - setup_volumes
    output: configured_artifact

  3_autostart:
    input:
      - artifact_type
      - artifact_path
    actions:
      - create_systemd_service
      - enable_service
      - configure_restart_policy
    output: service_name

  4_run:
    input:
      - service_name
    actions:
      - start_service
      - monitor_startup
      - report_status
    output: deployment_status
```

## 2. Repository Implementation

### Google Drive Repository
```java
// GoogleDriveRepository.java
public class GoogleDriveRepository implements Repository {
    private String clientId;
    private String clientSecret;
    private String refreshToken;
    private String folderId;
    
    @Override
    public InputStream downloadArtifact(String artifactId, String version) {
        // Implementation using Google Drive API v3
        Drive service = getDriveService();
        String fileId = findArtifact(artifactId, version);
        return service.files().get(fileId)
            .executeMediaAsInputStream();
    }
    
    @Override
    public Configuration downloadConfig(String artifactId, String profile) {
        // Download profile-specific configuration
        String configPath = String.format("%s/configs/%s/%s.yml", 
            folderId, artifactId, profile);
        return parseConfiguration(downloadFile(configPath));
    }
}
```

### Repository Factory Pattern
```java
public class RepositoryFactory {
    public static Repository create(RepositoryConfig config) {
        switch(config.getType()) {
            case GOOGLE_DRIVE:
                return new GoogleDriveRepository(config);
            case FTP:
                return new FtpRepository(config);
            case SFTP:
                return new SftpRepository(config);
            case HTTP:
                return new HttpRepository(config);
            default:
                throw new UnsupportedRepositoryException(config.getType());
        }
    }
}
```

## 3. Configuration Management Structure

### Directory Layout
```
clarity/
├── clients/
│   ├── telco-client-001/
│   │   ├── client.yml
│   │   ├── datacenters/
│   │   │   ├── dc-primary.yml
│   │   │   └── dc-backup.yml
│   │   ├── servers/
│   │   │   ├── prod-server-01.yml
│   │   │   └── prod-server-02.yml
│   │   └── deployments/
│   │       ├── monitoring-stack/
│   │       │   ├── dev.yml
│   │       │   ├── staging.yml
│   │       │   └── prod.yml
│   │       └── smsgateway/
│   │           ├── dev.yml
│   │           ├── staging.yml
│   │           └── prod.yml
│   └── telco-client-002/
│       └── ...
├── artifacts/
│   ├── monitoring-stack.yml
│   └── smsgateway.yml
└── repositories/
    └── repositories.yml
```

### Profile Configuration Example
```yaml
# clients/telco-client-001/deployments/monitoring-stack/prod.yml
artifact:
  id: monitoring-stack
  version: 2.45.0-10.0.1
  type: lxc

deployment:
  servers:
    - prod-server-01
    - prod-server-02 (backup)
  
  configuration:
    prometheus:
      retention_days: 30
      scrape_interval: 15s
      evaluation_interval: 15s
      targets:
        - job: node_exporter
          targets:
            - localhost:9100
            - 10.0.1.10:9100
    
    grafana:
      admin_password: ${VAULT:grafana_admin_pass}
      plugins:
        - grafana-piechart-panel
        - grafana-worldmap-panel
      datasources:
        - name: Prometheus
          type: prometheus
          url: http://localhost:9090
    
    volumes:
      - name: prometheus-data
        host: /data/${CLIENT_ID}/prometheus
        container: /prometheus
        mode: rw
      - name: grafana-data
        host: /data/${CLIENT_ID}/grafana
        container: /var/lib/grafana
        mode: rw
    
    network:
      ports:
        - 9090:9090  # Prometheus
        - 3000:3000  # Grafana
    
    autostart:
      enabled: true
      restart_policy: always
      startup_delay: 10
```

## 4. Database Schema

### PostgreSQL Tables
```sql
-- Clients
CREATE TABLE clients (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code VARCHAR(50) UNIQUE NOT NULL,
    name VARCHAR(255) NOT NULL,
    contact_email VARCHAR(255),
    contact_phone VARCHAR(50),
    status VARCHAR(20) DEFAULT 'active',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Datacenters
CREATE TABLE datacenters (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    client_id UUID REFERENCES clients(id),
    code VARCHAR(50) NOT NULL,
    name VARCHAR(255) NOT NULL,
    location_city VARCHAR(100),
    location_country VARCHAR(100),
    type VARCHAR(20), -- primary, backup, edge
    sl_no VARCHAR(50),
    description TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(client_id, code)
);

-- Servers
CREATE TABLE servers (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    datacenter_id UUID REFERENCES datacenters(id),
    hostname VARCHAR(255) NOT NULL,
    ip_address INET,
    server_type VARCHAR(20), -- host, vm, container
    hypervisor VARCHAR(50),
    os_name VARCHAR(100),
    os_version VARCHAR(50),
    cpu_cores INTEGER,
    ram_gb INTEGER,
    agent_version VARCHAR(20),
    agent_status VARCHAR(20),
    last_heartbeat TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(datacenter_id, hostname)
);

-- Artifacts
CREATE TABLE artifacts (
    id VARCHAR(100) PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    type VARCHAR(50) NOT NULL,
    latest_version VARCHAR(50),
    description TEXT,
    repository_type VARCHAR(50),
    repository_location TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Deployments
CREATE TABLE deployments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    client_id UUID REFERENCES clients(id),
    server_id UUID REFERENCES servers(id),
    artifact_id VARCHAR(100) REFERENCES artifacts(id),
    profile VARCHAR(20) NOT NULL, -- dev, staging, prod, mock
    version VARCHAR(50) NOT NULL,
    status VARCHAR(20) NOT NULL, -- pending, running, failed, completed
    configuration JSONB,
    deployed_at TIMESTAMP,
    deployed_by VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Deployment History
CREATE TABLE deployment_history (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    deployment_id UUID REFERENCES deployments(id),
    action VARCHAR(50) NOT NULL, -- download, configure, start, stop, rollback
    status VARCHAR(20) NOT NULL, -- success, failure
    message TEXT,
    details JSONB,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

## 5. API Specification

### REST API Endpoints
```yaml
# openapi.yml
paths:
  /api/v1/deployments:
    post:
      summary: Create new deployment
      requestBody:
        content:
          application/json:
            schema:
              type: object
              properties:
                client_id: string
                artifact_id: string
                version: string
                profile: string
                target_servers: array
      responses:
        201:
          description: Deployment created
          content:
            application/json:
              schema:
                type: object
                properties:
                  deployment_id: string
                  status: string

  /api/v1/agents/{server_id}/deploy:
    post:
      summary: Deploy artifact to specific server
      parameters:
        - name: server_id
          in: path
          required: true
      requestBody:
        content:
          application/json:
            schema:
              type: object
              properties:
                artifact_id: string
                profile: string
                version: string
                repository_config: object
```

## 6. Example Usage Scenarios

### Deploy Monitoring Stack
```bash
# Using CLI
clarity deploy \
  --client telco-client-001 \
  --artifact monitoring-stack \
  --version 2.45.0-10.0.1 \
  --profile prod \
  --servers prod-server-01,prod-server-02

# Using API
curl -X POST https://clarity.api/v1/deployments \
  -H "Content-Type: application/json" \
  -d '{
    "client_id": "telco-client-001",
    "artifact_id": "monitoring-stack",
    "version": "2.45.0-10.0.1",
    "profile": "prod",
    "target_servers": ["prod-server-01", "prod-server-02"]
  }'
```

### Deploy SMS Gateway
```bash
# Deploy to staging first
clarity deploy \
  --client telco-client-001 \
  --artifact smsgateway \
  --version 3.2.1 \
  --profile staging \
  --server staging-server-01

# After validation, deploy to production
clarity deploy \
  --client telco-client-001 \
  --artifact smsgateway \
  --version 3.2.1 \
  --profile prod \
  --servers prod-app-01,prod-app-02
```

## 7. Testing Strategy

### Unit Tests
- Repository implementations
- Configuration parsing
- Agent workflow steps

### Integration Tests
- End-to-end deployment flow
- Repository connectivity
- Agent-Orchestrix communication

### System Tests
- Multi-client deployment scenarios
- Rollback procedures
- High availability testing

## 8. Monitoring Setup

### Metrics to Track
```yaml
metrics:
  deployment:
    - deployment_duration_seconds
    - deployment_success_rate
    - artifact_download_time
    - configuration_apply_time
  
  agent:
    - agent_uptime
    - agent_memory_usage
    - agent_cpu_usage
    - heartbeat_frequency
  
  repository:
    - repository_connection_time
    - download_speed_mbps
    - authentication_failures
```

## 9. Security Implementation

### Agent Authentication
```go
// TLS mutual authentication
type AgentAuth struct {
    ServerCert   *x509.Certificate
    ClientCert   *x509.Certificate
    PrivateKey   *rsa.PrivateKey
}

func (a *Agent) Authenticate() error {
    tlsConfig := &tls.Config{
        Certificates: []tls.Certificate{
            {
                Certificate: [][]byte{a.Auth.ClientCert.Raw},
                PrivateKey:  a.Auth.PrivateKey,
            },
        },
        RootCAs: getCARootCerts(),
    }
    // Establish secure connection
}
```

### Configuration Encryption
```java
public class ConfigEncryption {
    private static final String ALGORITHM = "AES/GCM/NoPadding";
    
    public String encryptSensitive(String plaintext, String key) {
        // Encrypt sensitive configuration values
    }
    
    public String decryptSensitive(String ciphertext, String key) {
        // Decrypt on agent side
    }
}
```

## Next Steps

1. **Week 1**: Implement basic agent with Google Drive support
2. **Week 2**: Create configuration management structure
3. **Week 3**: Develop Orchestrix API
4. **Week 4**: Integration testing and documentation
5. **Month 2**: Add FTP/SFTP repositories and advanced features
6. **Month 3**: Production deployment and monitoring setup