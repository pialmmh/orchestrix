package com.telcobright.orchestrix.automation.devices.server.linux.containerdeploy.base;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.io.FileInputStream;
import java.io.IOException;

/**
 * Configuration for container deployment
 * Supports all container technologies with technology-specific extensions
 */
public class ContainerConfig {

    // Basic configuration
    private String containerName;
    private String imagePath;
    private String imageUrl;
    private String imageChecksum;
    private ContainerTechnology technology;

    // Network configuration
    private String networkMode;
    private String ipAddress;
    private String bridge;
    private Map<Integer, Integer> portMappings = new HashMap<>();
    private List<String> dnsServers = new ArrayList<>();

    // Storage configuration
    private List<VolumeMount> volumeMounts = new ArrayList<>();
    private List<BindMount> bindMounts = new ArrayList<>();
    private String storageDriver;
    private Long diskQuota;

    // Resource limits
    private Long memoryLimit;
    private Long memorySwap;
    private Integer cpuShares;
    private String cpuSet;
    private Integer pidsLimit;

    // Environment and labels
    private Map<String, String> environment = new HashMap<>();
    private Map<String, String> labels = new HashMap<>();

    // Runtime configuration
    private boolean privileged;
    private boolean autoStart;
    private String restartPolicy;
    private String user;
    private String workingDir;
    private List<String> capabilities = new ArrayList<>();

    // Technology-specific configurations
    private Map<String, Object> technologySpecific = new HashMap<>();

    public static class VolumeMount {
        private String volumeName;
        private String containerPath;
        private boolean readOnly;

        public VolumeMount(String volumeName, String containerPath, boolean readOnly) {
            this.volumeName = volumeName;
            this.containerPath = containerPath;
            this.readOnly = readOnly;
        }

        // Getters
        public String getVolumeName() { return volumeName; }
        public String getContainerPath() { return containerPath; }
        public boolean isReadOnly() { return readOnly; }
    }

    public static class BindMount {
        private String hostPath;
        private String containerPath;
        private boolean readOnly;

        public BindMount(String hostPath, String containerPath, boolean readOnly) {
            this.hostPath = hostPath;
            this.containerPath = containerPath;
            this.readOnly = readOnly;
        }

        // Getters
        public String getHostPath() { return hostPath; }
        public String getContainerPath() { return containerPath; }
        public boolean isReadOnly() { return readOnly; }
    }

    /**
     * Load configuration from properties file
     */
    public static ContainerConfig fromProperties(String path) throws IOException {
        Properties props = new Properties();
        try (FileInputStream fis = new FileInputStream(path)) {
            props.load(fis);
        }

        ContainerConfig config = new ContainerConfig();

        // Basic configuration
        config.containerName = props.getProperty("container.name");
        config.imagePath = props.getProperty("image.path");
        config.imageUrl = props.getProperty("image.url");
        config.imageChecksum = props.getProperty("image.checksum");

        String techStr = props.getProperty("container.technology", "LXC");
        config.technology = ContainerTechnology.valueOf(techStr.toUpperCase());

        // Network configuration
        config.networkMode = props.getProperty("network.mode", "bridge");
        config.ipAddress = props.getProperty("network.ip");
        config.bridge = props.getProperty("network.bridge", "lxdbr0");

        // Parse port mappings
        for (String key : props.stringPropertyNames()) {
            if (key.startsWith("port.")) {
                String[] parts = props.getProperty(key).split(":");
                if (parts.length == 2) {
                    config.portMappings.put(
                        Integer.parseInt(parts[0]),
                        Integer.parseInt(parts[1])
                    );
                }
            }
        }

        // Parse bind mounts
        for (String key : props.stringPropertyNames()) {
            if (key.startsWith("mount.") && key.endsWith(".host")) {
                String prefix = key.substring(0, key.lastIndexOf("."));
                String hostPath = props.getProperty(key);
                String containerPath = props.getProperty(prefix + ".container");
                boolean readOnly = Boolean.parseBoolean(
                    props.getProperty(prefix + ".readonly", "false")
                );
                config.bindMounts.add(new BindMount(hostPath, containerPath, readOnly));
            }
        }

        // Parse environment variables
        for (String key : props.stringPropertyNames()) {
            if (key.startsWith("env.")) {
                String envKey = key.substring(4);
                config.environment.put(envKey, props.getProperty(key));
            }
        }

        // Resource limits
        String memLimit = props.getProperty("resources.memory.limit");
        if (memLimit != null) {
            config.memoryLimit = parseMemorySize(memLimit);
        }

        String cpuShares = props.getProperty("resources.cpu.shares");
        if (cpuShares != null) {
            config.cpuShares = Integer.parseInt(cpuShares);
        }

        // Runtime configuration
        config.privileged = Boolean.parseBoolean(
            props.getProperty("runtime.privileged", "false")
        );
        config.autoStart = Boolean.parseBoolean(
            props.getProperty("runtime.autostart", "true")
        );
        config.restartPolicy = props.getProperty("runtime.restart", "unless-stopped");
        config.user = props.getProperty("runtime.user");
        config.workingDir = props.getProperty("runtime.workdir");

        return config;
    }

