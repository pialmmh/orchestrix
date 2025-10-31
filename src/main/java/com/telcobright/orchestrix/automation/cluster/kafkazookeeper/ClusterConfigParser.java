package com.telcobright.orchestrix.automation.cluster.kafkazookeeper;

import java.io.*;
import java.util.*;

/**
 * Parses the master cluster configuration file (link3/cluster.conf)
 * and creates ContainerConfig objects for all 6 containers
 */
public class ClusterConfigParser {

    private final Properties properties;
    private final Map<String, ServerConfig> servers;

    public ClusterConfigParser(String configFilePath) throws IOException {
        this.properties = new Properties();
        this.servers = new HashMap<>();

        // Load properties file
        try (FileInputStream fis = new FileInputStream(configFilePath)) {
            properties.load(fis);
        }

        // Parse server definitions
        parseServers();
    }

    private void parseServers() {
        int serverNum = 1;
        while (properties.containsKey("SERVER_" + serverNum)) {
            String serverDef = properties.getProperty("SERVER_" + serverNum);
            String[] parts = serverDef.split(":");

            if (parts.length != 4) {
                throw new IllegalArgumentException("Invalid SERVER_" + serverNum + " format: " + serverDef);
            }

            ServerConfig server = new ServerConfig(
                    parts[0], // host
                    Integer.parseInt(parts[1]), // port
                    parts[2], // user
                    parts[3]  // password
            );

            servers.put("SERVER_" + serverNum, server);
            serverNum++;
        }
    }

    /**
     * Parse all Zookeeper container configurations
     */
    public List<ContainerConfig> parseZookeeperConfigs() {
        List<ContainerConfig> configs = new ArrayList<>();

        for (int i = 1; i <= 3; i++) {
            String prefix = "ZK" + i + "_";

            String containerName = properties.getProperty(prefix + "CONTAINER_NAME");
            String hostIp = properties.getProperty(prefix + "HOST_IP");
            String serverRef = properties.getProperty(prefix + "SERVER");
            ServerConfig server = servers.get(serverRef);

            if (containerName == null || hostIp == null || server == null) {
                throw new IllegalArgumentException("Missing configuration for Zookeeper node " + i);
            }

            ContainerConfig config = new ContainerConfig(
                    containerName, hostIp, server, ContainerConfig.ContainerType.ZOOKEEPER
            );

            // Add Zookeeper-specific properties
            config.setProperty("SERVER_ID", properties.getProperty(prefix + "SERVER_ID"));
            config.setProperty("CLIENT_PORT", properties.getProperty(prefix + "CLIENT_PORT"));
            config.setProperty("PEER_PORT", properties.getProperty(prefix + "PEER_PORT"));
            config.setProperty("LEADER_PORT", properties.getProperty(prefix + "LEADER_PORT"));
            config.setProperty("DATA_DIR", properties.getProperty(prefix + "DATA_DIR"));
            config.setProperty("HEAP_OPTS", properties.getProperty(prefix + "HEAP_OPTS"));
            config.setProperty("BASE_IMAGE", properties.getProperty("ZOOKEEPER_BASE_IMAGE", "zookeeper-base"));

            // Add cluster servers config (customize for each node to bind to 0.0.0.0 for itself)
            String zkServers = properties.getProperty("ZK_SERVERS");
            String serverId = properties.getProperty(prefix + "SERVER_ID");
            String customizedZkServers = customizeZkServersForNode(zkServers, serverId, hostIp);
            config.setProperty("ZOO_SERVERS", customizedZkServers);

            // Add auto-start flag
            config.setProperty("AUTO_START", properties.getProperty("AUTO_START", "true"));

            configs.add(config);
        }

        return configs;
    }

    /**
     * Parse all Kafka container configurations
     */
    public List<ContainerConfig> parseKafkaConfigs() {
        List<ContainerConfig> configs = new ArrayList<>();

        for (int i = 1; i <= 3; i++) {
            String prefix = "KAFKA" + i + "_";

            String containerName = properties.getProperty(prefix + "CONTAINER_NAME");
            String hostIp = properties.getProperty(prefix + "HOST_IP");
            String serverRef = properties.getProperty(prefix + "SERVER");
            ServerConfig server = servers.get(serverRef);

            if (containerName == null || hostIp == null || server == null) {
                throw new IllegalArgumentException("Missing configuration for Kafka broker " + i);
            }

            ContainerConfig config = new ContainerConfig(
                    containerName, hostIp, server, ContainerConfig.ContainerType.KAFKA
            );

            // Add Kafka-specific properties
            config.setProperty("PORT", properties.getProperty(prefix + "PORT"));
            config.setProperty("BROKER_ID", properties.getProperty(prefix + "BROKER_ID"));
            config.setProperty("ZOOKEEPER_CONNECT", properties.getProperty(prefix + "ZOOKEEPER_CONNECT"));
            config.setProperty("LISTENERS", properties.getProperty(prefix + "LISTENERS"));
            config.setProperty("ADVERTISED_LISTENERS", properties.getProperty(prefix + "ADVERTISED_LISTENERS"));
            config.setProperty("LOG_DIRS", properties.getProperty(prefix + "LOG_DIRS"));
            config.setProperty("HEAP_OPTS", properties.getProperty(prefix + "HEAP_OPTS"));
            config.setProperty("BTRFS_QUOTA", properties.getProperty(prefix + "BTRFS_QUOTA"));
            config.setProperty("BASE_IMAGE", properties.getProperty("KAFKA_BASE_IMAGE", "kafka-base"));

            // Add auto-start flag
            config.setProperty("AUTO_START", properties.getProperty("AUTO_START", "true"));

            configs.add(config);
        }

        return configs;
    }

