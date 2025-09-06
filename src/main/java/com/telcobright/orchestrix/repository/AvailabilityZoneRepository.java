package com.telcobright.orchestrix.repository;

import com.telcobright.orchestrix.entity.AvailabilityZone;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface AvailabilityZoneRepository extends JpaRepository<AvailabilityZone, Long> {
    
    List<AvailabilityZone> findByRegionId(Long regionId);
    
    Optional<AvailabilityZone> findByRegionIdAndCode(Long regionId, String code);
    
    List<AvailabilityZone> findByZoneType(String zoneType);
    
    List<AvailabilityZone> findByStatus(String status);
    
    @Query("SELECT az FROM AvailabilityZone az LEFT JOIN FETCH az.datacenters WHERE az.id = :id")
    Optional<AvailabilityZone> findByIdWithDatacenters(@Param("id") Long id);
    
    @Query("SELECT az FROM AvailabilityZone az WHERE az.region.id = :regionId AND az.isDefault = true")
    Optional<AvailabilityZone> findDefaultByRegionId(@Param("regionId") Long regionId);
    
    @Query("SELECT az FROM AvailabilityZone az WHERE az.capabilities LIKE %:capability%")
    List<AvailabilityZone> findByCapability(@Param("capability") String capability);
}