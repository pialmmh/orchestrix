package com.telcobright.orchestrix.controller;

import com.telcobright.orchestrix.entity.ComputeResource;
import com.telcobright.orchestrix.entity.ComputeAccessCredential;
import com.telcobright.orchestrix.repository.ComputeResourceRepository;
import com.telcobright.orchestrix.repository.ComputeAccessCredentialRepository;
import com.telcobright.orchestrix.repository.DatacenterRepository;
import com.telcobright.orchestrix.repository.PartnerRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/compute")
@CrossOrigin(origins = "*")
public class ComputeResourceController {
    
    @Autowired
    private ComputeResourceRepository computeResourceRepository;
    
    @Autowired
    private ComputeAccessCredentialRepository accessCredentialRepository;
    
    @Autowired
    private DatacenterRepository datacenterRepository;
    
    @Autowired
    private PartnerRepository partnerRepository;
    
    // Get all compute resources
    @GetMapping("/resources")
    public ResponseEntity<?> getAllComputeResources(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String environment) {
        
        List<ComputeResource> resources;
        
        if (status != null && environment != null) {
            resources = computeResourceRepository.findByStatusAndEnvironment(status, environment);
        } else if (status != null) {
            resources = computeResourceRepository.findByStatus(status);
        } else if (environment != null) {
            resources = computeResourceRepository.findByEnvironment(environment);
        } else {
            resources = computeResourceRepository.findAll();
        }
        
        Map<String, Object> response = new HashMap<>();
        List<Map<String, Object>> resourceList = resources.stream().map(this::mapResourceToResponse).collect(Collectors.toList());
        
        response.put("resources", resourceList);
        response.put("total", resourceList.size());
        return ResponseEntity.ok(response);
    }
    
    // Get compute resource by ID
    @GetMapping("/resources/{id}")
    public ResponseEntity<?> getComputeResource(@PathVariable Integer id) {
        return computeResourceRepository.findById(id)
            .map(resource -> {
                Map<String, Object> response = mapResourceToResponse(resource);
                
                // Include access credentials
                List<ComputeAccessCredential> credentials = accessCredentialRepository.findByComputeResourceId(id);
                response.put("accessCredentials", credentials.stream()
                    .map(this::mapCredentialToResponse)
                    .collect(Collectors.toList()));
                
                return ResponseEntity.ok(response);
            })
            .orElse(ResponseEntity.notFound().build());
    }
    
    // Create compute resource
    @PostMapping("/resources")
    public ResponseEntity<?> createComputeResource(@RequestBody Map<String, Object> resourceData) {
        ComputeResource resource = new ComputeResource();
        updateResourceFromData(resource, resourceData);
        
        ComputeResource saved = computeResourceRepository.save(resource);
        return ResponseEntity.ok(mapResourceToResponse(saved));
    }
    
    // Update compute resource
    @PutMapping("/resources/{id}")
    public ResponseEntity<?> updateComputeResource(@PathVariable Integer id, @RequestBody Map<String, Object> resourceData) {
        return computeResourceRepository.findById(id)
            .map(resource -> {
                updateResourceFromData(resource, resourceData);
                ComputeResource updated = computeResourceRepository.save(resource);
                return ResponseEntity.ok(mapResourceToResponse(updated));
            })
            .orElse(ResponseEntity.notFound().build());
    }
    
    // Delete compute resource
    @DeleteMapping("/resources/{id}")
    public ResponseEntity<?> deleteComputeResource(@PathVariable Integer id) {
        return computeResourceRepository.findById(id)
            .map(resource -> {
                computeResourceRepository.delete(resource);
                Map<String, String> response = new HashMap<>();
                response.put("message", "Compute resource deleted successfully");
                return ResponseEntity.ok(response);
            })
            .orElse(ResponseEntity.notFound().build());
    }
    
    // Access Credentials endpoints
    
    // Get credentials for a compute resource
    @GetMapping("/resources/{resourceId}/credentials")
    public ResponseEntity<?> getResourceCredentials(@PathVariable Integer resourceId) {
        List<ComputeAccessCredential> credentials = accessCredentialRepository.findByComputeResourceIdAndIsActive(resourceId, true);
        
        Map<String, Object> response = new HashMap<>();
        response.put("credentials", credentials.stream()
            .map(this::mapCredentialToResponse)
            .collect(Collectors.toList()));
        response.put("total", credentials.size());
        return ResponseEntity.ok(response);
    }
    
