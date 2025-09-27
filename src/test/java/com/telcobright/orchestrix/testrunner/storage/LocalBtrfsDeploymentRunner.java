package com.telcobright.orchestrix.testrunner.storage;

import com.telcobright.orchestrix.automation.storage.base.*;
import com.telcobright.orchestrix.automation.storage.btrfs.*;
import com.telcobright.orchestrix.automation.devices.server.linux.containerdeploy.base.*;
import com.telcobright.orchestrix.automation.devices.server.linux.containerdeploy.lxc.impl.LxcContainerDeployment;
import com.telcobright.orchestrix.automation.devices.server.linux.containerdeploy.lxc.preparer.LxcPrerequisitePreparer;
import com.telcobright.orchestrix.automation.devices.server.linux.base.LinuxDistribution;
import com.telcobright.orchestrix.device.SshDevice;
import com.telcobright.orchestrix.device.LocalSshDevice;

import java.util.Properties;
import java.util.Map;
import java.util.HashMap;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.util.logging.ConsoleHandler;
import java.util.logging.SimpleFormatter;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.File;

/**
 * Test runner for local BTRFS container deployment on Ubuntu 24.04
 * Deploys containers on the local machine using localhost SSH
 */
public class LocalBtrfsDeploymentRunner {

    private static final Logger logger = Logger.getLogger(LocalBtrfsDeploymentRunner.class.getName());

    static {
        // Configure detailed logging
        ConsoleHandler consoleHandler = new ConsoleHandler();
        consoleHandler.setLevel(Level.ALL);
        consoleHandler.setFormatter(new SimpleFormatter());

        Logger rootLogger = Logger.getLogger("");
        rootLogger.setLevel(Level.INFO);
        rootLogger.addHandler(consoleHandler);
    }

    private final Properties config;
    private final SshDevice device;
    private final boolean useSudo;
    private StorageProvider storageProvider;
    private ContainerDeploymentAutomation containerDeployment;

    /**
     * Create runner from configuration file
     */
    public LocalBtrfsDeploymentRunner(String configPath) throws IOException {
        this.config = new Properties();

        File configFile = new File(configPath);
        if (!configFile.exists()) {
            throw new IOException("Configuration file not found: " + configPath);
        }

        try (FileInputStream fis = new FileInputStream(configFile)) {
            config.load(fis);
        }

        // Create local SSH device
        this.device = createLocalSshDevice();
        this.useSudo = Boolean.parseBoolean(config.getProperty("use_sudo", "true"));

        logger.info("Loaded configuration from: " + configPath);
        logger.info("Target: " + device.getHost() + " (local machine)");
    }

    /**
     * Create SSH device for localhost connection
     */
    private SshDevice createLocalSshDevice() {
        String host = config.getProperty("ssh.host", "localhost");
        int port = Integer.parseInt(config.getProperty("ssh.port", "22"));
        String username = config.getProperty("ssh.username", System.getProperty("user.name"));
        String password = config.getProperty("ssh.password", "");
        String keyPath = config.getProperty("ssh.key_path",
            System.getProperty("user.home") + "/.ssh/id_rsa");

        LocalSshDevice localDevice = new LocalSshDevice();
        localDevice.setHost(host);
        localDevice.setPort(port);
        localDevice.setUsername(username);

        // Try key authentication first
        File keyFile = new File(keyPath);
        if (keyFile.exists()) {
            localDevice.setPrivateKeyPath(keyPath);
            logger.info("Using SSH key authentication: " + keyPath);
        } else if (!password.isEmpty()) {
            localDevice.setPassword(password);
            logger.info("Using password authentication");
        } else {
            logger.warning("No authentication method configured, may fail to connect");
        }

        return localDevice;
    }

