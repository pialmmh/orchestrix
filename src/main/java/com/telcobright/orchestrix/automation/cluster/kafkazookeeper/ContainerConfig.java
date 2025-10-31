package com.telcobright.orchestrix.automation.cluster.kafkazookeeper;

import java.util.HashMap;
import java.util.Map;

/**
 * Configuration for a single container (Kafka or Zookeeper)
 */
public class ContainerConfig {
    private final String containerName;
    private final String staticIp;
    private final ServerConfig server;
    private final Map<String, String> properties;
    private final ContainerType type;

    public enum ContainerType {
        ZOOKEEPER, KAFKA
    }

    public ContainerConfig(String containerName, String staticIp, ServerConfig server, ContainerType type) {
        this.containerName = containerName;
        this.staticIp = staticIp;
        this.server = server;
        this.type = type;
        this.properties = new HashMap<>();
    }

    public void setProperty(String key, String value) {
        properties.put(key, value);
    }

    public String getProperty(String key) {
        return properties.get(key);
    }

    public String getProperty(String key, String defaultValue) {
        String value = properties.get(key);
        return (value != null && !value.isEmpty()) ? value : defaultValue;
    }

    public Map<String, String> getProperties() {
        return properties;
    }

    public String getContainerName() {
        return containerName;
    }

    public String getStaticIp() {
        return staticIp;
    }

    public ServerConfig getServer() {
        return server;
    }

    public ContainerType getType() {
        return type;
    }

    @Override
    public String toString() {
        return String.format("ContainerConfig{name='%s', ip='%s', type=%s, server=%s}",
                containerName, staticIp, type, server.getHost());
    }
}
