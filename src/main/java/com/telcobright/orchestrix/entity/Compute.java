package com.telcobright.orchestrix.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.List;
import java.util.ArrayList;

@Entity
@Table(name = "computes")
public class Compute {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "node_type", length = 50)
    private NodeType nodeType; // dedicated_server, vm

    private String hostname;

    @Column(name = "ip_address")
    @Deprecated // Kept for backward compatibility, use ipAddresses instead
    private String ipAddress;
    
    // Multiple IP addresses support
    @OneToMany(mappedBy = "compute", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonIgnoreProperties({"compute", "networkDevice", "container", "hibernateLazyInitializer", "handler"})
    private List<IPAddress> ipAddresses = new ArrayList<>();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "os_version_id")
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private OSVersion osVersion;

    @Column(name = "cpu_cores")
    private Integer cpuCores;

    @Column(name = "memory_gb")
    private Integer memoryGb;

    @Column(name = "disk_gb")
    private Integer diskGb;

    private String status; // ACTIVE, INACTIVE, MAINTENANCE

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cloud_id")
    @JsonBackReference
    private Cloud cloud;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "datacenter_id")
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private Datacenter datacenter;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resource_pool_id")
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "computes"})
    private ResourcePool resourcePool;
    
    @Column(name = "hypervisor", length = 50)
    private String hypervisor; // VMWARE, KVM, HYPERV, XEN
    
    @Column(name = "is_physical")
    private Boolean isPhysical = false;

    @OneToMany(mappedBy = "compute", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonManagedReference
    private List<Container> containers;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    // Hardware Information
    private String brand;
    private String model;
    @Column(name = "serial_number")
    private String serialNumber;
    @Column(name = "asset_tag")
    private String assetTag;
    @Column(name = "rack_location")
    private String rackLocation;
    @Column(name = "rack_unit")
    private Integer rackUnit;
    @Column(name = "power_consumption_watts")
    private Integer powerConsumptionWatts;
    @Column(name = "thermal_output_btu")
    private Integer thermalOutputBtu;
    
    // OS and Software
    @Column(name = "os_type")
    private String osType;
    @Column(name = "os_distribution")
    private String osDistribution;
    @Column(name = "kernel_version")
    private String kernelVersion;
    @Column(name = "firmware_version")
    private String firmwareVersion;
    @Column(name = "bios_version")
    private String biosVersion;
    
    // Compute Roles and Purpose
    @Column(name = "compute_role")
    private String computeRole;
    private String purpose;
    @Column(name = "environment_type")
    private String environmentType;
    @Column(name = "service_tier")
    private String serviceTier;
    @Column(name = "business_unit")
    private String businessUnit;
    @Column(name = "cost_center")
    private String costCenter;
    
    // Container and Virtualization Support
    @Column(name = "virtualization_type")
    private String virtualizationType;
    @Column(name = "supports_containers")
    private Boolean supportsContainers = false;
    @Column(name = "container_runtime")
    private String containerRuntime;
    @Column(name = "orchestration_platform")
    private String orchestrationPlatform;
    @Column(name = "max_containers")
    private Integer maxContainers;
    @Column(name = "current_containers")
    private Integer currentContainers = 0;
    
    // Network Configuration
    @Column(name = "primary_mac_address")
    private String primaryMacAddress;
    @Column(name = "management_ip")
    private String managementIp;
    @Column(name = "public_ip")
    private String publicIp;
    @Column(name = "private_ip")
    private String privateIp;
    @Column(name = "ipmi_ip")
    private String ipmiIp;
    @Column(name = "vlan_ids", columnDefinition = "TEXT")
    private String vlanIds;
    @Column(name = "network_interfaces_count")
    private Integer networkInterfacesCount = 1;
    @Column(name = "network_speed_gbps")
    private Integer networkSpeedGbps;
    
    // Storage Configuration
    @Column(name = "storage_type")
    private String storageType;
    @Column(name = "storage_raid_level")
    private String storageRaidLevel;
    @Column(name = "total_storage_gb")
    private Integer totalStorageGb;
    @Column(name = "used_storage_gb")
    private Integer usedStorageGb = 0;
    @Column(name = "storage_iops")
    private Integer storageIops;
    
    // Performance Metrics
    @Column(name = "cpu_benchmark_score")
    private Integer cpuBenchmarkScore;
    @Column(name = "memory_bandwidth_gbps")
    private Double memoryBandwidthGbps;
    @Column(name = "network_latency_ms")
    private Double networkLatencyMs;
    @Column(name = "uptime_days")
    private Integer uptimeDays = 0;
    @Column(name = "last_reboot_date")
    private LocalDateTime lastRebootDate;
    
    // Compliance and Security
    @Column(name = "compliance_status")
    private String complianceStatus;
    @Column(name = "security_zone")
    private String securityZone;
    @Column(name = "encryption_enabled")
    private Boolean encryptionEnabled = false;
    @Column(name = "last_security_scan_date")
    private LocalDateTime lastSecurityScanDate;
    @Column(name = "patch_level")
    private String patchLevel;
    @Column(name = "antivirus_status")
    private String antivirusStatus;
    
    // Maintenance and Support
    @Column(name = "warranty_expiry_date")
    private LocalDateTime warrantyExpiryDate;
    @Column(name = "support_contract_id")
    private String supportContractId;
    @Column(name = "maintenance_window")
    private String maintenanceWindow;
    @Column(name = "last_maintenance_date")
    private LocalDateTime lastMaintenanceDate;
    @Column(name = "next_maintenance_date")
    private LocalDateTime nextMaintenanceDate;
    
    // Monitoring and Management
    @Column(name = "monitoring_enabled")
    private Boolean monitoringEnabled = true;
    @Column(name = "monitoring_agent")
    private String monitoringAgent;
    @Column(name = "management_tool")
    private String managementTool;
    @Column(name = "backup_enabled")
    private Boolean backupEnabled = false;
    @Column(name = "backup_schedule")
    private String backupSchedule;
    @Column(name = "last_backup_date")
    private LocalDateTime lastBackupDate;
    
    // Additional Metadata
    @Column(columnDefinition = "TEXT")
    private String tags;
    @Column(columnDefinition = "TEXT")
    private String notes;
    @Column(name = "custom_attributes", columnDefinition = "TEXT")
    private String customAttributes;

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
    public Compute() {}

    public Compute(String name, String description, NodeType nodeType, String hostname, 
                   String ipAddress, OSVersion osVersion, Integer cpuCores, Integer memoryGb, 
                   Integer diskGb, String status) {
        this.name = name;
        this.description = description;
        this.nodeType = nodeType;
        this.hostname = hostname;
        this.ipAddress = ipAddress;
        this.osVersion = osVersion;
        this.cpuCores = cpuCores;
        this.memoryGb = memoryGb;
        this.diskGb = diskGb;
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

    public NodeType getNodeType() {
        return nodeType;
    }

    public void setNodeType(NodeType nodeType) {
        this.nodeType = nodeType;
    }

    public String getHostname() {
        return hostname;
    }

    public void setHostname(String hostname) {
        this.hostname = hostname;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }
    
    public List<IPAddress> getIpAddresses() {
        return ipAddresses;
    }
    
    public void setIpAddresses(List<IPAddress> ipAddresses) {
        this.ipAddresses = ipAddresses;
    }
    
    // Helper method to add an IP address
    public void addIpAddress(IPAddress ipAddress) {
        if (this.ipAddresses == null) {
            this.ipAddresses = new ArrayList<>();
        }
        ipAddress.setCompute(this);
        this.ipAddresses.add(ipAddress);
    }
    
    // Helper method to remove an IP address
    public void removeIpAddress(IPAddress ipAddress) {
        if (this.ipAddresses != null) {
            ipAddress.setCompute(null);
            this.ipAddresses.remove(ipAddress);
        }
    }
    
    // Helper method to get primary IP address
    public IPAddress getPrimaryIpAddress() {
        if (ipAddresses != null) {
            return ipAddresses.stream()
                .filter(IPAddress::getIsPrimary)
                .findFirst()
                .orElse(null);
        }
        return null;
    }

    public OSVersion getOsVersion() {
        return osVersion;
    }

    public void setOsVersion(OSVersion osVersion) {
        this.osVersion = osVersion;
    }

    public Integer getCpuCores() {
        return cpuCores;
    }

    public void setCpuCores(Integer cpuCores) {
        this.cpuCores = cpuCores;
    }

    public Integer getMemoryGb() {
        return memoryGb;
    }

    public void setMemoryGb(Integer memoryGb) {
        this.memoryGb = memoryGb;
    }

    public Integer getDiskGb() {
        return diskGb;
    }

    public void setDiskGb(Integer diskGb) {
        this.diskGb = diskGb;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Cloud getCloud() {
        return cloud;
    }

    public void setCloud(Cloud cloud) {
        this.cloud = cloud;
    }

    public Datacenter getDatacenter() {
        return datacenter;
    }

    public void setDatacenter(Datacenter datacenter) {
        this.datacenter = datacenter;
    }
    
    public ResourcePool getResourcePool() {
        return resourcePool;
    }
    
    public void setResourcePool(ResourcePool resourcePool) {
        this.resourcePool = resourcePool;
    }
    
    public String getHypervisor() {
        return hypervisor;
    }
    
    public void setHypervisor(String hypervisor) {
        this.hypervisor = hypervisor;
    }
    
    public Boolean getIsPhysical() {
        return isPhysical;
    }
    
    public void setIsPhysical(Boolean isPhysical) {
        this.isPhysical = isPhysical;
    }

    public List<Container> getContainers() {
        return containers;
    }

    public void setContainers(List<Container> containers) {
        this.containers = containers;
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
    
    // Hardware Information Getters and Setters
    public String getBrand() { return brand; }
    public void setBrand(String brand) { this.brand = brand; }
    
    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }
    
    public String getSerialNumber() { return serialNumber; }
    public void setSerialNumber(String serialNumber) { this.serialNumber = serialNumber; }
    
    public String getAssetTag() { return assetTag; }
    public void setAssetTag(String assetTag) { this.assetTag = assetTag; }
    
    public String getRackLocation() { return rackLocation; }
    public void setRackLocation(String rackLocation) { this.rackLocation = rackLocation; }
    
    public Integer getRackUnit() { return rackUnit; }
    public void setRackUnit(Integer rackUnit) { this.rackUnit = rackUnit; }
    
    public Integer getPowerConsumptionWatts() { return powerConsumptionWatts; }
    public void setPowerConsumptionWatts(Integer powerConsumptionWatts) { this.powerConsumptionWatts = powerConsumptionWatts; }
    
    public Integer getThermalOutputBtu() { return thermalOutputBtu; }
    public void setThermalOutputBtu(Integer thermalOutputBtu) { this.thermalOutputBtu = thermalOutputBtu; }
    
    // OS and Software Getters and Setters
    public String getOsType() { return osType; }
    public void setOsType(String osType) { this.osType = osType; }
    
    public String getOsDistribution() { return osDistribution; }
    public void setOsDistribution(String osDistribution) { this.osDistribution = osDistribution; }
    
    public String getKernelVersion() { return kernelVersion; }
    public void setKernelVersion(String kernelVersion) { this.kernelVersion = kernelVersion; }
    
    public String getFirmwareVersion() { return firmwareVersion; }
    public void setFirmwareVersion(String firmwareVersion) { this.firmwareVersion = firmwareVersion; }
    
    public String getBiosVersion() { return biosVersion; }
    public void setBiosVersion(String biosVersion) { this.biosVersion = biosVersion; }
    
    // Compute Roles and Purpose Getters and Setters
    public String getComputeRole() { return computeRole; }
    public void setComputeRole(String computeRole) { this.computeRole = computeRole; }
    
    public String getPurpose() { return purpose; }
    public void setPurpose(String purpose) { this.purpose = purpose; }
    
    public String getEnvironmentType() { return environmentType; }
    public void setEnvironmentType(String environmentType) { this.environmentType = environmentType; }
    
    public String getServiceTier() { return serviceTier; }
    public void setServiceTier(String serviceTier) { this.serviceTier = serviceTier; }
    
    public String getBusinessUnit() { return businessUnit; }
    public void setBusinessUnit(String businessUnit) { this.businessUnit = businessUnit; }
    
    public String getCostCenter() { return costCenter; }
    public void setCostCenter(String costCenter) { this.costCenter = costCenter; }
    
    // Container and Virtualization Support Getters and Setters
    public String getVirtualizationType() { return virtualizationType; }
    public void setVirtualizationType(String virtualizationType) { this.virtualizationType = virtualizationType; }
    
    public Boolean getSupportsContainers() { return supportsContainers; }
    public void setSupportsContainers(Boolean supportsContainers) { this.supportsContainers = supportsContainers; }
    
    public String getContainerRuntime() { return containerRuntime; }
    public void setContainerRuntime(String containerRuntime) { this.containerRuntime = containerRuntime; }
    
    public String getOrchestrationPlatform() { return orchestrationPlatform; }
    public void setOrchestrationPlatform(String orchestrationPlatform) { this.orchestrationPlatform = orchestrationPlatform; }
    
    public Integer getMaxContainers() { return maxContainers; }
    public void setMaxContainers(Integer maxContainers) { this.maxContainers = maxContainers; }
    
    public Integer getCurrentContainers() { return currentContainers; }
    public void setCurrentContainers(Integer currentContainers) { this.currentContainers = currentContainers; }
    
    // Network Configuration Getters and Setters
    public String getPrimaryMacAddress() { return primaryMacAddress; }
    public void setPrimaryMacAddress(String primaryMacAddress) { this.primaryMacAddress = primaryMacAddress; }
    
    public String getManagementIp() { return managementIp; }
    public void setManagementIp(String managementIp) { this.managementIp = managementIp; }
    
    public String getPublicIp() { return publicIp; }
    public void setPublicIp(String publicIp) { this.publicIp = publicIp; }
    
    public String getPrivateIp() { return privateIp; }
    public void setPrivateIp(String privateIp) { this.privateIp = privateIp; }
    
    public String getIpmiIp() { return ipmiIp; }
    public void setIpmiIp(String ipmiIp) { this.ipmiIp = ipmiIp; }
    
    public String getVlanIds() { return vlanIds; }
    public void setVlanIds(String vlanIds) { this.vlanIds = vlanIds; }
    
    public Integer getNetworkInterfacesCount() { return networkInterfacesCount; }
    public void setNetworkInterfacesCount(Integer networkInterfacesCount) { this.networkInterfacesCount = networkInterfacesCount; }
    
    public Integer getNetworkSpeedGbps() { return networkSpeedGbps; }
    public void setNetworkSpeedGbps(Integer networkSpeedGbps) { this.networkSpeedGbps = networkSpeedGbps; }
    
    // Storage Configuration Getters and Setters
    public String getStorageType() { return storageType; }
    public void setStorageType(String storageType) { this.storageType = storageType; }
    
    public String getStorageRaidLevel() { return storageRaidLevel; }
    public void setStorageRaidLevel(String storageRaidLevel) { this.storageRaidLevel = storageRaidLevel; }
    
    public Integer getTotalStorageGb() { return totalStorageGb; }
    public void setTotalStorageGb(Integer totalStorageGb) { this.totalStorageGb = totalStorageGb; }
    
    public Integer getUsedStorageGb() { return usedStorageGb; }
    public void setUsedStorageGb(Integer usedStorageGb) { this.usedStorageGb = usedStorageGb; }
    
    public Integer getStorageIops() { return storageIops; }
    public void setStorageIops(Integer storageIops) { this.storageIops = storageIops; }
    
    // Performance Metrics Getters and Setters
    public Integer getCpuBenchmarkScore() { return cpuBenchmarkScore; }
    public void setCpuBenchmarkScore(Integer cpuBenchmarkScore) { this.cpuBenchmarkScore = cpuBenchmarkScore; }
    
    public Double getMemoryBandwidthGbps() { return memoryBandwidthGbps; }
    public void setMemoryBandwidthGbps(Double memoryBandwidthGbps) { this.memoryBandwidthGbps = memoryBandwidthGbps; }
    
    public Double getNetworkLatencyMs() { return networkLatencyMs; }
    public void setNetworkLatencyMs(Double networkLatencyMs) { this.networkLatencyMs = networkLatencyMs; }
    
    public Integer getUptimeDays() { return uptimeDays; }
    public void setUptimeDays(Integer uptimeDays) { this.uptimeDays = uptimeDays; }
    
    public LocalDateTime getLastRebootDate() { return lastRebootDate; }
    public void setLastRebootDate(LocalDateTime lastRebootDate) { this.lastRebootDate = lastRebootDate; }
    
    // Compliance and Security Getters and Setters
    public String getComplianceStatus() { return complianceStatus; }
    public void setComplianceStatus(String complianceStatus) { this.complianceStatus = complianceStatus; }
    
    public String getSecurityZone() { return securityZone; }
    public void setSecurityZone(String securityZone) { this.securityZone = securityZone; }
    
    public Boolean getEncryptionEnabled() { return encryptionEnabled; }
    public void setEncryptionEnabled(Boolean encryptionEnabled) { this.encryptionEnabled = encryptionEnabled; }
    
    public LocalDateTime getLastSecurityScanDate() { return lastSecurityScanDate; }
    public void setLastSecurityScanDate(LocalDateTime lastSecurityScanDate) { this.lastSecurityScanDate = lastSecurityScanDate; }
    
    public String getPatchLevel() { return patchLevel; }
    public void setPatchLevel(String patchLevel) { this.patchLevel = patchLevel; }
    
    public String getAntivirusStatus() { return antivirusStatus; }
    public void setAntivirusStatus(String antivirusStatus) { this.antivirusStatus = antivirusStatus; }
    
    // Maintenance and Support Getters and Setters
    public LocalDateTime getWarrantyExpiryDate() { return warrantyExpiryDate; }
    public void setWarrantyExpiryDate(LocalDateTime warrantyExpiryDate) { this.warrantyExpiryDate = warrantyExpiryDate; }
    
    public String getSupportContractId() { return supportContractId; }
    public void setSupportContractId(String supportContractId) { this.supportContractId = supportContractId; }
    
    public String getMaintenanceWindow() { return maintenanceWindow; }
    public void setMaintenanceWindow(String maintenanceWindow) { this.maintenanceWindow = maintenanceWindow; }
    
    public LocalDateTime getLastMaintenanceDate() { return lastMaintenanceDate; }
    public void setLastMaintenanceDate(LocalDateTime lastMaintenanceDate) { this.lastMaintenanceDate = lastMaintenanceDate; }
    
    public LocalDateTime getNextMaintenanceDate() { return nextMaintenanceDate; }
    public void setNextMaintenanceDate(LocalDateTime nextMaintenanceDate) { this.nextMaintenanceDate = nextMaintenanceDate; }
    
    // Monitoring and Management Getters and Setters
    public Boolean getMonitoringEnabled() { return monitoringEnabled; }
    public void setMonitoringEnabled(Boolean monitoringEnabled) { this.monitoringEnabled = monitoringEnabled; }
    
    public String getMonitoringAgent() { return monitoringAgent; }
    public void setMonitoringAgent(String monitoringAgent) { this.monitoringAgent = monitoringAgent; }
    
    public String getManagementTool() { return managementTool; }
    public void setManagementTool(String managementTool) { this.managementTool = managementTool; }
    
    public Boolean getBackupEnabled() { return backupEnabled; }
    public void setBackupEnabled(Boolean backupEnabled) { this.backupEnabled = backupEnabled; }
    
    public String getBackupSchedule() { return backupSchedule; }
    public void setBackupSchedule(String backupSchedule) { this.backupSchedule = backupSchedule; }
    
    public LocalDateTime getLastBackupDate() { return lastBackupDate; }
    public void setLastBackupDate(LocalDateTime lastBackupDate) { this.lastBackupDate = lastBackupDate; }
    
    // Additional Metadata Getters and Setters
    public String getTags() { return tags; }
    public void setTags(String tags) { this.tags = tags; }
    
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    
    public String getCustomAttributes() { return customAttributes; }
    public void setCustomAttributes(String customAttributes) { this.customAttributes = customAttributes; }

    public enum NodeType {
        dedicated_server,
        vm
    }
}