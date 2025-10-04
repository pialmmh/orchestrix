package com.telcobright.orchestrix.automation.devices.server.linux.containerdeploy.base;

import com.telcobright.orchestrix.automation.devices.server.linux.base.AbstractLinuxAutomation;
import com.telcobright.orchestrix.automation.devices.server.linux.base.LinuxDistribution;
import com.telcobright.orchestrix.automation.core.device.SshDevice;

import java.util.Map;
import java.util.HashMap;
import java.io.File;
import java.net.URL;

/**
 * Abstract base implementation for container deployments
 * Provides common functionality across all container technologies
 */
public abstract class AbstractContainerDeployment extends AbstractLinuxAutomation
    implements ContainerDeploymentAutomation {

    protected final ContainerConfig config;
    protected final ContainerTechnology technology;

    protected AbstractContainerDeployment(ContainerConfig config,
                                         ContainerTechnology technology,
                                         boolean useSudo) {
        super(LinuxDistribution.UNKNOWN, useSudo);
        this.config = config;
        this.technology = technology;
    }

    @Override
    public boolean execute(SshDevice device) throws Exception {
        return deployContainer(device, config);
    }

    @Override
    public boolean verify(SshDevice device) throws Exception {
        if (!verifyPrerequisites(device)) {
            logger.warning("Prerequisites not met for " + technology);
            return false;
        }

        if (!containerExists(device, config.getContainerName())) {
            logger.warning("Container " + config.getContainerName() + " does not exist");
            return false;
        }

        ContainerStatus status = getContainerStatus(device, config.getContainerName());
        return status != null && status.isRunning();
    }

    @Override
    public boolean deployContainer(SshDevice device, ContainerConfig config) throws Exception {
        logger.info("Starting container deployment: " + config.getContainerName());

        // Step 1: Verify prerequisites
        if (!verifyPrerequisites(device)) {
            logger.severe("Prerequisites check failed");
            return false;
        }

        // Step 2: Prepare image
        String imagePath = prepareImage(device, config);
        if (imagePath == null) {
            logger.severe("Failed to prepare image");
            return false;
        }

        // Step 3: Check if container already exists
        if (containerExists(device, config.getContainerName())) {
            logger.info("Container already exists, removing old instance");
            if (!removeContainer(device, config.getContainerName())) {
                logger.severe("Failed to remove existing container");
                return false;
            }
        }

        // Step 4: Import/create container
        if (!importContainer(device, imagePath, config.getContainerName())) {
            logger.severe("Failed to import container");
            return false;
        }

        // Step 5: Configure container
        if (!configureContainer(device, config)) {
            logger.severe("Failed to configure container");
            return false;
        }

        // Step 6: Start container if requested
        if (config.isAutoStart()) {
            if (!startContainer(device, config.getContainerName())) {
                logger.severe("Failed to start container");
                return false;
            }
        }

        logger.info("Container deployment completed successfully");
        return true;
    }

    /**
     * Prepare container image (download or verify local)
     */
    protected String prepareImage(SshDevice device, ContainerConfig config) throws Exception {
        if (config.getImagePath() != null) {
            // Local image path
            String checkCmd = String.format("test -f %s && echo exists", config.getImagePath());
            String result = executeCommand(device, checkCmd);

            if (result != null && result.contains("exists")) {
                logger.info("Using local image: " + config.getImagePath());
                return config.getImagePath();
            } else {
                logger.severe("Local image not found: " + config.getImagePath());
                return null;
            }
        } else if (config.getImageUrl() != null) {
            // Download image from URL
            String fileName = new File(new URL(config.getImageUrl()).getPath()).getName();
            String targetPath = "/tmp/" + fileName;

            logger.info("Downloading image from: " + config.getImageUrl());
            String downloadCmd = String.format("wget -O %s %s", targetPath, config.getImageUrl());
            String result = executeCommand(device, downloadCmd);

            if (result != null && !result.contains("ERROR")) {
                // Verify checksum if provided
                if (config.getImageChecksum() != null) {
                    if (!verifyChecksum(device, targetPath, config.getImageChecksum())) {
                        logger.severe("Checksum verification failed");
                        executeCommand(device, "rm -f " + targetPath);
                        return null;
                    }
                }
                return targetPath;
            } else {
                logger.severe("Failed to download image");
                return null;
            }
        }

        logger.severe("No image source specified");
        return null;
    }

    /**
     * Verify file checksum
     */
    protected boolean verifyChecksum(SshDevice device, String filePath, String expectedChecksum)
        throws Exception {

        String checksumType = "sha256";
        if (expectedChecksum.startsWith("md5:")) {
            checksumType = "md5";
            expectedChecksum = expectedChecksum.substring(4);
        } else if (expectedChecksum.startsWith("sha256:")) {
            expectedChecksum = expectedChecksum.substring(7);
        }

        String cmd = String.format("%ssum %s | awk '{print $1}'", checksumType, filePath);
        String actualChecksum = executeCommand(device, cmd);

        if (actualChecksum != null) {
            actualChecksum = actualChecksum.trim();
            boolean match = actualChecksum.equalsIgnoreCase(expectedChecksum);

            if (match) {
                logger.info("Checksum verified successfully");
            } else {
                logger.severe(String.format("Checksum mismatch: expected=%s, actual=%s",
                    expectedChecksum, actualChecksum));
            }
            return match;
        }

        return false;
    }

    /**
     * Configure container with settings from config
     */
    protected abstract boolean configureContainer(SshDevice device, ContainerConfig config)
        throws Exception;

    /**
     * Apply network configuration to container
     */
    protected boolean configureNetwork(SshDevice device, ContainerConfig config) throws Exception {
        // Common network configuration logic
        // Subclasses can override for technology-specific implementation
        return true;
    }

    /**
     * Apply storage configuration to container
     */
    protected boolean configureStorage(SshDevice device, ContainerConfig config) throws Exception {
        // Common storage configuration logic
        // Subclasses can override for technology-specific implementation
        return true;
    }

    /**
     * Apply resource limits to container
     */
    protected boolean configureResourceLimits(SshDevice device, ContainerConfig config)
        throws Exception {
        // Common resource limits configuration
        // Subclasses can override for technology-specific implementation
        return true;
    }

    @Override
    public ContainerTechnology getContainerTechnology() {
        return technology;
    }

    @Override
    public Map<String, String> getStatus(SshDevice device) {
        Map<String, String> status = new HashMap<>();
        try {
            status.put("technology", technology.name());
            status.put("container", config.getContainerName());

            if (containerExists(device, config.getContainerName())) {
                ContainerStatus containerStatus = getContainerStatus(device,
                    config.getContainerName());
                if (containerStatus != null) {
                    status.put("state", containerStatus.getState().toString());
                    status.put("ip", containerStatus.getIpAddress());
                }
            } else {
                status.put("state", "NOT_EXISTS");
            }
        } catch (Exception e) {
            status.put("error", e.getMessage());
        }
        return status;
    }

    @Override
    protected boolean isPackageInstalled(SshDevice device, String packageName) throws Exception {
        String result = executeCommand(device, "which " + packageName);
        return result != null && !result.isEmpty();
    }

    @Override
    protected String getPackageManagerCommand() {
        return "apt";
    }

    @Override
    public String getName() {
        return technology.getDescription() + " Deployment";
    }

    @Override
    public String getDescription() {
        return "Deploy container using " + technology.getDescription();
    }
}