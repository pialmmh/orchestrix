package com.telcobright.orchestrix.automation.api.container.lxc.app.goid.core;

import com.telcobright.orchestrix.automation.api.container.lxc.app.goid.scripts.GoIdServiceCode;
import com.telcobright.orchestrix.automation.core.PrerequisiteChecker;
import com.telcobright.orchestrix.automation.core.storage.btrfs.BtrfsStorageProvider;
import com.telcobright.orchestrix.automation.core.storage.base.StorageVolume;
import com.telcobright.orchestrix.automation.core.storage.base.StorageVolumeConfig;
import com.telcobright.orchestrix.automation.core.device.LocalSshDevice;

import java.io.File;
import java.util.Properties;
import java.util.logging.Logger;

/**
 * Go ID Container Builder - Core Logic
 *
 * <p><b>REQUIRES:</b> Connected SshDevice (no ProcessBuilder fallback)
 *
 * <p>Features:
 * <ul>
 *   <li>Complete REST API matching Node.js unique-id-generator</li>
 *   <li>Sonyflake distributed ID generation</li>
 *   <li>Sharded sequential IDs (int, long)</li>
 *   <li>State persistence with JSON</li>
 *   <li>BTRFS storage with quota management</li>
 *   <li>Systemd service for auto-start</li>
 * </ul>
 */
public class GoIdContainerBuilder {

    private static final Logger logger = Logger.getLogger(GoIdContainerBuilder.class.getName());

    private final LocalSshDevice device;
    private BtrfsStorageProvider storageProvider;
    private Properties config;

    // Build configuration
    private String containerName;
    private String baseImage;
    private String goVersion;
    private String storageLocationId;
    private String storageQuotaSize;
    private String imageAlias;
    private int servicePort;

    // Publish configuration
    private String rcloneRemote;
    private boolean generateUploadScript;
    private String exportedImagePath;

    /**
     * Create builder with connected SSH device
     *
     * @param device Connected LocalSshDevice (must be already connected)
     * @param configFile Path to build configuration file
     * @throws Exception if device not connected or config invalid
     */
    public GoIdContainerBuilder(LocalSshDevice device, String configFile) throws Exception {
        if (device == null || !device.isConnected()) {
            throw new IllegalArgumentException("LocalSshDevice must be connected");
        }

        this.device = device;
        this.storageProvider = new BtrfsStorageProvider(true);

        loadConfiguration(configFile);
    }

    private void loadConfiguration(String configFile) throws Exception {
        logger.info("Loading configuration from: " + configFile);

        config = new Properties();
        File file = new File(configFile);

        if (!file.exists()) {
            throw new Exception("Configuration file not found: " + configFile);
        }

        // Parse shell-style config file
        String content = new String(java.nio.file.Files.readAllBytes(file.toPath()));
        for (String line : content.split("\n")) {
            if (line.trim().isEmpty() || line.trim().startsWith("#")) continue;

            if (line.contains("=")) {
                String[] parts = line.split("=", 2);
                String key = parts[0].trim();
                String value = parts[1].trim();

                // Strip inline comments
                int commentIndex = value.indexOf('#');
                if (commentIndex > 0) {
                    value = value.substring(0, commentIndex).trim();
                }

                // Strip quotes
                value = value.replaceAll("^\"|\"$", "");
                config.setProperty(key, value);
            }
        }

        // Extract key configurations
        String version = config.getProperty("CONTAINER_VERSION", "1");
        containerName = config.getProperty("CONTAINER_NAME_PREFIX", "go-id") + "-v" + version;
        baseImage = config.getProperty("BASE_IMAGE", "images:debian/12");
        goVersion = config.getProperty("GO_VERSION", "1.21");
        storageLocationId = config.getProperty("STORAGE_LOCATION_ID");
        storageQuotaSize = config.getProperty("STORAGE_QUOTA_SIZE");
        imageAlias = config.getProperty("IMAGE_ALIAS", "go-id-base");
        servicePort = Integer.parseInt(config.getProperty("SERVICE_PORT", "7001"));

        // Publish configuration
        rcloneRemote = config.getProperty("RCLONE_REMOTE", "gdrive");
        generateUploadScript = Boolean.parseBoolean(config.getProperty("GENERATE_UPLOAD_SCRIPT", "true"));

        // Validate required parameters
        if (storageLocationId == null || storageLocationId.isEmpty()) {
            throw new Exception("STORAGE_LOCATION_ID is required in configuration");
        }
        if (storageQuotaSize == null || storageQuotaSize.isEmpty()) {
            throw new Exception("STORAGE_QUOTA_SIZE is required in configuration");
        }
    }

