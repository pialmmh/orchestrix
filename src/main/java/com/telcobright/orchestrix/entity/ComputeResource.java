package com.telcobright.orchestrix.entity;

import javax.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import com.fasterxml.jackson.annotation.JsonIgnore;
import java.time.LocalDateTime;
import java.util.List;
import java.util.ArrayList;

@Entity
@Table(name = "compute_resource")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ComputeResource {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    
    @Column(nullable = false, length = 100)
    private String name;
    
    @Column(nullable = false)
    private String hostname;
    
    @Column(name = "ip_address", length = 45)
    private String ipAddress;
    
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "datacenter_id")
    private Datacenter datacenter;
    
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "partner_id")
    private Partner partner;
    
    // Resource specifications
    @Column(name = "cpu_cores")
    private Integer cpuCores;
    
    @Column(name = "memory_gb")
    private Integer memoryGb;
    
    @Column(name = "storage_gb")
    private Integer storageGb;
    
    @Column(name = "os_type", length = 50)
    private String osType;
    
    @Column(name = "os_version", length = 100)
    private String osVersion;
    
    // Status and management
    @Column(length = 50)
    private String status = "ACTIVE"; // ACTIVE, SUSPENDED, DISCONTINUED, MAINTENANCE
    
    @Column(length = 50)
    private String environment; // PRODUCTION, STAGING, DEVELOPMENT, TEST
    
    private String purpose;
    
    // Metadata
    @Column(columnDefinition = "JSON")
    @Convert(converter = TagsConverter.class)
    private List<String> tags = new ArrayList<>();
    
    @Column(columnDefinition = "TEXT")
    private String notes;
    
    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at", insertable = false, updatable = false)
    private LocalDateTime updatedAt;
    
    @OneToMany(mappedBy = "computeResource", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonIgnore
    private List<ComputeAccessCredential> accessCredentials = new ArrayList<>();
    
    @OneToMany(mappedBy = "computeResource", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonIgnore
    private List<ComputeAutomationConfig> automationConfigs = new ArrayList<>();
}