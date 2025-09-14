package com.orchestrix.automation.runners.btclsbc;

import com.orchestrix.automation.networking.LxcNetworkCreator;
import com.orchestrix.network.entity.SshDevice;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

/**
 * Runner for BTCL SBC network automation
 * Creates LXC bridge network for FusionPBX deployment
 */
public class BtclRunner {

    private static final Logger logger = LoggerFactory.getLogger(BtclRunner.class);

    private Map<String, Object> config;
    private SshDevice sshDevice;

    public static void main(String[] args) {
        try {
            // Determine config file path
            String configPath = "src/main/resources/btcl-sbc-network.yml";
            if (args.length > 0) {
                configPath = args[0];
            }

            BtclRunner runner = new BtclRunner();
            runner.loadConfiguration(configPath);
            runner.execute();

        } catch (Exception e) {
            logger.error("Failed to execute BTCL network automation", e);
            System.exit(1);
        }
    }

    /**
     * Load configuration from YAML file
     */
    private void loadConfiguration(String configPath) throws Exception {
        logger.info("Loading configuration from: {}", configPath);

        File configFile = new File(configPath);
        if (!configFile.exists()) {
            // Try as resource
            try (InputStream input = getClass().getClassLoader().getResourceAsStream("btcl-sbc-network.yml")) {
                if (input != null) {
                    Yaml yaml = new Yaml();
                    config = yaml.load(input);
                } else {
                    throw new RuntimeException("Configuration file not found: " + configPath);
                }
            }
        } else {
            try (FileInputStream input = new FileInputStream(configFile)) {
                Yaml yaml = new Yaml();
                config = yaml.load(input);
            }
        }

        logger.info("Configuration loaded successfully");
    }

    /**
     * Execute the network automation
     */
    private void execute() throws Exception {
        // Get SSH configuration
        Map<String, Object> sshConfig = (Map<String, Object>) config.get("ssh");
        if (sshConfig == null) {
            throw new RuntimeException("SSH configuration not found in config file");
        }

        // Create SSH device
        createSshDevice(sshConfig);

        // Get network configuration
        Map<String, Object> networkConfig = (Map<String, Object>) config.get("network");
        if (networkConfig == null) {
            throw new RuntimeException("Network configuration not found in config file");
        }

        // Get automation settings
        Map<String, Object> automationConfig = (Map<String, Object>) config.get("automation");
        boolean dryRun = automationConfig != null && Boolean.TRUE.equals(automationConfig.get("dryRun"));
        boolean autoRollback = automationConfig != null && Boolean.TRUE.equals(automationConfig.get("autoRollback"));

        // Create network creator
        LxcNetworkCreator networkCreator = new LxcNetworkCreator(
            sshDevice,
            (String) networkConfig.get("lxcInterfaceName"),
            (String) networkConfig.get("hostInterfaceName"),
            (Integer) networkConfig.get("subnetMaskPrefixLen"),
            (String) networkConfig.get("hostInterfaceIp"),
            (String) networkConfig.get("lxcInternalIp")
        );

        if (dryRun) {
            logger.info("DRY RUN MODE ENABLED - No actual changes will be made");
            networkCreator.enableDryRun();
        }

        // Print execution plan
        printExecutionPlan(networkConfig);

        // Validate prerequisites
        logger.info("Validating prerequisites...");
        if (!networkCreator.validate()) {
            logger.error("Validation failed. Please check the requirements.");
            System.exit(1);
        }

        // Execute the automation
        logger.info("Starting network creation...");
        boolean success = networkCreator.execute();

        if (!success) {
            logger.error("Network creation failed");

            if (autoRollback && networkCreator.supportsRollback()) {
                logger.info("Attempting automatic rollback...");
                if (networkCreator.rollback()) {
                    logger.info("Rollback completed successfully");
                } else {
                    logger.error("Rollback failed");
                }
            }

            System.exit(1);
        }

        logger.info("===========================================");
        logger.info("Network creation completed successfully!");
        logger.info("===========================================");
        logger.info("LXC Bridge: {}", networkConfig.get("lxcInterfaceName"));
        logger.info("Gateway IP: {}/{}", networkConfig.get("lxcInternalIp"), networkConfig.get("subnetMaskPrefixLen"));
        logger.info("");
        logger.info("Next steps:");
        logger.info("1. Deploy FusionPBX containers using this network");
        logger.info("2. Configure static IPs from the 10.0.3.0/24 range");
        logger.info("3. No NAT traversal issues for VoIP!");
    }

    /**
     * Create SSH device from configuration
     */
    private void createSshDevice(Map<String, Object> sshConfig) throws Exception {
        String host = (String) sshConfig.get("host");
        Integer port = (Integer) sshConfig.get("port");
        String username = (String) sshConfig.get("username");
        Boolean useKey = (Boolean) sshConfig.get("useKey");

        if (host == null || username == null) {
            throw new RuntimeException("SSH host and username are required");
        }

        sshDevice = new SshDevice();
        sshDevice.setHost(host);
        sshDevice.setPort(port != null ? port : 22);
        sshDevice.setUsername(username);

        if (Boolean.TRUE.equals(useKey)) {
            String keyPath = (String) sshConfig.get("keyPath");
            if (keyPath != null) {
                // Expand tilde
                if (keyPath.startsWith("~")) {
                    keyPath = System.getProperty("user.home") + keyPath.substring(1);
                }

                Path keyFile = Paths.get(keyPath);
                if (Files.exists(keyFile)) {
                    String keyContent = new String(Files.readAllBytes(keyFile));
                    sshDevice.setSshKey(keyContent);
                    logger.info("Using SSH key authentication from: {}", keyPath);
                } else {
                    logger.warn("SSH key file not found: {}", keyPath);
                }
            }
        }

        String password = (String) sshConfig.get("password");
        if (password != null && !password.isEmpty()) {
            sshDevice.setPassword(password);
            logger.info("Using password authentication");
        }

        // Initialize SSH connection
        logger.info("Connecting to {}@{}:{}", username, host, port);
        sshDevice.initialize();
    }

    /**
     * Print execution plan
     */
    private void printExecutionPlan(Map<String, Object> networkConfig) {
        logger.info("===========================================");
        logger.info("EXECUTION PLAN");
        logger.info("===========================================");
        logger.info("Action: Create LXC Bridge Network");
        logger.info("Bridge Name: {}", networkConfig.get("lxcInterfaceName"));
        logger.info("Network: {}/{}", networkConfig.get("lxcInternalIp"), networkConfig.get("subnetMaskPrefixLen"));
        logger.info("NAT: Disabled (Bridge Mode)");
        logger.info("Purpose: VoIP/FusionPBX deployment");

        String hostInterface = (String) networkConfig.get("hostInterfaceName");
        if (hostInterface != null && !hostInterface.isEmpty()) {
            logger.info("Host Interface: {}", hostInterface);
        }

        logger.info("===========================================");
    }
}