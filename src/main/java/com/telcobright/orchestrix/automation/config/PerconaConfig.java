package com.telcobright.orchestrix.automation.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * Percona MySQL 5.7 Master-Slave Configuration Parser
 *
 * Parses cluster-config.conf for Percona MySQL master-slave deployment.
 * Follows the same pattern as KafkaConfig for consistency.
 *
 * Config file format:
 * <pre>
 * CLUSTER_NAME="ks_network_mysql"
 * MYSQL_IMAGE="percona:5.7"
 * MYSQL_ROOT_PASSWORD="SecureRootPass123!"
 * INNODB_BUFFER_POOL_SIZE="4G"
 *
 * # Master Node
 * MASTER_MGMT_IP="172.27.27.132"
 * MASTER_MYSQL_IP="10.10.199.30"
 * MASTER_SERVER_ID=1
 *
 * # Slave Count
 * SLAVE_COUNT=1
 *
 * # Slave 1
 * SLAVE1_MGMT_IP="172.27.27.136"
 * SLAVE1_MYSQL_IP="10.10.198.30"
 * SLAVE1_SERVER_ID=2
 * </pre>
 *
 * Usage:
 * <pre>
 * PerconaConfig config = new PerconaConfig("deployments/ks_network/percona");
 * PerconaMasterConfig master = config.getMaster();
 * List&lt;PerconaSlaveConfig&gt; slaves = config.getSlaves();
 * </pre>
 *
 * @author TelcoBright Orchestrix Team
 * @since 2025-11-27
 */
public class PerconaConfig {

    private static final Logger log = LoggerFactory.getLogger(PerconaConfig.class);

    private final Properties properties;
    private final String configDir;

    // Cluster settings
    private String clusterName;
    private String mysqlImage;
    private String mysqlVersion;
    private String rootPassword;
    private String replUser;
    private String replPassword;

    // InnoDB settings
    private String innodbBufferPoolSize;
    private String innodbLogFileSize;
    private int innodbFlushLogAtTrxCommit;

    // GTID settings
    private boolean gtidMode;
    private boolean enforceGtidConsistency;

    // Data directory
    private String dataDir;

    // Master node
    private PerconaMasterConfig master;

    // Slave nodes
    private List<PerconaSlaveConfig> slaves;

    // SSH defaults
    private int sshPort = 22;

    /**
     * Load configuration from directory containing cluster-config.conf
     *
     * @param configDir Directory path (e.g., "deployments/ks_network/percona")
     */
    public PerconaConfig(String configDir) throws IOException {
        this.configDir = configDir;
        this.properties = new Properties();

        Path configFile = Paths.get(configDir, "cluster-config.conf");
        log.info("Loading Percona config from: {}", configFile);

        try (FileInputStream fis = new FileInputStream(configFile.toFile())) {
            properties.load(fis);
        }

        parseConfig();
        validate();

        log.info("Percona config loaded: cluster={}, master={}, slaves={}",
                clusterName, master.getMysqlIp(), slaves.size());
    }

    /**
     * Parse configuration properties
     */
    private void parseConfig() {
        // Cluster settings
        clusterName = getRequired("CLUSTER_NAME");
        mysqlImage = getOrDefault("MYSQL_IMAGE", "percona:5.7");
        mysqlVersion = getOrDefault("MYSQL_VERSION", "5.7");
        rootPassword = getRequired("MYSQL_ROOT_PASSWORD");
        replUser = getOrDefault("MYSQL_REPL_USER", "replicator");
        replPassword = getRequired("MYSQL_REPL_PASSWORD");

        // InnoDB settings
        innodbBufferPoolSize = getOrDefault("INNODB_BUFFER_POOL_SIZE", "4G");
        innodbLogFileSize = getOrDefault("INNODB_LOG_FILE_SIZE", "512M");
        innodbFlushLogAtTrxCommit = getIntOrDefault("INNODB_FLUSH_LOG_AT_TRX_COMMIT", 1);

        // GTID settings
        gtidMode = getBooleanOrDefault("GTID_MODE", true);
        enforceGtidConsistency = getBooleanOrDefault("ENFORCE_GTID_CONSISTENCY", true);

        // Data directory
        dataDir = getOrDefault("MYSQL_DATA_DIR", "/var/lib/mysql");

        // SSH defaults
        sshPort = getIntOrDefault("SSH_PORT", 22);

        // Parse master node
        master = new PerconaMasterConfig();
        master.mgmtIp = getRequired("MASTER_MGMT_IP");
        master.sshUser = getOrDefault("MASTER_SSH_USER", "csas");
        master.sshPassword = getRequired("MASTER_SSH_PASS");
        master.sshPort = sshPort;
        master.mysqlIp = getRequired("MASTER_MYSQL_IP");
        master.serverId = getIntOrDefault("MASTER_SERVER_ID", 1);
        master.bridgeName = getOrDefault("MASTER_BRIDGE_NAME", "lxdbr0");

        // Parse slave nodes
        int slaveCount = getIntOrDefault("SLAVE_COUNT", 0);
        slaves = new ArrayList<>();

        for (int i = 1; i <= slaveCount; i++) {
            PerconaSlaveConfig slave = new PerconaSlaveConfig();
            slave.slaveIndex = i;
            slave.mgmtIp = getRequired("SLAVE" + i + "_MGMT_IP");
            slave.sshUser = getOrDefault("SLAVE" + i + "_SSH_USER", "csas");
            slave.sshPassword = getRequired("SLAVE" + i + "_SSH_PASS");
            slave.sshPort = sshPort;
            slave.mysqlIp = getRequired("SLAVE" + i + "_MYSQL_IP");
            slave.serverId = getIntOrDefault("SLAVE" + i + "_SERVER_ID", i + 1);
            slave.bridgeName = getOrDefault("SLAVE" + i + "_BRIDGE_NAME", "lxdbr0");
            slaves.add(slave);

            log.debug("Parsed slave {}: {} -> {}", i, slave.mgmtIp, slave.mysqlIp);
        }
    }

