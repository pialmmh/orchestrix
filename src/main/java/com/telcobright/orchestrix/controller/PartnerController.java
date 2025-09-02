package com.telcobright.orchestrix.controller;

import com.telcobright.orchestrix.entity.Partner;
import com.telcobright.orchestrix.repository.PartnerRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/partners")
@CrossOrigin(origins = "*")
public class PartnerController {
    
    @Autowired
    private PartnerRepository partnerRepository;
    
    // Get all partners
    @GetMapping
    public ResponseEntity<?> getAllPartners(@RequestParam(required = false) String type) {
        List<Partner> partners;
        
        if (type != null) {
            if (type.equals("cloud-provider")) {
                partners = partnerRepository.findCloudProviders();
            } else if (type.equals("vendor")) {
                partners = partnerRepository.findVendors();
            } else {
                partners = partnerRepository.findByType(type);
            }
        } else {
            partners = partnerRepository.findAll();
        }
        
        Map<String, Object> response = new HashMap<>();
        List<Map<String, Object>> partnerList = partners.stream().map(partner -> {
            Map<String, Object> partnerMap = new HashMap<>();
            partnerMap.put("id", partner.getId());
            partnerMap.put("name", partner.getName());
            partnerMap.put("displayName", partner.getDisplayName());
            partnerMap.put("type", partner.getType());
            partnerMap.put("roles", partner.getRoles());
            partnerMap.put("contactEmail", partner.getContactEmail());
            partnerMap.put("contactPhone", partner.getContactPhone());
            partnerMap.put("website", partner.getWebsite());
            partnerMap.put("billingAccountId", partner.getBillingAccountId());
            partnerMap.put("status", partner.getStatus());
            partnerMap.put("createdAt", partner.getCreatedAt());
            partnerMap.put("updatedAt", partner.getUpdatedAt());
            return partnerMap;
        }).collect(Collectors.toList());
        
        response.put("partners", partnerList);
        response.put("total", partnerList.size());
        return ResponseEntity.ok(response);
    }
    
    // Get cloud providers specifically
    @GetMapping("/cloud-providers")
    public ResponseEntity<?> getCloudProviders() {
        List<Partner> cloudProviders = partnerRepository.findCloudProviders();
        
        Map<String, Object> response = new HashMap<>();
        List<Map<String, Object>> providerList = cloudProviders.stream().map(partner -> {
            Map<String, Object> providerMap = new HashMap<>();
            providerMap.put("id", partner.getId());
            providerMap.put("name", partner.getName());
            providerMap.put("displayName", partner.getDisplayName());
            providerMap.put("status", partner.getStatus());
            return providerMap;
        }).collect(Collectors.toList());
        
        response.put("providers", providerList);
        response.put("total", providerList.size());
        return ResponseEntity.ok(response);
    }
    
    // Get partner by ID
    @GetMapping("/{id}")
    public ResponseEntity<?> getPartnerById(@PathVariable Integer id) {
        return partnerRepository.findById(id)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }
    
    // Create new partner
    @PostMapping
    public ResponseEntity<?> createPartner(@RequestBody Partner partner) {
        Partner saved = partnerRepository.save(partner);
        return ResponseEntity.ok(saved);
    }
    
    // Update partner
    @PutMapping("/{id}")
    public ResponseEntity<?> updatePartner(@PathVariable Integer id, @RequestBody Map<String, Object> partnerData) {
        return partnerRepository.findById(id)
            .map(partner -> {
                if (partnerData.get("name") != null) {
                    partner.setName((String) partnerData.get("name"));
                }
                if (partnerData.get("displayName") != null) {
                    partner.setDisplayName((String) partnerData.get("displayName"));
                }
                if (partnerData.get("type") != null) {
                    partner.setType((String) partnerData.get("type"));
                }
                if (partnerData.get("roles") != null) {
                    partner.setRoles((List<String>) partnerData.get("roles"));
                }
                if (partnerData.get("contactEmail") != null) {
                    partner.setContactEmail((String) partnerData.get("contactEmail"));
                }
                if (partnerData.get("contactPhone") != null) {
                    partner.setContactPhone((String) partnerData.get("contactPhone"));
                }
                if (partnerData.get("website") != null) {
                    partner.setWebsite((String) partnerData.get("website"));
                }
                if (partnerData.get("billingAccountId") != null) {
                    partner.setBillingAccountId((String) partnerData.get("billingAccountId"));
                }
                if (partnerData.get("apiKey") != null) {
                    partner.setApiKey((String) partnerData.get("apiKey"));
                }
                if (partnerData.get("apiSecret") != null) {
                    partner.setApiSecret((String) partnerData.get("apiSecret"));
                }
                if (partnerData.get("status") != null) {
                    partner.setStatus((String) partnerData.get("status"));
                }
                
                Partner updated = partnerRepository.save(partner);
                return ResponseEntity.ok(updated);
            })
            .orElse(ResponseEntity.notFound().build());
    }
    
    // Delete partner
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deletePartner(@PathVariable Integer id) {
        return partnerRepository.findById(id)
            .map(partner -> {
                partnerRepository.delete(partner);
                Map<String, String> response = new HashMap<>();
                response.put("message", "Partner deleted successfully");
                return ResponseEntity.ok(response);
            })
            .orElse(ResponseEntity.notFound().build());
    }
}