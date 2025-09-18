package com.telcobright.orchestrix.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import javax.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.math.BigDecimal;

@Entity
@Table(name = "compute")
public class ComputeEnhanced {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ============= Basic Information =============
    @Column(nullable = false)
    private String name;
    
    private String description;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "node_type", length = 50)
    private NodeType nodeType; // DEDICATED, VM
    
    private String hostname;
    
    @Column(name = "ip_address")
    private String ipAddress;
    
    private String status; // ACTIVE, INACTIVE, MAINTENANCE

    // ============= Hardware Information =============
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
    
    @Column(name = "cpu_cores")
    private Integer cpuCores;
    
    @Column(name = "memory_gb")
    private Integer memoryGb;
    
    @Column(name = "disk_gb")
    private Integer diskGb;
    
    @Column(name = "power_consumption_watts")
    private Integer powerConsumptionWatts;
    
    @Column(name = "thermal_output_btu")
    private Integer thermalOutputBtu;

    // ============= OS and Software =============
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "os_version_id")
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private OSVersion osVersion;
    
    @Column(name = "os_type")
    private String osType; // LINUX, WINDOWS, UNIX, MACOS
    
    @Column(name = "os_distribution")
    private String osDistribution; // Ubuntu, RHEL, CentOS, Windows Server, etc.
    
    @Column(name = "os_version", insertable = false, updatable = false)
    private String osVersionString;
    
    @Column(name = "kernel_version")
    private String kernelVersion;
    
    @Column(name = "firmware_version")
    private String firmwareVersion;
    
    @Column(name = "bios_version")
    private String biosVersion;

    // ============= Compute Roles and Purpose =============
    @Enumerated(EnumType.STRING)
    @Column(name = "compute_role")
    private ComputeRole computeRole;
    
    @Column(columnDefinition = "TEXT")
    private String purpose;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "environment_type")
    private EnvironmentType environmentType;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "service_tier")
    private ServiceTier serviceTier;
    
    @Column(name = "business_unit")
    private String businessUnit;
    
    @Column(name = "cost_center")
    private String costCenter;

    // ============= Container and Virtualization Support =============
    @Column(name = "hypervisor", length = 50)
    private String hypervisor; // VMWARE, KVM, HYPERV, XEN
    
    @Column(name = "is_physical")
    private Boolean isPhysical = false;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "virtualization_type")
    private VirtualizationType virtualizationType;
    
    @Column(name = "supports_containers")
    private Boolean supportsContainers = false;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "container_runtime")
    private ContainerRuntime containerRuntime;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "orchestration_platform")
    private OrchestrationPlatform orchestrationPlatform;
    
    @Column(name = "max_containers")
    private Integer maxContainers;
    
    @Column(name = "current_containers")
    private Integer currentContainers = 0;

    // ============= Network Configuration =============
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
    private String vlanIds; // JSON array
    
    @Column(name = "network_interfaces_count")
    private Integer networkInterfacesCount = 1;
    
    @Column(name = "network_speed_gbps")
    private Integer networkSpeedGbps;

    // ============= Storage Configuration =============
    @Enumerated(EnumType.STRING)
    @Column(name = "storage_type")
    private StorageType storageType;
    
    @Column(name = "storage_raid_level")
    private String storageRaidLevel;
    
    @Column(name = "total_storage_gb")
    private Integer totalStorageGb;
    
    @Column(name = "used_storage_gb")
    private Integer usedStorageGb = 0;
    
    @Column(name = "storage_iops")
    private Integer storageIops;

    // ============= Performance Metrics =============
    @Column(name = "cpu_benchmark_score")
    private Integer cpuBenchmarkScore;
    
    @Column(name = "memory_bandwidth_gbps", precision = 10, scale = 2)
    private BigDecimal memoryBandwidthGbps;
    
    @Column(name = "network_latency_ms", precision = 10, scale = 2)
    private BigDecimal networkLatencyMs;
    
    @Column(name = "uptime_days")
    private Integer uptimeDays = 0;
    
    @Column(name = "last_reboot_date")
    private LocalDateTime lastRebootDate;

    // ============= Compliance and Security =============
    @Enumerated(EnumType.STRING)
    @Column(name = "compliance_status")
    private ComplianceStatus complianceStatus;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "security_zone")
    private SecurityZone securityZone;
    
    @Column(name = "encryption_enabled")
    private Boolean encryptionEnabled = false;
    
    @Column(name = "last_security_scan_date")
    private LocalDate lastSecurityScanDate;
    
    @Column(name = "patch_level")
    private String patchLevel;
    
    @Column(name = "antivirus_status")
    private String antivirusStatus;

    // ============= Maintenance and Support =============
    @Column(name = "warranty_expiry_date")
    private LocalDate warrantyExpiryDate;
    
    @Column(name = "support_contract_id")
    private String supportContractId;
    
    @Column(name = "maintenance_window")
    private String maintenanceWindow;
    
    @Column(name = "last_maintenance_date")
    private LocalDate lastMaintenanceDate;
    
    @Column(name = "next_maintenance_date")
    private LocalDate nextMaintenanceDate;

    // ============= Monitoring and Management =============
    @Column(name = "monitoring_enabled")
    private Boolean monitoringEnabled = true;
    
    @Column(name = "monitoring_agent")
    private String monitoringAgent;
    
    @Column(name = "management_tool")
    private String managementTool; // ANSIBLE, PUPPET, CHEF, etc.
    
    @Column(name = "backup_enabled")
    private Boolean backupEnabled = false;
    
    @Column(name = "backup_schedule")
    private String backupSchedule;
    
    @Column(name = "last_backup_date")
    private LocalDateTime lastBackupDate;

    // ============= Additional Metadata =============
    @Column(columnDefinition = "TEXT")
    private String tags; // JSON array
    
    @Column(columnDefinition = "TEXT")
    private String notes;
    
    @Column(name = "custom_attributes", columnDefinition = "TEXT")
    private String customAttributes; // JSON object

    // ============= Relationships =============
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

    @OneToMany(mappedBy = "compute", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonManagedReference
    private List<Container> containers;

    // ============= Timestamps =============
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

    // ============= Enums =============
    public enum NodeType {
        DEDICATED, VM
    }

    public enum ComputeRole {
        DATABASE, APPLICATION, WEB, CACHE, QUEUE, 
        LOAD_BALANCER, PROXY, STORAGE, COMPUTE, 
        MONITORING, BACKUP, MANAGEMENT, OTHER
    }

    public enum EnvironmentType {
        PRODUCTION, STAGING, DEVELOPMENT, TEST, QA, UAT, DR
    }

    public enum ServiceTier {
        CRITICAL, HIGH, MEDIUM, LOW
    }

    public enum VirtualizationType {
        KVM, VMWARE, HYPERV, XEN, DOCKER, LXC, LXD, 
        VIRTUALBOX, QEMU, OPENVZ, PROXMOX, NONE
    }

    public enum ContainerRuntime {
        DOCKER, CONTAINERD, CRIO, PODMAN, LXC, NONE
    }

    public enum OrchestrationPlatform {
        KUBERNETES, OPENSHIFT, SWARM, MESOS, NOMAD, 
        RANCHER, ECS, EKS, GKE, AKS, NONE
    }

    public enum StorageType {
        SSD, HDD, NVME, HYBRID, SAN, NAS, DAS
    }

    public enum ComplianceStatus {
        COMPLIANT, NON_COMPLIANT, PENDING_REVIEW, EXEMPT
    }

    public enum SecurityZone {
        DMZ, INTERNAL, EXTERNAL, RESTRICTED, PUBLIC, PRIVATE
    }

    // ============= Constructors =============
    public ComputeEnhanced() {}

    // ============= Getters and Setters =============
    // Basic getters/setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    public NodeType getNodeType() { return nodeType; }
    public void setNodeType(NodeType nodeType) { this.nodeType = nodeType; }
    
    public String getHostname() { return hostname; }
    public void setHostname(String hostname) { this.hostname = hostname; }
    
    public String getIpAddress() { return ipAddress; }
    public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }
    
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    // Hardware getters/setters
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
    
    public Integer getCpuCores() { return cpuCores; }
    public void setCpuCores(Integer cpuCores) { this.cpuCores = cpuCores; }
    
    public Integer getMemoryGb() { return memoryGb; }
    public void setMemoryGb(Integer memoryGb) { this.memoryGb = memoryGb; }
    
    public Integer getDiskGb() { return diskGb; }
    public void setDiskGb(Integer diskGb) { this.diskGb = diskGb; }
    
    public Integer getPowerConsumptionWatts() { return powerConsumptionWatts; }
    public void setPowerConsumptionWatts(Integer powerConsumptionWatts) { this.powerConsumptionWatts = powerConsumptionWatts; }
    
    public Integer getThermalOutputBtu() { return thermalOutputBtu; }
    public void setThermalOutputBtu(Integer thermalOutputBtu) { this.thermalOutputBtu = thermalOutputBtu; }

    // OS and Software getters/setters
    public OSVersion getOsVersion() { return osVersion; }
    public void setOsVersion(OSVersion osVersion) { this.osVersion = osVersion; }
    
    public String getOsType() { return osType; }
    public void setOsType(String osType) { this.osType = osType; }
    
    public String getOsDistribution() { return osDistribution; }
    public void setOsDistribution(String osDistribution) { this.osDistribution = osDistribution; }
    
    public String getOsVersionString() { return osVersionString; }
    public void setOsVersionString(String osVersionString) { this.osVersionString = osVersionString; }
    
    public String getKernelVersion() { return kernelVersion; }
    public void setKernelVersion(String kernelVersion) { this.kernelVersion = kernelVersion; }
    
    public String getFirmwareVersion() { return firmwareVersion; }
    public void setFirmwareVersion(String firmwareVersion) { this.firmwareVersion = firmwareVersion; }
    
    public String getBiosVersion() { return biosVersion; }
    public void setBiosVersion(String biosVersion) { this.biosVersion = biosVersion; }

    // Compute Role getters/setters
    public ComputeRole getComputeRole() { return computeRole; }
    public void setComputeRole(ComputeRole computeRole) { this.computeRole = computeRole; }
    
    public String getPurpose() { return purpose; }
    public void setPurpose(String purpose) { this.purpose = purpose; }
    
    public EnvironmentType getEnvironmentType() { return environmentType; }
    public void setEnvironmentType(EnvironmentType environmentType) { this.environmentType = environmentType; }
    
    public ServiceTier getServiceTier() { return serviceTier; }
    public void setServiceTier(ServiceTier serviceTier) { this.serviceTier = serviceTier; }
    
    public String getBusinessUnit() { return businessUnit; }
    public void setBusinessUnit(String businessUnit) { this.businessUnit = businessUnit; }
    
    public String getCostCenter() { return costCenter; }
    public void setCostCenter(String costCenter) { this.costCenter = costCenter; }

    // Virtualization getters/setters
    public String getHypervisor() { return hypervisor; }
    public void setHypervisor(String hypervisor) { this.hypervisor = hypervisor; }
    
    public Boolean getIsPhysical() { return isPhysical; }
    public void setIsPhysical(Boolean isPhysical) { this.isPhysical = isPhysical; }
    
    public VirtualizationType getVirtualizationType() { return virtualizationType; }
    public void setVirtualizationType(VirtualizationType virtualizationType) { this.virtualizationType = virtualizationType; }
    
    public Boolean getSupportsContainers() { return supportsContainers; }
    public void setSupportsContainers(Boolean supportsContainers) { this.supportsContainers = supportsContainers; }
    
    public ContainerRuntime getContainerRuntime() { return containerRuntime; }
    public void setContainerRuntime(ContainerRuntime containerRuntime) { this.containerRuntime = containerRuntime; }
    
    public OrchestrationPlatform getOrchestrationPlatform() { return orchestrationPlatform; }
    public void setOrchestrationPlatform(OrchestrationPlatform orchestrationPlatform) { this.orchestrationPlatform = orchestrationPlatform; }
    
    public Integer getMaxContainers() { return maxContainers; }
    public void setMaxContainers(Integer maxContainers) { this.maxContainers = maxContainers; }
    
    public Integer getCurrentContainers() { return currentContainers; }
    public void setCurrentContainers(Integer currentContainers) { this.currentContainers = currentContainers; }

    // Network getters/setters
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

    // Storage getters/setters
    public StorageType getStorageType() { return storageType; }
    public void setStorageType(StorageType storageType) { this.storageType = storageType; }
    
    public String getStorageRaidLevel() { return storageRaidLevel; }
    public void setStorageRaidLevel(String storageRaidLevel) { this.storageRaidLevel = storageRaidLevel; }
    
    public Integer getTotalStorageGb() { return totalStorageGb; }
    public void setTotalStorageGb(Integer totalStorageGb) { this.totalStorageGb = totalStorageGb; }
    
    public Integer getUsedStorageGb() { return usedStorageGb; }
    public void setUsedStorageGb(Integer usedStorageGb) { this.usedStorageGb = usedStorageGb; }
    
    public Integer getStorageIops() { return storageIops; }
    public void setStorageIops(Integer storageIops) { this.storageIops = storageIops; }

    // Performance getters/setters
    public Integer getCpuBenchmarkScore() { return cpuBenchmarkScore; }
    public void setCpuBenchmarkScore(Integer cpuBenchmarkScore) { this.cpuBenchmarkScore = cpuBenchmarkScore; }
    
    public BigDecimal getMemoryBandwidthGbps() { return memoryBandwidthGbps; }
    public void setMemoryBandwidthGbps(BigDecimal memoryBandwidthGbps) { this.memoryBandwidthGbps = memoryBandwidthGbps; }
    
    public BigDecimal getNetworkLatencyMs() { return networkLatencyMs; }
    public void setNetworkLatencyMs(BigDecimal networkLatencyMs) { this.networkLatencyMs = networkLatencyMs; }
    
    public Integer getUptimeDays() { return uptimeDays; }
    public void setUptimeDays(Integer uptimeDays) { this.uptimeDays = uptimeDays; }
    
    public LocalDateTime getLastRebootDate() { return lastRebootDate; }
    public void setLastRebootDate(LocalDateTime lastRebootDate) { this.lastRebootDate = lastRebootDate; }

    // Compliance and Security getters/setters
    public ComplianceStatus getComplianceStatus() { return complianceStatus; }
    public void setComplianceStatus(ComplianceStatus complianceStatus) { this.complianceStatus = complianceStatus; }
    
    public SecurityZone getSecurityZone() { return securityZone; }
    public void setSecurityZone(SecurityZone securityZone) { this.securityZone = securityZone; }
    
    public Boolean getEncryptionEnabled() { return encryptionEnabled; }
    public void setEncryptionEnabled(Boolean encryptionEnabled) { this.encryptionEnabled = encryptionEnabled; }
    
    public LocalDate getLastSecurityScanDate() { return lastSecurityScanDate; }
    public void setLastSecurityScanDate(LocalDate lastSecurityScanDate) { this.lastSecurityScanDate = lastSecurityScanDate; }
    
    public String getPatchLevel() { return patchLevel; }
    public void setPatchLevel(String patchLevel) { this.patchLevel = patchLevel; }
    
    public String getAntivirusStatus() { return antivirusStatus; }
    public void setAntivirusStatus(String antivirusStatus) { this.antivirusStatus = antivirusStatus; }

    // Maintenance getters/setters
    public LocalDate getWarrantyExpiryDate() { return warrantyExpiryDate; }
    public void setWarrantyExpiryDate(LocalDate warrantyExpiryDate) { this.warrantyExpiryDate = warrantyExpiryDate; }
    
    public String getSupportContractId() { return supportContractId; }
    public void setSupportContractId(String supportContractId) { this.supportContractId = supportContractId; }
    
    public String getMaintenanceWindow() { return maintenanceWindow; }
    public void setMaintenanceWindow(String maintenanceWindow) { this.maintenanceWindow = maintenanceWindow; }
    
    public LocalDate getLastMaintenanceDate() { return lastMaintenanceDate; }
    public void setLastMaintenanceDate(LocalDate lastMaintenanceDate) { this.lastMaintenanceDate = lastMaintenanceDate; }
    
    public LocalDate getNextMaintenanceDate() { return nextMaintenanceDate; }
    public void setNextMaintenanceDate(LocalDate nextMaintenanceDate) { this.nextMaintenanceDate = nextMaintenanceDate; }

    // Monitoring getters/setters
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

    // Metadata getters/setters
    public String getTags() { return tags; }
    public void setTags(String tags) { this.tags = tags; }
    
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    
    public String getCustomAttributes() { return customAttributes; }
    public void setCustomAttributes(String customAttributes) { this.customAttributes = customAttributes; }

    // Relationship getters/setters
    public Cloud getCloud() { return cloud; }
    public void setCloud(Cloud cloud) { this.cloud = cloud; }
    
    public Datacenter getDatacenter() { return datacenter; }
    public void setDatacenter(Datacenter datacenter) { this.datacenter = datacenter; }
    
    public ResourcePool getResourcePool() { return resourcePool; }
    public void setResourcePool(ResourcePool resourcePool) { this.resourcePool = resourcePool; }
    
    public List<Container> getContainers() { return containers; }
    public void setContainers(List<Container> containers) { this.containers = containers; }

    // Timestamp getters/setters
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}