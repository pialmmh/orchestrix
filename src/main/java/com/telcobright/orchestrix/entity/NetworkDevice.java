package com.telcobright.orchestrix.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import lombok.Data;
import javax.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.ArrayList;

@Entity
@Table(name = "network_device")
@Data
public class NetworkDevice {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private String name;
    
    @Column(name = "display_name")
    private String displayName;
    
    @Column(name = "device_type", nullable = false, length = 50)
    private String deviceType; // Router, Switch, Firewall, etc.
    
    @Column(length = 100)
    private String vendor;
    
    @Column(length = 100)
    private String model;
    
    @Column(length = 50)
    private String version;
    
    @Column(name = "serial_number", length = 100)
    private String serialNumber;
    
    @Column(name = "mac_address", length = 18)
    private String macAddress;
    
    // Network Configuration
    @Column(name = "management_ip", length = 45)
    @Deprecated // Kept for backward compatibility, use ipAddresses instead
    private String managementIp;
    
    // Multiple IP addresses support
    @OneToMany(mappedBy = "networkDevice", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonIgnoreProperties({"compute", "networkDevice", "container", "hibernateLazyInitializer", "handler"})
    private List<IPAddress> ipAddresses = new ArrayList<>();
    
    @Column(name = "management_port")
    private Integer managementPort = 22;
    
    @Column(name = "management_protocol", length = 20)
    private String managementProtocol = "SSH"; // SSH, HTTPS, HTTP, SNMP
    
    @Column(length = 45)
    private String subnet;
    
    @Column(length = 45)
    private String gateway;
    
    @Column(name = "dns_servers", columnDefinition = "TEXT")
    private String dnsServers;
    
    @Column(name = "vlan_id")
    private Integer vlanId;
    
    // Physical Location
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "datacenter_id")
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "networkDevices"})
    private Datacenter datacenter;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resource_pool_id")
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "networkDevices"})
    private ResourcePool resourcePool;
    
    @Column(name = "rack_position", length = 50)
    private String rackPosition;
    
    @Column(name = "port_count")
    private Integer portCount = 0;
    
    @Column(name = "power_consumption_watts")
    private Integer powerConsumptionWatts = 0;
    
    // Operational Info
    @Column(length = 50)
    private String status = "ACTIVE"; // ACTIVE, INACTIVE, MAINTENANCE, ERROR
    
    @Column(name = "operational_status", length = 50)
    private String operationalStatus = "UNKNOWN"; // UP, DOWN, DEGRADED, UNKNOWN
    
    @Column(name = "last_seen")
    private LocalDateTime lastSeen;
    
    @Column(name = "uptime_hours")
    private Long uptimeHours = 0L;
    
    // Performance & Capacity
    @Column(name = "cpu_utilization_percent", precision = 5, scale = 2)
    private BigDecimal cpuUtilizationPercent = BigDecimal.ZERO;
    
    @Column(name = "memory_utilization_percent", precision = 5, scale = 2)
    private BigDecimal memoryUtilizationPercent = BigDecimal.ZERO;
    
    @Column(name = "bandwidth_utilization_mbps", precision = 10, scale = 2)
    private BigDecimal bandwidthUtilizationMbps = BigDecimal.ZERO;
    
    @Column(name = "total_memory_mb")
    private Integer totalMemoryMb = 0;
    
    @Column(name = "available_storage_gb")
    private Integer availableStorageGb = 0;
    
    // Configuration & Management
    @Column(name = "firmware_version", length = 100)
    private String firmwareVersion;
    
    @Column(name = "configuration_backup_path", length = 500)
    private String configurationBackupPath;
    
    @Column(name = "last_backup_date")
    private LocalDateTime lastBackupDate;
    
    @Column(name = "snmp_community", length = 100)
    private String snmpCommunity;
    
    @Column(name = "ssh_username", length = 100)
    private String sshUsername;
    
    @Column(name = "ssh_key_path", length = 500)
    private String sshKeyPath;
    
    // Business & Compliance
    @Column(length = 50)
    private String environment = "production"; // production, staging, development, test
    
    @Column(length = 50)
    private String criticality = "medium"; // low, medium, high, critical
    
    @Column(name = "compliance_zone", length = 100)
    private String complianceZone;
    
    @Column(name = "cost_center", length = 100)
    private String costCenter;
    
    @Column(name = "owner_contact")
    private String ownerContact;
    
    @Column(name = "support_contract")
    private String supportContract;
    
    // Monitoring & Alerting
    @Column(name = "monitoring_enabled")
    private Boolean monitoringEnabled = true;
    
    @Column(name = "alert_email")
    private String alertEmail;
    
    @Column(name = "alert_threshold_cpu", precision = 5, scale = 2)
    private BigDecimal alertThresholdCpu = new BigDecimal("80.0");
    
    @Column(name = "alert_threshold_memory", precision = 5, scale = 2)
    private BigDecimal alertThresholdMemory = new BigDecimal("85.0");
    
    @Column(name = "alert_threshold_bandwidth", precision = 5, scale = 2)
    private BigDecimal alertThresholdBandwidth = new BigDecimal("90.0");
    
    // Metadata
    @Column(columnDefinition = "TEXT")
    private String description;
    
    @Column(columnDefinition = "TEXT")
    private String tags; // JSON array of tags
    
    @Column(columnDefinition = "TEXT")
    private String notes;
    
    // Audit fields
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @Column(name = "created_by", length = 100)
    private String createdBy;
    
    @Column(name = "updated_by", length = 100)
    private String updatedBy;
    
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
    public Boolean isOnline() {
        return "UP".equals(operationalStatus);
    }
    
    public Boolean isActive() {
        return "ACTIVE".equals(status);
    }
    
    public String getFullDisplayName() {
        return displayName != null ? displayName : name;
    }
    
    public String getDeviceIdentifier() {
        return String.format("%s (%s)", getFullDisplayName(), deviceType);
    }
    
    public Boolean isCritical() {
        return "critical".equalsIgnoreCase(criticality);
    }
    
    public Boolean isHighUtilization() {
        return cpuUtilizationPercent != null && cpuUtilizationPercent.compareTo(new BigDecimal("80")) >= 0 ||
               memoryUtilizationPercent != null && memoryUtilizationPercent.compareTo(new BigDecimal("85")) >= 0;
    }
    
    public String getStatusIcon() {
        if ("UP".equals(operationalStatus) && "ACTIVE".equals(status)) {
            return "check_circle"; // Green checkmark
        } else if ("DOWN".equals(operationalStatus)) {
            return "error"; // Red error
        } else if ("DEGRADED".equals(operationalStatus)) {
            return "warning"; // Yellow warning
        } else if ("MAINTENANCE".equals(status)) {
            return "build"; // Maintenance icon
        } else {
            return "help"; // Unknown status
        }
    }
    
    public String getStatusColor() {
        if ("UP".equals(operationalStatus) && "ACTIVE".equals(status)) {
            return "#4CAF50"; // Green
        } else if ("DOWN".equals(operationalStatus)) {
            return "#F44336"; // Red
        } else if ("DEGRADED".equals(operationalStatus)) {
            return "#FF9800"; // Orange
        } else if ("MAINTENANCE".equals(status)) {
            return "#2196F3"; // Blue
        } else {
            return "#9E9E9E"; // Gray
        }
    }
}