package com.telcobright.orchestrix.automation.example;

import com.telcobright.orchestrix.automation.cluster.perconadocker.PerconaMasterSlaveDeployment;
import com.telcobright.orchestrix.automation.config.PerconaConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Deploy Percona MySQL 5.7 Master-Slave cluster to KS Network
 *
 * Deployment topology:
 *   Master: 10.10.199.10:3306 (Node 1: 172.27.27.132)
 *   Slave:  10.10.198.10:3306 (Node 2: 172.27.27.136)
 *
 * Prerequisites:
 *   - WireGuard overlay network configured (10.9.10.0/24)
 *   - FRR BGP routing container subnets
 *   - Docker installed on both nodes
 *   - lxdbr0 bridge configured
 *
 * Run with:
 *   mvn compile exec:java -Dexec.mainClass="com.telcobright.orchestrix.automation.example.KsNetworkPerconaDeployment"
 *
 * @author TelcoBright Orchestrix Team
 * @since 2025-11-27
 */
public class KsNetworkPerconaDeployment {

    private static final Logger log = LoggerFactory.getLogger(KsNetworkPerconaDeployment.class);

    public static void main(String[] args) {
        log.info("╔════════════════════════════════════════════════════════════════╗");
        log.info("║    KS Network Percona MySQL Master-Slave Deployment           ║");
        log.info("╚════════════════════════════════════════════════════════════════╝");
        log.info("");

        try {
            // Load configuration
            String configDir = "deployments/ks_network/percona";
            log.info("Loading configuration from: {}", configDir);

            PerconaConfig config = new PerconaConfig(configDir);

            // Display configuration summary
            log.info("");
            log.info("Configuration:");
            log.info("  Cluster: {}", config.getClusterName());
            log.info("  Image: {}", config.getMysqlImage());
            log.info("  Master: {} (server-id={})",
                    config.getMaster().getMysqlIp(),
                    config.getMaster().getServerId());

            for (var slave : config.getSlaves()) {
                log.info("  Slave {}: {} (server-id={})",
                        slave.getSlaveIndex(),
                        slave.getMysqlIp(),
                        slave.getServerId());
            }

            log.info("  InnoDB Buffer Pool: {}", config.getInnodbBufferPoolSize());
            log.info("  GTID Mode: {}", config.isGtidMode() ? "ON" : "OFF");
            log.info("");

            // Deploy
            PerconaMasterSlaveDeployment deployment = new PerconaMasterSlaveDeployment(config);
            PerconaMasterSlaveDeployment.DeploymentResult result = deployment.deploy();

            // Report result
            log.info("");
            if (result.isSuccess()) {
                log.info("Deployment SUCCESSFUL");
                log.info("  Master: {}:3306", result.getMasterIp());
                log.info("  Root password: {}", config.getRootPassword());
            } else {
                log.error("Deployment FAILED: {}", result.getMessage());
                if (result.getException() != null) {
                    log.error("Exception: ", result.getException());
                }
                System.exit(1);
            }

        } catch (Exception e) {
            log.error("Fatal error during deployment", e);
            System.exit(1);
        }
    }
}
