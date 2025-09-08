package com.telcobright.orchestrix.service.secret;

import com.telcobright.orchestrix.dto.SecretCredentialDto;
import com.telcobright.orchestrix.dto.SecretItemDto;
import java.util.List;
import java.util.Map;

/**
 * Interface for secret management providers (Bitwarden, HashiCorp Vault, AWS Secrets Manager, etc.)
 */
public interface SecretProvider {
    
    /**
     * Get the provider type
     */
    SecretProviderType getType();
    
    /**
     * Test connection to the secret provider
     */
    boolean testConnection();
    
    /**
     * Authenticate with the secret provider
     */
    boolean authenticate();
    
    /**
     * Save or update a credential in the secret provider
     */
    SecretItemDto saveCredential(SecretCredentialDto credential);
    
    /**
     * Retrieve a credential from the secret provider
     */
    SecretCredentialDto getCredential(String itemId);
    
    /**
     * Delete a credential from the secret provider
     */
    boolean deleteCredential(String itemId);
    
    /**
     * List all credentials in a specific namespace/collection/folder
     */
    List<SecretItemDto> listCredentials(String namespace);
    
    /**
     * Search credentials by name or metadata
     */
    List<SecretItemDto> searchCredentials(String searchTerm);
    
    /**
     * Get provider-specific configuration requirements
     */
    Map<String, String> getConfigurationRequirements();
    
    /**
     * Validate provider configuration
     */
    boolean validateConfiguration(Map<String, String> config);
    
    /**
     * Check if the provider is properly configured
     */
    boolean isConfigured();
    
    /**
     * Get provider status and health information
     */
    SecretProviderStatus getStatus();
    
    /**
     * Rotate credentials if supported by provider
     */
    default boolean rotateCredential(String itemId) {
        return false;
    }
    
    /**
     * Get credential history if supported by provider
     */
    default List<SecretItemDto> getCredentialHistory(String itemId) {
        return List.of();
    }
    
    /**
     * Enum for secret provider types
     */
    enum SecretProviderType {
        BITWARDEN("Bitwarden/Vaultwarden", "Self-hosted password manager"),
        HASHICORP_VAULT("HashiCorp Vault", "Enterprise secrets management"),
        AWS_SECRETS_MANAGER("AWS Secrets Manager", "AWS managed secrets service"),
        AZURE_KEY_VAULT("Azure Key Vault", "Azure managed secrets service"),
        GCP_SECRET_MANAGER("GCP Secret Manager", "Google Cloud secrets service"),
        KUBERNETES_SECRETS("Kubernetes Secrets", "K8s native secrets"),
        LOCAL_ENCRYPTED("Local Encrypted", "Locally encrypted storage"),
        CYBERARK("CyberArk", "Enterprise privileged access management"),
        KEEPER("Keeper Security", "Cloud-based password manager"),
        ONEPASSWORD("1Password", "Team password manager"),
        LASTPASS("LastPass", "Cloud password manager"),
        DOPPLER("Doppler", "Developer secrets management"),
        INFISICAL("Infisical", "Open-source secret management"),
        CUSTOM("Custom Provider", "Custom implementation");
        
        private final String displayName;
        private final String description;
        
        SecretProviderType(String displayName, String description) {
            this.displayName = displayName;
            this.description = description;
        }
        
        public String getDisplayName() {
            return displayName;
        }
        
        public String getDescription() {
            return description;
        }
    }
    
    /**
     * Provider status information
     */
    class SecretProviderStatus {
        private boolean connected;
        private boolean authenticated;
        private String version;
        private Long itemCount;
        private String lastSync;
        private String errorMessage;
        private Map<String, Object> metadata;
        
        // Getters and setters
        public boolean isConnected() { return connected; }
        public void setConnected(boolean connected) { this.connected = connected; }
        
        public boolean isAuthenticated() { return authenticated; }
        public void setAuthenticated(boolean authenticated) { this.authenticated = authenticated; }
        
        public String getVersion() { return version; }
        public void setVersion(String version) { this.version = version; }
        
        public Long getItemCount() { return itemCount; }
        public void setItemCount(Long itemCount) { this.itemCount = itemCount; }
        
        public String getLastSync() { return lastSync; }
        public void setLastSync(String lastSync) { this.lastSync = lastSync; }
        
        public String getErrorMessage() { return errorMessage; }
        public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
        
        public Map<String, Object> getMetadata() { return metadata; }
        public void setMetadata(Map<String, Object> metadata) { this.metadata = metadata; }
    }
}