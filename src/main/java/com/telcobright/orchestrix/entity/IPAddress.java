package com.telcobright.orchestrix.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "ip_addresses")
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class IPAddress {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;
    
    @Column(nullable = false, unique = true)
    private String ipAddress;
    
    @Column(nullable = false)
    private Integer subnetMask = 24; // Default to /24
    
    private String gateway;
    
    @Column(nullable = false)
    private Boolean isPrimary = false;
    
    @Column(nullable = false)
    private Boolean isActive = true;
    
    // IP Address Types (can have multiple)
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "ip_address_types", joinColumns = @JoinColumn(name = "ip_address_id"))
    @Column(name = "type")
    @Enumerated(EnumType.STRING)
    private Set<IPAddressType> types = new HashSet<>();
    
    // Device associations
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "compute_id")
    @JsonIgnoreProperties({"ipAddresses", "hibernateLazyInitializer", "handler"})
    private Compute compute;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "network_device_id")
    @JsonIgnoreProperties({"ipAddresses", "hibernateLazyInitializer", "handler"})
    private NetworkDevice networkDevice;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "container_id")
    @JsonIgnoreProperties({"ipAddresses", "hibernateLazyInitializer", "handler"})
    private Container container;
    
    // Network information
    private String vlanId;
    private String macAddress;
    private String dnsServers; // Comma-separated list
    private String networkSegment;
    private String networkZone; // DMZ, Internal, External, etc.
    
    // Assignment information
    @Enumerated(EnumType.STRING)
    private AssignmentMethod assignmentMethod = AssignmentMethod.STATIC;
    
    private LocalDateTime assignedAt;
    private String assignedBy;
    private LocalDateTime lastSeenAt;
    private LocalDateTime leaseExpiresAt; // For DHCP assignments
    
    // Additional metadata
    private String description;
    private String notes;
    private String tags; // Comma-separated tags
    
    @Column(updatable = false)
    private LocalDateTime createdAt;
    
    private LocalDateTime updatedAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (assignedAt == null) {
            assignedAt = LocalDateTime.now();
        }
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
    
    // Enum for IP Address Types
    public enum IPAddressType {
        PUBLIC("Public IP"),
        PRIVATE("Private IP"),
        MANAGEMENT("Management Network"),
        REMOTE_ACCESS("Remote Access"),
        VPN("VPN Access"),
        LOAD_BALANCER("Load Balancer VIP"),
        CLUSTER("Cluster IP"),
        FLOATING("Floating IP"),
        ELASTIC("Elastic IP"),
        SERVICE("Service IP"),
        BACKUP("Backup Network"),
        STORAGE("Storage Network"),
        REPLICATION("Replication Network"),
        HEARTBEAT("Heartbeat/Keepalive"),
        MONITORING("Monitoring Network"),
        API("API Endpoint"),
        WEB("Web Interface"),
        DATABASE("Database Access"),
        INTERNAL("Internal Only"),
        EXTERNAL("External Facing");
        
        private final String displayName;
        
        IPAddressType(String displayName) {
            this.displayName = displayName;
        }
        
        public String getDisplayName() {
            return displayName;
        }
    }
    
    // Enum for Assignment Methods
    public enum AssignmentMethod {
        STATIC("Static Assignment"),
        DHCP("DHCP"),
        RESERVED("DHCP Reservation"),
        AUTO("Auto-assigned"),
        MANUAL("Manual Entry");
        
        private final String displayName;
        
        AssignmentMethod(String displayName) {
            this.displayName = displayName;
        }
        
        public String getDisplayName() {
            return displayName;
        }
    }
    
    // Helper method to get the device type
    @JsonIgnore
    public String getDeviceType() {
        if (compute != null) return "COMPUTE";
        if (networkDevice != null) return "NETWORK_DEVICE";
        if (container != null) return "CONTAINER";
        return null;
    }
    
    // Helper method to get the device ID
    @JsonIgnore
    public Long getDeviceId() {
        if (compute != null) return compute.getId();
        if (networkDevice != null) return networkDevice.getId();
        if (container != null) return container.getId();
        return null;
    }
    
    // Helper method to get the device name
    @JsonIgnore
    public String getDeviceName() {
        if (compute != null) return compute.getName();
        if (networkDevice != null) return networkDevice.getName();
        if (container != null) return container.getName();
        return null;
    }
    
    // Helper method to check if IP is in use
    public boolean isInUse() {
        return compute != null || networkDevice != null || container != null;
    }
    
    // Helper method to format IP with subnet
    public String getFullAddress() {
        return ipAddress + "/" + subnetMask;
    }
    
    // Helper method to check if this is a public IP
    public boolean isPublicIP() {
        return types.contains(IPAddressType.PUBLIC);
    }
    
    // Helper method to check if this is a private IP
    public boolean isPrivateIP() {
        return types.contains(IPAddressType.PRIVATE);
    }
}