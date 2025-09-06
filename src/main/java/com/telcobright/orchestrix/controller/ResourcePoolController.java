package com.telcobright.orchestrix.controller;

import com.telcobright.orchestrix.entity.ResourcePool;
import com.telcobright.orchestrix.entity.Datacenter;
import com.telcobright.orchestrix.repository.ResourcePoolRepository;
import com.telcobright.orchestrix.repository.DatacenterRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/resource-pools")
@CrossOrigin(origins = "*")
public class ResourcePoolController {
    
    @Autowired
    private ResourcePoolRepository resourcePoolRepository;
    
    @Autowired
    private DatacenterRepository datacenterRepository;
    
    // Get all resource pools
    @GetMapping
    public List<ResourcePool> getAllResourcePools() {
        return resourcePoolRepository.findAll();
    }
    
    // Get resource pool by ID
    @GetMapping("/{id}")
    public ResponseEntity<?> getResourcePoolById(@PathVariable Long id) {
        return resourcePoolRepository.findById(id)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }
    
    // Get resource pools by datacenter
    @GetMapping("/datacenter/{datacenterId}")
    public List<ResourcePool> getResourcePoolsByDatacenter(@PathVariable Integer datacenterId) {
        return resourcePoolRepository.findAll().stream()
            .filter(rp -> rp.getDatacenter() != null && rp.getDatacenter().getId().equals(datacenterId))
            .toList();
    }
    
    // Create new resource pool
    @PostMapping
    public ResponseEntity<?> createResourcePool(@RequestBody Map<String, Object> resourcePoolData) {
        try {
            ResourcePool resourcePool = new ResourcePool();
            resourcePool.setName((String) resourcePoolData.get("name"));
            
            // Set type
            if (resourcePoolData.get("type") != null) {
                ResourcePool.PoolType type = ResourcePool.PoolType.valueOf(
                    resourcePoolData.get("type").toString().toUpperCase()
                );
                resourcePool.setType(type);
            } else {
                resourcePool.setType(ResourcePool.PoolType.COMPUTE);
            }
            
            // Set optional fields
            if (resourcePoolData.get("hypervisor") != null) {
                resourcePool.setHypervisor((String) resourcePoolData.get("hypervisor"));
            }
            if (resourcePoolData.get("orchestrator") != null) {
                resourcePool.setOrchestrator((String) resourcePoolData.get("orchestrator"));
            }
            if (resourcePoolData.get("description") != null) {
                resourcePool.setDescription((String) resourcePoolData.get("description"));
            }
            if (resourcePoolData.get("status") != null) {
                resourcePool.setStatus((String) resourcePoolData.get("status"));
            }
            
            // Set resource capacity
            if (resourcePoolData.get("totalCpuCores") != null) {
                resourcePool.setTotalCpuCores(Integer.valueOf(resourcePoolData.get("totalCpuCores").toString()));
            }
            if (resourcePoolData.get("totalMemoryGb") != null) {
                resourcePool.setTotalMemoryGb(Integer.valueOf(resourcePoolData.get("totalMemoryGb").toString()));
            }
            if (resourcePoolData.get("totalStorageTb") != null) {
                resourcePool.setTotalStorageTb(Integer.valueOf(resourcePoolData.get("totalStorageTb").toString()));
            }
            
            // Set datacenter if provided
            if (resourcePoolData.get("datacenterId") != null) {
                Integer datacenterId = Integer.valueOf(resourcePoolData.get("datacenterId").toString());
                Datacenter datacenter = datacenterRepository.findById(datacenterId).orElse(null);
                if (datacenter != null) {
                    resourcePool.setDatacenter(datacenter);
                } else {
                    return ResponseEntity.badRequest().body("Invalid datacenter ID");
                }
            }
            
            ResourcePool saved = resourcePoolRepository.save(resourcePool);
            return ResponseEntity.ok(saved);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Failed to create resource pool: " + e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }
    
    // Update resource pool
    @PutMapping("/{id}")
    public ResponseEntity<?> updateResourcePool(@PathVariable Long id, @RequestBody Map<String, Object> resourcePoolData) {
        return resourcePoolRepository.findById(id)
            .map(resourcePool -> {
                if (resourcePoolData.get("name") != null) {
                    resourcePool.setName((String) resourcePoolData.get("name"));
                }
                if (resourcePoolData.get("type") != null) {
                    ResourcePool.PoolType type = ResourcePool.PoolType.valueOf(
                        resourcePoolData.get("type").toString().toUpperCase()
                    );
                    resourcePool.setType(type);
                }
                if (resourcePoolData.get("hypervisor") != null) {
                    resourcePool.setHypervisor((String) resourcePoolData.get("hypervisor"));
                }
                if (resourcePoolData.get("orchestrator") != null) {
                    resourcePool.setOrchestrator((String) resourcePoolData.get("orchestrator"));
                }
                if (resourcePoolData.get("description") != null) {
                    resourcePool.setDescription((String) resourcePoolData.get("description"));
                }
                if (resourcePoolData.get("status") != null) {
                    resourcePool.setStatus((String) resourcePoolData.get("status"));
                }
                
                // Update resource capacity
                if (resourcePoolData.get("totalCpuCores") != null) {
                    resourcePool.setTotalCpuCores(Integer.valueOf(resourcePoolData.get("totalCpuCores").toString()));
                }
                if (resourcePoolData.get("totalMemoryGb") != null) {
                    resourcePool.setTotalMemoryGb(Integer.valueOf(resourcePoolData.get("totalMemoryGb").toString()));
                }
                if (resourcePoolData.get("totalStorageTb") != null) {
                    resourcePool.setTotalStorageTb(Integer.valueOf(resourcePoolData.get("totalStorageTb").toString()));
                }
                
                // Update datacenter if provided
                if (resourcePoolData.get("datacenterId") != null) {
                    Integer datacenterId = Integer.valueOf(resourcePoolData.get("datacenterId").toString());
                    Datacenter datacenter = datacenterRepository.findById(datacenterId).orElse(null);
                    if (datacenter != null) {
                        resourcePool.setDatacenter(datacenter);
                    } else {
                        Map<String, String> error = new HashMap<>();
                        error.put("error", "Invalid datacenter ID");
                        return ResponseEntity.badRequest().body(error);
                    }
                }
                
                ResourcePool updated = resourcePoolRepository.save(resourcePool);
                return ResponseEntity.ok(updated);
            })
            .orElse(ResponseEntity.notFound().build());
    }
    
    // Delete resource pool
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteResourcePool(@PathVariable Long id) {
        return resourcePoolRepository.findById(id)
            .map(resourcePool -> {
                resourcePoolRepository.delete(resourcePool);
                Map<String, String> response = new HashMap<>();
                response.put("message", "Resource pool deleted successfully");
                return ResponseEntity.ok(response);
            })
            .orElse(ResponseEntity.notFound().build());
    }
}