    private static long parseMemorySize(String size) {
        size = size.toUpperCase();
        if (size.endsWith("G")) {
            return Long.parseLong(size.substring(0, size.length() - 1)) * 1024 * 1024 * 1024;
        } else if (size.endsWith("M")) {
            return Long.parseLong(size.substring(0, size.length() - 1)) * 1024 * 1024;
        } else if (size.endsWith("K")) {
            return Long.parseLong(size.substring(0, size.length() - 1)) * 1024;
        }
        return Long.parseLong(size);
    }

    // Builder pattern for programmatic configuration
    public static class Builder {
        private ContainerConfig config = new ContainerConfig();

        public Builder containerName(String name) {
            config.containerName = name;
            return this;
        }

        public Builder technology(ContainerTechnology tech) {
            config.technology = tech;
            return this;
        }

        public Builder imagePath(String path) {
            config.imagePath = path;
            return this;
        }

        public Builder network(String mode, String ip, String bridge) {
            config.networkMode = mode;
            config.ipAddress = ip;
            config.bridge = bridge;
            return this;
        }

        public Builder addPortMapping(int host, int container) {
            config.portMappings.put(host, container);
            return this;
        }

        public Builder addBindMount(String host, String container, boolean readOnly) {
            config.bindMounts.add(new BindMount(host, container, readOnly));
            return this;
        }

        public Builder addEnvironment(String key, String value) {
            config.environment.put(key, value);
            return this;
        }

        public Builder memoryLimit(long bytes) {
            config.memoryLimit = bytes;
            return this;
        }

        public Builder cpuShares(int shares) {
            config.cpuShares = shares;
            return this;
        }

        public Builder privileged(boolean priv) {
            config.privileged = priv;
            return this;
        }

        public ContainerConfig build() {
            return config;
        }
    }

    // Getters
    public String getContainerName() { return containerName; }
    public String getImagePath() { return imagePath; }
    public String getImageUrl() { return imageUrl; }
    public String getImageChecksum() { return imageChecksum; }
    public ContainerTechnology getTechnology() { return technology; }
    public String getNetworkMode() { return networkMode; }
    public String getIpAddress() { return ipAddress; }
    public String getBridge() { return bridge; }
    public Map<Integer, Integer> getPortMappings() { return portMappings; }
    public List<BindMount> getBindMounts() { return bindMounts; }
    public List<VolumeMount> getVolumeMounts() { return volumeMounts; }
    public Map<String, String> getEnvironment() { return environment; }
    public Map<String, String> getLabels() { return labels; }
    public Long getMemoryLimit() { return memoryLimit; }
    public Integer getCpuShares() { return cpuShares; }
    public boolean isPrivileged() { return privileged; }
    public boolean isAutoStart() { return autoStart; }
    public String getRestartPolicy() { return restartPolicy; }
    public Map<String, Object> getTechnologySpecific() { return technologySpecific; }
}