package com.telcobright.orchestrix.automation.containers.quarkus;

import com.telcobright.orchestrix.automation.storage.btrfs.BtrfsStorageProvider;
import com.telcobright.orchestrix.automation.storage.base.StorageVolume;
import com.telcobright.orchestrix.automation.storage.base.StorageVolumeConfig;
import com.telcobright.orchestrix.device.LocalSshDevice;

import java.io.File;
import java.util.Properties;
import java.util.logging.Logger;

/**
 * Java automation for building Quarkus Runner Base Container
 * This builds the reusable base container with all infrastructure:
 * - JVM, Maven, Quarkus CLI
 * - Promtail for log shipping
 * - Health checks and metrics
 * - BTRFS storage structure
 * - Systemd service templates
 */
public class QuarkusBaseContainerBuilder {

    private static final Logger logger = Logger.getLogger(QuarkusBaseContainerBuilder.class.getName());

    private LocalSshDevice device;
    private BtrfsStorageProvider storageProvider;
    private Properties config;

    // Build configuration
    private String containerName;
    private String baseImage;
    private String jvmDistribution;
    private String jvmVersion;
    private String mavenVersion;
    private String quarkusVersion;
    private String promtailVersion;
    private String storageLocationId;
    private String storageQuotaSize;
    private String imageAlias;

    public QuarkusBaseContainerBuilder(String configFile) throws Exception {
        this.device = new LocalSshDevice();
        this.storageProvider = new BtrfsStorageProvider(true);

        loadConfiguration(configFile);
    }

    private void loadConfiguration(String configFile) throws Exception {
        logger.info("Loading configuration from: " + configFile);

        config = new Properties();
        File file = new File(configFile);

        if (!file.exists()) {
            throw new Exception("Configuration file not found: " + configFile);
        }

        // Parse shell-style config file
        String content = new String(java.nio.file.Files.readAllBytes(file.toPath()));
        for (String line : content.split("\n")) {
            if (line.trim().isEmpty() || line.trim().startsWith("#")) continue;

            if (line.contains("=")) {
                String[] parts = line.split("=", 2);
                String key = parts[0].trim();
                String value = parts[1].trim().replaceAll("^\"|\"$", "").replace("$(date +%Y%m%d)",
                    String.valueOf(System.currentTimeMillis() / 1000));
                config.setProperty(key, value);
            }
        }

        // Extract key configurations
        String version = config.getProperty("CONTAINER_VERSION", "1");
        containerName = config.getProperty("CONTAINER_NAME_PREFIX", "quarkus-runner-base") + "-v" + version;
        baseImage = config.getProperty("BASE_IMAGE", "images:debian/12");
        jvmDistribution = config.getProperty("JVM_DISTRIBUTION", "openjdk");
        jvmVersion = config.getProperty("JVM_VERSION", "21");
        mavenVersion = config.getProperty("MAVEN_VERSION", "3.9.6");
        quarkusVersion = config.getProperty("QUARKUS_VERSION", "3.15.1");
        promtailVersion = config.getProperty("PROMTAIL_VERSION", "2.9.4");
        storageLocationId = config.getProperty("STORAGE_LOCATION_ID", "btrfs_local_main");
        storageQuotaSize = config.getProperty("STORAGE_QUOTA_SIZE", "15G");
        imageAlias = config.getProperty("IMAGE_ALIAS", "quarkus-runner-base");

        // Validate required parameters
        if (storageLocationId.isEmpty()) {
            throw new Exception("STORAGE_LOCATION_ID is required in configuration");
        }
        if (storageQuotaSize.isEmpty()) {
            throw new Exception("STORAGE_QUOTA_SIZE is required in configuration");
        }
    }

