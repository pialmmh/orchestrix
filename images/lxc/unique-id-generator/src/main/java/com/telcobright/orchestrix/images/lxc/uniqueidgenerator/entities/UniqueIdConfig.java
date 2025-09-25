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

    // Source file paths
    private String serverJsPath = "scripts/server.js";
    private String packageJsonPath = "scripts/package.json";
    private String imageName = "unique-id-generator-base";

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
        return imageName;
    }

    public void setImageName(String imageName) {
        this.imageName = imageName;
    }

    public String getServerJsPath() {
        return serverJsPath;
    }

    public void setServerJsPath(String serverJsPath) {
        this.serverJsPath = serverJsPath;
    }

    public String getPackageJsonPath() {
        return packageJsonPath;
    }

    public void setPackageJsonPath(String packageJsonPath) {
        this.packageJsonPath = packageJsonPath;
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

        // Parse shell-style config file
        java.util.Map<String, String> props = new java.util.HashMap<>();

        try (java.io.BufferedReader reader = new java.io.BufferedReader(
                new java.io.FileReader(configFile))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                // Skip comments and empty lines
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }
                // Parse KEY=VALUE format
                int equalPos = line.indexOf('=');
                if (equalPos > 0) {
                    String key = line.substring(0, equalPos).trim();
                    String value = line.substring(equalPos + 1).trim();
                    // Remove quotes if present
                    if (value.startsWith("\"") && value.endsWith("\"")) {
                        value = value.substring(1, value.length() - 1);
                    }
                    props.put(key, value);
                }
            }
        }

        // Load values from properties
        if (props.containsKey("BASE_IMAGE")) {
            config.setBaseImage(props.get("BASE_IMAGE"));
        }
        if (props.containsKey("BUILD_CONTAINER_NAME")) {
            config.setBuildContainerName(props.get("BUILD_CONTAINER_NAME"));
        }
        if (props.containsKey("BRIDGE")) {
            config.setBridge(props.get("BRIDGE"));
        }
        if (props.containsKey("BUILD_IP")) {
            config.setIpAddress(props.get("BUILD_IP"));
        }
        if (props.containsKey("GATEWAY")) {
            config.setGateway(props.get("GATEWAY"));
        }
        if (props.containsKey("DNS_PRIMARY") && props.containsKey("DNS_SECONDARY")) {
            config.setDnsServers(new String[] {
                props.get("DNS_PRIMARY"),
                props.get("DNS_SECONDARY")
            });
        }
        if (props.containsKey("NODE_VERSION")) {
            config.setNodeVersion(props.get("NODE_VERSION"));
        }
        if (props.containsKey("SERVICE_PORT")) {
            config.setServicePort(Integer.parseInt(props.get("SERVICE_PORT")));
        }
        if (props.containsKey("SERVICE_USER")) {
            config.setServiceUser(props.get("SERVICE_USER"));
        }
        if (props.containsKey("SERVICE_GROUP")) {
            config.setServiceGroup(props.get("SERVICE_GROUP"));
        }
        if (props.containsKey("DATA_DIRECTORY")) {
            config.setDataDirectory(props.get("DATA_DIRECTORY"));
        }
        if (props.containsKey("LOG_FILE")) {
            config.setLogFile(props.get("LOG_FILE"));
        }
        if (props.containsKey("SERVER_JS_PATH")) {
            config.setServerJsPath(props.get("SERVER_JS_PATH"));
        }
        if (props.containsKey("PACKAGE_JSON_PATH")) {
            config.setPackageJsonPath(props.get("PACKAGE_JSON_PATH"));
        }
        if (props.containsKey("IMAGE_NAME")) {
            config.setImageName(props.get("IMAGE_NAME"));
        }

        return config;
    }
}