    /**
     * Execute the deployment
     */
    public boolean execute() {
        logger.info("========================================");
        logger.info("Starting Local BTRFS Container Deployment");
        logger.info("========================================");

        try {
            // Step 1: Check system requirements
            if (!checkSystemRequirements()) {
                logger.severe("System requirements not met");
                return false;
            }

            // Step 2: Connect to local machine via SSH
            if (!connectToDevice()) {
                logger.severe("Failed to connect to local machine");
                return false;
            }

            // Step 3: Verify BTRFS setup
            if (!verifyBtrfsSetup()) {
                logger.severe("BTRFS setup verification failed");
                logger.info("Run: sudo ./scripts/setup-btrfs-local.sh");
                return false;
            }

            // Step 4: Setup storage provider
            if (!setupStorageProvider()) {
                logger.severe("Failed to setup storage provider");
                return false;
            }

            // Step 5: Create storage volume
            if (!createStorageVolume()) {
                logger.severe("Failed to create storage volume");
                return false;
            }

            // Step 6: Prepare container deployment
            if (!prepareContainerDeployment()) {
                logger.severe("Failed to prepare container deployment");
                return false;
            }

            // Step 7: Deploy container
            if (!deployContainer()) {
                logger.severe("Failed to deploy container");
                return false;
            }

            // Step 8: Configure storage mount
            if (!configureStorageMount()) {
                logger.severe("Failed to configure storage mount");
                return false;
            }

            // Step 9: Verify deployment
            if (!verifyDeployment()) {
                logger.severe("Deployment verification failed");
                return false;
            }

            // Step 10: Display deployment info
            displayDeploymentInfo();

            logger.info("========================================");
            logger.info("Deployment completed successfully!");
            logger.info("========================================");
            return true;

        } catch (Exception e) {
            logger.severe("Deployment failed with exception: " + e.getMessage());
            e.printStackTrace();
            return false;
        } finally {
            disconnectFromDevice();
        }
    }

    /**
     * Check system requirements
     */
    private boolean checkSystemRequirements() throws Exception {
        logger.info("Checking system requirements...");

        // Check Ubuntu version
        String osRelease = executeLocalCommand("lsb_release -rs");
        if (osRelease != null) {
            logger.info("Ubuntu version: " + osRelease.trim());
            if (!osRelease.trim().equals("24.04")) {
                logger.warning("This runner is optimized for Ubuntu 24.04");
            }
        }

        // Check if running as appropriate user
        String currentUser = System.getProperty("user.name");
        logger.info("Running as user: " + currentUser);

        // Check if storage locations config exists
        File storageConfig = new File("/etc/orchestrix/storage-locations.conf");
        if (!storageConfig.exists()) {
            logger.warning("Storage configuration not found at: " + storageConfig.getPath());
            logger.info("Please run: sudo ./scripts/setup-btrfs-local.sh");
            return false;
        }

        return true;
    }

    /**
     * Connect to local device via SSH
     */
    private boolean connectToDevice() throws Exception {
        logger.info("Connecting to localhost via SSH...");

        // First check if SSH service is running
        String sshStatus = executeLocalCommand("systemctl is-active ssh || systemctl is-active sshd");
        if (sshStatus == null || !sshStatus.trim().equals("active")) {
            logger.warning("SSH service not running, starting it...");
            executeLocalCommand("sudo systemctl start ssh || sudo systemctl start sshd");
        }

        return device.connect();
    }

    /**
     * Verify BTRFS setup
     */
    private boolean verifyBtrfsSetup() throws Exception {
        logger.info("Verifying BTRFS setup...");

        // Check if BTRFS tools are installed
        String btrfsVersion = device.sendAndReceive("btrfs --version").get();
        if (btrfsVersion == null || !btrfsVersion.contains("btrfs")) {
            logger.severe("BTRFS tools not installed");
            return false;
        }
        logger.info("BTRFS version: " + btrfsVersion.trim());

        // Check if kernel module is loaded
        String lsmod = device.sendAndReceive("lsmod | grep btrfs").get();
        if (lsmod == null || lsmod.isEmpty()) {
            logger.warning("BTRFS kernel module not loaded");
            device.sendAndReceive("sudo modprobe btrfs").get();
        }

        // Check storage directory
        String storageRoot = config.getProperty("storage.root.path",
            System.getProperty("user.home") + "/telcobright/btrfs");

        String checkDir = device.sendAndReceive("test -d " + storageRoot + " && echo exists").get();
        if (checkDir == null || !checkDir.contains("exists")) {
            logger.warning("Storage directory not found: " + storageRoot);
            logger.info("Creating storage directory...");
            device.sendAndReceive("mkdir -p " + storageRoot + "/containers").get();
            device.sendAndReceive("mkdir -p " + storageRoot + "/snapshots").get();
            device.sendAndReceive("mkdir -p " + storageRoot + "/backups").get();
        }

        return true;
    }

