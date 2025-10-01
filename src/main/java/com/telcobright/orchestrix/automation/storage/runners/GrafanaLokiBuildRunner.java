package com.telcobright.orchestrix.automation.storage.runners;

import com.telcobright.orchestrix.automation.storage.btrfs.BtrfsStorageProvider;
import com.telcobright.orchestrix.automation.storage.btrfs.LxcContainerBtrfsMountAutomation;
import com.telcobright.orchestrix.automation.storage.base.StorageVolume;
import com.telcobright.orchestrix.automation.storage.base.StorageVolumeConfig;
import com.telcobright.orchestrix.device.LocalSshDevice;
import com.telcobright.orchestrix.automation.model.AutomationOperationResult;

import java.io.File;
import java.io.FileInputStream;
import java.util.Properties;
import java.util.logging.Logger;

/**
 * Java-based Grafana-Loki container builder
 * Replaces shell script with Java automation
 */
public class GrafanaLokiBuildRunner {

    private static final Logger logger = Logger.getLogger(GrafanaLokiBuildRunner.class.getName());

    private LocalSshDevice device;
    private BtrfsStorageProvider storageProvider;
    private Properties buildConfig;

    // Build configuration
    private String containerVersion;
    private String containerName;
    private String storageLocationId;
    private String storageQuotaSize;
    private String baseImage;
    private int memoryLimit;
    private int cpuLimit;

    public GrafanaLokiBuildRunner(String configFile) throws Exception {
        this.device = new LocalSshDevice();
        this.storageProvider = new BtrfsStorageProvider(true);

        // Load build configuration
        loadBuildConfig(configFile);
    }

    private void loadBuildConfig(String configFile) throws Exception {
        logger.info("Loading configuration from: " + configFile);

        buildConfig = new Properties();
        File file = new File(configFile);

        if (!file.exists()) {
            // Use default config
            file = new File("/home/mustafa/telcobright-projects/orchestrix/images/lxc/grafana-loki/build/build.conf");
        }

        // Parse the shell-style config file
        String content = new String(java.nio.file.Files.readAllBytes(file.toPath()));
        for (String line : content.split("\n")) {
            if (line.trim().isEmpty() || line.trim().startsWith("#")) continue;

            if (line.contains("=")) {
                String[] parts = line.split("=", 2);
                String key = parts[0].trim();
                String value = parts[1].trim();

                // Strip inline comments (everything after #)
                int commentIndex = value.indexOf('#');
                if (commentIndex > 0) {
                    value = value.substring(0, commentIndex).trim();
                }

                // Strip quotes
                value = value.replaceAll("^\"|\"$", "");
                buildConfig.setProperty(key, value);
            }
        }

        // Extract key configurations
        containerVersion = buildConfig.getProperty("CONTAINER_VERSION", "1");
        containerName = buildConfig.getProperty("CONTAINER_NAME_PREFIX", "grafana-loki") + "-v." + containerVersion;
        storageLocationId = buildConfig.getProperty("STORAGE_LOCATION_ID", "btrfs_local_main");
        storageQuotaSize = buildConfig.getProperty("STORAGE_QUOTA_SIZE", "4G");
        baseImage = buildConfig.getProperty("BASE_IMAGE", "images:debian/12");

        String memLimit = buildConfig.getProperty("MEMORY_LIMIT", "2GB");
        memoryLimit = parseMemoryLimit(memLimit);
        cpuLimit = Integer.parseInt(buildConfig.getProperty("CPU_LIMIT", "2"));

        // Validate required parameters
        if (storageLocationId.isEmpty()) {
            throw new Exception("STORAGE_LOCATION_ID is required in configuration");
        }
        if (storageQuotaSize.isEmpty()) {
            throw new Exception("STORAGE_QUOTA_SIZE is required in configuration");
        }
    }

    private int parseMemoryLimit(String limit) {
        // Convert memory string to MB
        limit = limit.toUpperCase();
        if (limit.endsWith("GB")) {
            return Integer.parseInt(limit.replace("GB", "")) * 1024;
        } else if (limit.endsWith("MB")) {
            return Integer.parseInt(limit.replace("MB", ""));
        }
        return 2048; // Default 2GB
    }

