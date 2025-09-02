package com.telcobright.orchestrix.repository;

import com.telcobright.orchestrix.entity.ComputeAccessCredential;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface ComputeAccessCredentialRepository extends JpaRepository<ComputeAccessCredential, Integer> {
    
    List<ComputeAccessCredential> findByComputeResourceId(Integer computeResourceId);
    
    List<ComputeAccessCredential> findByAccessType(String accessType);
    
    List<ComputeAccessCredential> findByComputeResourceIdAndIsActive(Integer computeResourceId, Boolean isActive);
    
    @Query("SELECT cac FROM ComputeAccessCredential cac WHERE cac.computeResource.id = :resourceId AND cac.accessType = :accessType AND cac.isActive = true")
    List<ComputeAccessCredential> findActiveByResourceAndType(@Param("resourceId") Integer resourceId, @Param("accessType") String accessType);
    
    @Query("SELECT cac FROM ComputeAccessCredential cac WHERE cac.computeResource.id = :resourceId AND cac.isPrimary = true")
    Optional<ComputeAccessCredential> findPrimaryByResource(@Param("resourceId") Integer resourceId);
    
    List<ComputeAccessCredential> findByAuthType(String authType);
    
    List<ComputeAccessCredential> findByLastTestStatus(String lastTestStatus);
}