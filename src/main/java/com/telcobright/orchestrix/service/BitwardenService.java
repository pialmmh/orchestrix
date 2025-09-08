package com.telcobright.orchestrix.service;

import com.telcobright.orchestrix.dto.BitwardenItemDto;
import com.telcobright.orchestrix.dto.BitwardenCredentialDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.HttpClientErrorException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;

import javax.annotation.PostConstruct;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Service for integrating with self-hosted Bitwarden/Vaultwarden API
 * Handles credential storage, retrieval, and management
 */
@Service
@Slf4j
public class BitwardenService {
    
    @Value("${bitwarden.api.url:http://localhost:8080}")
    private String bitwardenApiUrl;
    
    @Value("${bitwarden.api.identity-url:http://localhost:8080/identity}")
    private String bitwardenIdentityUrl;
    
    @Value("${bitwarden.client.id:}")
    private String clientId;
    
    @Value("${bitwarden.client.secret:}")
    private String clientSecret;
    
    @Value("${bitwarden.organization.id:}")
    private String defaultOrganizationId;
    
    @Value("${bitwarden.collection.default:Infrastructure}")
    private String defaultCollectionName;
    
    private RestTemplate restTemplate;
    private ObjectMapper objectMapper;
    private String accessToken;
    private LocalDateTime tokenExpiry;
    
    @PostConstruct
    public void init() {
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
    }
    
