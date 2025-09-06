package com.telcobright.orchestrix.controller;

import com.telcobright.orchestrix.entity.Compute;
import com.telcobright.orchestrix.entity.ResourcePool;
import com.telcobright.orchestrix.repository.ComputeRepository;
import com.telcobright.orchestrix.repository.ResourcePoolRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/computes")
@CrossOrigin(origins = "*")
public class ComputeController {

    @Autowired
    private ComputeRepository computeRepository;
    
    @Autowired
    private ResourcePoolRepository resourcePoolRepository;

    @GetMapping
    public List<Compute> getAllComputes() {
        return computeRepository.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Compute> getComputeById(@PathVariable Long id) {
        Compute compute = computeRepository.findByIdWithContainers(id);
        if (compute != null) {
            return ResponseEntity.ok(compute);
        }
        return ResponseEntity.notFound().build();
    }

    @GetMapping("/cloud/{cloudId}")
    public List<Compute> getComputesByCloud(@PathVariable Long cloudId) {
        return computeRepository.findByCloudId(cloudId);
    }

    @GetMapping("/datacenter/{datacenterId}")
    public List<Compute> getComputesByDatacenter(@PathVariable Long datacenterId) {
        return computeRepository.findByDatacenterId(datacenterId);
    }

    @PostMapping
    public ResponseEntity<?> createCompute(@RequestBody Map<String, Object> computeData) {
        try {
            Compute compute = new Compute();
            compute.setName((String) computeData.get("name"));
            compute.setHostname((String) computeData.get("hostname"));
            compute.setIpAddress((String) computeData.get("ipAddress"));
            
            // Set optional fields
            if (computeData.get("description") != null) {
                compute.setDescription((String) computeData.get("description"));
            }
            if (computeData.get("nodeType") != null) {
                String nodeTypeStr = (String) computeData.get("nodeType");
                Compute.NodeType nodeType = Compute.NodeType.valueOf(nodeTypeStr.toLowerCase());
                compute.setNodeType(nodeType);
            }
            // Note: osVersion is an entity reference, not a string
            // We'll skip setting it for now in the create method
            if (computeData.get("cpuCores") != null) {
                compute.setCpuCores(Integer.valueOf(computeData.get("cpuCores").toString()));
            }
            if (computeData.get("memoryGb") != null) {
                compute.setMemoryGb(Integer.valueOf(computeData.get("memoryGb").toString()));
            }
            if (computeData.get("diskGb") != null) {
                compute.setDiskGb(Integer.valueOf(computeData.get("diskGb").toString()));
            }
            if (computeData.get("status") != null) {
                compute.setStatus((String) computeData.get("status"));
            }
            
            // Set resource pool if provided
            if (computeData.get("resourcePoolId") != null) {
                Long resourcePoolId = Long.valueOf(computeData.get("resourcePoolId").toString());
                ResourcePool resourcePool = resourcePoolRepository.findById(resourcePoolId).orElse(null);
                if (resourcePool != null) {
                    compute.setResourcePool(resourcePool);
                }
            }
            
            Compute saved = computeRepository.save(compute);
            return ResponseEntity.ok(saved);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Failed to create compute: " + e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<Compute> updateCompute(@PathVariable Long id, @RequestBody Compute computeDetails) {
        Optional<Compute> optionalCompute = computeRepository.findById(id);
        if (optionalCompute.isPresent()) {
            Compute compute = optionalCompute.get();
            compute.setName(computeDetails.getName());
            compute.setDescription(computeDetails.getDescription());
            if (computeDetails.getNodeType() != null) {
                compute.setNodeType(computeDetails.getNodeType());
            }
            compute.setHostname(computeDetails.getHostname());
            compute.setIpAddress(computeDetails.getIpAddress());
            if (computeDetails.getOsVersion() != null) {
                compute.setOsVersion(computeDetails.getOsVersion());
            }
            compute.setCpuCores(computeDetails.getCpuCores());
            compute.setMemoryGb(computeDetails.getMemoryGb());
            compute.setDiskGb(computeDetails.getDiskGb());
            compute.setStatus(computeDetails.getStatus());
            return ResponseEntity.ok(computeRepository.save(compute));
        }
        return ResponseEntity.notFound().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteCompute(@PathVariable Long id) {
        if (computeRepository.existsById(id)) {
            computeRepository.deleteById(id);
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.notFound().build();
    }
}