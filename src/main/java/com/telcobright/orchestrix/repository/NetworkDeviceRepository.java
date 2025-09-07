package com.telcobright.orchestrix.repository;

import com.telcobright.orchestrix.entity.NetworkDevice;
import com.telcobright.orchestrix.entity.ResourcePool;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface NetworkDeviceRepository extends JpaRepository<NetworkDevice, Long> {
    
    // Find by basic properties
    Optional<NetworkDevice> findByName(String name);
    
    List<NetworkDevice> findByDeviceType(String deviceType);
    
    List<NetworkDevice> findByVendor(String vendor);
    
    List<NetworkDevice> findByStatus(String status);
    
    List<NetworkDevice> findByOperationalStatus(String operationalStatus);
    
    List<NetworkDevice> findByEnvironment(String environment);
    
    List<NetworkDevice> findByCriticality(String criticality);
    
    // Find by location
    List<NetworkDevice> findByDatacenterId(Integer datacenterId);
    
    List<NetworkDevice> findByResourcePoolId(Long resourcePoolId);
    
    // Find by network properties
    List<NetworkDevice> findByManagementIp(String managementIp);
    
    Optional<NetworkDevice> findBySerialNumber(String serialNumber);
    
    Optional<NetworkDevice> findByMacAddress(String macAddress);
    
    // Complex queries with JOIN FETCH for performance
    @Query("SELECT nd FROM NetworkDevice nd LEFT JOIN FETCH nd.datacenter LEFT JOIN FETCH nd.resourcePool WHERE nd.datacenter.id = :datacenterId")
    List<NetworkDevice> findByDatacenterIdWithDetails(@Param("datacenterId") Integer datacenterId);
    
    @Query("SELECT nd FROM NetworkDevice nd LEFT JOIN FETCH nd.datacenter LEFT JOIN FETCH nd.resourcePool")
    List<NetworkDevice> findAllWithDetails();
    
    @Query("SELECT nd FROM NetworkDevice nd LEFT JOIN FETCH nd.datacenter LEFT JOIN FETCH nd.resourcePool WHERE nd.id = :id")
    Optional<NetworkDevice> findByIdWithDetails(@Param("id") Long id);
    
    // Find active devices
    @Query("SELECT nd FROM NetworkDevice nd WHERE nd.status = 'ACTIVE' ORDER BY nd.name")
    List<NetworkDevice> findAllActive();
    
    @Query("SELECT nd FROM NetworkDevice nd WHERE nd.status = 'ACTIVE' AND nd.operationalStatus = 'UP' ORDER BY nd.name")
    List<NetworkDevice> findAllActiveAndOnline();
    
    // Find by device type and status
    @Query("SELECT nd FROM NetworkDevice nd WHERE nd.deviceType = :deviceType AND nd.status = 'ACTIVE' ORDER BY nd.name")
    List<NetworkDevice> findActiveByDeviceType(@Param("deviceType") String deviceType);
    
    // Performance and monitoring queries
    @Query("SELECT nd FROM NetworkDevice nd WHERE nd.monitoringEnabled = true ORDER BY nd.name")
    List<NetworkDevice> findAllMonitored();
    
    @Query("SELECT nd FROM NetworkDevice nd WHERE nd.cpuUtilizationPercent > :cpuThreshold OR nd.memoryUtilizationPercent > :memoryThreshold")
    List<NetworkDevice> findHighUtilization(@Param("cpuThreshold") Double cpuThreshold, @Param("memoryThreshold") Double memoryThreshold);
    
    @Query("SELECT nd FROM NetworkDevice nd WHERE nd.operationalStatus = 'DOWN' OR nd.status = 'ERROR'")
    List<NetworkDevice> findProblematicDevices();
    
    @Query("SELECT nd FROM NetworkDevice nd WHERE nd.criticality = 'critical' ORDER BY nd.name")
    List<NetworkDevice> findCriticalDevices();
    
    // Search functionality
    @Query("SELECT nd FROM NetworkDevice nd WHERE " +
           "LOWER(nd.name) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(nd.displayName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(nd.description) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(nd.vendor) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(nd.model) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "nd.managementIp LIKE CONCAT('%', :searchTerm, '%') " +
           "ORDER BY nd.name")
    List<NetworkDevice> searchDevices(@Param("searchTerm") String searchTerm);
    
    // Statistics and aggregation queries
    @Query("SELECT COUNT(nd) FROM NetworkDevice nd WHERE nd.status = 'ACTIVE'")
    Long countActiveDevices();
    
    @Query("SELECT COUNT(nd) FROM NetworkDevice nd WHERE nd.operationalStatus = 'UP'")
    Long countOnlineDevices();
    
    @Query("SELECT nd.deviceType, COUNT(nd) FROM NetworkDevice nd WHERE nd.status = 'ACTIVE' GROUP BY nd.deviceType ORDER BY nd.deviceType")
    List<Object[]> countDevicesByType();
    
    @Query("SELECT nd.vendor, COUNT(nd) FROM NetworkDevice nd WHERE nd.status = 'ACTIVE' GROUP BY nd.vendor ORDER BY COUNT(nd) DESC")
    List<Object[]> countDevicesByVendor();
    
    @Query("SELECT nd.environment, COUNT(nd) FROM NetworkDevice nd GROUP BY nd.environment ORDER BY nd.environment")
    List<Object[]> countDevicesByEnvironment();
    
    // Maintenance and backup queries
    @Query("SELECT nd FROM NetworkDevice nd WHERE nd.lastBackupDate IS NULL OR nd.lastBackupDate < :beforeDate ORDER BY nd.lastBackupDate")
    List<NetworkDevice> findDevicesNeedingBackup(@Param("beforeDate") java.time.LocalDateTime beforeDate);
    
    @Query("SELECT nd FROM NetworkDevice nd WHERE nd.status = 'MAINTENANCE' ORDER BY nd.updatedAt DESC")
    List<NetworkDevice> findDevicesInMaintenance();
    
    // Network topology queries
    @Query("SELECT nd FROM NetworkDevice nd WHERE nd.subnet = :subnet ORDER BY nd.managementIp")
    List<NetworkDevice> findBySubnet(@Param("subnet") String subnet);
    
    @Query("SELECT nd FROM NetworkDevice nd WHERE nd.vlanId = :vlanId ORDER BY nd.name")
    List<NetworkDevice> findByVlanId(@Param("vlanId") Integer vlanId);
    
    // Capacity and resource queries  
    @Query("SELECT nd FROM NetworkDevice nd WHERE nd.resourcePool = :resourcePool")
    List<NetworkDevice> findByResourcePool(@Param("resourcePool") ResourcePool resourcePool);
    
    @Query("SELECT SUM(nd.powerConsumptionWatts) FROM NetworkDevice nd WHERE nd.status = 'ACTIVE' AND nd.datacenter.id = :datacenterId")
    Long getTotalPowerConsumptionByDatacenter(@Param("datacenterId") Integer datacenterId);
    
    // Custom query methods for dashboard and reporting
    @Query("SELECT nd FROM NetworkDevice nd WHERE nd.createdAt >= :fromDate ORDER BY nd.createdAt DESC")
    List<NetworkDevice> findDevicesCreatedSince(@Param("fromDate") java.time.LocalDateTime fromDate);
    
    @Query("SELECT nd FROM NetworkDevice nd WHERE nd.lastSeen IS NOT NULL AND nd.lastSeen < :beforeDate ORDER BY nd.lastSeen")
    List<NetworkDevice> findStaleDevices(@Param("beforeDate") java.time.LocalDateTime beforeDate);
    
    // Tenant-based queries
    @Query("SELECT nd FROM NetworkDevice nd WHERE nd.datacenter.cloud.partner.roles LIKE '%self%'")
    List<NetworkDevice> findAllByOrganizationTenant();
    
    @Query("SELECT nd FROM NetworkDevice nd WHERE nd.datacenter.cloud.partner.roles NOT LIKE '%self%'")
    List<NetworkDevice> findAllByOtherTenant();
}