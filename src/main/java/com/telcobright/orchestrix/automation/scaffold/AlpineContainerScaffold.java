package com.telcobright.orchestrix.automation.scaffold;

import com.telcobright.orchestrix.automation.core.device.CommandExecutor;
import com.telcobright.orchestrix.automation.scaffold.entity.AlpineScaffoldConfig;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.ArrayList;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Alpine Container Scaffolding Automation
 *
 * Creates minimal Alpine-based LXC containers from standalone binaries.
 * Follows LXC container conventions with versioning inside container directory.
 *
 * Features:
 * - Checks for existing binaries in standalone-binaries folder
 * - Creates Alpine container with ~85% size reduction vs Debian
 * - Generates build.sh, launch.sh, sample-config.conf
 * - Supports multiple binary versions
 *
 * Usage:
 *   AlpineContainerScaffold scaffold = new AlpineContainerScaffold(device, config);
 *   scaffold.checkExistingBinaries();
 *   scaffold.scaffoldContainer();
 */
public class AlpineContainerScaffold {

    private static final Logger logger = Logger.getLogger(AlpineContainerScaffold.class.getName());

    private final CommandExecutor device;
    private final AlpineScaffoldConfig config;

    public AlpineContainerScaffold(CommandExecutor device, AlpineScaffoldConfig config) {
        this.device = device;
        this.config = config;
    }

    /**
     * Check for existing binaries in standalone-binaries folder
     * @return List of available binary versions
     */
    public List<String> checkExistingBinaries() throws Exception {
        logger.info("Checking for existing binaries...");

        String binaryPath = config.getStandaloneBinaryPath();
        if (!Files.exists(Paths.get(binaryPath))) {
            logger.warning("Binary path does not exist: " + binaryPath);
            return new ArrayList<>();
        }

        // List version directories
        String result = device.executeCommand("ls -d " + binaryPath + "/*/");
        String[] lines = result.split("\n");

        List<String> versions = new ArrayList<>();
        for (String line : lines) {
            if (line.contains("-binary-v.")) {
                String version = line.substring(line.indexOf("-v.") + 3);
                version = version.replaceAll("[^0-9.]", "");

                // Check if binary exists
                String binaryFile = line.trim() + "/" + config.getBinaryName();
                String checkCmd = "[ -f " + binaryFile + " ] && echo 'exists' || echo 'missing'";
                String exists = device.executeCommand(checkCmd).trim();

                if ("exists".equals(exists)) {
                    versions.add(version);
                    logger.info("  Found version " + version + ": " + binaryFile);
                }
            }
        }

        if (versions.isEmpty()) {
            logger.warning("No binaries found in " + binaryPath);
        } else {
            logger.info("Found " + versions.size() + " binary version(s): " +
                       versions.stream().collect(Collectors.joining(", ")));
        }

        return versions;
    }

    /**
     * Scaffold Alpine container with existing binary
     */
    public void scaffoldContainer() throws Exception {
        logger.info("=================================================================");
        logger.info("  Alpine Container Scaffolding");
        logger.info("=================================================================");
        logger.info("Service: " + config.getServiceName());
        logger.info("Version: " + config.getContainerVersion());
        logger.info("Binary: " + config.getBinaryPath());
        logger.info("");

        // Create directory structure
        createDirectoryStructure();

        // Create build.sh that copies existing binary
        createBuildScript();

        // Create launch script
        createLaunchScript();

        // Create sample config
        createSampleConfig();

        // Create README
        createReadme();

        // Create startDefault.sh
        createStartDefault();

        logger.info("");
        logger.info("=================================================================");
        logger.info("  Scaffolding Complete!");
        logger.info("=================================================================");
        logger.info("");
        logger.info("Directory structure created:");
        logger.info("  " + config.getContainerPath());
        logger.info("");
        logger.info("Build the container:");
        logger.info("  cd " + config.getContainerPath());
        logger.info("  ./build/build.sh");
        logger.info("");
        logger.info("Quick test:");
        logger.info("  cd " + config.getVersionPath() + "/generated");
        logger.info("  ./startDefault.sh");
        logger.info("");
    }

    private void createDirectoryStructure() throws Exception {
        logger.info("Creating directory structure...");

        String dirs = String.join(" ",
            config.getContainerPath() + "/build",
            config.getContainerPath() + "/scripts",
            config.getContainerPath() + "/templates",
            config.getVersionPath() + "/generated/artifact",
            config.getVersionPath() + "/generated/publish",
            config.getVersionPath() + "/generated/test"
        );

        device.executeCommand("mkdir -p " + dirs);
    }