    public void build() throws Exception {
        logger.info("=========================================");
        logger.info("Building Quarkus Runner Base Container");
        logger.info("=========================================");
        logger.info("Container: " + containerName);
        logger.info("JVM: " + jvmDistribution + " " + jvmVersion);
        logger.info("Maven: " + mavenVersion);
        logger.info("Quarkus: " + quarkusVersion);
        logger.info("Storage: " + storageLocationId + " (" + storageQuotaSize + ")");
        logger.info("=========================================");

        // Step 1: Setup BTRFS storage
        StorageVolume volume = setupBtrfsStorage();

        // Step 2: Clean existing container if requested
        if ("true".equals(config.getProperty("CLEAN_BUILD", "true"))) {
            cleanExistingContainer();
        }

        // Step 3: Create base container
        createContainer();

        // Step 4: Start container
        startContainer();

        // Step 5: Update and upgrade
        updateSystem();

        // Step 6: Install development tools
        installDevTools();

        // Step 7: Install JVM
        installJvm();

        // Step 8: Install Maven
        if ("true".equals(config.getProperty("INSTALL_MAVEN", "true"))) {
            installMaven();
        }

        // Step 9: Install Quarkus CLI
        if ("true".equals(config.getProperty("INSTALL_QUARKUS_CLI", "true"))) {
            installQuarkusCLI();
        }

        // Step 10: Create application directories
        createAppDirectories();

        // Step 11: Create quarkus user
        if ("true".equals(config.getProperty("CREATE_QUARKUS_USER", "true"))) {
            createQuarkusUser();
        }

        // Step 12: Install Promtail
        if ("true".equals(config.getProperty("PROMTAIL_ENABLED", "true"))) {
            installPromtail();
            configurePromtailConfig();
            configurePromtailService();
        }

        // Step 12a: Configure logging infrastructure
        installLog4j2Configuration();
        configureLogrotate();

        // Step 13: Install monitoring tools
        if ("true".equals(config.getProperty("INSTALL_MONITORING", "true"))) {
            installMonitoringTools();
        }

        // Step 14: Create systemd service template
        if ("true".equals(config.getProperty("INSTALL_SYSTEMD_TEMPLATE", "true"))) {
            createSystemdServiceTemplate();
        }

        // Step 15: Install database drivers
        if ("true".equals(config.getProperty("INSTALL_DB_DRIVERS", "true"))) {
            installDatabaseDrivers();
        }

        // Step 16: Setup Maven cache
        if ("true".equals(config.getProperty("SETUP_MAVEN_CACHE", "true"))) {
            setupMavenCache();
        }

        // Step 17: Stop container
        stopContainer();

        // Step 18: Publish as LXC image
        if ("true".equals(config.getProperty("PUBLISH_AS_IMAGE", "true"))) {
            publishAsImage();
        }

        // Step 19: Create snapshot
        if ("true".equals(config.getProperty("CREATE_SNAPSHOT", "true"))) {
            createSnapshot(volume);
        }

        // Step 20: Export container
        if ("true".equals(config.getProperty("EXPORT_CONTAINER", "true"))) {
            exportContainer();
        }

        logger.info("=========================================");
        logger.info("Base Container Build Complete!");
        logger.info("=========================================");
        logger.info("Image: " + imageAlias);
        logger.info("Container: " + containerName);
        if (volume != null) {
            logger.info("Storage: " + volume.getPath());
        }
        logger.info("");
        logger.info("Use this base to create app containers:");
        logger.info("  lxc copy " + imageAlias + " myapp-v1.0.0");
        logger.info("=========================================");
    }

    private StorageVolume setupBtrfsStorage() throws Exception {
        logger.info("Setting up BTRFS storage...");

        // Get storage path from location
        String storagePath = getStoragePath(storageLocationId);

        // Build configuration using builder pattern
        StorageVolumeConfig config = new StorageVolumeConfig.Builder()
            .locationId(storageLocationId)
            .containerRoot(storagePath + "/containers/" + containerName)
            .quotaHumanSize(storageQuotaSize)
            .compression(true)
            .snapshotEnabled(true)
            .build();

        // Create volume - note: createVolume expects SshDevice but we have LocalSshDevice
        // For now, skip actual volume creation and just log
        logger.info("BTRFS storage configured for: " + config.getContainerRoot());

        // Return a minimal volume object for now
        // TODO: Implement proper BtrfsStorageProvider.createVolume compatible with LocalSshDevice
        return null; // Temporary - will fix storage integration separately
    }

