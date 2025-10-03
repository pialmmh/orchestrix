package com.telcobright.orchestrix.images.core;

import com.telcobright.orchestrix.automation.core.BaseAutomation;
import com.telcobright.orchestrix.automation.api.model.AutomationConfig;
import com.telcobright.orchestrix.automation.api.model.CommandResult;
import com.telcobright.orchestrix.images.model.ContainerConfig;
import com.telcobright.orchestrix.images.model.ImageBuildContext;
import com.telcobright.orchestrix.images.model.ImageBuildContext.BuildPhase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Base class for all container image builders
 * Uses the refactored model structure
 */
public abstract class BaseImageBuilder extends BaseAutomation {

    protected static final Logger logger = LoggerFactory.getLogger(BaseImageBuilder.class);
    protected final ContainerConfig containerConfig;
    protected final ImageBuildContext buildContext;

    public BaseImageBuilder(ContainerConfig containerConfig, AutomationConfig automationConfig) {
        super(automationConfig);
        this.containerConfig = containerConfig;
        this.buildContext = new ImageBuildContext(containerConfig);
    }

    @Override
    public boolean execute() {
        logger.info("===========================================");
        logger.info("Starting {} Build", getName());
        logger.info("Version: {}", containerConfig.getVersion());
        logger.info("Base Image: {}", containerConfig.getBaseImage());
        logger.info("===========================================");

        try {
            // Execute build phases
            for (BuildPhase phase : BuildPhase.values()) {
                if (!executePhase(phase)) {
                    return false;
                }
            }

            buildContext.setEndTime(System.currentTimeMillis());
            logger.info("Build completed successfully in {}ms", buildContext.getBuildDuration());
            return true;

        } catch (Exception e) {
            logger.error("Build failed at phase {}: {}",
                buildContext.getCurrentPhase(), e.getMessage(), e);
            handleBuildFailure();
            return false;
        }
    }

    /**
     * Execute a single build phase
     */
    protected boolean executePhase(BuildPhase phase) {
        // Skip if already completed (useful for retry)
        if (buildContext.isPhaseCompleted(phase)) {
            logger.info("[{}] Already completed, skipping", phase.getDescription());
            return true;
        }

        buildContext.setCurrentPhase(phase);
        logger.info("=== PHASE: {} ===", phase.getDescription());

        try {
            boolean success = false;

            switch (phase) {
                case PREREQUISITES:
                    success = validatePrerequisites();
                    break;
                case CONTAINER_CREATE:
                    success = createContainer();
                    break;
                case NETWORK_SETUP:
                    success = setupNetwork();
                    break;
                case PACKAGE_INSTALL:
                    success = installPackages();
                    break;
                case SERVICE_SETUP:
                    success = setupServices();
                    break;
                case APPLICATION_DEPLOY:
                    success = deployApplication();
                    break;
                case VERIFICATION:
                    success = verifyInstallation();
                    break;
                case OPTIMIZATION:
                    success = optimizeImage();
                    break;
                case IMAGE_PUBLISH:
                    success = publishImage();
                    break;
                case CLEANUP:
                    success = cleanup();
                    break;
            }

            if (success) {
                buildContext.markPhaseCompleted(phase);
                logger.info("✓ {} completed", phase.getDescription());
                return true;
            } else {
                throw new BuildException("Phase failed: " + phase.getDescription());
            }

        } catch (Exception e) {
            buildContext.markPhaseFailed(phase, e.getMessage());

            if (shouldRetryPhase(phase)) {
                logger.warn("Phase {} failed, attempting retry", phase.getDescription());
                return retryPhase(phase);
            }

            throw new BuildException("Phase failed: " + phase.getDescription(), e);
        }
    }

    /**
     * Retry a failed phase
     */
    protected boolean retryPhase(BuildPhase phase) {
        int maxRetries = config.getMaxRetries();

        for (int i = 1; i <= maxRetries; i++) {
            logger.info("Retry attempt {} of {} for phase: {}",
                i, maxRetries, phase.getDescription());

            try {
                Thread.sleep(config.getRetryDelayMs());

                // Call phase-specific retry preparation
                preparePhaseRetry(phase);

                // Re-execute the phase
                if (executePhase(phase)) {
                    return true;
                }

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                logger.error("Retry {} failed: {}", i, e.getMessage());
            }
        }

        return false;
    }

    /**
     * Prepare for phase retry (override for specific cleanup)
     */
    protected void preparePhaseRetry(BuildPhase phase) {
        // Default: no special preparation
        // Subclasses can override for phase-specific cleanup
    }

    /**
     * Determine if a phase should be retried
     */
    protected boolean shouldRetryPhase(BuildPhase phase) {
        // Don't retry cleanup or publish by default
        return phase != BuildPhase.CLEANUP && phase != BuildPhase.IMAGE_PUBLISH;
    }

    /**
     * Handle build failure
     */
    protected void handleBuildFailure() {
        if (containerConfig.isCleanupOnFailure()) {
            logger.info("Cleaning up after failure...");
            try {
                cleanup();
            } catch (Exception e) {
                logger.error("Cleanup failed: {}", e.getMessage());
            }
        }
    }

    // Abstract methods for subclasses to implement
    protected abstract boolean validatePrerequisites();
    protected abstract boolean createContainer();
    protected abstract boolean setupNetwork();
    protected abstract boolean installPackages();
    protected abstract boolean setupServices();
    protected abstract boolean deployApplication();
    protected abstract boolean verifyInstallation();
    protected abstract boolean optimizeImage();
    protected abstract boolean publishImage();
    protected abstract boolean cleanup();

    /**
     * Helper method to execute container commands
     */
    protected CommandResult containerExec(String command) {
        String fullCommand;

        if (containerConfig.isLxc()) {
            fullCommand = String.format("lxc exec %s -- bash -c '%s'",
                containerConfig.getContainerName(), command.replace("'", "'\\''"));
        } else if (containerConfig.isDocker()) {
            fullCommand = String.format("docker exec %s bash -c '%s'",
                containerConfig.getContainerName(), command.replace("'", "'\\''"));
        } else {
            throw new UnsupportedOperationException("Unknown container type: " +
                containerConfig.getContainerType());
        }

        return executeCommand(fullCommand);
    }

    /**
     * Helper method to copy files to container
     */
    protected void copyToContainer(String localPath, String containerPath) {
        String command;

        if (containerConfig.isLxc()) {
            command = String.format("lxc file push %s %s%s",
                localPath, containerConfig.getContainerName(), containerPath);
        } else if (containerConfig.isDocker()) {
            command = String.format("docker cp %s %s:%s",
                localPath, containerConfig.getContainerName(), containerPath);
        } else {
            throw new UnsupportedOperationException("Unknown container type: " +
                containerConfig.getContainerType());
        }

        executeCommand(command);
    }

    /**
     * Build exception
     */
    public static class BuildException extends RuntimeException {
        public BuildException(String message) {
            super(message);
        }

        public BuildException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}