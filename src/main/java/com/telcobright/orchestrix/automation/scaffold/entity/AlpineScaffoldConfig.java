package com.telcobright.orchestrix.automation.scaffold.entity;

/**
 * Configuration for Alpine container scaffolding
 */
public class AlpineScaffoldConfig {

    // Service details
    private String serviceName;          // e.g., "Go-ID Generator"
    private String serviceDescription;   // e.g., "Distributed unique ID generation service"
    private int servicePort = 7001;      // Default service port

    // Container details
    private String containerName;        // e.g., "go-id"
    private String containerVersion;     // e.g., "2"

    // Binary details
    private String binaryName;           // e.g., "go-id"
    private String binaryVersion;        // e.g., "1"
    private String binaryPath;           // Full path to binary file
    private String binaryBuilderClass;   // Java class for building binary

    // Paths
    private String orchestrixRoot;       // Root of orchestrix project
    private String containerPath;        // Path to container directory
    private String standaloneBinaryPath; // Path to standalone-binaries folder

    // Additional API endpoints (for documentation)
    private String additionalEndpoints = "";

    public AlpineScaffoldConfig(String serviceName, String containerName, String containerVersion) {
        this.serviceName = serviceName;
        this.containerName = containerName;
        this.containerVersion = containerVersion;

        // Determine orchestrix root
        String currentDir = System.getProperty("user.dir");
        if (currentDir.contains("orchestrix-lxc-work")) {
            this.orchestrixRoot = currentDir.replace("orchestrix-lxc-work", "orchestrix");
        } else if (currentDir.endsWith("orchestrix")) {
            this.orchestrixRoot = currentDir;
        } else {
            this.orchestrixRoot = "/home/mustafa/telcobright-projects/orchestrix";
        }

        this.containerPath = orchestrixRoot + "/images/containers/lxc/" + containerName;
        this.standaloneBinaryPath = orchestrixRoot + "/images/standalone-binaries/" + containerName;
    }

    // Computed properties
    public String getVersionDirName() {
        return containerName + "-v." + containerVersion;
    }

    public String getVersionPath() {
        return containerPath + "/" + getVersionDirName();
    }

    public String getCapitalizedName() {
        // Convert go-id to GoId
        String[] parts = containerName.split("-");
        StringBuilder result = new StringBuilder();
        for (String part : parts) {
            if (!part.isEmpty()) {
                result.append(Character.toUpperCase(part.charAt(0)));
                result.append(part.substring(1));
            }
        }
        return result.toString();
    }

    // Getters and setters
    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    public String getServiceDescription() {
        return serviceDescription;
    }

    public void setServiceDescription(String serviceDescription) {
        this.serviceDescription = serviceDescription;
    }

    public int getServicePort() {
        return servicePort;
    }

    public void setServicePort(int servicePort) {
        this.servicePort = servicePort;
    }

    public String getContainerName() {
        return containerName;
    }

    public void setContainerName(String containerName) {
        this.containerName = containerName;
    }

    public String getContainerVersion() {
        return containerVersion;
    }

    public void setContainerVersion(String containerVersion) {
        this.containerVersion = containerVersion;
    }

    public String getBinaryName() {
        return binaryName;
    }

    public void setBinaryName(String binaryName) {
        this.binaryName = binaryName;
    }

    public String getBinaryVersion() {
        return binaryVersion;
    }

    public void setBinaryVersion(String binaryVersion) {
        this.binaryVersion = binaryVersion;
    }

    public String getBinaryPath() {
        return binaryPath;
    }

    public void setBinaryPath(String binaryPath) {
        this.binaryPath = binaryPath;
    }

    public String getBinaryBuilderClass() {
        return binaryBuilderClass;
    }

    public void setBinaryBuilderClass(String binaryBuilderClass) {
        this.binaryBuilderClass = binaryBuilderClass;
    }

    public String getOrchestrixRoot() {
        return orchestrixRoot;
    }

    public void setOrchestrixRoot(String orchestrixRoot) {
        this.orchestrixRoot = orchestrixRoot;
    }

    public String getContainerPath() {
        return containerPath;
    }

    public void setContainerPath(String containerPath) {
        this.containerPath = containerPath;
    }

    public String getStandaloneBinaryPath() {
        return standaloneBinaryPath;
    }

    public void setStandaloneBinaryPath(String standaloneBinaryPath) {
        this.standaloneBinaryPath = standaloneBinaryPath;
    }

    public String getAdditionalEndpoints() {
        return additionalEndpoints;
    }

    public void setAdditionalEndpoints(String additionalEndpoints) {
        this.additionalEndpoints = additionalEndpoints;
    }
}