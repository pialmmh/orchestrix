package com.telcobright.orchestrix.automation.consul;

import com.telcobright.orchestrix.automation.core.device.CommandExecutor;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * Consul Cluster Builder for LXC
 *
 * Creates a minimal 3-node Consul cluster using Alpine containers.
 * Each node is ~35MB (Alpine + Consul binary).
 *
 * Features:
 * - 3-node cluster (can handle 1 failure)
 * - Alpine-based for minimal size
 * - Auto-join using LXC networking
 * - Web UI on leader node
 * - Ready for service registration
 */
public class ConsulClusterBuilder {

    private static final Logger logger = Logger.getLogger(ConsulClusterBuilder.class.getName());

    private final CommandExecutor device;
    private final ConsulClusterConfig config;
    private final List<String> nodeIPs = new ArrayList<>();

    public ConsulClusterBuilder(CommandExecutor device, ConsulClusterConfig config) {
        this.device = device;
        this.config = config;
    }

    /**
     * Build and deploy Consul cluster
     */
    public void buildCluster() throws Exception {
        logger.info("=================================================================");
        logger.info("  Building Consul Cluster");
        logger.info("=================================================================");
        logger.info("Nodes: " + config.getNodeCount());
        logger.info("Base Port: " + config.getBasePort());
        logger.info("");

        // Clean up existing containers
        cleanupExisting();

        // Create each node
        for (int i = 1; i <= config.getNodeCount(); i++) {
            createConsulNode(i);
        }

        // Start Consul on each node
        for (int i = 1; i <= config.getNodeCount(); i++) {
            startConsulService(i);
        }

        // Wait for cluster to form
        Thread.sleep(5000);

        // Verify cluster
        verifyCluster();

        logger.info("");
        logger.info("=================================================================");
        logger.info("  Consul Cluster Ready!");
        logger.info("=================================================================");
        logger.info("Leader: " + getLeader());
        logger.info("UI: http://" + nodeIPs.get(0) + ":" + config.getHttpPort());
        logger.info("");
        logger.info("Test service registration:");
        logger.info("  curl -X PUT -d '{\"ID\":\"test\",\"Name\":\"test\",\"Port\":8080}' \\");
        logger.info("    http://" + nodeIPs.get(0) + ":" + config.getHttpPort() + "/v1/agent/service/register");
        logger.info("");
    }

    private void cleanupExisting() throws Exception {
        logger.info("Cleaning up existing Consul containers...");

        for (int i = 1; i <= config.getNodeCount(); i++) {
            String containerName = config.getContainerPrefix() + i;
            try {
                device.executeCommand("lxc stop " + containerName + " --force 2>/dev/null");
                device.executeCommand("lxc delete " + containerName + " --force 2>/dev/null");
            } catch (Exception ignored) {}
        }
    }

    private void createConsulNode(int nodeId) throws Exception {
        String containerName = config.getContainerPrefix() + nodeId;
        logger.info("Creating node " + nodeId + ": " + containerName);

        // Create Alpine container
        device.executeCommand("lxc launch images:alpine/3.18 " + containerName);

        // Wait for network
        Thread.sleep(2000);

        // Get container IP
        String ip = device.executeCommand(
            "lxc list " + containerName + " -c4 --format csv | cut -d' ' -f1"
        ).trim();
        nodeIPs.add(ip);
        logger.info("  IP: " + ip);

        // Install Consul binary
        logger.info("  Installing Consul...");

        // Download Consul if not cached
        String consulPath = "/tmp/consul_" + config.getConsulVersion() + "_linux_amd64";
        if (!device.executeCommand("[ -f " + consulPath + " ] && echo exists || echo missing").contains("exists")) {
            device.executeCommand(
                "cd /tmp && wget -q https://releases.hashicorp.com/consul/" + config.getConsulVersion() +
                "/consul_" + config.getConsulVersion() + "_linux_amd64.zip && " +
                "unzip -q consul_" + config.getConsulVersion() + "_linux_amd64.zip && " +
                "mv consul " + consulPath
            );
        }

        // Copy Consul to container
        device.executeCommand("lxc file push " + consulPath + " " + containerName + "/usr/local/bin/consul");
        device.executeCommand("lxc exec " + containerName + " -- chmod +x /usr/local/bin/consul");

        // Create Consul directories
        device.executeCommand("lxc exec " + containerName + " -- mkdir -p /etc/consul /var/lib/consul");

        // Create Consul configuration
        String consulConfig = generateConsulConfig(nodeId, ip);
        device.executeCommand(
            "echo '" + consulConfig + "' | lxc exec " + containerName + " -- tee /etc/consul/consul.json"
        );

        logger.info("  ✓ Node " + nodeId + " ready");
    }

