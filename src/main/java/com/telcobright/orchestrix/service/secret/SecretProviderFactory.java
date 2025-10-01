package com.telcobright.orchestrix.service.secret;

import com.telcobright.orchestrix.service.secret.SecretProvider.SecretProviderType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Factory for managing and providing secret providers
 */
@Service
public class SecretProviderFactory {

    private static final Logger log = LoggerFactory.getLogger(SecretProviderFactory.class);
    
    @Value("${secret.provider.default:LOCAL_ENCRYPTED}")
    private String defaultProviderType;
    
    @Value("${secret.provider.enabled:LOCAL_ENCRYPTED,BITWARDEN}")
    private List<String> enabledProviders;
    
    private final Map<SecretProviderType, SecretProvider> providers = new HashMap<>();
    private SecretProviderType defaultProvider;
    
    @Autowired(required = false)
    private BitwardenSecretProvider bitwardenProvider;
    
    @Autowired(required = false)
    private LocalEncryptedSecretProvider localProvider;
    
    // @Autowired(required = false)
    // private HashiCorpVaultSecretProvider vaultProvider;
    
    @PostConstruct
    public void init() {
        // Register available providers
        if (bitwardenProvider != null) {
            registerProvider(SecretProviderType.BITWARDEN, bitwardenProvider);
        }
        
        if (localProvider != null) {
            registerProvider(SecretProviderType.LOCAL_ENCRYPTED, localProvider);
        }
        
        // if (vaultProvider != null) {
        //     registerProvider(SecretProviderType.HASHICORP_VAULT, vaultProvider);
        // }
        
        // Set default provider
        try {
            defaultProvider = SecretProviderType.valueOf(defaultProviderType);
        } catch (IllegalArgumentException e) {
            log.warn("Invalid default provider type: {}. Using LOCAL_ENCRYPTED", defaultProviderType);
            defaultProvider = SecretProviderType.LOCAL_ENCRYPTED;
        }
        
        // Ensure local encrypted provider is always available as fallback
        if (!providers.containsKey(SecretProviderType.LOCAL_ENCRYPTED)) {
            registerProvider(SecretProviderType.LOCAL_ENCRYPTED, new LocalEncryptedSecretProvider());
        }
        
        log.info("SecretProviderFactory initialized with {} providers. Default: {}", 
                providers.size(), defaultProvider);
        log.info("Enabled providers: {}", enabledProviders);
    }
    
    /**
     * Register a secret provider
     */
    public void registerProvider(SecretProviderType type, SecretProvider provider) {
        providers.put(type, provider);
        log.info("Registered secret provider: {}", type);
    }
    
    /**
     * Get a specific secret provider
     */
    public SecretProvider getProvider(SecretProviderType type) {
        if (!isProviderEnabled(type)) {
            log.warn("Provider {} is not enabled. Using default provider: {}", type, defaultProvider);
            return getDefaultProvider();
        }
        
        SecretProvider provider = providers.get(type);
        if (provider == null) {
            log.warn("Provider {} not found. Using default provider: {}", type, defaultProvider);
            return getDefaultProvider();
        }
        
        if (!provider.isConfigured()) {
            log.warn("Provider {} is not properly configured. Using default provider: {}", type, defaultProvider);
            return getDefaultProvider();
        }
        
        return provider;
    }
    
    /**
     * Get the default secret provider
     */
    public SecretProvider getDefaultProvider() {
        SecretProvider provider = providers.get(defaultProvider);
        if (provider == null) {
            // Fallback to local encrypted if default is not available
            provider = providers.get(SecretProviderType.LOCAL_ENCRYPTED);
            if (provider == null) {
                throw new IllegalStateException("No secret providers available");
            }
        }
        return provider;
    }
    
    /**
     * Get all available providers
     */
    public Map<SecretProviderType, SecretProvider> getAllProviders() {
        return new HashMap<>(providers);
    }
    
    /**
     * Get all enabled and configured providers
     */
    public Map<SecretProviderType, SecretProvider> getEnabledProviders() {
        return providers.entrySet().stream()
            .filter(entry -> isProviderEnabled(entry.getKey()) && entry.getValue().isConfigured())
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }
    
    /**
     * Check if a provider is enabled
     */
    public boolean isProviderEnabled(SecretProviderType type) {
        return enabledProviders.contains(type.name());
    }
    
    /**
     * Get provider status for all enabled providers
     */
    public Map<SecretProviderType, SecretProvider.SecretProviderStatus> getAllProviderStatuses() {
        Map<SecretProviderType, SecretProvider.SecretProviderStatus> statuses = new HashMap<>();
        
        for (Map.Entry<SecretProviderType, SecretProvider> entry : getEnabledProviders().entrySet()) {
            try {
                statuses.put(entry.getKey(), entry.getValue().getStatus());
            } catch (Exception e) {
                log.error("Error getting status for provider {}: {}", entry.getKey(), e.getMessage());
                SecretProvider.SecretProviderStatus errorStatus = new SecretProvider.SecretProviderStatus();
                errorStatus.setConnected(false);
                errorStatus.setErrorMessage(e.getMessage());
                statuses.put(entry.getKey(), errorStatus);
            }
        }
        
        return statuses;
    }
    
    /**
     * Test all enabled providers
     */
    public Map<SecretProviderType, Boolean> testAllProviders() {
        Map<SecretProviderType, Boolean> results = new HashMap<>();
        
        for (Map.Entry<SecretProviderType, SecretProvider> entry : getEnabledProviders().entrySet()) {
            try {
                boolean result = entry.getValue().testConnection();
                results.put(entry.getKey(), result);
                log.info("Provider {} test result: {}", entry.getKey(), result);
            } catch (Exception e) {
                log.error("Error testing provider {}: {}", entry.getKey(), e.getMessage());
                results.put(entry.getKey(), false);
            }
        }
        
        return results;
    }
    
    /**
     * Get configuration requirements for a provider type
     */
    public Map<String, String> getProviderConfigurationRequirements(SecretProviderType type) {
        SecretProvider provider = providers.get(type);
        if (provider != null) {
            return provider.getConfigurationRequirements();
        }
        return new HashMap<>();
    }
    
    /**
     * Validate configuration for a provider type
     */
    public boolean validateProviderConfiguration(SecretProviderType type, Map<String, String> config) {
        SecretProvider provider = providers.get(type);
        if (provider != null) {
            return provider.validateConfiguration(config);
        }
        return false;
    }
    
    /**
     * Get list of supported provider types with descriptions
     */
    public Map<String, String> getSupportedProviderTypes() {
        Map<String, String> supported = new LinkedHashMap<>();
        for (SecretProviderType type : SecretProviderType.values()) {
            supported.put(type.name(), type.getDisplayName() + " - " + type.getDescription());
        }
        return supported;
    }
    
    /**
     * Get list of available (registered) provider types
     */
    public List<SecretProviderType> getAvailableProviderTypes() {
        return new ArrayList<>(providers.keySet());
    }
}