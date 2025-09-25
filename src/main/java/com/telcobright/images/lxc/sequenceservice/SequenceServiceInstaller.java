package com.telcobright.images.lxc.sequenceservice;

import com.orchestrix.automation.shellexec.bash.LocalAutomation;
import com.orchestrix.automation.shellexec.bash.BashExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Installer for Sequence Service LXC Container
 * Each method represents a retryable section of the installation
 */
public class SequenceServiceInstaller extends LocalAutomation {

    private static final Logger logger = LoggerFactory.getLogger(SequenceServiceInstaller.class);

    private final String containerName;
    private final String baseImage;
    private final NetworkConfig networkConfig;

    public SequenceServiceInstaller(String containerName, String baseImage, NetworkConfig networkConfig) {
        super();
        this.containerName = containerName;
        this.baseImage = baseImage;
        this.networkConfig = networkConfig;
    }

    @Override
    public String getName() {
        return "Sequence Service Installer";
    }

    @Override
    public String getDescription() {
        return "Installs and configures Sequence Service in LXC container";
    }

    @Override
    public boolean execute() {
        try {
            // Each section can be retried independently if it fails
            validatePrerequisites();
            createContainer();
            configureNetwork();
            installNodeJs();
            setupSequenceService();
            configureSystemd();
            verifyInstallation();
            publishImage();

            logger.info("Sequence Service installation completed successfully");
            return true;

        } catch (Exception e) {
            logger.error("Installation failed: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * Section 1: Validate prerequisites
     * Can be retried if network/LXC issues
     */
    private void validatePrerequisites() {
        logger.info("=== SECTION 1: Validating Prerequisites ===");

        // Check LXC is installed
        if (!commandExists("lxc")) {
            throw new RuntimeException("LXC is not installed");
        }

        // Validate network configuration
        validateNetwork();

        // Clean up any existing containers
        BashExecutor.CommandResult result = executeWithResult(
            "lxc info " + containerName + " 2>/dev/null");

        if (result.isSuccess()) {
            logger.warn("Container {} already exists, removing...", containerName);
            executeCommand("lxc delete --force " + containerName);
        }

        logger.info("✓ Prerequisites validated");
    }

    /**
     * Section 2: Create and start container
     * Retryable if container creation fails
     */
    private void createContainer() {
        logger.info("=== SECTION 2: Creating Container ===");

        String command = String.format("lxc launch %s %s", baseImage, containerName);
        executeCommand(command);

        // Wait for container to be ready
        Thread.sleep(3000);

        // Verify container is running
        String status = executeCommand("lxc info " + containerName + " | grep Status:");
        if (!status.contains("Running")) {
            throw new RuntimeException("Container failed to start");
        }

        logger.info("✓ Container created and running");
    }

    /**
     * Section 3: Configure network
     * Retryable if network setup fails
     */
    private void configureNetwork() {
        logger.info("=== SECTION 3: Configuring Network ===");

        // Validate /24 subnet
        if (!networkConfig.getContainerIp().endsWith("/24")) {
            throw new RuntimeException("Container IP must include /24 notation");
        }

        // Remove default network device
        executeCommand("lxc config device remove " + containerName + " eth0 2>/dev/null || true");

        // Add bridge network with static IP
        String ipWithoutMask = networkConfig.getContainerIp().replace("/24", "");
        String command = String.format(
            "lxc config device add %s eth0 nic nictype=bridged parent=%s ipv4.address=%s",
            containerName, networkConfig.getBridgeName(), ipWithoutMask
        );
        executeCommand(command);

        // Configure DNS inside container
        lxcExec("echo 'nameserver 8.8.8.8' > /etc/resolv.conf");
        lxcExec("echo 'nameserver 8.8.4.4' >> /etc/resolv.conf");

        // Add default route
        String gateway = networkConfig.getGatewayIp().replace("/24", "");
        lxcExec("ip route add default via " + gateway + " 2>/dev/null || true");

        // Test internet connectivity with retry
        testInternetWithRetry();

        logger.info("✓ Network configured successfully");
    }

    /**
     * Section 4: Install Node.js
     * Retryable if package installation fails
     */
    private void installNodeJs() {
        logger.info("=== SECTION 4: Installing Node.js ===");

        // Update package list
        lxcExec("apt-get update");

        // Install prerequisites
        lxcExec("DEBIAN_FRONTEND=noninteractive apt-get install -y curl ca-certificates gnupg");

        // Add NodeSource repository
        String nodeVersion = "20";
        lxcExec("mkdir -p /etc/apt/keyrings");
        lxcExec("curl -fsSL https://deb.nodesource.com/gpgkey/nodesource-repo.gpg.key | " +
                "gpg --dearmor -o /etc/apt/keyrings/nodesource.gpg");
        lxcExec("echo 'deb [signed-by=/etc/apt/keyrings/nodesource.gpg] " +
                "https://deb.nodesource.com/node_" + nodeVersion + ".x nodistro main' > " +
                "/etc/apt/sources.list.d/nodesource.list");

        // Install Node.js
        lxcExec("apt-get update");
        lxcExec("DEBIAN_FRONTEND=noninteractive apt-get install -y nodejs");

        // Verify installation
        String version = lxcExec("node --version");
        if (!version.contains("v" + nodeVersion)) {
            throw new RuntimeException("Node.js installation failed");
        }

        logger.info("✓ Node.js {} installed successfully", version.trim());
    }

    /**
     * Section 5: Setup Sequence Service
     * Retryable if file operations fail
     */
    private void setupSequenceService() {
        logger.info("=== SECTION 5: Setting up Sequence Service ===");

        // Create service directory
        lxcExec("mkdir -p /opt/sequence-service");

        // Copy service files
        String scriptsDir = "/home/mustafa/telcobright-projects/orchestrix/images/lxc/auto-increment-service/scripts";
        copyFileToContainer(scriptsDir + "/server.js", "/opt/sequence-service/server.js");
        copyFileToContainer(scriptsDir + "/package.json", "/opt/sequence-service/package.json");
        copyFileToContainer(scriptsDir + "/reset-data.sh", "/usr/local/bin/sequence-reset");

        // Set permissions
        lxcExec("chmod 755 /usr/local/bin/sequence-reset");

        // Install npm dependencies
        lxcExec("cd /opt/sequence-service && npm install --production");

        // Create data directory
        lxcExec("mkdir -p /var/lib/sequence-service");
        lxcExec("chown nobody:nogroup /var/lib/sequence-service");

        logger.info("✓ Sequence Service setup complete");
    }

    /**
     * Section 6: Configure systemd service
     * Retryable if systemd configuration fails
     */
    private void configureSystemd() {
        logger.info("=== SECTION 6: Configuring Systemd ===");

        // Copy systemd service file
        String serviceFile = "/home/mustafa/telcobright-projects/orchestrix/images/lxc/" +
                            "auto-increment-service/scripts/sequence-service.service";
        copyFileToContainer(serviceFile, "/etc/systemd/system/sequence-service.service");

        // Set ownership
        lxcExec("chown -R nobody:nogroup /opt/sequence-service");

        // Create log file
        lxcExec("touch /var/log/sequence-service.log");
        lxcExec("chown nobody:nogroup /var/log/sequence-service.log");

        // Enable and start service
        lxcExec("systemctl daemon-reload");
        lxcExec("systemctl enable sequence-service");
        lxcExec("systemctl start sequence-service");

        // Verify service is running
        Thread.sleep(2000);
        String status = lxcExec("systemctl is-active sequence-service");
        if (!status.contains("active")) {
            throw new RuntimeException("Service failed to start");
        }

        logger.info("✓ Systemd service configured and running");
    }

    /**
     * Section 7: Verify installation
     * Retryable if verification fails
     */
    private void verifyInstallation() {
        logger.info("=== SECTION 7: Verifying Installation ===");

        // Get container IP
        String ip = executeCommand("lxc list " + containerName +
            " -c 4 --format csv | grep -oE '[0-9]+\\.[0-9]+\\.[0-9]+\\.[0-9]+' | head -1").trim();

        // Test health endpoint
        String health = executeCommand("curl -s http://" + ip + ":7001/health");
        if (!health.contains("healthy")) {
            throw new RuntimeException("Health check failed");
        }

        // Test API types endpoint
        String types = executeCommand("curl -s http://" + ip + ":7001/api/types");
        if (!types.contains("uuid8")) {
            throw new RuntimeException("API verification failed");
        }

        logger.info("✓ Installation verified successfully");
    }

    /**
     * Section 8: Publish container as image
     * Retryable if publishing fails
     */
    private void publishImage() {
        logger.info("=== SECTION 8: Publishing Image ===");

        // Stop container
        executeCommand("lxc stop " + containerName);

        // Delete existing alias if exists
        executeCommand("lxc image alias delete sequence-service-base 2>/dev/null || true");

        // Publish as image
        executeCommand("lxc publish " + containerName + " --alias sequence-service-base --public");

        // Delete build container
        executeCommand("lxc delete " + containerName);

        logger.info("✓ Image published as sequence-service-base");
    }

    // Helper methods

    private void lxcExec(String command) {
        String fullCommand = String.format("lxc exec %s -- bash -c '%s'",
            containerName, command.replace("'", "'\\''"));
        executeCommand(fullCommand);
    }

    private void copyFileToContainer(String localPath, String containerPath) {
        String command = String.format("lxc file push %s %s%s",
            localPath, containerName, containerPath);
        executeCommand(command);
    }

    private void validateNetwork() {
        // Validate subnet is 10.10.199.0/24
        if (!networkConfig.getContainerIp().startsWith("10.10.199.")) {
            throw new RuntimeException("Container IP must be in 10.10.199.0/24 subnet");
        }

        // Check bridge exists
        String bridges = executeCommand("lxc network list --format csv");
        if (!bridges.contains(networkConfig.getBridgeName())) {
            throw new RuntimeException("Bridge " + networkConfig.getBridgeName() + " does not exist");
        }
    }

    private void testInternetWithRetry() {
        int maxRetries = 3;
        for (int i = 0; i < maxRetries; i++) {
            BashExecutor.CommandResult result = executeWithResult(
                String.format("lxc exec %s -- ping -c 1 google.com", containerName)
            );

            if (result.isSuccess()) {
                logger.info("✓ Internet connectivity confirmed");
                return;
            }

            logger.warn("No internet connectivity, attempt {}/{}", i+1, maxRetries);

            if (i < maxRetries - 1) {
                logger.info("Applying network fixes...");
                executeCommand("sudo sysctl -w net.ipv4.ip_forward=1");
                executeCommand("sudo iptables -t nat -A POSTROUTING -s 10.10.199.0/24 -j MASQUERADE");

                try {
                    Thread.sleep(5000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }

        throw new RuntimeException("Failed to establish internet connectivity after " + maxRetries + " attempts");
    }

    // Configuration class
    public static class NetworkConfig {
        private final String containerIp;
        private final String gatewayIp;
        private final String bridgeName;

        public NetworkConfig(String containerIp, String gatewayIp, String bridgeName) {
            this.containerIp = containerIp;
            this.gatewayIp = gatewayIp;
            this.bridgeName = bridgeName;
        }

        public String getContainerIp() { return containerIp; }
        public String getGatewayIp() { return gatewayIp; }
        public String getBridgeName() { return bridgeName; }
    }
}