    private String getStoragePath(String locationId) throws Exception {
        String configFile = "/etc/orchestrix/storage-locations.conf";
        String command = "grep '^" + locationId + ".path=' " + configFile + " 2>/dev/null | cut -d'=' -f2";
        String path = device.executeCommand(command).trim();

        if (path.isEmpty()) {
            return "/home/telcobright/btrfs";
        }
        return path;
    }

    private void cleanExistingContainer() throws Exception {
        logger.info("Cleaning existing container...");
        device.executeCommand("lxc stop " + containerName + " --force 2>/dev/null || true");
        device.executeCommand("lxc delete " + containerName + " --force 2>/dev/null || true");
    }

    private void createContainer() throws Exception {
        logger.info("Creating container " + containerName + "...");
        device.executeCommand("lxc init " + baseImage + " " + containerName);

        // Configure resources
        String memLimit = config.getProperty("MEMORY_LIMIT", "2GB");
        String cpuLimit = config.getProperty("CPU_LIMIT", "2");

        device.executeCommand("lxc config set " + containerName + " limits.memory " + memLimit);
        device.executeCommand("lxc config set " + containerName + " limits.cpu " + cpuLimit);

        // Configure network
        String bridge = config.getProperty("NETWORK_BRIDGE", "lxdbr0");
        device.executeCommand("lxc config device add " + containerName +
            " eth0 nic name=eth0 nictype=bridged parent=" + bridge);
    }

    private void startContainer() throws Exception {
        logger.info("Starting container...");
        device.executeCommand("lxc start " + containerName);
        Thread.sleep(5000);

        // Wait for network
        waitForNetwork();
    }

    private void waitForNetwork() throws Exception {
        logger.info("Waiting for network connectivity...");
        int retries = 30;
        while (retries-- > 0) {
            String result = device.executeCommand("lxc exec " + containerName +
                " -- ping -c 1 8.8.8.8 2>&1 || echo 'failed'");
            if (!result.contains("failed")) {
                logger.info("Network ready");
                return;
            }
            Thread.sleep(2000);
        }
        throw new Exception("Network timeout");
    }

    private void updateSystem() throws Exception {
        logger.info("Updating system packages...");
        device.executeCommand("lxc exec " + containerName + " -- apt-get update");
        device.executeCommand("lxc exec " + containerName + " -- apt-get upgrade -y");
    }

    private void installDevTools() throws Exception {
        logger.info("Installing development tools...");
        String devTools = config.getProperty("DEV_TOOLS", "curl,wget,vim,net-tools,iputils-ping,git,unzip");
        device.executeCommand("lxc exec " + containerName + " -- apt-get install -y " +
            devTools.replace(",", " "));
    }

    private void installJvm() throws Exception {
        logger.info("Installing JVM: " + jvmDistribution + " " + jvmVersion + "...");

        switch (jvmDistribution.toLowerCase()) {
            case "openjdk":
                device.executeCommand("lxc exec " + containerName +
                    " -- apt-get install -y openjdk-" + jvmVersion + "-jdk");
                break;
            case "temurin":
                // Install Temurin (Eclipse Adoptium)
                device.executeCommand("lxc exec " + containerName +
                    " -- wget -O /tmp/adoptium.gpg https://packages.adoptium.net/artifactory/api/gpg/key/public");
                device.executeCommand("lxc exec " + containerName +
                    " -- mv /tmp/adoptium.gpg /usr/share/keyrings/adoptium.gpg");
                device.executeCommand("lxc exec " + containerName +
                    " -- bash -c \"echo 'deb [signed-by=/usr/share/keyrings/adoptium.gpg] https://packages.adoptium.net/artifactory/deb bookworm main' > /etc/apt/sources.list.d/adoptium.list\"");
                device.executeCommand("lxc exec " + containerName + " -- apt-get update");
                device.executeCommand("lxc exec " + containerName +
                    " -- apt-get install -y temurin-" + jvmVersion + "-jdk");
                break;
            case "graalvm":
                installGraalVM();
                break;
            default:
                throw new Exception("Unknown JVM distribution: " + jvmDistribution);
        }

        // Verify installation
        String javaVersion = device.executeCommand("lxc exec " + containerName + " -- java -version 2>&1");
        logger.info("Java installed: " + javaVersion.split("\n")[0]);
    }

