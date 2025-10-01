package com.telcobright.orchestrix.controller;

import com.telcobright.orchestrix.entity.IPAddress;
import com.telcobright.orchestrix.entity.IPAddress.IPAddressType;
import com.telcobright.orchestrix.service.IPAddressService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/ip-addresses")
@CrossOrigin(origins = "*")
public class IPAddressController {

    private static final Logger log = LoggerFactory.getLogger(IPAddressController.class);
    
    @Autowired
    private IPAddressService ipAddressService;
    
    // Get all IP addresses
    @GetMapping
    public ResponseEntity<List<IPAddress>> getAllIpAddresses() {
        return ResponseEntity.ok(ipAddressService.getAllIpAddresses());
    }
    
    // Get IP address by ID
    @GetMapping("/{id}")
    public ResponseEntity<IPAddress> getIpAddressById(@PathVariable Long id) {
        return ipAddressService.getIpAddressById(id)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }
    
    // Get IP address by IP string
    @GetMapping("/by-ip/{ip}")
    public ResponseEntity<IPAddress> getIpAddressByIp(@PathVariable String ip) {
        return ipAddressService.getIpAddressByIp(ip)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }
    
    // Create new IP address
    @PostMapping(consumes = "application/json", produces = "application/json")
    public ResponseEntity<IPAddress> createIpAddress(@RequestBody IPAddress ipAddress) {
        try {
            IPAddress created = ipAddressService.saveIpAddress(ipAddress);
            return ResponseEntity.status(HttpStatus.CREATED).body(created);
        } catch (IllegalArgumentException e) {
            log.error("Error creating IP address: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }
    
    // Update IP address
    @PutMapping(value = "/{id}", consumes = "application/json", produces = "application/json")
    public ResponseEntity<IPAddress> updateIpAddress(@PathVariable Long id, @RequestBody IPAddress ipAddress) {
        return ipAddressService.getIpAddressById(id)
            .map(existing -> {
                ipAddress.setId(id);
                IPAddress updated = ipAddressService.saveIpAddress(ipAddress);
                return ResponseEntity.ok(updated);
            })
            .orElse(ResponseEntity.notFound().build());
    }
    
    // Delete IP address
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteIpAddress(@PathVariable Long id) {
        if (ipAddressService.getIpAddressById(id).isPresent()) {
            ipAddressService.deleteIpAddress(id);
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }
    
    // Get IP addresses for a device
    @GetMapping("/device/{deviceType}/{deviceId}")
    public ResponseEntity<List<IPAddress>> getIpAddressesForDevice(
            @PathVariable String deviceType,
            @PathVariable Long deviceId) {
        try {
            List<IPAddress> ips = ipAddressService.getIpAddressesForDevice(deviceType, deviceId);
            return ResponseEntity.ok(ips);
        } catch (IllegalArgumentException e) {
            log.error("Error getting IP addresses for device: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }
    
    // Assign IP to device
    @PostMapping("/{ipId}/assign/{deviceType}/{deviceId}")
    public ResponseEntity<IPAddress> assignIpToDevice(
            @PathVariable Long ipId,
            @PathVariable String deviceType,
            @PathVariable Long deviceId) {
        try {
            IPAddress assigned = ipAddressService.assignIpToDevice(ipId, deviceType, deviceId);
            return ResponseEntity.ok(assigned);
        } catch (IllegalArgumentException e) {
            log.error("Error assigning IP to device: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }
    
    // Unassign IP from device
    @PostMapping("/{ipId}/unassign")
    public ResponseEntity<IPAddress> unassignIpFromDevice(@PathVariable Long ipId) {
        try {
            IPAddress unassigned = ipAddressService.unassignIpFromDevice(ipId);
            return ResponseEntity.ok(unassigned);
        } catch (IllegalArgumentException e) {
            log.error("Error unassigning IP: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }
    
    // Set primary IP for device
    @PostMapping("/{ipId}/set-primary/{deviceType}/{deviceId}")
    public ResponseEntity<IPAddress> setPrimaryIpForDevice(
            @PathVariable Long ipId,
            @PathVariable String deviceType,
            @PathVariable Long deviceId) {
        try {
            IPAddress primary = ipAddressService.setPrimaryIpForDevice(ipId, deviceType, deviceId);
            return ResponseEntity.ok(primary);
        } catch (IllegalArgumentException e) {
            log.error("Error setting primary IP: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }
    
    // Update IP types
    @PutMapping("/{ipId}/types")
    public ResponseEntity<IPAddress> updateIpTypes(
            @PathVariable Long ipId,
            @RequestBody Set<IPAddressType> types) {
        try {
            IPAddress updated = ipAddressService.setIpTypes(ipId, types);
            return ResponseEntity.ok(updated);
        } catch (IllegalArgumentException e) {
            log.error("Error updating IP types: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }
    
    // Add IP type
    @PostMapping("/{ipId}/types/{type}")
    public ResponseEntity<IPAddress> addIpType(
            @PathVariable Long ipId,
            @PathVariable IPAddressType type) {
        try {
            IPAddress updated = ipAddressService.addIpType(ipId, type);
            return ResponseEntity.ok(updated);
        } catch (IllegalArgumentException e) {
            log.error("Error adding IP type: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }
    
    // Remove IP type
    @DeleteMapping("/{ipId}/types/{type}")
    public ResponseEntity<IPAddress> removeIpType(
            @PathVariable Long ipId,
            @PathVariable IPAddressType type) {
        try {
            IPAddress updated = ipAddressService.removeIpType(ipId, type);
            return ResponseEntity.ok(updated);
        } catch (IllegalArgumentException e) {
            log.error("Error removing IP type: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }
    
    // Get unassigned IP addresses
    @GetMapping("/unassigned")
    public ResponseEntity<List<IPAddress>> getUnassignedIpAddresses() {
        return ResponseEntity.ok(ipAddressService.getUnassignedIpAddresses());
    }
    
    // Get IP addresses by type
    @GetMapping("/by-type/{type}")
    public ResponseEntity<List<IPAddress>> getIpAddressesByType(@PathVariable IPAddressType type) {
        return ResponseEntity.ok(ipAddressService.getIpAddressesByType(type));
    }
    
    // Search IP addresses
    @GetMapping("/search")
    public ResponseEntity<List<IPAddress>> searchIpAddresses(@RequestParam String query) {
        return ResponseEntity.ok(ipAddressService.searchIpAddresses(query));
    }
    
    // Get duplicate IP addresses
    @GetMapping("/duplicates")
    public ResponseEntity<List<String>> getDuplicateIpAddresses() {
        return ResponseEntity.ok(ipAddressService.findDuplicateIpAddresses());
    }
    
    // Create IP range
    @PostMapping("/create-range")
    public ResponseEntity<?> createIpRange(@RequestBody Map<String, Object> request) {
        try {
            String startIp = (String) request.get("startIp");
            String endIp = (String) request.get("endIp");
            Integer subnetMask = request.get("subnetMask") != null ? 
                Integer.valueOf(request.get("subnetMask").toString()) : 24;
            String gateway = (String) request.get("gateway");
            
            Set<IPAddressType> types = new HashSet<>();
            if (request.get("types") != null) {
                List<String> typeStrings = (List<String>) request.get("types");
                for (String typeStr : typeStrings) {
                    types.add(IPAddressType.valueOf(typeStr));
                }
            }
            
            List<IPAddress> created = ipAddressService.createIpRange(startIp, endIp, subnetMask, gateway, types);
            return ResponseEntity.ok(Map.of(
                "created", created.size(),
                "ipAddresses", created
            ));
        } catch (Exception e) {
            log.error("Error creating IP range: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
    
    // Get IP address statistics
    @GetMapping("/statistics")
    public ResponseEntity<Map<String, Object>> getIpAddressStatistics() {
        return ResponseEntity.ok(ipAddressService.getIpAddressStatistics());
    }
    
    // Get all IP address types
    @GetMapping("/types")
    public ResponseEntity<List<Map<String, String>>> getAllIpAddressTypes() {
        List<Map<String, String>> types = new ArrayList<>();
        for (IPAddressType type : IPAddressType.values()) {
            Map<String, String> typeMap = new HashMap<>();
            typeMap.put("value", type.name());
            typeMap.put("label", type.getDisplayName());
            types.add(typeMap);
        }
        return ResponseEntity.ok(types);
    }
    
    // Get all assignment methods
    @GetMapping("/assignment-methods")
    public ResponseEntity<List<Map<String, String>>> getAllAssignmentMethods() {
        List<Map<String, String>> methods = new ArrayList<>();
        for (IPAddress.AssignmentMethod method : IPAddress.AssignmentMethod.values()) {
            Map<String, String> methodMap = new HashMap<>();
            methodMap.put("value", method.name());
            methodMap.put("label", method.getDisplayName());
            methods.add(methodMap);
        }
        return ResponseEntity.ok(methods);
    }
}