    private String generateConsulConfig(int nodeId, String nodeIP) {
        boolean isServer = nodeId <= 3; // First 3 nodes are servers
        boolean isBootstrap = nodeId == 1; // First node bootstraps

        StringBuilder config = new StringBuilder();
        config.append("{\n");
        config.append("  \"node_name\": \"consul-").append(nodeId).append("\",\n");
        config.append("  \"datacenter\": \"").append(this.config.getDatacenter()).append("\",\n");
        config.append("  \"data_dir\": \"/var/lib/consul\",\n");
        config.append("  \"log_level\": \"INFO\",\n");
        config.append("  \"server\": ").append(isServer).append(",\n");

        if (isServer) {
            config.append("  \"bootstrap_expect\": ").append(Math.min(3, this.config.getNodeCount())).append(",\n");
        }

        config.append("  \"bind_addr\": \"").append(nodeIP).append("\",\n");
        config.append("  \"client_addr\": \"0.0.0.0\",\n");

        // Join other nodes
        if (!isBootstrap && !nodeIPs.isEmpty()) {
            config.append("  \"retry_join\": [");
            for (int i = 0; i < nodeIPs.size() - 1; i++) {
                if (i > 0) config.append(", ");
                config.append("\"").append(nodeIPs.get(i)).append("\"");
            }
            config.append("],\n");
        }

        config.append("  \"ui_config\": {\n");
        config.append("    \"enabled\": ").append(nodeId == 1).append("\n");
        config.append("  },\n");

        config.append("  \"connect\": {\n");
        config.append("    \"enabled\": true\n");
        config.append("  },\n");

        config.append("  \"ports\": {\n");
        config.append("    \"grpc\": 8502\n");
        config.append("  },\n");

        config.append("  \"acl\": {\n");
        config.append("    \"enabled\": false\n");
        config.append("  }\n");

        config.append("}");

        return config.toString();
    }

    private void startConsulService(int nodeId) throws Exception {
        String containerName = config.getContainerPrefix() + nodeId;
        logger.info("Starting Consul on node " + nodeId + "...");

        // Create init script
        String initScript = """
#!/bin/sh
# Consul service
exec /usr/local/bin/consul agent -config-dir=/etc/consul
""";

        device.executeCommand(
            "echo '" + initScript + "' | lxc exec " + containerName +
            " -- tee /etc/init.d/consul"
        );

        device.executeCommand("lxc exec " + containerName + " -- chmod +x /etc/init.d/consul");

        // Start Consul in background
        device.executeCommand(
            "lxc exec " + containerName + " -- sh -c " +
            "'/usr/local/bin/consul agent -config-dir=/etc/consul > /var/log/consul.log 2>&1 &'"
        );

        logger.info("  ✓ Consul started on node " + nodeId);
    }

    private void verifyCluster() throws Exception {
        logger.info("Verifying cluster formation...");

        Thread.sleep(3000);

        String members = device.executeCommand(
            "lxc exec " + config.getContainerPrefix() + "1 -- consul members"
        );

        int aliveCount = 0;
        for (String line : members.split("\n")) {
            if (line.contains("alive")) {
                aliveCount++;
            }
        }

        if (aliveCount < config.getNodeCount()) {
            logger.warning("Only " + aliveCount + " of " + config.getNodeCount() + " nodes are alive");
        } else {
            logger.info("✓ All " + config.getNodeCount() + " nodes are alive");
        }
    }

    private String getLeader() throws Exception {
        try {
            String leader = device.executeCommand(
                "curl -s http://" + nodeIPs.get(0) + ":" + config.getHttpPort() +
                "/v1/status/leader"
            );
            return leader.replace("\"", "");
        } catch (Exception e) {
            return "Unknown";
        }
    }

    /**
     * Stop all Consul containers
     */
    public void stopCluster() throws Exception {
        logger.info("Stopping Consul cluster...");

        for (int i = 1; i <= config.getNodeCount(); i++) {
            String containerName = config.getContainerPrefix() + i;
            device.executeCommand("lxc stop " + containerName + " --force");
        }

        logger.info("✓ Cluster stopped");
    }

    /**
     * Delete all Consul containers
     */
    public void deleteCluster() throws Exception {
        logger.info("Deleting Consul cluster...");

        for (int i = 1; i <= config.getNodeCount(); i++) {
            String containerName = config.getContainerPrefix() + i;
            device.executeCommand("lxc stop " + containerName + " --force 2>/dev/null || true");
            device.executeCommand("lxc delete " + containerName + " --force");
        }

        logger.info("✓ Cluster deleted");
    }
}