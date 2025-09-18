package com.telcobright.orchestrix.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.List;
import java.util.ArrayList;

@Entity
@Table(name = "container")
public class Container {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "container_type")
    private ContainerType containerType; // LXC, LXD, DOCKER, PODMAN, CONTAINERD, KUBERNETES

    @Column(name = "container_id")
    private String containerId;

    private String image;

    @Column(name = "image_version")
    private String imageVersion;

    private String status; // RUNNING, STOPPED, PAUSED, RESTARTING

    @Column(name = "ip_address")
    @Deprecated // Kept for backward compatibility, use ipAddresses instead
    private String ipAddress;
    
    // Multiple IP addresses support
    @OneToMany(mappedBy = "container", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonIgnoreProperties({"compute", "networkDevice", "container", "hibernateLazyInitializer", "handler"})
    private List<IPAddress> ipAddresses = new ArrayList<>();

    @Column(name = "exposed_ports")
    private String exposedPorts;

    @Column(name = "environment_vars")
    private String environmentVars;

    @Column(name = "mount_points")
    private String mountPoints;

    @Column(name = "cpu_limit")
    private String cpuLimit;

    @Column(name = "memory_limit")
    private String memoryLimit;

    @Column(name = "auto_start")
    private Boolean autoStart = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "compute_id")
    @JsonBackReference
    private Compute compute;

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
    public Container() {}

    public Container(String name, String description, ContainerType containerType, String containerId,
                     String image, String imageVersion, String status, String ipAddress, Boolean autoStart) {
        this.name = name;
        this.description = description;
        this.containerType = containerType;
        this.containerId = containerId;
        this.image = image;
        this.imageVersion = imageVersion;
        this.status = status;
        this.ipAddress = ipAddress;
        this.autoStart = autoStart;
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

    public ContainerType getContainerType() {
        return containerType;
    }

    public void setContainerType(ContainerType containerType) {
        this.containerType = containerType;
    }

    public String getContainerId() {
        return containerId;
    }

    public void setContainerId(String containerId) {
        this.containerId = containerId;
    }

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }

    public String getImageVersion() {
        return imageVersion;
    }

    public void setImageVersion(String imageVersion) {
        this.imageVersion = imageVersion;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public String getExposedPorts() {
        return exposedPorts;
    }

    public void setExposedPorts(String exposedPorts) {
        this.exposedPorts = exposedPorts;
    }

    public String getEnvironmentVars() {
        return environmentVars;
    }

    public void setEnvironmentVars(String environmentVars) {
        this.environmentVars = environmentVars;
    }

    public String getMountPoints() {
        return mountPoints;
    }

    public void setMountPoints(String mountPoints) {
        this.mountPoints = mountPoints;
    }

    public String getCpuLimit() {
        return cpuLimit;
    }

    public void setCpuLimit(String cpuLimit) {
        this.cpuLimit = cpuLimit;
    }

    public String getMemoryLimit() {
        return memoryLimit;
    }

    public void setMemoryLimit(String memoryLimit) {
        this.memoryLimit = memoryLimit;
    }

    public Boolean getAutoStart() {
        return autoStart;
    }

    public void setAutoStart(Boolean autoStart) {
        this.autoStart = autoStart;
    }

    public Compute getCompute() {
        return compute;
    }

    public void setCompute(Compute compute) {
        this.compute = compute;
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

    public enum ContainerType {
        LXC,
        LXD,
        DOCKER,
        PODMAN,
        CONTAINERD,
        KUBERNETES
    }
}