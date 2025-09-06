package com.telcobright.orchestrix.entity;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "clouds")
public class Cloud {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    private String description;

    @Column(name = "client_name")
    private String clientName;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "partner_id", nullable = false)
    private Partner partner;

    @Column(name = "deployment_region")
    private String deploymentRegion;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    private String status; // ACTIVE, INACTIVE, MAINTENANCE

    @OneToMany(mappedBy = "cloud", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonIgnore
    private List<Datacenter> datacenters;

    @OneToMany(mappedBy = "cloud", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonIgnore
    private List<Compute> computes;

    @OneToMany(mappedBy = "cloud", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonIgnore
    private List<Storage> storages;

    @OneToMany(mappedBy = "cloud", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonIgnore
    private List<Networking> networks;
    
    @OneToMany(mappedBy = "cloud", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonIgnoreProperties({"cloud"})
    private List<Region> regions;

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
    public Cloud() {}

    public Cloud(String name, String description, String clientName, String deploymentRegion, String status) {
        this.name = name;
        this.description = description;
        this.clientName = clientName;
        this.deploymentRegion = deploymentRegion;
        this.status = status;
    }

    public Cloud(String name, String description, Partner partner, String deploymentRegion, String status) {
        this.name = name;
        this.description = description;
        this.partner = partner;
        this.deploymentRegion = deploymentRegion;
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

    public String getClientName() {
        return clientName;
    }

    public void setClientName(String clientName) {
        this.clientName = clientName;
    }

    public Partner getPartner() {
        return partner;
    }

    public void setPartner(Partner partner) {
        this.partner = partner;
    }

    public String getDeploymentRegion() {
        return deploymentRegion;
    }

    public void setDeploymentRegion(String deploymentRegion) {
        this.deploymentRegion = deploymentRegion;
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

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public List<Datacenter> getDatacenters() {
        return datacenters;
    }

    public void setDatacenters(List<Datacenter> datacenters) {
        this.datacenters = datacenters;
    }

    public List<Compute> getComputes() {
        return computes;
    }

    public void setComputes(List<Compute> computes) {
        this.computes = computes;
    }

    public List<Storage> getStorages() {
        return storages;
    }

    public void setStorages(List<Storage> storages) {
        this.storages = storages;
    }

    public List<Networking> getNetworks() {
        return networks;
    }

    public void setNetworks(List<Networking> networks) {
        this.networks = networks;
    }
    
    public List<Region> getRegions() {
        return regions;
    }
    
    public void setRegions(List<Region> regions) {
        this.regions = regions;
    }
}