    /**
     * Authenticate with Bitwarden API using client credentials
     */
    public boolean authenticate() {
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
                tokenExpiry = LocalDateTime.now().plusSeconds(expiresIn - 60); // Refresh 1 minute early
                log.info("Successfully authenticated with Bitwarden API");
                return true;
            }
        } catch (Exception e) {
            log.error("Failed to authenticate with Bitwarden: {}", e.getMessage());
        }
        return false;
    }
    
    /**
     * Ensure we have a valid access token
     */
    private void ensureAuthenticated() {
        if (accessToken == null || tokenExpiry == null || LocalDateTime.now().isAfter(tokenExpiry)) {
            authenticate();
        }
    }
    
    /**
     * Create or update a credential item in Bitwarden
     */
    public BitwardenItemDto saveCredential(BitwardenCredentialDto credential) {
        ensureAuthenticated();
        
        try {
            HttpHeaders headers = createAuthHeaders();
            
            Map<String, Object> item = new HashMap<>();
            item.put("organizationId", credential.getOrganizationId() != null ? 
                credential.getOrganizationId() : defaultOrganizationId);
            item.put("type", 1); // Login type
            item.put("name", credential.getName());
            item.put("notes", credential.getNotes());
            item.put("favorite", false);
            
            // Login details
            Map<String, Object> login = new HashMap<>();
            login.put("username", credential.getUsername());
            login.put("password", credential.getPassword());
            
            // URIs
            if (credential.getUris() != null && !credential.getUris().isEmpty()) {
                List<Map<String, Object>> uris = new ArrayList<>();
                for (String uri : credential.getUris()) {
                    Map<String, Object> uriObj = new HashMap<>();
                    uriObj.put("uri", uri);
                    uriObj.put("match", null);
                    uris.add(uriObj);
                }
                login.put("uris", uris);
            }
            
            // SSH Key
            if (credential.getSshKey() != null) {
                login.put("sshKey", credential.getSshKey());
            }
            
            // TOTP
            if (credential.getTotp() != null) {
                login.put("totp", credential.getTotp());
            }
            
            item.put("login", login);
            
            // Custom fields for additional data
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
            
            if (credential.getPort() != null) {
                fields.add(createCustomField("Port", String.valueOf(credential.getPort()), false));
            }
            
            if (credential.getProtocol() != null) {
                fields.add(createCustomField("Protocol", credential.getProtocol(), false));
            }
            
            if (!fields.isEmpty()) {
                item.put("fields", fields);
            }
            
            // Collection IDs
            if (credential.getCollectionIds() != null && !credential.getCollectionIds().isEmpty()) {
                item.put("collectionIds", credential.getCollectionIds());
            }
            
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(item, headers);
            
            String url = credential.getId() != null ? 
                bitwardenApiUrl + "/api/cipher/" + credential.getId() :
                bitwardenApiUrl + "/api/cipher";
            
            ResponseEntity<Map> response = credential.getId() != null ?
                restTemplate.exchange(url, HttpMethod.PUT, request, Map.class) :
                restTemplate.postForEntity(url, request, Map.class);
            
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return mapToBitwardenItem(response.getBody());
            }
            
        } catch (Exception e) {
            log.error("Failed to save credential to Bitwarden: {}", e.getMessage());
        }
        
        return null;
    }
    
    /**
     * Retrieve a credential item from Bitwarden
     */
    public BitwardenCredentialDto getCredential(String itemId) {
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
                return mapToBitwardenCredential(response.getBody());
            }
            
        } catch (HttpClientErrorException.NotFound e) {
            log.warn("Bitwarden item not found: {}", itemId);
        } catch (Exception e) {
            log.error("Failed to retrieve credential from Bitwarden: {}", e.getMessage());
        }
        
        return null;
    }
    
    /**
     * Delete a credential item from Bitwarden
     */
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
    
    /**
     * List all credentials in an organization
     */
    public List<BitwardenItemDto> listOrganizationCredentials(String organizationId) {
        ensureAuthenticated();
        List<BitwardenItemDto> items = new ArrayList<>();
        
        try {
            HttpHeaders headers = createAuthHeaders();
            HttpEntity<Void> request = new HttpEntity<>(headers);
            
            String orgId = organizationId != null ? organizationId : defaultOrganizationId;
            ResponseEntity<List> response = restTemplate.exchange(
                bitwardenApiUrl + "/api/organizations/" + orgId + "/ciphers",
                HttpMethod.GET,
                request,
                List.class
            );
            
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                for (Object item : response.getBody()) {
                    if (item instanceof Map) {
                        items.add(mapToBitwardenItem((Map) item));
                    }
                }
            }
            
        } catch (Exception e) {
            log.error("Failed to list organization credentials: {}", e.getMessage());
        }
        
        return items;
    }
    
    /**
     * Search credentials by name
     */
    public List<BitwardenItemDto> searchCredentials(String searchTerm) {
        ensureAuthenticated();
        List<BitwardenItemDto> items = new ArrayList<>();
        
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
                        items.add(mapToBitwardenItem(item));
                    }
                }
            }
            
        } catch (Exception e) {
            log.error("Failed to search credentials: {}", e.getMessage());
        }
        
        return items;
    }
    
    /**
     * Get or create a collection
     */
    public String ensureCollection(String collectionName) {
        ensureAuthenticated();
        
        try {
            // First, try to find existing collection
            HttpHeaders headers = createAuthHeaders();
            HttpEntity<Void> request = new HttpEntity<>(headers);
            
            ResponseEntity<List> response = restTemplate.exchange(
                bitwardenApiUrl + "/api/organizations/" + defaultOrganizationId + "/collections",
                HttpMethod.GET,
                request,
                List.class
            );
            
            if (response.getBody() != null) {
                for (Object item : response.getBody()) {
                    if (item instanceof Map) {
                        Map collection = (Map) item;
                        if (collectionName.equals(collection.get("name"))) {
                            return (String) collection.get("id");
                        }
                    }
                }
            }
            
            // Collection doesn't exist, create it
            Map<String, Object> newCollection = new HashMap<>();
            newCollection.put("name", collectionName);
            newCollection.put("organizationId", defaultOrganizationId);
            
            HttpEntity<Map<String, Object>> createRequest = new HttpEntity<>(newCollection, headers);
            ResponseEntity<Map> createResponse = restTemplate.postForEntity(
                bitwardenApiUrl + "/api/organizations/" + defaultOrganizationId + "/collections",
                createRequest,
                Map.class
            );
            
            if (createResponse.getBody() != null) {
                return (String) createResponse.getBody().get("id");
            }
            
        } catch (Exception e) {
            log.error("Failed to ensure collection: {}", e.getMessage());
        }
        
        return null;
    }
    
    /**
     * Test connection to Bitwarden API
     */
    public boolean testConnection() {
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
    
    private HttpHeaders createAuthHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(accessToken);
        return headers;
    }
    
    private Map<String, Object> createCustomField(String name, String value, boolean hidden) {
        Map<String, Object> field = new HashMap<>();
        field.put("name", name);
        field.put("value", value);
        field.put("type", hidden ? 1 : 0); // 1 = hidden, 0 = text
        return field;
    }
    
    private BitwardenItemDto mapToBitwardenItem(Map item) {
        BitwardenItemDto dto = new BitwardenItemDto();
        dto.setId((String) item.get("id"));
        dto.setName((String) item.get("name"));
        dto.setOrganizationId((String) item.get("organizationId"));
        dto.setFolderId((String) item.get("folderId"));
        dto.setNotes((String) item.get("notes"));
        
        if (item.containsKey("collectionIds")) {
            dto.setCollectionIds((List<String>) item.get("collectionIds"));
        }
        
        if (item.containsKey("revisionDate")) {
            dto.setRevisionDate((String) item.get("revisionDate"));
        }
        
        return dto;
    }
    
    private BitwardenCredentialDto mapToBitwardenCredential(Map item) {
        BitwardenCredentialDto dto = new BitwardenCredentialDto();
        dto.setId((String) item.get("id"));
        dto.setName((String) item.get("name"));
        dto.setOrganizationId((String) item.get("organizationId"));
        dto.setNotes((String) item.get("notes"));
        
        if (item.containsKey("login")) {
            Map login = (Map) item.get("login");
            dto.setUsername((String) login.get("username"));
            dto.setPassword((String) login.get("password"));
            dto.setSshKey((String) login.get("sshKey"));
            dto.setTotp((String) login.get("totp"));
            
            if (login.containsKey("uris")) {
                List<Map> uris = (List<Map>) login.get("uris");
                List<String> uriStrings = new ArrayList<>();
                for (Map uri : uris) {
                    uriStrings.add((String) uri.get("uri"));
                }
                dto.setUris(uriStrings);
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
                    case "Port":
                        dto.setPort(Integer.parseInt(fieldValue));
                        break;
                    case "Protocol":
                        dto.setProtocol(fieldValue);
                        break;
                }
            }
        }
        
        if (item.containsKey("collectionIds")) {
            dto.setCollectionIds((List<String>) item.get("collectionIds"));
        }
        
        return dto;
    }
}