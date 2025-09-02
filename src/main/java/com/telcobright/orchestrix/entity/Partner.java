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
@Table(name = "partners")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Partner {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    
    @Column(nullable = false, unique = true, length = 100)
    private String name;
    
    @Column(name = "display_name", nullable = false, length = 100)
    private String displayName;
    
    @Column(nullable = false, length = 50)
    private String type; // cloud-provider, vendor, both
    
    @Column(columnDefinition = "JSON")
    @Convert(converter = RolesConverter.class)
    private List<String> roles = new ArrayList<>();
    
    @Column(name = "contact_email")
    private String contactEmail;
    
    @Column(name = "contact_phone", length = 50)
    private String contactPhone;
    
    private String website;
    
    @Column(name = "billing_account_id", length = 100)
    private String billingAccountId;
    
    @Column(name = "api_key")
    @JsonIgnore
    private String apiKey;
    
    @Column(name = "api_secret")
    @JsonIgnore
    private String apiSecret;
    
    @Column(length = 50)
    private String status = "ACTIVE";
    
    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at", insertable = false, updatable = false)
    private LocalDateTime updatedAt;
    
    @OneToMany(mappedBy = "partner", fetch = FetchType.LAZY)
    @JsonIgnore
    private List<Datacenter> datacenters = new ArrayList<>();
}