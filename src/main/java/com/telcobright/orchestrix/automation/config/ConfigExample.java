package com.telcobright.orchestrix.automation.config;

/**
 * Example usage of CommonConfig and FrrConfig
 *
 * Demonstrates the reusable configuration architecture:
 * - CommonConfig: Automation-agnostic base class
 * - FrrConfig extends CommonConfig: FRR/BGP-specific
 * - Future: KafkaConfig, MySqlConfig, etc. extend CommonConfig
 *
 * @author TelcoBright Orchestrix Team
 * @since 2025-11-16
 */
public class ConfigExample {

    public static void main(String[] args) {
        try {
            exampleCommonConfig();
            System.out.println();
            exampleFrrConfig();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Example 1: Load common config only
     * Use for automations that don't need service-specific config
     */
    private static void exampleCommonConfig() throws Exception {
        System.out.println("╔════════════════════════════════════════════════════════════════╗");
        System.out.println("║           Example 1: CommonConfig (Generic)                  ║");
        System.out.println("╚════════════════════════════════════════════════════════════════╝");

        // Load common config
        CommonConfig config = new CommonConfig("deployments/netlab");

        // Access generic parameters (works for ANY automation)
        System.out.println("Tenant: " + config.getTenantName());
        System.out.println("Environment: " + config.getEnvironment());
        System.out.println();

        System.out.println("SSH Connection:");
        System.out.println("  User: " + config.getSshUser());
        System.out.println("  Password: " + (config.getSshPassword() != null ? "***" : "not set"));
        System.out.println("  Port: " + config.getSshPort());
        System.out.println();

        System.out.println("Nodes:");
        System.out.println("  Node 1: " + config.getNode1Hostname() + " @ " + config.getNode1Ip());
        System.out.println("  Node 2: " + config.getNode2Hostname() + " @ " + config.getNode2Ip());
        System.out.println("  Node 3: " + config.getNode3Hostname() + " @ " + config.getNode3Ip());
        System.out.println();

        System.out.println("Network:");
        System.out.println("  Overlay: " + config.getOverlayNetwork());
        System.out.println("  Container Supernet: " + config.getContainerSupernet());
        System.out.println("  Management: " + config.getManagementNetwork());
    }

    /**
     * Example 2: Load FRR config (extends CommonConfig)
     * Gets both common params + FRR-specific params
     */
    private static void exampleFrrConfig() throws Exception {
        System.out.println("╔════════════════════════════════════════════════════════════════╗");
        System.out.println("║        Example 2: FrrConfig (extends CommonConfig)            ║");
        System.out.println("╚════════════════════════════════════════════════════════════════╝");

        // Load FRR config (automatically loads common.conf + frr/node1-config.conf)
        FrrConfig config = new FrrConfig("deployments/netlab", "frr/node1-config.conf");

        // Access common parameters (inherited from CommonConfig)
        System.out.println("Common Parameters:");
        System.out.println("  Tenant: " + config.getTenantName());
        System.out.println("  SSH User: " + config.getSshUser());
        System.out.println("  Node 1 IP: " + config.getNode1Ip());
        System.out.println("  Overlay Network: " + config.getOverlayNetwork());
        System.out.println();

        // Access FRR-specific parameters
        System.out.println("FRR-Specific Parameters:");
        System.out.println("  Container: " + config.getContainerName());
        System.out.println("  Base Image: " + config.getBaseImage());
        System.out.println("  BGP ASN: " + config.getBgpAsn());
        System.out.println("  BGP Neighbors: " + config.getBgpNeighbors());
        System.out.println("  BGP Networks: " + config.getBgpNetworks());
        System.out.println("  BGP Timers: " + config.getBgpTimers());
        System.out.println();

        System.out.println("Full Config: " + config);
    }

    /**
     * Example 3: Future - Kafka config (would extend CommonConfig)
     */
    @SuppressWarnings("unused")
    private static void exampleKafkaConfig() throws Exception {
        // Future implementation
        // KafkaConfig config = new KafkaConfig("deployments/netlab", "kafka/cluster-config.conf");
        //
        // // Access common parameters (inherited)
        // String sshUser = config.getSshUser();
        // String node1Ip = config.getNode1Ip();
        //
        // // Access Kafka-specific parameters
        // String brokerId = config.getBrokerId();
        // String topics = config.getKafkaTopics();
        // int retentionHours = config.getRetentionHours();
    }
}
