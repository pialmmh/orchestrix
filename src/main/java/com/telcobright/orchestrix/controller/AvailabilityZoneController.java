package com.telcobright.orchestrix.controller;

import com.telcobright.orchestrix.entity.AvailabilityZone;
import com.telcobright.orchestrix.entity.Region;
import com.telcobright.orchestrix.repository.AvailabilityZoneRepository;
import com.telcobright.orchestrix.repository.RegionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/availability-zones")
@CrossOrigin(origins = "*")
public class AvailabilityZoneController {
    
    @Autowired
    private AvailabilityZoneRepository availabilityZoneRepository;
    
    @Autowired
    private RegionRepository regionRepository;
    
    // Get all availability zones
    @GetMapping
    public List<AvailabilityZone> getAllAvailabilityZones() {
        return availabilityZoneRepository.findAll();
    }
    
    // Get availability zone by ID
    @GetMapping("/{id}")
    public ResponseEntity<?> getAvailabilityZoneById(@PathVariable Long id) {
        return availabilityZoneRepository.findByIdWithDatacenters(id)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }
    
    // Get availability zones by region
    @GetMapping("/region/{regionId}")
    public List<AvailabilityZone> getAvailabilityZonesByRegion(@PathVariable Long regionId) {
        return availabilityZoneRepository.findByRegionId(regionId);
    }
    
    // Create new availability zone
    @PostMapping
    public ResponseEntity<?> createAvailabilityZone(@RequestBody Map<String, Object> azData) {
        try {
            AvailabilityZone az = new AvailabilityZone();
            az.setName((String) azData.get("name"));
            az.setCode((String) azData.get("code"));
            az.setZoneType((String) azData.getOrDefault("zoneType", "STANDARD"));
            az.setIsDefault((Boolean) azData.getOrDefault("isDefault", false));
            az.setCapabilities((String) azData.get("capabilities"));
            az.setStatus((String) azData.getOrDefault("status", "ACTIVE"));
            
            // Set region
            if (azData.get("regionId") != null) {
                Long regionId = Long.valueOf(azData.get("regionId").toString());
                Region region = regionRepository.findById(regionId).orElse(null);
                if (region == null) {
                    return ResponseEntity.badRequest().body("Invalid region ID");
                }
                az.setRegion(region);
            } else {
                return ResponseEntity.badRequest().body("Region ID is required");
            }
            
            AvailabilityZone saved = availabilityZoneRepository.save(az);
            return ResponseEntity.ok(saved);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Failed to create availability zone: " + e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }
    
    // Update availability zone
    @PutMapping("/{id}")
    public ResponseEntity<?> updateAvailabilityZone(@PathVariable Long id, @RequestBody Map<String, Object> azData) {
        return availabilityZoneRepository.findById(id)
            .map(az -> {
                if (azData.get("name") != null) {
                    az.setName((String) azData.get("name"));
                }
                if (azData.get("code") != null) {
                    az.setCode((String) azData.get("code"));
                }
                if (azData.get("zoneType") != null) {
                    az.setZoneType((String) azData.get("zoneType"));
                }
                if (azData.get("isDefault") != null) {
                    az.setIsDefault((Boolean) azData.get("isDefault"));
                }
                if (azData.get("capabilities") != null) {
                    az.setCapabilities((String) azData.get("capabilities"));
                }
                if (azData.get("status") != null) {
                    az.setStatus((String) azData.get("status"));
                }
                
                AvailabilityZone updated = availabilityZoneRepository.save(az);
                return ResponseEntity.ok(updated);
            })
            .orElse(ResponseEntity.notFound().build());
    }
    
    // Delete availability zone
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteAvailabilityZone(@PathVariable Long id) {
        return availabilityZoneRepository.findById(id)
            .map(az -> {
                availabilityZoneRepository.delete(az);
                Map<String, String> response = new HashMap<>();
                response.put("message", "Availability zone deleted successfully");
                return ResponseEntity.ok(response);
            })
            .orElse(ResponseEntity.notFound().build());
    }
}