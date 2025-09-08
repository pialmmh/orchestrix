package com.telcobright.orchestrix.repository;

import com.telcobright.orchestrix.entity.RemoteAccess;
import com.telcobright.orchestrix.entity.RemoteAccess.DeviceType;
import com.telcobright.orchestrix.entity.RemoteAccess.AccessType;
import com.telcobright.orchestrix.entity.RemoteAccess.TestStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface RemoteAccessRepository extends JpaRepository<RemoteAccess, Integer> {
    
    // Find by device
    List<RemoteAccess> findByDeviceTypeAndDeviceId(DeviceType deviceType, Integer deviceId);
    
    List<RemoteAccess> findByDeviceTypeAndDeviceIdAndIsActive(DeviceType deviceType, Integer deviceId, Boolean isActive);
    
    Optional<RemoteAccess> findByDeviceTypeAndDeviceIdAndIsPrimary(DeviceType deviceType, Integer deviceId, Boolean isPrimary);
    
    // Find by Bitwarden
    Optional<RemoteAccess> findByBitwardenItemId(String bitwardenItemId);
    
    List<RemoteAccess> findByBitwardenOrganizationId(String bitwardenOrganizationId);
    
    List<RemoteAccess> findByBitwardenCollectionId(String bitwardenCollectionId);
    
    // Find by access type
    List<RemoteAccess> findByAccessType(AccessType accessType);
    
    List<RemoteAccess> findByAccessTypeAndIsActive(AccessType accessType, Boolean isActive);
    
    // Find credentials needing rotation
    @Query("SELECT ra FROM RemoteAccess ra WHERE ra.rotationRequired = true AND ra.nextRotationDate <= :date AND ra.isActive = true")
    List<RemoteAccess> findCredentialsNeedingRotation(@Param("date") LocalDateTime date);
    
    // Find expired credentials
    @Query("SELECT ra FROM RemoteAccess ra WHERE ra.validUntil IS NOT NULL AND ra.validUntil <= :date")
    List<RemoteAccess> findExpiredCredentials(@Param("date") LocalDateTime date);
    
    // Find credentials by test status
    List<RemoteAccess> findByLastTestStatus(TestStatus status);
    
    @Query("SELECT ra FROM RemoteAccess ra WHERE ra.lastTestStatus = :status AND ra.lastTestedAt <= :beforeDate")
    List<RemoteAccess> findByLastTestStatusAndTestedBefore(@Param("status") TestStatus status, @Param("beforeDate") LocalDateTime beforeDate);
    
    // Find credentials that haven't been tested recently
    @Query("SELECT ra FROM RemoteAccess ra WHERE ra.isActive = true AND (ra.lastTestedAt IS NULL OR ra.lastTestedAt <= :beforeDate)")
    List<RemoteAccess> findCredentialsNeedingTest(@Param("beforeDate") LocalDateTime beforeDate);
    
    // Find by compliance
    @Query(value = "SELECT * FROM remote_access WHERE JSON_CONTAINS(compliance_tags, :tag)", nativeQuery = true)
    List<RemoteAccess> findByComplianceTag(@Param("tag") String tag);
    
    // Find emergency access
    List<RemoteAccess> findByIsEmergencyAccess(Boolean isEmergencyAccess);
    
    // Find with MFA
    List<RemoteAccess> findByRequiresMfa(Boolean requiresMfa);
    
    // Count by device type
    Long countByDeviceType(DeviceType deviceType);
    
    Long countByDeviceTypeAndIsActive(DeviceType deviceType, Boolean isActive);
    
    // Statistics queries
    @Query("SELECT ra.deviceType, COUNT(ra) FROM RemoteAccess ra WHERE ra.isActive = true GROUP BY ra.deviceType")
    List<Object[]> countActiveByDeviceType();
    
    @Query("SELECT ra.accessType, COUNT(ra) FROM RemoteAccess ra WHERE ra.isActive = true GROUP BY ra.accessType")
    List<Object[]> countActiveByAccessType();
    
    @Query("SELECT ra.authMethod, COUNT(ra) FROM RemoteAccess ra WHERE ra.isActive = true GROUP BY ra.authMethod")
    List<Object[]> countActiveByAuthMethod();
    
    // Find credentials with certificates expiring soon
    @Query("SELECT ra FROM RemoteAccess ra WHERE ra.certificateExpiry IS NOT NULL AND ra.certificateExpiry <= :expiryDate AND ra.isActive = true")
    List<RemoteAccess> findCertificatesExpiringSoon(@Param("expiryDate") LocalDateTime expiryDate);
    
    // Search by various criteria
    @Query("SELECT ra FROM RemoteAccess ra WHERE " +
           "(LOWER(ra.deviceName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(ra.accessName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(ra.host) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(ra.username) LIKE LOWER(CONCAT('%', :searchTerm, '%')))")
    List<RemoteAccess> searchByTerm(@Param("searchTerm") String searchTerm);
    
    // Bulk operations
    @Query("UPDATE RemoteAccess ra SET ra.isActive = false WHERE ra.deviceType = :deviceType AND ra.deviceId = :deviceId")
    void deactivateAllForDevice(@Param("deviceType") DeviceType deviceType, @Param("deviceId") Integer deviceId);
    
    @Query("DELETE FROM RemoteAccess ra WHERE ra.deviceType = :deviceType AND ra.deviceId = :deviceId")
    void deleteAllForDevice(@Param("deviceType") DeviceType deviceType, @Param("deviceId") Integer deviceId);
}