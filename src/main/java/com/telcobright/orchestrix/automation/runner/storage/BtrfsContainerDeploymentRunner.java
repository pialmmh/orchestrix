package com.telcobright.orchestrix.automation.runner.storage;

import com.telcobright.orchestrix.automation.devices.server.linux.storage.base.*;
import com.telcobright.orchestrix.automation.devices.server.linux.storage.btrfs.*;
import com.telcobright.orchestrix.automation.devices.server.linux.containerdeploy.base.*;
import com.telcobright.orchestrix.automation.devices.server.linux.containerdeploy.lxc.impl.LxcContainerDeployment;
import com.telcobright.orchestrix.automation.devices.server.linux.containerdeploy.lxc.preparer.LxcPrerequisitePreparer;
import com.telcobright.orchestrix.automation.devices.server.linux.base.LinuxDistribution;
import com.telcobright.orchestrix.automation.devices.server.linux.common.SystemDetector;
import com.telcobright.orchestrix.device.SshDevice;
import com.telcobright.orchestrix.device.SshDeviceImpl;

import java.util.Properties;
import java.util.Map;
import java.util.HashMap;
import java.util.logging.Logger;
import java.io.FileInputStream;
import java.io.IOException;

/**
 * Runner for deploying containers with BTRFS storage
 * Config-driven execution of container deployment with storage management
 */
public class BtrfsContainerDeploymentRunner {

    private static final Logger logger = Logger.getLogger(BtrfsContainerDeploymentRunner.class.getName());

    private final Properties config;
    private final SshDevice device;
    private final boolean useSudo;
    private StorageProvider storageProvider;
    private ContainerDeploymentAutomation containerDeployment;

    /**
     * Create runner from configuration file
     */
    public BtrfsContainerDeploymentRunner(String configPath) throws IOException {
        this.config = new Properties();
        try (FileInputStream fis = new FileInputStream(configPath)) {
            config.load(fis);
        }

        // Create SSH device from config
        this.device = createSshDevice();
        this.useSudo = Boolean.parseBoolean(config.getProperty("ssh.use_sudo", "true"));
    }

    /**
     * Create SSH device from configuration
     */
    private SshDevice createSshDevice() {
        String host = config.getProperty("ssh.host");
        int port = Integer.parseInt(config.getProperty("ssh.port", "22"));
        String username = config.getProperty("ssh.username");
        String password = config.getProperty("ssh.password", "");
        String keyPath = config.getProperty("ssh.key_path", "");

        if (host == null || username == null) {
            throw new IllegalArgumentException("SSH host and username are required");
        }

        SshDeviceImpl sshDevice = new SshDeviceImpl();
        sshDevice.setHost(host);
        sshDevice.setPort(port);
        sshDevice.setUsername(username);

        if (!password.isEmpty()) {
            sshDevice.setPassword(password);
        }
        if (!keyPath.isEmpty()) {
            sshDevice.setPrivateKeyPath(keyPath);
        }

        return sshDevice;
    }

