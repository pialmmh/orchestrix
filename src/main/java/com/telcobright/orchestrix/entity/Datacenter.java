package com.telcobright.orchestrix.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import javax.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "datacenters")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Datacenter {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    
    @Column(name = "name", nullable = false, length = 100)
    private String name;
    
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "country_id")
    private Country country;
    
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "state_id")
    private State state;
    
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "city_id")
    private City city;
    
    @Column(name = "location_other", length = 255)
    private String locationOther;
    
    @Column(name = "type", length = 50)
    private String type;
    
    @Column(name = "status", length = 50)
    private String status;
    
    @Column(name = "provider", length = 100)
    private String provider;
    
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "partner_id")
    private Partner partner;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cloud_id")
    @JsonBackReference
    private Cloud cloud;
    
    @Column(name = "is_dr_site")
    private Boolean isDrSite = false;
    
    @Column(name = "latitude", precision = 10, scale = 8)
    private BigDecimal latitude;
    
    @Column(name = "longitude", precision = 11, scale = 8)
    private BigDecimal longitude;
    
    @Column(name = "servers")
    private Integer servers = 0;
    
    @Column(name = "storage_tb")
    private Integer storageTb = 0;
    
    @Column(name = "utilization")
    private Integer utilization = 0;
    
    // New fields for cloud-native hierarchy
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "availability_zone_id")
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "datacenters"})
    private AvailabilityZone availabilityZone;
    
    @Column(name = "tier")
    private Integer tier = 3; // Tier 1-4 datacenter classification
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dr_paired_with")
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private Datacenter drPairedDatacenter;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "environment_id")
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "datacenters"})
    private Environment environment;
    
    @OneToMany(mappedBy = "datacenter", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonIgnoreProperties({"datacenter"})
    private List<ResourcePool> resourcePools;
    
    @OneToMany(mappedBy = "datacenter", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonIgnoreProperties({"datacenter"})
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