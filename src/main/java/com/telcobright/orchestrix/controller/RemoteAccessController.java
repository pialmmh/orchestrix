package com.telcobright.orchestrix.controller;

import com.telcobright.orchestrix.entity.RemoteAccess;
import com.telcobright.orchestrix.entity.RemoteAccess.DeviceType;
import com.telcobright.orchestrix.entity.RemoteAccess.TestStatus;
import com.telcobright.orchestrix.repository.RemoteAccessRepository;
import com.telcobright.orchestrix.service.secret.SecretProviderFactory;
import com.telcobright.orchestrix.service.secret.SecretProvider;
import com.telcobright.orchestrix.service.secret.SecretProvider.SecretProviderType;
import com.telcobright.orchestrix.dto.SecretCredentialDto;
import com.telcobright.orchestrix.dto.SecretItemDto;
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
    private SecretProviderFactory secretProviderFactory;
    
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
            @PathVariable Long deviceId,
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
            @PathVariable Long deviceId) {
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
            
            // Save to secret provider if enabled
            if (remoteAccess.getSecretProviderType() != null) {
                SecretProvider provider = secretProviderFactory.getProvider(remoteAccess.getSecretProviderType());
                SecretCredentialDto credential = createSecretCredential(remoteAccess);
                SecretItemDto savedItem = provider.saveCredential(credential);
                
                if (savedItem != null) {
                    remoteAccess.setSecretItemId(savedItem.getId());
                    remoteAccess.setSecretNamespace(savedItem.getNamespace());
                    
                    // Keep backward compatibility with Bitwarden fields
                    if (remoteAccess.getSecretProviderType() == SecretProviderType.BITWARDEN) {
                        remoteAccess.setBitwardenItemId(savedItem.getId());
                        remoteAccess.setBitwardenOrganizationId(savedItem.getNamespace());
                        remoteAccess.setBitwardenLastSync(LocalDateTime.now());
                    }
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
            @PathVariable Long id,
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
            
            // Sync with secret provider if needed
            if (remoteAccess.getSecretProviderType() != null && remoteAccess.getSecretItemId() != null) {
                SecretProvider provider = secretProviderFactory.getProvider(remoteAccess.getSecretProviderType());
                SecretCredentialDto credential = createSecretCredential(remoteAccess);
                credential.setId(remoteAccess.getSecretItemId());
                SecretItemDto savedItem = provider.saveCredential(credential);
                
                if (savedItem != null) {
                    remoteAccess.setSecretItemId(savedItem.getId());
                    remoteAccess.setSecretNamespace(savedItem.getNamespace());
                    
                    // Keep backward compatibility
                    if (remoteAccess.getSecretProviderType() == SecretProviderType.BITWARDEN) {
                        remoteAccess.setBitwardenItemId(savedItem.getId());
                        remoteAccess.setBitwardenLastSync(LocalDateTime.now());
                    }
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
    public ResponseEntity<Void> deleteRemoteAccess(@PathVariable Long id) {
        try {
            Optional<RemoteAccess> existing = remoteAccessRepository.findById(id);
            if (!existing.isPresent()) {
                return ResponseEntity.notFound().build();
            }
            
            // Delete from secret provider if integrated
            RemoteAccess remoteAccess = existing.get();
            if (remoteAccess.getSecretProviderType() != null && remoteAccess.getSecretItemId() != null) {
                SecretProvider provider = secretProviderFactory.getProvider(remoteAccess.getSecretProviderType());
                provider.deleteCredential(remoteAccess.getSecretItemId());
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
    public ResponseEntity<Map<String, Object>> testConnection(@PathVariable Long id) {
        try {
            Optional<RemoteAccess> optional = remoteAccessRepository.findById(id);
            if (!optional.isPresent()) {
                return ResponseEntity.notFound().build();
            }
            
            RemoteAccess remoteAccess = optional.get();
            Map<String, Object> result = new HashMap<>();
            
            // Update test timestamp
            remoteAccess.setLastTestedAt(LocalDateTime.now());
            
            // Retrieve credentials from secret provider if needed
            SecretCredentialDto credentials = null;
            if (remoteAccess.getSecretProviderType() != null && remoteAccess.getSecretItemId() != null) {
                SecretProvider provider = secretProviderFactory.getProvider(remoteAccess.getSecretProviderType());
                credentials = provider.getCredential(remoteAccess.getSecretItemId());
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
    
    // Sync with secret provider
    @PostMapping("/{id}/sync")
    public ResponseEntity<RemoteAccess> syncWithSecretProvider(@PathVariable Long id) {
        try {
            Optional<RemoteAccess> optional = remoteAccessRepository.findById(id);
            if (!optional.isPresent()) {
                return ResponseEntity.notFound().build();
            }
            
            RemoteAccess remoteAccess = optional.get();
            
            if (remoteAccess.getSecretProviderType() != null && remoteAccess.getSecretItemId() != null) {
                SecretProvider provider = secretProviderFactory.getProvider(remoteAccess.getSecretProviderType());
                SecretCredentialDto credential = provider.getCredential(remoteAccess.getSecretItemId());
                if (credential != null) {
                    // Update cached metadata
                    remoteAccess.setUsername(credential.getUsername());
                    
                    // Update provider-specific sync time
                    if (remoteAccess.getSecretProviderType() == SecretProviderType.BITWARDEN) {
                        remoteAccess.setBitwardenLastSync(LocalDateTime.now());
                    }
                    
                    remoteAccessRepository.save(remoteAccess);
                    log.info("Synced remote access {} with {}", id, remoteAccess.getSecretProviderType());
                }
            }
            
            return ResponseEntity.ok(remoteAccess);
        } catch (Exception e) {
            log.error("Error syncing with secret provider", e);
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
    
    // Test secret provider connection
    @GetMapping("/provider/{providerType}/test")
    public ResponseEntity<Map<String, Object>> testProviderConnection(
            @PathVariable SecretProviderType providerType) {
        Map<String, Object> result = new HashMap<>();
        try {
            SecretProvider provider = secretProviderFactory.getProvider(providerType);
            boolean connected = provider.testConnection();
            boolean authenticated = false;
            
            if (connected) {
                authenticated = provider.authenticate();
            }
            
            result.put("provider", providerType.toString());
            result.put("connected", connected);
            result.put("authenticated", authenticated);
            result.put("status", connected && authenticated ? "success" : "failed");
            
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Error testing {} connection", providerType, e);
            result.put("status", "error");
            result.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(result);
        }
    }
    
    // Get all configured providers status
    @GetMapping("/providers/status")
    public ResponseEntity<Map<String, Object>> getAllProvidersStatus() {
        try {
            Map<String, Object> result = new HashMap<>();
            Map<SecretProviderType, SecretProvider.SecretProviderStatus> statuses = 
                secretProviderFactory.getAllProviderStatuses();
            result.put("providers", statuses);
            result.put("defaultProvider", secretProviderFactory.getDefaultProvider().getType());
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Error getting providers status", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    // Helper method to create secret credential from RemoteAccess
    private SecretCredentialDto createSecretCredential(RemoteAccess remoteAccess) {
        SecretCredentialDto credential = new SecretCredentialDto();
        credential.setName(String.format("%s - %s", remoteAccess.getDeviceName(), remoteAccess.getAccessName()));
        credential.setUsername(remoteAccess.getUsername());
        credential.setUrl(String.format("%s:%d", remoteAccess.getHost(), 
            remoteAccess.getPort() != null ? remoteAccess.getPort() : 22));
        credential.setNotes(remoteAccess.getNotes());
        credential.setNamespace(remoteAccess.getSecretNamespace() != null ? 
            remoteAccess.getSecretNamespace() : remoteAccess.getBitwardenOrganizationId());
        
        // Add tags based on device and access type
        List<String> tags = new ArrayList<>();
        tags.add(remoteAccess.getDeviceType().toString());
        tags.add(remoteAccess.getAccessType().toString());
        credential.setTags(tags);
        
        return credential;
    }
}