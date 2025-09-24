package com.telcobright.orchestrix.images.lxc.uniqueidgenerator.entities;

/**
 * Configuration for Unique ID Generator container
 */
public class UniqueIdConfig {

    // Container settings
    private String containerName = "uid-generator";
    private String baseImage = "images:debian/12";
    private String buildContainerName = "uid-generator-build-temp";

    // Network configuration
    private String ipAddress = "10.10.199.50/24";
    private String gateway = "10.10.199.1/24";
    private String bridge = "lxdbr0";
    private String[] dnsServers = {"8.8.8.8", "8.8.4.4"};

    // Service configuration
    private int servicePort = 7001;
    private String serviceUser = "nobody";
    private String serviceGroup = "nogroup";
    private String dataDirectory = "/var/lib/unique-id-generator";
    private String logFile = "/var/log/unique-id-generator.log";

    // Node.js configuration
    private String nodeVersion = "20";

    // Build options
    private boolean optimizeSize = true;
    private boolean cleanupOnFailure = true;

    // Runtime shard configuration (passed at launch, not build)
    private Integer shardId;
    private Integer totalShards;

    // Constructors
    public UniqueIdConfig() {}

    public UniqueIdConfig(String containerName, String ipAddress) {
        this.containerName = containerName;
        this.ipAddress = ipAddress;
    }

    // Validation
    public boolean isValid() {
        return containerName != null && !containerName.isEmpty() &&
               baseImage != null && !baseImage.isEmpty() &&
               ipAddress != null && ipAddress.endsWith("/24") &&
               gateway != null && gateway.endsWith("/24") &&
               servicePort > 0 && servicePort < 65536;
    }

    public boolean isInRequiredSubnet() {
        return ipAddress != null && ipAddress.startsWith("10.10.199.");
    }

    // Getters and Setters
    public String getContainerName() { return containerName; }
    public void setContainerName(String containerName) { this.containerName = containerName; }

    public String getBaseImage() { return baseImage; }
    public void setBaseImage(String baseImage) { this.baseImage = baseImage; }

    public String getBuildContainerName() { return buildContainerName; }
    public void setBuildContainerName(String buildContainerName) { this.buildContainerName = buildContainerName; }

    public String getIpAddress() { return ipAddress; }
    public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }

    public String getGateway() { return gateway; }
    public void setGateway(String gateway) { this.gateway = gateway; }

    public String getBridge() { return bridge; }
    public void setBridge(String bridge) { this.bridge = bridge; }

    public String[] getDnsServers() { return dnsServers; }
    public void setDnsServers(String[] dnsServers) { this.dnsServers = dnsServers; }

    public int getServicePort() { return servicePort; }
    public void setServicePort(int servicePort) { this.servicePort = servicePort; }

    public String getServiceUser() { return serviceUser; }
    public void setServiceUser(String serviceUser) { this.serviceUser = serviceUser; }

    public String getServiceGroup() { return serviceGroup; }
    public void setServiceGroup(String serviceGroup) { this.serviceGroup = serviceGroup; }

    public String getDataDirectory() { return dataDirectory; }
    public void setDataDirectory(String dataDirectory) { this.dataDirectory = dataDirectory; }

    public String getLogFile() { return logFile; }
    public void setLogFile(String logFile) { this.logFile = logFile; }

    public String getNodeVersion() { return nodeVersion; }
    public void setNodeVersion(String nodeVersion) { this.nodeVersion = nodeVersion; }

    public boolean isOptimizeSize() { return optimizeSize; }
    public void setOptimizeSize(boolean optimizeSize) { this.optimizeSize = optimizeSize; }

    public boolean isCleanupOnFailure() { return cleanupOnFailure; }
    public void setCleanupOnFailure(boolean cleanupOnFailure) { this.cleanupOnFailure = cleanupOnFailure; }

    public Integer getShardId() { return shardId; }
    public void setShardId(Integer shardId) { this.shardId = shardId; }

    public Integer getTotalShards() { return totalShards; }
    public void setTotalShards(Integer totalShards) { this.totalShards = totalShards; }

    public String getIpWithoutMask() {
        return ipAddress != null ? ipAddress.replace("/24", "") : null;
    }

    public String getGatewayWithoutMask() {
        return gateway != null ? gateway.replace("/24", "") : null;
    }

    public String getImageName() {
        return "unique-id-generator-base";
    }

    public String getServerJsPath() {
        return "scripts/server.js";
    }

    public String getPackageJsonPath() {
        return "scripts/package.json";
    }

    public boolean validate() {
        return isValid() && isInRequiredSubnet();
    }

    /**
     * Load configuration from a file
     * @param configFile Path to configuration file
     * @return Loaded configuration
     */
    public static UniqueIdConfig loadFromFile(String configFile) throws Exception {
        UniqueIdConfig config = new UniqueIdConfig();

        // Simple properties file parsing
        java.util.Properties props = new java.util.Properties();
        try (java.io.FileInputStream fis = new java.io.FileInputStream(configFile)) {
            props.load(fis);
        }

        // Load values from properties
        if (props.containsKey("BASE_IMAGE")) {
            config.setBaseImage(props.getProperty("BASE_IMAGE"));
        }
        if (props.containsKey("BUILD_CONTAINER_NAME")) {
            config.setBuildContainerName(props.getProperty("BUILD_CONTAINER_NAME"));
        }
        if (props.containsKey("BRIDGE")) {
            config.setBridge(props.getProperty("BRIDGE"));
        }
        if (props.containsKey("IP_ADDRESS")) {
            config.setIpAddress(props.getProperty("IP_ADDRESS"));
        }
        if (props.containsKey("GATEWAY")) {
            config.setGateway(props.getProperty("GATEWAY"));
        }
        if (props.containsKey("DNS_PRIMARY") && props.containsKey("DNS_SECONDARY")) {
            config.setDnsServers(new String[] {
                props.getProperty("DNS_PRIMARY"),
                props.getProperty("DNS_SECONDARY")
            });
        }
        if (props.containsKey("NODE_VERSION")) {
            config.setNodeVersion(props.getProperty("NODE_VERSION"));
        }
        if (props.containsKey("SERVICE_PORT")) {
            config.setServicePort(Integer.parseInt(props.getProperty("SERVICE_PORT")));
        }
        if (props.containsKey("SERVICE_USER")) {
            config.setServiceUser(props.getProperty("SERVICE_USER"));
        }
        if (props.containsKey("SERVICE_GROUP")) {
            config.setServiceGroup(props.getProperty("SERVICE_GROUP"));
        }
        if (props.containsKey("DATA_DIRECTORY")) {
            config.setDataDirectory(props.getProperty("DATA_DIRECTORY"));
        }
        if (props.containsKey("LOG_FILE")) {
            config.setLogFile(props.getProperty("LOG_FILE"));
        }

        return config;
    }
}