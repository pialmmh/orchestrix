package com.telcobright.orchestrix.automation.api.container.lxc.app.quarkus.example;

import com.telcobright.orchestrix.automation.core.PrerequisiteChecker;
import com.telcobright.orchestrix.device.LocalSshDevice;

import java.io.File;
import java.util.Properties;
import java.util.logging.Logger;

/**
 * Java automation for building Quarkus App Containers from base
 * Clones the base container and adds application JAR
 *
 * Usage: When you have an app JAR ready
 */
public class QuarkusAppContainerBuilder {

    private static final Logger logger = Logger.getLogger(QuarkusAppContainerBuilder.class.getName());

    private LocalSshDevice device;
    private Properties config;

    private String baseContainer;
    private String appName;
    private String appVersion;
    private String jarPath;
    private String containerName;

    public QuarkusAppContainerBuilder(String configFile) throws Exception {
        this.device = new LocalSshDevice();
        loadConfiguration(configFile);
    }

    private void loadConfiguration(String configFile) throws Exception {
        logger.info("Loading app configuration from: " + configFile);

        config = new Properties();
        File file = new File(configFile);

        if (!file.exists()) {
            throw new Exception("Configuration file not found: " + configFile);
        }

        // Parse config (same as base builder)
        String content = new String(java.nio.file.Files.readAllBytes(file.toPath()));
        for (String line : content.split("\n")) {
            if (line.trim().isEmpty() || line.trim().startsWith("#")) continue;
            if (line.contains("=")) {
                String[] parts = line.split("=", 2);
                config.setProperty(parts[0].trim(), parts[1].trim().replaceAll("^\"|\"$", ""));
            }
        }

        // Extract app config
        baseContainer = config.getProperty("BASE_CONTAINER", "quarkus-runner-base");
        appName = config.getProperty("APP_NAME");
        appVersion = config.getProperty("APP_VERSION");
        jarPath = config.getProperty("APP_JAR_PATH");
        containerName = appName + "-v" + appVersion;

        // Validate
        if (appName == null || appName.isEmpty()) {
            throw new Exception("APP_NAME is required");
        }
        if (appVersion == null || appVersion.isEmpty()) {
            throw new Exception("APP_VERSION is required");
        }
        if (jarPath == null || jarPath.isEmpty()) {
            throw new Exception("APP_JAR_PATH is required");
        }
        if (!new File(jarPath).exists()) {
            throw new Exception("JAR file not found: " + jarPath);
        }
    }

    public void build() throws Exception {
        logger.info("=========================================");
        logger.info("Building App Container from Base");
        logger.info("=========================================");
        logger.info("Base: " + baseContainer);
        logger.info("App: " + appName + " v" + appVersion);
        logger.info("JAR: " + jarPath);
        logger.info("Container: " + containerName);
        logger.info("=========================================");

        // Step 0: Check prerequisites
        logger.info("");
        PrerequisiteChecker checker = new PrerequisiteChecker(device, true);
        if (!checker.checkAll()) {
            throw new Exception("Prerequisite checks failed. Please fix the errors above and try again.");
        }
        logger.info("");

        // Clone base container
        cloneBaseContainer();

        // Deploy JAR
        deployJar();

        // Configure app
        configureApp();

        // Configure resources
        configureResources();

        // Create systemd service
        createAppService();

        // Configure logging
        configureLogging();

        // Activate Promtail for app
        activatePromtailForApp();

        // Test container
        testContainer();

        // Export
        exportContainer();

        logger.info("=========================================");
        logger.info("App Container Build Complete!");
        logger.info("=========================================");
        logger.info("Container: " + containerName);
        logger.info("Start: lxc start " + containerName);
        logger.info("=========================================");
    }

    private void cloneBaseContainer() throws Exception {
        logger.info("Cloning base container...");

        // Check if base exists as image
        String checkImage = device.executeCommand("lxc image list | grep " + baseContainer + " || echo 'not found'");
        if (checkImage.contains("not found")) {
            throw new Exception("Base container image not found: " + baseContainer + "\nBuild it first with: ./build/build.sh");
        }

        // Delete if exists
        device.executeCommand("lxc stop " + containerName + " --force 2>/dev/null || true");
        device.executeCommand("lxc delete " + containerName + " --force 2>/dev/null || true");

        // Clone from image
        device.executeCommand("lxc init " + baseContainer + " " + containerName);
    }

    private void deployJar() throws Exception {
        logger.info("Deploying JAR to container...");

        String destPath = "/opt/quarkus/" + appName + ".jar";
        device.executeCommand("lxc file push " + jarPath + " " + containerName + destPath);
        device.executeCommand("lxc exec " + containerName + " -- ln -sf " + destPath + " /opt/quarkus/app.jar");
    }

    private void configureApp() throws Exception {
        logger.info("Configuring application...");

        // Set environment variables
        String profile = config.getProperty("QUARKUS_PROFILE", "prod");
        device.executeCommand("lxc config set " + containerName + " environment.QUARKUS_PROFILE=" + profile);
        device.executeCommand("lxc config set " + containerName + " environment.APP_NAME=" + appName);
        device.executeCommand("lxc config set " + containerName + " environment.APP_VERSION=" + appVersion);
    }

