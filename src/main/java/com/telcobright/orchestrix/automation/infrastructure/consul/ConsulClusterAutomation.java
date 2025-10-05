package com.telcobright.orchestrix.automation.infrastructure.consul;

import com.telcobright.orchestrix.automation.core.device.SshDevice;
import com.telcobright.orchestrix.automation.infrastructure.consul.entity.ConsulClusterConfig;
import com.telcobright.orchestrix.automation.infrastructure.consul.entity.ConsulNode;
import com.telcobright.orchestrix.automation.infrastructure.consul.entity.ConsulNodeRole;

import java.util.List;
import java.util.logging.Logger;

/**
 * Minimal Consul cluster automation
 * Sets up Consul cluster without DNS (IP-based only)
 *
 * Usage:
 *   ConsulClusterConfig config = new ConsulClusterConfig();
 *   config.addNode("server-1", "192.168.1.100", ConsulNodeRole.SERVER);
 *   config.addNode("server-2", "192.168.1.101", ConsulNodeRole.SERVER);
 *   config.addNode("server-3", "192.168.1.102", ConsulNodeRole.SERVER);
 *
 *   ConsulClusterAutomation automation = new ConsulClusterAutomation(sshDevice);
 *   automation.setupCluster(config);
 */
public class ConsulClusterAutomation {
    private static final Logger logger = Logger.getLogger(ConsulClusterAutomation.class.getName());

    private final SshDevice device;

    public ConsulClusterAutomation(SshDevice device) {
        this.device = device;
    }

    /**
     * Setup complete Consul cluster
     */
    public void setupCluster(ConsulClusterConfig config) throws Exception {
        logger.info("========================================");
        logger.info("Setting up Consul Cluster");
        logger.info("========================================");
        logger.info(config.toString());
        logger.info("");

        validateConfig(config);

        // Install Consul on all nodes
        for (ConsulNode node : config.getNodes()) {
            installConsul(node, config);
        }

        // Configure all nodes
        for (ConsulNode node : config.getNodes()) {
            configureNode(node, config);
        }

        // Start servers first
        for (ConsulNode node : config.getServerNodes()) {
            startConsul(node);
        }

        // Wait for cluster to form
        logger.info("Waiting for server cluster to form (15 seconds)...");
        Thread.sleep(15000);

        // Start clients
        for (ConsulNode node : config.getClientNodes()) {
            startConsul(node);
        }

        // Wait for clients to join
        logger.info("Waiting for clients to join (5 seconds)...");
        Thread.sleep(5000);

        // Verify cluster
        verifyCluster(config);

        logger.info("");
        logger.info("========================================");
        logger.info("✓ Consul Cluster Setup Complete");
        logger.info("========================================");
        logger.info("Consul UI: http://" + config.getServerNodes().get(0).getIpAddress() + ":8500/ui");
        logger.info("");
    }

    /**
     * Install Consul on a node
     */
    public void installConsul(ConsulNode node, ConsulClusterConfig config) throws Exception {
        logger.info("Installing Consul on " + node.getName() + " (" + node.getIpAddress() + ")...");

        String downloadUrl = String.format(
            "https://releases.hashicorp.com/consul/%s/consul_%s_linux_amd64.zip",
            config.getConsulVersion(), config.getConsulVersion()
        );

        String installScript = String.join("\n",
            "set -e",
            "cd /tmp",
            "wget -q " + downloadUrl,
            "unzip -o consul_" + config.getConsulVersion() + "_linux_amd64.zip",
            "sudo mv consul /usr/local/bin/",
            "sudo chmod +x /usr/local/bin/consul",
            "sudo mkdir -p /etc/consul.d /var/lib/consul",
            "sudo useradd -r -s /bin/false consul 2>/dev/null || true",
            "sudo chown -R consul:consul /var/lib/consul",
            "rm -f consul_" + config.getConsulVersion() + "_linux_amd64.zip",
            "echo 'Consul installed successfully'"
        );

        device.executeCommand("ssh -o StrictHostKeyChecking=no root@" + node.getIpAddress() + " '" + installScript + "'");
        logger.info("✓ Consul installed on " + node.getName());
    }

