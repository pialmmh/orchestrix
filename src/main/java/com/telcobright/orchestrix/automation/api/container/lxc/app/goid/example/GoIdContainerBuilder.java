package com.telcobright.orchestrix.automation.api.container.lxc.app.goid.example;

import com.telcobright.orchestrix.automation.core.PrerequisiteChecker;
import com.telcobright.orchestrix.automation.core.storage.btrfs.BtrfsStorageProvider;
import com.telcobright.orchestrix.automation.core.storage.base.StorageVolume;
import com.telcobright.orchestrix.automation.core.storage.base.StorageVolumeConfig;
import com.telcobright.orchestrix.device.LocalSshDevice;

import java.io.File;
import java.util.Properties;
import java.util.logging.Logger;

/**
 * Java automation for building Go-based Unique ID Generator Container
 *
 * <p>Features:
 * <ul>
 *   <li>Go-based REST API for unique ID generation</li>
 *   <li>Snowflake algorithm for distributed ID generation</li>
 *   <li>Small image size (10-30MB vs 150-300MB Node.js)</li>
 *   <li>High concurrency with goroutines</li>
 *   <li>BTRFS storage with quota management</li>
 *   <li>Systemd service for auto-start</li>
 * </ul>
 */
public class GoIdContainerBuilder {

    private static final Logger logger = Logger.getLogger(GoIdContainerBuilder.class.getName());

    private LocalSshDevice device;
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

