package com.telcobright.orchestrix.images.model;

import com.telcobright.orchestrix.model.NetworkConfig;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Container configuration - specific to images but reusable across container types
 */
public class ContainerConfig {
    private String containerName;
    private String containerType; // lxc, docker
    private String baseImage;
    private String version;
    private NetworkConfig networkConfig;
    private ResourceLimits resourceLimits;
    private List<VolumeMount> volumeMounts = new ArrayList<>();
    private Map<String, String> environment = new HashMap<>();
    private List<String> exposedPorts = new ArrayList<>();
    private boolean privileged = false;
    private String hostname;

    // Build-specific
    private String buildDirectory;
    private boolean cleanupOnFailure = true;
    private boolean optimizeSize = true;

    // Constructors
    public ContainerConfig() {}

    public ContainerConfig(String containerName, String containerType, String baseImage) {
        this.containerName = containerName;
        this.containerType = containerType;
        this.baseImage = baseImage;
    }

    // Helper methods
    public void addVolumeMount(String hostPath, String containerPath) {
        volumeMounts.add(new VolumeMount(hostPath, containerPath));
    }

    public void addVolumeMount(String hostPath, String containerPath, boolean readOnly) {
        volumeMounts.add(new VolumeMount(hostPath, containerPath, readOnly));
    }

    public void addEnvironment(String key, String value) {
        environment.put(key, value);
    }

    public void exposePort(int port) {
        exposedPorts.add(String.valueOf(port));
    }

    public void exposePort(String portMapping) {
        exposedPorts.add(portMapping);
    }

    public boolean isLxc() {
        return "lxc".equalsIgnoreCase(containerType);
    }

    public boolean isDocker() {
        return "docker".equalsIgnoreCase(containerType);
    }

    // Getters and Setters
    public String getContainerName() { return containerName; }
    public void setContainerName(String containerName) { this.containerName = containerName; }

    public String getContainerType() { return containerType; }
    public void setContainerType(String containerType) { this.containerType = containerType; }

    public String getBaseImage() { return baseImage; }
    public void setBaseImage(String baseImage) { this.baseImage = baseImage; }

    public String getVersion() { return version; }
    public void setVersion(String version) { this.version = version; }

    public NetworkConfig getNetworkConfig() { return networkConfig; }
    public void setNetworkConfig(NetworkConfig networkConfig) { this.networkConfig = networkConfig; }

    public ResourceLimits getResourceLimits() { return resourceLimits; }
    public void setResourceLimits(ResourceLimits resourceLimits) { this.resourceLimits = resourceLimits; }

    public List<VolumeMount> getVolumeMounts() { return volumeMounts; }
    public void setVolumeMounts(List<VolumeMount> volumeMounts) { this.volumeMounts = volumeMounts; }

    public Map<String, String> getEnvironment() { return environment; }
    public void setEnvironment(Map<String, String> environment) { this.environment = environment; }

    public List<String> getExposedPorts() { return exposedPorts; }
    public void setExposedPorts(List<String> exposedPorts) { this.exposedPorts = exposedPorts; }

    public boolean isPrivileged() { return privileged; }
    public void setPrivileged(boolean privileged) { this.privileged = privileged; }

    public String getHostname() { return hostname; }
    public void setHostname(String hostname) { this.hostname = hostname; }

    public String getBuildDirectory() { return buildDirectory; }
    public void setBuildDirectory(String buildDirectory) { this.buildDirectory = buildDirectory; }

    public boolean isCleanupOnFailure() { return cleanupOnFailure; }
    public void setCleanupOnFailure(boolean cleanupOnFailure) { this.cleanupOnFailure = cleanupOnFailure; }

    public boolean isOptimizeSize() { return optimizeSize; }
    public void setOptimizeSize(boolean optimizeSize) { this.optimizeSize = optimizeSize; }

    /**
     * Volume mount configuration
     */
    public static class VolumeMount {
        private String hostPath;
        private String containerPath;
        private boolean readOnly = false;

        public VolumeMount(String hostPath, String containerPath) {
            this.hostPath = hostPath;
            this.containerPath = containerPath;
        }

        public VolumeMount(String hostPath, String containerPath, boolean readOnly) {
            this.hostPath = hostPath;
            this.containerPath = containerPath;
            this.readOnly = readOnly;
        }

        public String getHostPath() { return hostPath; }
        public void setHostPath(String hostPath) { this.hostPath = hostPath; }

        public String getContainerPath() { return containerPath; }
        public void setContainerPath(String containerPath) { this.containerPath = containerPath; }

        public boolean isReadOnly() { return readOnly; }
        public void setReadOnly(boolean readOnly) { this.readOnly = readOnly; }

        @Override
        public String toString() {
            return hostPath + ":" + containerPath + (readOnly ? ":ro" : "");
        }
    }

    /**
     * Resource limits configuration
     */
    public static class ResourceLimits {
        private String memory; // e.g., "512MB", "2GB"
        private Double cpuShares; // e.g., 1.5 = 150%
        private String diskSize; // e.g., "10GB"
        private Integer pidsLimit;

        public String getMemory() { return memory; }
        public void setMemory(String memory) { this.memory = memory; }

        public Double getCpuShares() { return cpuShares; }
        public void setCpuShares(Double cpuShares) { this.cpuShares = cpuShares; }

        public String getDiskSize() { return diskSize; }
        public void setDiskSize(String diskSize) { this.diskSize = diskSize; }

        public Integer getPidsLimit() { return pidsLimit; }
        public void setPidsLimit(Integer pidsLimit) { this.pidsLimit = pidsLimit; }
    }
}