    /**
     * Setup storage provider
     */
    private boolean setupStorageProvider() throws Exception {
        logger.info("Setting up BTRFS storage provider...");

        // Create BTRFS storage provider
        storageProvider = new BtrfsStorageProvider(LinuxDistribution.UBUNTU, useSudo);

        // Verify it's working
        if (!storageProvider.verifyHealth(device)) {
            logger.warning("BTRFS health check failed, attempting to fix...");

            // Try to install if needed
            if (!storageProvider.isInstalled(device)) {
                logger.info("Installing BTRFS...");
                storageProvider.install(device);
            }
        }

        return true;
    }

    /**
     * Create storage volume
     */
    private boolean createStorageVolume() throws Exception {
        logger.info("Creating storage volume...");

        // Build volume configuration
        String locationId = config.getProperty("storage.location.id", "btrfs_local_main");
        String containerRoot = config.getProperty("storage.container.root", "test-container");
        String quotaSize = config.getProperty("storage.quota.size", "10G");

        StorageVolumeConfig volumeConfig = new StorageVolumeConfig.Builder()
            .locationId(locationId)
            .containerRoot(containerRoot)
            .quotaHumanSize(quotaSize)
            .compression(Boolean.parseBoolean(config.getProperty("storage.compression", "true")))
            .snapshotEnabled(Boolean.parseBoolean(config.getProperty("storage.snapshot.enabled", "false")))
            .build();

        logger.info("Volume configuration:");
        logger.info("  Location: " + locationId);
        logger.info("  Root: " + containerRoot);
        logger.info("  Quota: " + quotaSize);

        // Create the volume
        StorageVolume volume = storageProvider.createVolume(device, volumeConfig);

        // Store volume path for later use
        config.setProperty("_volume_path", volume.getPath());

        logger.info("Volume created at: " + volume.getPath());
        return true;
    }

    /**
     * Prepare container deployment
     */
    private boolean prepareContainerDeployment() throws Exception {
        logger.info("Preparing container deployment...");

        // Create container configuration
        String containerName = config.getProperty("container.name", "test-container");
        String imagePath = config.getProperty("container.image.path");

        ContainerConfig.Builder configBuilder = new ContainerConfig.Builder()
            .containerName(containerName)
            .autoStart(Boolean.parseBoolean(config.getProperty("container.autostart", "false")));

        // Set image source
        if (imagePath != null && !imagePath.isEmpty()) {
            configBuilder.imagePath(imagePath);
        } else {
            String imageUrl = config.getProperty("container.image.url");
            if (imageUrl != null && !imageUrl.isEmpty()) {
                configBuilder.imageUrl(imageUrl);
            }
        }

        // Set resource limits
        String memLimit = config.getProperty("container.limits.memory");
        if (memLimit != null && !memLimit.isEmpty()) {
            configBuilder.memoryLimit(parseMemoryLimit(memLimit));
        }

        String cpuLimit = config.getProperty("container.limits.cpu");
        if (cpuLimit != null && !cpuLimit.isEmpty()) {
            configBuilder.cpuShares(Integer.parseInt(cpuLimit));
        }

        ContainerConfig containerConfig = configBuilder.build();

        // Check if we need to prepare LXC prerequisites
        boolean preparePrereqs = Boolean.parseBoolean(
            config.getProperty("lxc.prepare_prerequisites", "false"));

        if (preparePrereqs) {
            logger.info("Preparing LXC prerequisites...");

            LxcPrerequisitePreparer.PrerequisiteConfig prereqConfig =
                new LxcPrerequisitePreparer.PrerequisiteConfig.Builder()
                    .requireLxd(true)
                    .requireSnapPath(false)  // Ubuntu 24.04 handles this well
                    .requireIpForward(true)
                    .requireBridge(true)
                    .requireMasquerade(true)
                    .bridgeNetwork("10.0.3.0/24")
                    .bridgeName("lxdbr0")
                    .build();

            containerDeployment = new LxcContainerDeployment(
                containerConfig, prereqConfig, useSudo);
        } else {
            containerDeployment = new LxcContainerDeployment(containerConfig, useSudo);
        }

        return true;
    }

    /**
     * Deploy the container
     */
    private boolean deployContainer() throws Exception {
        logger.info("Deploying container...");
        return containerDeployment.execute(device);
    }