    /**
     * Configure Consul node
     */
    public void configureNode(ConsulNode node, ConsulClusterConfig config) throws Exception {
        logger.info("Configuring " + node.getName() + " as " + node.getRole() + "...");

        // Build retry_join list
        List<String> retryJoinList = config.getRetryJoinList();
        StringBuilder retryJoinHcl = new StringBuilder();
        for (String ip : retryJoinList) {
            retryJoinHcl.append("  \"").append(ip).append("\",\n");
        }

        String configHcl;
        if (node.getRole() == ConsulNodeRole.SERVER) {
            configHcl = String.join("\n",
                "datacenter = \"" + config.getDatacenter() + "\"",
                "data_dir = \"/var/lib/consul\"",
                "log_level = \"" + config.getLogLevel() + "\"",
                "",
                "server = true",
                "bootstrap_expect = " + config.getServerCount(),
                "",
                "bind_addr = \"" + node.getIpAddress() + "\"",
                "client_addr = \"0.0.0.0\"",
                "",
                "retry_join = [",
                retryJoinHcl.toString().replaceAll(",\n$", "\n"),
                "]",
                "",
                (config.isEnableUI() ? "ui_config {\n  enabled = true\n}\n" : ""),
                "performance {",
                "  raft_multiplier = 1",
                "}",
                "",
                (config.isEnableDNS() ? "" : "# DNS disabled - using HTTP API only"),
                "ports {",
                "  http = " + node.getPort(),
                (config.isEnableDNS() ? "  dns = " + config.getDnsPort() : "  dns = -1  # DNS disabled"),
                "}"
            );
        } else {
            configHcl = String.join("\n",
                "datacenter = \"" + config.getDatacenter() + "\"",
                "data_dir = \"/var/lib/consul\"",
                "log_level = \"" + config.getLogLevel() + "\"",
                "",
                "server = false",
                "",
                "bind_addr = \"" + node.getIpAddress() + "\"",
                "client_addr = \"0.0.0.0\"",
                "",
                "retry_join = [",
                retryJoinHcl.toString().replaceAll(",\n$", "\n"),
                "]",
                "",
                (config.isEnableDNS() ? "" : "# DNS disabled - using HTTP API only"),
                "ports {",
                "  http = " + node.getPort(),
                (config.isEnableDNS() ? "  dns = " + config.getDnsPort() : "  dns = -1  # DNS disabled"),
                "}"
            );
        }

        // Write config to node
        String escapedConfig = configHcl.replace("'", "'\\''");
        device.executeCommand("ssh -o StrictHostKeyChecking=no root@" + node.getIpAddress() +
            " 'echo '\"'" + escapedConfig + "'\"' | sudo tee /etc/consul.d/consul.hcl > /dev/null'");

        // Set ownership
        device.executeCommand("ssh -o StrictHostKeyChecking=no root@" + node.getIpAddress() +
            " 'sudo chown consul:consul /etc/consul.d/consul.hcl'");

        // Create systemd service
        createSystemdService(node);

        logger.info("✓ " + node.getName() + " configured");
    }

    /**
     * Create systemd service for Consul
     */
    private void createSystemdService(ConsulNode node) throws Exception {
        String serviceContent = String.join("\n",
            "[Unit]",
            "Description=Consul Agent",
            "Documentation=https://www.consul.io/",
            "After=network-online.target",
            "Wants=network-online.target",
            "",
            "[Service]",
            "Type=notify",
            "User=consul",
            "Group=consul",
            "ExecStart=/usr/local/bin/consul agent -config-dir=/etc/consul.d/",
            "ExecReload=/bin/kill -HUP $MAINPID",
            "KillMode=process",
            "KillSignal=SIGTERM",
            "Restart=on-failure",
            "LimitNOFILE=65536",
            "",
            "[Install]",
            "WantedBy=multi-user.target"
        );

        String escapedService = serviceContent.replace("'", "'\\''");
        device.executeCommand("ssh -o StrictHostKeyChecking=no root@" + node.getIpAddress() +
            " 'echo '\"'" + escapedService + "'\"' | sudo tee /etc/systemd/system/consul.service > /dev/null'");

        device.executeCommand("ssh -o StrictHostKeyChecking=no root@" + node.getIpAddress() +
            " 'sudo systemctl daemon-reload'");
    }

    /**
     * Start Consul on a node
     */
    public void startConsul(ConsulNode node) throws Exception {
        logger.info("Starting Consul on " + node.getName() + "...");

        device.executeCommand("ssh -o StrictHostKeyChecking=no root@" + node.getIpAddress() +
            " 'sudo systemctl enable consul && sudo systemctl start consul'");

        logger.info("✓ Consul started on " + node.getName());
    }

    /**
     * Verify cluster is healthy
     */
    public void verifyCluster(ConsulClusterConfig config) throws Exception {
        logger.info("\nVerifying cluster...");

        ConsulNode firstServer = config.getServerNodes().get(0);
        String members = device.executeCommand("ssh -o StrictHostKeyChecking=no root@" +
            firstServer.getIpAddress() + " 'consul members'");

        logger.info("\nCluster Members:");
        logger.info(members);

        String peers = device.executeCommand("ssh -o StrictHostKeyChecking=no root@" +
            firstServer.getIpAddress() + " 'consul operator raft list-peers'");

        logger.info("\nRaft Peers:");
        logger.info(peers);
    }

    /**
     * Validate configuration
     */
    private void validateConfig(ConsulClusterConfig config) throws Exception {
        if (config.getNodes().isEmpty()) {
            throw new Exception("No nodes configured");
        }

        int serverCount = config.getServerCount();
        if (serverCount == 0) {
            throw new Exception("At least one server node is required");
        }

        if (serverCount == 2) {
            logger.warning("⚠ Only 2 servers configured - not recommended for HA");
            logger.warning("  Recommend 1 server (dev) or 3+ servers (production)");
        }

        if (serverCount % 2 == 0 && serverCount > 2) {
            logger.warning("⚠ Even number of servers - can cause split-brain");
            logger.warning("  Recommend odd number: 1, 3, 5, or 7 servers");
        }
    }

    /**
     * Check if Consul is installed on a node
     */
    public boolean isConsulInstalled(ConsulNode node) throws Exception {
        try {
            String result = device.executeCommand("ssh -o StrictHostKeyChecking=no root@" +
                node.getIpAddress() + " 'which consul'");
            return result != null && !result.isEmpty();
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Get Consul version on a node
     */
    public String getConsulVersion(ConsulNode node) throws Exception {
        return device.executeCommand("ssh -o StrictHostKeyChecking=no root@" +
            node.getIpAddress() + " 'consul version | head -1'");
    }
}
