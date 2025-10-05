package com.telcobright.orchestrix.automation.api.container.lxc.app.goid.example;

import com.telcobright.orchestrix.automation.api.container.lxc.app.goid.core.GoIdContainerBuilder;
import com.telcobright.orchestrix.automation.core.device.LocalSshDevice;

import java.util.logging.Logger;

/**
 * Main runner for Go ID Container build automation
 *
 * <p>This class establishes SSH connection and delegates build to GoIdContainerBuilder.
 *
 * <p><b>SSH Requirement:</b> Build MUST use SSH connection (no ProcessBuilder fallback).
 * This ensures commands execute properly without hanging.
 *
 * <p><b>Usage:</b>
 * <pre>
 * mvn exec:java \
 *   -Dexec.mainClass=com.telcobright.orchestrix.automation.api.container.lxc.app.goid.example.GoIdBuildRunner \
 *   -Dexec.args="/path/to/build.conf"
 * </pre>
 */
public class GoIdBuildRunner {

    private static final Logger logger = Logger.getLogger(GoIdBuildRunner.class.getName());

    public static void main(String[] args) {
        if (args.length < 1) {
            System.err.println("Usage: GoIdBuildRunner <config-file>");
            System.err.println("Example: GoIdBuildRunner /path/to/build.conf");
            System.exit(1);
        }

        String configFile = args[0];
        LocalSshDevice device = null;

        try {
            logger.info("=================================================================");
            logger.info("  Go ID Container Build - SSH-Based Automation");
            logger.info("=================================================================");
            logger.info("");

            // Create SSH device for localhost - MUST use SSH, no ProcessBuilder fallback
            device = new LocalSshDevice();

            logger.info("Connecting to localhost via SSH...");
            logger.info("  Host: localhost:22");
            logger.info("  User: " + System.getProperty("user.name"));
            logger.info("");

            // Connect via SSH - MUST succeed (no fallback to ProcessBuilder)
            boolean connected = device.connect();

            // Check if ACTUAL SSH session exists (not ProcessBuilder fallback)
            if (!connected || !device.isConnected()) {
                throw new Exception(
                    "Failed to connect to localhost via SSH.\n" +
                    "\n" +
                    "SSH connection is REQUIRED for container automation (no ProcessBuilder fallback).\n" +
                    "\n" +
                    "LocalSshDevice fell back to ProcessBuilder - this is NOT allowed.\n" +
                    "\n" +
                    "Please ensure:\n" +
                    "1. SSH server is running: sudo systemctl start ssh\n" +
                    "2. SSH key exists: ls -la ~/.ssh/id_rsa.pub\n" +
                    "3. SSH key is authorized: cat ~/.ssh/id_rsa.pub >> ~/.ssh/authorized_keys\n" +
                    "4. Test SSH manually: ssh localhost whoami\n" +
                    "5. Run build without sudo (as current user, not root)\n"
                );
            }

            logger.info("✓ SSH connection established (verified session exists)");
            logger.info("");

            // Create and run builder with SSH device
            GoIdContainerBuilder builder = new GoIdContainerBuilder(device, configFile);
            builder.build();

            logger.info("");
            logger.info("=================================================================");
            logger.info("  Build Completed Successfully");
            logger.info("=================================================================");

        } catch (Exception e) {
            logger.severe("Build failed: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        } finally {
            // Cleanup SSH connection
            if (device != null && device.isConnected()) {
                device.disconnect();
                logger.info("SSH connection closed");
            }
        }
    }
}
