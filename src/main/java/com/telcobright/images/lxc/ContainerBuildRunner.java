package com.telcobright.images.lxc;

import com.telcobright.images.lxc.sequenceservice.SequenceServiceInstaller;
import com.telcobright.images.lxc.sequenceservice.SequenceServiceInstaller.NetworkConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Main runner for building LXC containers
 * Demonstrates retryable sections and error handling
 */
public class ContainerBuildRunner {

    private static final Logger logger = LoggerFactory.getLogger(ContainerBuildRunner.class);

    public static void main(String[] args) {
        if (args.length < 1) {
            System.err.println("Usage: java ContainerBuildRunner <container-type>");
            System.err.println("Available types: sequence-service, postgres, dev-env");
            System.exit(1);
        }

        String containerType = args[0];
        boolean success = false;

        try {
            switch (containerType) {
                case "sequence-service":
                    success = buildSequenceService();
                    break;

                case "postgres":
                    // success = buildPostgres();
                    logger.error("PostgreSQL container not yet implemented");
                    break;

                case "dev-env":
                    // success = buildDevEnv();
                    logger.error("Dev environment container not yet implemented");
                    break;

                default:
                    logger.error("Unknown container type: {}", containerType);
                    System.exit(1);
            }

            if (success) {
                logger.info("Container build completed successfully!");
            } else {
                logger.error("Container build failed!");
                System.exit(1);
            }

        } catch (Exception e) {
            logger.error("Fatal error during build", e);
            System.exit(1);
        }
    }

    /**
     * Build Sequence Service container with retry logic
     */
    private static boolean buildSequenceService() {
        logger.info("========================================");
        logger.info("Building Sequence Service Container");
        logger.info("========================================");

        // Configuration
        NetworkConfig networkConfig = new NetworkConfig(
            "10.10.199.50/24",
            "10.10.199.1/24",
            "lxdbr0"
        );

        SequenceServiceInstaller installer = new SequenceServiceInstaller(
            "sequence-build-temp",
            "images:debian/12",
            networkConfig
        );

        // Enable output streaming for visibility
        installer.enableOutputStreaming();

        // Validate prerequisites
        if (!installer.validate()) {
            logger.error("Prerequisites validation failed");
            return false;
        }

        // Execute with section retry capability
        int maxRetries = 3;
        int attempt = 0;

        while (attempt < maxRetries) {
            attempt++;
            logger.info("Build attempt {} of {}", attempt, maxRetries);

            try {
                if (installer.execute()) {
                    return true;
                }
            } catch (Exception e) {
                logger.error("Build attempt {} failed: {}", attempt, e.getMessage());

                if (attempt < maxRetries) {
                    logger.info("Retrying in 10 seconds...");
                    try {
                        Thread.sleep(10000);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }

                    // For demonstration: could implement partial retry
                    // by tracking which section failed and retrying from there
                    logger.info("Attempting recovery from failure point...");

                    // Clean up failed container if exists
                    cleanupFailedContainer("sequence-build-temp");
                }
            }
        }

        logger.error("Build failed after {} attempts", maxRetries);
        return false;
    }

    /**
     * Clean up failed container
     */
    private static void cleanupFailedContainer(String containerName) {
        try {
            logger.info("Cleaning up failed container: {}", containerName);

            Runtime runtime = Runtime.getRuntime();
            Process process = runtime.exec("lxc delete --force " + containerName);
            int exitCode = process.waitFor();

            if (exitCode == 0) {
                logger.info("Cleanup successful");
            } else {
                logger.warn("Cleanup may have failed, exit code: {}", exitCode);
            }
        } catch (Exception e) {
            logger.error("Error during cleanup", e);
        }
    }

    /**
     * Example of section-specific retry
     * This could be called when a specific section fails
     */
    private static boolean retrySectionWithContext(String section, Runnable task) {
        logger.info("Retrying section: {}", section);

        int sectionRetries = 3;
        for (int i = 0; i < sectionRetries; i++) {
            try {
                task.run();
                logger.info("Section {} completed successfully", section);
                return true;
            } catch (Exception e) {
                logger.error("Section {} failed on attempt {}: {}",
                    section, i + 1, e.getMessage());

                if (i < sectionRetries - 1) {
                    try {
                        Thread.sleep(5000);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        }

        logger.error("Section {} failed after {} attempts", section, sectionRetries);
        return false;
    }
}