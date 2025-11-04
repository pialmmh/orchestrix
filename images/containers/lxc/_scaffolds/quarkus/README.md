# Quarkus LXC Deployment Scaffold

Interactive generator for creating complete Quarkus LXC deployment structures.

## Features

- **Maven integration** - Auto-runs `mvn clean install -DskipTests`
- **Version-specific artifacts** - Each version has isolated deployments
- **Config override** - External `application.properties` injection
- **Multi-version support** - Deploy v1.0.0 while testing v1.1.0
- **Copy between versions** - Migrate configs from previous versions
- **Auto-copy from Java project** - Copies `application.properties` from your source
- **Local & remote deployment** - SSH-based remote deployment

## Usage

```bash
cd /home/mustafa/telcobright-projects/orchestrix/images/lxc/
./_scaffolds/quarkus/quarkus-scaffold.sh
```

**Interactive Prompts:**
- Application name
- Version
- Java project path
- JAR file location
- Ports (application, debug)
- JVM settings (heap sizes)
- Database name

**Creates:**

```
<app-name>/
├── build/
│   ├── build.conf              # Pre-filled from prompts
│   └── build.sh                # Maven + LXC build
├── scripts/
│   ├── deploy.sh               # Deployment script
│   └── new-deployment.sh       # Template
└── <app>-v<version>/
    ├── generated/artifacts/     # Build output
    └── deployments/            # Version-specific deployments
```

## Generated Structure

```
<app-name>/
├── build/
│   ├── build.conf              # Pre-filled configuration
│   └── build.sh                # Maven + LXC image build
│
├── scripts/
│   ├── deploy.sh               # Deploy to local/remote
│   └── new-deployment.sh       # Template
│
├── <app>-v<version>/
│   ├── generated/
│   │   └── artifacts/
│   │       ├── <app>-<version>.jar
│   │       ├── <app>-<version>.tar.gz
│   │       └── <app>-<version>.tar.gz.md5
│   │
│   └── deployments/
│       ├── new-deployment.sh   # Create deployment configs
│       └── <deployment-name>/
│           ├── deployment.yaml
│           ├── application.properties
│           └── README.md
│
└── README.md                   # App-specific workflow
```

## The 6-Phase Workflow

### 1. Run Scaffold
```bash
./_scaffolds/quarkus/quarkus-scaffold.sh
```

### 2. Review Config (optional)
```bash
cd <app-name>
vim build/build.conf
```

### 3. Build
```bash
cd build
./build.sh
```

This:
- Runs `mvn clean install -DskipTests`
- Creates LXC container image
- Exports to tarball
- Creates version-specific deployments folder

### 4. Create Deployment
```bash
cd ../<app>-v<version>/deployments
./new-deployment.sh
```

This:
- Prompts for deployment details
- **Copies `application.properties` from Java project**
- Can copy from previous version deployments
- Generates `deployment.yaml`

### 5. Customize
```bash
vim <deployment-name>/application.properties
vim <deployment-name>/deployment.yaml
```

### 6. Deploy
```bash
cd ../..
./scripts/deploy.sh <app>-v<version>/deployments/<deployment-name>
```

## Config Override System

**Build Phase (once):**
- Maven builds JAR with default `application.properties`
- LXC image created with JAR + empty `/opt/<app>/config/`

**Deploy Phase (multiple times):**
- `deploy.sh` injects deployment-specific `application.properties` to `/opt/<app>/config/`
- Quarkus loads: JAR defaults → config override

**Result:** Same build, different configs per deployment!

## Version Management

Deploy multiple versions simultaneously:

```
<app-name>/
├── <app>-v1.0.0/deployments/
│   ├── link3-prod/          # Production v1.0.0
│   └── link3-staging/       # Staging v1.0.0
│
└── <app>-v1.1.0/deployments/
    ├── link3-staging-v1.1/  # Test v1.1.0 in staging
    └── link3-prod-v1.1/     # Upgrade prod when ready
```

## Copy Between Versions

When creating deployment for v1.1.0:
```bash
cd <app>-v1.1.0/deployments
./new-deployment.sh

? Deployment name: link3-prod-v1.1
? Copy from previous version? y
# Copies from v1.0.0/deployments/link3-prod/
# Auto-updates version references
```

## Remote Deployment

Deployment YAML example:
```yaml
remote:
  host: 10.10.199.27
  user: ubuntu
  use_ssh_agent: true
```

Deploy script handles:
- SSH to remote server
- Copy tarball
- Import LXC image
- Create container
- Inject configs
- Start service

## Local Deployment

For local testing:
```yaml
remote:
  host:              # Empty = local
```

## See Also

- Main scaffolds overview: `../_scaffolds/README.md`
- Generated app README: `<app-name>/README.md`

