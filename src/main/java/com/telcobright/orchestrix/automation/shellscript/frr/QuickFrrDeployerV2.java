package com.telcobright.orchestrix.automation.shellscript.frr;

import com.jcraft.jsch.*;
import com.telcobright.orchestrix.automation.routing.NetworkingGuidelineHelper;
import com.telcobright.orchestrix.automation.routing.NetworkingGuidelineHelper.NodeNetworkConfig;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Quick FRR BGP deployment automation - Version 2
 *
 * Updated to follow TelcoBright Container Networking Guideline:
 * - Uses NetworkingGuidelineHelper for IP calculations
 * - Overlay IPs: 10.9.9.N
 * - BGP AS: 65(200-N)
 * - Router ID: Uses overlay IP (not management IP)
 * - Includes critical ebgp-multihop 2 setting
 *
 * Based on successful BDCOM deployment experience.
 *
 * @author TelcoBright Orchestrix Team
 * @since 2025-11-10
 */
public class QuickFrrDeployerV2 {

    /**
     * Enhanced node configuration with overlay network support
     */
    public static class NodeConfig {
        public final String name;
        public final String hostname;
        public final String managementIp;
        public final int sshPort;
        public final String sshUser;
        public final String sshPassword;
        public final int hostNumber;

        // Calculated fields from guideline
        public final String overlayIp;
        public final String containerSubnet;
        public final int bgpAsn;

        public NodeConfig(String name, String hostname, String managementIp,
                         int sshPort, String sshUser, String sshPassword,
                         int hostNumber) {
            this.name = name;
            this.hostname = hostname;
            this.managementIp = managementIp;
            this.sshPort = sshPort;
            this.sshUser = sshUser;
            this.sshPassword = sshPassword;
            this.hostNumber = hostNumber;

            // Calculate network parameters per guideline
            this.overlayIp = NetworkingGuidelineHelper.calculateOverlayIp(hostNumber);
            this.containerSubnet = NetworkingGuidelineHelper.calculateContainerSubnet(hostNumber);
            this.bgpAsn = NetworkingGuidelineHelper.calculateBgpAsNumber(hostNumber);
        }

        @Override
        public String toString() {
            return String.format("%s [%s] - Overlay: %s, AS: %d, Subnet: %s",
                name, managementIp, overlayIp, bgpAsn, containerSubnet);
        }
    }

    // Example configuration - LINK3 cluster using guideline-compliant values
    private static final NodeConfig[] LINK3_NODES = {
        new NodeConfig("node1", "SMSAppDBmaster", "123.200.0.50", 8210, "tbsms", "TB@l38800", 1),
        new NodeConfig("node2", "spark", "123.200.0.117", 8210, "tbsms", "TB@l38800", 2),
        new NodeConfig("node3", "SMSApplication", "123.200.0.51", 8210, "tbsms", "TB@l38800", 3)
    };

