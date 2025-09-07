package com.telcobright.orchestrix.controller;

import com.telcobright.orchestrix.entity.NetworkDevice;
import com.telcobright.orchestrix.entity.Datacenter;
import com.telcobright.orchestrix.entity.ResourcePool;
import com.telcobright.orchestrix.repository.NetworkDeviceRepository;
import com.telcobright.orchestrix.repository.DatacenterRepository;
import com.telcobright.orchestrix.repository.ResourcePoolRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/network-devices")
@CrossOrigin(origins = "*")
public class NetworkDeviceController {
    
    @Autowired
    private NetworkDeviceRepository networkDeviceRepository;
    
    @Autowired
    private DatacenterRepository datacenterRepository;
    
    @Autowired
    private ResourcePoolRepository resourcePoolRepository;
    
    // Get all network devices
    @GetMapping
    public List<NetworkDevice> getAllNetworkDevices(@RequestParam(value = "tenant", required = false) String tenant) {
        if ("organization".equals(tenant)) {
            return networkDeviceRepository.findAllByOrganizationTenant();
        } else if ("other".equals(tenant)) {
            return networkDeviceRepository.findAllByOtherTenant();
        }
        return networkDeviceRepository.findAllWithDetails();
    }
    
    // Get network device by ID
    @GetMapping("/{id}")
    public ResponseEntity<?> getNetworkDeviceById(@PathVariable Long id) {
        return networkDeviceRepository.findByIdWithDetails(id)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }
    
    // Get active network devices
    @GetMapping("/active")
    public List<NetworkDevice> getActiveNetworkDevices() {
        return networkDeviceRepository.findAllActive();
    }
    
    // Get network devices by datacenter
    @GetMapping("/datacenter/{datacenterId}")
    public List<NetworkDevice> getNetworkDevicesByDatacenter(@PathVariable Integer datacenterId) {
        return networkDeviceRepository.findByDatacenterIdWithDetails(datacenterId);
    }
    
    // Get network devices by device type
    @GetMapping("/type/{deviceType}")
    public List<NetworkDevice> getNetworkDevicesByType(@PathVariable String deviceType) {
        return networkDeviceRepository.findActiveByDeviceType(deviceType);
    }
    
    // Get network devices by vendor
    @GetMapping("/vendor/{vendor}")
    public List<NetworkDevice> getNetworkDevicesByVendor(@PathVariable String vendor) {
        return networkDeviceRepository.findByVendor(vendor);
    }
    
    // Search network devices
    @GetMapping("/search")
    public List<NetworkDevice> searchNetworkDevices(@RequestParam("q") String searchTerm) {
        return networkDeviceRepository.searchDevices(searchTerm);
    }
    
    // Get critical network devices
    @GetMapping("/critical")
    public List<NetworkDevice> getCriticalNetworkDevices() {
        return networkDeviceRepository.findCriticalDevices();
    }
    
    // Get problematic network devices (down or error status)
    @GetMapping("/problems")
    public List<NetworkDevice> getProblematicNetworkDevices() {
        return networkDeviceRepository.findProblematicDevices();
    }
    
    // Get high utilization devices
    @GetMapping("/high-utilization")
    public List<NetworkDevice> getHighUtilizationDevices(
            @RequestParam(defaultValue = "80.0") Double cpuThreshold,
            @RequestParam(defaultValue = "85.0") Double memoryThreshold) {
        return networkDeviceRepository.findHighUtilization(cpuThreshold, memoryThreshold);
    }
    
    // Get devices needing backup
    @GetMapping("/backup-needed")
    public List<NetworkDevice> getDevicesNeedingBackup(@RequestParam(defaultValue = "7") Integer daysAgo) {
        LocalDateTime beforeDate = LocalDateTime.now().minusDays(daysAgo);
        return networkDeviceRepository.findDevicesNeedingBackup(beforeDate);
    }
    
    // Get devices statistics
    @GetMapping("/statistics")
    public Map<String, Object> getNetworkDeviceStatistics() {
        Map<String, Object> stats = new HashMap<>();
        
        stats.put("totalDevices", networkDeviceRepository.count());
        stats.put("activeDevices", networkDeviceRepository.countActiveDevices());
        stats.put("onlineDevices", networkDeviceRepository.countOnlineDevices());
        stats.put("devicesByType", networkDeviceRepository.countDevicesByType());
        stats.put("devicesByVendor", networkDeviceRepository.countDevicesByVendor());
        stats.put("devicesByEnvironment", networkDeviceRepository.countDevicesByEnvironment());
        
        return stats;
    }
    
