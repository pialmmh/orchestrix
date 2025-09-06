package com.telcobright.orchestrix.controller;

import com.telcobright.orchestrix.entity.Region;
import com.telcobright.orchestrix.entity.Cloud;
import com.telcobright.orchestrix.repository.RegionRepository;
import com.telcobright.orchestrix.repository.CloudRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/regions")
@CrossOrigin(origins = "*")
public class RegionController {
    
    @Autowired
    private RegionRepository regionRepository;
    
    @Autowired
    private CloudRepository cloudRepository;
    
    // Get all regions
    @GetMapping
    public List<Region> getAllRegions() {
        return regionRepository.findAll();
    }
    
    // Get region by ID
    @GetMapping("/{id}")
    public ResponseEntity<?> getRegionById(@PathVariable Long id) {
        return regionRepository.findByIdWithAvailabilityZones(id)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }
    
    // Get regions by cloud
    @GetMapping("/cloud/{cloudId}")
    public List<Region> getRegionsByCloud(@PathVariable Long cloudId) {
        return regionRepository.findByCloudIdWithAvailabilityZones(cloudId);
    }
    
    // Create new region
    @PostMapping
    public ResponseEntity<?> createRegion(@RequestBody Map<String, Object> regionData) {
        try {
            Region region = new Region();
            region.setName((String) regionData.get("name"));
            region.setCode((String) regionData.get("code"));
            region.setGeographicArea((String) regionData.get("geographicArea"));
            region.setComplianceZones((String) regionData.get("complianceZones"));
            region.setDescription((String) regionData.get("description"));
            region.setStatus((String) regionData.getOrDefault("status", "ACTIVE"));
            
            // Set cloud
            if (regionData.get("cloudId") != null) {
                Long cloudId = Long.valueOf(regionData.get("cloudId").toString());
                Cloud cloud = cloudRepository.findById(cloudId).orElse(null);
                if (cloud == null) {
                    return ResponseEntity.badRequest().body("Invalid cloud ID");
                }
                region.setCloud(cloud);
            } else {
                return ResponseEntity.badRequest().body("Cloud ID is required");
            }
            
            Region saved = regionRepository.save(region);
            return ResponseEntity.ok(saved);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Failed to create region: " + e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }
    
    // Update region
    @PutMapping("/{id}")
    public ResponseEntity<?> updateRegion(@PathVariable Long id, @RequestBody Map<String, Object> regionData) {
        return regionRepository.findById(id)
            .map(region -> {
                if (regionData.get("name") != null) {
                    region.setName((String) regionData.get("name"));
                }
                if (regionData.get("code") != null) {
                    region.setCode((String) regionData.get("code"));
                }
                if (regionData.get("geographicArea") != null) {
                    region.setGeographicArea((String) regionData.get("geographicArea"));
                }
                if (regionData.get("complianceZones") != null) {
                    region.setComplianceZones((String) regionData.get("complianceZones"));
                }
                if (regionData.get("description") != null) {
                    region.setDescription((String) regionData.get("description"));
                }
                if (regionData.get("status") != null) {
                    region.setStatus((String) regionData.get("status"));
                }
                
                Region updated = regionRepository.save(region);
                return ResponseEntity.ok(updated);
            })
            .orElse(ResponseEntity.notFound().build());
    }
    
    // Delete region
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteRegion(@PathVariable Long id) {
        return regionRepository.findById(id)
            .map(region -> {
                regionRepository.delete(region);
                Map<String, String> response = new HashMap<>();
                response.put("message", "Region deleted successfully");
                return ResponseEntity.ok(response);
            })
            .orElse(ResponseEntity.notFound().build());
    }
}