    public static void main(String[] args) {
        System.out.println("╔════════════════════════════════════════════════════════════════╗");
        System.out.println("║        FRR BGP Deployment - Guideline Compliant v2            ║");
        System.out.println("╚════════════════════════════════════════════════════════════════╝");
        System.out.println();

        // Validate configurations
        System.out.println("Cluster Configuration:");
        for (NodeConfig node : LINK3_NODES) {
            System.out.println("  " + node);
        }
        System.out.println();

        QuickFrrDeployerV2 deployer = new QuickFrrDeployerV2();

        try {
            // Step 1: Install FRR on all nodes
            System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            System.out.println("📍 Step 1: Installing FRR on all nodes");
            System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            for (NodeConfig node : LINK3_NODES) {
                System.out.println("Installing FRR on " + node.name + "...");
                deployer.installFrr(node);
                System.out.println("✓ " + node.name + " installation complete\n");
            }

            // Step 2: Configure BGP on all nodes
            System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            System.out.println("📍 Step 2: Configuring BGP on all nodes");
            System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            for (NodeConfig node : LINK3_NODES) {
                System.out.println("Configuring BGP on " + node.name + "...");
                deployer.configureBgp(node);
                System.out.println("✓ " + node.name + " BGP configured\n");
            }

            // Step 3: Verify BGP sessions
            Thread.sleep(5000); // Wait for BGP sessions to establish
            System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            System.out.println("📍 Step 3: Verifying BGP sessions");
            System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            for (NodeConfig node : LINK3_NODES) {
                System.out.println("\n" + node.name + " BGP Status:");
                deployer.verifyBgp(node);
            }

            System.out.println();
            System.out.println("╔════════════════════════════════════════════════════════════════╗");
            System.out.println("║              FRR BGP Deployment Complete                      ║");
            System.out.println("╚════════════════════════════════════════════════════════════════╝");
            System.out.println();
            System.out.println("✓ All nodes configured with BGP using networking guideline");
            System.out.println("✓ Overlay IPs: 10.9.9.1, 10.9.9.2, 10.9.9.3");
            System.out.println("✓ BGP AS: 65199, 65198, 65197");
            System.out.println();

        } catch (Exception e) {
            System.err.println("❌ Deployment failed: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void installFrr(NodeConfig node) throws Exception {
        String script = generateInstallScript(node);
        executeScript(node, script, "frr-install.sh");
    }

    private void configureBgp(NodeConfig node) throws Exception {
        String script = generateConfigScript(node);
        executeScript(node, script, "frr-configure.sh");
    }

    private void verifyBgp(NodeConfig node) throws Exception {
        executeCommand(node, "sudo vtysh -c 'show ip bgp summary'");
    }

    private String generateInstallScript(NodeConfig node) throws IOException {
        String template = Files.readString(
            Paths.get("images/shell-automations/frr/v1/templates/frr-install.sh.template")
        );
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);

        return template
            .replace("{{TIMESTAMP}}", timestamp)
            .replace("{{HOSTNAME}}", node.hostname)
            .replace("{{HOST_IP}}", node.managementIp);
    }

    private String generateConfigScript(NodeConfig node) throws IOException {
        String template = Files.readString(
            Paths.get("images/shell-automations/frr/v1/templates/frr-configure.sh.template")
        );
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);

        // Generate BGP neighbors configuration
        StringBuilder neighborsConfig = new StringBuilder();
        for (NodeConfig peer : LINK3_NODES) {
            if (!peer.name.equals(node.name)) {
                // Use overlay IP as neighbor address, not management IP
                neighborsConfig.append(" neighbor ").append(peer.overlayIp)
                    .append(" peer-group BGP_PEERS\n");
                neighborsConfig.append(" neighbor ").append(peer.overlayIp)
                    .append(" remote-as ").append(peer.bgpAsn).append("\n");
            }
        }

        // Generate network announcements
        String networkConfig = " network " + node.containerSubnet;

        // Use overlay IP as router ID (per guideline)
        return template
            .replace("{{TIMESTAMP}}", timestamp)
            .replace("{{HOSTNAME}}", node.hostname)
            .replace("{{HOST_IP}}", node.managementIp)
            .replace("{{ROUTER_ID}}", node.overlayIp)  // Changed from managementIp
            .replace("{{BGP_ASN}}", String.valueOf(node.bgpAsn))
            .replace("{{BGP_NEIGHBOR_CONFIG}}", neighborsConfig.toString())
            .replace("{{NETWORK_CONFIG}}", networkConfig);
    }

    private void executeScript(NodeConfig node, String scriptContent, String scriptName) throws Exception {
        JSch jsch = new JSch();
        Session session = null;

        try {
            // Create SSH session
            session = jsch.getSession(node.sshUser, node.managementIp, node.sshPort);
            session.setPassword(node.sshPassword);
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
            execChannel.setInputStream(null);
            execChannel.setErrStream(System.err);

            java.io.InputStream in = execChannel.getInputStream();
            execChannel.connect();

            byte[] tmp = new byte[1024];
            while (true) {
                while (in.available() > 0) {
                    int i = in.read(tmp, 0, 1024);
                    if (i < 0) break;
                    System.out.print(new String(tmp, 0, i));
                }
                if (execChannel.isClosed()) {
                    if (in.available() > 0) continue;
                    if (execChannel.getExitStatus() != 0) {
                        throw new RuntimeException("Script execution failed with exit code: " +
                            execChannel.getExitStatus());
                    }
                    break;
                }
                Thread.sleep(100);
            }
            execChannel.disconnect();

        } finally {
            if (session != null) {
                session.disconnect();
            }
        }
    }

    private void executeCommand(NodeConfig node, String command) throws Exception {
        JSch jsch = new JSch();
        Session session = null;

        try {
            session = jsch.getSession(node.sshUser, node.managementIp, node.sshPort);
            session.setPassword(node.sshPassword);
            session.setConfig("StrictHostKeyChecking", "no");
            session.connect();

            ChannelExec execChannel = (ChannelExec) session.openChannel("exec");
            execChannel.setCommand(command);
            execChannel.setInputStream(null);
            execChannel.setErrStream(System.err);

            java.io.InputStream in = execChannel.getInputStream();
            execChannel.connect();

            byte[] tmp = new byte[1024];
            while (true) {
                while (in.available() > 0) {
                    int i = in.read(tmp, 0, 1024);
                    if (i < 0) break;
                    System.out.print(new String(tmp, 0, i));
                }
                if (execChannel.isClosed()) {
                    if (in.available() > 0) continue;
                    break;
                }
                Thread.sleep(100);
            }
            execChannel.disconnect();

        } finally {
            if (session != null) {
                session.disconnect();
            }
        }
    }
}
