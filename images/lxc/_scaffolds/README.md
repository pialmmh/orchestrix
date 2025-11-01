# LXC Deployment Scaffolds

Centralized scaffold generators for creating LXC deployment structures.

## Directory Structure

```
_scaffolds/
├── README.md                    # This file
│
├── quarkus/                     # Quarkus applications
│   ├── quarkus-scaffold.sh      # Interactive generator
│   └── README.md                # Quarkus-specific documentation
│
├── spring-boot/                 # Future: Spring Boot applications
│   ├── spring-boot-scaffold.sh
│   └── README.md
│
└── generic-java/                # Future: Generic Java applications
    ├── generic-java-scaffold.sh
    └── README.md
```

## Available Scaffolds

### 🚀 Quarkus

**Path**: `_scaffolds/quarkus/quarkus-scaffold.sh`

Creates complete deployment structure for Quarkus applications with:
- Maven integration (auto-runs `mvn clean install -DskipTests`)
- Version-specific artifact management
- Deployment configuration per version
- Config override support (external `application.properties`)
- Copy configs between versions
- Local and remote deployment

**Usage:**

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

**Documentation**: See `_scaffolds/quarkus/README.md`

---

## Future Scaffolds

### Spring Boot (Planned)

```bash
./_scaffolds/spring-boot/spring-boot-scaffold.sh
```

Features:
- Spring Boot specific configuration
- Gradle or Maven support
- Spring profiles per deployment
- Actuator endpoints

### Generic Java (Planned)

```bash
./_scaffolds/generic-java/generic-java-scaffold.sh
```

Features:
- Works with any Java application
- Custom startup scripts
- Flexible configuration

---

## Common Workflow

All scaffolds follow the same 6-phase workflow:

1. **Run scaffold** to create app structure
2. **Review configuration** (optional, pre-filled)
3. **Build** to generate LXC image
4. **Create deployment** configurations
5. **Customize** deployment-specific configs
6. **Deploy** to local or remote servers

---

## Adding New Scaffolds

To add a new scaffold type:

1. Create subdirectory: `_scaffolds/<type>/`
2. Create scaffold script: `<type>-scaffold.sh`
3. Create documentation: `README.md`
4. Update this file to list the new scaffold

---

## Best Practices

- **One scaffold per app type** - Quarkus, Spring Boot, etc.
- **Interactive prompts** - No command-line arguments
- **Pre-fill configs** - Save user time
- **Self-contained** - All logic in one script
- **Documented** - Clear README per scaffold

---

## Getting Started

1. Choose your application type (currently: Quarkus)
2. Run the appropriate scaffold script
3. Follow the workflow in the generated app's README
4. Deploy to your servers!

For detailed documentation, see the README in each scaffold's subdirectory.

