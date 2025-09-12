package com.telcobright.orchestrix.repository;

import com.telcobright.orchestrix.entity.IPAddress;
import com.telcobright.orchestrix.entity.IPAddress.IPAddressType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public interface IPAddressRepository extends JpaRepository<IPAddress, Long> {
    
    // Find by IP address
    Optional<IPAddress> findByIpAddress(String ipAddress);
    
    // Check if IP address exists
    boolean existsByIpAddress(String ipAddress);
    
    // Find all IP addresses for a compute device
    List<IPAddress> findByComputeId(Long computeId);
    
    // Find all IP addresses for a network device
    List<IPAddress> findByNetworkDeviceId(Long networkDeviceId);
    
    // Find all IP addresses for a container
    List<IPAddress> findByContainerId(Long containerId);
    
    // Find all active IP addresses
    List<IPAddress> findByIsActiveTrue();
    
    // Find all primary IP addresses
    List<IPAddress> findByIsPrimaryTrue();
    
    // Find IP addresses by type
    @Query("SELECT ip FROM IPAddress ip JOIN ip.types t WHERE t IN :types")
    List<IPAddress> findByTypes(@Param("types") Set<IPAddressType> types);
    
    // Find IP addresses by single type
    @Query("SELECT ip FROM IPAddress ip JOIN ip.types t WHERE t = :type")
    List<IPAddress> findByType(@Param("type") IPAddressType type);
    
    // Find IP addresses by network segment
    List<IPAddress> findByNetworkSegment(String networkSegment);
    
    // Find IP addresses by network zone
    List<IPAddress> findByNetworkZone(String networkZone);
    
    // Find IP addresses by VLAN ID
    List<IPAddress> findByVlanId(String vlanId);
    
    // Find unassigned IP addresses
    @Query("SELECT ip FROM IPAddress ip WHERE ip.compute IS NULL AND ip.networkDevice IS NULL AND ip.container IS NULL")
    List<IPAddress> findUnassigned();
    
    // Find IP addresses assigned to any device
    @Query("SELECT ip FROM IPAddress ip WHERE ip.compute IS NOT NULL OR ip.networkDevice IS NOT NULL OR ip.container IS NOT NULL")
    List<IPAddress> findAssigned();
    
    // Find IP addresses by MAC address
    Optional<IPAddress> findByMacAddress(String macAddress);
    
    // Find IP addresses by gateway
    List<IPAddress> findByGateway(String gateway);
    
    // Find IP addresses by subnet mask
    List<IPAddress> findBySubnetMask(Integer subnetMask);
    
    // Search IP addresses by partial match
    @Query("SELECT ip FROM IPAddress ip WHERE ip.ipAddress LIKE %:search% OR ip.description LIKE %:search% OR ip.notes LIKE %:search%")
    List<IPAddress> searchIpAddresses(@Param("search") String search);
    
    // Find IP addresses with specific tags
    @Query("SELECT ip FROM IPAddress ip WHERE ip.tags LIKE %:tag%")
    List<IPAddress> findByTag(@Param("tag") String tag);
    
    // Get count of IP addresses by type for a specific device
    @Query("SELECT COUNT(ip) FROM IPAddress ip JOIN ip.types t WHERE t = :type AND " +
           "((:deviceType = 'COMPUTE' AND ip.compute.id = :deviceId) OR " +
           "(:deviceType = 'NETWORK_DEVICE' AND ip.networkDevice.id = :deviceId) OR " +
           "(:deviceType = 'CONTAINER' AND ip.container.id = :deviceId))")
    Long countByDeviceAndType(@Param("deviceType") String deviceType, 
                              @Param("deviceId") Long deviceId, 
                              @Param("type") IPAddressType type);
    
    // Find duplicate IP addresses (same IP assigned to multiple devices)
    @Query("SELECT ip.ipAddress, COUNT(ip) as count FROM IPAddress ip " +
           "GROUP BY ip.ipAddress HAVING COUNT(ip) > 1")
    List<Object[]> findDuplicateIpAddresses();
    
    // Find IP addresses by assignment method
    List<IPAddress> findByAssignmentMethod(IPAddress.AssignmentMethod assignmentMethod);
    
    // Find expired DHCP leases
    @Query("SELECT ip FROM IPAddress ip WHERE ip.assignmentMethod = 'DHCP' " +
           "AND ip.leaseExpiresAt IS NOT NULL AND ip.leaseExpiresAt < CURRENT_TIMESTAMP")
    List<IPAddress> findExpiredDhcpLeases();
}