    private void createBuildScript() throws Exception {
        logger.info("Creating build/build.sh...");

        String script = """
#!/bin/bash
#
# Build script for %s Alpine container
# Copies existing standalone binary into minimal Alpine container
#

set -e

# Load configuration
CONFIG_FILE="${1:-$(dirname "$0")/build.conf}"
if [ ! -f "$CONFIG_FILE" ]; then
    echo "Configuration file not found: $CONFIG_FILE"
    exit 1
fi

echo "Loading configuration from: $CONFIG_FILE"
source "$CONFIG_FILE"

echo "========================================="
echo "Building Alpine Container: ${CONTAINER_NAME}"
echo "========================================="
echo "Version: ${VERSION}"
echo "Binary: ${BINARY_SOURCE}"
echo "========================================="

# Check if binary exists
if [ ! -f "${BINARY_SOURCE}" ]; then
    echo "ERROR: Binary not found: ${BINARY_SOURCE}"
    echo ""
    echo "Please build the binary first:"
    echo "  cd %s"
    echo "  mvn exec:java -Dexec.mainClass=\"%s\" -Dexec.args=\"${BINARY_VERSION}\""
    exit 1
fi

# Clean existing container
echo "Checking for existing container..."
if lxc info ${BUILD_CONTAINER} >/dev/null 2>&1; then
    echo "Removing existing build container..."
    lxc stop ${BUILD_CONTAINER} --force >/dev/null 2>&1 || true
    lxc delete ${BUILD_CONTAINER} --force
fi

# Create Alpine container
echo "Creating Alpine container..."
lxc launch images:alpine/3.20 ${BUILD_CONTAINER}

# Wait for network
echo "Waiting for container network..."
for i in {1..30}; do
    if lxc exec ${BUILD_CONTAINER} -- ping -c 1 8.8.8.8 >/dev/null 2>&1; then
        break
    fi
    sleep 1
done

# Copy binary
echo "Copying binary to container..."
lxc file push ${BINARY_SOURCE} ${BUILD_CONTAINER}/usr/local/bin/${BINARY_NAME}
lxc exec ${BUILD_CONTAINER} -- chmod +x /usr/local/bin/${BINARY_NAME}

# Create startup script
echo "Creating startup script..."
cat << 'EOF' | lxc exec ${BUILD_CONTAINER} -- tee /etc/init.d/${SERVICE_NAME}
#!/sbin/openrc-run

name="${SERVICE_NAME}"
description="${SERVICE_DESC}"
command="/usr/local/bin/${BINARY_NAME}"
command_args=""
pidfile="/var/run/${SERVICE_NAME}.pid"
command_background=true

depend() {
    need net
}

start_pre() {
    # Environment setup
    export SERVICE_PORT=${SERVICE_PORT}
    export SHARD_ID=${SHARD_ID}
    export TOTAL_SHARDS=${TOTAL_SHARDS}
}
EOF

lxc exec ${BUILD_CONTAINER} -- chmod +x /etc/init.d/${SERVICE_NAME}
lxc exec ${BUILD_CONTAINER} -- rc-update add ${SERVICE_NAME} default

# Stop and publish as image
echo "Stopping container..."
lxc stop ${BUILD_CONTAINER}

# Publish container as image
TIMESTAMP=$(date +%%s)
BASE_DIR="/home/mustafa/telcobright-projects/orchestrix/images/containers/lxc/${CONTAINER_NAME}"
IMAGE_FILE="${BASE_DIR}/${CONTAINER_NAME}-v.${VERSION}/generated/artifact/${CONTAINER_NAME}-v${VERSION}-${TIMESTAMP}.tar.gz"
IMAGE_ALIAS="${CONTAINER_NAME}-v${VERSION}-${TIMESTAMP}"

echo "Publishing as image: ${IMAGE_ALIAS}"
lxc publish ${BUILD_CONTAINER} --alias "${IMAGE_ALIAS}"

# Export the image
echo "Exporting image to: ${IMAGE_FILE}"
lxc image export "${IMAGE_ALIAS}" "${IMAGE_FILE%%.tar.gz}"

# The export creates a .tar.gz automatically, rename if needed
if [ -f "${IMAGE_FILE%%.tar.gz}.tar.gz" ]; then
    mv "${IMAGE_FILE%%.tar.gz}.tar.gz" "${IMAGE_FILE}"
fi

# Generate MD5
md5sum "${IMAGE_FILE}" > "${IMAGE_FILE}.md5"

# Delete the temporary image alias
lxc image delete "${IMAGE_ALIAS}"

# Copy templates to version-specific generated directory
cp ${BASE_DIR}/templates/sample.conf ${BASE_DIR}/${CONTAINER_NAME}-v.${VERSION}/generated/
cp ${BASE_DIR}/templates/startDefault.sh ${BASE_DIR}/${CONTAINER_NAME}-v.${VERSION}/generated/
chmod +x ${BASE_DIR}/${CONTAINER_NAME}-v.${VERSION}/generated/startDefault.sh

# Clean up
lxc delete ${BUILD_CONTAINER}

echo "========================================="
echo "Build Complete!"
echo "========================================="
echo "Image: ${IMAGE_FILE}"
echo "Size: $(du -h ${IMAGE_FILE} | cut -f1)"
echo ""
echo "Test with:"
echo "  cd ${BASE_DIR}/${CONTAINER_NAME}-v.${VERSION}/generated"
echo "  ./startDefault.sh"
echo "========================================="
""".formatted(
            config.getServiceName(),
            config.getStandaloneBinaryPath(),
            config.getBinaryBuilderClass()
        );

        String buildScript = config.getContainerPath() + "/build/build.sh";
        Files.write(Paths.get(buildScript), script.getBytes());
        device.executeCommand("chmod +x " + buildScript);

        // Create build.conf
        createBuildConfig();
    }