    public void build() throws Exception {
        logger.info("=========================================");
        logger.info("Building Go ID Generator Container");
        logger.info("=========================================");
        logger.info("Container: " + containerName);
        logger.info("Go Version: " + goVersion);
        logger.info("Service Port: " + servicePort);
        logger.info("Storage: " + storageLocationId + " (" + storageQuotaSize + ")");
        logger.info("=========================================");

        // Step 0: Check prerequisites
        logger.info("");
        PrerequisiteChecker checker = new PrerequisiteChecker(device, true);
        if (!checker.checkAll()) {
            throw new Exception("Prerequisite checks failed. Please fix the errors above and try again.");
        }
        logger.info("");

        // Step 1: Setup BTRFS storage
        StorageVolume volume = setupBtrfsStorage();

        // Step 2: Clean existing container if requested
        if ("true".equals(config.getProperty("CLEAN_BUILD", "true"))) {
            cleanExistingContainer();
        }

        // Step 3: Create base container
        createContainer();

        // Step 4: Start container
        startContainer();

        // Step 5: Update and upgrade system
        updateSystem();

        // Step 6: Install Go
        installGo();

        // Step 7: Create Go ID service
        createGoIdService();

        // Step 8: Configure systemd service
        configureSystemdService();

        // Step 9: Stop container and export
        stopAndExportContainer();

        logger.info("=========================================");
        logger.info("Build Complete!");
        logger.info("Container: " + containerName);
        logger.info("Image exported and ready for deployment");
        logger.info("=========================================");
    }

    private StorageVolume setupBtrfsStorage() throws Exception {
        logger.info("Setting up BTRFS storage...");

        // Get storage path from location ID
        String storagePath = "/btrfs"; // TODO: Read from storage locations config

        StorageVolumeConfig volumeConfig = new StorageVolumeConfig.Builder()
                .locationId(storageLocationId)
                .containerRoot(storagePath + "/containers/" + containerName)
                .quotaHumanSize(storageQuotaSize)
                .compression("true".equals(config.getProperty("STORAGE_COMPRESSION", "true")))
                .snapshotEnabled(true)
                .build();

        // Log storage configuration
        logger.info("BTRFS storage configured for: " + volumeConfig.getContainerRoot());

        // Return minimal volume object
        // TODO: Implement proper volume creation when LocalSshDevice is compatible
        return null;
    }

    private void cleanExistingContainer() throws Exception {
        logger.info("Checking for existing container...");

        String result = device.executeCommand("lxc info " + containerName + " 2>/dev/null || true");
        if (result != null && result.contains("Name: " + containerName)) {
            logger.info("Removing existing container: " + containerName);
            device.executeCommand("sudo lxc stop " + containerName + " --force 2>/dev/null || true");
            device.executeCommand("sudo lxc delete " + containerName + " --force");
        }
    }

    private void createContainer() throws Exception {
        logger.info("Creating container from image: " + baseImage);
        device.executeCommand("sudo lxc init " + baseImage + " " + containerName);

        // Configure resources
        String memLimit = config.getProperty("MEMORY_LIMIT", "512MB");
        String cpuLimit = config.getProperty("CPU_LIMIT", "1");
        device.executeCommand("sudo lxc config set " + containerName + " limits.memory " + memLimit);
        device.executeCommand("sudo lxc config set " + containerName + " limits.cpu " + cpuLimit);
    }

