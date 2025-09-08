package com.telcobright.orchestrix.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.util.List;
import java.util.Map;

/**
 * Generic credential DTO for all secret providers
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SecretCredentialDto {
    // Basic item info
    private String id;
    private String name;
    private String description;
    private String namespace; // Organization/Collection/Folder/Path depending on provider
    private List<String> tags;
    private Map<String, String> metadata;
    
    // Common credentials
    private String username;
    private String password;
    private String url;
    private String notes;
    
    // SSH credentials
    private String sshKey;
    private String sshKeyPassphrase;
    private String sshPublicKey;
    
    // Certificate credentials  
    private String certificate;
    private String certificateKey;
    private String certificateChain;
    private String caCertificate;
    
    // API credentials
    private String apiKey;
    private String apiSecret;
    private String apiToken;
    private String bearerToken;
    
    // Cloud credentials
    private String accessKeyId;
    private String secretAccessKey;
    private String sessionToken;
    private String region;
    
    // Database credentials
    private String connectionString;
    private String database;
    private String schema;
    
    // OAuth credentials
    private String clientId;
    private String clientSecret;
    private String refreshToken;
    private String scope;
    
    // MFA/TOTP
    private String totpSecret;
    private String mfaBackupCodes;
    
    // Provider-specific fields stored as key-value pairs
    private Map<String, Object> customFields;
    
    // Metadata
    private String createdAt;
    private String updatedAt;
    private String expiresAt;
    private String lastAccessedAt;
    private Integer version;
}