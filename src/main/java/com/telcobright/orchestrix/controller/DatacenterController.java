package com.telcobright.orchestrix.controller;

import com.telcobright.orchestrix.entity.Datacenter;
import com.telcobright.orchestrix.entity.Cloud;
import com.telcobright.orchestrix.entity.AvailabilityZone;
import com.telcobright.orchestrix.entity.ResourceGroup;
import com.telcobright.orchestrix.entity.DatacenterResourceGroup;
import com.telcobright.orchestrix.repository.DatacenterRepository;
import com.telcobright.orchestrix.repository.CloudRepository;
import com.telcobright.orchestrix.repository.AvailabilityZoneRepository;
import com.telcobright.orchestrix.repository.ResourceGroupRepository;
import com.telcobright.orchestrix.repository.DatacenterResourceGroupRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/datacenters")
@CrossOrigin(origins = "*")
public class DatacenterController {
    
    @Autowired
    private DatacenterRepository datacenterRepository;
    
    @Autowired
    private CloudRepository cloudRepository;
    
    @Autowired
    private AvailabilityZoneRepository availabilityZoneRepository;
    
    @Autowired
    private ResourceGroupRepository resourceGroupRepository;
    
    @Autowired
    private DatacenterResourceGroupRepository datacenterResourceGroupRepository;
    
    // Get all datacenters
    @GetMapping
    public List<Datacenter> getAllDatacenters() {
        return datacenterRepository.findAll();
    }
    
    // Get datacenter by ID
    @GetMapping("/{id}")
    public ResponseEntity<?> getDatacenterById(@PathVariable Integer id) {
        return datacenterRepository.findById(id)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }
    
    // Get datacenters by cloud
    @GetMapping("/cloud/{cloudId}")
    public List<Datacenter> getDatacentersByCloud(@PathVariable Long cloudId) {
        return datacenterRepository.findAll().stream()
            .filter(dc -> dc.getCloud() != null && dc.getCloud().getId().equals(cloudId))
            .toList();
    }
    
    // Create new datacenter
    @PostMapping
    public ResponseEntity<?> createDatacenter(@RequestBody Map<String, Object> datacenterData) {
        try {
            Datacenter datacenter = new Datacenter();
            datacenter.setName((String) datacenterData.get("name"));
            datacenter.setLocationOther((String) datacenterData.get("location"));
            datacenter.setType((String) datacenterData.getOrDefault("type", "PRIMARY"));
            datacenter.setProvider((String) datacenterData.get("provider"));
            datacenter.setIsDrSite((Boolean) datacenterData.getOrDefault("isDrSite", false));
            datacenter.setStatus((String) datacenterData.getOrDefault("status", "ACTIVE"));
            
            // Set tier if provided
            if (datacenterData.get("tier") != null) {
                datacenter.setTier(Integer.valueOf(datacenterData.get("tier").toString()));
            }
            
            // Set cloud if provided
            if (datacenterData.get("cloudId") != null) {
                Long cloudId = Long.valueOf(datacenterData.get("cloudId").toString());
                Cloud cloud = cloudRepository.findById(cloudId).orElse(null);
                if (cloud != null) {
                    datacenter.setCloud(cloud);
                } else {
                    return ResponseEntity.badRequest().body("Invalid cloud ID");
                }
            }
            
            // Set availability zone if provided
            if (datacenterData.get("availabilityZoneId") != null) {
                Long azId = Long.valueOf(datacenterData.get("availabilityZoneId").toString());
                AvailabilityZone az = availabilityZoneRepository.findById(azId).orElse(null);
                if (az != null) {
                    datacenter.setAvailabilityZone(az);
                } else {
                    return ResponseEntity.badRequest().body("Invalid availability zone ID");
                }
            }
            
            Datacenter saved = datacenterRepository.save(datacenter);
            
            // Automatically assign all active resource groups to the new datacenter
            List<ResourceGroup> activeGroups = resourceGroupRepository.findByIsActiveOrderBySortOrder(true);
            for (ResourceGroup group : activeGroups) {
                // Check if assignment already exists to avoid duplicates
                boolean exists = datacenterResourceGroupRepository
                    .findByDatacenterIdAndResourceGroupId(saved.getId(), group.getId())
                    .isPresent();
                    
                if (!exists) {
                    DatacenterResourceGroup assignment = new DatacenterResourceGroup();
                    assignment.setDatacenter(saved);
                    assignment.setResourceGroup(group);
                    assignment.setStatus("ACTIVE");
                    datacenterResourceGroupRepository.save(assignment);
                }
            }
            
            // Reload the datacenter with all its relationships
            Datacenter result = datacenterRepository.findById(saved.getId())
                .orElse(saved);
            
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Failed to create datacenter: " + e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }
    
    // Update datacenter
    @PutMapping("/{id}")
    public ResponseEntity<?> updateDatacenter(@PathVariable Integer id, @RequestBody Map<String, Object> datacenterData) {
        return datacenterRepository.findById(id)
            .map(datacenter -> {
                if (datacenterData.get("name") != null) {
                    datacenter.setName((String) datacenterData.get("name"));
                }
                if (datacenterData.get("location") != null) {
                    datacenter.setLocationOther((String) datacenterData.get("location"));
                }
                if (datacenterData.get("type") != null) {
                    datacenter.setType((String) datacenterData.get("type"));
                }
                if (datacenterData.get("provider") != null) {
                    datacenter.setProvider((String) datacenterData.get("provider"));
                }
                if (datacenterData.get("isDrSite") != null) {
                    datacenter.setIsDrSite((Boolean) datacenterData.get("isDrSite"));
                }
                if (datacenterData.get("status") != null) {
                    datacenter.setStatus((String) datacenterData.get("status"));
                }
                
                // Update cloud if provided
                if (datacenterData.get("cloudId") != null) {
                    Long cloudId = Long.valueOf(datacenterData.get("cloudId").toString());
                    Cloud cloud = cloudRepository.findById(cloudId).orElse(null);
                    if (cloud != null) {
                        datacenter.setCloud(cloud);
                    } else {
                        Map<String, String> error = new HashMap<>();
                        error.put("error", "Invalid cloud ID");
                        return ResponseEntity.badRequest().body(error);
                    }
                }
                
                Datacenter updated = datacenterRepository.save(datacenter);
                return ResponseEntity.ok(updated);
            })
            .orElse(ResponseEntity.notFound().build());
    }
    
    // Delete datacenter
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteDatacenter(@PathVariable Integer id) {
        return datacenterRepository.findById(id)
            .map(datacenter -> {
                // Check if datacenter has any resource pools or resource groups
                if (datacenter.getResourcePools() != null && !datacenter.getResourcePools().isEmpty()) {
                    Map<String, String> error = new HashMap<>();
                    error.put("error", "Cannot delete datacenter with existing resource pools");
                    return ResponseEntity.badRequest().body(error);
                }
                
                if (datacenter.getDatacenterResourceGroups() != null && !datacenter.getDatacenterResourceGroups().isEmpty()) {
                    Map<String, String> error = new HashMap<>();
                    error.put("error", "Cannot delete datacenter with assigned resource groups");
                    return ResponseEntity.badRequest().body(error);
                }
                
                datacenterRepository.delete(datacenter);
                Map<String, String> response = new HashMap<>();
                response.put("message", "Datacenter deleted successfully");
                return ResponseEntity.ok(response);
            })
            .orElse(ResponseEntity.notFound().build());
    }
}