package com.telcobright.orchestrix.automation.config;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/**
 * Common Configuration Base Class
 *
 * Automation-agnostic configuration loaded from deployments/{tenant}/common.conf
 *
 * Contains shared parameters used across ALL automations:
 * - SSH credentials and connection parameters
 * - Node definitions (IPs, hostnames)
 * - Network configuration (overlay, container supernet, management)
 * - LXD/LXC defaults
 * - Deployment options
 *
 * Automation-specific configs (FRR, Kafka, MySQL) extend this class
 *
 * @author TelcoBright Orchestrix Team
 * @since 2025-11-16
 */
public class CommonConfig {

    protected final Map<String, String> properties;
    protected String configFilePath;

    /**
     * Load common configuration from tenant directory
     */
    public CommonConfig(String tenantDir) throws IOException {
        this.configFilePath = tenantDir + "/common.conf";
        this.properties = new HashMap<>();
        parseConfig(this.configFilePath);
    }

    /**
     * For subclasses that need custom initialization
     */
    protected CommonConfig() {
        this.properties = new HashMap<>();
    }

    /**
     * Parse shell-style configuration file (KEY="value" or KEY=value)
     */
    protected void parseConfig(String filePath) throws IOException {
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
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

        // Auto-load password from file if SSH_PASSWORD_FILE is set
        loadPasswordFromFile();
    }

    /**
     * Load password from SSH_PASSWORD_FILE if specified
     */
    private void loadPasswordFromFile() {
        String passwordFile = getProperty("SSH_PASSWORD_FILE");
        if (passwordFile != null && !passwordFile.isEmpty()) {
            try {
                String password = Files.readString(Path.of(passwordFile)).trim();
                properties.put("SSH_PASSWORD", password);
            } catch (IOException e) {
                System.err.println("Warning: Could not load password from: " + passwordFile);
            }
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

    // ============================================================
    // Tenant Information
    // ============================================================

    public String getTenantName() {
        return getProperty("TENANT_NAME");
    }

    public String getTenantId() {
        return getProperty("TENANT_ID");
    }

    public String getEnvironment() {
        return getProperty("ENVIRONMENT", "production");
    }

    // ============================================================
    // SSH Connection Parameters
    // ============================================================

    public String getSshUser() {
        return getProperty("SSH_USER");
    }

    public String getSshPassword() {
        return getProperty("SSH_PASSWORD");
    }

    public String getSshPasswordFile() {
        return getProperty("SSH_PASSWORD_FILE");
    }

    public String getSshKeyFile() {
        return getProperty("SSH_KEY_FILE");
    }

    public int getSshPort() {
        return Integer.parseInt(getProperty("SSH_PORT", "22"));
    }

    public int getSshTimeout() {
        return Integer.parseInt(getProperty("SSH_TIMEOUT", "30"));
    }

    public String getSshOptions() {
        return getProperty("SSH_OPTIONS", "");
    }

    // ============================================================
    // Node Definitions
    // ============================================================

    public String getNode1Ip() {
        return getProperty("NODE1_IP");
    }

    public String getNode2Ip() {
        return getProperty("NODE2_IP");
    }

    public String getNode3Ip() {
        return getProperty("NODE3_IP");
    }

    public String getNode1Hostname() {
        return getProperty("NODE1_HOSTNAME");
    }

    public String getNode2Hostname() {
        return getProperty("NODE2_HOSTNAME");
    }

    public String getNode3Hostname() {
        return getProperty("NODE3_HOSTNAME");
    }

    /**
     * Get node IP by index (1-based)
     */
    public String getNodeIp(int nodeNumber) {
        return getProperty("NODE" + nodeNumber + "_IP");
    }

    /**
     * Get node hostname by index (1-based)
     */
    public String getNodeHostname(int nodeNumber) {
        return getProperty("NODE" + nodeNumber + "_HOSTNAME");
    }

    // ============================================================
    // Network Configuration
    // ============================================================

    public String getOverlayNetwork() {
        return getProperty("OVERLAY_NETWORK");
    }

    public int getOverlayPort() {
        return Integer.parseInt(getProperty("OVERLAY_PORT", "51820"));
    }

    public String getContainerSupernet() {
        return getProperty("CONTAINER_SUPERNET");
    }

    public String getManagementNetwork() {
        return getProperty("MANAGEMENT_NETWORK");
    }

    public String getManagementGateway() {
        return getProperty("MANAGEMENT_GATEWAY");
    }

    // ============================================================
    // LXD/LXC Configuration
    // ============================================================

    public String getLxdBridge() {
        return getProperty("LXD_BRIDGE", "lxdbr0");
    }

    public String getDefaultMemoryLimit() {
        return getProperty("DEFAULT_MEMORY_LIMIT", "512MB");
    }

    public String getDefaultCpuLimit() {
        return getProperty("DEFAULT_CPU_LIMIT", "1.0");
    }

    // ============================================================
    // Deployment Options
    // ============================================================

    public boolean isVerbose() {
        return Boolean.parseBoolean(getProperty("VERBOSE", "false"));
    }

    public boolean isDryRun() {
        return Boolean.parseBoolean(getProperty("DRY_RUN", "false"));
    }

    public boolean isAutoConfirm() {
        return Boolean.parseBoolean(getProperty("AUTO_CONFIRM", "false"));
    }

    public boolean isCreateBackup() {
        return Boolean.parseBoolean(getProperty("CREATE_BACKUP", "true"));
    }

    // ============================================================
    // Image Repository
    // ============================================================

    public String getFrrBaseImage() {
        return getProperty("FRR_BASE_IMAGE", "frr-router-base-v.1.0.0");
    }

    public String getLxdBaseImage() {
        return getProperty("LXD_BASE_IMAGE", "debian/12");
    }

    @Override
    public String toString() {
        return "CommonConfig{" +
                "tenant=" + getTenantName() +
                ", env=" + getEnvironment() +
                ", nodes=[" + getNode1Ip() + ", " + getNode2Ip() + ", " + getNode3Ip() + "]" +
                ", overlay=" + getOverlayNetwork() +
                '}';
    }
}
