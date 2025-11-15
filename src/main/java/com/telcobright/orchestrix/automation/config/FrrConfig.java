package com.telcobright.orchestrix.automation.config;

import java.io.IOException;

/**
 * FRR Router Configuration
 *
 * Extends CommonConfig with FRR/BGP-specific parameters
 *
 * Loads configuration in two stages:
 * 1. Common config: deployments/{tenant}/common.conf
 * 2. FRR-specific: deployments/{tenant}/frr/{node}-config.conf
 *
 * Usage:
 * <pre>
 * FrrConfig config = new FrrConfig("deployments/netlab", "frr/node1-config.conf");
 * // Access common params
 * String sshUser = config.getSshUser();
 * String node1Ip = config.getNode1Ip();
 * // Access FRR-specific params
 * String bgpAsn = config.getBgpAsn();
 * String containerName = config.getContainerName();
 * </pre>
 *
 * @author TelcoBright Orchestrix Team
 * @since 2025-11-16
 */
public class FrrConfig extends CommonConfig {

    /**
     * Load FRR configuration for a specific node
     *
     * @param tenantDir Base tenant directory (e.g., "deployments/netlab")
     * @param frrConfigPath FRR config path relative to tenant dir (e.g., "frr/node1-config.conf")
     */
    public FrrConfig(String tenantDir, String frrConfigPath) throws IOException {
        super(); // Don't call super(tenantDir) - we'll load manually

        // Step 1: Load common config
        parseConfig(tenantDir + "/common.conf");

        // Step 2: Load FRR-specific config (overlays/extends common params)
        parseConfig(tenantDir + "/" + frrConfigPath);

        // Validate required FRR parameters
        validate();
    }

    /**
     * Validate required FRR parameters
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

    // ============================================================
    // Container Configuration
    // ============================================================

    public String getContainerName() {
        return getProperty("CONTAINER_NAME");
    }

    public String getBaseImage() {
        return getProperty("BASE_IMAGE");
    }

    public String getMemoryLimit() {
        // FRR-specific override, or fall back to common default
        return getProperty("MEMORY_LIMIT", getDefaultMemoryLimit());
    }

    public String getCpuLimit() {
        // FRR-specific override, or fall back to common default
        return getProperty("CPU_LIMIT", getDefaultCpuLimit());
    }

    // ============================================================
    // BGP Configuration (FRR-specific)
    // ============================================================

    public String getBgpAsn() {
        return getProperty("BGP_ASN");
    }

    public String getBgpNeighbors() {
        return getProperty("BGP_NEIGHBORS", "");
    }

    public String getBgpNetworks() {
        return getProperty("BGP_NETWORKS", "");
    }

    public String getBgpTimers() {
        return getProperty("BGP_TIMERS", "10 30");
    }

    public String getRouterId() {
        return getProperty("ROUTER_ID", null);  // null = auto-detect
    }

    public String getRouterHostname() {
        return getProperty("ROUTER_HOSTNAME", getContainerName());
    }

    // ============================================================
    // Protocol Flags
    // ============================================================

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
        return "FrrConfig{" +
                "tenant=" + getTenantName() +
                ", container=" + getContainerName() +
                ", asn=" + getBgpAsn() +
                ", neighbors=" + getBgpNeighbors() +
                ", networks=" + getBgpNetworks() +
                ", nodes=[" + getNode1Ip() + ", " + getNode2Ip() + ", " + getNode3Ip() + "]" +
                '}';
    }
}
