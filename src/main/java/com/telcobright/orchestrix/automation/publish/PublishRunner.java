package com.telcobright.orchestrix.automation.publish;

import com.telcobright.orchestrix.automation.core.device.impl.LocalSshDevice;
import com.telcobright.orchestrix.automation.core.executor.ExecutionMode;
import com.telcobright.orchestrix.automation.core.executor.MultiTargetExecutor;
import com.telcobright.orchestrix.automation.core.executor.Task;
import com.telcobright.orchestrix.automation.core.executor.TaskResult;
import com.telcobright.orchestrix.automation.publish.entity.*;

import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.logging.Logger;

/**
 * Publish Runner - Entry point for artifact publishing automation
 * Supports publishing to multiple locations in parallel or sequential mode
 */
public class PublishRunner {
    private static final Logger logger = Logger.getLogger(PublishRunner.class.getName());

    public static void main(String[] args) {
        if (args.length == 0) {
            System.err.println("Usage: PublishRunner <publish-config.conf>");
            System.exit(1);
        }

        String configPath = args[0];

        try {
            logger.info("=================================================================");
            logger.info("  Artifact Publish Runner - Multi-Target Support");
            logger.info("=================================================================\n");

            // Load configuration
            PublishConfig config = loadConfiguration(configPath);

            logger.info("Configuration loaded:");
            logger.info("  Artifact: " + config.getArtifactName() + " " + config.getArtifactVersion());
            logger.info("  Locations: " + config.getPublishLocations().size());
            logger.info("  Execution Mode: " + config.getExecutionMode());
            logger.info("");

            // Create local SSH device for executing commands
            LocalSshDevice device = new LocalSshDevice("localhost", 22, System.getProperty("user.name"));
            device.connect();

            try {
                // Create publish manager
                PublishManager publishManager = new PublishManager(device);

                // Create artifact metadata
                Artifact artifact = publishManager.createArtifact(
                        config.getArtifactType(),
                        config.getArtifactName(),
                        config.getArtifactVersion(),
                        config.getArtifactFile(),
                        config.getArtifactPath(),
                        config.getBuildTimestamp()
                );

                // Generate MD5 hash file
                publishManager.generateHashFile(config.getArtifactPath());

                // Create multi-target executor
                MultiTargetExecutor<ArtifactPublish> executor =
                        new MultiTargetExecutor<>(config.getExecutionMode());

                // Create tasks for each publish location
                List<String> targetNames = new ArrayList<>();
                List<Task<ArtifactPublish>> tasks = new ArrayList<>();

                for (PublishLocation location : config.getPublishLocations()) {
                    targetNames.add(location.getName());

                    // Create task that publishes to this location
                    tasks.add(() -> publishManager.publishArtifact(artifact, location));
                }

                // Execute all publish tasks
                List<TaskResult<ArtifactPublish>> results = executor.executeTasks(targetNames, tasks);

                // Print summary and determine success
                boolean allSuccess = executor.printSummary(results);

                if (allSuccess) {
                    logger.info("✓ All publish operations completed successfully");
                    System.exit(0);
                } else {
                    logger.severe("✗ Some publish operations failed");
                    System.exit(1);
                }

            } finally {
                device.disconnect();
            }

        } catch (Exception e) {
            logger.severe("Publish error: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }

    /**
     * Load publish configuration from file
     * Supports multiple publish locations with format:
     * LOCATION_1_NAME=location-name
     * LOCATION_1_RCLONE_REMOTE=gdrive
     * LOCATION_1_RCLONE_TARGET_DIR=path/to/dir
     */
    private static PublishConfig loadConfiguration(String configPath) throws Exception {
        File configFile = new File(configPath);
        if (!configFile.exists()) {
            throw new Exception("Configuration file not found: " + configPath);
        }

        Properties props = new Properties();

        // Parse shell-style config file
        String content = new String(Files.readAllBytes(configFile.toPath()));
        for (String line : content.split("\n")) {
            line = line.trim();
            if (line.isEmpty() || line.startsWith("#")) continue;

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
                props.setProperty(key, value);
            }
        }

        PublishConfig config = new PublishConfig();

        // Artifact information
        config.setArtifactType(props.getProperty("ARTIFACT_TYPE", "lxc-container"));
        config.setArtifactName(props.getProperty("ARTIFACT_NAME"));
        config.setArtifactVersion(props.getProperty("ARTIFACT_VERSION"));
        config.setArtifactFile(props.getProperty("ARTIFACT_FILE"));
        config.setArtifactPath(props.getProperty("ARTIFACT_PATH"));

        String timestamp = props.getProperty("BUILD_TIMESTAMP");
        if (timestamp != null && !timestamp.isEmpty()) {
            config.setBuildTimestamp(Long.parseLong(timestamp));
        }

        // Execution mode
        String mode = props.getProperty("EXECUTION_MODE", "SEQUENTIAL");
        config.setExecutionMode(ExecutionMode.valueOf(mode.toUpperCase()));

        // Database connection (optional)
        config.setDbHost(props.getProperty("DB_HOST"));
        String dbPort = props.getProperty("DB_PORT");
        if (dbPort != null && !dbPort.isEmpty()) {
            config.setDbPort(Integer.parseInt(dbPort));
        }
        config.setDbName(props.getProperty("DB_NAME"));
        config.setDbUser(props.getProperty("DB_USER"));
        config.setDbPassword(props.getProperty("DB_PASSWORD"));

        // Parse publish locations (supports multiple)
        // Format: LOCATION_1_NAME, LOCATION_1_RCLONE_REMOTE, etc.
        for (int i = 1; i <= 100; i++) {  // Support up to 100 locations
            String name = props.getProperty("LOCATION_" + i + "_NAME");
            if (name == null || name.isEmpty()) {
                break;  // No more locations
            }

            PublishLocation location = new PublishLocation();
            location.setName(name);
            location.setType(props.getProperty("LOCATION_" + i + "_TYPE", "gdrive"));
            location.setRcloneRemote(props.getProperty("LOCATION_" + i + "_RCLONE_REMOTE"));
            location.setRcloneTargetDir(props.getProperty("LOCATION_" + i + "_RCLONE_TARGET_DIR"));

            config.addPublishLocation(location);
        }

        // Backward compatibility: single location without index
        if (config.getPublishLocations().isEmpty()) {
            String remoteName = props.getProperty("RCLONE_REMOTE");
            if (remoteName != null && !remoteName.isEmpty()) {
                PublishLocation location = new PublishLocation();
                location.setName("default");
                location.setType("gdrive");
                location.setRcloneRemote(remoteName);
                location.setRcloneTargetDir(props.getProperty("RCLONE_TARGET_DIR"));
                config.addPublishLocation(location);
            }
        }

        // Validate required fields
        if (config.getArtifactName() == null || config.getArtifactName().isEmpty()) {
            throw new Exception("ARTIFACT_NAME is required in configuration");
        }
        if (config.getArtifactPath() == null || config.getArtifactPath().isEmpty()) {
            throw new Exception("ARTIFACT_PATH is required in configuration");
        }
        if (config.getPublishLocations().isEmpty()) {
            throw new Exception("At least one publish location must be configured");
        }

        return config;
    }
}