    /**
     * Customize ZK_SERVERS for a specific node to bind to 0.0.0.0 for itself
     * Example: server.1=123.200.0.50:2888:3888;server.2=123.200.0.117:2888:3888;server.3=123.200.0.51:2888:3888
     * For server 1, becomes: server.1=0.0.0.0:2888:3888;server.2=123.200.0.117:2888:3888;server.3=123.200.0.51:2888:3888
     */
    private String customizeZkServersForNode(String zkServers, String serverId, String hostIp) {
        // Replace the entry for this server ID with 0.0.0.0
        String serverEntry = "server." + serverId + "=" + hostIp;
        String replacedEntry = "server." + serverId + "=0.0.0.0";
        return zkServers.replace(serverEntry, replacedEntry);
    }

    /**
     * Generate individual container config file content
     */
    public String generateContainerConfigFile(ContainerConfig config) {
        StringBuilder sb = new StringBuilder();

        sb.append("# ").append(config.getType()).append(" Container Configuration\n");
        sb.append("# Auto-generated from cluster config\n\n");

        sb.append("# Container Configuration\n");
        sb.append("CONTAINER_NAME=\"").append(config.getContainerName()).append("\"\n");
        sb.append("BASE_IMAGE=\"").append(config.getProperty("BASE_IMAGE")).append("\"\n\n");

        sb.append("# Network Configuration\n");
        sb.append("STATIC_IP=\"").append(config.getStaticIp()).append("\"\n");
        sb.append("GATEWAY=\"").append(config.getProperty("GATEWAY")).append("\"\n");
        sb.append("DNS_SERVERS=\"").append(config.getProperty("DNS_SERVERS")).append("\"\n");
        sb.append("BRIDGE=\"").append(config.getProperty("BRIDGE")).append("\"\n\n");

        if (config.getType() == ContainerConfig.ContainerType.ZOOKEEPER) {
            sb.append("# Zookeeper Configuration\n");
            sb.append("SERVER_ID=\"").append(config.getProperty("SERVER_ID")).append("\"\n");
            sb.append("CLIENT_PORT=\"").append(config.getProperty("CLIENT_PORT")).append("\"\n");
            sb.append("PEER_PORT=\"").append(config.getProperty("PEER_PORT")).append("\"\n");
            sb.append("LEADER_PORT=\"").append(config.getProperty("LEADER_PORT")).append("\"\n");
            sb.append("DATA_DIR=\"").append(config.getProperty("DATA_DIR")).append("\"\n");
            sb.append("ZOO_HEAP_OPTS=\"").append(config.getProperty("HEAP_OPTS")).append("\"\n");
            sb.append("ZOO_SERVERS=\"").append(config.getProperty("ZOO_SERVERS")).append("\"\n");
        } else {
            sb.append("# Kafka Configuration\n");
            sb.append("BROKER_ID=\"").append(config.getProperty("BROKER_ID")).append("\"\n");
            sb.append("ZOOKEEPER_CONNECT=\"").append(config.getProperty("ZOOKEEPER_CONNECT")).append("\"\n");
            sb.append("LISTENERS=\"").append(config.getProperty("LISTENERS")).append("\"\n");
            sb.append("ADVERTISED_LISTENERS=\"").append(config.getProperty("ADVERTISED_LISTENERS")).append("\"\n");
            sb.append("LOG_DIRS=\"").append(config.getProperty("LOG_DIRS")).append("\"\n");
            sb.append("KAFKA_HEAP_OPTS=\"").append(config.getProperty("HEAP_OPTS")).append("\"\n");
        }

        sb.append("\n# Auto-start Configuration\n");
        sb.append("AUTO_START=\"").append(config.getProperty("AUTO_START")).append("\"\n");

        return sb.toString();
    }

    public String getProperty(String key) {
        return properties.getProperty(key);
    }

    public String getProperty(String key, String defaultValue) {
        return properties.getProperty(key, defaultValue);
    }
}
