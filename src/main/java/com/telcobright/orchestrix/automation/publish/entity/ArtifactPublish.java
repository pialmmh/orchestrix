package com.telcobright.orchestrix.automation.publish.entity;

import java.sql.Timestamp;

/**
 * ArtifactPublish entity
 * Represents a publish record of an artifact to a remote location
 */
public class ArtifactPublish {
    private Integer id;
    private Integer artifactId;
    private Integer publishLocationId;
    private String remotePath;
    private Timestamp publishedAt;
    private Boolean verified;
    private Timestamp verificationTimestamp;

    // Constructors
    public ArtifactPublish() {
    }

    public ArtifactPublish(Integer artifactId, Integer publishLocationId, String remotePath) {
        this.artifactId = artifactId;
        this.publishLocationId = publishLocationId;
        this.remotePath = remotePath;
        this.verified = false;
    }

    // Getters and Setters
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getArtifactId() {
        return artifactId;
    }

    public void setArtifactId(Integer artifactId) {
        this.artifactId = artifactId;
    }

    public Integer getPublishLocationId() {
        return publishLocationId;
    }

    public void setPublishLocationId(Integer publishLocationId) {
        this.publishLocationId = publishLocationId;
    }

    public String getRemotePath() {
        return remotePath;
    }

    public void setRemotePath(String remotePath) {
        this.remotePath = remotePath;
    }

    public Timestamp getPublishedAt() {
        return publishedAt;
    }

    public void setPublishedAt(Timestamp publishedAt) {
        this.publishedAt = publishedAt;
    }

    public Boolean getVerified() {
        return verified;
    }

    public void setVerified(Boolean verified) {
        this.verified = verified;
    }

    public Timestamp getVerificationTimestamp() {
        return verificationTimestamp;
    }

    public void setVerificationTimestamp(Timestamp verificationTimestamp) {
        this.verificationTimestamp = verificationTimestamp;
    }

    @Override
    public String toString() {
        return "ArtifactPublish{" +
                "id=" + id +
                ", artifactId=" + artifactId +
                ", publishLocationId=" + publishLocationId +
                ", remotePath='" + remotePath + '\'' +
                ", publishedAt=" + publishedAt +
                ", verified=" + verified +
                '}';
    }
}