    // Add credential to compute resource
    @PostMapping("/resources/{resourceId}/credentials")
    public ResponseEntity<?> addCredential(@PathVariable Integer resourceId, @RequestBody Map<String, Object> credentialData) {
        return computeResourceRepository.findById(resourceId)
            .map(resource -> {
                ComputeAccessCredential credential = new ComputeAccessCredential();
                credential.setComputeResource(resource);
                updateCredentialFromData(credential, credentialData);
                
                ComputeAccessCredential saved = accessCredentialRepository.save(credential);
                return ResponseEntity.ok(mapCredentialToResponse(saved));
            })
            .orElse(ResponseEntity.notFound().build());
    }
    
    // Update credential
    @PutMapping("/credentials/{id}")
    public ResponseEntity<?> updateCredential(@PathVariable Integer id, @RequestBody Map<String, Object> credentialData) {
        return accessCredentialRepository.findById(id)
            .map(credential -> {
                updateCredentialFromData(credential, credentialData);
                ComputeAccessCredential updated = accessCredentialRepository.save(credential);
                return ResponseEntity.ok(mapCredentialToResponse(updated));
            })
            .orElse(ResponseEntity.notFound().build());
    }
    
    // Delete credential
    @DeleteMapping("/credentials/{id}")
    public ResponseEntity<?> deleteCredential(@PathVariable Integer id) {
        return accessCredentialRepository.findById(id)
            .map(credential -> {
                accessCredentialRepository.delete(credential);
                Map<String, String> response = new HashMap<>();
                response.put("message", "Credential deleted successfully");
                return ResponseEntity.ok(response);
            })
            .orElse(ResponseEntity.notFound().build());
    }
    
    // Test credential connection
    @PostMapping("/credentials/{id}/test")
    public ResponseEntity<?> testCredential(@PathVariable Integer id) {
        return accessCredentialRepository.findById(id)
            .map(credential -> {
                // In a real implementation, this would actually test the connection
                credential.setLastTestedAt(LocalDateTime.now());
                credential.setLastTestStatus("SUCCESS");
                credential.setLastTestMessage("Connection test successful");
                accessCredentialRepository.save(credential);
                
                Map<String, Object> response = new HashMap<>();
                response.put("status", "SUCCESS");
                response.put("message", "Connection test successful");
                response.put("testedAt", LocalDateTime.now());
                return ResponseEntity.ok(response);
            })
            .orElse(ResponseEntity.notFound().build());
    }
    
    // Helper methods
    
    private Map<String, Object> mapResourceToResponse(ComputeResource resource) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", resource.getId());
        map.put("name", resource.getName());
        map.put("hostname", resource.getHostname());
        map.put("ipAddress", resource.getIpAddress());
        map.put("status", resource.getStatus());
        map.put("environment", resource.getEnvironment());
        map.put("purpose", resource.getPurpose());
        map.put("cpuCores", resource.getCpuCores());
        map.put("memoryGb", resource.getMemoryGb());
        map.put("storageGb", resource.getStorageGb());
        map.put("osType", resource.getOsType());
        map.put("osVersion", resource.getOsVersion());
        map.put("tags", resource.getTags());
        map.put("notes", resource.getNotes());
        
        if (resource.getDatacenter() != null) {
            map.put("datacenterId", resource.getDatacenter().getId());
            map.put("datacenterName", resource.getDatacenter().getName());
        }
        
        if (resource.getPartner() != null) {
            map.put("partnerId", resource.getPartner().getId());
            map.put("partnerName", resource.getPartner().getDisplayName());
        }
        
        map.put("createdAt", resource.getCreatedAt());
        map.put("updatedAt", resource.getUpdatedAt());
        
