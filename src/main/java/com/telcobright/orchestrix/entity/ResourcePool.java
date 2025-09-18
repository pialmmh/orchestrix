package com.telcobright.orchestrix.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "resource_pool")
@Data
public class ResourcePool {
    
    public enum PoolType {
        COMPUTE, STORAGE, KUBERNETES, VMWARE, OPENSTACK, HYPERV
    }
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, length = 100)
    private String name;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private PoolType type;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "datacenter_id")
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "resourcePools"})
    private Datacenter datacenter;
    
    @Column(name = "total_cpu_cores")
    private Integer totalCpuCores = 0;
    
    @Column(name = "total_memory_gb")
    private Integer totalMemoryGb = 0;
    
    @Column(name = "total_storage_tb")
    private Integer totalStorageTb = 0;
    
    @Column(name = "used_cpu_cores")
    private Integer usedCpuCores = 0;
    
    @Column(name = "used_memory_gb")
    private Integer usedMemoryGb = 0;
    
    @Column(name = "used_storage_tb")
    private Integer usedStorageTb = 0;
    
    @Column(length = 50)
    private String hypervisor; // VMWARE, KVM, HYPERV, XEN
    
    @Column(length = 50)
    private String orchestrator; // KUBERNETES, OPENSHIFT, DOCKER_SWARM
    
    @Column(columnDefinition = "TEXT")
    private String description;
    
    @Column(length = 50)
    private String status = "ACTIVE";
    
    @OneToMany(mappedBy = "resourcePool", fetch = FetchType.LAZY)
    @JsonIgnoreProperties({"resourcePool"})
    private List<Compute> computes;
    
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
    
    // Utility methods
    public Integer getAvailableCpuCores() {
        return totalCpuCores - usedCpuCores;
    }
    
    public Integer getAvailableMemoryGb() {
        return totalMemoryGb - usedMemoryGb;
    }
    
    public Integer getAvailableStorageTb() {
        return totalStorageTb - usedStorageTb;
    }
    
    public Double getCpuUtilizationPercent() {
        return totalCpuCores > 0 ? (usedCpuCores * 100.0) / totalCpuCores : 0;
    }
    
    public Double getMemoryUtilizationPercent() {
        return totalMemoryGb > 0 ? (usedMemoryGb * 100.0) / totalMemoryGb : 0;
    }
    
    public Double getStorageUtilizationPercent() {
        return totalStorageTb > 0 ? (usedStorageTb * 100.0) / totalStorageTb : 0;
    }
}