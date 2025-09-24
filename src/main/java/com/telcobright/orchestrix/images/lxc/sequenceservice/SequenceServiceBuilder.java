package com.telcobright.orchestrix.images.lxc.sequenceservice;

import com.telcobright.orchestrix.automation.model.AutomationConfig;
import com.telcobright.orchestrix.automation.model.CommandResult;
import com.telcobright.orchestrix.images.core.BaseImageBuilder;
import com.telcobright.orchestrix.images.model.ContainerConfig;
import com.telcobright.orchestrix.model.NetworkConfig;
import com.telcobright.orchestrix.model.ServiceConfig;

/**
 * Sequence Service LXC Container Builder
 * Using refactored model structure
 */
public class SequenceServiceBuilder extends BaseImageBuilder {

    private static final String SERVICE_NAME = "sequence-service";
    private static final String NODE_VERSION = "20";
    private static final int SERVICE_PORT = 7001;

    private final ServiceConfig serviceConfig;

    public SequenceServiceBuilder(ContainerConfig containerConfig, AutomationConfig automationConfig) {
        super(containerConfig, automationConfig);

        // Initialize service configuration
        this.serviceConfig = new ServiceConfig(
            SERVICE_NAME,
            "/usr/bin/node /opt/sequence-service/server.js",
            SERVICE_PORT
        );
        serviceConfig.setWorkingDirectory("/opt/sequence-service");
        serviceConfig.addEnvironment("NODE_ENV", "production");
        serviceConfig.addEnvironment("PORT", String.valueOf(SERVICE_PORT));
    }

    @Override
    public String getName() {
        return "Sequence Service Builder";
    }

    @Override
    public String getDescription() {
        return "Builds LXC container with sequence/UUID generation service";
    }

    @Override
    public boolean validate() {
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

        return true;
    }

    @Override
    protected boolean validatePrerequisites() {
        logger.info("Validating prerequisites...");

        // Clean up existing container if present
        CommandResult result = executeCommand(
            "lxc info " + containerConfig.getContainerName() + " 2>/dev/null", false);

        if (result.isSuccess()) {
            logger.warn("Container already exists, removing...");
            executeCommand("lxc delete --force " + containerConfig.getContainerName());
        }

        // Check bridge exists
        result = executeCommand("lxc network show " + containerConfig.getNetworkConfig().getBridge(), false);
        if (!result.isSuccess()) {
            logger.error("Bridge {} not found", containerConfig.getNetworkConfig().getBridge());
            return false;
        }

        return true;
    }

    @Override
    protected boolean createContainer() {
        logger.info("Creating container {}...", containerConfig.getContainerName());

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

        return true;
    }

    @Override
    protected boolean setupNetwork() {
        logger.info("Setting up network...");

        NetworkConfig network = containerConfig.getNetworkConfig();

        // Remove default network
        executeCommand("lxc config device remove " + containerConfig.getContainerName() + " eth0 2>/dev/null || true");

        // Add bridge with static IP
        String ip = network.getIpAddress().replace("/24", "");
        String command = String.format(
            "lxc config device add %s eth0 nic nictype=bridged parent=%s ipv4.address=%s",
            containerConfig.getContainerName(),
            network.getBridge(),
            ip
        );
        executeCommand(command);

        // Configure DNS
        containerExec("echo 'nameserver 8.8.8.8' > /etc/resolv.conf");
        containerExec("echo 'nameserver 8.8.4.4' >> /etc/resolv.conf");

        // Add default route
        String gateway = network.getGateway().replace("/24", "");
        containerExec("ip route add default via " + gateway + " 2>/dev/null || true");

        // Test connectivity
        return testInternetConnectivity();
    }

    @Override
    protected boolean installPackages() {
        logger.info("Installing packages...");

        // Update system
        containerExec("apt-get update");

        // Install base packages
        containerExec("DEBIAN_FRONTEND=noninteractive apt-get install -y " +
            "curl ca-certificates gnupg systemd python3");

        // Install Node.js
        return installNodeJs();
    }

    @Override
    protected boolean setupServices() {
        logger.info("Setting up services...");

        // Create service directory
        containerExec("mkdir -p /opt/sequence-service");
        containerExec("mkdir -p /var/lib/sequence-service");

        // Copy service files
        String scriptsDir = containerConfig.getBuildDirectory() + "/scripts";
        copyToContainer(scriptsDir + "/server.js", "/opt/sequence-service/server.js");
        copyToContainer(scriptsDir + "/package.json", "/opt/sequence-service/package.json");
        copyToContainer(scriptsDir + "/reset-data.sh", "/usr/local/bin/sequence-reset");

        // Set permissions
        containerExec("chmod 755 /usr/local/bin/sequence-reset");
        containerExec("chown -R nobody:nogroup /opt/sequence-service");
        containerExec("chown nobody:nogroup /var/lib/sequence-service");

        // Install npm dependencies
        containerExec("cd /opt/sequence-service && npm install --production");

        // Setup systemd service
        return setupSystemdService();
    }

