package com.telcobright.orchestrix.automation.deploy.entity;

/**
 * Single deployment target (server)
 */
public class DeploymentTarget {
    private String name;              // Target name for logging
    private String serverIp;
    private int serverPort = 22;
    private String sshUser;
    private String sshPassword;
    private String sshKeyPath;
    private String containerName;     // Specific container name for this target
    private String containerIp;       // IP address for this container
    private String portMapping;       // Override port mapping if needed

    public DeploymentTarget() {
    }

    public DeploymentTarget(String name, String serverIp, String sshUser) {
        this.name = name;
        this.serverIp = serverIp;
        this.sshUser = sshUser;
    }

    // Getters and Setters
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

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

    public String getContainerName() {
        return containerName;
    }

    public void setContainerName(String containerName) {
        this.containerName = containerName;
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

    @Override
    public String toString() {
        return "DeploymentTarget{" +
                "name='" + name + '\'' +
                ", serverIp='" + serverIp + '\'' +
                ", containerName='" + containerName + '\'' +
                '}';
    }
}
