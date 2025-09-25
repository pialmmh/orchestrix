package com.telcobright.orchestrix.images.lxc.uniqueidgenerator;

import com.telcobright.orchestrix.images.lxc.uniqueidgenerator.entities.UniqueIdConfig;
import com.telcobright.orchestrix.images.lxc.uniqueidgenerator.scripts.*;
import java.io.*;
import java.nio.file.*;
import java.util.*;
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
    private final NetworkSetup networkSetup;
    private final NodeJsInstaller nodeJsInstaller;
    private final ServiceSetup serviceSetup;
    private final SystemdConfig systemdConfig;

    public UniqueIdGeneratorBuilder(UniqueIdConfig config) {
        this.config = config;
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

        String script = String.format("""
            #!/bin/bash
            # Create LXC container

            CONTAINER="%s"
            BASE_IMAGE="%s"

            # Check if container already exists
            if lxc list | grep -q "$CONTAINER"; then
                echo "Container $CONTAINER already exists, removing..."
                lxc delete --force "$CONTAINER"
            fi

            # Create new container
            echo "Creating container $CONTAINER from $BASE_IMAGE..."
            lxc launch "$BASE_IMAGE" "$CONTAINER"

            # Wait for container to be ready
            sleep 5

            # Check container status
            if lxc list | grep "$CONTAINER" | grep -q "RUNNING"; then
                echo "✓ Container created and running"
            else
                echo "✗ Container failed to start"
                exit 1
            fi
            """,
            config.getBuildContainerName(),
            config.getBaseImage());

        executeScript("Create Container", script);
    }

    /**
     * Configure container network
     */
    private void configureNetwork() throws Exception {
        LOGGER.info("Configuring network...");

        // Validate and configure network on host
        executeScript("Validate Network", networkSetup.getValidateNetworkScript());
        executeScript("Configure Container Network", networkSetup.getConfigureNetworkScript());

        // Apply network fixes if needed
        executeScript("Fix Host Networking", networkSetup.getFixNetworkScript());

        // Configure network inside container
        executeInContainer("Configure Internal Network",
            networkSetup.getInternalNetworkScript());

        // Test connectivity
        executeInContainer("Test Internet Connectivity",
            networkSetup.getTestConnectivityScript());
    }

    /**
     * Install Node.js and dependencies
     */
    private void installNodeJs() throws Exception {
        LOGGER.info("Installing Node.js...");

        String[] scripts = nodeJsInstaller.getCriticalScripts();
        String[] scriptNames = {
            "Update System",
            "Install Prerequisites",
            "Install Node.js",
            "Verify Node.js"
        };

        for (int i = 0; i < scripts.length; i++) {
            executeInContainer(scriptNames[i], scripts[i]);
        }

        // Optional tools (don't fail build if these fail)
        try {
            executeInContainer("Install Additional Tools",
                nodeJsInstaller.getInstallToolsScript());
        } catch (Exception e) {
            LOGGER.warning("Additional tools installation failed (non-critical): " + e.getMessage());
        }
    }

    /**
     * Deploy service files to container
     */
    private void deployServiceFiles() throws Exception {
        LOGGER.info("Deploying service files...");

        // Copy server.js file
        String copyServerScript = String.format("""
            #!/bin/bash
            # Copy server.js to container

            CONTAINER="%s"
            SOURCE_FILE="%s"

            if [ ! -f "$SOURCE_FILE" ]; then
                echo "✗ Source file not found: $SOURCE_FILE"
                exit 1
            fi

            lxc file push "$SOURCE_FILE" "$CONTAINER/opt/unique-id-generator/server.js"

            if [ $? -eq 0 ]; then
                echo "✓ server.js deployed"
            else
                echo "✗ Failed to deploy server.js"
                exit 1
            fi
            """,
            config.getBuildContainerName(),
            config.getServerJsPath());

        executeScript("Deploy server.js", copyServerScript);

        // Copy package.json file
        String copyPackageScript = String.format("""
            #!/bin/bash
            # Copy package.json to container

            CONTAINER="%s"
            SOURCE_FILE="%s"

            if [ ! -f "$SOURCE_FILE" ]; then
                echo "✗ Source file not found: $SOURCE_FILE"
                exit 1
            fi

            lxc file push "$SOURCE_FILE" "$CONTAINER/opt/unique-id-generator/package.json"

            if [ $? -eq 0 ]; then
                echo "✓ package.json deployed"
            else
                echo "✗ Failed to deploy package.json"
                exit 1
            fi
            """,
            config.getBuildContainerName(),
            config.getPackageJsonPath());

        executeScript("Deploy package.json", copyPackageScript);
    }

    /**
     * Setup the service
     */
    private void setupService() throws Exception {
        LOGGER.info("Setting up service...");

        String[] scripts = serviceSetup.getAllScripts();
        String[] scriptNames = {
            "Create Directories",
            "Install npm Dependencies",
            "Set Permissions",
            "Create Environment File",
            "Verify Service Files",
            "Test Service Startup"
        };

        for (int i = 0; i < scripts.length; i++) {
            executeInContainer(scriptNames[i], scripts[i]);
        }
    }

    /**
     * Configure systemd
     */
    private void configureSystemd() throws Exception {
        LOGGER.info("Configuring systemd...");

        String[] scripts = systemdConfig.getAllScripts();
        String[] scriptNames = {
            "Create Service File",
            "Enable Service",
            "Configure Logrotate"
        };

        for (int i = 0; i < scripts.length; i++) {
            executeInContainer(scriptNames[i], scripts[i]);
        }

        // Note: We don't start the service during build
        LOGGER.info("Systemd configured (service will start at container launch)");
    }

    /**
     * Clean up container before creating image
     */
    private void cleanupContainer() throws Exception {
        LOGGER.info("Cleaning up container...");

        String cleanupScript = """
            #!/bin/bash
            # Clean up container

            echo "Cleaning package caches..."
            apt-get clean
            apt-get autoremove -y
            rm -rf /var/lib/apt/lists/*

            echo "Clearing temporary files..."
            rm -rf /tmp/*
            rm -rf /var/tmp/*

            echo "Clearing logs..."
            find /var/log -type f -exec truncate -s 0 {} \\;

            echo "✓ Cleanup complete"
            """;

        executeInContainer("Cleanup", cleanupScript);

        // Stop the container
        String stopScript = String.format("""
            #!/bin/bash
            # Stop container

            CONTAINER="%s"

            echo "Stopping container..."
            lxc stop "$CONTAINER"

            # Wait for container to stop
            sleep 3

            if lxc list | grep "$CONTAINER" | grep -q "STOPPED"; then
                echo "✓ Container stopped"
            else
                echo "✗ Container failed to stop"
                exit 1
            fi
            """,
            config.getBuildContainerName());

        executeScript("Stop Container", stopScript);
    }

    /**
     * Create the final image
     */
    private void createImage() throws Exception {
        LOGGER.info("Creating image...");

        String createImageScript = String.format("""
            #!/bin/bash
            # Create image from container

            CONTAINER="%s"
            IMAGE_NAME="%s"

            # Delete existing image if it exists
            if lxc image list | grep -q "$IMAGE_NAME"; then
                echo "Deleting existing image..."
                lxc image delete "$IMAGE_NAME"
            fi

            # Create new image
            echo "Creating image $IMAGE_NAME..."
            lxc publish "$CONTAINER" --alias "$IMAGE_NAME" \\
                description="Unique ID Generator with sharding support"

            if [ $? -eq 0 ]; then
                echo "✓ Image created: $IMAGE_NAME"

                # Show image info
                lxc image info "$IMAGE_NAME"
            else
                echo "✗ Failed to create image"
                exit 1
            fi

            # Delete build container
            echo "Deleting build container..."
            lxc delete "$CONTAINER"

            echo "✓ Build complete!"
            """,
            config.getBuildContainerName(),
            config.getImageName());

        executeScript("Create Image", createImageScript);
    }

    /**
     * Execute a script on the host
     */
    private void executeScript(String taskName, String script) throws Exception {
        executeWithRetry(taskName, script, false);
    }

    /**
     * Execute a script inside the container
     */
    private void executeInContainer(String taskName, String script) throws Exception {
        executeWithRetry(taskName, script, true);
    }

    /**
     * Execute a script with retry logic
     */
    private void executeWithRetry(String taskName, String script, boolean inContainer)
            throws Exception {

        for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
            try {
                LOGGER.info(String.format("Executing task: %s (attempt %d/%d)",
                    taskName, attempt, MAX_RETRIES));

                // Create temporary script file
                Path tempScript = Files.createTempFile("unique-id-gen-", ".sh");
                Files.write(tempScript, script.getBytes());

                String command;
                if (inContainer) {
                    // Push script to container and execute
                    String containerScript = "/tmp/" + tempScript.getFileName();

                    ProcessBuilder push = new ProcessBuilder(
                        "lxc", "file", "push",
                        tempScript.toString(),
                        config.getBuildContainerName() + containerScript
                    );
                    Process pushProcess = push.start();
                    if (!pushProcess.waitFor(30, TimeUnit.SECONDS)) {
                        throw new Exception("Script push timeout");
                    }

                    command = String.format("lxc exec %s -- bash %s",
                        config.getBuildContainerName(), containerScript);
                } else {
                    command = "bash " + tempScript;
                }

                // Execute the script
                ProcessBuilder pb = new ProcessBuilder("bash", "-c", command);
                pb.redirectErrorStream(true);
                Process process = pb.start();

                // Capture output
                BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()));
                String line;
                StringBuilder output = new StringBuilder();
                while ((line = reader.readLine()) != null) {
                    LOGGER.info("  " + line);
                    output.append(line).append("\n");
                }

                // Wait for completion
                if (!process.waitFor(300, TimeUnit.SECONDS)) {
                    process.destroyForcibly();
                    throw new Exception("Script execution timeout");
                }

                // Check exit code
                int exitCode = process.exitValue();
                if (exitCode != 0) {
                    throw new Exception("Script failed with exit code: " + exitCode);
                }

                // Success
                Files.delete(tempScript);
                LOGGER.info("✓ Task completed: " + taskName);
                return;

            } catch (Exception e) {
                LOGGER.warning(String.format("Task failed: %s - %s",
                    taskName, e.getMessage()));

                if (attempt < MAX_RETRIES) {
                    LOGGER.info("Retrying in " + RETRY_DELAY_MS + "ms...");
                    Thread.sleep(RETRY_DELAY_MS);
                } else {
                    throw new Exception("Task failed after " + MAX_RETRIES +
                        " attempts: " + taskName, e);
                }
            }
        }
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
            Runtime.getRuntime().addShutdownHook(new Thread() {
                @Override
                public void run() {
                    System.out.println("\n\nBuild interrupted - cleaning up...");

                    // Attempt to clean up the build container if it exists
                    try {
                        String containerName = finalConfig.getBuildContainerName();
                        if (containerName != null && !containerName.isEmpty()) {
                            System.out.println("Checking for build container: " + containerName);

                            Process checkProcess = new ProcessBuilder("lxc", "list", "--format=json")
                                .start();
                            checkProcess.waitFor();

                            Process deleteProcess = new ProcessBuilder("lxc", "delete", "--force", containerName)
                                .redirectErrorStream(true)
                                .start();

                            if (deleteProcess.waitFor(5, TimeUnit.SECONDS)) {
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