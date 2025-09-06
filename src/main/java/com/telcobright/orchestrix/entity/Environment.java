package com.telcobright.orchestrix.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "environments")
@Data
public class Environment {
    
    public enum EnvironmentType {
        PRODUCTION, STAGING, DEVELOPMENT, QA, DR
    }
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, length = 100)
    private String name;
    
    @Column(nullable = false, length = 50)
    private String code;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private EnvironmentType type;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "partner_id")
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private Partner partner;
    
    @Column(columnDefinition = "TEXT")
    private String description;
    
    @Column(length = 50)
    private String status = "ACTIVE";
    
    @OneToMany(mappedBy = "environment", fetch = FetchType.LAZY)
    @JsonIgnoreProperties({"environment"})
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