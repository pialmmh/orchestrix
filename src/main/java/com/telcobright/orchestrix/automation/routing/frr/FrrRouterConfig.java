package com.telcobright.orchestrix.automation.routing.frr;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * FRR Router Configuration Parser
 *
 * Parses minimal FRR router config files for host networking mode
 *
 * Required Parameters:
 * - CONTAINER_NAME: Name of the LXC container
 * - BASE_IMAGE: LXC base image (e.g., frr-router-base-v.1.0.0)
 * - BGP_ASN: BGP Autonomous System Number
 * - BGP_NEIGHBORS: Comma-separated list of neighbor_ip:remote_asn
 * - BGP_NETWORKS: Comma-separated list of networks to announce
 *
 * Optional Parameters (auto-detected if not provided):
 * - ROUTER_ID: Router ID (defaults to host's external IP)
 * - ROUTER_HOSTNAME: Router hostname (defaults to CONTAINER_NAME)
 * - MEMORY_LIMIT: Container memory limit (default: 100MB)
 * - CPU_LIMIT: Container CPU limit (default: 1)
 * - ENABLE_BGP: Enable BGP (default: true)
 * - ENABLE_OSPF: Enable OSPF (default: false)
 */
public class FrrRouterConfig {

    private final Map<String, String> properties;
    private final String configFilePath;

    public FrrRouterConfig(String configFilePath) throws IOException {
        this.configFilePath = configFilePath;
        this.properties = new HashMap<>();
        parseConfig();
        validate();
    }

    /**
     * Parse configuration file
     */
    private void parseConfig() throws IOException {
        try (BufferedReader reader = new BufferedReader(new FileReader(configFilePath))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();

                // Skip empty lines and comments
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }

                // Parse key=value (handle quoted values)
                if (line.contains("=")) {
                    int equalsIndex = line.indexOf('=');
                    String key = line.substring(0, equalsIndex).trim();
                    String value = line.substring(equalsIndex + 1).trim();

                    // Remove quotes if present
                    if (value.startsWith("\"") && value.endsWith("\"")) {
                        value = value.substring(1, value.length() - 1);
                    }

                    properties.put(key, value);
                }
            }
        }
    }

    /**
     * Validate required parameters
     */
    private void validate() throws IllegalArgumentException {
        requireProperty("CONTAINER_NAME", "Container name is required");
        requireProperty("BASE_IMAGE", "Base image is required");
        requireProperty("BGP_ASN", "BGP ASN is required");

        // BGP_NEIGHBORS and BGP_NETWORKS are optional - can have empty router
    }

    private void requireProperty(String key, String message) {
        if (!properties.containsKey(key) || properties.get(key).trim().isEmpty()) {
            throw new IllegalArgumentException(message + " (missing: " + key + ")");
        }
    }

    /**
     * Get property value
     */
    public String getProperty(String key) {
        return properties.get(key);
    }

    /**
     * Get property with default value
     */
    public String getProperty(String key, String defaultValue) {
        return properties.getOrDefault(key, defaultValue);
    }

    /**
     * Check if property exists
     */
    public boolean hasProperty(String key) {
        return properties.containsKey(key) && !properties.get(key).trim().isEmpty();
    }

    // Convenience getters

    public String getContainerName() {
        return getProperty("CONTAINER_NAME");
    }

    public String getBaseImage() {
        return getProperty("BASE_IMAGE");
    }

    public String getBgpAsn() {
        return getProperty("BGP_ASN");
    }

    public String getBgpNeighbors() {
        return getProperty("BGP_NEIGHBORS", "");
    }

    public String getBgpNetworks() {
        return getProperty("BGP_NETWORKS", "");
    }

    public String getRouterId() {
        return getProperty("ROUTER_ID", null);  // null = auto-detect
    }

    public String getRouterHostname() {
        return getProperty("ROUTER_HOSTNAME", getContainerName());
    }

    public String getMemoryLimit() {
        return getProperty("MEMORY_LIMIT", "100MB");
    }

    public String getCpuLimit() {
        return getProperty("CPU_LIMIT", "1");
    }

    public boolean isBgpEnabled() {
        return Boolean.parseBoolean(getProperty("ENABLE_BGP", "true"));
    }

    public boolean isOspfEnabled() {
        return Boolean.parseBoolean(getProperty("ENABLE_OSPF", "false"));
    }

    public String getOspfArea() {
        return getProperty("OSPF_AREA", "0.0.0.0");
    }

    public String getOspfNetworks() {
        return getProperty("OSPF_NETWORKS", "");
    }

    @Override
    public String toString() {
        return "FrrRouterConfig{" +
                "container=" + getContainerName() +
                ", image=" + getBaseImage() +
                ", asn=" + getBgpAsn() +
                ", neighbors=" + getBgpNeighbors() +
                ", networks=" + getBgpNetworks() +
                '}';
    }
}