    /**
     * Execute the deployment
     */
    public boolean execute() {
        try {
            logger.info("Starting BTRFS container deployment");
            logger.info("Target: " + device.getHost());

            // Step 1: Connect to device
            if (!connectToDevice()) {
                logger.severe("Failed to connect to device");
                return false;
            }

            // Step 2: Detect system and setup storage provider
            if (!setupStorageProvider()) {
                logger.severe("Failed to setup storage provider");
                return false;
            }

            // Step 3: Prepare storage volume
            if (!prepareStorageVolume()) {
                logger.severe("Failed to prepare storage volume");
                return false;
            }

            // Step 4: Setup container deployment
            if (!setupContainerDeployment()) {
                logger.severe("Failed to setup container deployment");
                return false;
            }

            // Step 5: Deploy container
            if (!deployContainer()) {
                logger.severe("Failed to deploy container");
                return false;
            }

            // Step 6: Configure storage mount
            if (!configureStorageMount()) {
                logger.severe("Failed to configure storage mount");
                return false;
            }

            // Step 7: Verify deployment
            if (!verifyDeployment()) {
                logger.severe("Deployment verification failed");
                return false;
            }

            logger.info("Container deployment completed successfully");
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
     * Connect to SSH device
     */
    private boolean connectToDevice() throws Exception {
        logger.info("Connecting to " + device.getHost() + ":" + device.getPort());
        return device.connect();
    }

    /**
     * Setup storage provider
     */
    private boolean setupStorageProvider() throws Exception {
        logger.info("Setting up storage provider");

        // Detect Linux distribution
        LinuxDistribution distribution = SystemDetector.detectDistribution(device);
        logger.info("Detected distribution: " + distribution);

        // Create storage provider (BTRFS mandatory for now)
        StorageTechnology storageTech = StorageTechnology.fromCode(
            config.getProperty("storage.provider", "btrfs"));

        storageProvider = StorageProviderFactory.createStorageProvider(
            storageTech, device, useSudo);

        // Install if not present
        if (!storageProvider.isInstalled(device)) {
            logger.info("Installing storage provider: " + storageTech);
            if (!storageProvider.install(device)) {
                logger.severe("Failed to install storage provider");
                return false;
            }
        }

        return storageProvider.verifyHealth(device);
    }

    /**
     * Prepare storage volume for container
     */
    private boolean prepareStorageVolume() throws Exception {
        logger.info("Preparing storage volume");

        // Load storage configuration
        StorageVolumeConfig volumeConfig = new StorageVolumeConfig.Builder()
            .locationId(config.getProperty("storage.location.id"))
            .containerRoot(config.getProperty("storage.container.root"))
            .quotaHumanSize(config.getProperty("storage.quota.size"))
            .compression(Boolean.parseBoolean(config.getProperty("storage.compression", "false")))
            .snapshotEnabled(Boolean.parseBoolean(config.getProperty("storage.snapshot.enabled", "false")))
            .snapshotFrequency(config.getProperty("storage.snapshot.frequency", "daily"))
            .snapshotRetention(Integer.parseInt(config.getProperty("storage.snapshot.retain", "7")))
            .build();

        // Create volume
        StorageVolume volume = storageProvider.createVolume(device, volumeConfig);
        logger.info("Created storage volume: " + volume.getPath());

        // Store volume path for container configuration
        config.setProperty("_volume_path", volume.getPath());

        return true;
    }

    /**
     * Setup container deployment automation
     */
    private boolean setupContainerDeployment() throws Exception {
        logger.info("Setting up container deployment");

        // Create container configuration
        ContainerConfig containerConfig = new ContainerConfig.Builder()
            .containerName(config.getProperty("container.name"))
            .imagePath(config.getProperty("container.image.path"))
            .imageUrl(config.getProperty("container.image.url"))
            .ipAddress(config.getProperty("container.network.ip"))
            .gateway(config.getProperty("container.network.gateway"))
            .memoryLimit(parseMemoryLimit(config.getProperty("container.limits.memory")))
            .cpuShares(parseCpuShares(config.getProperty("container.limits.cpu")))
            .autoStart(Boolean.parseBoolean(config.getProperty("container.autostart", "true")))
            .build();

        // Add port mappings
        String portMappings = config.getProperty("container.ports");
        if (portMappings != null && !portMappings.isEmpty()) {
            for (String mapping : portMappings.split(",")) {
                String[] parts = mapping.split(":");
                if (parts.length == 2) {
                    int hostPort = Integer.parseInt(parts[0]);
                    int containerPort = Integer.parseInt(parts[1]);
                    containerConfig.addPortMapping(hostPort, containerPort);
                }
            }
        }

        // Add bind mounts (including storage volume)
        String volumePath = config.getProperty("_volume_path");
        if (volumePath != null) {
            containerConfig.addBindMount(volumePath, "/", false);
        }

        String bindMounts = config.getProperty("container.mounts");
        if (bindMounts != null && !bindMounts.isEmpty()) {
            for (String mount : bindMounts.split(",")) {
                String[] parts = mount.split(":");
                if (parts.length >= 2) {
                    String hostPath = parts[0];
                    String containerPath = parts[1];
                    boolean readOnly = parts.length > 2 && "ro".equals(parts[2]);
                    containerConfig.addBindMount(hostPath, containerPath, readOnly);
                }
            }
        }

        // Determine container technology
        ContainerTechnology tech = ContainerTechnology.fromCode(
            config.getProperty("container.technology", "lxc"));

        // Create deployment based on technology
        switch (tech) {
            case LXC:
                // Check if prerequisites preparation is needed
                boolean preparePrereqs = Boolean.parseBoolean(
                    config.getProperty("lxc.prepare_prerequisites", "false"));

                if (preparePrereqs) {
                    // Create with prerequisite preparer
                    LxcPrerequisitePreparer.PrerequisiteConfig prereqConfig =
                        new LxcPrerequisitePreparer.PrerequisiteConfig.Builder()
                            .requireLxd(true)
                            .requireSnapPath(true)
                            .requireIpForward(true)
                            .requireBridge(true)
                            .requireMasquerade(true)
                            .bridgeNetwork(config.getProperty("lxc.bridge.network", "10.0.3.0/24"))
                            .bridgeName(config.getProperty("lxc.bridge.name", "lxdbr0"))
                            .build();

                    containerDeployment = new LxcContainerDeployment(
                        containerConfig, prereqConfig, useSudo);
                } else {
                    // Create without preparer
                    containerDeployment = new LxcContainerDeployment(containerConfig, useSudo);
                }
                break;

            case DOCKER:
                // Future implementation
                throw new UnsupportedOperationException("Docker deployment not yet implemented");

            case PODMAN:
                // Future implementation
                throw new UnsupportedOperationException("Podman deployment not yet implemented");

            default:
                throw new IllegalArgumentException("Unsupported container technology: " + tech);
        }

        return true;
    }

    /**
     * Deploy the container
     */
    private boolean deployContainer() throws Exception {
        logger.info("Deploying container");
        return containerDeployment.execute(device);
    }

    /**
     * Configure storage mount for container
     */
    private boolean configureStorageMount() throws Exception {
        logger.info("Configuring storage mount");

        String containerName = config.getProperty("container.name");
        String volumePath = config.getProperty("_volume_path");

        if (volumePath == null) {
            logger.warning("No volume path found, skipping mount configuration");
            return true;
        }

        // Create mount automation
        StorageVolumeConfig volumeConfig = new StorageVolumeConfig.Builder()
            .locationId(config.getProperty("storage.location.id"))
            .containerRoot(config.getProperty("storage.container.root"))
            .quotaHumanSize(config.getProperty("storage.quota.size"))
            .build();

        LinuxDistribution distribution = SystemDetector.detectDistribution(device);
        LxcContainerBtrfsMountAutomation mountAutomation =
            new LxcContainerBtrfsMountAutomation(distribution, volumeConfig, containerName, useSudo);

        return mountAutomation.execute(device);
    }

    /**
     * Verify the deployment
     */
    private boolean verifyDeployment() throws Exception {
        logger.info("Verifying deployment");

        // Verify container
        if (!containerDeployment.verify(device)) {
            logger.severe("Container verification failed");
            return false;
        }

        // Verify storage
        String volumePath = config.getProperty("_volume_path");
        if (volumePath != null && !storageProvider.volumeExists(device, volumePath)) {
            logger.severe("Storage volume verification failed");
            return false;
        }

        // Get deployment status
        Map<String, String> status = containerDeployment.getStatus(device);
        logger.info("Deployment status:");
        for (Map.Entry<String, String> entry : status.entrySet()) {
            logger.info("  " + entry.getKey() + ": " + entry.getValue());
        }

        return true;
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
     * Parse memory limit from human-readable format
     */
    private Long parseMemoryLimit(String limit) {
        if (limit == null || limit.isEmpty()) {
            return null;
        }

        limit = limit.toUpperCase().trim();
        long multiplier = 1;

        if (limit.endsWith("G") || limit.endsWith("GB")) {
            multiplier = 1024L * 1024L * 1024L;
            limit = limit.replaceAll("[GB]", "");
        } else if (limit.endsWith("M") || limit.endsWith("MB")) {
            multiplier = 1024L * 1024L;
            limit = limit.replaceAll("[MB]", "");
        }

        return (long)(Double.parseDouble(limit) * multiplier);
    }

    /**
     * Parse CPU shares
     */
    private Integer parseCpuShares(String cpu) {
        if (cpu == null || cpu.isEmpty()) {
            return null;
        }
        return Integer.parseInt(cpu);
    }

    /**
     * Main method for standalone execution
     */
    public static void main(String[] args) {
        if (args.length != 1) {
            System.err.println("Usage: java BtrfsContainerDeploymentRunner <config-file>");
            System.exit(1);
        }

        try {
            BtrfsContainerDeploymentRunner runner = new BtrfsContainerDeploymentRunner(args[0]);
            boolean success = runner.execute();
            System.exit(success ? 0 : 1);
        } catch (Exception e) {
            System.err.println("Failed to run deployment: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}