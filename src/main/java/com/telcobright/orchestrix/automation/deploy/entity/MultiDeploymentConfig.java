package com.telcobright.orchestrix.automation.deploy.entity;

import com.telcobright.orchestrix.automation.core.executor.ExecutionMode;

import java.util.ArrayList;
import java.util.List;

/**
 * Multi-target deployment configuration
 * Supports deploying to multiple servers in parallel or sequential mode
 */
public class MultiDeploymentConfig {
    // Artifact information
    private String artifactPath;
    private String artifactName;
    private String artifactVersion;

    // Container configuration
    private String imageName;
    private String containerNamePrefix;  // Will append index for multiple deployments
    private String networkBridge;
    private String portMapping;
    private String storageQuota;
    private Integer servicePort;

    // Execution mode
    private ExecutionMode executionMode = ExecutionMode.SEQUENTIAL;

    // Multiple deployment targets
    private List<DeploymentTarget> deploymentTargets = new ArrayList<>();

    public MultiDeploymentConfig() {
    }

    // Getters and Setters
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

    public String getImageName() {
        return imageName;
    }

    public void setImageName(String imageName) {
        this.imageName = imageName;
    }

    public String getContainerNamePrefix() {
        return containerNamePrefix;
    }

    public void setContainerNamePrefix(String containerNamePrefix) {
        this.containerNamePrefix = containerNamePrefix;
    }

    public String getNetworkBridge() {
        return networkBridge;
    }

    public void setNetworkBridge(String networkBridge) {
        this.networkBridge = networkBridge;
    }

    public String getPortMapping() {
        return portMapping;
    }

    public void setPortMapping(String portMapping) {
        this.portMapping = portMapping;
    }

    public String getStorageQuota() {
        return storageQuota;
    }

    public void setStorageQuota(String storageQuota) {
        this.storageQuota = storageQuota;
    }

    public Integer getServicePort() {
        return servicePort;
    }

    public void setServicePort(Integer servicePort) {
        this.servicePort = servicePort;
    }

    public ExecutionMode getExecutionMode() {
        return executionMode;
    }

    public void setExecutionMode(ExecutionMode executionMode) {
        this.executionMode = executionMode;
    }

    public List<DeploymentTarget> getDeploymentTargets() {
        return deploymentTargets;
    }

    public void setDeploymentTargets(List<DeploymentTarget> deploymentTargets) {
        this.deploymentTargets = deploymentTargets;
    }

    public void addDeploymentTarget(DeploymentTarget target) {
        this.deploymentTargets.add(target);
    }

    @Override
    public String toString() {
        return "MultiDeploymentConfig{" +
                "artifactName='" + artifactName + '\'' +
                ", executionMode=" + executionMode +
                ", deploymentTargets=" + deploymentTargets.size() +
                '}';
    }
}
