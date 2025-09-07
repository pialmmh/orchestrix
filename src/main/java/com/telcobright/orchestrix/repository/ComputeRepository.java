package com.telcobright.orchestrix.repository;

import com.telcobright.orchestrix.entity.Compute;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ComputeRepository extends JpaRepository<Compute, Long> {
    
    List<Compute> findByCloudId(Long cloudId);
    
    List<Compute> findByDatacenterId(Long datacenterId);
    
    List<Compute> findByStatus(String status);
    
    List<Compute> findByNodeType(Compute.NodeType nodeType);
    
    @Query("SELECT c FROM Compute c LEFT JOIN FETCH c.containers WHERE c.id = ?1")
    Compute findByIdWithContainers(Long id);
    
    @Query("SELECT c FROM Compute c WHERE c.cloud.id = ?1 AND c.status = 'ACTIVE'")
    List<Compute> findActiveByCloudId(Long cloudId);
    
    // Tenant-based queries
    @Query("SELECT c FROM Compute c WHERE c.cloud.partner.roles LIKE '%self%'")
    List<Compute> findAllByOrganizationTenant();
    
    @Query("SELECT c FROM Compute c WHERE c.cloud.partner.roles NOT LIKE '%self%'")
    List<Compute> findAllByOtherTenant();
}