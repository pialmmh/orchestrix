package com.telcobright.orchestrix.automation.publish.entity;

import java.sql.Timestamp;

/**
 * Publish location entity
 * Represents a remote storage location for artifacts (Google Drive via rclone)
 */
public class PublishLocation {
    private Integer id;
    private String name;
    private String type;  // Currently only 'gdrive'
    private String rcloneRemote;
    private String rcloneTargetDir;
    private Boolean isActive;
    private Timestamp createdAt;
    private Timestamp updatedAt;

    // Constructors
    public PublishLocation() {
    }

    public PublishLocation(String name, String type, String rcloneRemote, String rcloneTargetDir) {
        this.name = name;
        this.type = type;
        this.rcloneRemote = rcloneRemote;
        this.rcloneTargetDir = rcloneTargetDir;
        this.isActive = true;
    }

    // Getters and Setters
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getRcloneRemote() {
        return rcloneRemote;
    }

    public void setRcloneRemote(String rcloneRemote) {
        this.rcloneRemote = rcloneRemote;
    }

    public String getRcloneTargetDir() {
        return rcloneTargetDir;
    }

    public void setRcloneTargetDir(String rcloneTargetDir) {
        this.rcloneTargetDir = rcloneTargetDir;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }

    public Timestamp getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Timestamp updatedAt) {
        this.updatedAt = updatedAt;
    }

    @Override
    public String toString() {
        return "PublishLocation{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", type='" + type + '\'' +
                ", rcloneRemote='" + rcloneRemote + '\'' +
                ", rcloneTargetDir='" + rcloneTargetDir + '\'' +
                ", isActive=" + isActive +
                '}';
    }
}
