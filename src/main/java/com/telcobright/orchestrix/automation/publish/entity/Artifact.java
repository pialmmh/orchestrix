package com.telcobright.orchestrix.automation.publish.entity;

import java.sql.Timestamp;

/**
 * Artifact entity
 * Represents a built artifact (LXC container image)
 */
public class Artifact {
    private Integer id;
    private String artifactType;    // e.g., 'lxc-container'
    private String name;            // e.g., 'go-id'
    private String version;         // e.g., 'v1'
    private String fileName;        // e.g., 'go-id-v1-1759653102.tar.gz'
    private Long fileSizeBytes;
    private String md5Hash;
    private String localPath;
    private Long buildTimestamp;
    private Timestamp createdAt;

    // Constructors
    public Artifact() {
    }

    public Artifact(String artifactType, String name, String version, String fileName,
                    Long fileSizeBytes, String md5Hash, String localPath, Long buildTimestamp) {
        this.artifactType = artifactType;
        this.name = name;
        this.version = version;
        this.fileName = fileName;
        this.fileSizeBytes = fileSizeBytes;
        this.md5Hash = md5Hash;
        this.localPath = localPath;
        this.buildTimestamp = buildTimestamp;
    }

    // Getters and Setters
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getArtifactType() {
        return artifactType;
    }

    public void setArtifactType(String artifactType) {
        this.artifactType = artifactType;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public Long getFileSizeBytes() {
        return fileSizeBytes;
    }

    public void setFileSizeBytes(Long fileSizeBytes) {
        this.fileSizeBytes = fileSizeBytes;
    }

    public String getMd5Hash() {
        return md5Hash;
    }

    public void setMd5Hash(String md5Hash) {
        this.md5Hash = md5Hash;
    }

    public String getLocalPath() {
        return localPath;
    }

    public void setLocalPath(String localPath) {
        this.localPath = localPath;
    }

    public Long getBuildTimestamp() {
        return buildTimestamp;
    }

    public void setBuildTimestamp(Long buildTimestamp) {
        this.buildTimestamp = buildTimestamp;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public String toString() {
        return "Artifact{" +
                "id=" + id +
                ", artifactType='" + artifactType + '\'' +
                ", name='" + name + '\'' +
                ", version='" + version + '\'' +
                ", fileName='" + fileName + '\'' +
                ", fileSizeBytes=" + fileSizeBytes +
                ", md5Hash='" + md5Hash + '\'' +
                ", buildTimestamp=" + buildTimestamp +
                '}';
    }
}
