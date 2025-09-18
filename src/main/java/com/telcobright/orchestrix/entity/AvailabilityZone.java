package com.telcobright.orchestrix.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "availability_zone")
@Data
public class AvailabilityZone {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, length = 100)
    private String name;
    
    @Column(nullable = false, length = 50)
    private String code;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "region_id", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "availabilityZones"})
    private Region region;
    
    @Column(name = "zone_type", length = 50)
    private String zoneType = "STANDARD"; // STANDARD, EDGE, LOCAL
    
    @Column(name = "is_default")
    private Boolean isDefault = false;
    
    @Column
    private String capabilities; // CSV of capabilities like GPU,HIGH_MEMORY
    
    @Column(length = 50)
    private String status = "ACTIVE";
    
    @OneToMany(mappedBy = "availabilityZone", fetch = FetchType.LAZY)
    @JsonIgnoreProperties({"availabilityZone"})
    private List<Datacenter> datacenters;
    
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