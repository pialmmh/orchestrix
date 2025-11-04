package com.telcobright.orchestrix.images.lxc.uniqueidgenerator;

import com.telcobright.orchestrix.automation.api.model.AutomationConfig;
import com.telcobright.orchestrix.automation.api.model.CommandResult;
import com.telcobright.orchestrix.images.core.BaseImageBuilder;
import com.telcobright.orchestrix.images.model.ContainerConfig;
import com.telcobright.orchestrix.model.NetworkConfig;
import com.telcobright.orchestrix.model.ServiceConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Unique ID Generator LXC Container Builder
 * Builds container with sharding support for high availability
 */
public class UniqueIdGeneratorBuilder extends BaseImageBuilder {

    private static final Logger logger = LoggerFactory.getLogger(UniqueIdGeneratorBuilder.class);

    private static final String SERVICE_NAME = "unique-id-generator";
    private static final String NODE_VERSION = "20";
    private static final int SERVICE_PORT = 7001;

    private final ServiceConfig serviceConfig;
    private final int shardId;
    private final int totalShards;

    public UniqueIdGeneratorBuilder(ContainerConfig containerConfig,
                                   AutomationConfig automationConfig,
                                   int shardId,
                                   int totalShards) {
        super(containerConfig, automationConfig);

        this.shardId = shardId;
        this.totalShards = totalShards;

        // Initialize service configuration
        this.serviceConfig = new ServiceConfig(
            SERVICE_NAME,
            "/usr/bin/node /opt/unique-id-generator/server.js",
            SERVICE_PORT
        );

        serviceConfig.setWorkingDirectory("/opt/unique-id-generator");
        serviceConfig.addEnvironment("NODE_ENV", "production");
        serviceConfig.addEnvironment("PORT", String.valueOf(SERVICE_PORT));
        serviceConfig.addEnvironment("SHARD_ID", String.valueOf(shardId));
        serviceConfig.addEnvironment("TOTAL_SHARDS", String.valueOf(totalShards));
        serviceConfig.setUser("nobody");
        serviceConfig.setGroup("nogroup");
        serviceConfig.setMemoryLimit(256L * 1024 * 1024); // 256MB
        serviceConfig.setCpuLimit(0.5); // 50% CPU
    }

    @Override
    public String getName() {
        return "Unique ID Generator Builder (Shard " + shardId + "/" + totalShards + ")";
    }

    @Override
    public String getDescription() {
        return "Builds LXC container with sharded unique ID generation service for high availability";
    }

    @Override
    public boolean validate() {
        logger.info("Validating Unique ID Generator build prerequisites...");

        // Check if LXC is available
        if (!commandExists("lxc")) {
            logger.error("LXC is not installed");
            return false;
        }

        // Validate network configuration
        NetworkConfig network = containerConfig.getNetworkConfig();
        if (network == null || !network.isValid()) {
            logger.error("Invalid network configuration");
            return false;
        }

        // Check if container IP is in required subnet
        if (!network.getIpAddress().startsWith("10.10.199.")) {
            logger.error("Container IP must be in 10.10.199.0/24 subnet");
            return false;
        }

        // Validate sharding configuration
        if (shardId < 1 || shardId > totalShards) {
            logger.error("Invalid shard configuration: shardId={}, totalShards={}",
                        shardId, totalShards);
            return false;
        }

        logger.info("✓ Prerequisites validated - Shard {}/{}", shardId, totalShards);
        return true;
    }

    @Override
    protected boolean validatePrerequisites() {
        logger.info("Checking prerequisites for shard {}...", shardId);

        // Clean up existing container if present
        String containerName = containerConfig.getContainerName();
        CommandResult result = executeCommand(
            "lxc info " + containerName + " 2>/dev/null", false);

        if (result.isSuccess()) {
            logger.warn("Container {} already exists, removing...", containerName);
            executeCommand("lxc delete --force " + containerName);
        }

        // Check if bridge exists
        String bridge = containerConfig.getNetworkConfig().getBridge();
        result = executeCommand("lxc network show " + bridge + " 2>/dev/null", false);
        if (!result.isSuccess()) {
            logger.error("Bridge {} not found", bridge);
            return false;
        }

        // Clean up old image alias if exists
        executeCommand("lxc image alias delete " + SERVICE_NAME + "-base 2>/dev/null || true");

        return true;
    }

    @Override
    protected boolean createContainer() {
        logger.info("Creating container {} for shard {}...",
                   containerConfig.getContainerName(), shardId);

        String command = String.format("lxc launch %s %s",
            containerConfig.getBaseImage(),
            containerConfig.getContainerName());

        executeCommand(command);

        // Wait for container to be ready
        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }

        // Verify container is running
        CommandResult result = executeCommand(
            "lxc info " + containerConfig.getContainerName() + " | grep Status:");