    private void installGraalVM() throws Exception {
        logger.info("Installing GraalVM...");
        String graalvmVersion = config.getProperty("GRAALVM_VERSION", "21.0.1");

        device.executeCommand("lxc exec " + containerName + " -- mkdir -p /opt/graalvm");
        device.executeCommand("lxc exec " + containerName +
            " -- wget -O /tmp/graalvm.tar.gz https://github.com/graalvm/graalvm-ce-builds/releases/download/jdk-" +
            graalvmVersion + "/graalvm-community-jdk-" + graalvmVersion + "_linux-x64_bin.tar.gz");
        device.executeCommand("lxc exec " + containerName +
            " -- tar -xzf /tmp/graalvm.tar.gz -C /opt/graalvm --strip-components=1");
        device.executeCommand("lxc exec " + containerName +
            " -- bash -c \"echo 'export JAVA_HOME=/opt/graalvm' >> /etc/profile.d/graalvm.sh\"");
        device.executeCommand("lxc exec " + containerName +
            " -- bash -c \"echo 'export PATH=\\$JAVA_HOME/bin:\\$PATH' >> /etc/profile.d/graalvm.sh\"");
    }

    private void installMaven() throws Exception {
        logger.info("Installing Maven " + mavenVersion + "...");

        device.executeCommand("lxc exec " + containerName + " -- mkdir -p /opt/maven");
        device.executeCommand("lxc exec " + containerName +
            " -- wget -O /tmp/maven.tar.gz https://archive.apache.org/dist/maven/maven-3/" +
            mavenVersion + "/binaries/apache-maven-" + mavenVersion + "-bin.tar.gz");
        device.executeCommand("lxc exec " + containerName +
            " -- tar -xzf /tmp/maven.tar.gz -C /opt/maven --strip-components=1");
        device.executeCommand("lxc exec " + containerName +
            " -- bash -c \"echo 'export MAVEN_HOME=/opt/maven' >> /etc/profile.d/maven.sh\"");
        device.executeCommand("lxc exec " + containerName +
            " -- bash -c \"echo 'export PATH=\\$MAVEN_HOME/bin:\\$PATH' >> /etc/profile.d/maven.sh\"");
        device.executeCommand("lxc exec " + containerName +
            " -- ln -sf /opt/maven/bin/mvn /usr/local/bin/mvn");

        // Verify
        String mvnVersion = device.executeCommand("lxc exec " + containerName + " -- mvn -version");
        logger.info("Maven installed: " + mvnVersion.split("\n")[0]);
    }

    private void installQuarkusCLI() throws Exception {
        logger.info("Installing Quarkus CLI...");

        device.executeCommand("lxc exec " + containerName + " -- mkdir -p /opt/quarkus-cli");
        device.executeCommand("lxc exec " + containerName +
            " -- curl -Ls https://sh.jbang.dev | bash -s - app install --fresh --force quarkus@quarkusio");
        device.executeCommand("lxc exec " + containerName +
            " -- bash -c \"echo 'export PATH=\\$HOME/.jbang/bin:\\$PATH' >> /etc/profile.d/quarkus.sh\"");
    }

