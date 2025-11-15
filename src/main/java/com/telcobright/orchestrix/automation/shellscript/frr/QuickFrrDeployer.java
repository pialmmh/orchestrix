package com.telcobright.orchestrix.automation.shellscript.frr;

import com.jcraft.jsch.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Quick FRR Deployer for Link3 Cluster
 * One-click deployment of FRR BGP mesh to Link3 servers
 */
public class QuickFrrDeployer {

    private static final Logger log = LoggerFactory.getLogger(QuickFrrDeployer.class);

    // Link3 cluster configuration
    private static final NodeConfig[] LINK3_NODES = {
        new NodeConfig("node1", "SMSAppDBmaster", "123.200.0.50", 8210, "tbsms", "TB@l38800",
                "123.200.0.50", 65196, "10.10.196.0/24"),
        new NodeConfig("node2", "spark", "123.200.0.117", 8210, "tbsms", "TB@l38800",
                "123.200.0.117", 65195, "10.10.195.0/24"),
        new NodeConfig("node3", "SMSApplication", "123.200.0.51", 8210, "tbsms", "TB@l38800",
                "123.200.0.51", 65194, "10.10.194.0/24")
    };

    public static void main(String[] args) {
        log.info("========================================");
        log.info("FRR BGP Deployment - Link3 Cluster");
        log.info("========================================");
        log.info("");

        QuickFrrDeployer deployer = new QuickFrrDeployer();

        try {
            // Deploy to all nodes
            for (NodeConfig node : LINK3_NODES) {
                log.info("Deploying to {} ({})...", node.hostname, node.host);
                deployer.deployToNode(node);
                log.info("");
            }

            log.info("========================================");
            log.info("Deployment Complete!");
            log.info("========================================");
            log.info("");
            log.info("Verify BGP peering:");
            for (NodeConfig node : LINK3_NODES) {
                log.info("  ssh -p {} {}@{} \"sudo vtysh -c 'show ip bgp summary'\"",
                        node.port, node.user, node.host);
            }

        } catch (Exception e) {
            log.error("Deployment failed", e);
            System.exit(1);
        }
    }

    private void deployToNode(NodeConfig node) throws Exception {
        // Step 1: Install FRR
        log.info("[{}] Installing FRR...", node.name);
        String installScript = generateInstallScript(node);
        executeScript(node, installScript, "frr-install.sh");

        // Step 2: Configure BGP
        log.info("[{}] Configuring BGP...", node.name);
        String configScript = generateConfigScript(node);
        executeScript(node, configScript, "frr-configure.sh");

        // Step 3: Verify
        log.info("[{}] Verifying installation...", node.name);
        String status = executeCommand(node, "sudo systemctl is-active frr");
        if (status.trim().equals("active")) {
            log.info("[{}] ✓ FRR is running", node.name);
        } else {
            log.error("[{}] ✗ FRR is not running", node.name);
        }
    }

    private String generateInstallScript(NodeConfig node) throws IOException {
        String template = Files.readString(Paths.get("images/shell-automations/frr/v1/templates/frr-install.sh.template"));
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);

        return template
            .replace("{{TIMESTAMP}}", timestamp)
            .replace("{{HOSTNAME}}", node.hostname)
            .replace("{{HOST_IP}}", node.host);
    }

    private String generateConfigScript(NodeConfig node) throws IOException {
        String template = Files.readString(Paths.get("images/shell-automations/frr/v1/templates/frr-configure.sh.template"));
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);

        // Generate BGP neighbors configuration
        StringBuilder neighborsConfig = new StringBuilder();
        for (NodeConfig peer : LINK3_NODES) {
            if (!peer.name.equals(node.name)) {
                neighborsConfig.append(" neighbor ").append(peer.routerId).append(" peer-group BGP_PEERS\n");
                neighborsConfig.append(" neighbor ").append(peer.routerId).append(" remote-as ").append(peer.bgpAsn).append("\n");
            }
        }

        // Generate network announcements
        String networkConfig = " network " + node.subnet;

        return template
            .replace("{{TIMESTAMP}}", timestamp)
            .replace("{{HOSTNAME}}", node.hostname)
            .replace("{{HOST_IP}}", node.host)
            .replace("{{ROUTER_ID}}", node.routerId)
            .replace("{{BGP_ASN}}", String.valueOf(node.bgpAsn))
            .replace("{{BGP_NEIGHBOR_CONFIG}}", neighborsConfig.toString())
            .replace("{{NETWORK_CONFIG}}", networkConfig);
    }

    private void executeScript(NodeConfig node, String scriptContent, String scriptName) throws Exception {
        JSch jsch = new JSch();
        Session session = null;

        try {
            // Create SSH session
            session = jsch.getSession(node.user, node.host, node.port);
            session.setPassword(node.password);
            session.setConfig("StrictHostKeyChecking", "no");
            session.connect();

            // Upload script
            ChannelSftp sftpChannel = (ChannelSftp) session.openChannel("sftp");
            sftpChannel.connect();

            String remotePath = "/tmp/" + scriptName;
            sftpChannel.put(new ByteArrayInputStream(scriptContent.getBytes()), remotePath);
            sftpChannel.disconnect();

            // Execute script
            ChannelExec execChannel = (ChannelExec) session.openChannel("exec");
            execChannel.setCommand("chmod +x " + remotePath + " && sudo " + remotePath);

            InputStream in = execChannel.getInputStream();
            execChannel.connect();

            // Read output
            BufferedReader reader = new BufferedReader(new InputStreamReader(in));
            String line;
            while ((line = reader.readLine()) != null) {
                log.info("[{}] {}", node.name, line);
            }

            execChannel.disconnect();

        } finally {
            if (session != null) {
                session.disconnect();
            }
        }
    }

    private String executeCommand(NodeConfig node, String command) throws Exception {
        JSch jsch = new JSch();
        Session session = null;

        try {
            session = jsch.getSession(node.user, node.host, node.port);
            session.setPassword(node.password);
            session.setConfig("StrictHostKeyChecking", "no");
            session.connect();

            ChannelExec channel = (ChannelExec) session.openChannel("exec");
            channel.setCommand(command);

            InputStream in = channel.getInputStream();
            channel.connect();

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            byte[] buffer = new byte[1024];
            int read;
            while ((read = in.read(buffer)) != -1) {
                baos.write(buffer, 0, read);
            }

            channel.disconnect();
            return baos.toString();

        } finally {
            if (session != null) {
                session.disconnect();
            }
        }
    }

    static class NodeConfig {
        final String name;
        final String hostname;
        final String host;
        final int port;
        final String user;
        final String password;
        final String routerId;
        final int bgpAsn;
        final String subnet;

        NodeConfig(String name, String hostname, String host, int port, String user, String password,
                   String routerId, int bgpAsn, String subnet) {
            this.name = name;
            this.hostname = hostname;
            this.host = host;
            this.port = port;
            this.user = user;
            this.password = password;
            this.routerId = routerId;
            this.bgpAsn = bgpAsn;
            this.subnet = subnet;
        }
    }
}
