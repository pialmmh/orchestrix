package com.telcobright.orchestrix.repository;

import com.telcobright.orchestrix.entity.Environment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface EnvironmentRepository extends JpaRepository<Environment, Long> {
    
    Optional<Environment> findByCode(String code);
    
    List<Environment> findByType(Environment.EnvironmentType type);
    
    List<Environment> findByPartnerId(Integer partnerId);
    
    Optional<Environment> findByPartnerIdAndCode(Integer partnerId, String code);
    
    List<Environment> findByStatus(String status);
    
    @Query("SELECT e FROM Environment e LEFT JOIN FETCH e.datacenters WHERE e.id = :id")
    Optional<Environment> findByIdWithDatacenters(@Param("id") Long id);
}