package com.telcobright.orchestrix.repository;

import com.telcobright.orchestrix.entity.Container;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ContainerRepository extends JpaRepository<Container, Long> {
    
    List<Container> findByComputeId(Long computeId);
    
    List<Container> findByContainerType(Container.ContainerType containerType);
    
    List<Container> findByStatus(String status);
    
    List<Container> findByComputeIdAndContainerType(Long computeId, Container.ContainerType containerType);
    
    List<Container> findByAutoStartTrue();
}