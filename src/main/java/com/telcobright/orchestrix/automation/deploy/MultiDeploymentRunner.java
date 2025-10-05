package com.telcobright.orchestrix.automation.deploy;

import com.telcobright.orchestrix.automation.core.executor.ExecutionMode;
import com.telcobright.orchestrix.automation.core.executor.MultiTargetExecutor;
import com.telcobright.orchestrix.automation.core.executor.Task;
import com.telcobright.orchestrix.automation.core.executor.TaskResult;
import com.telcobright.orchestrix.automation.deploy.entity.DeploymentConfig;
import com.telcobright.orchestrix.automation.deploy.entity.DeploymentTarget;
import com.telcobright.orchestrix.automation.deploy.entity.MultiDeploymentConfig;

import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.logging.Logger;

/**
 * Multi-Deployment Runner - Deploy to multiple servers in parallel or sequential mode
 * Entry point called from test-runner.sh via Maven
 */
public class MultiDeploymentRunner {
    private static final Logger logger = Logger.getLogger(MultiDeploymentRunner.class.getName());

    public static void main(String[] args) {
        if (args.length == 0) {
            System.err.println("Usage: MultiDeploymentRunner <test-runner.conf>");
            System.exit(1);
        }

        String configPath = args[0];

        try {
            logger.info("=================================================================");
            logger.info("  Multi-Target Deployment Runner");
            logger.info("=================================================================\n");

            // Load configuration
            MultiDeploymentConfig config = loadConfiguration(configPath);

            logger.info("Configuration loaded:");
            logger.info("  Artifact: " + config.getArtifactName());
            logger.info("  Targets: " + config.getDeploymentTargets().size());
            logger.info("  Execution Mode: " + config.getExecutionMode());
            logger.info("");

            // Create multi-target executor
            MultiTargetExecutor<Boolean> executor =
                    new MultiTargetExecutor<>(config.getExecutionMode());

            // Create tasks for each deployment target
            List<String> targetNames = new ArrayList<>();
            List<Task<Boolean>> tasks = new ArrayList<>();

            for (DeploymentTarget target : config.getDeploymentTargets()) {
                targetNames.add(target.getName());

                // Create deployment config for this specific target
                DeploymentConfig deployConfig = createDeploymentConfig(config, target);

                // Create task that deploys to this target
                tasks.add(() -> {
                    DeploymentManager manager = new DeploymentManager(deployConfig);
                    return manager.deploy();
                });
            }

            // Execute all deployment tasks
            List<TaskResult<Boolean>> results = executor.executeTasks(targetNames, tasks);

            // Print summary and determine success
            boolean allSuccess = executor.printSummary(results);

            if (allSuccess) {
                logger.info("✓ All deployments completed successfully");
                System.exit(0);
            } else {
                logger.severe("✗ Some deployments failed");
                System.exit(1);
            }

        } catch (Exception e) {
            logger.severe("Deployment error: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }

    /**
     * Create single-target DeploymentConfig from multi-target config and specific target
     */
    private static DeploymentConfig createDeploymentConfig(MultiDeploymentConfig multiConfig,
                                                            DeploymentTarget target) {
        DeploymentConfig config = new DeploymentConfig();

        // Server configuration
        config.setServerIp(target.getServerIp());
        config.setServerPort(target.getServerPort());
        config.setSshUser(target.getSshUser());
        config.setSshPassword(target.getSshPassword());
        config.setSshKeyPath(target.getSshKeyPath());

        // Artifact information
        config.setArtifactPath(multiConfig.getArtifactPath());
        config.setArtifactName(multiConfig.getArtifactName());
        config.setArtifactVersion(multiConfig.getArtifactVersion());

        // Container configuration
        config.setContainerName(target.getContainerName());
        config.setImageName(multiConfig.getImageName());
        config.setNetworkBridge(multiConfig.getNetworkBridge());
        config.setContainerIp(target.getContainerIp());

        // Port mapping - use target-specific or global
        config.setPortMapping(target.getPortMapping() != null ?
                target.getPortMapping() : multiConfig.getPortMapping());

        // Service configuration
        config.setServicePort(multiConfig.getServicePort());
        config.setStorageQuota(multiConfig.getStorageQuota());

        return config;
    }

    /**
     * Load multi-deployment configuration from file
     * Supports multiple targets with format:
     * TARGET_1_NAME=server-1
     * TARGET_1_SERVER_IP=192.168.1.100
     * TARGET_1_CONTAINER_NAME=go-id-shard-0
     * TARGET_1_CONTAINER_IP=10.200.100.50
     */
    private static MultiDeploymentConfig loadConfiguration(String configPath) throws Exception {
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

        MultiDeploymentConfig config = new MultiDeploymentConfig();

        // Artifact information
        config.setArtifactPath(props.getProperty("ARTIFACT_PATH"));
        config.setArtifactName(props.getProperty("ARTIFACT_NAME"));
        config.setArtifactVersion(props.getProperty("ARTIFACT_VERSION"));

        // Container configuration
        config.setImageName(props.getProperty("IMAGE_NAME"));
        config.setContainerNamePrefix(props.getProperty("CONTAINER_NAME_PREFIX", "container"));
        config.setNetworkBridge(props.getProperty("NETWORK_BRIDGE"));
        config.setPortMapping(props.getProperty("PORT_MAPPING"));
        config.setStorageQuota(props.getProperty("STORAGE_QUOTA"));

        String servicePort = props.getProperty("SERVICE_PORT");
        if (servicePort != null && !servicePort.isEmpty()) {
            config.setServicePort(Integer.parseInt(servicePort));
        }

        // Execution mode
        String mode = props.getProperty("EXECUTION_MODE", "SEQUENTIAL");
        config.setExecutionMode(ExecutionMode.valueOf(mode.toUpperCase()));

        // Parse deployment targets (supports multiple)
        // Format: TARGET_1_NAME, TARGET_1_SERVER_IP, etc.
        for (int i = 1; i <= 100; i++) {  // Support up to 100 targets
            String name = props.getProperty("TARGET_" + i + "_NAME");
            if (name == null || name.isEmpty()) {
                break;  // No more targets
            }

            DeploymentTarget target = new DeploymentTarget();
            target.setName(name);
            target.setServerIp(props.getProperty("TARGET_" + i + "_SERVER_IP"));

            String port = props.getProperty("TARGET_" + i + "_SERVER_PORT");
            if (port != null && !port.isEmpty()) {
                target.setServerPort(Integer.parseInt(port));
            }

            target.setSshUser(props.getProperty("TARGET_" + i + "_SSH_USER"));
            target.setSshPassword(props.getProperty("TARGET_" + i + "_SSH_PASSWORD"));
            target.setSshKeyPath(props.getProperty("TARGET_" + i + "_SSH_KEY_PATH"));
            target.setContainerName(props.getProperty("TARGET_" + i + "_CONTAINER_NAME"));
            target.setContainerIp(props.getProperty("TARGET_" + i + "_CONTAINER_IP"));
            target.setPortMapping(props.getProperty("TARGET_" + i + "_PORT_MAPPING"));

            config.addDeploymentTarget(target);
        }

        // Backward compatibility: single target without index
        if (config.getDeploymentTargets().isEmpty()) {
            String serverIp = props.getProperty("SERVER_IP");
            if (serverIp != null && !serverIp.isEmpty()) {
                DeploymentTarget target = new DeploymentTarget();
                target.setName("default");
                target.setServerIp(serverIp);

                String port = props.getProperty("SERVER_PORT", "22");
                target.setServerPort(Integer.parseInt(port));

                target.setSshUser(props.getProperty("SSH_USER"));
                target.setSshPassword(props.getProperty("SSH_PASSWORD"));
                target.setSshKeyPath(props.getProperty("SSH_KEY_PATH"));
                target.setContainerName(props.getProperty("CONTAINER_NAME"));
                target.setContainerIp(props.getProperty("CONTAINER_IP"));
                target.setPortMapping(props.getProperty("PORT_MAPPING"));

                config.addDeploymentTarget(target);
            }
        }

        // Validate required fields
        if (config.getArtifactPath() == null || config.getArtifactPath().isEmpty()) {
            throw new Exception("ARTIFACT_PATH is required in configuration");
        }
        if (config.getDeploymentTargets().isEmpty()) {
            throw new Exception("At least one deployment target must be configured");
        }

        // Validate each target
        for (DeploymentTarget target : config.getDeploymentTargets()) {
            if (target.getServerIp() == null || target.getServerIp().isEmpty()) {
                throw new Exception("SERVER_IP is required for target: " + target.getName());
            }
            if (target.getSshUser() == null || target.getSshUser().isEmpty()) {
                throw new Exception("SSH_USER is required for target: " + target.getName());
            }
            if (target.getContainerName() == null || target.getContainerName().isEmpty()) {
                throw new Exception("CONTAINER_NAME is required for target: " + target.getName());
            }
        }

        return config;
    }
}
