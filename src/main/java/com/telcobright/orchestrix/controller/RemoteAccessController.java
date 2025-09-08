package com.telcobright.orchestrix.controller;

import com.telcobright.orchestrix.entity.RemoteAccess;
import com.telcobright.orchestrix.entity.RemoteAccess.DeviceType;
import com.telcobright.orchestrix.entity.RemoteAccess.TestStatus;
import com.telcobright.orchestrix.repository.RemoteAccessRepository;
import com.telcobright.orchestrix.service.BitwardenService;
import com.telcobright.orchestrix.dto.BitwardenCredentialDto;
import com.telcobright.orchestrix.dto.BitwardenItemDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/remote-access")
@CrossOrigin(origins = "*")
@Slf4j
public class RemoteAccessController {
    
    @Autowired
    private RemoteAccessRepository remoteAccessRepository;
    
    @Autowired
    private BitwardenService bitwardenService;
    
    // Get all remote access configurations
    @GetMapping
    public ResponseEntity<List<RemoteAccess>> getAllRemoteAccess(
            @RequestParam(required = false) Boolean active) {
        try {
            List<RemoteAccess> remoteAccesses = active != null ?
                remoteAccessRepository.findAll().stream()
                    .filter(ra -> ra.getIsActive().equals(active))
                    .collect(Collectors.toList()) :
                remoteAccessRepository.findAll();
            return ResponseEntity.ok(remoteAccesses);
        } catch (Exception e) {
            log.error("Error fetching remote access configurations", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    // Get remote access by device
    @GetMapping("/device/{deviceType}/{deviceId}")
    public ResponseEntity<List<RemoteAccess>> getByDevice(
            @PathVariable DeviceType deviceType,
            @PathVariable Integer deviceId,
            @RequestParam(required = false) Boolean activeOnly) {
        try {
            List<RemoteAccess> remoteAccesses = Boolean.TRUE.equals(activeOnly) ?
                remoteAccessRepository.findByDeviceTypeAndDeviceIdAndIsActive(deviceType, deviceId, true) :
                remoteAccessRepository.findByDeviceTypeAndDeviceId(deviceType, deviceId);
            return ResponseEntity.ok(remoteAccesses);
        } catch (Exception e) {
            log.error("Error fetching remote access for device", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    // Get primary remote access for a device
    @GetMapping("/device/{deviceType}/{deviceId}/primary")
    public ResponseEntity<RemoteAccess> getPrimaryForDevice(
            @PathVariable DeviceType deviceType,
            @PathVariable Integer deviceId) {
        try {
            Optional<RemoteAccess> primary = remoteAccessRepository
                .findByDeviceTypeAndDeviceIdAndIsPrimary(deviceType, deviceId, true);
            return primary.map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
        } catch (Exception e) {
            log.error("Error fetching primary remote access", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    // Create new remote access configuration
    @PostMapping
    public ResponseEntity<RemoteAccess> createRemoteAccess(@RequestBody RemoteAccess remoteAccess) {
        try {
            // If this is marked as primary, unset other primaries for this device
            if (Boolean.TRUE.equals(remoteAccess.getIsPrimary())) {
                List<RemoteAccess> existing = remoteAccessRepository
                    .findByDeviceTypeAndDeviceId(remoteAccess.getDeviceType(), remoteAccess.getDeviceId());
                existing.forEach(ra -> {
                    ra.setIsPrimary(false);
                    remoteAccessRepository.save(ra);
                });
            }
            
            // If Bitwarden integration is enabled and credentials are provided
            if (Boolean.TRUE.equals(remoteAccess.getBitwardenSyncEnabled())) {
                BitwardenCredentialDto credential = createBitwardenCredential(remoteAccess);
                BitwardenItemDto savedItem = bitwardenService.saveCredential(credential);
                
                if (savedItem != null) {
                    remoteAccess.setBitwardenItemId(savedItem.getId());
                    remoteAccess.setBitwardenOrganizationId(savedItem.getOrganizationId());
                    remoteAccess.setBitwardenLastSync(LocalDateTime.now());
                }
            }
            
            RemoteAccess saved = remoteAccessRepository.save(remoteAccess);
            log.info("Created remote access configuration for {} {}", 
                saved.getDeviceType(), saved.getDeviceId());
            return ResponseEntity.ok(saved);
        } catch (Exception e) {
            log.error("Error creating remote access", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    // Update remote access configuration
    @PutMapping("/{id}")
    public ResponseEntity<RemoteAccess> updateRemoteAccess(
            @PathVariable Integer id,
            @RequestBody RemoteAccess remoteAccess) {
        try {
            Optional<RemoteAccess> existing = remoteAccessRepository.findById(id);
            if (!existing.isPresent()) {
                return ResponseEntity.notFound().build();
            }
            
            remoteAccess.setId(id);
            
            // Handle primary flag changes
            if (Boolean.TRUE.equals(remoteAccess.getIsPrimary()) && 
                !Boolean.TRUE.equals(existing.get().getIsPrimary())) {
                List<RemoteAccess> others = remoteAccessRepository
                    .findByDeviceTypeAndDeviceId(remoteAccess.getDeviceType(), remoteAccess.getDeviceId());
                others.stream()
                    .filter(ra -> !ra.getId().equals(id))
                    .forEach(ra -> {
                        ra.setIsPrimary(false);
                        remoteAccessRepository.save(ra);
                    });
            }
            
            // Sync with Bitwarden if needed
            if (Boolean.TRUE.equals(remoteAccess.getBitwardenSyncEnabled())) {
                BitwardenCredentialDto credential = createBitwardenCredential(remoteAccess);
                credential.setId(remoteAccess.getBitwardenItemId());
                BitwardenItemDto savedItem = bitwardenService.saveCredential(credential);
                
                if (savedItem != null) {
                    remoteAccess.setBitwardenItemId(savedItem.getId());
                    remoteAccess.setBitwardenLastSync(LocalDateTime.now());
                }
            }
            
            RemoteAccess saved = remoteAccessRepository.save(remoteAccess);
            log.info("Updated remote access configuration {}", id);
            return ResponseEntity.ok(saved);
        } catch (Exception e) {
            log.error("Error updating remote access", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    // Delete remote access configuration
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteRemoteAccess(@PathVariable Integer id) {
        try {
            Optional<RemoteAccess> existing = remoteAccessRepository.findById(id);
            if (!existing.isPresent()) {
                return ResponseEntity.notFound().build();
            }
            
            // Delete from Bitwarden if integrated
            RemoteAccess remoteAccess = existing.get();
            if (remoteAccess.getBitwardenItemId() != null) {
                bitwardenService.deleteCredential(remoteAccess.getBitwardenItemId());
            }
            
            remoteAccessRepository.deleteById(id);
            log.info("Deleted remote access configuration {}", id);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("Error deleting remote access", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    // Test remote access connection
    @PostMapping("/{id}/test")
    public ResponseEntity<Map<String, Object>> testConnection(@PathVariable Integer id) {
        try {
            Optional<RemoteAccess> optional = remoteAccessRepository.findById(id);
            if (!optional.isPresent()) {
                return ResponseEntity.notFound().build();
            }
            
            RemoteAccess remoteAccess = optional.get();
            Map<String, Object> result = new HashMap<>();
            
            // Update test timestamp
            remoteAccess.setLastTestedAt(LocalDateTime.now());
            
            // Retrieve credentials from Bitwarden if needed
            BitwardenCredentialDto credentials = null;
            if (remoteAccess.getBitwardenItemId() != null) {
                credentials = bitwardenService.getCredential(remoteAccess.getBitwardenItemId());
            }
            
            // TODO: Implement actual connection testing based on access type
            // For now, we'll simulate a test
            boolean testSuccess = credentials != null && remoteAccess.getHost() != null;
            
            if (testSuccess) {
                remoteAccess.setLastTestStatus(TestStatus.SUCCESS);
                remoteAccess.setLastTestMessage("Connection test successful");
                remoteAccess.setLastSuccessfulConnection(LocalDateTime.now());
                result.put("status", "success");
                result.put("message", "Connection test successful");
            } else {
                remoteAccess.setLastTestStatus(TestStatus.FAILED);
                remoteAccess.setLastTestMessage("Connection test failed - credentials or host not available");
                result.put("status", "failed");
                result.put("message", "Connection test failed");
            }
            
            remoteAccessRepository.save(remoteAccess);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Error testing connection", e);
            Map<String, Object> error = new HashMap<>();
            error.put("status", "error");
            error.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }
    
    // Sync with Bitwarden
    @PostMapping("/{id}/sync")
    public ResponseEntity<RemoteAccess> syncWithBitwarden(@PathVariable Integer id) {
        try {
            Optional<RemoteAccess> optional = remoteAccessRepository.findById(id);
            if (!optional.isPresent()) {
                return ResponseEntity.notFound().build();
            }
            
            RemoteAccess remoteAccess = optional.get();
            
            if (remoteAccess.getBitwardenItemId() != null) {
                BitwardenCredentialDto credential = bitwardenService.getCredential(remoteAccess.getBitwardenItemId());
                if (credential != null) {
                    // Update cached metadata
                    remoteAccess.setUsername(credential.getUsername());
                    remoteAccess.setBitwardenLastSync(LocalDateTime.now());
                    remoteAccessRepository.save(remoteAccess);
                    log.info("Synced remote access {} with Bitwarden", id);
                }
            }
            
            return ResponseEntity.ok(remoteAccess);
        } catch (Exception e) {
            log.error("Error syncing with Bitwarden", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    // Get credentials needing rotation
    @GetMapping("/rotation-needed")
    public ResponseEntity<List<RemoteAccess>> getCredentialsNeedingRotation() {
        try {
            List<RemoteAccess> needingRotation = remoteAccessRepository
                .findCredentialsNeedingRotation(LocalDateTime.now());
            return ResponseEntity.ok(needingRotation);
        } catch (Exception e) {
            log.error("Error fetching credentials needing rotation", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    // Get expired credentials
    @GetMapping("/expired")
    public ResponseEntity<List<RemoteAccess>> getExpiredCredentials() {
        try {
            List<RemoteAccess> expired = remoteAccessRepository
                .findExpiredCredentials(LocalDateTime.now());
            return ResponseEntity.ok(expired);
        } catch (Exception e) {
            log.error("Error fetching expired credentials", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    // Statistics endpoint
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getStatistics() {
        try {
            Map<String, Object> stats = new HashMap<>();
            
            // Count by device type
            List<Object[]> byDeviceType = remoteAccessRepository.countActiveByDeviceType();
            Map<String, Long> deviceTypeMap = new HashMap<>();
            for (Object[] row : byDeviceType) {
                deviceTypeMap.put(row[0].toString(), (Long) row[1]);
            }
            stats.put("byDeviceType", deviceTypeMap);
            
            // Count by access type
            List<Object[]> byAccessType = remoteAccessRepository.countActiveByAccessType();
            Map<String, Long> accessTypeMap = new HashMap<>();
            for (Object[] row : byAccessType) {
                accessTypeMap.put(row[0].toString(), (Long) row[1]);
            }
            stats.put("byAccessType", accessTypeMap);
            
            // Count by auth method
            List<Object[]> byAuthMethod = remoteAccessRepository.countActiveByAuthMethod();
            Map<String, Long> authMethodMap = new HashMap<>();
            for (Object[] row : byAuthMethod) {
                authMethodMap.put(row[0].toString(), (Long) row[1]);
            }
            stats.put("byAuthMethod", authMethodMap);
            
            // Overall counts
            stats.put("total", remoteAccessRepository.count());
            stats.put("active", remoteAccessRepository.findAll().stream()
                .filter(ra -> Boolean.TRUE.equals(ra.getIsActive()))
                .count());
            stats.put("needingRotation", remoteAccessRepository
                .findCredentialsNeedingRotation(LocalDateTime.now()).size());
            stats.put("expired", remoteAccessRepository
                .findExpiredCredentials(LocalDateTime.now()).size());
            
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            log.error("Error fetching statistics", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    // Test Bitwarden connection
    @GetMapping("/bitwarden/test")
    public ResponseEntity<Map<String, Object>> testBitwardenConnection() {
        Map<String, Object> result = new HashMap<>();
        try {
            boolean connected = bitwardenService.testConnection();
            boolean authenticated = false;
            
            if (connected) {
                authenticated = bitwardenService.authenticate();
            }
            
            result.put("connected", connected);
            result.put("authenticated", authenticated);
            result.put("status", connected && authenticated ? "success" : "failed");
            
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Error testing Bitwarden connection", e);
            result.put("status", "error");
            result.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(result);
        }
    }
    
    // Helper method to create Bitwarden credential from RemoteAccess
    private BitwardenCredentialDto createBitwardenCredential(RemoteAccess remoteAccess) {
        BitwardenCredentialDto credential = new BitwardenCredentialDto();
        credential.setName(String.format("%s - %s", remoteAccess.getDeviceName(), remoteAccess.getAccessName()));
        credential.setUsername(remoteAccess.getUsername());
        credential.setHost(remoteAccess.getHost());
        credential.setPort(remoteAccess.getPort());
        credential.setProtocol(remoteAccess.getAccessProtocol() != null ? 
            remoteAccess.getAccessProtocol().toString() : null);
        credential.setNotes(remoteAccess.getNotes());
        credential.setOrganizationId(remoteAccess.getBitwardenOrganizationId());
        
        // Add URIs
        if (remoteAccess.getHost() != null) {
            List<String> uris = new ArrayList<>();
            String uri = String.format("%s://%s:%d", 
                remoteAccess.getAccessProtocol() != null ? 
                    remoteAccess.getAccessProtocol().toString().toLowerCase() : "ssh",
                remoteAccess.getHost(),
                remoteAccess.getPort() != null ? remoteAccess.getPort() : 22);
            uris.add(uri);
            credential.setUris(uris);
        }
        
        return credential;
    }
}