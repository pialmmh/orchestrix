package com.telcobright.orchestrix.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "computes")
public class Compute {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "node_type", length = 50)
    private NodeType nodeType; // dedicated_server, vm

    private String hostname;

    @Column(name = "ip_address")
    private String ipAddress;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "os_version_id")
    private OSVersion osVersion;

    @Column(name = "cpu_cores")
    private Integer cpuCores;

    @Column(name = "memory_gb")
    private Integer memoryGb;

    @Column(name = "disk_gb")
    private Integer diskGb;

    private String status; // ACTIVE, INACTIVE, MAINTENANCE

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cloud_id")
    @JsonBackReference
    private Cloud cloud;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "datacenter_id")
    private Datacenter datacenter;

    @OneToMany(mappedBy = "compute", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonManagedReference
    private List<Container> containers;

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
    public Compute() {}

    public Compute(String name, String description, NodeType nodeType, String hostname, 
                   String ipAddress, OSVersion osVersion, Integer cpuCores, Integer memoryGb, 
                   Integer diskGb, String status) {
        this.name = name;
        this.description = description;
        this.nodeType = nodeType;
        this.hostname = hostname;
        this.ipAddress = ipAddress;
        this.osVersion = osVersion;
        this.cpuCores = cpuCores;
        this.memoryGb = memoryGb;
        this.diskGb = diskGb;
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

    public NodeType getNodeType() {
        return nodeType;
    }

    public void setNodeType(NodeType nodeType) {
        this.nodeType = nodeType;
    }

    public String getHostname() {
        return hostname;
    }

    public void setHostname(String hostname) {
        this.hostname = hostname;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public OSVersion getOsVersion() {
        return osVersion;
    }

    public void setOsVersion(OSVersion osVersion) {
        this.osVersion = osVersion;
    }

    public Integer getCpuCores() {
        return cpuCores;
    }

    public void setCpuCores(Integer cpuCores) {
        this.cpuCores = cpuCores;
    }

    public Integer getMemoryGb() {
        return memoryGb;
    }

    public void setMemoryGb(Integer memoryGb) {
        this.memoryGb = memoryGb;
    }

    public Integer getDiskGb() {
        return diskGb;
    }

    public void setDiskGb(Integer diskGb) {
        this.diskGb = diskGb;
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

    public List<Container> getContainers() {
        return containers;
    }

    public void setContainers(List<Container> containers) {
        this.containers = containers;
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

    public enum NodeType {
        dedicated_server,
        vm
    }
}