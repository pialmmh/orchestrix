package com.telcobright.orchestrix.automation.deploy;

import com.telcobright.orchestrix.automation.core.device.impl.RemoteSshDevice;
import com.telcobright.orchestrix.automation.deploy.entity.DeploymentConfig;
import com.telcobright.orchestrix.automation.publish.util.MD5Util;

import java.io.File;
import java.util.logging.Logger;

/**
 * Deployment Manager - Orchestrates LXC container deployment to remote servers
 * <p>
 * Workflow:
 * 1. Connect to remote server via SSH
 * 2. Check prerequisites (LXC, network, storage)
 * 3. Transfer container image
 * 4. Verify MD5 hash
 * 5. Import image
 * 6. Create and configure container
 * 7. Start container
 * 8. Verify deployment
 */
public class DeploymentManager {
    private static final Logger logger = Logger.getLogger(DeploymentManager.class.getName());
    private final DeploymentConfig config;
    private RemoteSshDevice sshDevice;

    public DeploymentManager(DeploymentConfig config) {
        this.config = config;
    }

    /**
     * Deploy container to remote server
     *
     * @return true if deployment successful
     * @throws Exception if deployment fails
     */
    public boolean deploy() throws Exception {
        logger.info("=== Starting Container Deployment ===");
        logger.info("Target: " + config.getSshUser() + "@" + config.getServerIp());
        logger.info("Container: " + config.getContainerName());
        logger.info("Artifact: " + config.getArtifactName());

        try {
            // Step 1: Connect to remote server
            connectToServer();

            // Step 2: Check prerequisites
            checkPrerequisites();

            // Step 3: Transfer and verify image
            transferImage();

            // Step 4: Import image
            importImage();

            // Step 5: Create and configure container
            createContainer();

            // Step 6: Start container
            startContainer();

            // Step 7: Verify deployment
            verifyDeployment();

            logger.info("\n=== Deployment Successful ===");
            return true;

        } catch (Exception e) {
            logger.severe("Deployment failed: " + e.getMessage());
            throw e;
        } finally {
            if (sshDevice != null && sshDevice.isConnected()) {
                sshDevice.disconnect();
            }
        }
    }

    private void connectToServer() throws Exception {
        logger.info("\n[1/7] Connecting to remote server...");

        sshDevice = new RemoteSshDevice(
                config.getServerIp(),
                config.getServerPort(),
                config.getSshUser()
        );

        // Connect with password or key
        if (config.getSshPassword() != null && !config.getSshPassword().isEmpty()) {
            sshDevice.connect(config.getSshPassword());
        } else if (config.getSshKeyPath() != null && !config.getSshKeyPath().isEmpty()) {
            sshDevice.connectWithKey(config.getSshKeyPath());
        } else {
            throw new Exception("No SSH authentication method provided (password or key)");
        }

        logger.info("✓ Connected to " + config.getServerIp());
    }

    private void checkPrerequisites() throws Exception {
        logger.info("\n[2/7] Checking prerequisites...");

        // Check LXD/LXC
        String lxcVersion = sshDevice.executeCommand("lxc version");
        if (lxcVersion == null || lxcVersion.isEmpty()) {
            throw new Exception("LXC/LXD not found on remote server");
        }
        logger.info("✓ LXC version: " + lxcVersion.split("\n")[0]);

        // Check network bridge
        if (config.getNetworkBridge() != null) {
            String networkCheck = sshDevice.executeCommand("lxc network show " + config.getNetworkBridge() + " 2>/dev/null || echo NOT_FOUND");
            if (networkCheck.contains("NOT_FOUND")) {
                logger.warning("⚠ Network bridge not found: " + config.getNetworkBridge());
                logger.info("  Will use default network");
            } else {
                logger.info("✓ Network bridge available: " + config.getNetworkBridge());
            }
        }

        // Check storage
        String storageCheck = sshDevice.executeCommand("lxc storage list --format csv");
        if (storageCheck == null || storageCheck.trim().isEmpty()) {
            logger.warning("⚠ No LXC storage pools found - using default");
        } else {
            logger.info("✓ Storage pools available");
        }

        logger.info("✓ Prerequisites check passed");
    }

    private void transferImage() throws Exception {
        logger.info("\n[3/7] Transferring container image...");

        File artifactFile = new File(config.getArtifactPath());
        if (!artifactFile.exists()) {
            throw new Exception("Artifact file not found: " + config.getArtifactPath());
        }

        // Create remote temp directory
        String remoteTempDir = "/tmp/lxc-deploy-" + System.currentTimeMillis();
        sshDevice.executeCommand("mkdir -p " + remoteTempDir);

        // Calculate local MD5
        logger.info("  Calculating MD5 hash...");
        String localMd5 = MD5Util.generate(artifactFile);
        logger.info("  Local MD5: " + localMd5);

        // Transfer file using scp (via SSH device)
        String remoteImagePath = remoteTempDir + "/" + artifactFile.getName();
        logger.info("  Transferring " + formatBytes(artifactFile.length()) + "...");

        transferFileToRemote(artifactFile, remoteImagePath);

        // Verify MD5 on remote
        logger.info("  Verifying MD5 on remote...");
        String remoteMd5 = sshDevice.executeCommand("md5sum \"" + remoteImagePath + "\" | awk '{print $1}'").trim();
        logger.info("  Remote MD5: " + remoteMd5);

        if (!localMd5.equalsIgnoreCase(remoteMd5)) {
            throw new Exception("MD5 verification failed - transfer corrupted!");
        }

        logger.info("✓ Image transferred and verified: " + remoteImagePath);

        // Store for later use
        config.setArtifactPath(remoteImagePath);
    }