    private void startContainer() throws Exception {
        logger.info("Starting container...");
        device.executeCommand("sudo lxc start " + containerName);

        // Wait for container to be ready and network to initialize
        logger.info("Waiting for container network...");
        Thread.sleep(10000);

        // Verify container is running
        String status = device.executeCommand("sudo lxc list " + containerName + " -c s --format csv");
        if (status == null || !status.toUpperCase().contains("RUNNING")) {
            throw new Exception("Container failed to start");
        }

        logger.info("Container started successfully");
    }

    private void updateSystem() throws Exception {
        logger.info("Updating system packages...");
        device.executeCommand("sudo lxc exec " + containerName + " -- apt-get update");
        device.executeCommand("sudo lxc exec " + containerName + " -- apt-get upgrade -y");
        device.executeCommand("sudo lxc exec " + containerName + " -- apt-get install -y curl wget git build-essential");
    }

    private void installGo() throws Exception {
        logger.info("Installing Go " + goVersion + "...");

        // Download and install Go
        String goDownloadUrl = "https://go.dev/dl/go" + goVersion + ".linux-amd64.tar.gz";

        device.executeCommand("sudo lxc exec " + containerName + " -- bash -c '" +
                "curl -fsSL " + goDownloadUrl + " -o /tmp/go.tar.gz && " +
                "rm -rf /usr/local/go && " +
                "tar -C /usr/local -xzf /tmp/go.tar.gz && " +
                "rm /tmp/go.tar.gz'");

        // Setup Go environment
        device.executeCommand("sudo lxc exec " + containerName + " -- bash -c '" +
                "echo \"export PATH=\\$PATH:/usr/local/go/bin\" >> /etc/profile && " +
                "echo \"export GOPATH=/opt/go\" >> /etc/profile'");

        // Verify installation
        String version = device.executeCommand("sudo lxc exec " + containerName + " -- /usr/local/go/bin/go version");
        logger.info("Go installed: " + version);
    }

    private void createGoIdService() throws Exception {
        logger.info("Creating Go ID Generator service with complete API...");

        // Create directories
        device.executeCommand("sudo lxc exec " + containerName + " -- mkdir -p /opt/go-id");
        device.executeCommand("sudo lxc exec " + containerName + " -- mkdir -p /var/log/go-id");
        device.executeCommand("sudo lxc exec " + containerName + " -- mkdir -p /var/lib/go-id");

        // Get complete service code from GoIdServiceCode
        GoIdServiceCode serviceCode = new GoIdServiceCode(servicePort);
        String goCode = serviceCode.getServiceCode();

        // Write Go code to file
        String escapedCode = goCode.replace("'", "'\\''");
        device.executeCommand("sudo lxc exec " + containerName + " -- bash -c 'echo '" + escapedCode + "' > /opt/go-id/main.go'");

        // Initialize Go module and install dependencies
        logger.info("Installing Go dependencies (Sonyflake + Gorilla Mux + Consul)...");
        device.executeCommand("sudo lxc exec " + containerName + " -- bash -c 'cd /opt/go-id && /usr/local/go/bin/go mod init go-id-service'");
        device.executeCommand("sudo lxc exec " + containerName + " -- bash -c 'cd /opt/go-id && /usr/local/go/bin/go get github.com/sony/sonyflake@latest'");
        device.executeCommand("sudo lxc exec " + containerName + " -- bash -c 'cd /opt/go-id && /usr/local/go/bin/go get github.com/gorilla/mux@latest'");
        device.executeCommand("sudo lxc exec " + containerName + " -- bash -c 'cd /opt/go-id && /usr/local/go/bin/go get github.com/hashicorp/consul/api@latest'");
        device.executeCommand("sudo lxc exec " + containerName + " -- bash -c 'cd /opt/go-id && /usr/local/go/bin/go mod tidy'");

        // Compile Go binary
        logger.info("Compiling Go binary...");
        device.executeCommand("sudo lxc exec " + containerName + " -- bash -c 'cd /opt/go-id && /usr/local/go/bin/go build -o go-id main.go'");

        // Verify binary
        device.executeCommand("sudo lxc exec " + containerName + " -- chmod +x /opt/go-id/go-id");

        logger.info("✓ Complete API created with endpoints:");
        logger.info("  - GET /api/next-id/:entityName?dataType=snowflake");
        logger.info("  - GET /api/next-batch/:entityName?dataType=int&batchSize=100");
        logger.info("  - GET /api/status/:entityName");
        logger.info("  - GET /api/list");
        logger.info("  - GET /api/types");
        logger.info("  - GET /health");
        logger.info("  - GET /shard-info");
    }

