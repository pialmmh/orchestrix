# Infrastructure as Code (IaC)

This directory contains all infrastructure-as-code configurations and tools for the Orchestrix project.

## Directory Structure

```
iac/
├── pulumi/           # Pulumi cloud infrastructure configuration
│   └── pulumi.conf   # Minimal Pulumi settings (API keys, endpoints)
└── jenkins/          # Jenkins CI/CD configuration
    ├── server/       # Jenkins server configuration
    └── agent/        # Jenkins agent configuration
```

## Pulumi Configuration

The `pulumi/pulumi.conf` contains minimal infrastructure settings:

- **Pulumi Backend**: Access tokens and organization settings
- **Cloud Providers**: AWS, Azure, GCP, DigitalOcean credentials
- **Container Registries**: Docker registry configuration
- **Database Endpoints**: Connection strings for PostgreSQL, MySQL, MongoDB, Redis
- **API Endpoints**: External service URLs and authentication
- **Monitoring**: DataDog, NewRelic, Grafana integration

### Usage

```bash
# Load Pulumi configuration
source iac/pulumi/pulumi.conf

# Use in Pulumi programs
pulumi config set aws:accessKey $AWS_ACCESS_KEY_ID
pulumi config set aws:secretKey $AWS_SECRET_ACCESS_KEY
```

## Jenkins Configuration

Jenkins configurations are stored in `jenkins/` with separate folders for server and agent setups.

### Server Configuration
- `jenkins/server/config.yml` - Main Jenkins server settings
- Security configurations
- Plugin management

### Agent Configuration  
- `jenkins/agent/config.yml` - Agent connection settings
- Agent-specific configurations

## Separation of Concerns

This IaC directory is intentionally separated from:
- **Container Configurations**: Located in `/images/containers/lxc/`
- **Network Configurations**: Located in `/network/`
- **Application Code**: Located in `/automation/`

This separation ensures:
1. Infrastructure definitions remain cloud-agnostic
2. Container orchestration is independent of cloud providers
3. Network topology can be managed separately
4. Clear boundaries between infrastructure and application layers

## Best Practices

1. **No Hardcoding**: All sensitive values should use environment variables
2. **Version Control**: Commit configuration files without secrets
3. **Documentation**: Keep configurations well-documented
4. **Modularity**: Each tool has its own configuration space
5. **Minimal Config**: Only essential infrastructure settings, no application logic