        if (!result.getStdout().contains("Running")) {
            logger.error("Container failed to start");
            return false;
        }

        logger.info("✓ Container created successfully");
        return true;
    }

    @Override
    protected boolean setupNetwork() {
        logger.info("Configuring network for shard {}...", shardId);

        NetworkConfig network = containerConfig.getNetworkConfig();
        String containerName = containerConfig.getContainerName();

        // Remove default network device
        executeCommand("lxc config device remove " + containerName + " eth0 2>/dev/null || true");

        // Add bridge with static IP
        String ip = network.getIpAddress().replace("/24", "");
        String command = String.format(
            "lxc config device add %s eth0 nic nictype=bridged parent=%s ipv4.address=%s",
            containerName,
            network.getBridge(),
            ip
        );
        executeCommand(command);

        // Configure DNS inside container
        containerExec("echo 'nameserver 8.8.8.8' > /etc/resolv.conf");
        containerExec("echo 'nameserver 8.8.4.4' >> /etc/resolv.conf");

        // Add default route
        String gateway = network.getGateway().replace("/24", "");
        containerExec("ip route add default via " + gateway + " 2>/dev/null || true");

        // Test internet connectivity
        return testInternetConnectivity();
    }

    @Override
    protected boolean installPackages() {
        logger.info("Installing packages for unique-id-generator...");

        // Update system
        containerExec("apt-get update");

        // Install base packages
        containerExec("DEBIAN_FRONTEND=noninteractive apt-get install -y " +
            "curl ca-certificates gnupg systemd python3 nano less");

        // Install Node.js
        logger.info("Installing Node.js {}...", NODE_VERSION);

        containerExec("mkdir -p /etc/apt/keyrings");
        containerExec("curl -fsSL https://deb.nodesource.com/gpgkey/nodesource-repo.gpg.key | " +
            "gpg --dearmor -o /etc/apt/keyrings/nodesource.gpg");
        containerExec("echo 'deb [signed-by=/etc/apt/keyrings/nodesource.gpg] " +
            "https://deb.nodesource.com/node_" + NODE_VERSION + ".x nodistro main' > " +
            "/etc/apt/sources.list.d/nodesource.list");

        containerExec("apt-get update");
        containerExec("DEBIAN_FRONTEND=noninteractive apt-get install -y nodejs");

        // Verify Node.js installation
        CommandResult result = containerExec("node --version");
        if (!result.getStdout().contains("v" + NODE_VERSION)) {
            logger.error("Node.js installation failed");
            return false;
        }

        logger.info("✓ Packages installed successfully");
        return true;
    }

    @Override
    protected boolean setupServices() {
        logger.info("Setting up unique-id-generator service for shard {}...", shardId);

        // Create service directories
        containerExec("mkdir -p /opt/unique-id-generator");
        containerExec("mkdir -p /var/lib/unique-id-generator");

        // Copy service files
        String scriptsDir = "/home/mustafa/telcobright-projects/orchestrix/images/containers/lxc/unique-id-generator/scripts";
        copyToContainer(scriptsDir + "/server.js", "/opt/unique-id-generator/server.js");
        copyToContainer(scriptsDir + "/package.json", "/opt/unique-id-generator/package.json");

        // Set ownership
        containerExec("chown -R nobody:nogroup /opt/unique-id-generator");
        containerExec("chown nobody:nogroup /var/lib/unique-id-generator");

        // Install npm dependencies
        logger.info("Installing npm dependencies...");
        containerExec("cd /opt/unique-id-generator && npm install --production");

        // Setup systemd service with shard configuration
        return setupSystemdService();
    }

    @Override
    protected boolean deployApplication() {
        logger.info("Deploying unique-id-generator application (shard {})...", shardId);

        // Create log file
        containerExec("touch /var/log/unique-id-generator.log");
        containerExec("chown nobody:nogroup /var/log/unique-id-generator.log");

        // Enable and start service
        containerExec("systemctl daemon-reload");
        containerExec("systemctl enable " + SERVICE_NAME);
        containerExec("systemctl start " + SERVICE_NAME);

        // Wait for service to start
        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }

        // Check service status
        CommandResult result = containerExec("systemctl is-active " + SERVICE_NAME);
        if (!result.getStdout().contains("active")) {
            logger.error("Service failed to start");

            // Get service logs for debugging
            CommandResult logs = containerExec("journalctl -u " + SERVICE_NAME + " -n 50");
            logger.error("Service logs: {}", logs.getStdout());

            return false;
        }

        logger.info("✓ Application deployed successfully");
        return true;
    }

    @Override
    protected boolean verifyInstallation() {
        logger.info("Verifying unique-id-generator installation (shard {})...", shardId);

        // Get container IP
        String containerName = containerConfig.getContainerName();
        CommandResult result = executeCommand(
            "lxc list " + containerName +
            " -c 4 --format csv | grep -oE '[0-9]+\\.[0-9]+\\.[0-9]+\\.[0-9]+' | head -1");

        String ip = result.getStdout().trim();
        if (ip.isEmpty()) {
            logger.error("Could not determine container IP");
            return false;
        }

        logger.info("Container IP: {}", ip);

        // Test health endpoint
        result = executeCommand("curl -s http://" + ip + ":" + SERVICE_PORT + "/health");
        if (!result.getStdout().contains("healthy")) {
            logger.error("Health check failed: {}", result.getStdout());
            return false;
        }

        // Test shard info endpoint
        result = executeCommand("curl -s http://" + ip + ":" + SERVICE_PORT + "/shard-info");
        if (!result.getStdout().contains("\"shardId\":" + shardId)) {
            logger.error("Shard configuration verification failed");
            return false;
        }

        // Test API types endpoint
        result = executeCommand("curl -s http://" + ip + ":" + SERVICE_PORT + "/api/types");
        if (!result.getStdout().contains("uuid8")) {
            logger.error("API types verification failed");
            return false;
        }

        // Test ID generation
        result = executeCommand("curl -s 'http://" + ip + ":" + SERVICE_PORT +
                               "/api/next-id/test?dataType=int'");
        if (!result.getStdout().contains("\"shard\":" + shardId)) {
            logger.error("ID generation test failed");
            return false;
        }

        logger.info("✓ Installation verified successfully for shard {}", shardId);
        logger.info("Service is accessible at http://{}:{}", ip, SERVICE_PORT);

        return true;
    }

    @Override
    protected boolean optimizeImage() {
        if (!containerConfig.isOptimizeSize()) {
            logger.info("Skipping optimization");
            return true;
        }

        logger.info("Optimizing container image size...");

        containerExec("apt-get clean");
        containerExec("apt-get autoremove -y");
        containerExec("rm -rf /var/lib/apt/lists/*");
        containerExec("rm -rf /tmp/* /var/tmp/*");
        containerExec("rm -rf /usr/share/doc/*");
        containerExec("rm -rf /usr/share/man/*");
        containerExec("find /var/log -type f -exec truncate -s 0 {} \\;");

        logger.info("✓ Image optimized");
        return true;
    }

    @Override
    protected boolean publishImage() {
        logger.info("Publishing unique-id-generator image...");

        String containerName = containerConfig.getContainerName();

        // Stop container
        executeCommand("lxc stop " + containerName);

        // Publish as image
        String imageName = SERVICE_NAME + "-base";
        executeCommand("lxc publish " + containerName + " --alias " + imageName + " --public");

        logger.info("✓ Image published as {}", imageName);
        return true;
    }

    @Override
    protected boolean cleanup() {
        logger.info("Cleaning up build artifacts...");

        // Delete build container
        String containerName = containerConfig.getContainerName();
        executeCommand("lxc delete --force " + containerName + " 2>/dev/null || true");

        logger.info("✓ Cleanup completed");
        return true;
    }

    // Helper methods

    private boolean setupSystemdService() {
        logger.info("Creating systemd service with shard configuration...");

        // Generate service file content with shard environment variables
        String serviceContent = serviceConfig.generateSystemdService();

        // Write service file to container
        String servicePath = "/etc/systemd/system/" + SERVICE_NAME + ".service";
        String tempFile = "/tmp/" + SERVICE_NAME + ".service";

        try {
            // Write to temp file on host
            executeCommand("cat > " + tempFile + " << 'EOF'\n" + serviceContent + "\nEOF");

            // Copy to container
            copyToContainer(tempFile, servicePath);

            // Clean up temp file
            executeCommand("rm " + tempFile);

            logger.info("✓ Systemd service configured for shard {}", shardId);
            return true;

        } catch (Exception e) {
            logger.error("Failed to setup systemd service", e);
            return false;
        }
    }

    private boolean testInternetConnectivity() {
        logger.info("Testing internet connectivity...");

        int maxAttempts = 3;
        for (int i = 1; i <= maxAttempts; i++) {
            CommandResult result = containerExec("ping -c 1 google.com");
            if (result.isSuccess()) {
                logger.info("✓ Internet connectivity confirmed");
                return true;
            }

            logger.warn("Attempt {}/{}: No internet connectivity", i, maxAttempts);

            if (i < maxAttempts) {
                logger.info("Applying network fixes...");
                executeCommand("sudo sysctl -w net.ipv4.ip_forward=1", false);
                executeCommand("sudo iptables -t nat -A POSTROUTING -s 10.10.199.0/24 -j MASQUERADE", false);

                try {
                    Thread.sleep(5000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }

        logger.error("Failed to establish internet connectivity");
        return false;
    }
}