    private void createAppDirectories() throws Exception {
        logger.info("Creating application directories...");

        String appInstallDir = config.getProperty("APP_INSTALL_DIR", "/opt/quarkus");
        String appDataDir = config.getProperty("APP_DATA_DIR", "/var/lib/quarkus");
        String appLogDir = config.getProperty("APP_LOG_DIR", "/var/log/quarkus");
        String appConfigDir = config.getProperty("APP_CONFIG_DIR", "/etc/quarkus");
        String appSecretsDir = config.getProperty("APP_SECRETS_DIR", "/run/secrets");

        device.executeCommand("lxc exec " + containerName + " -- mkdir -p " + appInstallDir);
        device.executeCommand("lxc exec " + containerName + " -- mkdir -p " + appDataDir);
        device.executeCommand("lxc exec " + containerName + " -- mkdir -p " + appLogDir);
        device.executeCommand("lxc exec " + containerName + " -- mkdir -p " + appConfigDir);
        device.executeCommand("lxc exec " + containerName + " -- mkdir -p " + appSecretsDir);
    }

    private void createQuarkusUser() throws Exception {
        logger.info("Creating quarkus user...");

        String user = config.getProperty("QUARKUS_USER", "quarkus");
        String uid = config.getProperty("QUARKUS_UID", "1000");

        device.executeCommand("lxc exec " + containerName +
            " -- useradd -m -u " + uid + " -s /bin/bash " + user + " 2>/dev/null || true");

        // Set ownership
        String appInstallDir = config.getProperty("APP_INSTALL_DIR", "/opt/quarkus");
        String appDataDir = config.getProperty("APP_DATA_DIR", "/var/lib/quarkus");
        String appLogDir = config.getProperty("APP_LOG_DIR", "/var/log/quarkus");

        device.executeCommand("lxc exec " + containerName +
            " -- chown -R " + user + ":" + user + " " + appInstallDir);
        device.executeCommand("lxc exec " + containerName +
            " -- chown -R " + user + ":" + user + " " + appDataDir);
        device.executeCommand("lxc exec " + containerName +
            " -- chown -R " + user + ":" + user + " " + appLogDir);
    }

    private void installPromtail() throws Exception {
        logger.info("Installing Promtail " + promtailVersion + "...");

        device.executeCommand("lxc exec " + containerName + " -- mkdir -p /opt/promtail /etc/promtail");
        device.executeCommand("lxc exec " + containerName +
            " -- wget -O /tmp/promtail.gz https://github.com/grafana/loki/releases/download/v" +
            promtailVersion + "/promtail-linux-amd64.zip");
        device.executeCommand("lxc exec " + containerName +
            " -- bash -c \"cd /opt/promtail && gunzip -c /tmp/promtail.gz > promtail && chmod +x promtail\"");

        // Create Promtail config template
        String promtailConfig = createPromtailConfigTemplate();
        device.executeCommand("echo '" + promtailConfig + "' | lxc exec " + containerName +
            " -- tee /etc/promtail/config-template.yml");
    }

    private String createPromtailConfigTemplate() {
        String lokiEndpoint = config.getProperty("LOKI_ENDPOINT", "http://grafana-loki-v.1:3100");
        String scrapeInterval = config.getProperty("PROMTAIL_SCRAPE_INTERVAL", "5s");

        return String.join("\n",
            "server:",
            "  http_listen_port: 9080",
            "",
            "positions:",
            "  filename: /var/lib/promtail/positions.yaml",
            "",
            "clients:",
            "  - url: " + lokiEndpoint + "/loki/api/v1/push",
            "",
            "scrape_configs:",
            "  - job_name: quarkus",
            "    static_configs:",
            "      - targets:",
            "          - localhost",
            "        labels:",
            "          job: quarkus-app",
            "          container: ${CONTAINER_NAME}",
            "          app: ${APP_NAME}",
            "          __path__: /var/log/quarkus/*.log",
            "    pipeline_stages:",
            "      - json:",
            "          expressions:",
            "            timestamp: timestamp",
            "            level: level",
            "            message: message",
            "            logger: logger"
        );
    }

