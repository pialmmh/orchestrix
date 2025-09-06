package com.telcobright.orchestrix.repository;

import com.telcobright.orchestrix.entity.Region;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface RegionRepository extends JpaRepository<Region, Long> {
    
    Optional<Region> findByCode(String code);
    
    List<Region> findByCloudId(Long cloudId);
    
    List<Region> findByGeographicArea(String geographicArea);
    
    List<Region> findByStatus(String status);
    
    @Query("SELECT r FROM Region r LEFT JOIN FETCH r.availabilityZones WHERE r.id = :id")
    Optional<Region> findByIdWithAvailabilityZones(@Param("id") Long id);
    
    @Query("SELECT r FROM Region r LEFT JOIN FETCH r.availabilityZones WHERE r.cloud.id = :cloudId")
    List<Region> findByCloudIdWithAvailabilityZones(@Param("cloudId") Long cloudId);
    
    @Query("SELECT r FROM Region r WHERE r.complianceZones LIKE %:compliance%")
    List<Region> findByComplianceZone(@Param("compliance") String compliance);
}