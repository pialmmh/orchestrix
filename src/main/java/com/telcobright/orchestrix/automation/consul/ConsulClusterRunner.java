package com.telcobright.orchestrix.automation.consul;

import com.telcobright.orchestrix.automation.core.device.LocalSshDevice;
import java.util.logging.Logger;

/**
 * Runner for deploying Consul cluster
 *
 * Usage:
 *   mvn exec:java \
 *     -Dexec.mainClass="com.telcobright.orchestrix.automation.consul.ConsulClusterRunner"
 *
 * Or with custom node count:
 *   mvn exec:java \
 *     -Dexec.mainClass="com.telcobright.orchestrix.automation.consul.ConsulClusterRunner" \
 *     -Dexec.args="5"
 */
public class ConsulClusterRunner {

    private static final Logger logger = Logger.getLogger(ConsulClusterRunner.class.getName());

    public static void main(String[] args) {
        try {
            // Parse arguments
            int nodeCount = 3; // Default
            if (args.length > 0) {
                try {
                    nodeCount = Integer.parseInt(args[0]);
                    if (nodeCount < 1 || nodeCount > 7) {
                        logger.warning("Node count should be between 1 and 7. Using 3.");
                        nodeCount = 3;
                    }
                } catch (NumberFormatException e) {
                    logger.warning("Invalid node count. Using default: 3");
                }
            }

            logger.info("=================================================================");
            logger.info("  Consul Cluster Deployment");
            logger.info("=================================================================");
            logger.info("");

            // Connect to local device
            LocalSshDevice device = new LocalSshDevice();
            device.connect();

            try {
                // Create configuration
                ConsulClusterConfig config = new ConsulClusterConfig(nodeCount);
                config.setDatacenter("dc1");

                // Create builder and deploy
                ConsulClusterBuilder builder = new ConsulClusterBuilder(device, config);
                builder.buildCluster();

                logger.info("=================================================================");
                logger.info("  Deployment Complete!");
                logger.info("=================================================================");
                logger.info("");
                logger.info("Cluster Details:");
                logger.info("  Nodes: " + nodeCount);
                logger.info("  Containers: consul-node-1 to consul-node-" + nodeCount);
                logger.info("");
                logger.info("Commands:");
                logger.info("  View members: lxc exec consul-node-1 -- consul members");
                logger.info("  View logs: lxc exec consul-node-1 -- tail -f /var/log/consul.log");
                logger.info("  Access UI: http://[consul-node-1-ip]:8500");
                logger.info("");
                logger.info("To stop cluster:");
                logger.info("  for i in {1.." + nodeCount + "}; do lxc stop consul-node-$i --force; done");
                logger.info("");
                logger.info("To delete cluster:");
                logger.info("  for i in {1.." + nodeCount + "}; do lxc delete consul-node-$i --force; done");
                logger.info("");

            } finally {
                device.disconnect();
            }

        } catch (Exception e) {
            logger.severe("Consul cluster deployment failed: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}