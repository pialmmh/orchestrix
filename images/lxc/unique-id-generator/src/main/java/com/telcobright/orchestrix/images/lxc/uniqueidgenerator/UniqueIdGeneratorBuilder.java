package com.telcobright.orchestrix.images.lxc.uniqueidgenerator;

import com.telcobright.orchestrix.device.LxcDevice;
import com.telcobright.orchestrix.images.lxc.uniqueidgenerator.entities.UniqueIdConfig;
import com.telcobright.orchestrix.images.lxc.uniqueidgenerator.scripts.*;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.*;

/**
 * Main orchestrator for building Unique ID Generator LXC container
 * Executes all build tasks in sequence with retry capability
 */
public class UniqueIdGeneratorBuilder {

    private static final Logger LOGGER = Logger.getLogger(UniqueIdGeneratorBuilder.class.getName());
    private static final int MAX_RETRIES = 3;
    private static final long RETRY_DELAY_MS = 5000;

    private final UniqueIdConfig config;
    private final LxcDevice lxcDevice;
    private final NetworkSetup networkSetup;
    private final NodeJsInstaller nodeJsInstaller;
    private final ServiceSetup serviceSetup;
    private final SystemdConfig systemdConfig;

    public UniqueIdGeneratorBuilder(UniqueIdConfig config) {
        this.config = config;
        this.lxcDevice = new LxcDevice();
        this.networkSetup = new NetworkSetup(config);
        this.nodeJsInstaller = new NodeJsInstaller(config);
        this.serviceSetup = new ServiceSetup(config);
        this.systemdConfig = new SystemdConfig(config);
    }

    /**
     * Main build process
     */
    public void build() throws Exception {
        LOGGER.info("Starting Unique ID Generator container build...");

        // Phase 1: Container creation and network setup
        createContainer();
        configureNetwork();

        // Phase 2: Software installation
        installNodeJs();

        // Phase 3: Service deployment
        deployServiceFiles();
        setupService();

        // Phase 4: Systemd configuration
        configureSystemd();

        // Phase 5: Final cleanup and image creation
        cleanupContainer();
        createImage();

        LOGGER.info("Build completed successfully!");
    }

    /**
     * Create the LXC container
     */
    private void createContainer() throws Exception {
        LOGGER.info("Creating container: " + config.getBuildContainerName());

        String containerName = config.getBuildContainerName();

        // Check if container already exists
        if (lxcDevice.containerExists(containerName)) {
            LOGGER.info("Container already exists, removing...");
            lxcDevice.deleteContainer(containerName, true).get();
            LOGGER.info("✓ Old container removed");
        }

        // Launch new container
        LOGGER.info("Launching container from " + config.getBaseImage() + "...");
        boolean launched = lxcDevice.launchContainer(containerName, config.getBaseImage()).get();

        if (!launched) {
            throw new Exception("Failed to create container");
        }

        // Wait for container to be ready
        LOGGER.info("Waiting for container to initialize...");
        boolean ready = lxcDevice.waitForContainer(containerName, 30).get();

        if (!ready) {
            throw new Exception("Container failed to start");
        }

        LOGGER.info("✓ Container created and running successfully");
    }

    /**
     * Configure container network
     */
    private void configureNetwork() throws Exception {
        LOGGER.info("Configuring network...");

        String containerName = config.getBuildContainerName();
        String ipAddress = config.getIpAddress();
        String gateway = config.getGateway();

        // Configure network using LxcDevice
        boolean networkConfigured = lxcDevice.configureNetwork(containerName, ipAddress, gateway).get();

        if (!networkConfigured) {
            throw new Exception("Failed to configure network");
        }

        // Test connectivity
        String testResult = lxcDevice.execInContainer(containerName, "ping -c 1 8.8.8.8").get();

        if (testResult.contains("1 received")) {
            LOGGER.info("✓ Network configured and internet connectivity verified");
        } else {
            LOGGER.warning("Network configured but connectivity test failed");
        }
    }

    /**
     * Install Node.js and dependencies
     */
    private void installNodeJs() throws Exception {
        LOGGER.info("Installing Node.js...");

        String containerName = config.getBuildContainerName();

        // Update system
        LOGGER.info("Updating system packages...");
        lxcDevice.execInContainer(containerName, "apt-get update").get();

        // Install curl and prerequisites
        LOGGER.info("Installing prerequisites...");
        lxcDevice.execInContainer(containerName, "DEBIAN_FRONTEND=noninteractive apt-get install -y curl").get();

        // Install Node.js via NodeSource
        LOGGER.info("Adding NodeSource repository...");
        lxcDevice.execInContainer(containerName,
            "curl -fsSL https://deb.nodesource.com/setup_20.x | bash -").get();

        LOGGER.info("Installing Node.js...");
        lxcDevice.execInContainer(containerName,
            "DEBIAN_FRONTEND=noninteractive apt-get install -y nodejs").get();

        // Verify installation
        String nodeVersion = lxcDevice.execInContainer(containerName, "node --version").get();
        String npmVersion = lxcDevice.execInContainer(containerName, "npm --version").get();

        LOGGER.info("✓ Node.js installed: " + nodeVersion.trim());
        LOGGER.info("✓ npm installed: " + npmVersion.trim());

        // Optional tools (don't fail build if these fail)
        try {
            lxcDevice.installPackage(containerName, "git", "vim", "net-tools").get();
            LOGGER.info("Additional tools installed");
        } catch (Exception e) {
            LOGGER.warning("Additional tools installation failed (non-critical): " + e.getMessage());
        }
    }

