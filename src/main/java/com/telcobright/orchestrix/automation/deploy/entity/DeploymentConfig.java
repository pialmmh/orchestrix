package com.telcobright.orchestrix.automation.deploy.entity;

/**
 * Deployment configuration for remote server
 */
public class DeploymentConfig {
    private String serverIp;
    private int serverPort;
    private String sshUser;
    private String sshPassword;
    private String sshKeyPath;

    // Artifact information
    private String artifactPath;
    private String artifactName;
    private String artifactVersion;

    // Container configuration
    private String containerName;
    private String imageName;
    private String networkBridge;
    private String containerIp;
    private String portMapping;

    // Service configuration
    private Integer servicePort;
    private String storageQuota;

    public DeploymentConfig() {
    }

    // Getters and Setters
    public String getServerIp() {
        return serverIp;
    }

    public void setServerIp(String serverIp) {
        this.serverIp = serverIp;
    }

    public int getServerPort() {
        return serverPort;
    }

    public void setServerPort(int serverPort) {
        this.serverPort = serverPort;
    }

    public String getSshUser() {
        return sshUser;
    }

    public void setSshUser(String sshUser) {
        this.sshUser = sshUser;
    }

    public String getSshPassword() {
        return sshPassword;
    }

    public void setSshPassword(String sshPassword) {
        this.sshPassword = sshPassword;
    }

    public String getSshKeyPath() {
        return sshKeyPath;
    }

    public void setSshKeyPath(String sshKeyPath) {
        this.sshKeyPath = sshKeyPath;
    }

    public String getArtifactPath() {
        return artifactPath;
    }

    public void setArtifactPath(String artifactPath) {
        this.artifactPath = artifactPath;
    }

    public String getArtifactName() {
        return artifactName;
    }

    public void setArtifactName(String artifactName) {
        this.artifactName = artifactName;
    }

    public String getArtifactVersion() {
        return artifactVersion;
    }

    public void setArtifactVersion(String artifactVersion) {
        this.artifactVersion = artifactVersion;
    }

    public String getContainerName() {
        return containerName;
    }

    public void setContainerName(String containerName) {
        this.containerName = containerName;
    }

    public String getImageName() {
        return imageName;
    }

    public void setImageName(String imageName) {
        this.imageName = imageName;
    }

    public String getNetworkBridge() {
        return networkBridge;
    }

    public void setNetworkBridge(String networkBridge) {
        this.networkBridge = networkBridge;
    }

    public String getContainerIp() {
        return containerIp;
    }

    public void setContainerIp(String containerIp) {
        this.containerIp = containerIp;
    }

    public String getPortMapping() {
        return portMapping;
    }

    public void setPortMapping(String portMapping) {
        this.portMapping = portMapping;
    }

    public Integer getServicePort() {
        return servicePort;
    }

    public void setServicePort(Integer servicePort) {
        this.servicePort = servicePort;
    }

    public String getStorageQuota() {
        return storageQuota;
    }

    public void setStorageQuota(String storageQuota) {
        this.storageQuota = storageQuota;
    }

    @Override
    public String toString() {
        return "DeploymentConfig{" +
                "serverIp='" + serverIp + '\'' +
                ", serverPort=" + serverPort +
                ", sshUser='" + sshUser + '\'' +
                ", containerName='" + containerName + '\'' +
                ", imageName='" + imageName + '\'' +
                ", containerIp='" + containerIp + '\'' +
                '}';
    }
}
