package com.telcobright.orchestrix.service;

import com.telcobright.orchestrix.entity.*;
import com.telcobright.orchestrix.entity.RemoteAccess.DeviceType;
import com.telcobright.orchestrix.repository.*;
import com.telcobright.orchestrix.service.secret.SecretProvider;
import com.telcobright.orchestrix.service.secret.SecretProviderFactory;
import com.telcobright.orchestrix.dto.SecretCredentialDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import java.util.List;
import java.util.Optional;

/**
 * Service for managing device credentials and their secret provider integration
 */
@Service
@Slf4j
@Transactional
public class DeviceCredentialService {
    
    @Autowired
    private RemoteAccessRepository remoteAccessRepository;
    
    @Autowired
    private SecretProviderFactory secretProviderFactory;
    
    @Autowired
    private ComputeRepository computeRepository;
    
    @Autowired
    private NetworkDeviceRepository networkDeviceRepository;
    
    // @Autowired
    // private StorageRepository storageRepository;
    
    @Autowired
    private ContainerRepository containerRepository;
    
    @Autowired
    private EntityManager entityManager;
    
    /**
     * Get all remote access configurations for a device
     */
    public List<RemoteAccess> getDeviceCredentials(DeviceType deviceType, Long deviceId) {
        return remoteAccessRepository.findByDeviceTypeAndDeviceId(deviceType, deviceId);
    }
    
    /**
     * Get the primary remote access configuration for a device
     */
    public Optional<RemoteAccess> getPrimaryCredential(DeviceType deviceType, Long deviceId) {
        return remoteAccessRepository.findByDeviceTypeAndDeviceIdAndIsPrimary(deviceType, deviceId, true);
    }
    
    /**
     * Get active credentials for a device
     */
    public List<RemoteAccess> getActiveCredentials(DeviceType deviceType, Long deviceId) {
        return remoteAccessRepository.findByDeviceTypeAndDeviceIdAndIsActive(deviceType, deviceId, true);
    }
    
    /**
     * Create a new remote access configuration for a device
     */
    public RemoteAccess createDeviceCredential(DeviceType deviceType, Long deviceId, RemoteAccess remoteAccess) {
        // Set device information
        remoteAccess.setDeviceType(deviceType);
        remoteAccess.setDeviceId(deviceId);
        
        // Get device name for caching
        String deviceName = getDeviceName(deviceType, deviceId);
        remoteAccess.setDeviceName(deviceName);
        
        // Set default values based on device type
        applyDeviceTypeDefaults(deviceType, remoteAccess);
        
        // Handle primary flag
        if (Boolean.TRUE.equals(remoteAccess.getIsPrimary())) {
            clearPrimaryFlag(deviceType, deviceId);
        }
        
        // Save to secret provider if configured
        if (remoteAccess.getSecretProviderType() != null) {
            saveToSecretProvider(remoteAccess);
        }
        
        return remoteAccessRepository.save(remoteAccess);
    }
    
    /**
     * Update an existing remote access configuration
     */
    public RemoteAccess updateDeviceCredential(Long credentialId, RemoteAccess updates) {
        Optional<RemoteAccess> existing = remoteAccessRepository.findById(credentialId);
        if (!existing.isPresent()) {
            throw new RuntimeException("Remote access configuration not found: " + credentialId);
        }
        
        RemoteAccess remoteAccess = existing.get();
        
        // Update fields
        if (updates.getAccessName() != null) remoteAccess.setAccessName(updates.getAccessName());
        if (updates.getAccessType() != null) remoteAccess.setAccessType(updates.getAccessType());
        if (updates.getAccessProtocol() != null) remoteAccess.setAccessProtocol(updates.getAccessProtocol());
        if (updates.getHost() != null) remoteAccess.setHost(updates.getHost());
        if (updates.getPort() != null) remoteAccess.setPort(updates.getPort());
        if (updates.getAuthMethod() != null) remoteAccess.setAuthMethod(updates.getAuthMethod());
        if (updates.getUsername() != null) remoteAccess.setUsername(updates.getUsername());
        
        // Handle primary flag
        if (Boolean.TRUE.equals(updates.getIsPrimary()) && !Boolean.TRUE.equals(remoteAccess.getIsPrimary())) {
            clearPrimaryFlag(remoteAccess.getDeviceType(), remoteAccess.getDeviceId());
            remoteAccess.setIsPrimary(true);
        }
        
        // Update in secret provider if configured
        if (remoteAccess.getSecretProviderType() != null && remoteAccess.getSecretItemId() != null) {
            updateInSecretProvider(remoteAccess);
        }
        
        return remoteAccessRepository.save(remoteAccess);
    }
    