    private void configureResources() throws Exception {
        logger.info("Configuring resources...");

        String memory = config.getProperty("MEMORY_LIMIT", "1GB");
        String cpu = config.getProperty("CPU_LIMIT", "1");

        device.executeCommand("lxc config set " + containerName + " limits.memory=" + memory);
        device.executeCommand("lxc config set " + containerName + " limits.cpu=" + cpu);

        // Auto-calculate JVM heap (75% of memory)
        String jvmHeap = calculateJvmHeap(memory);
        device.executeCommand("lxc config set " + containerName + " environment.JAVA_OPTS=-Xmx" + jvmHeap);
    }

    private String calculateJvmHeap(String memory) {
        long mb = parseMemoryToMB(memory);
        long heapMB = (long) (mb * 0.75);
        return heapMB + "m";
    }

    private long parseMemoryToMB(String memory) {
        memory = memory.toUpperCase();
        if (memory.endsWith("GB")) {
            return Long.parseLong(memory.replace("GB", "")) * 1024;
        } else if (memory.endsWith("MB")) {
            return Long.parseLong(memory.replace("MB", ""));
        }
        return 1024; // Default 1GB
    }

    private void createAppService() throws Exception {
        logger.info("Creating systemd service...");

        String serviceName = appName + ".service";
        String serviceContent = String.join("\n",
            "[Unit]",
            "Description=" + appName + " Quarkus Application",
            "After=network.target",
            "",
            "[Service]",
            "Type=simple",
            "User=quarkus",
            "WorkingDirectory=/opt/quarkus",
            "Environment=\"QUARKUS_PROFILE=prod\"",
            "ExecStart=/usr/bin/java $JAVA_OPTS -jar /opt/quarkus/app.jar",
            "Restart=on-failure",
            "RestartSec=10",
            "",
            "[Install]",
            "WantedBy=multi-user.target"
        );

        device.executeCommand("echo '" + serviceContent + "' | lxc exec " + containerName +
            " -- tee /etc/systemd/system/" + serviceName);
        device.executeCommand("lxc exec " + containerName + " -- systemctl enable " + serviceName);
    }

    private void configureLogging() throws Exception {
        logger.info("Configuring Promtail for app...");

        String labels = config.getProperty("LOG_LABELS", "app=" + appName);
        // Promtail config already in base, just update labels
    }

    private void activatePromtailForApp() throws Exception {
        logger.info("Activating Promtail for app container...");

        // Update Promtail config with app-specific values
        device.executeCommand("lxc exec " + containerName +
            " -- sed -i 's/\\${CONTAINER_NAME}/" + containerName + "/g' /etc/promtail/config.yml");
        device.executeCommand("lxc exec " + containerName +
            " -- sed -i 's/\\${APP_NAME}/" + appName + "/g' /etc/promtail/config.yml");

        String env = config.getProperty("DEPLOYMENT_ENV", "dev");
        device.executeCommand("lxc exec " + containerName +
            " -- sed -i 's/\\${ENV:-dev}/" + env + "/g' /etc/promtail/config.yml");

        // Update Log4j2 config with app name
        device.executeCommand("lxc exec " + containerName +
            " -- cp /etc/quarkus/log4j2.xml.template /etc/quarkus/log4j2.xml");

        // Set environment variables for logging
        device.executeCommand("lxc config set " + containerName + " environment.APP_NAME=" + appName);
        device.executeCommand("lxc config set " + containerName + " environment.CONTAINER_NAME=" + containerName);
        device.executeCommand("lxc config set " + containerName + " environment.ENV=" + env);

        logger.info("Promtail configuration updated for " + appName);
    }

    private void testContainer() throws Exception {
        logger.info("Testing container...");

        device.executeCommand("lxc start " + containerName);
        Thread.sleep(10000);

        // Check if running
        String status = device.executeCommand("lxc list " + containerName + " --format csv -c s");
        logger.info("Container status: " + status);

        // Check Promtail service
        try {
            String promtailStatus = device.executeCommand("lxc exec " + containerName +
                " -- systemctl is-active promtail.service 2>&1");
            logger.info("Promtail status: " + promtailStatus.trim());

            if (promtailStatus.contains("active")) {
                logger.info("✓ Promtail is running");
            } else {
                logger.warning("⚠ Promtail is not active: " + promtailStatus);
            }
        } catch (Exception e) {
            logger.warning("Could not check Promtail status: " + e.getMessage());
        }
    }

    private void exportContainer() throws Exception {
        String exportPath = "/tmp/" + containerName + "-" + System.currentTimeMillis() + ".tar.gz";
        device.executeCommand("lxc stop " + containerName);
        device.executeCommand("lxc export " + containerName + " " + exportPath);
        logger.info("Exported to: " + exportPath);
    }

    public static void main(String[] args) {
        try {
            String configFile = args.length > 0 ? args[0] :
                "configs/myapp-build.conf";

            QuarkusAppContainerBuilder builder = new QuarkusAppContainerBuilder(configFile);
            builder.build();
            System.exit(0);
        } catch (Exception e) {
            logger.severe("Build failed: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}
