package com.telcobright.orchestrix.images.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Build context for creating container images
 * Tracks build state and allows retry from failure points
 */
public class ImageBuildContext {
    private ContainerConfig containerConfig;
    private String buildId;
    private BuildPhase currentPhase;
    private List<BuildPhase> completedPhases = new ArrayList<>();
    private List<BuildPhase> failedPhases = new ArrayList<>();
    private String lastError;
    private int retryCount = 0;
    private long startTime;
    private long endTime;

    // Build phases
    public enum BuildPhase {
        PREREQUISITES("Prerequisites Check"),
        CONTAINER_CREATE("Container Creation"),
        NETWORK_SETUP("Network Configuration"),
        PACKAGE_INSTALL("Package Installation"),
        SERVICE_SETUP("Service Configuration"),
        APPLICATION_DEPLOY("Application Deployment"),
        VERIFICATION("Installation Verification"),
        OPTIMIZATION("Size Optimization"),
        IMAGE_PUBLISH("Image Publishing"),
        CLEANUP("Cleanup");

        private final String description;

        BuildPhase(String description) {
            this.description = description;
        }

        public String getDescription() { return description; }
    }

    // Constructor
    public ImageBuildContext(ContainerConfig containerConfig) {
        this.containerConfig = containerConfig;
        this.buildId = generateBuildId();
        this.startTime = System.currentTimeMillis();
    }

    // Helper methods
    private String generateBuildId() {
        return containerConfig.getContainerName() + "-" + System.currentTimeMillis();
    }

    public void markPhaseCompleted(BuildPhase phase) {
        if (!completedPhases.contains(phase)) {
            completedPhases.add(phase);
        }
        failedPhases.remove(phase);
    }

    public void markPhaseFailed(BuildPhase phase, String error) {
        if (!failedPhases.contains(phase)) {
            failedPhases.add(phase);
        }
        this.lastError = error;
        this.currentPhase = phase;
    }

    public boolean isPhaseCompleted(BuildPhase phase) {
        return completedPhases.contains(phase);
    }

    public boolean canRetryFromPhase(BuildPhase phase) {
        // Can retry from a phase if all previous phases are completed
        for (BuildPhase p : BuildPhase.values()) {
            if (p == phase) {
                return true;
            }
            if (!completedPhases.contains(p)) {
                return false;
            }
        }
        return true;
    }

    public void incrementRetryCount() {
        this.retryCount++;
    }

    public long getBuildDuration() {
        if (endTime > 0) {
            return endTime - startTime;
        }
        return System.currentTimeMillis() - startTime;
    }

    public double getProgress() {
        return (double) completedPhases.size() / BuildPhase.values().length * 100;
    }

    // Getters and Setters
    public ContainerConfig getContainerConfig() { return containerConfig; }
    public void setContainerConfig(ContainerConfig containerConfig) {
        this.containerConfig = containerConfig;
    }

    public String getBuildId() { return buildId; }

    public BuildPhase getCurrentPhase() { return currentPhase; }
    public void setCurrentPhase(BuildPhase currentPhase) { this.currentPhase = currentPhase; }

    public List<BuildPhase> getCompletedPhases() { return completedPhases; }
    public List<BuildPhase> getFailedPhases() { return failedPhases; }

    public String getLastError() { return lastError; }
    public void setLastError(String lastError) { this.lastError = lastError; }

    public int getRetryCount() { return retryCount; }

    public long getStartTime() { return startTime; }
    public long getEndTime() { return endTime; }
    public void setEndTime(long endTime) { this.endTime = endTime; }

    @Override
    public String toString() {
        return String.format("BuildContext[id=%s, phase=%s, progress=%.1f%%, retries=%d]",
            buildId, currentPhase, getProgress(), retryCount);
    }
}