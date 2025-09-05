package com.telcobright.orchestrix.controller;

import com.telcobright.orchestrix.entity.Container;
import com.telcobright.orchestrix.repository.ContainerRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/containers")
@CrossOrigin(origins = "*")
public class ContainerController {

    @Autowired
    private ContainerRepository containerRepository;

    @GetMapping
    public List<Container> getAllContainers() {
        return containerRepository.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Container> getContainerById(@PathVariable Long id) {
        Optional<Container> container = containerRepository.findById(id);
        if (container.isPresent()) {
            return ResponseEntity.ok(container.get());
        }
        return ResponseEntity.notFound().build();
    }

    @GetMapping("/compute/{computeId}")
    public List<Container> getContainersByCompute(@PathVariable Long computeId) {
        return containerRepository.findByComputeId(computeId);
    }

    @GetMapping("/compute/{computeId}/type/{containerType}")
    public List<Container> getContainersByComputeAndType(
            @PathVariable Long computeId, 
            @PathVariable Container.ContainerType containerType) {
        return containerRepository.findByComputeIdAndContainerType(computeId, containerType);
    }

    @PostMapping
    public Container createContainer(@RequestBody Container container) {
        return containerRepository.save(container);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Container> updateContainer(@PathVariable Long id, @RequestBody Container containerDetails) {
        Optional<Container> optionalContainer = containerRepository.findById(id);
        if (optionalContainer.isPresent()) {
            Container container = optionalContainer.get();
            container.setName(containerDetails.getName());
            container.setDescription(containerDetails.getDescription());
            container.setContainerType(containerDetails.getContainerType());
            container.setContainerId(containerDetails.getContainerId());
            container.setImage(containerDetails.getImage());
            container.setImageVersion(containerDetails.getImageVersion());
            container.setStatus(containerDetails.getStatus());
            container.setIpAddress(containerDetails.getIpAddress());
            container.setExposedPorts(containerDetails.getExposedPorts());
            container.setEnvironmentVars(containerDetails.getEnvironmentVars());
            container.setMountPoints(containerDetails.getMountPoints());
            container.setCpuLimit(containerDetails.getCpuLimit());
            container.setMemoryLimit(containerDetails.getMemoryLimit());
            container.setAutoStart(containerDetails.getAutoStart());
            return ResponseEntity.ok(containerRepository.save(container));
        }
        return ResponseEntity.notFound().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteContainer(@PathVariable Long id) {
        if (containerRepository.existsById(id)) {
            containerRepository.deleteById(id);
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.notFound().build();
    }
}