    /**
     * Validate configuration
     */
    private void validate() {
        List<String> errors = new ArrayList<>();

        if (clusterName == null || clusterName.isEmpty()) {
            errors.add("CLUSTER_NAME is required");
        }

        if (rootPassword == null || rootPassword.isEmpty()) {
            errors.add("MYSQL_ROOT_PASSWORD is required");
        }

        if (replPassword == null || replPassword.isEmpty()) {
            errors.add("MYSQL_REPL_PASSWORD is required");
        }

        if (master.mysqlIp == null || master.mysqlIp.isEmpty()) {
            errors.add("MASTER_MYSQL_IP is required");
        }

        if (master.serverId <= 0) {
            errors.add("MASTER_SERVER_ID must be positive");
        }

        // Validate slaves
        for (PerconaSlaveConfig slave : slaves) {
            if (slave.mysqlIp == null || slave.mysqlIp.isEmpty()) {
                errors.add("SLAVE" + slave.slaveIndex + "_MYSQL_IP is required");
            }
            if (slave.serverId <= 0) {
                errors.add("SLAVE" + slave.slaveIndex + "_SERVER_ID must be positive");
            }
            if (slave.serverId == master.serverId) {
                errors.add("SLAVE" + slave.slaveIndex + "_SERVER_ID must be unique (conflicts with master)");
            }
        }

        if (!errors.isEmpty()) {
            throw new IllegalArgumentException("Configuration errors:\n" + String.join("\n", errors));
        }
    }

    // Helper methods for property parsing
    private String getRequired(String key) {
        String value = properties.getProperty(key);
        if (value == null || value.isEmpty()) {
            throw new IllegalArgumentException("Required property missing: " + key);
        }
        return value.replace("\"", "").trim();
    }

    private String getOrDefault(String key, String defaultValue) {
        String value = properties.getProperty(key);
        if (value == null || value.isEmpty()) {
            return defaultValue;
        }
        return value.replace("\"", "").trim();
    }

    private int getIntOrDefault(String key, int defaultValue) {
        String value = properties.getProperty(key);
        if (value == null || value.isEmpty()) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(value.replace("\"", "").trim());
        } catch (NumberFormatException e) {
            log.warn("Invalid integer for {}: {}, using default {}", key, value, defaultValue);
            return defaultValue;
        }
    }

    private boolean getBooleanOrDefault(String key, boolean defaultValue) {
        String value = properties.getProperty(key);
        if (value == null || value.isEmpty()) {
            return defaultValue;
        }
        String cleaned = value.replace("\"", "").trim().toUpperCase();
        return "ON".equals(cleaned) || "TRUE".equals(cleaned) || "1".equals(cleaned);
    }

    // Getters
    public String getClusterName() { return clusterName; }
    public String getMysqlImage() { return mysqlImage; }
    public String getMysqlVersion() { return mysqlVersion; }
    public String getRootPassword() { return rootPassword; }
    public String getReplUser() { return replUser; }
    public String getReplPassword() { return replPassword; }
    public String getInnodbBufferPoolSize() { return innodbBufferPoolSize; }
    public String getInnodbLogFileSize() { return innodbLogFileSize; }
    public int getInnodbFlushLogAtTrxCommit() { return innodbFlushLogAtTrxCommit; }
    public boolean isGtidMode() { return gtidMode; }
    public boolean isEnforceGtidConsistency() { return enforceGtidConsistency; }
    public String getDataDir() { return dataDir; }
    public PerconaMasterConfig getMaster() { return master; }
    public List<PerconaSlaveConfig> getSlaves() { return slaves; }
    public int getSlaveCount() { return slaves.size(); }

    /**
     * Get connection string for master
     */
    public String getMasterConnectionString() {
        return master.getMysqlIp() + ":3306";
    }

    /**
     * Get all nodes (master + slaves) for iteration
     */
    public List<PerconaNodeConfig> getAllNodes() {
        List<PerconaNodeConfig> all = new ArrayList<>();
        all.add(master);
        all.addAll(slaves);
        return all;
    }

    /**
     * Base configuration for a Percona node
     */
    public static abstract class PerconaNodeConfig {
        protected String mgmtIp;
        protected String sshUser;
        protected String sshPassword;
        protected int sshPort;
        protected String mysqlIp;
        protected int serverId;
        protected String bridgeName;

        public String getMgmtIp() { return mgmtIp; }
        public String getSshUser() { return sshUser; }
        public String getSshPassword() { return sshPassword; }
        public int getSshPort() { return sshPort; }
        public String getMysqlIp() { return mysqlIp; }
        public int getServerId() { return serverId; }
        public String getBridgeName() { return bridgeName; }

        public abstract boolean isMaster();
        public abstract String getNodeName();
    }

    /**
     * Master node configuration
     */
    public static class PerconaMasterConfig extends PerconaNodeConfig {
        @Override
        public boolean isMaster() { return true; }

        @Override
        public String getNodeName() { return "master"; }
    }

    /**
     * Slave node configuration
     */
    public static class PerconaSlaveConfig extends PerconaNodeConfig {
        private int slaveIndex;

        public int getSlaveIndex() { return slaveIndex; }

        @Override
        public boolean isMaster() { return false; }

        @Override
        public String getNodeName() { return "slave" + slaveIndex; }
    }
}