    private void transferFileToRemote(File localFile, String remotePath) throws Exception {
        // Use SCP via shell command (requires sshpass or key-based auth)
        String scpCommand;
        if (config.getSshPassword() != null && !config.getSshPassword().isEmpty()) {
            scpCommand = "sshpass -p '" + config.getSshPassword() + "' scp -P " + config.getServerPort() +
                    " -o StrictHostKeyChecking=no \"" + localFile.getAbsolutePath() + "\" " +
                    config.getSshUser() + "@" + config.getServerIp() + ":\"" + remotePath + "\"";
        } else {
            scpCommand = "scp -P " + config.getServerPort() + " -i \"" + config.getSshKeyPath() +
                    "\" -o StrictHostKeyChecking=no \"" + localFile.getAbsolutePath() + "\" " +
                    config.getSshUser() + "@" + config.getServerIp() + ":\"" + remotePath + "\"";
        }

        // Execute locally to transfer file
        Process process = Runtime.getRuntime().exec(new String[]{"/bin/bash", "-c", scpCommand});
        int exitCode = process.waitFor();

        if (exitCode != 0) {
            throw new Exception("SCP transfer failed with exit code: " + exitCode);
        }
    }

    private void importImage() throws Exception {
        logger.info("\n[4/7] Importing container image...");

        // Check if image already exists
        String existingImage = sshDevice.executeCommand("lxc image list --format csv | grep " + config.getImageName() + " || true");
        if (!existingImage.trim().isEmpty()) {
            logger.info("  Image already exists, deleting...");
            sshDevice.executeCommand("lxc image delete " + config.getImageName());
        }

        // Import image
        sshDevice.executeCommand("lxc image import \"" + config.getArtifactPath() + "\" --alias " + config.getImageName());

        logger.info("✓ Image imported as: " + config.getImageName());

        // Cleanup transferred file
        sshDevice.executeCommand("rm -f \"" + config.getArtifactPath() + "\"");
    }

    private void createContainer() throws Exception {
        logger.info("\n[5/7] Creating container...");

        // Check if container already exists
        String existingContainer = sshDevice.executeCommand("lxc list --format csv | grep ^" + config.getContainerName() + ", || true");
        if (!existingContainer.trim().isEmpty()) {
            logger.info("  Container already exists, deleting...");
            sshDevice.executeCommand("lxc stop " + config.getContainerName() + " --force 2>/dev/null || true");
            sshDevice.executeCommand("lxc delete " + config.getContainerName() + " --force");
        }

        // Create container
        sshDevice.executeCommand("lxc init " + config.getImageName() + " " + config.getContainerName());

        // Configure network
        if (config.getNetworkBridge() != null && config.getContainerIp() != null) {
            logger.info("  Configuring network: " + config.getContainerIp());
            sshDevice.executeCommand("lxc config device add " + config.getContainerName() + " eth0 nic " +
                    "nictype=bridged parent=" + config.getNetworkBridge() + " ipv4.address=" + config.getContainerIp());
        }

        // Configure port mapping
        if (config.getPortMapping() != null) {
            String[] parts = config.getPortMapping().split(":");
            if (parts.length == 2) {
                logger.info("  Configuring port mapping: " + config.getPortMapping());
                sshDevice.executeCommand("lxc config device add " + config.getContainerName() + " http-port proxy " +
                        "listen=tcp:0.0.0.0:" + parts[0] + " connect=tcp:127.0.0.1:" + parts[1]);
            }
        }

        // Configure storage quota
        if (config.getStorageQuota() != null) {
            logger.info("  Setting storage quota: " + config.getStorageQuota());
            sshDevice.executeCommand("lxc config device override " + config.getContainerName() + " root size=" + config.getStorageQuota());
        }

        logger.info("✓ Container created: " + config.getContainerName());
    }

    private void startContainer() throws Exception {
        logger.info("\n[6/7] Starting container...");

        sshDevice.executeCommand("lxc start " + config.getContainerName());

        // Wait for container to start
        Thread.sleep(3000);

        // Verify running
        String status = sshDevice.executeCommand("lxc list " + config.getContainerName() + " -c s --format csv");
        if (!status.toUpperCase().contains("RUNNING")) {
            throw new Exception("Container failed to start");
        }

        logger.info("✓ Container started successfully");
    }

    private void verifyDeployment() throws Exception {
        logger.info("\n[7/7] Verifying deployment...");

        // Check container status
        String containerInfo = sshDevice.executeCommand("lxc list " + config.getContainerName());
        logger.info("Container status:");
        logger.info(containerInfo);

        // If service port specified, wait and check
        if (config.getServicePort() != null) {
            logger.info("  Waiting for service to start (port " + config.getServicePort() + ")...");
            Thread.sleep(5000);

            // Try to check if port is listening inside container
            String portCheck = sshDevice.executeCommand(
                    "lxc exec " + config.getContainerName() + " -- netstat -tuln 2>/dev/null | grep :" + config.getServicePort() + " || echo 'NOT_LISTENING'"
            );

            if (portCheck.contains("NOT_LISTENING")) {
                logger.warning("⚠ Service port " + config.getServicePort() + " not listening yet");
                logger.warning("  Check container logs: lxc exec " + config.getContainerName() + " -- bash");
            } else {
                logger.info("✓ Service is listening on port " + config.getServicePort());
            }
        }

        logger.info("✓ Deployment verification completed");
    }

    private String formatBytes(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024) return String.format("%.1f MB", bytes / (1024.0 * 1024));
        return String.format("%.1f GB", bytes / (1024.0 * 1024 * 1024));
    }
}