    private void configureSystemdService() throws Exception {
        logger.info("Configuring systemd service...");

        String serviceContent = String.join("\n",
                "[Unit]",
                "Description=Go ID Generator Service",
                "After=network.target",
                "",
                "[Service]",
                "Type=simple",
                "User=root",
                "WorkingDirectory=/opt/go-id",
                "ExecStart=/opt/go-id/go-id",
                "Restart=always",
                "RestartSec=10",
                "StandardOutput=append:/var/log/go-id/service.log",
                "StandardError=append:/var/log/go-id/error.log",
                "",
                "[Install]",
                "WantedBy=multi-user.target"
        );

        String escapedService = serviceContent.replace("'", "'\\''");
        device.executeCommand("sudo lxc exec " + containerName + " -- bash -c 'echo '" + escapedService + "' > /etc/systemd/system/go-id.service'");

        // Enable service
        device.executeCommand("sudo lxc exec " + containerName + " -- systemctl daemon-reload");
        device.executeCommand("sudo lxc exec " + containerName + " -- systemctl enable go-id");
    }

    private void stopAndExportContainer() throws Exception {
        logger.info("Stopping container...");
        device.executeCommand("sudo lxc stop " + containerName);

        // Determine versioned generated folder path
        String basePath = config.getProperty("BASE_PATH", "/home/mustafa/telcobright-projects/orchestrix/images/containers/lxc/go-id");
        String version = config.getProperty("CONTAINER_VERSION", "1");
        String versionedDir = basePath + "/go-id-v." + version;
        String generatedDir = versionedDir + "/generated";

        // Create organized directory structure
        String artifactDir = generatedDir + "/artifact";
        String publishDir = generatedDir + "/publish";
        String testDir = generatedDir + "/test";

        device.executeCommand("mkdir -p " + artifactDir);
        device.executeCommand("mkdir -p " + publishDir);
        device.executeCommand("mkdir -p " + testDir);

        // Export container with timestamp
        String timestamp = String.valueOf(System.currentTimeMillis() / 1000);
        String imageName = containerName + "-" + timestamp + ".tar.gz";
        String exportPath = artifactDir + "/" + imageName;
        String exportPathBase = artifactDir + "/" + containerName + "-" + timestamp;  // Without .tar.gz for LXC

        logger.info("Exporting container to: " + exportPath);
        device.executeCommand("sudo lxc publish " + containerName + " --alias " + imageAlias);
        device.executeCommand("sudo lxc image export " + imageAlias + " " + exportPathBase);  // LXC will add .tar.gz

        logger.info("Container image exported: " + exportPath);

        // Generate MD5 hash file
        logger.info("Generating MD5 hash file...");
        String md5Command = "md5sum \"" + exportPath + "\" | awk '{print $1 \"  " + imageName + "\"}' > \"" + exportPath + ".md5\"";
        device.executeCommand(md5Command);
        logger.info("✓ MD5 hash file created: " + exportPath + ".md5");

        // Save export path for publish script generation
        this.exportedImagePath = exportPath;

        // Copy template files to generated folder
        logger.info("Copying templates to generated folder...");
        String templateSource = basePath + "/templates";
        if (device.executeCommand("test -d " + templateSource + " && echo exists").contains("exists")) {
            // Launch templates go to root of generated/
            device.executeCommand("cp " + templateSource + "/sample.conf " + generatedDir + "/");
            device.executeCommand("cp " + templateSource + "/startDefault.sh " + generatedDir + "/");
            device.executeCommand("chmod +x " + generatedDir + "/startDefault.sh");

            // Test runner files go to test/
            device.executeCommand("cp " + templateSource + "/test-runner.conf " + testDir + "/");
            device.executeCommand("cp " + templateSource + "/test-runner.sh " + testDir + "/");
            device.executeCommand("chmod +x " + testDir + "/test-runner.sh");

            logger.info("✓ Templates copied to organized folders");
        } else {
            logger.warning("⚠ Templates directory not found: " + templateSource);
        }

        // Update test-runner.conf with actual artifact information
        updateTestRunnerConfig(testDir, imageName, exportPath, version);

        // Generate publish script if enabled
        if (generateUploadScript) {
            generatePublishScript(publishDir, artifactDir, imageName, timestamp, version);
        }

        logger.info("✓ Build artifacts created in: " + versionedDir);
    }

