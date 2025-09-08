package com.telcobright.orchestrix.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.util.List;
import java.util.Map;

/**
 * Generic secret item reference DTO for all secret providers
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SecretItemDto {
    private String id;
    private String name;
    private String namespace; // Organization/Collection/Folder/Path
    private String type; // LOGIN, NOTE, CARD, IDENTITY, SSH_KEY, API_KEY, etc.
    private List<String> tags;
    private String createdAt;
    private String updatedAt;
    private String lastAccessedAt;
    private Boolean favorite;
    private Integer version;
    private Map<String, String> metadata;
    
    // Provider-specific reference
    private String providerType;
    private String providerId;
    private String providerPath;
}