    // Create new network device
    @PostMapping
    public ResponseEntity<?> createNetworkDevice(@RequestBody NetworkDevice networkDevice) {
        try {
            // Validate unique constraints
            if (networkDevice.getName() != null && networkDeviceRepository.findByName(networkDevice.getName()).isPresent()) {
                Map<String, String> error = new HashMap<>();
                error.put("error", "Network device with this name already exists");
                return ResponseEntity.badRequest().body(error);
            }
            
            if (networkDevice.getSerialNumber() != null && !networkDevice.getSerialNumber().isEmpty() &&
                networkDeviceRepository.findBySerialNumber(networkDevice.getSerialNumber()).isPresent()) {
                Map<String, String> error = new HashMap<>();
                error.put("error", "Network device with this serial number already exists");
                return ResponseEntity.badRequest().body(error);
            }
            
            if (networkDevice.getMacAddress() != null && !networkDevice.getMacAddress().isEmpty() &&
                networkDeviceRepository.findByMacAddress(networkDevice.getMacAddress()).isPresent()) {
                Map<String, String> error = new HashMap<>();
                error.put("error", "Network device with this MAC address already exists");
                return ResponseEntity.badRequest().body(error);
            }
            
            // Set relationships if provided
            if (networkDevice.getDatacenter() != null && networkDevice.getDatacenter().getId() != null) {
                Optional<Datacenter> datacenter = datacenterRepository.findById(networkDevice.getDatacenter().getId());
                if (datacenter.isPresent()) {
                    networkDevice.setDatacenter(datacenter.get());
                } else {
                    Map<String, String> error = new HashMap<>();
                    error.put("error", "Invalid datacenter ID");
                    return ResponseEntity.badRequest().body(error);
                }
            }
            
            if (networkDevice.getResourcePool() != null && networkDevice.getResourcePool().getId() != null) {
                Optional<ResourcePool> resourcePool = resourcePoolRepository.findById(networkDevice.getResourcePool().getId());
                if (resourcePool.isPresent()) {
                    networkDevice.setResourcePool(resourcePool.get());
                } else {
                    Map<String, String> error = new HashMap<>();
                    error.put("error", "Invalid resource pool ID");
                    return ResponseEntity.badRequest().body(error);
                }
            }
            
            // Set audit fields
            networkDevice.setCreatedBy("system"); // TODO: Get from authentication
            
            NetworkDevice saved = networkDeviceRepository.save(networkDevice);
            return ResponseEntity.ok(saved);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Failed to create network device: " + e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }
    
    // Update network device
    @PutMapping("/{id}")
    public ResponseEntity<?> updateNetworkDevice(@PathVariable Long id, @RequestBody NetworkDevice networkDeviceData) {
        return networkDeviceRepository.findById(id)
            .map(networkDevice -> {
                try {
                    // Update basic properties
                    if (networkDeviceData.getName() != null) {
                        networkDevice.setName(networkDeviceData.getName());
                    }
                    if (networkDeviceData.getDisplayName() != null) {
                        networkDevice.setDisplayName(networkDeviceData.getDisplayName());
                    }
                    if (networkDeviceData.getDeviceType() != null) {
                        networkDevice.setDeviceType(networkDeviceData.getDeviceType());
                    }
                    if (networkDeviceData.getVendor() != null) {
                        networkDevice.setVendor(networkDeviceData.getVendor());
                    }
                    if (networkDeviceData.getModel() != null) {
                        networkDevice.setModel(networkDeviceData.getModel());
                    }
                    if (networkDeviceData.getVersion() != null) {
                        networkDevice.setVersion(networkDeviceData.getVersion());
                    }
                    if (networkDeviceData.getSerialNumber() != null) {
                        networkDevice.setSerialNumber(networkDeviceData.getSerialNumber());
                    }
                    if (networkDeviceData.getMacAddress() != null) {
                        networkDevice.setMacAddress(networkDeviceData.getMacAddress());
                    }
                    
                    // Update network configuration
                    if (networkDeviceData.getManagementIp() != null) {
                        networkDevice.setManagementIp(networkDeviceData.getManagementIp());
                    }
                    if (networkDeviceData.getManagementPort() != null) {
                        networkDevice.setManagementPort(networkDeviceData.getManagementPort());
                    }
                    if (networkDeviceData.getManagementProtocol() != null) {
                        networkDevice.setManagementProtocol(networkDeviceData.getManagementProtocol());
                    }
                    if (networkDeviceData.getSubnet() != null) {
                        networkDevice.setSubnet(networkDeviceData.getSubnet());
                    }
                    if (networkDeviceData.getGateway() != null) {
                        networkDevice.setGateway(networkDeviceData.getGateway());
                    }
                    if (networkDeviceData.getDnsServers() != null) {
                        networkDevice.setDnsServers(networkDeviceData.getDnsServers());
                    }
                    if (networkDeviceData.getVlanId() != null) {
                        networkDevice.setVlanId(networkDeviceData.getVlanId());
                    }
                    
                    // Update location
                    if (networkDeviceData.getDatacenter() != null && networkDeviceData.getDatacenter().getId() != null) {
                        Optional<Datacenter> datacenter = datacenterRepository.findById(networkDeviceData.getDatacenter().getId());
                        datacenter.ifPresent(networkDevice::setDatacenter);
                    }
                    
                    if (networkDeviceData.getResourcePool() != null && networkDeviceData.getResourcePool().getId() != null) {
                        Optional<ResourcePool> resourcePool = resourcePoolRepository.findById(networkDeviceData.getResourcePool().getId());
                        resourcePool.ifPresent(networkDevice::setResourcePool);
                    }
                    
                    if (networkDeviceData.getRackPosition() != null) {
                        networkDevice.setRackPosition(networkDeviceData.getRackPosition());
                    }
                    if (networkDeviceData.getPortCount() != null) {
                        networkDevice.setPortCount(networkDeviceData.getPortCount());
                    }
                    if (networkDeviceData.getPowerConsumptionWatts() != null) {
                        networkDevice.setPowerConsumptionWatts(networkDeviceData.getPowerConsumptionWatts());
                    }
                    
                    // Update operational status
                    if (networkDeviceData.getStatus() != null) {
                        networkDevice.setStatus(networkDeviceData.getStatus());
                    }
                    if (networkDeviceData.getOperationalStatus() != null) {
                        networkDevice.setOperationalStatus(networkDeviceData.getOperationalStatus());
                    }
                    if (networkDeviceData.getLastSeen() != null) {
                        networkDevice.setLastSeen(networkDeviceData.getLastSeen());
                    }
                    if (networkDeviceData.getUptimeHours() != null) {
                        networkDevice.setUptimeHours(networkDeviceData.getUptimeHours());
                    }
                    
                    // Update performance metrics
                    if (networkDeviceData.getCpuUtilizationPercent() != null) {
                        networkDevice.setCpuUtilizationPercent(networkDeviceData.getCpuUtilizationPercent());
                    }
                    if (networkDeviceData.getMemoryUtilizationPercent() != null) {
                        networkDevice.setMemoryUtilizationPercent(networkDeviceData.getMemoryUtilizationPercent());
                    }
                    if (networkDeviceData.getBandwidthUtilizationMbps() != null) {
                        networkDevice.setBandwidthUtilizationMbps(networkDeviceData.getBandwidthUtilizationMbps());
                    }
                    if (networkDeviceData.getTotalMemoryMb() != null) {
                        networkDevice.setTotalMemoryMb(networkDeviceData.getTotalMemoryMb());
                    }
                    if (networkDeviceData.getAvailableStorageGb() != null) {
                        networkDevice.setAvailableStorageGb(networkDeviceData.getAvailableStorageGb());
                    }
                    
                    // Update configuration and management
                    if (networkDeviceData.getFirmwareVersion() != null) {
                        networkDevice.setFirmwareVersion(networkDeviceData.getFirmwareVersion());
                    }
                    if (networkDeviceData.getConfigurationBackupPath() != null) {
                        networkDevice.setConfigurationBackupPath(networkDeviceData.getConfigurationBackupPath());
                    }
                    if (networkDeviceData.getLastBackupDate() != null) {
                        networkDevice.setLastBackupDate(networkDeviceData.getLastBackupDate());
                    }
                    if (networkDeviceData.getSnmpCommunity() != null) {
                        networkDevice.setSnmpCommunity(networkDeviceData.getSnmpCommunity());
                    }
                    if (networkDeviceData.getSshUsername() != null) {
                        networkDevice.setSshUsername(networkDeviceData.getSshUsername());
                    }
                    if (networkDeviceData.getSshKeyPath() != null) {
                        networkDevice.setSshKeyPath(networkDeviceData.getSshKeyPath());
                    }
                    
                    // Update business fields
                    if (networkDeviceData.getEnvironment() != null) {
                        networkDevice.setEnvironment(networkDeviceData.getEnvironment());
                    }
                    if (networkDeviceData.getCriticality() != null) {
                        networkDevice.setCriticality(networkDeviceData.getCriticality());
                    }
                    if (networkDeviceData.getComplianceZone() != null) {
                        networkDevice.setComplianceZone(networkDeviceData.getComplianceZone());
                    }
                    if (networkDeviceData.getCostCenter() != null) {
                        networkDevice.setCostCenter(networkDeviceData.getCostCenter());
                    }
                    if (networkDeviceData.getOwnerContact() != null) {
                        networkDevice.setOwnerContact(networkDeviceData.getOwnerContact());
                    }
                    if (networkDeviceData.getSupportContract() != null) {
                        networkDevice.setSupportContract(networkDeviceData.getSupportContract());
                    }
                    
                    // Update monitoring settings
                    if (networkDeviceData.getMonitoringEnabled() != null) {
                        networkDevice.setMonitoringEnabled(networkDeviceData.getMonitoringEnabled());
                    }
                    if (networkDeviceData.getAlertEmail() != null) {
                        networkDevice.setAlertEmail(networkDeviceData.getAlertEmail());
                    }
                    if (networkDeviceData.getAlertThresholdCpu() != null) {
                        networkDevice.setAlertThresholdCpu(networkDeviceData.getAlertThresholdCpu());
                    }
                    if (networkDeviceData.getAlertThresholdMemory() != null) {
                        networkDevice.setAlertThresholdMemory(networkDeviceData.getAlertThresholdMemory());
                    }
                    if (networkDeviceData.getAlertThresholdBandwidth() != null) {
                        networkDevice.setAlertThresholdBandwidth(networkDeviceData.getAlertThresholdBandwidth());
                    }
                    
                    // Update metadata
                    if (networkDeviceData.getDescription() != null) {
                        networkDevice.setDescription(networkDeviceData.getDescription());
                    }
                    if (networkDeviceData.getTags() != null) {
                        networkDevice.setTags(networkDeviceData.getTags());
                    }
                    if (networkDeviceData.getNotes() != null) {
                        networkDevice.setNotes(networkDeviceData.getNotes());
                    }
                    
                    // Set audit fields
                    networkDevice.setUpdatedBy("system"); // TODO: Get from authentication
                    
                    NetworkDevice updated = networkDeviceRepository.save(networkDevice);
                    return ResponseEntity.ok(updated);
                } catch (Exception e) {
                    Map<String, String> error = new HashMap<>();
                    error.put("error", "Failed to update network device: " + e.getMessage());
                    return ResponseEntity.badRequest().body(error);
                }
            })
            .orElse(ResponseEntity.notFound().build());
    }
    
    // Delete network device
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteNetworkDevice(@PathVariable Long id) {
        return networkDeviceRepository.findById(id)
            .map(networkDevice -> {
                networkDeviceRepository.delete(networkDevice);
                Map<String, String> response = new HashMap<>();
                response.put("message", "Network device deleted successfully");
                return ResponseEntity.ok(response);
            })
            .orElse(ResponseEntity.notFound().build());
    }
    
    // Update device status
    @PatchMapping("/{id}/status")
    public ResponseEntity<?> updateDeviceStatus(@PathVariable Long id, @RequestBody Map<String, String> statusData) {
        return networkDeviceRepository.findById(id)
            .map(networkDevice -> {
                if (statusData.containsKey("status")) {
                    networkDevice.setStatus(statusData.get("status"));
                }
                if (statusData.containsKey("operationalStatus")) {
                    networkDevice.setOperationalStatus(statusData.get("operationalStatus"));
                    if ("UP".equals(statusData.get("operationalStatus"))) {
                        networkDevice.setLastSeen(LocalDateTime.now());
                    }
                }
                
                networkDevice.setUpdatedBy("system");
                NetworkDevice updated = networkDeviceRepository.save(networkDevice);
                return ResponseEntity.ok(updated);
            })
            .orElse(ResponseEntity.notFound().build());
    }
    
    // Update device performance metrics
    @PatchMapping("/{id}/metrics")
    public ResponseEntity<?> updateDeviceMetrics(@PathVariable Long id, @RequestBody Map<String, Object> metricsData) {
        return networkDeviceRepository.findById(id)
            .map(networkDevice -> {
                if (metricsData.containsKey("cpuUtilizationPercent")) {
                    networkDevice.setCpuUtilizationPercent(new java.math.BigDecimal(metricsData.get("cpuUtilizationPercent").toString()));
                }
                if (metricsData.containsKey("memoryUtilizationPercent")) {
                    networkDevice.setMemoryUtilizationPercent(new java.math.BigDecimal(metricsData.get("memoryUtilizationPercent").toString()));
                }
                if (metricsData.containsKey("bandwidthUtilizationMbps")) {
                    networkDevice.setBandwidthUtilizationMbps(new java.math.BigDecimal(metricsData.get("bandwidthUtilizationMbps").toString()));
                }
                if (metricsData.containsKey("uptimeHours")) {
                    networkDevice.setUptimeHours(Long.valueOf(metricsData.get("uptimeHours").toString()));
                }
                
                networkDevice.setLastSeen(LocalDateTime.now());
                networkDevice.setUpdatedBy("monitoring-system");
                NetworkDevice updated = networkDeviceRepository.save(networkDevice);
                return ResponseEntity.ok(updated);
            })
            .orElse(ResponseEntity.notFound().build());
    }
}