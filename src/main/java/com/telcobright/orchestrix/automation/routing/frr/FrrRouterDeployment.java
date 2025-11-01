package com.telcobright.orchestrix.automation.routing.frr;

import com.telcobright.orchestrix.automation.core.device.SshDevice;
import com.telcobright.orchestrix.automation.core.device.impl.RemoteSshDevice;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * FRR Router Deployment Automation
 *
 * Deploys FRR router containers to remote hosts using host networking mode.
 *
 * Deployment Flow:
 * 1. Connect to target host via SSH
 * 2. Transfer container image tarball + MD5
 * 3. Verify MD5 checksum
 * 4. Transfer config file and launch script
 * 5. Import LXC image
 * 6. Launch container (host networking mode)
 * 7. Verify BGP status
 *
 * Key Features:
 * - Host networking mode (no port forwarding needed)
 * - Auto-detection of host's external IP for ROUTER_ID
 * - Minimal configuration required
 * - BGP peering verification
 */
public class FrrRouterDeployment {

    private static final Logger log = LoggerFactory.getLogger(FrrRouterDeployment.class);

    private final String orchestrixPath;
    private final String frrRouterPath;
    private final String artifactPath;

    public FrrRouterDeployment(String orchestrixPath) {
        this.orchestrixPath = orchestrixPath;
        this.frrRouterPath = orchestrixPath + "/images/lxc/frr-router";
        this.artifactPath = orchestrixPath + "/images/lxc/frr-router-v.1.0.0/generated/artifact";
    }

    /**
     * Deploy FRR router to a single node
     */
    public FrrDeploymentResult deployToNode(String host, int port, String user, String configFilePath) {
        log.info("========================================");
        log.info("FRR Router Deployment");
        log.info("========================================");
        log.info("Target: {}@{}:{}", user, host, port);
        log.info("Config: {}", configFilePath);
        log.info("");

        FrrDeploymentResult result = new FrrDeploymentResult(host);
        SshDevice ssh = null;

        try {
            // Parse config
            log.info("Parsing configuration...");
            FrrRouterConfig config = new FrrRouterConfig(configFilePath);
            log.info("Container: {}", config.getContainerName());
            log.info("BGP ASN: {}", config.getBgpAsn());
            log.info("BGP Neighbors: {}", config.getBgpNeighbors());
            log.info("Networks to announce: {}", config.getBgpNetworks());
            log.info("");

            // Connect to host
            log.info("Connecting to host...");
            ssh = new RemoteSshDevice(host, port, user, null);
            ssh.connect();
            log.info("✓ Connected");
            log.info("");

            // Find container tarball
            log.info("Locating container image...");
            String tarballPath = findContainerTarball();
            String md5Path = tarballPath + ".md5";
            log.info("✓ Found: {}", tarballPath);
            log.info("");

            // Create remote deployment directory
            log.info("Creating remote deployment directory...");
            ssh.execute("mkdir -p /tmp/frr-router-deployment");
            log.info("✓ Directory created");
            log.info("");

            // Transfer files
            transferFiles(ssh, tarballPath, md5Path, configFilePath);

            // Verify MD5
            verifyMd5(ssh, tarballPath);

            // Import LXC image
            importLxcImage(ssh, config);

            // Launch container
            launchContainer(ssh, config);

            // Wait for container to be ready
            log.info("Waiting for container to initialize...");
            Thread.sleep(5000);
            log.info("");

            // Verify deployment
            verifyDeployment(ssh, config);

            result.setSuccess(true);
            result.setMessage("Deployment successful");
            result.setContainerName(config.getContainerName());

            log.info("");
            log.info("========================================");
            log.info("✓ Deployment Complete");
            log.info("========================================");
            log.info("");

        } catch (Exception e) {
            log.error("Deployment failed", e);
            result.setSuccess(false);
            result.setMessage("Deployment failed: " + e.getMessage());
        } finally {
            if (ssh != null && ssh.isConnected()) {
                ssh.disconnect();
            }
        }

        return result;
    }

