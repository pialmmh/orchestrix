package com.telcobright.orchestrix.repository;

import com.telcobright.orchestrix.entity.ComputeResource;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ComputeResourceRepository extends JpaRepository<ComputeResource, Integer> {
    
    List<ComputeResource> findByStatus(String status);
    
    List<ComputeResource> findByEnvironment(String environment);
    
    List<ComputeResource> findByDatacenterId(Integer datacenterId);
    
    List<ComputeResource> findByPartnerId(Integer partnerId);
    
    @Query("SELECT cr FROM ComputeResource cr WHERE cr.status = :status AND cr.environment = :environment")
    List<ComputeResource> findByStatusAndEnvironment(@Param("status") String status, @Param("environment") String environment);
    
    @Query("SELECT cr FROM ComputeResource cr WHERE cr.osType = :osType")
    List<ComputeResource> findByOsType(@Param("osType") String osType);
    
    @Query(value = "SELECT * FROM compute_resources WHERE JSON_CONTAINS(tags, :tag, '$')", nativeQuery = true)
    List<ComputeResource> findByTag(@Param("tag") String tag);
    
    List<ComputeResource> findByHostnameContaining(String hostname);
    
    List<ComputeResource> findByIpAddress(String ipAddress);
}