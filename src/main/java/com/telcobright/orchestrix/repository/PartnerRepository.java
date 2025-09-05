package com.telcobright.orchestrix.repository;

import com.telcobright.orchestrix.entity.Partner;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface PartnerRepository extends JpaRepository<Partner, Integer> {
    
    Optional<Partner> findByName(String name);
    
    List<Partner> findByType(String type);
    
    List<Partner> findByStatus(String status);
    
    @Query(value = "SELECT * FROM partners p WHERE JSON_CONTAINS(p.roles, :role, '$')", nativeQuery = true)
    List<Partner> findByRole(@Param("role") String role);
    
    @Query(value = "SELECT * FROM partners p WHERE JSON_CONTAINS(p.roles, JSON_QUOTE(:role), '$')", nativeQuery = true)
    List<Partner> findByRolesContaining(@Param("role") String role);
    
    @Query("SELECT p FROM Partner p WHERE p.type IN ('cloud-provider', 'both')")
    List<Partner> findCloudProviders();
    
    @Query("SELECT p FROM Partner p WHERE p.type IN ('vendor', 'both')")
    List<Partner> findVendors();
    
    List<Partner> findByStatusOrderByDisplayNameAsc(String status);
}