package com.telcobright.orchestrix.repository;

import com.telcobright.orchestrix.entity.DatacenterResourceGroup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface DatacenterResourceGroupRepository extends JpaRepository<DatacenterResourceGroup, Long> {
    
    List<DatacenterResourceGroup> findByDatacenterId(Integer datacenterId);
    
    List<DatacenterResourceGroup> findByResourceGroupId(Long resourceGroupId);
    
    Optional<DatacenterResourceGroup> findByDatacenterIdAndResourceGroupId(Integer datacenterId, Long resourceGroupId);
    
    @Query("SELECT drg FROM DatacenterResourceGroup drg " +
           "JOIN FETCH drg.resourceGroup rg " +
           "LEFT JOIN FETCH rg.serviceTypes " +
           "WHERE drg.datacenter.id = ?1")
    List<DatacenterResourceGroup> findByDatacenterIdWithResourceGroups(Integer datacenterId);
    
    @Query("SELECT drg FROM DatacenterResourceGroup drg " +
           "JOIN FETCH drg.datacenter " +
           "JOIN FETCH drg.resourceGroup rg " +
           "LEFT JOIN FETCH rg.serviceTypes")
    List<DatacenterResourceGroup> findAllWithDetails();
}