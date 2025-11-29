package com.telcobright.orchestrix.automation.example;

import com.telcobright.orchestrix.automation.config.OverlayConfig;
import com.telcobright.orchestrix.automation.overlay.OverlayNetworkDeployment;
import com.telcobright.orchestrix.automation.overlay.OverlayNetworkDeployment.DeploymentResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Restore BTCL Overlay Network after Reboot
 *
 * Re-deploys WireGuard + FRR BGP overlay network to all 3 BTCL nodes.
 */
public class RestoreBtclOverlay {
    private static final Logger log = LoggerFactory.getLogger(RestoreBtclOverlay.class);

    public static void main(String[] args) {
        log.info("╔════════════════════════════════════════════════════════════════╗");
        log.info("║       Restore BTCL Overlay Network (Post-Reboot)             ║");
        log.info("╚════════════════════════════════════════════════════════════════╝");

        String configPath = "deployments/btcl_network";

        try {
            // Load overlay configuration
            OverlayConfig config = new OverlayConfig(configPath);

            // Deploy overlay network
            OverlayNetworkDeployment deployment = new OverlayNetworkDeployment(config);
            DeploymentResult result = deployment.deploy();

            if (result.isSuccess()) {
                log.info("\n╔════════════════════════════════════════════════════════════════╗");
                log.info("║       ✓ BTCL Overlay Network Restored Successfully!          ║");
                log.info("╚════════════════════════════════════════════════════════════════╝");
            } else {
                log.error("\n╔════════════════════════════════════════════════════════════════╗");
                log.error("║       ✗ BTCL Overlay Network Restoration Failed!             ║");
                log.error("╚════════════════════════════════════════════════════════════════╝");
                log.error("\nError: {}", result.getMessage());
                if (result.getException() != null) {
                    log.error("Exception:", result.getException());
                }
                System.exit(1);
            }

        } catch (Exception e) {
            log.error("\n╔════════════════════════════════════════════════════════════════╗");
            log.error("║       ✗ Deployment Failed with Exception!                    ║");
            log.error("╚════════════════════════════════════════════════════════════════╝");
            log.error("\nError: {}", e.getMessage(), e);
            System.exit(1);
        }
    }
}
