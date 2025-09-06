package com.telcobright.orchestrix.repository;

import com.telcobright.orchestrix.entity.ResourceGroup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface ResourceGroupRepository extends JpaRepository<ResourceGroup, Long> {
    
    Optional<ResourceGroup> findByName(String name);
    
    List<ResourceGroup> findByIsActiveOrderBySortOrder(Boolean isActive);
    
    List<ResourceGroup> findByCategoryOrderBySortOrder(String category);
    
    @Query("SELECT rg FROM ResourceGroup rg LEFT JOIN FETCH rg.serviceTypes WHERE rg.isActive = true ORDER BY rg.sortOrder")
    List<ResourceGroup> findAllActiveWithServices();
    
    @Query("SELECT rg FROM ResourceGroup rg LEFT JOIN FETCH rg.serviceTypes WHERE rg.id = ?1")
    Optional<ResourceGroup> findByIdWithServices(Long id);
}