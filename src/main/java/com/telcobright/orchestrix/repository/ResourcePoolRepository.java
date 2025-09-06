package com.telcobright.orchestrix.repository;

import com.telcobright.orchestrix.entity.ResourcePool;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface ResourcePoolRepository extends JpaRepository<ResourcePool, Long> {
    
    List<ResourcePool> findByDatacenterId(Integer datacenterId);
    
    List<ResourcePool> findByType(ResourcePool.PoolType type);
    
    List<ResourcePool> findByHypervisor(String hypervisor);
    
    List<ResourcePool> findByOrchestrator(String orchestrator);
    
    List<ResourcePool> findByStatus(String status);
    
    @Query("SELECT rp FROM ResourcePool rp LEFT JOIN FETCH rp.computes WHERE rp.id = :id")
    Optional<ResourcePool> findByIdWithComputes(@Param("id") Long id);
    
    @Query("SELECT rp FROM ResourcePool rp WHERE rp.datacenter.id = :datacenterId AND rp.type = :type")
    List<ResourcePool> findByDatacenterIdAndType(@Param("datacenterId") Integer datacenterId, @Param("type") ResourcePool.PoolType type);
    
    @Query("SELECT rp FROM ResourcePool rp WHERE (rp.totalCpuCores - rp.usedCpuCores) >= :minCores")
    List<ResourcePool> findByAvailableCpuCores(@Param("minCores") Integer minCores);
    
    @Query("SELECT rp FROM ResourcePool rp WHERE (rp.totalMemoryGb - rp.usedMemoryGb) >= :minMemory")
    List<ResourcePool> findByAvailableMemory(@Param("minMemory") Integer minMemory);
}