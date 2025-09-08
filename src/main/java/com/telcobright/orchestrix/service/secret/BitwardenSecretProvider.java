package com.telcobright.orchestrix.service.secret;

import com.telcobright.orchestrix.dto.SecretCredentialDto;
import com.telcobright.orchestrix.dto.SecretItemDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.HttpClientErrorException;
import com.fasterxml.jackson.databind.ObjectMapper;

import javax.annotation.PostConstruct;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Bitwarden/Vaultwarden implementation of SecretProvider
 */
@Service
@Slf4j
public class BitwardenSecretProvider implements SecretProvider {
    
    @Value("${secret.provider.bitwarden.api.url:http://localhost:8080}")
    private String bitwardenApiUrl;
    
    @Value("${secret.provider.bitwarden.identity.url:http://localhost:8080/identity}")
    private String bitwardenIdentityUrl;
    
    @Value("${secret.provider.bitwarden.client.id:}")
    private String clientId;
    
    @Value("${secret.provider.bitwarden.client.secret:}")
    private String clientSecret;
    
    @Value("${secret.provider.bitwarden.organization.id:}")
    private String defaultOrganizationId;
    
    @Value("${secret.provider.bitwarden.collection.default:Infrastructure}")
    private String defaultCollectionName;
    
    @Value("${secret.provider.bitwarden.enabled:false}")
    private boolean enabled;
    
    private RestTemplate restTemplate;
    private ObjectMapper objectMapper;
    private String accessToken;
    private LocalDateTime tokenExpiry;
    
    @PostConstruct
    public void init() {
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
    }
    
    @Override
    public SecretProviderType getType() {
        return SecretProviderType.BITWARDEN;
    }
    
    @Override
    public boolean testConnection() {
        if (!enabled) {
            log.warn("Bitwarden provider is not enabled");
            return false;
        }
        
        try {
            ResponseEntity<String> response = restTemplate.getForEntity(
                bitwardenApiUrl + "/api/alive",
                String.class
            );
            return response.getStatusCode() == HttpStatus.OK;
        } catch (Exception e) {
            log.error("Bitwarden connection test failed: {}", e.getMessage());
            return false;
        }
    }
    