    /**
     * Delete a remote access configuration
     */
    public void deleteDeviceCredential(Long credentialId) {
        Optional<RemoteAccess> existing = remoteAccessRepository.findById(credentialId);
        if (existing.isPresent()) {
            RemoteAccess remoteAccess = existing.get();
            
            // Delete from secret provider if configured
            if (remoteAccess.getSecretProviderType() != null && remoteAccess.getSecretItemId() != null) {
                deleteFromSecretProvider(remoteAccess);
            }
            
            remoteAccessRepository.deleteById(credentialId);
        }
    }
    
    /**
     * Get the actual device entity
     */
    public Object getDevice(DeviceType deviceType, Long deviceId) {
        switch (deviceType) {
            case COMPUTE:
                return computeRepository.findById(deviceId).orElse(null);
            case NETWORK_DEVICE:
                return networkDeviceRepository.findById(deviceId).orElse(null);
            case STORAGE:
                // Storage repository not yet implemented
                return null;
            case CONTAINER:
                return containerRepository.findById(deviceId).orElse(null);
            default:
                return null;
        }
    }
    
    /**
     * Get device name for caching
     */
    private String getDeviceName(DeviceType deviceType, Long deviceId) {
        Object device = getDevice(deviceType, deviceId);
        if (device != null) {
            switch (deviceType) {
                case COMPUTE:
                    return ((Compute) device).getName();
                case NETWORK_DEVICE:
                    return ((NetworkDevice) device).getName();
                case STORAGE:
                    return ((Storage) device).getName();
                case CONTAINER:
                    return ((Container) device).getName();
                default:
                    return "Unknown Device";
            }
        }
        return "Unknown Device";
    }
    
    /**
     * Apply device type specific defaults
     */
    private void applyDeviceTypeDefaults(DeviceType deviceType, RemoteAccess remoteAccess) {
        switch (deviceType) {
            case NETWORK_DEVICE:
                // Network devices often need enable mode
                if (remoteAccess.getEnableModeRequired() == null) {
                    remoteAccess.setEnableModeRequired(true);
                }
                // Default to SSH for network devices
                if (remoteAccess.getAccessType() == null) {
                    remoteAccess.setAccessType(RemoteAccess.AccessType.SSH);
                }
                if (remoteAccess.getPort() == null) {
                    remoteAccess.setPort(22);
                }
                break;
                
            case COMPUTE:
                // Compute resources typically use SSH or RDP
                if (remoteAccess.getAccessType() == null) {
                    remoteAccess.setAccessType(RemoteAccess.AccessType.SSH);
                }
                if (remoteAccess.getPort() == null) {
                    remoteAccess.setPort(22);
                }
                break;
                
            case STORAGE:
                // Storage might use HTTPS API
                if (remoteAccess.getAccessType() == null) {
                    remoteAccess.setAccessType(RemoteAccess.AccessType.HTTPS);
                }
                if (remoteAccess.getPort() == null) {
                    remoteAccess.setPort(443);
                }
                break;
                
            case CONTAINER:
                // Containers might use Docker API
                if (remoteAccess.getAccessType() == null) {
                    remoteAccess.setAccessType(RemoteAccess.AccessType.DOCKER_API);
                }
                if (remoteAccess.getPort() == null) {
                    remoteAccess.setPort(2376);
                }
                break;
        }
    }
    