    public GoIdContainerBuilder(String configFile) throws Exception {
        this.device = new LocalSshDevice();
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

        // Wait for container to be ready
        Thread.sleep(5000);

        // Wait for network
        for (int i = 0; i < 30; i++) {
            String result = device.executeCommand("sudo lxc exec " + containerName + " -- ping -c 1 8.8.8.8 2>/dev/null || true");
            if (result != null && result.contains("1 packets transmitted, 1 received")) {
                logger.info("Container network ready");
                return;
            }
            Thread.sleep(2000);
        }
        throw new Exception("Container network not ready after 60 seconds");
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
        logger.info("Creating Go ID Generator service...");

        // Create directories
        device.executeCommand("sudo lxc exec " + containerName + " -- mkdir -p /opt/go-id");
        device.executeCommand("sudo lxc exec " + containerName + " -- mkdir -p /var/log/go-id");

        // Create Go source code using Sonyflake library (same config as Node.js version)
        String goCode = String.join("\n",
                "package main",
                "",
                "import (",
                "    \"encoding/json\"",
                "    \"fmt\"",
                "    \"log\"",
                "    \"net/http\"",
                "    \"os\"",
                "    \"strconv\"",
                "    \"time\"",
                "",
                "    \"github.com/sony/sonyflake\"",
                ")",
                "",
                "var sf *sonyflake.Sonyflake",
                "",
                "// Get shard ID from environment (compatible with Node.js config: SHARD_ID)",
                "func getShardID() uint16 {",
                "    if shardID := os.Getenv(\"SHARD_ID\"); shardID != \"\" {",
                "        id, err := strconv.ParseUint(shardID, 10, 16)",
                "        if err == nil && id > 0 && id < 65536 {",
                "            return uint16(id)",
                "        }",
                "        log.Printf(\"WARNING: Invalid SHARD_ID=%s. Must be 1-65535. Using default=1\", shardID)",
                "    }",
                "    log.Println(\"WARNING: SHARD_ID not set. Using default=1. Set SHARD_ID env for distributed setup.\")",
                "    return 1",
                "}",
                "",
                "func init() {",
                "    shardID := getShardID()",
                "    log.Printf(\"Initializing Sonyflake with SHARD_ID=%d\", shardID)",
                "",
                "    // Configure Sonyflake with custom settings",
                "    // - Custom time source support (for NTP or clock rollback handling)",
                "    // - Configurable machine ID from SHARD_ID env var",
                "    // - 16-bit machine ID (vs 10-bit in Twitter Snowflake)",
                "    settings := sonyflake.Settings{",
                "        StartTime: time.Date(2022, 1, 1, 0, 0, 0, 0, time.UTC),",
                "        MachineID: func() (uint16, error) {",
                "            return shardID, nil",
                "        },",
                "        CheckMachineID: func(id uint16) bool {",
                "            // Validate machine ID is in allowed range",
                "            return id > 0 && id < 65536",
                "        },",
                "    }",
                "",
                "    var err error",
                "    sf = sonyflake.NewSonyflake(settings)",
                "    if sf == nil {",
                "        log.Fatalf(\"Failed to initialize Sonyflake: %v\", err)",
                "    }",
                "}",
                "",
                "func generateHandler(w http.ResponseWriter, r *http.Request) {",
                "    id, err := sf.NextID()",
                "    if err != nil {",
                "        log.Printf(\"Error generating ID: %v\", err)",
                "        http.Error(w, fmt.Sprintf(\"Error generating ID: %v\", err), http.StatusInternalServerError)",
                "        return",
                "    }",
                "",
                "    response := map[string]interface{}{",
                "        \"id\":        id,",
                "        \"timestamp\": time.Now().Unix(),",
                "    }",
                "    w.Header().Set(\"Content-Type\", \"application/json\")",
                "    json.NewEncoder(w).Encode(response)",
                "}",
                "",
                "func healthHandler(w http.ResponseWriter, r *http.Request) {",
                "    w.WriteHeader(http.StatusOK)",
                "    fmt.Fprintf(w, \"OK\")",
                "}",
                "",
                "func main() {",
                "    http.HandleFunc(\"/generate\", generateHandler)",
                "    http.HandleFunc(\"/health\", healthHandler)",
                "    log.Printf(\"Starting Go ID Generator on port " + servicePort + "\")",
                "    log.Fatal(http.ListenAndServe(\":" + servicePort + "\", nil))",
                "}"
        );

        // Write Go code to file
        String escapedCode = goCode.replace("'", "'\\''");
        device.executeCommand("sudo lxc exec " + containerName + " -- bash -c 'echo '" + escapedCode + "' > /opt/go-id/main.go'");

        // Initialize Go module and install Sonyflake
        logger.info("Installing Sonyflake dependency...");
        device.executeCommand("sudo lxc exec " + containerName + " -- bash -c 'cd /opt/go-id && /usr/local/go/bin/go mod init go-id-service'");
        device.executeCommand("sudo lxc exec " + containerName + " -- bash -c 'cd /opt/go-id && /usr/local/go/bin/go get github.com/sony/sonyflake@latest'");
        device.executeCommand("sudo lxc exec " + containerName + " -- bash -c 'cd /opt/go-id && /usr/local/go/bin/go mod tidy'");

        // Compile Go binary with Sonyflake
        logger.info("Compiling Go binary with Sonyflake...");
        device.executeCommand("sudo lxc exec " + containerName + " -- bash -c 'cd /opt/go-id && /usr/local/go/bin/go build -o go-id main.go'");

        // Verify binary
        device.executeCommand("sudo lxc exec " + containerName + " -- chmod +x /opt/go-id/go-id");
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

        // Export container
        String exportPath = config.getProperty("EXPORT_PATH", "/tmp") + "/" + containerName + "-" +
                            System.currentTimeMillis() / 1000 + ".tar.gz";
        logger.info("Exporting container to: " + exportPath);
        device.executeCommand("sudo lxc publish " + containerName + " --alias " + imageAlias);
        device.executeCommand("sudo lxc image export " + imageAlias + " " + exportPath);

        logger.info("Container image exported: " + exportPath);
    }

    public static void main(String[] args) {
        try {
            String configFile = args.length > 0 ? args[0] :
                "/home/mustafa/telcobright-projects/orchestrix/images/lxc/go-id/build/build.conf";

            GoIdContainerBuilder builder = new GoIdContainerBuilder(configFile);
            builder.build();

        } catch (Exception e) {
            Logger.getLogger(GoIdContainerBuilder.class.getName()).severe("Build failed: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}