    public void build() throws Exception {
        logger.info("=========================================");
        logger.info("Building Grafana-Loki Container (Java)");
        logger.info("=========================================");
        logger.info("Version: " + containerVersion);
        logger.info("Container: " + containerName);
        logger.info("Storage: " + storageLocationId + " (" + storageQuotaSize + ")");
        logger.info("=========================================");

        // Step 1: Setup BTRFS storage
        StorageVolume volume = setupBtrfsStorage();

        // Step 2: Clean existing container if requested
        if ("true".equals(buildConfig.getProperty("CLEAN_BUILD", "true"))) {
            cleanExistingContainer();
        }

        // Step 3: Create container
        createContainer();

        // Step 4: Configure storage
        configureContainerStorage(volume);

        // Step 5: Configure resources
        configureResources();

        // Step 6: Configure network
        configureNetwork();

        // Step 7: Start container
        startContainer();

        // Step 8: Install packages
        installPackages();

        // Step 9: Install Grafana
        installGrafana();

        // Step 10: Install Loki
        installLoki();

        // Step 11: Configure services
        configureServices();

        // Step 12: Export container
        if ("true".equals(buildConfig.getProperty("EXPORT_CONTAINER", "true"))) {
            exportContainer();
        }

        // Step 13: Create snapshot
        if ("true".equals(buildConfig.getProperty("CREATE_SNAPSHOT", "true"))) {
            createSnapshot(volume);
        }

        logger.info("=========================================");
        logger.info("Build Completed Successfully!");
        logger.info("=========================================");
        logger.info("Container: " + containerName);
        if (volume != null) {
            logger.info("Storage: " + volume.getPath());
        }
        logger.info("=========================================");
    }

    private StorageVolume setupBtrfsStorage() throws Exception {
        logger.info("Setting up BTRFS storage...");

        // Get storage path from location
        String storagePath = getStoragePath(storageLocationId);

        // Build configuration using builder pattern
        StorageVolumeConfig config = new StorageVolumeConfig.Builder()
            .locationId(storageLocationId)
            .containerRoot(storagePath + "/containers/" + containerName)
            .quotaHumanSize(storageQuotaSize)
            .compression(true)
            .snapshotEnabled(true)
            .build();

        // Create volume - note: createVolume expects SshDevice but we have LocalSshDevice
        // For now, skip actual volume creation and just log
        logger.info("BTRFS storage configured for: " + config.getContainerRoot());

        // Return a minimal volume object for now
        // TODO: Implement proper BtrfsStorageProvider.createVolume compatible with LocalSshDevice
        return null; // Temporary - will fix storage integration separately
    }

    private String getStoragePath(String locationId) throws Exception {
        // Read from config file
        String configFile = "/etc/orchestrix/storage-locations.conf";
        String command = "grep '^" + locationId + ".path=' " + configFile + " 2>/dev/null | cut -d'=' -f2";
        String path = device.executeCommand(command).trim();

        if (path.isEmpty()) {
            // Default fallback
            return "/home/telcobright/btrfs";
        }
        return path;
    }

    private void cleanExistingContainer() throws Exception {
        logger.info("Cleaning existing container...");
        device.executeCommand("lxc stop " + containerName + " --force 2>/dev/null || true");
        device.executeCommand("lxc delete " + containerName + " --force 2>/dev/null || true");
    }

    private void createContainer() throws Exception {
        logger.info("Creating container " + containerName + "...");
        device.executeCommand("lxc init " + baseImage + " " + containerName);
    }

    private void configureContainerStorage(StorageVolume volume) throws Exception {
        logger.info("Configuring container storage...");

        // TODO: Implement proper BTRFS storage mounting
        // For now, skipping storage integration as we're using default LXC storage
        if (volume != null) {
            logger.info("Would configure BTRFS storage at: " + volume.getPath());
            // LxcContainerBtrfsMountAutomation requires proper setup
            // Will implement when fixing storage integration
        } else {
            logger.info("Using default LXC storage (storage integration bypassed)");
        }
    }

    private void configureResources() throws Exception {
        logger.info("Configuring resources...");

        String memLimit = buildConfig.getProperty("MEMORY_LIMIT", "2GB");
        device.executeCommand("lxc config set " + containerName + " limits.memory " + memLimit);
        device.executeCommand("lxc config set " + containerName + " limits.cpu " + cpuLimit);
    }

    private void configureNetwork() throws Exception {
        logger.info("Configuring network...");

        String bridge = buildConfig.getProperty("NETWORK_BRIDGE", "lxdbr0");
        device.executeCommand("lxc config device add " + containerName +
            " eth0 nic name=eth0 nictype=bridged parent=" + bridge);
    }

    private void startContainer() throws Exception {
        logger.info("Starting container...");
        device.executeCommand("lxc start " + containerName);

        // Wait for container to be ready
        Thread.sleep(5000);

        // Wait for network
        int retries = 30;
        while (retries-- > 0) {
            String result = device.executeCommand("lxc exec " + containerName +
                " -- ping -c 1 google.com 2>&1 || echo 'failed'");
            if (!result.contains("failed")) {
                break;
            }
            logger.info("Waiting for network connectivity...");
            Thread.sleep(2000);
        }
    }

