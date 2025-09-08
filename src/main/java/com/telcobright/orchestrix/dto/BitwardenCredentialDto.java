package com.telcobright.orchestrix.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BitwardenCredentialDto {
    // Basic item info
    private String id;
    private String organizationId;
    private String name;
    private String notes;
    private List<String> collectionIds;
    
    // Login credentials
    private String username;
    private String password;
    private List<String> uris;
    private String totp; // Time-based One-Time Password
    
    // SSH credentials
    private String sshKey;
    private String sshKeyPassphrase;
    
    // Certificate credentials
    private String certificate;
    private String certificateKey;
    private String caCertificate;
    
    // API credentials
    private String apiKey;
    private String apiSecret;
    private String bearerToken;
    
    // Connection details
    private String host;
    private Integer port;
    private String protocol;
    
    // Additional custom fields
    private Map<String, String> customFields;
}