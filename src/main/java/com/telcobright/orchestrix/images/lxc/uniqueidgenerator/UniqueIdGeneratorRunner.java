package com.telcobright.orchestrix.images.lxc.uniqueidgenerator;

import com.telcobright.orchestrix.automation.api.model.AutomationConfig;
import com.telcobright.orchestrix.images.model.ContainerConfig;
import com.telcobright.orchestrix.model.NetworkConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Runner for building Unique ID Generator LXC containers
 */
public class UniqueIdGeneratorRunner {

    private static final Logger logger = LoggerFactory.getLogger(UniqueIdGeneratorRunner.class);

    public static void main(String[] args) {
        // Parse arguments
        int shardId = 1;
        int totalShards = 1;

        if (args.length >= 1) {
            shardId = Integer.parseInt(args[0]);
        }
        if (args.length >= 2) {
            totalShards = Integer.parseInt(args[1]);
        }

        logger.info("========================================");
        logger.info("Unique ID Generator Container Build");
        logger.info("Shard: {} of {}", shardId, totalShards);
        logger.info("========================================");

        boolean success = buildUniqueIdGenerator(shardId, totalShards);

        if (success) {
            logger.info("Build completed successfully!");
            System.exit(0);
        } else {
            logger.error("Build failed!");
            System.exit(1);
        }
    }

    /**
     * Build Unique ID Generator container
     */
    private static boolean buildUniqueIdGenerator(int shardId, int totalShards) {
        // Container configuration
        ContainerConfig containerConfig = new ContainerConfig();
        containerConfig.setContainerName("uid-generator-build-temp");
        containerConfig.setContainerType("lxc");
        containerConfig.setBaseImage("images:debian/12");
        containerConfig.setVersion("1.0.0");
        containerConfig.setOptimizeSize(true);
        containerConfig.setCleanupOnFailure(true);

        // Network configuration for the shard
        NetworkConfig networkConfig = new NetworkConfig();
        // Assign IP based on shard ID: 10.10.199.50 + shardId
        String shardIp = "10.10.199." + (50 + shardId) + "/24";
        networkConfig.setIpAddress(shardIp);
        networkConfig.setGateway("10.10.199.1/24");
        networkConfig.setBridge("lxdbr0");
        networkConfig.setDnsServers(new String[]{"8.8.8.8", "8.8.4.4"});
        containerConfig.setNetworkConfig(networkConfig);

        // Automation configuration
        AutomationConfig automationConfig = new AutomationConfig("local");
        automationConfig.setStreamOutput(true);
        automationConfig.setVerbose(true);
        automationConfig.setMaxRetries(3);
        automationConfig.setRetryDelayMs(5000);
        automationConfig.setCommandTimeoutMs(300000); // 5 minutes

        // Create and execute builder
        UniqueIdGeneratorBuilder builder = new UniqueIdGeneratorBuilder(
            containerConfig,
            automationConfig,
            shardId,
            totalShards
        );

        // Validate prerequisites
        if (!builder.validate()) {
            logger.error("Prerequisites validation failed");
            return false;
        }

        // Execute build
        try {
            return builder.execute();
        } catch (Exception e) {
            logger.error("Build failed with exception", e);
            return false;
        }
    }

    /**
     * Build multiple shards
     */
    public static boolean buildAllShards(int totalShards) {
        logger.info("Building {} shards of Unique ID Generator", totalShards);

        for (int i = 1; i <= totalShards; i++) {
            logger.info("Building shard {} of {}", i, totalShards);

            if (!buildUniqueIdGenerator(i, totalShards)) {
                logger.error("Failed to build shard {}", i);
                return false;
            }

            logger.info("Shard {} built successfully", i);
        }

        logger.info("All {} shards built successfully", totalShards);
        return true;
    }
}