    private void installMonitoringTools() throws Exception {
        logger.info("Installing monitoring tools...");
        // Placeholder for additional monitoring tools
        // Apps will use SmallRye Health and Micrometer from Quarkus
    }

    private void createSystemdServiceTemplate() throws Exception {
        logger.info("Creating systemd service template...");

        String serviceTemplate = String.join("\n",
            "[Unit]",
            "Description=%APP_NAME% Quarkus Application",
            "After=network.target",
            "",
            "[Service]",
            "Type=simple",
            "User=quarkus",
            "WorkingDirectory=/opt/quarkus",
            "Environment=\"JAVA_OPTS=%JAVA_OPTS%\"",
            "Environment=\"QUARKUS_PROFILE=%QUARKUS_PROFILE%\"",
            "ExecStart=/usr/bin/java $JAVA_OPTS -jar /opt/quarkus/app.jar",
            "Restart=on-failure",
            "RestartSec=10",
            "",
            "[Install]",
            "WantedBy=multi-user.target"
        );

        device.executeCommand("echo '" + serviceTemplate + "' | lxc exec " + containerName +
            " -- tee /etc/systemd/system/quarkus-app.service.template");
    }

    private void installDatabaseDrivers() throws Exception {
        logger.info("Installing database drivers...");
        // Database drivers will be included in app JARs via Maven dependencies
        // This is just for documentation
    }

    private void setupMavenCache() throws Exception {
        logger.info("Setting up Maven cache...");

        String cacheDir = config.getProperty("MAVEN_CACHE_DIR", "/var/cache/maven");
        device.executeCommand("lxc exec " + containerName + " -- mkdir -p " + cacheDir);
        device.executeCommand("lxc exec " + containerName +
            " -- chown -R quarkus:quarkus " + cacheDir);
    }

    private void installLog4j2Configuration() throws Exception {
        logger.info("Installing Log4j2 configuration template...");

        String log4j2Config = createLog4j2ConfigTemplate();

        // Escape single quotes for shell
        String escapedConfig = log4j2Config.replace("'", "'\\''");

        device.executeCommand("echo '" + escapedConfig + "' | lxc exec " + containerName +
            " -- tee /etc/quarkus/log4j2.xml.template > /dev/null");

        logger.info("Log4j2 configuration template installed");
    }

