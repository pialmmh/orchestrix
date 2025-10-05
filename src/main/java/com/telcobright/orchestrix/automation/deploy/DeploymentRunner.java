package com.telcobright.orchestrix.automation.deploy;

import com.telcobright.orchestrix.automation.deploy.entity.DeploymentConfig;

import java.io.File;
import java.nio.file.Files;
import java.util.Properties;
import java.util.logging.Logger;

/**
 * Deployment Runner - Entry point for container deployment automation
 * Called from test-runner.sh via Maven
 */
public class DeploymentRunner {
    private static final Logger logger = Logger.getLogger(DeploymentRunner.class.getName());

    public static void main(String[] args) {
        if (args.length == 0) {
            System.err.println("Usage: DeploymentRunner <test-runner.conf>");
            System.exit(1);
        }

        String configPath = args[0];

        try {
            logger.info("Loading deployment configuration from: " + configPath);

            // Load configuration
            DeploymentConfig config = loadConfiguration(configPath);

            // Create deployment manager
            DeploymentManager deploymentManager = new DeploymentManager(config);

            // Execute deployment
            boolean success = deploymentManager.deploy();

            if (success) {
                logger.info("Deployment completed successfully");
                System.exit(0);
            } else {
                logger.severe("Deployment failed");
                System.exit(1);
            }

        } catch (Exception e) {
            logger.severe("Deployment error: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }

    private static DeploymentConfig loadConfiguration(String configPath) throws Exception {
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

        // Build DeploymentConfig
        DeploymentConfig config = new DeploymentConfig();

        // Server configuration
        config.setServerIp(props.getProperty("SERVER_IP"));
        config.setServerPort(Integer.parseInt(props.getProperty("SERVER_PORT", "22")));
        config.setSshUser(props.getProperty("SSH_USER"));
        config.setSshPassword(props.getProperty("SSH_PASSWORD"));
        config.setSshKeyPath(props.getProperty("SSH_KEY_PATH"));

        // Artifact information
        config.setArtifactPath(props.getProperty("ARTIFACT_PATH"));
        config.setArtifactName(props.getProperty("ARTIFACT_NAME"));
        config.setArtifactVersion(props.getProperty("ARTIFACT_VERSION"));

        // Container configuration
        config.setContainerName(props.getProperty("CONTAINER_NAME"));
        config.setImageName(props.getProperty("IMAGE_NAME"));
        config.setNetworkBridge(props.getProperty("NETWORK_BRIDGE"));
        config.setContainerIp(props.getProperty("CONTAINER_IP"));
        config.setPortMapping(props.getProperty("PORT_MAPPING"));

        // Service configuration
        String servicePort = props.getProperty("SERVICE_PORT");
        if (servicePort != null && !servicePort.isEmpty()) {
            config.setServicePort(Integer.parseInt(servicePort));
        }
        config.setStorageQuota(props.getProperty("STORAGE_QUOTA"));

        // Validate required fields
        if (config.getServerIp() == null || config.getServerIp().isEmpty()) {
            throw new Exception("SERVER_IP is required in configuration");
        }
        if (config.getSshUser() == null || config.getSshUser().isEmpty()) {
            throw new Exception("SSH_USER is required in configuration");
        }
        if (config.getArtifactPath() == null || config.getArtifactPath().isEmpty()) {
            throw new Exception("ARTIFACT_PATH is required in configuration");
        }
        if (config.getContainerName() == null || config.getContainerName().isEmpty()) {
            throw new Exception("CONTAINER_NAME is required in configuration");
        }
        if (config.getImageName() == null || config.getImageName().isEmpty()) {
            throw new Exception("IMAGE_NAME is required in configuration");
        }

        return config;
    }
}