    /**
     * Find the latest container tarball
     */
    private String findContainerTarball() throws IOException {
        Path artifactDir = Paths.get(artifactPath);
        if (!Files.exists(artifactDir)) {
            throw new IOException("Artifact directory not found: " + artifactPath);
        }

        File[] tarballs = artifactDir.toFile().listFiles((dir, name) ->
            name.endsWith(".tar.gz") && !name.endsWith(".md5")
        );

        if (tarballs == null || tarballs.length == 0) {
            throw new IOException("No container tarball found in: " + artifactPath);
        }

        // Return the most recent tarball
        File latestTarball = tarballs[0];
        for (File tarball : tarballs) {
            if (tarball.lastModified() > latestTarball.lastModified()) {
                latestTarball = tarball;
            }
        }

        return latestTarball.getAbsolutePath();
    }

    /**
     * Transfer files to remote host
     */
    private void transferFiles(SshDevice ssh, String tarballPath, String md5Path, String configPath) throws Exception {
        log.info("Transferring files...");

        // Transfer tarball
        File tarballFile = new File(tarballPath);
        log.info("  Transferring container image ({} MB)...", tarballFile.length() / 1024 / 1024);
        ssh.uploadFile(tarballPath, "/tmp/frr-router-deployment/" + tarballFile.getName());

        // Transfer MD5
        log.info("  Transferring MD5 hash...");
        ssh.uploadFile(md5Path, "/tmp/frr-router-deployment/" + new File(md5Path).getName());

        // Transfer config
        log.info("  Transferring configuration...");
        ssh.uploadFile(configPath, "/tmp/frr-router-deployment/launch-config.conf");

        // Transfer launch script
        log.info("  Transferring launch script...");
        String launchScriptPath = frrRouterPath + "/launchFrrRouter.sh";
        ssh.uploadFile(launchScriptPath, "/tmp/frr-router-deployment/launchFrrRouter.sh");

        log.info("✓ Files transferred");
        log.info("");
    }

    /**
     * Verify MD5 checksum
     */
    private void verifyMd5(SshDevice ssh, String tarballPath) throws Exception {
        log.info("Verifying MD5 checksum...");
        String tarballName = new File(tarballPath).getName();
        String md5Name = tarballName + ".md5";

        String result = ssh.execute("cd /tmp/frr-router-deployment && md5sum -c " + md5Name);
        if (!result.contains("OK")) {
            throw new IOException("MD5 verification failed!");
        }
        log.info("✓ MD5 verified");
        log.info("");
    }

    /**
     * Import LXC image
     */
    private void importLxcImage(SshDevice ssh, FrrRouterConfig config) throws Exception {
        log.info("Importing LXC image...");

        // Delete existing image if present
        ssh.execute("sudo lxc image delete " + config.getBaseImage() + " 2>/dev/null || true");

        // Import new image
        String importCmd = "cd /tmp/frr-router-deployment && " +
                "TARBALL=$(ls *.tar.gz | grep -v '.md5') && " +
                "sudo lxc image import \"$TARBALL\" --alias " + config.getBaseImage();
        ssh.execute(importCmd);

        log.info("✓ Image imported");
        log.info("");
    }

    /**
     * Launch container
     */
    private void launchContainer(SshDevice ssh, FrrRouterConfig config) throws Exception {
        log.info("Launching FRR router container...");

        // Delete existing container if present
        ssh.execute("sudo lxc delete --force " + config.getContainerName() + " 2>/dev/null || true");

        // Make launch script executable and run it
        String launchCmd = "cd /tmp/frr-router-deployment && " +
                "chmod +x launchFrrRouter.sh && " +
                "sudo bash ./launchFrrRouter.sh launch-config.conf";
        ssh.execute(launchCmd);

        log.info("✓ Container launched");
        log.info("");
    }

    /**
     * Verify deployment
     */
    private void verifyDeployment(SshDevice ssh, FrrRouterConfig config) throws Exception {
        log.info("Verifying deployment...");

        // Check container status
        String status = ssh.execute("sudo lxc list " + config.getContainerName() + " --format csv -c ns");
        if (!status.contains("RUNNING")) {
            throw new Exception("Container is not running");
        }
        log.info("✓ Container is running");

        // Check BGP status
        if (config.isBgpEnabled()) {
            log.info("Checking BGP status...");
            String bgpSummary = ssh.execute("sudo lxc exec " + config.getContainerName() +
                    " -- vtysh -c 'show ip bgp summary'");
            log.info("BGP Summary:");
            log.info(bgpSummary);
        }

        log.info("✓ Deployment verified");
    }
}