    @Override
    public boolean authenticate() {
        if (!enabled) {
            return false;
        }
        
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            
            String body = String.format(
                "grant_type=client_credentials&scope=api&client_id=%s&client_secret=%s",
                clientId, clientSecret
            );
            
            HttpEntity<String> request = new HttpEntity<>(body, headers);
            ResponseEntity<Map> response = restTemplate.postForEntity(
                bitwardenIdentityUrl + "/connect/token", 
                request, 
                Map.class
            );
            
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                Map<String, Object> responseBody = response.getBody();
                accessToken = (String) responseBody.get("access_token");
                Integer expiresIn = (Integer) responseBody.get("expires_in");
                tokenExpiry = LocalDateTime.now().plusSeconds(expiresIn - 60);
                log.info("Successfully authenticated with Bitwarden API");
                return true;
            }
        } catch (Exception e) {
            log.error("Failed to authenticate with Bitwarden: {}", e.getMessage());
        }
        return false;
    }
    
    @Override
    public SecretItemDto saveCredential(SecretCredentialDto credential) {
        ensureAuthenticated();
        
        try {
            HttpHeaders headers = createAuthHeaders();
            Map<String, Object> item = convertToBitwardenItem(credential);
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(item, headers);
            
            String url = credential.getId() != null ? 
                bitwardenApiUrl + "/api/cipher/" + credential.getId() :
                bitwardenApiUrl + "/api/cipher";
            
            ResponseEntity<Map> response = credential.getId() != null ?
                restTemplate.exchange(url, HttpMethod.PUT, request, Map.class) :
                restTemplate.postForEntity(url, request, Map.class);
            
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return mapToSecretItem(response.getBody());
            }
        } catch (Exception e) {
            log.error("Failed to save credential to Bitwarden: {}", e.getMessage());
        }
        return null;
    }
    
    @Override
    public SecretCredentialDto getCredential(String itemId) {
        ensureAuthenticated();
        
        try {
            HttpHeaders headers = createAuthHeaders();
            HttpEntity<Void> request = new HttpEntity<>(headers);
            
            ResponseEntity<Map> response = restTemplate.exchange(
                bitwardenApiUrl + "/api/cipher/" + itemId,
                HttpMethod.GET,
                request,
                Map.class
            );
            
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                return mapToSecretCredential(response.getBody());
            }
        } catch (HttpClientErrorException.NotFound e) {
            log.warn("Bitwarden item not found: {}", itemId);
        } catch (Exception e) {
            log.error("Failed to retrieve credential from Bitwarden: {}", e.getMessage());
        }
        return null;
    }
    
    @Override
    public boolean deleteCredential(String itemId) {
        ensureAuthenticated();
        
        try {
            HttpHeaders headers = createAuthHeaders();
            HttpEntity<Void> request = new HttpEntity<>(headers);
            
            restTemplate.exchange(
                bitwardenApiUrl + "/api/cipher/" + itemId,
                HttpMethod.DELETE,
                request,
                Void.class
            );
            
            log.info("Successfully deleted Bitwarden item: {}", itemId);
            return true;
        } catch (Exception e) {
            log.error("Failed to delete credential from Bitwarden: {}", e.getMessage());
        }
        return false;
    }
    
    @Override
    public List<SecretItemDto> listCredentials(String namespace) {
        ensureAuthenticated();
        List<SecretItemDto> items = new ArrayList<>();
        
        try {
            HttpHeaders headers = createAuthHeaders();
            HttpEntity<Void> request = new HttpEntity<>(headers);
            
            String orgId = namespace != null ? namespace : defaultOrganizationId;
            ResponseEntity<List> response = restTemplate.exchange(
                bitwardenApiUrl + "/api/organizations/" + orgId + "/ciphers",
                HttpMethod.GET,
                request,
                List.class
            );
            
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                for (Object item : response.getBody()) {
                    if (item instanceof Map) {
                        items.add(mapToSecretItem((Map) item));
                    }
                }
            }
        } catch (Exception e) {
            log.error("Failed to list organization credentials: {}", e.getMessage());
        }
        return items;
    }
    
    @Override
    public List<SecretItemDto> searchCredentials(String searchTerm) {
        ensureAuthenticated();
        List<SecretItemDto> items = new ArrayList<>();
        
        try {
            HttpHeaders headers = createAuthHeaders();
            HttpEntity<Void> request = new HttpEntity<>(headers);
            
            ResponseEntity<Map> response = restTemplate.exchange(
                bitwardenApiUrl + "/api/ciphers?search=" + searchTerm,
                HttpMethod.GET,
                request,
                Map.class
            );
            
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                Map responseBody = response.getBody();
                if (responseBody.containsKey("Data")) {
                    List<Map> data = (List<Map>) responseBody.get("Data");
                    for (Map item : data) {
                        items.add(mapToSecretItem(item));
                    }
                }
            }
        } catch (Exception e) {
            log.error("Failed to search credentials: {}", e.getMessage());
        }
        return items;
    }
    
    @Override
    public Map<String, String> getConfigurationRequirements() {
        Map<String, String> requirements = new HashMap<>();
        requirements.put("api.url", "Bitwarden/Vaultwarden API URL");
        requirements.put("identity.url", "Identity/Auth endpoint URL");
        requirements.put("client.id", "API Client ID");
        requirements.put("client.secret", "API Client Secret");
        requirements.put("organization.id", "Default Organization ID (optional)");
        requirements.put("collection.default", "Default Collection Name (optional)");
        return requirements;
    }
    
    @Override
    public boolean validateConfiguration(Map<String, String> config) {
        return config.containsKey("api.url") && 
               config.containsKey("client.id") && 
               config.containsKey("client.secret");
    }
    
    @Override
    public boolean isConfigured() {
        return enabled && 
               bitwardenApiUrl != null && !bitwardenApiUrl.isEmpty() &&
               clientId != null && !clientId.isEmpty() &&
               clientSecret != null && !clientSecret.isEmpty();
    }
    
    @Override
    public SecretProviderStatus getStatus() {
        SecretProviderStatus status = new SecretProviderStatus();
        status.setConnected(testConnection());
        status.setAuthenticated(accessToken != null && tokenExpiry != null && 
                                LocalDateTime.now().isBefore(tokenExpiry));
        
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("apiUrl", bitwardenApiUrl);
        metadata.put("organizationId", defaultOrganizationId);
        metadata.put("enabled", enabled);
        status.setMetadata(metadata);
        
        return status;
    }
    
    // Helper methods
    private void ensureAuthenticated() {
        if (accessToken == null || tokenExpiry == null || LocalDateTime.now().isAfter(tokenExpiry)) {
            authenticate();
        }
    }
    
    private HttpHeaders createAuthHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(accessToken);
        return headers;
    }
    
    private Map<String, Object> convertToBitwardenItem(SecretCredentialDto credential) {
        Map<String, Object> item = new HashMap<>();
        item.put("organizationId", credential.getNamespace() != null ? 
            credential.getNamespace() : defaultOrganizationId);
        item.put("type", 1); // Login type
        item.put("name", credential.getName());
        item.put("notes", credential.getNotes());
        item.put("favorite", false);
        
        // Login details
        Map<String, Object> login = new HashMap<>();
        login.put("username", credential.getUsername());
        login.put("password", credential.getPassword());
        
        // URIs
        if (credential.getUrl() != null) {
            List<Map<String, Object>> uris = new ArrayList<>();
            Map<String, Object> uriObj = new HashMap<>();
            uriObj.put("uri", credential.getUrl());
            uriObj.put("match", null);
            uris.add(uriObj);
            login.put("uris", uris);
        }
        
        // SSH Key
        if (credential.getSshKey() != null) {
            login.put("sshKey", credential.getSshKey());
        }
        
        // TOTP
        if (credential.getTotpSecret() != null) {
            login.put("totp", credential.getTotpSecret());
        }
        
        item.put("login", login);
        
        // Custom fields
        List<Map<String, Object>> fields = new ArrayList<>();
        
        if (credential.getSshKeyPassphrase() != null) {
            fields.add(createCustomField("SSH Key Passphrase", credential.getSshKeyPassphrase(), true));
        }
        
        if (credential.getCertificate() != null) {
            fields.add(createCustomField("Certificate", credential.getCertificate(), true));
        }
        
        if (credential.getCertificateKey() != null) {
            fields.add(createCustomField("Certificate Key", credential.getCertificateKey(), true));
        }
        
        if (credential.getApiKey() != null) {
            fields.add(createCustomField("API Key", credential.getApiKey(), true));
        }
        
        if (credential.getApiSecret() != null) {
            fields.add(createCustomField("API Secret", credential.getApiSecret(), true));
        }
        
        if (!fields.isEmpty()) {
            item.put("fields", fields);
        }
        
        return item;
    }
    
    private Map<String, Object> createCustomField(String name, String value, boolean hidden) {
        Map<String, Object> field = new HashMap<>();
        field.put("name", name);
        field.put("value", value);
        field.put("type", hidden ? 1 : 0);
        return field;
    }
    
    private SecretItemDto mapToSecretItem(Map item) {
        SecretItemDto dto = new SecretItemDto();
        dto.setId((String) item.get("id"));
        dto.setName((String) item.get("name"));
        dto.setNamespace((String) item.get("organizationId"));
        dto.setType("LOGIN");
        dto.setProviderType("BITWARDEN");
        dto.setProviderId((String) item.get("id"));
        
        if (item.containsKey("revisionDate")) {
            dto.setUpdatedAt((String) item.get("revisionDate"));
        }
        
        return dto;
    }
    
    private SecretCredentialDto mapToSecretCredential(Map item) {
        SecretCredentialDto dto = new SecretCredentialDto();
        dto.setId((String) item.get("id"));
        dto.setName((String) item.get("name"));
        dto.setNamespace((String) item.get("organizationId"));
        dto.setNotes((String) item.get("notes"));
        
        if (item.containsKey("login")) {
            Map login = (Map) item.get("login");
            dto.setUsername((String) login.get("username"));
            dto.setPassword((String) login.get("password"));
            dto.setSshKey((String) login.get("sshKey"));
            dto.setTotpSecret((String) login.get("totp"));
            
            if (login.containsKey("uris")) {
                List<Map> uris = (List<Map>) login.get("uris");
                if (!uris.isEmpty()) {
                    dto.setUrl((String) uris.get(0).get("uri"));
                }
            }
        }
        
        if (item.containsKey("fields")) {
            List<Map> fields = (List<Map>) item.get("fields");
            for (Map field : fields) {
                String fieldName = (String) field.get("name");
                String fieldValue = (String) field.get("value");
                
                switch (fieldName) {
                    case "SSH Key Passphrase":
                        dto.setSshKeyPassphrase(fieldValue);
                        break;
                    case "Certificate":
                        dto.setCertificate(fieldValue);
                        break;
                    case "Certificate Key":
                        dto.setCertificateKey(fieldValue);
                        break;
                    case "API Key":
                        dto.setApiKey(fieldValue);
                        break;
                    case "API Secret":
                        dto.setApiSecret(fieldValue);
                        break;
                }
            }
        }
        
        return dto;
    }
}