    /**
     * Deploy service files to container
     */
    private void deployServiceFiles() throws Exception {
        LOGGER.info("Deploying service files...");

        String containerName = config.getBuildContainerName();

        // Create service directory
        lxcDevice.createDirectoryInContainer(containerName, "/opt/unique-id-generator").get();

        // Check if source files exist
        File serverJs = new File(config.getServerJsPath());
        File packageJson = new File(config.getPackageJsonPath());

        if (!serverJs.exists()) {
            throw new Exception("Source file not found: " + config.getServerJsPath());
        }
        if (!packageJson.exists()) {
            throw new Exception("Source file not found: " + config.getPackageJsonPath());
        }

        // Push server.js file
        boolean serverPushed = lxcDevice.pushFile(
            config.getServerJsPath(),
            containerName,
            "/opt/unique-id-generator/server.js"
        ).get();

        if (!serverPushed) {
            throw new Exception("Failed to deploy server.js");
        }
        LOGGER.info("✓ server.js deployed");

        // Push package.json file
        boolean packagePushed = lxcDevice.pushFile(
            config.getPackageJsonPath(),
            containerName,
            "/opt/unique-id-generator/package.json"
        ).get();

        if (!packagePushed) {
            throw new Exception("Failed to deploy package.json");
        }
        LOGGER.info("✓ package.json deployed");
    }

    /**
     * Setup the service
     */
    private void setupService() throws Exception {
        LOGGER.info("Setting up service...");

        String containerName = config.getBuildContainerName();

        // Create necessary directories
        LOGGER.info("Creating directories...");
        lxcDevice.createDirectoryInContainer(containerName, "/opt/unique-id-generator/data").get();
        lxcDevice.createDirectoryInContainer(containerName, "/var/log/unique-id-generator").get();

        // Install npm dependencies
        LOGGER.info("Installing npm dependencies...");
        String npmInstall = lxcDevice.execInContainer(containerName,
            "cd /opt/unique-id-generator && npm install --production").get();

        if (npmInstall.contains("error")) {
            throw new Exception("Failed to install npm dependencies");
        }
        LOGGER.info("✓ npm dependencies installed");

        // Set permissions
        LOGGER.info("Setting permissions...");
        lxcDevice.execInContainer(containerName,
            "chown -R nobody:nogroup /opt/unique-id-generator").get();
        lxcDevice.execInContainer(containerName,
            "chown -R nobody:nogroup /var/log/unique-id-generator").get();

        // Create environment file with defaults
        LOGGER.info("Creating environment file...");
        String envContent = """
            # Default configuration
            PORT=8080
            SHARD_ID=1
            TOTAL_SHARDS=1
            """;

        lxcDevice.writeFileInContainer(containerName,
            "/opt/unique-id-generator/.env",
            envContent).get();

        // Test service startup
        LOGGER.info("Testing service startup...");
        String testResult = lxcDevice.execInContainer(containerName,
            "cd /opt/unique-id-generator && timeout 2 node server.js 2>&1 || true").get();

        if (testResult.contains("Server running")) {
            LOGGER.info("✓ Service test successful");
        } else {
            LOGGER.warning("Service test output: " + testResult);
        }
    }