    @Override
    protected boolean deployApplication() {
        logger.info("Deploying application...");

        // Start the service
        containerExec("systemctl daemon-reload");
        containerExec("systemctl enable " + SERVICE_NAME);
        containerExec("systemctl start " + SERVICE_NAME);

        // Wait for service to start
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }

        // Check service status
        CommandResult result = containerExec("systemctl is-active " + SERVICE_NAME);
        return result.getStdout().contains("active");
    }

    @Override
    protected boolean verifyInstallation() {
        logger.info("Verifying installation...");

        // Get container IP
        CommandResult result = executeCommand(
            "lxc list " + containerConfig.getContainerName() +
            " -c 4 --format csv | grep -oE '[0-9]+\\.[0-9]+\\.[0-9]+\\.[0-9]+' | head -1");

        String ip = result.getStdout().trim();
        if (ip.isEmpty()) {
            logger.error("Could not get container IP");
            return false;
        }

        // Test health endpoint
        result = executeCommand("curl -s http://" + ip + ":" + SERVICE_PORT + "/health");
        if (!result.getStdout().contains("healthy")) {
            logger.error("Health check failed");
            return false;
        }

        // Test API endpoint
        result = executeCommand("curl -s http://" + ip + ":" + SERVICE_PORT + "/api/types");
        if (!result.getStdout().contains("uuid8")) {
            logger.error("API verification failed");
            return false;
        }

        logger.info("✓ Service verified successfully");
        return true;
    }

    @Override
    protected boolean optimizeImage() {
        if (!containerConfig.isOptimizeSize()) {
            logger.info("Skipping optimization");
            return true;
        }

        logger.info("Optimizing image size...");

        containerExec("apt-get clean");
        containerExec("apt-get autoremove -y");
        containerExec("rm -rf /var/lib/apt/lists/*");
        containerExec("rm -rf /tmp/* /var/tmp/*");
        containerExec("find /var/log -type f -exec truncate -s 0 {} \\;");

        return true;
    }

    @Override
    protected boolean publishImage() {
        logger.info("Publishing image...");

        // Stop container
        executeCommand("lxc stop " + containerConfig.getContainerName());

        // Delete existing alias
        executeCommand("lxc image alias delete " + SERVICE_NAME + "-base 2>/dev/null || true");

        // Publish as image
        executeCommand("lxc publish " + containerConfig.getContainerName() +
            " --alias " + SERVICE_NAME + "-base --public");

        logger.info("✓ Image published as {}-base", SERVICE_NAME);
        return true;
    }

    @Override
    protected boolean cleanup() {
        logger.info("Cleaning up...");

        // Delete build container
        executeCommand("lxc delete --force " + containerConfig.getContainerName());

        return true;
    }

    // Helper methods

    private boolean installNodeJs() {
        logger.info("Installing Node.js {}...", NODE_VERSION);

        containerExec("mkdir -p /etc/apt/keyrings");
        containerExec("curl -fsSL https://deb.nodesource.com/gpgkey/nodesource-repo.gpg.key | " +
            "gpg --dearmor -o /etc/apt/keyrings/nodesource.gpg");
        containerExec("echo 'deb [signed-by=/etc/apt/keyrings/nodesource.gpg] " +
            "https://deb.nodesource.com/node_" + NODE_VERSION + ".x nodistro main' > " +
            "/etc/apt/sources.list.d/nodesource.list");
        containerExec("apt-get update");
        containerExec("DEBIAN_FRONTEND=noninteractive apt-get install -y nodejs");

        // Verify installation
        CommandResult result = containerExec("node --version");
        return result.getStdout().contains("v" + NODE_VERSION);
    }

    private boolean setupSystemdService() {
        logger.info("Setting up systemd service...");

        // Generate service file content
        String serviceContent = serviceConfig.generateSystemdService();

        // Write to temp file and copy
        try {
            String tempFile = "/tmp/" + SERVICE_NAME + ".service";
            executeCommand("cat > " + tempFile + " << 'EOF'\n" + serviceContent + "\nEOF");
            copyToContainer(tempFile, "/etc/systemd/system/" + SERVICE_NAME + ".service");
            executeCommand("rm " + tempFile);
            return true;
        } catch (Exception e) {
            logger.error("Failed to setup systemd service", e);
            return false;
        }
    }

    private boolean testInternetConnectivity() {
        logger.info("Testing internet connectivity...");

        for (int i = 0; i < 3; i++) {
            CommandResult result = containerExec("ping -c 1 google.com");
            if (result.isSuccess()) {
                logger.info("✓ Internet connectivity confirmed");
                return true;
            }

            logger.warn("No connectivity, applying fixes...");
            executeCommand("sudo sysctl -w net.ipv4.ip_forward=1");
            executeCommand("sudo iptables -t nat -A POSTROUTING -s 10.10.199.0/24 -j MASQUERADE");

            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }

        return false;
    }
}