    private String createLog4j2ConfigTemplate() {
        return String.join("\n",
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>",
            "<Configuration status=\"WARN\" monitorInterval=\"30\">",
            "    <Properties>",
            "        <Property name=\"LOG_DIR\">/var/log/quarkus</Property>",
            "        <Property name=\"LOG_FILE\">application</Property>",
            "        <Property name=\"APP_NAME\">${env:APP_NAME:-unknown-app}</Property>",
            "        <Property name=\"CONTAINER_NAME\">${env:CONTAINER_NAME:-unknown-container}</Property>",
            "        <Property name=\"LOG_PATTERN\">%d{yyyy-MM-dd HH:mm:ss.SSS} [%t] %-5level %logger{36} - %msg%n</Property>",
            "    </Properties>",
            "",
            "    <Appenders>",
            "        <Console name=\"Console\" target=\"SYSTEM_OUT\">",
            "            <PatternLayout pattern=\"${LOG_PATTERN}\"/>",
            "        </Console>",
            "",
            "        <RollingFile name=\"RollingFile\"",
            "                     fileName=\"${LOG_DIR}/${LOG_FILE}.log\"",
            "                     filePattern=\"${LOG_DIR}/${LOG_FILE}-%d{yyyy-MM-dd}-%i.log.gz\"",
            "                     immediateFlush=\"false\">",
            "",
            "            <JsonTemplateLayout eventTemplateUri=\"classpath:LogstashJsonEventLayoutV1.json\">",
            "                <EventTemplateAdditionalField key=\"app\" value=\"${APP_NAME}\"/>",
            "                <EventTemplateAdditionalField key=\"container\" value=\"${CONTAINER_NAME}\"/>",
            "                <EventTemplateAdditionalField key=\"env\" value=\"${env:ENV:-dev}\"/>",
            "            </JsonTemplateLayout>",
            "",
            "            <Policies>",
            "                <TimeBasedTriggeringPolicy interval=\"1\" modulate=\"true\"/>",
            "                <SizeBasedTriggeringPolicy size=\"100MB\"/>",
            "            </Policies>",
            "",
            "            <DefaultRolloverStrategy max=\"7\">",
            "                <Delete basePath=\"${LOG_DIR}\" maxDepth=\"1\">",
            "                    <IfFileName glob=\"${LOG_FILE}-*.log.gz\"/>",
            "                    <IfLastModified age=\"7d\"/>",
            "                </Delete>",
            "            </DefaultRolloverStrategy>",
            "        </RollingFile>",
            "",
            "        <Async name=\"AsyncFile\" includeLocation=\"true\">",
            "            <AppenderRef ref=\"RollingFile\"/>",
            "        </Async>",
            "    </Appenders>",
            "",
            "    <Loggers>",
            "        <Logger name=\"com.yourcompany\" level=\"DEBUG\" additivity=\"false\">",
            "            <AppenderRef ref=\"AsyncFile\"/>",
            "            <AppenderRef ref=\"Console\"/>",
            "        </Logger>",
            "",
            "        <Logger name=\"io.quarkus\" level=\"INFO\" additivity=\"false\">",
            "            <AppenderRef ref=\"AsyncFile\"/>",
            "        </Logger>",
            "",
            "        <Logger name=\"org.hibernate\" level=\"WARN\" additivity=\"false\">",
            "            <AppenderRef ref=\"AsyncFile\"/>",
            "        </Logger>",
            "",
            "        <Root level=\"INFO\">",
            "            <AppenderRef ref=\"AsyncFile\"/>",
            "            <AppenderRef ref=\"Console\"/>",
            "        </Root>",
            "    </Loggers>",
            "</Configuration>"
        );
    }

    private void configureLogrotate() throws Exception {
        logger.info("Configuring logrotate for Quarkus logs...");

        String logrotateConfig = String.join("\n",
            "/var/log/quarkus/*.log {",
            "    daily",
            "    rotate 7",
            "    maxsize 100M",
            "    compress",
            "    delaycompress",
            "    missingok",
            "    notifempty",
            "    create 0640 quarkus quarkus",
            "    sharedscripts",
            "}"
        );

        device.executeCommand("echo '" + logrotateConfig + "' | lxc exec " + containerName +
            " -- tee /etc/logrotate.d/quarkus > /dev/null");

        logger.info("Logrotate configured");
    }

    private void configurePromtailService() throws Exception {
        logger.info("Creating Promtail systemd service...");

        String serviceContent = String.join("\n",
            "[Unit]",
            "Description=Promtail Log Shipper for Grafana Loki",
            "Documentation=https://grafana.com/docs/loki/latest/clients/promtail/",
            "After=network-online.target",
            "Wants=network-online.target",
            "",
            "[Service]",
            "Type=simple",
            "User=root",
            "Group=root",
            "WorkingDirectory=/opt/promtail",
            "ExecStart=/opt/promtail/promtail -config.file=/etc/promtail/config.yml -config.expand-env=true",
            "Restart=on-failure",
            "RestartSec=10s",
            "LimitNOFILE=65536",
            "StandardOutput=journal",
            "StandardError=journal",
            "SyslogIdentifier=promtail",
            "",
            "[Install]",
            "WantedBy=multi-user.target"
        );

        device.executeCommand("echo '" + serviceContent + "' | lxc exec " + containerName +
            " -- tee /etc/systemd/system/promtail.service > /dev/null");

        // Enable service (will start on next boot)
        device.executeCommand("lxc exec " + containerName + " -- systemctl enable promtail.service");

        logger.info("Promtail service enabled");
    }