    private void createBuildConfig() throws Exception {
        logger.info("Creating build/build.conf...");

        String conf = """
# Build Configuration for %s Alpine Container
# Version: %s

# Container Settings
CONTAINER_NAME="%s"
VERSION="%s"
BUILD_CONTAINER="${CONTAINER_NAME}-build-temp"
SERVICE_NAME="%s"
SERVICE_DESC="%s"

# Binary Settings
BINARY_NAME="%s"
BINARY_VERSION="%s"
BINARY_SOURCE="%s"

# Service Configuration
SERVICE_PORT=%d
SHARD_ID=1
TOTAL_SHARDS=1

# Output
OUTPUT_DIR="../%s/generated"
""".formatted(
            config.getServiceName(),
            config.getContainerVersion(),
            config.getContainerName(),
            config.getContainerVersion(),
            config.getServiceName(),
            config.getServiceDescription(),
            config.getBinaryName(),
            config.getBinaryVersion(),
            config.getBinaryPath(),
            config.getServicePort(),
            config.getVersionDirName()
        );

        String confFile = config.getContainerPath() + "/build/build.conf";
        Files.write(Paths.get(confFile), conf.getBytes());
    }

    private void createLaunchScript() throws Exception {
        logger.info("Creating launch script...");

        String script = """
#!/bin/bash
#
# Launch script for %s Alpine container
# Usage: ./launch%s.sh [config-file]
#

CONFIG_FILE="${1:-sample-config.conf}"

if [ ! -f "$CONFIG_FILE" ]; then
    echo "Configuration file not found: $CONFIG_FILE"
    echo "Usage: $0 [config-file]"
    exit 1
fi

# Load configuration
source "$CONFIG_FILE"

# Import image if provided
if [ -n "${IMAGE_FILE}" ] && [ -f "${IMAGE_FILE}" ]; then
    echo "Importing image: ${IMAGE_FILE}"
    lxc image import "${IMAGE_FILE}" --alias "${IMAGE_ALIAS}"
fi

# Launch container
echo "Launching container: ${CONTAINER_NAME}"
lxc launch "${IMAGE_ALIAS}" "${CONTAINER_NAME}"

# Set environment variables
lxc exec "${CONTAINER_NAME}" -- sh -c "echo 'export SERVICE_PORT=${SERVICE_PORT}' >> /etc/profile"
lxc exec "${CONTAINER_NAME}" -- sh -c "echo 'export SHARD_ID=${SHARD_ID}' >> /etc/profile"
lxc exec "${CONTAINER_NAME}" -- sh -c "echo 'export TOTAL_SHARDS=${TOTAL_SHARDS}' >> /etc/profile"

# Start service
echo "Starting %s service..."
lxc exec "${CONTAINER_NAME}" -- rc-service %s start

echo "Container ${CONTAINER_NAME} is running"
echo "Test with: curl http://$(lxc list ${CONTAINER_NAME} -c4 --format csv | cut -d' ' -f1):${SERVICE_PORT}/health"
""".formatted(
            config.getServiceName(),
            config.getCapitalizedName(),
            config.getServiceName(),
            config.getServiceName()
        );

        String launchScript = config.getContainerPath() + "/launch" + config.getCapitalizedName() + ".sh";
        Files.write(Paths.get(launchScript), script.getBytes());
        device.executeCommand("chmod +x " + launchScript);
    }

    private void createSampleConfig() throws Exception {
        logger.info("Creating sample config...");

        String conf = """
# Sample configuration for %s Alpine container
# Copy and modify this file for your deployment

# Container Settings
CONTAINER_NAME="%s-instance"
IMAGE_ALIAS="%s-alpine-v%s"
IMAGE_FILE="./artifact/%s-v%s-*.tar.gz"

# Service Configuration
SERVICE_PORT=%d
SHARD_ID=1
TOTAL_SHARDS=1

# Optional: Consul Registration
# CONSUL_URL="http://consul-server:8500"
# CONTAINER_IP="auto"
""".formatted(
            config.getServiceName(),
            config.getContainerName(),
            config.getContainerName(),
            config.getContainerVersion(),
            config.getContainerName(),
            config.getContainerVersion(),
            config.getServicePort()
        );

        String confFile = config.getContainerPath() + "/templates/sample.conf";
        Files.write(Paths.get(confFile), conf.getBytes());
    }

