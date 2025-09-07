package com.telcobright.orchestrix.repository;

import com.telcobright.orchestrix.entity.Cloud;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface CloudRepository extends JpaRepository<Cloud, Long> {
    
    List<Cloud> findByStatus(String status);
    
    List<Cloud> findByClientName(String clientName);
    
    @Query("SELECT DISTINCT c FROM Cloud c LEFT JOIN FETCH c.datacenters")
    List<Cloud> findAllWithDatacenters();
    
    @Query("SELECT c FROM Cloud c LEFT JOIN FETCH c.datacenters WHERE c.id = ?1")
    Cloud findByIdWithDatacenters(Long id);
    
    // Tenant-based queries
    @Query("SELECT DISTINCT c FROM Cloud c LEFT JOIN FETCH c.datacenters WHERE c.partner.roles LIKE '%self%'")
    List<Cloud> findAllByOrganizationTenant();
    
    @Query("SELECT DISTINCT c FROM Cloud c LEFT JOIN FETCH c.datacenters WHERE c.partner.roles NOT LIKE '%self%'")
    List<Cloud> findAllByOtherTenant();
}