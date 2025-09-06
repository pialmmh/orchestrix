package com.telcobright.orchestrix.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "resource_groups")
@Data
public class ResourceGroup {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, unique = true, length = 100)
    private String name;
    
    @Column(nullable = false, length = 100)
    private String displayName;
    
    @Column(columnDefinition = "TEXT")
    private String description;
    
    @Column(nullable = false)
    private String category; // IaaS, PaaS, SaaS
    
    @ElementCollection
    @CollectionTable(name = "resource_group_services", 
                      joinColumns = @JoinColumn(name = "resource_group_id"))
    @Column(name = "service_type")
    private List<String> serviceTypes;
    
    @Column(nullable = false)
    private Boolean isActive = true;
    
    @Column(nullable = false)
    private Integer sortOrder = 0;
    
    @Column(length = 50)
    private String icon; // Material icon name
    
    @Column(length = 7)
    private String color; // Hex color code
    
    @OneToMany(mappedBy = "resourceGroup", fetch = FetchType.LAZY)
    @JsonIgnoreProperties({"resourceGroup"})
    private List<DatacenterResourceGroup> datacenterResourceGroups;
    
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