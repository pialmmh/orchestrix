package com.telcobright.orchestrix.automation.routing.frr;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * CLI Runner for FRR Router Deployment
 *
 * Usage examples:
 *
 * Deploy to single node:
 *   java FrrDeploymentRunner \
 *     --orchestrix-path /home/user/orchestrix \
 *     --host 10.255.246.173 \
 *     --port 15605 \
 *     --user bdcom \
 *     --config /path/to/node1-config.conf
 *
 * Deploy to BDCOM nodes:
 *   java FrrDeploymentRunner \
 *     --orchestrix-path /home/user/orchestrix \
 *     --deploy-bdcom-nodes
 */
public class FrrDeploymentRunner {

    private static final Logger log = LoggerFactory.getLogger(FrrDeploymentRunner.class);

    // BDCOM node configurations
    private static final List<NodeConfig> BDCOM_NODES = Arrays.asList(
        new NodeConfig("node1", "10.255.246.173", 15605, "bdcom"),
        new NodeConfig("node2", "10.255.246.174", 15605, "bdcom"),
        new NodeConfig("node3", "10.255.246.175", 15605, "bdcom")
    );

    public static void main(String[] args) {
        try {
            Map<String, String> params = parseArgs(args);

            if (!params.containsKey("orchestrix-path")) {
                printUsage();
                System.exit(1);
            }

            String orchestrixPath = params.get("orchestrix-path");
            FrrRouterDeployment deployment = new FrrRouterDeployment(orchestrixPath);

            if (params.containsKey("deploy-bdcom-nodes")) {
                // Deploy to all BDCOM nodes
                deployToBdcomNodes(deployment, orchestrixPath);
            } else if (params.containsKey("host") && params.containsKey("port") &&
                       params.containsKey("user") && params.containsKey("config")) {
                // Deploy to single node
                deployToSingleNode(deployment, params);
            } else {
                printUsage();
                System.exit(1);
            }

        } catch (Exception e) {
            log.error("Deployment failed", e);
            System.exit(1);
        }
    }

    /**
     * Deploy to single node
     */
    private static void deployToSingleNode(FrrRouterDeployment deployment, Map<String, String> params) {
        String host = params.get("host");
        int port = Integer.parseInt(params.get("port"));
        String user = params.get("user");
        String configPath = params.get("config");

        FrrDeploymentResult result = deployment.deployToNode(host, port, user, configPath);

        if (result.isSuccess()) {
            log.info("✓ Deployment successful");
            System.exit(0);
        } else {
            log.error("✗ Deployment failed: {}", result.getMessage());
            System.exit(1);
        }
    }

    /**
     * Deploy to all BDCOM nodes
     */
    private static void deployToBdcomNodes(FrrRouterDeployment deployment, String orchestrixPath) {
        log.info("========================================");
        log.info("Deploying FRR Routers to BDCOM Nodes");
        log.info("========================================");
        log.info("");

        List<FrrDeploymentResult> results = new ArrayList<>();
        List<String> failedNodes = new ArrayList<>();

        for (NodeConfig node : BDCOM_NODES) {
            String configPath = orchestrixPath + "/images/containers/lxc/frr-router/deployment-configs/" +
                    node.getName() + "-config.conf";

            log.info("Deploying to {} ({})...", node.getName(), node.getHost());
            FrrDeploymentResult result = deployment.deployToNode(
                    node.getHost(),
                    node.getPort(),
                    node.getUser(),
                    configPath
            );

            results.add(result);

            if (!result.isSuccess()) {
                failedNodes.add(node.getName());
            }

            log.info("");
        }

        // Print summary
        log.info("========================================");
        log.info("Deployment Summary");
        log.info("========================================");

        if (failedNodes.isEmpty()) {
            log.info("✓ All nodes deployed successfully!");
            log.info("");
            log.info("Next steps:");
            log.info("1. Verify BGP peering:");
            for (NodeConfig node : BDCOM_NODES) {
                log.info("   ssh -p {} {}@{}", node.getPort(), node.getUser(), node.getHost());
                log.info("   sudo lxc exec frr-router-{} -- vtysh -c 'show ip bgp summary'", node.getName());
            }
            log.info("");
            log.info("2. Check announced routes:");
            log.info("   sudo lxc exec frr-router-node1 -- vtysh -c 'show ip bgp'");
            log.info("");
            System.exit(0);
        } else {
            log.error("⚠ Deployment failed for: {}", failedNodes);
            log.info("");
            for (FrrDeploymentResult result : results) {
                if (!result.isSuccess()) {
                    log.error("  - {}: {}", result.getHost(), result.getMessage());
                }
            }
            log.info("");
            System.exit(1);
        }
    }

    /**
     * Parse command line arguments
     */
    private static Map<String, String> parseArgs(String[] args) {
        Map<String, String> params = new HashMap<>();
        for (int i = 0; i < args.length; i++) {
            if (args[i].startsWith("--")) {
                String key = args[i].substring(2);
                if (i + 1 < args.length && !args[i + 1].startsWith("--")) {
                    params.put(key, args[i + 1]);
                    i++;
                } else {
                    params.put(key, "true");
                }
            }
        }
        return params;
    }

    /**
     * Print usage information
     */
    private static void printUsage() {
        System.out.println("FRR Router Deployment");
        System.out.println("");
        System.out.println("Usage:");
        System.out.println("  Deploy to single node:");
        System.out.println("    java FrrDeploymentRunner \\");
        System.out.println("      --orchestrix-path /home/user/orchestrix \\");
        System.out.println("      --host 10.255.246.173 \\");
        System.out.println("      --port 15605 \\");
        System.out.println("      --user bdcom \\");
        System.out.println("      --config /path/to/config.conf");
        System.out.println("");
        System.out.println("  Deploy to BDCOM nodes:");
        System.out.println("    java FrrDeploymentRunner \\");
        System.out.println("      --orchestrix-path /home/user/orchestrix \\");
        System.out.println("      --deploy-bdcom-nodes");
        System.out.println("");
    }

    /**
     * Node configuration holder
     */
    private static class NodeConfig {
        private final String name;
        private final String host;
        private final int port;
        private final String user;

        public NodeConfig(String name, String host, int port, String user) {
            this.name = name;
            this.host = host;
            this.port = port;
            this.user = user;
        }

        public String getName() {
            return name;
        }

        public String getHost() {
            return host;
        }

        public int getPort() {
            return port;
        }

        public String getUser() {
            return user;
        }
    }
}
