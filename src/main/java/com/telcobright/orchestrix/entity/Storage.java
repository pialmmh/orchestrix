package com.telcobright.orchestrix.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "storages")
public class Storage {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "storage_type")
    private StorageType storageType; // SAN, NAS, LOCAL, OBJECT

    @Column(name = "capacity_gb")
    private Long capacityGb;

    @Column(name = "used_gb")
    private Long usedGb = 0L;

    @Column(name = "available_gb")
    private Long availableGb;

    private String protocol; // NFS, iSCSI, S3, etc.

    @Column(name = "mount_path")
    private String mountPath;

    private String status; // ACTIVE, INACTIVE, MAINTENANCE

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cloud_id")
    @JsonBackReference
    private Cloud cloud;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "datacenter_id")
    private Datacenter datacenter;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (availableGb == null && capacityGb != null && usedGb != null) {
            availableGb = capacityGb - usedGb;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
        if (capacityGb != null && usedGb != null) {
            availableGb = capacityGb - usedGb;
        }
    }

    // Constructors
    public Storage() {}

    public Storage(String name, String description, StorageType storageType, Long capacityGb, 
                   String protocol, String mountPath, String status) {
        this.name = name;
        this.description = description;
        this.storageType = storageType;
        this.capacityGb = capacityGb;
        this.protocol = protocol;
        this.mountPath = mountPath;
        this.status = status;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public StorageType getStorageType() {
        return storageType;
    }

    public void setStorageType(StorageType storageType) {
        this.storageType = storageType;
    }

    public Long getCapacityGb() {
        return capacityGb;
    }

    public void setCapacityGb(Long capacityGb) {
        this.capacityGb = capacityGb;
    }

    public Long getUsedGb() {
        return usedGb;
    }

    public void setUsedGb(Long usedGb) {
        this.usedGb = usedGb;
    }

    public Long getAvailableGb() {
        return availableGb;
    }

    public void setAvailableGb(Long availableGb) {
        this.availableGb = availableGb;
    }

    public String getProtocol() {
        return protocol;
    }

    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    public String getMountPath() {
        return mountPath;
    }

    public void setMountPath(String mountPath) {
        this.mountPath = mountPath;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Cloud getCloud() {
        return cloud;
    }

    public void setCloud(Cloud cloud) {
        this.cloud = cloud;
    }

    public Datacenter getDatacenter() {
        return datacenter;
    }

    public void setDatacenter(Datacenter datacenter) {
        this.datacenter = datacenter;
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

    public enum StorageType {
        SAN,
        NAS,
        LOCAL,
        OBJECT
    }
}