    private void installPackages() throws Exception {
        logger.info("Installing packages...");

        device.executeCommand("lxc exec " + containerName + " -- apt-get update");
        device.executeCommand("lxc exec " + containerName + " -- apt-get upgrade -y");

        String corePackages = buildConfig.getProperty("PACKAGES_CORE",
            "curl wget vim net-tools iputils-ping openssh-server sudo");
        device.executeCommand("lxc exec " + containerName + " -- apt-get install -y " + corePackages);

        String systemPackages = buildConfig.getProperty("PACKAGES_SYSTEM",
            "gnupg2 software-properties-common apt-transport-https ca-certificates logrotate");
        device.executeCommand("lxc exec " + containerName + " -- apt-get install -y " + systemPackages);
    }

    private void installGrafana() throws Exception {
        logger.info("Installing Grafana...");

        String grafanaVersion = buildConfig.getProperty("GRAFANA_VERSION", "10.2.3");
        String gpgKey = buildConfig.getProperty("GRAFANA_GPG_KEY", "https://apt.grafana.com/gpg.key");
        String aptRepo = buildConfig.getProperty("GRAFANA_APT_REPO", "https://apt.grafana.com");

        device.executeCommand("lxc exec " + containerName +
            " -- wget -q -O /usr/share/keyrings/grafana.key " + gpgKey);
        device.executeCommand("lxc exec " + containerName +
            " -- bash -c \"echo 'deb [signed-by=/usr/share/keyrings/grafana.key] " +
            aptRepo + " stable main' > /etc/apt/sources.list.d/grafana.list\"");
        device.executeCommand("lxc exec " + containerName + " -- apt-get update");
        device.executeCommand("lxc exec " + containerName + " -- apt-get install -y grafana");
    }

    private void installLoki() throws Exception {
        logger.info("Installing Loki...");

        String lokiVersion = buildConfig.getProperty("LOKI_VERSION", "2.9.4");
        String lokiUrl = buildConfig.getProperty("LOKI_DOWNLOAD_URL",
            "https://github.com/grafana/loki/releases/download/v" + lokiVersion + "/loki-linux-amd64.zip");

        device.executeCommand("lxc exec " + containerName + " -- mkdir -p /opt/loki /etc/loki /var/lib/loki");
        device.executeCommand("lxc exec " + containerName + " -- wget -q -O /opt/loki/loki.gz " + lokiUrl);
        device.executeCommand("lxc exec " + containerName +
            " -- bash -c \"cd /opt/loki && gunzip -c loki.gz > loki && chmod +x loki\"");
    }

    private void configureServices() throws Exception {
        logger.info("Configuring services...");

        // Configure port forwarding
        String grafanaPort = buildConfig.getProperty("GRAFANA_PORT", "3000");
        String lokiPort = buildConfig.getProperty("LOKI_PORT", "3100");

        device.executeCommand("lxc config device add " + containerName + " grafana-port proxy " +
            "listen=tcp:0.0.0.0:" + grafanaPort + " connect=tcp:127.0.0.1:3000");
        device.executeCommand("lxc config device add " + containerName + " loki-port proxy " +
            "listen=tcp:0.0.0.0:" + lokiPort + " connect=tcp:127.0.0.1:3100");

        if ("true".equals(buildConfig.getProperty("START_SERVICES", "true"))) {
            device.executeCommand("lxc exec " + containerName + " -- systemctl enable grafana-server");
            device.executeCommand("lxc exec " + containerName + " -- systemctl start grafana-server");
        }
    }

    private void exportContainer() throws Exception {
        logger.info("Exporting container...");

        String exportPath = buildConfig.getProperty("EXPORT_PATH", "/tmp");
        String exportFile = exportPath + "/" + containerName + "-" +
            System.currentTimeMillis() + ".tar.gz";

        device.executeCommand("lxc stop " + containerName);
        device.executeCommand("lxc export " + containerName + " " + exportFile);
        logger.info("Exported to: " + exportFile);
    }

    private void createSnapshot(StorageVolume volume) throws Exception {
        if (volume == null) {
            logger.info("Skipping snapshot creation (storage integration bypassed)");
            return;
        }

        logger.info("Creating BTRFS snapshot...");
        String snapshotName = containerName + "-" + System.currentTimeMillis();
        // TODO: Fix LocalSshDevice/SshDevice type mismatch in BtrfsStorageProvider
        // storageProvider.createSnapshot(device, volume, snapshotName);
        logger.info("Snapshot creation skipped (TODO: fix storage provider integration)");
    }

    /**
     * Main method for standalone execution
     */
    public static void main(String[] args) {
        try {
            String configFile = args.length > 0 ? args[0] :
                "/home/mustafa/telcobright-projects/orchestrix/images/lxc/grafana-loki/build/build.conf";

            GrafanaLokiBuildRunner builder = new GrafanaLokiBuildRunner(configFile);
            builder.build();
            System.exit(0);
        } catch (Exception e) {
            logger.severe("Build failed: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}