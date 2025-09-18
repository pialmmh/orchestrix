package com.telcobright.orchestrix.entity;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "os_version")
public class OSVersion {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "os_type", nullable = false, length = 50)
    private String osType;

    @Column(nullable = false, length = 100)
    private String distribution;

    @Column(nullable = false, length = 50)
    private String version;

    @Column(length = 20)
    private String architecture = "amd64";

    @Column(name = "display_name", nullable = false, length = 200)
    private String displayName;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // Constructors
    public OSVersion() {}

    public OSVersion(String osType, String distribution, String version, String displayName) {
        this.osType = osType;
        this.distribution = distribution;
        this.version = version;
        this.displayName = displayName;
    }

    public OSVersion(String osType, String distribution, String version, String architecture, String displayName) {
        this.osType = osType;
        this.distribution = distribution;
        this.version = version;
        this.architecture = architecture;
        this.displayName = displayName;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getOsType() {
        return osType;
    }

    public void setOsType(String osType) {
        this.osType = osType;
    }

    public String getDistribution() {
        return distribution;
    }

    public void setDistribution(String distribution) {
        this.distribution = distribution;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getArchitecture() {
        return architecture;
    }

    public void setArchitecture(String architecture) {
        this.architecture = architecture;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}