package com.telcobright.orchestrix.controller;

import com.telcobright.orchestrix.entity.ResourceGroup;
import com.telcobright.orchestrix.entity.DatacenterResourceGroup;
import com.telcobright.orchestrix.entity.Datacenter;
import com.telcobright.orchestrix.repository.ResourceGroupRepository;
import com.telcobright.orchestrix.repository.DatacenterResourceGroupRepository;
import com.telcobright.orchestrix.repository.DatacenterRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/resource-groups")
@CrossOrigin(origins = "*")
public class ResourceGroupController {
    
    @Autowired
    private ResourceGroupRepository resourceGroupRepository;
    
    @Autowired
    private DatacenterResourceGroupRepository datacenterResourceGroupRepository;
    
    @Autowired
    private DatacenterRepository datacenterRepository;
    
    // Get all resource groups
    @GetMapping
    public List<ResourceGroup> getAllResourceGroups(@RequestParam(value = "tenant", required = false) String tenant) {
        // Resource groups are shared across tenants, but we can filter by the tenant parameter if needed
        // For now, return all active resource groups regardless of tenant
        return resourceGroupRepository.findAllActiveWithServices();
    }
    
    // Get resource group by ID
    @GetMapping("/{id}")
    public ResponseEntity<?> getResourceGroupById(@PathVariable Long id) {
        return resourceGroupRepository.findByIdWithServices(id)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }
    
    // Get resource groups by category
    @GetMapping("/category/{category}")
    public List<ResourceGroup> getResourceGroupsByCategory(@PathVariable String category) {
        return resourceGroupRepository.findByCategoryOrderBySortOrder(category);
    }
    
    // Assign resource group to datacenter
    @PostMapping("/assign")
    public ResponseEntity<?> assignResourceGroupToDatacenter(@RequestBody Map<String, Object> assignmentData) {
        try {
            Integer datacenterId = Integer.valueOf(assignmentData.get("datacenterId").toString());
            Long resourceGroupId = Long.valueOf(assignmentData.get("resourceGroupId").toString());
            
            // Check if assignment already exists
            if (datacenterResourceGroupRepository.findByDatacenterIdAndResourceGroupId(datacenterId, resourceGroupId).isPresent()) {
                Map<String, String> error = new HashMap<>();
                error.put("error", "Resource group already assigned to this datacenter");
                return ResponseEntity.badRequest().body(error);
            }
            
            Datacenter datacenter = datacenterRepository.findById(datacenterId).orElse(null);
            ResourceGroup resourceGroup = resourceGroupRepository.findById(resourceGroupId).orElse(null);
            
            if (datacenter == null || resourceGroup == null) {
                Map<String, String> error = new HashMap<>();
                error.put("error", "Invalid datacenter or resource group ID");
                return ResponseEntity.badRequest().body(error);
            }
            
            DatacenterResourceGroup assignment = new DatacenterResourceGroup();
            assignment.setDatacenter(datacenter);
            assignment.setResourceGroup(resourceGroup);
            
            if (assignmentData.get("status") != null) {
                assignment.setStatus((String) assignmentData.get("status"));
            }
            if (assignmentData.get("configuration") != null) {
                assignment.setConfiguration(assignmentData.get("configuration").toString());
            }
            
            DatacenterResourceGroup saved = datacenterResourceGroupRepository.save(assignment);
            return ResponseEntity.ok(saved);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Failed to assign resource group: " + e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }
    
    // Get resource groups for a datacenter
    @GetMapping("/datacenter/{datacenterId}")
    public List<DatacenterResourceGroup> getResourceGroupsForDatacenter(@PathVariable Integer datacenterId) {
        return datacenterResourceGroupRepository.findByDatacenterIdWithResourceGroups(datacenterId);
    }
    
    // Get all datacenter-resource group assignments
    @GetMapping("/datacenter/all")
    public List<DatacenterResourceGroup> getAllDatacenterResourceGroups() {
        return datacenterResourceGroupRepository.findAllWithDetails();
    }
    
    // Remove resource group from datacenter
    @DeleteMapping("/assign/{assignmentId}")
    public ResponseEntity<?> removeResourceGroupFromDatacenter(@PathVariable Long assignmentId) {
        return datacenterResourceGroupRepository.findById(assignmentId)
            .map(assignment -> {
                datacenterResourceGroupRepository.delete(assignment);
                Map<String, String> response = new HashMap<>();
                response.put("message", "Resource group assignment removed successfully");
                return ResponseEntity.ok(response);
            })
            .orElse(ResponseEntity.notFound().build());
    }
    
    // Create new resource group (admin function)
    @PostMapping
    public ResponseEntity<?> createResourceGroup(@RequestBody ResourceGroup resourceGroup) {
        try {
            // Check if name already exists
            if (resourceGroupRepository.findByName(resourceGroup.getName()).isPresent()) {
                Map<String, String> error = new HashMap<>();
                error.put("error", "Resource group with this name already exists");
                return ResponseEntity.badRequest().body(error);
            }
            
            ResourceGroup saved = resourceGroupRepository.save(resourceGroup);
            return ResponseEntity.ok(saved);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Failed to create resource group: " + e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }
    
    // Update resource group (admin function)
    @PutMapping("/{id}")
    public ResponseEntity<?> updateResourceGroup(@PathVariable Long id, @RequestBody ResourceGroup resourceGroupData) {
        return resourceGroupRepository.findById(id)
            .map(resourceGroup -> {
                resourceGroup.setDisplayName(resourceGroupData.getDisplayName());
                resourceGroup.setDescription(resourceGroupData.getDescription());
                resourceGroup.setCategory(resourceGroupData.getCategory());
                resourceGroup.setIsActive(resourceGroupData.getIsActive());
                resourceGroup.setSortOrder(resourceGroupData.getSortOrder());
                resourceGroup.setIcon(resourceGroupData.getIcon());
                resourceGroup.setColor(resourceGroupData.getColor());
                if (resourceGroupData.getServiceTypes() != null) {
                    resourceGroup.setServiceTypes(resourceGroupData.getServiceTypes());
                }
                
                ResourceGroup updated = resourceGroupRepository.save(resourceGroup);
                return ResponseEntity.ok(updated);
            })
            .orElse(ResponseEntity.notFound().build());
    }
    
    // Delete resource group (admin function)
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteResourceGroup(@PathVariable Long id) {
        return resourceGroupRepository.findById(id)
            .map(resourceGroup -> {
                // Check if resource group is assigned to any datacenter
                List<DatacenterResourceGroup> assignments = datacenterResourceGroupRepository.findByResourceGroupId(id);
                if (!assignments.isEmpty()) {
                    Map<String, String> error = new HashMap<>();
                    error.put("error", "Cannot delete resource group that is assigned to datacenters");
                    return ResponseEntity.badRequest().body(error);
                }
                
                resourceGroupRepository.delete(resourceGroup);
                Map<String, String> response = new HashMap<>();
                response.put("message", "Resource group deleted successfully");
                return ResponseEntity.ok(response);
            })
            .orElse(ResponseEntity.notFound().build());
    }
}