    /**
     * Clear primary flag for other credentials of the same device
     */
    private void clearPrimaryFlag(DeviceType deviceType, Long deviceId) {
        List<RemoteAccess> existing = remoteAccessRepository.findByDeviceTypeAndDeviceId(deviceType, deviceId);
        for (RemoteAccess ra : existing) {
            if (Boolean.TRUE.equals(ra.getIsPrimary())) {
                ra.setIsPrimary(false);
                remoteAccessRepository.save(ra);
            }
        }
    }
    
    /**
     * Save credentials to secret provider
     */
    private void saveToSecretProvider(RemoteAccess remoteAccess) {
        try {
            SecretProvider provider = secretProviderFactory.getProvider(remoteAccess.getSecretProviderType());
            SecretCredentialDto credential = createSecretCredential(remoteAccess);
            var savedItem = provider.saveCredential(credential);
            
            if (savedItem != null) {
                remoteAccess.setSecretItemId(savedItem.getId());
                remoteAccess.setSecretNamespace(savedItem.getNamespace());
            }
        } catch (Exception e) {
            log.error("Failed to save to secret provider: {}", e.getMessage());
        }
    }
    
    /**
     * Update credentials in secret provider
     */
    private void updateInSecretProvider(RemoteAccess remoteAccess) {
        try {
            SecretProvider provider = secretProviderFactory.getProvider(remoteAccess.getSecretProviderType());
            SecretCredentialDto credential = createSecretCredential(remoteAccess);
            credential.setId(remoteAccess.getSecretItemId());
            provider.saveCredential(credential);
        } catch (Exception e) {
            log.error("Failed to update in secret provider: {}", e.getMessage());
        }
    }
    
    /**
     * Delete credentials from secret provider
     */
    private void deleteFromSecretProvider(RemoteAccess remoteAccess) {
        try {
            SecretProvider provider = secretProviderFactory.getProvider(remoteAccess.getSecretProviderType());
            provider.deleteCredential(remoteAccess.getSecretItemId());
        } catch (Exception e) {
            log.error("Failed to delete from secret provider: {}", e.getMessage());
        }
    }
    
    /**
     * Create secret credential DTO from remote access
     */
    private SecretCredentialDto createSecretCredential(RemoteAccess remoteAccess) {
        SecretCredentialDto credential = new SecretCredentialDto();
        credential.setName(String.format("%s - %s", remoteAccess.getDeviceName(), remoteAccess.getAccessName()));
        credential.setUsername(remoteAccess.getUsername());
        credential.setUrl(String.format("%s:%d", remoteAccess.getHost(), remoteAccess.getPort()));
        credential.setNotes(remoteAccess.getNotes());
        credential.setNamespace(remoteAccess.getSecretNamespace());
        return credential;
    }
    
    /**
     * Create credential templates for device types
     */
    public RemoteAccess createCredentialTemplate(DeviceType deviceType) {
        RemoteAccess template = new RemoteAccess();
        template.setDeviceType(deviceType);
        template.setIsActive(true);
        
        applyDeviceTypeDefaults(deviceType, template);
        
        switch (deviceType) {
            case NETWORK_DEVICE:
                template.setAccessName("Primary SSH Access");
                template.setAuthMethod(RemoteAccess.AuthMethod.PASSWORD);
                template.setEnableModeRequired(true);
                template.setSudoMethod("enable");
                break;
                
            case COMPUTE:
                template.setAccessName("Primary SSH Access");
                template.setAuthMethod(RemoteAccess.AuthMethod.SSH_KEY);
                template.setSudoEnabled(true);
                template.setSudoMethod("sudo");
                break;
                
            case STORAGE:
                template.setAccessName("Management API");
                template.setAuthMethod(RemoteAccess.AuthMethod.API_KEY);
                break;
                
            case CONTAINER:
                template.setAccessName("Container API");
                template.setAuthMethod(RemoteAccess.AuthMethod.CERTIFICATE);
                break;
        }
        
        return template;
    }
}