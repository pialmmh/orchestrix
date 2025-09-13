package com.telcobright.orchestrix.network.entity.lxc;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * LXC Container entity
 */
@Entity
@Table(name = "lxc_containers")
public class LxcContainer {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "container_name", unique = true, nullable = false)
    private String containerName;
    
    @Column(name = "base_image")
    private String baseImage; // e.g., "fusion-pbx-base", "debian/12"
    
    @Column(name = "state")
    @Enumerated(EnumType.STRING)
    private ContainerState state;
    
    @Column(name = "host_machine")
    private String hostMachine;
    
    @OneToOne(mappedBy = "container", cascade = CascadeType.ALL)
    private LxcNetworkConfig networkConfig;
    
    @OneToMany(mappedBy = "container", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<LxcMount> mounts = new ArrayList<>();
    
    @Column(name = "memory_limit")
    private String memoryLimit; // e.g., "2GB"
    
    @Column(name = "cpu_limit")
    private Integer cpuLimit; // Number of CPUs
    
    @Column(name = "autostart")
    private Boolean autostart = false;
    
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
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public String getContainerName() {
        return containerName;
    }
    
    public void setContainerName(String containerName) {
        this.containerName = containerName;
    }
    
    public String getBaseImage() {
        return baseImage;
    }
    
    public void setBaseImage(String baseImage) {
        this.baseImage = baseImage;
    }
    
    public ContainerState getState() {
        return state;
    }
    
    public void setState(ContainerState state) {
        this.state = state;
    }
    
    public String getHostMachine() {
        return hostMachine;
    }
    
    public void setHostMachine(String hostMachine) {
        this.hostMachine = hostMachine;
    }
    
    public LxcNetworkConfig getNetworkConfig() {
        return networkConfig;
    }
    
    public void setNetworkConfig(LxcNetworkConfig networkConfig) {
        this.networkConfig = networkConfig;
    }
    
    public List<LxcMount> getMounts() {
        return mounts;
    }
    
    public void setMounts(List<LxcMount> mounts) {
        this.mounts = mounts;
    }
    
    public String getMemoryLimit() {
        return memoryLimit;
    }
    
    public void setMemoryLimit(String memoryLimit) {
        this.memoryLimit = memoryLimit;
    }
    
    public Integer getCpuLimit() {
        return cpuLimit;
    }
    
    public void setCpuLimit(Integer cpuLimit) {
        this.cpuLimit = cpuLimit;
    }
    
    public Boolean getAutostart() {
        return autostart;
    }
    
    public void setAutostart(Boolean autostart) {
        this.autostart = autostart;
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
    
    public enum ContainerState {
        RUNNING,
        STOPPED,
        FROZEN,
        STARTING,
        STOPPING,
        ABORTING,
        FREEZING,
        THAWED,
        ERROR
    }
}