package com.telcobright.orchestrix.automation.cluster.perconadocker;

import com.telcobright.orchestrix.automation.config.PerconaConfig;
import com.telcobright.orchestrix.automation.config.PerconaConfig.PerconaMasterConfig;
import com.telcobright.orchestrix.automation.config.PerconaConfig.PerconaNodeConfig;
import com.telcobright.orchestrix.automation.config.PerconaConfig.PerconaSlaveConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Docker Compose Generator for Percona MySQL 5.7 Master-Slave
 *
 * Generates docker-compose.yml files for:
 * - Master node with binary logging and GTID
 * - Slave nodes with read-only configuration
 *
 * Uses host networking mode for overlay network compatibility.
 *
 * Usage:
 * <pre>
 * PerconaComposeGenerator generator = new PerconaComposeGenerator(config);
 * String masterYaml = generator.generateForMaster();
 * String slaveYaml = generator.generateForSlave(slaveConfig);
 * </pre>
 *
 * @author TelcoBright Orchestrix Team
 * @since 2025-11-27
 */
public class PerconaComposeGenerator {

    private static final Logger log = LoggerFactory.getLogger(PerconaComposeGenerator.class);
    private final PerconaConfig config;

    public PerconaComposeGenerator(PerconaConfig config) {
        this.config = config;
    }

    /**
     * Generate docker-compose.yml for the master node
     */
    public String generateForMaster() {
        PerconaMasterConfig master = config.getMaster();
        log.debug("Generating master docker-compose.yml for server_id={}", master.getServerId());
        return generateCompose(master, true);
    }

    /**
     * Generate docker-compose.yml for a slave node
     */
    public String generateForSlave(PerconaSlaveConfig slave) {
        log.debug("Generating slave{} docker-compose.yml for server_id={}",
                slave.getSlaveIndex(), slave.getServerId());
        return generateCompose(slave, false);
    }

    /**
     * Generate docker-compose.yml with metadata comments
     */
    public String generateForMasterWithMetadata() {
        PerconaMasterConfig master = config.getMaster();
        return addMetadata(generateForMaster(), master);
    }

    /**
     * Generate docker-compose.yml for slave with metadata
     */
    public String generateForSlaveWithMetadata(PerconaSlaveConfig slave) {
        return addMetadata(generateForSlave(slave), slave);
    }

    /**
     * Generate compose YAML content
     */
    private String generateCompose(PerconaNodeConfig node, boolean isMaster) {
        StringBuilder yaml = new StringBuilder();

        // Version
        yaml.append("version: '3.8'\n\n");

        // Services
        yaml.append("services:\n");
        yaml.append("  percona:\n");
        yaml.append("    image: ").append(config.getMysqlImage()).append("\n");
        yaml.append("    container_name: percona-").append(node.getNodeName()).append("\n");
        yaml.append("    restart: always\n");
        yaml.append("    network_mode: host\n");
        yaml.append("    environment:\n");
        yaml.append("      MYSQL_ROOT_PASSWORD: \"").append(config.getRootPassword()).append("\"\n");
        yaml.append("    command:\n");

        // MySQL server configuration via command line
        yaml.append("      - --server-id=").append(node.getServerId()).append("\n");
        yaml.append("      - --bind-address=").append(node.getMysqlIp()).append("\n");

        // Binary logging (required for replication)
        yaml.append("      - --log-bin=mysql-bin\n");
        yaml.append("      - --binlog-format=ROW\n");
        yaml.append("      - --binlog-row-image=FULL\n");
        yaml.append("      - --expire-logs-days=7\n");  // Purge binary logs after 7 days

        // GTID mode for easier replication management
        if (config.isGtidMode()) {
            yaml.append("      - --gtid-mode=ON\n");
        }
        if (config.isEnforceGtidConsistency()) {
            yaml.append("      - --enforce-gtid-consistency=ON\n");
        }

        // InnoDB settings
        yaml.append("      - --innodb-buffer-pool-size=").append(config.getInnodbBufferPoolSize()).append("\n");
        yaml.append("      - --innodb-log-file-size=").append(config.getInnodbLogFileSize()).append("\n");
        yaml.append("      - --innodb-flush-log-at-trx-commit=").append(config.getInnodbFlushLogAtTrxCommit()).append("\n");
        yaml.append("      - --innodb-flush-method=O_DIRECT\n");

        // Slave-specific settings
        if (!isMaster) {
            yaml.append("      - --read-only=ON\n");
            yaml.append("      - --super-read-only=ON\n");
            yaml.append("      - --relay-log=relay-bin\n");
            yaml.append("      - --log-slave-updates=ON\n");
            yaml.append("      - --skip-slave-start\n");  // Don't auto-start replication
        }

        // Performance settings
        yaml.append("      - --max-connections=500\n");
        yaml.append("      - --max-allowed-packet=64M\n");
        yaml.append("      - --thread-cache-size=128\n");
        yaml.append("      - --table-open-cache=4000\n");
        yaml.append("      - --table-definition-cache=2000\n");

        // Character set
        yaml.append("      - --character-set-server=utf8mb4\n");
        yaml.append("      - --collation-server=utf8mb4_unicode_ci\n");

        // Logging
        yaml.append("      - --slow-query-log=ON\n");
        yaml.append("      - --long-query-time=2\n");

        // Volume
        yaml.append("    volumes:\n");
        yaml.append("      - ").append(config.getDataDir()).append(":/var/lib/mysql\n");

        return yaml.toString();
    }

    /**
     * Add metadata comments to compose file
     */
    private String addMetadata(String yaml, PerconaNodeConfig node) {
        StringBuilder sb = new StringBuilder();

        sb.append("# ============================================================\n");
        sb.append("# Percona MySQL 5.7 - ").append(node.isMaster() ? "MASTER" : "SLAVE").append("\n");
        sb.append("# ============================================================\n");
        sb.append("# Cluster: ").append(config.getClusterName()).append("\n");
        sb.append("# Node: ").append(node.getNodeName()).append("\n");
        sb.append("# Server ID: ").append(node.getServerId()).append("\n");
        sb.append("# MySQL IP: ").append(node.getMysqlIp()).append(":3306\n");

        if (node.isMaster()) {
            sb.append("# Role: MASTER (read-write)\n");
        } else {
            sb.append("# Role: SLAVE (read-only)\n");
            sb.append("# Replicates from: ").append(config.getMaster().getMysqlIp()).append(":3306\n");
        }

        sb.append("# ============================================================\n");
        sb.append("# InnoDB Buffer Pool: ").append(config.getInnodbBufferPoolSize()).append("\n");
        sb.append("# GTID Mode: ").append(config.isGtidMode() ? "ON" : "OFF").append("\n");
        sb.append("# ============================================================\n");
        sb.append("\n");

        sb.append(yaml);

        return sb.toString();
    }

    /**
     * Validate docker-compose YAML
     */
    public boolean validate(String yaml) {
        if (yaml == null || yaml.isEmpty()) {
            log.error("Docker Compose YAML is empty");
            return false;
        }

        if (!yaml.contains("version:")) {
            log.error("Missing 'version:' in docker-compose.yml");
            return false;
        }

        if (!yaml.contains("services:")) {
            log.error("Missing 'services:' in docker-compose.yml");
            return false;
        }

        if (!yaml.contains("server-id=")) {
            log.error("Missing server-id configuration");
            return false;
        }

        if (!yaml.contains("percona:5.7") && !yaml.contains("percona:")) {
            log.error("Missing Percona image reference");
            return false;
        }

        log.debug("Docker Compose YAML validation passed");
        return true;
    }
}