    private void createStartDefault() throws Exception {
        logger.info("Creating startDefault.sh...");

        String script = """
#!/bin/bash
#
# Quick start script for %s Alpine container
# Launches a test instance with default configuration
#

cd "$(dirname "$0")"

# Use sample config
cp sample.conf test.conf

# Update config for test instance
sed -i 's/CONTAINER_NAME=.*/CONTAINER_NAME="%s-test"/' test.conf

# Launch container
../../launch%s.sh test.conf

echo ""
echo "Test container launched: %s-test"
echo "Check health: curl http://localhost:%d/health"
echo "Stop with: lxc stop %s-test --force && lxc delete %s-test"
""".formatted(
            config.getServiceName(),
            config.getContainerName(),
            config.getCapitalizedName(),
            config.getContainerName(),
            config.getServicePort(),
            config.getContainerName(),
            config.getContainerName()
        );

        String startScript = config.getContainerPath() + "/templates/startDefault.sh";
        Files.write(Paths.get(startScript), script.getBytes());
        device.executeCommand("chmod +x " + startScript);
    }

    private void createReadme() throws Exception {
        logger.info("Creating README.md...");

        String readme = """
# %s Alpine Container

Minimal Alpine Linux container with %s standalone binary.

## Size Comparison

| Type | Base OS | Binary | Total | Savings |
|------|---------|--------|-------|---------|
| Original | Debian 12 | Go runtime | ~169 MB | - |
| **Alpine** | Alpine 3.18 | Standalone | **~25 MB** | **85%%** |

## Quick Start

Build the container:
```bash
cd build
./build.sh
```

Test with default configuration:
```bash
cd %s/generated
./startDefault.sh
```

## Directory Structure

```
%s/
├── build/
│   ├── build.sh          # Build script (copies binary)
│   └── build.conf        # Build configuration
├── launch%s.sh    # Launch script
├── templates/
│   ├── sample.conf       # Sample configuration
│   └── startDefault.sh   # Quick start script
└── %s/
    └── generated/
        ├── artifact/     # Container images
        ├── publish/      # Publishing scripts
        └── test/         # Test runners
```

## Binary Source

This container uses a pre-built standalone binary from:
```
%s
```

To rebuild the binary:
```bash
cd %s
mvn exec:java -Dexec.mainClass="%s" -Dexec.args="%s"
```

## Configuration

Edit `sample.conf` to configure:
- Container name
- Service port
- Shard ID (for clustering)
- Consul registration (optional)

## API Endpoints

- `GET /health` - Health check
- `GET /shard-info` - Shard configuration
%s

## Deployment

Launch on remote server:
```bash
./launch%s.sh /path/to/custom-config.conf
```

Multiple instances (sharding):
```bash
# Shard 1
SHARD_ID=1 ./launch%s.sh shard1.conf

# Shard 2
SHARD_ID=2 ./launch%s.sh shard2.conf

# Shard 3
SHARD_ID=3 ./launch%s.sh shard3.conf
```

## Monitoring

Check container status:
```bash
lxc list %s
```

View logs:
```bash
lxc exec %s-instance -- tail -f /var/log/%s.log
```

## Troubleshooting

### Container won't start
- Check if binary exists: `ls -la %s`
- Build binary if missing (see Binary Source above)

### Service not responding
- Check if service is running: `lxc exec CONTAINER -- rc-status`
- Check logs: `lxc exec CONTAINER -- cat /var/log/%s.log`

### Port conflict
- Change SERVICE_PORT in config file
- Ensure port is not in use: `ss -tulpn | grep PORT`
""".formatted(
            config.getServiceName(),
            config.getServiceName(),
            config.getVersionDirName(),
            config.getContainerName(),
            config.getCapitalizedName(),
            config.getVersionDirName(),
            config.getBinaryPath(),
            config.getStandaloneBinaryPath(),
            config.getBinaryBuilderClass(),
            config.getBinaryVersion(),
            config.getAdditionalEndpoints(),
            config.getCapitalizedName(),
            config.getCapitalizedName(),
            config.getCapitalizedName(),
            config.getCapitalizedName(),
            config.getContainerName(),
            config.getContainerName(),
            config.getServiceName(),
            config.getBinaryPath(),
            config.getServiceName()
        );

        String readmeFile = config.getContainerPath() + "/README.md";
        Files.write(Paths.get(readmeFile), readme.getBytes());
    }
}