    /**
     * Configure systemd
     */
    private void configureSystemd() throws Exception {
        LOGGER.info("Configuring systemd...");

        String containerName = config.getBuildContainerName();

        // Create systemd service file
        String serviceContent = """
            [Unit]
            Description=Unique ID Generator Service
            After=network.target

            [Service]
            Type=simple
            User=nobody
            Group=nogroup
            WorkingDirectory=/opt/unique-id-generator
            Environment="NODE_ENV=production"
            EnvironmentFile=-/opt/unique-id-generator/.env
            ExecStart=/usr/bin/node /opt/unique-id-generator/server.js
            Restart=always
            RestartSec=10
            StandardOutput=append:/var/log/unique-id-generator/service.log
            StandardError=append:/var/log/unique-id-generator/error.log

            [Install]
            WantedBy=multi-user.target
            """;

        LOGGER.info("Creating systemd service file...");
        lxcDevice.writeFileInContainer(containerName,
            "/etc/systemd/system/unique-id-generator.service",
            serviceContent).get();

        // Enable the service
        LOGGER.info("Enabling service...");
        lxcDevice.enableService(containerName, "unique-id-generator").get();

        // Configure logrotate
        String logrotateContent = """
            /var/log/unique-id-generator/*.log {
                daily
                rotate 7
                compress
                delaycompress
                missingok
                notifempty
                create 644 nobody nogroup
                sharedscripts
                postrotate
                    systemctl reload unique-id-generator 2>/dev/null || true
                endscript
            }
            """;

        LOGGER.info("Configuring logrotate...");
        lxcDevice.writeFileInContainer(containerName,
            "/etc/logrotate.d/unique-id-generator",
            logrotateContent).get();

        LOGGER.info("✓ Systemd configured (service will start at container launch)");
    }

    /**
     * Clean up container before creating image
     */
    private void cleanupContainer() throws Exception {
        LOGGER.info("Cleaning up container...");

        String containerName = config.getBuildContainerName();

        // Clean package caches
        LOGGER.info("Cleaning package caches...");
        lxcDevice.execInContainer(containerName, "apt-get clean").get();
        lxcDevice.execInContainer(containerName, "apt-get autoremove -y").get();
        lxcDevice.execInContainer(containerName, "rm -rf /var/lib/apt/lists/*").get();

        // Clear temporary files
        LOGGER.info("Clearing temporary files...");
        lxcDevice.execInContainer(containerName, "rm -rf /tmp/*").get();
        lxcDevice.execInContainer(containerName, "rm -rf /var/tmp/*").get();

        // Clear logs
        LOGGER.info("Clearing logs...");
        lxcDevice.execInContainer(containerName, "find /var/log -type f -exec truncate -s 0 {} \\;").get();

        LOGGER.info("✓ Cleanup complete");

        // Stop the container
        LOGGER.info("Stopping container...");
        boolean stopped = lxcDevice.stopContainer(containerName).get();

        if (!stopped) {
            throw new Exception("Failed to stop container");
        }

        LOGGER.info("✓ Container stopped");
    }

    /**
     * Create the final image
     */
    private void createImage() throws Exception {
        LOGGER.info("Creating image...");

        String containerName = config.getBuildContainerName();
        String imageName = config.getImageName();

        // Create image from container
        boolean imageCreated = lxcDevice.createImage(
            containerName,
            imageName,
            "Unique ID Generator with sharding support"
        ).get();

        if (!imageCreated) {
            throw new Exception("Failed to create image");
        }

        LOGGER.info("✓ Image created: " + imageName);

        // Delete build container
        LOGGER.info("Deleting build container...");
        boolean deleted = lxcDevice.deleteContainer(containerName, false).get();

        if (!deleted) {
            LOGGER.warning("Failed to delete build container, may need manual cleanup");
        } else {
            LOGGER.info("✓ Build container deleted");
        }

        LOGGER.info("✓ Build complete!");
    }


    /**
     * Main entry point
     */
    public static void main(String[] args) {
        UniqueIdConfig config = null;

        try {
            // Load configuration
            if (args.length > 0) {
                // Load from specified config file
                config = UniqueIdConfig.loadFromFile(args[0]);
            } else {
                // Use default configuration
                config = new UniqueIdConfig();
            }

            // Validate configuration
            if (!config.validate()) {
                System.err.println("Invalid configuration");
                System.exit(1);
            }

            // Add shutdown hook for cleanup
            final UniqueIdConfig finalConfig = config;
            final LxcDevice cleanupDevice = new LxcDevice();
            Runtime.getRuntime().addShutdownHook(new Thread() {
                @Override
                public void run() {
                    System.out.println("\n\nBuild interrupted - cleaning up...");

                    // Attempt to clean up the build container if it exists
                    try {
                        String containerName = finalConfig.getBuildContainerName();
                        if (containerName != null && !containerName.isEmpty()) {
                            System.out.println("Checking for build container: " + containerName);

                            if (cleanupDevice.containerExists(containerName)) {
                                cleanupDevice.deleteContainer(containerName, true).get(5, TimeUnit.SECONDS);
                                System.out.println("Cleaned up build container: " + containerName);
                            }
                        }
                    } catch (Exception e) {
                        // Best effort cleanup - don't fail
                        System.err.println("Warning: Could not clean up container: " + e.getMessage());
                    }

                    System.out.println("Cleanup complete");
                }
            });

            // Run the builder
            UniqueIdGeneratorBuilder builder = new UniqueIdGeneratorBuilder(config);
            builder.build();

            System.out.println("Build completed successfully!");
            System.out.println("Image created: " + config.getImageName());

        } catch (Exception e) {
            System.err.println("Build failed: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}