package com.telcobright.orchestrix.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "datacenter_resource_groups")
@Data
public class DatacenterResourceGroup {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "datacenter_id", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "datacenterResourceGroups"})
    private Datacenter datacenter;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resource_group_id", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "datacenterResourceGroups"})
    private ResourceGroup resourceGroup;
    
    @Column(nullable = false)
    private String status = "ACTIVE"; // ACTIVE, INACTIVE, MAINTENANCE
    
    @Column(columnDefinition = "TEXT")
    private String configuration; // JSON configuration for this specific instance
    
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
}