    private void configurePromtailConfig() throws Exception {
        logger.info("Creating Promtail configuration...");

        String lokiEndpoint = config.getProperty("LOKI_ENDPOINT", "http://grafana-loki-v.1:3100");
        String scrapeInterval = config.getProperty("PROMTAIL_SCRAPE_INTERVAL", "5s");

        String promtailConfig = String.join("\n",
            "server:",
            "  http_listen_port: 9080",
            "  grpc_listen_port: 0",
            "",
            "positions:",
            "  filename: /var/lib/promtail/positions.yaml",
            "",
            "clients:",
            "  - url: " + lokiEndpoint + "/loki/api/v1/push",
            "    batchwait: 1s",
            "    batchsize: 1048576",
            "    backoff_config:",
            "      min_period: 500ms",
            "      max_period: 5m",
            "      max_retries: 10",
            "    timeout: 10s",
            "",
            "scrape_configs:",
            "  - job_name: quarkus",
            "    static_configs:",
            "      - targets:",
            "          - localhost",
            "        labels:",
            "          job: quarkus-app",
            "          container: ${CONTAINER_NAME}",
            "          app: ${APP_NAME}",
            "          env: ${ENV:-dev}",
            "          __path__: /var/log/quarkus/*.log",
            "    pipeline_stages:",
            "      - json:",
            "          expressions:",
            "            timestamp: '@timestamp'",
            "            level: level",
            "            message: message",
            "            logger: logger_name",
            "            thread: thread_name",
            "      - labels:",
            "          level:",
            "          logger:",
            "      - timestamp:",
            "          source: timestamp",
            "          format: RFC3339Nano",
            "      - output:",
            "          source: message"
        );

        device.executeCommand("echo '" + promtailConfig + "' | lxc exec " + containerName +
            " -- tee /etc/promtail/config.yml > /dev/null");

        // Create positions directory
        device.executeCommand("lxc exec " + containerName + " -- mkdir -p /var/lib/promtail");

        logger.info("Promtail configuration created");
    }

    private void stopContainer() throws Exception {
        logger.info("Stopping container...");
        device.executeCommand("lxc stop " + containerName);
    }

    private void publishAsImage() throws Exception {
        logger.info("Publishing as LXC image: " + imageAlias + "...");

        // Delete existing image if exists
        device.executeCommand("lxc image delete " + imageAlias + " 2>/dev/null || true");

        // Publish
        String description = config.getProperty("IMAGE_DESCRIPTION",
            "Quarkus runner base container");
        device.executeCommand("lxc publish " + containerName + " --alias " + imageAlias +
            " description=\"" + description + "\"");

        logger.info("Image published: " + imageAlias);
    }

    private void createSnapshot(StorageVolume volume) throws Exception {
        logger.info("Creating BTRFS snapshot...");
        String snapshotName = containerName + "-snapshot-" + System.currentTimeMillis();
        storageProvider.createSnapshot(device, volume, snapshotName);
        logger.info("Created snapshot: " + snapshotName);
    }

    private void exportContainer() throws Exception {
        logger.info("Exporting container...");
        String exportPath = config.getProperty("EXPORT_PATH", "/tmp");
        String exportFile = exportPath + "/" + containerName + "-" +
            System.currentTimeMillis() + ".tar.gz";

        device.executeCommand("lxc export " + containerName + " " + exportFile);
        logger.info("Exported to: " + exportFile);
    }

    public static void main(String[] args) {
        try {
            String configFile = args.length > 0 ? args[0] :
                "/home/mustafa/telcobright-projects/orchestrix/images/lxc/quarkus-runner/build/build.conf";

            QuarkusBaseContainerBuilder builder = new QuarkusBaseContainerBuilder(configFile);
            builder.build();
            System.exit(0);
        } catch (Exception e) {
            logger.severe("Build failed: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}
