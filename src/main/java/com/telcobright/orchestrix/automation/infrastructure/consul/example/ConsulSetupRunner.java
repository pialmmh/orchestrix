package com.telcobright.orchestrix.automation.infrastructure.consul.example;

import com.telcobright.orchestrix.automation.core.device.impl.LocalSshDevice;
import com.telcobright.orchestrix.automation.infrastructure.consul.ConsulClusterAutomation;
import com.telcobright.orchestrix.automation.infrastructure.consul.ConsulClusterAutomationFactory;
import com.telcobright.orchestrix.automation.infrastructure.consul.entity.ConsulClusterConfig;
import com.telcobright.orchestrix.automation.infrastructure.consul.entity.ConsulNodeRole;

import java.util.logging.Logger;

/**
 * Example: Setup minimal Consul cluster for small cloud
 *
 * Run via Maven:
 *   mvn exec:java \
 *     -Dexec.mainClass="com.telcobright.orchestrix.automation.infrastructure.consul.example.ConsulSetupRunner" \
 *     -Dexec.args="server1_ip server2_ip server3_ip [client1_ip client2_ip ...]"
 *
 * Example:
 *   mvn exec:java \
 *     -Dexec.mainClass="com.telcobright.orchestrix.automation.infrastructure.consul.example.ConsulSetupRunner" \
 *     -Dexec.args="192.168.1.100 192.168.1.101 192.168.1.102"
 */
public class ConsulSetupRunner {
    private static final Logger logger = Logger.getLogger(ConsulSetupRunner.class.getName());

    public static void main(String[] args) {
        if (args.length < 1) {
            System.err.println("Usage: ConsulSetupRunner <server_ips...> [client_ips...]");
            System.err.println("");
            System.err.println("Examples:");
            System.err.println("  # 3-server cluster (HA):");
            System.err.println("  mvn exec:java -Dexec.mainClass=\"...ConsulSetupRunner\" \\");
            System.err.println("    -Dexec.args=\"192.168.1.100 192.168.1.101 192.168.1.102\"");
            System.err.println("");
            System.err.println("  # 3 servers + 2 clients:");
            System.err.println("  mvn exec:java -Dexec.mainClass=\"...ConsulSetupRunner\" \\");
            System.err.println("    -Dexec.args=\"192.168.1.100 192.168.1.101 192.168.1.102 192.168.1.103:client 192.168.1.104:client\"");
            System.exit(1);
        }

        try {
            logger.info("=================================================================");
            logger.info("  Consul Cluster Setup - Minimal Configuration");
            logger.info("=================================================================\n");

            // Build configuration from command line arguments
            ConsulClusterConfig config = new ConsulClusterConfig();
            config.setDatacenter("dc1");
            config.setConsulVersion("1.17.0");
            config.setEnableUI(true);
            config.setEnableDNS(false);  // Minimal - no DNS

            // Parse arguments
            // Format: IP or IP:role
            // If no role specified, first 3 are servers, rest are clients
            int serverCount = 0;
            for (int i = 0; i < args.length; i++) {
                String arg = args[i];
                String[] parts = arg.split(":");
                String ip = parts[0];
                ConsulNodeRole role;

                if (parts.length > 1) {
                    // Explicit role specified
                    role = parts[1].equalsIgnoreCase("client") ?
                        ConsulNodeRole.CLIENT : ConsulNodeRole.SERVER;
                } else {
                    // Auto-assign: first 3 are servers
                    role = serverCount < 3 ? ConsulNodeRole.SERVER : ConsulNodeRole.CLIENT;
                }

                if (role == ConsulNodeRole.SERVER) {
                    serverCount++;
                }

                String nodeName = (role == ConsulNodeRole.SERVER ? "server-" : "client-") + (i + 1);
                config.addNode(nodeName, ip, role);
            }

            logger.info("Configuration:");
            logger.info("  Datacenter: " + config.getDatacenter());
            logger.info("  Consul Version: " + config.getConsulVersion());
            logger.info("  Servers: " + config.getServerCount());
            logger.info("  Clients: " + config.getClientNodes().size());
            logger.info("  DNS: " + (config.isEnableDNS() ? "Enabled" : "Disabled (IP-based only)"));
            logger.info("  UI: " + (config.isEnableUI() ? "Enabled" : "Disabled"));
            logger.info("");

            // Create SSH device (local)
            LocalSshDevice device = new LocalSshDevice("localhost", 22, System.getProperty("user.name"));
            device.connect();

            try {
                // Create automation
                ConsulClusterAutomation automation = ConsulClusterAutomationFactory.create(device);

                // Setup cluster
                automation.setupCluster(config);

                logger.info("\n=================================================================");
                logger.info("  Consul Cluster Setup Complete!");
                logger.info("=================================================================\n");

                logger.info("Next Steps:");
                logger.info("1. Access Consul UI:");
                logger.info("   http://" + config.getServerNodes().get(0).getIpAddress() + ":8500/ui");
                logger.info("");
                logger.info("2. Configure your services:");
                logger.info("   lxc config set <container> environment.CONSUL_URL \\");
                logger.info("     \"http://" + config.getServerNodes().get(0).getIpAddress() + ":8500\"");
                logger.info("");
                logger.info("3. Verify cluster:");
                logger.info("   ssh root@" + config.getServerNodes().get(0).getIpAddress() + " 'consul members'");
                logger.info("");

            } finally {
                device.disconnect();
            }

        } catch (Exception e) {
            logger.severe("Setup failed: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}