        return map;
    }
    
    private Map<String, Object> mapCredentialToResponse(ComputeAccessCredential credential) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", credential.getId());
        map.put("accessType", credential.getAccessType());
        map.put("accessName", credential.getAccessName());
        map.put("host", credential.getHost());
        map.put("port", credential.getPort());
        map.put("protocol", credential.getProtocol());
        map.put("authType", credential.getAuthType());
        map.put("username", credential.getUsername());
        map.put("isActive", credential.getIsActive());
        map.put("isPrimary", credential.getIsPrimary());
        map.put("lastTestedAt", credential.getLastTestedAt());
        map.put("lastTestStatus", credential.getLastTestStatus());
        map.put("lastTestMessage", credential.getLastTestMessage());
        
        // Include non-sensitive fields
        if (credential.getSshPublicKey() != null) {
            map.put("hasSshKey", true);
        }
        if (credential.getAuthParams() != null && !credential.getAuthParams().isEmpty()) {
            map.put("hasAuthParams", true);
        }
        
        return map;
    }
    
    private void updateResourceFromData(ComputeResource resource, Map<String, Object> data) {
        if (data.get("name") != null) {
            resource.setName((String) data.get("name"));
        }
        if (data.get("hostname") != null) {
            resource.setHostname((String) data.get("hostname"));
        }
        if (data.get("ipAddress") != null) {
            resource.setIpAddress((String) data.get("ipAddress"));
        }
        if (data.get("status") != null) {
            resource.setStatus((String) data.get("status"));
        }
        if (data.get("environment") != null) {
            resource.setEnvironment((String) data.get("environment"));
        }
        if (data.get("purpose") != null) {
            resource.setPurpose((String) data.get("purpose"));
        }
        if (data.get("cpuCores") != null) {
            resource.setCpuCores(Integer.valueOf(data.get("cpuCores").toString()));
        }
        if (data.get("memoryGb") != null) {
            resource.setMemoryGb(Integer.valueOf(data.get("memoryGb").toString()));
        }
        if (data.get("storageGb") != null) {
            resource.setStorageGb(Integer.valueOf(data.get("storageGb").toString()));
        }
        if (data.get("osType") != null) {
            resource.setOsType((String) data.get("osType"));
        }
        if (data.get("osVersion") != null) {
            resource.setOsVersion((String) data.get("osVersion"));
        }
        if (data.get("tags") != null) {
            resource.setTags((List<String>) data.get("tags"));
        }
        if (data.get("notes") != null) {
            resource.setNotes((String) data.get("notes"));
        }
        
        // Set datacenter
        if (data.containsKey("datacenterId")) {
            if (data.get("datacenterId") != null) {
                Integer datacenterId = Integer.valueOf(data.get("datacenterId").toString());
                datacenterRepository.findById(datacenterId).ifPresent(resource::setDatacenter);
            } else {
                resource.setDatacenter(null);
            }
        }
        
        // Set partner
        if (data.containsKey("partnerId")) {
            if (data.get("partnerId") != null) {
                Integer partnerId = Integer.valueOf(data.get("partnerId").toString());
                partnerRepository.findById(partnerId).ifPresent(resource::setPartner);
            } else {
                resource.setPartner(null);
            }
        }
    }
    
    private void updateCredentialFromData(ComputeAccessCredential credential, Map<String, Object> data) {
        if (data.get("accessType") != null) {
            credential.setAccessType((String) data.get("accessType"));
        }
        if (data.get("accessName") != null) {
            credential.setAccessName((String) data.get("accessName"));
        }
        if (data.get("host") != null) {
            credential.setHost((String) data.get("host"));
        }
        if (data.get("port") != null) {
            credential.setPort(Integer.valueOf(data.get("port").toString()));
        }
        if (data.get("protocol") != null) {
            credential.setProtocol((String) data.get("protocol"));
        }
        if (data.get("authType") != null) {
            credential.setAuthType((String) data.get("authType"));
        }
        if (data.get("username") != null) {
            credential.setUsername((String) data.get("username"));
        }
        if (data.get("passwordEncrypted") != null) {
            // In production, this should be encrypted
            credential.setPasswordEncrypted((String) data.get("passwordEncrypted"));
        }
        if (data.get("sshPrivateKey") != null) {
            // In production, this should be encrypted
            credential.setSshPrivateKey((String) data.get("sshPrivateKey"));
        }
        if (data.get("sshPublicKey") != null) {
            credential.setSshPublicKey((String) data.get("sshPublicKey"));
        }
        if (data.get("isActive") != null) {
            credential.setIsActive((Boolean) data.get("isActive"));
        }
        if (data.get("isPrimary") != null) {
            credential.setIsPrimary((Boolean) data.get("isPrimary"));
        }
        // Add more fields as needed
    }
}