    /**
     * Configure storage mount
     */
    private boolean configureStorageMount() throws Exception {
        logger.info("Configuring storage mount...");

        String containerName = config.getProperty("container.name");
        String volumePath = config.getProperty("_volume_path");

        if (volumePath == null) {
            logger.warning("No volume path found, skipping mount configuration");
            return true;
        }

        // Create mount configuration
        StorageVolumeConfig volumeConfig = new StorageVolumeConfig.Builder()
            .locationId(config.getProperty("storage.location.id", "btrfs_local_main"))
            .containerRoot(config.getProperty("storage.container.root", "test-container"))
            .quotaHumanSize(config.getProperty("storage.quota.size", "10G"))
            .build();

        LxcContainerBtrfsMountAutomation mountAutomation =
            new LxcContainerBtrfsMountAutomation(
                LinuxDistribution.UBUNTU, volumeConfig, containerName, useSudo);

        return mountAutomation.execute(device);
    }

    /**
     * Verify deployment
     */
    private boolean verifyDeployment() throws Exception {
        logger.info("Verifying deployment...");
        return containerDeployment.verify(device);
    }

    /**
     * Display deployment information
     */
    private void displayDeploymentInfo() throws Exception {
        String containerName = config.getProperty("container.name");

        logger.info("");
        logger.info("Deployment Information:");
        logger.info("=======================");

        // Get container info
        String containerInfo = device.sendAndReceive("lxc list " + containerName + " --format csv").get();
        if (containerInfo != null && !containerInfo.isEmpty()) {
            String[] parts = containerInfo.split(",");
            if (parts.length > 2) {
                logger.info("Container: " + parts[0]);
                logger.info("State: " + parts[1]);
                logger.info("IP Address: " + parts[2]);
            }
        }

        // Get storage info
        String volumePath = config.getProperty("_volume_path");
        if (volumePath != null) {
            logger.info("Storage Volume: " + volumePath);

            Map<String, Object> stats = storageProvider.getVolumeStats(device, volumePath);
            if (stats.containsKey("referenced_bytes")) {
                long bytes = (Long) stats.get("referenced_bytes");
                logger.info("Storage Used: " + humanReadableBytes(bytes));
            }
        }

        logger.info("");
        logger.info("To access the container:");
        logger.info("  lxc exec " + containerName + " -- /bin/bash");
        logger.info("");
        logger.info("To stop the container:");
        logger.info("  lxc stop " + containerName);
    }

    /**
     * Disconnect from device
     */
    private void disconnectFromDevice() {
        try {
            if (device != null) {
                device.disconnect();
            }
        } catch (Exception e) {
            logger.warning("Error disconnecting: " + e.getMessage());
        }
    }

    /**
     * Execute command locally
     */
    private String executeLocalCommand(String command) {
        try {
            ProcessBuilder pb = new ProcessBuilder("bash", "-c", command);
            Process process = pb.start();
            process.waitFor();

            byte[] output = process.getInputStream().readAllBytes();
            return new String(output);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Parse memory limit
     */
    private Long parseMemoryLimit(String limit) {
        if (limit == null || limit.isEmpty()) return null;

        limit = limit.toUpperCase().trim();
        long multiplier = 1;

        if (limit.endsWith("G")) {
            multiplier = 1024L * 1024L * 1024L;
            limit = limit.substring(0, limit.length() - 1);
        } else if (limit.endsWith("M")) {
            multiplier = 1024L * 1024L;
            limit = limit.substring(0, limit.length() - 1);
        }

        return (long)(Double.parseDouble(limit) * multiplier);
    }

    /**
     * Convert bytes to human readable format
     */
    private String humanReadableBytes(long bytes) {
        if (bytes < 1024) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(1024));
        String pre = "KMGTPE".charAt(exp-1) + "";
        return String.format("%.1f %sB", bytes / Math.pow(1024, exp), pre);
    }

    /**
     * Main method for testing
     */
    public static void main(String[] args) {
        if (args.length != 1) {
            System.err.println("Usage: java LocalBtrfsDeploymentRunner <config-file>");
            System.err.println("");
            System.err.println("Example:");
            System.err.println("  java LocalBtrfsDeploymentRunner local-deployment.properties");
            System.exit(1);
        }

        try {
            LocalBtrfsDeploymentRunner runner = new LocalBtrfsDeploymentRunner(args[0]);
            boolean success = runner.execute();
            System.exit(success ? 0 : 1);
        } catch (Exception e) {
            System.err.println("Failed to run deployment: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}