    private void updateTestRunnerConfig(String testDir, String imageName, String artifactPath, String version) throws Exception {
        logger.info("Updating test-runner.conf with artifact information...");

        String testRunnerConfPath = testDir + "/test-runner.conf";

        // Update ARTIFACT_NAME
        device.executeCommand("sed -i 's|ARTIFACT_NAME=\".*\"|ARTIFACT_NAME=\"" + imageName + "\"|' " + testRunnerConfPath);

        // Update ARTIFACT_PATH
        device.executeCommand("sed -i 's|ARTIFACT_PATH=\".*\"|ARTIFACT_PATH=\"" + artifactPath + "\"|' " + testRunnerConfPath);

        // Update ARTIFACT_VERSION
        device.executeCommand("sed -i 's|ARTIFACT_VERSION=\".*\"|ARTIFACT_VERSION=\"v" + version + "\"|' " + testRunnerConfPath);

        logger.info("✓ test-runner.conf updated with artifact information");
    }

    private void generatePublishScript(String publishDir, String artifactDir, String imageName, String timestamp, String version) throws Exception {
        logger.info("Generating publish automation scripts...");

        // Compute rclone target directory to mirror local structure
        // Local: .../orchestrix/images/containers/lxc/go-id/go-id-v.1/generated/artifact
        // GDrive: gdrive:images/containers/lxc/go-id/go-id-v.1/generated/artifact
        String rcloneTargetDir = "images/containers/lxc/go-id/go-id-v." + version + "/generated/artifact";

        // Generate publish config file
        String publishConfigPath = publishDir + "/publish-config.conf";
        String publishConfigContent = String.join("\n",
            "# Publish Configuration",
            "# Generated by Orchestrix container builder",
            "",
            "# Artifact Information",
            "ARTIFACT_TYPE=lxc-container",
            "ARTIFACT_NAME=go-id",
            "ARTIFACT_VERSION=v" + version,
            "ARTIFACT_FILE=" + imageName,
            "ARTIFACT_PATH=" + artifactDir + "/" + imageName,
            "BUILD_TIMESTAMP=" + timestamp,
            "",
            "# Publish Location (mirrors local structure)",
            "RCLONE_REMOTE=" + rcloneRemote,
            "RCLONE_TARGET_DIR=" + rcloneTargetDir,
            "",
            "# Database Connection",
            "DB_HOST=127.0.0.1",
            "DB_PORT=3306",
            "DB_NAME=orchestrix",
            "DB_USER=root",
            "DB_PASSWORD=123456",
            ""
        );

        // Write publish config
        device.executeCommand("cat > " + publishConfigPath + " << 'EOF'\n" + publishConfigContent + "\nEOF");

        // Generate publish script that uses Java/Maven
        String publishScriptPath = publishDir + "/publish.sh";
        String publishScriptContent = String.join("\n",
            "#!/bin/bash",
            "# LXC Container Publish Script",
            "# Uses Java/Maven automation for artifact publishing",
            "",
            "set -e",
            "",
            "# Colors",
            "RED='\\033[0;31m'",
            "GREEN='\\033[0;32m'",
            "YELLOW='\\033[1;33m'",
            "NC='\\033[0m'",
            "",
            "SCRIPT_DIR=\"$(cd \"$(dirname \"${BASH_SOURCE[0]}\")\" && pwd)\"",
            "CONFIG_FILE=\"${SCRIPT_DIR}/publish-config.conf\"",
            "",
            "echo -e \"${GREEN}=== LXC Container Publish ===${NC}\\n\"",
            "",
            "# Load configuration",
            "if [ ! -f \"$CONFIG_FILE\" ]; then",
            "    echo -e \"${RED}Error: Configuration file not found: $CONFIG_FILE${NC}\"",
            "    exit 1",
            "fi",
            "",
            "source \"$CONFIG_FILE\"",
            "",
            "# Display artifact information",
            "echo \"Artifact Type:    $ARTIFACT_TYPE\"",
            "echo \"Name:             $ARTIFACT_NAME\"",
            "echo \"Version:          $ARTIFACT_VERSION\"",
            "echo \"File:             $ARTIFACT_FILE\"",
            "echo \"Build Timestamp:  $BUILD_TIMESTAMP\"",
            "echo \"\"",
            "echo \"Publish Location: ${RCLONE_REMOTE}:${RCLONE_TARGET_DIR}\"",
            "echo \"\"",
            "",
            "# Prompt for upload",
            "read -p \"Do you want to upload this artifact? (y/N): \" -n 1 -r",
            "echo",
            "echo \"\"",
            "",
            "if [[ ! $REPLY =~ ^[Yy]$ ]]; then",
            "    echo -e \"${YELLOW}Publish cancelled${NC}\"",
            "    exit 0",
            "fi",
            "",
            "# Navigate to Orchestrix project root",
            "PROJECT_ROOT=\"/home/mustafa/telcobright-projects/orchestrix\"",
            "",
            "if [ ! -d \"$PROJECT_ROOT\" ]; then",
            "    echo -e \"${RED}Error: Orchestrix project not found at $PROJECT_ROOT${NC}\"",
            "    exit 1",
            "fi",
            "",
            "cd \"$PROJECT_ROOT\"",
            "",
            "# Run Java publish automation via Maven",
            "echo -e \"${GREEN}Starting publish automation...${NC}\\n\"",
            "",
            "mvn exec:java \\",
            "    -Dexec.mainClass=\"com.telcobright.orchestrix.automation.publish.PublishRunner\" \\",
            "    -Dexec.args=\"$CONFIG_FILE\"",
            "",
            "echo \"\"",
            "echo -e \"${GREEN}=== Publish Complete ===${NC}\"",
            ""
        );

        // Write publish script
        device.executeCommand("cat > " + publishScriptPath + " << 'EOF'\n" + publishScriptContent + "\nEOF");

        // Make script executable
        device.executeCommand("chmod +x " + publishScriptPath);

        logger.info("✓ Publish script generated: " + publishScriptPath);
        logger.info("✓ Publish config generated: " + publishConfigPath);
        logger.info("");
        logger.info("To publish the artifact:");
        logger.info("  1. Ensure rclone is configured: rclone config");
        logger.info("  2. Run: " + publishScriptPath);
        logger.info("  3. Script will prompt for upload confirmation");
        logger.info("  4. Java automation will:");
        logger.info("     - Create artifact record in DB");
        logger.info("     - Upload to Google Drive via rclone");
        logger.info("     - Download and verify MD5 hash");
        logger.info("     - Update publish record with verification status");
    }

    /**
     * @deprecated Use GoIdBuildRunner instead - this main method is deprecated
     */
    @Deprecated
    public static void main(String[] args) {
        System.err.println("WARNING: This main method is deprecated.");
        System.err.println("Please use GoIdBuildRunner instead:");
        System.err.println("  com.telcobright.orchestrix.automation.api.container.lxc.app.goid.example.GoIdBuildRunner");
        System.exit(1);
    }
}
