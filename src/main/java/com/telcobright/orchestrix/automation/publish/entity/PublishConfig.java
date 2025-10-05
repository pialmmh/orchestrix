package com.telcobright.orchestrix.automation.publish.entity;

import com.telcobright.orchestrix.automation.core.executor.ExecutionMode;

import java.util.ArrayList;
import java.util.List;

/**
 * Multi-target publish configuration
 * Supports publishing to multiple locations in parallel or sequential mode
 */
public class PublishConfig {
    // Artifact information
    private String artifactType;
    private String artifactName;
    private String artifactVersion;
    private String artifactFile;
    private String artifactPath;
    private long buildTimestamp;

    // Execution mode
    private ExecutionMode executionMode = ExecutionMode.SEQUENTIAL;

    // Multiple publish locations
    private List<PublishLocation> publishLocations = new ArrayList<>();

    // Database connection (optional)
    private String dbHost;
    private int dbPort;
    private String dbName;
    private String dbUser;
    private String dbPassword;

    public PublishConfig() {
    }

    // Getters and Setters
    public String getArtifactType() {
        return artifactType;
    }

    public void setArtifactType(String artifactType) {
        this.artifactType = artifactType;
    }

    public String getArtifactName() {
        return artifactName;
    }

    public void setArtifactName(String artifactName) {
        this.artifactName = artifactName;
    }

    public String getArtifactVersion() {
        return artifactVersion;
    }

    public void setArtifactVersion(String artifactVersion) {
        this.artifactVersion = artifactVersion;
    }

    public String getArtifactFile() {
        return artifactFile;
    }

    public void setArtifactFile(String artifactFile) {
        this.artifactFile = artifactFile;
    }

    public String getArtifactPath() {
        return artifactPath;
    }

    public void setArtifactPath(String artifactPath) {
        this.artifactPath = artifactPath;
    }

    public long getBuildTimestamp() {
        return buildTimestamp;
    }

    public void setBuildTimestamp(long buildTimestamp) {
        this.buildTimestamp = buildTimestamp;
    }

    public ExecutionMode getExecutionMode() {
        return executionMode;
    }

    public void setExecutionMode(ExecutionMode executionMode) {
        this.executionMode = executionMode;
    }

    public List<PublishLocation> getPublishLocations() {
        return publishLocations;
    }

    public void setPublishLocations(List<PublishLocation> publishLocations) {
        this.publishLocations = publishLocations;
    }

    public void addPublishLocation(PublishLocation location) {
        this.publishLocations.add(location);
    }

    public String getDbHost() {
        return dbHost;
    }

    public void setDbHost(String dbHost) {
        this.dbHost = dbHost;
    }

    public int getDbPort() {
        return dbPort;
    }

    public void setDbPort(int dbPort) {
        this.dbPort = dbPort;
    }

    public String getDbName() {
        return dbName;
    }

    public void setDbName(String dbName) {
        this.dbName = dbName;
    }

    public String getDbUser() {
        return dbUser;
    }

    public void setDbUser(String dbUser) {
        this.dbUser = dbUser;
    }

    public String getDbPassword() {
        return dbPassword;
    }

    public void setDbPassword(String dbPassword) {
        this.dbPassword = dbPassword;
    }

    @Override
    public String toString() {
        return "PublishConfig{" +
                "artifactName='" + artifactName + '\'' +
                ", artifactVersion='" + artifactVersion + '\'' +
                ", executionMode=" + executionMode +
                ", publishLocations=" + publishLocations.size() +
                '}';
    }
}
